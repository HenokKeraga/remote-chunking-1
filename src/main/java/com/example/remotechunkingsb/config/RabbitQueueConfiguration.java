package com.example.remotechunkingsb.config;

import com.example.remotechunkingsb.util.AppConstant;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@Profile({"master","worker"})
public class RabbitQueueConfiguration {

    @Bean
    public Queue requestQueue(){
        return new Queue(AppConstant.QUEUE_REQUEST,false);
    }
    @Bean
    public Queue replyQueue(){
        return  new Queue(AppConstant.QUEUE_REPLY,false);
    }
}
