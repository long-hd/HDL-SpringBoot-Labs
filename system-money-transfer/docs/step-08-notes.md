# Bước 8 — Saga + Idempotency (phần lõi)

Vá đồng thời hai lỗ hổng đã tận mắt thấy:
- "Tiền kẹt" (bước 0): trừ nguồn xong, cộng đích fail -> tiền rời nguồn không tới đích.
- Double-debit của retry (bước 7): retry có thể trừ hai lần nếu phản hồi bị mất.

## Hai thứ TÁCH BIỆT, làm cùng nhau vì dính nhau

| Việc | Ở đâu | Lưu trữ |
|---|---|---|
| Saga (điều phối + bù trừ) | transfer-service | bảng `transfer` cũ + vài trạng thái mới |
| Idempotency (chống áp dụng 2 lần) | account-service | bảng MỚI `processed_operation` |

Saga sinh ra bù trừ + retry -> sinh khả năng gọi lại -> BẮT BUỘC có idempotency thì mới an toàn.
Làm Saga mà thiếu idempotency thì chính Saga đẻ ra lỗi trừ/cộng nhiều lần.

## Saga (orchestration) — transfer-service làm nhạc trưởng

Luồng + các trạng thái của `transfer`:
```
PENDING
  ├─ debit(nguồn) fail  ─────────────────────────►  FAILED   (thường là chưa trừ -> an toàn)
  └─ debit OK
        ├─ credit(đích) OK  ───────────────────────►  COMPLETED
        └─ credit fail  ──►  COMPENSATING
                                ├─ credit hoàn về nguồn OK  ─►  COMPENSATED
                                └─ hoàn fail  ──────────────►  COMPENSATION_FAILED (cần đối soát)
```
Không "rollback" (không undo được qua mạng) mà "làm giao dịch ngược" (compensating): bù trừ =
một credit hoàn đúng khoản về tài khoản nguồn.

## Idempotency — account-service khử trùng theo operationId

- Mỗi bước Saga có operationId ỔN ĐỊNH theo id lệnh chuyển:
  `transfer-{id}-debit`, `transfer-{id}-credit`, `transfer-{id}-compensate`.
  Ổn định (không phải UUID ngẫu nhiên mỗi lần) là điều kiện để idempotency có tác dụng: retry
  dùng lại đúng khóa.
- account-service, trước khi trừ/cộng: `saveAndFlush(ProcessedOperation(operationId,...))`.
  operationId là KHÓA CHÍNH -> trùng ném DataIntegrityViolation -> nghĩa là đã xử lý -> BỎ QUA.
  Ghi khóa + đổi số dư nằm CÙNG một @Transactional -> nguyên tử.
- Vì sao lưu DB (không Redis): với tiền, độ bền của khóa quan trọng hơn tốc độ.

## Sửa kèm: retry KHÔNG thử lại khi circuit OPEN (lỗi phát hiện ở bước 7)

Trước đây circuit OPEN -> fallback ném AccountUnavailableException -> retry tưởng đáng thử ->
thử lại cái đã bị chặn 3 lần (vô ích). Nay: fallback nhận CallNotPermittedException -> ném
AccountClientException BASE (không nằm trong retry-exceptions) -> retry bỏ qua -> fail nhanh 1
lần. (Bài học: retry và circuit breaker chồng lên nhau, phải cấu hình để không "đánh nhau".)

## Contract thay đổi (cách A phát huy)

DebitRequest/CreditRequest thêm `operationId`. Vì DTO nằm ở jar chung, cả account-service (server)
lẫn transfer-service (client) cùng thấy -> đổi contract báo lỗi COMPILE nếu lệch, không phải chờ runtime.

## Cách tự kiểm chứng (khi chạy ở máy)

Chạy đủ: docker compose (Postgres+Redis) -> discovery -> config -> account -> transfer -> gateway.

1. **Happy path**: chuyển 1->2 hợp lệ -> transfer COMPLETED; số dư 1 giảm, 2 tăng.
2. **Bù trừ (compensating)**: chuyển tới đích KHÔNG tồn tại (vd toAccountId=999) -> debit nguồn OK,
   credit đích 404 -> transfer COMPENSATING -> hoàn về nguồn -> COMPENSATED. Kiểm số dư nguồn:
   TRỞ LẠI như cũ (đã hoàn). Xem log thấy "đã BÙ TRỪ xong".
   Kiểm trạng thái: `curl http://localhost:8082/transfers/{id}`? (chưa có endpoint GET by id —
   xem trong DB transfer_db hoặc thêm sau). Cách nhanh: xem log + số dư nguồn.
3. **Idempotency**: gọi thẳng account 2 lần CÙNG operationId:
   ```bash
   curl -s -X POST http://localhost:8081/accounts/1/debit -H 'Content-Type: application/json' \
     -d '{"operationId":"test-abc","amount":1000}'
   curl -s -X POST http://localhost:8081/accounts/1/debit -H 'Content-Type: application/json' \
     -d '{"operationId":"test-abc","amount":1000}'
   curl http://localhost:8081/accounts/1     # số dư chỉ giảm 1000 MỘT lần, không phải 2000
   ```
   Lần hai: account log "Bỏ qua DEBIT (idempotent)". Đây là bằng chứng retry an toàn.
4. **Retry không double-debit**: (khó giả lập mất-phản-hồi) — điểm 3 đã chứng minh cơ chế.

## Giới hạn còn lại (ghi nhận, chưa làm)

- **Reconcile**: ca "trừ nguồn xong nhưng mất phản hồi" -> transfer đánh FAILED nhưng tiền đã
  trừ. Idempotency chống trừ-2-lần, nhưng ca "tưởng fail mà thật ra thành công" cần job đối soát
  định kỳ so khớp với account-service. Giống reconcile job trong Soar. -> để bước mở rộng.
- **Client-level idempotency**: chống client double-submit POST /transfers (tạo 2 lệnh) là lớp
  khác (Idempotency-Key ở tầng transfer) — chưa làm, ghi nhận.
- **Known-gap từ trước**: giao dịch FAILED vẫn trả HTTP 200 (client dễ tưởng thành công); gateway
  bypass; reconcile — xem build-plan.

> Chưa build/chạy trong môi trường tạo file. Điểm dễ vấp: contract DTO đổi (phải rebuild
> account-service-api + cài lại: `mvn -pl account-service-api install`), và bảng mới
> processed_operation (ddl-auto=update tự tạo). Vướng gì gửi log.
