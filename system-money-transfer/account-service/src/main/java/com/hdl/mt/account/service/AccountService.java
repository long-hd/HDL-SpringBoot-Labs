package com.hdl.mt.account.service;

import com.hdl.mt.account.domain.Account;
import com.hdl.mt.account.exception.AccountNotFoundException;
import com.hdl.mt.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Nghiệp vụ tài khoản: đọc số dư, trừ tiền, cộng tiền.
 *
 * <p>Service ở đây mỏng vì quy tắc bất biến đã nằm trong entity {@link Account}. Việc
 * của service là: tìm tài khoản, gọi hành vi trên entity, và bọc trong một transaction
 * để thay đổi được ghi xuống DB một cách nguyên tử.</p>
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /** Đọc thông tin một tài khoản. Chỉ đọc nên đánh dấu readOnly để tối ưu. */
    @Transactional(readOnly = true)
    public Account getById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    /**
     * Trừ tiền.
     *
     * <p>{@code @Transactional}: nạp entity, gọi {@link Account#debit}, thay đổi được
     * flush xuống DB khi transaction commit. Nếu debit ném lỗi (không đủ tiền), transaction
     * rollback và số dư không đổi.</p>
     */
    @Transactional
    public Account debit(Long accountId, BigDecimal amount) {
        Account account = getById(accountId);
        account.debit(amount);
        return account; // JPA tự ghi thay đổi khi commit (dirty checking)
    }

    /** Cộng tiền — tương tự debit. */
    @Transactional
    public Account credit(Long accountId, BigDecimal amount) {
        Account account = getById(accountId);
        account.credit(amount);
        return account;
    }
}
