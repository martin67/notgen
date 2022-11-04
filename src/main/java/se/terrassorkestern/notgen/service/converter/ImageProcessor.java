package se.terrassorkestern.notgen.service.converter;

import se.terrassorkestern.notgen.model.ScoreType;

public interface ImageProcessor extends Runnable {

    ScoreType getScoreType();
}
