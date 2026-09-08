package com.hdl.mt.transfer.client;

import com.hdl.mt.account.api.CreditRequest;
import com.hdl.mt.account.api.DebitRequest;
import feign.FeignException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter dùng OpenFeign ({@link AccountFeignClient}) để hiện thực {@link AccountPort}.
 *
 * <p>Kích hoạt khi {@code account-service.client=feign}. Khi đó adapter HTTP Interface
 * (mặc định) không được tạo, và {@code TransferService} nhận đúng adapter này — mà bản thân
 * {@code TransferService} KHÔNG hề biết đã đổi client. Đó là lợi ích của {@link AccountPort}.</p>
 *
 * <p>Chống ăn mòn: OpenFeign ném {@link FeignException} khi account-service trả lỗi; adapter
 * bắt và dịch sang {@link AccountClientException} chung — cùng loại lỗi mà adapter HTTP
 * Interface ném ra, nên tầng nghiệp vụ xử lý đồng nhất bất kể client nào.</p>
 */
@Component
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
        } catch (FeignException ex) {
            throw new AccountClientException(
                    "Gọi debit qua OpenFeign thất bại (accountId=" + accountId + ")", ex);
        }
    }

    @Override
    public void credit(Long accountId, BigDecimal amount) {
        try {
            feignClient.credit(accountId, new CreditRequest(amount));
        } catch (FeignException ex) {
            throw new AccountClientException(
                    "Gọi credit qua OpenFeign thất bại (accountId=" + accountId + ")", ex);
        }
    }
}
