package com.example.android.whereis;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int PERMISSION_FINE_LOCATION_REQUEST_ID = 1122;
    private Location mLocation;
    private View mCustomMarkerView;
    private ImageView mProfileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        GoogleServicesUtil.initFusedLocationProviderClient(this);

        mCustomMarkerView = getLayoutInflater().inflate(R.layout.view_map_icon, null);
        mProfileImageView = mCustomMarkerView.findViewById(R.id.user_photo_image_view);

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

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_FINE_LOCATION_REQUEST_ID);
        } else {
            this.getLastLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_FINE_LOCATION_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.getLastLocation();
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        Log.v(MapsActivity.class.getSimpleName(), "getLastLocation");
        GoogleServicesUtil.mFusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(final Location location) {
                        Log.v(MapsActivity.class.getSimpleName(), location == null ? "null" : location.getLatitude() + "");

                        if (location != null) {
                            mLocation = location;
                            new DownloadProfileImageTask(MapsActivity.this).execute(null, null, null);
                        }
                    }
                });
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

    public void addMarker(Bitmap userProfileImage) {
        if (mLocation != null) {
            LatLng position = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            BitmapDescriptor markerIcon = getMarkerIconFromDrawable(userProfileImage);
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(markerIcon)
                    .anchor(0.5f, 0.7f)
                    .title(UserInfoUtil.getUserDisplayName(MapsActivity.this)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
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
            activityReference.get().addMarker(userProfileImage);
        }
    }
}
