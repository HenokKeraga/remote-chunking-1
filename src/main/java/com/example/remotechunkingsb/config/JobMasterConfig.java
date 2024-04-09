package com.example.remotechunkingsb.config;

import com.example.remotechunkingsb.model.Student;
import com.example.remotechunkingsb.model.StudentRowMapper;
import com.example.remotechunkingsb.tasklet.PurgeRabbitTasklet;
import com.example.remotechunkingsb.util.AppConstant;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter;
import org.springframework.batch.integration.chunk.RemoteChunkHandlerFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Profile("master")
public class JobMasterConfig {
    private final JobRepository jobRepository;

    private final PlatformTransactionManager platformTransactionManager;

    public JobMasterConfig(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
    }


    @Bean
    public Job batchJob(Flow stepFlow) {

        return new JobBuilder("batchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepFlow)
                .end()
                .build();
    }

    @Bean
    public Flow stepFlow(TaskletStep purgeRabbitMq, TaskletStep prcPacProcessStep) {
        return new FlowBuilder<SimpleFlow>("stepFlow")
                .start(purgeRabbitMq)
//                .next(cleanUp)
//                .next(createDirectorey)
                .next(prcPacProcessStep)
//                .next(agg)
                .end();
    }

    @Bean
    public TaskletStep purgeRabbitMq(PurgeRabbitTasklet purgeRabbitTasklet) {

        return new StepBuilder("purgeRabbitMq", jobRepository)
                .tasklet(purgeRabbitTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    public SimpleChunkProvider<Student> simpleChunkProvider(ItemReader<Student> itemReader){
        var objectSimpleChunkProvider = new SimpleChunkProvider<Student>(itemReader, new RepeatOperations() {
            @Override
            public RepeatStatus iterate(RepeatCallback callback) throws RepeatException {
                return RepeatStatus.FINISHED;
            }
        });


        return objectSimpleChunkProvider;

    }

    @Bean
    public TaskletStep prcPacProcessStep(ItemReader<Student> itemReader,
                                         ItemWriter<Student> itemWriter,
                                         ThreadPoolTaskExecutor pacTheadPoolTaskExecutor) {
        return new StepBuilder("prcPacProcessStep", jobRepository)
                .<Student, Student>chunk(10, platformTransactionManager)
                .reader(itemReader)
                .writer(itemWriter)
                .taskExecutor(pacTheadPoolTaskExecutor)
                .build();
    }

    @Bean
    public ThreadPoolTaskExecutor pacTheadPoolTaskExecutor() {
        var threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setQueueCapacity(Integer.MAX_VALUE);
        threadPoolTaskExecutor.initialize();

        return threadPoolTaskExecutor;

    }

    @Bean
    public JdbcCursorItemReader<Student> itemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Student>()
                .name("itemReader")
                .sql("Select * from student")
                .dataSource(dataSource)
                .rowMapper(new StudentRowMapper())
                .build();
    }


// configure messaging gateway
    @Bean
    public MessagingTemplate messagingTemplate(DirectChannel request) {
        MessagingTemplate template = new MessagingTemplate();
        template.setDefaultChannel(request);
        template.setReceiveTimeout(2000);
        return template;
    }

    @Bean
    public TaskExecutor taskExecutorCustom() {
        var threadPoolExecutor = new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(2);
        threadPoolExecutor.setMaxPoolSize(3);
        threadPoolExecutor.setThreadGroupName("dsf");
        threadPoolExecutor.initialize();
        return threadPoolExecutor;
    }


    @Bean
    public IntegrationFlow messageIn(ConnectionFactory connectionFactory,QueueChannel reply) {
        return IntegrationFlow
                .from(Amqp.inboundAdapter(connectionFactory, AppConstant.QUEUE_REPLY))
                .channel(reply)
                .get();
    }

    @Bean
    public IntegrationFlow messageOut(AmqpTemplate amqpTemplate) {
        return IntegrationFlow.from(AppConstant.CHANNEL_REQUEST)
                .handle(Amqp.outboundAdapter(amqpTemplate)
                        .routingKey(AppConstant.QUEUE_REQUEST))
                .get();
    }
    @Bean
    public ChunkMessageChannelItemWriter<Student> itemWriter(QueueChannel reply,MessagingTemplate messagingTemplate) {
        var chunkMessageChannelItemWriter = new ChunkMessageChannelItemWriter<Student>();
        chunkMessageChannelItemWriter.setMessagingOperations(messagingTemplate);

        chunkMessageChannelItemWriter.setReplyChannel(reply);
        chunkMessageChannelItemWriter.setThrottleLimit(100);
        chunkMessageChannelItemWriter.setMaxWaitTimeouts(800000);

        return chunkMessageChannelItemWriter;
    }

    @Bean
    public RemoteChunkHandlerFactoryBean<Student> chunkHandler(TaskletStep prcPacProcessStep,ChunkMessageChannelItemWriter<Student> itemWriter) {
        RemoteChunkHandlerFactoryBean<Student> remoteChunkHandlerFactoryBean = new RemoteChunkHandlerFactoryBean<>();
        remoteChunkHandlerFactoryBean.setChunkWriter(itemWriter);
        remoteChunkHandlerFactoryBean.setStep(prcPacProcessStep);
        return remoteChunkHandlerFactoryBean;
    }

//    @Bean
//    public ItemWriter<Student> gitemWriter() {
//
//        return new ItemWriter<Student>() {
//            @Override
//            public void write(Chunk<? extends Student> chunk) throws Exception {
//                chunk.getItems().forEach(System.out::println);
//            }
//        };
//    }

}
