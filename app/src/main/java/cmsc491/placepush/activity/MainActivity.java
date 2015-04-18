package cmsc491.placepush.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import cmsc491.placepush.R;


public class MainActivity extends ActionBarActivity implements OnMapReadyCallback {

    public LocationManager locationManager;
    public MapLocationListener mapLocationListener = new MapLocationListener();
    private GoogleMap gMap;
    private Marker posMarker;
    private ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    }

    private void handleIntent(Intent intent){
        // Handle Search Intent
        if(intent.ACTION_SEARCH.equals(intent.getAction())){
            String query = intent.getStringExtra(SearchManager.QUERY);
            // do something with query
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
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        LatLng coordinates = new LatLng(location.getLatitude(), location.getLongitude());
        posMarker = gMap.addMarker(new MarkerOptions()
            .position(coordinates));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(coordinates));
        gMap.moveCamera(CameraUpdateFactory.zoomTo(15));


    }

    private class MapLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            LatLng coordinates = new LatLng(location.getLatitude(), location.getLongitude());
            posMarker.setPosition(coordinates);
            gMap.moveCamera(CameraUpdateFactory.newLatLng(coordinates));

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
}
