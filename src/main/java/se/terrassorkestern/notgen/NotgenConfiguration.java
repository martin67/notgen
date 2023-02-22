package se.terrassorkestern.notgen;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import se.terrassorkestern.notgen.repository.SearchRepositoryImpl;

@Configuration
@EnableJpaRepositories(repositoryBaseClass = SearchRepositoryImpl.class)
public class NotgenConfiguration {
}