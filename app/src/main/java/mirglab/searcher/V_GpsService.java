package mirglab.searcher;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;



public class V_GpsService extends Service {

    public V_GpsService() {
    }

    V_DataBase db;
    private LocationManager locationManager;
    String smsText = "", number = "";
    int time,id=1;
    int timer = 0;
    String LOG_TAG = "____";
    String operationID;
    String groupName;
    final Context context = this;
    PowerManager.WakeLock wakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Mint.initAndStartSession(this.getApplication(), "0ff11f04");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyWakeLock");
        //wakeLock.acquire(180*60*1000L /*180 minutes*/);
        wakeLock.acquire();
        //context = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Mint.initAndStartSession(this.getApplication(), "0ff11f04");
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            db = new V_DataBase(this);
            db.open();

            operationID = intent.getStringExtra("id");
            //groupName = CustomSharedPreferences.getDefaults("groupName", context);

            Cursor cursor = db.getIdData("Operations", operationID);
            if(cursor.moveToFirst()) {
                number = cursor.getString(cursor.getColumnIndex("number"));
                groupName = cursor.getString(cursor.getColumnIndex("groupName"));
            } else {
                Log.d(LOG_TAG, "SendSMS cursor 0");
            }
            cursor.close();

            //String r = intent.getStringExtra("number");
            //name = intent.getStringExtra("name");
            timer = 60*1000*intent.getIntExtra("timer", 1);

            Toast.makeText(this, "Отрисовка маршрута начата", Toast.LENGTH_SHORT).show();

            if (Objects.equals(intent.getAction(), Constants.ACTION.STARTFOREGROUND_ACTION)) {

                Intent notificationIntent = new Intent(this, V_MissionMapSidebarActivity.class);
                notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                notificationIntent.putExtra("id", operationID);
                //Intent resultIntent = new Intent(this, V_MissionMapSidebarActivity.class);
                //resultIntent.putExtra("id", operationID);
                Intent resultIntent = intent;
                PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("defaultGPS",
                            "Лиза Алерт: Волонтеры",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription("Отрисовка маршрута");
                    mNotificationManager.createNotificationChannel(channel);
                }
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "defaultGPS")
                        .setSmallIcon(R.mipmap.ic_launcher_lzsqr)
                        .setContentTitle("Лиза Алерт: Волонтеры") // title for notification
                        .setContentText("Отрисовка маршрута")// message for notification
                        .setContentIntent(resultPendingIntent);
                Notification notification = builder.build();
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(1048, notification);
                startForeground(1048, notification);
            }
            if (intent.getAction().equals(
                    Constants.ACTION.STOPFOREGROUND_ACTION)) {
                stopSelf();
                stopForeground(true);
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        timer, 0, locationListener);
                //Toast.makeText(this, "Запрос координат", Toast.LENGTH_SHORT).show();
            } catch (SecurityException ex)
            {
                //Toast.makeText(this, "catch", Toast.LENGTH_SHORT).show();
            }
        } else {
            //dialogGPS();
            Toast.makeText(this, "Включите GPS", Toast.LENGTH_SHORT).show();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Отрисовка маршрута остановлена", Toast.LENGTH_SHORT).show();
        locationManager.removeUpdates(locationListener);
        wakeLock.release();
        super.onDestroy();
    }

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
            //Шаблон СМС-сообщения
            //22304|55.12345|37.213445|21:31:12
            //smsSend(operationID + "|" + groupName + "|" + smsText);

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
            Log.d(LOG_TAG, "onProviderDisabled");
            stopSelf();
            stopForeground(true);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(LOG_TAG, "onProviderEnabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(LOG_TAG, "onStatusChanged");
        }
    };

    @SuppressLint("DefaultLocale")
    private String formatLocation(Location location) {
        if (location == null)
            return "";
        return String.format("%1$.6f|%2$.6f|%3$tT", location.getLatitude(), location.getLongitude(), new Date(location.getTime()));
    }
}
