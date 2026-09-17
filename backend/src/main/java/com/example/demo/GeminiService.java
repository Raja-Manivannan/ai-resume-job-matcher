package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public ScanResponse evaluateMatch(String resume, String jobDescription) {
        String prompt = "You are an ATS resume evaluator. Analyze the following resume against the job description.\\n\\n" +
                "RESUME:\\n" + resume.replace("\"", "\\\"").replace("\n", "\\n") + "\\n\\n" +
                "JOB DESCRIPTION:\\n" + jobDescription.replace("\"", "\\\"").replace("\n", "\\n") + "\\n\\n" +
                "Provide an ATS match percentage (0-100) on the first line strictly as 'MATCH_SCORE: XX%'. " +
                "Then provide detailed feedback on matching skills, missing skills, and suggestions.";

        String requestBody = "{\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}]}]}";

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + apiKey;        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int score = 75;
            String text = response.body();

            // Extract text from Gemini JSON response
            Pattern pattern = Pattern.compile("\"text\":\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                text = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"");
            }

            // Extract numeric score
            Pattern scorePattern = Pattern.compile("MATCH_SCORE:\\s*(\\d+)%");
            Matcher scoreMatcher = scorePattern.matcher(text);
            if (scoreMatcher.find()) {
                score = Integer.parseInt(scoreMatcher.group(1));
            }

            return new ScanResponse(score, text);
        } catch (Exception e) {
            return new ScanResponse(50, "Error generating AI review: " + e.getMessage());
        }
    }
}