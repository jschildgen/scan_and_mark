package org.example.model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Page implements Comparable<Page> {
    private Path path;

    public Page(Path path) {
        this.path = path;
    }

    public Image getImage() throws IOException {
        InputStream stream = Files.newInputStream(this.path);
        return new Image(stream);
    }

    public Image getAnswerImage(Exercise exercise) throws IOException {
        Image fullPageImage = this.getImage();
        double[][] pos = exercise.getPos();
        int x = (int) Math.round(pos[0][0]);
        int y = (int) Math.round(pos[0][1]);
        int width = (int) Math.round(pos[1][0]) - x;
        int height = (int) Math.round(pos[1][1]) - y;

        PixelReader reader = fullPageImage.getPixelReader();
        return new WritableImage(reader, x, y, width, height);
    }

    @Override
    public String toString() {
        return this.getPageNo();
    }

    /**
     *
     * @return page filename withot extension
     */
    public String getPageNo() {
        return this.path.getFileName().toString().replaceFirst("[.][^.]+$", "");
    }

    @Override
    public int compareTo(Page that) {
        try {
            int thisId = Integer.parseInt(that.path.getFileName().toString());
            int thatId = Integer.parseInt(that.path.getFileName().toString());
            return thisId - thatId;
        } catch (Exception e) {
            return this.path.getFileName().toString().compareTo(that.path.getFileName().toString());
        }
    }
}
