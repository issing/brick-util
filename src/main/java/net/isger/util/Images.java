package net.isger.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    public static Image getImage(URL url) {
        return Toolkit.getDefaultToolkit().getImage(url);
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        // 加载所有像素
        BufferedImage result = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // 创建缓冲图像
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            result = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), Transparency.BITMASK);
        } catch (HeadlessException e) {
            e.printStackTrace();
        }
        if (result == null) {
            result = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        }
        // 绘制原图至缓冲
        Graphics g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    public static BufferedImage getBufferedImage(String imagePath) {
        return getBufferedImage(new File(imagePath));
    }

    public static BufferedImage getBufferedImage(File imageFile) {
        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            return null;
        }
    }

    public static InputStream getQrcodeStream(String content) {
        return toInputStream(getQrcodeImage(content));
    }

    public static BufferedImage getQrcodeImage(final String content) {
        return getQrcodeImage(content, null);
    }

    public static BufferedImage getQrcodeImage(String content, Image logo) {
        return getQrcodeImage(content, DEFAULT_WIDTH, DEFAULT_HEIGHT, logo);
    }

    public static BufferedImage getQrcodeImage(String content, int width, int height, Image logo) {
        return getQrcodeImage(content, width, height, ErrorCorrectionLevel.M, logo);
    }

    public static BufferedImage getQrcodeImage(String content, int width, int height, ErrorCorrectionLevel level, Image logo) {
        BufferedImage image = toBufferedImage(createMatrix(content, width, height, level, BarcodeFormat.QR_CODE));
        if (logo != null) {
            drawLogo(image, logo);
        }
        return image;
    }

    public static BitMatrix createMatrix(String content, int width, int height, ErrorCorrectionLevel level, BarcodeFormat format) {
        HashMap<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, level);
        BitMatrix matrix = null;
        try {
            matrix = new MultiFormatWriter().encode(content, format, width, height, hints);
        } catch (WriterException e) {
        }
        return matrix;
    }

    public static BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
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

    public static InputStream toInputStream(RenderedImage image, String format) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = null;
        try {
            ios = ImageIO.createImageOutputStream(baos);
            ImageIO.write(image, format, ios);
        } catch (IOException e) {
        } finally {
            Files.close(ios);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public static File toFile(RenderedImage image, String imagePath) {
        return toFile(image, DEFAULT_FORMAT, imagePath);
    }

    public static File toFile(RenderedImage image, String format, String imagePath) {
        return toFile(image, format, new File(imagePath));
    }

    public static File toFile(RenderedImage image, String format, File imageFile) {
        try {
            if (ImageIO.write(image, format, imageFile)) {
                return imageFile;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static void drawLogo(BufferedImage image, Image logo) {
        Graphics2D g = image.createGraphics();
        try {
            // 尺寸
            int width = image.getWidth() / 5;
            int height = image.getHeight() / 5;
            int x = (image.getWidth() - width) / 2;
            int y = (image.getHeight() - height) / 2;
            // 绘制图
            g.drawImage(logo, x, y, width, height, null);
            // 画边框
            g.setStroke(new BasicStroke(3));
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, y, width, height, 3, 3);
        } catch (Exception e) {
        } finally {
            g.dispose();
        }
    }

}
