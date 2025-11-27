package com.artourguide;

import com.google.gson.*;
import java.io.File;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class GeminiVisionAPI {

    private static final String API_KEY = "your_api_key_here";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=";

    
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    /**
     * Describe the content of an image asynchronously using Gemini Vision.
     * @param path Path to saved image file (JPEG)
     * @return CompletableFuture<String> with Gemini’s textual description
     */
    public static CompletableFuture<String> describeImageAsync(String path) {
        try {
            byte[] imgBytes = Files.readAllBytes(new File(path).toPath());
            String base64 = Base64.getEncoder().encodeToString(imgBytes);

            // --- Build request body JSON ---
            var textPart = new JsonObject();
            textPart.addProperty("text",
                    "Identify the main object or landmark in this image and describe it briefly.");

            var inlineData = new JsonObject();
            inlineData.addProperty("mime_type", "image/jpeg");
            inlineData.addProperty("data", base64);

            var imagePart = new JsonObject();
            imagePart.add("inline_data", inlineData);

            var parts = new JsonArray();
            parts.add(textPart);
            parts.add(imagePart);

            var content = new JsonObject();
            content.add("parts", parts);

            var contents = new JsonArray();
            contents.add(content);

            var body = new JsonObject();
            body.add("contents", contents);

          
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + API_KEY))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

         
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(GeminiVisionAPI::parseGeminiResponse)
                    .exceptionally(ex -> "Error contacting Gemini: " + ex.getMessage());

        } catch (Exception e) {
            return CompletableFuture.completedFuture("Error reading image: " + e.getMessage());
        }
    }

  
    private static String parseGeminiResponse(String jsonResponse) {
        try {
            var json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            return json.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            return "Unable to clearly identify the object.";
        }
    }
}
