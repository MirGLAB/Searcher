package mirglab.liza_alert_app;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.splunk.mint.Mint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class V_MissionMapSidebarActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback {

    Context context = this;
    boolean bound = false;
    ServiceConnection sConn;
    Intent intent, smsOnMap, gpsService;
    private GoogleMap mMap;
    //Volunteer volunteer = null;
    Map<Integer, Marker> markers;
    //ArrayList<V_MapsActivity.Volunteer> volunteers;
    ArrayList<String> numbers = new ArrayList<>();
    List<LatLng> points = new ArrayList<>();
    List<LatLng> centerCoords = new ArrayList<>();
    List<LatLng> nodes = new ArrayList<>();
    List<Double> latNodes = new ArrayList<>();
    List<Double> lngNodes = new ArrayList<>();
    public int k=0;
    Random rnd = new Random();
    int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    //интервал обновления положения всплывающего окна.
    //для плавности необходимо 60 fps, то есть 1000 ms / 60 = 16 ms между обновлениями.
    private static final int POPUP_POSITION_REFRESH_INTERVAL = 60*1000;
    //Handler, запускающий обновление окна с заданным интервалом
    private Handler handler;
    //Runnable, который обновляет положение окна
    private Runnable updater;
    SharedPreferences sPref;
    double centerLat, centerLng;
    mirglab.liza_alert_app.V_DataBase db;
    final String LOG_TAG = "____";
    String operationID, groupName;
    NavigationView navigationView;
    DrawerLayout drawerLayout;
    String spMissionContinuationKey;
    Button btnStartRequestSMSService, btnStopRequestSMSService, btnStartService, btnStopService, btnShowZone, btnShowCenter,
            btnStartServiceLocal, btnStopServiceLocal, btnShowZoneLocal, btnShowCenterLocal,
            seekBarOk, seekBarCancel, seekBarLocalOk, seekBarLocalCancel, btnSaveRoute;
    ExpandableListView expListGroups;
    CustomAdapter adapter;
    ArrayList<Parent> arrayParents;
    ArrayList<String> arrayChildren;
    ArrayList<Boolean> btnZonesClick;
    int groupsCount;
    ArrayList<Group> groups;
    ArrayList<Polygon> groupsZones;
    Integer[] colors;
    SeekBar seekBar, seekBarLocal;
    TextView seekBarText, seekBarTextLocal, seekBarTextPreview, seekBarTextLocalPreview;
    mirglab.liza_alert_app.V_MissionMapSidebarActivity VMissionMapSidebarActivity;
    private LocationManager locationManager;
    int timer = 60*1000;
    ArrayList<Polygon> groupZones = new ArrayList<>();
    Marker centerMarker;
    Dialog dialogInterval, dialogIntervalLocal;
    LayoutInflater inflater;
    Boolean mapReady = false;
    ArrayList<Polyline> netPolylines = new ArrayList<>();
    ArrayList<GroundOverlay> nodesGroundOverlays = new ArrayList<>();
    String alhpabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    Intent startIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.v_activity_mission_map_sidebar);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage("Для дальнейшей работы интернет и Bluetooth не требуются. Отключите их для экономии энергии.")
                .setCancelable(false)
                .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                    }
                });
        builder.create();
        builder.show();

        VMissionMapSidebarActivity = this;

        db = new mirglab.liza_alert_app.V_DataBase(this);
        db.open();

        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
        //enableBroadcastReceiver();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //volunteers = new ArrayList();
        markers = new HashMap<Integer, Marker>();

        btnZonesClick = new ArrayList<>();

        operationID = getIntent().getStringExtra("id");
        Log.d(LOG_TAG, "MissionMapActivity of " + operationID);
        groupName = getIntent().getStringExtra("groupName");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        /*
        seekBarTextLocal = findViewById(R.id.seekBarTextLocal);
        seekBarLocal = findViewById(R.id.seekBarLocal);
        btnStartServiceLocal = findViewById(R.id.btnStartServiceLocal);
        btnStartServiceLocal.setEnabled(true);
        btnStartServiceLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    enableBroadcastReceiver();
                    Intent startIntent = new Intent(V_MissionMapSidebarActivity.this, V_GpsService.class);
                    startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    startIntent.putExtra("timer", seekBarLocal.getProgress());
                    startIntent.putExtra("id", operationID);
                    //startIntent.putExtra("groupName", groupName);
                    startService(startIntent);
                    btnStartServiceLocal.setEnabled(false);
                    btnStopServiceLocal.setEnabled(true);
                }
                else {
                    dialogGPS();
                }
            }
        });

        btnStopServiceLocal = findViewById(R.id.btnStopServiceLocal);
        btnStopServiceLocal.setEnabled(false);
        btnStopServiceLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBroadcastReceiver();
                Intent stopIntent = new Intent(V_MissionMapSidebarActivity.this, V_GpsService.class);
                stopService(stopIntent);
                btnStopServiceLocal.setEnabled(false);
                btnStartServiceLocal.setEnabled(true);
            }
        });
        */

        /*
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            enableBroadcastReceiver();
            Intent startIntent = new Intent(V_MissionMapSidebarActivity.this, V_GpsService.class);
            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            startIntent.putExtra("timer", seekBarLocal.getProgress());
            startIntent.putExtra("id", operationID);
            //startIntent.putExtra("groupName", groupName);
            startService(startIntent);
            btnStartServiceLocal.setEnabled(false);
            btnStopServiceLocal.setEnabled(true);
        }
        else {
            dialogGPS();
        }
        */

        inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        dialogInterval = new Dialog(this);
        View layout = inflater.inflate(R.layout.v_dialog_interval, (ViewGroup)findViewById(R.id.dialogInterval));
        dialogInterval.setContentView(layout);
        //dialogInterval.setTitle("Интервал отправки GPS-координат по СМС");
        seekBarTextPreview = (TextView)layout.findViewById(R.id.seekBarTextPreview);
        seekBarText = (TextView)layout.findViewById(R.id.seekBarText);
        seekBar = (SeekBar)layout.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBarVar) {
                if (seekBarVar.getProgress() < seekBarLocal.getProgress()) {
                    seekBarVar.setProgress(seekBarLocal.getProgress());
                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                            .setMessage("Интервал отправки координат не может быть меньше интервала отрисовки маршрута")
                            .setCancelable(false)
                            .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.create();
                    builder.show();
                }
                CustomSharedPreferences.setDefaults("timer", Integer.toString(seekBar.getProgress()), context);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //add code here
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarText.setText("Отправлять каждые " + String.valueOf(seekBar.getProgress()) + " минут");
                if (seekBar.getProgress() == 0)
                    seekBar.setProgress(1);
            }
        });
        seekBarOk = (Button)layout.findViewById(R.id.seekBarOk);
        seekBarOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CustomSharedPreferences.setDefaults("timer", Integer.toString(seekBar.getProgress()), context);
                dialogInterval.dismiss();
            }
        });
        seekBarCancel = (Button)layout.findViewById(R.id.seekBarCancel);
        seekBarCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogInterval.dismiss();
            }
        });

        dialogIntervalLocal = new Dialog(this);
        layout = inflater.inflate(R.layout.v_dialog_interval_local, (ViewGroup)findViewById(R.id.dialogIntervalLocal));
        dialogIntervalLocal.setContentView(layout);
        //dialogIntervalLocal.setTitle("Интервал отрисовки маршрута");
        seekBarTextLocalPreview = (TextView)layout.findViewById(R.id.seekBarTextLocalPreview);
        seekBarTextLocal = (TextView)layout.findViewById(R.id.seekBarTextLocal);
        seekBarLocal = (SeekBar)layout.findViewById(R.id.seekBarLocal);
        seekBarLocal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBarVar) {
                if (seekBarVar.getProgress() > seekBar.getProgress()) {
                    seekBarVar.setProgress(seekBar.getProgress());
                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                            .setMessage("Интервал отрисовки маршрута не может быть больше интервала отправки координат")
                            .setCancelable(false)
                            .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.create();
                    builder.show();
                }
                CustomSharedPreferences.setDefaults("timerLocal", Integer.toString(seekBarVar.getProgress()), context);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //add code here
            }

            @Override
            public void onProgressChanged(SeekBar seekBarVar, int progress, boolean fromUser) {
                seekBarTextLocal.setText("Отправлять каждые " + String.valueOf(seekBarVar.getProgress()) + " минут");
                    if (seekBarVar.getProgress() == 0)
                        seekBarVar.setProgress(1);
                    /*
                    if (seekBarVar.getProgress() > seekBar.getProgress()) {
                        seekBarVar.setProgress(seekBar.getProgress());
                        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                .setMessage("Интервал отрисовки маршрута не может быть больше интервала отправки координат")
                                .setCancelable(false)
                                .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        builder.create();
                        builder.show();
                    }
                    */
            }
        });
        seekBarLocalOk = (Button)layout.findViewById(R.id.seekBarLocalOk);
        seekBarLocalOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CustomSharedPreferences.setDefaults("timerLocal", Integer.toString(seekBarLocal.getProgress()), context);
                dialogIntervalLocal.dismiss();
            }
        });
        seekBarLocalCancel = (Button)layout.findViewById(R.id.seekBarLocalCancel);
        seekBarLocalCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogIntervalLocal.dismiss();
            }
        });

        //seekBarText = findViewById(R.id.seekBarText);
        //seekBar = findViewById(R.id.seekBar);
        btnStartService = findViewById(R.id.btnStartService);
        btnStartService.setEnabled(true);
        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    enableBroadcastReceiver();
                    //Intent startIntent = new Intent(V_MissionMapSidebarActivity.this, V_SendSmsAndGpsService.class);
                    startIntent = new Intent(mirglab.liza_alert_app.V_MissionMapSidebarActivity.this, mirglab.liza_alert_app.V_SendSmsAndGpsService.class);
                    startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    startIntent.putExtra("timer", seekBar.getProgress());
                    startIntent.putExtra("timerLocal", seekBarLocal.getProgress());
                    startIntent.putExtra("id", operationID);
                    //startIntent.putExtra("groupName", groupName);
                    startService(startIntent);
                    btnStartService.setEnabled(false);
                    btnStopService.setEnabled(true);
                }
                else {
                    dialogGPS();
                }
            }
        });

        btnStopService = findViewById(R.id.btnStopService);
        btnStopService.setEnabled(false);
        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBroadcastReceiver();
                Intent stopIntent = new Intent(mirglab.liza_alert_app.V_MissionMapSidebarActivity.this, mirglab.liza_alert_app.V_SendSmsAndGpsService.class);
                stopService(stopIntent);
                //stopService(startIntent);
                btnStopService.setEnabled(false);
                btnStartService.setEnabled(true);
            }
        });

        btnShowZone = findViewById(R.id.btnShowZone);
        btnShowZone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i = 0; i < groupZones.size(); i++) {
                    if(groupZones.get(i).isVisible())
                        groupZones.get(i).setVisible(false);
                    else {
                        if(i == 0) {
                            float zoomLevel = 13.5f;
                            List<LatLng> zoneCoords = new ArrayList();
                            zoneCoords = groupZones.get(i).getPoints();
                            Double lat1 = 0.0, lat2 = 0.0, lng1 = 0.0, lng2 = 0.0;
                            LatLng zoneCenter;
                            for(int j = 0; j < zoneCoords.size(); j++) {
                                if(j == 0) {
                                    lat1 = zoneCoords.get(j).latitude;
                                    lng1 = zoneCoords.get(j).longitude;
                                } else if(j == 1) {
                                    lng2 = zoneCoords.get(j).longitude;
                                } else if(j == 2) {
                                    lat2 = zoneCoords.get(j).latitude;
                                }
                            }
                            zoneCenter = new LatLng((lat2 + lat1)/2, (lng2 + lng1)/2);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoneCenter, zoomLevel));
                            groupZones.get(i).setVisible(true);
                        } else
                            groupZones.get(i).setVisible(true);
                    }
                }
            }
        });

        btnShowCenter = findViewById(R.id.btnShowCenter);
        btnShowCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(centerMarker == null) {
                    float zoomLevel = 14.5f;
                    LatLng zoneCenter = new LatLng(centerLat, centerLng);
                    centerMarker = mMap.addMarker(new MarkerOptions()
                            .position(zoneCenter)
                            .title("Штаб")
                            .icon(BitmapDescriptorFactory.defaultMarker(180))
                            .draggable(false));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoneCenter, zoomLevel));
                    centerMarker.showInfoWindow();
                } else if(centerMarker.isVisible()) {
                    centerMarker.setVisible(false);
                } else {
                    float zoomLevel = 14.5f;
                    LatLng zoneCenter = new LatLng(centerLat, centerLng);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoneCenter, zoomLevel));
                    centerMarker.setVisible(true);
                    centerMarker.showInfoWindow();
                }
            }
        });

        btnSaveRoute = findViewById(R.id.btnSaveRoute);
        btnSaveRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File sdCard = Environment.getExternalStorageDirectory();
                Date date = new Date();
                String strDateFormat = "hh:mm:ss";
                DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
                String formattedDate= dateFormat.format(date);
                File dir = new File(sdCard.getAbsolutePath() + "/Liza_Alert_Volunteer/" + operationID + "_" + formattedDate + ".txt");
                FileWriter writer = null;

                Cursor cursor = db.getAllData("User");
                String number = "";
                if(cursor.moveToFirst())
                    number = cursor.getString(cursor.getColumnIndex("number"));
                cursor.close();

                try {
                    writer = new FileWriter(dir);

                    writer.append(operationID);
                    writer.append("\n");
                    writer.append(groupName);
                    writer.append("\n");
                    writer.append(number);
                    writer.append("\n");

                    Cursor markersCursor = db.getFieldData("Markers", "operation", operationID);
                    if(markersCursor.moveToFirst()) {
                        int timeColIndex = markersCursor.getColumnIndex("time");
                        int latColIndex = markersCursor.getColumnIndex("lat");
                        int lngColIndex = markersCursor.getColumnIndex("lng");

                        do {
                            String time = markersCursor.getString(timeColIndex);
                            Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                            Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                            writer.append(time + " " + Double.toString(lat) + " " + Double.toString(lng));
                            writer.append("\n");
                        } while (markersCursor.moveToNext());
                    } else {

                    }
                    markersCursor.close();

                    writer.flush();
                    writer.close();
                    Toast.makeText(context, "Маршрут сохранен", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //expListGroups = (ExpandableListView) findViewById(R.id.missionMapListGroups);

        //fillList();

        //sets the adapter that provides data to the list.
        //adapter = new CustomAdapter(this, arrayParents);
        //expListGroups.setAdapter(new CustomAdapter(this, arrayParents));

        //sPref = getPreferences(MODE_PRIVATE);
        //int savedTime = sPref.getInt("time", 0);
        int savedTime = 1, savedTimeLocal = 1;
        if(CustomSharedPreferences.getDefaults("timer", this) != null) {
            savedTime = Integer.parseInt(CustomSharedPreferences.getDefaults("timer", this));
            Log.d("Saved time", Integer.toString(savedTime));
            seekBar.setProgress(savedTime);
            seekBarText.setText("Отправлять каждые " + savedTime + " минут");
        } else {
            seekBar.setProgress(1);
            seekBarText.setText("Отправлять каждые 1 минут");
        }
        //seekBar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener) this);

        if(CustomSharedPreferences.getDefaults("timerLocal", this) != null) {
            savedTimeLocal = Integer.parseInt(CustomSharedPreferences.getDefaults("timerLocal", this));
            Log.d("Saved time local", Integer.toString(savedTime));
            seekBarLocal.setProgress(savedTimeLocal);
            seekBarTextLocal.setText("Отправлять каждые " + savedTimeLocal + " минут");
        } else {
            seekBarLocal.setProgress(1);
            seekBarTextLocal.setText("Отправлять каждые 1 минут");
        }
        //seekBarLocal.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener) this);

        /*
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        timer, 0, locationListener);
                //Toast.makeText(this, "Запрос координат", Toast.LENGTH_SHORT).show();
            } catch (SecurityException ex) {
                //Toast.makeText(this, "catch", Toast.LENGTH_SHORT).show();
            }
        } else {
            locationDialog();
        }
        */
    }

    @Override
    public void onResume() {
        super.onResume();
        if(startIntent != null)
            stopService(startIntent);
        Intent stopIntent = new Intent(mirglab.liza_alert_app.V_MissionMapSidebarActivity.this, mirglab.liza_alert_app.V_SendSmsAndGpsService.class);
        stopService(stopIntent);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            enableBroadcastReceiver();
            Intent startIntent = new Intent(mirglab.liza_alert_app.V_MissionMapSidebarActivity.this, mirglab.liza_alert_app.V_SendSmsAndGpsService.class);
            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            startIntent.putExtra("timer", seekBar.getProgress());
            startIntent.putExtra("timerLocal", seekBarLocal.getProgress());
            startIntent.putExtra("id", operationID);
            //startIntent.putExtra("groupName", groupName);
            startService(startIntent);
            btnStartService.setEnabled(false);
            btnStopService.setEnabled(true);
        }
        else {
            dialogGPS();
        }
    }

    @Override
    protected void onDestroy() {
        if(startIntent != null)
            stopService(startIntent);
        super.onDestroy();
    }

    public void locationDialog() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setMessage("Требуется доступ к местоположению")
                    .setCancelable(false)
                    .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            locationDialog();
                        }
                    })
                    .setNeutralButton("Настройки", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    });
            builder.create();
            builder.show();
        } else {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        timer, 0, locationListener);
                //Toast.makeText(this, "Запрос координат", Toast.LENGTH_SHORT).show();
            } catch (SecurityException ex) {
                Toast.makeText(this, "Не удается определить местоположение устройства", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public mirglab.liza_alert_app.V_MissionMapSidebarActivity getVMissionMapSidebarActivity() {
        return VMissionMapSidebarActivity;
    }

    public void enableBroadcastReceiver()
    {
        handler = new Handler(Looper.getMainLooper());
        updater = new mirglab.liza_alert_app.V_MissionMapSidebarActivity.Updater();
        handler.post(updater);
    }

    public void disableBroadcastReceiver()
    {
        handler.removeCallbacks(updater);
    }

    /*
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if(seekBar == this.seekBar) {
            //CustomSharedPreferences.setDefaults("time", Integer.toString());
            seekBarText.setText("Отправлять каждые " + String.valueOf(seekBar.getProgress()) + " минут");
            if (seekBar.getProgress() == 0)
                seekBar.setProgress(1);
        } else if(seekBar == seekBarLocal) {
            seekBarTextLocal.setText("Отправлять каждые " + String.valueOf(seekBar.getProgress()) + " минут");
            if (seekBar.getProgress() == 0)
                seekBar.setProgress(1);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(seekBar == this.seekBar)
            CustomSharedPreferences.setDefaults("timer", Integer.toString(seekBar.getProgress()), this);
        else if (seekBar == seekBarLocal)
            CustomSharedPreferences.setDefaults("timerLocal", Integer.toString(seekBar.getProgress()), this);
    }
    */

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            String markerText = formatLocation(location);
            String[] markerTextParts = markerText.split("\\|");

            String latMarker = markerTextParts[0].replace(",", ".");
            String lngMarker = markerTextParts[1].replace(",", ".");
            String timeMarker = markerTextParts[2];

            ArrayList<String> markerInfo = new ArrayList<>(Arrays.asList(operationID, timeMarker, latMarker, lngMarker));
            db.addRec("Markers", markerInfo);

            Cursor markersCursor = db.getFieldData("Markers", "operation", operationID);
            if(markersCursor.moveToFirst()) {
                int onMapColIndex = markersCursor.getColumnIndex("onMap");
                int idColIndex = markersCursor.getColumnIndex("_id");
                int timeColIndex = markersCursor.getColumnIndex("time");
                int latColIndex = markersCursor.getColumnIndex("lat");
                int lngColIndex = markersCursor.getColumnIndex("lng");

                do {
                    if(markersCursor.getString(onMapColIndex).equals("false")) {
                        String id = markersCursor.getString(idColIndex);
                        String time = markersCursor.getString(timeColIndex);
                        Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                        Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                        String onMap = markersCursor.getString(onMapColIndex);

                        MarkerOptions markerOptions;
                        LatLng currentMarkerPosition = new LatLng(lat, lng);

                        markerOptions = new MarkerOptions()
                                .position(currentMarkerPosition)
                                .title(time)
                                .draggable(false);

                        Log.d(LOG_TAG, "Updater before markers.put " + markers.size());
                        markers.put(Integer.parseInt(id), mMap.addMarker(markerOptions));
                        db.modifyFieldData("Markers", "_id", Integer.parseInt(id), "onMap", "true");
                        //fillList();
                        //adapter = new CustomAdapter(context, arrayParents);
                        //expListGroups.setAdapter(new CustomAdapter(context, arrayParents));
                        Log.d(LOG_TAG, "Updater after markers.put " + markers.size());

                        LatLng prevMarkerPosition = null;
                        int prevId = 0;
                        Date prevDate = null;
                        Date curDate = null;
                        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        try {
                            curDate = format.parse(time);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        Cursor prevMarkerCursor = db.getFieldData("Markers", "operation", operationID);
                        if(prevMarkerCursor.moveToLast()) {
                            do {
                                prevId = Integer.parseInt(prevMarkerCursor
                                        .getString(prevMarkerCursor.getColumnIndex("_id")));
                                try {
                                    prevDate = format.parse(prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("time")));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                int currentId = Integer.parseInt(id);
                                String prevOnMap = prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("onMap"));
                                //if((currentId > prevId) && (prevOnMap.equals("true"))) {
                                if(curDate.after(prevDate) && (prevOnMap.equals("true"))) {
                                    Double prevLat = Double.parseDouble(prevMarkerCursor.getString(latColIndex));
                                    Double prevLng = Double.parseDouble(prevMarkerCursor.getString(lngColIndex));
                                    prevMarkerPosition = new LatLng(prevLat, prevLng);
                                }
                            } while(prevMarkerCursor.moveToPrevious() && (prevMarkerPosition == null));
                        } else {
                            Log.d(LOG_TAG, "prevMarkerCursor 0");
                        }
                        prevMarkerCursor.close();

                        if(prevMarkerPosition != null) {
                            if(markers.get(prevId) != null)
                                markers.get(prevId).setVisible(false);
                            mMap.addCircle(new CircleOptions()
                                    .center(prevMarkerPosition)
                                    .clickable(false)
                                    .fillColor(Color.RED)
                                    .strokeColor(Color.RED)
                                    .radius(0.5));
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .add(prevMarkerPosition, currentMarkerPosition)
                                    .width(5)
                                    .color(Color.RED);
                            mMap.addPolyline(polylineOptions);
                        }

                    }
                } while(markersCursor.moveToNext());
            } else {
                Log.d(LOG_TAG, "Groups count - 0");
            }
            markersCursor.close();

            /*
            SQLiteDatabase db = getBaseContext().openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
            db.execSQL(TABLE_CREATE);
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, id);
            values.put(COLUMN_LAT,String.valueOf(location.getLatitude()));
            values.put(COLUMN_LNG,String.valueOf(location.getLongitude()));
            Date tim = new Date(location.getTime());
            Format formatter = new SimpleDateFormat("HH:mm:ss dd MM");
            String s = formatter.format(tim);
            values.put(COLUMN_TIME,s);
            db.insert(TABLE_PATH, null,values);

            db.close();
            str=smsText;
            id++;
            */
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };

    @SuppressLint("DefaultLocale")
    private String formatLocation(Location location) {
        if (location == null)
            return "";
        return String.format("%1$.6f|%2$.6f|%3$tT", location.getLatitude(), location.getLongitude(), new Date(location.getTime()));
    }

    private class Updater implements Runnable {

        @Override
        public void run() {
            //помещаем в очередь следующий цикл обновления
            handler.postDelayed(this, POPUP_POSITION_REFRESH_INTERVAL);
            try {

                Log.d(LOG_TAG, "Updater: operationID " + operationID);
                //Cursor markersCursor = db.getMarkersData(operationID, )

                Cursor markersCursor = db.getFieldData("Markers", "operation", operationID);
                if(markersCursor.moveToFirst()) {
                    int onMapColIndex = markersCursor.getColumnIndex("onMap");
                    int idColIndex = markersCursor.getColumnIndex("_id");
                    int timeColIndex = markersCursor.getColumnIndex("time");
                    int latColIndex = markersCursor.getColumnIndex("lat");
                    int lngColIndex = markersCursor.getColumnIndex("lng");

                    do {
                        if(markersCursor.getString(onMapColIndex).equals("false")) {
                            String id = markersCursor.getString(idColIndex);
                            String time = markersCursor.getString(timeColIndex);
                            Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                            Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                            String onMap = markersCursor.getString(onMapColIndex);

                            MarkerOptions markerOptions;
                            LatLng currentMarkerPosition = new LatLng(lat, lng);

                            markerOptions = new MarkerOptions()
                                    .position(currentMarkerPosition)
                                    .title(time)
                                    .draggable(false);

                            Log.d(LOG_TAG, "Updater before markers.put " + markers.size());
                            markers.put(Integer.parseInt(id), mMap.addMarker(markerOptions));
                            db.modifyFieldData("Markers", "_id", Integer.parseInt(id), "onMap", "true");
                            //fillList();
                            //adapter = new CustomAdapter(context, arrayParents);
                            //expListGroups.setAdapter(new CustomAdapter(context, arrayParents));
                            Log.d(LOG_TAG, "Updater after markers.put " + markers.size());

                            LatLng prevMarkerPosition = null;
                            int prevId = 0;
                            Date prevDate = null;
                            Date curDate = null;
                            DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            try {
                                curDate = format.parse(time);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            Cursor prevMarkerCursor = db.getFieldData("Markers", "operation", operationID);
                            if(prevMarkerCursor.moveToLast()) {
                                do {
                                    prevId = Integer.parseInt(prevMarkerCursor
                                            .getString(prevMarkerCursor.getColumnIndex("_id")));
                                    try {
                                        prevDate = format.parse(prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("time")));
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    int currentId = Integer.parseInt(id);
                                    String prevOnMap = prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("onMap"));
                                    //if((currentId > prevId) && (prevOnMap.equals("true"))) {
                                    if(curDate.after(prevDate) && (prevOnMap.equals("true"))) {
                                        Double prevLat = Double.parseDouble(prevMarkerCursor.getString(latColIndex));
                                        Double prevLng = Double.parseDouble(prevMarkerCursor.getString(lngColIndex));
                                        prevMarkerPosition = new LatLng(prevLat, prevLng);
                                    }
                                } while(prevMarkerCursor.moveToPrevious() && (prevMarkerPosition == null));
                            } else {
                                Log.d(LOG_TAG, "prevMarkerCursor 0");
                            }
                            prevMarkerCursor.close();

                            if(prevMarkerPosition != null) {
                                if(markers.get(prevId) != null)
                                    markers.get(prevId).setVisible(false);
                                mMap.addCircle(new CircleOptions()
                                        .center(prevMarkerPosition)
                                        .clickable(false)
                                        .fillColor(Color.RED)
                                        .strokeColor(Color.RED)
                                        .radius(0.5));
                                PolylineOptions polylineOptions = new PolylineOptions()
                                        .add(prevMarkerPosition, currentMarkerPosition)
                                        .width(5)
                                        .color(Color.RED);
                                mMap.addPolyline(polylineOptions);
                            }

                        }
                    } while(markersCursor.moveToNext());
                } else {
                    Log.d(LOG_TAG, "Groups count - 0");
                }
                markersCursor.close();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        void stop() {
            this.stop();
        }
    }

    public void drawRoutes() {

        Cursor markersCursor = db.getFieldData("Markers", "operation", operationID);
        if(markersCursor.moveToFirst()) {
            int onMapColIndex = markersCursor.getColumnIndex("onMap");
            int idColIndex = markersCursor.getColumnIndex("_id");
            int timeColIndex = markersCursor.getColumnIndex("time");
            int latColIndex = markersCursor.getColumnIndex("lat");
            int lngColIndex = markersCursor.getColumnIndex("lng");

            do {
                String id = markersCursor.getString(idColIndex);
                String time = markersCursor.getString(timeColIndex);
                Log.d(LOG_TAG, "get marker " + id + " " + time);
            } while(markersCursor.moveToNext());
        } else {
            Log.d(LOG_TAG, "Groups count - 0");
        }
        markersCursor.close();

        markers.clear();
        markersCursor = db.getFieldData("Markers", "operation", operationID);
        if(markersCursor.moveToFirst()) {
            int onMapColIndex = markersCursor.getColumnIndex("onMap");
            int idColIndex = markersCursor.getColumnIndex("_id");
            int timeColIndex = markersCursor.getColumnIndex("time");
            int latColIndex = markersCursor.getColumnIndex("lat");
            int lngColIndex = markersCursor.getColumnIndex("lng");

            do {
                String id = markersCursor.getString(idColIndex);
                String time = markersCursor.getString(timeColIndex);
                Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                String onMap = markersCursor.getString(onMapColIndex);

                MarkerOptions markerOptions;
                LatLng currentMarkerPosition = new LatLng(lat, lng);

                markerOptions = new MarkerOptions()
                        .position(currentMarkerPosition)
                        .title(time)
                        .draggable(false);

                Log.d(LOG_TAG, "Updater before markers.put " + markers.size());
                markers.put(Integer.parseInt(id), mMap.addMarker(markerOptions));
                db.modifyFieldData("Markers", "_id", Integer.parseInt(id), "onMap", "true");
                //fillList();
                //adapter = new CustomAdapter(context, arrayParents);
                //expListGroups.setAdapter(new CustomAdapter(context, arrayParents));
                Log.d(LOG_TAG, "Updater after markers.put " + markers.size());

                LatLng prevMarkerPosition = null;
                int prevId = 0;
                Date prevDate = null;
                Date curDate = null;
                DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                try {
                    curDate = format.parse(time);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                Cursor prevMarkerCursor = db.getFieldData("Markers", "operation", operationID);
                if(prevMarkerCursor.moveToLast()) {
                    do {
                        prevId = Integer.parseInt(prevMarkerCursor
                                .getString(prevMarkerCursor.getColumnIndex("_id")));
                        try {
                            prevDate = format.parse(prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("time")));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        int currentId = Integer.parseInt(id);
                        //String prevOnMap = prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("onMap"));
                        //if((currentId > prevId) && (prevOnMap.equals("true"))) {
                        //if((currentId > prevId) && (prevOnMap.equals("true"))) {
                        if(curDate.after(prevDate)) {
                            Double prevLat = Double.parseDouble(prevMarkerCursor.getString(latColIndex));
                            Double prevLng = Double.parseDouble(prevMarkerCursor.getString(lngColIndex));
                            prevMarkerPosition = new LatLng(prevLat, prevLng);
                        }
                    } while(prevMarkerCursor.moveToPrevious() && (prevMarkerPosition == null));
                } else {
                    Log.d(LOG_TAG, "prevMarkerCursor 0");
                }
                prevMarkerCursor.close();

                if(prevMarkerPosition != null) {
                    if (markers.get(prevId) != null)
                        markers.get(prevId).setVisible(false);
                    mMap.addCircle(new CircleOptions()
                            .center(prevMarkerPosition)
                            .clickable(false)
                            .fillColor(Color.RED)
                            .strokeColor(Color.RED)
                            .clickable(true)
                            .radius(0.5));
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .add(prevMarkerPosition, currentMarkerPosition)
                            .width(5)
                            .color(Color.RED);
                    mMap.addPolyline(polylineOptions);
                }
            } while(markersCursor.moveToNext());
        } else {
            Log.d(LOG_TAG, "Groups count - 0");
        }
        markersCursor.close();
    }

    //public latLng getPreviousGroupMarker(String groupName)

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        Log.d(LOG_TAG, "onMapReady");
        mapReady = true;
        //fillZones();
        //LatLng moscow = new LatLng(55.751244, 37.618423);
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(moscow));
        //mMap.setOnInfoWindowClickListener(this);
        Cursor cursor = db.getFieldData("Operations", "_id", operationID);

        if(cursor.moveToFirst()) {

            int centerLatIndex = cursor.getColumnIndex("centerLat");
            int centerLngIndex = cursor.getColumnIndex("centerLng");
            Log.d(LOG_TAG + "maps", centerLatIndex + " " + centerLngIndex);

            do {
                centerLat = Double.parseDouble(cursor.getString(centerLatIndex));
                centerLng = Double.parseDouble(cursor.getString(centerLngIndex));
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "children - 0 rows");
        cursor.close();

        final LatLng center = new LatLng(centerLat, centerLng);
        centerCoords.add(center);
        drawNet(centerCoords);
        sortNodes();
        drawNodes(Color.BLACK);
        fillZones();
        drawRoutes();
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(center));
        float zoomLevel = 11.5f;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,zoomLevel));

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                final LatLng choosenLatLng = latLng;

                AlertDialog.Builder builder = new AlertDialog.Builder(mirglab.liza_alert_app.V_MissionMapSidebarActivity.this);

                // Set the positive/yes button click listener
                builder.setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {









                    }
                });

                // Specify the dialog is not cancelable
                builder.setCancelable(true);

                // Set a title for alert dialog
                builder.setTitle("Выберите действие");

                // Set the neutral/cancel button click listener
                builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do something when click the neutral button
                    }
                });

                AlertDialog dialog = builder.create();
                // Display the alert dialog on interface
                dialog.show();
            }
        });

        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                /*
                LatLng coords = circle.getCenter();
                String lat = Double.toString(coords.latitude);
                String lng = Double.toString(coords.longitude);
                Cursor cursor = db.getMarkersData(operationID, lat, lng);
                String title = "";
                if(cursor.moveToFirst()) {
                    title = cursor.getString(cursor.getColumnIndex("time"));
                }
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(coords)
                        .title(title)
                        .draggable(false));
                marker.showInfoWindow();
                */
            }
        });
    }


    public void fillZones() {

        String zone = "";
        Cursor cursorOperation = db.getIdData("Operations", operationID);
        if (cursorOperation.moveToFirst()) {
            int groupZoneColIndex = cursorOperation.getColumnIndex("groupZone");
            zone = cursorOperation.getString(groupZoneColIndex);
        } else
            Log.d(LOG_TAG, "cursorOperation - 0 rows");
        cursorOperation.close();

        Log.d(LOG_TAG, "MissionMap Group " + groupName + " zone = " + zone);

        if (!zone.equals("null")) {
            String[] zones = zone.split("\\@");
            for (int k = 0; k < zones.length; k++) {

                String curZone = zones[k];
                String firstNode = curZone.substring(0, 3);
                String secondNode = curZone.substring(3);
                Log.d(LOG_TAG, "!!! " + firstNode + " - " + secondNode);

                double firstLat, firstLng, secondLat, secondLng;
                firstLng = lngNodes.get(alhpabet.indexOf(firstNode.substring(0,1)));
                firstLat = latNodes.get(Integer.parseInt(firstNode.substring(1)) - 1);
                secondLng = lngNodes.get(alhpabet.indexOf(secondNode.substring(0,1)));
                secondLat = latNodes.get(Integer.parseInt(secondNode.substring(1)) - 1);
                Log.d(LOG_TAG, "Node: " + firstNode + " - " + Integer.toString(alhpabet.indexOf(firstNode.substring(0,1))) +
                        " " + Integer.toString(Integer.parseInt(firstNode.substring(1)) - 1));
                Log.d(LOG_TAG, "Node: " + secondNode + " - " + Integer.toString(alhpabet.indexOf(secondNode.substring(0,1))) +
                        " " + Integer.toString(Integer.parseInt(secondNode.substring(1)) - 1));

                //String hexColor = "#5f" + Integer.toHexString(colors.get(i)).substring(2);
                groupZones.add(mMap.addPolygon(new PolygonOptions()
                        .strokeColor(Color.WHITE)
                        .strokeWidth(0)
                        .fillColor(0x4F00FF00)
                        .geodesic(true)
                        .add(new LatLng(firstLat, firstLng),
                                new LatLng(firstLat, secondLng),
                                new LatLng(secondLat, secondLng),
                                new LatLng(secondLat, firstLng))));

                /*
                String[] latlng = zones[k].split("\\|");
                int j = 0;
                while (j < latlng.length) {
                    Log.d(LOG_TAG, "Split zone | " + latlng[j]);
                    String regexp = "\\d{2}\\.\\d{4,20}";
                    Pattern pattern = Pattern.compile(regexp);
                    Matcher matcher = pattern.matcher(latlng[j]);
                    if (matcher.find()) {
                        //Log.d(LOG_TAG, "Fill color is " + Integer.toHexString(colors[i]));
                        //String hexColor = "#0x4" + String.format("%06X", (0xFFFFFF & colors[i]));
                        //String hexColor = "#5f" + Integer.toHexString(colors[i]).substring(2);
                        PolygonOptions polygon = new PolygonOptions()
                                .strokeColor(Color.WHITE)
                                .fillColor(0x4F00FF00)
                                .strokeWidth(0)
                                .geodesic(true)
                                .add(new LatLng(Double.parseDouble(latlng[j]), Double.parseDouble(latlng[j + 1])),
                                        new LatLng(Double.parseDouble(latlng[j + 2]), Double.parseDouble(latlng[j + 3])),
                                        new LatLng(Double.parseDouble(latlng[j + 4]), Double.parseDouble(latlng[j + 5])),
                                        new LatLng(Double.parseDouble(latlng[j + 6]), Double.parseDouble(latlng[j + 7])));
                        groupZones.add(mMap.addPolygon(polygon));
                        groupZones.get(groupZones.size()-1).setVisible(false);

                        if (j + 8 < latlng.length)
                            j = j + 7;
                        else
                            j = latlng.length;
                    } else
                        j++;
                }
                */
            }
        }
    }

    public void fillList() {

        arrayParents = new ArrayList<Parent>();
        arrayChildren = new ArrayList<String>();

        Cursor groupCursor = db.getFieldData("Groups", "operation", operationID);
        if(groupCursor.moveToFirst()) {

            // определяем номера столбцов по имени в выборке
            int nameColIndex = groupCursor.getColumnIndex("name");

            do {
                String groupName = groupCursor.getString(nameColIndex);
                Parent parent = new Parent();
                parent.setTitle(groupName);
                arrayChildren = new ArrayList<String>();

                Cursor childrenCursor = db.getFieldData("Markers", "operation", operationID);

                if(childrenCursor.moveToLast()) {

                    int markerIdColIndex = childrenCursor.getColumnIndex("_id");
                    int markerTimeColIndex = childrenCursor.getColumnIndex("time");
                    int markerPhoneColIndex = childrenCursor.getColumnIndex("memberNumber");
                    int markerInfoColIndex = childrenCursor.getColumnIndex("info");
                    int markerGroupColIndex = childrenCursor.getColumnIndex("groupName");
                    Log.d(LOG_TAG, Integer.toString(markerTimeColIndex) + " " + Integer.toString(markerPhoneColIndex) +
                            " " + Integer.toString(markerGroupColIndex));
                    do {
                        //String memberName = childrenCursor.getString(memberNameColIndex);
                        //Log.d(LOG_TAG, memberName);
                        String memberGroup = childrenCursor.getString(markerGroupColIndex);
                        //Log.d(LOG_TAG, memberGroup);
                        if(memberGroup.equals(groupName)) {
                            String markerId = childrenCursor.getString(markerIdColIndex);
                            String markerTime = childrenCursor.getString(markerTimeColIndex);
                            String markerPhone = childrenCursor.getString(markerPhoneColIndex);
                            String markerInfo = childrenCursor.getString(markerInfoColIndex);
                            arrayChildren.add(markerTime + " - " + markerPhone + " - " + markerInfo);
                        }
                    } while (childrenCursor.moveToPrevious());
                } else
                    Log.d(LOG_TAG, "children - 0 rows");
                childrenCursor.close();

                parent.setArrayChildren(arrayChildren);
                arrayParents.add(parent);

                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (groupCursor.moveToNext());
        } else
            Log.d(LOG_TAG, "group - 0 rows");
        groupCursor.close();
    }

    void drawNet(List<LatLng> points)
    {
        draw(centerCoords.get(0),270,0);
        draw(points.get(0),0,270);
        draw(points.get(0),90,0);
        draw(points.get(0),0,90);
        draw(points.get(0),180,90);
        draw(points.get(0),90,180);
        draw(points.get(0),180,270);
        draw(points.get(0),270,180);
    }

    void draw(LatLng l, double a, double b) {

        List<LatLng> tt1 = new ArrayList<>();
        LatLng t1, t2 = l;
        for (int k = 0; k < 11; k++) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(3).color(Color.WHITE);

            LatLng j = t2;
            for (int i = 0; i < 10; i++) {
                tt1.add(j);
                t1 = SphericalUtil.computeOffset(j, 500, a);
                tt1.add(t1);
                polylineOptions.addAll(tt1);

                //nodes.addAll(tt1);
                for(int n = 0; n < tt1.size(); n++) {
                    latNodes.add(tt1.get(n).latitude);
                    lngNodes.add(tt1.get(n).longitude);
                }

                j = t1;
                tt1.clear();
            }
            t2 = SphericalUtil.computeOffset(t2, 500, b);
            //mMap.addPolyline(polylineOptions);
        }
    }

    void sortNodes() {

        Log.d(LOG_TAG, "before latNodes size: " + latNodes.size());
        Collections.sort(latNodes);
        Collections.reverse(latNodes);
        for(int i = 0; i < latNodes.size() - 1; i++) {
            //double diff =  Math.abs(latNodes.get(i) - latNodes.get(i+1));
            //Log.d(LOG_TAG, "lat diff: " + i + ") " + formattedDouble);
            while(Math.abs(latNodes.get(i) - latNodes.get(i+1)) < 0.0001) {
                //Log.d(LOG_TAG, "lat diff: " + i + ") " + (Math.abs(latNodes.get(i) - latNodes.get(i+1))));
                latNodes.remove(i + 1);
                if(i+1 == latNodes.size())
                    break;
            }
            //Log.d(LOG_TAG, "lat diff: " + i + ") " + (Math.abs(latNodes.get(i) - latNodes.get(i+1))));
        }
        Log.d(LOG_TAG, "after latNodes size: " + latNodes.size());

        Log.d(LOG_TAG, "before lngNodes size: " + lngNodes.size());
        Collections.sort(lngNodes);
        //Collections.reverse(lngNodes);
        for(int i = 0; i < lngNodes.size() - 1; i++) {
            while(Math.abs(lngNodes.get(i) - lngNodes.get(i+1)) < 0.001) {
                //Log.d(LOG_TAG, "lng diff: " + i + ") " + (Math.abs(lngNodes.get(i) - lngNodes.get(i+1))));
                lngNodes.remove(i + 1);
                if(i+1 == lngNodes.size())
                    break;
            }
            //Log.d(LOG_TAG, "lng diff: " + i + ") " + (Math.abs(lngNodes.get(i) - lngNodes.get(i+1))));
        }
        Log.d(LOG_TAG, "after lngNodes size: " + lngNodes.size());

        drawNet(Color.BLACK);
        /*
        for(int i = 0; i < latNodes.size(); i++) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(3).color(Color.WHITE);
            polylineOptions.add(new LatLng(latNodes.get(i), lngNodes.get(0)), new LatLng(latNodes.get(i), lngNodes.get(lngNodes.size() - 1)));
            mMap.addPolyline(polylineOptions);
        }

        for(int i = 0; i < lngNodes.size(); i++) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(3).color(Color.WHITE);
            polylineOptions.add(new LatLng(latNodes.get(0), lngNodes.get(i)), new LatLng(latNodes.get(latNodes.size() - 1), lngNodes.get(i)));
            mMap.addPolyline(polylineOptions);
        }
        */
    }

    void drawNet(int color) {
        for(int i = 0; i < latNodes.size(); i++) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(3).color(color);
            polylineOptions.add(new LatLng(latNodes.get(i), lngNodes.get(0)), new LatLng(latNodes.get(i), lngNodes.get(lngNodes.size() - 1)));
            netPolylines.add(mMap.addPolyline(polylineOptions));
        }

        for(int i = 0; i < lngNodes.size(); i++) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(3).color(color);
            polylineOptions.add(new LatLng(latNodes.get(0), lngNodes.get(i)), new LatLng(latNodes.get(latNodes.size() - 1), lngNodes.get(i)));
            netPolylines.add(mMap.addPolyline(polylineOptions));
        }
    }

    void drawNodes(int color) {

        Log.d(LOG_TAG, "Start creating nodes");

        int size = latNodes.size();

        Bitmap[][] bmpTexts = new Bitmap[size][size];
        String[][] nodeName = new String[size][size];

        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                String strText = "";
                if(Integer.toString(i+1).length() < 2)
                    strText = alhpabet.charAt(j) + "0" + (i+1);
                else
                    strText = alhpabet.charAt(j) + "" + (i+1);
                nodeName[i][j] = strText;
                Log.d(LOG_TAG, "Node " + nodeName[i][j]);
                Rect boundsText = new Rect();
                Paint paintText = new Paint();
                paintText.setColor(color);
                paintText.setTextSize(70);
                paintText.getTextBounds(nodeName[i][j], 0, nodeName[i][j].length(), boundsText);
                Bitmap.Config conf = Bitmap.Config.ARGB_4444;
                Bitmap bmpText = Bitmap.createBitmap(boundsText.width()*3, boundsText.height(), conf);
                Canvas canvasText = new Canvas(bmpText);
                canvasText.drawText(nodeName[i][j], canvasText.getWidth()/2, canvasText.getHeight(), paintText);
                bmpTexts[i][j] = bmpText;
            }
        }

        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                LatLng pos = new LatLng(latNodes.get(i) + 0.0004, lngNodes.get(j) + 0.0001);
                nodesGroundOverlays.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(bmpTexts[i][j]))
                        .transparency(0.3f)
                        .position(pos, 250f, 75f)));
            }
        }

        Log.d(LOG_TAG, "Finish creating nodes");
    }

    @Override
    public void onInfoWindowClick(Marker marker)
    {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        String mes_mas[] = null;
        String s=marker.getSnippet();
        mes_mas = s.split("\\s+");
        String numb=mes_mas[1];
        callIntent.setData(Uri.parse("tel:"+numb));
        startActivity(callIntent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.v_mission_map_sidebar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_map_type) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mirglab.liza_alert_app.V_MissionMapSidebarActivity.this);
            final String[] mapType = {"По умолчанию", "Спутник", "Рельеф"};

            final int[] checked = {0};
            builder.setSingleChoiceItems(mapType, checked[0], new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    checked[0] = which;
                }
            });

            // Set the positive/yes button click listener
            builder.setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mapReady) {
                        for(int i = 0; i < netPolylines.size(); i++)
                            netPolylines.get(i).remove();
                        for(int i = 0; i < nodesGroundOverlays.size(); i++)
                            nodesGroundOverlays.get(i).remove();
                        netPolylines.clear();
                        nodesGroundOverlays.clear();
                        switch (checked[0]) {
                            case 0:
                                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                                drawNet(Color.BLACK);
                                drawNodes(Color.BLACK);
                                break;
                            case 1:
                                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                drawNet(Color.WHITE);
                                drawNodes(Color.WHITE);
                                break;
                            case 2:
                                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                drawNet(Color.BLACK);
                                drawNodes(Color.BLACK);
                                break;
                        }
                    }
                }
            });

            // Specify the dialog is not cancelable
            builder.setCancelable(true);

            // Set a title for alert dialog
            builder.setTitle("Выберете тип карты");

            // Set the neutral/cancel button click listener
            builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do something when click the neutral button
                }
            });

            AlertDialog dialog = builder.create();
            // Display the alert dialog on interface
            dialog.show();
        }

        if(id == R.id.action_interval) {
            //dialogInterval.setTitle("Интервал отправки GPS-координат по СМС");
            dialogInterval.show();
        }

        if(id == R.id.action_interval_local) {
            //dialogIntervalLocal.setTitle("Интервал отрисовки маршрута");
            dialogIntervalLocal.show();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        /*
        int id = item.getItemId();

        if (id == R.id.startRequestSMSService) {
            enableBroadcastReceiver();
        } else if (id == R.id.stopRequestSMSService) {
            disableBroadcastReceiver();
        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void dialogGPS() {
        //Toast.makeText(this, "Для отправки координат включите GPS", Toast.LENGTH_SHORT).show();
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setMessage("Требуется доступ к местоположению")
                    .setCancelable(false)
                    .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                                dialogGPS();
                        }
                    })
                    .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    });
            builder.create();
            builder.show();
        }
    }

    public class Parent {
        private String mTitle;
        private ArrayList<String> mArrayChildren;

        public String getTitle() {
            return mTitle;
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public ArrayList<String> getArrayChildren() {
            return mArrayChildren;
        }

        public void setArrayChildren(ArrayList<String> arrayChildren) {
            mArrayChildren = arrayChildren;
        }
    }

    public class CustomAdapter extends BaseExpandableListAdapter {

        private LayoutInflater inflater;
        private ArrayList<Parent> mParent;

        public CustomAdapter(Context context, ArrayList<Parent> parent){
            mParent = parent;
            inflater = LayoutInflater.from(context);
        }

        @Override
        //counts the number of group/parent items so the list knows how many times calls getGroupView() method
        public int getGroupCount() {
            return mParent.size();
        }

        @Override
        //counts the number of children items so the list knows how many times calls getChildView() method
        public int getChildrenCount(int i) {
            return mParent.get(i).getArrayChildren().size();
        }

        @Override
        //gets the title of each parent/group
        public Object getGroup(int i) {
            return mParent.get(i).getTitle();
        }

        @Override
        //gets the name of each item
        public Object getChild(int i, int i1) {
            return mParent.get(i).getArrayChildren().get(i1);
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        //in this method you must set the text to see the parent/group on the list
        public View getGroupView(final int groupPosition, boolean b, View view, ViewGroup viewGroup) {

            ViewHolder holder = new ViewHolder();
            holder.groupPosition = groupPosition;

            if (view == null) {
                view = inflater.inflate(R.layout.v_list_item_mission_map_parent, viewGroup,false);
            }

            TextView textView = (TextView) view.findViewById(R.id.list_item_text_view);
            textView.setText(getGroup(groupPosition).toString());

            final String groupName = getGroup(groupPosition).toString();

            Button btnGroupShowZones = (Button) view.findViewById(R.id.btnGroupShowZones);
            btnGroupShowZones.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    groups.get(groupPosition).fillZones();
                }
            });

            view.setTag(holder);

            int color = 0;
            Cursor cursor = db.getFieldData("Groups", "operation", operationID);
            if(cursor.moveToFirst()) {
                do {
                    if (cursor.getString(cursor.getColumnIndex("name")).equals(groupName)) {
                        color = Integer.parseInt(cursor.getString(cursor.getColumnIndex("color")));
                        break;
                    }
                } while(cursor.moveToNext());
            } else
                Log.d(LOG_TAG, "Color cursor group getView - 0 rows");
            cursor.close();

            view.setBackgroundColor(color);

            //return the entire view
            return view;
        }

        @Override
        //in this method you must set the text to see the children on the list
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup viewGroup) {

            ViewHolder holder = new ViewHolder();
            holder.childPosition = childPosition;
            holder.groupPosition = groupPosition;

            if (view == null) {
                view = inflater.inflate(R.layout.v_list_item_child, viewGroup,false);
            }

            TextView textView = (TextView) view.findViewById(R.id.list_item_text_child);
            textView.setText(mParent.get(groupPosition).getArrayChildren().get(childPosition));

            view.setTag(holder);

            //return the entire view
            return view;
        }

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            /* used to make the notifyDataSetChanged() method work */
            super.registerDataSetObserver(observer);
        }

        protected class ViewHolder {
            protected int childPosition;
            protected int groupPosition;
            protected Button button;
        }
    }

    public class Group {
        String groupName;
        ArrayList<Polygon> zones = new ArrayList<>();

        Group(String name) {
            this.groupName = name;
        }

        public void addZone(PolygonOptions polygonOptions) {
            zones.add(mMap.addPolygon(polygonOptions));
            zones.get(zones.size()-1).setVisible(false);
        }

        public void fillZones() {
            for(int i = 0; i < zones.size(); i++) {
                if(zones.get(i).isVisible())
                    zones.get(i).setVisible(false);
                else {
                    if(i == 0) {
                        float zoomLevel = 13.5f;
                        List<LatLng> zoneCoords = new ArrayList();
                        zoneCoords = zones.get(i).getPoints();
                        Double lat1 = 0.0, lat2 = 0.0, lng1 = 0.0, lng2 = 0.0;
                        LatLng zoneCenter;
                        for(int j = 0; j < zoneCoords.size(); j++) {
                            if(j == 0) {
                                lat1 = zoneCoords.get(j).latitude;
                                lng1 = zoneCoords.get(j).longitude;
                            } else if(j == 1) {
                                lng2 = zoneCoords.get(j).longitude;
                            } else if(j == 2) {
                                lat2 = zoneCoords.get(j).latitude;
                            }
                        }
                        zoneCenter = new LatLng((lat2 + lat1)/2, (lng2 + lng1)/2);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoneCenter, zoomLevel));
                        zones.get(i).setVisible(true);
                    } else
                        zones.get(i).setVisible(true);
                }
            }
        }
    }
}
