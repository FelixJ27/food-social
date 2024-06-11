package com.imooc.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imooc.commons.constant.OrderStatus;
import com.imooc.commons.model.dto.OrderMessageDTO;
import com.imooc.commons.model.po.OrderDetailPO;
import com.imooc.order.mapper.OrderDetailMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


@Slf4j
@Service
public class OrderMessageService {

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(
//            containerFactory = "rabbitListenerContainerFactory",
//            queues = "queue.order",
//            admin = "rabbitAdmin",
            bindings = {
                    @QueueBinding(
                            value = @Queue(name = "${imooc.order-queue}",
                                    arguments = {
                                            //  @Argument(name =
                                            //  "x-message-ttl", value =
                                            //  "1000", type = "java.lang
                                            //  .Integer"),
                                            //  @Argument(name =
                                            //  "x-dead-letter-exchange",
                                            //  value = "aaaaa"),
                                            //  @Argument(name =
                                            //  "x-dead-letter-routing-key", value = "bbbb")
                                    }),
                            exchange = @Exchange(name = "exchange.order.restaurant", type = ExchangeTypes.DIRECT),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(name = "queue.order"),
                            exchange = @Exchange(name = "exchange.order.deliveryman", type = ExchangeTypes.DIRECT),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(name = "queue.order"),
                            exchange = @Exchange(name = "exchange.settlement.order", type = ExchangeTypes.FANOUT),
                            key = "key.order"
                    ),
                    @QueueBinding(
                            value = @Queue(name = "queue.order"),
                            exchange = @Exchange(name = "exchange.order.reward", type = ExchangeTypes.TOPIC),
                            key = "key.order"
                    )
            }
    )
    public void handleMessage(byte[] messageBody) throws IOException {
        log.info("deliverCallback:messageBody:{}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
                    OrderMessageDTO.class);
            OrderDetailPO orderPO = orderDetailMapper.selectOrder(orderMessageDTO.getOrderId());

            switch (orderPO.getStatus()) {

                case ORDER_CREATING:
                    if (orderMessageDTO.getConfirmed() && null != orderMessageDTO.getPrice()) {
                        orderPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        orderPO.setPrice(orderMessageDTO.getPrice());
                        orderDetailMapper.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.deliveryman", "key.deliveryman", null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderPO);
                    }
                    break;
                case RESTAURANT_CONFIRMED:
                    if (null != orderMessageDTO.getDeliverymanId()) {
                        orderPO.setStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        orderPO.setDeliverymanId(orderMessageDTO.getDeliverymanId());
                        orderDetailMapper.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.settlement", "key.settlement", null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderPO);
                    }
                    break;
                case DELIVERYMAN_CONFIRMED:
                    if (null != orderMessageDTO.getSettlementId()) {
                        orderPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        orderPO.setSettlementId(orderMessageDTO.getSettlementId());
                        orderDetailMapper.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.reward", "key.reward", null, messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderPO);
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    if (null != orderMessageDTO.getRewardId()) {
                        orderPO.setStatus(OrderStatus.ORDER_CREATED);
                        orderPO.setRewardId(orderMessageDTO.getRewardId());
                        orderDetailMapper.update(orderPO);
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderPO);
                    }
                    break;
            }

        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    ;
}
