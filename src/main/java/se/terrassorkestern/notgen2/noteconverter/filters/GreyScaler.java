package se.terrassorkestern.notgen2.noteconverter.filters;

import java.awt.image.BufferedImage;

public interface GreyScaler {
    BufferedImage toGreyScale(BufferedImage in);
}
