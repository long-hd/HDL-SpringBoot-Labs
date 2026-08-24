package com.hdl.springboot.labs.lab03.kafkademo.producer;

import com.hdl.springboot.labs.lab03.kafkademo.message.Demo01Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class Demo01Producer {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    /**
     * Gửi message đồng bộ: gọi {@code KafkaTemplate#send} rồi {@link CompletableFuture#get()}
     * để block thread hiện tại đến khi broker hoàn tất (thành công hoặc lỗi).
     *
     * @param id định danh đưa vào body {@link Demo01Message}
     * @return kết quả gửi (topic, partition, offset, …) sau khi future hoàn thành
     * @throws ExecutionException   nếu gửi thất bại; nguyên nhân gốc nằm trong {@link ExecutionException#getCause()}
     * @throws InterruptedException nếu thread bị interrupt trong lúc chờ {@code get()}
     */
    public SendResult<Object, Object> syncSend(Integer id) throws ExecutionException, InterruptedException {
        Demo01Message message = new Demo01Message();
        message.setId(id);

        return kafkaTemplate.send(Demo01Message.TOPIC, message).get();
    }

    /**
     * Gửi message bất đồng bộ: trả {@link CompletableFuture} ngay, không block caller.
     * Caller tự gắn callback ({@code whenComplete}, {@code thenAccept}, …) hoặc {@code get()}/{@code join()}
     * nếu cần chờ kết quả sau.
     *
     * @param id định danh đưa vào body {@link Demo01Message}
     * @return future hoàn thành khi gửi thành công ({@link SendResult}) hoặc exceptionally nếu lỗi
     */
    public CompletableFuture<SendResult<Object, Object>> asyncSend(Integer id) {
        Demo01Message message = new Demo01Message();
        message.setId(id);

        return kafkaTemplate.send(Demo01Message.TOPIC, message);
    }

}
