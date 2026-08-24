# Spring RabbitMQ — Ghi chú học tập (lab-04)

> Tổng hợp từ `lab-04-rabbitmq`.  
> Yudao source: Boot **2.2** + JDK `Serializable`. HDL: Boot **3.5** + **JSON** (`Jackson2JsonMessageConverter`).  
> Pha 2 — Message (Rabbit trước Kafka).

---

## 1. Lab đứng đâu?

```text
@Async (Pha 1)     = nhiều thread trong 1 JVM
RabbitMQ (lab-04)  = đẩy việc qua broker; app khác / restart vẫn còn (durable)
Redis Pub/Sub      = tín hiệu realtime, không thay queue có ack/retry
```

```text
Producer  →  Exchange  →  Binding  →  Queue  →  @RabbitListener
                ↑
           RabbitMQ broker
```

---

## 2. Kế hoạch submodule HDL


| #   | Module                             | Trạng thái | Học gì                                       |
| --- | ---------------------------------- | ---------- | -------------------------------------------- |
| 1   | `lab-04-rabbitmq-demo`             | ✅          | Direct / Topic / Fanout / Headers            |
| 2   | `lab-04-rabbitmq-ack`              | ✅          | Manual ack (`deliveryTag`, Unacked vs Ready) |
| 3   | `lab-04-rabbitmq-consume-retry`    | ✅          | Listener retry + DLQ                         |
| 4   | `lab-04-rabbitmq-demo-concurrency` | ✅          | Nhiều consumer thread / `concurrency`        |
| 5   | `lab-04-rabbitmq-demo-orderly`     | ✅          | Shard queue (`id % N`), order theo entity    |


Overview / skip lần đầu: native, json (yudao tách riêng — HDL đã JSON trong demo), batch, delay, confirm, rpc, transaction, error-handler, message-model.

---



## 3. Broker (Podman)

```bash
podman run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

- AMQP: `5672` — UI: [http://localhost:15672](http://localhost:15672) (`guest` / `guest`)
- App declare Queue/Exchange/Binding khi start (bean trong `RabbitConfig`)

`application.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

---



## 4. Module demo — topology (`RabbitConfig`)

Config **không** gửi/nhận message. Chỉ khai báo đường ống trên broker.


| Demo | Exchange | Binding rule                            |
| ---- | -------- | --------------------------------------- |
| 01   | Direct   | Routing key exact                       |
| 02   | Topic    | Pattern `#.yu.nai`                      |
| 03   | Fanout   | Broadcast → queue A + B (không cần key) |
| 04   | Headers  | Header `color` matches `red`            |


Queue thường: `durable=true`, `exclusive=false`, `autoDelete=false`.

---



## 5. Bốn exchange — kết quả đã chứng minh



### Demo01 — Direct

- `syncSend(exchange, routingKey, msg)` → qua Direct + binding.
- `syncSendDefault(queue, msg)` → **default exchange** `""`, routing key = tên queue (không qua `EXCHANGE_DEMO_01`).
- `asyncSend` = `@Async` + `CompletableFuture` bọc `syncSend` (Pha 1), không phải async của broker.
- Consumer: `@RabbitListener(queues=…)` trên class + `@RabbitHandler` theo kiểu payload.
- Test: producer trên `main`, consumer trên `ntContainer#…` (thread khác).



### Demo02 — Topic

Wildcard: `*` = đúng 1 word; `#` = 0..n word (word tách bởi `.`).


| Publish key    | Match `#.yu.nai`?                               |
| -------------- | ----------------------------------------------- |
| `da.yu.nai`    | Có → consumer log                               |
| `yu.nai.shuai` | Không → không vào queue (producer vẫn “gửi OK”) |




### Demo03 — Fanout

Một publish → **cả** Consumer A và B log cùng `id` (hai listener container khác nhau).

`convertAndSend(exchange, null, message)` — phải gửi **object message**, không gửi nhầm `id` (`Integer`).

### Demo04 — Headers

Binding: `.where("color").matches("red")`.

Producer phải gắn header vào **AMQP Message**:

