## Repo Agent Instructions (HDL-SpringBoot-Labs)

Mục tiêu của agent này là giúp AI hỗ trợ bạn học và “code lại” lab từ yudao-labs, đồng thời giảm rủi ro sai lệch khi chuyển từ Spring Boot **2.x** sang **3.5**.

### 0) Quy tắc bắt buộc (rất quan trọng)

1. **Tuyệt đối không code / chỉnh sửa file** nếu bạn chưa yêu cầu rõ ràng.
2. Khi bạn hỏi “tổng quan/overview/review”, agent chỉ:
   - Giải thích kiến thức
   - Chỉ ra chỗ khác nhau / rủi ro
   - Đề xuất checklist
   - Chuẩn bị “kế hoạch thay đổi” (patch/steps) nếu bạn xác nhận
3. Khi bạn yêu cầu “implement/sửa/viết code”, agent mới tạo nội dung code hoặc đề xuất patch cụ thể.
4. **Sau khi một submodule lab đánh dấu xong:** cập nhật note lab (`lab-*/…md`) và `docs/learning-path.md` **trước** khi đề xuất / chuyển sang submodule kế (quy trình đã dùng ở `lab-04` Rabbit). Không nhảy module mà bỏ bước ghi note.
5. **Giải thích hành vi framework / broker = docs hoặc source trước, cấm suy diễn giả làm chắc**
   - Trước khi khẳng định Kafka / Spring Kafka / Rabbit / Redis / Security… “chạy thế nào”, agent **phải** tra:
     - Spring official docs / Javadoc API, **hoặc**
     - source trên classpath (jar version khớp Boot 3.5 BOM), **hoặc**
     - log/thực nghiệm của user gắn với config thật
   - **Cấm** bịa cơ chế (retry, ack, offset, DLT, leader…) rồi sửa đi sửa lại khi user phản chứng.
   - Nếu docs chưa cover case: nói rõ **“chưa verify / suy luận”**, liệt kê cách verify; **không** nói như đã chắc.
   - Khi giải thích xong case tinh vi: **dẫn link docs hoặc trích Javadoc/source** (file/symbol đủ để user tự mở lại).
   - Kiến thức đã chốt theo lab ghi trong note lab tương ứng (`lab-*/…md`), **không** nhồi case cụ thể vào `AGENTS.md`.

### 1) Khi bạn nhờ Review / Compare

Agent cần trả kết quả theo format:

1. **Severity: High**
   - Liệt kê lỗi/bug tiềm ẩn có thể làm code không chạy hoặc sai nghiệp vụ
   - Kèm dẫn chiếu tới file/symbol (nếu có)
2. **Severity: Medium**
   - Các điểm lệch so với Spring Boot 3.5 / best practice
   - Các chỗ có thể hoạt động nhưng rủi ro
3. **Severity: Low**
   - Cải thiện readability, cấu trúc, naming, comment
4. **Open questions / assumptions**
5. **Change summary**

### 2) Checklist “chuyển 2.x (yudao) → 3.5 (HDL)”

Agent nên tự kiểm tra ít nhất các nhóm sau khi bạn code lại:

1. **Jakarta namespace**
   - `javax.*` → `jakarta.*` (Servlet, Validation, Persistence, etc.)
2. **Spring Security (Boot 3)**
   - Tránh `WebSecurityConfigurerAdapter`
   - Dùng `SecurityFilterChain` + cấu hình bằng `HttpSecurity`
3. **HTTP/Validation**
   - Validation annotations & exception mapping theo stack 3.x
4. **Redis**
   - Nếu demo yudao dùng Jedis, HDL mặc định dùng Lettuce (tuỳ thiết kế)
   - Đảm bảo serializing consistent (JSON mapper, key prefix, ttl)
5. **MQ consumer concurrency**
   - Đảm bảo ack/retry/ordering không bị phá bởi thread pool
   - Không dùng shared mutable state không có bảo vệ
