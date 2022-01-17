package lt.logonitas.guardtoursystem;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;


public class IncidentReport extends AppCompatActivity implements View.OnClickListener {

    private Button buttonChoose;
    private Button buttonUpload;
    private Button buttonCamera;

    private ImageView imageView;

    private EditText editTextName;

    private Bitmap bitmap;

    private int PICK_IMAGE_REQUEST = 1;
    private int CAMERA_REQUEST = 2;

    private String KEY_IMAGE    = "post_image";
    private String KEY_REPORT     = "post_report";
    private String KEY_ANDROID_ID   = "post_android_id";
    private String KEY_ANDROID_NAME = "post_android_name";
    private String KEY_LOCATION     = "post_location";


    ContentValues cv;
    Uri imageUri;

    private Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_report);

        buttonChoose = findViewById(R.id.buttonChoose);
        buttonUpload = findViewById(R.id.buttonUpload);
        buttonCamera = findViewById(R.id.buttonCamera);

        editTextName = findViewById(R.id.editText);

        imageView  = findViewById(R.id.imageView);

        buttonChoose.setOnClickListener(this);
        buttonUpload.setOnClickListener(this);
        buttonCamera.setOnClickListener(this);

    }

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    private void uploadImage(final String set_android_id, final String set_android_name){

        // Get URL from settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String settings_url = prefs.getString("settings_url", "");


        //Showing the progress dialog
        final ProgressDialog loading = ProgressDialog.show(this,
                "Uploading...",
                "Please wait...",
                false,
                false);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, settings_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        //Disimissing the progress dialog
                        loading.dismiss();
                        //Showing toast message of the response
                        Toast.makeText(IncidentReport.this, s , Toast.LENGTH_LONG).show();

                        // Return to Camera Activity after upload
                        finish();
                        overridePendingTransition(0, 0);
                        startActivity(getIntent());
                        overridePendingTransition(0, 0);


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        //Dismissing the progress dialog
                        loading.dismiss();

                        //Showing toast
                        Toast.makeText(IncidentReport.this, volleyError.getMessage().toString(), Toast.LENGTH_LONG).show();
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                //Converting Bitmap to String
                String image = getStringImage(bitmap);

                //Getting Image Name
                String report = editTextName.getText().toString().trim();

                //Creating parameters
                Map<String,String> params = new Hashtable<String, String>();


                //Adding parameters
                params.put(KEY_IMAGE, image);
                params.put(KEY_REPORT, report);
                params.put(KEY_ANDROID_ID, set_android_id);
                params.put(KEY_ANDROID_NAME, set_android_name);
                params.put(KEY_LOCATION, String.valueOf(getLocationGPS()));

                //returning parameters
                return params;
            }
        };

        //Creating a Request Queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        //Adding request to the queue
        requestQueue.add(stringRequest);
    }

    // Get files from Gallery
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Open Camera
    // Also take high quality photo, not only thumbnail
    private void openCamera() {
        cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "My Picture");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            try {
                // Getting the Bitmap from Gallery
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                // Setting the Bitmap to ImageView
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {

            try {
                // Getting the Bitmap from Camera
                bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                // Setting the Bitmap to ImageView
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {

        if(v == buttonChoose){
            showFileChooser();
        }

        if(v == buttonUpload){
            // Get android id
            final String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

            // Get phone name from settings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            final String android_name = prefs.getString("settings_phone_name", "");

            uploadImage(android_id, android_name);
        }

        if(v == buttonCamera){
            openCamera();
        }
    }

    // Hide Keyboard if user press somewhere in the background
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();

        if (v != null &&
                (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) &&
                v instanceof EditText &&
                !v.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            v.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + v.getLeft() - scrcoords[0];
            float y = ev.getRawY() + v.getTop() - scrcoords[1];

            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom())
                hideKeyboard(this);
        }
        return super.dispatchTouchEvent(ev);
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    /*
     * Lets get GPS location. Also check for provider and fallback locations
     */
    public Location getLocationGPS(){

        FallbackLocationTracker GPSFallback = new FallbackLocationTracker(IncidentReport.this, ProviderLocationTracker.ProviderType.GPS);
        ProviderLocationTracker GPSProvider = new ProviderLocationTracker(IncidentReport.this, ProviderLocationTracker.ProviderType.GPS);

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