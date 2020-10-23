package se.terrassorkestern.notgen2.filters;

import java.awt.image.BufferedImage;

public interface Binarizer {
    BufferedImage toBinary(BufferedImage in);
}
