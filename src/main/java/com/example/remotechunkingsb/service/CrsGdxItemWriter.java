package com.example.remotechunkingsb.service;

import com.example.remotechunkingsb.model.Student;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class CrsGdxItemWriter implements ItemWriter<Student> {
    @Override
    public void write(Chunk<? extends Student> chunk) throws Exception {
        chunk.getItems().forEach(item -> System.out.println("CrsGdxItemWriter "+item));

    }
}
