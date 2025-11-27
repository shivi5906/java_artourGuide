package com.artourguide;

import javafx.scene.image.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

public class Framecapture {

   
    public static Image mat2Image(Mat frame) {

        int width = frame.width();
        int height = frame.height();

        int channels = frame.channels(); // should be 3

        byte[] bgr = new byte[width * height * channels];
        frame.get(0, 0, bgr);

        byte[] bgra = new byte[width * height * 4];

        int j = 0;
        for (int i = 0; i < bgr.length; i += 3) {
            bgra[j++] = bgr[i];     // B
            bgra[j++] = bgr[i + 1]; // G
            bgra[j++] = bgr[i + 2]; // R
            bgra[j++] = (byte) 255; // A = 255
        }

        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();

        pw.setPixels(
                0, 0, width, height,
                PixelFormat.getByteBgraInstance(),
                bgra, 0, width * 4
        );

        return img;
    }

    public static void saveFrame(Mat frame, String filename) {
        Imgcodecs.imwrite(filename, frame);
        System.out.println("Captured frame: " + filename);
    }
}
