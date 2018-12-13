package uk.ac.bbk.wifiaplogger;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class GoogleApiLocationService extends Service {

    private static final String TAG = "GoogleApiLocService";
    private static final int LOCATION_REQUEST_INTERVAL = 5000;

    private Location mLocation;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(final LocationResult locationResult) {
            mLocation = locationResult.getLastLocation();
            Log.d(TAG, String.format("%-25s loc=%s", "onLocationResult()", mLocation));
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
        } else {
            mLocationRequest = new LocationRequest()
                    .setInterval(LOCATION_REQUEST_INTERVAL)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            if (hasFineOrCoarseLocationPermissions()) {
                getLastLocation();
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location permissions are not granted", Toast.LENGTH_SHORT).show();
            }
        }
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
                            mLocation = location;
                            Log.d(TAG, String.format("%-25s loc=%s", "getLastLocation()", mLocation));
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
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        Log.d(TAG, String.format("%-25s", "onDestroy()"));
    }

    public class GoogleApiLocationServiceBinder extends Binder {
        GoogleApiLocationService getGoogleApiLocationService() {
            return GoogleApiLocationService.this;
        }
    }

    /**
     * Returns location provided by Google Play services.
     *
     * @return current location
     */
    public Location getLocation() {
        return mLocation;
    }
}
