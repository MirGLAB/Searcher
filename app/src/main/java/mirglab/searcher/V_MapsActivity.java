package mirglab.searcher;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.splunk.mint.Mint;

import java.util.ArrayList;
import java.util.List;



public class V_MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String DB_NAME = "database.db";
    public static final String TABLE_PATH = "path";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_LNG = "lng";
    public static final String COLUMN_TIME = "time";
    private static final String TABLE_CREATE = "create table IF NOT EXISTS " + TABLE_PATH + "(" + COLUMN_ID + " INTEGER PRIMARY KEY , " + COLUMN_LAT + " text, "
            + COLUMN_LNG + " text, " + COLUMN_TIME + " text);";
    /* boolean bound = false;
     boolean fl=false;
     ServiceConnection sConn;
     Intent intent, smsOnMap;
     GetSmsService myServiceGet;*/
    LatLng top,bottom;
    private GoogleMap mMap;


    ArrayList<Marker> markers;
    List<LatLng> points = new ArrayList<>();
    List<String> time = new ArrayList<>();
    public int k = 0;
    double Lat;
    double Lng;
    int id = 1;
    // Random rnd = new Random();
    //int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    //интервал обновления положения всплывающего окна.
    //для плавности необходимо 60 fps, то есть 1000 ms / 60 = 16 ms между обновлениями.
    // private static final int POPUP_POSITION_REFRESH_INTERVAL = 1000;
    //Handler, запускающий обновление окна с заданным интервалом
    // private Handler handler;
    //Runnable, который обновляет положение окна
    // private Runnable updater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mint.initAndStartSession(this.getApplication(), "0ff11f04");
        // enableBroadcastReceiver();
        setContentView(R.layout.v_activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        markers = new ArrayList<>();
       /* smsOnMap = new Intent (V_MapsActivity.this, SMSMonitor.class);
        intent = new Intent(this, GetSmsService.class);
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                myServiceGet = ((GetSmsService.MyBinderGet) binder).getService();
                bound = true;
            }
            public void onServiceDisconnected(ComponentName name) {
                bound = false;
            }
        };*/
        //  handler = new Handler(Looper.getMainLooper());
        // updater = new Updater();

        //запускаем периодическое обновление
        // handler.post(updater);
    }

    public void onClickUpdateMap(View view) {
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        db.execSQL(TABLE_CREATE);
        Cursor query = db.rawQuery("SELECT * FROM path;", null);
        if (query != null) {
            if (query.moveToFirst()) {
                points.clear();
                time.clear();
                mMap.clear();
                do {
                    id = query.getInt(0);
                    Lat = Double.parseDouble(query.getString(1).replace(",", "."));
                    Lng = Double.parseDouble(query.getString(2).replace(",", "."));
                    points.add(new LatLng(Lat, Lng));
                    time.add(query.getString(3));
                    // mMap.addMarker(new MarkerOptions().position(new LatLng(Lat, Lng)).title(time));
                }
                while (query.moveToNext());
                for (int i = 0; i < points.size(); i = i + 12) {
                    mMap.addMarker(new MarkerOptions().position(points.get(i)).title(time.get(i)));
                }

                mMap.addMarker(new MarkerOptions().position(points.get(points.size() - 1)).title(time.get(time.size() - 1)));
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.addAll(points);
                polylineOptions.width(5).color(Color.RED);
                mMap.addPolyline(polylineOptions);
                query.close();
            }
        }
        db.close();
        id++;
        if (points.size() > 1) {
            bottom = SphericalUtil.computeOffset(points.get(0), 6000, 0);
            bottom = SphericalUtil.computeOffset(bottom, 6000, 90);
            top = SphericalUtil.computeOffset(points.get(0), 6000, 180);
            top = SphericalUtil.computeOffset(top, 6000, 270);
            LatLngBounds ADELAIDE = new LatLngBounds(top, bottom);
            //LatLng l=(top,bottom);

            // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0),14.0f));
            // mMap.setMaxZoomPreference(17.0f);
            // mMap.setLatLngBoundsForCameraTarget(ADELAIDE);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(ADELAIDE, 0));
            mMap.setLatLngBoundsForCameraTarget(ADELAIDE);
            mMap.setMinZoomPreference(13.0f);

            drawnet();


        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //LatLng moscow = new LatLng(55.751244, 37.618423);
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(moscow));
        //mMap.setOnInfoWindowClickListener(this);


    }

   /* @Override
    public void onBackPressed() {
        if(!getSupportFragmentManager().popBackStackImmediate()) {
            moveTaskToBack(true);
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.v_menu_maps, menu);
        return true;
    }

    public  void onClickclearpath(MenuItem item)
    {
        mMap.clear();
        points.clear();
        time.clear();
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        db.delete("path",null,null);
        db.close();
        id=1;
    }

   /* public  void onClickStartReceiveService(MenuItem item)
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
*/

   void drawnet()
   {
       draw(points.get(0),270,0);
       draw(points.get(0),0,270);
       draw(points.get(0),90,0);
       draw(points.get(0),0,90);
       draw(points.get(0),180,90);
       draw(points.get(0),90,180);
       draw(points.get(0),180,270);
       draw(points.get(0),270,180);
   }

   void draw(LatLng l, double a, double b)
    {   List<LatLng> tt1 = new ArrayList<>();
        LatLng t1,t2=l;
        for (int k = 0; k < 11; k++)
        {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(2).color(Color.BLACK);

            LatLng j = t2;
            for (int i = 0; i < 10; i++) {

                tt1.add(j);
                t1 = SphericalUtil.computeOffset(j, 500, a);
                tt1.add(t1);
                polylineOptions.addAll(tt1);

                j = t1;
                tt1.clear();
            }

            t2 = SphericalUtil.computeOffset(t2, 500, b);

            mMap.addPolyline(polylineOptions);
        }
    }
}

