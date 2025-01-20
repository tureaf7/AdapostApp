package com.example.adapostapp;

import java.util.stream.Stream;

public class Animal {
    private int Id;
    private String Name;
    private String Gen;
    private String Species;
    private String Breed;
    private int Age;
    private com.google.firebase.Timestamp ArrivalDate;
    private boolean Adopted;
    private String Photo;

    public Animal() {
        // Constructor necesar pentru Firebase
    }

    public Animal(int Id, String name, String gen, String species, String breed, int age, com.google.firebase.Timestamp arrivalDate, boolean adopted, String photo) {
        Id = Id;
        Name = name;
        Gen = gen;
        Species = species;
        Breed = breed;
        Age = age;
        ArrivalDate = arrivalDate;
        Adopted = adopted;
        Photo = photo;
    }

    // Getteri și setteri
    public int getId() { return Id; }
    public void setId(int id) { Id = id; }

    public String getName() { return Name; }
    public void setName(String name) { Name = name; }

    public String getGen() { return Gen; }
    public void setGen(String gen) { Gen = gen; }

    public String getSpecies() { return Species; }
    public void setSpecies(String species) { Species = species; }

    public String getBreed() { return Breed; }
    public void setBreed(String breed) { Breed = breed; }

    public int getAge() { return Age; }
    public void setAge(int age) { Age = age; }

    public com.google.firebase.Timestamp getArrivalDate() { return ArrivalDate; }
    public void setArrivalDate(com.google.firebase.Timestamp arrivalDate) { ArrivalDate = arrivalDate; }

    public boolean isAdopted() { return Adopted; }
    public void setAdopted(boolean adopted) { Adopted = adopted; }

    public String getPhoto() { return Photo; }
    public void setPhoto(String photo) { Photo = photo; }
}
