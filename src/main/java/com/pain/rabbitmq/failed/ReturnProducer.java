package com.pain.rabbitmq.failed;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ReturnProducer {
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/");

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "test_return_exchange";
        String routingKey = "return.test";
        String errorRoutingKey = "error.test";
        String msg = "Hello return exchange";

        channel.addReturnListener(new ReturnListener() {
            @Override
            public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("================");
                System.out.println("replyCode: " + replyCode);
                System.out.println("replyText:" + replyText);
                System.out.println("exchange: " + exchange);
                System.out.println("routingKey: " + routingKey);
                System.out.println("properties: " + properties);
                System.out.println("msg: " + new String(body));
            }
        });

        // mandatory 为 true 表示监听器接收路由不可达消息；反之表示 broker 端自动删除不可达消息
        channel.basicPublish(exchangeName, errorRoutingKey, true, null, msg.getBytes());
    }
}
