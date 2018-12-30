package io.aipark.android.example.mapbox.map.mapBox.mapItems;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import de.aipark.api.parkevent.ParkEventLiveLeaving;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;

public class MapBoxLiveEventIcon {
    public static final long PARK_EVENT_CURRENT_TIME_IN_MINUTES = 15;
    private final static float ALPHA_NOT_SELECTED_MARKER = 1f;
    private final static float ALPHA_SELECTED_MARKER = 1f;
    private boolean isCurrent;

    private MapboxMap mMap;
    private Marker marker;
    private ParkEventLiveLeaving parkEventLiveLeaving;
    private boolean visible;

    public MapBoxLiveEventIcon(ParkEventLiveLeaving parkEventLiveLeaving, MapboxMap mMap) {
        this.parkEventLiveLeaving = parkEventLiveLeaving;
        this.mMap = mMap;
        isCurrent = true;
        this.visible = true;
        refresh();
    }

    public void refresh() {
        if (isVisible()) {
            int maturityInMinutes = (int) ((double) System.currentTimeMillis() - parkEventLiveLeaving.getTimestamp().getTime()) / 1000 / 60;
            //Log.i("maturityInMinutes","" + maturityInMinutes + " : " + parkEventLiveLeaving);
            if (marker == null) {
                marker = mMap.addMarker(new MarkerOptions().position(new LatLng(parkEventLiveLeaving.getPoint().getY(), parkEventLiveLeaving.getPoint().getX())).title(getIconTitle(maturityInMinutes)));
                //marker.setAlpha(ALPHA_NOT_SELECTED_MARKER);
            }
            marker.setTitle(getIconTitle(maturityInMinutes));
            if (maturityInMinutes < 2) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_1));
            } else if (maturityInMinutes < 3) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_2));
            } else if (maturityInMinutes < 4) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_3));
            } else if (maturityInMinutes < 5) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_4));
            } else if (maturityInMinutes < 6) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_5));
            } else if (maturityInMinutes < 7) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_6));
            } else if (maturityInMinutes < 8) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_7));
            } else if (maturityInMinutes < 9) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_8));
            } else if (maturityInMinutes < 10) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_9));
            } else if (maturityInMinutes < 11) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_10));
            } else if (maturityInMinutes < 12) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_11));
            } else if (maturityInMinutes < 13) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_12));
            } else if (maturityInMinutes < 14) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_13));
            } else if (maturityInMinutes < 15) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_14));
            } else if (maturityInMinutes < 16) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_15));
            } else {
                /////////// debug ////////////
                //marker.setYesIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_leaving_5));
                marker.remove();
                isCurrent = false;
            }
        } else {
            remove();
        }
    }

    private String getIconTitle(int maturityInMinutes) {
        return AiParkApp.getActivity().getResources().getString(R.string.leavingParkingSpace) + " " + maturityInMinutes + " " + AiParkApp.getContext().getString(R.string.live_event_tooltip_postfix);
    }

    public boolean isCurrent() {
        return isCurrent;
    }


    public ParkEventLiveLeaving getParkEventLiveLeaving() {
        return parkEventLiveLeaving;
    }

    public void remove() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }

    synchronized public void setVisibility(boolean visible) {
        if (this.visible && !visible) {
            remove();
            this.visible = false;
        }
        if (!this.visible && visible) {
            refresh();
            this.visible = true;
        }
    }

    public boolean isVisible() {
        return visible;
    }
}
