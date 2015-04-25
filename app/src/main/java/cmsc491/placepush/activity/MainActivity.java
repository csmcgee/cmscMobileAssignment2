package cmsc491.placepush.activity;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.app.DialogFragment;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.PendingIntent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.Geofence;

import com.google.gson.Gson;

import cmsc491.placepush.GoogleAPI.GooglePlaceResponse;
import cmsc491.placepush.GoogleAPI.PPGooglePlaceSearch;
import cmsc491.placepush.R;

import com.google.android.gms.common.api.GoogleApiClient;


import java.util.ArrayList;



public class MainActivity extends ActionBarActivity
        implements OnMapReadyCallback, MarkerConfirmation.NoticeDialogListener{

    public LocationManager locationManager;
    public MapLocationListener mapLocationListener = new MapLocationListener();



    private GoogleMap gMap;
    private Marker posMarker;
    private LatLng currentPosition;
    private ActionBar mActionBar;
    private ProgressBar progress;
    private float distanceFromMe;
    private float benchmarkDistance;
    private Geofence POI;
    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    protected GoogleApiClient mGoogleApiClient;
    private GeofenceControlPanel geoPanel;
    private Context currentContext;
    private String queriedLocation;
    private double newGeoLat, newGeoLon;
    private String newGeoTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        benchmarkDistance = 200f;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress = (ProgressBar) findViewById(R.id.progressBar);

        intializeMap();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // minimum 25 meters, every 5 seconds
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 25, mapLocationListener);

        // Action bar configurations
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setLogo(R.mipmap.ic_launcher);
        mActionBar.setDisplayUseLogoEnabled(true);

        // intent handling
        handleIntent(getIntent());

        mGeofencePendingIntent = null;

        //initialize geofence list
        mGeofenceList = new ArrayList<Geofence>();

        // Create new Geofences Control Panel
        geoPanel = new GeofenceControlPanel(this);






    }



    private void handleIntent(Intent intent){
        // Handle Search Intent
        if(intent.ACTION_SEARCH.equals(intent.getAction())){
            String query = intent.getStringExtra(SearchManager.QUERY);
            progress.setVisibility(View.VISIBLE);
            new WebRequestTask().execute(query);

        }
    }



    private class WebRequestTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... queries) {
            PPGooglePlaceSearch search = new PPGooglePlaceSearch();
            String response = search.searchPlaces(queries[0], currentPosition.latitude, currentPosition.longitude);
            return response;
        }

        protected void onPostExecute(String result){
            progress.setVisibility(View.INVISIBLE);
            GooglePlaceResponse response = new Gson().fromJson(result, GooglePlaceResponse.class);
            gMap.clear();
            posMarker = gMap.addMarker(new MarkerOptions()
                    .position(currentPosition));
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(posMarker.getPosition());

            for(int i = 0; i < response.results.size(); i++){
                GooglePlaceResponse.Place place = response.results.get(i);
                LatLng coordinates = new LatLng(place.geometry.location.lat, place.geometry.location.lng);
                builder.include(coordinates);
                MarkerOptions marker = new MarkerOptions().position(coordinates);
                marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                marker.title(place.name);
                marker.snippet(place.formatted_address);
                gMap.addMarker(marker);
            }

            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 100);

            gMap.animateCamera(cu);
        }
    }



    @Override
    protected void onNewIntent(Intent intent){
        setIntent(intent);
        handleIntent(intent);
    }

    private void intializeMap(){
        if(gMap == null){
            MapFragment mapFrag = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
            mapFrag.getMapAsync(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true); // Do not iconify the widget; expand it by default
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        currentContext = this;
        gMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Special case (current location point)
                if (marker.getId().equals(posMarker.getId()))
                    return null;

                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);
                TextView title = (TextView) v.findViewById(R.id.infoWindowTitle);
                TextView lineOne = (TextView) v.findViewById(R.id.infoLine1);
                title.setText(marker.getTitle());
                lineOne.setText(marker.getSnippet());

                return v;
            }
        });


        // Place current position marker on the map if Location services are enabled
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null) {
            final LatLng myCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
            posMarker = gMap.addMarker(new MarkerOptions()
                    .position(myCoordinates));
            currentPosition = myCoordinates;
            gMap.moveCamera(CameraUpdateFactory.newLatLng(myCoordinates));
            gMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        }

        // Set the onClickListener for the markers on the map
        gMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                Log.i("MARKER", "CLICKED");
                LatLng coords = marker.getPosition();
                newGeoLon = coords.longitude;
                newGeoLat = coords.latitude;
                newGeoTitle = marker.getTitle();
                float[] results = new float[3];
                Location.distanceBetween(currentPosition.latitude, currentPosition.longitude, coords.latitude, coords.longitude, results);
                distanceFromMe = results[0];
                Log.i("DISTANCE", String.valueOf(results[0]));

                if(distanceFromMe <= benchmarkDistance) {
                    Context context = getApplicationContext();
                    CharSequence text = "Already within " + String.valueOf(benchmarkDistance) + " meters.";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();

                }
                MarkerConfirmation conf = new MarkerConfirmation();
                conf.show(getFragmentManager(), "MarkerConfirmation");
                return false;
            }
        });
    }

    public void onDialogYes(){
        geoPanel.addNewGeofence(newGeoLat, newGeoLon, benchmarkDistance, newGeoTitle);
        Log.i("LOCATION", newGeoTitle);
        geoPanel.addGeofencesMarkerChosen(currentContext);
    }

    @Override
    protected void onStart(){
        super.onStart();
        geoPanel.connectApiClient();
    }

    @Override
    protected void onStop(){
        super.onStop();
        geoPanel.disconnectApiClient();
    }

    private class MapLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if(location != null) {
                LatLng coordinates = new LatLng(location.getLatitude(), location.getLongitude());
                if(posMarker != null)
                    posMarker.setPosition(coordinates);
                else{
                    posMarker = gMap.addMarker(new MarkerOptions().position(coordinates));
                    gMap.moveCamera(CameraUpdateFactory.newLatLng(coordinates));
                    gMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                }
                currentPosition = coordinates;
            }
            else Log.i("LOCATION", "NULL");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        geoPanel.addNewGeofence(newGeoLat, newGeoLon, benchmarkDistance, newGeoTitle);
        Log.i("LOCATION", newGeoTitle);
        geoPanel.addGeofencesMarkerChosen(currentContext);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }
}
