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

## Trạng thái hiện tại: đã xong bước 6 (Config Server + refresh nóng)

Xem lộ trình đầy đủ 13 bước và tiến độ ở [`docs/build-plan.md`](docs/build-plan.md).

## Kiến trúc (từ bước 5)

```
   client
     │  http://localhost:8080/transfers , /accounts/...
     ▼
┌──────────────────┐  api-gateway :8080  ── route lb:// + rate limit (RedisRateLimiter)
│    api-gateway    │──────────────┬───────────────────────────┐
└────────┬─────────┘              │                            │
         │ tra Eureka             ▼ /transfers/**              ▼ /accounts/**
         │              ┌──────────────────┐        ┌──────────────────┐
         │              │ transfer-service │ ─────► │  account-service │
         │              │      :8082       │ lb://  │  :8081  (n bản)  │
         │              │   transfer_db    │        │    account_db    │
         │              └──────────────────┘        └──────────────────┘
         ▼
┌──────────────────┐        ┌──────────────┐
│ discovery-server │        │ redis :6379  │  (token bucket cho rate limit)
│  Eureka  :8761   │        └──────────────┘
└──────────────────┘

  transfer→account: HTTP Interface hoặc OpenFeign (chọn qua account-service.client)
  account-service-api: jar contract chung (DTO + AccountApi @HttpExchange, phía client)
```

Từ bước 5: client đi qua **một cửa** (gateway :8080). Gateway route theo tên service và áp
rate limit. Các bước sau bồi tiếp: config, resilience, saga, kafka, tracing, security, k8s.

## Module

| Module | Cổng | DB | Vai trò |
|---|---|---|---|
| `discovery-server` | 8761 | — | Eureka — danh bạ đăng ký/tra cứu service |
| `config-server` | 8888 | — | Phát config nghiệp vụ tập trung (native backend) |
| `api-gateway` | 8080 | — | Cửa vào duy nhất: route + rate limit |
| `account-service-api` | — | — | Jar contract chung (DTO + AccountApi) |
| `account-service` | 8081 | account_db | Giữ số dư, xử lý trừ/cộng tiền |
| `transfer-service` | 8082 | transfer_db | Điều phối một lần chuyển tiền |

## Cách chạy (từ bước 5)

Cần: JDK 21, Maven, Docker.

```bash
# 1) Bật hạ tầng: Postgres (account_db + transfer_db) + Redis (rate limit)
cd infra
docker compose up -d

# 2) Cài parent + api vào local maven (một lần)
cd ..
mvn -N install
mvn -pl account-service-api install

# 3) Chạy theo thứ tự, mỗi cái một terminal
mvn -pl discovery-server spring-boot:run   # Eureka  :8761
mvn -pl config-server    spring-boot:run   # Config  :8888
mvn -pl account-service  spring-boot:run   # :8081
mvn -pl transfer-service spring-boot:run   # :8082
mvn -pl api-gateway      spring-boot:run   # :8080  (cửa vào)
```

Mở http://localhost:8761 xem các service đã đăng ký.
Đổi client HTTP Interface ↔ Feign: sửa `account-service.client` trong `transfer-service/application.yml`.

## Thử nhanh (qua gateway :8080)

```bash
# Xem số dư (đã seed: id 1 = 1.000.000, id 2 = 500.000, id 3 = 0)
curl http://localhost:8080/accounts/1

# Chuyển 200.000 từ 1 sang 2 QUA GATEWAY -> kỳ vọng status COMPLETED
curl -X POST http://localhost:8080/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":200000}'

# Demo rate limit: bắn dồn > 4 request/giây -> vài cái đầu 200, sau đó HTTP 429
for i in $(seq 1 12); do curl -s -o /dev/null -w "%{http_code}
" http://localhost:8080/accounts/1; done
```

## Lỗ hổng cố ý của bước 0 (bài học)

Nếu **trừ nguồn xong mà cộng đích lỗi**, tiền bị "kẹt" (đã rời nguồn, chưa tới đích) và
bước 0 **chưa hoàn lại**. Đây là chủ đích — để thấy vì sao cần **Saga + compensating
transaction** (bước 8). Chi tiết ở [`docs/step-00-notes.md`](docs/step-00-notes.md).
