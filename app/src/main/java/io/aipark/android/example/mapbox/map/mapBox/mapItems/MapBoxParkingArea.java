package io.aipark.android.example.mapbox.map.mapBox.mapItems;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.aipark.api.occupancy.Occupancy;
import de.aipark.api.occupancy.OccupancyType;
import de.aipark.api.parkingarea.ParkingAreaType;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;
import io.aipark.android.example.mapbox.map.mapBox.AiparkMapBoxMapContainer;

public class MapBoxParkingArea {
    private final static int ALPHA_NOT_SELECTED_AREA_DARK_MAP = 50;
    private final static int ALPHA_NOT_SELECTED_AREA_LIGHT_MAP = 200;
    private final static int ALPHA_SELECTED_AREA = 255;

    private final static int POLYLINE_WIDTH = 5;

    private final static int POLYGON_FILL_BLUE = Color.argb(ALPHA_NOT_SELECTED_AREA_DARK_MAP, 0, 102, 255);
    private final static int POLYGON_STROKE_BLUE = Color.argb(ALPHA_NOT_SELECTED_AREA_DARK_MAP, 0, 102, 255);

    private final static int POLYGON_FILL_GREEN = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorGreen), ALPHA_NOT_SELECTED_AREA_DARK_MAP);
    private final static int POLYGON_STROKE_GREEN = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorGreen), 255);

    private final static int POLYGON_FILL_ORANGE = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorOrange), ALPHA_NOT_SELECTED_AREA_DARK_MAP);
    private final static int POLYGON_STROKE_ORANGE = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorOrange), 255);

    private final static int POLYGON_FILL_YELLOW = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorYellow), ALPHA_NOT_SELECTED_AREA_DARK_MAP);
    private final static int POLYGON_STROKE_YELLOW = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorYellow), 255);

    private final static int POLYGON_FILL_GREENYELLOW = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorGreenYellow), ALPHA_NOT_SELECTED_AREA_DARK_MAP);
    private final static int POLYGON_STROKE_GREENYELLOW = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorGreenYellow), 255);

    private final static int POLYGON_FILL_RED = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorRed), ALPHA_NOT_SELECTED_AREA_DARK_MAP);
    private final static int POLYGON_STROKE_RED = getARGB(AiParkApp.getContext().getResources().getColor(R.color.colorRed), 255);

    public final static float LOAD_UNKNOWN = -1f;

    private MapboxMap mMap;
    private List<Polygon> polygons;
    private List<Polyline> polylines;
    private de.aipark.api.parkingarea.ParkingArea data;
    private Marker marker;
    private boolean isParkingDeck;
    private boolean selected = false;
    private boolean visible;
    private Occupancy occupancy;
    AiparkMapBoxMapContainer aiparkMapBoxMapContainer;

    public MapBoxParkingArea(AiparkMapBoxMapContainer aiparkMapBoxMapContainer, de.aipark.api.parkingarea.ParkingArea data) {
        this.aiparkMapBoxMapContainer = aiparkMapBoxMapContainer;
        //Log.i("areaType",data.getParkingAreaType().toString());
        this.mMap = aiparkMapBoxMapContainer.getmMap();
        this.polygons = new LinkedList<>();
        this.polylines = new LinkedList<>();
        this.data = data;
        this.isParkingDeck = false;
        visible = true;
        addAreaToMap();
        //setLoad(new Occupancy(data.getId(),null,LOAD_UNKNOWN,OccupancyType.U));
    }

    private void addAreaToMap() {
        int countEqualPoints = 1;
        if (data.getParkingAreaType() == ParkingAreaType.CAR_PARK
                || data.getShape().getGeometryType().equals("Point")) {
            //Log.i("areaType2","PARKHAUS");
            this.isParkingDeck = true;
        } else {
            // iterate throw points and add polygons
            for (int i = 0; i < data.getShape().getNumGeometries(); i++) {
                //Log.i("geometrytype",data.getShape().getGeometryType().toString());
                if (data.getShape().getGeometryType().equals("LineString")) {
                    com.vividsolutions.jts.geom.LineString lineString = (com.vividsolutions.jts.geom.LineString) data.getShape().getGeometryN(i);

                    PolylineOptions polylineOptions = new PolylineOptions().width(POLYLINE_WIDTH).color(Color.BLUE);
                    for (Coordinate coordinate : lineString.getCoordinates()) {
                        polylineOptions.add(new LatLng(coordinate.y, coordinate.x));
                    }
                    Polyline polyline = mMap.addPolyline(polylineOptions);
                    //polyline.setClickable(true);
                    polylines.add(polyline);
                    //Log.i("geometrytype","added polyline " + polyline);
                } else if (data.getShape().getGeometryType().equals("MultiPolygon")) {
                    List<LatLng> outer = new ArrayList<>();
                    PolygonOptions polyOptions = new PolygonOptions();
                    com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon) data.getShape().getGeometryN(i);

                    //Gets each polygon outer coordinates
                    Coordinate[] outerCoordinates = polygon.getExteriorRing().getCoordinates();
                    LatLng newPoint = null;
                    LatLng oldPoint = null;
                    for (Coordinate outerCoordinate : outerCoordinates) {
                        newPoint = new LatLng(outerCoordinate.y, outerCoordinate.x);
                        if (oldPoint != null && oldPoint.equals(newPoint)) {
                            countEqualPoints++;
                        }
                        oldPoint = newPoint;
                        outer.add(new LatLng(outerCoordinate.y, outerCoordinate.x));
                    }
                    polyOptions.addAll(outer);
                    //polyOptions.visible(false);

                    //Getting each polygon interior coordinates (hole)  if they exist
                    if (polygon.getNumInteriorRing() > 0) {
                        for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
                            List<LatLng> inner = new ArrayList<LatLng>();
                            Coordinate[] innerCoordinates = polygon.getInteriorRingN(j).getCoordinates();
                            for (Coordinate innerCoordinate : innerCoordinates) {
                                inner.add(new LatLng(innerCoordinate.y, innerCoordinate.x));
                            }
                            polyOptions.addHole(inner);
                        }
                    }
                    com.mapbox.mapboxsdk.annotations.Polygon newPolygon = mMap.addPolygon(polyOptions);
                    //newPolygon.setClickable(true);
                    polygons.add(newPolygon);
                }

            }
        }

        // set marker for parking area
        if (isParkingDeck) {
            marker = mMap.addMarker(new MarkerOptions().position(new LatLng(data.getCenter().getY(), data.getCenter().getX())).title(data.getName()));
            //marker.set(ALPHA_NOT_SELECTED_MARKER);
        }
        if (occupancy != null) {
            setLoadIntern(occupancy);
            setSelected(selected);
        }
    }

    // remove parking area from map
    public void remove() {
        for (Polygon polygon : polygons) {
            polygon.remove();
        }
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        if (marker != null) {
            marker.remove();
        }
    }

    public long getId() {
        return data.getId();
    }

    public String getName() {
        return data.getName();
    }

    public void setLoad(Occupancy occupancy) {
        this.occupancy = occupancy;
        setLoadIntern(occupancy);
        setSelected(selected);
    }

    private void setLoadIntern(Occupancy occupancy) {
        float load = occupancy.getValue();
        if (occupancy.getType().equals(OccupancyType.L) || occupancy.getType().equals(OccupancyType.LP)) {
            load = Math.min(occupancy.getValue() * 2, 100);
        }

        // polygon color
        if (occupancy.getType().equals(OccupancyType.U)) {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_green));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(POLYGON_FILL_BLUE);
                polygon.setStrokeColor(POLYGON_STROKE_BLUE);
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(POLYGON_FILL_BLUE);
            }
        } else if (occupancy.getType().equals(OccupancyType.C)) {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_closed));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(POLYGON_FILL_RED);
                polygon.setStrokeColor(POLYGON_STROKE_RED);
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(POLYGON_FILL_RED);
            }
        } else if (load <= AiParkApp.getColorThresholds().get(0)) {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_red));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(POLYGON_FILL_RED);
                polygon.setStrokeColor(POLYGON_STROKE_RED);
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(POLYGON_FILL_RED);
            }
        } else if (load <= AiParkApp.getColorThresholds().get(1)) {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_orange));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(POLYGON_FILL_ORANGE);
                polygon.setStrokeColor(POLYGON_STROKE_ORANGE);
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(POLYGON_FILL_ORANGE);
            }
        } else if (load <= AiParkApp.getColorThresholds().get(2)) {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_yellow));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(POLYGON_FILL_YELLOW);
                polygon.setStrokeColor(POLYGON_STROKE_YELLOW);
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(POLYGON_FILL_YELLOW);
            }
        } else if (load <= AiParkApp.getColorThresholds().get(3)) {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_green_yellow));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(POLYGON_FILL_GREENYELLOW);
                polygon.setStrokeColor(POLYGON_STROKE_GREENYELLOW);
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(POLYGON_FILL_GREENYELLOW);
            }
        } else if (load <= AiParkApp.getColorThresholds().get(4)) {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_green));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(POLYGON_FILL_GREEN);
                polygon.setStrokeColor(POLYGON_STROKE_GREEN);
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(POLYGON_FILL_GREEN);
            }
        } else {
            if (marker != null) {
                marker.setIcon(IconFactory.getInstance(AiParkApp.getContext()).fromResource(R.drawable.ic_pin_green));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(POLYGON_FILL_BLUE);
                polygon.setStrokeColor(POLYGON_STROKE_BLUE);
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(POLYGON_FILL_BLUE);
            }
        }
    }

    public synchronized void setSelected(boolean selected) {
        if (selected) {
            if (marker != null) {
                marker.setIcon(IconHelper.tintIcon(marker.getIcon(), ContextCompat.getColor(AiParkApp.getContext(), R.color.whiteTransparentMarker)));
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(Color.argb(ALPHA_SELECTED_AREA, Color.red(polygon.getFillColor()), Color.green(polygon.getFillColor()), Color.blue(polygon.getFillColor())));
                polygon.setStrokeColor(Color.argb(ALPHA_SELECTED_AREA, Color.red(polygon.getStrokeColor()), Color.green(polygon.getStrokeColor()), Color.blue(polygon.getStrokeColor())));
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(Color.argb(ALPHA_SELECTED_AREA, Color.red(polyline.getColor()), Color.green(polyline.getColor()), Color.blue(polyline.getColor())));
            }
        } else if (!selected) {
            int alpha = 0;
            switch (aiparkMapBoxMapContainer.getTheme()) {
                case DARK:
                    alpha = ALPHA_NOT_SELECTED_AREA_DARK_MAP;
                    break;
                case LIGHT:
                default:
                    alpha = ALPHA_NOT_SELECTED_AREA_LIGHT_MAP;
            }
            if (marker != null) {
                //marker.setAlpha(ALPHA_NOT_SELECTED_MARKER);
                setLoadIntern(occupancy);
            }
            for (Polygon polygon : polygons) {
                polygon.setFillColor(Color.argb(alpha, Color.red(polygon.getFillColor()), Color.green(polygon.getFillColor()), Color.blue(polygon.getFillColor())));
                polygon.setStrokeColor(Color.argb(alpha, Color.red(polygon.getStrokeColor()), Color.green(polygon.getStrokeColor()), Color.blue(polygon.getStrokeColor())));
            }
            for (Polyline polyline : polylines) {
                polyline.setColor(Color.argb(alpha, Color.red(polyline.getColor()), Color.green(polyline.getColor()), Color.blue(polyline.getColor())));
            }
        }
        this.selected = selected;
    }

    public de.aipark.api.parkingarea.ParkingArea getData() {
        return data;
    }

    public Marker getMarker() {
        return marker;
    }

    synchronized public void setVisibility(boolean visible) {
        if (this.visible && !visible) {
            remove();
            this.visible = false;
        }
        if (!this.visible && visible) {
            addAreaToMap();
            this.visible = true;
        }
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }

    public List<Polyline> getPolylines() {
        return polylines;
    }

    public static int getARGB(int rgba, int alpha) {
        return Color.argb(alpha, Color.red(rgba), Color.green(rgba), Color.blue(rgba));
    }

    public boolean isVisible() {
        return visible;
    }

    public Occupancy getOccupancy() {
        return occupancy;
    }
}
