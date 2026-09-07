package com.hdl.mt.account.api;

import java.math.BigDecimal;

/**
 * Yêu cầu TRỪ tiền khỏi một tài khoản.
 *
 * <p>Cố ý là một DTO riêng (không dùng chung với credit) để mỗi thao tác có tên rõ
 * nghĩa nghiệp vụ ở tầng API — người đọc code/log biết ngay đây là lệnh trừ tiền,
 * thay vì một "amount" chung chung không rõ chiều.</p>
 *
 * @param amount số tiền cần trừ (phải dương; account-service sẽ kiểm tra và từ chối
 *               nếu &le; 0 hoặc vượt quá số dư)
 */
public record DebitRequest(
        BigDecimal amount
) {
}
