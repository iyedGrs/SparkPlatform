package com.spark.platform.models;

import java.sql.Timestamp;

public class Material {
    private int materialId;
    private Integer courseId;
    private Integer uploadedBy;
    private Integer parentId;
    private String type; // PDF, MIND_MAP, QUIZ, FLASHCARD, SLIDE, VIDEO, AUDIO
    private String title;
    private String filePath;
    private String content;
    private Integer pageCount;
    private Integer questionCount;
    private Integer cardCount;
    private String topic;
    private String visibility;
    private String status;
    private Timestamp createdAt;

    public Material() {}

    public int getMaterialId() { return materialId; }
    public void setMaterialId(int materialId) { this.materialId = materialId; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public Integer getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Integer uploadedBy) { this.uploadedBy = uploadedBy; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public Integer getCardCount() { return cardCount; }
    public void setCardCount(Integer cardCount) { this.cardCount = cardCount; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Material{" + materialId + ", " + type + ", " + title + "}";
    }
}