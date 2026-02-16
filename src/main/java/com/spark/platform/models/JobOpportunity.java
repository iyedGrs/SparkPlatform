package com.spark.platform.models;

import java.sql.Date;
import java.sql.Timestamp;

public class JobOpportunity {
    private int jobId;
    private String title;
    private String company;
    private String description;
    private String location;
    private String specialization;
    private String type;
    private String requiredSkillsJson;
    private String salaryRange;
    private Date deadline;
    private Integer postedBy;
    private String source;
    private String externalUrl;
    private String status;
    private Timestamp createdAt;

    public JobOpportunity() {}

    public int getJobId() { return jobId; }
    public void setJobId(int jobId) { this.jobId = jobId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRequiredSkillsJson() { return requiredSkillsJson; }
    public void setRequiredSkillsJson(String requiredSkillsJson) { this.requiredSkillsJson = requiredSkillsJson; }

    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public Integer getPostedBy() { return postedBy; }
    public void setPostedBy(Integer postedBy) { this.postedBy = postedBy; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "JobOpportunity{" + jobId + ", " + title + " at " + company + "}";
    }
}