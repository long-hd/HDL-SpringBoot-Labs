# M1 — Nền distributed systems (đọc trước khi học tool)

Mọi pattern phía sau (retry, circuit breaker, saga, tracing) đều là HỆ QUẢ của một sự thật:
**mạng không đáng tin, và một phần hệ thống có thể chết trong khi phần khác vẫn sống.**
Không nắm cái này thì học tool sẽ thành học vẹt. Note ngắn để nắm "why", không phải giáo trình.

## 1. Vì sao chẻ service — và vì sao KHÔNG nên chẻ bừa

Chẻ service để: nhiều team làm song song không dẫm chân nhau, deploy độc lập, scale riêng phần
nóng, cô lập lỗi. **Nếu không có mấy áp lực đó thì monolith thắng.** Microservices là lời giải
cho bài toán tổ chức/quy mô, không phải nấc kỹ thuật cao hơn. (Đây cũng là lý do Soar — solo,
monolith — KHÔNG nên chẻ; khu luyện tập này mới là nơi để học microservices.)

## 2. Cái giá phải trả (8 fallacies of distributed computing)

Những giả định SAI mà người mới hay mắc khi gọi qua mạng như gọi hàm cục bộ:
mạng luôn thông, độ trễ bằng 0, băng thông vô hạn, mạng an toàn, topology không đổi,
một admin duy nhất, chi phí truyền tải bằng 0, mạng đồng nhất. **Tất cả đều sai.**
Hệ quả trực tiếp: mọi lời gọi liên service phải tính tới chậm, mất gói, và lỗi từng phần.

## 3. Partial failure — điểm khác biệt lớn nhất so với monolith

- Trong monolith: hoặc chạy, hoặc crash. Một method gọi method khác thì chắc chắn hoặc chạy
  hoặc ném exception ngay.
- Trong phân tán: service A gọi B — B có thể **chết**, **chậm**, hoặc **đã làm xong nhưng phản
  hồi bị mất trên đường về**. A không phân biệt được ba trường hợp này chỉ từ việc "không nhận
  được trả lời". Đây là gốc rễ của timeout, retry, và **idempotency** (vì A có thể phải gọi lại
  mà không biết lần trước B đã làm hay chưa).

## 4. Mất ACID khi vượt qua ranh giới service

Trong một DB, transaction cho ta ACID: hoặc tất cả thay đổi cùng thành công, hoặc cùng cuộn ngược.
Khi một nghiệp vụ động tới **hai service, hai DB** (như: trừ tiền ở account-service, cộng ở...
account-service khác), **không còn một transaction chung nào** cuộn ngược được cả hai. Đây chính
xác là lỗ hổng ta cố ý để lộ ở bước 0. Lời giải KHÔNG phải 2PC/XA (chậm, coupling chặt, không
chịu được partial failure) mà là:
- **Saga**: chuỗi transaction cục bộ + bước bù trừ (compensating) khi có bước sau thất bại.
- **Transactional outbox**: ghi "việc cần làm" cùng transaction nghiệp vụ, rồi relay ra ngoài.
- **Idempotency**: để gọi lại an toàn.
- **Eventual consistency + reconcile**: chấp nhận nhất quán sau một khoảng, và có cơ chế đối soát.

## 5. CAP ở mức ý niệm

Khi mạng phân mảnh (Partition) — điều CHẮC CHẮN xảy ra trong phân tán — ta phải chọn giữa
Consistency (mọi nơi thấy cùng dữ liệu) và Availability (vẫn phục vụ). Thực tế phần lớn hệ
nghiệp vụ chọn AP + **eventual consistency** rồi bù bằng đối soát, thay vì khoá chặt để nhất
quán tuyệt đối. (Không cần học vẹt định lý; cần hiểu vì sao "nhất quán tức thì xuyên service"
là đắt và thường không đáng.)

## Nối vào khu này

- Bước 0 phơi ra **partial failure** (credit lỗi sau khi debit) và **mất ACID xuyên service**.
- Bước 7 (Resilience4j) xử lý **service chậm/chết** (timeout, circuit breaker).
- Bước 8 (Saga/outbox/idempotency) xử lý **nhất quán dữ liệu** — vá đúng lỗ hổng bước 0.
- Bước 10 (tracing) xử lý **"lỗi ở đâu trong chuỗi"** khi một request đi qua nhiều service.
