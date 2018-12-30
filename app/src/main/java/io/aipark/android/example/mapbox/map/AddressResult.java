package io.aipark.android.example.mapbox.map;

import com.google.maps.model.LatLng;

public class AddressResult {
    String description;
    String placeId;
    LatLng coordinate;
    AddressResult.Source source;
    private static String DELIMITER = "%&/12345";

    public AddressResult(String description, String placeId) {
        this.description = description;
        this.placeId = placeId;
        this.source = AddressResult.Source.SERVER;
    }

    public AddressResult(String fromString) {
        String[] splittedString = fromString.split(DELIMITER);
        this.description = splittedString[0];
        this.placeId = splittedString[1];
        this.coordinate = new LatLng(Double.valueOf(splittedString[2]), Double.valueOf(splittedString[3]));
        this.source = AddressResult.Source.SERVER;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlaceId() {
        return this.placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public LatLng getCoordinate() {
        return this.coordinate;
    }

    public void setCoordinate(LatLng coordinate) {
        this.coordinate = coordinate;
    }

    public AddressResult.Source getSource() {
        return this.source;
    }

    public void setSource(AddressResult.Source source) {
        this.source = source;
    }

    public boolean equals(Object o) {
        AddressResult other = (AddressResult) o;
        return this.description.equals(other.getDescription());
    }

    public String toString() {
        return this.description + DELIMITER + this.placeId + DELIMITER + this.coordinate.lat + DELIMITER + this.coordinate.lng;
    }

    public static enum Source {
        SERVER,
        Cache;

        private Source() {
        }
    }
}
