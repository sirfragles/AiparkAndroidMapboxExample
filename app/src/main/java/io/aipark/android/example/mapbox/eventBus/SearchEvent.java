package io.aipark.android.example.mapbox.eventBus;

import java.sql.Timestamp;

import io.aipark.android.example.mapbox.map.AddressResult;

public class SearchEvent {
    private AddressResult addressResult;
    private AddressResult departureAdressResult;
    private boolean speakResult;
    private boolean animateCamera;
    private Timestamp departureTimestamp;
    private Timestamp arrivalTimestamp;

    public SearchEvent(AddressResult addressResult) {
        this.addressResult = addressResult;
        speakResult = false;
        animateCamera = false;
    }

    public SearchEvent(AddressResult addressResult, boolean speakResult) {
        this.addressResult = addressResult;
        this.speakResult = speakResult;
        this.animateCamera = false;
        departureTimestamp = new Timestamp(System.currentTimeMillis());
    }

    public SearchEvent(AddressResult addressResult, boolean speakResult, boolean animateCamera) {
        this.addressResult = addressResult;
        this.speakResult = speakResult;
        this.animateCamera = animateCamera;
        departureTimestamp = new Timestamp(System.currentTimeMillis());
    }

    public SearchEvent(AddressResult addressResult, AddressResult departureAdressResult) {
        this.addressResult = addressResult;
        this.departureAdressResult = departureAdressResult;

        speakResult = false;
        animateCamera = false;
        departureTimestamp = new Timestamp(System.currentTimeMillis());
    }

    public SearchEvent(AddressResult addressResult, AddressResult departureAdressResult, Timestamp departureTimestamp, Timestamp arrivalTimestamp) {
        this.addressResult = addressResult;
        this.departureAdressResult = departureAdressResult;
        this.departureTimestamp = departureTimestamp;
        this.arrivalTimestamp = arrivalTimestamp;

        speakResult = false;
        animateCamera = false;
    }

    public AddressResult getAddressResult() {
        return addressResult;
    }

    public void setAddressResult(AddressResult addressResult) {
        this.addressResult = addressResult;
    }

    public boolean isSpeakResult() {
        return speakResult;
    }

    public void setSpeakResult(boolean speakResult) {
        this.speakResult = speakResult;
    }

    public boolean isAnimateCamera() {
        return animateCamera;
    }

    public void setAnimateCamera(boolean animateCamera) {
        this.animateCamera = animateCamera;
    }

    public AddressResult getDepartureAdressResult() {
        return departureAdressResult;
    }

    public void setDepartureAdressResult(AddressResult departureAdressResult) {
        this.departureAdressResult = departureAdressResult;
    }

    @Override
    public String toString() {
        return "SearchEvent{" +
                "addressResult=" + addressResult +
                ", departureAdressResult=" + departureAdressResult +
                ", speakResult=" + speakResult +
                ", animateCamera=" + animateCamera +
                ", departureTimestamp=" + departureTimestamp +
                ", arrivalTimestamp=" + arrivalTimestamp +
                '}';
    }
}
