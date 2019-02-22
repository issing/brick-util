package net.isger.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class Images {

    private static final String DEFAULT_FORMAT = "jpg";

    private static final int DEFAULT_WIDTH = 240;

    private static final int DEFAULT_HEIGHT = 240;

    private Images() {
    }

    public static BufferedImage getImage(String imagePath) {
        return getImage(new File(imagePath));
    }

    public static BufferedImage getImage(File imageFile) {
        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            return null;
        }
    }

    public static BufferedImage getQrcodeImage(final String content) {
        return getQrcodeImage(content, null);
    }

    public static BufferedImage getQrcodeImage(String content, Image logo) {
        return getQrcodeImage(content, DEFAULT_WIDTH, DEFAULT_HEIGHT, logo);
    }

    public static BufferedImage getQrcodeImage(String content, int width,
            int height, Image logo) {
        return getQrcodeImage(content, width, height, ErrorCorrectionLevel.M,
                logo);
    }

    public static BufferedImage getQrcodeImage(String content, int width,
            int height, ErrorCorrectionLevel level, Image logo) {
        BufferedImage image = toImage(
                createQrcode(content, width, height, level));
        if (logo != null) {
            drawLogo(image, logo);
        }
        return image;
    }

    public static InputStream getQrcodeStream(String content) {
        return toInputStream(getQrcodeImage(content));
    }

    public static BitMatrix createQrcode(String content, int width, int height,
            ErrorCorrectionLevel level) {
        HashMap<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, level);
        BitMatrix matrix = null;
        try {
            matrix = new MultiFormatWriter().encode(content,
                    BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException e) {
        }
        return matrix;
    }

    public static BufferedImage toImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_BYTE_BINARY);
        int onColor = 0xFF000000;
        int offColor = 0xFFFFFFFF;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? onColor : offColor);
            }
        }
        return image;
    }

    public static InputStream toInputStream(RenderedImage image) {
        return toInputStream(image, DEFAULT_FORMAT);
    }

    public static InputStream toInputStream(RenderedImage image,
            String format) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios;
        try {
            ios = ImageIO.createImageOutputStream(baos);
            ImageIO.write(image, format, ios);
        } catch (IOException e) {
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public static File toFile(BufferedImage image, String imagePath) {
        return toFile(image, DEFAULT_FORMAT, imagePath);
    }

    public static File toFile(BufferedImage image, String format,
            String imagePath) {
        return toFile(image, format, new File(imagePath));
    }

    public static File toFile(BufferedImage image, String format,
            File imageFile) {
        try {
            if (ImageIO.write(image, format, imageFile)) {
                return imageFile;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static void drawLogo(BufferedImage image, Image logo) {
        try {
            Graphics2D g = image.createGraphics();

            // 大头贴
            int width = image.getWidth() / 5;
            int height = image.getHeight() / 5;
            int x = (image.getWidth() - width) / 2;
            int y = (image.getHeight() - height) / 2;
            // 绘制图
            g.drawImage(logo, x, y, width, height, null);
            // 画边框
            g.setStroke(new BasicStroke(5));
            g.setColor(Color.WHITE);
            g.drawRect(x, y, width, height);

            g.dispose();
        } catch (Exception e) {
        }
    }

}
