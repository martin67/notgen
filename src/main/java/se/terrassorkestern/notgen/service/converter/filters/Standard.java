package se.terrassorkestern.notgen.service.converter.filters;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Standard implements Binarizer, GreyScaler {

    @Override
    public BufferedImage toBinary(BufferedImage in) {
        BufferedImage bw = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = bw.createGraphics();
        g.drawImage(in, 0, 0, null);
        return bw;
    }

    @Override
    public BufferedImage toGreyScale(BufferedImage in) {
        BufferedImage grey = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grey.getGraphics();
        g.drawImage(in, 0, 0, null);
        g.dispose();
        return grey;
    }
}
