package io.aipark.android.example.mapbox.map.mapBox;

import android.content.Context;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.model.EncodedPolyline;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.aipark.android.sdk.ParkingPosition;
import de.aipark.api.chargingstation.ChargingStation;
import de.aipark.api.livedata.spot.LiveSpot;
import de.aipark.api.occupancy.Occupancy;
import de.aipark.api.occupancy.ParkingAreaWithOccupancy;
import de.aipark.api.optimalTrip.OptimalTrip;
import de.aipark.api.parkevent.ParkEventLiveLeaving;
import de.aipark.api.parkingarea.MapEntry;
import de.aipark.api.parkingarea.ParkingArea;
import de.aipark.api.requestsResponse.getParkingAreasForTileWithOccupancy.GetParkingAreasForTileWithOccupancyResponse;
import de.aipark.api.tile.Tile;
import de.aipark.api.tile.TileMapper;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;
import io.aipark.android.example.mapbox.eventBus.MapThemeChangeEvent;
import io.aipark.android.example.mapbox.geo.GeoFunctions;
import io.aipark.android.example.mapbox.map.MapContext;
import io.aipark.android.example.mapbox.map.MapLatLng;
import io.aipark.android.example.mapbox.map.MapPresenter;
import io.aipark.android.example.mapbox.map.MapViewInterface;
import io.aipark.android.example.mapbox.map.mapBox.mapItems.IconHelper;
import io.aipark.android.example.mapbox.map.mapBox.mapItems.MapBoxChargingStationIcon;
import io.aipark.android.example.mapbox.map.mapBox.mapItems.MapBoxFindMyCar;
import io.aipark.android.example.mapbox.map.mapBox.mapItems.MapBoxLiveEventIcon;
import io.aipark.android.example.mapbox.map.mapBox.mapItems.MapBoxLiveSpot;
import io.aipark.android.example.mapbox.map.mapBox.mapItems.MapBoxParkingArea;
import rx.Subscriber;

public class AiparkMapBoxMapContainer implements MapViewInterface {
    public static final float START_LOADING_AREAS_ZOOM_LEVEL = 14.5f;
    public static final float START_LOADING_LIVE_EVENTS_ZOOM_LEVEL = 14.5f;
    public static final float START_LOADING_LIVE_SPOTS_ZOOM_LEVEL = 16f;

    public static final float MOVE_TO_CURRENT_POSITION_ZOOM_LEVEL = 10.0f;
    public static final float INTERSECTION_TEST_ZOOM_LEVEL = 18.0f;
    private static final float DEFAULT_ZOOM_LEVEL = 15.0f;
    private static final float DEFAULT_TILT = 0f;
    private static final float DEFAULT_BEARING = 0f;
    private static final int CAMERA_CHANGE_THRESHOLD_MS_DEFAULT = 250;
    private static final int CAMERA_CHANGE_THRESHOLD_MS_NAVIGATION = 4000;
    private static final int CAMERA_CHANGE_THRESHOLD_MS_NAVIGATION_INITIAL = 100;

    private final static int ROUTE_WIDTH_OPTIMAL_TRIP = 5;
    private final static int ROUTE_COLOR_OPTIMAL_TRIP = Color.argb(255, 69, 158, 246);
    private final static int ROUTE_WIDTH_OPTIMAL_TRIP_SEARCH = 10;
    private final static int ROUTE_COLOR_OPTIMAL_TRIP_SEARCH_LIGHT_THEME = Color.argb(150, 50, 100, 170);
    private final static int ROUTE_COLOR_OPTIMAL_TRIP_SEARCH_DARK_THEME = Color.argb(1, 50, 100, 170);
    private final static int ROUTE_WIDTH_WALKING = 5;
    private final static int ROUTE_WIDTH_DRIVING = 5;
    private final static int ROUTE_COLOR_DRIVING = Color.argb(255, 69, 158, 246);
    private final static int ROUTE_COLOR_WALKING = Color.argb(255, 150, 150, 150);

    private MapboxMap mMap;

    private Map<Tile, Map<Long, MapBoxParkingArea>> loadedParkingAreasForTiles;
    private Map<Long, MapBoxParkingArea> loadedParkingAreasForId;
    private List<Tile> visibleTiles;
    private List<Polyline> loadedRoutes;
    private List<Marker> dashedRoutesMarker;
    private List<Polyline> dashedRoutesPolyline;
    private List<Marker> routeMarkers;
    private List<Marker> dashedRoutesPolylineMarker;

    private Map<Long, MapBoxLiveEventIcon> renderedParkEventLiveLeavings;
    private Map<Tile, Map<Long, MapBoxLiveEventIcon>> renderedParkEventLiveLeavingsForTile;
    private Map<Point, MapBoxLiveSpot> renderedParkEventLiveSpots;

    private Map<Integer, MapBoxChargingStationIcon> renderedChargingStations;
    private Map<Tile, List<MapBoxChargingStationIcon>> loadedChargingStationsForTile;

    private CameraPosition lastKnownPosition;
    private Toast toast;
    private long selectedAreaId = 0;
    private MapBoxFindMyCar findMyCarMarker;

