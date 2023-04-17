package se.terrassorkestern.notgen;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import se.terrassorkestern.notgen.repository.SearchRepositoryImpl;

@Configuration
@EnableScheduling
@EnableJpaRepositories(repositoryBaseClass = SearchRepositoryImpl.class)
public class NotgenConfiguration {
}