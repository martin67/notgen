package se.terrassorkestern.notgen2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import se.terrassorkestern.notgen2.instrument.Instrument;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.user.UserRepositoryUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;


// Gick inte att köra med @DataJpaTest, då den försöker hitta UserDetails bean
// som inte laddas när man kör DataJpaTest. Däremot med SpringBootTest så laddas allt
// Men då behövdes det lite extra @Auto samt @Transactional
//

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(UserRepositoryUserDetailsService.class)
//@SpringBootTest
//@AutoConfigureDataJpa
//@AutoConfigureTestDatabase
//@AutoConfigureTestEntityManager
//@Transactional
public class InstrumentRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InstrumentRepository instrumentRepository;

    // write test cases here

    @Test
    public void whenFindByName_thenReturnInstrument() {
        // given
        Instrument sax = new Instrument();
        sax.setName("Saxofon");
        sax.setSortOrder(10);
        entityManager.persist(sax);
        entityManager.flush();

        // when
        Instrument found = instrumentRepository.findByName(sax.getName()).get(0);

        // then
        assertThat(found.getName())
                .isEqualTo(sax.getName());
    }
}

