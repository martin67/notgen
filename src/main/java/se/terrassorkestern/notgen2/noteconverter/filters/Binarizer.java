package se.terrassorkestern.notgen2.noteconverter.filters;

import java.awt.image.BufferedImage;

public interface Binarizer {
    BufferedImage toBinary(BufferedImage in);
}
