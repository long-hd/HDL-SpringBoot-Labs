package com.hdl.mt.account.service;

import com.hdl.mt.account.domain.Account;
import com.hdl.mt.account.domain.ProcessedOperation;
import com.hdl.mt.account.exception.AccountNotFoundException;
import com.hdl.mt.account.repository.AccountRepository;
import com.hdl.mt.account.repository.ProcessedOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Nghiệp vụ tài khoản: đọc số dư, trừ tiền, cộng tiền — có IDEMPOTENCY (bước 8).
 *
 * <p>Idempotency hoạt động theo trình tự "ghi khóa trước":
 * <ol>
 *   <li>Ghi {@code operationId} vào bảng processed_operation (khóa chính -> trùng sẽ ném
 *       {@link DataIntegrityViolationException}). Dùng {@code saveAndFlush} để lỗi trùng xảy ra
 *       NGAY trong transaction, không bị hoãn tới lúc commit.</li>
 *   <li>Nếu trùng -> thao tác đã xử lý trước đó -> BỎ QUA (không trừ/cộng lần hai), trả trạng
 *       thái hiện tại.</li>
 *   <li>Nếu chưa -> trừ/cộng tiền. Vì cùng một {@code @Transactional}, việc ghi khóa và đổi số
 *       dư "thành công cùng nhau hoặc rollback cùng nhau".</li>
 * </ol>
 * Nhờ vậy, retry ở transfer-service (bước 7) gọi lại cùng {@code operationId} sẽ KHÔNG trừ tiền
 * lần hai — vá đúng rủi ro double-debit đã cảnh báo.</p>
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final ProcessedOperationRepository processedOperationRepository;

    public AccountService(AccountRepository accountRepository,
                          ProcessedOperationRepository processedOperationRepository) {
        this.accountRepository = accountRepository;
        this.processedOperationRepository = processedOperationRepository;
    }

    @Transactional(readOnly = true)
    public Account getById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional
    public Account debit(Long accountId, BigDecimal amount, String operationId) {
        if (alreadyProcessed(operationId, "DEBIT", accountId, amount)) {
            return getById(accountId); // idempotent: đã trừ trước đó, không trừ lại
        }
        Account account = getById(accountId);
        account.debit(amount);

        // TẠM để test timeout — nhớ xóa sau
        // try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return account;
    }

    @Transactional
    public Account credit(Long accountId, BigDecimal amount, String operationId) {
        if (alreadyProcessed(operationId, "CREDIT", accountId, amount)) {
            return getById(accountId); // idempotent: đã cộng trước đó, không cộng lại
        }
        Account account = getById(accountId);
        account.credit(amount);
        return account;
    }

    /**
     * Thử ghi khóa idempotency. Trả {@code true} nếu khóa ĐÃ tồn tại (thao tác đã xử lý -> nên
     * bỏ qua), {@code false} nếu đây là lần đầu (nên tiếp tục trừ/cộng).
     */
    private boolean alreadyProcessed(String operationId, String type, Long accountId, BigDecimal amount) {
        try {
            processedOperationRepository.saveAndFlush(
                    new ProcessedOperation(operationId, type, accountId, amount));
            return false; // ghi được -> lần đầu
        } catch (DataIntegrityViolationException dup) {
            log.info("Bỏ qua {} (idempotent): operationId '{}' đã xử lý trước đó", type, operationId);
            return true;  // trùng -> đã xử lý
        }
    }
}
