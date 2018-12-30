package io.aipark.android.example.mapbox.popup;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.MarkerImage;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.FSize;
import com.google.maps.model.LatLng;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.aipark.api.occupancy.Occupancy;
import de.aipark.api.occupancy.OccupancyType;
import de.aipark.api.parkingarea.ParkingArea;
import de.aipark.api.parkingarea.ParkingAreaType;
import de.aipark.api.requestsResponse.getOccupancyForParkingAreas.GetOccupancyForParkingAreasRequest;
import de.aipark.api.requestsResponse.getOccupancyForParkingAreas.GetOccupancyForParkingAreasResponse;
import de.aipark.api.requestsResponse.getOccupancyForParkingAreas.RequestEntry;
import de.aipark.api.route.TrafficMode;
import io.aipark.android.example.mapbox.AiParkApp;
import io.aipark.android.example.mapbox.R;
import io.aipark.android.example.mapbox.map.mapBox.ParkingAreaResult;
import io.aipark.android.example.mapbox.navigation.mapbox.MapBoxNavigation;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * popup to show information about a paring area
 */

public class ParkingAreaPopup {
    public ParkingAreaPopup(final ParkingAreaResult parkingAreaResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AiParkApp.getActivity());

        LayoutInflater inflater = AiParkApp.getActivity().getLayoutInflater();
        View detailsView = inflater.inflate(R.layout.parking_area_details, null);
        AppCompatTextView parkingAreaName = (AppCompatTextView) detailsView.findViewById(R.id.details_parking_area_name);
        TextView disabledText = (TextView) detailsView.findViewById(R.id.textViewDisabled);
        TextView residentialText = (TextView) detailsView.findViewById(R.id.textViewResident);
        TextView womenText = (TextView) detailsView.findViewById(R.id.textViewWomen);
        TextView slotsText = (TextView) detailsView.findViewById(R.id.textViewSlots);
        TextView priceStringText = (TextView) detailsView.findViewById(R.id.textViewPriceString);
        TextView openingHoursText = (TextView) detailsView.findViewById(R.id.textViewOpeningHoursString);
        final TextView textViewChartHeading = (TextView) detailsView.findViewById(R.id.textViewChartHeading);

        ImageView imageViewDisabled = (ImageView) detailsView.findViewById(R.id.imageViewDisabled);
        ImageView imageViewResident = (ImageView) detailsView.findViewById(R.id.imageViewResident);
        ImageView imageViewWomen = (ImageView) detailsView.findViewById(R.id.imageViewWomen);
        ImageView imageViewSlots = (ImageView) detailsView.findViewById(R.id.imageViewSlots);

        LinearLayout linearLayoutGroupWomen = (LinearLayout) detailsView.findViewById(R.id.groupWomen);
        LinearLayout linearLayoutGroupDisabled = (LinearLayout) detailsView.findViewById(R.id.groupDisabled);
        LinearLayout linearLayoutGroupResident = (LinearLayout) detailsView.findViewById(R.id.groupResident);
        LinearLayout linearLayoutGroupSlots = (LinearLayout) detailsView.findViewById(R.id.groupSlots);
        LinearLayout linearLayoutGroupCosts = (LinearLayout) detailsView.findViewById(R.id.groupCosts);
        LinearLayout linearLayoutGroupOpeningHours = (LinearLayout) detailsView.findViewById(R.id.groupOpeningHours);
        LinearLayout linearLayoutGroupOtherAttr = (LinearLayout) detailsView.findViewById(R.id.groupOtherAttr);

        Button detailsCancelButton = (Button) detailsView.findViewById(R.id.cancel_button);
        Button navigationButton = detailsView.findViewById(R.id.navigationButton);
        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    List<LatLng> destination = new ArrayList<>();
                    destination.add(new LatLng(parkingAreaResult.getData().getCenter().getY(), parkingAreaResult.getData().getCenter().getX()));
                    LatLng finalDestination = null;

