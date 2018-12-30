package io.aipark.android.example.mapbox.popup;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import de.aipark.api.occupancy.ParkingAreaWithOccupancy;
import de.aipark.api.optimalTrip.OptimalTrip;
import de.aipark.api.parkingarea.ParkingArea;
import de.aipark.api.parkingarea.ParkingAreaDataFilter;
import de.aipark.api.requestsResponse.getOptimalTrip.GetOptimalTripResponse;
import de.aipark.api.requestsResponse.getParkingAreasForPosition.GetParkingAreasForPositionRequest;
import de.aipark.api.requestsResponse.getParkingAreasForPosition.GetParkingAreasForPositionResponse;
import de.aipark.api.route.TrafficMode;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;
import io.aipark.android.example.mapbox.navigation.mapbox.MapBoxNavigation;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * popup showing optimal trip
 */

public class OptimalTripPopup {
    public OptimalTripPopup(final GetOptimalTripResponse getOptimalTripResponse, final OptimalTrip optimalTrip) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AiParkApp.getActivity());

        LayoutInflater inflater = AiParkApp.getActivity().getLayoutInflater();
        View detailsView = inflater.inflate(R.layout.optimla_trip_popup, null);
        AppCompatTextView parkingAreaName = (AppCompatTextView) detailsView.findViewById(R.id.popup_heading);
        TextView situationText = (TextView) detailsView.findViewById(R.id.textViewSituation);
        TextView priceStringText = (TextView) detailsView.findViewById(R.id.textViewPriceString);
        TextView searchTimeText = (TextView) detailsView.findViewById(R.id.textViewSearchTime);
        final TextView voucherText = (TextView) detailsView.findViewById(R.id.textViewVoucher);

        LinearLayout linearLayoutGroupSlots = (LinearLayout) detailsView.findViewById(R.id.groupSlots);
        LinearLayout linearLayoutGroupCosts = (LinearLayout) detailsView.findViewById(R.id.groupCosts);
        final LinearLayout linearLayoutGroupVoucher = (LinearLayout) detailsView.findViewById(R.id.groupVoucher);

        Button detailsCancelButton = (Button) detailsView.findViewById(R.id.details_cancel_button);
        Button startNaviationButton = (Button) detailsView.findViewById(R.id.navigateButton);
        Button startNaviationPRButton = (Button) detailsView.findViewById(R.id.navigateButtonPR);

        situationText.setText(OptimalTripInterpreter.getOptimalTripDescription(getOptimalTripResponse));

        String priceString = "";
        if (optimalTrip.getParkingAreaResultsOptimalTrip().size() > 0
                && optimalTrip.getParkingAreaResultsOptimalTrip().get(0).getParkingArea() != null
                && optimalTrip.getParkingAreaResultsOptimalTrip().get(0).getParkingArea().getPriceString() != null) {
            optimalTrip.getParkingAreaResultsOptimalTrip().get(0).getParkingArea().getPriceString().replace("\t", "\u0009\u0009");
            priceStringText.setText(priceString);
            priceStringText.setTypeface(Typeface.MONOSPACE);
        }

        if (priceString.isEmpty()) {
            linearLayoutGroupCosts.setVisibility(View.GONE);
        }

        // search time
        String searchRouteString = "";
        if (optimalTrip.getEstimatedSearchDuration() != null) {
            if (optimalTrip.getEstimatedSearchDuration() < 60) {
                searchRouteString = AiParkApp.getContext().getString(R.string.circa) + " " + String.format("%d", (int) optimalTrip.getEstimatedSearchDuration()) + " " + AiParkApp.getContext().getString(R.string.seconds_short);
            } else {
                searchRouteString = AiParkApp.getContext().getString(R.string.circa) + " " + String.format("%d", (int) (optimalTrip.getEstimatedSearchDuration() / 60)) + " " + AiParkApp.getContext().getString(R.string.minutes_short);
            }
        }

        // search NOX, just an estimation to show this use case
        int nox = (int) (120 * ((double) optimalTrip.getEstimatedSearchDuration() / 60));
        int co2 = (int) (190 * ((double) optimalTrip.getEstimatedSearchDuration() / 60));
        searchRouteString += ", " + nox + "mg NOx, " + co2 + "g CO2";

        searchTimeText.setText(searchRouteString);

        builder.setView(detailsView);
        final AlertDialog dialog = builder.create();
        try {
            dialog.show();
        } catch (Throwable t) {
            Log.e("Popup", "error popup creating", t);
            return;
        }

        startNaviationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<LatLng> destination = new ArrayList<>();
                for (ParkingAreaWithOccupancy parkingAreaResult :
                        optimalTrip.getParkingAreaResultsOptimalTrip()) {
                    ParkingArea p = parkingAreaResult.getParkingArea();
                    destination.add(new LatLng(p.getCenter().getY(), p.getCenter().getX()));
                }
                new MapBoxNavigation().startNavigationTo(destination, new LatLng(optimalTrip.getDestination().getY(), optimalTrip.getDestination().getX()),
                        AiParkApp.getContext(), TrafficMode.DRIVING);
                dialog.cancel();
            }
        });

        startNaviationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<LatLng> destination = new ArrayList<>();
                for (ParkingAreaWithOccupancy parkingAreaResult :
                        optimalTrip.getParkingAreaResultsOptimalTrip()) {
                    ParkingArea p = parkingAreaResult.getParkingArea();
                    destination.add(new LatLng(p.getCenter().getY(), p.getCenter().getX()));
                }
                new MapBoxNavigation().startNavigationTo(destination, new LatLng(optimalTrip.getDestination().getY(), optimalTrip.getDestination().getX()),
                        AiParkApp.getContext(), TrafficMode.DRIVING);
                dialog.cancel();
            }
        });

        startNaviationPRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<ParkingAreaDataFilter> filters = new ArrayList<>();
                filters.add(ParkingAreaDataFilter.PARK_AND_RIDE);
                AiParkApp
                        .getAiparkSDK()
                        .getApi()
                        .getParkingAreasForPosition(
                                new GetParkingAreasForPositionRequest(optimalTrip.getDestination(), 1, filters)
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Observer<GetParkingAreasForPositionResponse>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(GetParkingAreasForPositionResponse getParkingAreasForPositionResponse) {
                                List<LatLng> destination = new ArrayList<>();
                                for (ParkingArea p :
                                        getParkingAreasForPositionResponse.getParkingAreas()) {
                                    destination.add(new LatLng(p.getCenter().getY(), p.getCenter().getX()));
                                }
                                new MapBoxNavigation().startNavigationTo(destination, null,
                                        AiParkApp.getContext(), TrafficMode.DRIVING);
                            }
                        });
            }
        });

        detailsCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("optimalTrip", "close");
                dialog.cancel();
            }
        });
    }
}
