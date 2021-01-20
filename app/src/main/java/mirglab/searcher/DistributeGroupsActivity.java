package mirglab.searcher;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;
import com.splunk.mint.Mint;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;



public class DistributeGroupsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Context context = this;
    boolean bound = false;
    ServiceConnection sConn;
    Intent intent, smsOnMap;
    GetSmsService myServiceGet;
    private GoogleMap mMap;
    //Volunteer volunteer = null;
    ArrayList<Marker> markers;
    //ArrayList<Volunteer> volunteers;
    ArrayList<String> numbers = new ArrayList<>();
    List<LatLng> points = new ArrayList<>();
    List<LatLng> centerCoords = new ArrayList<>();
    List<LatLng> nodes = new ArrayList<>();
    List<Double> latNodes = new ArrayList<>();
    List<Double> lngNodes = new ArrayList<>();
    Button btnFinishDelegation;
    public int k=0;
    Random rnd = new Random();
    int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    //интервал обновления положения всплывающего окна.
    //для плавности необходимо 60 fps, то есть 1000 ms / 60 = 16 ms между обновлениями.
    private static final int POPUP_POSITION_REFRESH_INTERVAL = 1000;
    //Handler, запускающий обновление окна с заданным интервалом
    private Handler handler;
    //Runnable, который обновляет положение окна
    private Runnable updater;
    SharedPreferences sPref;
    double centerLat, centerLng;
    DataBase db;
    final String LOG_TAG = "____";
    String operationID;
    String alhpabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    Boolean mapReady = false;
    ArrayList<Polyline> netPolylines = new ArrayList<>();
    ArrayList<GroundOverlay> nodesGroundOverlays = new ArrayList<>();
    ArrayList<String> delegatedZones = new ArrayList<>();
    Marker centerMarker;
    ArrayList<Polygon> zonesPolygons = new ArrayList<>();
    //ArrayList<Integer> colors = new ArrayList<>();
    String currentMapType = "Normal";
    int textColor = Color.BLACK;
    ArrayList<GroundOverlay> groupNames = new ArrayList<>();

    private Message messageLat;
    private Message messageLng;
    private Message messageGroupZone;
    private static final Gson gson = new Gson();
    private GoogleApiClient mGoogleApiClient;
    boolean canSendMessage = false;

    private static final int TTL_IN_SECONDS = 300 * 60; // Three minutes.
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DataBase(this);
        db.open();

        //buildGoogleApiClient();

        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
        //enableBroadcastReceiver();
        setContentView(R.layout.activity_distribute_groups);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //volunteers = new ArrayList();
        //markers = new ArrayList();

        //smsOnMap = new Intent(DistributeGroupsActivity.this, SMSMonitor.class);

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
        //updater = new Updater();
        //запускаем периодическое обновление
        //handler.post(updater);

        operationID = getIntent().getStringExtra("id");

        int newGroupCount = 0;
        if(getIntent().getStringExtra("groupCount")!=null)
            newGroupCount = Integer.parseInt(getIntent().getStringExtra("groupCount"));
        int groupCount = db.getFieldData("Groups", "operation", operationID).getCount();
        if(newGroupCount != 0) {
            if (groupCount < newGroupCount) {
                for (int i = 0; i < newGroupCount - groupCount; i++)
                    db.addRec("Groups", new ArrayList<String>(Arrays.asList(operationID, "Лиза")));
                groupCount = newGroupCount;
            } else if (groupCount > newGroupCount) {
                int delta = groupCount - newGroupCount;
                for(int i = 0; i < delta; i++) {
                    groupCount = db.getFieldData("Groups", "operation", operationID).getCount();
                    Cursor cursor = db.getFieldData("Members", "operation", operationID);
                    if (cursor.moveToFirst()) {
                        int memberGroupColIndex = cursor.getColumnIndex("groupName");
                        int memberIdColIndex = cursor.getColumnIndex("_id");
                        do {
                            if (cursor.getString(memberGroupColIndex).equals("Лиза-" + Integer.toString(groupCount))) {
                                String memberId = cursor.getString(memberIdColIndex);
                                db.modifyFieldData("Members", operationID, "_id", memberId, "groupName", "-");
                            }
                        } while (cursor.moveToNext());
                    } else
                        Log.d(LOG_TAG, "members - 0 rows");
                    cursor.close();
                    db.deleteFiledData("Groups", "_id", Integer.toString(groupCount));
                }
            }
        }

        fillColors(groupCount);
        /*
        int color;
        int j = 0;
        for (int i = 0; (i < 360)&&(j < groupCount); i += 360 / groupCount) {
            float[] hsv = new float[3];
            hsv[0] = i;
            hsv[1] = 90;
            hsv[2] = 50;
            //hsv[1] = 90 + (float) Math.random() * 10;
            //hsv[2] = 50 + (float) Math.random() * 10;
            color = Color.HSVToColor(hsv);
            db.modifyFieldData("Groups", "name", "Лиза-" + Integer.toString(j+1),
                    "color", Integer.toString(color));
            db.modifyFieldData("Groups", "name", "Лиза-" + Integer.toString(j+1),
                    "markerColor", Float.toString(hsv[0]));
            j++;
        }
        */

        btnFinishDelegation = (Button) findViewById(R.id.btnFinishDelegation);
        btnFinishDelegation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                int groupCount = db.getFieldData("Groups", "operation", operationID).getCount();
                int color;
                int j = 0;
                for (int i = 0; (i < 360)&&(j < groupCount); i += 360 / groupCount) {
                    float[] hsv = new float[3];
                    hsv[0] = i;
                    hsv[1] = 90;
                    hsv[2] = 50;
                    //hsv[1] = 90 + (float) Math.random() * 10;
                    //hsv[2] = 50 + (float) Math.random() * 10;
                    color = Color.HSVToColor(hsv);
                    db.modifyFieldData("Groups", "name", "Лиза-" + Integer.toString(j+1),
                            "color", Integer.toString(color));
                    db.modifyFieldData("Groups", "name", "Лиза-" + Integer.toString(j+1),
                            "markerColor", Float.toString(hsv[0]));
                    j++;
                }
                */

                /*
                Cursor cursor = db.getFieldData("Groups", "operation", operationID);
                if(cursor.moveToFirst()) {
                    int colorColIndex = cursor.getColumnIndex("color");
                    do {
                        db.modifyFieldData("Groups", );
                    } while (cursor.moveToNext());
                } else
                    Log.d(LOG_TAG, "Color cursor - 0 rows");
                cursor.close();
                */

                Intent intent = new Intent(DistributeGroupsActivity.this, GroupsActivity.class);
                intent.putExtra("id", operationID);
                //startActivity(intent);
                startActivityForResult(intent, 2);

                /*
                Intent intent = new Intent();
                setResult(2, intent);
                finish();
                */
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        Log.d(LOG_TAG, "onMapReady");
        mapReady = true;
        context = this;
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
        //drawNodes();
        drawNodes(Color.BLACK);
        showZones();
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(center));
        float zoomLevel = 11.5f;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,zoomLevel));

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                delegateZoneDialog(latLng);
                /*
                final LatLng choosenLatLng = latLng;

                AlertDialog.Builder builder = new AlertDialog.Builder(DistributeGroupsActivity.this);
                final String[] group = {""};

                final ArrayList<String> arrayGroups = new ArrayList<>();
                Cursor cursor = db.getFieldData("Groups", "operation", operationID);
                if(cursor.moveToFirst()) {

                    int nameColIndex = cursor.getColumnIndex("name");

                    do {
                        String groupName = cursor.getString(nameColIndex);
                        //Log.d(LOG_TAG, "What about id? " + groupName);
                        arrayGroups.add(groupName);
                    } while (cursor.moveToNext());
                } else
                    Log.d(LOG_TAG, "Groups - 0 rows");
                cursor.close();

                String[] groupsString = new String[arrayGroups.size()];
                for (int i = 0; i < arrayGroups.size(); i++)
                    groupsString[i] = arrayGroups.get(i);

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

                        group[0] = arrayGroups.get(checked[0]);
                        Log.d(LOG_TAG, "Delegate zone to " + group[0]);
                        delegateZone(choosenLatLng, group[0]);
                    }
                });

                builder.setNeutralButton("Добавить группу", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.addRec("Groups", new ArrayList<String>(Arrays.asList(operationID, "Лиза")));
                    }
                });

                // Specify the dialog is not cancelable
                builder.setCancelable(true);

                // Set a title for alert dialog
                builder.setTitle("Выбрать группу");

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
                */
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == resultCode) {
            Intent intent = new Intent();
            setResult(1, intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_distribute_groups, menu);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(DistributeGroupsActivity.this);
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
                                currentMapType = "Normal";
                                drawNet(Color.BLACK);
                                drawNodes(Color.BLACK);
                                break;
                            case 1:
                                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                currentMapType = "Satellite";
                                drawNet(Color.WHITE);
                                drawNodes(Color.WHITE);
                                break;
                            case 2:
                                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                currentMapType = "Terrain";
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

        if (id == R.id.action_show_center) {
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

        return super.onOptionsItemSelected(item);
    }

    void fillColors(int groupCount) {
        int color;
        int j = 0;
        for (int i = 0; (i < 360)&&(j < groupCount); i += 360 / groupCount) {
            float[] hsv = new float[3];
            hsv[0] = i;
            hsv[1] = 90;
            hsv[2] = 50;
            //hsv[1] = 90 + (float) Math.random() * 10;
            //hsv[2] = 50 + (float) Math.random() * 10;
            color = Color.HSVToColor(hsv);
            db.modifyFieldData("Groups", operationID, "name", "Лиза-" + Integer.toString(j+1),
                    "color", Integer.toString(color));
            db.modifyFieldData("Groups", operationID, "name", "Лиза-" + Integer.toString(j+1),
                    "markerColor", Float.toString(hsv[0]));
            j++;
            //colors.add(color);
        }
    }

    void showZones() {
        delegatedZones.clear();

        for(int i = 0; i < zonesPolygons.size(); i++)
            zonesPolygons.get(i).remove();
        zonesPolygons.clear();

        for(int i = 0; i < groupNames.size(); i++)
            groupNames.get(i).remove();
        groupNames.clear();

        ArrayList<Integer> colors = new ArrayList<>();
        ArrayList<String> stringGroupNames = new ArrayList<>();
        Cursor cursor = db.getFieldData("Groups", "operation", operationID);
        if (cursor.moveToFirst()) {
            int groupZoneColIndex = cursor.getColumnIndex("zone");
            int groupNameColIndex = cursor.getColumnIndex("name");
            do {
                if (cursor.getString(groupZoneColIndex) != null)
                    if (!cursor.getString(groupZoneColIndex).equals("")) {
                        delegatedZones.add(cursor.getString(groupZoneColIndex));
                        colors.add(Integer.parseInt(cursor.getString(cursor.getColumnIndex("color"))));
                        stringGroupNames.add(cursor.getString(groupNameColIndex));
                    }
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "members - 0 rows");
        cursor.close();

        for(int i = 0; i < delegatedZones.size(); i++) {
            String[] zones = delegatedZones.get(i).split("\\@");
            for(int j = 0; j < zones.length; j++) {
                String zone = zones[j];
                String firstNode = zone.substring(0, 3);
                String secondNode = zone.substring(3);
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

                String hexColor = "#5f" + Integer.toHexString(colors.get(i)).substring(2);
                zonesPolygons.add(mMap.addPolygon(new PolygonOptions()
                        .strokeColor(Color.WHITE)
                        .strokeWidth(0)
                        .fillColor(Color.parseColor(hexColor))
                        .geodesic(true)
                        .add(new LatLng(firstLat, firstLng + 0.00003),
                                new LatLng(firstLat, secondLng + 0.00003),
                                new LatLng(secondLat, secondLng + 0.00003),
                                new LatLng(secondLat, firstLng + 0.00003))));

                if(currentMapType.equals("Normal") || currentMapType.equals("Terrain"))
                    textColor = Color.BLACK;
                if(currentMapType.equals("Satellite"))
                    textColor = Color.WHITE;

                Rect boundsText = new Rect();
                Paint paintText = new Paint();
                paintText.setColor(textColor);
                paintText.setTextSize(70);

                String groupName = stringGroupNames.get(i);
                paintText.getTextBounds(groupName, 0, groupName.length(), boundsText);
                Bitmap.Config conf = Bitmap.Config.ARGB_4444;
                Bitmap bmpText = Bitmap.createBitmap(boundsText.width()*3, boundsText.height(), conf);
                Canvas canvasText = new Canvas(bmpText);
                canvasText.drawText(groupName, canvasText.getWidth()/2, canvasText.getHeight(), paintText);
                canvasText.drawText(groupName, canvasText.getWidth()/2, canvasText.getHeight(), paintText);

                LatLng pos;
                if(firstLng <= secondLng)
                    pos = new LatLng((firstLat + secondLat) / 2,
                            firstLng + 0.0001);
                else
                    pos = new LatLng((firstLat + secondLat) / 2,
                            secondLng + 0.0001);

                groupNames.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(bmpText))
                        .transparency(0.3f)
                        .position(pos, 250f, 75f)));
            }
        }
    }

    void removeZone(String zone) {

    }

    String createZone(LatLng latLng) {

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

        LatLng pos;
        if(lngNodes.get(nearFirstLngPos) <= lngNodes.get(nearSecondLngPos))
            pos = new LatLng((latNodes.get(nearFirstLatPos) + latNodes.get(nearSecondLatPos)) / 2,
                    lngNodes.get(nearFirstLngPos) + 0.0001);
        else
            pos = new LatLng((latNodes.get(nearFirstLatPos) + latNodes.get(nearSecondLatPos)) / 2,
                    lngNodes.get(nearSecondLngPos) + 0.0001);

        Log.d(LOG_TAG, "Rectangle on " + lat + " " + lng);
        Log.d(LOG_TAG, latNodes.get(nearFirstLatPos) + " " + lngNodes.get(nearFirstLngPos));
        Log.d(LOG_TAG, latNodes.get(nearFirstLatPos) + " " + lngNodes.get(nearSecondLngPos));
        Log.d(LOG_TAG, latNodes.get(nearSecondLatPos) + " " + lngNodes.get(nearFirstLngPos));
        Log.d(LOG_TAG, latNodes.get(nearSecondLatPos) + " " + lngNodes.get(nearSecondLngPos));

        String firstNode = "", secondNode = "", firstNumber = "", secondNumber = "";
        if(nearFirstLngPos < nearSecondLngPos) {
            firstNode += alhpabet.charAt(nearFirstLngPos);
            secondNode += alhpabet.charAt(nearSecondLngPos);
        } else {
            firstNode += alhpabet.charAt(nearSecondLngPos);
            secondNode += alhpabet.charAt(nearFirstLngPos);
        }

        if(Integer.toString(nearFirstLatPos + 1).length() < 2)
            firstNumber = "0" + (nearFirstLatPos + 1);
        else
            firstNumber = "" + (nearFirstLatPos + 1);

        if(Integer.toString(nearSecondLatPos + 1).length() < 2)
            secondNumber = "0" + (nearSecondLatPos + 1);
        else
            secondNumber = "" + (nearSecondLatPos + 1);

        if(nearFirstLatPos < nearSecondLatPos) {
            firstNode += firstNumber;
            secondNode += secondNumber;
        } else {
            firstNode += secondNumber;
            secondNode += firstNumber;
        }
        Log.d(LOG_TAG, "Delegate zone " + firstNode + " - " + secondNode);

        //String zone = "@" + firstNode + secondNode + "@";
        String zone = firstNode + secondNode;
        return zone;

        //db.addFieldData("Groups", "name", groupName, operationID, "zone", zone);

        ///messageGroupZone = new Message(gson.toJson(groupName + zone).getBytes(Charset.forName("UTF-8")));
    }

    void checkZone(final String zone) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage("Изменить распределение зоны?")
                .setCancelable(false)
                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Cursor cursor = db.getFieldData("Groups", "operation", operationID);
                        if (cursor.moveToFirst()) {
                            int groupZoneColIndex = cursor.getColumnIndex("zone");
                            int groupNameColIndex = cursor.getColumnIndex("name");
                            do {
                                if (cursor.getString(groupZoneColIndex) != null)
                                    if (!cursor.getString(groupZoneColIndex).equals(""))
                                        if (cursor.getString(groupZoneColIndex).contains(zone)) {
                                            db.removeZoneFromGroup(operationID, cursor.getString(groupNameColIndex), zone);
                                            break;
                                        }
                            } while (cursor.moveToNext());
                        } else
                            Log.d(LOG_TAG, "members - 0 rows");
                        cursor.close();
                        dialog.dismiss();
                        showZones();
                    }
                })
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create();
        builder.show();
        /*
        for(int i = 0; i < delegatedZones.size(); i++) {
            String[] zones = delegatedZones.get(i).split("\\@");
            for (int j = 0; j < zones.length; j++) {
                if(zone.equals(zones[j])) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                            .setMessage("Изменить распределение зоны?")
                            .setCancelable(false)
                            .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Cursor cursor = db.getFieldData("Groups", "operation", operationID);
                                    if (cursor.moveToFirst()) {
                                        int groupZoneColIndex = cursor.getColumnIndex("zone");
                                        int groupNameColIndex = cursor.getColumnIndex("name");
                                        do {
                                            if (cursor.getString(groupZoneColIndex) != null)
                                                if (!cursor.getString(groupZoneColIndex).equals(""))
                                                    if (cursor.getString(groupZoneColIndex).contains(zone)) {
                                                        db.removeZoneFromGroup(operationID, cursor.getString(groupNameColIndex), zone);
                                                        break;
                                                    }
                                        } while (cursor.moveToNext());
                                    } else
                                        Log.d(LOG_TAG, "members - 0 rows");
                                    cursor.close();
                                    showZones();
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.create();
                    builder.show();
                    break;
                }
            }
        }
        */
    }

    void delegateZoneDialog(final LatLng latLng) {

        final LatLng choosenLatLng = latLng;

        AlertDialog.Builder builder = new AlertDialog.Builder(DistributeGroupsActivity.this);
        final String[] group = {""};

        final ArrayList<String> arrayGroups = new ArrayList<>();
        Cursor cursor = db.getFieldData("Groups", "operation", operationID);
        if(cursor.moveToFirst()) {

            int nameColIndex = cursor.getColumnIndex("name");

            do {
                String groupName = cursor.getString(nameColIndex);
                //Log.d(LOG_TAG, "What about id? " + groupName);
                arrayGroups.add(groupName);
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "Groups - 0 rows");
        cursor.close();

        String[] groupsString = new String[arrayGroups.size()];
        for (int i = 0; i < arrayGroups.size(); i++)
            groupsString[i] = arrayGroups.get(i);

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

                group[0] = arrayGroups.get(checked[0]);
                Log.d(LOG_TAG, "Delegate zone to " + group[0]);
                delegateZone(choosenLatLng, group[0]);
            }
        });

        builder.setNeutralButton("+", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.addRec("Groups", new ArrayList<String>(Arrays.asList(operationID, "Лиза")));
                fillColors(db.getFieldData("Groups", "operation", operationID).getCount());
                dialog.dismiss();
                delegateZoneDialog(latLng);
            }
        });

        // Specify the dialog is not cancelable
        builder.setCancelable(true);

        // Set a title for alert dialog
        builder.setTitle("Выбрать группу");
        //builder.setMessage("+ добавить группу, - удалить группу");

        // Set the neutral/cancel button click listener
        builder.setNegativeButton("-", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int groupCount = db.getFieldData("Groups", "operation", operationID).getCount();
                Cursor cursor = db.getFieldData("Members", "operation", operationID);
                if(cursor.moveToFirst()) {
                    int memberGroupColIndex = cursor.getColumnIndex("groupName");
                    int memberIdColIndex = cursor.getColumnIndex("_id");
                    do {
                        if(cursor.getString(memberGroupColIndex).equals("Лиза-" + Integer.toString(groupCount))) {
                            String memberId = cursor.getString(memberIdColIndex);
                            db.modifyFieldData("Members", operationID, "_id", memberId, "groupName", "-");
                        }
                    } while (cursor.moveToNext());
                } else
                    Log.d(LOG_TAG, "members - 0 rows");
                cursor.close();
                db.deleteFiledData("Groups", "_id", Integer.toString(groupCount));

                fillColors(db.getFieldData("Groups", "operation", operationID).getCount());
                dialog.dismiss();
                delegateZoneDialog(latLng);
            }
        });

        AlertDialog dialog = builder.create();

        delegatedZones.clear();
        cursor = db.getFieldData("Groups", "operation", operationID);
        if (cursor.moveToFirst()) {
            int groupZoneColIndex = cursor.getColumnIndex("zone");
            int groupNameColIndex = cursor.getColumnIndex("name");
            do {
                if (cursor.getString(groupZoneColIndex) != null)
                    if (!cursor.getString(groupZoneColIndex).equals("")) {
                        delegatedZones.add(cursor.getString(groupZoneColIndex));
                    }
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "members - 0 rows");
        cursor.close();

        //checkZone(createZone(choosenLatLng));
        Boolean showDialog = true;
        String zone = createZone(choosenLatLng);
        for(int i = 0; i < delegatedZones.size(); i++) {
            String[] zones = delegatedZones.get(i).split("\\@");
            for (int j = 0; j < zones.length; j++)
                if (zone.equals(zones[j])) {
                    showDialog = false;
                    checkZone(zone);
                    break;
                }
        }
        // Display the alert dialog on interface
        if(showDialog)
            dialog.show();
    }

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

        if(currentMapType.equals("Normal") || currentMapType.equals("Terrain"))
            textColor = Color.BLACK;
        if(currentMapType.equals("Satellite"))
            textColor = Color.WHITE;

        //Rect boundsText = new Rect(0, 200, 400, 0);
        Rect boundsText = new Rect();
        Paint paintText = new Paint();
        paintText.setColor(textColor);
        paintText.setTextSize(70);
        //paintText.setTextScaleX(1.f);
        paintText.getTextBounds(groupName, 0, groupName.length(), boundsText);
        Bitmap.Config conf = Bitmap.Config.ARGB_4444;
        Bitmap bmpText = Bitmap.createBitmap(boundsText.width()*3, boundsText.height(), conf);
        Canvas canvasText = new Canvas(bmpText);
        canvasText.drawText(groupName, canvasText.getWidth()/2, canvasText.getHeight(), paintText);
        //canvasText.drawText(groupName, canvasText.getMaximumBitmapWidth(), canvasText.getMaximumBitmapHeight(), paintText);

        LatLng pos;
        if(lngNodes.get(nearFirstLngPos) <= lngNodes.get(nearSecondLngPos))
            pos = new LatLng((latNodes.get(nearFirstLatPos) + latNodes.get(nearSecondLatPos)) / 2,
                    lngNodes.get(nearFirstLngPos) + 0.0001);
        else
            pos = new LatLng((latNodes.get(nearFirstLatPos) + latNodes.get(nearSecondLatPos)) / 2,
                    lngNodes.get(nearSecondLngPos) + 0.0001);

        groupNames.add(mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bmpText))
                .transparency(0.5f)
                .position(pos, 250f, 75f)));

        int colorGroup = 0;
        Cursor cursorOperation = db.getFieldData("Groups", "operation", operationID);
        if(cursorOperation.moveToFirst()) {
            int nameColIndex = cursorOperation.getColumnIndex("name");
            do {
                if(cursorOperation.getString(nameColIndex).equals(groupName)) {
                    Cursor cursorGroup = db.getFieldData("Groups", "name", groupName);
                    if(cursorGroup.moveToFirst()) {
                        colorGroup = Integer.parseInt(cursorGroup.getString(cursorGroup.getColumnIndex("color")));
                    } else
                        Log.d(LOG_TAG, "cursorGroup - 0 rows");
                    cursorGroup.close();
                    break;
                }
            } while (cursorOperation.moveToNext());
        } else
            Log.d(LOG_TAG, "cursorOperation - 0 rows");
        cursorOperation.close();

        Log.d(LOG_TAG, "Rectangle on " + lat + " " + lng);
        Log.d(LOG_TAG, latNodes.get(nearFirstLatPos) + " " + lngNodes.get(nearFirstLngPos));
        Log.d(LOG_TAG, latNodes.get(nearFirstLatPos) + " " + lngNodes.get(nearSecondLngPos));
        Log.d(LOG_TAG, latNodes.get(nearSecondLatPos) + " " + lngNodes.get(nearFirstLngPos));
        Log.d(LOG_TAG, latNodes.get(nearSecondLatPos) + " " + lngNodes.get(nearSecondLngPos));
        String hexColor = "#5f" + Integer.toHexString(colorGroup).substring(2);
        zonesPolygons.add(mMap.addPolygon(new PolygonOptions()
                .strokeColor(Color.WHITE)
                .fillColor(Color.parseColor(hexColor))
                .strokeWidth(0)
                .geodesic(true)
                .add(new LatLng(latNodes.get(nearFirstLatPos), lngNodes.get(nearFirstLngPos) + 0.00003),
                        new LatLng(latNodes.get(nearFirstLatPos), lngNodes.get(nearSecondLngPos) + 0.00003),
                        new LatLng(latNodes.get(nearSecondLatPos), lngNodes.get(nearSecondLngPos) + 0.00003),
                        new LatLng(latNodes.get(nearSecondLatPos), lngNodes.get(nearFirstLngPos) + 0.00003))));

        /*
        String zone = "@" + latNodes.get(nearFirstLatPos) + "|" + lngNodes.get(nearFirstLngPos) + "|" +
                latNodes.get(nearFirstLatPos) + "|" + lngNodes.get(nearSecondLngPos) + "|" +
                latNodes.get(nearSecondLatPos) + "|" + lngNodes.get(nearSecondLngPos) + "|" +
                latNodes.get(nearSecondLatPos) + "|" + lngNodes.get(nearFirstLngPos) + "@";
                */

        String firstNode = "", secondNode = "", firstNumber = "", secondNumber = "";
        if(nearFirstLngPos < nearSecondLngPos) {
            firstNode += alhpabet.charAt(nearFirstLngPos);
            secondNode += alhpabet.charAt(nearSecondLngPos);
        } else {
            firstNode += alhpabet.charAt(nearSecondLngPos);
            secondNode += alhpabet.charAt(nearFirstLngPos);
        }

        if(Integer.toString(nearFirstLatPos + 1).length() < 2)
            firstNumber = "0" + (nearFirstLatPos + 1);
        else
            firstNumber = "" + (nearFirstLatPos + 1);

        if(Integer.toString(nearSecondLatPos + 1).length() < 2)
            secondNumber = "0" + (nearSecondLatPos + 1);
        else
            secondNumber = "" + (nearSecondLatPos + 1);

        if(nearFirstLatPos < nearSecondLatPos) {
            firstNode += firstNumber;
            secondNode += secondNumber;
        } else {
            firstNode += secondNumber;
            secondNode += firstNumber;
        }
        Log.d(LOG_TAG, "Delegate zone " + firstNode + " - " + secondNode);

        //String zone = "@" + firstNode + secondNode + "@";
        String zone = firstNode + secondNode + "@";

        db.addFieldData("Groups", "name", groupName, operationID, "zone", zone);

        messageGroupZone = new Message(gson.toJson(groupName + zone).getBytes(Charset.forName("UTF-8")));
        //if(canSendMessage)
        //    publish(messageGroupZone);
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

    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    private void publish(Message message) {
        Log.d(LOG_TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.d(LOG_TAG, "No longer publishing");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }
                }).build();

        Nearby.Messages.publish(mGoogleApiClient, message, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            //Toast.makeText(context, "Данные операции отправлены", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG, "Published successfully.");
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                    .setMessage("Не удается передать и получить данные, проверьте подключение к интернету")
                                    .setCancelable(false)
                                    .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            buildGoogleApiClient();
                                        }
                                    })
                                    .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                                        }
                                    });
                            builder.create();
                            builder.show();
                            //Toast.makeText(context, "Для отправки данных требуется подключение к интернету", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG,"Could not publish, status = " + status);
                        }
                    }
                });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //publish();
        canSendMessage = true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage("Не удается передать и получить данные, проверьте подключение к интернету")
                .setCancelable(false)
                .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        buildGoogleApiClient();
                    }
                })
                .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    }
                });
        builder.create();
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        //if(!getSupportFragmentManager().popBackStackImmediate()) {
        //    moveTaskToBack(true);
        //}
        super.onBackPressed();
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maps, menu);
        return true;
    }

    public  void onClickStartReceiveService(MenuItem item)
    {
        enableBroadcastReceiver();
    }

    public void enableBroadcastReceiver()
    {
        ComponentName receiver = new ComponentName(this, SMSMonitor.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        Toast.makeText(this, "Enabled broadcast receiver", Toast.LENGTH_SHORT).show();
    }

    public void onClickStopReceiveService(MenuItem item)
    {
        disableBroadcastReceiver();

    }

    public void disableBroadcastReceiver()
    {
        ComponentName receiver = new ComponentName(this, SMSMonitor.class);
        PackageManager pm = this.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        Toast.makeText(this, "Disabled broadcast receiver", Toast.LENGTH_SHORT).show();
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

    /*
    private class Updater implements Runnable {

        @Override
        public void run() {
            //помещаем в очередь следующий цикл обновления
            handler.postDelayed(this, POPUP_POSITION_REFRESH_INTERVAL);
            try {

                sPref = getSharedPreferences("liza_alert_sms", Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = sPref.edit();
                String smsText = sPref.getString("smsText", null);
                String smsNum = sPref.getString("smsNum", null);
                ed.remove("smsText");
                ed.remove("smsNum");
                ed.apply();
                Log.d("SMS!", smsText + " " + smsNum);

                if((!Objects.equals(smsText, null)) && (!Objects.equals(smsNum, null))) {

                    Log.d("SMS!", "SMS заебатое");

                    volunteer = new Volunteer(smsText, smsNum);
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
    /*
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }*/
}
