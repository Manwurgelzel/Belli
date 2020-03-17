package com.appberger.Belli;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.ParticleEvent;
import io.particle.android.sdk.cloud.ParticleEventHandler;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.Async;

public class NotificationService extends Service {
    private static final int SILENT_NOTIF_ID = 1;
    private static final int DOORBELL_NOTIF_ID = 2;

    private static final String SILENT_CHANNEL_ID = "Silent";
    private static final String DOORBELL_CHANNEL_ID = "Doorbell";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        ParticleCloudSDK.init(this);
        createNotificationChannel1();
        createNotificationChannel2();
        //******
        //LOGIN
        //******
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {

            private ParticleDevice mDevice;

            @Override
            public Object callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException, IOException {
                sparkCloud.logIn("beauwberger@gmail.com", "Widji123ji");
                sparkCloud.getDevices();
                try {
                    mDevice = sparkCloud.getDevices().get(0);
                } catch (IndexOutOfBoundsException iobEx) {
                    throw new RuntimeException("Your account must have at least one device for this example app to work");
                }
                return -1;
            }
                @Override
                public void onSuccess(@NonNull Object value) {
                   // Toaster.l(MainActivity.this, "Logged in");
                    //Intent intent = ValueActivity.buildIntent(LoginActivity.this, 123, mDevice.getID());
                    //startActivity(intent);
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException e) {
                    //Toaster.l(MainActivity.this, e.getBestMessage());
                    e.printStackTrace();
                    Log.d("info", e.getBestMessage());
                }
            });
        //******
        //SUBSCRIBE
        //******
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {

            @Override
            public Object callApi(@NonNull ParticleCloud sparkCloud) throws IOException {

                ParticleCloudSDK.getCloud().subscribeToMyDevicesEvents(
                        null,  // the first argument, "eventNamePrefix", is optional
                        new ParticleEventHandler() {
                            public void onEvent(String eventName, ParticleEvent event) {
                               Log.println(Log.INFO,"PARTICLEEVENT", "Received event with payload: " + eventName );
                                // Create an explicit intent for an Activity in your app
                                Intent intent = new Intent(NotificationService.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                PendingIntent pendingIntent = PendingIntent.getActivity(NotificationService.this, 0, intent, 0);

                                NotificationCompat.Builder builder = new NotificationCompat.Builder(NotificationService.this, DOORBELL_CHANNEL_ID)
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setContentTitle("My notification")
                                        .setContentText("Hello World!")
                                        .setPriority(NotificationManager.IMPORTANCE_HIGH)
                                        // Set the intent that will fire when the user taps the notification
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true);

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(NotificationService.this);

                                // notificationId is a unique int for each notification that you must define
                                notificationManager.notify(DOORBELL_NOTIF_ID, builder.build());

                            }

                            public void onEventError(Exception e) {
                                //Toaster.s(MainActivity.this, "Failed!");
                            }
                        });
            return -1;}
            @Override
            public void onSuccess(@NonNull Object value) {
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException e) {
            }
        });

        startForeground();

        return START_STICKY;
    }
    private void createNotificationChannel1() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "doorbell";
            String description = "doorbell";
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel(SILENT_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager!=null){
                notificationManager.createNotificationChannel(channel);}
        }
    }
    private void createNotificationChannel2() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "doorbell";
            String description = "doorbell";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(DOORBELL_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager!=null){
                notificationManager.createNotificationChannel(channel);}
        }
    }
    private void startForeground() {


        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        startForeground(SILENT_NOTIF_ID, new NotificationCompat.Builder(this,
                SILENT_CHANNEL_ID) // don't forget create a notification channel first
                //.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .setSound(null)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .build());
    }
}

