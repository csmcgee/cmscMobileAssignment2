package cmsc491.placepush.activity;

import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.Geofence;

import com.google.gson.Gson;

import cmsc491.placepush.GoogleAPI.GooglePlaceResponse;
import cmsc491.placepush.GoogleAPI.PPGooglePlaceSearch;
import cmsc491.placepush.R;
import cmsc491.placepush.domain.PPConsts;
import cmsc491.placepush.geofence.GeofenceControlPanel;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity
        implements OnMapReadyCallback, MarkerConfirmation.NoticeDialogListener{

    public LocationManager locationManager;
    public MapLocationListener mapLocationListener = new MapLocationListener();
    private GoogleMap gMap;
    private GooglePlaceResponse.Place savedPlace;
    private Marker savedMarker;
    private Marker posMarker;
    private LatLng currentPosition;
    private ActionBar mActionBar;
    private ProgressBar progress;
    private float distanceFromMe;
    private ArrayList<Geofence> mGeofenceList;
    private GeofenceControlPanel geoPanel;
    private Context currentContext;
    private double newGeoLat, newGeoLon;
    private String newGeoTitle, newGeoSnippet;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress = (ProgressBar) findViewById(R.id.progressBar);

        intializeMap();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // minimum 10 meters, every 5 seconds
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, mapLocationListener);

        // Action bar configurations
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setLogo(R.mipmap.ic_launcher);
        mActionBar.setDisplayUseLogoEnabled(true);

        // intent handling
        handleIntent(getIntent());

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
            searchView.clearFocus();
        }
        // Handle Pending Notification Intent
        else if(intent.getAction().equals(PPConsts.GF_ACTION)){
            String name = intent.getStringExtra(PPConsts.PLACE_NAME);
            String address = intent.getStringExtra(PPConsts.PLACE_ADDR);
            double lat = intent.getDoubleExtra(PPConsts.PLACE_LAT, 0);
            double lng = intent.getDoubleExtra(PPConsts.PLACE_LNG, 0);
            GooglePlaceResponse.Place place = new GooglePlaceResponse.Place(name, address, lat, lng);
            // set saved place for use when map is ready
            savedPlace = place;
        }
    }

    // Convenience method to help build google place marker
    private MarkerOptions buildGooglePlaceMarker(GooglePlaceResponse.Place place, LatLng coordinates){
        MarkerOptions marker = new MarkerOptions().position(coordinates);
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        marker.title(place.name);
        marker.snippet(place.formatted_address);
        return marker;
    }

    // Convenience method to help build circle option
    private CircleOptions buildPlaceRadius(LatLng placeCoordinates){
        CircleOptions circleOptions = new CircleOptions()
                .center(placeCoordinates)
                .radius(PPConsts.BENCHMARK_DISTANCE)
                .fillColor(PPConsts.CIRCLE_FILL_COLOR)
                .strokeColor(PPConsts.CIRCLE_STROKE_COLOR);
        return circleOptions;
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
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
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

        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        currentContext = this;

        // Set custom info-window and click listener
        gMap.setInfoWindowAdapter(new PPGoogleInfoAdapter());
        gMap.setOnMarkerClickListener(new PPGoogleMarkerClickListener());
        gMap.setOnInfoWindowClickListener(new PPGoogleInfoClickListener());

        // Get last known location from GPS, if null then network provider
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location == null)
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if(location != null) {
            LatLng myCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
            posMarker = gMap.addMarker(new MarkerOptions()
                            .position(myCoordinates));
            currentPosition = myCoordinates;
            gMap.moveCamera(CameraUpdateFactory.newLatLng(myCoordinates));
            gMap.moveCamera(CameraUpdateFactory.zoomTo(PPConsts.GMAP_ZOOM));
        }

        // Check if savedPlace is set, if it is add marker and circle
        if(savedPlace != null){
            LatLng placeCoordinates = new LatLng(savedPlace.geometry.location.lat, savedPlace.geometry.location.lng);
            MarkerOptions marker = buildGooglePlaceMarker(savedPlace,placeCoordinates);
            savedMarker = gMap.addMarker(marker);
            gMap.addCircle(buildPlaceRadius(placeCoordinates));
        }
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

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // Add place to geofence
        GooglePlaceResponse.Place place = new GooglePlaceResponse.Place(newGeoTitle, newGeoSnippet, newGeoLat, newGeoLon);
        geoPanel.addGeofencesMarkerChosen(currentContext, place, PPConsts.BENCHMARK_DISTANCE);
        savedPlace = place;

        // Reset map and plot current location, geofenced location and radius
        LatLng placeCoordinates = new LatLng(place.geometry.location.lat, place.geometry.location.lng);
        LatLng currentPosition = posMarker.getPosition();
        MarkerOptions placeMarkerOptions = buildGooglePlaceMarker(place, placeCoordinates);
        gMap.clear();
        savedMarker = gMap.addMarker(placeMarkerOptions);
        posMarker = gMap.addMarker(new MarkerOptions().position(currentPosition));
        gMap.addCircle(buildPlaceRadius(placeCoordinates));
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) { }

    // Task to query google places and return data to map
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

            // Add current position
            posMarker = gMap.addMarker(new MarkerOptions()
                    .position(currentPosition));
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(posMarker.getPosition());

            // Add all places returned from response
            for(int i = 0; i < response.results.size(); i++){
                GooglePlaceResponse.Place place = response.results.get(i);
                LatLng coordinates = new LatLng(place.geometry.location.lat, place.geometry.location.lng);
                builder.include(coordinates);
                MarkerOptions marker = buildGooglePlaceMarker(place, coordinates);
                gMap.addMarker(marker);
            }

            // Zoom to fit all markers
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 100);
            gMap.animateCamera(cu);
        }
    }

    private class PPGoogleInfoAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            // Special case (current location point) don't show info window
            if (marker.getId().equals(posMarker.getId()))
                return null;

            View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);
            TextView title = (TextView) v.findViewById(R.id.infoWindowTitle);
            TextView lineOne = (TextView) v.findViewById(R.id.infoLine1);
            title.setText(marker.getTitle());
            lineOne.setText(marker.getSnippet());

            return v;
        }
    }

    private class PPGoogleInfoClickListener implements GoogleMap.OnInfoWindowClickListener {
        @Override
        public void onInfoWindowClick(Marker marker) {
            LatLng coords = marker.getPosition();
            newGeoLon = coords.longitude;
            newGeoLat = coords.latitude;
            newGeoTitle = marker.getTitle();
            newGeoSnippet = marker.getSnippet();

            MarkerConfirmation conf = new MarkerConfirmation();
            conf.show(getFragmentManager(), "MarkerConfirmation");
        }
    }

    private class PPGoogleMarkerClickListener implements GoogleMap.OnMarkerClickListener {

        @Override
        public boolean onMarkerClick(Marker marker) {
            if (marker.getId().equals(posMarker.getId()))
                return false;

            LatLng coords = marker.getPosition();
            float[] results = new float[3];
            Location.distanceBetween(currentPosition.latitude, currentPosition.longitude, coords.latitude, coords.longitude, results);
            distanceFromMe = results[0];

            if(distanceFromMe <= PPConsts.BENCHMARK_DISTANCE) {
                Context context = getApplicationContext();
                String text = String.format("Already within %d meters.", (int) PPConsts.BENCHMARK_DISTANCE);
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
            return false;
        }
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
                    gMap.animateCamera(CameraUpdateFactory.zoomTo(PPConsts.GMAP_ZOOM));

                }
                currentPosition = coordinates;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) { }
    }
}
