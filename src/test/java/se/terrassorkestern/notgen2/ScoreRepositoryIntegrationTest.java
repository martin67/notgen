package se.terrassorkestern.notgen2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import se.terrassorkestern.notgen2.score.Score;
import se.terrassorkestern.notgen2.score.ScoreRepository;
import se.terrassorkestern.notgen2.user.UserRepositoryUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;


// Gick inte att köra med @DataJpaTest, då den försöker hitta UserDetails bean
// som inte laddas när man kör DataJpaTest. Däremot med SpringBootTest så laddas allt
// Men då behövdes det lite extra @Auto samt @Transactional
//

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(UserRepositoryUserDetailsService.class)
public class ScoreRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ScoreRepository scoreRepository;

    // write test cases here

    @Test
    public void whenFindByTitle_thenReturnSong() {
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

