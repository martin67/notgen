package se.terrassorkestern.notgen.filters;

import java.awt.image.BufferedImage;

public interface Binarizer {
    BufferedImage toBinary(BufferedImage in);
}
