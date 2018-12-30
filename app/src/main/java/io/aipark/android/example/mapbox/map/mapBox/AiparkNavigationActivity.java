package io.aipark.android.example.mapbox.map.mapBox;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import io.aipark.android.example.mapbox.R;
import io.aipark.android.example.mapbox.map.MapContext;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiparkNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener {
    private static final String TAG = "mapboxNavigation";
    private NavigationView navigationView;
    private AiparkMapBoxMapContainer aiparkMapBoxMapContainer;
    private static Point origin, destination;
    private static String directionsCriteria;
    private static NavigationListener navigationListener;

    public static void init(Point origin, Point destination, NavigationListener navigationListener, String directionsCriteria) {
        AiparkNavigationActivity.origin = origin;
        AiparkNavigationActivity.destination = destination;
        AiparkNavigationActivity.navigationListener = navigationListener;
        AiparkNavigationActivity.directionsCriteria = directionsCriteria;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        setTheme(R.style.MyCustomTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aipark_activity_navigation);
        navigationView = findViewById(R.id.navigationView);
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
        Log.d(TAG, "onLowMemory");
    }

    @Override
    public void onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
    }

    @Override
    public void onCancelNavigation() {
        if (navigationListener != null) {
            navigationListener.onCancelNavigation();
        }
        finish();
    }

    @Override
    public void onNavigationFinished() {
        if (navigationListener != null) {
            navigationListener.onNavigationFinished();
        }
        finish();
    }

    @Override
    public void onNavigationRunning() {
        if (navigationListener != null) {
            navigationListener.onNavigationRunning();
        }
    }

    private void getRoute(Point origin, Point destination) {
        try {
            NavigationRoute.builder(this)
                    .accessToken(Mapbox.getAccessToken())
                    .origin(origin)
                    .profile(directionsCriteria)
                    .destination(destination)
                    .build()
                    .getRoute(new Callback<DirectionsResponse>() {
                        @Override
                        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                            try {
                                // You can get the generic HTTP info about the response
                                Log.d(TAG, "Response code: " + response.code());
                                if (response.body() == null) {
                                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                                    return;
                                } else if (response.body().routes().size() < 1) {
                                    Log.e(TAG, "No routes found");
                                    return;
                                }

                                DirectionsRoute currentRoute = response.body().routes().get(0);

                                NavigationViewOptions options = NavigationViewOptions.builder()
                                        .directionsRoute(currentRoute)
                                        .shouldSimulateRoute(false)
                                        .navigationListener(AiparkNavigationActivity.this)
                                        .build();


                                navigationView.startNavigation(options);
                                aiparkMapBoxMapContainer = new AiparkMapBoxMapContainer(MapContext.NAVIGATION);
                                //aiparkMapBoxMapContainer.onMapReady(navigationView.retrieveNavigationMapboxMap().retrieveMap());
                                aiparkMapBoxMapContainer.onMapReady(navigationView.getMapboxMap());
                            } catch (Exception e) {
                                Log.e(TAG, e.getLocalizedMessage());
                            }
                        }

                        @Override
                        public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                            Log.e(TAG, "Error: " + throwable.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public void onNavigationReady() {
        getRoute(origin, destination);
    }

    /*@Override
    public void onNavigationReady(boolean isRunning) {
        getRoute(origin, destination);
    }*/
}