                    Log.d("navigation", "start");
                    new MapBoxNavigation().startNavigationTo(destination, finalDestination,
                            AiParkApp.getContext(), TrafficMode.DRIVING);
                } catch (NullPointerException n) {
                    Log.e("ResultcardsAdapter", n.getMessage());
                }
            }
        });

        View.OnClickListener shareOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String uri = "http://maps.google.com/maps?daddr=" + parkingAreaResult.getData().getCenter().getY() + "," + parkingAreaResult.getData().getCenter().getX();

                String aiparkUri = "deeplink.aipark.de/parkingArea?id=" + parkingAreaResult.getData().getId() + "&x=" + parkingAreaResult.getData().getCenter().getX() + "&y=" + parkingAreaResult.getData().getCenter().getY();

                String textToShare = parkingAreaResult.getData().getName() + "\nNavigation: " + uri;

                String openingString = parkingAreaResult.getData().getOpeningHoursString();

                String textToShare2 =
                        (parkingAreaResult.getData().getParkingAreaType().equals(ParkingAreaType.CAR_PARK) ? AiParkApp.getContext().getString(R.string.car_park_heading) : AiParkApp.getContext().getString(R.string.parking_area_heading)) + ":\n" +
                                parkingAreaResult.getData().getName() + "\n" +
                                "\n" +
                                (parkingAreaResult.getData().getPriceString().isEmpty() ? "" : (AiParkApp.getContext().getString(R.string.price) + " :\n" + parkingAreaResult.getData().getPriceString()) + "\n") +
                                (openingString.isEmpty() ? "" : (AiParkApp.getContext().getString(R.string.opening_hours) + " :\n" + openingString + "\n")) +
                                "\n" +
                                AiParkApp.getContext().getString(R.string.navigation_start_heading) + ":\n" +
                                "AIPARK" + ":\n" +
                                aiparkUri + "\n" +
                                "Google Maps" + ":\n" +
                                uri + "\n" +
                                "\n" +
                                AiParkApp.getContext().getString(R.string.find_parking_with_aipark) + ":\n" +
                                "\n" +
                                "Android:\n" + "https://play.google.com/store/apps/details?id=tu.bs.mobilelab.android.aipark\n" +
                                "\n" +
                                "iOS:\n" + "https://itunes.apple.com/de/app/aipark/id1198079295?mt=8\n";

                Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("text/plain");

                intent.putExtra(Intent.EXTRA_TEXT, textToShare2);

                AiParkApp.getContext().startActivity(Intent.createChooser(intent, "Share"));
            }
        };

        final ImageView share = (ImageView) detailsView.findViewById(R.id.shareView);
        share.setOnClickListener(shareOnClickListener);


        final LineChart chart = (LineChart) detailsView.findViewById(R.id.chartForecast);
        // TODO: put in string resource file
        chart.setNoDataText("Daten werden geladen");
        chart.setNoDataTextColor(ContextCompat.getColor(AiParkApp.getContext(), R.color.colorGreen));
        chart.invalidate();


        final long arrivalTime = (parkingAreaResult.getOccupancy().getTimestamp() != null) ? parkingAreaResult.getOccupancy().getTimestamp().getTime() : new Date().getTime();
        final Calendar arrivalCal = Calendar.getInstance();
        arrivalCal.setTimeInMillis(arrivalTime);
        final float arrivalHour = (float) arrivalCal.get(Calendar.HOUR_OF_DAY) + ((float) arrivalCal.get(Calendar.MINUTE) / 60f);
        getDayPredictionForParkingArea(parkingAreaResult.getData(), arrivalTime)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<GetOccupancyForParkingAreasResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        chart.setVisibility(View.GONE);
                        textViewChartHeading.setText(R.string.no_occupancies_available);
                    }

                    @Override
                    public void onNext(GetOccupancyForParkingAreasResponse parkingAreaPredictions) {
                        List<Entry> entries = new ArrayList<Entry>();
                        // forecast for arrival is implicitly first entry
                        Occupancy firstEntry = parkingAreaPredictions.getOccupancies().remove(0);

                        // handle forecast not available case
                        if (firstEntry.getValue() == -1 && firstEntry.getType().equals(OccupancyType.U)) {
                            chart.setVisibility(View.GONE);
                            textViewChartHeading.setText(R.string.no_occupancies_available);
                            return;
                        }

                        //textViewChartHeading.setText("Ankunft: " + arrivalHour);

                        int h = 0;
                        boolean liveValues = false;
                        for (Occupancy p : parkingAreaPredictions.getOccupancies()) {
                            if (p.getType().equals(OccupancyType.L) || p.getType().equals(OccupancyType.LP)) {
                                liveValues = true;
                            }
                        }
                        float[] colorThreshold = new float[5];
                        if (liveValues) {
                            textViewChartHeading.setText(R.string.numberOfFreeSpots);
                            float percentageForOneSpot = 1f / parkingAreaResult.getData().getCapacity() * 2f;
                            Log.i("colorline", "percentageForOneSpot: " + percentageForOneSpot);
                            float percentageFull = percentageForOneSpot * 100;
                            Log.i("colorline", "percentageFull: " + percentageFull);
                            for (int i = 0; i < 4; i++) {
                                colorThreshold[i] = 1f - (float) AiParkApp.getColorThresholds().get(4 - i) / 100f * percentageFull;
                                Log.i("colorline", "colorThreshold[" + i + "]: " + colorThreshold[i]);
                            }
                            colorThreshold[4] = 1f - (float) AiParkApp.getColorThresholds().get(0) / 100f;
                            Log.i("colorline", "colorThreshold[" + 4 + "]: " + colorThreshold[4]);

                            entries.add(new Entry(arrivalHour, firstEntry.getValue() / (float) parkingAreaResult.getData().getCapacity() * 100));
                            for (Occupancy p : parkingAreaPredictions.getOccupancies()) {
                                //Log.d("closeDebug","pred: " + p.getTime() + " : " + p.getType());
                                entries.add(new Entry(h++, Math.min(p.getValue() / (float) parkingAreaResult.getData().getCapacity() * 100, 100)));
                            }
                        } else {
                            for (int i = 0; i < 5; i++) {
                                colorThreshold[i] = 1f - (float) AiParkApp.getColorThresholds().get(4 - i) / 100f;
                                Log.i("colorline", "2. colorThreshold[" + i + "]: " + colorThreshold[i]);
                            }

                            entries.add(new Entry(arrivalHour, firstEntry.getValue()));
                            for (Occupancy p : parkingAreaPredictions.getOccupancies()) {
                                entries.add(new Entry(h++, p.getValue()));
                            }
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "Prognose");
                        dataSet.setDrawCircles(false);
                        dataSet.setDrawValues(false);
                        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                        dataSet.setCubicIntensity(0.1f);
                        dataSet.setLineWidth(2);
                        dataSet.setColors(ContextCompat.getColor(AiParkApp.getContext(), R.color.mainColor), ContextCompat.getColor(AiParkApp.getContext(), R.color.colorGreen));
                        dataSet.setDrawHighlightIndicators(false);

                        LineData lineData = new LineData(dataSet);

                        chart.setExtraOffsets(0, -40f, 0, 0);
                        chart.getDescription().setEnabled(false);
                        chart.getLegend().setEnabled(false);
                        chart.getAxisRight().setEnabled(false);
                        chart.animateY(1000);
                        MarkerImage marker = new MarkerImage(AiParkApp.getContext(), R.drawable.arrival);
                        marker.setSize(new FSize(100f, 100f));
                        marker.setOffset(-50f, -100f);
                        chart.setMarker(marker);

                        Paint paint = chart.getRenderer().getPaintRender();
                        LinearGradient linearGradient = new LinearGradient(0, 0, 0, chart.getHeight(),
                                new int[]{
                                        ContextCompat.getColor(AiParkApp.getContext(), R.color.colorGreen),
                                        ContextCompat.getColor(AiParkApp.getContext(), R.color.colorGreenYellow),
                                        ContextCompat.getColor(AiParkApp.getContext(), R.color.colorYellow),
                                        ContextCompat.getColor(AiParkApp.getContext(), R.color.colorOrange),
                                        ContextCompat.getColor(AiParkApp.getContext(), R.color.colorRed)}, colorThreshold,
                                Shader.TileMode.REPEAT);
                        paint.setShader(linearGradient);
                        YAxis yAxis = chart.getAxisLeft();
                        yAxis.setAxisMinimum(-5);
                        yAxis.setAxisMaximum(105);
                        yAxis.setGranularity(25);
                        yAxis.setDrawAxisLine(false);
                        // TODO: delete yAxis.setEnabled(false); (just added for testing)
                        yAxis.setDrawLabels(false);
                        yAxis.setTextColor(ContextCompat.getColor(AiParkApp.getContext(), R.color.grey_600));
                        yAxis.setGridColor(ContextCompat.getColor(AiParkApp.getContext(), R.color.grey_300));
                        XAxis xAxis = chart.getXAxis();
                        xAxis.setTextColor(ContextCompat.getColor(AiParkApp.getContext(), R.color.grey_600));
                        xAxis.setGranularity(5);
                        xAxis.setDrawAxisLine(false);
                        xAxis.setAxisMinimum(1);
                        xAxis.setGranularityEnabled(true);
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        yAxis.setSpaceTop(0);
                        xAxis.setDrawGridLines(false);
                        xAxis.setValueFormatter(new IAxisValueFormatter() {

                            @Override
                            public String getFormattedValue(float value, AxisBase axis) {
                                return String.format("%02d Uhr", (int) value);
                            }
                        });
                        yAxis.setValueFormatter(new IAxisValueFormatter() {
                            @Override
                            public String getFormattedValue(float value, AxisBase axis) {
                                return "" + (int) value + "%";
                            }
                        });

                        chart.setTouchEnabled(false);
                        chart.setScaleEnabled(false);
                        chart.setData(lineData);
                        chart.highlightValue(arrivalHour, 0);
                        chart.invalidate();
                    }
                });

        if (parkingAreaResult.getData().getCapacityDisabled() != null && parkingAreaResult.getData().getCapacityDisabled() > 0) {
            disabledText.setTextColor(AiParkApp.getActivity().getResources().getColor(R.color.simpleGrey));
        } else {
            disabledText.setTextColor(AiParkApp.getActivity().getResources().getColor(R.color.simpleGrey));
        }

        parkingAreaName.setText(parkingAreaResult.getData().getName());

        slotsText.setText(parkingAreaResult.getData().getCapacity() + " " + AiParkApp.getContext().getString(R.string.spots));

        String priceString = parkingAreaResult.getData().getPriceString().replace("\t", "\u0009\u0009");
        priceStringText.setText(priceString);
        priceStringText.setTypeface(Typeface.MONOSPACE);

        openingHoursText.setText(parkingAreaResult.getData().getOpeningHoursString());
        openingHoursText.setTypeface(Typeface.MONOSPACE);

        if (parkingAreaResult.getData().getSchedulePriceModel().getEntryList().size() < 1) {
            linearLayoutGroupCosts.setVisibility(View.GONE);
        }

        if (parkingAreaResult.getData().getScheduleOpen().getEntryList().size() < 1) {
            linearLayoutGroupOpeningHours.setVisibility(View.GONE);
        }

        if (parkingAreaResult.getData().getScheduleResidential().getEntryList().size() < 1) {
            linearLayoutGroupResident.setVisibility(View.GONE);
        }

        if (parkingAreaResult.getData().getCapacityDisabled() == null ||
                (parkingAreaResult.getData().getCapacityDisabled() != null && parkingAreaResult.getData().getCapacityDisabled() == 0)) {
            linearLayoutGroupDisabled.setVisibility(View.GONE);
        }

        if (parkingAreaResult.getData().getCapacityWoman() == null ||
                (parkingAreaResult.getData().getCapacityWoman() != null && parkingAreaResult.getData().getCapacityWoman() == 0)) {
            linearLayoutGroupWomen.setVisibility(View.GONE);
        }

        if (parkingAreaResult.getData().getScheduleResidential().getEntryList().size() < 1
                && (parkingAreaResult.getData().getCapacityDisabled() == null ||
                (parkingAreaResult.getData().getCapacityDisabled() != null && parkingAreaResult.getData().getCapacityDisabled() == 0))
                && (parkingAreaResult.getData().getCapacityWoman() == null ||
                (parkingAreaResult.getData().getCapacityWoman() != null && parkingAreaResult.getData().getCapacityWoman() == 0))) {
            linearLayoutGroupOtherAttr.setVisibility(View.GONE);
        }

        String maxStayString = parkingAreaResult.getData().getMaxStayString();
        if (!maxStayString.isEmpty()) {
            ((TextView) detailsView.findViewById(R.id.textViewMaxStay)).setText(maxStayString);
        } else {
            LinearLayout linearLayoutGroupOperator = detailsView.findViewById(R.id.groupMaxStay);
            linearLayoutGroupOperator.setVisibility(View.GONE);
        }

        builder.setView(detailsView);
        final AlertDialog dialog = builder.create();
        detailsCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        try {
            dialog.show();
        } catch (Throwable t) {
            return;
        }
    }

    public Observable<GetOccupancyForParkingAreasResponse> getDayPredictionForParkingArea(ParkingArea parkingAreaData, long arrivalTime) {
        List<Timestamp> timeStampList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        timeStampList.add(new Timestamp(arrivalTime));
        for (int i = 0; i <= 24; i++) {
            timeStampList.add(new Timestamp(cal.getTimeInMillis()));
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }

        List<RequestEntry> requestEntries = new ArrayList<>();
        for (Timestamp timestamp : timeStampList) {
            requestEntries.add(new RequestEntry(timestamp, parkingAreaData.getId()));
        }
        GetOccupancyForParkingAreasRequest getOccupancyForParkingAreasRequest = new GetOccupancyForParkingAreasRequest();
        getOccupancyForParkingAreasRequest.setTimeParkingAreaId(requestEntries);
        return AiParkApp.getAiparkSDK().getApi().getOccupancyForParkingAreas(getOccupancyForParkingAreasRequest);
    }
}
