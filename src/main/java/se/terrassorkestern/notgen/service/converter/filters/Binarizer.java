package se.terrassorkestern.notgen.service.converter.filters;

import java.awt.image.BufferedImage;

public interface Binarizer {
    BufferedImage toBinary(BufferedImage in);
}
