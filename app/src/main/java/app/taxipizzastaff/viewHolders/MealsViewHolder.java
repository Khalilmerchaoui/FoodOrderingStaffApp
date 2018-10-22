package app.taxipizzastaff.viewHolders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import app.taxipizzastaff.R;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by user on 08/03/2018.
 */

public class MealsViewHolder extends RecyclerView.ViewHolder{

    public TextView txtMealName;
    public ImageView imgCount;
    public CircleImageView imgMeal;

    public MealsViewHolder(View itemView) {
        super(itemView);

        txtMealName = itemView.findViewById(R.id.txtMealName);
        imgMeal = itemView.findViewById(R.id.imgMeal);
        imgCount = itemView.findViewById(R.id.imgCount);
    }
}
