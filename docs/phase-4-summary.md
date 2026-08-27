# Pha 4 — Tổng kết: Mở rộng (Job · Stream skip)

> Hoàn thành: 2026-08-27  
> Stack: Java 21, Spring Boot **3.5.14**  
> Nguồn học: yudao `lab-28` (Boot 2.x) → code lại HDL; `labx-11` Stream **không** code lại.

---

## 1. Pha 4 là gì?

**Phạm vi:** Chủ đề **tùy nhu cầu** sau 12 block core — scheduled task, (tuỳ chọn) MQ qua Spring Cloud Stream.

```text
Pha 1  Nền                    ✅ (2026-08-21)
Pha 2  Message                 ✅ (2026-08-24)
Pha 3  Spring Cloud           ✅ (2026-08-26)
Pha 4  Mở rộng                 ✅ (2026-08-27)
       #11 Stream Kafka        skip — đã vững lab-03 spring-kafka
       #12 Job (lab-28)        task-demo + quartz-jdbc; XXL-JOB skip
```

**Kết quả pha (HDL):** Biết 3 mức job — `@Scheduled` trong app → Quartz JDBC cluster → (overview) XXL-JOB platform. Không làm Spring Cloud Stream abstraction.

---

## 2. Hai block — trạng thái

| # | Lab yudao | HDL | Trạng thái |
|---|-----------|-----|------------|
| 11 | `labx-11` Stream Kafka | — | **skip** — `lab-03` đủ cho prod Kafka client |
| 12 | `lab-28` Job | `lab-28-task` | **xong** — `task-demo` + `quartz-jdbc`; memory + XXL skip |

Note chi tiết: [Spring Boot Job.md](../lab-28-task/Spring%20Boot%20Job.md).

---

## 3. Block #12 — đã chứng minh gì

### task-demo

- `@EnableScheduling` + `@Scheduled(fixedRate = 2000)`
- `spring.task.scheduling.*` — thread prefix, pool, graceful shutdown
- **Giới hạn:** 1 JVM; multi-pod → job trùng

### quartz-jdbc

- Dual datasource: `@Primary user` + `@QuartzDataSource quartz`
- MySQL: `lab-28-quartz-jdbc-user` (trống) + `lab-28-quartz-jdbc-quartz` (`QRTZ_*`)
- `JobDetail` / `Trigger` bean; `@DisallowConcurrentExecution`
- **Cluster:** `Application` + `Application02` — Job01 không fire gấp đôi; node chia trigger

### Boot 3 lesson (không có trên yudao copy-paste)

Bỏ `org.quartz.jobStore.dataSource` trong yaml khi dùng `@QuartzDataSource` — tránh `no DataSource named 'quartzDataSource'`.

---

## 4. Skip có chủ đích

| Mục | Lý do |
|-----|--------|
| `labx-11` Stream (~16 module yudao) | Abstraction mỏng trên Kafka; prod hay dùng spring-kafka trực tiếp |
| `quartz-memory` | API trùng jdbc |
| `xxl-job` | Admin Docker + API cũ; Quartz cluster đủ cho lab distributed job |
| `QuartzSchedulerTest` chạy song song bean | Trùng JobKey — giữ test, không execute khi dùng `ScheduleConfig` |

---

## 5. Checklist “đã xong Pha 4?”

- [ ] `@Scheduled` vs Quartz JobDetail/Trigger
- [ ] Tại sao tách 2 database (user vs quartz)
- [ ] `@QuartzDataSource` vs `@Primary`
- [ ] Cluster: cùng `scheduler-name`, compete trigger qua MySQL
- [ ] Biết khi nào cần XXL-JOB (UI ops) vs Quartz embed

---

## 6. Lộ trình 12 block

```text
✅ Pha 1–3  (10 block bắt buộc)
✅ Pha 4    #12 Job
⏭ Pha 4    #11 Stream (skip có lý do)
```

**Tiến độ lộ trình tối ưu:** **11 / 12** block hoàn thành + **1 skip** (#11). Coi **lộ trình chính đã đóng.**

Slot tùy chọn sau: Rabbit `confirm` / `confirm-async`, OAuth, observability — xem `learning-path.md`.

---

## 7. Tài liệu liên quan

- [learning-path.md](./learning-path.md)
- [phase-1-summary.md](./phase-1-summary.md)
- [phase-2-summary.md](./phase-2-summary.md)
- [phase-3-summary.md](./phase-3-summary.md)
- [Spring Boot Job.md](../lab-28-task/Spring%20Boot%20Job.md)

---

**Cập nhật lần cuối:** 2026-08-27
