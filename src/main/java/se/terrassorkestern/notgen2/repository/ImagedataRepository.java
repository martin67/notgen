package se.terrassorkestern.notgen2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen2.model.Imagedata;

@Repository
public interface ImagedataRepository extends JpaRepository<Imagedata, Integer> {

    @Query(value = "SELECT sum(fileSize) FROM Imagedata")
    long sumSize();
}