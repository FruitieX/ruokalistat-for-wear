package org.fruitiex.ruokalistatforwear;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class UpdateService extends Service {
    Alarm alarm = new Alarm();
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        alarm.SetAlarm(UpdateService.this);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
