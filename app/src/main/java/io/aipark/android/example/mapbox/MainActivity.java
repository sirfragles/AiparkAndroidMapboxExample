package io.aipark.android.example.mapbox;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.maps.model.LatLng;

import rx.Subscriber;

/**
 * main activity, used to request fine location permission
 */
public class MainActivity extends AppCompatActivity {
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AiParkApp.setActivity(this);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("location", "MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    AiParkApp.getLastKnownLocation().subscribe(new Subscriber<LatLng>() {
                        @Override
                        public void onCompleted() {
                            Log.i("location", "getLastKnownLocation onCompleted");
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.i("location", "getLastKnownLocation error");
                        }

                        @Override
                        public void onNext(LatLng latLng) {
                            Log.i("location", "location known: " + latLng);
                        }
                    });

                    AiParkApp.reconnectLocationService(AiParkApp.getContext());
                    try {
                        if (AiParkApp.isInitialCenterCurrentPosition()) {
                            AiParkApp.getMapFragement().centerCurrentPosition();
                        } else {
                            AiParkApp.getMapFragement().centerPosition(AiParkApp.getInitialPosition());
                        }
                    } catch (Exception e) {
                        Log.e("centerPosition", Log.getStackTraceString(e));
                    }
                } else {
                    Log.i("location", "exit");
                    System.exit(0);
                }
                return;
            }
        }
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(i);
    }
}
