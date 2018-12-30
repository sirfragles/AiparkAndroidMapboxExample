package io.aipark.android.example.mapbox.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.aipark.android.example.mapbox.AiParkApp;
import rx.Observable;
import rx.Subscriber;

public class GoogleApiLocation implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DetectionAPI";

    GoogleApiClient mGoogleApiClient;
    Context context;
    LocationListener locationListener;
    boolean connected;
    private LocationRequest locationRequest;
    private List<Subscriber<? super LatLng>> lastKnownLocationQuery = Collections.synchronizedList(new ArrayList<Subscriber<? super LatLng>>());

    public GoogleApiLocation(Context context) {
        this.context = context;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            connected = true;
            if (locationRequest != null && locationListener != null) {
                LocationServices.FusedLocationApi.removeLocationUpdates(
                        mGoogleApiClient, locationListener);
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, locationRequest, locationListener);
            }
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            for (Subscriber<? super LatLng> subscriber : lastKnownLocationQuery) {
                if (subscriber != null) {
                    if (location != null) {
                        subscriber.onNext(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        subscriber.onNext(null);
                    }
                    subscriber.onCompleted();
                }
            }
            lastKnownLocationQuery = new ArrayList<>();
        } else {
            for (Subscriber<? super LatLng> subscriber : lastKnownLocationQuery) {
                if (subscriber != null) {
                    subscriber.onNext(null);
                    subscriber.onCompleted();
                }
            }
            lastKnownLocationQuery = new ArrayList<>();
        }
    }

    public void setLocationListener(LocationListener locationListener) {
        this.locationListener = locationListener;
    }

    public void setRequestInterval(long requestInterval, int locationPriority) {
        if (locationListener == null) {
            return;
        }
        locationRequest = new LocationRequest();
        locationRequest.setInterval(requestInterval);
        locationRequest.setFastestInterval(requestInterval);
        locationRequest.setPriority(locationPriority);
        if (connected == true) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, locationListener);
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, locationRequest, locationListener);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        connected = false;
        try {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            apiAvailability.getErrorDialog(AiParkApp.getActivity(), apiAvailability.isGooglePlayServicesAvailable(AiParkApp.getContext()), 1).show();
        } catch (Exception e) {
            Toast.makeText(AiParkApp.getActivity(), "Failure", Toast.LENGTH_LONG).show();
        }
    }

    public Observable<LatLng> getLastKnownLocationObservable() {
        if (connected) {
            return Observable.create(new Observable.OnSubscribe<LatLng>() {
                @Override
                public void call(Subscriber<? super LatLng> subscriber) {
                    if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        try {
                            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            subscriber.onNext(new LatLng(location.getLatitude(), location.getLongitude()));
                            subscriber.onCompleted();
                        } catch (NullPointerException n) {
                            subscriber.onError(n);
                        }
                    } else {
                        subscriber.onNext(null);
                        subscriber.onCompleted();
                    }
                }
            });
        } else {
            return Observable.create(new Observable.OnSubscribe<LatLng>() {
                @Override
                public void call(Subscriber<? super LatLng> subscriber) {
                    //subscriber.onCompleted();
                    lastKnownLocationQuery.add(subscriber);
                }
            });
        }
    }

    public PendingResult<AutocompletePredictionBuffer> placeResult(String search, com.google.android.gms.maps.model.LatLng latLng) {
        if (connected) {
            return Places.GeoDataApi
                    .getAutocompletePredictions(mGoogleApiClient, search, new LatLngBounds(latLng, latLng), null);
        } else {
            return null;
        }
    }

    public PendingResult<PlaceBuffer> getPlaceForId(String placeId) {
        if (connected) {
            return Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
        } else {
            return null;
        }
    }
}
