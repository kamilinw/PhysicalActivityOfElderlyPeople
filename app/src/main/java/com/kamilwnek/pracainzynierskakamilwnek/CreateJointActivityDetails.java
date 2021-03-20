package com.kamilwnek.pracainzynierskakamilwnek;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

public class CreateJointActivityDetails extends AppCompatActivity {

    EditText editTextDate;
    EditText editTextActivity;
    DatePickerDialog.OnDateSetListener date;
    NumberPicker np;
    int activityID;

    public void onClickGetCoordinates(View view){
        if (!editTextDate.getText().toString().matches("") && !editTextActivity.getText().toString().matches("")){

            Intent intent = new Intent(getApplicationContext(),CreateJointActivityMap.class);
            intent.putExtra("date", np.getValue());
            intent.putExtra("activityID", activityID);
            startActivityForResult(intent,1);
        } else
            Toast.makeText(this, getResources().getString(R.string.fillInDetails), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1){
            if (resultCode == 1){
                Intent returnIntent = new Intent();
                setResult(1,returnIntent);
                finish();
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_joint_details);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.createJointActivity);

        editTextDate = findViewById(R.id.editTextDate);
        editTextActivity = findViewById(R.id.editTextActivity);
        activityID = -1;
        editTextDate.setShowSoftInputOnFocus(false);
        editTextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        editTextActivity.setShowSoftInputOnFocus(false);
        editTextActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(CreateJointActivityDetails.this);
                builder.setTitle(R.string.selectActivity)
                        .setSingleChoiceItems(R.array.activities, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                editTextActivity.setText(getResources().getStringArray(R.array.activities)[i]);
                                activityID = i;
                                dialogInterface.dismiss();
                            }
                        });

                builder.create();
                AlertDialog mDialog = builder.create();
                mDialog.show();
            }
        });
    }

    private void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.number_picker, null);
        np = view.findViewById(R.id.numberPicker1);
        np.setMaxValue(300);
        np.setMinValue(5);
        np.setWrapSelectorWheel(false);
        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editTextDate.setText(String.valueOf(np.getValue()));
                    }
                })
                .setNegativeButton(R.string.camcel, null)
                .setTitle(R.string.selectTime)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }
}