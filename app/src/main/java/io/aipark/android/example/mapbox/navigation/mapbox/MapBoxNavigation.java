package io.aipark.android.example.mapbox.navigation.mapbox;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.maps.model.LatLng;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;

import de.aipark.api.route.TrafficMode;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.map.mapBox.AiparkNavigationActivity;
import io.aipark.android.example.mapbox.navigation.Navigation;
import io.aipark.android.example.mapbox.navigation.OnArrivalListener;

/**
 * mapbox navigation implementation
 */

public class MapBoxNavigation extends Navigation {
    private static final String TAG = "MapBoxNavigation";

    public MapBoxNavigation() {
    }

    @Override
    public void startNavigation(LatLng origin, LatLng destination, Context context, TrafficMode trafficMode, final OnArrivalListener onArrivalListener) {
        String directionsCriteria = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC;
        if (trafficMode.equals(TrafficMode.WALKING)) {
            directionsCriteria = DirectionsCriteria.PROFILE_WALKING;
        }

        AiparkNavigationActivity.init(Point.fromLngLat(origin.lng, origin.lat), Point.fromLngLat(destination.lng, destination.lat), new NavigationListener() {
            boolean callback = false;

            @Override
            public void onCancelNavigation() {
                if (!callback) {
                    onArrivalListener.onDestinationArrived();
                    callback = true;
                }
            }

            @Override
            public void onNavigationFinished() {
                if (!callback) {
                    onArrivalListener.onDestinationArrived();
                    callback = true;
                }
            }

            @Override
            public void onNavigationRunning() {
            }
        }, directionsCriteria);

        try {
            Intent intent = new Intent(AiParkApp.getActivity(), AiparkNavigationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            AiParkApp.getContext().startActivity(intent);
        } catch (Exception e) {
            Log.e("mapboxProfile", Log.getStackTraceString(e));
        }
    }
}
