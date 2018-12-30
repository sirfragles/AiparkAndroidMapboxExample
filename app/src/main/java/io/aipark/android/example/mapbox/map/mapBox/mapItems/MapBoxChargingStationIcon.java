package io.aipark.android.example.mapbox.map.mapBox.mapItems;

import android.support.v4.content.ContextCompat;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import de.aipark.api.chargingstation.ChargingStation;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;

public class MapBoxChargingStationIcon {
    private final static float ALPHA_NOT_SELECTED_MARKER = 0.5f;
    private final static float ALPHA_SELECTED_MARKER = 1f;

    private MapboxMap mMap;
    private Marker marker;

    private ChargingStation chargingStation;

    private boolean selected;
    private boolean visible;

    public MapBoxChargingStationIcon(MapboxMap mMap, ChargingStation chargingStation) {
        this.mMap = mMap;
        this.chargingStation = chargingStation;
        this.visible = true;
        addChargingStationToMap();
    }

    public void addChargingStationToMap() {
        if (marker == null) {
            marker = mMap.addMarker(new MarkerOptions().position(new LatLng(chargingStation.getPosition().getY(), chargingStation.getPosition().getX())).title(chargingStation.getName()));
            //marker.setAlpha(ALPHA_NOT_SELECTED_MARKER);
            marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_battery));
        }
    }

    public ChargingStation getChargingStation() {
        return chargingStation;
    }

    public Marker getMarker() {
        return marker;
    }

    public void remove() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            if (marker != null) {
                marker.setIcon(IconHelper.tintIcon(marker.getIcon(), ContextCompat.getColor(AiParkApp.getContext(), R.color.whiteTransparentMarker)));
            }
        } else {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_battery));
            }
        }
    }

    synchronized public void setVisibility(boolean visible) {
        if (this.visible && !visible) {
            remove();
            this.visible = false;
        }
        if (!this.visible && visible) {
            addChargingStationToMap();
            this.visible = true;
        }
    }
}
