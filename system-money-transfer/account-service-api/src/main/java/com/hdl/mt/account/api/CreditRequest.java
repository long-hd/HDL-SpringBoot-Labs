package com.hdl.mt.account.api;

import java.math.BigDecimal;

/**
 * Yêu cầu CỘNG tiền vào một tài khoản.
 *
 * @param amount số tiền cần cộng (phải dương; account-service sẽ từ chối nếu &le; 0)
 */
public record CreditRequest(
        BigDecimal amount
) {
}
