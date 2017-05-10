package com.placefinder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.placefinder.xslt.GeocodingResult;
import com.placefinder.xslt.NetworkUtils;
import com.placefinder.xslt.XSLTConverters;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        FloatingActionButton.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener ,
        NavigationView.OnNavigationItemSelectedListener,
        LocationSource, LocationListener {

    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 428;
    private final String LOG_TAG = "MapActivity";

    private GoogleMap mMap;
    private FloatingActionButton locationFAB;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private OnLocationChangedListener mLocationListener;

    private DrawerLayout mDrawerLayout;
    private FloatingSearchView mFloatingSearchView;
    private NavigationView mNavigationView;

    private TextView userNameTextView;
    private TextView userEmailTextView;
    private CircleImageView userImage;

    private FirebaseAuth mapAuth;
    private FirebaseUser currentUser;

    private boolean isGoogleApiConnected = false;

    //<editor-fold desc="Life cycle">
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        setUpMap();

        if (mGoogleApiClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.web_client_id))
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
        }

        mapAuth = FirebaseAuth.getInstance();
        currentUser = mapAuth.getCurrentUser();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mFloatingSearchView = (FloatingSearchView) findViewById(R.id.floating_search_view);
        mFloatingSearchView.attachNavigationDrawerToMenuButton(mDrawerLayout);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        View header = mNavigationView.getHeaderView(0);

        userNameTextView = (TextView) header.findViewById(R.id.user_name);
        userEmailTextView = (TextView) header.findViewById(R.id.user_email);
        userImage = (CircleImageView) header.findViewById(R.id.user_icon);

        setNavigationViewUserData();

        locationFAB = (FloatingActionButton) findViewById(R.id.fab_my_location);
        locationFAB.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMap();
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
    //</editor-fold>

    //<editor-fold desc="Map">
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap != null) {
            return;
        }
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSIONS_REQUEST_CODE);
            return;
        }
        setUpMyLocationLayer();

    }

    private void setUpMap() {
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Toast.makeText(this, "Run task", Toast.LENGTH_SHORT).show();
        new GeolocationSearchTask().execute(NetworkUtils.buildUrl(latLng));
    }

    private void moveCamera(CameraUpdate update){
        mMap.animateCamera(update);
    }


    //</editor-fold>

    //<editor-fold desc="Google API Client">

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        isGoogleApiConnected = true;
        enableLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "GoogleApiClient connection has been suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient connection has been failed");
    }

    private void signOut() {
        // Firebase sign out
        mapAuth.signOut();

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        MapActivity.this.finish();
                    }
                });
    }

    //</editor-fold>

    //<editor-fold desc="User location">

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                if(mMap != null)
                    setUpMyLocationLayer();
                if(isGoogleApiConnected)
                    enableLocationUpdates();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setUpMyLocationLayer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSIONS_REQUEST_CODE);

            Toast.makeText(this, "No permission", Toast.LENGTH_LONG).show();
            return;
        }

        mMap.setLocationSource(this);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
    }

    @Override
    public void onClick(View view) {
        if(mLastLocation == null){
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            return;
        }
        moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 13));
    }

    private void enableLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSIONS_REQUEST_CODE);

            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (mLastLocation != null) {
            mLocationListener.onLocationChanged(mLastLocation);
            moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 11));
        }
        if(mLocationRequest == null) {
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000)
                    .setFastestInterval(1000);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        mLocationListener.onLocationChanged(location);
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mLocationListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mLocationListener = null;
    }


    //</editor-fold>

    //<editor-fold desc="Google user data">
    private void setNavigationViewUserData(){
        if(currentUser != null){
            userNameTextView.setText(currentUser.getDisplayName());
            userEmailTextView.setText(currentUser.getEmail());

            if(currentUser.getPhotoUrl() != null)
                new GetUserImageTask().execute(currentUser.getPhotoUrl());
        }
    }
    //</editor-fold>

    private void runConverter(String response) {
        response = XSLTConverters.xsl(this, response);

        if (response == null) {
            Toast.makeText(this, "Failed to convert data", Toast.LENGTH_SHORT).show();
            return;
        }

        List<GeocodingResult> results = XSLTConverters.getResults(response);

        if (results == null) {
            Toast.makeText(this, "Failed to represent converted data", Toast.LENGTH_SHORT).show();
            return;
        } else if (results.size() == 0) {
            Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, "Place found", Toast.LENGTH_SHORT).show();
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(results.get(0).getLatitude(), results.get(0).getLongitude()))
                    .title(results.get(0).getAdress()));
        }
    }

    //<editor-fold desc="Navigation view">
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if(item.isChecked())
            item.setChecked(false);

        mDrawerLayout.closeDrawers();

        switch (item.getItemId()){
            case R.id.exit_app:
                signOut();
                return true;
            case R.id.get_place:
                //new TestRequestToServer.GetOnePlace().execute(1);
                //testConnectionToServerMethod();
                return false;
            default:
                return true;
        }

    }
    //</editor-fold>

    public class GetUserImageTask extends AsyncTask<Uri, Void, Bitmap>{

        @Override
        protected Bitmap doInBackground(Uri... params) {
            try {
                URL url = new URL(params[0].toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                // Log exception
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            userImage.setImageBitmap(bitmap);
        }
    }

    public class GeolocationSearchTask extends AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... params) {
            URL searchUrl = params[0];

            String result = null;

            try {

                result = NetworkUtils.getResponseFromHttpUrl(searchUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            // COMPLETED (27) As soon as the loading is complete, hide the loading indicator

            if (result != null && !result.equals("")) {
                // COMPLETED (17) Call showJsonDataView if we have valid, non-null results
                Toast.makeText(MapActivity.this, "Data caught", Toast.LENGTH_SHORT).show();
                runConverter(result);
            } else {
                // COMPLETED (16) Call showErrorMessage if the result is null in onPostExecute
                Toast.makeText(MapActivity.this, "Fail, no data", Toast.LENGTH_SHORT).show();

            }
        }
    }


}
