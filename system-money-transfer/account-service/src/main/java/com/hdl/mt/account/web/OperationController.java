package com.hdl.mt.account.web;

import com.hdl.mt.account.api.OperationStatus;
import com.hdl.mt.account.repository.ProcessedOperationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cho phép hỏi "một operationId đã được xử lý chưa?" — phục vụ ĐỐI SOÁT (reconcile) ở
 * transfer-service. Chỉ đọc từ bảng processed_operation (idempotency log), không đụng số dư.
 */
@RestController
@RequestMapping("/operations")
public class OperationController {

    private final ProcessedOperationRepository processedOperationRepository;

    public OperationController(ProcessedOperationRepository processedOperationRepository) {
        this.processedOperationRepository = processedOperationRepository;
    }

    @GetMapping("/{operationId}")
    public OperationStatus getStatus(@PathVariable String operationId) {
        boolean processed = processedOperationRepository.existsById(operationId);
        return new OperationStatus(operationId, processed);
    }
}
