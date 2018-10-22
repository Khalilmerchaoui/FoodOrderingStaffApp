package app.taxipizzastaff.models;

import java.io.Serializable;

/**
 * Created by user on 05/03/2018.
 */

public class Sorted implements Serializable{
    private String key;
    private Float distance;
    private String deliveryLocation;

    public Sorted() {
    }

    public Sorted(String key, Float distance, String deliveryLocation) {
        this.key = key;
        this.distance = distance;
        this.deliveryLocation = deliveryLocation;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }
}
