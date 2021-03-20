package com.kamilwnek.pracainzynierskakamilwnek;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

public class Common {

    /**
     * Check if there is an internet connection. If there is not, displays
     * alert dialog.
     * @param context context
     * @return Returns true if there is internet connection. Otherwise false.
     */
    public static boolean checkInternetConnection(Context context){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        boolean connection = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (connection){
            return true;
        } else {
            infoAlertDialog(context, R.string.noInternetConnection);
            return false;
        }
    }

    /**
     * Builds an info alert dialog
     * @param context context
     * @param title Alert dialog title
     */
    public static void infoAlertDialog(Context context, int title){
        new AlertDialog.Builder(context)
                .setMessage(title)
                .setPositiveButton("OK",null)
                .show();
    }

    /**
     * @return milliseconds since 1.1.1970 for today 0:00:00 local timezone
     */
    public static long getToday() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /**
     * Converts distance in double into String variable with suitable format and unit
     * @param distance Value you want to convert
     * @return String value of distance with unit
     */
    public static String distanceFormatter (double distance){
        String distanceString;
        if (distance<1000){
            distanceString = String.format(Locale.getDefault(),"%.1f",distance) + " m";

        } else {
            distanceString = String.format(Locale.getDefault(),"%.2f",distance/1000) + " km";
        }
        return distanceString;
    }

    /**
     * Formats given time in seconds to string value
     * @param sec time in seconds
     * @return formatted time as string
     */
    public static String timeFormatter (int sec){
        String timeString;
        if (sec > 3600){
            timeString = String.format(Locale.getDefault(),"%02d", sec/3600) + ":" +
                    String.format(Locale.getDefault(),"%02d", (sec/60)%60) + ":" +
                    String.format(Locale.getDefault(),"%02d", sec%60);
        } else {
            timeString = "00:"+String.format(Locale.getDefault(),"%02d", sec/60) + ":" +
                    String.format(Locale.getDefault(),"%02d", sec%60);
        }
        return timeString;
    }
}