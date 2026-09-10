package com.hdl.mt.transfer.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Lớp bọc RESILIENCE quanh cổng {@link AccountPort} (cách 2): circuit breaker + retry đặt MỘT
 * CHỖ, dùng chung cho cả hai adapter. {@code @Primary} nên TransferService nhận lớp này; nó
 * inject adapter thô qua {@code @Qualifier("rawAccountPort")}.
 *
 * <p>BƯỚC 8: truyền {@code operationId} xuống delegate (khóa idempotency). Vì operationId là
 * THAM SỐ cố định trước khi vào retry, các lần retry dùng lại đúng khóa -> account-service khử
 * trùng -> retry không gây double-debit.</p>
 */
@Component
@Primary
public class ResilientAccountPort implements AccountPort {

    private static final Logger log = LoggerFactory.getLogger(ResilientAccountPort.class);

    private final AccountPort delegate;

    public ResilientAccountPort(@Qualifier("rawAccountPort") AccountPort delegate) {
        this.delegate = delegate;
    }

    @Override
    @CircuitBreaker(name = "account", fallbackMethod = "debitFallback")
    @Retry(name = "account")
    public void debit(Long accountId, BigDecimal amount, String operationId) {
        delegate.debit(accountId, amount, operationId);
    }

    @Override
    @CircuitBreaker(name = "account", fallbackMethod = "creditFallback")
    @Retry(name = "account")
    public void credit(Long accountId, BigDecimal amount, String operationId) {
        delegate.credit(accountId, amount, operationId);
    }

    // ----- Fallback (chữ ký = tham số gốc + Throwable ở cuối) -----

    public void debitFallback(Long accountId, BigDecimal amount, String operationId, Throwable t) {
        throw translate("debit", accountId, t);
    }

    public void creditFallback(Long accountId, BigDecimal amount, String operationId, Throwable t) {
        throw translate("credit", accountId, t);
    }

    /**
     * Dịch lỗi thành exception domain, và QUYẾT ĐỊNH lỗi đó có được retry tiếp không.
     *
     * <p>Đây là chỗ sửa lỗi "retry thử lại khi circuit đã OPEN" (phát hiện ở bước 7):
     * <ul>
     *   <li>Lỗi nghiệp vụ -> ném nguyên {@link AccountBusinessException} (đã cấu hình không retry).</li>
     *   <li>Circuit đang OPEN ({@link CallNotPermittedException}) -> ném {@link AccountClientException}
     *       BASE (KHÔNG nằm trong retry-exceptions) -> retry KHÔNG thử lại -> fail nhanh, 1 lần.
     *       (Trước đây ta ném AccountUnavailableException nên retry cứ thử lại cái đã bị chặn 3 lần.)</li>
     *   <li>Lỗi hạ tầng thật (5xx/timeout) -> {@link AccountUnavailableException} (được retry).</li>
     * </ul></p>
     */
    private RuntimeException translate(String op, Long accountId, Throwable t) {
        if (t instanceof AccountBusinessException businessError) {
            return businessError;
        }
        if (t instanceof CallNotPermittedException) {
            log.warn("{} bị chặn: circuit 'account' đang OPEN (accountId={})", op, accountId);
            return new AccountClientException(op + ": circuit 'account' đang OPEN, bỏ qua gọi", t);
        }
        log.warn("{} fallback (accountId={}): account-service không khả dụng: {}", op, accountId, t.toString());
        return new AccountUnavailableException(op + ": account-service không khả dụng", t);
    }
}
