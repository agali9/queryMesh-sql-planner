package com.dsqp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DistributedSqlPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedSqlPlannerApplication.class, args);
    }
}
