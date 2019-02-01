package se.terrassorkestern.notgen2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import se.terrassorkestern.notgen2.song.Song;
import se.terrassorkestern.notgen2.song.SongRepository;
import se.terrassorkestern.notgen2.user.UserRepositoryUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;


// Gick inte att köra med @DataJpaTest, då den försöker hitta UserDetails bean
// som inte laddas när man kör DataJpaTest. Däremot med SpringBootTest så laddas allt
// Men då behövdes det lite extra @Auto samt @Transactional
//

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(UserRepositoryUserDetailsService.class)
public class SongRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SongRepository songRepository;

    // write test cases here

    @Test
    public void whenFindByTitle_thenReturnSong() {
        // given
        Song song1 = new Song();
        song1.setTitle("Testsång");
        entityManager.persist(song1);
        entityManager.flush();

        // when
        Song found = songRepository.findByTitle(song1.getTitle()).get(0);

        // then
        assertThat(found.getTitle())
                .isEqualTo(song1.getTitle());
    }
}

