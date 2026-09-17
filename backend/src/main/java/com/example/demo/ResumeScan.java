package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_scans")
public class ResumeScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String resumeText;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String jobDescription;

    private int matchScore;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String feedback;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ResumeScan() {}

    public ResumeScan(String resumeText, String jobDescription, int matchScore, String feedback) {
        this.resumeText = resumeText;
        this.jobDescription = jobDescription;
        this.matchScore = matchScore;
        this.feedback = feedback;
    }

    public Long getId() { return id; }
    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }
    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}