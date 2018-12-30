package io.aipark.android.example.mapbox.geo;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * some helpful geo functions
 */
public class GeoFunctions {

    public static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    public static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    public static double getConvertedDistance(LatLng latlng1, LatLng latlng2) {
        double distance = distance(latlng1.latitude,
                latlng1.longitude,
                latlng2.latitude,
                latlng2.longitude);
        BigDecimal bd = new BigDecimal(distance);
        BigDecimal res = bd.setScale(3, RoundingMode.DOWN);
        return res.doubleValue();
    }

    private static double distance(double lat1, double lon1, double lat2,
                                   double lon2) {

        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else
            return distance(lat1, lon1, lat2, lon2, 'K');
    }

    private static double distance(double lat1, double lon1, double lat2,
                                   double lon2, char unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == 'K') {
            dist = dist * 1.609344;
        } else if (unit == 'N') {
            dist = dist * 0.8684;
        }
        return (dist);
    }

    /**
     * @param pLatitude         latitude of central point
     * @param pLongitude        longitude of central point
     * @param pDistanceInMeters distance from central point of the bounding box
     * @return bounding box
     */
    public static LatLngBounds getBoundingBox(final double pLatitude, final double pLongitude, final int pDistanceInMeters) {

        final double[] boundingBox = new double[4];

        final double latRadian = Math.toRadians(pLatitude);

        final double degLatKm = 110.574235;
        final double degLongKm = 110.572833 * Math.cos(latRadian);
        final double deltaLat = pDistanceInMeters / 1000.0 / degLatKm;
        final double deltaLong = pDistanceInMeters / 1000.0 /
                degLongKm;

        final double minLat = pLatitude - deltaLat;
        final double minLong = pLongitude - deltaLong;
        final double maxLat = pLatitude + deltaLat;
        final double maxLong = pLongitude + deltaLong;

        boundingBox[0] = minLat;
        boundingBox[1] = minLong;
        boundingBox[2] = maxLat;
        boundingBox[3] = maxLong;

        return new LatLngBounds(new LatLng(boundingBox[0], boundingBox[1]), new LatLng(boundingBox[2], boundingBox[3]));
    }

    /**
     * @param origin      origin
     * @param destination destination
     * @return linear distance in meters
     */
    public static Double getLinearDistance(LatLng origin, LatLng destination) {
        double lat1 = origin.latitude,
                lat2 = destination.latitude,
                lon1 = origin.longitude,
                lon2 = destination.longitude,
                el1 = 0,
                el2 = 0;
        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
}
