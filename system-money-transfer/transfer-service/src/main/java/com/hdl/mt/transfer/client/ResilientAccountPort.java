package com.hdl.mt.transfer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Lớp bọc RESILIENCE quanh cổng {@link AccountPort} (cách 2 đã chốt): đặt circuit breaker +
 * retry MỘT CHỖ, dùng chung cho cả adapter HTTP Interface lẫn OpenFeign.
 *
 * <p>Cấu trúc bean:
 * <ul>
 *   <li>Hai adapter thô mang {@code @Qualifier("rawAccountPort")}; chỉ một cái hoạt động (theo
 *       {@code account-service.client}).</li>
 *   <li>Lớp này {@code @Primary}, nên khi {@code TransferService} xin một {@link AccountPort},
 *       nó nhận lớp bọc này — không phải adapter thô. Còn lớp bọc thì inject adapter thô qua
 *       {@code @Qualifier("rawAccountPort")}. Nhờ vậy {@code TransferService} KHÔNG đổi dòng nào.</li>
 * </ul></p>
 *
 * <p>Hai tầng resilience: đây là tầng ỨNG DỤNG (trong code, biết được ý nghĩa nghiệp vụ để chọn
 * fallback). Khác với tầng HẠ TẦNG (service mesh) — cái đó nằm ngoài code, chưa dùng ở dự án này.</p>
 *
 * <p><b>Vì sao KHÔNG dùng {@code @TimeLimiter}:</b> {@code @TimeLimiter} chỉ áp cho lời gọi bất
 * đồng bộ (trả {@code CompletableFuture}/{@code Mono}). Lời gọi ở đây đồng bộ (void), nên timeout
 * được đặt ở TẦNG HTTP CLIENT (connect/read timeout của RestClient và OpenFeign) — xem
 * AccountClientConfig và application.yml.</p>
 *
 * <p><b>⚠️ Cảnh báo retry + idempotency (bài học nối sang bước 8):</b> {@code @Retry} gọi lại
 * lời gọi đã thất bại. Nếu debit thực chất ĐÃ chạy ở account-service nhưng phản hồi bị mất
 * (timeout sau khi xử lý), thì retry sẽ TRỪ TIỀN LẦN HAI. Retry chỉ an toàn khi thao tác
 * IDEMPOTENT — mà idempotency chưa có (bước 8 mới thêm khoá idempotency). Vì thế bước 7 chỉ
 * retry loại lỗi {@link AccountUnavailableException}; nhưng phải nhớ rủi ro này cho tới bước 8.</p>
 */
@Component
@Primary
public class ResilientAccountPort implements AccountPort {

    private static final Logger log = LoggerFactory.getLogger(ResilientAccountPort.class);

    /** Adapter thô được bọc (HTTP Interface hoặc Feign, tuỳ cấu hình). */
    private final AccountPort delegate;

    public ResilientAccountPort(@Qualifier("rawAccountPort") AccountPort delegate) {
        this.delegate = delegate;
    }

    @Override
    @CircuitBreaker(name = "account", fallbackMethod = "debitFallback")
    @Retry(name = "account")
    public void debit(Long accountId, BigDecimal amount) {
        delegate.debit(accountId, amount);
    }

    @Override
    @CircuitBreaker(name = "account", fallbackMethod = "creditFallback")
    @Retry(name = "account")
    public void credit(Long accountId, BigDecimal amount) {
        delegate.credit(accountId, amount);
    }

    /**
     * Fallback của debit — được gọi khi lỗi hạ tầng đã hết retry, hoặc khi circuit đang MỞ
     * (Resilience4j ném CallNotPermittedException). Lỗi nghiệp vụ ({@link AccountBusinessException})
     * đã được cấu hình "ignore" nên KHÔNG vào đây; guard instanceof chỉ để phòng hờ và ném
     * nguyên trạng, tránh nuốt mất lỗi nghiệp vụ.
     */
    public void debitFallback(Long accountId, BigDecimal amount, Throwable t) {
        if (t instanceof AccountBusinessException businessError) {
            throw businessError;
        }
        log.warn("debit fallback (accountId={}): account-service không khả dụng: {}", accountId, t.toString());
        throw new AccountUnavailableException("debit: account-service không khả dụng", t);
    }

    public void creditFallback(Long accountId, BigDecimal amount, Throwable t) {
        if (t instanceof AccountBusinessException businessError) {
            throw businessError;
        }
        log.warn("credit fallback (accountId={}): account-service không khả dụng: {}", accountId, t.toString());
        throw new AccountUnavailableException("credit: account-service không khả dụng", t);
    }
}
