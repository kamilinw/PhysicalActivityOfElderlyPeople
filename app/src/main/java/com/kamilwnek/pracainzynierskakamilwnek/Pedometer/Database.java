package com.kamilwnek.pracainzynierskakamilwnek.Pedometer;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import com.kamilwnek.pracainzynierskakamilwnek.BuildConfig;
import com.kamilwnek.pracainzynierskakamilwnek.Common;
import com.kamilwnek.pracainzynierskakamilwnek.WelcomeActivity;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Database extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "Pedometer";

    private Context context;
    private static Database instance;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + DATABASE_NAME + " (date INTEGER, steps INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        if (i == 1) {
            // drop PRIMARY KEY constraint
            db.execSQL("CREATE TABLE " + DATABASE_NAME + "2 (date INTEGER, steps INTEGER)");
            db.execSQL("INSERT INTO " + DATABASE_NAME + "2 (date, steps) SELECT date, steps FROM " +
                    DATABASE_NAME);
            db.execSQL("DROP TABLE " + DATABASE_NAME);
            db.execSQL("ALTER TABLE " + DATABASE_NAME + "2 RENAME TO " + DATABASE_NAME + "");
        }
    }

    public static synchronized Database getInstance(final Context c) {
        if (instance == null) {
            instance = new Database(c.getApplicationContext());
        }

        return instance;
    }

    /**
     * Inserts a new entry in the database, if there is no entry for the given
     * date yet. Steps should be the current number of steps and it's negative
     * value will be used as offset for the new date. Also adds 'steps' steps to
     * the previous day, if there is an entry for that date. Also updates data
     * to server.
     * <p/>
     * To restore data from a backup, use {@link #insertDayFromBackup}
     *
     * @param date  the date in ms since 1970
     * @param steps the current step value to be used as negative offset for the
     *              new day; must be >= 0
     */
    public void insertNewDay(long date, int steps) {
        getWritableDatabase().beginTransaction();
        try {
            Cursor cDate = getReadableDatabase().query(DATABASE_NAME, new String[]{"date"}, "date = ?",
                    new String[]{String.valueOf(date)}, null, null, null);
            if (cDate.getCount() == 0 && steps >= 0) {

                // add 'steps' to yesterdays count
                addToLastEntry(steps);

                // add today
                ContentValues values = new ContentValues();
                values.put("date", date);

                // use the negative steps as offset
                values.put("steps", -steps);
                getWritableDatabase().insert(DATABASE_NAME, null, values);

                // update data to parse server
                updateOnServer(false, null);
            }
            cDate.close();
            if (BuildConfig.DEBUG) {
                Log.i("Database","insertDay " + date + " / " + steps);
            }
            getWritableDatabase().setTransactionSuccessful();
        } finally {
            getWritableDatabase().endTransaction();
        }
    }

    /**
     * Uploads data on Parse Server. If parameter logOut is true it logs out
     * current user
     * @param logOut true if you want to log out after uploading data
     * @param context context if logOut is true. null when logOut is false
     */
    public void updateOnServer(final boolean logOut, final Context context) {
        Cursor cursor = getReadableDatabase().query(DATABASE_NAME, new String[]{"date","steps"}, null,
                null, null, null, "date ASC");

        final List<Long> date = new ArrayList<>();
        final List<Integer> steps = new ArrayList<>();

        if (cursor.getCount() > 0){
            cursor.moveToFirst();
            do {
                Log.i("Database UpdateOnServer", cursor.getLong(0) + " - " + cursor.getInt(1) + " steps");
                date.add(cursor.getLong(0));
                steps.add(cursor.getInt(1));
            } while (cursor.moveToNext());

        } else {
            Log.i("Database UpdateOnServer", "Error, cursor is equal to: " + cursor.getCount());
            if (logOut){
                ParseUser.logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null){
                            Intent intent = new Intent(context, WelcomeActivity.class);
                            context.startActivity(intent);
                            deleteData();

                        } else {
                            Log.i("Database", "log out in background failed with error: " + e.toString());
                        }
                    }
                });

            }
            return;
        }
        if (date.get(0) == -1){
            steps.set(steps.size()-1, steps.get(steps.size()-1) + steps.get(0));
            date.remove(0);
            steps.remove(0);
        } else {
            steps.set(steps.size()-1, 0);
        }
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedometer");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null){
                    if (objects.size() > 0){
                        Log.i("Database", "Updating object to server");
                        objects.get(0).put("username", ParseUser.getCurrentUser().getUsername());
                        objects.get(0).put("steps", Arrays.asList(steps));
                        objects.get(0).put("date", Arrays.asList(date));
                        objects.get(0).saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null){
                                    Log.i("Database", "Saving database on server completed successful");
                                } else {
                                    Log.i("Database", "Saving database on server failed witch error: " + e.toString());

                                }
                            }
                        });
                    } else {
                        Log.i("Database", "Adding new object to server");

                        ParseObject parseObject = new ParseObject("Pedometer");
                        parseObject.put("username", ParseUser.getCurrentUser().getUsername());
                        parseObject.addAllUnique("steps", Arrays.asList(steps));
                        parseObject.addAllUnique("date", Arrays.asList(date));
                        parseObject.saveEventually(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null){
                                    Log.i("Database", "Saving database on server completed successful");
                                } else {
                                    Log.i("Database", "Saving database on server failed witch error: " + e.toString());
                                }
                            }
                        });
                    }
                    if (logOut){
                        ParseUser.logOutInBackground(new LogOutCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null){
                                    Intent intent = new Intent(context, WelcomeActivity.class);
                                    context.startActivity(intent);
                                    deleteData();
                                } else {
                                    Log.i("Database", "log out in background failed with error: " + e.toString());
                                }
                            }
                        });
                    }
                } else {
                    Log.i("Database", "update on server query failed with error: " + e.toString());
                    if (logOut){
                        ParseUser.logOutInBackground(new LogOutCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null){
                                    Intent intent = new Intent(context, WelcomeActivity.class);
                                    context.startActivity(intent);
                                    deleteData();
                                } else {
                                    Log.i("Database", "log out in background failed with error: " + e.toString());
                                }
                            }
                        });
                    }
                }
            }
        });


    }

    /**
     * Download data from Parse Server and save them in local database
     */
    public void downloadDataFromServer (){


        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedometer");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size()>0){
                    final JSONArray[] dateJSON = new JSONArray[1];
                    final JSONArray[] stepsJSON = new JSONArray[1];
                    dateJSON[0] = objects.get(0).getJSONArray("date");
                    stepsJSON[0] = objects.get(0).getJSONArray("steps");

                        try {
                            if (dateJSON[0].getJSONArray(0).length() > 0 &&
                                    dateJSON[0].getJSONArray(0).length() == stepsJSON[0].getJSONArray(0).length()){

                                for (int i=0; i<dateJSON[0].getJSONArray(0).length(); i++){

                                    Log.i("Database", i+1 + " row of lists: " + dateJSON[0].getJSONArray(0).getLong(i) +
                                            ", " + stepsJSON[0].getJSONArray(0).getInt(i));
                                    insertDayFromBackup(dateJSON[0].getJSONArray(0).getLong(i),stepsJSON[0].getJSONArray(0).getInt(i));
                                }

                                setBackupFlag();

                            } else {
                                Log.i("Database", "DownloadDataFromServer: invalid data! ");
                            }
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }

                    Log.i("Database", "downloadDataFromServer, find in background success: " + dateJSON[0].toString());
                    Log.i("Database", "downloadDataFromServer, find in background success: " + stepsJSON[0].toString());
                } else if (e != null) {
                    Log.i("Database", "downloadDataFromServer, find in background failed with error: " + e.toString());
                }
            }
        });

    }

    /**
     * Sets backup flag in SharedPreferences when data is recovered from server
     */
    public void setBackupFlag() {

        context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                .putBoolean("isBackupDone", true).commit();

        if (BuildConfig.DEBUG) {
            SharedPreferences prefs =
                    context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);

            boolean b = prefs.getBoolean("isBackupDone", false);
            Log.i("Database","backup flag is set " + b);
        }
    }

    /**
     * Resets backup flag in SharedPreferences when data is recovered from server
     */
    public void resetBackupFlag() {
        context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                .putBoolean("isBackupDone", false).commit();

        if (BuildConfig.DEBUG) {
            SharedPreferences prefs =
                    context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);

            boolean b = prefs.getBoolean("isBackupDone", false);
            Log.i("Database","backup flag is reset " + b);
        }
    }

    /**
     * Gives an information if there was a data recovered from cloud
     * @return true if flag is set, false if fag is not set
     */
    public boolean getBackupFlag(){
        SharedPreferences prefs =
                context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        return prefs.getBoolean("isBackupDone", false);
    }

    /**
     * Deletes all data in local database
     */
    public void deleteData(){
        getReadableDatabase().delete(DATABASE_NAME,null,null);
    }

    /**
     * Adds the given number of steps to the last entry in the database
     *
     * @param steps the number of steps to add
     */
    public void addToLastEntry(int steps) {
        getWritableDatabase().execSQL("UPDATE " + DATABASE_NAME + " SET steps = steps + " + steps +
                " WHERE date = (SELECT MAX(date) FROM " + DATABASE_NAME + ")");
    }

    /**
     * Removes all entries with negative values.
     * <p/>
     * Only call this directly after boot, otherwise it might remove the current
     * day as the current offset is likely to be negative
     */
    void removeNegativeEntries() {
        getWritableDatabase().delete(DATABASE_NAME, "steps < ?", new String[]{"0"});
    }

    /**
     * Inserts a new entry in the database, overwriting any existing entry for the given date.
     * Use this method for restoring data from a backup.
     *
     * @param date  the date in ms since 1970
     * @param steps the step value for 'date'; must be >= 0
     * @return true if a new entry was created, false if there was already an
     * entry for 'date' (and it was overwritten)
     */
    public boolean insertDayFromBackup(long date, int steps) {
        getWritableDatabase().beginTransaction();
        boolean newEntryCreated = false;
        try {
            ContentValues values = new ContentValues();
            if (date == Common.getToday()){
                int stepsFromReboot = getCurrentSteps();
                values.put("steps", -(stepsFromReboot - steps));
            } else {
                values.put("steps", steps);
            }


            int updatedRows = getWritableDatabase()
                    .update(DATABASE_NAME, values, "date = ?", new String[]{String.valueOf(date)});
            if (updatedRows == 0) {
                values.put("date", date);
                getWritableDatabase().insert(DATABASE_NAME, null, values);
                newEntryCreated = true;
            }
            getWritableDatabase().setTransactionSuccessful();
        } finally {
            getWritableDatabase().endTransaction();
        }
        return newEntryCreated;
    }

    /**
     * Gets the last num entries in descending order of date (newest first)
     *
     * @param num the number of entries to get. if num is -1 function gets all entries
     * @return a list of long,integer pair - the first being the date, the second the number of steps
     */
    public List<Pair<Long, Integer>> getLastEntries(int num) {
        Cursor c;
        if (num==-1){
            c = getReadableDatabase()
                    .query(DATABASE_NAME, new String[]{"date", "steps"}, "date > 0", null, null, null,
                            "date DESC", null);
        }else {
            c = getReadableDatabase()
                    .query(DATABASE_NAME, new String[]{"date", "steps"}, "date > 0", null, null, null,
                            "date DESC", String.valueOf(num));
        }

        int max = c.getCount();
        List<Pair<Long, Integer>> result = new ArrayList<>(max);
        if (c.moveToFirst()) {
            do {
                result.add(new Pair<>(c.getLong(0), c.getInt(1)));
            } while (c.moveToNext());
        }
        return result;
    }

    /**
     * Get the number of steps taken for a specific date.
     * <p/>
     * If date is Common.getToday(), this method returns the offset which needs to
     * be added to the value returned by getCurrentSteps() to get todays steps.
     *
     * @param date the date in millis since 1970
     * @return the steps taken on this date or Integer.MIN_VALUE if date doesn't
     * exist in the database
     */
    public int getSteps(final long date) {
        Cursor c = getReadableDatabase().query(DATABASE_NAME, new String[]{"steps"}, "date = ?",
                new String[]{String.valueOf(date)}, null, null, null);
        c.moveToFirst();
        int re;
        if (c.getCount() == 0) re = 0; //TODO if (c.getCount() == 0) re = Integer.MIN_VALUE;
        else re = c.getInt(0);
        c.close();
        return re;
    }

    /**
     * Saves the current 'steps since boot' sensor value in the database.
     *
     * @param steps since boot
     */
    public void saveCurrentSteps(int steps) {
        ContentValues values = new ContentValues();
        values.put("steps", steps);
        if (getWritableDatabase().update(DATABASE_NAME, values, "date = -1", null) == 0) {
            values.put("date", -1);
            getWritableDatabase().insert(DATABASE_NAME, null, values);

            if (BuildConfig.DEBUG) {
                Log.i("Database","saving steps in db: " + steps);
            }

        }
    }

    /**
     * Reads the latest saved value for the 'steps since boot' sensor value.
     *
     * @return the current number of steps saved in the database or 0 if there
     * is no entry
     */
    public int getCurrentSteps() {
        int re = getSteps(-1);
        return re == Integer.MIN_VALUE ? 0 : re;
    }

    /**
     * Get the maximum of steps walked in one day
     *
     * @return the maximum number of steps walked in one day
     */
    public int getRecord() {
        Cursor c = getReadableDatabase()
                .query(DATABASE_NAME, new String[]{"MAX(steps)"}, "date > 0", null, null, null, null);
        c.moveToFirst();
        int re = 0;
        if (c.getInt(0) >= 0)
            re = c.getInt(0);
        c.close();
        return re;
    }

    /**
     * Get the value of all steps taken since the beginning
     *
     * @return the value of all steps taken since the beginning
     */
    public long getAllSteps (){
        long allSteps = 0;
        List<Pair<Long, Integer>> allDays = getLastEntries(-1);

        if (allDays.size()>1){
            for (int i=1; i<allDays.size(); i++){
                allSteps = allSteps + allDays.get(i).second;
            }
        }
        return allSteps;
    }

    /**
     * Get the average steps taken per day
      * @return the value of average steps per day
     */
    public int getAverageSteps(){
        long allSteps = 0;
        List<Pair<Long, Integer>> allDays = getLastEntries(-1);
        int averageSteps = 0;

        if (allDays.size()>1){
            for (int i=1; i<allDays.size(); i++){
                allSteps = allSteps + allDays.get(i).second;

            }
            averageSteps = (int)allSteps/allDays.size();
        }

        return averageSteps;
    }
}
