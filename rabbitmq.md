Producer -> Server -> Virtual host -> Exchange -> Queue -> Consumer
Server/Broker
Message: 由 Properties 与 Body 组成
Virtual host: 虚拟地址，逻辑隔离，一个 Virtual host 里面可以有多个 Exchange 和 Queue
Exchange: 交换机，接收消息，根据路由键转发消息到绑定的队列，可以绑定多个队列
Binding: Exchange 与 Queue 之间的虚拟连接，binding 中可以包含 routing key
Routing key: 路由规则

```sh
# 启动
rabbitmq-server start &

# 停止
rabbitmqctl stop_app

# 启动
rabbitmqctl start_app

# 集群状态
rabbitmqctl status

# 管理插件
rabbitmq-plugins enable rabbitmq_management
```
```sh
lsof -i:5672 
```
```sh
# 添加用户
rabbitmqctl add_user username password

# 列出所有用户
rabbitmqctl list_users

# 删除用户
rabbitmqctl delete_user username

# 清除用户权限
rabbitmqctl clear_permissions -p vhostpath username

# 列出用户权限
rabbitmqctl list_user_permissions username

# 修改密码
rabbitmqctl change_password username newpassword

# 设置用户权限
rabbitmqctl set_permissions -p vhostpath username ".*"".*"".*"
```
```sh
# 创建虚拟主机
rabbitmqctl add_vhost vhostpath

# 列出所有虚拟主机
rabbitmqctl list_vhosts

# 列出虚拟机上所有权限
rabbitmqctl list_permissions -p vhostpath

# 删除虚拟主机
rabbitmqctl delete_vhost vhostpath
```
```sh
# 查看所有队列信息
rabbitmqctl list_queues

# 查看交换机
rabbitmqctl list_exchanges

# 清除队列里的信息
rabbitmqctl -p vhostpath purge_queue blue
```
```sh
# 移出所有数据，在 stop_app 之后使用
rabbitmqctl reset

# 组成集群
rabbitmqctl join_cluster <clusternode> [--ram]

# 查看集群状态
rabbitmqctl cluster_status

# 修改集群节点的存储形式
rabbitmqctl change_cluster_node_type disc|ram

# 忘记节点
rabbitmqctl forget_cluster_node [--offline]

# 修改节点名称
rabbitmqctl rename_cluster_node oldnode1 newnode1 oldnode2 newnode2
```


消息可靠性投递
1. 消息成功发出
2. MQ 节点成功接收
3. 发送端收到 MQ 确认应答
4. 消息补偿机制

生产端解决消息可靠性投递
1. 消息落库，对消息状态进行打标
step1: 业务数据入库（业务表），消息入库（消息表）
step2: 发送消息
step3: 消息监听器进行消息确认
step4: 更新消息状态（消息表）
step5: 分布式定时任务，抓取确认超时消息（消息表），重新投递
step6: 重试投递达到最大次数，跟新消息状态为失败状态（消息表）

高并发场景下，并不合适

2. 消息的延迟投递，做二次确认，回调检查
step1: 业务数据入库
step2: 第一次消息发送，发送到下游消费服务
step3: 第二次消息发送，发送延迟消息检查到 callback 服务
step4: 消费端消费消息
step5: 消费端发送确认消息
step6: callback 服务接收消费端的确认消息，更新消息表
step7: callback 服务接收到延迟消息，从数据库中检查之前的消息是否消费成功
step8: 延迟检查失败，发送 ReSend 命令给发送端，告知之前发送的消息发送失败

消费端幂等性
1. 唯一 id + 指纹码机制，利用数据库主键去重（根据 id 分库分表进行路由算法）
2. 利用 redis 原子性

消息变为死信有以下情况
1. 消息被拒绝，且 requeue 参数为 false
2. 消息 ttl 过期
3. 队列达到最大长度


```
amqp-client
spring-boot-starter-amqp
```

```
@Configuration
@ComponentScan({""})
public class RabbitMQConfig {
    @Bean
    public ConnectionFactory connectionFactory() {
        CacheingConnectionFactory f = new CachingConnectionFactory();
        f.setAddresses("192.168.1.1:5672");
        f.setUsername("");
        f.setPassword("");
        f.setVirtualHost("/");
        return f;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

        // 必须设为 true，否则 spring 不会加载 RabbitAdmin
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }
}
```

rabbitAdmin.delareExchange(new DirectExchange("name", false, false));
rabbitAdmin.delareBinding()

```
@Bean
public TopicExchange exchange() {
    return TopicExchange("name", true, false);
}

@Bean
public Queue queue() {
    return new Queue("name", true);
}

@Bean
public Binding binding() {
    return BindingBuilder.bind(queue()).to(exchange()).with("routingkey");
}

@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    return template;
}
```
```java
// spring
MessageProperties properties = new MessageProperties();
properties.getHeaders.put("desc", "");
properties.setContentType("text/plain");
Message message = new Message("".getBytes(), properties);

template.convertAndSend("exchange", "routingkey", message, new MessagePostProcessor() {
    public Message postProcessMessage(Message message) {
        properties.getHeaders.put("desc", "");
        return message;
    }
});
```

SimpleMessageListenerContainer
简单消息监听容器
```java
@Bean
public SimpleMessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);

    container.setQueues(queue(), queue1());
    container.setConcurrentConsumers(1);
    container.setMaxConcurrentConsumers(5);
    container.setDefaultRequeueRejected(false);
    container.setAcknowledgeMode(AcknowledegMode.AUTO);
    container.setConsumerTagStrategy(new ConsumerTagStrategy() {
        public String createConsumerTag(String queue) {
            return queue + "/" + UUID.randomUUID().toString();
        }
    });

    container.setMessageListener(new ChannelAwareMessageListener() {
        public void onMessage(Message message, Channel channel) {
            String msg = new String(message.getBody());
            System.out.println(msg);
        }
    });
    return container;
}
```
```java
public class MessageDelegate {
    public void handleMessage(byte[] msgBody) {}

    public void consumeMessage(byte[] msgBody) {}

    public void consumeMessage(String msgBody) {}
}
```
```java
public class TextMessageConverter implements MessageConverter {
    public Message toMessage(Object object, MessageProperties messageProperties) {
        return new Message(object.toString().getBytes(), messageProperties);
    }

    public Object fromMessage(Message message) {
        String contentType = message.getMessageProperties().getContentType();

        if (null != contentType && contentType.contains("text")) {
            return new String(message.getBody());
        }

        return message.getBody();
    }
}
```
```java
MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
adapter.setDefaultListenerMethod("consumeMessage");
adapter.setMessageConverter(new TextMessageConverter());

Map map = new HashMap();
map.put("queue", "consumeMessage");
map.put("queue", "handleMessage");
adapter.setQueueOrTagToMethodName(map);
container.setMessageListener(adapter);
```

Jackon2JsonMessageConverter

ConfirmCallback
ReturnCallback

MessageListenerAdapter




