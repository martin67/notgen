package se.terrassorkestern.notgen.filters;

import java.awt.image.BufferedImage;

public interface GreyScaler {
    BufferedImage toGreyScale(BufferedImage in);
}
