package com.hdl.mt.transfer.service;

import com.hdl.mt.transfer.client.AccountClientException;
import com.hdl.mt.transfer.client.AccountPort;
import com.hdl.mt.transfer.config.TransferProperties;
import com.hdl.mt.transfer.domain.Transfer;
import com.hdl.mt.transfer.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Bộ ĐIỀU PHỐI Saga (orchestration) cho một lần chuyển tiền.
 *
 * <p>transfer-service đóng vai "nhạc trưởng": tự gọi từng bước (trừ nguồn -> cộng đích) và tự
 * gọi BÙ TRỪ nếu một bước sau thất bại. Đây là kiểu orchestration (đã chốt) — hợp vì chỉ 2-3
 * bước và luồng dễ theo dõi.</p>
 *
 * <p><b>Vì sao KHÔNG @Transactional:</b> các bước đi qua mạng, không có transaction chung cuộn
 * ngược được số dư ở account-service. Thay vì rollback, Saga dùng COMPENSATING TRANSACTION
 * (giao dịch ngược) để triệt tiêu hậu quả.</p>
 *
 * <p><b>Idempotency:</b> mỗi bước có một {@code operationId} ổn định theo id lệnh chuyển
 * ("transfer-{id}-debit/-credit/-compensate"). Retry (bước 7) dùng lại đúng khóa nên
 * account-service không áp dụng lần hai. Khóa ổn định (không phải UUID ngẫu nhiên mỗi lần) là
 * điều kiện để idempotency có tác dụng.</p>
 *
 * <p><b>Giới hạn còn lại (cần đối soát/reconcile — chưa làm ở bước 8):</b> nếu bước TRỪ nguồn
 * ném lỗi hạ tầng SAU KHI đã trừ (timeout mất phản hồi), transfer-service tưởng thất bại và
 * đánh FAILED, nhưng tiền có thể đã trừ. Idempotency chống trừ-hai-lần, nhưng ca "tưởng fail mà
 * thực ra thành công" cần một job đối soát định kỳ so khớp với account-service. Ghi nhận để làm
 * sau (giống reconcile job trong Soar).</p>
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final AccountPort accountPort;
    private final TransferProperties properties;

    public TransferService(TransferRepository transferRepository,
                           AccountPort accountPort,
                           TransferProperties properties) {
        this.transferRepository = transferRepository;
        this.accountPort = accountPort;
        this.properties = properties;
    }

    public Transfer transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Tài khoản nguồn và đích không được trùng nhau");
        }
        BigDecimal maxAmount = properties.getMaxAmountPerTransaction();
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException(
                    "Số tiền " + amount + " vượt hạn mức mỗi lần chuyển (" + maxAmount + ")");
        }

        Transfer transfer = transferRepository.save(new Transfer(fromAccountId, toAccountId, amount));
        long id = transfer.getId();
        String debitOpId = "transfer-" + id + "-debit";
        String creditOpId = "transfer-" + id + "-credit";
        String compensateOpId = "transfer-" + id + "-compensate";

        // ----- BƯỚC 1: TRỪ nguồn -----
        try {
            accountPort.debit(fromAccountId, amount, debitOpId);
        } catch (AccountClientException ex) {
            // Trừ fail: trường hợp phổ biến (thiếu tiền) là chưa trừ -> an toàn, chỉ FAILED.
            // (Ca hiếm: đã trừ nhưng mất phản hồi -> cần reconcile, xem Javadoc lớp.)
            log.warn("Chuyển tiền #{} thất bại khi TRỪ nguồn {}: {}", id, fromAccountId, ex.getMessage());
            transfer.markFailed("Trừ tài khoản nguồn thất bại: " + ex.getMessage());
            return transferRepository.save(transfer);
        }

        // ----- BƯỚC 2: CỘNG đích -----
        try {
            accountPort.credit(toAccountId, amount, creditOpId);
        } catch (AccountClientException ex) {
            // Đã trừ nguồn nhưng cộng đích fail -> phải BÙ TRỪ (hoàn tiền về nguồn).
            log.warn("Chuyển tiền #{} cộng đích {} thất bại -> bắt đầu bù trừ. Lỗi: {}",
                    id, toAccountId, ex.getMessage());
            transfer.markCompensating("Cộng đích thất bại: " + ex.getMessage());
            transferRepository.save(transfer);
            return compensate(transfer, fromAccountId, amount, compensateOpId);
        }

        transfer.markCompleted();
        log.info("Chuyển tiền #{} thành công: {} từ {} sang {}", id, amount, fromAccountId, toAccountId);
        return transferRepository.save(transfer);
    }

    /**
     * Bù trừ: hoàn lại đúng khoản đã trừ cho tài khoản nguồn (một credit ngược chiều).
     * Có idempotency (compensateOpId) + resilience (qua AccountPort) như mọi lời gọi khác.
     */
    private Transfer compensate(Transfer transfer, Long fromAccountId, BigDecimal amount, String compensateOpId) {
        try {
            accountPort.credit(fromAccountId, amount, compensateOpId);
            transfer.markCompensated();
            log.info("Chuyển tiền #{} đã BÙ TRỪ xong: hoàn {} về nguồn {}",
                    transfer.getId(), amount, fromAccountId);
        } catch (AccountClientException ex) {
            // Bù trừ cũng fail -> tiền đang kẹt ở nguồn. Thực tế cần job retry/đối soát + cảnh báo.
            log.error("NGHIÊM TRỌNG: Chuyển tiền #{} BÙ TRỪ THẤT BẠI: hoàn {} về nguồn {} không xong. "
                            + "Tiền đang kẹt, cần đối soát! Lỗi: {}",
                    transfer.getId(), amount, fromAccountId, ex.getMessage());
            transfer.markCompensationFailed("Bù trừ thất bại: " + ex.getMessage());
        }
        return transferRepository.save(transfer);
    }
}
