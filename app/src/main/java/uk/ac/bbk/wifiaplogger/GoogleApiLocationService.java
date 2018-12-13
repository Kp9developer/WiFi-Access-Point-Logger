package uk.ac.bbk.wifiaplogger;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

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
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = new LocationRequest()
                .setInterval(LOCATION_REQUEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        Log.d(TAG, "onCreate()");
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        mGoogleApiClient.connect();
        Log.d(TAG, "onStartCommand(Intent, int, int)");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
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
