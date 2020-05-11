/*
  Image binarization - Otsu algorithm

  Author: Bostjan Cigan (http://zerocool.is-a-geek.net)

 */

package se.terrassorkestern.notgen2.noteconverter;

import javax.imageio.ImageIO;
import javax.validation.constraints.NotNull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class OtsuBinarize {

    private static BufferedImage original;
    private static BufferedImage grayscale;
    private static BufferedImage binarized;

  /*
    public static void main(String[] args) throws IOException {

        File original_f = new File(args[0]+".jpg");
        String output_f = args[0]+"_bin";
        original = ImageIO.read(original_f);
        grayscale = toGray(original);
        binarized = binarize(grayscale);
        writeImage(output_f);

    }
   */

    private static void writeImage(String output) throws IOException {
        File file = new File(output + ".jpg");
        ImageIO.write(binarized, "jpg", file);
    }

    // Return histogram of grayscale image
    private static int[] imageHistogram(BufferedImage input) {

        int[] histogram = new int[256];

        for (int i = 0; i < input.getWidth(); i++) {
            for (int j = 0; j < input.getHeight(); j++) {
                int red = new Color(input.getRGB(i, j)).getRed();
                histogram[red]++;
            }
        }

        return histogram;

    }

    // Check wich method that works best
    // luminosity - problem with some lyrics
    // Bäst: decompMax
    BufferedImage toGray(BufferedImage original) {
        return decompMax(original);
    }

    // The luminance method
    private static BufferedImage luminosity(@NotNull BufferedImage original) {

        int alpha;
        int red;
        int green;
        int blue;
        int newPixel;

        BufferedImage lum = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {

                // Get pixels by R, G, B
                alpha = new Color(original.getRGB(i, j)).getAlpha();
                red = new Color(original.getRGB(i, j)).getRed();
                green = new Color(original.getRGB(i, j)).getGreen();
                blue = new Color(original.getRGB(i, j)).getBlue();

                red = (int) (0.21 * red + 0.71 * green + 0.07 * blue);
                // Return back to original format
                newPixel = colorToRgb(alpha, red, red, red);

                // Write pixels into image
                lum.setRGB(i, j, newPixel);

            }
        }

        return lum;

    }

    // The average grayscale method
    private static BufferedImage avg(@NotNull BufferedImage original) {

        int alpha;
        int red;
        int green;
        int blue;
        int newPixel;

        BufferedImage avgGray = new BufferedImage(original.getWidth(),
                original.getHeight(), original.getType());
        int[] avgLut = new int[766];
        for (int i = 0; i < avgLut.length; i++) {
            avgLut[i] = i / 3;
        }

        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {

                // Get pixels by R, G, B
                alpha = new Color(original.getRGB(i, j)).getAlpha();
                red = new Color(original.getRGB(i, j)).getRed();
                green = new Color(original.getRGB(i, j)).getGreen();
                blue = new Color(original.getRGB(i, j)).getBlue();

                newPixel = red + green + blue;
                newPixel = avgLut[newPixel];
                // Return back to original format
                newPixel = colorToRgb(alpha, newPixel, newPixel, newPixel);

                // Write pixels into image
                avgGray.setRGB(i, j, newPixel);

            }
        }

        return avgGray;

    }

    // The desaturation method
    private static BufferedImage desaturation(@NotNull BufferedImage original) {

        int alpha;
        int red;
        int green;
        int blue;
        int newPixel;
        int[] pixel = new int[3];

        BufferedImage des = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        int[] desLut = new int[511];
        for (int i = 0; i < desLut.length; i++) {
            desLut[i] = i / 2;
        }

        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {

                // Get pixels by R, G, B
                alpha = new Color(original.getRGB(i, j)).getAlpha();
                red = new Color(original.getRGB(i, j)).getRed();
                green = new Color(original.getRGB(i, j)).getGreen();
                blue = new Color(original.getRGB(i, j)).getBlue();

                pixel[0] = red;
                pixel[1] = green;
                pixel[2] = blue;

                int newval = (findMax(pixel) + findMin(pixel));
                newval = desLut[newval];

                // Return back to original format
                newPixel = colorToRgb(alpha, newval, newval, newval);

                // Write pixels into image
                des.setRGB(i, j, newPixel);

            }
        }

        return des;

    }

    // The minimal decomposition method
    private static BufferedImage decompMin(@NotNull BufferedImage original) {

        int alpha;
        int red;
        int green;
        int blue;
        int newPixel;
        int[] pixel = new int[3];

        BufferedImage decomp = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {

                // Get pixels by R, G, B
                alpha = new Color(original.getRGB(i, j)).getAlpha();
                red = new Color(original.getRGB(i, j)).getRed();
                green = new Color(original.getRGB(i, j)).getGreen();
                blue = new Color(original.getRGB(i, j)).getBlue();

                pixel[0] = red;
                pixel[1] = green;
                pixel[2] = blue;

                int newval = findMin(pixel);

                // Return back to original format
                newPixel = colorToRgb(alpha, newval, newval, newval);

                // Write pixels into image
                decomp.setRGB(i, j, newPixel);

            }
        }

        return decomp;

    }

    // The maximum decomposition method
    private static BufferedImage decompMax(@NotNull BufferedImage original) {

        int alpha;
        int red;
        int green;
        int blue;
        int newPixel;

        int[] pixel = new int[3];

        BufferedImage decomp = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {

                // Get pixels by R, G, B
                alpha = new Color(original.getRGB(i, j)).getAlpha();
                red = new Color(original.getRGB(i, j)).getRed();
                green = new Color(original.getRGB(i, j)).getGreen();
                blue = new Color(original.getRGB(i, j)).getBlue();

                pixel[0] = red;
                pixel[1] = green;
                pixel[2] = blue;

                int newval = findMax(pixel);

                // Return back to original format
                newPixel = colorToRgb(alpha, newval, newval, newval);

                // Write pixels into image
                decomp.setRGB(i, j, newPixel);

            }

        }

        return decomp;

    }

    // The "pick the color" method
    private static BufferedImage rgb(@NotNull BufferedImage original, int color) {

        int alpha;
        int red;
        int green;
        int blue;
        int newPixel;
        int[] pixel = new int[3];

        BufferedImage rgb = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {

                // Get pixels by R, G, B
                alpha = new Color(original.getRGB(i, j)).getAlpha();
                red = new Color(original.getRGB(i, j)).getRed();
                green = new Color(original.getRGB(i, j)).getGreen();
                blue = new Color(original.getRGB(i, j)).getBlue();

                pixel[0] = red;
                pixel[1] = green;
                pixel[2] = blue;

                int newval = pixel[color];

                // Return back to original format
                newPixel = colorToRgb(alpha, newval, newval, newval);

                // Write pixels into image
                rgb.setRGB(i, j, newPixel);

            }

        }

        return rgb;

    }


    // Get binary treshold using Otsu's method
    private static int otsuTreshold(BufferedImage original) {

        int[] histogram = imageHistogram(original);
        int total = original.getHeight() * original.getWidth();

        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        float sumB = 0;
        int weightBackground = 0;
        int weightForeground;

        float varMax = 0;
        int threshold = 0;

        for (int i = 0; i < 256; i++) {
            weightBackground += histogram[i];
            if (weightBackground == 0) {
                continue;
            }
            weightForeground = total - weightBackground;

            if (weightForeground == 0) {
                break;
            }

            sumB += (float) (i * histogram[i]);
            float meanBackground = sumB / weightBackground;
            float meanForeground = (sum - sumB) / weightForeground;

            float varBetween = (float) weightBackground * (float) weightForeground * (meanBackground - meanForeground) * (meanBackground - meanForeground);

            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }

        return threshold;

    }

    BufferedImage binarize(BufferedImage original) {

        int red;
        int newPixel;

        int threshold = otsuTreshold(original);

        BufferedImage binarized = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

        for (int i = 0; i < original.getWidth(); i++) {
            for (int j = 0; j < original.getHeight(); j++) {

                // Get pixels
                red = new Color(original.getRGB(i, j)).getRed();
                int alpha = new Color(original.getRGB(i, j)).getAlpha();
                if (red > threshold) {
                    newPixel = 255;
                } else {
                    newPixel = 0;
                }
                newPixel = colorToRgb(alpha, newPixel, newPixel, newPixel);
                binarized.setRGB(i, j, newPixel);

            }
        }

        return binarized;

    }

    // Convert R, G, B, Alpha to standard 8 bit
    private static int colorToRgb(int alpha, int red, int green, int blue) {

        int newPixel = 0;
        newPixel += alpha;
        newPixel = newPixel << 8;
        newPixel += red;
        newPixel = newPixel << 8;
        newPixel += green;
        newPixel = newPixel << 8;
        newPixel += blue;

        return newPixel;

    }

    private static int findMin(@NotNull int[] pixels) {

        int min = pixels[0];

        for (int pixel : pixels) {
            if (pixel < min) {
                min = pixel;
            }
        }

        return min;

    }

    private static int findMax(@NotNull int[] pixels) {

        int max = pixels[0];

        for (int pixel : pixels) {
            if (pixel > max) {
                max = pixel;
            }
        }

        return max;

    }
}