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

## Kiến trúc (bước 0)

```
   client
     │  POST /transfers {fromAccountId, toAccountId, amount}
     ▼
┌──────────────────┐     RestClient (URL cứng http://localhost:8081)
│ transfer-service │ ─────────────────────────────────────────────►┐
│      :8082       │   POST /accounts/{id}/debit                    │
│   transfer_db    │   POST /accounts/{id}/credit                   ▼
└──────────────────┘                                    ┌──────────────────┐
                                                         │  account-service │
                                                         │      :8081       │
                                                         │    account_db    │
                                                         └──────────────────┘

  account-service-api (jar contract chung: DebitRequest/CreditRequest/AccountResponse)
  ↑ account-service implement, transfer-service dùng lại  (đây là "cách A" chia sẻ contract)
```

Ở bước 0: gọi nhau bằng **URL cứng**, chưa discovery/gateway/config/resilience/saga.
Các bước sau sẽ bồi từng mảnh vào chính khung này.

## Module

| Module | Cổng | DB | Vai trò |
|---|---|---|---|
| `account-service-api` | — | — | Jar contract chung (DTO). Bước 2 thêm interface HTTP. |
| `account-service` | 8081 | account_db | Giữ số dư, xử lý trừ/cộng tiền |
| `transfer-service` | 8082 | transfer_db | Điều phối một lần chuyển tiền |

## Cách chạy (bước 0)

Cần: JDK 21, Maven, Docker.

```bash
# 1) Bật Postgres (tạo sẵn account_db + transfer_db)
cd infra
docker compose up -d

# 2) Cài parent + api vào local maven (một lần, để service kế thừa version + thấy jar contract)
cd ..
mvn -N install                       # cài parent pom
mvn -pl account-service-api install  # build + install jar contract

# 3) Chạy hai service ở hai terminal
mvn -pl account-service spring-boot:run
mvn -pl transfer-service spring-boot:run
```

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
