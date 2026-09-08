# system-money-transfer

Khu luyện tập **microservices với Spring Cloud (stack quốc tế)**, tách khỏi các `lab`/`labx`
rời trong repo. Mục tiêu: **implement thật** một hệ nhiều service chạy cùng nhau, để học đủ
các mảnh (discovery, gateway, config, resilience, async, saga, tracing, security, K8s).

Domain: **chuyển tiền giữa các tài khoản nội bộ**. Chọn domain này vì một lần chuyển tiền
buộc phải trừ tiền ở một service/DB và cộng ở service/DB khác → **không thể dùng một
transaction ACID chung** → ép ta học Saga, outbox, idempotency (phần khó và giá trị nhất
cho CV fintech). Domain này **khác hẳn Soar** (Soar tích hợp cổng thanh toán ngoài VNPay;
khu này chuyển tiền nội bộ giữa account) nên không lẫn.

> ⚠️ Đây là project **luyện tập**. Javadoc viết bằng **tiếng Việt** cho dễ học. Không phải
> code production.

## Trạng thái hiện tại: Bước 0 (khung tối giản, chưa có Spring Cloud)

Xem lộ trình đầy đủ 13 bước và tiến độ ở [`docs/build-plan.md`](docs/build-plan.md).

## Kiến trúc (từ bước 3)

```
   client
     │  POST /transfers {fromAccountId, toAccountId, amount}
     ▼
┌──────────────────┐   gọi theo TÊN "account-service" (load-balanced qua Eureka)
│ transfer-service │ ──────────────────────────────────────────────┐
│      :8082       │   HTTP Interface HOẶC OpenFeign                 │
│   transfer_db    │   (chọn qua account-service.client)             ▼
└────────┬─────────┘                                     ┌──────────────────┐
         │ đăng ký / tra cứu                             │  account-service │
         ▼                                               │  :8081  (n bản)  │
┌──────────────────┐   ◄── đăng ký ─────────────────────│    account_db    │
│ discovery-server │                                     └──────────────────┘
│  Eureka  :8761   │
└──────────────────┘

  account-service-api (jar contract chung: DTO + interface AccountApi @HttpExchange, dùng phía client)
```

Từ bước 3: không còn URL cứng — transfer-service tra Eureka để biết account-service ở đâu.
Các bước sau bồi tiếp: gateway, config, resilience, saga, kafka, tracing, security, k8s.

## Module

| Module | Cổng | DB | Vai trò |
|---|---|---|---|
| `account-service-api` | — | — | Jar contract chung (DTO). Bước 2 thêm interface HTTP. |
| `account-service` | 8081 | account_db | Giữ số dư, xử lý trừ/cộng tiền |
| `transfer-service` | 8082 | transfer_db | Điều phối một lần chuyển tiền |

## Cách chạy (từ bước 3 — có Eureka)

Cần: JDK 21, Maven, Docker.

```bash
# 1) Bật Postgres (tạo sẵn account_db + transfer_db)
cd infra
docker compose up -d

# 2) Cài parent + api vào local maven (một lần)
cd ..
mvn -N install                       # cài parent pom
mvn -pl account-service-api install  # build + install jar contract

# 3) Chạy Eureka TRƯỚC (các service cần nó để đăng ký), rồi hai service — mỗi cái một terminal
mvn -pl discovery-server spring-boot:run   # http://localhost:8761 (mở xem dashboard Eureka)
mvn -pl account-service spring-boot:run
mvn -pl transfer-service spring-boot:run
```

Mở http://localhost:8761 sẽ thấy `ACCOUNT-SERVICE` và `TRANSFER-SERVICE` đã đăng ký.
Đổi client HTTP Interface ↔ Feign: sửa `account-service.client` trong `transfer-service/application.yml`.

## Thử nhanh

```bash
# Xem số dư (đã seed sẵn: id 1 = 1.000.000, id 2 = 500.000, id 3 = 0)
curl http://localhost:8081/accounts/1

# Chuyển 200.000 từ tài khoản 1 sang 2 -> kỳ vọng status COMPLETED
curl -X POST http://localhost:8082/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":200000}'

# Chuyển vượt số dư -> account-service trả 409, transfer status FAILED (chưa mất tiền)
curl -X POST http://localhost:8082/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":3,"toAccountId":1,"amount":999999}'
```

## Lỗ hổng cố ý của bước 0 (bài học)

Nếu **trừ nguồn xong mà cộng đích lỗi**, tiền bị "kẹt" (đã rời nguồn, chưa tới đích) và
bước 0 **chưa hoàn lại**. Đây là chủ đích — để thấy vì sao cần **Saga + compensating
transaction** (bước 8). Chi tiết ở [`docs/step-00-notes.md`](docs/step-00-notes.md).
