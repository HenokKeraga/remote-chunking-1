package com.example.remotechunkingsb.config;

import com.example.remotechunkingsb.util.AppConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;


@Configuration
@Profile({"master","worker"})
public class RabbitQueueConfiguration {

    @Bean
    public Queue requestQueue(){
        return new Queue(AppConstant.REQUEST_QUEUE,false);
    }
    @Bean
    public Queue replyQueue(){
        return  new Queue(AppConstant.REPLY_QUEUE,false);
    }

    @Bean
    public DirectChannel request() {
        return new DirectChannel();
    }

    @Bean
    public QueueChannel reply() {
        return new QueueChannel();
    }
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("remote-chunking-exchange");
    }

    @Bean
    Binding repliesBinding(TopicExchange exchange, Queue replyQueue) {
        return BindingBuilder.bind(replyQueue).to(exchange).with(AppConstant.REP_KEY);
    }

    @Bean
    Binding requestBinding(TopicExchange exchange, Queue requestQueue) {
        return BindingBuilder.bind(requestQueue).to(exchange).with(AppConstant.REQ_KEY);
    }

    // configure messaging gateway
    @Bean
    public MessagingTemplate messagingTemplate(DirectChannel request) {
        MessagingTemplate template = new MessagingTemplate();
        template.setDefaultChannel(request);
        template.setReceiveTimeout(2000);
        return template;
    }


}
