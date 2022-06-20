package se.terrassorkestern.notgen.model;

import lombok.Data;

@Data
public class Progress {
    int total;
    int progress;
    String message;

    public Progress(int total, int progress, String message) {
        this.total = total;
        this.progress = progress;
        this.message = message;
    }

    public Progress(int progress, String message) {
        this.total = -1;
        this.progress = progress;
        this.message = message;
    }

    public Progress(int progress) {
        this.total = -1;
        this.progress = progress;
        this.message = "";
    }
}
