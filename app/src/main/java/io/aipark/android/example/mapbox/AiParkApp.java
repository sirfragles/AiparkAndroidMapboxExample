package io.aipark.android.example.mapbox;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.maps.model.LatLng;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.aipark.android.sdk.AiparkSDK;
import de.aipark.android.sdk.OnNewParkingPositionListener;
import de.aipark.android.sdk.ParkingPosition;
import de.aipark.api.optimalTrip.Preference;
import de.aipark.api.parkingarea.ParkingAreaDataFilter;
import de.aipark.api.requestsResponse.getOptimalTrip.GetOptimalTripRequest;
import de.aipark.api.requestsResponse.getOptimalTrip.GetOptimalTripResponse;
import io.aipark.android.example.mapbox.eventBus.CenterPositionEvent;
import io.aipark.android.example.mapbox.eventBus.MapClickedEvent;
import io.aipark.android.example.mapbox.eventBus.MapThemeChangeEvent;
import io.aipark.android.example.mapbox.eventBus.OptimalTripEvent;
import io.aipark.android.example.mapbox.eventBus.ParkingAreaSelectedEvent;
import io.aipark.android.example.mapbox.eventBus.SearchEvent;
import io.aipark.android.example.mapbox.location.GoogleApiLocation;
import io.aipark.android.example.mapbox.map.MapLatLng;
import io.aipark.android.example.mapbox.map.MapViewInterface;
import io.aipark.android.example.mapbox.map.mapBox.ParkingAreaResult;
import io.aipark.android.example.mapbox.popup.OptimalTripPopup;
import io.aipark.android.example.mapbox.popup.ParkingAreaPopup;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.Schedulers;

/**
 * Application class, main entrance point to interact with the map and configure everything
 */
public class AiParkApp extends Application {
    public final static String MAP_DAY_MODE = "mapDayMode";

    // default thresholds for occupancy coloring
    private static List<Integer> defaultColorThresholds = Arrays.asList(20, 40, 60, 80, 100);

    // setting for first app start, if initialCenterCurrentPosition is set to true, the current location of the user will be centerd
    private static MapLatLng initialPosition = new MapLatLng(52.264763, 10.526972);
    private static boolean initialCenterCurrentPosition = false;

    // location service to get current location of the user
    private static GoogleApiLocation googleApiLocation;

    // map view interface to have access to the map
    private static MapViewInterface mapFragement;

    // the aipark sdk is used to access parking information {@link 'https://github.com/AIPARK-Open-Source/AiparkAndroidSDK'}
    private static AiparkSDK aiparkSDK;

    private static MainActivity mActivity;
    private static int appVersion = 0;

    // last known parking position of the user
    private static ParkingPosition parkingPosition;

    private Subscription searchSubscription;

    @Override
    public void onCreate() {
        super.onCreate();
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            appVersion = (int) (Double.valueOf(version) * 100);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        googleApiLocation = new GoogleApiLocation(this);
        aiparkSDK = new AiparkSDK(
                this, BuildConfig.MY_AIPARK_API_KEY
        );
        aiparkSDK.setOnNewParkingPositionListener(new OnNewParkingPositionListener() {
            @Override
            public void onNewParkingPosition(ParkingPosition parkingPosition) {
                AiParkApp.parkingPosition = parkingPosition;
                EventBus.getDefault().post(
                        parkingPosition
                );
            }
        });
        EventBus.getDefault().register(this);
    }

    /**********************************************************************************************************
     *
     * in the following are different event callbacks that can be used to access map events and trigger actions
     *
     **********************************************************************************************************/

