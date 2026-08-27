# Spring Boot Job — Ghi chú học tập (lab-28)

> Scheduled task / distributed job trong Spring Boot.  
> Yudao: Boot **2.2** — Spring Task, Quartz memory/jdbc, XXL-JOB 2.1.  
> HDL: Java 21, Boot **3.5.14** — `task-demo` + `quartz-jdbc` cluster.  
> Pha 4 — block **#12** (sau Redis lock concept ở Pha 1).

**Tiến độ:** 2026-08-27 — **task-demo** + **quartz-jdbc** xong (cluster 2 JVM). **quartz-memory skip.** **XXL-JOB skip** (§6). Block **#12 đóng.** **Pha 4 xong** (kèm #11 Stream skip — xem `learning-path.md`).

---

## Mục lục

1. [Lab đứng đâu?](#1-lab-đứng-đâu)
2. [Kế hoạch submodule HDL](#2-kế-hoạch-submodule-hdl)
3. [task-demo — Spring Task](#3-task-demo--spring-task)
4. [quartz-jdbc — cluster](#4-quartz-jdbc--cluster)
5. [Boot 3 vs yudao 2.x](#5-boot-3-vs-yudao-2x)
6. [Skip](#6-skip)
7. [Proof checklist](#7-proof-checklist)
8. [Lỗi thường gặp](#8-lỗi-thường-gặp)

---

## 1. Lab đứng đâu?

```text
Pha 1  lab-29 Async     = thread pool trong app (gần scheduling)
Pha 4  task-demo       = @Scheduled — 1 JVM, không cluster
Pha 4  quartz-jdbc     = Quartz + MySQL — nhiều instance, không fire trùng
Pha 4  (skip) XXL-JOB  = admin tách + executor — platform job
```

```text
                    ┌─────────────────┐
  @Scheduled        │  task-demo      │  RAM, 1 process
  (Spring Task)     └─────────────────┘

                    ┌─────────────────┐
  Quartz JDBC       │ quartz-jdbc     │  MySQL QRTZ_*, cluster
  cluster           └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
         Application                   Application02
         (port mặc định)                (server.port=0)
```

**Không nhầm:** Spring Task / Quartz embedded ≠ XXL-JOB (scheduler ngoài app). Stream Kafka (`labx-11`) = messaging — **skip** vì đã có `lab-03` spring-kafka.

---

## 2. Kế hoạch submodule HDL

| # | Module | Trạng thái | Yudao | Học gì |
|---|--------|------------|-------|--------|
| 1 | `lab-28-task-demo` | ✅ | task-demo | `@EnableScheduling`, `@Scheduled`, `spring.task.scheduling.*` |
| 2 | `lab-28-task-quartz-memory` | **skip** | quartz-memory | Trùng API Quartz; nhảy thẳng jdbc |
| 3 | `lab-28-task-quartz-jdbc` | ✅ | quartz-jdbc | Dual DS, `@QuartzDataSource`, JobDetail/Trigger, cluster |
| 4 | `lab-28-task-xxl-job` | **skip** | xxl-job | Admin + executor — infra Docker; §6 |

Parent Maven: `lab-28-task/pom.xml` (aggregator). Module con parent trực tiếp `spring-boot-starter-parent` **3.5.14** (giống các lab HDL khác).

---

## 3. task-demo — Spring Task

**Một câu:** Bật scheduler trong JVM, method `@Scheduled` chạy theo fixedRate/cron — **không** persist, **không** an toàn multi-instance.

| Thành phần | Ghi chú |
|------------|---------|
| `ScheduleConfig` | `@EnableScheduling` |
| `DemoJob` | `@Scheduled(fixedRate = 2000)` — log đếm lần chạy |
| `application.yml` | `spring.task.scheduling.thread-name-prefix`, `pool.size`, `shutdown.await-termination` |

**Proof (2026-08-27):** log `[hdl-demo-1]` (sau thêm yaml) hoặc `[scheduling-1]` (default), chu kỳ ~2s, `counts` tăng liên tục (singleton bean).

**Giới hạn prod:** scale 2 pod → job chạy **2 lần**; đổi cron = redeploy.

---

## 4. quartz-jdbc — cluster

**Một câu:** Job/trigger lưu MySQL; nhiều app cùng `scheduler-name` compete trigger — **một** node fire mỗi lần.

### Hạ tầng MySQL

| Database | Mục đích |
|----------|----------|
| `lab-28-quartz-jdbc-user` | DB business giả — `@Primary userDataSource` (lab không query) |
| `lab-28-quartz-jdbc-quartz` | Bảng `QRTZ_*` — script `tables_mysql_innodb.sql` (Quartz dist) |

`spring.quartz.jdbc.initialize-schema: never` — DDL thủ công.

### Code chính

| Class | Vai trò |
|-------|---------|
| `DataSourceConfig` | `userDataSource` @Primary + `quartzDataSource` @QuartzDataSource |
| `ScheduleConfig` | Bean `JobDetail` + `Trigger` — DemoJob01 Simple 5s, DemoJob02 Cron 10s |
| `DemoJob01/02` | `QuartzJobBean`, `@DisallowConcurrentExecution`, inject `DemoService` |
| `Application02` | Instance 2 — `server.port=0` |

**Đăng ký job:** HDL dùng **bean auto** (`ScheduleConfig`). `QuartzSchedulerTest` có sẵn (JUnit 5) — **không chạy** cùng lúc với bean nếu cùng JobKey (`overwrite-existing-jobs: false`).

### Yaml Quartz (Boot 3 — quan trọng)

- `spring.quartz.job-store-type: jdbc`
- `scheduler-name: clusteredScheduler` → cột `SCHED_NAME`
- `properties.org.quartz.jobStore.isClustered: true`
- **Không** set `org.quartz.jobStore.dataSource: quartzDataSource` khi dùng `@QuartzDataSource` — Boot 3 gắn DS qua `SchedulerFactoryBean`; set tên trong yaml → lỗi `There is no DataSource named 'quartzDataSource'` (Quartz `DBConnectionManager` ≠ Spring bean).

Ref: [Spring Boot 3.5 — Quartz Scheduler](https://docs.spring.io/spring-boot/3.5/reference/io/quartz.html) (`@QuartzDataSource`).

### Proof cluster (2026-08-27)

Chạy song song `Application` + `Application02`:

| Quan sát | Kỳ vọng | Kết quả |
|----------|---------|---------|
| Job01 ~5s toàn hệ thống | Không gấp đôi khi 2 JVM | ✅ app1 Job01 dừng ~46s, app2 nhận Job01 ~5s |
| Job02 ~10s | Một node giữ trigger | ✅ chủ yếu app1 |
| Inject `DemoService` | Mỗi JVM một bean | ✅ hash khác nhau |

Startup lần đầu có thể **burst** log (misfire trigger cũ trong DB) — truncate `QRTZ_*` nếu cần log sạch.

---

## 5. Boot 3 vs yudao 2.x

| Chủ đề | Yudao 2.x | HDL 3.5 |
|--------|-----------|---------|
| MySQL driver | `mysql-connector-java` 5.x | `mysql-connector-j` (BOM) |
| Driver class | `com.mysql.jdbc.Driver` | `com.mysql.cj.jdbc.Driver` |
| Quartz JDBC DS trong yaml | `jobStore.dataSource` + `JobStoreTX` | **Bỏ** `dataSource` / `JobStoreTX`; dùng `@QuartzDataSource` |
| Test | JUnit 4 + `SpringRunner` | JUnit 5 + `@SpringBootTest` |
| `await-termination-period` | số giây (Boot 2) | `Duration` — `60s` (task-demo) |
| XXL-JOB | `@JobHandler` + `IJobHandler` 2.1 | skip; nếu làm sau: `@XxlJob` 2.4+ |

API Quartz (`JobBuilder`, `QuartzJobBean`, `@DisallowConcurrentExecution`) **giữ nguyên**.

---

## 6. Skip

### quartz-memory (yudao)

RAMJobStore, single JVM — API trùng jdbc. Học jdbc trực tiếp đủ.

### XXL-JOB (yudao `lab-28-task-xxl-job`)

| Lý do skip |
|------------|
| Cần deploy **xxl-job-admin** + MySQL riêng (Docker/JAR :8080) |
| API yudao `@JobHandler` / `IJobHandler` cũ — Boot 3 cần `@XxlJob` |
| Kiến thức **distributed job** đã có từ Quartz JDBC cluster |
| Prod: dùng XXL khi cần **UI ops** (cron trên web, log, alarm) — đọc overview yudao / doc XXL khi gặp công ty |

### labx-11 Stream (block #11 — ngoài lab-28)

Skip riêng trong `learning-path.md`: đã vững `lab-03` spring-kafka; Stream = abstraction mỏng, ROI thấp.

---

## 7. Proof checklist

**task-demo**

- [x] Startup, log ~2s/lần
- [x] `spring.task.scheduling` (prefix thread pool)

**quartz-jdbc**

- [x] 2 database + `QRTZ_*` trên DB quartz
- [x] Startup không lỗi DS
- [x] DemoJob01 ~5s, DemoJob02 giây 0/10/20…
- [x] `DemoService` inject trên Job01
- [x] `Application` + `Application02` — Job01 không fire trùng / không tăng gấp đôi tần suất
- [ ] (Tuỳ chọn) `SELECT * FROM QRTZ_SCHEDULER_STATE` — 2 row khi 2 instance

---

## 8. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Xử lý |
|-------------|-------------|--------|
| `no DataSource named 'quartzDataSource'` | yaml `jobStore.dataSource` + Boot 3 `@QuartzDataSource` conflict | Bỏ `dataSource` / `JobStoreTX` khỏi yaml — §4 |
| Table `QRTZ_*` doesn't exist | Chưa chạy script | `tables_mysql_innodb.sql` trên DB quartz |
| Burst log lúc startup | Misfire / trigger cũ trong DB | Truncate `QRTZ_*` hoặc bỏ qua nếu sau đó ổn |
| `ObjectAlreadyExistsException` | Bean + `QuartzSchedulerTest` cùng JobKey | Chọn một cách đăng ký |
| Shutdown log `NON_CLUSTERED` | Format log / bind property | Hành vi runtime (2 JVM không double Job01) quan trọng hơn; có thể thêm key phẳng `org.quartz.jobStore.isClustered` + `instanceId: AUTO` nếu muốn chắc config |

---

## Tài liệu tham chiếu

- Yudao: `lab-28/《芋道 Spring Boot 定时任务入门》.md`
- [Task Execution and Scheduling — Boot 3.5](https://docs.spring.io/spring-boot/3.5/reference/features/task-execution-and-scheduling.html)
- [Quartz Scheduler — Boot 3.5](https://docs.spring.io/spring-boot/3.5/reference/io/quartz.html)

---

**Cập nhật lần cuối:** 2026-08-27
