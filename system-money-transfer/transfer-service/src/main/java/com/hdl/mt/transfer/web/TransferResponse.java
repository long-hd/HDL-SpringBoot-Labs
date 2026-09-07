package com.hdl.mt.transfer.web;

import com.hdl.mt.transfer.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kết quả một lệnh chuyển tiền trả về cho client.
 *
 * @param id            id lệnh chuyển
 * @param fromAccountId tài khoản nguồn
 * @param toAccountId   tài khoản đích
 * @param amount        số tiền
 * @param status        trạng thái cuối (COMPLETED/FAILED)
 * @param createdAt     thời điểm tạo
 * @param failureReason lý do thất bại, null nếu thành công
 */
public record TransferResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String status,
        Instant createdAt,
        String failureReason
) {
    /** Tạo DTO từ entity Transfer. */
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getId(),
                t.getFromAccountId(),
                t.getToAccountId(),
                t.getAmount(),
                t.getStatus().name(),
                t.getCreatedAt(),
                t.getFailureReason()
        );
    }
}
