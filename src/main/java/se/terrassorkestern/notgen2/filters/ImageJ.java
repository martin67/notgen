package se.terrassorkestern.notgen2.filters;

import ij.ImagePlus;

import java.awt.image.BufferedImage;

public class ImageJ implements GreyScaler {

    @Override
    public BufferedImage toGreyScale(BufferedImage in) {
        ImagePlus ip = new ImagePlus("Hejsan", in);
         return null;
    }
}
