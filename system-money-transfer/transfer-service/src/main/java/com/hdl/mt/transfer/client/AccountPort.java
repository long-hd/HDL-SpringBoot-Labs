package com.hdl.mt.transfer.client;

import java.math.BigDecimal;

/**
 * Cổng (port) mà transfer-service dùng để tác động lên tài khoản.
 *
 * <p>Vì sao có lớp trừu tượng này (ports-and-adapters): {@code TransferService} chỉ nên
 * quan tâm "trừ tiền / cộng tiền", KHÔNG nên biết việc đó thực hiện bằng OpenFeign hay
 * HTTP Interface. Nhờ vậy, đổi cơ chế client (bước 2 làm cả hai để so sánh) hay đổi cách
 * định địa chỉ (bước 3 dùng discovery) đều KHÔNG phải sửa {@code TransferService}.</p>
 *
 * <p>Cổng dùng ngôn ngữ nghiệp vụ (accountId, amount) và ném {@link AccountClientException}
 * chung — đây là ranh giới chống ăn mòn (anti-corruption): mỗi adapter tự dịch lỗi hạ tầng
 * riêng của nó (RestClientException của HTTP Interface, FeignException của OpenFeign) sang
 * một loại lỗi domain duy nhất, để tầng nghiệp vụ không bị dính vào chi tiết thư viện.</p>
 */
public interface AccountPort {

    /** Trừ {@code amount} khỏi tài khoản {@code accountId}. */
    void debit(Long accountId, BigDecimal amount);

    /** Cộng {@code amount} vào tài khoản {@code accountId}. */
    void credit(Long accountId, BigDecimal amount);
}
