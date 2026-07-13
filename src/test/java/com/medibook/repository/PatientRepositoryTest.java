package com.medibook.repository;

import com.medibook.domain.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class PatientRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired PatientRepository repository;

    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);

    private Patient patient(String email) {
        return new Patient("John Doe", email, DOB, "555-0000");
    }

    @Test
    void findByEmail_returnsPatientWhenExists() {
        em.persistAndFlush(patient("john@test.com"));

        Optional<Patient> result = repository.findByEmail("john@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void findByEmail_returnsEmptyWhenNotExists() {
        Optional<Patient> result = repository.findByEmail("nobody@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    void save_rejectsDuplicateEmailAtDbLevel_throwsDataIntegrityViolation() {
        em.persistAndFlush(patient("john@test.com"));

        assertThatThrownBy(() -> {
            repository.save(patient("john@test.com"));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByEmail_isCaseSensitiveAtRawRepositoryLevel() {
        em.persistAndFlush(patient("john@test.com"));

        Optional<Patient> result = repository.findByEmail("John@Test.com");

        assertThat(result).isEmpty();
    }
}
