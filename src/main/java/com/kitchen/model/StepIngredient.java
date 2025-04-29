package com.kitchen.model;

public class StepIngredient {
    private String name;
    

    public StepIngredient(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
       this.name = name;
    }


    @Override
    public String toString() {
        return name; 
    }
}