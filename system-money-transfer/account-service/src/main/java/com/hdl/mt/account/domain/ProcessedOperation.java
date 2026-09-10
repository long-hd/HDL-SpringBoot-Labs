package com.hdl.mt.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ghi nhận một thao tác (debit/credit) ĐÃ được xử lý, khóa theo {@code operationId}.
 *
 * <p>Đây là cốt lõi của idempotency ở account-service: trước khi trừ/cộng tiền, account-service
 * ghi {@code operationId} vào bảng này (operationId là KHÓA CHÍNH, nên trùng sẽ bị DB từ chối).
 * Nếu ghi trùng -> nghĩa là thao tác này đã làm rồi -> BỎ QUA, không áp dụng lần hai.</p>
 *
 * <p>Vì sao lưu ở DB (không phải Redis): với tiền, độ BỀN của khóa quan trọng hơn tốc độ — mất
 * khóa (ví dụ Redis restart) đồng nghĩa có thể trừ/cộng lại lần hai. Và ghi khóa nằm CÙNG một
 * transaction với việc trừ tiền nên hai việc "thành công cùng nhau hoặc rollback cùng nhau".</p>
 */
@Entity
@Table(name = "processed_operation")
public class ProcessedOperation {

    /** Khóa idempotency do bên gọi (transfer-service) sinh ra. Là KHÓA CHÍNH -> chống trùng. */
    @Id
    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    /** DEBIT hoặc CREDIT — để audit/đọc log cho dễ. */
    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedOperation() {
    }

    public ProcessedOperation(String operationId, String operationType, Long accountId, BigDecimal amount) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.accountId = accountId;
        this.amount = amount;
        this.processedAt = Instant.now();
    }

    public String getOperationId() {
        return operationId;
    }
}
