package app.taxipizzastaff.activities;

import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.taxipizzastaff.R;
import app.taxipizzastaff.Utils.Config;
import app.taxipizzastaff.models.Food;
import app.taxipizzastaff.models.Order;
import app.taxipizzastaff.models.Sorted;
import app.taxipizzastaff.viewHolders.MealsViewHolder;
import retrofit2.Call;
import retrofit2.Callback;

public class ConfirmOrderActivity extends AppCompatActivity {


    FirebaseDatabase database;
    DatabaseReference requests, meals;

    FirebaseRecyclerAdapter<Order, MealsViewHolder> adapter;

    RecyclerView recyclerView;
    Button btnConfirm;
    TextView txtTotal;

    LatLng deliveryLocation, myLocationLatLng;
    String orderId, total, delivery, myLocation;
    List<Sorted> sortedOrders = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_order);

        recyclerView = findViewById(R.id.recyclerMeals);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        btnConfirm = findViewById(R.id.btnConfirm);
        txtTotal = findViewById(R.id.txtTotal);

        Intent intent = getIntent();
        if(intent != null) {
            orderId = intent.getStringExtra("orderId");
            total = intent.getStringExtra("total");
            delivery = intent.getStringExtra("delivery");
            myLocation = intent.getStringExtra("myLocation");
            sortedOrders = (List<Sorted>) intent.getSerializableExtra("orders");
        }

        double lat = Double.parseDouble(delivery.split(",")[0]);
        double lng = Double.parseDouble(delivery.split(",")[1]);
        deliveryLocation = new LatLng(lat, lng);

        double lat1 = Double.parseDouble(myLocation.split(",")[0]);
        double lng1 = Double.parseDouble(myLocation.split(",")[1]);
        myLocationLatLng = new LatLng(lat1, lng1);

        txtTotal.setText(total);
        database = FirebaseDatabase.getInstance();
        meals = database.getReference("Meals");
        requests = database.getReference("Requests");

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateOrderStatusToFour();
            }
        });

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

                        Picasso.with(getApplicationContext())
                                .load(food.getImageOne())
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
    }

    private void updateOrderStatusToFour() {
        requests.child(orderId).child("status").setValue("4");
        if (sortedOrders.size() > 1) {
            sortedOrders.remove(0);
            setDistance(myLocationLatLng, deliveryLocation);
        } else {
            finish();
        }
    }

    private void setDistance(final LatLng currentPosition, final LatLng deliveryLocation) {
        String requestAPI = null;
        try {
            requestAPI = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination=" + deliveryLocation.latitude + "," + deliveryLocation.longitude + "&" +
                    "keys=" + getResources().getString(R.string.google_direction_api);

            Config.getGoogleAPI().getPath(requestAPI).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                    try {

                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray routes = jsonObject.getJSONArray("routes");
                        JSONObject object = routes.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");
                        JSONObject legObject = legs.getJSONObject(0);

                        JSONObject distanceObject = legObject.getJSONObject("distance");
                        String distance = distanceObject.getString("text");

                        List<Sorted> newList = new ArrayList<>();
                        for (Sorted sorted : sortedOrders)
                            newList.add(new Sorted(sorted.getKey(), Float.parseFloat(distance), sorted.getDeliveryLocation()));
                        startActivity(new Intent(ConfirmOrderActivity.this, MapsActivity.class).putExtra("orders", (Serializable) newList));


                    } catch (Exception e) {
                        setDistance(currentPosition, deliveryLocation);
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
