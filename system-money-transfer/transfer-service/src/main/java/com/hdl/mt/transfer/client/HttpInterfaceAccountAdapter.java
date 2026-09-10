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
 * Adapter "thô" (raw) dùng HTTP Interface (proxy {@link AccountApi}). Mặc định
 * ({@code account-service.client=http-interface}). Bọc bởi {@link ResilientAccountPort}.
 *
 * <p>Phân loại lỗi (bước 7): 4xx -> {@link AccountBusinessException} (không retry);
 * 5xx/timeout/mất kết nối -> {@link AccountUnavailableException} (retry + circuit breaker).
 * Bước 8: truyền thêm {@code operationId} vào DTO để account-service khử trùng.</p>
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
    public void debit(Long accountId, BigDecimal amount, String operationId) {
        try {
            accountApi.debit(accountId, new DebitRequest(operationId, amount));
        } catch (HttpClientErrorException e) {
            throw new AccountBusinessException(
                    "debit bị account-service từ chối (4xx, accountId=" + accountId + ")", e);
        } catch (RestClientException e) {
            throw new AccountUnavailableException(
                    "debit không gọi được account-service (accountId=" + accountId + ")", e);
        }
    }

    @Override
    public void credit(Long accountId, BigDecimal amount, String operationId) {
        try {
            accountApi.credit(accountId, new CreditRequest(operationId, amount));
        } catch (HttpClientErrorException e) {
            throw new AccountBusinessException(
                    "credit bị account-service từ chối (4xx, accountId=" + accountId + ")", e);
        } catch (RestClientException e) {
            throw new AccountUnavailableException(
                    "credit không gọi được account-service (accountId=" + accountId + ")", e);
        }
    }
}
