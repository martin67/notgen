package se.terrassorkestern.notgen.model;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "imagedata")
public class Imagedata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int page;
    private long fileSize;
    private String format;
    private int width;
    private int widthDpi;
    private int height;
    private int heightDpi;
    private int colorDepth;
    private String colorType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Imagedata imagedata = (Imagedata) o;
        return id != null && Objects.equals(id, imagedata.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
