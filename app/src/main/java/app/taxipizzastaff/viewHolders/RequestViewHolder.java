package app.taxipizzastaff.viewHolders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import app.taxipizzastaff.R;

/**
 * Created by user on 05/03/2018.
 */

public class RequestViewHolder extends RecyclerView.ViewHolder {

    public TextView txtId, txtDistance, txtTotal, txtTime;

    public RequestViewHolder(View itemView) {
        super(itemView);
        txtId = itemView.findViewById(R.id.txtId);
        txtDistance = itemView.findViewById(R.id.txtDistance);
        txtTotal = itemView.findViewById(R.id.txtTotal);
        txtTime = itemView.findViewById(R.id.txtTime);
    }
}
