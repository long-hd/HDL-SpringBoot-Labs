package com.hdl.mt.transfer.repository;

import com.hdl.mt.transfer.domain.Transfer;
import com.hdl.mt.transfer.domain.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Lưu/đọc bản ghi Transfer trong transfer_db. */
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /** Lấy các lệnh chuyển theo trạng thái — reconcile job dùng để quét FAILED / COMPENSATION_FAILED. */
    List<Transfer> findByStatus(TransferStatus status);
}
