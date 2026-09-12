package com.hdl.mt.account.api;

/**
 * Trạng thái một thao tác theo khóa idempotency — dùng cho ĐỐI SOÁT (reconcile).
 *
 * <p>reconcile job ở transfer-service hỏi account-service "opId này đã xử lý chưa?" để phát hiện
 * phantom debit (transfer đánh FAILED nhưng account thực ra đã trừ). Đối chiếu theo BẢN GHI THAO
 * TÁC (operation log) đáng tin hơn so số dư — số dư đổi liên tục, còn "opId đã xử lý" là sự thật
 * bất biến.</p>
 *
 * @param operationId khóa idempotency được hỏi
 * @param processed   true nếu account-service đã xử lý thao tác này (đã có trong processed_operation)
 */
public record OperationStatus(
        String operationId,
        boolean processed
) {
}