    private int cameraChangeThresholdMsDefault;

    private CountDownTimer countDownTimerLoadParkingAreasAndChargingStations, countDownTimerLiveEvents, countDownTimerLiveSpots;
    private List<Integer> countDownParkingAreasFinished, countDownLiveEventsFinished, countDownLiveSpotsFinished;

    private MapContext mapContext;
    private MapView mapView;
    private MapThemeChangeEvent.Theme theme;

    private MapPresenter presenter;


    public AiparkMapBoxMapContainer(MapContext mapContext) {
        this.mapContext = mapContext;
        init();
    }

    private void init() {
        switch (mapContext) {
            case NAVIGATION:
                cameraChangeThresholdMsDefault = CAMERA_CHANGE_THRESHOLD_MS_NAVIGATION;
                break;
            case MAP:
            default:
                cameraChangeThresholdMsDefault = CAMERA_CHANGE_THRESHOLD_MS_DEFAULT;
                break;
        }
        presenter = new MapPresenter(this, mapContext);
        loadedParkingAreasForTiles = new HashMap<>();
        loadedParkingAreasForId = new HashMap<>();
        visibleTiles = new ArrayList<>();
        loadedRoutes = new ArrayList<>();
        dashedRoutesMarker = new ArrayList<>();
        dashedRoutesPolyline = new ArrayList<>();
        routeMarkers = new ArrayList<>();
        dashedRoutesPolylineMarker = new ArrayList<>();
        renderedParkEventLiveLeavings = new HashMap<>();
        renderedParkEventLiveSpots = new HashMap<>();
        renderedParkEventLiveLeavingsForTile = new HashMap<>();
        renderedChargingStations = new HashMap<>();
        loadedChargingStationsForTile = new HashMap<>();
        countDownParkingAreasFinished = new ArrayList<>();
        countDownLiveEventsFinished = new ArrayList<>();
        countDownLiveSpotsFinished = new ArrayList<>();
    }

    private void refreshMapStyle() {
        switch (theme) {
            case DARK:
                mMap.setStyle(Style.DARK);
                break;
            case LIGHT:
                mMap.setStyle(Style.LIGHT);
                break;
            default:
                mMap.setStyle(Style.LIGHT);
        }
    }

