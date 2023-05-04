package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface SearchRepository<T, I extends Serializable> extends JpaRepository<T, I> {

    List<T> searchBy(String text, int limit, String... fields);
}