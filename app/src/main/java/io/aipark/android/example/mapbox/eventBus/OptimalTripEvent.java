package io.aipark.android.example.mapbox.eventBus;

import de.aipark.api.requestsResponse.getOptimalTrip.GetOptimalTripResponse;

public class OptimalTripEvent {
    GetOptimalTripResponse getOptimalTripResponse;

    public OptimalTripEvent(GetOptimalTripResponse getOptimalTripResponse) {
        this.getOptimalTripResponse = getOptimalTripResponse;
    }

    public GetOptimalTripResponse getGetOptimalTripResponse() {
        return getOptimalTripResponse;
    }

    public void setGetOptimalTripResponse(GetOptimalTripResponse getOptimalTripResponse) {
        this.getOptimalTripResponse = getOptimalTripResponse;
    }
}
