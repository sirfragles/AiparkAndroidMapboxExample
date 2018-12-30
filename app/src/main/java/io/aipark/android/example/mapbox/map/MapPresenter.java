package io.aipark.android.example.mapbox.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.aipark.android.sdk.ParkingPosition;
import de.aipark.api.chargingstation.ChargingStation;
import de.aipark.api.chargingstation.ChargingStationFilter;
import de.aipark.api.livedata.spot.LiveSpot;
import de.aipark.api.occupancy.Occupancy;
import de.aipark.api.occupancy.ParkingAreaWithOccupancy;
import de.aipark.api.parkevent.ParkEventLiveLeaving;
import de.aipark.api.parkingarea.MapEntry;
import de.aipark.api.requestsResponse.getChargingStationsForTile.GetChargingStationsForTileRequest;
import de.aipark.api.requestsResponse.getChargingStationsForTile.GetChargingStationsForTileResponse;
import de.aipark.api.requestsResponse.getLiveParkEvents.GetLiveParkEventsRequest;
import de.aipark.api.requestsResponse.getLiveSpots.GetLiveSpotsForTileRequest;
import de.aipark.api.requestsResponse.getParkingAreasForTileWithOccupancy.GetParkingAreasForTileWithOccupancyResponse;
import de.aipark.api.requestsResponse.getParkingAreasForTileWithOccupancyForDeparture.GetParkingAreasForTileWithOccupancyForDepartureRequest;
import de.aipark.api.tile.Tile;
import de.aipark.api.tile.TileMapper;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;
import io.aipark.android.example.mapbox.eventBus.CenterCurrentPositionEvent;
import io.aipark.android.example.mapbox.eventBus.CenterPositionEvent;
import io.aipark.android.example.mapbox.eventBus.MapClickedEvent;
import io.aipark.android.example.mapbox.eventBus.MapThemeChangeEvent;
import io.aipark.android.example.mapbox.eventBus.OnFailureEvent;
import io.aipark.android.example.mapbox.eventBus.OptimalTripEvent;
import io.aipark.android.example.mapbox.eventBus.ParkingAreaSelectedEvent;
import io.aipark.android.example.mapbox.eventBus.ReloadMapItemsEvent;
import io.aipark.android.example.mapbox.eventBus.SearchEvent;
import io.aipark.android.example.mapbox.map.mapBox.ParkingAreaResult;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static io.aipark.android.example.mapbox.map.mapBox.mapItems.MapBoxLiveEventIcon.PARK_EVENT_CURRENT_TIME_IN_MINUTES;

/**
 * presenter for the map containing the main logic for loading parking areas, live leaving parking area events and live free spots from sensors
 */
public class MapPresenter {
    private static final int TILE_ZOOM_LEVEL_PARKING_AREAS = 16;
    private static final int TILE_ZOOM_LEVEL_CHARGING_STATIONS = 16;
    private static final int TILE_ZOOM_LEVEL_LIVE_EVENTS = 16;
    private static final int TILE_ZOOM_LEVEL_LIVE_SPOTS = 16;
    private static final long TILE_OUTDATED_THRESHOLD_MS = 5 * 60 * 1000;
    private static final long TILE_REQUEST_THRESHOLD_MS = 15 * 1000;
    private static final long TILE_REQUEST_THRESHOLD_MS_LIVE_PARK_EVENTS = 5 * 1000;
    private static final long TILE_REQUEST_THRESHOLD_MS_LIVE_SPOTS = 5 * 1000;

    public final static String CURRENT_POSITION_LAT = "currentMapPositionLat";
    public final static String CURRENT_POSITION_LNG = "currentMapPositionLng";
    public final static String CURRENT_POSITION_ZOOM = "currentMapPositionZoom";
    public final static String CURRENT_POSITION_TILT = "currentMapPositionTile";
    public final static String CURRENT_POSITION_BEARING = "currentMapPositionBearing";
    public static String LAYER_CHARGING = "LAYER_CHARGING";
    public static String LAYER_LIVE_EVENTS = "LAYER_LIVE_EVENTS";

