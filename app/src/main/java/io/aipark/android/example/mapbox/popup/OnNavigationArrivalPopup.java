package io.aipark.android.example.mapbox.popup;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;


/**
 * popup shown when navigation is finished and more destination are given, e.g. for an optimal trip
 */

public class OnNavigationArrivalPopup {
    public interface OnDecisionListener {
        void onFinalDestination();

        void onNextParkingArea();
    }

    public OnNavigationArrivalPopup(final OnDecisionListener onDecisionListener, boolean nextArea, boolean finalDestination) {
        if (!nextArea && !finalDestination) {
            return;
        }
        LayoutInflater inflater = AiParkApp.getActivity().getLayoutInflater();
        View detailsView = inflater.inflate(R.layout.on_arrival_popup_landscape, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(AiParkApp.getActivity());
        builder.setView(detailsView);
        final AlertDialog dialog = builder.create();
        try {
            dialog.show();
        } catch (Throwable t) {
            Log.e("Popup", "error popup creating", t);
            return;
        }

        Button onCancelButton = detailsView.findViewById(R.id.cancel_button);
        onCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        Button onFinalDestinationButton = detailsView.findViewById(R.id.final_destination_button);
        if (finalDestination) {
            onFinalDestinationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onDecisionListener.onFinalDestination();
                    dialog.cancel();
                }
            });
        } else {
            detailsView.findViewById(R.id.final_destination_layout).setVisibility(View.GONE);
        }
        Button onNextParkingAreaButton = detailsView.findViewById(R.id.next_area_button);
        if (nextArea) {
            onNextParkingAreaButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onDecisionListener.onNextParkingArea();
                    dialog.cancel();
                }
            });
        } else {
            detailsView.findViewById(R.id.next_area_button_layout).setVisibility(View.GONE);
        }
    }
}
