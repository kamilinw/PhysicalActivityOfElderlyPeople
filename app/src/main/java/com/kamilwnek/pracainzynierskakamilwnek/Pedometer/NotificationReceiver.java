package com.kamilwnek.pracainzynierskakamilwnek.Pedometer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.kamilwnek.pracainzynierskakamilwnek.Common;
import com.kamilwnek.pracainzynierskakamilwnek.MainActivity;
import com.kamilwnek.pracainzynierskakamilwnek.PedometerFragment;
import com.kamilwnek.pracainzynierskakamilwnek.R;
import com.parse.ParseUser;

public class NotificationReceiver extends BroadcastReceiver {

    Context mContext;
    private static final String NOTIFICATION_CHANNEL_ID = "10002";

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        Log.i("NotificationReceiver", "Broadcast received");

        createNotification();
    }

    void createNotification()
    {
        if (Build.VERSION.SDK_INT >= 26) {
            Intent notificationIntent = new Intent(mContext, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                    notificationIntent, 0);

            NotificationManager manager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel =
                    new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME",
                            NotificationManager.IMPORTANCE_DEFAULT);
            channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(false);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200});
            channel.setBypassDnd(false);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);

            String notificationTitle = mContext.getResources().getString(R.string.pedometerNotificationTitle);
            String notificationContent = getNotificationContent();

            manager.notify(0, new NotificationCompat.Builder(mContext,
                    NOTIFICATION_CHANNEL_ID)
                    .setOngoing(false)
                    .setSmallIcon(R.drawable.ic_directions_bike_black_24dp)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build());

            Log.i("SensorListener", "starting notification");
        } else {
            Intent notificationIntent = new Intent(mContext, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                    notificationIntent, 0);

            NotificationManager manager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            String notificationTitle = mContext.getResources().getString(R.string.pedometerNotificationTitle);
            String notificationContent = getNotificationContent();

            manager.notify(0, new NotificationCompat.Builder(mContext,
                    NOTIFICATION_CHANNEL_ID)
                    .setOngoing(false)
                    .setSmallIcon(R.drawable.ic_directions_bike_black_24dp)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build());

            Log.i("SensorListener", "starting notification");
        }
    }

    private String getNotificationContent() {
        String content;
        Database db = Database.getInstance(mContext);
        int todayOffset = db.getSteps(Common.getToday());
        int since_boot = db.getCurrentSteps();
        int steps_today = Math.max(todayOffset + since_boot, 0);
        String sex = ParseUser.getCurrentUser().getString("sex");
        int goal = PedometerFragment.goal;

        String stepsString = nounVariation(sex, steps_today);

        if(steps_today < goal/4){
            content = mContext.getResources().getString(R.string.notifPedometerContent1) + " " + stepsString;
        } else if (steps_today >= goal/4 && steps_today < goal/2){
            content = mContext.getResources().getString(R.string.notifPedometerContent2) + " " + stepsString;
        } else if (steps_today >= goal/2 && steps_today < 3*goal/4){
            content = mContext.getResources().getString(R.string.notifPedometerContent3) + " " + stepsString;
        } else if (steps_today >= 3*goal/4 && steps_today < goal) {
            content = mContext.getResources().getString(R.string.notifPedometerContent4) + " " + stepsString;
        } else {
            content = mContext.getResources().getString(R.string.notifPedometerContent5) + " " + stepsString;
        }

        return content;
    }

    private String nounVariation(String sex, int value) {
        String verb = "Dzisiaj przebyłeś ";
        if (sex.matches("female"))  verb = "Dzisiaj przebyłaś ";

        if (value == 1){
            return verb + value + " krok";
        } else if (value % 10 >= 2 && value % 10 <= 4 && (value < 10 || value > 20)){
            return verb + value + " kroki";
        } else
            return verb + value + " kroków";
    }
}
