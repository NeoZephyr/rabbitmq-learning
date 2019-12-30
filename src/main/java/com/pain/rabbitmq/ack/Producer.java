package com.pain.rabbitmq.ack;

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

        String exchangeName = "test_ack_exchange";
        String routingKey = "test.ack.save";

        String templateMsg = "Hello ack consumer, %d";

        for (int i = 0; i < 5; ++i) {
            Map header = new HashMap();
            header.put("num", i);

            AMQP.BasicProperties headers = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .contentEncoding("UTF-8")

                    // 设置消息过期时间为 10s
                    .expiration("10000")
                    .headers(header)
                    .build();
            channel.basicPublish(exchangeName, routingKey, headers, String.format(templateMsg, i).getBytes());
        }
    }
}
