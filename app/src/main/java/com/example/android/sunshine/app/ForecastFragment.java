package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by Jonsson on 2016-08-28.
 */
public class ForecastFragment extends Fragment {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Notify that this fragment has an options menu.
        setHasOptionsMenu(true);

        Log.d(LOG_TAG, "OnCreate called.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(LOG_TAG, "OnDestroy called.");
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(LOG_TAG, "OnResuom called.");
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(LOG_TAG, "onPause called.");
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();

        Log.d(LOG_TAG, "onStart called.");
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(LOG_TAG, "onStop called.");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Create dummy data for weather.
        ArrayList<String> weatherStrings = new ArrayList<String>();
        weatherStrings.add("Today . Cold .");
        weatherStrings.add("Tomorrow . Somewhat okay .");
        weatherStrings.add("Tuesday . Somewhat okayer .");
        weatherStrings.add("Wednesday . Cold again .");
        weatherStrings.add("Thursday . Warm! .");
        weatherStrings.add("Friday . Warmer!! .");
        weatherStrings.add("Saturday . Dip .");

        // Crete an ArrayAdapter to convert raw data into items in a View that inherits
        // from AdapterView (such as ListView).
        mForecastAdapter = new ArrayAdapter<String>(
                // Current context (fragment's parent activity)
                getActivity(),
                // ID of list item layout
                R.layout.list_item_forecast,
                // ID of the TextView to populate
                R.id.list_item_forecast_textview, // Question: What happens if this textview is not within the given layout item (list_item_forecast)?
                // Forecast data
                new ArrayList<String>());

        // Find reference to ListView
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        // Assign an adapter to the ListView
        listView.setAdapter(mForecastAdapter);

        // Set a click listener such that stuff may happen when items are clicked in the list.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the text from the clicked item.
                String clickedContent = mForecastAdapter.getItem(position);

                // Present a toast of the text.
                //Toast toast = Toast.makeText(getActivity(), clickedContent, Toast.LENGTH_SHORT);
                //toast.setGravity(Gravity.BOTTOM, 0, 10);
                //toast.show();

                // Create an intent to start a new activity presenting the weather details.
                Intent detailedScreenIntent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, clickedContent);
                startActivity(detailedScreenIntent);
            }
        });

        return rootView;
    }

    public static double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex)
        throws JSONException {
        JSONObject weather = new JSONObject(weatherJsonStr);
        JSONArray days = weather.getJSONArray("list");
        JSONObject dayInfo = days.getJSONObject(dayIndex);
        JSONObject temperatureInfo = dayInfo.getJSONObject("temp");
        return temperatureInfo.getDouble("max");
    }


    private String createOpenWeatherURL(String postalCode, int numberOfDays) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("http")
                .authority("api.openweathermap.org")
                .appendPath("data")
                .appendPath("2.5")
                .appendPath("forecast")
                .appendPath("daily")
                .appendQueryParameter("q", postalCode)
                .appendQueryParameter("mode", "json")
                .appendQueryParameter("units", "metric")
                .appendQueryParameter("cnt", Integer.toString(numberOfDays))
                .appendQueryParameter("APPID", "4bea9adc6b526574133f4fa26a967c7b");

        String urlString = uriBuilder.build().toString();
        return urlString;
    }

    private void updateWeather() {
        // Get the preference object,
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Fetch the postal code string.
        String locationPostalCode = preferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        // Execute the task of fetching weather data.
        new FetchWeatherTask().execute(locationPostalCode);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        // Implement doInBackground
        @Override
        protected String[] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String[] weatherStrings;

            try {
                // Get API call string.
                String URLstring = createOpenWeatherURL(params[0], 7);

                //Log.v(LOG_TAG, URLstring);

                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&APPID=4bea9adc6b526574133f4fa26a967c7b");
                URL url = new URL(URLstring);

                // Check permissions for internet.
                // checkSelfPermission();

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                //Log.v(LOG_TAG, forecastJsonStr);

                weatherStrings = getWeatherDataFromJson(forecastJsonStr, 7);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } catch (SecurityException e) {
                Log.e(LOG_TAG, "Error ", e);
                // Permissions were not set correctly.
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error: ", e);
                // Error in JSON parsing.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return weatherStrings;
        }

        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for (String str : result) {
                    mForecastAdapter.add(str);
                }
            }
        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         * * Includes converting from metric to imperial units if necessary.
         */
        private String formatHighLows(double high, double low) {
            // Assume metric input. Check settings and convert to imperial if necessary.
            // Get the preference object.
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            // Fetch the unit string.
            String preferredUnit = preferences.getString(
                    getString(R.string.pref_unit_key),
                    getString(R.string.pref_unit_default));

            if (preferredUnit.equals(getString(R.string.pref_units_imperial))) {
                // Convert if imperial.
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            }
            else if (!preferredUnit.equals(getString(R.string.pref_units_metric))) {
                Log.d(LOG_TAG, "Unit type not found: " + preferredUnit);
            }

            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                //Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;

        }
    }
}
