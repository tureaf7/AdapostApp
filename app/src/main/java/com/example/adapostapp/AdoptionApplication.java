package com.example.adapostapp;

import com.google.firebase.firestore.ServerTimestamp;

import com.google.firebase.Timestamp;
import java.util.Date;

public class AdoptionApplication {
    private String applicationId;
    private String userId;
    private String animalId;
    @ServerTimestamp
    private Date applicationDate;
    private Date dateAnswer;
    private String phoneNumber;
    private String address;
    private String previousPetsDetails;
    private String petSpecie;
    private String petAge;
    private String petTemperament;
    private String careOfAnimal;
    private String vacationPlan;
    private String healthBehaviorIssues;
    private String messageToShelter;
    private boolean havePetsBefore;
    private boolean haveOtherPets;
    private boolean adoptedBefore;
    private String livingEnvironment;
    private String rentOrOwn;
    private boolean ownerPermission;
    private boolean allergicFamilyMember;
    private String status;
    private String adminId;
    private String details;

    // Constructor implicit necesar pentru Firestore
    public AdoptionApplication() {}

    // Constructor cu parametri
    public AdoptionApplication(String userId, String animalId, String phoneNumber, String address,
                               String previousPetsDetails, String petSpecie, String petAge, String petTemperament,
                               String careOfAnimal, String vacationPlan, String healthBehaviorIssues,
                               String messageToShelter, boolean havePetsBefore, boolean haveOtherPets,
                               boolean adoptedBefore, String livingEnvironment, String rentOrOwn,
                               Boolean ownerPermission, Boolean allergicFamilyMember, String status, String adminId, String details) {
        this.userId = userId;
        this.animalId = animalId;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.previousPetsDetails = previousPetsDetails;
        this.petSpecie = petSpecie;
        this.petAge = petAge;
        this.petTemperament = petTemperament;
        this.careOfAnimal = careOfAnimal;
        this.vacationPlan = vacationPlan;
        this.healthBehaviorIssues = healthBehaviorIssues;
        this.messageToShelter = messageToShelter;
        this.havePetsBefore = havePetsBefore;
        this.haveOtherPets = haveOtherPets;
        this.adoptedBefore = adoptedBefore;
        this.livingEnvironment = livingEnvironment;
        this.rentOrOwn = rentOrOwn;
        this.ownerPermission = ownerPermission;
        this.allergicFamilyMember = allergicFamilyMember;
        this.status = status;
        this.adminId = adminId;
        this.details = details;
    }

    // Getters și Setters
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAnimalId() { return animalId; }
    public void setAnimalId(String animalId) { this.animalId = animalId; }

    public Date getApplicationDate() { return applicationDate; }
    public void setApplicationDate(Date timestamp) { this.applicationDate = timestamp; }

    public Date getDateAnswer() { return dateAnswer; }
    public void setDateAnswer(Date dateAnswer) { this.dateAnswer = dateAnswer; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPreviousPetsDetails() { return previousPetsDetails; }
    public void setPreviousPetsDetails(String previousPetsDetails) { this.previousPetsDetails = previousPetsDetails; }

    public String getPetSpecie() { return petSpecie; }
    public void setPetSpecie(String petSpecie) { this.petSpecie = petSpecie; }

    public String getPetAge() { return petAge; }
    public void setPetAge(String petAge) { this.petAge = petAge; }

    public String getPetTemperament() { return petTemperament; }
    public void setPetTemperament(String petTemperament) { this.petTemperament = petTemperament; }

    public String getCareOfAnimal() { return careOfAnimal; }
    public void setCareOfAnimal(String careOfAnimal) { this.careOfAnimal = careOfAnimal; }

    public String getVacationPlan() { return vacationPlan; }
    public void setVacationPlan(String vacationPlan) { this.vacationPlan = vacationPlan; }

    public String getHealthBehaviorIssues() { return healthBehaviorIssues; }
    public void setHealthBehaviorIssues(String healthBehaviorIssues) { this.healthBehaviorIssues = healthBehaviorIssues; }

    public String getMessageToShelter() { return messageToShelter; }
    public void setMessageToShelter(String messageToShelter) { this.messageToShelter = messageToShelter; }

    public boolean isHavePetsBefore() { return havePetsBefore; }
    public void setHavePetsBefore(boolean havePetsBefore) { this.havePetsBefore = havePetsBefore; }

    public boolean isHaveOtherPets() { return haveOtherPets; }
    public void setHaveOtherPets(boolean haveOtherPets) { this.haveOtherPets = haveOtherPets; }

    public boolean isAdoptedBefore() { return adoptedBefore; }
    public void setAdoptedBefore(boolean adoptedBefore) { this.adoptedBefore = adoptedBefore; }

    public String getLivingEnvironment() { return livingEnvironment; }
    public void setLivingEnvironment(String livingEnvironment) { this.livingEnvironment = livingEnvironment; }

    public String getRentOrOwn() { return rentOrOwn; }
    public void setRentOrOwn(String rentOrOwn) { this.rentOrOwn = rentOrOwn; }

    public Boolean getOwnerPermission() { return ownerPermission; }
    public void setOwnerPermission(Boolean ownerPermission) { this.ownerPermission = ownerPermission; }

    public Boolean getAllergicFamilyMember() { return allergicFamilyMember; }
    public void setAllergicFamilyMember(Boolean allergicFamilyMember) { this.allergicFamilyMember = allergicFamilyMember; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminName) { this.adminId = adminName; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
