package com.kamilwnek.pracainzynierskakamilwnek.Pedometer;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.kamilwnek.pracainzynierskakamilwnek.BuildConfig;
import com.kamilwnek.pracainzynierskakamilwnek.Common;
import com.kamilwnek.pracainzynierskakamilwnek.MainActivity;
import com.kamilwnek.pracainzynierskakamilwnek.R;

import java.util.Calendar;
import java.util.Date;


public class SensorListener extends Service implements SensorEventListener {

    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";
    private final static long SAVE_OFFSET_TIME = AlarmManager.INTERVAL_HALF_HOUR;
    private final static int SAVE_OFFSET_STEPS = 50;
    private static int steps;
    private static int lastSaveSteps;
    private static long lastSaveTime;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.values[0] > Integer.MAX_VALUE) {
            if (BuildConfig.DEBUG) Log.i("SensorListener","probably not a real value: " + sensorEvent.values[0]);
            return;
        } else {
            steps = (int) sensorEvent.values[0];
            updateIfNecessary();
        }
    }

    private boolean updateIfNecessary() {
        Database db = Database.getInstance(this);
        if (db.getBackupFlag()){
            int todaySteps = db.getSteps(Common.getToday());
            db.insertDayFromBackup(Common.getToday(), -(steps - todaySteps));
            db.resetBackupFlag();
            Log.i("SensorListener",
                    "restoring today steps from backup: " + todaySteps +
                            ", Putting value into today steps: " + -(steps-todaySteps));
            db.saveCurrentSteps(steps);
        }
        if (steps > lastSaveSteps + SAVE_OFFSET_STEPS ||
                (steps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME)) {
            if (BuildConfig.DEBUG) Log.i("SensorListener",
                    "saving steps: steps=" + steps + " lastSave=" + lastSaveSteps +
                            " lastSaveTime=" + new Date(lastSaveTime));


            if (db.getSteps(Common.getToday()) == 0) {
                int pauseDifference = steps -
                        getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                                .getInt("pauseCount", steps);
                db.insertNewDay(Common.getToday(), steps - pauseDifference);
                if (pauseDifference > 0) {
                    getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("pauseCount", steps).commit();
                }
            }
            db.saveCurrentSteps(steps);
            db.close();
            lastSaveSteps = steps;
            lastSaveTime = System.currentTimeMillis();
            return true;
        } else {
            db.close();
            return false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // won't happen
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("SensorListener","Service started");

        reRegisterSensor();
        startForeground();
        PedometerNotification();

        return super.onStartCommand(intent, flags, startId);
    }

    public void PedometerNotification(){

        Calendar alarmStartTime = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        alarmStartTime.set(Calendar.HOUR_OF_DAY, 18);
        alarmStartTime.set(Calendar.MINUTE, 30);
        alarmStartTime.set(Calendar.SECOND, 00);
        if (now.after(alarmStartTime)) {
            alarmStartTime.add(Calendar.DATE, 1);
        }
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this,NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,alarmStartTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY,pendingIntent);
        }
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            NotificationManager manager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel =
                    new NotificationChannel(NOTIF_CHANNEL_ID, NOTIF_CHANNEL_ID,
                            NotificationManager.IMPORTANCE_NONE);
            channel.setImportance(NotificationManager.IMPORTANCE_MIN);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setBypassDnd(false);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);

            startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                    NOTIF_CHANNEL_ID) // don't forget create a notification channel first
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.steps)
                    .setContentTitle(getString(R.string.applicationName))
                    .setContentText(getString(R.string.serviceBackground))
                    .setContentIntent(pendingIntent)
                    .build());

            Log.i("SensorListener", "starting notification");
        } else {
            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                    NOTIF_CHANNEL_ID)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.steps)
                    .setContentTitle(getString(R.string.applicationName))
                    .setContentText(getString(R.string.serviceBackground))
                    .setContentIntent(pendingIntent)
                    .build());

            Log.i("SensorListener", "starting notification");
        }
    }


    private void reRegisterSensor() {
        if (BuildConfig.DEBUG) Log.i("SensorListener","re-register sensor listener");
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.i("SensorListener",e.toString());
            e.printStackTrace();
        }

        if (BuildConfig.DEBUG) {
            Log.i("SensorListener","step sensors: " + sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size());
            if (sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size() < 1) return; // emulator
            Log.i("SensorListener","default: " + sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).getName());
        }

        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL, (int) (5 * MICROSECONDS_IN_ONE_MINUTE));
    }
}