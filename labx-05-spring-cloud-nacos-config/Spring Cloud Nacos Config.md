# Spring Cloud Nacos Config — Ghi chú học tập (labx-05)

> Config ngoài jar: dataId / group / namespace trên Nacos, app kéo lúc start; sửa UI có thể refresh **không** rebuild.  
> Yudao: Boot **2.2** + Cloud **Hoxton** + Alibaba **2.2** + `bootstrap.yml`.  
> HDL: Java 21, Boot **3.5.14** + Cloud **2025.0.3** + Alibaba **2025.0.0.0** + `spring.config.import`.  
> Pha 3 — block **#8** (sau Nacos Discovery + Feign). Cùng server Nacos block #7.

**Xong:** 2026-08-25 (2/2 module bắt buộc).

---

## Mục lục

1. [Lab đứng đâu?](#1-lab-đứng-đâu)
2. [Kế hoạch submodule HDL](#2-kế-hoạch-submodule-hdl)
3. [BOM / dependency](#3-bom--dependency)
4. [Demo (+ auto-refresh)](#4-demo--auto-refresh)
5. [Profiles (namespace)](#5-profiles-namespace)
6. [K8s analogue](#6-k8s-analogue)
7. [Lệch API 2.x → 3.5](#7-lệch-api-2x--35)
8. [Việc còn lại](#8-việc-còn-lại)

---

## 1. Lab đứng đâu?

```text
Pha 3  #7 Discovery+Feign = service ở đâu, gọi theo tên
Pha 3  #8 (lab này)       = config nằm ngoài jar
Pha 3  #9 Gateway         = cổng vào
Pha 3  #10 Sentinel       = limit / circuit
```

```text
Nacos :8848 (API)  :18080 (console)
        ▲
        │ dataId demo-application.yaml / DEFAULT_GROUP
   ┌────┴─────────────────────────────┐
   │ config-demo     :8084  namespace public  │
   │ config-profiles :8085  namespace dev     │
   └──────────────────────────────────────────┘
```

Cổng máy lab (Nacos giống #7):

| Process | Host |
|---------|------|
| Nacos console | 18080 |
| Nacos API / gRPC | 8848 / 9848 |
| config-demo | 8084 |
| config-profiles | 8085 |

Starter **`spring-cloud-starter-alibaba-nacos-config`** — **không** `nacos-discovery` (pom demo từng dính nhầm discovery).

---

## 2. Kế hoạch submodule HDL

| # | Module | Trạng thái | Yudao | Học gì |
|---|--------|------------|-------|--------|
| 1 | `labx-05-sc-nacos-config-demo` | ✅ | demo **+** auto-refresh (gộp 1 app) | Import yaml Nacos; `@ConfigurationProperties` vs `@Value` + `@RefreshScope` |
| 2 | `labx-05-sc-nacos-config-profiles` | ✅ | demo-profiles | Namespace isolation (`dev` ≠ `public`) |

**Bắt buộc:** 2/2 xong. **Không** clone 6 Maven module yudao.

Overview / skip lần đầu:

| Yudao | HDL |
|-------|-----|
| auto-refresh (module riêng) | **Gộp vào demo** — không skip |
| `demo-multi` | Skip code. Nên biết: nhiều dataId (`shared` / `extension` / vài dòng `spring.config.import`) |
| `demo-jasypt` | Skip. Secret: encrypt / K8s Secret / vault |
| `demo-actuator` | Skip. `/actuator/env` xem property |

Nacos namespace ≠ Kubernetes namespace (pod/service). Isolation env trên K8s ≈ ConfigMap/Secret khác overlay.

---

## 3. BOM / dependency

Giống #7: Boot parent **3.5.14** + `spring-cloud-dependencies` **2025.0.3** + `spring-cloud-alibaba-dependencies` **2025.0.0.0**. **Không** `2025.1` (Boot 4).

| Module | Starter |
|--------|---------|
| demo / profiles | `web` + **`nacos-config`** |

HDL **không** `bootstrap.yml`. Import bắt buộc ([SCA 2025 Nacos](https://sca.aliyun.com/en/docs/2025.x/user-guide/nacos/advanced-guide/)):

```yaml
spring.config.import:
  - nacos:demo-application.yaml?group=DEFAULT_GROUP&refreshEnabled=true
```

Thiếu `nacos:` import → starter báo lỗi. Lab **không** `optional:` lần đầu (fail rõ nếu Nacos/dataId sai).

Yudao: `bootstrap.yaml` + tự ghép dataId = `spring.application.name` + `file-extension`. HDL ghi **dataId tường minh**.

---

## 4. Demo (+ auto-refresh)

Port **8084**. Namespace mặc định **`public`**. Data ID `demo-application.yaml`, group `DEFAULT_GROUP`.

| Class | Bind |
|-------|------|
| `OrderProperties` | `@ConfigurationProperties(prefix = "order")` — **không** `@RefreshScope` |
| `DemoController` | `@RefreshScope` + `@Value("${order.pay-timeout-seconds}")` … |

Yaml Nacos lúc tạo: `pay-timeout-seconds: 30`, `create-frequency-seconds: 2`. Sau đó sửa UI → `60` / `3`, **không restart**.

**Chứng minh (2026-08-25):**

```text
GET :8084/demo/test01  → {"payTimeoutSeconds":30,"createFrequencySeconds":2}
GET :8084/demo/test02  → {"payTimeoutSeconds":60,"createFrequencySeconds":3}
```

Hai JSON lệch là **đúng bài**:

| Endpoint | Đọc từ | Ý nghĩa |
|----------|--------|---------|
| `/test01` | `OrderProperties` | Snapshot lúc **start** |
| `/test02` | `@Value` trên bean `@RefreshScope` | Environment **đã** đổi sau Publish trên UI |

Refresh **chạy** trên BOM này với `@Value` + `@RefreshScope`. `OrderProperties` không đổi vì **không** `@RefreshScope` (yudao demo cũng vậy). Muốn `/test01` theo UI: gắn `@RefreshScope` lên `OrderProperties` — lab **không** làm bước đó.

`@RefreshScope`: [Spring Cloud Commons — Refresh Scope](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/config.html).

---

## 5. Profiles (namespace)

Port **8085**. `spring.profiles.active: dev`. Copy `OrderProperties` + `DemoController` từ demo.

**Import + `server-addr` + `namespace` nằm cùng `application-dev.yml` / `application-prod.yml`.** Không để `spring.config.import` ở `application.yml` rồi `namespace` ở profile — import có thể chạy trước, dính **`public`** (số 30/60).

`application.yml` chỉ: port, `name: demo-application`, `profiles.active: dev`.

Lab này: `namespace: dev` (custom Namespace ID trên UI = `dev`). Không copy UUID máy yudao.

Nacos: tạo namespace **dev**, **chọn đúng namespace** rồi tạo **cùng** dataId `demo-application.yaml` với số **khác** public (`10` / `1`). [Console API — Create Namespace](https://nacos.io/en/docs/latest/manual/admin/console-api/): để trống ID thì Nacos sinh UUID; yaml phải khớp **ID**, không phải chỉ tên hiển thị.

**Chứng minh (2026-08-25):**

```text
GET :8085/demo/test01  → {"payTimeoutSeconds":10,"createFrequencySeconds":1}
```

Không phải `30/2` hay `60/3` của `public`. `/test01` = bind lúc start → boot đã import **dev**.

`application-prod.yml` còn placeholder `REPLACE_ME_PROD_NAMESPACE_ID` — **không** đếm vào xong isolation. Lặp namespace `prod` + số khác nếu muốn đủ cặp.

---

## 6. K8s analogue

Cùng **ý**: config không nhét trong image/jar. Khác **cách**.

| | Nacos Config (lab) | K8s thường gặp |
|--|-------------------|----------------|
| Cất | dataId / group / namespace | ConfigMap / Secret |
| App lấy | `spring.config.import` + client Nacos | mount file, env, hoặc Spring Cloud Kubernetes |
| Đổi lúc chạy | Listener + `@RefreshScope` (đã chứng minh `@Value`) | **Mặc định không.** Hay rolling restart / Reloader |

JD VN/US: ConfigMap/Secret + restart (hoặc GitOps). Live `@RefreshScope` = stack Alibaba, không mặc định cụm K8s.

---

## 7. Lệch API 2.x → 3.5

| | Yudao 2.x | HDL 3.5 |
|--|-----------|---------|
| Cloud / Alibaba | Hoxton / 2.2.0 | **2025.0.3** / **2025.0.0.0** |
| Load config | `bootstrap.yml` | `application.yml` + `spring.config.import` |
| Data ID | hay ghép từ `spring.application.name` | ghi tường minh `nacos:demo-application.yaml?group=…` |
| Profiles yaml | `bootstrap-dev.yaml` (cả namespace) | `application-dev.yml` (cả **import + namespace**) |
| Nacos UI | `:8848/nacos` | Nacos 3 console **:18080** |
| Starter | `nacos-config` | giữ; **không** nhầm `nacos-discovery` |

---

## 8. Việc còn lại

Block #8 **bắt buộc xong**. **#9 Gateway** xong. **#10 Sentinel** đóng (nacos; Feign skip) — `labx-04-spring-cloud-alibaba-sentinel/Spring Cloud Sentinel.md`. **Pha 3 xong.**

Không đếm vào xong #8: `prod` namespace, multi dataId, jasypt, actuator, `@RefreshScope` trên `OrderProperties`.
