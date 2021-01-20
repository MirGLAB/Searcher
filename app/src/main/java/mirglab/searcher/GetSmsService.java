package mirglab.searcher;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.splunk.mint.Mint;

public class GetSmsService extends Service {

    MyBinderGet binderGet = new MyBinderGet();
    private static String sms_body = "";
    private static String sms_from = "";
    //static boolean have_sms = false;
    public IBinder onBind(Intent arg0) {
        return binderGet;
    }

    class MyBinderGet extends Binder {
        GetSmsService getService() {
            return GetSmsService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        try
        {
            sms_body = intent.getExtras().getString("&", "");
            sms_from = intent.getExtras().getString("#", "");
        }
        catch (Exception NullPointerException)
        {

        }

        //have_sms = true;
        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");

        return START_STICKY;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    static String getSms_body() {
        String backUp = sms_body;
        sms_body = "";
        return backUp;
    }

    static String getSms_number() {
        String backUp = sms_from;
        sms_from = "";
        return backUp;
    }
}