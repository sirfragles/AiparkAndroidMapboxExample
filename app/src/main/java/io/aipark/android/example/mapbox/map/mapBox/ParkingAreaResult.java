package io.aipark.android.example.mapbox.map.mapBox;

import de.aipark.api.occupancy.Occupancy;
import de.aipark.api.parkingarea.ParkingArea;

public class ParkingAreaResult {
    private ParkingArea data;
    private Occupancy occupancy;

    public ParkingAreaResult(ParkingArea data,
                             Occupancy occupancy) {
        this.data = data;
        this.occupancy = occupancy;
    }

    public ParkingArea getData() {
        return data;
    }

    public void setData(ParkingArea data) {
        this.data = data;
    }

    public Occupancy getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(Occupancy occupancy) {
        this.occupancy = occupancy;
    }

    @Override
    public String toString() {
        return "ParkingAreaResult{" +
                "data=" + data +
                ", occupancy=" + occupancy +
                '}';
    }
}
