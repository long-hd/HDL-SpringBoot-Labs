package com.hdl.mt.transfer.reconcile;

import com.hdl.mt.transfer.client.AccountPort;
import com.hdl.mt.transfer.client.AccountQueryClient;
import com.hdl.mt.transfer.domain.Transfer;
import com.hdl.mt.transfer.domain.TransferStatus;
import com.hdl.mt.transfer.repository.TransferRepository;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Job ĐỐI SOÁT định kỳ — biến hệ thành "tự chữa" (self-healing), vá hai ca mà idempotency +
 * Saga đồng bộ chưa xử lý hết:
 *
 * <ol>
 *   <li><b>Phantom debit</b>: lệnh chuyển ở trạng thái FAILED nhưng account THỰC RA đã trừ tiền
 *       (ca "trừ xong nhưng mất phản hồi" ở bước 7-8). Job hỏi account "opId debit đã xử lý chưa";
 *       nếu RỒI -> tiền đã rời nguồn cho một lệnh FAILED -> hoàn về nguồn -> COMPENSATED.</li>
 *   <li><b>Tiền kẹt</b>: lệnh ở COMPENSATION_FAILED (bù trừ trước đó thất bại) -> thử bù lại; nếu
 *       account đã hồi -> COMPENSATED.</li>
 * </ol>
 *
 * <p>Đối chiếu dựa trên OPERATION LOG (opId đã xử lý chưa), không so số dư — bền và đáng tin hơn.
 * Mọi hành động hoàn tiền dùng compensateOpId ỔN ĐỊNH nên idempotent: chạy nhiều lượt / nhiều
 * node cũng không hoàn hai lần. Chạy dưới {@code @SchedulerLock} nên mỗi lượt chỉ một node làm.</p>
 *
 * <p>Giới hạn: chưa xử lý ca PENDING treo lâu (service chết giữa chừng, phải suy ra đang ở bước
 * nào) — để mở rộng sau.</p>
 */
@Service
public class ReconcileService {

    private static final Logger log = LoggerFactory.getLogger(ReconcileService.class);

    private final TransferRepository transferRepository;
    private final AccountPort accountPort;
    private final AccountQueryClient accountQueryClient;

    public ReconcileService(TransferRepository transferRepository,
                            AccountPort accountPort,
                            AccountQueryClient accountQueryClient) {
        this.transferRepository = transferRepository;
        this.accountPort = accountPort;
        this.accountQueryClient = accountQueryClient;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    @SchedulerLock(name = "reconcileTransfers", lockAtMostFor = "5m", lockAtLeastFor = "5s")
    public void reconcile() {
        LockAssert.assertLocked(); // đảm bảo đang chạy dưới khóa (bắt lỗi cấu hình sai)
        reconcilePhantomDebits();
        reconcileFailedCompensations();
    }

    /** Ca 1: FAILED nhưng debit đã xử lý -> hoàn tiền về nguồn. */
    private void reconcilePhantomDebits() {
        List<Transfer> failed = transferRepository.findByStatus(TransferStatus.FAILED);
        for (Transfer t : failed) {
            String debitOpId = "transfer-" + t.getId() + "-debit";
            try {
                if (!accountQueryClient.isProcessed(debitOpId)) {
                    continue; // debit chưa từng chạy -> FAILED thật, không cần làm gì
                }
                // Tiền đã bị trừ cho một lệnh FAILED -> phantom debit -> hoàn về nguồn.
                String compensateOpId = "transfer-" + t.getId() + "-compensate";
                accountPort.credit(t.getFromAccountId(), t.getAmount(), compensateOpId);
                t.markCompensated();
                transferRepository.save(t);
                log.warn("Reconcile: phát hiện phantom debit ở lệnh #{} -> đã hoàn {} về nguồn {}",
                        t.getId(), t.getAmount(), t.getFromAccountId());
            } catch (Exception e) {
                // account chưa trả lời / lỗi tạm thời -> bỏ qua, lượt sau thử lại.
                log.info("Reconcile: bỏ qua lệnh #{} lượt này ({})", t.getId(), e.getMessage());
            }
        }
    }

    /** Ca 2: COMPENSATION_FAILED -> thử bù trừ lại. */
    private void reconcileFailedCompensations() {
        List<Transfer> stuck = transferRepository.findByStatus(TransferStatus.COMPENSATION_FAILED);
        for (Transfer t : stuck) {
            String compensateOpId = "transfer-" + t.getId() + "-compensate";
            try {
                accountPort.credit(t.getFromAccountId(), t.getAmount(), compensateOpId);
                t.markCompensated();
                transferRepository.save(t);
                log.warn("Reconcile: bù trừ lại thành công cho lệnh #{} -> đã hoàn {} về nguồn {}",
                        t.getId(), t.getAmount(), t.getFromAccountId());
            } catch (Exception e) {
                log.info("Reconcile: lệnh #{} vẫn chưa bù được lượt này ({})", t.getId(), e.getMessage());
            }
        }
    }
}
