# Spring Kafka — Ghi chú học tập (lab-03)

> Tổng hợp từ `lab-03-kafka`.  
> Yudao source: Boot **2.x** + `ListenableFuture` / `SeekToCurrentErrorHandler` / `javax.*`.  
> HDL: Boot **3.5** + `CompletableFuture` / `DefaultErrorHandler` / `jakarta.*` + JSON (`JsonSerializer` / `JsonDeserializer`).  
> Pha 2 — Message (sau RabbitMQ).

---

## Mục lục

1. [Lab đứng đâu?](#1-lab-đứng-đâu)
2. [Kế hoạch submodule HDL](#2-kế-hoạch-submodule-hdl)
3. [Broker (Podman / KRaft)](#3-broker-podman--kraft)
4. [CLI thường dùng](#4-cli-thường-dùng)
5. [Dependency (Boot 3.5)](#5-dependency-boot-35)
6. [Module demo — `lab-03-kafka-demo`](#6-module-demo--lab-03-kafka-demo-)
7. [Module ack — `lab-03-kafka-demo-ack`](#7-module-ack--lab-03-kafka-demo-ack-)
8. [Module concurrency — `lab-03-kafka-demo-concurrency`](#8-module-concurrency--lab-03-kafka-demo-concurrency-)
9. [Module batch — `lab-03-kafka-demo-batch`](#9-module-batch--lab-03-kafka-demo-batch-)
10. [Lệch API 2.x → 3.5](#10-lệch-api-2x--35-checklist-nhanh)
11. [Kiểm chứng demo (tay)](#11-kiểm-chứng-demo-tay)
12. [Việc còn lại](#12-việc-còn-lại)
13. [So nhanh với Rabbit lab-04](#13-so-nhanh-với-rabbit-lab-04)

---

## 1. Lab đứng đâu?

```text
RabbitMQ (lab-04)  = Exchange → Queue; ack/DLQ trên queue
Kafka (lab-03)     = Topic → Partition (log); offset + consumer group; DLT = topic chết
```

```text
Producer  →  Topic (nhiều Partition)  →  Consumer Group(s)
                    ↑
              Kafka broker (KRaft)
```

| Khái niệm | Ý nghĩa ngắn |
|---|---|
| Topic | Luồng event theo tên |
| Partition | Phân mảnh song song + thứ tự **trong** partition |
| Key | Thường quyết định partition (cùng key → cùng partition) |
| Offset | Vị trí đọc trên partition; Kafka **giữ** message theo retention |
| Consumer group | Cùng group chia partition; **khác group** mỗi group nhận một bản (fan-out) |
| DLT | Dead Letter **Topic** (không gọi DLX như Rabbit) |

---

## 2. Kế hoạch submodule HDL

| # | Module | Trạng thái | Học gì | Chi tiết |
|---|---|---|---|---|
| 1 | `lab-03-kafka-demo` | ✅ | Sync/async, 2 group, fail → retry → DLT | [§6](#6-module-demo--lab-03-kafka-demo-) |
| 2 | `lab-03-kafka-demo-ack` | ✅ | Manual ack; lab `%2` cố ý không ack một số tin | [§7](#7-module-ack--lab-03-kafka-demo-ack-) |
| 3 | `lab-03-kafka-demo-concurrency` | ✅ | `concurrency` vs partition; key / sticky | [§8](#8-module-concurrency--lab-03-kafka-demo-concurrency-) |
| 4 | `lab-03-kafka-demo-batch` | ✅ | **Producer** batch (`linger.ms` / `batch.size`); không phải batch-consume | [§9](#9-module-batch--lab-03-kafka-demo-batch-) |

**Sau mỗi submodule xong:** cập nhật note này (+ `docs/learning-path.md`) **trước** khi sang module kế — giống quy trình `lab-04` / `Spring RabbitMQ.md`.

**Lab-03 bắt buộc:** 4/4 xong. Overview / skip lần đầu (yudao): `native`, `batch-consume`, `broadcast`, `transaction`.

**Ánh xạ số Demo HDL ↔ yudao:**

| HDL | Yudao | Module | Nội dung |
|---|---|---|---|
| Demo01 (+ Demo01A) | Demo01 (+ Demo01A) | demo | Sync/async + hai `groupId` khác nhau |
| Demo02 | Demo04 | demo | Consumer cố ý throw + error handler + DLT |
| Demo03 | Demo08 | ack | Manual ack; chỉ `acknowledge()` khi `id % 2 == 1` |
| Demo04 | Demo06 | concurrency | `concurrency="2"`; `syncSend` / `syncSendOrderly` |
| Demo05 | Demo02 (`demo-batch`) | batch | Producer async + `linger.ms`; topic HDL `DEMO_05` (tránh trùng `DEMO_02` DLT ở module demo) |

---

## 3. Broker (Podman / KRaft)

Lab dùng image Kafka KRaft (không ZooKeeper), port **9092**.

Ví dụ (điều chỉnh theo máy bạn nếu container đã chạy):

```bash
podman run -d --name kafka \
  -p 9092:9092 \
  apache/kafka:3.8.1
```

- Broker: `127.0.0.1:9092`
- **Không có UI sẵn** như Rabbit `15672`. Muốn UI: AKHQ / Kafdrop / Kafka UI (tool ngoài), hoặc CLI bên dưới.

`application.yml` (lab):

```yaml
spring:
  kafka:
    bootstrap-servers: 127.0.0.1:9092
```

Profile tham khảo prod: `application-prod.yml` — bật bằng `--spring.profiles.active=prod`.

---

## 4. CLI thường dùng

Binary trong image `apache/kafka` thường ở `/opt/kafka/bin/`. Chạy qua container (đổi tên `kafka` nếu container khác):

```bash
podman exec -it kafka /opt/kafka/bin/<lệnh> ...
# Ví dụ:
podman exec -it kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --list
```

Nếu chạy CLI **trong** container, `--bootstrap-server` có thể là `localhost:9092`. Từ máy host gọi vào port map thì dùng `127.0.0.1:9092`.

### 4.1 Topic

```bash
# Liệt kê topic
kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --list

# Chi tiết 1 topic (partition, ISR, …)
kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --describe --topic DEMO_02

# Tạo topic (lab/prod thường tạo trước)
kafka-topics.sh --bootstrap-server 127.0.0.1:9092 \
  --create --topic DEMO_02 --partitions 3 --replication-factor 1

# Xóa topic (chỉ khi broker cho phép delete.topic.enable)
kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --delete --topic DEMO_02
```

### 4.2 Đọc / ghi nhanh (debug)

```bash
# Gửi tay (mỗi dòng 1 message)
kafka-console-producer.sh --bootstrap-server 127.0.0.1:9092 --topic DEMO_01

# Đọc từ đầu (soi tin còn trên log)
kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 \
  --topic DEMO_02 --from-beginning

# Soi DLT sau retry
kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 \
  --topic DEMO_02.DLT --from-beginning
```

### 4.3 Consumer group

```bash
# Các group đang có
kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --list

# Offset / lag của 1 group (app đọc tới đâu, có bị kẹt không)
kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 \
  --describe --group demo02-consumer-group-DEMO_02

# Reset offset — cẩn thận; đừng reset khi app đang chạy nếu chưa hiểu hệ quả
kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 \
  --group demo02-consumer-group-DEMO_02 --topic DEMO_02 \
  --reset-offsets --to-earliest --execute
```

### 4.4 Nên thuộc lúc học lab

| Việc hay gặp | Lệnh |
|---|---|
| Topic / DLT đã có chưa? | `kafka-topics.sh --list` / `--describe` |
| Message còn trên topic / DLT? | `kafka-console-consumer.sh --from-beginning` |
| App có consume, lag bao nhiêu? | `kafka-consumer-groups.sh --describe --group …` |
| Muốn “sạch” chạy lại demo | xóa topic hoặc reset group offset (hiểu hệ quả) |

Ba nhóm đủ soi lab: **topics** + **console-consumer** + **consumer-groups --describe**. `kafka-configs.sh` (config broker sâu) học khi làm cluster thật.

---

## 5. Dependency (Boot 3.5)

**Không** dùng `spring-boot-starter-kafka` trên Boot **3.5** (starter đó thuộc dòng Boot 4+).

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId> <!-- version từ BOM Boot 3.5 -->
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-json</artifactId>
</dependency>
```

---

## 6. Module demo — `lab-03-kafka-demo` ✅

### 6.1 Cấu hình lỗi consumer (`KafkaConfig`)

```text
@KafkaListener throw
        → DefaultErrorHandler
        → FixedBackOff(10s, maxAttempts=3)  // ~4 lần vào listener / 1 record
        → DeadLetterPublishingRecoverer → <topic>.DLT  (vd. DEMO_02.DLT)
```

| Yudao (2.x) | HDL (3.5 / spring-kafka 3.3) |
|---|---|
| `ErrorHandler` + `SeekToCurrentErrorHandler` | `CommonErrorHandler` + `DefaultErrorHandler` |
| Cùng ý: backoff + DLT recoverer | Giữ `DeadLetterPublishingRecoverer` + `FixedBackOff` |

`FixedBackOff(interval, maxAttempts)`: sau mỗi lần fail, `nextBackOff()` còn cho retry trong hạn `maxAttempts` → thường thấy **~4 lần `onMessage`** rồi mới DLT. Không hiểu thành “chỉ đúng 3 dòng log”.

### 6.2 Demo01 — gửi + hai consumer group

- **Producer:** `syncSend` = `send(...).get()` (block); `asyncSend` = trả `CompletableFuture` (không block).
- **Demo01Consumer / Demo01AConsumer:** cùng topic `DEMO_01`, **`groupId` khác** → mỗi lần gửi cả hai đều nhận.
- **Demo01A:** nhận `ConsumerRecord` để xem metadata; generic nên khớp serializer (`String` key + `Demo01Message` value). Bản yudao `<Integer, String>` **không khớp** config JSON — HDL nên gõ đúng hoặc `<?>`.

### 6.3 Demo02 — fail / retry / DLT (yudao Demo04)

- Consumer **luôn throw** sau khi log → kích hoạt error handler.
- Topic còn tin cũ (offset thấp) + tin mới → log có **hai `id`**: Kafka giữ log theo offset, consumer xử lý tuần tự; không phải một lần `send` tạo hai message.
- Lab `auto-offset-reset: earliest` + topic chưa xóa → dễ “dính” lịch sử khi chạy lại test.

`AtomicInteger count` trên bản yudao Demo04: **dead field** (không dùng) — HDL không cần copy.

### 6.4 YAML lab vs `application-prod.yml`

| | Lab `application.yml` | `application-prod.yml` |
|---|---|---|
| Mục đích | Chạy local, dễ thấy hành vi | Tham khảo best practice |
| `acks` | `1` | `all` + idempotence |
| `auto-offset-reset` | `earliest` | `latest` (group mới) |
| `enable-auto-commit` | (default / không siết) | `false` |
| `missing-topics-fatal` | `false` | `true` |
| Log kafka | `ERROR` (dễ nuốt stack retry) | `INFO`/`WARN` để soi recover |

Retry/DLT **không** thay hết bằng yaml — vẫn cần Java config (+ prod nên ExponentialBackOff, monitor DLT).

---

## 7. Module ack — `lab-03-kafka-demo-ack` ✅

### 7.1 What — lab làm gì, chạy ra hiện tượng gì

**Mục tiêu module:** điều khiển **commit offset** của consumer group (progress “đã xử lý tới đâu”), không liên quan `producer.acks`.

**Cách lab cố ý viết:** mọi tin đều `onMessage` (log), nhưng chỉ gọi `acknowledge()` khi `id` lẻ (`id % 2 == 1`). Id chẵn cố tình không ack.

**Hiện tượng khi chạy test HDL** (gửi lần lượt id=1 rồi id=2, rồi chạy lại nhiều lần):

| Lần chạy | Consumer log | SendResult (offset trên topic) |
|---|---|---|
| 1 | `1` rồi `2` | `@0`, `@1` |
| 2, 5, … | thường chỉ `2 → 1 → 2` (~3 dòng) | offset tăng (`@2/@3` … `@8/@9`) |

Tóm hiện tượng cần giải thích:

- Lần 1: cả hai tin đều vào listener.
- Lần sau: **không** replay cả lịch sử “chưa ack”; topic dài nhưng log consumer vẫn gọn ~3 dòng, thường mở đầu bằng một `id=2`.
- Trong cùng một lần chạy đang treo: tin **không** tự log lại mỗi 10s (khác Demo02 throw + FixedBackOff).

### 7.2 How — cấu hình và code (HDL)

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false  # tắt commit tự động của Kafka client
      # trusted.packages: com.hdl...message
    listener:
      ack-mode: MANUAL           # phải tự gọi Acknowledgment.acknowledge()
```

```java
@KafkaListener(topics = Demo03Message.TOPIC, groupId = "demo03-consumer-group-" + Demo03Message.TOPIC)
public void onMessage(Demo03Message message, Acknowledgment ack) {
    logger.info("...", message);           // luôn log trước
    if (message.getId() % 2 == 1) {
        ack.acknowledge();                 // chỉ id lẻ — bài học, đừng copy sang prod
    }
}
```

```text
enable-auto-commit: false + ack-mode: MANUAL
        │
        ▼
onMessage → log
        ├─ id lẻ  → ack.acknowledge()  → OffsetCommit
        └─ id chẵn → return, không ack
```

Thiếu một trong `enable-auto-commit: false` / `MANUAL` / tham số `Acknowledgment` thì demo mất nghĩa.

**How “ack” đi tới broker:** thread consumer (listener → Spring Kafka → `kafka-clients`) gửi **OffsetCommit** (group + partition + next offset). Không gửi body message, không báo về producer. Broker chỉ lưu **một** committed offset / group / partition — không có list Unacked kiểu Rabbit.

### 7.3 Why — vì sao hiện tượng ở §7.1 xảy ra

Kafka **không** nhớ “tin nào chưa ack”. Chỉ nhớ một con trỏ committed offset cho group.

Spring quy ước khi gọi `acknowledge()` trên một record  
([Acknowledgment API](https://docs.spring.io/spring-kafka/docs/current/api/org/springframework/kafka/support/Acknowledgment.html)):

> Calling this method implies that **all the previous messages in the partition have been processed already**.

Cùng hướng: Kafka không giữ state từng record, chỉ committed offset  
([Manually Committing Offsets](https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/ooo-commits.html)).

Áp vào lab `%2`:

1. Lần 1: xử lý id=1 → ack (commit tiến); xử lý id=2 → không ack.
2. Lần sau: còn khả năng đọc lại id=2 “đứng sau lần ack cuối”.
3. Trong lần chạy đó lại có id=1 mới → **ack** → theo Javadoc, coi mọi tin **trước** offset đó trên partition cũng đã xong → các id=2 cũ hơn bị con trỏ **vượt qua**, group không redelivery cả đống.
4. Kết quả lặp lại: ~`2 → 1 → 2` + offset gửi trên topic vẫn tăng.

Tin vẫn nằm trên topic tới khi hết retention. “Mất” ở đây = group **mất cơ hội xử lý lại**, không phải broker xóa message.

**Vì sao không giống Demo02:** Demo02 `throw` → `DefaultErrorHandler` + `FixedBackOff` gọi lại listener trong cùng process. Module ack **return bình thường**; không bật error-handler retry. Chỉ khác ở có/không OffsetCommit.

### 7.4 When — khi nào dùng / khi nào đừng

| When | Việc nên làm |
|---|---|
| **Lab này** | Cố ý không ack id chẵn rồi ack id lẻ sau — để thấy hợp đồng §7.3 |
| **Prod** | Xử lý nghiệp vụ **thành công** rồi mới `acknowledge()`; fail → không ack (hoặc throw / error handler / DLT tùy thiết kế) |
| **Đừng** | Không ack lung tung rồi ack tin offset cao hơn nếu vẫn cần redelivery tin đứng trước |
| **Soát con trỏ** | Sau test: CLI §4 `kafka-consumer-groups.sh --describe --group demo03-consumer-group-DEMO_03` |

### 7.5 So Rabbit + Boot 3.5

| | Rabbit ack | Kafka ack (module này) |
|---|---|---|
| Object | Message + `deliveryTag` | Offset trên partition |
| API | `channel.basicAck` | `Acknowledgment.acknowledge()` |
| UI Unacked | Có (Management) | Không — CLI group describe |

Boot 3.5: `Acknowledgment` / `MANUAL` giữ được; nhớ `jakarta`, JUnit 5, `spring-kafka` + `starter-json`.

### 7.6 Checklist đã xong

- [x] Module trong parent `pom`
- [x] YAML + consumer `% 2` + test id=1,2
- [x] Ghi What / How / Why / When theo docs (§7.1–7.4)

---

## 8. Module concurrency — `lab-03-kafka-demo-concurrency` ✅

### 8.1 What — lab làm gì, hiện tượng gì

**Mục tiêu:** song song consume bằng `@KafkaListener(concurrency = "2")`, và thấy giới hạn bởi **số partition** + **key**.

| Thành phần | Việc làm |
|---|---|
| Consumer | `concurrency = "2"`, log `Thread ID` |
| `syncSend` | `send(topic, message)` — **không key** |
| `syncSendOrderly` | `send(topic, String.valueOf(id), message)` — **có key** |
| Topic HDL | `DEMO_04` (yudao `DEMO_06`) |

**Hiện tượng đã chạy trên HDL** (topic tạo sẵn **2 partition**, `replication-factor 1`):

| Test / cách gửi | Producer | Consumer |
|---|---|---|
| `syncSend` null key, gửi nhanh | Sticky: 10 tin thường **một** partition | Chỉ **một** thread (`#0-0`) dù concurrency=2 |
| `syncSendOrderly` luôn `id=1` | Mọi dòng `Partition: 1` | Chỉ **một** thread (`#0-1`) — đúng cùng key |
| `syncSendOrderly` `id=0..9` (khác key) | Tin rải P0 và P1 theo hash | **Hai** thread `#0-0` / `#0-1`; order chỉ trong từng partition |

### 8.2 How — cấu hình, tạo topic, cơ chế

```java
@KafkaListener(topics = Demo04Message.TOPIC,
        groupId = "demo04-consumer-group-" + Demo04Message.TOPIC,
        concurrency = "2")
public void onMessage(Demo04Message message) { ... }
```

`concurrency = "2"` → `ConcurrentMessageListenerContainer` tạo 2 consumer con trong **cùng group**  
([Message Listener Containers](https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/message-listener-container.html)).  
Javadoc: *Messages from within the same partition will be processed sequentially.*

**Lab nên tạo topic trước** (auto-create hay ra 1 partition → khó thấy 2 thread):

```bash
podman exec -it kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --create --topic DEMO_04 \
  --partitions 2 --replication-factor 1

# replication-factor 1 = một bản copy (đủ lab 1 broker)
podman exec -it kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe --topic DEMO_04
```

```text
Trong 1 group:  1 partition ↔ 1 consumer (tại một thời điểm)
Nhiều group:    mỗi group tự chia lại toàn bộ partition (fan-out, offset riêng)

concurrency hữu ích ≤ số partition được gán
concurrency > partitions → thread thừa ngồi không
```

### 8.3 Why — giải thích các hiện tượng §8.1

**Null key + gửi liên tiếp chỉ một thread**  
Kafka **2.4+** default **sticky partitioner** khi key = null ([KIP-480](https://cwiki.apache.org/confluence/display/KAFKA/KIP-480:+Sticky+Partitioner)): dính một partition đến khi batch xong rồi mới đổi. 10 tin nhanh → thường một partition → một consumer thread. **Không** phải concurrency hỏng; **không** phải thiếu partition trên topic.

**Cùng key (`id=1`) → một partition → một thread**  
Hash key cố định → order trên partition đó; thread concurrency còn lại không cầm partition đó.

**Khác key (`0..9`) → hai partition → hai thread**  
Tin vào cả P0 và P1; log xen hai `Thread ID`. Thứ tự toàn cục 0..9 **không** đảm bảo; chỉ order **trong** từng partition.

**Topic/partition tồn tại trên broker** sau khi tạo (app restart không xóa). Prod: tạo topic/partition **trước** (IaC), tránh dựa auto-create như lab.

### 8.4 When — khi nào dùng thế nào

| When | Việc nên làm |
|---|---|
| Muốn thấy concurrency lab | Topic ≥ 2 partition + tin vào ≥ 2 partition (khác key hoặc chờ sticky đổi batch) |
| Muốn order theo entity | Cùng Kafka **key** → cùng partition |
| Scale throughput | Tăng partition + concurrency (≤ partitions) và/hoặc thêm instance cùng group |
| Prod | Listener dùng chung nhiều thread → logic **thread-safe** ([Thread Safety](https://docs.spring.io/spring-kafka/reference/kafka/thread-safety.html)) |

### 8.5 So Rabbit concurrency

| | Rabbit | Kafka (module này) |
|---|---|---|
| Đơn vị song song | Message trên queue + nhiều consumer thread | Partition gán trong group |
| Giữ order | Shard queue / concurrency=1 từng shard | Cùng key → cùng partition |

### 8.6 Checklist đã xong

- [x] Module trong parent `pom`; `concurrency = "2"`
- [x] Topic `DEMO_04` 2 partition; mô tả CLI
- [x] Quan sát sticky / orderly cùng key / nhiều key → 2 thread
- [x] Ghi What / How / Why / When (§8.1–8.4)

---

## 9. Module batch — `lab-03-kafka-demo-batch` ✅

### 9.1 What — lab làm gì, hiện tượng gì

**Mục tiêu:** thấy **producer** gom nhiều record vào một batch trước khi gửi broker (`linger.ms` + `batch.size`). **Không** phải batch-consume (`List<Message>` / `batch-listener`) — module đó thuộc overview/skip.

| Thành phần | Việc làm |
|---|---|
| Producer | Chỉ `asyncSend` → `KafkaTemplate.send` trả `CompletableFuture` |
| YAML | `linger.ms: 30000`, `batch-size: 16384` (byte), `buffer-memory` |
| Consumer | `@KafkaListener` một record / lần gọi — như các module trước |
| Topic HDL | `DEMO_05` (yudao batch dùng `DEMO_02` — HDL tránh trùng DLT `DEMO_02` của module demo) |
| Test | 3 lần async, sleep **10s** giữa các lần; `CountDownLatch` treo JVM |

**Hiện tượng đã chạy trên HDL** (`testAsyncSend`, 2026-08-24):

| Thời điểm (log) | Việc |
|---|---|
| `16:05:43` | Bắt đầu; gửi 3 message async (id cách ~10s: `855948` → `855958` → `855968`) |
| `16:06:13` (~**+30s**) | Ba callback success gần như cùng ms trên thread `producer-1` |
| Ngay sau | Consumer nhận 3 tin liên tiếp cùng listener thread |
| Metadata | `DEMO_05-0@0`, `@1`, `@2` — offset liên tiếp cùng partition |

Ba success cùng lúc + offset liên tiếp ≈ flush chung sau cửa sổ linger; consumer vẫn gọi `onMessage` **từng** message.

### 9.2 How — cấu hình + test

```yaml
spring.kafka.producer:
  batch-size: 16384          # byte / partition batch (không phải “số message”)
  buffer-memory: 33554432
  properties:
    linger.ms: 30000         # lab: chờ tối đa 30s để gom batch
```

Boot map `linger.ms` qua `spring.kafka.producer.properties.linger.ms` (YAML lồng `linger.ms` dưới `properties`).

```java
public CompletableFuture<SendResult<String, Object>> asyncSend(Integer id) {
    Demo05Message message = new Demo05Message();
    message.setId(id);
    return kafkaTemplate.send(Demo05Message.TOPIC, message);
}
```

Test (rút gọn): loop 3 × `asyncSend` + `whenComplete` log + `Thread.sleep(10_000)` → `CountDownLatch(1).await()`.

**Vì sao async?** Sync `.get()` sẽ block đến khi batch thật sự gửi (có thể ~linger). Async trả ngay → nhiều message nằm chung buffer trong cửa sổ 30s.

### 9.3 Why — docs producer

Theo [Producer Configs](https://kafka.apache.org/38/generated/producer_config.html) (Kafka **3.8** khớp image lab):

- **`batch.size`:** upper bound kích thước batch **theo byte** cho một partition. Chưa đủ byte → producer có thể **linger**.
- **`linger.ms`:** trì hoãn nhân tạo để gom thêm record; đạt `batch.size` thì gửi ngay dù linger chưa hết; chưa đầy thì chờ tới hết `linger.ms`.

Lab đặt `linger.ms=30000` (lớn hơn prod) để mắt thường thấy delay ~30s rồi nhiều ack cùng lúc. Prod thường linger nhỏ (ms) + `batch.size` phù hợp throughput/latency.

Yudao comment từng viết `batch-size` như “số lượng message” — **sai đơn vị**; HDL note + YAML ghi **byte**.

### 9.4 When — khi nào dùng thế nào

| When | Việc nên làm |
|---|---|
| **Lab này** | Async + sleep < linger → quan sát gom batch |
| Prod cần throughput | Tăng vừa `batch.size` / `linger.ms` (+ compression); chấp nhận thêm latency |
| Prod cần latency thấp | `linger.ms` nhỏ; đừng copy `30000` từ lab |
| Cần batch **consume** | Module yudao `batch-consume` (skip lần đầu) — khác hẳn producer batch |

### 9.5 Checklist đã xong

- [x] Module trong parent `pom`; topic `DEMO_05`
- [x] YAML `linger.ms` / `batch-size`; producer chỉ async; consumer single-record
- [x] Test 3 tin + sleep 10s; log ~+30s rồi 3 success + 3 consume
- [x] Ghi What / How / Why / When (§9.1–9.4)

---

## 10. Lệch API 2.x → 3.5 (checklist nhanh)

| Chỗ | 2.x (yudao) | 3.5 (HDL) |
|---|---|---|
| Injection annotation | `javax.annotation.Resource` | `jakarta.annotation.Resource` |
| `KafkaTemplate.send` | `ListenableFuture` | `CompletableFuture` |
| Async callback | `addCallback(ListenableFutureCallback)` | `whenComplete` / `thenAccept` |
| Error handler | `SeekToCurrentErrorHandler` | `DefaultErrorHandler` |
| Test | JUnit 4 `@RunWith` | JUnit 5 `@Test` + `@SpringBootTest` |
| Starter | tùy pom yudao | `spring-kafka` + `starter-json` (Boot 3.5) |

---

## 11. Kiểm chứng demo (tay)

1. Broker `9092` đang chạy.
2. `Demo01ProducerTest`: gửi → log **hai** consumer (hai group).
3. `Demo02ProducerTest`: cùng `id` lặp ~10s → hết vòng → kiểm tra topic **`DEMO_02.DLT`** (xem §4 CLI hoặc listener riêng). Log level lab đang `ERROR` với `springframework.kafka` → có thể **không** thấy stack; nhìn lặp `onMessage` + DLT.
4. `Demo05ProducerTest` (batch): 3 async + sleep 10s → ~30s sau ba success gần như cùng lúc + consumer nhận sát nhau.
5. Test treo/`sleep` dài là trick demo, không phải assert CI.

---

## 12. Việc còn lại

**Bắt buộc lab-03:** xong (`demo` → `ack` → `concurrency` → `batch`).

- Overview / skip khi cần: `native`, `batch-consume`, `broadcast`, `transaction`.
- Lộ trình tiếp: **Pha 3** — `labx-01` + `labx-03` (Nacos discovery + OpenFeign). Tùy chọn Pha 4: `labx-11` Stream Kafka.

---

## 13. So nhanh với Rabbit lab-04

| | Rabbit | Kafka |
|---|---|---|
| Đường ống | Exchange + Queue + Binding | Topic + Partition |
| Song song | Nhiều consumer trên queue / concurrency | Partition + concurrency ≤ partitions |
| Lỗi sau retry | DLQ (queue) | DLT (topic `*.DLT`) |
| UI local | Management plugin sẵn | Không sẵn — tool ngoài / CLI |
| “Tin còn trên broker” | Unacked/Ready trên queue | Offset trên log (retention) |
| Producer “batch” | Không có cặp `linger`/`batch.size` kiểu Kafka; confirm là module riêng (HDL skip) | Client gom record theo `linger.ms` + `batch.size` (§9) |
