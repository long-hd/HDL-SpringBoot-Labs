# Bước 7 — Resilience (Resilience4j)

Mục tiêu: khi account-service chậm/lỗi, transfer-service KHÔNG treo theo và KHÔNG làm hỏng dây
chuyền. Thêm circuit breaker + retry + timeout cho lời gọi transfer -> account.

## Hai tầng resilience (đừng lẫn — điểm phỏng vấn)

- **Tầng HẠ TẦNG (infra)**: service mesh (Istio/Envoy) làm retry/timeout/circuit-break ở tầng
  network, dev không viết code. Dự án này CHƯA dùng (cần K8s + team platform).
- **Tầng ỨNG DỤNG (application)**: Resilience4j viết TRONG transfer-service. Đây là bước 7. Cần
  cái này vì chỉ code mới biết fallback nghiệp vụ đúng là gì; mesh chỉ biết "request lỗi".
Hai tầng bổ sung nhau, không thay thế.

## Đặt ở đâu — cách 2 (bọc quanh cổng)

```
TransferService ──► AccountPort (Primary = ResilientAccountPort)   ◄── @CircuitBreaker @Retry
                          │ delegate (@Qualifier "rawAccountPort")
                          ▼
             HttpInterfaceAccountAdapter | FeignAccountAdapter (chỉ 1 active)
```
Circuit breaker + retry đặt MỘT CHỖ (decorator), dùng chung cho cả hai client. `TransferService`
không đổi dòng nào (vẫn xin `AccountPort`, nhận bản @Primary).

## Điểm cốt lõi: KHÔNG retry lỗi nghiệp vụ

Phân loại lỗi ở adapter theo mã HTTP:
- **4xx** (thiếu tiền, không tìm thấy...) -> `AccountBusinessException`: gọi lại vô nghĩa, và
  không phản ánh account-service trục trặc -> Resilience4j `ignore` (không retry, không tính vào
  circuit breaker). Ném thẳng cho TransferService -> mark FAILED.
- **5xx / timeout / mất kết nối** -> `AccountUnavailableException`: tạm thời -> retry (3 lần);
  nếu tỉ lệ lỗi cao -> circuit MỞ -> fallback ném AccountUnavailableException ngay (không gọi
  account nữa trong 10s) -> tránh "chết chậm" và dồn tải lên service đang yếu.

Cả hai vẫn là con của `AccountClientException` nên TransferService bắt như cũ -> mark FAILED.
(Retry sai loại lỗi là lỗi thiết kế hay gặp: retry "thiếu tiền" chỉ tổ phí và làm nghẽn.)

## Timeout: vì sao KHÔNG dùng @TimeLimiter

`@TimeLimiter` của Resilience4j chỉ áp cho lời gọi BẤT ĐỒNG BỘ (trả CompletableFuture/Mono).
Lời gọi ở đây đồng bộ (void), nên timeout đặt ở TẦNG HTTP CLIENT:
- RestClient (HTTP Interface): `SimpleClientHttpRequestFactory` connect/read = 2s (AccountClientConfig).
- OpenFeign: `spring.cloud.openfeign.client.config.default.connect-timeout/read-timeout = 2s`.
Quá hạn -> lỗi I/O -> adapter dịch thành AccountUnavailableException -> retry/circuit breaker lo.

## ⚠️ Retry + idempotency (nối sang bước 8)

Retry gọi lại lời gọi đã "thất bại". Nhưng nếu debit ĐÃ chạy ở account-service mà phản hồi bị
mất (timeout SAU khi đã trừ tiền), thì retry sẽ TRỪ TIỀN LẦN HAI. Retry chỉ an toàn khi thao
tác IDEMPOTENT — hiện CHƯA có. Bước 8 thêm khoá idempotency để lần gọi lại không gây tác dụng
kép. Đây đúng câu "idempotency ngăn double-process, không tự ngăn double-charge" — nhớ cho phỏng vấn.
(Lỗ hổng "tiền kẹt" khi credit lỗi sau debit vẫn còn — cũng để bước 8 Saga xử lý.)

## Cấu hình (application.yml)

`resilience4j.circuitbreaker.instances.account` (sliding-window 10, mở khi >50% lỗi trong tối
thiểu 5 lời gọi, mở 10s) + `resilience4j.retry.instances.account` (3 lần, chờ 500ms, chỉ retry
AccountUnavailableException). Xem trạng thái qua actuator: `/actuator/circuitbreakers`, `/actuator/retries`.

## Cách tự kiểm chứng (khi chạy ở máy)

1. **Retry lỗi hạ tầng**: tắt account-service, chuyển tiền -> transfer thử lại 3 lần rồi FAILED
   (xem log có 3 lần gọi). Bật lại account-service, chuyển tiền -> OK.
2. **KHÔNG retry lỗi nghiệp vụ**: chuyển vượt số dư (account trả 409) -> FAILED NGAY, log chỉ 1
   lần gọi (không retry). Đây là điểm khác biệt then chốt so với lỗi hạ tầng.
3. **Circuit mở**: tắt account-service, bắn nhiều lệnh chuyển liên tục -> sau khi vượt ngưỡng
   lỗi, `/actuator/circuitbreakers` cho thấy state = OPEN; các lệnh sau FAILED tức thì (không
   chờ timeout nữa) trong 10s.
4. **Timeout**: (khó giả lập nếu account nhanh) — có thể thêm Thread.sleep tạm ở account để thử.

## Còn để dành

- Idempotency + Saga (bù trừ "tiền kẹt") — **bước 8**. Tracing (bước 10), security (11), K8s (12).

> Chưa build/chạy trong môi trường tạo file. Điểm dễ vấp: annotation @CircuitBreaker/@Retry cần
> AOP (đã thêm starter-aop) + resilience4j-spring-boot3 (starter Spring Cloud kéo về); và bean
> wiring @Primary/@Qualifier giữa decorator và adapter thô. Vướng gì gửi log.
