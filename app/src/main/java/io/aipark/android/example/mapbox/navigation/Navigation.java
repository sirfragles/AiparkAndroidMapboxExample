package io.aipark.android.example.mapbox.navigation;

import android.content.Context;

import com.google.maps.model.LatLng;

import java.util.List;

import de.aipark.api.route.TrafficMode;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.popup.OnNavigationArrivalPopup;
import rx.Observer;


/**
 * abstract navigation interface
 */
public abstract class Navigation {
    public void startNavigationTo(final List<LatLng> waypoints, final LatLng destination, final Context context, final TrafficMode trafficMode) {
        AiParkApp.getLastKnownLocation().subscribe(new Observer<LatLng>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public synchronized void onNext(final LatLng latLng) {
                startNavigationIntern(latLng, waypoints, 0, destination, context, trafficMode);
            }
        });
    }

    private void startNavigationIntern(final LatLng origin, final List<LatLng> waypoints, final int wayPointIdx, final LatLng finalDestination, final Context context, TrafficMode trafficMode) {
        LatLng destination;
        if (waypoints == null || waypoints.size() == 0 || wayPointIdx >= waypoints.size()) {
            if (finalDestination == null) {
                return;
            }
            destination = finalDestination;
            trafficMode = TrafficMode.WALKING;
        } else {
            destination = waypoints.get(wayPointIdx);
        }

        final TrafficMode finalTrafficMode = trafficMode;
        startNavigation(origin, destination, context, trafficMode, new OnArrivalListener() {
            @Override
            public void onDestinationArrived() {
                boolean nextArea = !(waypoints == null || waypoints.size() == 0 || wayPointIdx + 1 >= waypoints.size());
                boolean finalDestinationExist = finalDestination != null && finalTrafficMode != TrafficMode.WALKING;
                OnNavigationArrivalPopup onNavigationArrivalPopup = new OnNavigationArrivalPopup(new OnNavigationArrivalPopup.OnDecisionListener() {
                    @Override
                    public void onFinalDestination() {
                        AiParkApp.getLastKnownLocation().subscribe(new Observer<LatLng>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public synchronized void onNext(final LatLng latLng) {
                                startNavigation(latLng, finalDestination, context, TrafficMode.WALKING, new OnArrivalListener() {
                                    @Override
                                    public void onDestinationArrived() {

                                    }
                                });
                            }
                        });

                    }

                    @Override
                    public void onNextParkingArea() {
                        AiParkApp.getLastKnownLocation().subscribe(new Observer<LatLng>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public synchronized void onNext(final LatLng latLng) {
                                startNavigationIntern(latLng, waypoints, wayPointIdx + 1, finalDestination, context, finalTrafficMode);
                            }
                        });

                    }
                },
                        nextArea,
                        finalDestinationExist);
            }
        });
    }

    public abstract void startNavigation(LatLng origin, LatLng destination, Context context, TrafficMode trafficMode, OnArrivalListener onArrivalListener);
}
