package com.hdl.mt.account.exception;

import java.math.BigDecimal;

/**
 * Ném ra khi số dư không đủ để trừ.
 *
 * <p>Là lỗi NGHIỆP VỤ (business rule), không phải lỗi kỹ thuật — nên được map sang
 * mã HTTP 409 (Conflict) ở {@code GlobalExceptionHandler}, để bên gọi phân biệt được
 * "yêu cầu hợp lệ nhưng vi phạm quy tắc" với "hệ thống trục trặc" (5xx).</p>
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(Long accountId, BigDecimal balance, BigDecimal amount) {
        super("Tài khoản " + accountId + " không đủ số dư: hiện có " + balance
                + ", cần trừ " + amount);
    }
}
