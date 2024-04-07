package com.example.remotechunkingsb;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableRabbit
public class RemoteChunkingSbApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemoteChunkingSbApplication.class, args);
    }

}
