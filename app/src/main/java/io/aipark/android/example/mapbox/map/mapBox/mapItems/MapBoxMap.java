package io.aipark.android.example.mapbox.map.mapBox.mapItems;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.BuildConfig;
import io.aipark.android.example.mapbox.map.MapContext;
import io.aipark.android.example.mapbox.map.mapBox.AiparkMapBoxMapContainer;

public class MapBoxMap extends SupportMapFragment implements
        LocationEngineListener {
    public static final String ACCESS_TOKEN = BuildConfig.MY_MAPBOX_API_KEY;
    AiparkMapBoxMapContainer aiparkMapBoxMapContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(AiParkApp.getActivity(), ACCESS_TOKEN);
    }

    @Override
    public void onMapReady(final MapboxMap mapboxMap) {
        super.onMapReady(mapboxMap);
        enableLocationPlugin((MapView) getFragment().getView(), mapboxMap);
        aiparkMapBoxMapContainer = new AiparkMapBoxMapContainer(MapContext.MAP);
        aiparkMapBoxMapContainer.onMapReady(mapboxMap);
        AiParkApp.setMapFragement(aiparkMapBoxMapContainer);
    }

    public Fragment getFragment() {
        return this;
    }

    public void centerCurrentPosition() {
        if (aiparkMapBoxMapContainer != null) {
            aiparkMapBoxMapContainer.centerCurrentPosition();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (aiparkMapBoxMapContainer != null && aiparkMapBoxMapContainer.getPresenter() != null) {
            aiparkMapBoxMapContainer.getPresenter().onMapInit();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
    }

    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin(final MapView mapView, final MapboxMap mMap) {
        Log.i("location", "enableLocationPlugin");
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(AiParkApp.getActivity())) {
            // Create an instance of LOST location engine
            initializeLocationEngine();
            Log.i("location", "initializeLocationEngine");

            Log.i("location", "mapView " + mapView);
            locationPlugin = new LocationLayerPlugin(mapView, mMap, locationEngine);
            locationPlugin.setRenderMode(RenderMode.COMPASS);
        } else {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    enableLocationPlugin(mapView, mMap);
                }
            }, 1000L);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(AiParkApp.getActivity());
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            //setCameraPosition(new MapLatLng(lastLocation.getLatitude(),lastLocation.getLongitude()),DEFAULT_ZOOM_LEVEL, DEFAULT_TILT,DEFAULT_BEARING,false);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            originLocation = location;
            locationEngine.removeLocationEngineListener(this);
        }
    }
}
