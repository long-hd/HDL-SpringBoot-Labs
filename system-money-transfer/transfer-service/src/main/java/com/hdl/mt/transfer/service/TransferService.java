package com.hdl.mt.transfer.service;

import com.hdl.mt.transfer.client.AccountClientException;
import com.hdl.mt.transfer.client.AccountPort;
import com.hdl.mt.transfer.domain.Transfer;
import com.hdl.mt.transfer.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Điều phối một lần chuyển tiền = trừ tài khoản nguồn + cộng tài khoản đích.
 *
 * <p>BƯỚC 2 đổi so với bước 0: không còn gọi {@code RestClient} trực tiếp, mà đi qua cổng
 * {@link AccountPort}. Nhờ vậy {@code TransferService} không biết (và không cần biết) lời gọi
 * đang chạy bằng HTTP Interface hay OpenFeign — chọn cái nào là do cấu hình
 * {@code account-service.client}. Logic nghiệp vụ bên dưới GIỮ NGUYÊN so với bước 0.</p>
 *
 * <p><b>Vì sao KHÔNG bọc cả hàm trong {@code @Transactional}:</b> hai lời gọi đi qua mạng;
 * transaction DB của transfer-service không cuộn ngược được số dư nằm ở DB account-service.
 * Đây là "mất ACID xuyên service".</p>
 *
 * <p><b>⚠️ LỖ HỔNG CỐ Ý (vẫn còn từ bước 0, sẽ vá ở bước 8):</b> nếu TRỪ nguồn xong mà CỘNG
 * đích thất bại, tiền đã rời nguồn nhưng chưa tới đích — "tiền kẹt". Bước 2 vẫn chỉ đánh dấu
 * FAILED + log, chưa hoàn lại. Cách vá là compensating transaction trong Saga (bước 8).</p>
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final AccountPort accountPort;

    public TransferService(TransferRepository transferRepository, AccountPort accountPort) {
        this.transferRepository = transferRepository;
        this.accountPort = accountPort;
    }

    /**
     * Thực hiện một lần chuyển tiền.
     *
     * @return bản ghi Transfer với trạng thái cuối (COMPLETED hoặc FAILED)
     */
    public Transfer transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Tài khoản nguồn và đích không được trùng nhau");
        }

        // Ghi lại Ý ĐỊNH trước (PENDING) để luôn có dấu vết dù sau đó có sự cố.
        Transfer transfer = transferRepository.save(new Transfer(fromAccountId, toAccountId, amount));

        // ----- BƯỚC 1: TRỪ tài khoản nguồn -----
        try {
            accountPort.debit(fromAccountId, amount);
        } catch (AccountClientException ex) {
            // Trừ thất bại -> chưa tiền nào rời đi -> an toàn.
            log.warn("Chuyển tiền #{} thất bại khi TRỪ nguồn {}: {}",
                    transfer.getId(), fromAccountId, ex.getMessage());
            transfer.markFailed("Trừ tài khoản nguồn thất bại: " + ex.getMessage());
            return transferRepository.save(transfer);
        }

        // ----- BƯỚC 2: CỘNG tài khoản đích -----
        try {
            accountPort.credit(toAccountId, amount);
        } catch (AccountClientException ex) {
            // ⚠️ Nguồn ĐÃ bị trừ nhưng đích CHƯA được cộng -> tiền kẹt. Bước 8 (Saga) sẽ bù trừ.
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
}
