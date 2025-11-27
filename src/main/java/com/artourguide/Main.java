package com.artourguide;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;

import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application {

    static {
        try {
            System.load("C:\\Users\\shiva\\opencv\\build\\java\\x64\\opencv_java4120.dll");
            System.out.println("OpenCV 4.12.0 loaded successfully!");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV DLL: " + e.getMessage());
        }
    }

    private VideoCapture camera;
    private ImageView imageView;
    private Text overlayText;

    @Override
    public void start(Stage stage) {
        imageView = new ImageView();
        overlayText = new Text("");
        overlayText.setFont(Font.font("Arial", 22));
        overlayText.setFill(Color.LIGHTGREEN);

        overlayText.setWrappingWidth(600);  
        overlayText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        StackPane root = new StackPane(imageView, overlayText);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("AR Tour Guide - Gemini Vision");
        stage.show();

        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.err.println("Camera not detected.");
            return;
        }

        new Thread(this::streamCamera).start();
    }
    private String cleanGeminiResponse(String text) {
    if (text == null) return "";

    text = text.replace("*", "")
               .replace("**", "")
               .replace("###", "")
               .replace("##", "")
               .replace("\\n", "\n");

    text = text.replaceAll(" +", " ").trim();
    return text;
    }

    private void streamCamera() {
        Mat frame = new Mat();
        long lastCapture = System.currentTimeMillis();
        AtomicBoolean isProcessing = new AtomicBoolean(false);

        while (camera.isOpened()) {

            if (camera.read(frame)) {

                if (frame.empty()) continue;

                // Display BGR → JavaFX ARGB
                Image fxImg = Framecapture.mat2Image(frame);
                Platform.runLater(() -> imageView.setImage(fxImg));

                // Every 4 seconds → capture
                if (!isProcessing.get() &&
                        (System.currentTimeMillis() - lastCapture > 4000)) {

                    isProcessing.set(true);
                    lastCapture = System.currentTimeMillis();

                    String filename = "capture.jpg";
                    Framecapture.saveFrame(frame, filename);

                    GeminiVisionAPI.describeImageAsync(filename)
                             .thenAccept(desc -> {
        String clean = cleanGeminiResponse(desc);
        Platform.runLater(() -> overlayText.setText(clean));
                                              })
                            .exceptionally(ex -> {
                                Platform.runLater(() ->
                                        overlayText.setText("Gemini error: " + ex.getMessage()));
                                return null;
                            })
                            .thenRun(() -> isProcessing.set(false));
                }
            }

            try { Thread.sleep(25); } catch (Exception ignored) {}
        }
    }

    @Override
    public void stop() {
        if (camera != null && camera.isOpened())
            camera.release();
        System.out.println("Camera released.");
    }

    public static void main(String[] args) {
        System.out.println("OpenCV version: " + Core.VERSION);
        launch(args);
    }
}
