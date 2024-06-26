package com.example.remotechunkingsb.model;

import lombok.SneakyThrows;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CustomChunkProcessor extends SimpleChunkProcessor<Student, Student> {
    public CustomChunkProcessor(ItemWriter<? super Student> crsGdxItemWriter) {
        super(crsGdxItemWriter);
    }

    @Override
    protected Chunk<Student> transform(StepContribution contribution, Chunk<Student> inputs) throws Exception {
        return new Chunk<>(inputs.getItems().stream().map(this::processItem).collect(Collectors.toList()));
    }

    @SneakyThrows
    private Student processItem(Student student) {
        return doProcess(student);
    }
}
