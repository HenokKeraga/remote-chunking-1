package com.example.remotechunkingsb.config;

import com.example.remotechunkingsb.model.CustomChunkProcessor;
import com.example.remotechunkingsb.model.Student;
import com.example.remotechunkingsb.util.AppConstant;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.integration.chunk.ChunkProcessorChunkHandler;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
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
    public TopicExchange exchange() {
        return new TopicExchange("remote-chunking-exchange");
    }

    @Bean
    Binding repliesBinding(TopicExchange exchange,Queue replyQueue) {
        return BindingBuilder.bind(replyQueue).to(exchange).with(AppConstant.REPLY);
    }

    @Bean
    Binding requestBinding(TopicExchange exchange,Queue requestQueue) {
        return BindingBuilder.bind(requestQueue).to(exchange).with(AppConstant.REQUEST);
    }

    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory) {
        var simpleMessageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        simpleMessageListenerContainer.setConcurrentConsumers(1);
        simpleMessageListenerContainer.setPrefetchCount(1);
        simpleMessageListenerContainer.setIdleEventInterval(10000);
        simpleMessageListenerContainer.setListenerId("queue");
        simpleMessageListenerContainer.setQueueNames(AppConstant.REQUEST);

        return simpleMessageListenerContainer;
    }

    @Bean
    public DirectChannel request() {
        return new DirectChannel();
    }

    @Bean
    public DirectChannel reply() {
        return new DirectChannel();
    }


    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setConcurrentConsumers(1);
        container.setPrefetchCount(1);
        container.setIdleEventInterval(10000);
        container.setListenerId("queue");
        container.setQueueNames("request");
        return container;
    }

    //    @Bean
//    public IntegrationFlow messageIn(SimpleMessageListenerContainer messageListenerContainer) {
//        return IntegrationFlow.from(Amqp.inboundAdapter(messageListenerContainer))
//                .channel(orcRequests())
//                .get();
//    }
    @Bean
    public IntegrationFlow mesagesIn(ConnectionFactory connectionFactory) {
        return IntegrationFlow
                .from(Amqp.inboundAdapter(connectionFactory, AppConstant.REQUEST))
                .channel(request())
                .get();
    }


//    @Bean
//    public IntegrationFlow outgoingReplies(RabbitTemplate rabbitTemplate) {
//        return IntegrationFlow.from(orcRequests()) // Using the channel bean directly
//                .handle(Amqp.outboundAdapter(rabbitTemplate).routingKey(AppConstant.REPLY))
//                .get();
//    }

    @Bean
    public IntegrationFlow outgoingReplies(AmqpTemplate template) {
        return IntegrationFlow.from(AppConstant.REPLY)
                .handle(Amqp.outboundAdapter(template)
                        .routingKey(AppConstant.REPLY))
                .get();
    }

    @Bean
    @ServiceActivator(inputChannel = AppConstant.REQUEST, outputChannel = AppConstant.REPLY, autoStartup = "true")
    public ChunkProcessorChunkHandler<Student> chunkProcessorChunkHandler() {
        ChunkProcessorChunkHandler<Student> chunkHandler = new ChunkProcessorChunkHandler<>();
//        chunkHandler.setChunkProcessor(customChunkProcessor, itemWriter());
        chunkHandler.setChunkProcessor(
                new SimpleChunkProcessor<>((transaction) -> {
                    System.out.println(">> processing transaction: " + transaction);
                    Thread.sleep(5);
                    return transaction;
                }, itemWriter()));

        return chunkHandler;
    }

    @Bean
    public ItemWriter<Student> itemWriter() {

        return new ItemWriter<Student>() {
            @Override
            public void write(Chunk<? extends Student> chunk) throws Exception {
                chunk.getItems().forEach(System.out::println);
            }
        };
    }
}
