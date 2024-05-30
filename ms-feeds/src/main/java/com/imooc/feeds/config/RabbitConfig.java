package com.imooc.feeds.config;

import com.imooc.commons.constant.MQExchangeConstant;
import com.imooc.commons.constant.MQQueueConstant;
import com.imooc.commons.constant.MQRoutingKeyConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.createConnection();
        return connectionFactory;
    }

    @Bean
    public Exchange exchange() {
        return new DirectExchange(MQExchangeConstant.FEEDS_FOLLOW.getExchange());
    }

    @Bean
    public Queue queue() {
        return new Queue(MQQueueConstant.FEEDS_FOLLOW.getQueue());
    }

    @Bean
    public Binding binding() {
        return new Binding(MQQueueConstant.FEEDS_FOLLOW.getQueue(),
                Binding.DestinationType.QUEUE,
                MQExchangeConstant.FEEDS_FOLLOW.getExchange(),
                MQRoutingKeyConstant.FEEDS_FOLLOW.getKey(),
                null
        );
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    /*@Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        return rabbitTemplate;
    }*/
}
