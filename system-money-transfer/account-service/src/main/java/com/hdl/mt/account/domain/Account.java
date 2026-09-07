package com.hdl.mt.account.domain;

import com.hdl.mt.account.exception.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Tài khoản — giữ SỐ DƯ HIỆN TẠI (mutable).
 *
 * <p>Phân biệt với ledger (sổ cái, sẽ làm ở giai đoạn sau): account chỉ giữ một con
 * số {@code balance} thay đổi theo thời gian; ledger giữ lịch sử mọi bút toán, bất biến.
 * Ở bước 0 ta chỉ cần balance là đủ để dựng khung chuyển tiền.</p>
 *
 * <p>Điểm thiết kế quan trọng: logic trừ/cộng tiền đặt NGAY TRONG entity
 * ({@link #debit}/{@link #credit}) chứ không rải ở service. Đây là kiểu "rich domain
 * model" — quy tắc bất biến "số dư không được âm" sống cùng dữ liệu nó bảo vệ, nên
 * không có đường nào sửa balance mà lách qua được quy tắc.</p>
 */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    /**
     * Số dư. {@code precision=19, scale=2}: tối đa 17 chữ số phần nguyên + 2 chữ số lẻ.
     * Dùng {@link BigDecimal} để tránh sai số của số thực nhị phân trong tính tiền.
     */
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /** JPA bắt buộc phải có constructor rỗng. */
    protected Account() {
    }

    public Account(String ownerName, BigDecimal balance) {
        this.ownerName = ownerName;
        this.balance = balance;
    }

    /**
     * Trừ tiền khỏi tài khoản.
     *
     * @param amount số tiền cần trừ
     * @throws IllegalArgumentException    nếu {@code amount} không dương
     * @throws InsufficientBalanceException nếu số dư không đủ
     */
    public void debit(BigDecimal amount) {
        requirePositive(amount);
        // compareTo < 0 nghĩa là balance < amount -> không đủ tiền.
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(this.id, this.balance, amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Cộng tiền vào tài khoản.
     *
     * @param amount số tiền cần cộng
     * @throws IllegalArgumentException nếu {@code amount} không dương
     */
    public void credit(BigDecimal amount) {
        requirePositive(amount);
        this.balance = this.balance.add(amount);
    }

    /** Số tiền của một thao tác phải &gt; 0 — chặn cả null lẫn số âm/không. */
    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0, nhận được: " + amount);
        }
    }

    public Long getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
