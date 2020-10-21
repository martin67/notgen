package se.terrassorkestern.notgen2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen2.model.ImageData;

@Repository
public interface ImageDataRepository extends JpaRepository<ImageData, Integer> {

    long count();

    @Query("SELECT sum(height) FROM ImageData")
    Long sumSize();

}
