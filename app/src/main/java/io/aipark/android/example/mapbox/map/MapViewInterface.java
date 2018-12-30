package io.aipark.android.example.mapbox.map;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.model.EncodedPolyline;

import java.util.List;

import de.aipark.android.sdk.ParkingPosition;
import de.aipark.api.chargingstation.ChargingStation;
import de.aipark.api.livedata.spot.LiveSpot;
import de.aipark.api.occupancy.Occupancy;
import de.aipark.api.optimalTrip.OptimalTrip;
import de.aipark.api.parkevent.ParkEventLiveLeaving;
import de.aipark.api.parkingarea.ParkingArea;
import de.aipark.api.requestsResponse.getParkingAreasForTileWithOccupancy.GetParkingAreasForTileWithOccupancyResponse;
import de.aipark.api.tile.Tile;
import io.aipark.android.example.mapbox.eventBus.MapThemeChangeEvent;

/**
 * Interface to communicate with the aipark map.
 */
public interface MapViewInterface {
    void centerCurrentPosition();

    void centerPosition(MapLatLng mapLatLng);

    void setCameraPosition(MapLatLng position, float zoomLevel, float tilt, float bearing, boolean animate);

    void setCameraPosition(MapLatLng position, boolean animate);

    void centerParkingArea(ParkingArea area);

    void setLoad(ParkingArea area, Occupancy occupancy);

    void addRoute(EncodedPolyline route, float width, int color, boolean drawIcon);

    void removeAllRoutes();

    void setParkingAreaSelected(Long parkingAreaSelectedId, boolean directToPresenter);

    void setChargingStationSelected(ChargingStation chargingStationSelected);

    void setFindMyCar(ParkingPosition position);

    void drawOptimalTrip(OptimalTrip optimalTrip);

    void addOrUpdateParkingAreasForTile(GetParkingAreasForTileWithOccupancyResponse tileParkingAreasWithPredictionMaps);

    void addOrUpdateLiveParkEvents(List<ParkEventLiveLeaving> parkEventLiveLeavings, Tile tile);

    void addOrUpdateLiveSpots(List<LiveSpot> liveSpots, double zoom);

    void addOrUpdateChargingStations(List<ChargingStation> chargingStations, Tile tile);

    void makeItemsVisibleForTiles(List<Tile> tiles, LatLngBounds bounds, Float zoom);

    void reloadAllItems();

    void setDestinationIcon(MapLatLng latLng);

    void setMapTheme(MapThemeChangeEvent.Theme theme);
}