```java
MessageProperties props = new MessageProperties();
props.setHeader(Demo04Message.HEADER_KEY, headerValue);
Message message = rabbitTemplate.getMessageConverter()
        .toMessage(new Demo04Message().setId(id), props);
rabbitTemplate.send(Demo04Message.EXCHANGE, null, message);
```

Chỉ `convertAndSend(exchange, null, body)` → **mất header** → không vào queue.

- Success (`red`) → consumer log  
- Fail (`error`) → không log consumer

---



## 6. JSON trên Boot 3.5 (lệch yudao — bắt buộc nhớ)

Yudao dùng JDK `Serializable` + `SimpleMessageConverter`.  
Spring AMQP **3.x** chặn deserialize class lạ → `SecurityException` + bắt allowlist (hoặc `TRUST_ALL` — đừng dùng).

**HDL chọn JSON** (sát production, không allowlist):

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-json</artifactId>
</dependency>
```

```java
@Bean
public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
}
```

Đổi converter / format: **purge queue** cũ (message JDK còn lại sẽ fail convert).

---



## 7. Boot 3 khác yudao (demo)


| Yudao (Boot 2)                          | HDL (Boot 3.5)                                     |
| --------------------------------------- | -------------------------------------------------- |
| `AsyncResult` / `ListenableFuture`      | `CompletableFuture` + `whenComplete`               |
| JUnit 4 `SpringRunner`                  | JUnit 5 `@SpringBootTest`                          |
| JDK serialize                           | `Jackson2JsonMessageConverter`                     |
| `CountDownLatch(1).await()` treo vô hạn | `Thread.sleep(…)` hoặc `await(timeout)` đủ xem log |


API giữ nguyên: `RabbitTemplate`, `Queue` / `*Exchange` / `BindingBuilder`, `@RabbitListener`, `@RabbitHandler`.

---



## 8. Test — lưu ý

- `syncSend` return sớm; consumer chạy thread container → cần chờ ngắn để thấy log.
- Topic/Headers “fail”: **không** có `onMessage` là đúng (không match binding).
- Đừng gửi nhầm kiểu (`Integer` thay `Demo03Message`) → `No listener method found for class java.lang.Integer`.

---



## 9. Module ack — manual acknowledge



### Ack là gì?

Consumer báo broker: message **đã xử lý xong** → broker có thể bỏ khỏi queue.  
Không ack / consumer disconnect trước khi ack → message có thể **giao lại** (redeliver).


| Mode                  | Hành vi                                                     |
| --------------------- | ----------------------------------------------------------- |
| `AUTO` (demo trước)   | Method OK → Spring ack; exception → nack/requeue tùy config |
| `MANUAL` (module này) | Tự `channel.basicAck` / `basicNack` / `basicReject`         |


YAML:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
```



### `deliveryTag`

Id lần **giao message** trên channel hiện tại (không phải `id` nghiệp vụ trong payload).

```java
@RabbitHandler
public void onMessage(AckMessage message, Channel channel,
                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
    // ...
    channel.basicAck(deliveryTag, false); // false = chỉ ack đúng tag này
}
```

`multiple=true`: ack luôn các delivery chưa ack có tag ≤ giá trị đó (batch).

### Demo yudao / HDL (cố tình)

Gửi `id=1` và `id=2`; cả hai đều **log** `onMessage`.


| `id`       | Ack?          | Ý nghĩa                                             |
| ---------- | ------------- | --------------------------------------------------- |
| lẻ (`1`)   | `basicAck`    | Broker xóa message                                  |
| chẵn (`2`) | **Không** ack | Giữ / giao lại — **bài học**, không copy production |


Production: xử lý xong → luôn ack; lỗi → nack/DLQ; handler **idempotent**.  
Mode prod: **AUTO** đủ nhiều case; **MANUAL** khi cần điểm ack rõ (sau commit DB, batch…).

### UI Management — Ready vs Unacked


| Cột         | Nghĩa                                              |
| ----------- | -------------------------------------------------- |
| **Unacked** | Consumer **còn kết nối** đang giữ message chưa ack |
| **Ready**   | Message nằm trong queue, chờ giao                  |


```text
id=2 không ack + consumer còn sống (sleep dài)  →  Unacked ≥ 1
Tắt test / channel đóng                         →  Unacked = 0, message về Ready
```

