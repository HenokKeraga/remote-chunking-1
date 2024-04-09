package com.example.remotechunkingsb.tasklet;

import com.example.remotechunkingsb.service.PurgeQueueService;
import com.example.remotechunkingsb.util.AppConstant;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PurgeRabbitTasklet implements Tasklet {
    private  final PurgeQueueService purgeQueueService;

    public PurgeRabbitTasklet(PurgeQueueService purgeQueueService) {
        this.purgeQueueService = purgeQueueService;
    }

    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<String> queueNames = new ArrayList<>();
        queueNames.add(AppConstant.REPLY_QUEUE);
        queueNames.add(AppConstant.REQUEST_QUEUE);

        purgeQueueService.purgeQueue(queueNames);

        return RepeatStatus.FINISHED;
    }
}
