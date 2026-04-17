package ru.neo.study.dealapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DealApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealApiApplication.class, args);
    }

}
