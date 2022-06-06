package com.mantra.BiometricAttendance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mantra.BiometricAttendance.database.*;

public class MainActivity extends Activity {

    Button btnAttendance, btnRegistration;
    DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);

        btnAttendance = (Button) findViewById(R.id.btnAttendance);
        btnRegistration = (Button) findViewById(R.id.btnRegistration);
    }

    public void onControlClicked(View v) {

        switch (v.getId()) {
            case R.id.btnAttendance:
                GoToAttendance();
                break;
            case R.id.btnRegistration:
                GoToRegistration();
                break;
            default:
                break;
        }
    }

    private void GoToAttendance()
    {
        Toast.makeText(this, "Thread opening for Page Attendance.",
                Toast.LENGTH_LONG).show();
        Intent i = new Intent(getApplicationContext(), MFS100MatchFinger.class);
        startActivity(i);
    }

    private void GoToRegistration()
    {
        Toast.makeText(this, "Thread opening for Page Registration",
                Toast.LENGTH_LONG).show();
        Intent i = new Intent(getApplicationContext(), MFS100Registeration.class);
        startActivity(i);
    }
}
