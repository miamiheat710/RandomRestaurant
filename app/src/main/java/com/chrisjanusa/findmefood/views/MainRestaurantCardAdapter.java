package com.chrisjanusa.findmefood.views;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chrisjanusa.findmefood.R;
import com.chrisjanusa.findmefood.db.RestaurantDBHelper;
import com.chrisjanusa.findmefood.models.Restaurant;
import com.chrisjanusa.findmefood.utils.SavedListHolder;
import com.squareup.picasso.Picasso;

import java.util.Locale;
import com.chrisjanusa.findmefood.db.DislikeRestaurantDBHelper;
import com.chrisjanusa.findmefood.utils.DislikeListHolder;

/**
 * A RecyclerView Adapter for the main fragment. Only displays one card at a time, rather than a list.
 */
public class MainRestaurantCardAdapter extends RecyclerView.Adapter<MainRestaurantCardAdapter.RestaurantViewHolder>  {

    private Context context;
    private Restaurant restaurant;
    private SavedListHolder savedListHolder;
    private RestaurantDBHelper dbHelper;
    private DislikeListHolder dislikeListHolder;
    private DislikeRestaurantDBHelper dislikedbHelper;

    public MainRestaurantCardAdapter(Context con, Restaurant res) {
        this.context = con;
        this.restaurant = res;
        this.savedListHolder = SavedListHolder.getInstance();
        this.dbHelper = new RestaurantDBHelper(this.context, null);
        this.dislikeListHolder = DislikeListHolder.getInstance();
        this.dislikedbHelper = new DislikeRestaurantDBHelper(this.context, null);
        savedListHolder.setSavedList(dbHelper.getAll());
        dislikeListHolder.setSavedList(dislikedbHelper.getAll());
    }

    public void add(Restaurant res) {
        this.restaurant = res;
        notifyItemInserted(0);
    }

    public void remove() {
        notifyItemRemoved(0);
    }