    public void onMapReady(final MapboxMap mapboxMap) {
        this.mMap = mapboxMap;
        theme = AiParkApp.getMapTheme();
        refreshMapStyle();
        mMap.getUiSettings().setCompassEnabled(false);
        mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                boolean selected = false;
                for (MapBoxParkingArea area : loadedParkingAreasForId.values()) {
                    if (area.getMarker() != null && area.getMarker().equals(marker)) {
                        if (getPresenter() != null) {
                            getPresenter().onParkingAreaSelected(area.getData(), true, area.getOccupancy());
                        }
                        selected = true;
                    }
                }
                for (MapBoxChargingStationIcon chargingStationIcon : renderedChargingStations.values()) {
                    if (chargingStationIcon.getMarker() != null && chargingStationIcon.getMarker().equals(marker)) {
                        if (getPresenter() != null) {
                            //getPresenter().onChargingStationSelected(chargingStationIcon.getChargingStation());
                        }
                        selected = true;
                        MapBoxParkingArea parkingAreaUnselect = loadedParkingAreasForId.get(selectedAreaId);
                        if (parkingAreaUnselect != null) {
                            parkingAreaUnselect.setSelected(false);
                        }
                    } else if (chargingStationIcon.getMarker() != null) {
                        chargingStationIcon.setSelected(false);
                    }
                }
                if (findMyCarMarker != null && findMyCarMarker.getMarker() != null && findMyCarMarker.getMarker().equals(marker) && mapContext.equals(MapContext.MAP)) {
                    findMyCarMarker.onClick();
                    selected = true;
                }
                return selected;
            }
        });

        mapboxMap.setOnPolygonClickListener(new MapboxMap.OnPolygonClickListener() {
            @Override
            public void onPolygonClick(@NonNull Polygon polygon) {
                for (MapBoxParkingArea area : loadedParkingAreasForId.values()) {
                    for (Polygon part : area.getPolygons()) {
                        if (part.equals(polygon)) {
                            getPresenter().onParkingAreaSelected(area.getData(), true, area.getOccupancy());
                        }
                    }
                }
                for (MapBoxChargingStationIcon chargingStationIcon : renderedChargingStations.values()) {
                    chargingStationIcon.setSelected(false);
                }
            }
        });

        mapboxMap.setOnPolylineClickListener(new MapboxMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(@NonNull Polyline polyline) {
                for (MapBoxParkingArea area : loadedParkingAreasForId.values()) {
                    for (Polyline part : area.getPolylines()) {
                        if (part.equals(polyline)) {
                            getPresenter().onParkingAreaSelected(area.getData(), true, area.getOccupancy());
                        }
                    }
                }
                for (MapBoxChargingStationIcon chargingStationIcon : renderedChargingStations.values()) {
                    chargingStationIcon.setSelected(false);
                }
            }
        });

        mapboxMap.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(final CameraPosition position) {
                refreshForBounds(position);
            }
        });

        mapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng point) {
                getPresenter().onMapLongClicked(new MapLatLng(point.getLatitude(), point.getLongitude()));
            }
        });

        mapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull final LatLng point) {
                if (getPresenter() != null) {
                    getPresenter().onMapClicked(new MapLatLng(point.getLatitude(), point.getLongitude()));
                }
            }
        });

        if (AiParkApp.getParkingPosition() != null && AiParkApp.getParkingPosition().getPosition() != null) {
            setFindMyCar(AiParkApp.getParkingPosition());
        }

        if (getPresenter() != null) {
            getPresenter().onMapInit();
        }

        mMap.setStyle(Style.DARK);
    }

    public LatLngBounds getTileBasedLatLngBounds(LatLng latLng) {
        if (latLng == null) {
            return new LatLngBounds(
                    new com.google.android.gms.maps.model.LatLng(0, 0),
                    new com.google.android.gms.maps.model.LatLng(0, 0));
        }
        double tileRadius = 1;
        switch (mapContext) {
            case NAVIGATION:
                tileRadius = 1;
                break;
            case MAP:
            default:
                tileRadius = 1;
        }
        Tile tmp = TileMapper.getTileIndexFromLatLng((new GeometryFactory()).createPoint(new Coordinate(latLng.getLongitude(), latLng.getLatitude())), 16);
        Tile northeastTile = new Tile(tmp.getX() + tileRadius, tmp.getY() - tileRadius, tmp.getZoom());
        com.vividsolutions.jts.geom.Point northeastPoint = TileMapper.getLatLngFromTileIndex(northeastTile);
        Tile southwestTile = new Tile(tmp.getX() - tileRadius, tmp.getY() + tileRadius, tmp.getZoom());
        com.vividsolutions.jts.geom.Point southwestPoint = TileMapper.getLatLngFromTileIndex(southwestTile);

        return new LatLngBounds(
                new com.google.android.gms.maps.model.LatLng(southwestPoint.getY(), southwestPoint.getX()),
                new com.google.android.gms.maps.model.LatLng(northeastPoint.getY(), northeastPoint.getX()));
    }

    @Override
    public void centerCurrentPosition() {
        try {
            AiParkApp.getLastKnownLocation()
                    .subscribe(new Subscriber<com.google.maps.model.LatLng>() {
                        @Override
                        public void onCompleted() {
                            //Log.d("permission","onCompleted");
                        }

                        @Override
                        public void onError(Throwable e) {
                            //Log.d("permission",e.getStackTrace().toString());
                        }

                        @Override
                        public void onNext(com.google.maps.model.LatLng latLng) {
                            if (latLng != null) {
                                setCameraPosition(new MapLatLng(latLng), DEFAULT_ZOOM_LEVEL, DEFAULT_TILT, DEFAULT_BEARING, false);
                                if (toast != null) {
                                    toast.cancel();
                                }
                            } else {
                                toast.cancel();
                            }
                        }
                    });
        } catch (SecurityException e) {
            Log.d("GoogleMapFragment", "Can't center map to current position," +
                    " because location fine permission is not granted");
        }
    }

    @Override
    public void centerPosition(MapLatLng mapLatLng) {
        setCameraPosition(mapLatLng, DEFAULT_ZOOM_LEVEL, DEFAULT_TILT, DEFAULT_BEARING, false);
    }

    @Override
    public void setCameraPosition(MapLatLng position, float zoomLevel, float tilt, float bearing, boolean animate) {
        if (mMap != null) {
            CameraPosition.Builder builder = new CameraPosition.Builder();
            CameraPosition cameraPosition = builder.target(new LatLng(position.latitude, position.longitude)).zoom(zoomLevel).bearing(bearing).build();
            if (animate) {
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            } else {
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    @Override
    public void setCameraPosition(MapLatLng position, boolean animate) {
        setCameraPosition(position, DEFAULT_ZOOM_LEVEL, DEFAULT_TILT, DEFAULT_BEARING, animate);
    }

    @Override
    public void centerParkingArea(ParkingArea area) {
        if (mMap != null) {
            setCameraPosition(new MapLatLng(area.getCenter().getY(), area.getCenter().getX()), DEFAULT_ZOOM_LEVEL, DEFAULT_TILT, DEFAULT_BEARING, true);
        }
    }

    @Override
    public void setLoad(ParkingArea area, Occupancy occupancy) {
        MapBoxParkingArea parkingArea = loadedParkingAreasForId.get(area.getId());
        if (parkingArea != null) {
            parkingArea.setLoad(occupancy);
        }
    }

    @Override
    public void addRoute(EncodedPolyline route, float width, int color, boolean drawIcon) {
        if (mMap != null) {
            PolylineOptions polylineOptions = new PolylineOptions().width(width).color(color);
            for (com.google.maps.model.LatLng latLng : route.decodePath()) {
                polylineOptions.add(new LatLng(latLng.lat, latLng.lng));
            }
            Polyline newRoute = mMap.addPolyline(polylineOptions);
            loadedRoutes.add(newRoute);
            if (drawIcon) {
                routeMarkers.add(mMap.addMarker(new MarkerOptions().position(polylineOptions.getPoints().get(polylineOptions.getPoints().size() - 1)).icon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_p))));
            }
        }
    }

    @Override
    public void removeAllRoutes() {
        for (Polyline route : loadedRoutes) {
            route.remove();
        }
        for (Marker marker : dashedRoutesMarker) {
            marker.remove();
        }
        for (Polyline polyline : dashedRoutesPolyline) {
            polyline.remove();
        }
        for (Marker marker : routeMarkers) {
            marker.remove();
        }
        for (Marker marker : dashedRoutesPolylineMarker) {
            marker.remove();
        }
        loadedRoutes = new ArrayList<>();
        dashedRoutesMarker = new ArrayList<>();
        dashedRoutesPolyline = new ArrayList<>();
        routeMarkers = new ArrayList<>();
        dashedRoutesPolylineMarker = new ArrayList<>();
    }

    @Override
    public void setParkingAreaSelected(Long data, boolean directToPresenter) {
        if (data == null) {
            return;
        }
        MapBoxParkingArea parkingAreaUnselect = loadedParkingAreasForId.get(selectedAreaId);
        if (parkingAreaUnselect != null) {
            parkingAreaUnselect.setSelected(false);
        }

        selectedAreaId = data;
        MapBoxParkingArea parkingArea = loadedParkingAreasForId.get(data);
        if (parkingArea != null) {
            parkingArea.setSelected(true);
            /*if(directToPresenter && getPresenter() != null){
                getPresenter().onParkingAreaSelected(parkingArea.getData(),false,true);
            }*/
        }
    }

    @Override
    public void setChargingStationSelected(ChargingStation chargingStationSelected) {
        if (renderedChargingStations.get(chargingStationSelected.getId()) != null) {
            renderedChargingStations.get(chargingStationSelected.getId()).setSelected(true);
        }
    }

    @Override
    public void setFindMyCar(ParkingPosition position) {
        if (mMap != null && position != null && position.getPosition() != null) {
            if (findMyCarMarker != null) {
                findMyCarMarker.remove();
            }
            findMyCarMarker = new MapBoxFindMyCar(position, mMap);
        }
    }

    public Marker addMarker(MarkerOptions markerOptions) {
        Marker marker = mMap.addMarker(markerOptions);
        routeMarkers.add(marker);
        return marker;
    }

    @Override
    public void drawOptimalTrip(OptimalTrip optimalTrip) {
        removeAllRoutes();
        //List<LatLng> optimizedRoute = RouteOptimizer.optimizeRoute(optimalTrip.getRoute().getRoute().decodePath());
        List<com.google.maps.model.LatLng> latLngs = new ArrayList<>();
        for (Coordinate coordinate : optimalTrip.getRoute().getRoute()) {
            latLngs.add(
                    new com.google.maps.model.LatLng(
                            coordinate.y,
                            coordinate.x));
        }

        List<com.google.maps.model.LatLng> searchRoute = new ArrayList<>();

        Double minDistance = Double.MAX_VALUE;
        Integer minDistanceIdx = 0;

        for (int i = 0; i < latLngs.size(); i++) {
            com.google.maps.model.LatLng latLng = latLngs.get(i);
            Double distance = GeoFunctions.getLinearDistance(new com.google.android.gms.maps.model.LatLng(latLng.lat, latLng.lng), new com.google.android.gms.maps.model.LatLng(optimalTrip.getStopoverPoints().get(1).getY(), optimalTrip.getStopoverPoints().get(1).getX()));
            if (distance < minDistance) {
                minDistance = distance;
                minDistanceIdx = i;
            }
        }

        searchRoute = latLngs.subList(minDistanceIdx, latLngs.size());
        latLngs = latLngs.subList(0, Math.min(minDistanceIdx + 1, latLngs.size()));

        EncodedPolyline encodedPolyline = new EncodedPolyline(latLngs);
        addRoute(encodedPolyline, ROUTE_WIDTH_OPTIMAL_TRIP, ROUTE_COLOR_OPTIMAL_TRIP, false);

        if (searchRoute.size() > 0) {
            encodedPolyline = new EncodedPolyline(searchRoute);
            int color = ROUTE_COLOR_OPTIMAL_TRIP_SEARCH_DARK_THEME;
            switch (theme) {
                case DARK:
                    color = ROUTE_COLOR_OPTIMAL_TRIP_SEARCH_DARK_THEME;
                    break;
                case LIGHT:
                    color = ROUTE_COLOR_OPTIMAL_TRIP_SEARCH_LIGHT_THEME;
                    break;
            }
            addRoute(encodedPolyline, ROUTE_WIDTH_OPTIMAL_TRIP_SEARCH, color, false);
        }
        setCameraPosition(new MapLatLng(optimalTrip.getDestination().getY(), optimalTrip.getDestination().getX()), DEFAULT_ZOOM_LEVEL, DEFAULT_TILT, DEFAULT_BEARING, true);
        setDestinationIcon(new MapLatLng(optimalTrip.getDestination().getY(), optimalTrip.getDestination().getX()));
        for (int i = 0; i < optimalTrip.getParkingAreaResultsOptimalTrip().size(); i++) {
            MarkerOptions marker = new MarkerOptions().position(new LatLng(optimalTrip.getParkingAreaResultsOptimalTrip().get(i).getParkingArea().getCenter().getY() - 0.00001, optimalTrip.getParkingAreaResultsOptimalTrip().get(i).getParkingArea().getCenter().getX()));
            Marker markerIcon = addMarker(marker);
            switch (i) {
                case 0:
                    markerIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_1));
                    break;
                case 1:
                    markerIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_2));
                    break;
                case 2:
                    markerIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_3));
                    break;
                case 3:
                    markerIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_4));
                    break;
                case 4:
                    markerIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_5));
                    break;
                case 5:
                    markerIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_6));
                    break;
                case 6:
                    markerIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_7));
                    break;
                case 7:
                    markerIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_needle_8));
                    break;
            }
        }
    }

    @Override
    public void addOrUpdateParkingAreasForTile(GetParkingAreasForTileWithOccupancyResponse tileParkingAreasWithPredictionMaps) {
        for (MapEntry.Entry<Tile, List<ParkingAreaWithOccupancy>> mapEntry : tileParkingAreasWithPredictionMaps.getTileParkingAreaWithOccupancyMap().getEntryList()) {
            if (mapEntry.getValue() == null) {
                continue;
            }
            Map<Long, MapBoxParkingArea> tileAreas = loadedParkingAreasForTiles.get(mapEntry.getKey());
            // if areas for this tile are already loaded, update load of this areas
            if (tileAreas != null) {
                for (ParkingAreaWithOccupancy parkingAreaDataWithPrediction : mapEntry.getValue()) {
                    if (tileAreas.get(parkingAreaDataWithPrediction.getParkingArea().getId()) != null && parkingAreaDataWithPrediction != null) {
                        tileAreas.get(parkingAreaDataWithPrediction.getParkingArea().getId()).setLoad(parkingAreaDataWithPrediction.getOccupancy());
                    }
                }
            }
            // if areas for this tile are not loaded before, add areas to map
            else {
                tileAreas = new HashMap<>();
                for (ParkingAreaWithOccupancy parkingAreaDataWithPrediction : mapEntry.getValue()) {
                    if (parkingAreaDataWithPrediction != null && parkingAreaDataWithPrediction.getParkingArea() != null) {
                        MapBoxParkingArea parkingArea = loadedParkingAreasForId.get(parkingAreaDataWithPrediction.getParkingArea().getId());
                        if (parkingArea == null) {
                            parkingArea = new MapBoxParkingArea(this, parkingAreaDataWithPrediction.getParkingArea());
                            parkingArea.setLoad(parkingAreaDataWithPrediction.getOccupancy());
                            loadedParkingAreasForId.put(parkingArea.getId(), parkingArea);
                        }
                        tileAreas.put(parkingArea.getId(), parkingArea);
                        if (parkingArea.getId() == selectedAreaId) {
                            setParkingAreaSelected(parkingArea.getData().getId(), true);
                        }
                    }
                }
                loadedParkingAreasForTiles.put(mapEntry.getKey(), tileAreas);
            }
        }

        Log.d("mapbox", "get areas: " + tileParkingAreasWithPredictionMaps.getTileParkingAreaWithOccupancyMap().getEntryList().size());
    }

    @Override
    public void addOrUpdateLiveParkEvents(List<ParkEventLiveLeaving> parkEventLiveLeavings, Tile tile) {
        for (ParkEventLiveLeaving parkEventLiveLeaving : parkEventLiveLeavings) {
            if (renderedParkEventLiveLeavings.get(parkEventLiveLeaving.getId()) == null) {
                renderedParkEventLiveLeavings.put(parkEventLiveLeaving.getId(), new MapBoxLiveEventIcon(parkEventLiveLeaving, mMap));
                Map<Long, MapBoxLiveEventIcon> map = renderedParkEventLiveLeavingsForTile.get(tile);
                if (renderedParkEventLiveLeavingsForTile.get(tile) == null) {
                    map = new HashMap<>();
                    renderedParkEventLiveLeavingsForTile.put(tile, map);
                }
                map.put(parkEventLiveLeaving.getId(), renderedParkEventLiveLeavings.get(parkEventLiveLeaving.getId()));
            }
        }
        for (Long id : renderedParkEventLiveLeavings.keySet()) {
            renderedParkEventLiveLeavings.get(id).refresh();
            if (!renderedParkEventLiveLeavings.get(id).isCurrent()) {
                renderedParkEventLiveLeavings.remove(id);
            }
        }
    }

    @Override
    public void addOrUpdateLiveSpots(List<LiveSpot> liveSpots, double zoom) {
        for (LiveSpot liveSpot : liveSpots) {
            MapBoxLiveSpot tmp = renderedParkEventLiveSpots.get(liveSpot.getPosition());
            if (tmp == null) {
                renderedParkEventLiveSpots.put(liveSpot.getPosition(), new MapBoxLiveSpot(mMap, liveSpot, zoom));
            } else {
                tmp.setLiveSpotStatus(liveSpot.getOccupied(), zoom);
            }
        }
    }

    @Override
    public void addOrUpdateChargingStations(List<ChargingStation> chargingStations, Tile tile) {
        if (loadedChargingStationsForTile.get(tile) == null) {
            List<MapBoxChargingStationIcon> chargingStationIcons = new ArrayList<>();
            for (ChargingStation chargingStation : chargingStations) {
                MapBoxChargingStationIcon chargingStationIcon = new MapBoxChargingStationIcon(mMap, chargingStation);
                renderedChargingStations.put(chargingStation.getId(), chargingStationIcon);
                chargingStationIcons.add(chargingStationIcon);
            }
            loadedChargingStationsForTile.put(tile, chargingStationIcons);
        }
    }

    @Override
    public synchronized void makeItemsVisibleForTiles(List<Tile> tiles, LatLngBounds bounds, Float zoom) {
        List<Tile> visibleTilesNew = new ArrayList<>();
        for (Tile tile : visibleTiles) {
            if (tiles.contains(tile)) {
                visibleTilesNew.add(tile);
            } else {
                for (MapBoxParkingArea parkingArea : loadedParkingAreasForTiles.get(tile).values()) {
                    parkingArea.setVisibility(false);
                }
                if (loadedChargingStationsForTile.get(tile) != null) {
                    for (MapBoxChargingStationIcon chargingStationIcon : loadedChargingStationsForTile.get(tile)) {
                        chargingStationIcon.setVisibility(false);
                    }
                }
                if (renderedParkEventLiveLeavingsForTile.get(tile) != null) {
                    for (MapBoxLiveEventIcon liveEventIcon : renderedParkEventLiveLeavingsForTile.get(tile).values()) {
                        liveEventIcon.setVisibility(false);
                        Log.d("liveParkEvent", "visible false");
                    }
                }
            }
        }
        visibleTiles = visibleTilesNew;
        for (Tile tile : tiles) {
            if (!visibleTiles.contains(tile)) {
                if (loadedParkingAreasForTiles.get(tile) != null) {
                    visibleTiles.add(tile);
                }
            }
        }
        if (zoom >= INTERSECTION_TEST_ZOOM_LEVEL) {
            com.vividsolutions.jts.geom.Polygon boundsPolygon = null;
            if (zoom >= INTERSECTION_TEST_ZOOM_LEVEL) {
                Coordinate[] coordinates = new Coordinate[5];
                coordinates[0] = new Coordinate(bounds.northeast.longitude, bounds.northeast.latitude);
                coordinates[1] = new Coordinate(bounds.northeast.longitude, bounds.southwest.latitude);
                coordinates[2] = new Coordinate(bounds.southwest.longitude, bounds.southwest.latitude);
                coordinates[3] = new Coordinate(bounds.southwest.longitude, bounds.northeast.latitude);
                coordinates[4] = new Coordinate(bounds.northeast.longitude, bounds.northeast.latitude);
                boundsPolygon = (new GeometryFactory()).createPolygon(coordinates);
            }
            for (Tile tile : visibleTiles) {
                for (MapBoxParkingArea area : loadedParkingAreasForTiles.get(tile).values()) {
                    boolean isVisible = false;
                    for (Polygon polygon : area.getPolygons()) {
                        Coordinate[] coordinateArea = new Coordinate[polygon.getPoints().size()];
                        for (int i = 0; i < polygon.getPoints().size(); i++) {
                            coordinateArea[i] = new Coordinate(polygon.getPoints().get(i).getLongitude(), polygon.getPoints().get(i).getLatitude());
                        }
                        com.vividsolutions.jts.geom.Polygon areaPolygon = (new GeometryFactory()).createPolygon(coordinateArea);

                        if (boundsPolygon.intersects(areaPolygon)) {
                            isVisible = true;
                            break;
                        }
                    }
                    for (Polyline polyline : area.getPolylines()) {
                        Coordinate[] coordinateArea = new Coordinate[polyline.getPoints().size()];
                        for (int i = 0; i < polyline.getPoints().size(); i++) {
                            coordinateArea[i] = new Coordinate(polyline.getPoints().get(i).getLongitude(), polyline.getPoints().get(i).getLatitude());
                        }
                        LineString lineString = (new GeometryFactory()).createLineString(coordinateArea);

                        if (boundsPolygon.intersects(lineString)) {
                            isVisible = true;
                            break;
                        }
                    }
                    if (area.getMarker() != null && bounds.contains(new com.google.android.gms.maps.model.LatLng(area.getMarker().getPosition().getLatitude(), area.getMarker().getPosition().getLongitude()))) {
                        isVisible = true;
                    }
                    if (isVisible && !area.isVisible()) {
                        area.setVisibility(true);
                    } else {
                        area.setVisibility(false);
                    }
                }
                if (loadedChargingStationsForTile.get(tile) != null) {
                    for (MapBoxChargingStationIcon chargingStationIcon : loadedChargingStationsForTile.get(tile)) {
                        chargingStationIcon.setVisibility(true);
                    }
                }
            }
        } else {
            for (Tile tile : visibleTiles) {
                for (MapBoxParkingArea area : loadedParkingAreasForTiles.get(tile).values()) {
                    area.setVisibility(true);
                }
                if (loadedChargingStationsForTile.get(tile) != null) {
                    for (MapBoxChargingStationIcon chargingStationIcon : loadedChargingStationsForTile.get(tile)) {
                        chargingStationIcon.setVisibility(true);
                    }
                }
                if (renderedParkEventLiveLeavingsForTile.get(tile) != null) {
                    for (MapBoxLiveEventIcon liveEventIcon : renderedParkEventLiveLeavingsForTile.get(tile).values()) {
                        liveEventIcon.setVisibility(true);
                    }
                }

            }
        }
    }


    @Override
    public void reloadAllItems() {
        // parking areas
        selectedAreaId = -1;
        for (MapBoxParkingArea area : loadedParkingAreasForId.values()) {
            area.remove();
        }
        loadedParkingAreasForTiles.clear();
        loadedParkingAreasForId.clear();
        // charging stations
        for (MapBoxChargingStationIcon chargingStationIcon : renderedChargingStations.values()) {
            chargingStationIcon.remove();
        }
        renderedChargingStations.clear();
        loadedChargingStationsForTile.clear();
        // live events
        for (MapBoxLiveEventIcon parkEventLiveIcon : renderedParkEventLiveLeavings.values()) {
            parkEventLiveIcon.remove();
        }
        renderedParkEventLiveLeavings.clear();
        renderedParkEventLiveLeavingsForTile.clear();

        visibleTiles.clear();
        if (mMap.getCameraPosition().zoom < MOVE_TO_CURRENT_POSITION_ZOOM_LEVEL) {
            Log.d("locationBug", "centerCurrentPositionFirstTime");
            centerCurrentPosition();
        }
        lastKnownPosition = null;
        refreshForBounds(mMap.getCameraPosition());
    }

    @Override
    public void setDestinationIcon(MapLatLng latLng) {
        MarkerOptions destination = new MarkerOptions().position(new LatLng(latLng.latitude, latLng.longitude));
        Marker destinationIcon = addMarker(destination);
        switch (theme) {
            case DARK:
                destinationIcon.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_destination));
                break;
            case LIGHT:
            default:
                destinationIcon.setIcon(IconHelper.solidColorIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_destination), Color.BLACK));
        }
    }

    @Override
    public void setMapTheme(MapThemeChangeEvent.Theme theme) {
        this.theme = theme;
        refreshMapStyle();
    }

    private void refreshForBounds(final CameraPosition position) {
        if (getPresenter() == null || (lastKnownPosition != null && lastKnownPosition.equals(position)) || position.target == null) {
            return;
        }
        lastKnownPosition = position;
        getPresenter().saveMapPosition(new MapLatLng(position.target.getLatitude(), position.target.getLongitude()), (float) position.zoom, (float) position.tilt, (float) position.bearing);

        // live spots
        // always update existing
        for (MapBoxLiveSpot mapBoxLiveSpot : renderedParkEventLiveSpots.values()) {
            mapBoxLiveSpot.refresh(position.zoom);
        }
        if (position.zoom > START_LOADING_LIVE_SPOTS_ZOOM_LEVEL
                || (mapContext.equals(MapContext.NAVIGATION) && position.zoom > START_LOADING_AREAS_ZOOM_LEVEL)) {
            countDownTimerLiveSpots =
                    onSmoothCameraChange(countDownTimerLiveSpots, countDownLiveSpotsFinished, new SmoothedCameraChangedListener() {
                        @Override
                        public void onCameraChanged() {
                            LatLngBounds bounds = getTileBasedLatLngBounds(mMap.getCameraPosition().target);
                            getPresenter().loadOrRefreshLiveFreeSpots(bounds, position.zoom);
                        }
                    }, position);
        } else {
            for (MapBoxLiveSpot mapBoxLiveSpot : renderedParkEventLiveSpots.values()) {
                mapBoxLiveSpot.remove();
            }
            renderedParkEventLiveSpots = new HashMap<>();
            if (getPresenter() != null) {
                getPresenter().resetLiveSpots();
            }
        }

        if (position.zoom > START_LOADING_AREAS_ZOOM_LEVEL) {
            countDownTimerLoadParkingAreasAndChargingStations =
                    onSmoothCameraChange(countDownTimerLoadParkingAreasAndChargingStations, countDownParkingAreasFinished, new SmoothedCameraChangedListener() {
                        @Override
                        public void onCameraChanged() {
                            LatLngBounds bounds = getTileBasedLatLngBounds(mMap.getCameraPosition().target);
                            getPresenter().loadOrRefreshParkingAreas(bounds, (float) mMap.getCameraPosition().zoom);
                            if (getPresenter().showChargingLayer()) {
                                getPresenter().loadOrRefreshChargingStations(bounds);
                            } else if (renderedChargingStations.size() > 0) {
                                for (MapBoxChargingStationIcon chargingStationIcon : renderedChargingStations.values()) {
                                    chargingStationIcon.remove();
                                }
                                renderedChargingStations = new HashMap<>();
                                getPresenter().resetCharingStations();
                            }
                        }
                    }, position);
            if (toast != null && mapContext.equals(MapContext.MAP)) {
                toast.cancel();
            }
        } else {
            if (mapContext.equals(MapContext.MAP)) {
                try {
                    if (toast != null) {
                        toast.show();
                    } else {
                        Context context = AiParkApp.getContext();
                        CharSequence text = AiParkApp.getContext().getString(R.string.zoom_in_to_see_parking_areas);
                        int duration = Toast.LENGTH_LONG;
                        toast = Toast.makeText(context, text, duration);
                        toast.setGravity(Gravity.TOP, 0, 250);
                        toast.show();
                    }
                } catch (Exception e) {
                    Log.e("toast", Log.getStackTraceString(e));
                }
            }
        }
        if (position.zoom > START_LOADING_LIVE_EVENTS_ZOOM_LEVEL) {
            countDownTimerLiveEvents =
                    onSmoothCameraChange(countDownTimerLiveEvents, countDownLiveEventsFinished, new SmoothedCameraChangedListener() {
                        @Override
                        public void onCameraChanged() {
                            LatLngBounds bounds = getTileBasedLatLngBounds(mMap.getCameraPosition().target);
                            if (getPresenter().showLiveEventsLayer()) {
                                getPresenter().loadOrRefreshLiveParkEvents(bounds);
                                refreshLiveParkEvents();
                            } else if (renderedParkEventLiveLeavings.size() > 0) {
                                for (MapBoxLiveEventIcon parkEventLiveIcon : renderedParkEventLiveLeavings.values()) {
                                    parkEventLiveIcon.remove();
                                }
                                renderedParkEventLiveLeavings = new HashMap<>();
                                getPresenter().resetLiveParkEvents();
                            }
                        }
                    }, position);
        }
    }

    private void refreshLiveParkEvents() {
        Map<Long, MapBoxLiveEventIcon> tmp = new HashMap<>();
        for (MapBoxLiveEventIcon parkEventLiveIcon : renderedParkEventLiveLeavings.values()) {
            parkEventLiveIcon.refresh();
            if (parkEventLiveIcon.isCurrent()) {
                tmp.put(parkEventLiveIcon.getParkEventLiveLeaving().getId(), parkEventLiveIcon);
            }
        }
        renderedParkEventLiveLeavings = tmp;
    }

    public MapboxMap getmMap() {
        return mMap;
    }

    private CountDownTimer onSmoothCameraChange(CountDownTimer countDownTimer, final List<Integer> countDownTimerFinished, final SmoothedCameraChangedListener listener, final CameraPosition position) {
        if (mapContext.equals(MapContext.MAP)) {
            if (countDownTimerFinished.isEmpty()) {
                countDownTimerFinished.add(1);
                listener.onCameraChanged();
            } else {
                countDownTimer = new CountDownTimer(cameraChangeThresholdMsDefault, cameraChangeThresholdMsDefault) {
                    public void onTick(long millisUntilFinished) {

                    }

                    public void onFinish() {
                        if (mMap.getCameraPosition().equals(position)) {
                            listener.onCameraChanged();
                        }
                    }
                }.start();
            }
            return countDownTimer;
        } else {
            if (countDownTimer == null) {
                countDownTimerFinished.clear();
                if (mapContext.equals(MapContext.NAVIGATION)) {
                    countDownTimer = new CountDownTimer(CAMERA_CHANGE_THRESHOLD_MS_NAVIGATION_INITIAL, CAMERA_CHANGE_THRESHOLD_MS_NAVIGATION_INITIAL) {
                        public void onTick(long millisUntilFinished) {

                        }

                        public void onFinish() {
                            listener.onCameraChanged();
                            countDownTimerFinished.add(1);
                        }
                    }.start();
                } else {
                    listener.onCameraChanged();
                    countDownTimer = new CountDownTimer(cameraChangeThresholdMsDefault, cameraChangeThresholdMsDefault) {
                        public void onTick(long millisUntilFinished) {

                        }

                        public void onFinish() {
                            countDownTimerFinished.add(1);
                        }
                    }.start();
                }

            } else if (!countDownTimerFinished.isEmpty()) {
                countDownTimerFinished.clear();
                countDownTimer = new CountDownTimer(cameraChangeThresholdMsDefault, cameraChangeThresholdMsDefault) {
                    public void onTick(long millisUntilFinished) {

                    }

                    public void onFinish() {
                        listener.onCameraChanged();
                        countDownTimerFinished.add(1);
                    }
                }.start();
            }
            return countDownTimer;
        }
    }

    private interface SmoothedCameraChangedListener {
        void onCameraChanged();
    }

    public MapThemeChangeEvent.Theme getTheme() {
        return theme;
    }

    public MapPresenter getPresenter() {
        return presenter;
    }
}
