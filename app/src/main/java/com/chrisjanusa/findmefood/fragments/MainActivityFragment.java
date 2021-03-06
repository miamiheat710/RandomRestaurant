package com.chrisjanusa.findmefood.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.chrisjanusa.findmefood.BuildConfig;
import com.chrisjanusa.findmefood.MainActivity;
import com.chrisjanusa.findmefood.Manifest;
import com.chrisjanusa.findmefood.R;
import com.chrisjanusa.findmefood.db.HistoryDBHelper;
import com.chrisjanusa.findmefood.db.RestaurantDBHelper;
import com.chrisjanusa.findmefood.managers.UnscrollableLinearLayoutManager;
import com.chrisjanusa.findmefood.models.Restaurant;
import com.chrisjanusa.findmefood.utils.LocationProviderHelper;
import com.chrisjanusa.findmefood.utils.SavedListHolder;
import com.chrisjanusa.findmefood.utils.TypeOfError;
import com.chrisjanusa.findmefood.utils.YelpThread;
import com.chrisjanusa.findmefood.views.MainRestaurantCardAdapter;
import com.chrisjanusa.findmefood.db.DislikeRestaurantDBHelper;
import com.chrisjanusa.findmefood.utils.DislikeListHolder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;

import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import fr.castorflex.android.circularprogressbar.CircularProgressDrawable;
import pub.devrel.easypermissions.EasyPermissions;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;
import uk.co.deanwild.materialshowcaseview.shape.RectangleShape;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * A fragment containing the main activity.
 * Responsible for displaying to the user a random restaurant based off their location  / zip code.
 */

