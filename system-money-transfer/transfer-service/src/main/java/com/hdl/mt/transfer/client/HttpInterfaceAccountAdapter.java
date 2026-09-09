package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.AccountApi;
import com.hdl.mt.account.api.CreditRequest;
import com.hdl.mt.account.api.DebitRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Adapter "thô" (raw) dùng HTTP Interface (proxy {@link AccountApi}) để gọi account-service.
 *
 * <p>Kích hoạt khi {@code account-service.client=http-interface} (mặc định). Đánh dấu
 * {@code @Qualifier("rawAccountPort")} để {@link ResilientAccountPort} (lớp bọc resilience)
 * biết đây là delegate cần bọc, còn {@code TransferService} thì nhận lớp bọc (Primary).</p>
 *
 * <p>BƯỚC 7 — phân loại lỗi (điểm cốt lõi để resilience xử lý ĐÚNG):
 * <ul>
 *   <li>account trả 4xx ({@link HttpClientErrorException}) -> {@link AccountBusinessException}:
 *       lỗi nghiệp vụ, KHÔNG retry.</li>
 *   <li>account trả 5xx / mất kết nối / timeout (các {@link RestClientException} còn lại) ->
 *       {@link AccountUnavailableException}: lỗi hạ tầng, ĐƯỢC retry + tính vào circuit breaker.</li>
 * </ul>
 * (4xx là con của RestClientException nên phải catch {@code HttpClientErrorException} TRƯỚC.)</p>
 */
@Component
@Qualifier("rawAccountPort")
@ConditionalOnProperty(name = "account-service.client", havingValue = "http-interface", matchIfMissing = true)
public class HttpInterfaceAccountAdapter implements AccountPort {

    private final AccountApi accountApi;

    public HttpInterfaceAccountAdapter(AccountApi accountApi) {
        this.accountApi = accountApi;
    }

    @Override
    public void debit(Long accountId, BigDecimal amount) {
        try {
            accountApi.debit(accountId, new DebitRequest(amount));
        } catch (HttpClientErrorException e) {          // 4xx -> nghiệp vụ
            throw new AccountBusinessException(
                    "debit bị account-service từ chối (4xx, accountId=" + accountId + ")", e);
        } catch (RestClientException e) {               // 5xx / I/O / timeout -> hạ tầng
            throw new AccountUnavailableException(
                    "debit không gọi được account-service (accountId=" + accountId + ")", e);
        }
    }

    @Override
    public void credit(Long accountId, BigDecimal amount) {
        try {
            accountApi.credit(accountId, new CreditRequest(amount));
        } catch (HttpClientErrorException e) {
            throw new AccountBusinessException(
                    "credit bị account-service từ chối (4xx, accountId=" + accountId + ")", e);
        } catch (RestClientException e) {
            throw new AccountUnavailableException(
                    "credit không gọi được account-service (accountId=" + accountId + ")", e);
        }
    }
}
