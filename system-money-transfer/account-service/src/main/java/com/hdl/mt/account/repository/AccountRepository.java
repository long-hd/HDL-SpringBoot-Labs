package com.hdl.mt.account.repository;

import com.hdl.mt.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy xuất Account từ DB.
 *
 * <p>Kế thừa {@link JpaRepository} là có sẵn findById/save/... — không cần viết tay.</p>
 *
 * <p>Lưu ý về BƯỚC 0: ở đây chưa xử lý tương tranh (concurrency). Nếu hai lệnh trừ tiền
 * chạy song song trên cùng tài khoản, cả hai có thể cùng đọc số dư cũ rồi cùng ghi đè
 * (lost update). Vấn đề này để dành cho bước sau (khoá lạc quan/optimistic locking hoặc
 * cập nhật bằng câu UPDATE có điều kiện) — cố ý chưa vá để giữ bước 0 tối giản.</p>
 */
public interface AccountRepository extends JpaRepository<Account, Long> {
}
