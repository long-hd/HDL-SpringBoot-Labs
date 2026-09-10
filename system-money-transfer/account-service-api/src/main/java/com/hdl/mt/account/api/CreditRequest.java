package com.hdl.mt.account.api;

import java.math.BigDecimal;

/**
 * Yêu cầu CỘNG tiền vào một tài khoản.
 *
 * @param operationId khóa idempotency (xem giải thích ở DebitRequest). Bước bù trừ của Saga
 *                    (hoàn tiền về nguồn) cũng là một credit và cũng mang khóa riêng, để việc
 *                    hoàn tiền có gọi lại nhiều lần cũng không cộng dư.
 * @param amount      số tiền cần cộng (phải dương)
 */
public record CreditRequest(
        String operationId,
        BigDecimal amount
) {
}
