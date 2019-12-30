package com.pain.rabbitmq.basic;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Producer {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/");

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        Map headers = new HashMap();
        headers.put("type", "rabbitmq");

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .deliveryMode(2).contentEncoding("UTF-8").expiration("10000").headers(headers).build();

        for (int i = 0; i < 5; ++i) {
            String msg = "hello rabbitMQ consumer, %d";

            // 如果指定的 exchange 为空，则发往 default exchange，default exchange 选择与 routingKey 匹配的 queue
            channel.basicPublish("", "test_queue", properties, String.format(msg, i).getBytes());
        }

        channel.close();
        connection.close();
    }
}
