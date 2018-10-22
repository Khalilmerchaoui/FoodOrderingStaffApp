package app.taxipizzastaff.services;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.FirebaseDatabase;

import app.taxipizzastaff.Utils.Config;
import app.taxipizzastaff.models.Staff;

public class UpdateLocationService extends Service implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private static int Update_INTERVAL = 8000;
    private static int FASTEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 60;

    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;

    Location deliveryGuyLocation;
    FirebaseDatabase database;

    public UpdateLocationService() {
    }

    private void setUpLocation() {
        buildGoogleApiClient();
        createLocationRequest();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Update_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,  this);
    }

    private void updateLocation() {
        Toast.makeText(this, "Update", Toast.LENGTH_SHORT).show();
        try {
            deliveryGuyLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (deliveryGuyLocation != null) {
                LatLng deliveryGuy = new LatLng(deliveryGuyLocation.getLatitude(), deliveryGuyLocation.getLongitude());

                Staff updateStaff = Config.getCurrentUser(getApplicationContext());
                updateStaff.setLatitude(String.valueOf(deliveryGuy.latitude));
                updateStaff.setLongitude(String.valueOf(deliveryGuy.longitude));

                database = FirebaseDatabase.getInstance();
                database.getReference("Staff").child(Config.getCurrentUser(getApplicationContext()).getName()).setValue(updateStaff);

            }
        } catch (SecurityException e) {

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
            mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        setUpLocation();
        updateLocation();
    }
}
