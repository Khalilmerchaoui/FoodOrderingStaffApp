package app.taxipizzastaff.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.taxipizzastaff.Helper.CustomInfoWindow;
import app.taxipizzastaff.R;
import app.taxipizzastaff.Remote.IGoogleAPI;
import app.taxipizzastaff.Utils.Config;
import app.taxipizzastaff.Utils.DirectionJSONParser;
import app.taxipizzastaff.models.Food;
import app.taxipizzastaff.models.Order;
import app.taxipizzastaff.models.Request;
import app.taxipizzastaff.models.Sorted;
import app.taxipizzastaff.models.Staff;
import app.taxipizzastaff.models.User;
import app.taxipizzastaff.services.GpsService;
import app.taxipizzastaff.viewHolders.MealsViewHolder;
import retrofit2.Call;
import retrofit2.Callback;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {

    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;

    private GoogleMap mMap;

    FirebaseDatabase database;
    DatabaseReference requests, meals;

    RecyclerView recyclerView;

    List<Sorted> arrayList;
    List<Sorted> sortedOrders = new ArrayList<>();
    Button btnDrop;

    private IGoogleAPI mService;

    LatLng location;
    Location deliveryGuyLocation;
    TextView txtOrderId, txtDistance, txtTimeRemaining;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private static int Update_INTERVAL = 8000;
    private static int FASTEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 60;

    final String[] totalAmount = {""};

    Polyline directions;
    Marker deliveryGuyMarker;

    FirebaseRecyclerAdapter<Order, MealsViewHolder> adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        txtDistance = findViewById(R.id.txtDistance);
        txtOrderId = findViewById(R.id.txtOrderId);
        txtTimeRemaining = findViewById(R.id.txtTime);

        btnDrop = findViewById(R.id.btnDrop);
        if (getIntent() != null) {
            arrayList = (List<Sorted>) getIntent().getSerializableExtra("orders");
            sortedOrders = getSortedOrders(arrayList);
        }

        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");
        meals = database.getReference("Meals");

        mService = Config.getGoogleAPI();
        setUpLocation();

        updateOrderStatusToTwo();
        updateCurrentDeliveryGuyOrder();

        btnDrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPaymentDialog(sortedOrders.get(0).getKey(), totalAmount[0]);
            }
        });
    }

    private void updateCurrentDeliveryGuyOrder() {
        database.getReference("Staff")
                .child(Config.getCurrentUser(getApplicationContext()).getName())
                .child("currentOrder")
                .setValue(sortedOrders.get(0).getKey());
        Staff update = Config.getCurrentUser(getApplicationContext());
        update.setCurrentOrder(sortedOrders.get(0).getKey());
        Config.setCurrentUser(getApplicationContext(), update);
    }

    private void updateOrderStatusToTwo() {
        requests.child(sortedOrders.get(0).getKey()).child("status").setValue("2");
    }

    private void setUpLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);
        }
        else {
            if(checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    String clientNumber;
    private void displayLocation() {
        try {
            requests.child(sortedOrders.get(0).getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Request request = dataSnapshot.getValue(Request.class);
                    if (request != null) {
                        try {
                            String[] latlng = request.getDeliveryLocation().split(",");
                            double lat = Double.parseDouble(latlng[0]);
                            double lng = Double.parseDouble(latlng[1]);
                            location = new LatLng(lat, lng);
                            totalAmount[0] = request.getTotal();
                            database.getReference("Users").child(request.getPhone()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    User user = dataSnapshot.getValue(User.class);
                                    clientNumber = user.getPhone();
                                    mMap.addMarker(new MarkerOptions()
                                            .position(location)
                                            .title(user.getName())
                                            .snippet(user.getThumb_image() + "," + user.getPhone())
                                            .icon(BitmapDescriptorFactory.fromBitmap(Config.getDeliveryLocationMarker(getApplicationContext()))));
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }

                            deliveryGuyLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                            if (deliveryGuyLocation != null) {
                                LatLng deliveryGuy = new LatLng(deliveryGuyLocation.getLatitude(), deliveryGuyLocation.getLongitude());

                                if (deliveryGuyMarker != null)
                                    deliveryGuyMarker.remove();

                                deliveryGuyMarker = mMap.addMarker(new MarkerOptions()
                                        .position(deliveryGuy)
                                        .icon(BitmapDescriptorFactory.fromBitmap(Config.getGuyLocationMarker(getApplicationContext()))));

                                Staff updateStaff = Config.getCurrentUser(getApplicationContext());
                                updateStaff.setLatitude(String.valueOf(deliveryGuy.latitude));
                                updateStaff.setLongitude(String.valueOf(deliveryGuy.longitude));

                                database.getReference("Staff").child(Config.getCurrentUser(getApplicationContext()).getName()).setValue(updateStaff);

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(deliveryGuy, 17.0f));

                                txtOrderId.setText(String.format("Commande: #%s", sortedOrders.get(0).getKey()));

                                if (directions != null)
                                    directions.remove();

                                getDirections(deliveryGuy, location);
                            }

                        } catch (NullPointerException e) {
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }catch (Exception e) {}
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

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS) {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            else {
                Toast.makeText(this, "This deveice is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void showPaymentDialog(String orderId, String total) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.format("Commande: #%s", orderId));
        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.total_dialog, null);
        builder.setView(view);

        recyclerView = view.findViewById(R.id.recyclerMeals);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new FirebaseRecyclerAdapter<Order, MealsViewHolder>(
                Order.class,
                R.layout.meal_item_layout,
                MealsViewHolder.class,
                requests.child(orderId).child("orders")
        ) {
            @Override
            protected void populateViewHolder(final MealsViewHolder viewHolder, final Order model, int position) {

                meals.child(model.getProductId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Food food = dataSnapshot.getValue(Food.class);
                        viewHolder.txtMealName.setText(food.getName());

                        Picasso.with(MapsActivity.this)
                                .load(food.getImageOne())
                                .error(getResources().getDrawable(R.drawable.profile_icon))
                                .into(viewHolder.imgMeal);

                        TextDrawable drawable = TextDrawable.builder()
                                .buildRound("" + model.getQuantity(), ContextCompat.getColor(getApplicationContext(), R.color.btnSignUp));
                        viewHolder.imgCount.setImageDrawable(drawable);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };
        recyclerView.setAdapter(adapter);

        TextView txtTotal = view.findViewById(R.id.txtTotal);
        txtTotal.setText(total);
        builder.setPositiveButton("CONFIRMER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateOrderStatusToFour();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void updateOrderStatusToFour() {
        requests.child(sortedOrders.get(0).getKey()).child("status").setValue("4").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (sortedOrders.size() > 1) {
                    sortedOrders.remove(0);
                    List<Sorted> newList = new ArrayList<>();
                    for (Sorted sorted : sortedOrders)
                        newList.add(new Sorted(sorted.getKey(), calculateDistance(sorted.getDeliveryLocation()), sorted.getDeliveryLocation()));
                    startActivity(getIntent().putExtra("orders", (Serializable) newList));
                    finish();
                } else {
                    sortedOrders.clear();
                    database.getReference("Staff")
                            .child(Config.getCurrentUser(getApplicationContext()).getName())
                            .child("currentOrder")
                            .setValue("null");

                    Staff user = Config.getCurrentUser(getApplicationContext());
                    user.setCurrentOrder("null");
                    Config.setCurrentUser(getApplicationContext(), user);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String sortedJson = gson.toJson(sortedOrders);
        editor.putString("list", sortedJson);
        editor.commit();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new CustomInfoWindow(MapsActivity.this));
        mMap.setOnInfoWindowClickListener(this);
    }

    private List<Sorted> getSortedOrders(List<Sorted> orders) {
        for (int i = 0; i < orders.size(); i++) {
            for (int j = i; j < orders.size(); j++) {
                if (orders.get(i).getDistance() > orders.get(j).getDistance()) {
                    swapDistances(orders, i, j);
                    swapKeys(orders, i, j);
                    swapLocations(orders, i, j);
                }
            }
        }
        List<String> keys = new ArrayList<>();
        for (Sorted sorted : orders)
            keys.add(sorted.getKey());
        return orders;
    }

    private void swapLocations(List<Sorted> orders, int i, int j) {
        String temp = orders.get(i).getDeliveryLocation();
        orders.get(i).setDeliveryLocation(orders.get(j).getDeliveryLocation());
        orders.get(j).setDeliveryLocation(temp);
    }

    void swapDistances(List<Sorted> orders, int i, int j) {
        float temp = orders.get(i).getDistance();
        orders.get(i).setDistance(orders.get(j).getDistance());
        orders.get(j).setDistance(temp);
    }

    void swapKeys(List<Sorted> orders, int i, int j) {
        String temp = orders.get(i).getKey();
        orders.get(i).setKey(orders.get(j).getKey());
        orders.get(j).setKey(temp);
    }

    private float calculateDistance(String deliveryLocation) {
        float[] distance = new float[1];
        String[] latlng = deliveryLocation.split(",");
        double lat = Double.parseDouble(latlng[0]);
        double lng = Double.parseDouble(latlng[1]);
        LatLng location = new LatLng(lat, lng);
        GpsService gps = new GpsService(this);
        Location.distanceBetween(location.latitude, location.longitude, gps.getLatitude(), gps.getLongitude(), distance);
        return distance[0] / 1000;
    }

    @Override
    public void onLocationChanged(Location location) {
        deliveryGuyLocation = location;
        displayLocation();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,  this);
    }

    int CALL_PERMISSION_CODE = 1010;
    @Override
    public void onInfoWindowClick(Marker marker) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_CODE);
        } else {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + clientNumber));
            startActivity(intent);
        }
    }

    private void updateOrderToStatusThree() {
        requests.child(sortedOrders.get(0).getKey()).child("status").setValue("3");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PERMISSION_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + clientNumber));
                startActivity(intent);
            }
        } else if(requestCode == MY_PERMISSION_REQUEST_CODE) {
            setUpLocation();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return true;
    }


    @SuppressLint("StaticFieldLeak")
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionJSONParser parser = new DirectionJSONParser();
                routes = parser.parse(jsonObject);
            } catch (JSONException e) {
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {

            ArrayList points;
            PolylineOptions lineOptions = null;
            for (int i = 0; i < lists.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = lists.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(ContextCompat.getColor(getApplicationContext(), R.color.btnSignActive));
                lineOptions.geodesic(true);
            }
            try {
                directions = mMap.addPolyline(lineOptions);
            } catch (NullPointerException e) {}
        }
    }

    private void getDirections(final LatLng currentPosition, final LatLng deliveryLocation) {
        String requestAPI = null;
        try {
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination=" + deliveryLocation.latitude + "," + deliveryLocation.longitude + "&" +
                    "keys=" + getResources().getString(R.string.google_direction_api);

            mService.getPath(requestAPI).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                    try {
                        new ParserTask().execute(response.body().toString());

                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray routes = jsonObject.getJSONArray("routes");
                        JSONObject object = routes.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");
                        JSONObject legObject = legs.getJSONObject(0);

                        JSONObject distanceObject = legObject.getJSONObject("distance");
                        String distance = distanceObject.getString("text");
                        int dis = distanceObject.getInt("value");
                        txtDistance.setText(distance);

                        if(dis < 200)
                            updateOrderToStatusThree();

                        JSONObject durationObject = legObject.getJSONObject("duration");
                        String duration = durationObject.getString("text").replace(" mins", "").replace(" min", "");
                        txtTimeRemaining.setText(duration);
                    } catch (Exception e) {
                        getDirections(currentPosition, deliveryLocation);
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
