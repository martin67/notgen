package se.terrassorkestern.notgen.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import se.terrassorkestern.notgen.model.Score;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class ScoreRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ScoreRepository scoreRepository;

    // write test cases here

    @Test
    void whenFindByTitle_thenReturnSong() {
        // given
        Score score1 = new Score();
        score1.setTitle("Testsång");
        entityManager.persist(score1);
        entityManager.flush();

        // when
        Score found = scoreRepository.findByTitle(score1.getTitle()).get(0);

        // then
        assertThat(found.getTitle())
                .isEqualTo(score1.getTitle());
    }
}