Unacked **không** treo mãi = 1 sau khi tắt app. Muốn thấy Unacked: mở UI **trong lúc** test còn `sleep`.

Mỗi lần chạy lại mà không ack chẵn → message chẵn **cộng dồn** → lần sau consumer nhận lại nhiều `id=2`.  
Dọn: UI Purge / Delete, hoặc:

```bash
podman exec rabbitmq rabbitmqctl purge_queue QUEUE_DEMO_05
```

(Tên queue theo hằng trong message class của bạn.)

### Naming HDL

Số yudao (`Demo12*`) = số bài trong series, **không** phải thứ tự `pom` / lộ trình.  
HDL nên đặt theo concept: `AckMessage`, `AckProducer`, `AckConsumer`, queue `QUEUE_ACK` / `QUEUE_DEMO_05`…

Thứ tự `<modules>` trong `pom` yudao = danh sách build, **không** phải syllabus.

---



## 10. Module consume-retry — listener retry + DLQ



### Luồng đã chứng minh

```text
Publish → Queue chính → Consumer (throw)
              ↓
         listener.retry (max-attempts lần, trong app)
              ↓ hết lần
         RejectAndDontRequeue → DLX + dead routing key → Dead queue
              ↓
         DeadConsumer log
```

Đếm log: **N dòng INFO** `onMessage` trên consumer chính (`max-attempts=N`), rồi WARN exhausted, rồi DeadConsumer — **không** đếm số khối WARN stack.

### `template.retry` vs `listener.retry`


|                 | `template.retry`            | `listener.simple.retry`      |
| --------------- | --------------------------- | ---------------------------- |
| Ai              | Producer (`RabbitTemplate`) | Consumer (`@RabbitListener`) |
| Khi             | Gửi broker lỗi              | Handler throw                |
| Broker “retry”? | Không                       | Không — retry trong JVM      |


Lab này trọng tâm **listener.retry** + DLQ. `template.retry` là phụ.

Broker không có `max-attempts` Spring; chỉ ack/nack/requeue/DLX.

### Queue chính + DLX

```java
QueueBuilder.durable(QUEUE)
        .deadLetterExchange(EXCHANGE)
        .deadLetterRoutingKey(DEAD_ROUTING_KEY)
        .build();
```

- **Giữ:** `durable` + DLX/DLK.  
- **Bỏ (HDL):** `.exclusive()` / `.autoDelete()` của yudao — queue tạm, khó quan sát; không giống prod.  
- Dead queue: `Queue` riêng + binding dead key; **phải có** `@Bean` (thiếu → `404 NOT_FOUND` dead queue).  
- Đổi queue args (thêm DLX): thường phải **xóa queue cũ** rồi declare lại.



### Consumer

- Chính: cố tình `throw` → trigger retry.  
- Dead: listen dead queue, chỉ log.  
- Ack mode: **AUTO** (exception → Spring retry rồi reject).



### Queue chính vs dead queue

**Hai queue riêng** trên broker (không phải một queue hai ngăn).


|             | Vai trò                                           |
| ----------- | ------------------------------------------------- |
| Queue chính | Message bình thường; consumer chính listen        |
| Dead queue  | Message sau reject/hết retry; DeadConsumer listen |


Cùng exchange (lab này) chỉ khác **routing key** / binding. **DLX** = cấu hình trên queue chính: “khi chết thì publish sang exchange X với key Y” — DLX **không** phải kho lưu riêng.

### Message lưu ở đâu? Có mãi không?

- Logic: message nằm trong **queue** (Ready / Unacked).  
- Durable queue + persistent message → broker ghi đĩa (vẫn cache RAM); restart có thể còn. Transient → dễ mất khi restart.  
- **Không** lưu mãi: biến mất khi ack, TTL/policy, purge/delete queue, hoặc queue auto-delete/exclusive. Dead queue cũng vậy.

---



## 11. Checklist



### Demo

- [ ] Phân biệt Direct / Topic / Fanout / Headers  
- [ ] Default exchange vs Direct exchange tự declare  
- [ ] Topic `*` / `#`  
- [ ] Fanout = copy mọi queue gắn exchange  
- [ ] Headers: header phải nằm trên AMQP message  
- [ ] Vì sao Boot 3 dùng JSON thay JDK serialize  
- [ ] Producer thread ≠ consumer thread  



