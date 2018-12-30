package io.aipark.android.example.mapbox.eventBus;

import io.aipark.android.example.mapbox.map.mapBox.ParkingAreaResult;

public class ParkingAreaSelectedEvent {
    private ParkingAreaResult parkingAreaResult;

    public enum Status {
        STARTED, RESULT_LOADED
    }

    ;
    private Status status;

    public ParkingAreaSelectedEvent(ParkingAreaResult parkingAreaResult, Status status) {
        this.parkingAreaResult = parkingAreaResult;
        this.status = status;
    }

    public ParkingAreaResult getParkingAreaResult() {
        return parkingAreaResult;
    }

    public void setParkingAreaResult(ParkingAreaResult parkingAreaResult) {
        this.parkingAreaResult = parkingAreaResult;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
