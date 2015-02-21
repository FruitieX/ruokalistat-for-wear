package org.fruitiex.ruokalistatforwear;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * Wearable listener service for data layer messages
 */
public class ListenerService extends WearableListenerService{

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals("/ruokalistat")) {
            final String message = new String(messageEvent.getData());
            Log.v("myTag", "Message path received on watch is: " + messageEvent.getPath());
            Log.v("myTag", "Message received on watch is: " + message);

            JSONArray json = null;
            try {
                json = new JSONArray(message);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext());

                mBuilder.setSmallIcon(R.drawable.ic_launcher);

                boolean firstPage = true;

                for (int i = 0; i < json.length(); i++) {
                    JSONObject restaurant = json.getJSONObject(i);

                    if (restaurant.has("meals")) {
                        // get today's meal
                        JSONObject weekMeals = restaurant.getJSONObject("meals");
                        if (weekMeals.has("fi")) {

                            JSONArray weekMealsFi = weekMeals.getJSONArray("fi");

                            // any locale that starts week with monday
                            Calendar calendar = Calendar.getInstance(Locale.GERMANY);
                            int day = calendar.get(Calendar.DAY_OF_WEEK);
                            day = 3;

                            String s = "";
                            // restaurant name

                            // does restaurant have any meals today?
                            if (weekMealsFi.length() >= day) {
                                // today's meals
                                JSONArray meals = weekMealsFi.getJSONArray(day - 1);
                                for (int j = 0; j < meals.length(); j++) {
                                    s += meals.getString(j) + "\n";
                                }
                            } else
                                s += "No meals today.";

                            if (firstPage) {
                                mBuilder.setContentTitle(restaurant.getString("name"));
                                mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText(s));
                            } else {
                                // Create a big text style for the this page
                                NotificationCompat.BigTextStyle pageStyle = new NotificationCompat.BigTextStyle();
                                pageStyle.setBigContentTitle(restaurant.getString("name"))
                                        .bigText(s);

                                Notification page =
                                        new NotificationCompat.Builder(getBaseContext())
                                                .setStyle(pageStyle)
                                                .build();

                                mBuilder.extend(new NotificationCompat.WearableExtender()
                                        .addPage(page));
                            }
                        }
                    }
                }


                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                // mId allows you to update the notification later on.
                int mId = 1;
                mNotificationManager.notify(mId, mBuilder.build());
            } catch (JSONException e) {
                System.out.println(e);
            }
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }

}