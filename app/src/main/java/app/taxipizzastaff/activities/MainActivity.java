package app.taxipizzastaff.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import app.taxipizzastaff.R;
import app.taxipizzastaff.Utils.Config;
import app.taxipizzastaff.models.Request;
import app.taxipizzastaff.models.Sorted;
import app.taxipizzastaff.services.UpdateLocationService;
import app.taxipizzastaff.viewHolders.RequestViewHolder;
import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference orders;

    FirebaseRecyclerAdapter<Request, RequestViewHolder> adapter;

    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;

    Button btnStart;
    List<Sorted> sortedOrders = new ArrayList<>();

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = FirebaseDatabase.getInstance();
        orders = database.getReference("Requests");

        sharedPreferences = getSharedPreferences("MyPref", MODE_PRIVATE);

        String list = sharedPreferences.getString("list", "");
        Log.i("tagged", list.length() + "");
        if(list.length() > 2){
            Gson gson = new Gson();
            Type type = new TypeToken<List<Sorted>>(){}.getType();
            List<Sorted> sortedJson = gson.fromJson(list, type);
            Intent map = new Intent(MainActivity.this, MapsActivity.class);
            map.putExtra("orders", (Serializable) sortedJson);
            startActivity(map);
            finish();
        }

        recyclerView = findViewById(R.id.recyclerView);
        btnStart = findViewById(R.id.btnStart);
        layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);

        Query query = orders.orderByChild("status").equalTo("1");

        adapter = new FirebaseRecyclerAdapter<Request, RequestViewHolder>(
                Request.class,
                R.layout.order_item_layout,
                RequestViewHolder.class,
                query
        ) {
            @Override
            protected void populateViewHolder(RequestViewHolder viewHolder, Request model, int position) {
                viewHolder.txtId.setText(String.format("Commande ID: #%s", adapter.getRef(position).getKey()));
                viewHolder.txtTime.setText(Config.EpochToDate(Long.parseLong(model.getTimeStamp()), "HH:mm"));
                viewHolder.txtTotal.setText(String.format("Total: %s", model.getTotal()));

                double lat = Double.parseDouble(model.getDeliveryLocation().split(",")[0]);
                double lng = Double.parseDouble(model.getDeliveryLocation().split(",")[1]);
                LatLng deliveryLocation = new LatLng(lat, lng);
                setDistance(Config.TaxiPizzaLocation, deliveryLocation, viewHolder.txtDistance, position, model);
            }
        };
        recyclerView.setAdapter(adapter);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sortedOrders.size() != 0) {
                    Intent map = new Intent(MainActivity.this, MapsActivity.class);
                    map.putExtra("orders", (Serializable) sortedOrders);
                    startActivity(map);
                    startService(new Intent(MainActivity.this, UpdateLocationService.class));
                    finish();
                } else {
                    Snackbar.make(v,"Pas de commandes pour le moment", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setDistance(final LatLng currentPosition, final LatLng deliveryLocation, final TextView txtDistance, final int position, final Request model) {
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
                        txtDistance.setText(String.format("Distance: %s", distance));
                        sortedOrders.add(new Sorted(adapter.getRef(position).getKey(), Float.parseFloat(distance.replace(" km", "")), model.getDeliveryLocation()));

                    } catch (Exception e) {
                        setDistance(currentPosition, deliveryLocation, txtDistance, position, model);
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
