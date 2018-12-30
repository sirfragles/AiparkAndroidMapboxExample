package io.aipark.android.example.mapbox.popup;

import de.aipark.api.optimalTrip.OptimalTrip;
import de.aipark.api.optimalTrip.Situation;
import de.aipark.api.parkingarea.ParkingAreaType;
import de.aipark.api.requestsResponse.getOptimalTrip.GetOptimalTripResponse;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;

/**
 * create textual description from optimal trip
 */

public class OptimalTripInterpreter {
    public static String getOptimalTripDescription(GetOptimalTripResponse optimalTripResponse) {
        String result = "";
        OptimalTrip optimalTrip = optimalTripResponse.getOptimalTrips().getEntryList().get(0).getValue();
        switch (optimalTripResponse.getSituation()) {
            case GOOD:
                result += AiParkApp.getContext().getString(R.string.speech_good_situation) + " ";
                break;
            case MIDDLE:
                result += AiParkApp.getContext().getString(R.string.speech_middle_situation) + " ";
                break;
            case BAD:
                result += AiParkApp.getContext().getString(R.string.speech_bad_situation) + " ";
                break;
            case UNKNOWN:
                result += AiParkApp.getContext().getString(R.string.speech_unknown_situation) + " ";
                break;
            default:
        }
        if (optimalTripResponse.getSituation() != Situation.UNKNOWN) {
            result = getTypeText(result, optimalTrip);
            if (optimalTrip.getEstimatedSearchDuration() > 5 * 60) {
                result += AiParkApp.getContext().getString(R.string.speech_longer_search_time_pre) + " " + (int) (optimalTrip.getEstimatedSearchDuration() / 60) + " " + AiParkApp.getContext().getString(R.string.speech_longer_search_time_post);
            }
        }

        return result;
    }

    private static String getTypeText(String result, OptimalTrip optimalTrip) {
        if (optimalTrip.getParkingAreaResultsOptimalTrip().size() > 0) {
            if (optimalTrip.getParkingAreaResultsOptimalTrip().get(0).getParkingArea().getParkingAreaType().equals(ParkingAreaType.ON_STREET)) {
                result += AiParkApp.getContext().getString(R.string.speech_search_on_street_area);
            } else if (optimalTrip.getParkingAreaResultsOptimalTrip().get(0).getParkingArea().getParkingAreaType().equals(ParkingAreaType.CAR_PARK)) {
                result += AiParkApp.getContext().getString(R.string.speech_drive_to_car_park) + " \"" + optimalTrip.getParkingAreaResultsOptimalTrip().get(0).getParkingArea().getName() + "\".";
            } else {
                result += AiParkApp.getContext().getString(R.string.speech_drive_to_area_pre) + " \"" + optimalTrip.getParkingAreaResultsOptimalTrip().get(0).getParkingArea().getName() + "\" " + AiParkApp.getContext().getString(R.string.speech_drive_to_area_post);
            }
        }
        return result;
    }
}
