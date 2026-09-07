package com.hdl.mt.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một lệnh chuyển tiền: từ tài khoản {@code fromAccountId} sang {@code toAccountId}.
 *
 * <p>Đây là bản ghi do transfer-service tự quản trong DB riêng của nó (transfer_db).
 * Nó KHÔNG giữ số dư — số dư nằm ở account-service. Transfer chỉ ghi lại "đã có lệnh
 * chuyển thế này, kết quả ra sao".</p>
 */
@Entity
@Table(name = "transfer")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Lưu enum dạng chuỗi (STRING) cho dễ đọc trong DB và an toàn khi thêm giá trị mới. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Ghi lại lý do thất bại (nếu có) để dễ điều tra. */
    @Column(name = "failure_reason")
    private String failureReason;

    protected Transfer() {
    }

    public Transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = TransferStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markCompleted() {
        this.status = TransferStatus.COMPLETED;
    }

    public void markFailed(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
