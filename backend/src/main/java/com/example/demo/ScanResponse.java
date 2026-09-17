package com.example.demo;

public class ScanResponse {
    private int matchScore;
    private String feedback;

    public ScanResponse(int matchScore, String feedback) {
        this.matchScore = matchScore;
        this.feedback = feedback;
    }

    public int getMatchScore() { return matchScore; }
    public String getFeedback() { return feedback; }
}