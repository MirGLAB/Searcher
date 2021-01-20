package mirglab.liza_alert_app;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

public class MissionMapSidebarActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback {

    Context context = this;
    boolean bound = false;
    ServiceConnection sConn;
    Intent intent, smsOnMap;
    GetSmsService myServiceGet;
    private GoogleMap mMap;
    //Volunteer volunteer = null;
    Map<Integer, Marker> markers;
    //ArrayList<MapsActivity.Volunteer> volunteers;
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
    DataBase db;
    final String LOG_TAG = "____";
    String operationID;
    NavigationView navigationView;
    DrawerLayout drawerLayout;
    String spMissionContinuationKey;
    Button btnStartRequestSMSService, btnStopRequestSMSService, btnLoadRoutesFile;
    ExpandableListView expListGroups;
    CustomAdapter adapter;
    ArrayList<Parent> arrayParents;
    ArrayList<String> arrayChildren;
    ArrayList<Boolean> btnZonesClick;
    int groupsCount;
    ArrayList<Group> groups;
    ArrayList<Polygon> groupsZones;
    Integer[] colors;
    Boolean mapReady = false;
    ArrayList<Polyline> netPolylines = new ArrayList<>();
    ArrayList<GroundOverlay> nodesGroundOverlays = new ArrayList<>();
    String alhpabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    ArrayList<Circle> circles = new ArrayList<>();
    ArrayList<Polyline> polylines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_map_sidebar);

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

        db = new DataBase(this);
        db.open();

        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
        //enableBroadcastReceiver();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //volunteers = new ArrayList();
        markers = new HashMap<Integer, Marker>();

        btnZonesClick = new ArrayList<>();

        //smsOnMap = new Intent(MissionMapSidebarActivity.this, SMSMonitor.class);

        /*
        intent = new Intent(this, GetSmsService.class);
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                myServiceGet = ((GetSmsService.MyBinderGet) binder).getService();
                bound = true;
            }
            public void onServiceDisconnected(ComponentName name) {
                bound = false;
            }
        };
        */

        //handler = new Handler(Looper.getMainLooper());
        //updater = new MissionMapSidebarActivity.Updater();
        //запускаем периодическое обновление
        //handler.post(updater);

        operationID = getIntent().getStringExtra("id");
        Log.d(LOG_TAG, "MissionMapActivity of " + operationID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        spMissionContinuationKey = "missionContinuation" + operationID;
        Log.d(LOG_TAG, "MissionMapActivity key " + spMissionContinuationKey);
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(spMissionContinuationKey, "true");
        ed.apply();

        btnStartRequestSMSService = findViewById(R.id.btnStartRequestSMSService);
        btnStartRequestSMSService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBroadcastReceiver();
                btnStartRequestSMSService.setEnabled(false);
                btnStopRequestSMSService.setEnabled(true);
            }
        });

        btnStopRequestSMSService = findViewById(R.id.btnStopRequestSMSService);
        btnStopRequestSMSService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBroadcastReceiver();
                btnStopRequestSMSService.setEnabled(false);
                btnStartRequestSMSService.setEnabled(true);
            }
        });

        enableBroadcastReceiver();
        btnStartRequestSMSService.setEnabled(false);
        btnStopRequestSMSService.setEnabled(true);

        btnLoadRoutesFile = findViewById(R.id.btnLoadRoutesFile);
        btnLoadRoutesFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent()
                        .setType("text/plain")
                        .setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Выберите файл маршрута"), 123);
            }
        });

        expListGroups = findViewById(R.id.missionMapListGroups);

        expListGroups.setOnItemLongClickListener(new ExpandableListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int groupPosition, long id) {

                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {

                    final int childPos = ExpandableListView.getPackedPositionChild(id);
                    int parentPos = ExpandableListView.getPackedPositionGroup(id);
                    //Log.d(LOG_TAG, "Child position " + Integer.toString(ExpandableListView.getPackedPositionChild(id)+1));
                    //Log.d(LOG_TAG, "Parent title" + arrayParents.get(parentPos).getArrayChildren().get(childPos));

                    final String groupName = arrayParents.get(parentPos).getTitle();
                    final String childName = arrayParents.get(parentPos).getArrayChildren().get(childPos);
                    Log.d(LOG_TAG, "Marker info " + groupName + ": " + childName);
                    String[] markerInfo = childName.split("\\|");
                    Log.d(LOG_TAG, "Marker pos = " + markerInfo[2] + markerInfo[3]);
                    LatLng markerPos = new LatLng(Double.parseDouble(markerInfo[2]), Double.parseDouble(markerInfo[3]));
                    float zoomLevel = 20.5f;
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPos, zoomLevel));
                }
                return false;
            }
        });

        groupsCount = db.getFieldData("Groups", "operation", operationID).getCount();
        Log.d(LOG_TAG, "Group count " + groupsCount);

        fillList();

        //sets the adapter that provides data to the list.
        adapter = new CustomAdapter(this, arrayParents);
        expListGroups.setAdapter(new CustomAdapter(this, arrayParents));

        enableBroadcastReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        enableBroadcastReceiver();
    }

    public void enableBroadcastReceiver()
    {
        smsOnMap = new Intent(MissionMapSidebarActivity.this, SMSMonitor.class);
        //smsOnMap.putExtra("operationID", operationID);
        /*
        sPref = getSharedPreferences("SmsMonitor", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString("currentOperation", operationID);
        ed.apply();
        */
        CustomSharedPreferences.setDefaults("Current operation", operationID, this);

        handler = new Handler(Looper.getMainLooper());
        updater = new MissionMapSidebarActivity.Updater();
        handler.post(updater);

        ComponentName receiver = new ComponentName(this, SMSMonitor.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        Toast.makeText(this, "Сообщения принимаются", Toast.LENGTH_SHORT).show();
    }

    public void disableBroadcastReceiver()
    {
        ComponentName receiver = new ComponentName(this, SMSMonitor.class);
        PackageManager pm = this.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        handler.removeCallbacks(updater);
        Toast.makeText(this, "Прием сообщений завершен", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 123 && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            //Log.d(LOG_TAG, "Choosen file " + selectedfile);

            FileInputStream is;
            BufferedReader reader;
            File sdCard = Environment.getExternalStorageDirectory();
            File file = new File(sdCard.getAbsolutePath() + "/" + selectedfile.getPath().substring(selectedfile.getPath().indexOf(":") + 1));

            if (file.exists()) {
                try {
                    is = new FileInputStream(file);
                    reader = new BufferedReader(new InputStreamReader(is));
                    int lineNum = 0;
                    String operationIDText = "", groupNameText = "", numberText = "";
                    String line = reader.readLine();
                    if(line.equals(operationID)) {
                        while(line != null) {
                            lineNum++;
                            Log.d(LOG_TAG, "TextLine " + line);
                            if (lineNum == 1)
                                operationIDText = line;
                            else if (lineNum == 2)
                                groupNameText = line;
                            else if (lineNum == 3)
                                numberText = line;
                            else {
                                String[] textParts = line.split(" ");
                                for (int j = 0; j < textParts.length; j++)
                                    Log.d(LOG_TAG, "Text line parts: part " + j + " " + textParts[j]);
                                String textTime = textParts[0] + " " + textParts[1];
                                String textLat = textParts[2];
                                String textLng = textParts[3];
                                String messageInfo = "-";
                                ArrayList<String> textMarker = new ArrayList<>(Arrays.asList(operationIDText, groupNameText, numberText, textTime,
                                        textLat, textLng, messageInfo));
                                db.addRec("Markers", textMarker);
                            }
                            line = reader.readLine();
                        }
                    }
                    is.close();
                    drawRoutes();
                    fillList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(LOG_TAG, "File not exists " + sdCard.getAbsolutePath() + "/" + selectedfile.getPath().substring(selectedfile.getPath().indexOf(":") + 1));
            }
        }
    }

    /*
    public class Volunteer {

        String name, time, number;
        double Lat, Lng;
        MarkerOptions markOp;

        Volunteer(String mes,String num) {
            String mes_mas[] = null;
            try {
                mes_mas = mes.split(";");
                Lat = Double.parseDouble(mes_mas[2].replace(",", "."));
                Lng = Double.parseDouble(mes_mas[3].replace(",", "."));
                points.add(new LatLng(Lat, Lng));
                name = mes_mas[1];
                number = num;
                numbers.add(num);
                time = mes_mas[4];
                markOp = new MarkerOptions().position(new LatLng(Lat, Lng)).title(time).snippet(name + " " + number);
                k++;
            }
            catch (Exception ArrayIndexOutOfBoundsException)
            {

            }

        }
    }
    */

    private class Updater implements Runnable {

        @Override
        public void run() {
            //помещаем в очередь следующий цикл обновления
            handler.postDelayed(this, POPUP_POSITION_REFRESH_INTERVAL);
            try {
                /*
                sPref = getSharedPreferences(operationID + "_sms", Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = sPref.edit();
                String smsText = sPref.getString("smsText", null);
                String smsNum = sPref.getString("smsNum", null);
                ed.remove("smsText");
                ed.remove("smsNum");
                ed.apply();
                */

                Log.d(LOG_TAG, "Updater: operationID " + operationID);
                //Cursor markersCursor = db.getMarkersData(operationID, )

                Cursor markersCursor = db.getFieldData("Markers", "operation", operationID);
                if(markersCursor.moveToFirst()) {
                    int onMapColIndex = markersCursor.getColumnIndex("onMap");
                    int idColIndex = markersCursor.getColumnIndex("_id");
                    int groupNameColIndex = markersCursor.getColumnIndex("groupName");
                    int memberNumberColIndex = markersCursor.getColumnIndex("memberNumber");
                    int timeColIndex = markersCursor.getColumnIndex("time");
                    int latColIndex = markersCursor.getColumnIndex("lat");
                    int lngColIndex = markersCursor.getColumnIndex("lng");
                    int infoColIndex = markersCursor.getColumnIndex("info");
                    do {
                        String id = markersCursor.getString(idColIndex);
                        String groupName = markersCursor.getString(groupNameColIndex);
                        String memberNumber = markersCursor.getString(memberNumberColIndex);
                        String time = markersCursor.getString(timeColIndex);
                        Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                        Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                        String info = markersCursor.getString(infoColIndex);
                        String onMap = markersCursor.getString(onMapColIndex);
                        Log.d(LOG_TAG, id + " " + groupName + " " + time + " " + onMap);
                    } while (markersCursor.moveToNext());
                } else {

                }
                markersCursor.close();

                markersCursor = db.getFieldData("Markers", "operation", operationID);
                if(markersCursor.moveToFirst()) {
                    int onMapColIndex = markersCursor.getColumnIndex("onMap");
                    int idColIndex = markersCursor.getColumnIndex("_id");
                    int groupNameColIndex = markersCursor.getColumnIndex("groupName");
                    int memberNumberColIndex = markersCursor.getColumnIndex("memberNumber");
                    int timeColIndex = markersCursor.getColumnIndex("time");
                    int latColIndex = markersCursor.getColumnIndex("lat");
                    int lngColIndex = markersCursor.getColumnIndex("lng");
                    int infoColIndex = markersCursor.getColumnIndex("info");

                    /*
                    "_id" + " integer primary key autoincrement, " +
                    "operation" + " text, " +
                    "groupName" + " text, " +
                    "memberNumber" + " text, " +
                    "time" + " text, "  +
                    "lat" + " text, " +
                    "lng" + " text, " +
                    "info" + " text, " +
                    "onMap" + " text" +
                     */
                    do {
                        if(markersCursor.getString(onMapColIndex).equals("false")) {
                            String id = markersCursor.getString(idColIndex);
                            String groupName = markersCursor.getString(groupNameColIndex);
                            String memberNumber = markersCursor.getString(memberNumberColIndex);
                            String time = markersCursor.getString(timeColIndex);
                            Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                            Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                            String info = markersCursor.getString(infoColIndex);
                            String onMap = markersCursor.getString(onMapColIndex);

                            float hueColor = 0;
                            int color = 0;
                            Cursor groupCursor = db.getFieldData("Groups", "operation", operationID);
                            if(groupCursor.moveToFirst()) {
                                do {
                                    if(groupCursor.getString(groupCursor.getColumnIndex("name")).equals(groupName)) {
                                        hueColor = Float.parseFloat(groupCursor.getString(groupCursor.getColumnIndex("markerColor")));
                                        color = Integer.parseInt(groupCursor.getString(groupCursor.getColumnIndex("color")));
                                        break;
                                    }
                                } while (groupCursor.moveToNext());
                            } else {
                                Log.d(LOG_TAG, "Group cursor marker creation 0 rows");
                            }
                            groupCursor.close();

                            MarkerOptions markerOptions;
                            LatLng currentMarkerPosition = new LatLng(lat, lng);

                            if(!info.equals("-")) {
                                markerOptions = new MarkerOptions()
                                        .position(currentMarkerPosition)
                                        .title(groupName + " " + time)
                                        .snippet(info + " " + memberNumber)
                                        .draggable(false)
                                        .icon(BitmapDescriptorFactory.defaultMarker(hueColor));
                            } else {
                                markerOptions = new MarkerOptions()
                                        .position(currentMarkerPosition)
                                        .title(groupName + " " + time)
                                        .snippet(memberNumber)
                                        .draggable(false)
                                        .icon(BitmapDescriptorFactory.defaultMarker(hueColor));
                            }

                            //Log.d(LOG_TAG, "Updater before markers.put " + markers.size());
                            markers.put(Integer.parseInt(id), mMap.addMarker(markerOptions));
                            db.modifyFieldData("Markers", operationID, "_id", Integer.parseInt(id), "onMap", "true");
                            fillList();
                            adapter = new CustomAdapter(context, arrayParents);
                            expListGroups.setAdapter(new CustomAdapter(context, arrayParents));
                            //Log.d(LOG_TAG, "Updater after markers.put " + markers.size());

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
                                    if(prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("groupName")).equals(groupName)) {
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
                                        //if(curDate.after(prevDate) && (prevOnMap.equals("true"))) {
                                        if(curDate.after(prevDate)) {
                                            Double prevLat = Double.parseDouble(prevMarkerCursor.getString(latColIndex));
                                            Double prevLng = Double.parseDouble(prevMarkerCursor.getString(lngColIndex));
                                            prevMarkerPosition = new LatLng(prevLat, prevLng);
                                        }
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
                                .fillColor(color)
                                .strokeColor(color)
                                .radius(0.5));
                                PolylineOptions polylineOptions = new PolylineOptions()
                                        .add(prevMarkerPosition, currentMarkerPosition)
                                        .width(5)
                                        .color(color);
                                mMap.addPolyline(polylineOptions);
                            }

                        }
                    } while(markersCursor.moveToNext());
                } else {
                    Log.d(LOG_TAG, "Groups count - 0");
                }
                markersCursor.close();

                /*
                Log.d("SMS!", smsText + " " + smsNum);

                if((!Objects.equals(smsText, null)) && (!Objects.equals(smsNum, null))) {

                    Log.d("SMS!", "SMS принято");



                    /*
                    volunteer = new MissionMapSidebarActivity.Volunteer(smsText, smsNum);
                    ArrayList<LatLng> index=new ArrayList<>();
                    for (int j=0;j<numbers.size();j++) {
                        String n = numbers.get(j);
                        for (int t=0;t<numbers.size();t++) {
                            if (n.equals(numbers.get(t))) {
                                index.add(points.get(t));
                            }
                        }
                        if (index.size()>1) {
                            PolylineOptions polylineOptions = new PolylineOptions();
                            polylineOptions.addAll(index);
                            polylineOptions
                                    .width(5)
                                    .color(color);
                            mMap.addPolyline(polylineOptions);
                        }
                        index.clear();

                    }
                    mMap.addMarker(volunteer.markOp);
                    */
                //}

                /*
                if(SMSMonitor.check()) {
                    volunteer = new Volunteer(GetSmsService.getSms_body(),GetSmsService.getSms_number());
                    ArrayList<LatLng> index=new ArrayList<>();
                    for (int j=0;j<numbers.size();j++)
                    {
                        String n = numbers.get(j);
                        for (int t=0;t<numbers.size();t++)
                        {
                            if (n.equals(numbers.get(t)))
                            {
                                index.add(points.get(t));
                            }
                        }
                        if (index.size()>1)
                        {
                            PolylineOptions polylineOptions = new PolylineOptions();
                            polylineOptions.addAll(index);
                            polylineOptions
                                    .width(5)
                                    .color(color);
                            mMap.addPolyline(polylineOptions);

                        }
                        index.clear();

                    }

                    mMap.addMarker(volunteer.markOp);
                }
                */
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        void stop() {
            this.stop();
        }
    }

    public void drawRoutes() {

        markers.clear();
        for(int i = 0; i < circles.size(); i++)
            circles.get(i).remove();
        circles.clear();
        for(int i = 0; i < polylines.size(); i++)
            polylines.get(i).remove();
        polylines.clear();

        Cursor markersCursor = db.getFieldData("Markers", "operation", operationID);
        if(markersCursor.moveToFirst()) {
            int onMapColIndex = markersCursor.getColumnIndex("onMap");
            int idColIndex = markersCursor.getColumnIndex("_id");
            int groupNameColIndex = markersCursor.getColumnIndex("groupName");
            int memberNumberColIndex = markersCursor.getColumnIndex("memberNumber");
            int timeColIndex = markersCursor.getColumnIndex("time");
            int latColIndex = markersCursor.getColumnIndex("lat");
            int lngColIndex = markersCursor.getColumnIndex("lng");
            int infoColIndex = markersCursor.getColumnIndex("info");
            do {
                String id = markersCursor.getString(idColIndex);
                String groupName = markersCursor.getString(groupNameColIndex);
                String memberNumber = markersCursor.getString(memberNumberColIndex);
                String time = markersCursor.getString(timeColIndex);
                Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                String info = markersCursor.getString(infoColIndex);
                String onMap = markersCursor.getString(onMapColIndex);
                Log.d(LOG_TAG, id + " " + groupName + " " + time + " " + onMap);
            } while (markersCursor.moveToNext());
        } else {

        }
        markersCursor.close();

        markersCursor = db.getFieldData("Markers", "operation", operationID);
        if(markersCursor.moveToFirst()) {
            int onMapColIndex = markersCursor.getColumnIndex("onMap");
            int idColIndex = markersCursor.getColumnIndex("_id");
            int groupNameColIndex = markersCursor.getColumnIndex("groupName");
            int memberNumberColIndex = markersCursor.getColumnIndex("memberNumber");
            int timeColIndex = markersCursor.getColumnIndex("time");
            int latColIndex = markersCursor.getColumnIndex("lat");
            int lngColIndex = markersCursor.getColumnIndex("lng");
            int infoColIndex = markersCursor.getColumnIndex("info");

            do {
                String id = markersCursor.getString(idColIndex);
                String groupName = markersCursor.getString(groupNameColIndex);
                String memberNumber = markersCursor.getString(memberNumberColIndex);
                String time = markersCursor.getString(timeColIndex);
                Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                String info = markersCursor.getString(infoColIndex);
                String onMap = markersCursor.getString(onMapColIndex);

                float hueColor = 0;
                int color = 0;
                Cursor groupCursor = db.getFieldData("Groups", "operation", operationID);
                if(groupCursor.moveToFirst()) {
                    do {
                        if(groupCursor.getString(groupCursor.getColumnIndex("name")).equals(groupName)) {
                            hueColor = Float.parseFloat(groupCursor.getString(groupCursor.getColumnIndex("markerColor")));
                            color = Integer.parseInt(groupCursor.getString(groupCursor.getColumnIndex("color")));
                            break;
                        }
                    } while (groupCursor.moveToNext());
                } else {
                    Log.d(LOG_TAG, "Group cursor marker creation 0 rows");
                }
                groupCursor.close();

                MarkerOptions markerOptions;
                LatLng currentMarkerPosition = new LatLng(lat, lng);

                if(!info.equals("-")) {
                    markerOptions = new MarkerOptions()
                            .position(currentMarkerPosition)
                            .title(groupName + " " + time)
                            .snippet(info + " " + memberNumber)
                            .draggable(false)
                            .icon(BitmapDescriptorFactory.defaultMarker(hueColor));
                } else {
                    markerOptions = new MarkerOptions()
                            .position(currentMarkerPosition)
                            .title(groupName + " " + time)
                            .snippet(memberNumber)
                            .draggable(false)
                            .icon(BitmapDescriptorFactory.defaultMarker(hueColor));
                }

                //Log.d(LOG_TAG, "Updater before markers.put " + markers.get(groupName).size());
                markers.put(Integer.parseInt(id), mMap.addMarker(markerOptions));
                db.modifyFieldData("Markers", operationID, "_id", Integer.parseInt(id), "onMap", "true");
                //fillList();
                //adapter = new CustomAdapter(context, arrayParents);
                //expListGroups.setAdapter(new CustomAdapter(context, arrayParents));
                //Log.d(LOG_TAG, "Updater after markers.put " + markers.size());

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
                        if(prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("groupName")).equals(groupName)) {
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
                        }
                    } while(prevMarkerCursor.moveToPrevious() && (prevMarkerPosition == null));
                } else {
                    Log.d(LOG_TAG, "prevMarkerCursor 0");
                }
                prevMarkerCursor.close();

                if(prevMarkerPosition != null) {
                    if (markers.get(prevId) != null)
                        markers.get(prevId).setVisible(false);
                    circles.add(mMap.addCircle(new CircleOptions()
                            .center(prevMarkerPosition)
                            .clickable(false)
                            .fillColor(color)
                            .strokeColor(color)
                            .radius(0.5)));
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .add(prevMarkerPosition, currentMarkerPosition)
                            .width(5)
                            .color(color);
                    polylines.add(mMap.addPolyline(polylineOptions));
                }
            } while(markersCursor.moveToNext());
        } else {
            Log.d(LOG_TAG, "Groups count - 0");
        }
        markersCursor.close();
    }

    /*
    public void drawRoutes() {

        markers.clear();
        Cursor markersCursor = db.getFieldData("Markers", "operation", operationID);
        if(markersCursor.moveToFirst()) {
            int onMapColIndex = markersCursor.getColumnIndex("onMap");
            int idColIndex = markersCursor.getColumnIndex("_id");
            int groupNameColIndex = markersCursor.getColumnIndex("groupName");
            int memberNumberColIndex = markersCursor.getColumnIndex("memberNumber");
            int timeColIndex = markersCursor.getColumnIndex("time");
            int latColIndex = markersCursor.getColumnIndex("lat");
            int lngColIndex = markersCursor.getColumnIndex("lng");
            int infoColIndex = markersCursor.getColumnIndex("info");

            do {
                String id = markersCursor.getString(idColIndex);
                String groupName = markersCursor.getString(groupNameColIndex);
                String memberNumber = markersCursor.getString(memberNumberColIndex);
                String time = markersCursor.getString(timeColIndex);
                Double lat = Double.parseDouble(markersCursor.getString(latColIndex));
                Double lng = Double.parseDouble(markersCursor.getString(lngColIndex));
                String info = markersCursor.getString(infoColIndex);
                String onMap = markersCursor.getString(onMapColIndex);

                float hueColor = 0;
                int color = 0;
                Cursor groupCursor = db.getFieldData("Groups", "operation", operationID);
                if(groupCursor.moveToFirst()) {
                    do {
                        if(groupCursor.getString(groupCursor.getColumnIndex("name")).equals(groupName)) {
                            hueColor = Float.parseFloat(groupCursor.getString(groupCursor.getColumnIndex("markerColor")));
                            color = Integer.parseInt(groupCursor.getString(groupCursor.getColumnIndex("color")));
                            break;
                        }
                    } while (groupCursor.moveToNext());
                } else {
                    Log.d(LOG_TAG, "Group cursor marker creation 0 rows");
                }
                groupCursor.close();

                MarkerOptions markerOptions;
                LatLng currentMarkerPosition = new LatLng(lat, lng);

                if(!info.equals("-")) {
                    markerOptions = new MarkerOptions()
                            .position(currentMarkerPosition)
                            .title(time)
                            .snippet(info + " " + memberNumber)
                            .draggable(false)
                            .icon(BitmapDescriptorFactory.defaultMarker(hueColor));
                } else {
                    markerOptions = new MarkerOptions()
                            .position(currentMarkerPosition)
                            .title(time)
                            .snippet(memberNumber)
                            .draggable(false)
                            .icon(BitmapDescriptorFactory.defaultMarker(hueColor));
                }

                Log.d(LOG_TAG, "Updater before markers.put " + markers.size());
                markers.put(Integer.parseInt(id), mMap.addMarker(markerOptions));
                db.modifyFieldData("Markers", operationID, "_id", Integer.parseInt(id), "onMap", "true");
                //fillList();
                //adapter = new CustomAdapter(context, arrayParents);
                //expListGroups.setAdapter(new CustomAdapter(context, arrayParents));
                Log.d(LOG_TAG, "Updater after markers.put " + markers.size());

                LatLng prevMarkerPosition = null;
                int prevId = 0;
                Cursor prevMarkerCursor = db.getFieldData("Markers", "operation", operationID);
                if(prevMarkerCursor.moveToLast()) {
                    do {
                        if(prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("groupName")).equals(groupName)) {
                            prevId = Integer.parseInt(prevMarkerCursor
                                    .getString(prevMarkerCursor.getColumnIndex("_id")));
                            int currentId = Integer.parseInt(id);
                            String prevOnMap = prevMarkerCursor.getString(prevMarkerCursor.getColumnIndex("onMap"));
                            if((currentId > prevId) && (prevOnMap.equals("true"))) {
                                Double prevLat = Double.parseDouble(prevMarkerCursor.getString(latColIndex));
                                Double prevLng = Double.parseDouble(prevMarkerCursor.getString(lngColIndex));
                                prevMarkerPosition = new LatLng(prevLat, prevLng);
                            }
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
                            .fillColor(color)
                            .strokeColor(color)
                            .radius(0.5));
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .add(prevMarkerPosition, currentMarkerPosition)
                            .width(5)
                            .color(color);
                    mMap.addPolyline(polylineOptions);
                }
            } while(markersCursor.moveToNext());
        } else {
            Log.d(LOG_TAG, "Groups count - 0");
        }
        markersCursor.close();
    }
    */

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

        LatLng center = new LatLng(centerLat, centerLng);
        centerCoords.add(center);
        drawNet(centerCoords);
        sortNodes();
        drawNodes(Color.BLACK);
        fillZones();
        drawRoutes();
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(center));
        float zoomLevel = 11.5f;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,zoomLevel));

        /*
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                final LatLng choosenLatLng = latLng;

                AlertDialog.Builder builder = new AlertDialog.Builder(MissionMapSidebarActivity.this);


                final int[] checked = {0};
                builder.setSingleChoiceItems(groupsString, checked[0], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checked[0] = which;
                    }
                });


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
        */
    }

    public void fillZones() {
        groups = new ArrayList<>();
        groupsZones = new ArrayList<>();
        groupsCount = 0;
        Cursor groupsCursor = db.getFieldData("Groups", "operation", operationID);
        if(groupsCursor.moveToFirst()) {
            int groupNameColIndex = groupsCursor.getColumnIndex("name");
            do {
                groupsCount++;
                String groupName = groupsCursor.getString(groupNameColIndex);
                groups.add(new Group(groupName));
            } while(groupsCursor.moveToNext());
        } else {
            Log.d(LOG_TAG, "Groups count - 0");
        }
        groupsCursor.close();
        Log.d(LOG_TAG, "Groups count: " + groupsCount);

        //fillColors(groupsCount);
        colors = new Integer[groupsCount];

        for(int i = 0; i < groups.size(); i++) {
            String groupName = groups.get(i).groupName;
            String zone = "";
            Cursor cursorOperation = db.getFieldData("Groups", "operation", operationID);
            if(cursorOperation.moveToFirst()) {
                int nameColIndex = cursorOperation.getColumnIndex("name");
                do {
                    if(cursorOperation.getString(nameColIndex).equals(groupName)) {
                        zone = cursorOperation.getString(cursorOperation.getColumnIndex("zone"));
                        colors[i] = Integer.parseInt(cursorOperation.getString(cursorOperation.getColumnIndex("color")));
                    }
                } while (cursorOperation.moveToNext());
            } else
                Log.d(LOG_TAG, "cursorOperation - 0 rows");
            cursorOperation.close();

            Log.d(LOG_TAG, "MissionMap Group " + groupName + " zone = " + zone);

            if(zone != null) {
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
                    String hexColor = "#5f" + Integer.toHexString(colors[i]).substring(2);
                    groups.get(i).addZone(new PolygonOptions()
                            .strokeColor(Color.WHITE)
                            .strokeWidth(0)
                            .fillColor(Color.parseColor(hexColor))
                            .geodesic(true)
                            .add(new LatLng(firstLat, firstLng),
                                    new LatLng(firstLat, secondLng),
                                    new LatLng(secondLat, secondLng),
                                    new LatLng(secondLat, firstLng)));


                    /*
                    String[] latlng = zones[k].split("\\|");
                    int j = 0;
                    while (j < latlng.length) {
                        Log.d(LOG_TAG, "Split zone | " + latlng[j]);
                        String regexp = "\\d{2}\\.\\d{4,20}";
                        Pattern pattern = Pattern.compile(regexp);
                        Matcher matcher = pattern.matcher(latlng[j]);
                        if (matcher.find()) {
                            Log.d(LOG_TAG, "Fill color is " + Integer.toHexString(colors[i]));
                            //String hexColor = "#0x4" + String.format("%06X", (0xFFFFFF & colors[i]));
                            String hexColor = "#5f" + Integer.toHexString(colors[i]).substring(2);
                            PolygonOptions polygon = new PolygonOptions()
                                    .strokeColor(Color.WHITE)
                                    .fillColor(Color.parseColor(hexColor))
                                    .strokeWidth(0)
                                    .geodesic(true)
                                    .add(new LatLng(Double.parseDouble(latlng[j]), Double.parseDouble(latlng[j + 1])),
                                            new LatLng(Double.parseDouble(latlng[j + 2]), Double.parseDouble(latlng[j + 3])),
                                            new LatLng(Double.parseDouble(latlng[j + 4]), Double.parseDouble(latlng[j + 5])),
                                            new LatLng(Double.parseDouble(latlng[j + 6]), Double.parseDouble(latlng[j + 7])));
                            groups.get(i).addZone(polygon);

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
                    int markerLatColIndex = childrenCursor.getColumnIndex("lat");
                    int markerLngColIndex = childrenCursor.getColumnIndex("lng");
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
                            String markerLat = childrenCursor.getString(markerLatColIndex);
                            String markerLng = childrenCursor.getString(markerLngColIndex);
                            String markerInfo = childrenCursor.getString(markerInfoColIndex);
                            arrayChildren.add(markerTime + " | " + markerPhone + " | " + markerLat + " | " + markerLng + " | " + markerInfo);
                        }
                    } while (childrenCursor.moveToPrevious());
                } else
                    Log.d(LOG_TAG, "children - 0 rows");
                childrenCursor.close();

                /*
                //Cursor childrenCursorOperation = db.getFieldData("Members", "operation", operationID);
                Cursor childrenCursor = db.getFieldData("Members", "operation", operationID);

                if(childrenCursor.moveToFirst()) {

                    int memberIdColIndex = childrenCursor.getColumnIndex("_id");
                    int memberNameColIndex = childrenCursor.getColumnIndex("name");
                    int memberInfoColIndex = childrenCursor.getColumnIndex("info");
                    int memberGroupColIndex = childrenCursor.getColumnIndex("groupName");
                    Log.d(LOG_TAG, Integer.toString(memberNameColIndex) + " " + Integer.toString(memberInfoColIndex) +
                            " " + Integer.toString(memberGroupColIndex));
                    do {
                        //String memberName = childrenCursor.getString(memberNameColIndex);
                        //Log.d(LOG_TAG, memberName);
                        String memberGroup = childrenCursor.getString(memberGroupColIndex);
                        //Log.d(LOG_TAG, memberGroup);
                        if(memberGroup.equals(groupName)) {
                            String memberId = childrenCursor.getString(memberIdColIndex);
                            String memberName = childrenCursor.getString(memberNameColIndex);
                            String memberInfo = childrenCursor.getString(memberInfoColIndex);
                            arrayChildren.add(memberId + ") " + memberName + " - " + memberInfo);
                        }
                    } while (childrenCursor.moveToNext());
                } else
                    Log.d(LOG_TAG, "children - 0 rows");
                childrenCursor.close();
                */

                parent.setArrayChildren(arrayChildren);
                arrayParents.add(parent);

                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (groupCursor.moveToNext());
        } else
            Log.d(LOG_TAG, "group - 0 rows");
        groupCursor.close();
    }

    /*
    public void fillColors(int count) {
        colors = new Integer[count];
        for (int i = 0; i < 360; i += 360 / count) {
            float[] hsv = new float[3];
            hsv[0] = i;
            hsv[1] = (float) Math.random();
            hsv[2] = (float) Math.random();
            colors[i] = Color.HSVToColor(hsv);
        }
    }
    */

    /*
    void delegateZone(LatLng latLng, String groupName) {
        double minFirstLatDistance = Double.MAX_VALUE;
        double minFirstLngDistance = Double.MAX_VALUE;
        double minSecondLatDistance = Double.MAX_VALUE;
        double minSecondLngDistance = Double.MAX_VALUE;
        double lat = latLng.latitude;
        double lng = latLng.longitude;
        int nearFirstLatPos = 0;
        int nearFirstLngPos = 0;
        int nearSecondLatPos = 0;
        int nearSecondLngPos = 0;
        for(int i = 0; i < latNodes.size(); i++) {
            if(minFirstLatDistance >= Math.abs(lat - latNodes.get(i))) {
                minFirstLatDistance = Math.abs(lat - latNodes.get(i));
                nearFirstLatPos = i;
            }
        }
        for(int i = 0; i < latNodes.size(); i++) {
            if(nearFirstLatPos != i) {
                if (minSecondLatDistance >= Math.abs(lat - latNodes.get(i))) {
                    minSecondLatDistance = Math.abs(lat - latNodes.get(i));
                    nearSecondLatPos = i;
                }
            }
        }
        for(int i = 0; i < lngNodes.size(); i++) {
            if(minFirstLngDistance >= Math.abs(lng - lngNodes.get(i))) {
                minFirstLngDistance = Math.abs(lng - lngNodes.get(i));
                nearFirstLngPos = i;
            }
        }
        for(int i = 0; i < lngNodes.size(); i++) {
            if(nearFirstLngPos != i) {
                if (minSecondLngDistance >= Math.abs(lng - lngNodes.get(i))) {
                    minSecondLngDistance = Math.abs(lng - lngNodes.get(i));
                    nearSecondLngPos = i;
                }
            }
        }

        Rect boundsText = new Rect();
        Paint paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(70);
        paintText.getTextBounds(groupName, 0, groupName.length(), boundsText);
        Bitmap.Config conf = Bitmap.Config.ARGB_4444;
        Bitmap bmpText = Bitmap.createBitmap(boundsText.width()*2, boundsText.height(), conf);
        Canvas canvasText = new Canvas(bmpText);
        canvasText.drawText(groupName, canvasText.getWidth()/2, canvasText.getHeight(), paintText);

        LatLng pos;
        if(lngNodes.get(nearFirstLngPos) <= lngNodes.get(nearSecondLngPos))
            pos = new LatLng((latNodes.get(nearFirstLatPos) + latNodes.get(nearSecondLatPos)) / 2,
                    lngNodes.get(nearFirstLngPos) + 0.0001);
        else
            pos = new LatLng((latNodes.get(nearFirstLatPos) + latNodes.get(nearSecondLatPos)) / 2,
                    lngNodes.get(nearSecondLngPos) + 0.0001);
        mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bmpText))
                .transparency(0.3f)
                .position(pos, 250f, 75f));

        Log.d(LOG_TAG, "Rectangle on " + lat + " " + lng);
        Log.d(LOG_TAG, latNodes.get(nearFirstLatPos) + " " + lngNodes.get(nearFirstLngPos));
        Log.d(LOG_TAG, latNodes.get(nearFirstLatPos) + " " + lngNodes.get(nearSecondLngPos));
        Log.d(LOG_TAG, latNodes.get(nearSecondLatPos) + " " + lngNodes.get(nearFirstLngPos));
        Log.d(LOG_TAG, latNodes.get(nearSecondLatPos) + " " + lngNodes.get(nearSecondLngPos));
        mMap.addPolygon(new PolygonOptions()
                .strokeColor(Color.WHITE)
                .fillColor(0x4F00FF00)
                .strokeWidth(0)
                .geodesic(true)
                .add(new LatLng(latNodes.get(nearFirstLatPos), lngNodes.get(nearFirstLngPos) + 0.00003),
                        new LatLng(latNodes.get(nearFirstLatPos), lngNodes.get(nearSecondLngPos) + 0.00003),
                        new LatLng(latNodes.get(nearSecondLatPos), lngNodes.get(nearSecondLngPos) + 0.00003),
                        new LatLng(latNodes.get(nearSecondLatPos), lngNodes.get(nearFirstLngPos) + 0.00003)));

        String zone = latNodes.get(nearFirstLatPos) + "|" + lngNodes.get(nearFirstLngPos) + "|" +
                latNodes.get(nearFirstLatPos) + "|" + lngNodes.get(nearSecondLngPos) + "|" +
                latNodes.get(nearSecondLatPos) + "|" + lngNodes.get(nearSecondLngPos) + "|" +
                latNodes.get(nearSecondLatPos) + "|" + lngNodes.get(nearFirstLngPos) + "|";

        db.addFieldData("Groups", "name", groupName, "zone", zone);
    }
    */

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
        String alhpabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

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
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mission_map_sidebar, menu);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(MissionMapSidebarActivity.this);
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

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.startRequestSMSService) {
            enableBroadcastReceiver();
        } else if (id == R.id.stopRequestSMSService) {
            disableBroadcastReceiver();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                view = inflater.inflate(R.layout.list_item_mission_map_parent, viewGroup,false);
            }

            TextView textView = view.findViewById(R.id.list_item_text_view);
            textView.setText(getGroup(groupPosition).toString());

            final String groupName = getGroup(groupPosition).toString();

            Button btnGroupShowZones = view.findViewById(R.id.btnGroupShowZones);
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
                view = inflater.inflate(R.layout.list_item_child, viewGroup,false);
            }

            TextView textView = view.findViewById(R.id.list_item_text_child);
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
