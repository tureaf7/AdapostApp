package com.example.adapostapp;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.Map;

public class VolunteerApplications {
    private String phoneNumber, motivation, userId, status, id, experienceDetails, adminId, details;
    private Map <String, String> availability;
    @ServerTimestamp
    private Date submittedAt;
    private Date dateAnswer;
    private boolean experience;

    public VolunteerApplications() {}
    public VolunteerApplications(String phoneNumber, String motivation, String userId, Map<String, String> availability,
                                 String status, boolean experience, String experienceDetails, String adminId, String details, Date dateAnswer) {
        this.phoneNumber = phoneNumber;
        this.motivation = motivation;
        this.userId = userId;
        this.availability = availability;
        this.status = status;
        this.experience = experience;
        this.experienceDetails = experienceDetails;
        this.adminId = adminId;
        this.details = details;
        this.dateAnswer = dateAnswer;
    }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setMotivation(String motivation) { this.motivation = motivation; }
    public String getMotivation() { return motivation; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }
    public void setAvailability(Map<String, String> availability) { this.availability = availability; }
    public Map<String, String> getAvailability() { return availability; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }
    public Date getSubmittedAt() { return submittedAt; }
    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public void setExperience(boolean experience) { this.experience = experience; }
    public boolean getExperience() { return experience; }
    public void setExperienceDetails(String experienceDetails) { this.experienceDetails = experienceDetails; }
    public String getExperienceDetails() { return experienceDetails; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public String getAdminId() { return adminId; }
    public void setDetails(String details) { this.details = details; }
    public String getDetails() { return details; }
    public void setDateAnswer(Date dateAnswer) { this.dateAnswer = dateAnswer; }
    public Date getDateAnswer() { return dateAnswer; }

}
