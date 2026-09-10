package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.CreditRequest;
import com.hdl.mt.account.api.DebitRequest;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter "thô" (raw) dùng OpenFeign ({@code account-service.client=feign}). Bọc bởi
 * {@link ResilientAccountPort}. Phân loại lỗi qua {@link FeignException#status()}:
 * 4xx -> nghiệp vụ (không retry); còn lại -> hạ tầng (retry + circuit breaker).
 * Bước 8: truyền thêm {@code operationId} vào DTO để account-service khử trùng.
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
    public void debit(Long accountId, BigDecimal amount, String operationId) {
        try {
            feignClient.debit(accountId, new DebitRequest(operationId, amount));
        } catch (FeignException e) {
            throw classify("debit", accountId, e);
        }
    }

    @Override
    public void credit(Long accountId, BigDecimal amount, String operationId) {
        try {
            feignClient.credit(accountId, new CreditRequest(operationId, amount));
        } catch (FeignException e) {
            throw classify("credit", accountId, e);
        }
    }

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
