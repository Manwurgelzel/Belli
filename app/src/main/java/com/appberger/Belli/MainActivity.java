package com.appberger.Belli;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.appberger.mjpegviewer.MjpegView;

import java.io.IOException;
import java.util.ArrayList;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;


public class MainActivity extends AppCompatActivity {

    private MjpegView view1;
    public ParticleDevice mDevice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view1 = findViewById(R.id.mjpegview1);
        view1.setAdjustHeight(true);
        //view.setAdjustWidth(true);
        view1.setMode(MjpegView.MODE_FIT_WIDTH);
        view1.setUrl("http://192.168.1.60:8080/video");
        view1.setRecycleBitmap(true);
        createNotificationChannel();
        ParticleCloudSDK.init(this);

        //******
        //LOGIN
        //******
        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {


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
                Toaster.l(MainActivity.this, "Logged in");
                Toaster.l(MainActivity.this, mDevice.getID());
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException e) {
                //Toaster.l(MainActivity.this, e.getBestMessage());
                e.printStackTrace();
                Log.d("info", e.getBestMessage());
            }
        });
        // [START retrieve_current_token]
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        Log.d("FIRETOKEN",getString(R.string.msg_token_fmt,token));
                        // Log and toast
                                           }
                });
        // [END retrieve_current_token]

        FirebaseMessaging.getInstance().subscribeToTopic("particle");

    }
    @Override
    protected void onResume() {
        view1.startStream();
        super.onResume();
    }

    @Override
    protected void onPause() {
        view1.stopStream();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        view1.stopStream();
        super.onStop();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "doorbell";
            String description = "doorbell";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager!=null){
                notificationManager.createNotificationChannel(channel);}
        }
    }
    /** Called when the user touches the button */
    public void sendText(View view) throws ParticleCloudException {

        // "someDevice" is a ParticleDevice instance
        Async.executeAsync(mDevice, new Async.ApiWork<ParticleDevice, Integer>() {

            public Integer callApi(ParticleDevice particleDevice)
                    throws ParticleCloudException, IOException {
                ArrayList<String> args = new ArrayList<>();
                EditText text = (EditText)findViewById(R.id.displayText);
                args.add(text.getText().toString());
                try {
                    int resultCode = mDevice.callFunction("setText", args);
                    Toaster.l(MainActivity.this, "Result of calling sendText: " + resultCode);
                    return resultCode;
                } catch (ParticleDevice.FunctionDoesNotExistException e) {
                    e.printStackTrace();
                }
                return -1;
            }

            @Override
            public void onSuccess(Integer integer) {
                Toaster.l(MainActivity.this, "Success");
            }

            @Override
            public void onFailure(ParticleCloudException exception) {
                Toaster.l(MainActivity.this, exception.toString());
            }
        });
    }
}
