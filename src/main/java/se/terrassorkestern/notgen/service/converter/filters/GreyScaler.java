package se.terrassorkestern.notgen.service.converter.filters;

import java.awt.image.BufferedImage;

public interface GreyScaler {
    BufferedImage toGreyScale(BufferedImage in);
}
