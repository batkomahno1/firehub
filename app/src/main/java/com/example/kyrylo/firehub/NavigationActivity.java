package com.example.kyrylo.firehub;
//delivery version 1


import android.location.Location;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class NavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, OffRouteListener {
    //had to implement navigation within seperate activity because NavigationListener is defective
    //ToDo: manage resume/pause better

    private NavigationView navigationView;
    private DirectionsRoute route;
    private Point originPnt;
    private Point reservPnt;
    private Point destPnt;
    private NavigationListener listener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_NoActionBar);
        super.onCreate(savedInstanceState);
        //take the points from prev activity
        //ToDo:get rid of magic numbers
        double arr[] = this.getIntent().getDoubleArrayExtra(LocationListActivity.ROUTE_MESSAGE);
        if (arr.length == 6) {
            this.originPnt = Point.fromLngLat(arr[1], arr[0]);//(Point)arr[1];
            this.reservPnt = Point.fromLngLat(arr[3], arr[2]);//(Point)arr[2];
            this.destPnt = Point.fromLngLat(arr[5], arr[4]);//(Point)arr[3];
        }else if(arr.length==4){
            this.originPnt = Point.fromLngLat(arr[1], arr[0]);
            this.destPnt = Point.fromLngLat(arr[3], arr[2]);
        }else{
            //ToDo:remove magic string and number
            Toast.makeText(this,"No route!",Toast.LENGTH_LONG).show();
            this.finish();
        }

        //need to create a listener for the navigation UI buttons
        listener = new NavigationListener() {
            @Override
            public void onCancelNavigation() {
                NavigationActivity.super.finish();
            }

            @Override
            public void onNavigationFinished() {
                NavigationActivity.super.finish();
            }

            @Override
            public void onNavigationRunning() {
            }
        };

        setContentView(R.layout.activity_navigation);
        navigationView = findViewById(R.id.navigationView);
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        // nav view inactive -> call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
    }


    @Override
    public void onNavigationReady(boolean isRunning) {
        //this is where navigation starts
        //might need location engine to reroute

        //LocationEngine locEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        //locEngine.activate();

        this.navStart(this.originPnt);
    }

    private void navStart(Point origin){
        NavigationRoute tempRoute;
        if(this.reservPnt != null) {
            tempRoute = NavigationRoute.builder(NavigationActivity.this)
                    .accessToken(getString(R.string.mapbox_access_token))
                    .origin(origin)
                    .addWaypoint(NavigationActivity.this.reservPnt)
                    .destination(NavigationActivity.this.destPnt)
                    .build();
        }else{
            tempRoute = NavigationRoute.builder(NavigationActivity.this)
                    .accessToken(getString(R.string.mapbox_access_token))
                    .origin(origin)
                    //.addWaypoint(NavigationActivity.this.reservPnt)
                    .destination(NavigationActivity.this.destPnt)
                    .build();
        }
        tempRoute.getRoute(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.body() == null || response.body().routes().size() < 1) return;
                NavigationActivity.this.navigationView.startNavigation(
                        NavigationViewOptions.builder()
                                .directionsRoute(response.body().routes().get(0))
                                .shouldSimulateRoute(false)
                                .navigationListener(listener)
                                .build()
                );
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                //Todo: improve this error handling, carefull concurrency
                t.printStackTrace();
                runOnUiThread(() -> Toast.makeText(NavigationActivity.this,"No network!",Toast.LENGTH_LONG).show());
                //NavigationActivity.this.finish();
            }
        });
    }

    @Override
    public void userOffRoute(Location location) {
        this.navStart(Point.fromLngLat(location.getLongitude(),location.getLatitude()));
    }
}
