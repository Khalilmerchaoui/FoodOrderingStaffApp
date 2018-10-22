package app.taxipizzastaff.Helper;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import app.taxipizzastaff.R;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by user on 07/03/2018.
 */

public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {

    View myView;
    Context context;
    RelativeLayout layout;
    LinearLayout allLayout;

    public CustomInfoWindow(Context context) {
        myView = LayoutInflater.from(context).inflate(R.layout.custom_client_information, null);
        this.context = context;
    }

    private String imgUrl = "", phone = "";
    @Override
    public View getInfoWindow(Marker marker) {
        TextView txtName = myView.findViewById(R.id.txtName);
        TextView txtPhone = myView.findViewById(R.id.txtPhone);
        allLayout = myView.findViewById(R.id.allLayout);

        layout = myView.findViewById(R.id.layout);
        CircleImageView imgClient = myView.findViewById(R.id.imgClient);

        try {
            if (marker.getSnippet() != null) {
                imgUrl = marker.getSnippet().split(",")[0];
                phone = marker.getSnippet().split(",")[1];
                layout.setVisibility(View.VISIBLE);
            } else {
                imgUrl = "";
                phone = "";
                layout.setVisibility(View.GONE);
            }
            allLayout.setBackgroundColor(context.getResources().getColor(R.color.colorBackgroundInfo));
            Picasso.with(context)
                    .load(imgUrl)
                    .placeholder(R.drawable.profile_icon)
                    .error(R.drawable.profile_icon)
                    .into(imgClient, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {
                        }
                    });

            txtPhone.setText(phone);
            txtName.setText(marker.getTitle());
        } catch (Exception e) {
            Log.i("tagged", "error");
            layout.setVisibility(View.GONE);
            txtName.setText("");
            txtPhone.setText("");
            allLayout.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }
        return myView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
