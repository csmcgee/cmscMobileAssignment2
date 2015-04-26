package cmsc491.placepush.geofence;

import android.app.PendingIntent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.util.ArrayList;

import cmsc491.placepush.GoogleAPI.GooglePlaceResponse;
import cmsc491.placepush.domain.PPConsts;

public class GeofenceControlPanel implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    protected GoogleApiClient mGoogleApiClient;
    protected static final String TAG = "cm-geofences";
    private final int MILLISECOND_MULTIPLIER = 1000;
    private final int LOITERING_DELAY_SECS = 5;


    public GeofenceControlPanel(Context context){
        mGeofenceList = new ArrayList<Geofence>();
        mGeofencePendingIntent = null;
        buildGoogleApiClient(context);
    }

    protected synchronized void buildGoogleApiClient(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void connectApiClient(){
        mGoogleApiClient.connect();
    }

    public void disconnectApiClient(){
        mGoogleApiClient.disconnect();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(Context context, GooglePlaceResponse.Place place) {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
        intent.putExtra(PPConsts.PLACE_NAME, place.name);
        intent.putExtra(PPConsts.PLACE_ADDR, place.formatted_address);
        intent.putExtra(PPConsts.PLACE_LAT, place.geometry.location.lat);
        intent.putExtra(PPConsts.PLACE_LNG, place.geometry.location.lng);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(context, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    public void addGeofencesMarkerChosen(Context context, GooglePlaceResponse.Place place, float distance) {
        if (!mGoogleApiClient.isConnected()) {
            Log.i("MarkerChosen", "mGoogleApiClient not connected");
            return;
        }

        try {
            // empty list
            mGeofenceList = new ArrayList<Geofence>();
            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(place.name)
                    .setCircularRegion(place.geometry.location.lat, place.geometry.location.lng, distance)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setLoiteringDelay(LOITERING_DELAY_SECS * MILLISECOND_MULTIPLIER)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_DWELL |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());

            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent(context, place)
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.i("MarkerChosen", "mGoogleApiClient SECURITY exception");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onResult(Status status) {
        Log.i("OnResult", "Called");
    }
}