    private final static String CURRENT_SELECTED_PARKING_AREA_ID = "currentSelectedAreaId";

    private final int MAX_SUBSCRIPTION_SIZE = 100;

    private Map<Tile, List<ParkingAreaWithOccupancy>> loadedParkingAreaTiles;
    private Map<Tile, List<ChargingStation>> loadedChargingStationTiles;

    private Map<Tile, Long> requestedTileAreas;
    private Map<Tile, Long> requestedTileLiveEvents;
    private Map<Tile, Long> requestedTileChargingStations;
    private Map<Tile, Long> requestedTileLiveSpots;
    private MapContext mapContext = MapContext.MAP;

    private MapViewInterface view;

    private List<Subscription> mapBoundsSubscription = new ArrayList<>();

    public MapPresenter(MapViewInterface view, MapContext mapContext) {
        this.mapContext = mapContext;
        this.view = view;
        init();
    }

    private void init() {
        EventBus.getDefault().register(this);
        loadedParkingAreaTiles = new HashMap<>();
        loadedChargingStationTiles = new HashMap<>();
        requestedTileAreas = new HashMap<>();
        requestedTileLiveEvents = new HashMap<>();
        requestedTileChargingStations = new HashMap<>();
        requestedTileLiveSpots = new HashMap<>();
    }

