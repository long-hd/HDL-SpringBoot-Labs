# Reconcile job — tự chữa "tiền sai" đến cùng (bước mở rộng, sau bước 8)

Vá hai ô "tiền sai" mà idempotency + Saga đồng bộ chưa đóng được (xem bảng ở step-08):
- **Phantom debit**: transfer đánh FAILED nhưng account thực ra đã trừ (trừ xong, mất phản hồi).
- **Tiền kẹt**: COMPENSATION_FAILED (bù trừ lúc đó thất bại).

Idempotency chống trừ-hai-lần; reconcile đóng nốt các ca "tưởng fail mà thật ra thành công" và
"bù chưa xong". Đây là bước biến hệ thành SELF-HEALING (như reconcile job querydr trong Soar).

## Cách hoạt động

Job `@Scheduled` mỗi 30s trong transfer-service:
1. Quét transfer FAILED -> hỏi account "opId `transfer-{id}-debit` đã xử lý chưa?"
   (endpoint mới `GET /operations/{opId}` ở account-service, đọc bảng processed_operation).
   - Nếu RỒI -> phantom debit -> `accountPort.credit` hoàn về nguồn (compensateOpId ổn định) ->
     COMPENSATED.
   - Nếu CHƯA -> FAILED thật, bỏ qua.
2. Quét transfer COMPENSATION_FAILED -> thử bù trừ lại -> nếu account đã hồi -> COMPENSATED.

Đối chiếu dựa trên OPERATION LOG (opId đã xử lý chưa), KHÔNG so số dư — số dư đổi liên tục, còn
"opId đã xử lý" là sự thật bất biến, đáng tin hơn.

## Chạy đa node — rủi ro và khắc phục (điểm phỏng vấn)

- `@Scheduled` là lịch CỤC BỘ mỗi JVM -> nhiều node chạy trùng -> phí tài nguyên, và (nếu hành
  động không idempotent) có thể double refund — job đi sửa double charge lại tự gây double charge.
- **Khắc phục 2 lớp**:
  1. **ShedLock** (`@SchedulerLock`, khóa ở bảng shedlock/Postgres): mỗi lượt chỉ một node giành
     được khóa và chạy; node khác bỏ lượt. Khóa có hạn (`lockAtMostFor=5m`) -> node giữ khóa chết
     thì khóa tự nhả.
  2. **Idempotency của hành động** (compensateOpId ổn định): kể cả lock có kẽ hở (hết hạn khi node
     cũ chưa chết hẳn -> "hai leader" trong tích tắc), hoàn tiền trùng vẫn bị account khử -> không
     double refund. Lock giảm XÁC SUẤT trùng; idempotency chặn HẬU QUẢ. Dùng cả hai.

Vì sao ShedLock chứ không Quartz: chỉ cần chạy đều + chống trùng, không cần lịch động/nhiều
job/trạng thái bền -> ShedLock (một annotation) đủ; Quartz là dao mổ trâu. (Soar dùng Quartz vì
ở đó cần job cấu hình động — khác bài toán.)

## Đã thêm/đổi gì

- account-service: `GET /operations/{operationId}` -> `OperationStatus(operationId, processed)`.
- account-service-api: record `OperationStatus` (contract chung).
- transfer-service:
  - `AccountQueryClient` (chỉ-đọc, load-balanced, tách khỏi AccountPort của luồng nóng).
  - `ReconcileService` (@Scheduled + @SchedulerLock).
  - `ShedLockConfig` (@EnableScheduling + @EnableSchedulerLock + LockProvider Postgres, usingDbTime).
  - `TransferRepository.findByStatus(...)`.
  - `shedlock` table qua `schema.sql` + `spring.sql.init.mode=always`.
  - deps: shedlock-spring + shedlock-provider-jdbc-template (version 5.16.0 tường minh — KHÔNG
    trong BOM; có thể nâng khi cần).

## Cách tự kiểm chứng (khi chạy ở máy)

1. **Phantom debit tự chữa**: tạo cảnh transfer FAILED-mà-đã-trừ. Cách giả lập gọn:
   - Gọi thẳng account trừ tiền với đúng opId của một transfer sẽ-fail: khó dàn dựng thủ công.
   - Dễ hơn: tạm để account chậm/timeout ở bước credit để transfer đi vào COMPENSATION_FAILED,
     rồi bật account trở lại -> trong ~30s job tự bù -> COMPENSATED, số dư nguồn hoàn lại.
2. **Đa node**: chạy 2 instance transfer-service (cổng khác nhau). Xem log: mỗi lượt chỉ MỘT
   instance in dòng reconcile (instance kia bị ShedLock chặn). Kiểm bảng: `SELECT * FROM shedlock;`
   thấy 1 dòng khóa `reconcileTransfers` với locked_by = instance đang giữ.
3. Endpoint tra: `curl http://localhost:8081/operations/transfer-1-debit` -> {processed: true/false}.

## Còn để dành

- Ca PENDING treo lâu (crash giữa chừng) — cần suy ra bước dở rồi chạy tiếp. Chưa làm.
- Cảnh báo (alert) khi COMPENSATION_FAILED tồn tại quá lâu — thực tế nên có.

> Chưa build/chạy trong môi trường tạo file. Điểm dễ vấp: bảng shedlock phải tồn tại trước khi
> job chạy (đã lo bằng schema.sql + sql.init.mode=always); version ShedLock ghi tường minh. Vướng
> gì gửi log.
