package se.terrassorkestern.notgen2.noteconverter;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

/*
    Stats for one round of converting
 */
@Data
class NoteConverterStats {
    private int numberOfSongs;
    private int numberOfPdf;
    private int numberOfSrcImg;
    private int numberOfImgProcess;
    private int numberOfCovers;
    private int numberOfOCRs;
    private long numberOfBytes;
    private Instant startTime;
    private Instant endTime;


    public void incrementNumberOfSongs() {
        this.numberOfSongs++;
    }

    public void incrementNumberOfPdf(){
        this.numberOfPdf++;
    }

    public void incrementNumberOfCovers(){
        this.numberOfCovers++;
    }

    public void incrementNumberOfOCRs(){
        this.numberOfOCRs++;
    }

    public void incrementNumberOfImgProcess(){
        this.numberOfImgProcess++;
    }

    public void addNumberOfSrcImg(int numberOfSrcImg){
        this.numberOfSrcImg += numberOfSrcImg;
    }

    public void addNumberOfBytes(long numberOfBytes) {
        this.numberOfBytes += numberOfBytes;
    }

    void print() {
        System.out.println("Number of songs processed:          " + numberOfSongs);
        System.out.println("Number of files extracted:          " + numberOfSrcImg);
        System.out.println("Number of images processed:         " + numberOfImgProcess);
        System.out.println("Number of pdfs created:             " + numberOfPdf);
        System.out.println("Number of covers created:           " + numberOfCovers);
        System.out.println("Number of lyrics OCR:               " + numberOfOCRs);
        System.out.println("Number of bytes uploaded to Google: " + numberOfBytes);
        System.out.println("Total processing time:              " + Duration.between(endTime, startTime));
    }

}
