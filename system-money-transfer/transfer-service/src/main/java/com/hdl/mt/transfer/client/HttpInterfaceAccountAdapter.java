package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.AccountApi;
import com.hdl.mt.account.api.CreditRequest;
import com.hdl.mt.account.api.DebitRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Adapter dùng HTTP Interface (proxy {@link AccountApi}) để hiện thực {@link AccountPort}.
 *
 * <p>Kích hoạt khi {@code account-service.client=http-interface}. Đây cũng là MẶC ĐỊNH
 * ({@code matchIfMissing=true}) — nếu không cấu hình gì thì dùng adapter này.</p>
 *
 * <p>Nhiệm vụ chống ăn mòn: bắt {@link RestClientException} (lỗi mà proxy RestClient ném khi
 * account-service trả 4xx/5xx hoặc không kết nối được) và dịch sang {@link AccountClientException}
 * chung, để {@code TransferService} không phải biết tới kiểu lỗi của HTTP Interface.</p>
 */
@Component
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
        } catch (RestClientException ex) {
            throw new AccountClientException(
                    "Gọi debit qua HTTP Interface thất bại (accountId=" + accountId + ")", ex);
        }
    }

    @Override
    public void credit(Long accountId, BigDecimal amount) {
        try {
            accountApi.credit(accountId, new CreditRequest(amount));
        } catch (RestClientException ex) {
            throw new AccountClientException(
                    "Gọi credit qua HTTP Interface thất bại (accountId=" + accountId + ")", ex);
        }
    }
}
