package mirglab.searcher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.splunk.mint.Mint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;



public class V_SendSmsAndGpsService extends Service implements GoogleApiClient.ConnectionCallbacks, LocationListener,
        GoogleApiClient.OnConnectionFailedListener {

    V_DataBase db;
    private LocationManager locationManager;
    String smsText = "", number = "";
    int time, id = 1;
    int timer = 0, timerLocal = 0;
    String LOG_TAG = "____";
    String operationID;
    String groupName;
    Context context = this;
    //Handler, запускающий обновление окна с заданным интервалом
    private Handler handler, locationHandler;
    //Runnable, который обновляет положение окна
    private Runnable updater, locationUpdater;
    Location lastLocation;
    PowerManager.WakeLock wakeLock;
    final String systemService = LOCATION_SERVICE;
    Intent serviceIntent;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    LocationCallback locationCallback;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Mint.initAndStartSession(this.getApplication(), "0ff11f04");
        locationManager = (LocationManager) context.getSystemService(systemService);
        //PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        //wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        //wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        //wakeLock.acquire(180*60*1000L /*180 minutes*/);
        context = this;
    }

    protected synchronized void buildGoogleApiClient() {
        this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Mint.initAndStartSession(this.getApplication(), "0ff11f04");
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            buildGoogleApiClient();

            db = new V_DataBase(this);
            db.open();

            operationID = intent.getStringExtra("id");
            //groupName = CustomSharedPreferences.getDefaults("groupName", context);

            serviceIntent = intent;

            Cursor cursor = db.getIdData("Operations", operationID);
            if (cursor.moveToFirst()) {
                number = cursor.getString(cursor.getColumnIndex("number"));
                groupName = cursor.getString(cursor.getColumnIndex("groupName"));
            } else {
                Log.d(LOG_TAG, "SendSMS cursor 0");
            }
            cursor.close();

            //String r = intent.getStringExtra("number");
            //name = intent.getStringExtra("name");
            timer = 60 * 1000 * intent.getIntExtra("timer", 1);
            timerLocal = 60 * 1000 * intent.getIntExtra("timerLocal", 1);

            Toast.makeText(this, "Передача координат начата", Toast.LENGTH_SHORT).show();

            if (Objects.equals(intent.getAction(), Constants.ACTION.STARTFOREGROUND_ACTION)) {

                Intent notificationIntent = new Intent(this, V_MissionMapSidebarActivity.class);
                notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                notificationIntent.putExtra("id", operationID);
                Intent resultIntent = new Intent(this, V_MissionMapSidebarActivity.class);
                resultIntent.putExtra("id", operationID);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("default",
                            "Искатель",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription("Отправка GPS-координат по СМС");
                    mNotificationManager.createNotificationChannel(channel);
                }
                /*
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "default")
                        .setSmallIcon(R.mipmap.ic_launcher_lzsqr)
                        .setContentTitle("Лиза Алерт: Волонтеры") // title for notification
                        .setContentText("Отправка GPS-координат по СМС")// message for notification
                        .setContentIntent(resultPendingIntent);

                 */
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "default")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Искатель") // title for notification
                        .setContentText("Отправка GPS-координат по СМС")// message for notification
                        .setContentIntent(resultPendingIntent);
                Notification notification = builder.build();
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(1024, notification);
                startForeground(1024, notification);
            }
            if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
                stopSelf();
                stopForeground(true);
            }

            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (this.wakeLock == null) { //**Added this
                this.wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");

            }

            if (!this.wakeLock.isHeld()) { //**Added this
                this.wakeLock.acquire();
            }

            handler = new Handler(Looper.getMainLooper());
            updater = new Updater();
            handler.post(updater);

            startLocationUpdates();

        } else {
            //dialogGPS();
            Toast.makeText(this, "Для отправки координат включите GPS", Toast.LENGTH_SHORT).show();
        }
        return START_STICKY;
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(timerLocal);
        //mLocationRequest.setFastestInterval(timerLocal);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // do work here
                onLocationChanged(locationResult.getLastLocation());
            }
        };

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, locationCallback,
                Looper.myLooper());
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Передача координат остановлена", Toast.LENGTH_SHORT).show();
        //locationManager.removeUpdates(locationListener);
        if (this.mGoogleApiClient != null) {
            this.mGoogleApiClient.unregisterConnectionCallbacks(this);
            this.mGoogleApiClient.unregisterConnectionFailedListener(this);
            this.mGoogleApiClient.disconnect();
            // Destroy the current location client
            this.mGoogleApiClient = null;
        }

        LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(locationCallback);

        handler.removeCallbacks(updater);
        //locationHandler.removeCallbacks(locationUpdater);
        if(locationManager != null)
            locationManager.removeUpdates(this);
        if (this.wakeLock.isHeld())
            this.wakeLock.release();
        this.stopForeground(true);
        if(serviceIntent != null)
            stopService(serviceIntent);
        super.onDestroy();
    }

    public void smsSend(String mes) {
        SmsManager smsMgrVar = SmsManager.getDefault();
        smsMgrVar.sendTextMessage(number, null, mes, null, null);
        Log.d(LOG_TAG, "SMS sended");
    }

    @SuppressLint("DefaultLocale")
    private String formatLocation(Location location) {
        if (location == null)
            return "";
        //return String.format("%1$.6f|%2$.6f|%3$tT", location.getLatitude(), location.getLongitude(), new Date(location.getTime()));
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date(location.getTime());
        String formatted = format.format(date);
        return String.format("%1$.6f|%2$.6f", location.getLatitude(), location.getLongitude()) + "|" + formatted;
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        String markerText = formatLocation(location);
        String[] markerTextParts = markerText.split("\\|");

        String latMarker = markerTextParts[0].replace(",", ".");
        String lngMarker = markerTextParts[1].replace(",", ".");
        String timeMarker = markerTextParts[2];

        ArrayList<String> markerInfo = new ArrayList<>(Arrays.asList(operationID, timeMarker, latMarker, lngMarker));
        db.addRec("Markers", markerInfo);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d(LOG_TAG, "onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d(LOG_TAG, "onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d(LOG_TAG, "onProviderDisabled");
        stopSelf();
        stopForeground(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    public class Updater implements Runnable  {

        @Override
        public void run() {
            //помещаем в очередь следующий цикл обновления
            Log.d(LOG_TAG, "Updater timer " + timer);
            handler.postDelayed(this, timer);
            try {
                if (lastLocation != null) {
                    smsText = formatLocation(lastLocation);
                    //Шаблон СМС-сообщения
                    //22304|55.12345|37.213445|21:31:12
                    smsSend(operationID + "|" + groupName + "|" + smsText);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}