    /**
     * @param event containing information about the search event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSearchEvent(final SearchEvent event) {
        Log.d("searchEvent", event.toString());
        if (searchSubscription != null) {
            searchSubscription.unsubscribe();
        }

        // center search position on map
        EventBus.getDefault().postSticky(new CenterPositionEvent(new MapLatLng(event.getAddressResult().getCoordinate().lat, event.getAddressResult().getCoordinate().lng)));


        final Observable<LatLng> locationObservable = AiParkApp
                .getLastKnownLocation()
                .subscribeOn(Schedulers.io());
        locationObservable
                .subscribe(new Observer<LatLng>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("optimalTrip", "", e);
                    }

                    @Override
                    public void onNext(final LatLng latLng) {
                        AiParkApp.getAiparkSDK().getApi().getOptimalTrip(
                                new GetOptimalTripRequest(
                                        new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(latLng.lng, latLng.lat)}), new GeometryFactory()),
                                        new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(event.getAddressResult().getCoordinate().lng, event.getAddressResult().getCoordinate().lat)}), new GeometryFactory()),
                                        new Timestamp(System.currentTimeMillis()),
                                        AiParkApp.getFiltersFromSharedPreferences(),
                                        new ArrayList<Preference>(),
                                        null,
                                        800
                                )).subscribe(new Observer<GetOptimalTripResponse>() {
                            boolean resultExists = false;

                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e("optimalTrip", "", e);
                            }

                            @Override
                            public void onNext(GetOptimalTripResponse getOptimalTripResponse) {
                                if (getOptimalTripResponse.getOptimalTrips().getEntryList().size() > 0) {
                                    EventBus.getDefault().postSticky(new OptimalTripEvent(getOptimalTripResponse));
                                }
                            }
                        });
                    }
                });
    }

    /**
     * @param event containing information about the optimal trip
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOptimalTrip(final OptimalTripEvent event) {
        if (event.getGetOptimalTripResponse().getOptimalTrips().getEntryList().size() > 0) {
            new OptimalTripPopup(event.getGetOptimalTripResponse(), event.getGetOptimalTripResponse().getOptimalTrips().getEntryList().get(0).getValue());
        }
    }

    /**
     * @param event containing information about an map clicked event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMapClicked(final MapClickedEvent event) {
        Log.i("mapEvent", "on map clicked " + event.getMapLatLng());
    }

    /**
     * @param event containing information about the selected parking area on the map
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onParkingAreaSelected(final ParkingAreaSelectedEvent event) {
        Log.i("mapEvent", "parking area selected " + event.getParkingAreaResult());
        new ParkingAreaPopup(new ParkingAreaResult(event.getParkingAreaResult().getData(), event.getParkingAreaResult().getOccupancy()));
    }

    public static ParkingPosition getParkingPosition() {
        return parkingPosition;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    public static void reconnectLocationService(Context context) {
        googleApiLocation = new GoogleApiLocation(context);
    }

    public static MainActivity getActivity() {
        return mActivity;
    }

    public static void setActivity(MainActivity mActivity) {
        AiParkApp.mActivity = mActivity;
        googleApiLocation = new GoogleApiLocation(mActivity);
    }

    public static Context getContext() {
        return mActivity;
    }

    public synchronized static List<ParkingAreaDataFilter> getFiltersFromSharedPreferences() {
        ArrayList<ParkingAreaDataFilter> result = new ArrayList<>();
        SharedPreferences sharedPref = AiParkApp.getActivity().getPreferences(Context.MODE_PRIVATE);

        Boolean filterFreeChecked = sharedPref.getBoolean(ParkingAreaDataFilter.FREE.toString(), false);
        if (filterFreeChecked) {
            result.add(ParkingAreaDataFilter.FREE);
        }
        Boolean filterParkingDeckChecked = sharedPref.getBoolean(ParkingAreaDataFilter.CAR_PARK.toString(), false);
        if (filterParkingDeckChecked) {
            result.add(ParkingAreaDataFilter.CAR_PARK);
        }

        Boolean filterPrivateChecked = sharedPref.getBoolean(ParkingAreaDataFilter.NOT_PRIVATE.toString(), false);
        if (filterPrivateChecked) {
            result.add(ParkingAreaDataFilter.NOT_PRIVATE);
        }
        Boolean filterCustomerChecked = sharedPref.getBoolean(ParkingAreaDataFilter.NOT_CUSTOMER.toString(), false);
        if (filterCustomerChecked) {
            result.add(ParkingAreaDataFilter.NOT_CUSTOMER);
        }
        return result;
    }


    public static Observable<LatLng> getLastKnownLocation() {
        return googleApiLocation.getLastKnownLocationObservable();
    }

    public static MapViewInterface getMapFragement() {
        return mapFragement;
    }

    public static void setMapFragement(MapViewInterface mapFragement) {
        AiParkApp.mapFragement = mapFragement;
    }

    public static List<Integer> getColorThresholds() {
        return defaultColorThresholds;
    }

    public static AiparkSDK getAiparkSDK() {
        return aiparkSDK;
    }

    public static MapThemeChangeEvent.Theme getMapTheme() {
        if (AiParkApp.getActivity()
                .getPreferences(Context.MODE_PRIVATE)
                .getBoolean(AiParkApp.MAP_DAY_MODE, false)) {
            return MapThemeChangeEvent.Theme.LIGHT;
        } else {
            return MapThemeChangeEvent.Theme.DARK;
        }
    }

    public static MapLatLng getInitialPosition() {
        return initialPosition;
    }

    public static void setInitialPosition(MapLatLng initialPosition) {
        AiParkApp.initialPosition = initialPosition;
    }

    public static boolean isInitialCenterCurrentPosition() {
        return initialCenterCurrentPosition;
    }

    public static void setInitialCenterCurrentPosition(boolean initialCenterCurrentPosition) {
        AiParkApp.initialCenterCurrentPosition = initialCenterCurrentPosition;
    }
}
