package org.fruitiex.ruokalistatforwear;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

public class Alarm extends BroadcastReceiver implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient googleClient;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        // Put here YOUR code.

        // Build a new GoogleApiClient that includes the Wearable API
        googleClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();

        Toast.makeText(context, "Alarm triggered.", Toast.LENGTH_SHORT).show(); // For example

        wl.release();
    }

    public void SetAlarm(Context context)
    {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setInexactRepeating(AlarmManager.RTC, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, pi); // Millisec * Second * Minute
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
    public void updateLists() {
        new HttpAsyncTask().execute("http://lounasaika.net/api/v1/menus.json");
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    public static String GET(String url){
        InputStream inputStream;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String reqResult) {
            // hardcoded for now
            String userRestaurants[] = {"Täffä", "Teekkariravintolat", "Alvari", "TUAS-talo", "Kvarkki", "Cantina"};
            String userLang = "fi";

            try {
                // only this array is sent to the watch
                final JSONArray results = new JSONArray();

                JSONArray json = new JSONArray(reqResult);
                boolean mealFound = false;

                // traverse through all restaurants user is interested in, in order
                for (String userRestaurant : userRestaurants) {
                    // find corresponding restaurant in json
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject restaurant = json.getJSONObject(i);

                        // was this the correct restaurant?
                        if (!userRestaurant.equals(restaurant.getString("name")))
                            continue;

                        // only this object will be added to results
                        JSONObject restaurantResults = new JSONObject();
                        restaurantResults.put("name", restaurant.getString("name"));

                        // sanity check
                        if (restaurant.has("meals")) {
                            // get today's meal
                            JSONObject weekMealLangs = restaurant.getJSONObject("meals");

                            // sanity check
                            if (weekMealLangs.has(userLang)) {

                                JSONArray weekMeals = weekMealLangs.getJSONArray(userLang);

                                // sunday is day 1, monday day 2 etc...
                                Calendar calendar = Calendar.getInstance();
                                int day = calendar.get(Calendar.DAY_OF_WEEK);
                                day -= 2; // we want monday to be 0
                                if (day == -1)
                                    day = 6; // wrap around sunday to 6

                                day = 2; // DEBUG purposes only
                                Log.i("myTag", "getting meals for day " + day);

                                JSONArray mealResults = new JSONArray();
                                // does restaurant have any meals today?
                                if (weekMeals.length() > day) {
                                    // today's meals
                                    JSONArray meals = weekMeals.getJSONArray(day);
                                    for (int j = 0; j < meals.length(); j++) {
                                        mealResults.put(meals.getString(j));
                                    }
                                    mealFound = true;
                                } else
                                    mealResults.put("No meals today.");

                                restaurantResults.put("meals", mealResults);
                            }
                        }

                        results.put(restaurantResults);
                    }
                }

                if(mealFound)
                    new SendToDataLayerThread("/ruokalistat", results.toString()).start();

            } catch (JSONException e) {
                Log.e("myTag", e.getLocalizedMessage());
            }
        }
    }

    // Android Wear message API
    @Override
    public void onConnected(Bundle connectionHint) {
        //String message = "Hello wearable\n Via the data layer";
        //Requires a new thread to avoid blocking the UI
        //new SendToDataLayerThread("/message_path", message).start();
        updateLists();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
                googleClient.disconnect();
            }
        }
    }
}