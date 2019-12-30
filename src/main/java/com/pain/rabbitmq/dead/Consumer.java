package com.pain.rabbitmq.dead;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Consumer {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/");

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "test_dlx_exchange";
        String queueName = "test_dlx_queue";
        String routingKey = "test.dlx.*";

        String dlxExchangeName = "dlx_exchange";
        String dlxQueueName = "dlx_queue";

        Map arguments = new HashMap();
        arguments.put("x-dead-letter-exchange", dlxExchangeName);

        channel.exchangeDeclare(exchangeName, "topic", true, false, null);

        // 当消息在一个队列中变为死信之后，就会被重新 publish 到另一个 exchange
        channel.queueDeclare(queueName, true, false, false, arguments);
        channel.queueBind(queueName, exchangeName, routingKey);

        channel.exchangeDeclare(dlxExchangeName, "topic", true, false, false, null);
        channel.queueDeclare(dlxQueueName, true, false, false, null);
        channel.queueBind(dlxQueueName, dlxExchangeName, "#");

        channel.basicConsume(queueName, true, new AckConsumer(channel));

    }
}
