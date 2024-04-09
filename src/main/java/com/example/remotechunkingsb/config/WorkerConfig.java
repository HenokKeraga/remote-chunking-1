package com.example.remotechunkingsb.config;

import com.example.remotechunkingsb.model.CustomChunkProcessor;
import com.example.remotechunkingsb.model.Student;
import com.example.remotechunkingsb.util.AppConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.batch.integration.chunk.ChunkProcessorChunkHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;

@Profile("worker")
@Configuration
public class WorkerConfig {

    private final CustomChunkProcessor customChunkProcessor;

    public WorkerConfig(CustomChunkProcessor customChunkProcessor) {
        this.customChunkProcessor = customChunkProcessor;
    }



    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConcurrentConsumers(1);
        container.setPrefetchCount(1);
        container.setIdleEventInterval(10000);
        container.setListenerId("queue");
        container.setQueueNames(AppConstant.REQUEST_QUEUE);

        return container;
    }


    @Bean
    public IntegrationFlow messagesIn(SimpleMessageListenerContainer messageListenerContainer,
                                     DirectChannel request) {
        return IntegrationFlow
                .from(Amqp.inboundAdapter(messageListenerContainer))
                .channel(request)
                .get();
    }



    @Bean
    public IntegrationFlow messageOut(AmqpTemplate template) {
        return IntegrationFlow.from(AppConstant.CHANNEL_REPLY)
                .handle(Amqp.outboundAdapter(template)
                        .routingKey(AppConstant.REPLY_QUEUE))
                .get();
    }

    @Bean
    @ServiceActivator(inputChannel = AppConstant.CHANNEL_REQUEST, outputChannel = AppConstant.CHANNEL_REPLY, autoStartup = "true")
    public ChunkProcessorChunkHandler<Student> chunkProcessorChunkHandler() {
        ChunkProcessorChunkHandler<Student> chunkHandler = new ChunkProcessorChunkHandler<>();
        chunkHandler.setChunkProcessor(customChunkProcessor);
//        chunkHandler.setChunkProcessor(
//                new SimpleChunkProcessor<>((transaction) -> {
//                    System.out.println(">> processing transaction: " + transaction);
//                    Thread.sleep(5);
//                    return transaction;
//                }, itemWriter()));

        return chunkHandler;
    }


}
