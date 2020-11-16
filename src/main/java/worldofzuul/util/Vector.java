package worldofzuul.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Vector {
    private static final String delimiter = ",";
    private int x = 0;
    private int y = 0;
    private StringProperty vectorValue = new SimpleStringProperty();

    public Vector() {
        setVectorValue(this.toString());
    }
    public Vector(int x, int y) {
        this();
        setX(x);
        setY(y);
    }
    public Vector(String s) {
        this();
        if(s.contains(delimiter)){
            String[] vals = s.split(delimiter);

            if(vals.length == 2){
                setX(Math.tryParse(vals[0], 0));
                setY(Math.tryParse(vals[1], 0));
            }
        }

    }
    public Vector(Vector oldVector, Vector newVector){
        this();

        setX(newVector.getX() - oldVector.getX());
        setY(newVector.getY() - oldVector.getY());
    }



    @JsonIgnore
    public final String getVectorValue(){return vectorValue.get();}

    public final void setVectorValue(String value){vectorValue.set(value);}

    @JsonIgnore
    public StringProperty vectorValueProperty() {return vectorValue;}


    @Override
    public String toString() {
        return x + delimiter + y;
    }



    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
        setVectorValue(this.toString());
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
        setVectorValue(this.toString());
    }
}
