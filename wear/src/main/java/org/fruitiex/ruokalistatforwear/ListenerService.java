package org.fruitiex.ruokalistatforwear;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

            try {
                JSONArray json = new JSONArray(message);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext());
                NotificationCompat.WearableExtender wExtender = new NotificationCompat.WearableExtender();

                mBuilder.setSmallIcon(R.drawable.ic_launcher);

                boolean firstPage = true;

                for (int i = 0; i < json.length(); i++) {
                    JSONObject restaurant = json.getJSONObject(i);

                    String s = "";

                    // today's meals
                    JSONArray meals = restaurant.getJSONArray("meals");
                    for (int j = 0; j < meals.length(); j++) {
                        s += meals.getString(j) + "\n\n";
                    }

                    Log.v("myTag", "Adding notification: " + s);
                    if (firstPage) {
                        // first page has to be handled separately
                        mBuilder.setContentTitle(restaurant.getString("name"));
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(s));
                        firstPage = false;
                    } else {
                        Notification page =
                            new NotificationCompat.Builder(getBaseContext())
                                    .setContentTitle(restaurant.getString("name"))
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText(s))
                                    .build();

                        wExtender.addPage(page);
                    }
                }

                mBuilder.extend(wExtender);
                // send notification
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                // hardcoded id: will always replace old notification with new one
                int mId = 1;
                mNotificationManager.notify(mId, mBuilder.build());
            } catch (JSONException e) {
                Log.e("myTag", e.getLocalizedMessage());
            }
        } else if (messageEvent.getPath().equals("/message_path")) {
            Log.v("myTag", "got msg " + new String(messageEvent.getData()));
        } else {
            super.onMessageReceived(messageEvent);
        }
    }
}