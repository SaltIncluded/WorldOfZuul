package worldofzuul.world;

import worldofzuul.item.*;
import worldofzuul.parsing.Command;
import worldofzuul.parsing.CommandWord;

public class Field extends GameObject {
    private Fertilizer fertilizer;

    public Field() {
    }

    private float water;
    private Plant plant;
    private Float nutrition;

    public Field(Fertilizer fertilizer) {
        this.fertilizer = fertilizer;
    }

    public Field(Fertilizer fertilizer, float water) {
        this(fertilizer);
        this.water = water;
    }

    public Fertilizer getFertilizer() { return this.fertilizer; }

    public void setFertilizer(Fertilizer fertilizer) { this.fertilizer = fertilizer; }

    public float getWater() { return this.water; }

    public void setWater(float water) { this.water = water; }

    public Plant getPlant() { return this.plant; }

    public void setPlant(Plant plant) { this.plant = plant; }

    public float getNutrition() { return this.nutrition; }

    public void setNutrition(float nutrition) { this.nutrition = nutrition; }

    @Override
    public Command[] interact() {
        System.out.println("What are you trying to do?");
        return super.interact();
    }

    @Override
    public Command[] interact(Item item) {

        if(item instanceof Fertilizer){
            System.out.println("You used a fertilizer.");
            return useFertilizer((Fertilizer) item);
        }
        else if(item instanceof Seed){
            System.out.println("You used a seed on the field.");
            return useSeed((Seed) item);
        }
        else if(item instanceof Harvester){
            System.out.println("You tried to harvest this field.");
            return useHarvester((Harvester) item);
        }
        else {
            System.out.println("You can't use that item here.");
        }


        return super.interact(item);
    }

    private Command[] useFertilizer(Fertilizer item){
        return null; //TODO: Implement method.
    }
    private Command[] useSeed(Seed item){
        Command[] commands = new Command[1];
        if(item.getSeedCount() > 0){
            setPlant(item.getPlant());
        } else {
            commands[0] = new Command(CommandWord.REMOVEITEM, null);
        }


        return commands;
    }
    private Command[] useHarvester(Harvester item){
        return null; //TODO: Implement method.
    }



}
