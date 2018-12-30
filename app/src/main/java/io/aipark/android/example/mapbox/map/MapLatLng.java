package io.aipark.android.example.mapbox.map;

import com.google.maps.model.LatLng;

/**
 * abstract representation for latitude and longitude
 */

public class MapLatLng {
    public double latitude;
    public double longitude;

    public MapLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public MapLatLng(LatLng latLng) {
        this.latitude = latLng.lat;
        this.longitude = latLng.lng;
    }

    @Override
    public String toString() {
        return "MapLatLng{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