### Ack

- [ ] `acknowledge-mode: manual` + `basicAck(deliveryTag, false)`  
- [ ] `deliveryTag` ≠ business `id`  
- [ ] Unacked chỉ khi consumer còn giữ; tắt test → về Ready  
- [ ] Message không ack có thể cộng dồn / redeliver  



### Consume-retry

- [ ] Phân biệt `template.retry` vs `listener.retry`  
- [ ] `max-attempts` = tổng số lần gọi handler  
- [ ] DLX + dead routing key trên queue chính; dead queue là `@Bean`  
- [ ] Hết retry → dead consumer nhận message  

---



## 12. Module concurrency



### Mục tiêu

Một queue, nhiều consumer thread → throughput ↑; **thứ tự toàn cục không còn đảm bảo**.

```text
Queue ──┬── ntContainer#0-1 (thread A)
        └── ntContainer#0-2 (thread B)
```

Khác `@Async`: broker phân phối message cho nhiều consumer của queue.

### Cấu hình

```yaml
spring:
  rabbitmq:
    listener:
      type: simple
      simple:
        concurrency: 2
        max-concurrency: 10
```


| Property          | Ý nghĩa                                                   |
| ----------------- | --------------------------------------------------------- |
| `type: simple`    | `SimpleMessageListenerContainer` (scale bằng concurrency) |
| `type: direct`    | Container khác; scale bằng `consumers-per-queue`          |
| `concurrency`     | Số consumer/thread ban đầu                                |
| `max-concurrency` | Trần khi scale                                            |


`@RabbitListener(concurrency = "2")` gắn theo listener (có thể ghi đè yaml). Comment yudao: listener không concurrency / listen nhiều queue — chỉ ví dụ API.

**Listener** = `@RabbitListener` / handler của bạn.  
**ListenerContainer** = Spring lấy message, quản lý thread/ack/retry, gọi listener.

Prod: **simple** phổ biến; direct khi có lý do cụ thể — không phải simple=học / direct=prod.

### Kết quả đã thấy

Hai thread id xen kẽ (`#0-1` / `#0-2`).  
Test dùng `currentTimeMillis()/1000` trong vòng lặp nhanh → **cùng id** nhiều message; muốn phân biệt thì dùng `i` / nanoTime.

Handler phải thread-safe / idempotent nếu có shared state.

---



## 13. Checklist (bổ sung concurrency)

- [ ] `concurrency` → nhiều `ntContainer` / thread id  
- [ ] Phân biệt listener vs ListenerContainer; simple vs direct  
- [ ] Concurrency ↑ ↔ mất thứ tự toàn cục trên 1 queue  

---



## 14. Module orderly — thứ tự theo shard



### Tình huống nghiệp vụ (vì sao cần)

“Thứ tự” ở đây = thứ tự xử lý **event của cùng một đối tượng** (cùng đơn / user / account), không phải thứ tự mọi message trên toàn hệ thống.

Ví dụ cùng đơn hàng A: `tạo → thanh toán → giao` phải đúng chuỗi. Làm thanh toán trước khi tạo xong → sai.

**Concurrency (1 queue, nhiều thread)** tăng throughput nhưng **phá** thứ tự trong cùng entity: hai thread có thể xử lý lệch bước của cùng đơn A.

**Orderly** = vẫn muốn song song toàn hệ, nhưng **mọi event cùng key** phải tuần tự.

### Ý tưởng (không phải “chia dư cho vui”)

Rabbit không có partition sẵn như Kafka. Cách phổ biến:

```text
shard = hash(key) % N          // lab: id % 4
publish(exchange, routingKey = shard, message)
binding: queue-i ← key "i"
mỗi queue ≈ 1 consumer (concurrency = 1 trên queue đó)
```


| Phạm vi                    | Hành vi                               |
| -------------------------- | ------------------------------------- |
| Cùng shard (cùng `id % N`) | Cùng queue → xử lý **tuần tự** (FIFO) |
| Khác shard                 | Khác queue → **song song**            |




### Khác “xử lý tuần tự một hàng duy nhất”

