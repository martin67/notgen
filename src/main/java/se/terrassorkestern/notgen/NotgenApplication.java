package se.terrassorkestern.notgen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import se.terrassorkestern.notgen.index.Indexer;

@Slf4j
@SpringBootApplication
public class NotgenApplication {

    @Value("${spring.jpa.properties.hibernate.search.enabled:true}")
    private String jpaSearchEnabled;

    public static void main(String[] args) {
        SpringApplication.run(NotgenApplication.class, args);
    }

    @Bean
    public ApplicationRunner buildIndex(Indexer indexer) {

        if (Boolean.parseBoolean(jpaSearchEnabled)) {
            log.info("Starting search indexer");
            return (ApplicationArguments args) -> {
                indexer.indexPersistedData("se.terrassorkestern.notgen.model.Score");
            };
        } else {
            return null;
        }
    }
}
