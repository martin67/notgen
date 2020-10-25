package se.terrassorkestern.notgen2.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
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
}