    /**
     * Helper function to add a restaurant to the savedList.
     *
     * @param res: the restaurant object we want to add.
     */
    private void addToSavedList(Restaurant res) {
        new InsertIntoDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, res);
        savedListHolder.add(res);
        Toast.makeText(context, "Restaurant added to favorites list", Toast.LENGTH_SHORT).show();
    }
    private void addToDislikeList(Restaurant res) {
        new InsertIntoDislikeDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, restaurant);
        dislikeListHolder.add(res);
        Toast.makeText(context, "Restaurant added to blocked list", Toast.LENGTH_SHORT).show();
    }
    public void removeSaved(Restaurant deleteThis) {
        new MainRestaurantCardAdapter.DeleteFromDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, deleteThis);
        savedListHolder.remove(deleteThis);
        Toast.makeText(context, "Restaurant removed from favorites list", Toast.LENGTH_SHORT).show();
    }

    public void removeDislike(Restaurant deleteThis) {
        new MainRestaurantCardAdapter.DeleteFromDislikeDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, deleteThis);
        dislikeListHolder.remove(deleteThis);
        Toast.makeText(context, "Restaurant removed from blocked list", Toast.LENGTH_SHORT).show();
    }

    /**
     * Helper function to load the corresponding rating image for Yelp stars.
     * Since Yelp v3 API (dubbed Fusion), they stopped providing a URL for the rating image, so this
     * is why this function is needed.
     *
     * @param rating: rating to load stars for.
     * @return corresponding drawable to the rating.
     */
    private int loadStars(Double rating) {
        if (rating == 0.0)
            return R.drawable.ic_zero_stars;
        else if (rating == 1.0)
            return R.drawable.ic_one_stars;
        else if (rating == 1.5)
            return R.drawable.ic_onehalf_stars;
        else if (rating == 2.0)
            return R.drawable.ic_two_stars;
        else if (rating == 2.5)
            return R.drawable.ic_twohalf_stars;
        else if (rating == 3.0)
            return R.drawable.ic_three_stars;
        else if (rating == 3.5)
            return R.drawable.ic_threehalf_stars;
        else if (rating == 4.0)
            return R.drawable.ic_four_stars;
        else if (rating == 4.5)
            return R.drawable.ic_fourhalf_stars;
        else if (rating == 5.0)
            return R.drawable.ic_five_stars;
        else
            return R.drawable.ic_zero_stars;
    }

    @Override
    public RestaurantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_main_restaurant_card, parent, false);

        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RestaurantViewHolder holder, int position) {
        Picasso.with(context).load(restaurant.getThumbnailURL()).into(holder.thumbnail);
        holder.ratingImage.setImageResource(loadStars(restaurant.getRating()));
        holder.nameOfRestaurant.setText(restaurant.getName());
        holder.categories.setText(restaurant.getCategories().toString()
                .replace("[", "").replace("]", "").trim());

        if (restaurant.getDeal().length() != 0) {
            holder.deals.setVisibility(View.VISIBLE);
            holder.deals.setText(restaurant.getDeal());
        } else
            holder.deals.setVisibility(View.GONE);

        String priceText = String.format("%s", restaurant.getPrice());
        String reviewsText = String.format(Locale.ENGLISH, "%d reviews", restaurant.getReviewCount());
        String distanceText = String.format(Locale.ENGLISH, "%.2f mi away", restaurant.getDistance());

        // Color coding dollar signs for price and number of reviews.
        Spannable spannable = new SpannableString(String.format(Locale.ENGLISH, "%s | %s | %s",
                priceText, reviewsText, distanceText));

        int startIndex = 0;
        int endIndex = priceText.length();

        // Price
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#14AD5F")),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        startIndex = endIndex + 3;
        endIndex += 3 + String.valueOf(restaurant.getReviewCount()).length();

        // Review count
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#1A6C9D")),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        startIndex = endIndex + 3 +
                (reviewsText.length() - String.valueOf(restaurant.getReviewCount()).length());
        endIndex = startIndex + (distanceText.length() - " mi away".length());

        // Distance (miles away)
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#FF764A")),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.distancePriceReviewCount.setText(spannable);

        // Modify the save button depending on if the restaurant in the savedList or not.
        if (savedListHolder.resIsContained(restaurant)) {
            holder.saveButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.star_filled));
        } else
            holder.saveButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.star_not));
        if (dislikeListHolder.resIsContained(restaurant)) {
            holder.removeButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.block_red));
        } else
            holder.removeButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.block_not));
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class RestaurantViewHolder extends RecyclerView.ViewHolder {

        TextView nameOfRestaurant;
        ImageView ratingImage;
        ImageView thumbnail;
        TextView categories;
        TextView deals;
        TextView distancePriceReviewCount;

        ImageButton saveButton;
        ImageButton removeButton;

        RestaurantViewHolder(View itemView) {
            super(itemView);

            nameOfRestaurant = (TextView) itemView.findViewById(R.id.name);
            ratingImage = (ImageView) itemView.findViewById(R.id.rating);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            categories = (TextView) itemView.findViewById(R.id.categories);
            deals = (TextView) itemView.findViewById(R.id.deals);
            distancePriceReviewCount = (TextView) itemView.findViewById(R.id.distancePriceReviewCount);
            removeButton = (ImageButton) itemView.findViewById(R.id.removeButton);
            saveButton = (ImageButton) itemView.findViewById(R.id.saveButton);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dialog();
                }
            });

            // Adds current restaurant to the saved list on click.
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (savedListHolder.resIsContained(restaurant)) {
                        removeSaved(restaurant);
                        saveButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.star_not));
                        return;
                    }

                    addToSavedList(restaurant);
                    saveButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.star_filled));
                }
            });

            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (dislikeListHolder.resIsContained(restaurant)) {
                        removeDislike(restaurant);
                        removeButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.block_not));
                        return;
                    }

                    addToDislikeList(restaurant);
                    removeButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.block_red));
                }
            });
        }
    }

    public void dialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Open "+ restaurant.getName()+" in ")
                .setItems(R.array.Apis, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 1:
                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:"+restaurant.getLat()+","+restaurant.getLon()+"?z=10&q="+restaurant.getName()+"&q="+restaurant.getAddress())));
                                break;

                            case 2:
                                try {
                                    PackageManager pm = context.getPackageManager();
                                    pm.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES);
                                    String uri = "https://m.uber.com/ul/?action=setPickup&client_id=bUBQ-U07D9vS_RQBPdhfF5PiigfU17et&pickup=my_location&dropoff[formatted_address]="+restaurant.getAddress()+"&dropoff[latitude]="+restaurant.getLat()+"&dropoff[longitude]="+restaurant.getLon()+"\n";
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(uri));
                                    context.startActivity(intent);
                                } catch (PackageManager.NameNotFoundException e) {
                                    // No Uber app! Open mobile website.
                                    String url = "https://m.uber.com/sign-up?client_id=<CLIENT_ID>";
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setData(Uri.parse(url));
                                    context.startActivity(i);
                                }
                                break;
                            case 0:
                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(restaurant.getUrl())));
                                break;

                            default:
                                break;

                        }
                    }
                });
        builder.create();
        builder.show();
    }



    private class InsertIntoDB extends AsyncTask<Restaurant, Void, Void> {

        @Override
        protected Void doInBackground(Restaurant... params) {
            dbHelper.insert(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
    private class InsertIntoDislikeDB extends AsyncTask<Restaurant, Void, Void> {

        @Override
        protected Void doInBackground(Restaurant... params) {
            dislikedbHelper.insert(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class DeleteFromDB extends AsyncTask<Restaurant, Void, Void> {

        @Override
        protected Void doInBackground(Restaurant... params) {
            dbHelper.delete(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class DeleteFromDislikeDB extends AsyncTask<Restaurant, Void, Void> {

        @Override
        protected Void doInBackground(Restaurant... params) {
            dislikedbHelper.delete(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
