package app.taxipizzastaff.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;

import app.taxipizzastaff.R;
import app.taxipizzastaff.Remote.IGeoCoordinates;
import app.taxipizzastaff.Remote.IGoogleAPI;
import app.taxipizzastaff.Remote.RetrofitClient;
import app.taxipizzastaff.models.Staff;

/**
 * Created by user on 05/03/2018.
 */

public class Config {

    public static final LatLng TaxiPizzaLocation = new LatLng(35.8484589, 10.605323);

    public static final String baseUrl = "https://maps.googleapis.com";

    public static IGoogleAPI getGoogleAPI() {
        return RetrofitClient.getClient(baseUrl).create(IGoogleAPI.class);
    }

    public static void setCurrentUser(Context context, Staff currentStaff) {
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = context.getSharedPreferences("Pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("currentUser", gson.toJson(currentStaff));
        editor.apply();
    }

    public static Staff getCurrentUser(Context context) {
        try {
            Gson gson = new Gson();
            SharedPreferences sharedPreferences = context.getSharedPreferences("Pref", Context.MODE_PRIVATE);
            String json = sharedPreferences.getString("currentUser", "");
            return gson.fromJson(json, Staff.class);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public static void NetworkAlert(final Context context) {
        /*new AwesomeWarningDialog(context)
                .setTitle("No Internet Connection")
                .setMessage("Make sure your device is connected to the internet.")
                .setColoredCircle(R.color.colorPrimary)
                .setDialogIconAndColor(R.drawable.ic_dialog_warning, R.color.white)
                .setCancelable(false)
                .setButtonText("TRY AGAIN")
                .setButtonBackgroundColor(R.color.colorPrimary)
                .setButtonTextColor(android.R.color.white)
                .setWarningButtonClick(new Closure() {
                    @Override
                    public void exec() {
                        Activity current = (Activity)context;
                        context.startActivity(current.getIntent());
                    }
                })
                .show();*/
    }

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager != null) {
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if(info != null) {
                for(int i=0; i< info.length; i++) {
                    if(info[i].getState() == NetworkInfo.State.CONNECTED)
                        return true;
                }
            }
        }
        return false;
    }

    public static IGeoCoordinates getGeoCodeService() {
        return RetrofitClient.getClient(baseUrl).create(IGeoCoordinates.class);
    }

    public static String EpochToDate(long time, String formatString) {
        SimpleDateFormat format = new SimpleDateFormat(formatString);
        return format.format(new Date(time));

    }

    public static Bitmap getDeliveryLocationMarker(Context context) {
        Resources resources = context.getResources();
        int height = (int) resources.getDimension(R.dimen.height_76);
        int width = (int) resources.getDimension(R.dimen.width_76);
        BitmapDrawable bitmapdraw=(BitmapDrawable)context.getResources().getDrawable(R.drawable.delivery_marker);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap marker =  Bitmap.createScaledBitmap(b, width, height, false);
        return marker;
    }

    public static Bitmap getGuyLocationMarker(Context applicationContext) {
        Resources resources = applicationContext.getResources();
        int height = (int) resources.getDimension(R.dimen.height_76);
        int width = (int) resources.getDimension(R.dimen.width_63);
        BitmapDrawable bitmapdraw=(BitmapDrawable)applicationContext.getResources().getDrawable(R.drawable.meal_marker);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap marker =  Bitmap.createScaledBitmap(b, width, height, false);
        return marker;
    }
}
