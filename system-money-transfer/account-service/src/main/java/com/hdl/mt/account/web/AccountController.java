package com.hdl.mt.account.web;

import com.hdl.mt.account.api.AccountResponse;
import com.hdl.mt.account.api.CreditRequest;
import com.hdl.mt.account.api.DebitRequest;
import com.hdl.mt.account.domain.Account;
import com.hdl.mt.account.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cổng HTTP của account-service.
 *
 * <p>Nhận/trả các DTO trong {@code account-service-api} — chính là "hợp đồng" mà
 * transfer-service dựa vào (cách A). Controller chỉ chuyển đổi entity &lt;-&gt; DTO và
 * uỷ nhiệm nghiệp vụ cho {@link AccountService}, không chứa logic nghiệp vụ.</p>
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Xem thông tin + số dư một tài khoản. */
    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return toResponse(accountService.getById(id));
    }

    /** Trừ tiền khỏi tài khoản {id}. */
    @PostMapping("/{id}/debit")
    public AccountResponse debit(@PathVariable Long id, @RequestBody DebitRequest request) {
        return toResponse(accountService.debit(id, request.amount()));
    }

    /** Cộng tiền vào tài khoản {id}. */
    @PostMapping("/{id}/credit")
    public AccountResponse credit(@PathVariable Long id, @RequestBody CreditRequest request) {
        return toResponse(accountService.credit(id, request.amount()));
    }

    /** Chuyển entity nội bộ sang DTO công khai (không lộ entity ra ngoài ranh giới service). */
    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getOwnerName(), account.getBalance());
    }
}
