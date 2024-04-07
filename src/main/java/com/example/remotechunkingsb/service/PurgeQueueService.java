package com.example.remotechunkingsb.service;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurgeQueueService {

    private RabbitAdmin rabbitAdmin;

    public PurgeQueueService(ConnectionFactory connectionFactory) {
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
    }

    public void purgeQueue(List<String> queueNames) {
        for (String queue : queueNames) {
            rabbitAdmin.purgeQueue(queue);
        }
    }
}
