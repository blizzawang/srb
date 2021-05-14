package com.wwj.srb.rabbitutil.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class MQService {

    @Resource
    private AmqpTemplate amqpTemplate;

    /**
     *  发送消息
     * @param exchange 交换机
     * @param routingKey 路由
     * @param message 消息
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        log.info("发送消息...........");
        amqpTemplate.convertAndSend(exchange, routingKey, message);
        return true;
    }
}
