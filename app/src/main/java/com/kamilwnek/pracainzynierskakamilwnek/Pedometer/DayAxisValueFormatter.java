package com.kamilwnek.pracainzynierskakamilwnek.Pedometer;

import android.util.Pair;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DayAxisValueFormatter extends ValueFormatter
{
    List<Pair<Long, Integer>> dataFromDB;
    ArrayList<String> dates;

    final BarLineChartBase<?> chart;

    public DayAxisValueFormatter(BarLineChartBase<?> chart, List<Pair<Long, Integer>> dataFromDB) {
        this.chart = chart;
        this.dataFromDB = dataFromDB;

        dates = new ArrayList<>();
        for (int i=dataFromDB.size()-1; i>0; i--){
            Date date = new Date(dataFromDB.get(i).first);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            String dateString = sdf.format(calendar.getTime());
            dates.add(dateString);
        }
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return super.getAxisLabel(value, axis);
    }

    @Override
    public String getFormattedValue(float value) {

        if (value<=dates.size())
            return dates.get((int)value - 1);
        else return "";
    }
}