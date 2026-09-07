package com.hdl.mt.transfer.repository;

import com.hdl.mt.transfer.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lưu/đọc bản ghi Transfer trong transfer_db. */
public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
