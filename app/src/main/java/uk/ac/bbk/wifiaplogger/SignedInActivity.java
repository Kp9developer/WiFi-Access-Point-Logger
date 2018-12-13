package uk.ac.bbk.wifiaplogger;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class SignedInActivity extends AppCompatActivity {

    private static final int NOTIFICATION_ID = 423;
    private static final int REQUEST_LOCATION_PERMISSIONS = 0;
    private static final String TOAST_SIGN_OUT_FAILED = "Sign out failed!";
    private static final String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

    /* Tag for logging */
    private static final String TAG = "SignedInActivity";

    /* Location permission to request when activity starts */
    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION
    };

    private Spinner mSpinner;

    /* Reference to the location service */
    private GoogleApiLocationService mGoogleApiLocationService;

    /* Indicates whether or not the activity is bound to the service */
    private boolean mBound;

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

        /* Get a spinner view (i.e. drop-down list) and specify its default value */
        mSpinner = findViewById(R.id.logging_frequency);
        mSpinner.setSelection(0);

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
                bindGoogleApiLocationService();
            }
        });

        final Button scanStopButton = findViewById(R.id.scan_stop_button);
        scanStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                unbindGoogleApiLocationService();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
        } else {
            bindGoogleApiLocationService();
        }
    }

    /**
     * Helper method that checks if the app has appropriate location permissions.
     *
     * @return true if location permissions have been granted
     */
    private boolean hasLocationPermission() {
        final int result = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
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
        mBound = true;
        final Intent intent = new Intent(SignedInActivity.this, GoogleApiLocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this, GoogleApiLocationService.class);
                    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                } else {
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
