# Bước 0 — Khung tối giản (notes)

Mục tiêu: có một hệ CHẠY ĐƯỢC với luồng chuyển tiền happy-path, **chưa** Spring Cloud, để các
bước sau có cái mà bồi vào. Cố ý giữ đơn giản và cố ý chừa vài lỗ hổng làm bài học.

## Đã làm gì

- `account-service` (:8081, account_db): entity `Account` giữ số dư; endpoint xem/trừ/cộng.
  Quy tắc "số dư không âm" nằm trong entity (rich domain model). Lỗi nghiệp vụ map sang mã HTTP
  đúng nghĩa (404 không thấy tài khoản, 409 không đủ tiền, 400 số tiền sai).
- `transfer-service` (:8082, transfer_db): nhận lệnh chuyển, gọi account-service trừ nguồn rồi
  cộng đích bằng `RestClient` với **URL cứng**, lưu bản ghi `Transfer` với trạng thái cuối.
- `account-service-api`: jar contract chung (cách A) — hiện chỉ có DTO; interface HTTP thêm ở bước 2.

## Những chỗ CỐ Ý chưa làm (là lời giải của các bước sau, đừng "sửa sớm")

1. **URL cứng** khi transfer gọi account (`AccountClientConfig`). → Bước 2 (interface khai báo),
   bước 3 (gọi theo tên qua discovery), bước 4 (load balance nhiều instance).
2. **Lỗ hổng "tiền kẹt"** (`TransferService`): nếu TRỪ nguồn xong mà CỘNG đích lỗi, tiền đã rời
   nguồn nhưng chưa tới đích, và bước 0 CHƯA hoàn lại — chỉ đánh dấu FAILED + log `error`.
   → Bước 8 (Saga) thêm bước bù trừ: hoàn lại đúng khoản đã trừ cho nguồn.
3. **Chưa idempotency**: gọi `POST /transfers` hai lần (ví dụ client bấm lại do timeout) sẽ tạo
   hai lệnh chuyển và trừ tiền hai lần. → Bước 8 (khoá idempotency theo một request-id).
4. **Chưa chống tương tranh** ở account (lost update nếu hai lệnh trừ song song cùng tài khoản).
   → Bước sau: optimistic locking (`@Version`) hoặc UPDATE có điều kiện (CAS).
5. **`ddl-auto=update` + `data.sql`**: tiện cho học, nhưng dự án thật nên dùng Flyway. Seed dùng
   id tường minh (1,2,3) + `ON CONFLICT DO NOTHING`; vì bước 0 không có endpoint tạo tài khoản
   nên chưa lo lệch sequence của IDENTITY.

## Cách tự kiểm chứng (khi chạy ở máy)

- `GET /accounts/1` trả số dư seed (1.000.000).
- Chuyển hợp lệ 1 → 2: `Transfer.status = COMPLETED`, số dư 1 giảm, số dư 2 tăng đúng bằng amount.
- Chuyển vượt số dư: account-service trả 409 → `Transfer.status = FAILED`, số dư KHÔNG đổi (an toàn).
- (Mô phỏng lỗ hổng #2 để thấy bài học): tắt khả năng cộng đích — ví dụ chuyển tới một
  `toAccountId` không tồn tại. Debit nguồn xong, credit đích trả 404 → log `error` "tiền đang
  kẹt", số dư nguồn ĐÃ giảm mà không ai nhận. Đây là thứ Saga ở bước 8 sẽ chữa.

> Lưu ý: các file này được viết ra để bạn tải về đặt vào repo; **chưa build/chạy trong môi trường
> tạo ra chúng** (không có Maven Central ở đó). Hãy build + chạy ở máy bạn; nếu có lỗi
> compile/runtime, gửi lại stacktrace để rà theo debug workflow trong AGENTS.md.
