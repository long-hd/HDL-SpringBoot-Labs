package com.hdl.mt.transfer.web;

import java.math.BigDecimal;

/**
 * Dữ liệu vào để tạo một lệnh chuyển tiền.
 *
 * <p>DTO này thuộc riêng transfer-service (không nằm ở account-service-api), vì nó mô tả
 * hợp đồng của transfer-service với client bên ngoài, không liên quan account-service.</p>
 *
 * @param fromAccountId tài khoản nguồn (bị trừ)
 * @param toAccountId   tài khoản đích (được cộng)
 * @param amount        số tiền chuyển
 */
public record TransferRequest(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount
) {
}
