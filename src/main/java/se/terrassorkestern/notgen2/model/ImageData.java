package se.terrassorkestern.notgen2.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "imagedata")
public class ImageData {
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
