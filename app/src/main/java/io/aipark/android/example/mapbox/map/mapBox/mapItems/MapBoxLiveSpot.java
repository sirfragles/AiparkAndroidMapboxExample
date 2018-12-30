package io.aipark.android.example.mapbox.map.mapBox.mapItems;

import android.util.Log;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import de.aipark.api.livedata.spot.LiveSpot;
import de.aipark.api.livedata.spot.LiveSpotStatus;
import de.aipark.api.livedata.spot.ParkingSpotType;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;

public class MapBoxLiveSpot {
    private static double ICON_BIG_ZOOM_LEVEL = 16.9;
    private MapboxMap mMap;
    private Marker marker;
    private LiveSpot liveSpot;
    private Double zoom;
    private Boolean smallIcon;

    public MapBoxLiveSpot(MapboxMap mMap, LiveSpot liveSpot, double zoom) {
        Log.d("liveSpot", "create");
        this.mMap = mMap;
        this.liveSpot = liveSpot;
        this.zoom = zoom;
        render();
    }

    public void setLiveSpotStatus(LiveSpotStatus liveSpotStatus, double zoom) {
        Log.d("liveSpot", "update");
        liveSpot.setOccupied(liveSpotStatus);
        this.zoom = zoom;
        render();
    }

    public void render() {
        if (liveSpot.getType().equals(ParkingSpotType.NORMAL)) {
            if (!liveSpot.getOccupied().equals(LiveSpotStatus.U)) {
                if (marker == null) {
                    marker = mMap.addMarker(new MarkerOptions().position(new LatLng(liveSpot.getPosition().getY(), liveSpot.getPosition().getX())).title(""));
                }
                Log.d("liveSpot", "" + liveSpot);
                Log.d("liveSpot", "zoom: " + zoom + " smallIcon: " + smallIcon);
                if (liveSpot.getOccupied().equals(LiveSpotStatus.F)) {
                    //ic_live_spot_free
                    if (zoom > ICON_BIG_ZOOM_LEVEL && (smallIcon == null || smallIcon == true)) {
                        Log.d("liveSpot", "draw icon 1");
                        marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_spot_free_1));
                        smallIcon = false;
                    } else if (zoom <= ICON_BIG_ZOOM_LEVEL && (smallIcon == null || smallIcon == false)) {
                        Log.d("liveSpot", "draw icon 2");
                        marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.spot_free_small));
                        smallIcon = true;
                    }
                } else if (liveSpot.getOccupied().equals(LiveSpotStatus.O)) {
                    //ic_live_spot_occupied
                    if (zoom > ICON_BIG_ZOOM_LEVEL && (smallIcon == null || smallIcon == true)) {
                        Log.d("liveSpot", "draw icon 3");
                        marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.spot_occupied));
                        smallIcon = false;
                    } else if (zoom <= ICON_BIG_ZOOM_LEVEL && (smallIcon == null || smallIcon == false)) {
                        Log.d("liveSpot", "draw icon 4");
                        marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.spot_occupied_small));
                        smallIcon = true;
                    }
                }
            }
        }
    }

    public void remove() {
        if (marker != null) {
            marker.remove();
        }
    }

    public void refresh(double zoom) {
        this.zoom = zoom;
        render();
    }
}