    private Subscription searchSubscription;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOptimalTrip(final OptimalTripEvent event) {
        if (event.getGetOptimalTripResponse().getOptimalTrips().getEntryList().size() > 0) {
            getView().drawOptimalTrip(event.getGetOptimalTripResponse().getOptimalTrips().getEntryList().get(0).getValue());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewCarPosition(ParkingPosition event) {
        if (getView() != null) {
            getView().setFindMyCar(event);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCenterCurrentPosition(CenterCurrentPositionEvent event) {
        if (getView() != null) {
            getView().centerCurrentPosition();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCenterPosition(CenterPositionEvent event) {
        if (getView() != null) {
            getView().centerPosition(event.getMapLatLng());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReloadMapItems(ReloadMapItemsEvent event) {
        reloadEverything();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeChangeEvent(MapThemeChangeEvent event) {
        if (getView() != null) {
            getView().setMapTheme(event.getTheme());
        }
        reloadEverything();
    }

    /**
     * @param area                  selected parking area
     * @param putSelectedEventOnBus true, if parking area is selected by a touch event, false if otherwise
     */
    public void onParkingAreaSelected(final de.aipark.api.parkingarea.ParkingArea area, final boolean putSelectedEventOnBus, Occupancy occupancy) {
        SharedPreferences sharedPreferences = AiParkApp.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(CURRENT_SELECTED_PARKING_AREA_ID, (area.getId()));
        editor.apply();

        getView().setParkingAreaSelected(area.getId(), false);

        if (putSelectedEventOnBus) {
            EventBus.getDefault().postSticky(new ParkingAreaSelectedEvent(new ParkingAreaResult(area, occupancy), ParkingAreaSelectedEvent.Status.RESULT_LOADED));
        }
    }

    public void loadOrRefreshParkingAreas(final LatLngBounds bounds, final float zoom) {
        // make bounds bigger to get overlapping areas
        Point northeastPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(bounds.northeast.longitude, bounds.northeast.latitude)}), new GeometryFactory());
        Tile tmp = TileMapper.getTileIndexFromLatLng(northeastPoint, TILE_ZOOM_LEVEL_PARKING_AREAS);
        tmp = new Tile(tmp.getX() + 0, tmp.getY() - 0, tmp.getZoom());
        northeastPoint = TileMapper.getLatLngFromTileIndex(tmp);
        Point southwestPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(bounds.southwest.longitude, bounds.southwest.latitude)}), new GeometryFactory());
        tmp = TileMapper.getTileIndexFromLatLng(southwestPoint, TILE_ZOOM_LEVEL_PARKING_AREAS);
        tmp = new Tile(tmp.getX() - 0, tmp.getY() + 0, tmp.getZoom());
        southwestPoint = TileMapper.getLatLngFromTileIndex(tmp);

        Tile northeastTile = TileMapper.getTileIndexFromLatLng(northeastPoint, TILE_ZOOM_LEVEL_PARKING_AREAS).getRoundedTile();
        Tile southwestTile = TileMapper.getTileIndexFromLatLng(southwestPoint, TILE_ZOOM_LEVEL_PARKING_AREAS).getRoundedTile();
        northeastTile = new Tile(northeastTile.getX(), northeastTile.getY(), TILE_ZOOM_LEVEL_PARKING_AREAS);
        southwestTile = new Tile(southwestTile.getX(), southwestTile.getY(), TILE_ZOOM_LEVEL_PARKING_AREAS);
        final List<Tile> tilesInsideViewPort = new ArrayList<>();
        for (int x = (int) southwestTile.getX(); x <= northeastTile.getX(); x++) {
            for (int y = (int) northeastTile.getY(); y <= southwestTile.getY(); y++) {
                tilesInsideViewPort.add(new Tile(x, y, TILE_ZOOM_LEVEL_PARKING_AREAS));
            }
        }
        final List<Tile> tilesToAddOrUpdate = new ArrayList<>();
        for (Tile tile : tilesInsideViewPort) {
            if ((requestedTileAreas.get(tile) == null || (requestedTileAreas.get(tile) != null
                    && requestedTileAreas.get(tile) < System.currentTimeMillis() - TILE_REQUEST_THRESHOLD_MS))
                    && (loadedParkingAreaTiles.get(tile) == null
                    || (loadedParkingAreaTiles.get(tile) != null && loadedParkingAreaTiles.get(tile).size() > 0
                    && loadedParkingAreaTiles.get(tile).size() > 0 && loadedParkingAreaTiles.get(tile).get(0) != null && loadedParkingAreaTiles.get(tile).get(0).getOccupancy().getTimestamp().getTime() < System.currentTimeMillis() - TILE_OUTDATED_THRESHOLD_MS))) {
                tilesToAddOrUpdate.add(tile);
                requestedTileAreas.put(tile, System.currentTimeMillis());
            }
        }
        if (tilesToAddOrUpdate.size() > 0) {
            while (mapBoundsSubscription.size() > MAX_SUBSCRIPTION_SIZE) {
                mapBoundsSubscription.remove(mapBoundsSubscription.size() - 1);
            }
            Log.d("mapBox", "loadOrRefreshParkingAreas 2");
            mapBoundsSubscription.add(AiParkApp
                    .getLastKnownLocation()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<LatLng>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            //EventBus.getDefault().postSticky(new OnFailureEvent(e));
                        }

                        @Override
                        public void onNext(LatLng latLng) {
                            Log.d("mapBox", "loadOrRefreshParkingAreas 3");
                            Point ownPosition = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(latLng.lng, latLng.lat)}), new GeometryFactory());
                            for (Tile tile : tilesToAddOrUpdate) {
                                mapBoundsSubscription.add(AiParkApp.getAiparkSDK()
                                        .getApi()
                                        .getParkingAreasForTileWithOccupancyForPosition(
                                                new GetParkingAreasForTileWithOccupancyForDepartureRequest(tile, AiParkApp.getFiltersFromSharedPreferences(), new Timestamp(System.currentTimeMillis()), ownPosition))
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Subscriber<GetParkingAreasForTileWithOccupancyResponse>() {
                                            @Override
                                            public void onCompleted() {
                                                //Log.i("tileBased","onCompleted");
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                Log.i("tileBased", "error");
                                                e.printStackTrace();
                                                EventBus.getDefault().postSticky(new OnFailureEvent(e));
                                            }

                                            @Override
                                            public void onNext(GetParkingAreasForTileWithOccupancyResponse tileParkingAreasWithPredictionMaps) {
                                                if (tileParkingAreasWithPredictionMaps.getTileParkingAreaWithOccupancyMap() != null) {
                                                    for (MapEntry.Entry<Tile, List<ParkingAreaWithOccupancy>> mapEntry : tileParkingAreasWithPredictionMaps.getTileParkingAreaWithOccupancyMap().getEntryList()) {
                                                        loadedParkingAreaTiles.put(mapEntry.getKey(), mapEntry.getValue());
                                                    }
                                                    //Log.i("tileBased","onNext");

                                                    getView().addOrUpdateParkingAreasForTile(tileParkingAreasWithPredictionMaps);
                                                    getView().makeItemsVisibleForTiles(tilesInsideViewPort, bounds, zoom);
                                                }
                                            }
                                        }));
                            }
                        }
                    }));
        } else {
            getView().makeItemsVisibleForTiles(tilesInsideViewPort, bounds, zoom);
        }
    }

    public void resetLiveParkEvents() {
        requestedTileLiveEvents = new HashMap<>();
    }

    public synchronized void loadOrRefreshLiveParkEvents(final LatLngBounds bounds) {
        Point northeastPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(bounds.northeast.longitude, bounds.northeast.latitude)}), new GeometryFactory());
        Point southwestPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(bounds.southwest.longitude, bounds.southwest.latitude)}), new GeometryFactory());
        Tile northeastTile = TileMapper.getTileIndexFromLatLng(northeastPoint, TILE_ZOOM_LEVEL_LIVE_EVENTS).getRoundedTile();
        Tile southwestTile = TileMapper.getTileIndexFromLatLng(southwestPoint, TILE_ZOOM_LEVEL_LIVE_EVENTS).getRoundedTile();

        final List<Tile> tilesInsideViewPort = new ArrayList<>();
        for (int x = (int) southwestTile.getX(); x <= northeastTile.getX(); x++) {
            for (int y = (int) northeastTile.getY(); y <= southwestTile.getY(); y++) {
                Tile tile = new Tile(x, y, TILE_ZOOM_LEVEL_LIVE_EVENTS);
                Long lastRequestTime = requestedTileLiveEvents.get(tile);
                if (lastRequestTime == null || (lastRequestTime != null && lastRequestTime < System.currentTimeMillis() - TILE_REQUEST_THRESHOLD_MS_LIVE_PARK_EVENTS)) {
                    tilesInsideViewPort.add(new Tile(x, y, TILE_ZOOM_LEVEL_LIVE_EVENTS));
                }
                requestedTileLiveEvents.put(tile, System.currentTimeMillis());
            }
        }
        for (final Tile tile : tilesInsideViewPort) {
            mapBoundsSubscription.add(AiParkApp.getAiparkSDK().getApi()
                    .getLiveParkEvents(
                            new GetLiveParkEventsRequest(
                                    tile,
                                    new Timestamp(System.currentTimeMillis() - (1000 * 60 * PARK_EVENT_CURRENT_TIME_IN_MINUTES)),
                                    new Timestamp(System.currentTimeMillis()))
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<List<ParkEventLiveLeaving>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(List<ParkEventLiveLeaving> parkEventLiveLeavings) {
                            if (parkEventLiveLeavings.size() > 0) {
                                getView().addOrUpdateLiveParkEvents(parkEventLiveLeavings, tile);
                            }
                        }
                    }));
        }
    }

    public synchronized void loadOrRefreshLiveFreeSpots(final LatLngBounds bounds, final double zoom) {
        Log.d("liveSpot", "loadOrRefreshLiveFreeSpots");
        Point northeastPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(bounds.northeast.longitude, bounds.northeast.latitude)}), new GeometryFactory());
        Point southwestPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(bounds.southwest.longitude, bounds.southwest.latitude)}), new GeometryFactory());
        Tile northeastTile = TileMapper.getTileIndexFromLatLng(northeastPoint, TILE_ZOOM_LEVEL_LIVE_SPOTS).getRoundedTile();
        Tile southwestTile = TileMapper.getTileIndexFromLatLng(southwestPoint, TILE_ZOOM_LEVEL_LIVE_SPOTS).getRoundedTile();

        final List<Tile> tilesInsideViewPort = new ArrayList<>();
        for (int x = (int) southwestTile.getX(); x <= northeastTile.getX(); x++) {
            for (int y = (int) northeastTile.getY(); y <= southwestTile.getY(); y++) {
                Tile tile = new Tile(x, y, TILE_ZOOM_LEVEL_LIVE_SPOTS);
                Long lastRequestTime = requestedTileLiveSpots.get(tile);
                if (lastRequestTime == null || (lastRequestTime != null && lastRequestTime < System.currentTimeMillis() - TILE_REQUEST_THRESHOLD_MS_LIVE_SPOTS)) {
                    tilesInsideViewPort.add(new Tile(x, y, TILE_ZOOM_LEVEL_LIVE_SPOTS));
                }
                requestedTileLiveSpots.put(tile, System.currentTimeMillis());
            }
        }
        for (Tile tile : tilesInsideViewPort) {
            mapBoundsSubscription.add(AiParkApp.getAiparkSDK().getApi()
                    .getLiveSpotsForTile(
                            new GetLiveSpotsForTileRequest(
                                    tile,
                                    new Timestamp(System.currentTimeMillis()))
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<List<LiveSpot>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d("liveSpot", "onError" + e.getMessage());
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(List<LiveSpot> parkEventLiveSpots) {
                            if (parkEventLiveSpots.size() > 0) {
                                getView().addOrUpdateLiveSpots(parkEventLiveSpots, zoom);
                            }
                        }
                    }));
        }
    }

    public void resetCharingStations() {
        requestedTileChargingStations = new HashMap<>();
    }

    public void resetLiveSpots() {
        requestedTileLiveSpots = new HashMap<>();
    }

    public synchronized void loadOrRefreshChargingStations(final LatLngBounds bounds) {
        Point northeastPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(bounds.northeast.longitude, bounds.northeast.latitude)}), new GeometryFactory());
        Point southwestPoint = new Point(new CoordinateArraySequence(new Coordinate[]{new Coordinate(bounds.southwest.longitude, bounds.southwest.latitude)}), new GeometryFactory());
        Tile northeastTile = TileMapper.getTileIndexFromLatLng(northeastPoint, TILE_ZOOM_LEVEL_CHARGING_STATIONS).getRoundedTile();
        Tile southwestTile = TileMapper.getTileIndexFromLatLng(southwestPoint, TILE_ZOOM_LEVEL_CHARGING_STATIONS).getRoundedTile();

        final List<Tile> tilesInsideViewPort = new ArrayList<>();
        for (int x = (int) southwestTile.getX(); x <= northeastTile.getX(); x++) {
            for (int y = (int) northeastTile.getY(); y <= southwestTile.getY(); y++) {
                Tile tile = new Tile(x, y, TILE_ZOOM_LEVEL_CHARGING_STATIONS);
                if ((requestedTileChargingStations.get(tile) == null || (requestedTileChargingStations.get(tile) != null
                        && requestedTileChargingStations.get(tile) < System.currentTimeMillis() - TILE_REQUEST_THRESHOLD_MS))
                        && (loadedChargingStationTiles.get(tile) == null)) {
                    tilesInsideViewPort.add(tile);
                    requestedTileChargingStations.put(tile, System.currentTimeMillis());
                }
            }
        }
        for (Tile tile : tilesInsideViewPort) {
            mapBoundsSubscription.add(AiParkApp.getAiparkSDK().getApi()
                    .getChargingStationsForTile(
                            new GetChargingStationsForTileRequest(
                                    tile,
                                    new ChargingStationFilter())
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<GetChargingStationsForTileResponse>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(GetChargingStationsForTileResponse getChargingStationsForTileResponse) {
                            if (getChargingStationsForTileResponse.getChargingStations().size() > 0) {
                                getView().addOrUpdateChargingStations(getChargingStationsForTileResponse.getChargingStations(), getChargingStationsForTileResponse.getTile());
                            }
                        }
                    }));
        }

    }

    public void onMapLongClicked(MapLatLng position) {
        if (mapContext == MapContext.MAP) {
            getView().removeAllRoutes();
            getView().setDestinationIcon(position);
            AddressResult addressResult = new AddressResult(AiParkApp.getContext().getString(R.string.destination), "");
            addressResult.setCoordinate(new LatLng(position.latitude, position.longitude));
            SearchEvent searchEvent = new SearchEvent(addressResult);
            searchEvent.setAnimateCamera(true);
            EventBus.getDefault().postSticky(searchEvent);
        }
    }

    public void reloadEverything() {
        if (mapBoundsSubscription != null) {
            for (Subscription subscription : mapBoundsSubscription) {
                subscription.unsubscribe();
            }
            mapBoundsSubscription.clear();
        }
        loadedParkingAreaTiles.clear();
        requestedTileAreas.clear();
        loadedChargingStationTiles.clear();
        requestedTileChargingStations.clear();
        requestedTileLiveEvents.clear();
        if (getView() != null) {
            getView().reloadAllItems();
            getView().removeAllRoutes();
        }
    }

    public void onMapClicked(MapLatLng mapLatLng) {
        EventBus.getDefault().postSticky(new MapClickedEvent(mapLatLng));
    }

    public boolean showChargingLayer() {
        SharedPreferences sharedPref = AiParkApp.getActivity().getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean(LAYER_CHARGING, false);
    }

    public boolean showLiveEventsLayer() {
        SharedPreferences sharedPref = AiParkApp.getActivity().getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean(LAYER_LIVE_EVENTS, true);
    }


    public void onMapInit() {
        if (mapContext.equals(MapContext.MAP)) {
            SharedPreferences sharedPreferences = AiParkApp.getActivity().getPreferences(Context.MODE_PRIVATE);
            Float currentLat = sharedPreferences.getFloat(CURRENT_POSITION_LAT, 0);
            Float currentLng = sharedPreferences.getFloat(CURRENT_POSITION_LNG, 0);
            Float zoom = sharedPreferences.getFloat(CURRENT_POSITION_ZOOM, 0);
            Float tilt = sharedPreferences.getFloat(CURRENT_POSITION_TILT, 0);
            Float bearing = sharedPreferences.getFloat(CURRENT_POSITION_BEARING, 0);
            if (currentLat == 0 && currentLng == 0) {
                getView().centerCurrentPosition();
            } else {
                getView().setCameraPosition(new MapLatLng(currentLat, currentLng), zoom, tilt, bearing, false);
            }
            final Long selectedParkingAreaId = sharedPreferences.getLong(CURRENT_SELECTED_PARKING_AREA_ID, -1);
            if (selectedParkingAreaId > -1) {
                getView().setParkingAreaSelected(selectedParkingAreaId, true);
            }
        }
    }

    public void saveMapPosition(MapLatLng mapLatLng, float zoom, float tilt, float bearing) {
        SharedPreferences sharedPreferences = AiParkApp.getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(CURRENT_POSITION_LAT, (float) mapLatLng.latitude);
        editor.putFloat(CURRENT_POSITION_LNG, (float) mapLatLng.longitude);
        editor.putFloat(CURRENT_POSITION_ZOOM, zoom);
        editor.putFloat(CURRENT_POSITION_TILT, tilt);
        editor.putFloat(CURRENT_POSITION_BEARING, bearing);
        editor.apply();
    }

    public MapViewInterface getView() {
        return view;
    }

}
