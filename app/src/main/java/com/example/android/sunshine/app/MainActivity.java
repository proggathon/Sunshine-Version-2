package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Create an intent to start a new settings activity.
            Intent settingsScreenIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsScreenIntent);

            return true;
        }
        else if (id == R.id.action_map) {
            // Show the map.
            showPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * Implicit intent to display the given location on a map.
     */
    private void showPreferredLocationInMap() {
        // Get the preferred location.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String locationPostalCode = preferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        // Convert to input format of intent.
        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", locationPostalCode)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        // Check that an app exists that can resolve intent.
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + locationPostalCode + ", intent not accepted by other app");
        }
    }

}
