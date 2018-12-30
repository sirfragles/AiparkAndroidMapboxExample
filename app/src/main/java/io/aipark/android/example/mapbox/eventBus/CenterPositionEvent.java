package io.aipark.android.example.mapbox.eventBus;

import io.aipark.android.example.mapbox.map.MapLatLng;

public class CenterPositionEvent {
    MapLatLng mapLatLng;

    public CenterPositionEvent(MapLatLng mapLatLng) {
        this.mapLatLng = mapLatLng;
    }

    public MapLatLng getMapLatLng() {
        return mapLatLng;
    }

    public void setMapLatLng(MapLatLng mapLatLng) {
        this.mapLatLng = mapLatLng;
    }
}
