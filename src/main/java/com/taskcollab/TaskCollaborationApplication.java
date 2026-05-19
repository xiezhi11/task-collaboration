package com.taskcollab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.taskcollab.mapper")
public class TaskCollaborationApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskCollaborationApplication.class, args);
    }
}
