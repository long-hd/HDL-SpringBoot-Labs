package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.CreditRequest;
import com.hdl.mt.account.api.DebitRequest;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter "thô" (raw) dùng OpenFeign để gọi account-service.
 *
 * <p>Kích hoạt khi {@code account-service.client=feign}. Cũng đánh dấu
 * {@code @Qualifier("rawAccountPort")} để {@link ResilientAccountPort} bọc lên.</p>
 *
 * <p>BƯỚC 7 — phân loại lỗi giống adapter HTTP Interface, nhưng đọc mã trạng thái từ
 * {@link FeignException#status()}:
 * <ul>
 *   <li>4xx -> {@link AccountBusinessException} (không retry).</li>
 *   <li>còn lại: 5xx, hoặc {@code status()} âm/không có khi mất kết nối/timeout ->
 *       {@link AccountUnavailableException} (retry + circuit breaker).</li>
 * </ul></p>
 */
@Component
@Qualifier("rawAccountPort")
@ConditionalOnProperty(name = "account-service.client", havingValue = "feign")
public class FeignAccountAdapter implements AccountPort {

    private final AccountFeignClient feignClient;

    public FeignAccountAdapter(AccountFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    public void debit(Long accountId, BigDecimal amount) {
        try {
            feignClient.debit(accountId, new DebitRequest(amount));
        } catch (FeignException e) {
            throw classify("debit", accountId, e);
        }
    }

    @Override
    public void credit(Long accountId, BigDecimal amount) {
        try {
            feignClient.credit(accountId, new CreditRequest(amount));
        } catch (FeignException e) {
            throw classify("credit", accountId, e);
        }
    }

    /** 4xx -> nghiệp vụ (không retry); còn lại -> hạ tầng (retry + circuit breaker). */
    private AccountClientException classify(String op, Long accountId, FeignException e) {
        int status = e.status();
        if (status >= 400 && status < 500) {
            return new AccountBusinessException(
                    op + " bị account-service từ chối (" + status + ", accountId=" + accountId + ")", e);
        }
        return new AccountUnavailableException(
                op + " không gọi được account-service (status=" + status + ", accountId=" + accountId + ")", e);
    }
}
