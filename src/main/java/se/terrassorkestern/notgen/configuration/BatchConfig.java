package se.terrassorkestern.notgen.configuration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.ConverterService;

import java.util.Map;

@Configuration
public class BatchConfig {

    private final ScoreRepository scoreRepository;
    private final ConverterService converterService;


    public BatchConfig(ScoreRepository scoreRepository, ConverterService converterService) {
        this.scoreRepository = scoreRepository;
        this.converterService = converterService;
    }

    @Bean
    public RepositoryItemReader<Score> reader() {
        return new RepositoryItemReaderBuilder<Score>()
                .name("score reader")
                .repository(scoreRepository)
                .methodName("findAll")
                .sorts(Map.of("title", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ListItemWriter<Score> writer() {
        return new ListItemWriter<>();
    }

    @Bean
    public Job updateAllScores(JobRepository jobRepository, Step step1) {
        return new JobBuilder("update all scores", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("update score", jobRepository)
                .<Score, Score>chunk(10, transactionManager)
                .reader(reader())
                .processor(converterService)
                .writer(writer())
                .build();
    }

}