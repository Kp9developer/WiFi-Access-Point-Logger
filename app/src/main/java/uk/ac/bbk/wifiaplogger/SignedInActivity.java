package uk.ac.bbk.wifiaplogger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class SignedInActivity extends AppCompatActivity {

    private static final int NOTIFICATION_ID = 423;
    private static final int REQUEST_APP_PERMISSIONS = 0;
    private static final String TOAST_SIGN_OUT_FAILED = "Sign out failed!";
    private static final String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";
    private static final int DEFAULT_TEXTVIEW_UPDATE_FREQUENCY = 1;
    private static final int THOUSAND_MILLISECONDS = 1000;

    /* Tag for logging */
    private static final String TAG = "SignedInActivity";

    /* Permissions to request when activity starts */
    private static final String[] APP_REQUIRED_PERMISSIONS = new String[]{
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION,
            ACCESS_WIFI_STATE
    };

    /* Provides the primary API for managing all aspects of Wi-Fi connectivity */
    private WifiManager mWifiManager;

    /* Drop-down list to choose update frequency */
    private Spinner mSpinner;

    /* Reference to the location service */
    private GoogleApiLocationService mGoogleApiLocationService;

    /* Indicates whether or not the activity is bound to the service */
    private boolean mBound;

    /* Indicates whether user pressed start button */
    private boolean mIsStartButtonPressed;

    /* Interface that enables the activity to bind to a service */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            GoogleApiLocationService.GoogleApiLocationServiceBinder locationServiceBinder =
                    (GoogleApiLocationService.GoogleApiLocationServiceBinder) service;
            mGoogleApiLocationService = locationServiceBinder.getGoogleApiLocationService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBound = false;
        }
    };

    /* The entry point of the Firebase Authentication SDK */
    private final FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_in);

        /* Return to the MainActivity if user hasn't signed in */
        if (mFirebaseAuth.getCurrentUser() == null) {
            startActivity(MainActivity.createIntent(this));
            finish();
        }

        /* Initialize WifiManager */
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        /* Get a spinner view (i.e. drop-down list) and specify its default value */
        mSpinner = findViewById(R.id.logging_frequency);
        mSpinner.setSelection(DEFAULT_TEXTVIEW_UPDATE_FREQUENCY);

        /* Get signed in user email and user display name from extras */
        final String userEmailText = getIntent().getStringExtra(MainActivity.EXTRA_USER_EMAIL);
        final String displayNameText = getIntent().getStringExtra(MainActivity.EXTRA_DISPLAY_NAME);

        /* Display user email */
        final TextView userEmail = findViewById(R.id.user_email);
        userEmail.setText(userEmailText);

        /* Display user name */
        final TextView userName = findViewById(R.id.user_display_name);
        userName.setText(displayNameText);

        /* This button is responsible for signing user out */
        final Button mSignOutButton = findViewById(R.id.sign_out_button);
        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                signOut();
            }
        });

        final Button scanStartButton = findViewById(R.id.scan_start_button);
        scanStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mIsStartButtonPressed = true;
                updateScanResults();
            }
        });

        final Button scanStopButton = findViewById(R.id.scan_stop_button);
        scanStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mIsStartButtonPressed = false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, APP_REQUIRED_PERMISSIONS, REQUEST_APP_PERMISSIONS);
        } else {
            bindGoogleApiLocationService();
        }
        Log.d(TAG, String.format("%-25s mBound=%s mConnection=%s", "onStart()", mBound, mConnection));
    }

    /**
     * Helper method that handles {@code TextView} updates in separate thread.
     * The frequency of updates must be set using {@code mSpinner}
     */
    private void updateScanResults() {
        final TextView locationView = findViewById(R.id.location_coordinates_display);
        final TextView wifiNetworksNumberView = findViewById(R.id.wifi_networks_number_display);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                double longitude;
                double latitude;
                if (mIsStartButtonPressed && (mBound && mGoogleApiLocationService != null)) {
                    final List<ScanResult> scanResults = mWifiManager.getScanResults();
                    final String wifiNetworksNumber = "" + scanResults.size();
                    wifiNetworksNumberView.setText(wifiNetworksNumber);

                    Location location = mGoogleApiLocationService.getLocation();
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();

                    final String coordinates = String.format("long=%s lat=%s", longitude, latitude);
                    locationView.setText(coordinates);

                    final int updateFreqInSeconds = Integer.parseInt(mSpinner.getSelectedItem().toString());
                    final int updateFreqInMillis = THOUSAND_MILLISECONDS * updateFreqInSeconds;
                    handler.postDelayed(this, updateFreqInMillis);

                    Log.d(TAG, String.format("freq=%ds %s wifi=%s", updateFreqInSeconds, coordinates, wifiNetworksNumber));
                }
            }
        });
    }

    /**
     * Helper method that checks if the app has appropriate location permissions.
     *
     * @return true if location permissions have been granted
     */
    private boolean hasLocationPermission() {
        final int fine = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        final int coarse = ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION);
        final int wifi = ContextCompat.checkSelfPermission(this, ACCESS_WIFI_STATE);
        return fine == PERMISSION_GRANTED && coarse == PERMISSION_GRANTED && wifi == PERMISSION_GRANTED;
    }

    /**
     * Unbinds service from activity.
     */
    private void unbindGoogleApiLocationService() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindGoogleApiLocationService();
    }

    /**
     * Binds {@code SignInActivity} to the {@code GoogleApiLocationService}.
     */
    private void bindGoogleApiLocationService() {
        final Intent intent = new Intent(SignedInActivity.this, GoogleApiLocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        switch (requestCode) {
            case REQUEST_APP_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions have been granted!", Toast.LENGTH_SHORT).show();
                    bindGoogleApiLocationService();
                } else {
                    /* Will not show notification on Android API levels > 25 */
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_menu_compass)
                            .setContentTitle(getResources().getString(R.string.permission_denied))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVibrate(new long[]{1000, 1000})
                            .setAutoCancel(true);

                    Intent actionIntent = new Intent(this, MainActivity.class);
                    PendingIntent actionPendingIntent = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(actionPendingIntent);

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, builder.build());
                }
            }
        }
    }

    /**
     * This method signs users out of {@code SignedInActivity} and returns
     * them to the {@code MainActivity} if the task is successful.
     */
    public void signOut() {
        AuthUI.getInstance().signOut(SignedInActivity.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull final Task<Void> task) {
                        if (task.isSuccessful()) {
                            startActivity(MainActivity.createIntent(SignedInActivity.this));
                            finish();
                        } else {
                            Toast.makeText(SignedInActivity.this, TOAST_SIGN_OUT_FAILED, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Creates an intent to be used by the {@code MainActivity} class
     * to launch {@code SignedInActivity}
     *
     * @param context     the launching activity
     * @param idpResponse a container that encapsulates the result of user authentication
     * @return returns an intent to create {@code SignedInActivity}
     */
    public static Intent createIntent(final Context context, final IdpResponse idpResponse) {
        Intent intent = new Intent();
        intent.putExtra(ExtraConstants.IDP_RESPONSE, idpResponse);
        intent.setClass(context, SignedInActivity.class);
        return intent;
    }
}
