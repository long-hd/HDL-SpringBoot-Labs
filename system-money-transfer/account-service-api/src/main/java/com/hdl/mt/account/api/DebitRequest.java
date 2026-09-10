package com.hdl.mt.account.api;

import java.math.BigDecimal;

/**
 * Yêu cầu TRỪ tiền khỏi một tài khoản.
 *
 * @param operationId khóa idempotency — định danh DUY NHẤT cho thao tác này. account-service
 *                    nhớ khóa đã xử lý; nếu nhận lại đúng khóa (do retry/gọi lại) thì BỎ QUA,
 *                    không trừ lần hai. Đây là thứ khiến retry ở bước 7 trở nên an toàn.
 * @param amount      số tiền cần trừ (phải dương)
 */
public record DebitRequest(
        String operationId,
        BigDecimal amount
) {
}
