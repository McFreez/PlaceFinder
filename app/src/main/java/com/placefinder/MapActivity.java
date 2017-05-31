package com.placefinder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsStep;
import com.placefinder.DTO.Comment;
import com.placefinder.DTO.Photo;
import com.placefinder.DTO.Place;
import com.placefinder.DTO.Route;
import com.placefinder.adapters.CommentsAdapter;
import com.placefinder.adapters.ItemPagerAdapter;
import com.placefinder.network.GoogleApiRequests;
import com.placefinder.network.ServerRequests;
import com.placefinder.notification.PlaceFinderFirebaseMessagingService;
import com.placefinder.placedetailspanel.BottomSheetBehaviorGoogleMapsLike;
import com.placefinder.placedetailspanel.BottomPanelBehaviour;
import com.placefinder.placedetailspanel.MergedAppBarLayoutBehavior;
import com.placefinder.xslt.GeocodingResult;
import com.placefinder.xslt.NetworkUtils;
import com.placefinder.xslt.XSLTConverters;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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
        LocationSource, LocationListener, GoogleMap.OnMarkerClickListener {

    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 428;
    private final String LOG_TAG = "MapActivity";

    private GoogleMap mMap;
    private FloatingActionButton locationFAB;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private OnLocationChangedListener mLocationListener;
    private ItemPagerAdapter mViewPagerAdapter;
    private CommentsAdapter mCommentsAdapter;

    private DrawerLayout mDrawerLayout;
    private FloatingSearchView mFloatingSearchView;
    private NavigationView mNavigationView;
    private BottomSheetBehaviorGoogleMapsLike mBottomSheetBehaviour;
    private TextView mBottomSheetPeekTitle;
    private TextView mBottomSheetPeekDescription;
    private LinearLayout mButtonDeletePlace;
    private LinearLayout mButtonClearRoute;
    private LinearLayout mButtonBuildRouteToPlace;
    private NestedScrollView mBottomSheet;

    private TextView userNameTextView;
    private TextView userEmailTextView;
    private CircleImageView userImage;

    private FirebaseAuth mapAuth;
    private FirebaseUser mCurrentUser;
    private StorageReference mImageRef;
    private StorageReference mUserImagesRef;

    private boolean isGoogleApiConnected = false;
    private boolean isLoadingAllPlacesAroundUserStarted = false;

    private List<Place> places = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private Place mSelectedPlace;

    private BroadcastReceiver placesChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if(extras != null) {
                if(extras.containsKey("action")) {
                    String action = extras.getString("action");
                    if(action.equals(PlaceFinderFirebaseMessagingService.actionPlaceDeleted)){
                        if(extras.containsKey("id")) {
                            removePlace(Long.valueOf(extras.getString("id")));
                        }
                    }
                    else
                        if(action.equals(PlaceFinderFirebaseMessagingService.actionPlaceAdded)) {
                            Place place = new Place();
                            if (extras.containsKey("id"))
                                place.setId(Long.valueOf(extras.getString("id")));
                            if (extras.containsKey("title"))
                                place.setTitle(extras.getString("title"));
                            if (extras.containsKey("ownerGoogleId"))
                                place.setOwnerGoogleId(extras.getString("ownerGoogleId"));
                            if (extras.containsKey("description"))
                                place.setDescription(extras.getString("description"));
                            if (extras.containsKey("latitude"))
                                place.setLatitude(Double.valueOf(extras.getString("latitude")));
                            if (extras.containsKey("longitude"))
                                place.setLongitude(Double.valueOf(extras.getString("longitude")));

                            addPlace(place);
                        }
                }
            }
        }
    };

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
        mCurrentUser = mapAuth.getCurrentUser();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference(/*"gs://placefinder-65616.appspot.com/"*/);
        mImageRef = storageRef.child("images");
        mUserImagesRef = storageRef.child("userImages");

        initControls();

        registerReceiver(placesChangeReceiver, new IntentFilter(PlaceFinderFirebaseMessagingService.INTENT_FILTER));
    }

    private void initControls(){
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

        configureBottomSheet();
    }

    private void configureBottomSheet(){
        mBottomSheet = (NestedScrollView) findViewById(R.id.bottom_sheet);
        View bottomSheetContent = findViewById(R.id.bottom_sheet_content);

        RelativeLayout bottomSheetPeek = (RelativeLayout) bottomSheetContent.findViewById(R.id.bottom_sheet_peek);
        mButtonDeletePlace = (LinearLayout) bottomSheetContent.findViewById(R.id.button_delete_place);
        mButtonClearRoute = (LinearLayout) bottomSheetContent.findViewById(R.id.button_clear_route);
        mButtonBuildRouteToPlace = (LinearLayout) bottomSheetContent.findViewById(R.id.button_build_route);
        LinearLayout buttonAddPLaceImage = (LinearLayout) bottomSheetContent.findViewById(R.id.button_add_place_image);

        mBottomSheetPeekTitle = (TextView) bottomSheetContent.findViewById(R.id.bottom_sheet_peek_title);
        mBottomSheetPeekDescription = (TextView) findViewById(R.id.bottom_sheet_peek_details);
        mBottomSheetBehaviour = BottomSheetBehaviorGoogleMapsLike.from(mBottomSheet);

        BottomPanelBehaviour bottomPanelBehaviour = new BottomPanelBehaviour(this, mBottomSheetPeekTitle, mBottomSheetPeekDescription, bottomSheetPeek, locationFAB, mBottomSheetBehaviour, mFloatingSearchView);

        bottomSheetPeek.setOnClickListener(bottomPanelBehaviour);
        mButtonDeletePlace.setOnClickListener(bottomPanelBehaviour);
        mButtonClearRoute.setOnClickListener(bottomPanelBehaviour);
        mButtonClearRoute.setVisibility(View.GONE);
        mButtonBuildRouteToPlace.setOnClickListener(bottomPanelBehaviour);
        buttonAddPLaceImage.setOnClickListener(bottomPanelBehaviour);

        mBottomSheetBehaviour.addBottomSheetCallback(bottomPanelBehaviour);

        AppBarLayout mergedAppBarLayout = (AppBarLayout) findViewById(R.id.merged_appbarlayout);
        MergedAppBarLayoutBehavior mergedAppBarLayoutBehavior = MergedAppBarLayoutBehavior.from(mergedAppBarLayout);
        mergedAppBarLayoutBehavior.setToolbarTitle("Title Dummy");
        mergedAppBarLayoutBehavior.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheet.scrollTo(0, 0);
                mBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
            }
        });

        List<Bitmap> images = new ArrayList<>();
        List<Photo> photos = new ArrayList<>();

        mViewPagerAdapter = new ItemPagerAdapter(this, images, photos, mImageRef, mCurrentUser.getUid());
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(mViewPagerAdapter);
        viewPager.setPageTransformer(true, new DepthPageTransformer());

        RecyclerView commentsList = (RecyclerView) bottomSheetContent.findViewById(R.id.rv_comments);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        commentsList.setLayoutManager(layoutManager);
        commentsList.setHasFixedSize(false);

        EditText commentText = (EditText) bottomSheetContent.findViewById(R.id.et_comment_text);
        ImageView buttonCreateComment = (ImageView) bottomSheetContent.findViewById(R.id.button_create_comment);
        LinearLayout commentsReplacer = (LinearLayout) bottomSheetContent.findViewById(R.id.comments_replacer);

        List<Comment> comments = new ArrayList<>();
        mCommentsAdapter = new CommentsAdapter(comments, this, mCurrentUser, mSelectedPlace, commentText, buttonCreateComment, mUserImagesRef, commentsReplacer);
        commentsList.setAdapter(mCommentsAdapter);

        mBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN);

    }

    private void showBuildRouteButton(){
        mButtonClearRoute.setVisibility(View.GONE);
        mButtonBuildRouteToPlace.setVisibility(View.VISIBLE);
    }

    private void showClearRouteButton(){
        mButtonBuildRouteToPlace.setVisibility(View.GONE);
        mButtonClearRoute.setVisibility(View.VISIBLE);
    }

    private void setNavigationViewUserData(){
        if(mCurrentUser != null){
            userNameTextView.setText(mCurrentUser.getDisplayName());
            userEmailTextView.setText(mCurrentUser.getEmail());

            if(mCurrentUser.getPhotoUrl() != null)
                new GetUserImageTask().execute(mCurrentUser.getPhotoUrl());
        }
    }

    private void uploadUserImageToStorage(Bitmap image){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = mUserImagesRef.child(mCurrentUser.getUid()).putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(MapActivity.this.LOG_TAG, "Error uploading user image. User google id : " + mCurrentUser.getUid());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.i(MapActivity.this.LOG_TAG, "User image uploaded : " + mCurrentUser.getUid());
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(Route.innerLine != null && Route.outerLine != null && Route.placeDestination != null)
            clearCurrentRoute();
        else {
            switch (mBottomSheetBehaviour.getState()){
                case BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED:
                    hideBottomSheet();
                    break;
                case BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT:
                    mBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
                    break;
                case BottomSheetBehaviorGoogleMapsLike.STATE_EXPANDED:
                    mBottomSheet.scrollTo(0, 0);
                    mBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT);
                    break;
            }
        }
    }

    private void hideBottomSheet(){
        mBottomSheet.scrollTo(0, 0);
        mBottomSheetBehaviour.setHideable(true);
        mBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN);
    }

    private void showBottomSheet(){
        mBottomSheet.scrollTo(0, 0);
        mBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
        mBottomSheetBehaviour.setHideable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMap();
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();

        subscribeToPlacesUpdates();

    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(placesChangeReceiver);
        super.onDestroy();

    }

    //</editor-fold>

    //<editor-fold desc="Map">
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap != null) {
            return;
        }
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
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
    public boolean onMarkerClick(Marker marker) {

        long id = (long) marker.getTag();
        if(id != 0){

            Place selectedPlace = null;
            for (Place p : places){
                if(p.getId() == id) {
                    selectedPlace = p;
                    break;
                }
            }
            if(selectedPlace == null)
                Toast.makeText(this, "Can`t find this place", Toast.LENGTH_SHORT).show();
            else
            {
                if(!mCurrentUser.getUid().equals(selectedPlace.getOwnerGoogleId()))
                {
                    mButtonDeletePlace.setVisibility(View.GONE);
                }
                mBottomSheetPeekTitle.setText(selectedPlace.getTitle());
                mBottomSheetPeekDescription.setText(selectedPlace.getDescription());
                mViewPagerAdapter.updateDataSource(selectedPlace, false);
                mCommentsAdapter.updateDataSource(selectedPlace, false);
                showBottomSheet();
                mSelectedPlace = selectedPlace;

                if(mSelectedPlace == Route.placeDestination){
                    showClearRouteButton();
                }
                else {
                    showBuildRouteButton();
                }
            }
        }

        return true;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //Toast.makeText(this, "Run task", Toast.LENGTH_SHORT).show();
        GeolocationSearchTask task = new GeolocationSearchTask();
        task.location = latLng;
        task.execute(NetworkUtils.buildUrl(latLng));
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
            if(!isLoadingAllPlacesAroundUserStarted)
                loadAllPlacesAroundUser(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
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
        mLastLocation = location;
        if(!isLoadingAllPlacesAroundUserStarted)
            loadAllPlacesAroundUser(new LatLng(location.getLatitude(), location.getLongitude()));
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

    private void runConverter(String response, LatLng exactLocation) {
        response = XSLTConverters.xsl(this, response);

        if (response == null) {
            Toast.makeText(this, "Failed to convert data", Toast.LENGTH_SHORT).show();
            return;
        }

        List<GeocodingResult> results = XSLTConverters.getResults(response);

        if (results == null) {
            //Toast.makeText(this, "Failed to represent converted data", Toast.LENGTH_SHORT).show();
            return;
        } else if (results.size() == 0) {
            //Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show();
            return;
        } else {
            //Toast.makeText(this, "Place found", Toast.LENGTH_SHORT).show();
            openDialog(results.get(0).getAdress(), exactLocation);
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
/*                Place place = new Place();
                place.setTitle("newTitle");
                place.setDescription("new Description");
                //place.setOwnerGoogleId(mCurrentUser.getUid());
                place.setLatitude(55.3234);
                place.setLongitude(656.234);

                ServerRequests.PostPlaceTask postPlaceTask = new ServerRequests.PostPlaceTask(this);
                postPlaceTask.execute(place);*/
                /*ServerRequests.DeletePlaceTask deletePlaceTask = new ServerRequests.DeletePlaceTask(this);
                deletePlaceTask.execute((long) 7);*/
                /*locationFAB.animate().rotationX(10).setDuration(300).start();*/
                //mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                return false;
            default:
                return true;
        }

    }
    //</editor-fold>

    //<editor-fold desc="Places visualisation">
    private void openDialog(String fullAddress, LatLng location){
        PlaceInfoDialogFragment fragment = new PlaceInfoDialogFragment();
        fragment.fullAddress = fullAddress;
        fragment.latitude = location.latitude;
        fragment.longitude = location.longitude;
        fragment.creatorUid = mCurrentUser.getUid();

        fragment.show(getSupportFragmentManager(), PlaceInfoDialogFragment.TAG);
    }

    public void addPlaceDialogResult(Place place){
        Toast.makeText(this, place.getTitle(), Toast.LENGTH_LONG).show();
        ServerRequests.PostPlaceTask postPlaceTask = new ServerRequests.PostPlaceTask(this);
        postPlaceTask.execute(place);
    }

    public void onPlaceSaveFinished(ResponseEntity<Place> placeEntity){
        if(placeEntity.getBody() == null){
            Toast.makeText(this, "Failed to save place, code: " + placeEntity.getStatusCode().toString(), Toast.LENGTH_LONG).show();
        }
        else
        {
            addPlace(placeEntity.getBody());
            mSelectedPlace = placeEntity.getBody();
            mBottomSheetPeekTitle.setText(placeEntity.getBody().getTitle());
            mBottomSheetPeekDescription.setText(placeEntity.getBody().getDescription());
            mViewPagerAdapter.updateDataSource(mSelectedPlace, true);
            mCommentsAdapter.updateDataSource(mSelectedPlace, true);
            showBottomSheet();
        }
    }

    public void showToast(ResponseEntity<Place> placeEntity){
        Toast.makeText(this, placeEntity.getStatusCode().toString(), Toast.LENGTH_LONG).show();
    }

    public void onRemovePlaceFinished(Boolean response, long id){
        if(response){
            removePlace(id);
            mViewPagerAdapter.deleteAllImages();
            mViewPagerAdapter.clearData();

            mCommentsAdapter.clearData();
        }
        else
        {
            Toast.makeText(this, "Can`t remove this place", Toast.LENGTH_LONG).show();
        }
    }

    public void tryToRemovePlace(){
        if(mSelectedPlace != null){
            hideBottomSheet();
            new ServerRequests.DeletePlaceTask(this).execute(mSelectedPlace.getId());
        }
    }

    private void removePlace(long id){
        Marker selectedPlaceMarker = null;
        for (Marker m : markers){
            if((long)m.getTag() == id){
                selectedPlaceMarker = m;
                break;
            }
        }
        Place selectedPlace = null;
        for (Place p : places){
            if(p.getId() == id){
                selectedPlace = p;
                break;
            }
        }

        if(selectedPlaceMarker != null){
            markers.remove(selectedPlaceMarker);
            selectedPlaceMarker.remove();
        }

        if(selectedPlace != null){
            places.remove(selectedPlace);
            mSelectedPlace = null;
        }
    }

    private void addPlace(Place place){
        places.add(place);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(place.getLatitude(), place.getLongitude()))
                .title(place.getTitle()));
        marker.setTag(place.getId());
        markers.add(marker);
    }

    public void onGetAllAroundPlacesFinished(ResponseEntity<List<Place>> placeEntity){
        if(placeEntity.getStatusCode() == HttpStatus.OK){
            for(Place p : placeEntity.getBody()){
                addPlace(p);
            }
        }
        else
        {
            Toast.makeText(this, "Cannot load places " + placeEntity.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAllPlacesAroundUser(LatLng location){
        new ServerRequests.GetAllPlacesAroundTask(this).execute(location);
        isLoadingAllPlacesAroundUserStarted = true;
    }
    //</editor-fold>

    //<editor-fold desc="Route">
    public void buildRouteToCurrentPlace(){
        clearCurrentRoute();
        showBottomSheet();
        Route.placeDestination = mSelectedPlace;
        new GoogleApiRequests.DirectionTask(this)
                .execute(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()),
                        new LatLng(mSelectedPlace.getLatitude(), mSelectedPlace.getLongitude()));

    }

    public void onRouteBuildingFinished(DirectionsResult result){
        if(result != null){
            PolylineOptions po1 = new PolylineOptions().geodesic(true).color(Color.BLUE).width(20);
            PolylineOptions po2 = new PolylineOptions().geodesic(true).color(getResources().getColor(R.color.colorPrimary)).width(13);
            po1.add(convertLatLngType(result.routes[0].legs[0].startLocation));
            po2.add(convertLatLngType(result.routes[0].legs[0].startLocation));
            DirectionsStep[] steps = result.routes[0].legs[0].steps;
            for(int i = 0; i < steps.length; i++) {
                List<LatLng> points = PolyUtil.decode(steps[i].polyline.getEncodedPath());
                for(int j = 0; j < points.size(); j++){
                    po1.add(points.get(j));
                    po2.add(points.get(j));
                }
            }
            po1.add(convertLatLngType(result.routes[0].legs[0].endLocation));
            po2.add(convertLatLngType(result.routes[0].legs[0].endLocation));

            po1.endCap(new RoundCap());
            po1.startCap(new RoundCap());
            po1.jointType(JointType.ROUND);
            po2.endCap(new RoundCap());
            po2.startCap(new RoundCap());
            po2.jointType(JointType.ROUND);

            Route.outerLine = mMap.addPolyline(po1);
            Route.innerLine = mMap.addPolyline(po2);

            showClearRouteButton();
        }
        else
            Toast.makeText(this, "Cannot build route", Toast.LENGTH_SHORT).show();
    }

    private LatLng convertLatLngType(com.google.maps.model.LatLng latLng){
        return new LatLng(latLng.lat, latLng.lng);
    }

    public void clearCurrentRoute(){
        if(mBottomSheetBehaviour.getState() == BottomSheetBehaviorGoogleMapsLike.STATE_EXPANDED || mBottomSheetBehaviour.getState() == BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT){
            mBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
        }

        if(Route.outerLine != null)
            Route.outerLine.remove();
        if(Route.innerLine != null)
            Route.innerLine.remove();

        Route.outerLine = null;
        Route.innerLine = null;
        Route.placeDestination = null;

        showBuildRouteButton();
    }
    //</editor-fold>

    private void subscribeToPlacesUpdates(){
        FirebaseMessaging.getInstance().subscribeToTopic("places");
    }

    public void chooseImage(){
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 100){
            final Uri imageUri = data.getData();
            try{

                final InputStream isDisplay = getContentResolver().openInputStream(imageUri);

                if(isDisplay == null)
                    throw new FileNotFoundException();

                Photo photo = new Photo();
                photo.setOwnerGoogleId(mCurrentUser.getUid());
                //photo.setPlace(mSelectedPlace);

                new ServerRequests.PostPhotoTask(mViewPagerAdapter, isDisplay, imageUri, mSelectedPlace).execute(photo);

            }catch (FileNotFoundException e){
                Log.e(MapActivity.this.LOG_TAG, "Error getting image from phone storage by uri : " + imageUri);
                Toast.makeText(MapActivity.this, "Cannot add image", Toast.LENGTH_SHORT).show();
            }
        }
    }

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
            uploadUserImageToStorage(bitmap);
        }
    }

    public class GeolocationSearchTask extends AsyncTask<URL, Void, String> {

        public LatLng location;
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
                //Toast.makeText(MapActivity.this, "Data caught", Toast.LENGTH_SHORT).show();
                runConverter(result, location);
            } else {
                // COMPLETED (16) Call showErrorMessage if the result is null in onPostExecute
                Toast.makeText(MapActivity.this, "Fail, no data", Toast.LENGTH_SHORT).show();

            }
        }
    }

}