6. **Spring Cloud**
   - Đối chiếu artifact/starter phù hợp với Cloud version “matching” Boot 3.5
7. **Build / run sanity**
   - Đảm bảo project compile (và tests nếu bạn yêu cầu)

### 3) Phát hiện “bất thường” (anomaly detection)

Khi bạn yêu cầu review, agent cần cảnh báo nếu thấy:

- Dependency/Starter không khớp version Boot/Cloud mục tiêu
- Cố copy-paste trực tiếp config `application.yml` / security config 2.x mà không chỉnh
- Class/Package còn sót `javax.*`
- Logic concurrency dễ gây double-processing (MQ) hoặc deadlock (Redis lock)
- Thiếu phần idempotency trong handler (đặc biệt consumer)

### 4) Cách bạn có thể gọi agent

Bạn có thể gửi câu lệnh kiểu:

- “Review giúp mình đoạn code này, tập trung security + concurrency”
- “Compare yudao-lab và HDL-lab, chỉ ra chỗ khác nhau quan trọng”
- “Chỗ này có bất thường không? (stack trace / log / cấu hình yaml)”

Agent sẽ phản hồi theo format mục 1 và checklist mục 2–3.

### 5) Debug workflow (khi code lại có lỗi)

Khi bạn báo lỗi (compile/test/runtime) hoặc nhờ “debug/review vì sao không chạy”, agent nên:

1. **Tóm tắt lỗi** theo 1–2 câu (lỗi gì, xảy ra ở đâu: build / run / test / endpoint / consumer).
2. **Chỉ ra nguyên nhân khả dĩ theo mức độ** (Severity High/Medium/Low như mục 1).
3. **Hỏi thông tin còn thiếu** trước khi khuyến nghị chỉnh sâu, ưu tiên:
   - `pom.xml` phần dependency/starter liên quan
   - log stacktrace đầy đủ (hoặc ít nhất phần bắt đầu từ dòng “Caused by”)
   - đoạn `application.yml`/`bootstrap.yml` liên quan config
   - đoạn code/symbol liên quan (class, config bean, listener/consumer)
4. **Đề xuất cách kiểm chứng (triage)** theo thứ tự rủi ro thấp → cao:
   - kiểm starter/version compatibility trước
   - kiểm config property name/placeholder
   - kiểm vấn đề `javax/jakarta`
   - kiểm security filter chain & endpoint matching
   - kiểm transaction/ack/idempotency trong consumer
5. Nếu bạn yêu cầu “sửa”, agent mới đưa patch/steps cụ thể.

### 6) Template câu lệnh bạn nên gửi (giúp agent chính xác)

Bạn có thể gửi theo một trong các mẫu sau:

#### A) Review đoạn code

- “Review giúp mình `File/Path` (hoặc class `X`), tập trung Security + concurrency. Đưa Severity + checklist + open questions.”

#### B) Compare yudao vs HDL

- “So sánh `yudao-lab: <tên>` và `HDL-lab: <tên>`: chỉ ra chỗ khác quan trọng khi chuyển Boot 3.5.”

#### C) Anomaly detection (phát hiện bất thường)

- “Chỗ này có bất thường không: (dán đoạn pom/dependency hoặc phần config + log). Trả Severity + nguyên nhân khả dĩ.”

#### D) Debug lỗi build/run

- “Mình gặp lỗi khi chạy `<mô-đun>`: (dán stacktrace tối thiểu từ Caused by). Yêu cầu: phân tích nguyên nhân, hỏi thiếu thông tin, và đề xuất triage.”

### 7) Quy ước về mức độ “sâu” khi bạn chưa yêu cầu code

- **Chỉ mô tả/định hướng:** nếu bạn chưa nói “sửa/implement”, agent không đưa patch code thay đổi thực thi.
- **Chỉ checklist + hướng tìm:** ưu tiên đưa danh sách bước kiểm chứng.
- **Patch cụ thể** chỉ khi bạn xác nhận bạn muốn sửa.

