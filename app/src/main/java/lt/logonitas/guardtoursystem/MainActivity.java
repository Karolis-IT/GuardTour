package lt.logonitas.guardtoursystem;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TextView textViewInfo;

    private String KEY_CARD_ID      = "post_card_id";
    private String KEY_ANDROID_ID   = "post_android_id";
    private String KEY_ANDROID_NAME = "post_android_name";
    private String KEY_LOCATION     = "post_location";

    private boolean flashLightStatus = false;

    private Location location;

    // Permissions
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewInfo = findViewById(R.id.info);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null){
            Toast.makeText(this,
                    "NFC NOT supported on this devices!",
                    Toast.LENGTH_LONG).show();
            finish();
        }else if(!nfcAdapter.isEnabled()){
            Toast.makeText(this,
                    "NFC NOT Enabled!",
                    Toast.LENGTH_LONG).show();
            finish();
        }


        /*
        * Flashlight button
        * Let's enable flashlight for our security guard
         */
        final Button flashLightButton = findViewById(R.id.flashlight);
        flashLightButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(!flashLightStatus){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        String cameraId = null;
                        try {
                            cameraId = camManager.getCameraIdList()[0];
                            camManager.setTorchMode(cameraId, true);   //Turn ON
                            flashLightButton.setText(R.string.turn_off_flashlight);
                            flashLightStatus = true;
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                    }
                }else{

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        String cameraId = null;
                        try {
                            cameraId = camManager.getCameraIdList()[0];
                            camManager.setTorchMode(cameraId, false);   //Turn OFF
                            flashLightButton.setText(R.string.turn_on_flashlight);

                            flashLightStatus = false;
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                    }

                }
            }
        });

        /*
        * Incident report button
         */
        final Button incidentReportButton = findViewById(R.id.incident_report);
        incidentReportButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, IncidentReport.class);
                MainActivity.this.startActivity(intent);
                overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);

            }
        });

        /*
        * Panic button to make a call
         */
        final Button PanicButtonCall = findViewById(R.id.panic_button_call);
        PanicButtonCall.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Get phone number to call
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final String panicPhoneNumber = prefs.getString("call_number", "");

                if(!panicPhoneNumber.equals("")){
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + panicPhoneNumber));
                    startActivity(intent);
                }else{
                    Toast.makeText(MainActivity.this, R.string.set_phone_number , Toast.LENGTH_LONG).show();
                }


            }
        });

        /*
        * Panic button to send SMS
         */
        final Button PanicButtonSendSMS = findViewById(R.id.panic_button_send_sms);
        PanicButtonSendSMS.setOnClickListener( new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                try {

                    // Get phone number and message to send panic message
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    final String panicMessageNumber = prefs.getString("message_number", "");
                    final String panicMessage = prefs.getString("message_text", "");

                    if(!panicMessageNumber.equals("")){
                        SmsManager sms = SmsManager.getDefault();
                        ArrayList<String> mSMSMessage = sms.divideMessage(panicMessage);

                        sms.sendMultipartTextMessage(panicMessageNumber, null, mSMSMessage,
                                null, null);

                        Toast.makeText(MainActivity.this, R.string.message_sent , Toast.LENGTH_LONG).show();

                    }else{
                        Toast.makeText(MainActivity.this, R.string.set_phone_number , Toast.LENGTH_LONG).show();
                    }



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @SuppressLint("HardwareIds")
    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getAction();

        // Get android id
        final String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        // Get phone name from settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String android_name = prefs.getString("settings_phone_name", "");


        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if(tag != null){
                StringBuilder tagInfo = new StringBuilder("\n");

                byte[] tagId = tag.getId();
                for (byte b : tagId) {
                    tagInfo.append(Integer.toHexString(b & 0xFF));
                }

                // Play sound of success
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);

                // Insert data to MySQL
                insertData(tagInfo.toString(), android_id, android_name);
            }
        }
    }

    /*
    *   Insert data into MySQL database
    */
    private void insertData(final String set_card_id, final String set_android_id, final String set_android_name){

        // Get URL from settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String settings_url = prefs.getString("settings_url", "");


        StringRequest stringRequest = new StringRequest(Request.Method.POST, settings_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Error response", response);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error response", String.valueOf(error));
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Creating parameters
                Map<String, String> params = new Hashtable<>();


                //Adding parameters
                params.put(KEY_CARD_ID, set_card_id);
                params.put(KEY_ANDROID_ID, set_android_id);
                params.put(KEY_ANDROID_NAME, set_android_name);
                params.put(KEY_LOCATION, String.valueOf(getLocationGPS()));

                // Returning parameters
                return params;
            }
        };

        // Creating a Request Queue
        Context intent = getBaseContext();
        RequestQueue requestQueue = Volley.newRequestQueue(intent);

        // Adding request to the queue
        requestQueue.add(stringRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            MainActivity.this.startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Request for permissions
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * Lets get GPS location. Also check for provider and fallback locations
     */
    public Location getLocationGPS(){

        FallbackLocationTracker GPSFallback = new FallbackLocationTracker(MainActivity.this, ProviderLocationTracker.ProviderType.GPS);
        ProviderLocationTracker GPSProvider = new ProviderLocationTracker(MainActivity.this, ProviderLocationTracker.ProviderType.GPS);

        if(GPSProvider.getLocation() != null) {
            location = GPSProvider.getPossiblyStaleLocation();
        }else if(GPSFallback.getLocation() != null){
            location = GPSProvider.getPossiblyStaleLocation();
        }else if(GPSProvider.getPossiblyStaleLocation() != null){
            location = GPSProvider.getPossiblyStaleLocation();
        }else if(GPSFallback.getPossiblyStaleLocation() != null){
            location = GPSFallback.getPossiblyStaleLocation();
        }

        return location;
    }

}
