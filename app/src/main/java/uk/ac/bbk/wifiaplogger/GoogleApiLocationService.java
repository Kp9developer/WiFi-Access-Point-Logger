package uk.ac.bbk.wifiaplogger;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class GoogleApiLocationService
        extends Service
        implements GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleApiLocService";
    private static final int LOCATION_REQUEST_INTERVAL = 5000;

    private double mLongitude = 0;
    private double mLatitude = 0;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(final LocationResult locationResult) {
            Location location = locationResult.getLastLocation();
            mLongitude = location.getLongitude();
            mLatitude = location.getLatitude();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return new GoogleApiLocationServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        final int result = availability.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (!availability.isUserResolvableError(result)) {
                Toast.makeText(this, "Google Play services are unavailable.", Toast.LENGTH_SHORT).show();
            }
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = new LocationRequest()
                .setInterval(LOCATION_REQUEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        Log.d(TAG, "onCreate()");
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        mGoogleApiClient.connect();
        Log.d(TAG, "onStartCommand(Intent, int, int)");
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (hasFineOrCoarseLocationPermissions()) {
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /* Looper */);
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (hasFineOrCoarseLocationPermissions()) {
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(final Location location) {
                            if (location != null) {
                                mLongitude = location.getLongitude();
                                mLatitude = location.getLatitude();
                                Log.d(TAG, String.format("-25%s long=%f lat=%f","getLastLocation()", mLongitude, mLatitude));
                            }
                        }
                    });
        }
    }

    /**
     * Checks if app has fine or coarse location permissions.
     *
     * @return true if fine or coarse location permissions are granted
     */
    private boolean hasFineOrCoarseLocationPermissions() {
        final int fineLocation = ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        final int coarseLocation = ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION);
        return fineLocation == PERMISSION_GRANTED || coarseLocation == PERMISSION_GRANTED;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        Log.d(TAG, "onConnected(Bundle)");
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Log.d(TAG, "onConnectionSuspended(int)");
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(ConnectionResult)");
    }

    public class GoogleApiLocationServiceBinder extends Binder {
        GoogleApiLocationService getGoogleApiLocationService() {
            return GoogleApiLocationService.this;
        }
    }
}
