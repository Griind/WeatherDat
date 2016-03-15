package com.svitukha.weatherLocation;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;


/**
 * Created by Roman Svitukha on 2/6/2016.
 */


public class MainActivity extends AppCompatActivity implements LocationListener
{
    Location location;
    MenuItem refresh;
    Data data;
    String logTag = "RomanLogMain";
    String[] urlLatLongArray;
    ProgressBar progressBar;
    TextView station, weather, wind, time, temp, urlXml;


    //app works fine on real device with gps on or off;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(com.svitukha.weatherLocation.R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(com.svitukha.weatherLocation.R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getLocation();


        data = new Data();//creating object to store data;

        station = (TextView) findViewById(com.svitukha.weatherLocation.R.id.textView);
        time = (TextView) findViewById(com.svitukha.weatherLocation.R.id.textView2);
        weather = (TextView) findViewById(com.svitukha.weatherLocation.R.id.textView3);
        temp = (TextView) findViewById(com.svitukha.weatherLocation.R.id.textView4);
        wind = (TextView) findViewById(com.svitukha.weatherLocation.R.id.textView5);
        urlXml = (TextView) findViewById(R.id.urlXml);
        progressBar = (ProgressBar) findViewById(com.svitukha.weatherLocation.R.id.progress_bar);

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))

        {
            buildAlertMessageNoGps();
        } else
        {


            getAllData(location);//calling method of all methods;

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(com.svitukha.weatherLocation.R.menu.menu_main, menu);
        // my refresh button
        refresh = menu.findItem(com.svitukha.weatherLocation.R.id.refresh);


        refresh.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
        {
            @SuppressWarnings("MissingPermission")
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                Snackbar.make(findViewById(android.R.id.content), "            Weather information is being updated", Snackbar.LENGTH_LONG)

                        .show();


                refresh.setActionView(com.svitukha.weatherLocation.R.layout.actionbar_indeterminate_progress);//changing button to progress view;
                getLocation();
                getAllData(location);


                return true;
            }
        });
        MenuItem map = menu.findItem(R.id.map);
        map.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                if (data != null)
                {
                    intent.putExtra("lat", data.getLat());
                    intent.putExtra("long", data.getLong());
                }
                startActivity(intent);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private String downloadXml(String xmlUrl) // a method to download xml;
    {
        String xmlFile = "";
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (isConnected)
        {
            Log.d(logTag, "CONNECTED");

            try
            {
                URL url = new URL(xmlUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream is = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                StringBuilder total = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                {
                    total.append(line);
                }
                xmlFile = total.toString();
                Log.i(logTag, xmlFile);

            } catch (MalformedURLException e)
            {
                Log.d(logTag, "----------MalformedURLException-------");
                e.printStackTrace();
            } catch (IOException e)
            {
                Log.d(logTag, "-----------IOexception---------");
                e.printStackTrace();
            }
        }


        return xmlFile;
    }

    public XmlPullParser getParser()
    {
        return getResources().getXml(com.svitukha.weatherLocation.R.xml.station_lookup1);
    }

    private void getAllData(Location location)
    {
        (new AsyncTask<Location, Data, Data>()//object type Data is being "transferred" back and forth in AsyncTask;
        {
            @Override
            protected Data doInBackground(Location... params)
            {


                try
                {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }


                //parse main xml;

                urlLatLongArray = new ParseMainXml(getParser(), params[0]).process();


                //call regular parser;


                data = new ParseApplication(downloadXml(urlLatLongArray[0])).process(data);//here i create an object of my parser and
                // call its method that returns edited data object;
                data.setLat(Double.parseDouble(urlLatLongArray[1]));
                data.setLong(Double.parseDouble(urlLatLongArray[2]));


                return data;

            }

            @Override
            protected void onPreExecute()
            {
                progressBar.setVisibility(View.VISIBLE);

                super.onPreExecute();
            }

            @SuppressLint("SetTextI18n")
            @Override
            protected void onPostExecute(Data aVoid)
            {
                station.setText(data.getStationId());
                time.setText(data.getObservationTime());
                weather.setText(data.getWeather());
                temp.setText(data.getTempretureString());
                wind.setText(data.getWindString());
                if (urlLatLongArray[0] == null)
                {
                    urlLatLongArray[0] = "Please check your location settings and permissions.";
                }
                urlXml.setText("  url: " + urlLatLongArray[0]);
                super.onPostExecute(aVoid);
                progressBar.setVisibility(View.GONE);
                if (refresh != null)
                {
                    refresh.setActionView(null);//changing appBar icon back to normal view;
                }
            }

            @Override
            protected void onProgressUpdate(Data... values)
            {

                super.onProgressUpdate(values);
            }
        }).execute(location);

    }


    public void getLocation()
    {

        LocationManager locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {

            return;
        }
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, MainActivity.this);


    }

    private void buildAlertMessageNoGps()
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id)
                    {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener()
                {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id)
                    {
                        Toast.makeText(getApplicationContext(), "You need to enable your location", Toast.LENGTH_LONG).show();
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        LocationManager locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {

            return;
        }
        locMgr.removeUpdates(this);

    }

    @Override
    public void onLocationChanged(Location location)
    {

        this.location = location;

        try
        {
            Log.i(logTag, "" + location.getLatitude());
            Log.i(logTag, "" + location.getLongitude());


        } catch (Exception e)
        {

            Toast.makeText(getApplicationContext(), "Unable to get Location"
                    , Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    @Override
    public void onProviderEnabled(String provider)
    {

    }

    @Override
    public void onProviderDisabled(String provider)
    {

    }
}
