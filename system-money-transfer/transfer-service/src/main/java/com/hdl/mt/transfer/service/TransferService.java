package com.hdl.mt.transfer.service;

import com.hdl.mt.account.api.AccountResponse;
import com.hdl.mt.account.api.CreditRequest;
import com.hdl.mt.account.api.DebitRequest;
import com.hdl.mt.transfer.domain.Transfer;
import com.hdl.mt.transfer.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Điều phối một lần chuyển tiền = trừ tài khoản nguồn + cộng tài khoản đích.
 *
 * <p><b>Vì sao KHÔNG bọc cả hàm trong một {@code @Transactional}:</b> hai lời gọi bên
 * dưới đi qua MẠNG tới account-service. Giữ một transaction DB mở suốt thời gian chờ
 * mạng là phản mẫu (giữ khoá + kết nối DB lâu, dễ nghẽn). Quan trọng hơn: transaction
 * DB của transfer-service KHÔNG thể cuộn ngược thay đổi số dư nằm ở DB của account-service.
 * Đây chính là lý do "mất ACID khi vượt qua ranh giới service" — và là lý do ta cần Saga
 * ở bước 8.</p>
 *
 * <p><b>⚠️ LỖ HỔNG CỐ Ý CỦA BƯỚC 0 (đọc kỹ — đây là bài học then chốt):</b><br>
 * Nếu TRỪ nguồn thành công nhưng CỘNG đích thất bại (ví dụ account-service đích lỗi,
 * hoặc mạng đứt giữa chừng), thì tiền đã rời khỏi tài khoản nguồn nhưng KHÔNG tới đích —
 * tiền "bốc hơi". Bước 0 chỉ đánh dấu FAILED và ghi log cảnh báo, CHƯA hoàn tiền lại.
 * Cách vá đúng là <i>compensating transaction</i> (hoàn lại khoản đã trừ) trong Saga —
 * để dành cho bước 8. Ta cố ý phơi vấn đề ra trước để hiểu vì sao Saga tồn tại.</p>
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final RestClient accountRestClient;

    public TransferService(TransferRepository transferRepository, RestClient accountRestClient) {
        this.transferRepository = transferRepository;
        this.accountRestClient = accountRestClient;
    }

    /**
     * Thực hiện một lần chuyển tiền.
     *
     * @return bản ghi Transfer với trạng thái cuối (COMPLETED hoặc FAILED)
     */
    public Transfer transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Chặn lỗi hiển nhiên trước khi động vào tiền.
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Tài khoản nguồn và đích không được trùng nhau");
        }

        // Ghi lại Ý ĐỊNH chuyển tiền trước (trạng thái PENDING) — để dù sau đó có sự cố
        // thì vẫn còn dấu vết một lệnh chuyển đã bắt đầu.
        Transfer transfer = transferRepository.save(new Transfer(fromAccountId, toAccountId, amount));

        // ----- BƯỚC 1: TRỪ tài khoản nguồn -----
        try {
            debit(fromAccountId, amount);
        } catch (RestClientException ex) {
            // Trừ thất bại -> chưa có tiền nào rời đi -> an toàn, chỉ cần đánh dấu FAILED.
            log.warn("Chuyển tiền #{} thất bại khi TRỪ nguồn {}: {}",
                    transfer.getId(), fromAccountId, ex.getMessage());
            transfer.markFailed("Trừ tài khoản nguồn thất bại: " + ex.getMessage());
            return transferRepository.save(transfer);
        }

        // ----- BƯỚC 2: CỘNG tài khoản đích -----
        try {
            credit(toAccountId, amount);
        } catch (RestClientException ex) {
            // ⚠️ ĐÂY là tình huống nguy hiểm: nguồn ĐÃ bị trừ nhưng đích CHƯA được cộng.
            // Bước 0 chưa hoàn tiền -> tiền đang "kẹt". Bước 8 (Saga) sẽ thêm bước bù trừ:
            // gọi credit hoàn lại đúng số tiền cho tài khoản nguồn.
            log.error("NGHIÊM TRỌNG: Chuyển tiền #{} đã TRỪ {} khỏi nguồn {} nhưng CỘNG đích {} thất bại. "
                            + "Tiền đang kẹt! (Bước 8 Saga sẽ tự hoàn trả). Lỗi: {}",
                    transfer.getId(), amount, fromAccountId, toAccountId, ex.getMessage());
            transfer.markFailed("Cộng tài khoản đích thất bại (tiền đang kẹt ở nguồn): " + ex.getMessage());
            return transferRepository.save(transfer);
        }

        // ----- Cả hai bước OK -----
        transfer.markCompleted();
        log.info("Chuyển tiền #{} thành công: {} từ {} sang {}",
                transfer.getId(), amount, fromAccountId, toAccountId);
        return transferRepository.save(transfer);
    }

    /** Gọi account-service trừ tiền. retrieve() sẽ ném lỗi nếu account-service trả 4xx/5xx. */
    private AccountResponse debit(Long accountId, BigDecimal amount) {
        return accountRestClient.post()
                .uri("/accounts/{id}/debit", accountId)
                .body(new DebitRequest(amount))
                .retrieve()
                .body(AccountResponse.class);
    }

    /** Gọi account-service cộng tiền. */
    private AccountResponse credit(Long accountId, BigDecimal amount) {
        return accountRestClient.post()
                .uri("/accounts/{id}/credit", accountId)
                .body(new CreditRequest(amount))
                .retrieve()
                .body(AccountResponse.class);
    }
}
