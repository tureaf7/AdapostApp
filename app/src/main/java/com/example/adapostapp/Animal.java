package com.example.adapostapp;

import com.google.firebase.Timestamp;

public class Animal {
    private String Id;
    private String Name;
    private String Gen;
    private String Species;
    private String Breed;
    private int Years;
    private int Months;
    private Timestamp ArrivalDate;
    private boolean Adopted;
    private String Photo;
    private String Description;
    private String Color;
    private boolean isSterilized;
    private boolean isVaccinated;

    public Animal() {
        // Constructor necesar pentru Firebase
    }

    public Animal(String name, String gen, String speciesSelected, boolean sterilized, boolean vaccinated, String color, String description, String breed, int years, int months , Timestamp arrivalDate, boolean adopted, String imageUrl) {
        this.Name = name;
        this.Gen = gen;
        this.Species = speciesSelected;
        this.isSterilized = sterilized;
        this.isVaccinated = vaccinated;
        this.Color = color;
        this.Description = description;
        this.Breed = breed;
        this.Years = years;
        this.Months = months;
        this.ArrivalDate = arrivalDate;
        this.Adopted = adopted;
        this.Photo = imageUrl;
    }

    // Getteri și setteri
    public String getId() { return Id; }
    public void setId(String id) { Id = id; }

    public String getName() { return Name; }
    public void setName(String name) { Name = name; }

    public String getGen() { return Gen; }
    public void setGen(String gen) { Gen = gen; }

    public String getSpecies() { return Species; }
    public void setSpecies(String species) { Species = species; }

    public String getBreed() { return Breed; }
    public void setBreed(String breed) { Breed = breed; }

    public int getYears() { return Years; }
    public void setYears(int years) { Years = years; }

    public int getMonths() { return Months; }
    public void setMonths(int months) { Months = months; }

    public Timestamp getArrivalDate() { return ArrivalDate; }
    public void setArrivalDate(Timestamp arrivalDate) { ArrivalDate = arrivalDate; }

    public boolean isAdopted() { return Adopted; }
    public void setAdopted(boolean adopted) { Adopted = adopted; }

    public String getPhoto() { return Photo; }
    public void setPhoto(String photo) { Photo = photo; }

    public String getDescription() { return Description; }
    public void setDescription(String description) { Description = description; }

    public String getColor() { return Color; }
    public void setColor(String color) { Color = color; }

    public boolean isSterilized() { return isSterilized; }
    public void setSterilized(boolean sterilized) { isSterilized = sterilized; }

    public boolean isVaccinated() { return isVaccinated; }
    public void setVaccinated(boolean vaccinated) { isVaccinated = vaccinated; }
}