public class MainActivityFragment extends Fragment implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, EasyPermissions.PermissionCallbacks,
        TimePickerFragment.TimePickerCallbacks {

    static ArrayList<Restaurant> restaurants = new ArrayList<>();
    static AsyncTask initialYelpQuery;
    boolean restartQuery = true;
    boolean taskRunning = false;
    Button generate;
    static CheckBox priceFour;
    static CheckBox priceOne;
    static CheckBox priceThree;
    static CheckBox priceTwo;
    CountDownLatch latch = new CountDownLatch(0);
    CountDownLatch errorLatch = new CountDownLatch(0);


    CircularProgressBar progressBar;
    EditText filterBox;
    FloatingSearchView searchLocationBox;
    GoogleApiClient mGoogleApiClient;
    GoogleMap map;
    static int errorInQuery;
    int generateBtnColor;
    ScrollView filtersLayout;
    LinearLayout mapCardContainer;
    LinearLayout priceFilterLayout;
    static long openAtTimeFilter = 0;
    MainRestaurantCardAdapter mainRestaurantCardAdapter;
    MapView mapView;
    RecyclerView restaurantView;
    RelativeLayout rootLayout;
    Restaurant currentRestaurant;
    static String accessToken;
    String filterQuery = "";
    String searchQuery = "";
    ToggleButton pickTime;
    RadioGroup radioGroup;
    boolean favBool;
    boolean located;
    HistoryDBHelper historyDBHelper;
    EditText miles;
    static int maxDistance;
    RatingBar rating;
    static double ratingNum;
    boolean showError;
    static ArrayList<YelpThread> threads = new ArrayList<>();
     private FusedLocationProviderClient mFusedLocationClient;
     Location location;
    static Boolean useGPS=true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this.getActivity());
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        showError=false;

        rootLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_main, container, false);
        filtersLayout = (ScrollView) rootLayout.findViewById(R.id.filtersScrollLayout);
        rating = (RatingBar) filtersLayout.findViewById(R.id.rating);
        priceFilterLayout = (LinearLayout) filtersLayout.findViewById(R.id.priceFilterLayout);
        miles = (EditText) filtersLayout.findViewById(R.id.milesBox);
        restaurantView = (RecyclerView) rootLayout.findViewById(R.id.restaurantView);
        restaurantView.setLayoutManager(new UnscrollableLinearLayoutManager(getContext()));
        historyDBHelper = new HistoryDBHelper(getContext(), null);

        located = false;

        mapCardContainer = (LinearLayout) rootLayout.findViewById(R.id.cardMapLayout);
        mapView = (MapView) rootLayout.findViewById(R.id.mapView);

        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null;
        mapView.onCreate(mapViewSavedInstanceState);

        filterBox = (EditText) rootLayout.findViewById(R.id.filterBox);
        priceOne = (CheckBox) rootLayout.findViewById(R.id.priceOne);
        priceTwo = (CheckBox) rootLayout.findViewById(R.id.priceTwo);
        priceThree = (CheckBox) rootLayout.findViewById(R.id.priceThree);
        priceFour = (CheckBox) rootLayout.findViewById(R.id.priceFour);
        pickTime = (ToggleButton) rootLayout.findViewById(R.id.pickTime);
        generate = (Button) rootLayout.findViewById(R.id.generate);

        radioGroup = (RadioGroup) rootLayout.findViewById(R.id.radioGroup);
        generateBtnColor = ContextCompat.getColor(getContext(), R.color.colorPrimary);

        // Yelp API access token
        accessToken = BuildConfig.API_ACCESS_TOKEN;

        progressBar = (CircularProgressBar) rootLayout.findViewById(R.id.circularProgressBarMainFragment);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                // If user has swiped left, perform a click on the Generate button.
                if (direction == 4)
                    generate.performClick();

                // If user has swiped right, open Yelp to current restaurant's page.
                if (direction == 8) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentRestaurant.getUrl())));

                    // We don't want to remove the restaurant here, so we add it back to the restaurantView.
                    mainRestaurantCardAdapter.remove();
                    mainRestaurantCardAdapter.add(currentRestaurant);

                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(restaurantView);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                favBool = checkedId == R.id.radioFav;
            }
        });

        return rootLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .enableAutoManage(getActivity(), this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Must be defined in onActivityCreated() because searchLocationBox is part of MainActivity.
        searchLocationBox = (FloatingSearchView) getActivity().findViewById(R.id.searchBox);

        // Create LocationProviderHelper instance.

        // Get Google Map using OnMapReadyCallback
        mapView.getMapAsync(this);

        if (savedInstanceState != null) {
            currentRestaurant = savedInstanceState.getParcelable("currentRestaurant");
            searchLocationBox.setSearchText(savedInstanceState.getString("locationQuery"));
            filterBox.setText(savedInstanceState.getString("filterQuery"));
            restaurants = savedInstanceState.getParcelableArrayList("restaurants");
            rating.setRating(savedInstanceState.getFloat("rating"));
            miles.setText(savedInstanceState.getString("maxDistance"));
        }
        else{
            searchLocationBox.setSearchText("Current Location");
        }

        // Define actions on menu button clicks inside searchLocationBox.
        searchLocationBox.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {
                int id = item.getItemId();
                restartQuery = true;

                if (id == R.id.search_box_gps) {
                    useGPS=true;
                    searchLocationBox.setSearchText("Current Location");
                    requestLocation();
                }
                else if (id == R.id.search_box_filter) {
                    if (filtersLayout.getVisibility() == View.GONE) {
                        showFilterElements();

                        // Show tutorial about entering multiple filters.
                        //displayShowcaseViewFilterBox();
                    } else if (filtersLayout.getVisibility() == View.VISIBLE) {
                        showNormalLayout();
                    }
                }
            }
        });

        searchLocationBox.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {}

            @Override
            public void onSearchAction(String currentQuery) {
                restartQuery = true;
                searchLocationBox.setSearchText(currentQuery);
                searchLocationBox.setSearchBarTitle(currentQuery);
            }
        });

        // Listener for when the user clicks done on keyboard after their input.
        filterBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideSoftKeyboard();
                    return true;
                }
                return false;
            }
        });

        pickTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    showTimePickerDialog();
                else
                    openAtTimeFilter = 0;
            }
        });

        // When the user clicks the Generate button.
        generate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                SavedListHolder savedListHolder;
                RestaurantDBHelper saveddbHelper = new RestaurantDBHelper(getContext(), null);
                savedListHolder = SavedListHolder.getInstance();
                savedListHolder.setSavedList(saveddbHelper.getAll());
                if(favBool && !savedListHolder.isEmpty()){
                    Restaurant current = savedListHolder.getRandom();
                    if (mainRestaurantCardAdapter == null) {
                        mainRestaurantCardAdapter = new MainRestaurantCardAdapter(getContext(), current);
                        restaurantView.setAdapter(mainRestaurantCardAdapter);
                    }

                    // These calls notify the RecyclerView that the data set has changed and we need to refresh.
                    mainRestaurantCardAdapter.remove();
                    mainRestaurantCardAdapter.add(current);

                    showNormalLayout();
                    updateMapWithRestaurant(current);
                    enableGenerateButton();
                    return;
                }
                /**
                 * If the user doesn't wait on the task to complete, warn them it is still running
                 * so we can prevent a long stack of requests from piling up.
                 */
                if (taskRunning) {
                    return;
                }

                /**
                 * Initialize searchQuery and filterQuery if they're empty.
                 * Else set restartQuery to true if the queries have changed.
                 */
                if(!located) {
                    requestLocation();
                    located = true;
                }
                if (searchQuery.isEmpty() && filterQuery.isEmpty()) {
                    searchQuery = searchLocationBox.getQuery();
                    filterQuery = filterBox.getText().toString();
                }
                else if (searchQuery.compareTo(searchLocationBox.getQuery()) != 0 ||
                    filterQuery.compareTo(filterBox.getText().toString()) != 0) {

                    searchQuery = searchLocationBox.getQuery();
                    filterQuery = filterBox.getText().toString();

                    // We want to use GPS if searchQuery contains the string "Current Location".
                    useGPS = searchQuery.contains(getActivity().getString(R.string.string_current_location));
                }

                // Replace all spaces from filters for Yelp query.
                String filterBoxText = filterBox.getText().toString().replaceAll(" ", "");


                if (useGPS) {
                    /**
                     * Check to make sure the location is not null before starting.
                     * Else, begin the AsyncTask.
                     */
                    double prevRating = ratingNum;
                    ratingNum = rating.getRating();
                    if(ratingNum!=prevRating){
                        restartQuery = true;
                    }
                    int prevDist = maxDistance;
                    String mileString = miles.getText().toString();
                    if(mileString.equals("")){
                        maxDistance = 10;
                    }
                    else {
                        maxDistance = Integer.parseInt(mileString);
                    }
                    if(maxDistance!=prevDist){
                        restartQuery=true;
                    }


                    if (location == null) {
                        if(showError) {
                            Snackbar.make(getView(),"Location Error: Check Location Settings", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        initialYelpQuery = new RunYelpQuery(
                                    filterBoxText,
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude()))
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } else {
                    /**
                     * Verify that the user has actually entered a location.
                     * Else, begin the AsyncTask.
                     */
                    if (searchLocationBox.getQuery().length() == 0 && restaurants.size() == 0) {
                        displayAlertDialog(R.string.string_enter_valid_location, "Error");
                    } else {
                            initialYelpQuery = new RunYelpQuery(
                                    String.valueOf(searchLocationBox.getQuery()),
                                    filterBoxText)
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            }
        });

        if (savedInstanceState != null) {
            searchLocationBox.setSearchText(savedInstanceState.getString("locationQuery"));
            filterBox.setText(savedInstanceState.getString("filterQuery"));
            currentRestaurant = savedInstanceState.getParcelable("currentRestaurant");
            restaurants = savedInstanceState.getParcelableArrayList("restaurants");
            rating.setRating(savedInstanceState.getFloat("rating"));
            miles.setText(savedInstanceState.getString("maxDistance"));
        }

        // Reset all cache for showcase id.
        //MaterialShowcaseView.resetAll(getContext());
        String[] perms = {"android.permission.ACCESS_COARSE_LOCATION"};
        if(EasyPermissions.hasPermissions(getContext(), perms))
            generate.performClick();
        showError=true;


        // A tutorial that displays only once explaining the input to the app.
        /*MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), BuildConfig.VERSION_NAME + "MAIN");
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(250);
        sequence.setConfig(config);

        sequence.addSequenceItem(buildShowcaseView(searchLocationBox, new RectangleShape(0, 0),
                "Enter any zip code, city, or address here.\n\n" +
                        "Click the GPS icon to use your current location.\n\n" +
                        "Tap the filter icon to see the filter options."
        ));

        sequence.start();*/
    }

    private void requestLocation() {
        if(EasyPermissions.hasPermissions(getActivity(),ACCESS_COARSE_LOCATION)) {
            try {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location newLocation) {
                                // Got last known location. In some rare situations this can be null.
                                if (newLocation != null) {
                                    location = newLocation;
                                    Log.d("location",location.toString());
                                } else {
                                    Snackbar.make(getView(),"Location Error: Check Location Settings", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } catch (SecurityException e) {
                Snackbar.make(getView(),"Location Error: Check Location Settings", Toast.LENGTH_SHORT).show();
            }
        } else {
            EasyPermissions.requestPermissions(getActivity(),"Location permission are required to use GPS",120,ACCESS_COARSE_LOCATION);
            if(EasyPermissions.hasPermissions(getActivity(),ACCESS_COARSE_LOCATION)) {
                requestLocation();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        final Bundle mapViewSaveState = new Bundle(outState);
        mapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle("mapViewSaveState", mapViewSaveState);
        outState.putString("locationQuery", searchLocationBox.getQuery());
        outState.putString("filterQuery", String.valueOf(filterBox.getText()));
        outState.putParcelable("currentRestaurant", currentRestaurant);
        outState.putParcelableArrayList("restaurants", restaurants);
        outState.putFloat("rating", rating.getRating());
        outState.putString("maxDistance", miles.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        // Try to cancel the AsyncTask.
        if (initialYelpQuery != null && initialYelpQuery.getStatus() == AsyncTask.Status.RUNNING) {
            initialYelpQuery.cancel(true);
            enableGenerateButton();

            if (mainRestaurantCardAdapter != null) {
                mainRestaurantCardAdapter.remove();
                mapCardContainer.setVisibility(View.GONE);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        // Refresh the RecyclerView on resume.
        if (mainRestaurantCardAdapter != null)
            mainRestaurantCardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        // Try to cancel the AsyncTask.
        if (initialYelpQuery != null && initialYelpQuery.getStatus() == AsyncTask.Status.RUNNING)
            initialYelpQuery.cancel(true);
    }

    // Google Maps API callback for MapFragment.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
    }

    // Callback for requesting permissions.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    // Callback for checking location settings.


    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getContext(), "Error " + connectionResult.getErrorCode() +
                        ": failed to connect to Google Play Services. This may affect acquiring your location.",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Function to hide the keyboard.
     */
    private void hideSoftKeyboard() {
        Activity activity = getActivity();
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Helper function to build a custom ShowcaseView for a sequence.
     *
     * @param target:      the target view that will be highlighted.
     * @param shape:       the type of shape.
     * @param contentText: the text to be displayed.
     */
    /*private MaterialShowcaseView buildShowcaseView(View target, uk.co.deanwild.materialshowcaseview.shape.Shape shape, String contentText) {
        return new MaterialShowcaseView.Builder(getActivity())
                .setTarget(target)
                .setShape(shape)
                .setMaskColour(ContextCompat.getColor(getContext(), R.color.colorAccent))
                .setContentText(contentText)
                .setDismissText("GOT IT")
                .build();
    }*/

    /**
     * Function to disable the generate button.
     */
    private void disableGenerateButton() {
        // Signal the task is running
        taskRunning = true;
        generate.setText(R.string.string_button_text_loading);
        generate.setBackgroundColor(Color.GRAY);
        generate.setEnabled(false);
    }

    /**
     * Function to enable the generate button.
     */
    private void enableGenerateButton() {
        // Signal the task is finished
        taskRunning = false;
        generate.setText("Pick Restaurant");
        generate.setBackgroundColor(generateBtnColor);
        generate.setEnabled(true);

        progressBar.setVisibility(View.GONE);
        progressBar.progressiveStop();
    }

    /**
     * Function to display the MaterialShowcaseView for filterBox.
     */
    /*private void displayShowcaseViewFilterBox() {
        MaterialShowcaseSequence filterShowcase = new MaterialShowcaseSequence(getActivity(), BuildConfig.VERSION_NAME + "FILTER");
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(250);
        filterShowcase.setConfig(config);

        filterShowcase.addSequenceItem(buildShowcaseView(filterBox, new RectangleShape(0, 0),
                "In the mood for multiple things? List your filters separated by a comma to combine the results!"
        ));

        filterShowcase.start();
    }*/

    private void showFilterElements() {
        filtersLayout.setVisibility(View.VISIBLE);
        mapCardContainer.setVisibility(View.GONE);
        restaurantView.setVisibility(View.GONE);
    }

    private void showNormalLayout() {
        filtersLayout.setVisibility(View.GONE);
        restaurantView.setVisibility(View.VISIBLE);
        mapCardContainer.setVisibility(View.VISIBLE);
    }

    private void hideNormalLayout() {
        mapCardContainer.setVisibility(View.GONE);
        restaurantView.setVisibility(View.GONE);
    }

    /**
     * Helper function to display an AlertDialog
     * @param stringToDisplay:  the string to display in the alert.
     * @param title:            the title of the dialog.
     */
    private void displayAlertDialog(int stringToDisplay, String title) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.setTitle(title);
        alert.setMessage(getActivity().getString(stringToDisplay));
        alert.show();
    }

    /**
     * Function to query Yelp for restaurants. Returns an ArrayList of Restaurants.
     *
     * @param lat:              the user's latitude, null if @param input is not empty.
     * @param lon:              the user's longitude, null if @param input is not empty.
     * @param input             the user's location string, e.g. zip, city, etc.
     * @param filter            the user's filterBox string, e.g. sushi, bbq, etc.
     * @param offset            the offset for the Yelp query.
     * @param whichAsyncTask    the AsyncTask to cancel if necessary.
     * @return true if successful querying Yelp; false otherwise.
     */
    public static boolean queryYelp(String lat, String lon, String input,
                              String filter, int offset, int whichAsyncTask, YelpThread thread, DislikeListHolder dislikeListHolder) {

        // Build Yelp request.
        try {
            URL url;
            HttpURLConnection urlConnection;
            String requestUrl = "https://api.yelp.com/v3/businesses/search";
            StringBuilder builder = new StringBuilder(requestUrl);

            builder.append("?term=").append("restaurants");
            builder.append("&limit=" + 50);
            builder.append("&offset=").append(offset);

            // Check if user wants to filter by categories.
            if (filter.length() != 0)
                builder.append("&categories=").append(filter.toLowerCase());

            // Check if the user wants to filter by price range.
            StringBuilder priceBuilder = new StringBuilder("");
            ArrayList<String> priceRange = new ArrayList<>();

            if (priceOne.isChecked() || priceTwo.isChecked() ||
                    priceThree.isChecked() || priceFour.isChecked())
                builder.append("&price=");

            if (priceOne.isChecked()) priceRange.add("1");
            if (priceTwo.isChecked()) priceRange.add("2");
            if (priceThree.isChecked()) priceRange.add("3");
            if (priceFour.isChecked()) priceRange.add("4");

            // Making sure to prevent trailing commas in price range list.
            for (String elem : priceRange) {
                priceBuilder.append(elem);
                if (priceRange.indexOf(elem) != (priceRange.size() - 1))
                    priceBuilder.append(",");
            }
            builder.append(priceBuilder.toString());

            if (openAtTimeFilter != 0)
                builder.append("&open_at=").append(openAtTimeFilter);
            else{
                builder.append("&open_now=").append("true");
            }
            if (useGPS) {
                builder.append("&latitude=").append(lat);
                builder.append("&longitude=").append(lon);
            } else {
                builder.append("&location=").append(input);
            }

            requestUrl = builder.toString();
            Log.d("Running Yelp", "URL: " + requestUrl);

            url = new URL(requestUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("Authorization", String.format("Bearer %s", accessToken));
            urlConnection.setConnectTimeout(10 * 1000);
            urlConnection.setReadTimeout(10 * 1000);

            // Make connection and read the response.
            urlConnection.connect();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String buf, jsonString;
            while ((buf = br.readLine()) != null)
                sb.append(buf);
            br.close();
            jsonString = sb.toString();

            JSONObject response = new JSONObject(jsonString);

            // Get JSON array that holds the listings from Yelp.
            JSONArray jsonBusinessesArray = response.getJSONArray("businesses");
            int length = jsonBusinessesArray.length();
            Log.d("Running Yelp", "Yelp Array length for offset " + offset + " is " + length);

            // This occurs if a network communication error occurs or if no restaurants were found.
            if (length <= 0) {
                errorInQuery = TypeOfError.NO_RESTAURANTS;
                Log.d("Running Yelp", "Latch decremented: Communication error for offset " + offset);
                threads.remove(thread);
                return false;
            }

            for (int i = 0; i < length; i++) {
                if (whichAsyncTask == 0 && initialYelpQuery.isCancelled())
                    break;

                Restaurant res = convertJSONToRestaurant(jsonBusinessesArray.getJSONObject(i));
                Log.d("Running Yelp", "Checking " + res.getName());
                if (res != null && isValidRestaurant(res, dislikeListHolder))
                    restaurants.add(res);
            }

            if (restaurants.isEmpty()) {
                errorInQuery = TypeOfError.NO_RESTAURANTS;
                Log.d("Running Yelp", "Latch decremented: No Valid Restaurants");
                threads.remove(thread);
                return false;
            }

        } catch (JSONException e) {
            if (e.getMessage().contains("No value for businesses"))
                errorInQuery = TypeOfError.NO_RESTAURANTS;
            else if (e.getMessage().contains("No value for"))
                errorInQuery = TypeOfError.MISSING_INFO;
            e.printStackTrace();
            Log.d("Running Yelp", "Latch decremented: Error in query");
            threads.remove(thread);
            return false;
        } catch (FileNotFoundException e) {
            errorInQuery = TypeOfError.INVALID_LOCATION;
            e.printStackTrace();
            Log.d("Running Yelp", "Latch decremented: invalid location");
            threads.remove(thread);
            return false;
        } catch (Exception e) {
            if (e.getMessage().contains("timed out")) errorInQuery = TypeOfError.TIMED_OUT;
            e.printStackTrace();
            Log.d("Running Yelp", "Latch decremented: timeout");
            threads.remove(thread);
            return false;
        }

        errorInQuery = TypeOfError.NO_ERROR;
        Log.d("Running Yelp", "Latch decremented: all good - #restaurants " +restaurants.size());
        threads.remove(thread);
        return true;
    }

    /**
     * Convert JSON to a Restaurant object that encapsulates a restaurant from Yelp.
     *
     * @param obj: JSONObject that holds all restaurant info.
     * @return Restaurant or null if an error occurs.
     */
    public static Restaurant convertJSONToRestaurant(JSONObject obj) {
        try {
            // Getting the JSON array of categories
            JSONArray categoriesJSON = obj.getJSONArray("categories");
            ArrayList<String> categories = new ArrayList<>();

            for (int i = 0; i < categoriesJSON.length(); i++)
                categories.add(categoriesJSON.getJSONObject(i).getString("title"));

            // Getting the restaurant's coordinates and price
            double lat = obj.getJSONObject("coordinates").getDouble("latitude");
            double lon = obj.getJSONObject("coordinates").getDouble("longitude");
            double distance = obj.getDouble("distance") * 0.000621371; // Convert to miles

            // Getting restaurant's address
            JSONObject locationJSON = obj.getJSONObject("location");
            JSONArray addressJSON = locationJSON.getJSONArray("display_address");
            ArrayList<String> address = new ArrayList<>();

            for (int i = 0; i < addressJSON.length(); i++)
                address.add(addressJSON.getString(i));

            // Get deals if JSON contains deals object.
            String deals;
            try {
                JSONArray dealsArray = obj.getJSONArray("deals");
                ArrayList<String> dealsList = new ArrayList<>();

                for (int i = 0; i < dealsArray.length(); i++) {
                    JSONObject jsonObject = dealsArray.getJSONObject(i);
                    dealsList.add(jsonObject.getString("title"));
                }
                deals = dealsList.toString().replace("[", "").replace("]", "").trim();
            } catch (Exception ignored) {
                deals = "";
            }

            // If restaurant doesn't have a price, put a question mark.
            String price;
            try {
                price = obj.getString("price");
            } catch (Exception ignored) {
                price = "?";
            }

            // If listing does not have an image, make sure it routes to localhost because
            // Picasso will complain when a URL string is empty.
            String imageUrl = obj.getString("image_url");
            if (imageUrl.length() == 0)
                imageUrl = "localhost";

            // Construct a new Restaurant object with all the info we gathered above and return it
            Restaurant res = new Restaurant(obj.getString("name"), (float) obj.getDouble("rating"),
                    imageUrl, obj.getInt("review_count"), obj.getString("url"),
                    categories, address, deals, price, distance, lat, lon);
            /*dislikedbHelper.
            dislikeListHolder.setSavedList(dislikedbHelper.getAll());
            if(dislikeListHolder.resIsContained(res)){
                generate.performClick();
            }*/
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update the map with a new marker based on restaurant's coordinates.
     *
     * @param restaurant the restaurant that will be on the map.
     */
    private void updateMapWithRestaurant(Restaurant restaurant) {
        // Clear all the markers on the map.
        map.clear();

        LatLng latLng = new LatLng(restaurant.getLat(), restaurant.getLon());

        map.addMarker(new MarkerOptions().position(latLng).title(String.format("%s: %s",
                restaurant.getName(), restaurant.getAddress())
                .replace("[", "").replace("]", "").trim()));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));

        mapCardContainer.setVisibility(View.VISIBLE);
    }

    /**
     * This function launches the TimePicker dialog from the @Fragment TimePickerFragment.
     * Set the callback listener to this class so we can receive the result in
     *
     * @func timePickerDataCallback()
     */
    public void showTimePickerDialog() {
        TimePickerFragment tpf = new TimePickerFragment();
        tpf.setListener(this);

        //DialogFragment newFragment = new TimePickerFragment();
        tpf.show(getActivity().getSupportFragmentManager(), "timePicker");
    }

    @Override
    public void timePickerDataCallback(long data) {

        // If we received a 0, then the user cancel out of the dialog.
        if (data == 0) {
            pickTime.setChecked(false);
            return;
        }

        openAtTimeFilter = data;

        Date date = new Date(data * 1000L);
        /*
         * Suppressing warning because we handle the timezone issue below
         * by setting the default timezone manually.
         */
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        sdf.setTimeZone(TimeZone.getDefault());
        String formattedDate = sdf.format(date);

        // https://stackoverflow.com/a/3792554/2193236
        pickTime.setTextOn(formattedDate);
        pickTime.setChecked(pickTime.isChecked());
    }

    // Async task that connects to Yelp's API and queries for restaurants based on location / zip code.
    public class RunYelpQuery extends AsyncTask<Void, Void, Restaurant> {
        String[] params;
        String userInputStr;
        String userFilterStr;
        boolean successfulQuery;

        RunYelpQuery(String... params) {
            this.params = params;

            if (params.length == 2) {
                userInputStr = params[0];
                userFilterStr = params[1];
            } else if (params.length == 3) {
                userInputStr = "";
                userFilterStr = params[0];
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            disableGenerateButton();

            if (restartQuery) {
                Log.d("Running Yelp", "PreExecute reached");
                ((CircularProgressDrawable) progressBar.getIndeterminateDrawable()).start();
                hideNormalLayout();
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            restartQuery = true;
            enableGenerateButton();
        }

        @Override
        protected Restaurant doInBackground(Void... aVoid) {
            Log.d("Running Yelp", "Running Yelp");
            if (isCancelled()) return null;

            // Check for parameters so we can send the appropriate request based on user input.
            String lat = "";
            String lon = "";

            try {
                // If the user entered some input, make sure to encode all spaces and "+" for URL query.
                if (userInputStr.length() != 0) {
                    userInputStr = userInputStr.replaceAll(" ", "+");
                }

                // If we have 3 parameters, then the user selected location and we must grab the lat / long.
                if (params.length == 3) {
                    lat = params[1];
                    lon = params[2];
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (restartQuery) {
                emptyList();
                restartQuery = false;
            }



            DislikeListHolder dislikeListHolder;
            DislikeRestaurantDBHelper dbHelper = new DislikeRestaurantDBHelper(getContext(), null);
            dislikeListHolder = DislikeListHolder.getInstance();
            dislikeListHolder.setSavedList(dbHelper.getAll());
            // Get restaurants only when the restaurants list is empty.
            Restaurant chosenRestaurant = null;
            if (restaurants == null || restaurants.isEmpty()) {
                Log.d("Running Yelp", "Refreshing List");
                refreshList(lat, lon, userInputStr, userFilterStr, 500, dislikeListHolder);
            }
            if (restaurants != null && !restaurants.isEmpty()) {
                Log.d("Running Yelp", "Choosing restaurant: " + restaurants.isEmpty());
                // Make sure the restaurants list is not empty before accessing it.
                chosenRestaurant = restaurants.get(new Random().nextInt(restaurants.size()));
                restaurants.remove(chosenRestaurant);
            }
            else{
                Log.d("Running Yelp", "No Restaurant Found");
                errorInQuery = TypeOfError.NO_RESTAURANTS;
            }
            return chosenRestaurant;
        }

        // Set UI appropriate UI elements to display mRestaurant info.
        @Override
        protected void onPostExecute(Restaurant restaurant) {

            if (restaurant == null) {
                switch (errorInQuery) {
                    case TypeOfError.NO_RESTAURANTS: {
                        Log.d("Error", "No restaurants");
                        Toast.makeText(getContext(),
                                R.string.string_no_restaurants_found,
                                Toast.LENGTH_LONG).show();
                        restaurants.clear();
                        break;
                    }

                    case TypeOfError.MISSING_INFO: {
                        // Try again if the current restaurant has missing info.
                        generate.performClick();
                        return;
                    }

                    case TypeOfError.NETWORK_CONNECTION_ERROR: {
                        Toast.makeText(getContext(),
                                R.string.string_no_network,
                                Toast.LENGTH_LONG).show();
                        break;
                    }

                    case TypeOfError.TIMED_OUT: {
                        Toast.makeText(getContext(),
                                R.string.string_timed_out_msg,
                                Toast.LENGTH_LONG).show();
                        break;
                    }

                    case TypeOfError.INVALID_LOCATION: {
                        Toast.makeText(getContext(),
                                R.string.string_no_restaurants_found,
                                Toast.LENGTH_LONG).show();
                        break;
                    }

                    default:
                        break;
                }

                if (initialYelpQuery!= null) initialYelpQuery.cancel(true);

                enableGenerateButton();
                return;
            }

            currentRestaurant = restaurant;

            // If the RecyclerView has not been set yet, set it with the currentRestaurant.
            if (mainRestaurantCardAdapter == null) {
                mainRestaurantCardAdapter = new MainRestaurantCardAdapter(getContext(), currentRestaurant);
                restaurantView.setAdapter(mainRestaurantCardAdapter);
            }

            // These calls notify the RecyclerView that the data set has changed and we need to refresh.
            mainRestaurantCardAdapter.remove();
            mainRestaurantCardAdapter.add(currentRestaurant);
            historyDBHelper.insert(currentRestaurant);

            showNormalLayout();
            updateMapWithRestaurant(currentRestaurant);
            enableGenerateButton();

            // A tutorial that displays only once explaining the action that can be done on the restaurant card.
            /*MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), BuildConfig.VERSION_NAME + "CARD");
            ShowcaseConfig config = new ShowcaseConfig();
            config.setDelay(100);
            sequence.setConfig(config);

            sequence.addSequenceItem(buildShowcaseView(restaurantView, new RectangleShape(0, 0),
                    "Swipe left to dismiss. Swipe right to open in Yelp. Tap star to save it favorites list. Tap block button to save it to blocked list and never see it again."));

            sequence.start();*/
        }
    }

    private static boolean isValidRestaurant(Restaurant chosenRestaurant, DislikeListHolder dislikeListHolder){
        boolean tooFar = chosenRestaurant.getDistance() > maxDistance;
        if(tooFar){
            Log.d("Checking Yelp", chosenRestaurant.getDistance() + " is farther than max distance of " + maxDistance);
        }
        boolean blocked = dislikeListHolder.resIsContained(chosenRestaurant);
        if(blocked){
            Log.d("Checking Yelp", chosenRestaurant + " is on the blocked list");
        }
        boolean tooBad = chosenRestaurant.getRating() < ratingNum;
        if(tooBad){
            Log.d("Checking Yelp", chosenRestaurant.getRating() + " is less than min rating of " + ratingNum);
        }
        return !(tooBad||tooFar||blocked);
    }

    private void refreshList(String lat, String lon, String userInputStr, String userFilterStr, int maxIndex, DislikeListHolder dislikeListHolder){
        latch = new CountDownLatch(1);
        errorLatch = new CountDownLatch(10);
        Log.d("Running Yelp", "Countdown latch set");
        if (restaurants == null || restaurants.size()<=10) {
            if(!threads.isEmpty() && (restaurants.isEmpty() || restaurants == null)){
                try {
                    latch.await();
                    Log.d("Running Yelp", "Thread finished");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            latch = new CountDownLatch(1);
            Log.d("Running Yelp", "Countdown Latch 2 Set");

            if (restaurants == null || restaurants.size()<=10) {
                Log.d("Running Yelp", "Still no restaurants so starting all threads");
                for (int i = 0; i < maxIndex; i += 50) {
                    threads.add(new YelpThread(lat, lon, userInputStr, userFilterStr, 0, i, latch, dislikeListHolder, errorLatch));
                    threads.get(threads.size()-1).start();
                }
                if((restaurants.isEmpty() || restaurants == null)){
                    try {
                        Log.d("Running Yelp", "Awaiting new threads");
                        latch.await();
                        for(int i = 10; i>=0; i--) {
                            errorLatch.countDown();
                        }
                        Log.d("Running Yelp", "Thread finished");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void refreshListBackground(String lat, String lon, String userInputStr, String userFilterStr, int maxIndex, DislikeListHolder dislikeListHolder) {
        if (restaurants == null || restaurants.size() <= 10) {
            for (int i = 0; i < maxIndex; i += 50) {
                threads.add(new YelpThread(lat, lon, userInputStr, userFilterStr, 0, i, latch, dislikeListHolder, errorLatch));
                threads.get(threads.size() - 1).start();
            }
        }
    }

    private void emptyList(){
        while(!threads.isEmpty()){
            try {
                threads.get(0).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        restaurants.clear();
    }
}