```text
Tuần tự toàn cục:  A1→A2→B1→B2→C1…   (một đường, chậm)

Orderly / shard:   Queue-0: A1→A2→A3
                   Queue-1: B1→B2      ⎫ song song giữa các entity
                   Queue-2: C1→C2→C3   ⎭
```

Tuần tự **có chọn lọc** (theo entity), không tuần tự cả thế giới.

### So với concurrency


|                    | Concurrency                 | Orderly                      |
| ------------------ | --------------------------- | ---------------------------- |
| Queue              | 1                           | N shard                      |
| Parallel           | Nhiều thread **cùng** queue | Song song **giữa** queue     |
| Order trong entity | Dễ mất                      | Giữ (nếu 1 consumer / shard) |


Gắn `concurrency > 1` lên **một** orderly queue → lại phá thứ tự trong shard.

### Lab HDL / yudao (`N = 4`)


| Phần     | Việc                                                                                 |
| -------- | ------------------------------------------------------------------------------------ |
| Config   | 4 queue (`QUEUE_DEMO_08-0`…`-3` hoặc yudao `10`); Direct; binding `"0"`…`"3"`        |
| Producer | `routingKey = String.valueOf(id % QUEUE_COUNT)`                                      |
| Consumer | 4 `@RabbitListener` (mỗi queue một listener / container)                             |
| Handler  | `@RabbitHandler(isDefault = true)` + `Message<T>` để đọc header `amqp_consumerQueue` |
| YAML     | Không bật concurrency cao trên các queue này                                         |


`isDefault = true`: handler dự phòng khi không match typed handler khác; lab dùng để nhận `Message<...>` (payload + headers).

### Kết quả test đã thấy

- `id=0` → queue `-0` / container `#0`; `1`→`-1`; `2`→`-2`; `3`→`-3`.  
- Hai vòng gửi → mỗi id hai lần, **cùng** queue + thread của shard.  
- Timestamp trùng giữa shard khác nhau → song song đa queue.



### Prod: `% N` có phải “chỉ demo”?

**Lõi = hash/modulo vào N queue** — lab không sai ý. Prod cùng family, thêm vận hành:


| Lab                   | Prod thường thêm                                            |
| --------------------- | ----------------------------------------------------------- |
| `id % 4`              | Key thật (`orderId`); N theo tải                            |
| Hardcode 4 queue      | Naming, automation declare                                  |
| 1 process, 4 listener | Topology nhiều instance; 1 queue ↔ consumer tuần tự         |
| Đổi N sửa code        | Re-shard đau — cần kế hoạch                                 |
| —                     | DLQ/monitor theo shard; hot key; đôi khi consistent hashing |


Cách khác cùng mục tiêu order-theo-key:

- **Kafka / Pulsar partition** (native theo key)  
- Một queue + `concurrency=1` (order toàn cục, chậm)  
- Consume song song + **lock theo entity** (Redis)  
- Outbox / single-thread per aggregate



### Rabbit vs Kafka (bài này)

Orderly trên Rabbit = **mô phỏng partition bằng tay**.  
Kafka: cùng key → cùng partition là mô hình mặc định.  
Không có nghĩa Rabbit thừa — routing linh hoạt, work-queue, pattern khác Rabbit vẫn mạnh; Kafka native hóa đúng bài order-by-key.

---



## 15. Checklist (bổ sung orderly)

- [ ] “Thứ tự” = theo entity/key, không phải mọi message toàn cục  
- [ ] `hash % N` → cùng key cùng queue; 1 consumer/shard giữ FIFO  
- [ ] Khác tuần tự một hàng: song song giữa shard  
- [ ] `concurrency > 1` trên shard phá order trong shard  
- [ ] Biết `% N` là lõi; prod thêm ops / hoặc chuyển Kafka partition  

---



## 16. Lab-04 bắt buộc — đã xong

```text
demo → ack → consume-retry → concurrency → orderly   ✅
```

**Tiếp theo (Pha 2):** `lab-03` Kafka (`demo` → `ack` → `concurrency` → `batch`).

Overview / skip còn lại của Rabbit: native, batch, delay, confirm, rpc, transaction, error-handler, message-model (đọc khi cần).