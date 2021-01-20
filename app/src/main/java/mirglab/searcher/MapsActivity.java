package mirglab.searcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.splunk.mint.Mint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;




public class MapsActivity extends AppCompatActivity implements GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback {

    Context context = this;
    boolean bound = false;
    ServiceConnection sConn;
    Intent intent, smsOnMap;
    GetSmsService myServiceGet;
    private GoogleMap mMap;
    Volunteer volunteer = null;
    ArrayList<Marker> markers;
    ArrayList<Volunteer> volunteers;
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
    NavigationView navigationView;
    DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DataBase(this);
        db.open();

        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
        enableBroadcastReceiver();
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        volunteers = new ArrayList();
        markers = new ArrayList();

        smsOnMap = new Intent(MapsActivity.this, SMSMonitor.class);

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

        handler = new Handler(Looper.getMainLooper());
        updater = new Updater();
        //запускаем периодическое обновление
        handler.post(updater);

        operationID = getIntent().getStringExtra("id");

        drawerLayout = findViewById(R.id.drawer_layout);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        //mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        Log.d(LOG_TAG, "onMapReady");
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
        drawNodes();
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(center));
        float zoomLevel = 11.5f;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,zoomLevel));

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                final LatLng choosenLatLng = latLng;

                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);

                /*
                final int[] checked = {0};
                builder.setSingleChoiceItems(groupsString, checked[0], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checked[0] = which;
                    }
                });
                */

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

        db.addFieldData("Groups", "name", groupName, operationID, "zone", zone);
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
            mMap.addPolyline(polylineOptions);
        }
    }

    void sortNodes() {

        Log.d(LOG_TAG, "before latNodes size: " + latNodes.size());
        Collections.sort(latNodes);
        Collections.reverse(latNodes);
        for(int i = 0; i < latNodes.size() - 1; i++) {
            while(Math.abs(latNodes.get(i) - latNodes.get(i+1)) < 0.00449) {
                latNodes.remove(i + 1);
                if(i+1 == latNodes.size())
                    break;
            }
        }
        Log.d(LOG_TAG, "after latNodes size: " + latNodes.size());

        Log.d(LOG_TAG, "before lngNodes size: " + lngNodes.size());
        Collections.sort(lngNodes);
        for(int i = 0; i < lngNodes.size() - 1; i++) {
            while(Math.abs(lngNodes.get(i) - lngNodes.get(i+1)) < 0.008) {
                lngNodes.remove(i + 1);
                if(i+1 == lngNodes.size())
                    break;
            }
        }
        Log.d(LOG_TAG, "after lngNodes size: " + lngNodes.size());
    }

    void drawNodes() {

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
                paintText.setColor(Color.WHITE);
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
                mMap.addGroundOverlay(new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(bmpTexts[i][j]))
                        .transparency(0.3f)
                        .position(pos, 250f, 75f));
            }
        }

        Log.d(LOG_TAG, "Finish creating nodes");
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        if(!getSupportFragmentManager().popBackStackImmediate()) {
            moveTaskToBack(true);
        }
    }

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
        Toast.makeText(this, "Сообщения принимаются", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Сообщения не принимаются", Toast.LENGTH_SHORT).show();
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

                    Log.d("SMS!", "SMS принято");

                    volunteer = new Volunteer(smsText, smsNum);
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
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}

