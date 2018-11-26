package se.terrassorkestern.notgen2;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "repertoire")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "titel")
    private String title;
    @Column(name = "subtitel")
    private String subtitle;
    @Column(name = "genre")
    private String genre;
    @Column(name = "musik")
    private String composer;
    @Column(name = "text")
    private String author;
    @Column(name = "arr")
    private String arranger;
    @Column(name = "ar")
    private Integer year;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL)
    private List<ScorePart> scoreParts;

    @Column(name = "inscannad")
    private Boolean scanned;
    @Column(name = "omslag")
    private Boolean cover;
    @Column(name = "bildbehandla")
    private Boolean imageProcess;
    private Boolean upperleft;
    private Boolean color;

    @Column(name = "filnamn")
    private String filename;


    public Song() {
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getArranger() {
        return arranger;
    }

    public void setArranger(String arranger) {
        this.arranger = arranger;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<ScorePart> getScoreParts() {
        return scoreParts;
    }

    public void setScoreParts(List<ScorePart> scoreParts) {
        this.scoreParts = scoreParts;
    }

    public boolean isScanned() {
        return scanned;
    }

    public void setScanned(boolean scanned) {
        this.scanned = scanned;
    }

    public boolean isCover() {
        return cover;
    }

    public void setCover(boolean cover) {
        this.cover = cover;
    }

    public boolean isImageProcess() {
        return imageProcess;
    }

    public void setImageProcess(boolean imageProcess) {
        this.imageProcess = imageProcess;
    }

    public boolean isUpperleft() {
        return upperleft;
    }

    public void setUpperleft(boolean upperleft) {
        this.upperleft = upperleft;
    }

    public boolean isColor() {
        return color;
    }

    public void setColor(boolean color) {
        this.color = color;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String toString() {
        return "Song{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", genre='" + genre + '\'' +
                ", composer='" + composer + '\'' +
                ", author='" + author + '\'' +
                ", arranger='" + arranger + '\'' +
                ", year=" + year +
                '}';
    }
}
