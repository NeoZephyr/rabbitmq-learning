package com.pain.rabbitmq.limit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Consumer {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/");

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "test_qos_exchange";
        String queueName = "test_qos_queue";
        String routingKey = "test.qos.*";

        channel.exchangeDeclare(exchangeName, "topic", true, false, false, null);
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, exchangeName, routingKey);

        // 在非自动确认消息的前提下，如果一定数目的消息未被确认，不进行消费新的消息
        // prefetchSize 为 0 表示单条消息大小不限制
        // prefetchCount 为 1 表示一次最多接收 1 条，一旦有 1 条消息没有 ack，则将 consumer block 直到有消息 ack
        // global 为 false 表示应用到 consumer 级别，为 true 则表示应用到 channel 级别
        channel.basicQos(0, 1, false);
        channel.basicConsume(queueName, false, new AckConsumer(channel));

    }
}
