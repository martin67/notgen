package se.terrassorkestern.notgen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class NotgenApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotgenApplication.class, args);
    }

}
