package se.terrassorkestern.notgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Imagedata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int page;
    private long fileSize;
    private String format;
    private int width;
    private int widthDpi;
    private int height;
    private int heightDpi;
    private int colorDepth;
    private String colorType;
}
