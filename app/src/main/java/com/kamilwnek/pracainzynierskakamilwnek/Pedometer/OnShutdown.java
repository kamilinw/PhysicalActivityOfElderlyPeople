package com.kamilwnek.pracainzynierskakamilwnek.Pedometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kamilwnek.pracainzynierskakamilwnek.Common;
import com.parse.ParseUser;

public class OnShutdown extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ParseUser.getCurrentUser() == null){
            Log.i("OnShutdown", "User not found");
            return;
        }
        Log.i("OnShutdown", "saving steps to db");

        context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                .putBoolean("correctShutdown", true).commit();

        Database db = Database.getInstance(context);
        if (db.getSteps(Common.getToday()) == 0) {
            int steps = db.getCurrentSteps();
            db.insertNewDay(Common.getToday(), steps);
        } else {
            db.addToLastEntry(db.getCurrentSteps());
        }
        db.close();
    }
}