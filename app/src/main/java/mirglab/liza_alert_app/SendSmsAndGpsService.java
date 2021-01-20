package mirglab.liza_alert_app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.splunk.mint.Mint;

import java.util.Date;
import java.util.Objects;

public class SendSmsAndGpsService extends Service {
    public SendSmsAndGpsService() {
    }

    private LocationManager locationManager;
    String smsText = "", number = "", symbl = "&", name = "Имя";
    int time;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            number = intent.getStringExtra("number");
            name = intent.getStringExtra("name");
            time = intent.getIntExtra("time", 1);

            Toast.makeText(this, "number = " + number + ", name = " + name + ", time = " + time, Toast.LENGTH_SHORT).show();

            if (Objects.equals(intent.getAction(), Constants.ACTION.STARTFOREGROUND_ACTION)) {

                Intent notificationIntent = new Intent(this, MainActivity.class);
                notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Intent resultIntent = new Intent(this, MainActivity.class);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher_lzsqr)
                        .setContentTitle("Liza Allert")
                        .setContentText("Отправка GPS координат по SMS")
                        .setContentIntent(resultPendingIntent);
                Notification notification = builder.build();
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(1024, notification);
                startForeground(1024, notification);
            }
            if (intent.getAction().equals(
                    Constants.ACTION.STOPFOREGROUND_ACTION)) {
                stopSelf();
                stopForeground(true);
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000 * 60 * time, 0, locationListener);
            } catch (java.lang.SecurityException ex) {

            }
        }
        else
        {
            Toast.makeText(this, "Для отправки координат включите GPS", Toast.LENGTH_SHORT).show();

        }
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Detroyed!", Toast.LENGTH_SHORT).show();
        locationManager.removeUpdates(locationListener);
    }

    public void smsSend(String mes) {
        SmsManager smsMgrVar = SmsManager.getDefault();
        smsMgrVar.sendTextMessage(number, null, mes, null, null);
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            smsText = formatLocation(location);
            smsSend(symbl + ";" + name + ";" + smsText);

        }

        @Override
        public void onProviderDisabled(String provider) {
            stopSelf();
            stopForeground(true);

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
        return String.format("%1$.6f;%2$.6f;%3$tT", location.getLatitude(), location.getLongitude(), new Date(location.getTime()));
    }

}
