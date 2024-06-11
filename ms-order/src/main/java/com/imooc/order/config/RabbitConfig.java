package com.imooc.order.config;

import com.imooc.commons.constant.MQExchangeConstant;
import com.imooc.commons.constant.MQQueueConstant;
import com.imooc.commons.constant.MQRoutingKeyConstant;
import com.imooc.order.service.OrderMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.port}")
    private Integer port;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;
    @Autowired
    private OrderMessageService orderMessageService;

    /*---------------------restaurant---------------------*/
    /*@Bean
    public Exchange exchange1() {
        return new DirectExchange(MQExchangeConstant.ORDER_RESTAURANT.getExchange());
    }

    @Bean
    public Queue queue1() {
        return new Queue(MQQueueConstant.ORDER.getQueue());
    }

    @Bean
    public Binding binding1() {
        return new Binding(
                MQQueueConstant.ORDER.getQueue(),
                Binding.DestinationType.QUEUE,
                MQExchangeConstant.ORDER_RESTAURANT.getExchange(),
                MQRoutingKeyConstant.ORDER_KEY.getKey(),
                null);
    }*/

    /*---------------------deliveryman---------------------*/
   /* @Bean
    public Exchange exchange2() {
        return new DirectExchange(MQExchangeConstant.ORDER_DELIVERYMAN.getExchange());
    }

    @Bean
    public Binding binding2() {
        return new Binding(
                MQQueueConstant.ORDER.getQueue(),
                Binding.DestinationType.QUEUE,
                MQExchangeConstant.ORDER_DELIVERYMAN.getExchange(),
                MQRoutingKeyConstant.ORDER_KEY.getKey(),
                null);
    }*/


    /*---------settlement---------*/
    /*@Bean
    public Exchange exchange3() {
        return new FanoutExchange(MQExchangeConstant.ORDER_SETTLEMENT.getExchange());
    }

    @Bean
    public Exchange exchange4() {
        return new FanoutExchange("exchange.settlement.order");
    }

    @Bean
    public Binding binding3() {
        return new Binding(
                MQQueueConstant.ORDER.getQueue(),
                Binding.DestinationType.QUEUE,
                MQExchangeConstant.ORDER_SETTLEMENT.getExchange(),
                MQRoutingKeyConstant.ORDER_KEY.getKey(),
                null);
    }*/

    /*--------------reward----------------*/
    /*@Bean
    public Exchange exchange5() {
        return new TopicExchange("exchange.order.reward");
    }

    @Bean
    public Binding binding4() {
        return new Binding(
                "queue.order",
                Binding.DestinationType.QUEUE,
                "exchange.order.reward",
                "key.order",
                null);
    }*/

   /* @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        //开启发送端确认机制
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        //开启消息返回机制
        connectionFactory.setPublisherReturns(true);
        connectionFactory.createConnection();
        return connectionFactory;
    }*/
/*
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory ) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) ->
                log.info("message:{}, replyCode:{}, replyText:{}, exchange:{}, routingKey:{}"
                , message, replyCode, replyText, exchange, routingKey));
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) ->
                log.info("correlationData:{}, ack:{}, cause:{}", correlationData, ack, cause));
        return rabbitTemplate;
    }

    @Bean
    public SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer simpleMessageListenerContainer =
                new SimpleMessageListenerContainer(connectionFactory);
        //消费端确认
        simpleMessageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);//手动确认
       *//* simpleMessageListenerContainer.setMessageListener(new ChannelAwareMessageListener() {
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                log.info("message:{}", message);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),
                        false);
            }
        });*//*
        //消费端限流
        simpleMessageListenerContainer.setPrefetchCount(1);
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter();
        messageListenerAdapter.setDelegate(orderMessageService);
        Map<String, String> methodMap = new HashMap<>(8);

        methodMap.put(MQQueueConstant.ORDER.getQueue(), "handleMessage1");
        messageListenerAdapter.setQueueOrTagToMethodName(methodMap);
        return simpleMessageListenerContainer;
    }*/
}
