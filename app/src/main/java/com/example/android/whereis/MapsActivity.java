package com.example.android.whereis;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int REQUEST_CHECK_SETTINGS = 543;
    private View mCustomMarkerView;
    private ImageView mProfileImageView;
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates;
    private Switch mIsLocationShared;
    private LocationCallback mLocationCallback;
    private Map<String, Bitmap> mUserProfileImages;
    private Map<String, List<Location>> mUserLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        this.mUserProfileImages = new HashMap<>();
        this.mUserLocations = new HashMap<>();
        this.mRequestingLocationUpdates = false;
        this.mIsLocationShared = findViewById(R.id.is_share_location);
        this.mIsLocationShared.setChecked(this.mRequestingLocationUpdates);
        this.mIsLocationShared.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mRequestingLocationUpdates = isChecked;
                if (isChecked) {
                    createLocationRequest();
                } else {
                    stopLocationUpdates();
                }
            }
        });

        new DownloadProfileImageTask(MapsActivity.this).execute(null, null, null);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        GoogleServicesUtil.initFusedLocationProviderClient(this);

        mCustomMarkerView = getLayoutInflater().inflate(R.layout.view_map_icon, null);
        mProfileImageView = mCustomMarkerView.findViewById(R.id.user_photo_image_view);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                String userId = UserInfoUtil.getUserId(MapsActivity.this);
                for (Location location : locationResult.getLocations()) {
                    Log.v(MapsActivity.class.getSimpleName(), "New location received> "
                            + location.getLatitude()
                            + " - "
                            + location.getLongitude());
                    List<Location> userLocations = mUserLocations.get(userId);
                    if (userLocations == null) {
                        userLocations = new ArrayList<>();
                        userLocations.add(location);
                    } else {
                        Location lastLocation = userLocations.get(userLocations.size() - 1);
                        if (lastLocation.getLongitude() != location.getLongitude() ||
                                lastLocation.getLatitude() != location.getLatitude()) {
                            userLocations.add(location);
                        }
                    }
                    mUserLocations.put(userId, userLocations);
                }
                updateMarkers(userId);
            };
        };
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = this.getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMinZoomPreference(17f);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS: {
                this.createLocationRequest();
            }
        }
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Bitmap image) {
        mProfileImageView.setImageBitmap(image);
        mCustomMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mCustomMarkerView.layout(0, 0, mCustomMarkerView.getMeasuredWidth(), mCustomMarkerView.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(mCustomMarkerView.getMeasuredWidth(),
                mCustomMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mCustomMarkerView.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void addMarker(Bitmap userProfileImage, Location location) {
        if (location != null) {
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            BitmapDescriptor markerIcon = getMarkerIconFromDrawable(userProfileImage);
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(markerIcon)
                    .anchor(0.5f, 0.7f)
                    .title(UserInfoUtil.getUserDisplayName(MapsActivity.this)));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                this.signOut();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        Log.v(MapsActivity.class.getSimpleName(), "signout");
        GoogleServicesUtil.mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.v(MapsActivity.class.getSimpleName(), "signOut redirect");
                        Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
    }

    public void updateMarkers(String userId) {
        mMap.clear();
        List<Location> locations = this.mUserLocations.get(userId);
        if (locations.size() == 0) {
            return;
        }
        for (int i = 0; i < locations.size() - 1; i++) {
            Location location = locations.get(i);
            mMap.addCircle(new CircleOptions()
                    .center(new LatLng(location.getLatitude(), location.getLongitude()))
                    .radius(2)
                    .strokeColor(getResources().getColor(R.color.colorPrimary))
                    .fillColor(getResources().getColor(R.color.colorPrimary)));
        }
        Bitmap userProfileImage = mUserProfileImages.get(userId);
        if (userProfileImage != null) {
            addMarker(userProfileImage, locations.get(locations.size() - 1));
        }
    }

    public void createLocationRequest() {
        this.mRequestingLocationUpdates = true;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapsActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        GoogleServicesUtil.mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    private void stopLocationUpdates() {
        String userId = UserInfoUtil.getUserId(this);
        GoogleServicesUtil.mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        mUserLocations.put(userId, new ArrayList<Location>());
        updateMarkers(userId);
    }

    private static class DownloadProfileImageTask extends AsyncTask<Void, Void, Bitmap> {
        private WeakReference<MapsActivity> activityReference;

        DownloadProfileImageTask(MapsActivity activity) {
            activityReference = new WeakReference<MapsActivity>(activity);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            return DownloadImageUtil.getBitmapFromURL(UserInfoUtil.getUserPhotoUrl(activityReference.get()));
        }

        @Override
        protected void onPostExecute(Bitmap userProfileImage) {
            super.onPostExecute(userProfileImage);
            activityReference.get().mUserProfileImages
                    .put(UserInfoUtil.getUserId(activityReference.get()), userProfileImage);
        }
    }
}
