# Spring Cloud & Microservices - Tài liệu tham khảo

> Tổng hợp kiến thức về Microservices, Spring Cloud, và các thành phần liên quan.  
> Phục vụ Pha 3 lộ trình HDL Spring Boot Labs.

**Cập nhật:** 2026-08-26

---

## Mục lục

1. [Microservices là gì?](#1-microservices-là-gì)
2. [Spring Cloud là gì?](#2-spring-cloud-là-gì)
3. [Các thành phần Spring Cloud](#3-các-thành-phần-spring-cloud)
4. [Kiến trúc Microservices](#4-kiến-trúc-microservices)
5. [Stack Nacos vs K8s](#5-stack-nacos-vs-k8s)
6. [Communication trong Microservices](#6-communication-trong-microservices)
7. [Các vấn đề cần giải quyết](#7-các-vấn-đề-cần-giải-quyết)
8. [Định vị trong lộ trình HDL](#8-định-vị-trong-lộ-trình-hdl)
9. [Thuật ngữ quan trọng](#9-thuật-ngữ-quan-trọng)

---



## 1. Microservices là gì?



### Khái niệm

**Microservices** = kiến trúc phần mềm: tách hệ thống thành **nhiều service nhỏ**, mỗi service:

- **Deploy độc lập**
- **Chạy process riêng**
- **Giao tiếp qua mạng** (HTTP, message queue)
- **Database riêng** (thường)
- **Team riêng** có thể phụ trách



### Monolith vs Microservices

```text
MONOLITH (Pha 1-2)
┌─────────────────────────────────────┐
│  Single Application (JAR/WAR)       │
│                                     │
│  ┌─────────┬─────────┬──────────┐   │
│  │  User   │  Order  │ Payment  │   │
│  │ Module  │ Module  │  Module  │   │
│  └─────────┴─────────┴──────────┘   │
│                                     │
│  ┌─────────────────────────────┐    │
│  │     Single Database         │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
     ↓ Deploy toàn bộ cùng lúc


MICROSERVICES (Pha 3)
┌────────────┐  ┌────────────┐  ┌────────────┐
│   User     │  │   Order    │  │  Payment   │
│  Service   │  │  Service   │  │  Service   │
│            │  │            │  │            │
│  ┌──────┐  │  │  ┌──────┐  │  │  ┌──────┐  │
│  │  DB  │  │  │  │  DB  │  │  │  │  DB  │  │
│  └──────┘  │  │  └──────┘  │  │  └──────┘  │
└─────┬──────┘  └─────┬──────┘  └─────┬──────┘
      │               │               │
      └───────────────┼───────────────┘
              Network (HTTP/MQ)
      ↓ Deploy riêng, scale riêng
```



### Ưu/Nhược điểm


| Ưu điểm                  | Nhược điểm                   |
| ------------------------ | ---------------------------- |
| Scale từng service riêng | Phức tạp vận hành            |
| Deploy độc lập           | Network latency              |
| Technology diversity     | Distributed transactions khó |
| Team autonomy            | Testing phức tạp hơn         |
| Fault isolation          | Monitoring phải tốt          |


---



## 2. Spring Cloud là gì?



### Định nghĩa

**Spring Cloud** = bộ **công cụ/thư viện** giúp xây dựng microservices với Spring Boot.

Không phải một framework, mà là **tập hợp các project** giải quyết vấn đề khi hệ thống phân tán.

### Vị trí trong stack

```text
┌─────────────────────────────────────────────┐
│  Application Layer                          │
│  - Spring Boot (business logic)             │
│  - REST APIs, Services                      │
│  - Pha 1: Redis, Security, Session, Async   │
├─────────────────────────────────────────────┤
│  Spring Cloud Layer ← Pha 3 HỌC Ở ĐÂY       │
│  - Service Discovery                        │
│  - API Gateway                              │
│  - Config Management                        │
│  - Circuit Breaker                          │
│  - Load Balancing                           │
├─────────────────────────────────────────────┤
│  Communication Layer                        │
│  - HTTP/REST (đồng bộ)                      │
│  - Message Queue (bất đồng bộ) ← Pha 2      │
├─────────────────────────────────────────────┤
│  Infrastructure Layer (Optional)            │
│  - Kubernetes                               │
│  - Docker                                   │
│  - Cloud Platform (AWS, GCP, Azure)         │
└─────────────────────────────────────────────┘
```

---



## 3. Các thành phần Spring Cloud



### Spring Cloud Projects

```text
Spring Cloud (Umbrella Project)
    │
    ├─► Gateway
    │   └─ API Gateway, routing, filters
    │
    ├─► OpenFeign
    │   └─ Declarative HTTP client
    │
    ├─► LoadBalancer
    │   └─ Client-side load balancing
    │
    ├─► Config
    │   └─ Centralized configuration
    │
    ├─► Alibaba ★ (yudao dùng)
    │   ├─ Nacos (Discovery + Config)
    │   └─ Sentinel (Circuit Breaker)
    │
    ├─► Netflix (deprecated nhiều)
    │   ├─ Eureka (Discovery)
    │   └─ Hystrix (Circuit Breaker) - archived
    │
    ├─► Consul
    │   └─ Consul integration
    │
    ├─► Kubernetes
    │   └─ K8s integration
    │
    └─► Stream
        └─ Message abstraction
```



### Các thành phần trong Pha 3


| Block | Component                                          | Vai trò                                              | Giờ    |
| ----- | -------------------------------------------------- | ---------------------------------------------------- | ------ |
| #7    | **Nacos Discovery** **OpenFeign** **LoadBalancer** | Service registry & client HTTP client Load balancing | 12-18h |
| #8    | **Nacos Config**                                   | Configuration management                             | 6-10h  |
| #9    | **Spring Cloud Gateway**                           | API Gateway                                          | 12-18h |
| #10   | **Sentinel**                                       | Circuit breaker, rate limit                          | 8-12h  |


---



## 4. Kiến trúc Microservices



### Kiến trúc tổng quan

```text
                    Internet
                        │
                        ▼
        ┌───────────────────────────┐
        │   Load Balancer (F5,      │
        │   Nginx, Cloud LB)        │
        └──────────┬────────────────┘
                   │
    ┌──────────────┼──────────────┐
    │  Microservices Platform     │
    │                             │
    │  ┌───────────────────────┐  │
    │  │  API Gateway          │  │
    │  │  (Spring Cloud GW     │  │
    │  │   hoặc K8s Ingress)   │  │
    │  └──────────┬────────────┘  │
    │             │               │
    │    ┌────────┼────────┐      │
    │    ▼        ▼        ▼      │
    │  ┌────┐  ┌────┐  ┌────┐     │
    │  │S1  │  │S2  │  │S3  │     │ Services
    │  └─┬──┘  └─┬──┘  └─┬──┘     │
    │    │       │       │        │
    │  ┌─┴───────┴───────┴──┐     │
    │  │  Service Registry  │     │
    │  │  (Nacos/Eureka/    │     │
    │  │   K8s Service)     │     │
    │  └────────────────────┘     │
    │                             │
    │  ┌────────────────────┐     │
    │  │  Config Server     │     │
    │  │  (Nacos Config/    │     │
    │  │   K8s ConfigMap)   │     │
    │  └────────────────────┘     │
    │                             │
    │  ┌────────────────────┐     │
    │  │  Message Broker    │     │
    │  │  (Kafka/RabbitMQ)  │     │
    │  └────────────────────┘     │
    └─────────────────────────────┘
```



### Service-to-Service Communication

```text
┌─────────────────────────────────────────────────────┐
│  Client Request                                     │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │   API Gateway         │
        │   (Single Entry)      │
        └───────────┬───────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
┌───────────────┐       ┌───────────────┐
│ User Service  │       │ Order Service │
│               │       │               │
│  @Feign ──────┼──────►│               │
│  Client       │  HTTP │               │
└───────┬───────┘       └───────┬───────┘
        │                       │
        │   ① Query registry    │
        │   ② Get instance list │
        │   ③ Load balance      │
        │   ④ HTTP call         │
        │                       │
        │       Kafka           │
        └──────►Topic◄──────────┘
           (async events)
```

---



## 5. Stack Nacos vs K8s



### Stack China (Alibaba/Nacos)

```text
┌─────────────────────────────────────────────┐
│  Client                                     │
└────────────────┬────────────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │ Spring Cloud   │
        │    Gateway     │
        └────────┬───────┘
                 │
        ┌────────┴───────────────┐
        │                        │
        ▼                        ▼
  ┌──────────┐            ┌──────────┐
  │  User    │◄─OpenFeign─┤  Order   │
  │ Service  │            │ Service  │
  └────┬─────┘            └────┬─────┘
       │                       │
       │ ① Đăng ký            │
       │ ② Query địa chỉ      │
       │                       │
       └───────┬───────────────┘
               ▼
       ┌───────────────┐
       │     Nacos     │
       │               │
       │ ┌───────────┐ │
       │ │ Discovery │ │
       │ └───────────┘ │
       │ ┌───────────┐ │
       │ │   Config  │ │
       │ └───────────┘ │
       └───────────────┘
       
       ┌───────────────┐
       │   Sentinel    │ (Circuit Breaker)
       └───────────────┘
       
       ┌───────────────┐
       │     Kafka     │
       └───────────────┘
```



### Stack VN/US (Kubernetes)

```text
┌─────────────────────────────────────────────┐
│  Client                                     │
└────────────────┬────────────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │ K8s Ingress    │
        │  (hoặc GW)     │
        └────────┬───────┘
                 │
     ┌───────────┴──────────────┐
     │  Kubernetes Cluster      │
     │                          │
     │  ┌────────────────────┐  │
     │  │  user-service      │  │
     │  │  ┌────┐  ┌────┐   │  │
     │  │  │Pod1│  │Pod2│   │  │
     │  │  └────┘  └────┘   │  │
     │  └──────┬─────────────┘  │
     │         │                │
     │         │ http://user-service:8080
     │         │ (K8s Service DNS)
     │         │                │
     │         ▼                │
     │  ┌────────────────────┐  │
     │  │  order-service     │  │
     │  │  ┌────┐  ┌────┐   │  │
     │  │  │Pod1│  │Pod2│   │  │
     │  │  └────┘  └────┘   │  │
     │  └────────────────────┘  │
     │                          │
     │  ┌────────────────────┐  │
     │  │  ConfigMap/Secret  │  │
     │  └────────────────────┘  │
     │                          │
     │  ┌────────────────────┐  │
     │  │  Resilience4j      │  │
     │  │  (Circuit Breaker) │  │
     │  └────────────────────┘  │
     └──────────────────────────┘
     
        ┌───────────────┐
        │     Kafka     │
        └───────────────┘
```



### So sánh trực tiếp


| Chức năng              | Stack Nacos (China)           | Stack K8s (VN/US)                     |
| ---------------------- | ----------------------------- | ------------------------------------- |
| **Service Discovery**  | Nacos Discovery (app đăng ký) | K8s Service DNS (tự động)             |
| **Load Balancing**     | Spring Cloud LoadBalancer     | K8s Service (built-in)                |
| **Config Management**  | Nacos Config Server           | K8s ConfigMap/Secret                  |
| **Circuit Breaker**    | Sentinel                      | Resilience4j                          |
| **API Gateway**        | Spring Cloud Gateway          | Spring Cloud Gateway hoặc K8s Ingress |
| **Container Platform** | Tùy chọn (Docker/K8s)         | **Bắt buộc K8s**                      |
| **JD thấy ở**          | China, Alibaba Cloud          | **VN, US, EU, Global**                |


---



## 6. Communication trong Microservices



### Hai họ giao tiếp

```text
┌──────────────────────────────────────────────┐
│  SYNCHRONOUS (Request/Response)              │
│                                              │
│  Client → Service A → Service B → Response  │
│           (chờ kết quả)                      │
│                                              │
│  Dùng: HTTP/REST, gRPC                       │
│  Tools: OpenFeign, RestTemplate, WebClient  │
│  Ưu: Đơn giản, kết quả ngay                  │
│  Nhược: Coupled, blocking, cascade failure   │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│  ASYNCHRONOUS (Event-Driven)                 │
│                                              │
│  Service A → Publish Event → Topic           │
│                               ↓              │
│                    ┌──────────┼──────────┐   │
│                    ▼          ▼          ▼   │
│                Service B  Service C  Service D│
│                (không chờ, xử lý sau)        │
│                                              │
│  Dùng: Kafka, RabbitMQ, AWS SQS              │
│  Ưu: Decoupled, scalable, fault tolerant    │
│  Nhược: Eventual consistency, phức tạp        │
└──────────────────────────────────────────────┘
```



### Khi nào dùng gì?


| Tình huống                                    | Dùng                       |
| --------------------------------------------- | -------------------------- |
| User xem trang profile (cần data ngay)        | **HTTP/REST**              |
| User đặt hàng → Gửi email, trừ kho, tính điểm | **Message Queue**          |
| Service A cần Service B confirm trước khi trả | **HTTP + Saga** (hoặc 2PC) |
| Analytics, logging, audit trail               | **Message Queue**          |
| Public API cho mobile/web                     | **HTTP/REST**              |




### Pattern thực tế

```text
┌────────────────────────────────────────────────┐
│  Order Flow (Hybrid Pattern)                  │
└────────────────────────────────────────────────┘

① User tạo order
    ↓ HTTP POST /orders
┌────────────┐
│   Order    │
│  Service   │
└─────┬──────┘
      │
      ├─► HTTP GET /inventory/check (đồng bộ)
      │   ▼
      │  ┌──────────────┐
      │  │  Inventory   │
      │  │   Service    │
      │  └──────────────┘
      │
      ├─► HTTP POST /payment/charge (đồng bộ)
      │   ▼
      │  ┌──────────────┐
      │  │   Payment    │
      │  │   Service    │
      │  └──────────────┘
      │
      └─► Publish "OrderCreated" event (bất đồng bộ)
          ▼
         Kafka
          │
          ├──► Email Service (gửi mail)
          ├──► Analytics Service (track)
          └──► Notification Service (push)
```

---



## 7. Các vấn đề cần giải quyết



### Problem Matrix


| Vấn đề                  | Monolith                  | Microservices                   | Giải pháp                            |
| ----------------------- | ------------------------- | ------------------------------- | ------------------------------------ |
| **Service tìm nhau**    | Không có (cùng process)   | IP/port động                    | Service Discovery (Nacos, K8s)       |
| **Load balancing**      | Server-side LB            | Client-side LB                  | Spring Cloud LoadBalancer            |
| **Configuration**       | application.yml trong jar | Nhiều service, config khác nhau | Config Server (Nacos, K8s ConfigMap) |
| **Circuit breaker**     | Ít cần (cùng process)     | Service chết → cascade          | Sentinel, Resilience4j               |
| **API Gateway**         | Một app, một endpoint     | Nhiều service, nhiều port       | Spring Cloud Gateway, K8s Ingress    |
| **Distributed tracing** | Stack trace đủ            | Request qua nhiều service       | Sleuth, Zipkin, Jaeger               |
| **Monitoring**          | Log một chỗ               | Log phân tán                    | ELK Stack, Prometheus, Grafana       |




### Service Discovery Flow

```text
WITHOUT DISCOVERY (Hard-coded)
┌────────────┐
│   Order    │  http://192.168.1.10:8080/users/123
│  Service   ├─────────────────────────────────────►
└────────────┘                                      
                                               ┌────────────┐
❌ IP thay đổi → fail                          │   User     │
❌ Scale nhiều instance → không biết           │  Service   │
❌ Instance chết → vẫn gọi                     └────────────┘


WITH DISCOVERY
                  ② Query: "user-service ở đâu?"
┌────────────┐    ┌───────────────────────┐
│   Order    ├───►│   Service Registry    │
│  Service   │◄───┤   (Nacos/K8s)         │
└─────┬──────┘    └───────────────────────┘
      │            ③ Return: [192.168.1.10:8080,
      │                       192.168.1.11:8080]
      │
      │ ④ Load Balance → pick 192.168.1.10
      │
      └─► http://192.168.1.10:8080/users/123
                                               ┌────────────┐
✅ Dynamic discovery                           │   User     │
✅ Auto load balance                           │  Service   │
✅ Health check                                │ (instance) │
                                               └────────────┘

      ① Startup
         ┌────────────┐
         │   User     │
         │  Service   ├──── Register
         └────────────┘      "user-service @ 192.168.1.10:8080"
                                 │
                                 ▼
                          Service Registry
```



### Config center (Nacos Config — block #8)

Config **không** nằm trong jar. Cùng dataId + group, **khác namespace** = khác file.

```text
Nacos Config
  public / demo-application.yaml     → 30/2 (rồi sửa UI 60/3)
  dev    / demo-application.yaml     → 10/1
        │
        │  spring.config.import: nacos:demo-application.yaml?group=DEFAULT_GROUP
        ▼
  Spring Environment
        │
        ├─ @ConfigurationProperties  (OrderProperties — snapshot lúc start trừ khi @RefreshScope)
        └─ @Value + @RefreshScope    (DemoController — tạo lại bean khi Nacos publish)
```

HDL: `labx-05-spring-cloud-nacos-config`. Note: [Spring Cloud Nacos Config.md](../labx-05-spring-cloud-nacos-config/Spring%20Cloud%20Nacos%20Config.md).

K8s analogue: ConfigMap/Secret. **Không** mặc định live-refresh như `@RefreshScope`.



### Circuit Breaker Pattern

```text
┌────────────────────────────────────────────────────┐
│  Circuit Breaker States                           │
└────────────────────────────────────────────────────┘

    CLOSED (Normal)
    ┌───────────┐
    │  Request  │
    │     ↓     │
    │  Success  │
    └───────────┘
         │
         │ Nhiều lỗi liên tiếp (threshold)
         ▼
    OPEN (Ngắt)
    ┌───────────┐
    │  Request  │
    │     ↓     │
    │  Fallback │ ← trả ngay, không gọi service
    │  (cached/ │
    │   default)│
    └───────────┘
         │
         │ Sau timeout (30s)
         ▼
    HALF-OPEN (Thử lại)
    ┌───────────┐
    │  Request  │
    │     ↓     │
    │   Test    │
    └─────┬─────┘
          │
    ┌─────┴─────┐
    │           │
    ▼           ▼
 Success      Fail
    │           │
    ▼           ▼
 CLOSED      OPEN


Flow diagram:
┌─────────┐     Error rate > 50%      ┌─────────┐
│ CLOSED  ├────────────────────────────►│  OPEN   │
└────▲────┘                            └────┬────┘
     │                                      │
     │                                      │ Wait 30s
     │                                      │
     │                                      ▼
     │                              ┌───────────────┐
     │         Success              │  HALF-OPEN    │
     └──────────────────────────────┤               │
                                    │  Try 1 request│
                                    └───────┬───────┘
                                            │ Fail
                                            │
                                            ▼
                                         OPEN
```

---



## 8. Định vị trong lộ trình HDL



### Lộ trình 4 pha

```text
┌──────────────────────────────────────────────────┐
│  Pha 1: Nền trong một process ✅                  │
│  Redis → Security → Session → Async              │
└──────────────────────────────────────────────────┘
                    │
                    │ Đủ kỹ năng xây 1 app
                    ▼
┌──────────────────────────────────────────────────┐
│  Pha 2: Message Queue ✅                          │
│  RabbitMQ → Kafka                                │
└──────────────────────────────────────────────────┘
                    │
                    │ Biết gửi/nhận event async
                    ▼
┌──────────────────────────────────────────────────┐
│  Pha 3: Spring Cloud ✅                            │
│  Nacos → Config → Gateway → Sentinel (nacos ✅; Feign skip) │
│  Học: Nhiều service gọi nhau + infra             │
└──────────────────────────────────────────────────┘
                    │
                    │ Biết xây microservices
                    ▼
┌──────────────────────────────────────────────────┐
│  Pha 4: Mở rộng ← BÂY GIỜ                         │
│  Stream Kafka → Job                              │
└──────────────────────────────────────────────────┘
```



### Pha 3 chi tiết


| Block   | Lab                   | Học                         | Giờ    | Trạng thái  |
| ------- | --------------------- | --------------------------- | ------ | ----------- |
| **#7**  | `labx-01` + `labx-03` | Nacos Discovery + OpenFeign | 12-18h | ✅ 2026-08-25. Note: [Spring Cloud Nacos Feign.md](../labx-01-spring-cloud-nacos-feign/Spring%20Cloud%20Nacos%20Feign.md) |
| **#8**  | `labx-05`             | Nacos Config                | 6-10h  | ✅ 2026-08-25. Note: [Spring Cloud Nacos Config.md](../labx-05-spring-cloud-nacos-config/Spring%20Cloud%20Nacos%20Config.md) |
| **#9**  | `labx-08`             | Spring Cloud Gateway        | 12-18h | ✅ 2026-08-26. 3/3: `static` :8086, `registry` :8087, `rate-limit` :8088. Note: [Spring Cloud Gateway.md](../labx-08-spring-cloud-gateway/Spring%20Cloud%20Gateway.md) |
| **#10** | `labx-04`             | Sentinel                    | 8-12h  | ✅ 2026-08-26. nacos :8090. Demo Dashboard skip. **Feign skip.** Note: [Spring Cloud Sentinel.md](../labx-04-spring-cloud-alibaba-sentinel/Spring%20Cloud%20Sentinel.md) §9 |


**Tổng Pha 3:** ~38-58h

---



## 9. Thuật ngữ quan trọng



### A-D

- **API Gateway**: Cổng vào cho client. Lab HDL: `static` :8086 URI tĩnh; `registry` :8087 `lb://` + Nacos; `rate-limit` :8088 `RequestRateLimiter` + Redis → **429**. Ba module **thay** nhau, không nối hai hop.
- **Circuit Breaker**: Pattern ngắt request khi service downstream chết/chậm, tránh cascade failure.
- **Client-side Load Balancing**: Client chọn instance nào gọi (vs Server-side LB).
- **Config Server**: Server tập trung cung cấp configuration cho các service (Nacos Config, Spring Cloud Config, K8s ConfigMap).
- **Data ID / Group / Namespace**: Khóa config trên Nacos. Cùng dataId+group, khác namespace = khác file. Namespace Nacos ≠ K8s namespace.



### E-M

- **Eureka**: Netflix service discovery (deprecated trong yudao, dùng Nacos).
- **Fallback**: Giá trị/hành động dự phòng khi service call thất bại.
- **Feign/OpenFeign**: Declarative HTTP client của Spring Cloud.
- **Microservices**: Kiến trúc nhiều service nhỏ, deploy độc lập.



### N-S

- **Nacos**: Alibaba service discovery + config server. Lab HDL: discovery = `nacos-discovery`; config = `nacos-config` (hai starter khác nhau).
- **@RefreshScope**: Spring Cloud Context — tạo lại bean khi Environment đổi (Nacos publish). Lab: `@Value` trên `DemoController` refresh; `OrderProperties` không gắn thì giữ snapshot start.
- **Pod**: Đơn vị nhỏ nhất trong K8s, chứa container.
- **Registry**: Service registry, nơi service đăng ký/discover.
- **Resilience**: Khả năng chịu lỗi của hệ thống.
- **Service Discovery**: Cơ chế service tìm nhau động.
- **Sentinel**: Alibaba circuit breaker + flow control **trong service** (không phải Gateway Redis 429). Lab HDL: `nacos` :8090 rule JSON Data ID `sentinel-nacos-flow-rule`; resource v6x = path `/demo/echo`. Note: [Spring Cloud Sentinel.md](../labx-04-spring-cloud-alibaba-sentinel/Spring%20Cloud%20Sentinel.md).



### Khác

- **Sidecar**: Container phụ chạy cùng pod với main container.
- **Spring Cloud**: Bộ công cụ xây microservices với Spring.
- **Service Mesh**: Infrastructure layer quản lý service-to-service communication (Istio, Linkerd).

---



## Tham khảo



### Docs chính thức

- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba)
- [Nacos](https://nacos.io/en-us/)
- [Kubernetes](https://kubernetes.io/docs/)



### Trong repo HDL

- [learning-path.md](./learning-path.md) - Lộ trình tổng thể
- [phase-1-summary.md](./phase-1-summary.md) - Tổng kết Pha 1
- [phase-2-summary.md](./phase-2-summary.md) - Tổng kết Pha 2
- [phase-3-summary.md](./phase-3-summary.md) - Tổng kết Pha 3
- [Spring Cloud Nacos Feign.md](../labx-01-spring-cloud-nacos-feign/Spring%20Cloud%20Nacos%20Feign.md) - Block #7
- [Spring Cloud Nacos Config.md](../labx-05-spring-cloud-nacos-config/Spring%20Cloud%20Nacos%20Config.md) - Block #8
- [Spring Cloud Gateway.md](../labx-08-spring-cloud-gateway/Spring%20Cloud%20Gateway.md) - Block #9 (3/3 xong)
- [Spring Cloud Sentinel.md](../labx-04-spring-cloud-alibaba-sentinel/Spring%20Cloud%20Sentinel.md) - Block #10 (nacos xong; Feign skip)

---

**Cập nhật lần cuối:** 2026-08-26  
**Tác giả:** HDL Spring Boot Labs