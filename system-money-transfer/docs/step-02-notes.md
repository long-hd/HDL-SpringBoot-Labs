# Bước 2 — Thay lời gọi thủ công bằng interface khai báo (HTTP Interface + OpenFeign)

Mục tiêu: bỏ kiểu tự viết `restClient.post().uri(...).body(...).retrieve()` của bước 0, thay
bằng interface KHAI BÁO. Làm cả hai cơ chế client để so sánh trực tiếp, và cả hai lên được CV.

## Thiết kế: cổng + hai adapter (ports-and-adapters)

```
TransferService ──► AccountPort (cổng nội bộ: debit/credit)
                        ▲            ▲
        ┌───────────────┘            └────────────────┐
 HttpInterfaceAccountAdapter               FeignAccountAdapter
   dùng AccountApi (@HttpExchange)           dùng AccountFeignClient (@FeignClient)
        (mặc định)                             (khi client=feign)
```

- `TransferService` chỉ biết `AccountPort`, KHÔNG biết client cụ thể -> đổi client bằng cấu
  hình, không sửa code nghiệp vụ. Đây là kiểu anh đã dùng ở Soar (ports-adapters).
- Chọn adapter bằng property `account-service.client` (`http-interface` mặc định | `feign`),
  qua `@ConditionalOnProperty`.
- Mỗi adapter DỊCH lỗi hạ tầng riêng (`RestClientException` của HTTP Interface, `FeignException`
  của OpenFeign) sang một lỗi domain chung `AccountClientException` -> tầng nghiệp vụ xử lý
  đồng nhất. Đây là ranh giới chống ăn mòn (anti-corruption).

## So sánh hai cách (điểm học)

| | HTTP Interface (`@HttpExchange`) | OpenFeign (`@FeignClient`) |
|---|---|---|
| Thuộc về | Core Spring (spring-web) | Thư viện Spring Cloud |
| Dependency thêm | Không (có sẵn trong starter-web) | `spring-cloud-starter-openfeign` |
| Annotation | `@PostExchange`, `@GetExchange`... | tái dùng `@PostMapping`, `@GetMapping`... |
| Đặt interface ở module chung | Được (trung lập client/server) | Không nên (`@FeignClient` là annotation client) |
| Hướng của Spring | Khuyến nghị cho dự án mới | Feature-complete từ Cloud 2022.0.0 |
| Discovery/LB (bước 3–4) | Mượt từ Cloud 2025.1 (Boot 4); ở Boot 3.5 phải config tay | Tích hợp discovery/LB sẵn từ lâu |

Kết luận thực dụng: HTTP Interface là hướng tương lai + gọn hơn; OpenFeign phổ biến trong JD
và tiện hơn khi ghép discovery ở Boot 3.5. Biết cả hai, chọn theo bối cảnh.

## Vì sao account-service KHÔNG đổi ở bước 2

Việc để controller server implement chính interface `@HttpExchange` (một interface, hai đầu
dùng) là tính năng được nhấn cho Spring Framework 7 / Boot 4. Repo đang ở Boot 3.5 (SF 6.2),
nên account-service giữ controller `@PostMapping` như bước 0. `@HttpExchange` chỉ dùng ở phía
client. Cách A vẫn đúng ở phần cốt lõi: DTO + interface client dùng chung, đổi contract báo
lỗi compile ngay.

> Tuỳ chọn nếu muốn thử (không bắt buộc): cho `AccountController implements AccountApi` để
> đồng bộ contract hai đầu, rồi build+chạy xem SF 6.2 có map endpoint không. Nếu không lên
> Boot 4 thì đây là điểm chưa chắc — nên mặc định ta để tách.

## Cách tự kiểm chứng (khi chạy ở máy)

1. Mặc định (`client: http-interface`): chạy lại luồng chuyển tiền bước 0 -> kết quả y hệt.
2. Đổi `account-service.client: feign` trong `transfer-service/application.yml`, khởi động lại
   transfer-service -> luồng vẫn chạy đúng như cũ, dù `TransferService` không đổi dòng nào.
   Đây là bằng chứng cổng `AccountPort` hoạt động.
3. (Tuỳ) bật log Feign hoặc xem request để thấy hai cách cùng gọi tới các endpoint như nhau.

## Lỗ hổng cố ý vẫn còn (chưa đụng ở bước 2)

- "Tiền kẹt" khi credit lỗi sau debit — vẫn để dành bước 8 (Saga).
- Chưa idempotency, chưa retry/circuit-breaker (bước 7), URL vẫn cứng (bước 3 discovery).

> Chưa build/chạy trong môi trường tạo file (không có Maven Central + JDK ở đó). Build + chạy
> ở máy bạn; vướng compile/runtime thì gửi stacktrace.
