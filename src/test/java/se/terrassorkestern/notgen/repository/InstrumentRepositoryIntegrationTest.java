package se.terrassorkestern.notgen.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import se.terrassorkestern.notgen.model.Instrument;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
// Problem with hibernate search
@DataJpaTest
class InstrumentRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InstrumentRepository instrumentRepository;

    // write test cases here

    @Test
    void whenFindByName_thenReturnInstrument() {
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

