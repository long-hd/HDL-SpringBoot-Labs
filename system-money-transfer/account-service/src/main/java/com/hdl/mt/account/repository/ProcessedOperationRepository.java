package com.hdl.mt.account.repository;

import com.hdl.mt.account.domain.ProcessedOperation;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lưu/đọc các khóa idempotency đã xử lý. Khóa chính là operationId (String). */
public interface ProcessedOperationRepository extends JpaRepository<ProcessedOperation, String> {
}
