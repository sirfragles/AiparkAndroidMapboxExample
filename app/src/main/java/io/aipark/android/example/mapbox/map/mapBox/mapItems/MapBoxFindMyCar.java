package io.aipark.android.example.mapbox.map.mapBox.mapItems;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import de.aipark.android.sdk.ParkingPosition;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;

public class MapBoxFindMyCar {
    private MapboxMap mMap;
    private Marker marker;
    private ParkingPosition position;

    public MapBoxFindMyCar(ParkingPosition position, MapboxMap mMap) {
        this.position = position;
        this.mMap = mMap;
        marker = mMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(position.getPosition().getY()
                                , position.getPosition().getX()))
                        .title(AiParkApp.getContext().getString(R.string.my_car_parking_position)));
        marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_find_my_car));
    }

    public void onClick() {
        //FindMyCarPopup findMyCarPopup = new FindMyCarPopup(position);
    }

    public Marker getMarker() {
        return marker;
    }

    public void remove() {
        if (marker != null) {
            marker.remove();
        }
    }
}
