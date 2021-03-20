package com.kamilwnek.pracainzynierskakamilwnek;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.kamilwnek.pracainzynierskakamilwnek.Pedometer.Database;
import com.kamilwnek.pracainzynierskakamilwnek.Pedometer.DayAxisValueFormatter;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PedometerFragment extends Fragment implements SensorEventListener {

    View rootView;
    private SensorManager sensorManager;
    private TextView pedometerTextView;
    boolean isActivityRunning;
    int todayOffset=0, since_boot=0;
    public static int goal=10000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_pedometer, container, false);

        pedometerTextView = rootView.findViewById(R.id.pedometerTextView);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        pedometerTextView = rootView.findViewById(R.id.pedometerTextView);
        Database db = Database.getInstance(getActivity());
        todayOffset = db.getSteps(Common.getToday());
        Log.i("PedometerFragment", "OnResume. Today steps from db: " + db.getSteps(Common.getToday()));

        SharedPreferences prefs =
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        since_boot = db.getCurrentSteps();
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        isActivityRunning = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (countSensor != null){
            sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(getActivity(),"Could not find sensor!", Toast.LENGTH_LONG).show();
        }

        since_boot -= pauseDifference;
        db.close();

        updateSteps();
        updateBars();
    }

    private void updateSteps() {
        pedometerTextView = rootView.findViewById(R.id.pedometerTextView);

        int steps_today = Math.max(todayOffset + since_boot, 0);
        Log.i("PedometerFragment", String.valueOf(steps_today));
        pedometerTextView.setText(String.valueOf(steps_today));

        updatePieChart(steps_today);

        double stepLength = 0.75;
        if (ParseUser.getCurrentUser().getString("sex").matches("female")){
            stepLength = 0.7;
        }

        TextView distanceTextView = rootView.findViewById(R.id.distanceTextView);
        double distance = stepLength * steps_today;
        String distanceString = String.format(Locale.getDefault(),"%.1f",distance) + " [m]";
        if (distance > 1000)
            distanceString = String.format(Locale.getDefault(),"%.2f",distance/1000) + " [km]";

        distanceTextView.setText(distanceString);

        Double weight = Double.parseDouble(ParseUser.getCurrentUser().getString("weight"));
        TextView caloriesTextView = rootView.findViewById(R.id.caloriesTextView);
        Double calories = 0.53 * weight * distance/1000;
        caloriesTextView.setText(String.format(Locale.getDefault(),"%.0f",calories) + " kcal");
    }

    private void updatePieChart(int steps_today) {
        PieChart pieChart = rootView.findViewById(R.id.pieChart);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(42f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setRotationAngle(270);
        pieChart.setRotationEnabled(false);
        pieChart.setHighlightPerTapEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setExtraOffsets(9.f, 9.f, 9.f, 9.f);

        ArrayList<LegendEntry> legendEntries = new ArrayList<>();
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(steps_today));

        if (Math.max(goal-steps_today,0) != 0){
            entries.add(new PieEntry(Math.max(goal-steps_today,0)));
            LegendEntry entry = new LegendEntry();
            entry.label = getString(R.string.stepsLeft);
            entry.formColor = ContextCompat.getColor(getActivity(),R.color.stepsTarget);
            legendEntries.add(entry);
        }

        LegendEntry entry2 = new LegendEntry();
        entry2.label = getString(R.string.stepsToday);
        entry2.formColor = ContextCompat.getColor(getActivity(),R.color.dailySteps);
        legendEntries.add(entry2);

        PieDataSet pieDataSet = new PieDataSet(entries, "Steps today");

        List<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(getActivity(),R.color.dailySteps)); // green
        colors.add(ContextCompat.getColor(getActivity(),R.color.stepsTarget));  // red

        pieDataSet.setColors(colors);
        pieDataSet.setUsingSliceColorAsValueLineColor(true);
        pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieDataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieDataSet.setValueLineVariableLength(true);
        pieDataSet.setValueLinePart1Length(0.5f);
        pieDataSet.setValueLinePart2Length(0.5f);
        pieDataSet.setValueLineWidth(1.5f);
        pieDataSet.setSelectionShift(10f);
        pieDataSet.setSliceSpace(3f);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setCustom(legendEntries);

        PieData data = new PieData(pieDataSet);
        data.setValueTextSize(11f);

        float percent = (float) steps_today/goal * 100;

        pieChart.setCenterText(String.format(Locale.getDefault(),"%.1f",percent) + " %");
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void updateBars() {
        Database db = Database.getInstance(getActivity());
        List<Pair<Long, Integer>> last = db.getLastEntries(-1);
        db.close();

        CombinedChart barChart = rootView.findViewById(R.id.chart1);

        barChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.LINE,
                CombinedChart.DrawOrder.BAR,
        });

        ValueFormatter xAxisFormatter = new DayAxisValueFormatter(barChart,last);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // only intervals of 1 day
        xAxis.setLabelCount(7);
        xAxis.setValueFormatter(xAxisFormatter);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawZeroLine(true);
        leftAxis.setDrawLabels(true);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(true);

        ArrayList<LegendEntry> legendEntries = new ArrayList<>();

        LegendEntry entry = new LegendEntry();
        entry.label = getString(R.string.stepsTarget) + goal;
        entry.formColor = ContextCompat.getColor(getActivity(),R.color.stepsTarget);
        legendEntries.add(entry);

        LegendEntry entry2 = new LegendEntry();
        entry2.label = getString(R.string.dailySteps);
        entry2.formColor = ContextCompat.getColor(getActivity(),R.color.dailySteps);
        legendEntries.add(entry2);

        Legend legend = barChart.getLegend();
        legend.setDrawInside(true);
        legend.setEnabled(true);
        legend.setCustom(legendEntries);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(10f);
        legend.setDrawInside(false);

        barChart.getAxisRight().setEnabled(false);

        barChart.getDescription().setEnabled(false);
        barChart.setHorizontalScrollBarEnabled(false);
        barChart.setVerticalScrollBarEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setHighlightPerTapEnabled(false);

        barChart.moveViewToX(last.size()-1);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<Entry> lineEntries = new ArrayList<>();

        for (int i=last.size()-1; i>0; i--){
            entries.add(new BarEntry(last.size()-i,last.get(i).second));
            lineEntries.add(new Entry(last.size()-i,goal));
        }

        LineDataSet set = new LineDataSet(lineEntries, "Line DataSet");
        set.setDrawCircles(false);
        set.setColor(ContextCompat.getColor(getActivity(),R.color.stepsTarget));
        set.setLineWidth(1.5f);
        set.setDrawValues(false);

        LineData lineData = new LineData();
        lineData.addDataSet(set);

        BarDataSet barDataSet = new BarDataSet(entries, "Bar DataSet");
        barDataSet.setHighlightEnabled(false);
        barDataSet.setDrawValues(true);
        barDataSet.setValueTextSize(15);
        barDataSet.setColor(ContextCompat.getColor(getActivity(),R.color.dailySteps));

        BarData barData = new BarData(barDataSet);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        combinedData.setData(lineData);

        xAxis.setAxisMaximum(combinedData.getXMax() + 0.5f);
        xAxis.setAxisMinimum(combinedData.getXMin() - 0.5f);

        barChart.setData(combinedData); // set the data and list of labels into chart
        barChart.moveViewTo(last.size()-1,last.size()-1, YAxis.AxisDependency.LEFT);
        barChart.animateY(1000);
        barChart.setDrawValueAboveBar(true);
        barChart.zoomOut();
        barChart.setVisibleXRangeMaximum(5f);
        barChart.invalidate();
    }

    @Override
    public void onPause() {
        super.onPause();
        isActivityRunning = false;

        try {
            SensorManager sm =
                    (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.i("MainActivity",e.toString());
        }
        Database db = Database.getInstance(getActivity());
        db.saveCurrentSteps(since_boot);
        db.close();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!isActivityRunning)
            return;
        if (sensorEvent.values[0] > Integer.MAX_VALUE || sensorEvent.values[0] == 0)
            return;

        Database db = Database.getInstance(getActivity());

        if (db.getBackupFlag()){
            int todaySteps = db.getSteps(Common.getToday());
            db.insertDayFromBackup(Common.getToday(), -((int)sensorEvent.values[0] - todaySteps));
            db.resetBackupFlag();
            todayOffset = -((int)sensorEvent.values[0] - todaySteps);
            db.saveCurrentSteps((int)sensorEvent.values[0]);
        }

        if (db.getSteps(Common.getToday()) == 0) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) sensorEvent.values[0];
            db.insertNewDay(Common.getToday(), (int) sensorEvent.values[0]);
            db.close();
        }
        since_boot = (int) sensorEvent.values[0];
        updateSteps();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // won't happen
    }
}