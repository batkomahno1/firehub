package com.example.kyrylo.firehub;
//delivery version 1

//standard Android classes
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

//some Java classes
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

//Mapbox classes
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geocoder.MapboxGeocoder;
import com.mapbox.geocoder.service.models.GeocoderResponse;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

// classes needed to add the location component
import com.mapbox.geojson.Point;
import android.widget.Toast;
import android.support.annotation.NonNull;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.android.core.permissions.PermissionsListener;

// classes needed to add a marker
import com.mapbox.mapboxsdk.annotations.Marker;

// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

// classes needed to launch navigation UI
import android.view.View;

public class LocationListActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener{
    //this activity maps the call location and water reservoirs
    //https://www.mapbox.com/help/android-navigation-sdk/
    //Todo: add a textbox with reverse geocoded dest address for confirmation
    //ToDo: add travel distance and time
    //ToDo: initial map zoom
    //ToDo: update location periodically

    //sample points
    private static final double[][] SAMPLE_POINTS =
            {{-79.935747, 43.267368},{-79.833732, 43.266537},{-79.766748, 43.24817}};

    //message to pass data with intent
    public static final String ROUTE_MESSAGE = R.string.app_web_path+".ROUTE_MESSAGE ";

    //map vars
    private MapView mapView;
    private String location;
    private double lat,lon;
    private MapboxMap mapboxMap;
    private PermissionsManager permitMgr;
    private Location originLoc;

    // variables for calculating and drawing a route
    private Point originPoint;
    private Point destPoint;
    private Point reservoirPoint;
    private DirectionsRoute route;
    private static final String TAG = "LocationListActivity";
    private NavigationMapRoute navMapRoute;
//    private NavigationView navigation;


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_NoActionBar);
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));//

        //get call location from prev. activity
        this.location = getIntent().getStringExtra(TweetListActivity.LOCATION_MESSAGE);
        this.locateCall();

        // create map
        setContentView(R.layout.activity_location_list);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        //this.navigation = (NavigationView) findViewById(R.id.navigationView2);
    }

    private void locateCall() {
        MapboxGeocoder client = new MapboxGeocoder.Builder()
                .setAccessToken(Mapbox.getAccessToken())
                .setLocation(this.location)
                .build();

        Thread thread = new Thread(){
            public void run(){
                retrofit.Response<GeocoderResponse> response = null;
                try {
                    response = client.execute();
                } catch (IOException e) {
                    //Todo:improve error handling
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(LocationListActivity.this,"No network!",Toast.LENGTH_LONG).show());
                    LocationListActivity.this.finish();
                }
                if (response != null) {
                    LocationListActivity.this.lat = response.body().getFeatures().get(0).getLatitude();
                    LocationListActivity.this.lon = response.body().getFeatures().get(0).getLongitude();
                }else {
                    //Todo:improve error handling
                    LocationListActivity.this.lat = 0;
                    LocationListActivity.this.lon = 0;
                }
            }

        };
        thread.start();
        try {
            thread.join();//ToDo:make the thread less hacky
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        //set destination marker
        // variables for adding a marker
        Marker destMarker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(this.lat, this.lon))
                .title("call @ " + this.location)
        );
        this.destPoint=
                Point.fromLngLat(destMarker.getPosition().getLongitude(),
                        destMarker.getPosition().getLatitude());
        //enable location component
        this.enableLocationComponent();
        //can pass any array of coordinates here
        this.createMakers(SAMPLE_POINTS);
        this.getRoute();
        this.markerClickListener();
    }
    private void getRoute() {
        Point origin = this.originPoint;
        Point reservoir = this.reservoirPoint;
        Point destination = this.destPoint;

        if( Mapbox.getAccessToken() == null){
            //Todo: improve error handling
            Toast.makeText(this, "No network!",Toast.LENGTH_LONG).show();
        }


        NavigationRoute.Builder builder =
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination);

        //add waypoint
        if(reservoir !=null) builder.addWaypoint(reservoir);

        builder.build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        //quit if no response
                        if (response.body() == null || response.body().routes().size() < 1) return;

                        route = response.body().routes().get(0);

                        // draw the route
                        if (navMapRoute != null)
                            navMapRoute.removeRoute();
                        else
                            navMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);

                        navMapRoute.addRoute(route);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        //Todo: improve error handling, carefull concurrency
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

    private void markerClickListener(){
        //add a waypoint to the route when user selects marker
        this.mapboxMap.setOnMarkerClickListener(marker -> {
            LocationListActivity.this.reservoirPoint = Point.fromLngLat(
                    marker.getPosition().getLongitude(),marker.getPosition().getLatitude());
            LocationListActivity.this.getRoute();
            return true;
        });
    }

    public void startNav(View view){
        //when Start Navigation button is pressed the NavigationActivity is launched
        Intent intent = new Intent(this,NavigationActivity.class);

        if(this.originPoint == null || this.destPoint == null) {
            runOnUiThread(() -> Toast.makeText(this,"No route!",Toast.LENGTH_SHORT).show());
            return;
        }

        double arr[];
        if(this.reservoirPoint != null) {
            arr = new double[]{this.originPoint.latitude(), this.originPoint.longitude(),
                    this.reservoirPoint.latitude(), this.reservoirPoint.longitude(),
                    this.destPoint.latitude(), this.destPoint.longitude()};
        }else{
            arr = new double[]{this.originPoint.latitude(), this.originPoint.longitude(),
                    this.destPoint.latitude(), this.destPoint.longitude()};
        }

        intent.putExtra(ROUTE_MESSAGE, arr);
        startActivity(intent);
    }

    private void createMakers(double[][] points){
        //add all markers and set icons
        Stream.of(points)
            .map(x -> Point.fromLngLat(x[0],x[1]))
            .forEach(p->this.mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(p.latitude(),p.longitude()))
                .icon(IconFactory.getInstance(this).fromResource(R.drawable.ic_water_drop))
                ));
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent() {
        //Todo: split this and improve location updates
        //checks permissions
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // activate tracking
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this);
            locationComponent.setLocationComponentEnabled(true);
            // set the camera
            locationComponent.setCameraMode(CameraMode.TRACKING);
            //set location
            this.originLoc = locationComponent.getLastKnownLocation();
            this.originPoint = Point.fromLngLat(this.originLoc.getLongitude(),
                    this.originLoc.getLatitude());
        } else {
            permitMgr = new PermissionsManager( this);
            permitMgr.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permitMgr.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission, Toast.LENGTH_LONG).show();
    }

    public void updateMapButton(View view){
        //Todo: automate this button
        this.enableLocationComponent();
        this.getRoute();
        Toast.makeText(this, "Route updated", Toast.LENGTH_SHORT).show();//Todo: magic string
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent();
        } else {
            Toast.makeText(this, R.string.user_location_permission, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        //ToDo:prevent memory leaks
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}