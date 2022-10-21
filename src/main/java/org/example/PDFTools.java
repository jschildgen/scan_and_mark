package org.example;

import com.google.zxing.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import javax.imageio.ImageIO;

public class PDFTools {
    public static void splitPDF(File pdffile, int numpages) throws IOException {
        PDDocument document = PDDocument.load(pdffile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int exam_id;
        int page_id;
        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            exam_id = page / numpages + 1;
            page_id = page % numpages + 1;
            Path folder = QRexam.base_dir.resolve(""+exam_id);
            if(!Files.exists(folder)) {
                Files.createDirectory(folder);
            }
            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
            double degree;

            try {
                degree = getQRAngle(bim);
            } catch (NotFoundException e) {
                degree = 0;
            }

            bim = rotateImage(bim, degree);

            Path path = folder.resolve(""+ page_id + ".jpg");
            ImageIOUtil.writeImage(bim, path.toString(), 300);
            System.out.printf("Create file %d/%d: %s (%.1f°)\n", page+1, document.getNumberOfPages(), path, degree);
        }
        document.close();
    }

    private static BufferedImage rotateImage(BufferedImage image, double degree) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage result = new BufferedImage(w, h, image.getType());
        Graphics2D g2 = result.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        g2.rotate(Math.toRadians(degree), w/2, h/2);
        g2.drawImage(image,null,0,0);
        return result;
    }

    private static double getQRAngle(BufferedImage image) throws NotFoundException {
        final String charset = "UTF-8"; // or "ISO-8859-1"
        final Map hintMap = new HashMap();
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(image)));

        Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap,
                hintMap);
        ResultPoint[] p = qrCodeResult.getResultPoints();

        double degree = Math.toDegrees(Math.atan( (p[0].getX()-p[1].getX()) / (p[0].getY() - p[1].getY())));
        double height = Math.sqrt( Math.pow(p[0].getX()-p[1].getX(),2) + Math.pow(p[0].getY()-p[1].getY(),2) );

        return degree;
    }
}
