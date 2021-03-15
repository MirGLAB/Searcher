package mirglab.searcher;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.gson.Gson;

import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static android.support.v4.app.ActivityCompat.requestPermissions;


public class V_MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        V_NoticeDialogFragment.NoticeDialogListener {

    ListView listOperations;
    V_DataBase db;
    SimpleCursorAdapter scAdapter;
    Long missionID;
    //private MessageListener subscribeMessageListener;

    private static final Gson gson = new Gson();

    //Режим подписки или публикации сообщения через Google Nearby API
    //1 - подписоваться (по умолчанию)
    //2 - публиковать
    int mode = 1;

    // благодоря этому классу мы будет разбирать данные на куски
    public Elements title;
    // то в чем будем хранить данные пока не передадим адаптеру
    public ArrayList<String> titleList = new ArrayList<String>();
    // Listview Adapter для вывода данных
    // List view
    final String LOG_TAG = "____";
    private static final int CM_DELETE_ID = 1;
    ArrayList<String> arrayMissions;
    CustomAdapter adapter;
    Context context = this;
    Button btnSearchMissions;
    AlertDialog.Builder internetDialog;
    String photoText = "";

    private static final int TTL_IN_SECONDS = 30 * 60; // Three minutes.

    // Key used in writing to and reading from SharedPreferences.
    private static final String KEY_UUID = "key_uuid";

    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to three
     * minutes in this sample.
     */
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    private static String getUUID(SharedPreferences sharedPreferences) {
        String uuid = sharedPreferences.getString(KEY_UUID, "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(KEY_UUID, uuid).apply();
        }
        return uuid;
    }

    /**
     * The entry point to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * The {@link Message} object used to broadcast information about the device to nearby devices.
     */
    private Message mPubMessage;

    /**
     * A {@link MessageListener} for processing messages from nearby devices.
     */
    private MessageListener mMessageListener;

    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public void dialogInternet() {
        //Toast.makeText(this, "Для отправки координат включите GPS", Toast.LENGTH_SHORT).show();
        if (!isOnline(context)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setMessage("Для работы приложения требуется подключение к интернету")
                    .setCancelable(false)
                    .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            /*
                            if (!isOnline(context))
                                dialogInternet();
                                */
                        }
                    })
                    .setNeutralButton("Открыть настройки", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 11);
                        }
                    });
            builder.create();
            builder.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!isOnline(context))
            dialogInternet();

        super.onCreate(savedInstanceState);

        /*
        if(!isOnline(context)) {
            internetDialog = new AlertDialog.Builder(context)
                    .setMessage("Для работы приложения требуется подключение к интернету")
                    .setCancelable(false)
                    .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setNeutralButton("Настройки", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 11);
                        }
                    })
                    .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (isOnline(context)) {
                                dialog.dismiss();
                            } else {
                                dialog.dismiss();
                                internetDialog.show();
                            }
                        }
                    });
            internetDialog.create();
            internetDialog.show();
        }
        */

        //super.onCreate(savedInstanceState);

        String firstLaunchS = CustomSharedPreferences.getDefaults("firstLaunchS", context);
        if(firstLaunchS == null) {
            //startActivityForResult(new Intent(V_MainActivity.this, V_InfoActivity.class), 12);
            startActivity(new Intent(V_MainActivity.this, V_InfoActivity.class));
        } else {

            setContentView(R.layout.v_activity_main);

            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS, Manifest.permission.ACCESS_NETWORK_STATE,
                                        Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
                }
                /*
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS, Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
                }
                 */
            }

            //this.deleteDatabase("Liza_Alert_DB");

            // открываем подключение к БД
            db = new V_DataBase(this);
            db.open();

            btnSearchMissions = (Button) findViewById(R.id.btnSearchMissions);
            btnSearchMissions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!isOnline(context))
                        dialogInternet();
                    else {
                        buildGoogleApiClient();
                    }
                }
            });

            listOperations = (ListView) findViewById(R.id.listOperations);
            registerForContextMenu(listOperations);

            fillList();
            adapter = new CustomAdapter(V_MainActivity.this, arrayMissions);
            listOperations.setAdapter(new CustomAdapter(V_MainActivity.this, arrayMissions));

            mMessageListener = new MessageListener() {
                @Override
                public void onFound(Message message) {
                    Log.d(LOG_TAG, "Message found: " + new String(message.getContent()));
                    //Toast.makeText(context, "", Toast.LENGTH_LONG).show();
                    String messageFull = new String(message.getContent());
                    //Boolean
                    if (messageFull.contains("Operation")) {
                        //Toast.makeText(context, "Получены данные новой операции", Toast.LENGTH_LONG).show();
                        messageFull = messageFull.substring("Operation ".length() + 1, messageFull.length() - 1);
                        String[] messageParts = new String[8];
                        messageParts = messageFull.split("\\|");
                        for (int j = 0; j < messageParts.length; j++)
                            Log.d(LOG_TAG, Integer.toString(j) + " - " + messageParts[j]);

                        ArrayList<String> info = new ArrayList<>();
                        info.add(messageParts[0]);
                        info.add(messageParts[1]);
                        info.add(messageParts[2]);
                        info.add(messageParts[3]);
                        info.add(messageParts[4]);
                        info.add(messageParts[5]);
                        info.add(messageParts[6]);
                        info.add(messageParts[7]);

                        db.addRec("Operations", info);

                        fillList();
                        adapter = new CustomAdapter(V_MainActivity.this, arrayMissions);
                        listOperations.setAdapter(new CustomAdapter(V_MainActivity.this, arrayMissions));
                    }
                }

                @Override
                public void onLost(Message message) {
                    Log.d(LOG_TAG, "Lost sight of message: " + new String(message.getContent()));
                }
            };

            //getSupportLoaderManager().initLoader(0, null, this);

            listOperations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(LOG_TAG, "itemClick: position = " + position + ", id = " + id);
                    missionID = id;

                    String groupName = "";
                    String groupZone = "";

                    Cursor cursor = db.getIdData("Operations", Long.toString(missionID));
                    if(cursor.moveToFirst()) {
                        groupName = cursor.getString(cursor.getColumnIndex("groupName"));
                        groupZone = cursor.getString(cursor.getColumnIndex("groupZone"));
                    }
                    cursor.close();

                    if(groupName.equals("-") || groupZone.equals("")) {
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        V_NoticeDialogFragment joDialog = new V_NoticeDialogFragment();
                        joDialog.show(fragmentManager, "dialog");
                    } else {
                        unsubscribe();
                        if(mGoogleApiClient != null)
                            mGoogleApiClient.disconnect();
                        Intent intent = new Intent(V_MainActivity.this, V_MissionActivity.class);
                        intent.putExtra("id", Long.toString(missionID));
                        startActivity(intent);
                    }

                    /*
                    Intent intent = new Intent(V_MainActivity.this, V_MissionActivity.class);
                    intent.putExtra("id", Long.toString(id));
                    startActivity(intent);
                    */
                }
            });

            //buildGoogleApiClient();

            // определение данных
            // запрос к нашему отдельному поток на выборку данны
            //new SiteParser().execute();
            // Добавляем данные для ListView
            //adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.product_name, titleList);

            //loadDB();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return false;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button

        /*
        String phone = "", name = "", region = "", userInfo = "";

        Cursor cursor = null;
        cursor = db.getAllData("User");
        if (cursor.moveToFirst()) {
            do {
                phone = cursor.getString(cursor.getColumnIndex("number"));
                name = cursor.getString(cursor.getColumnIndex("name"));
                region = cursor.getString(cursor.getColumnIndex("region"));
                userInfo = cursor.getString(cursor.getColumnIndex("info"));
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "0 rows");
        cursor.close();

        /*
        mPubMessage = new Message(gson.toJson( phone +"|" + name + "|" + region +
                "|" + userInfo).getBytes(Charset.forName("UTF-8")));*/
        //mode = 2;
        //buildGoogleApiClient();

        unsubscribe();
        if(mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
        Intent intent = new Intent(V_MainActivity.this, V_MissionActivity.class);
        intent.putExtra("id", Long.toString(missionID));
        startActivity(intent);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.v_menu_main, menu);
        return true;
    }

    public void onClickSettings(MenuItem item) {
        startActivity(new Intent(V_MainActivity.this, V_SettingsActivity.class));
    }

    public void onClickinfo(MenuItem item)
    {
        Intent intent = new Intent(V_MainActivity.this, V_InfoActivity.class);
        startActivityForResult(intent, 12);
    }

    public void onClickLocationSettings(MenuItem item)
    {
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    /**
     * Builds {@link GoogleApiClient}, enabling automatic lifecycle management using
     * {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity,
     * int, GoogleApiClient.OnConnectionFailedListener)}. I.e., GoogleApiClient connects in
     * {@link AppCompatActivity#onStart}, or if onStart() has already happened, it connects
     * immediately, and disconnects automatically in {@link AppCompatActivity#onStop}.
     */
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

    private void subscribe() {
        Log.i(LOG_TAG, "Subscribing");
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.d(LOG_TAG, "No longer subscribing");
                        //Toast.makeText(context, "", Toast.LENGTH_LONG).show();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(LOG_TAG, "Subscribed successfully.");
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
                            Log.d(LOG_TAG,"Could not subscribe, status = " + status);
                        }
                    }
                });
    }

    private void unsubscribe() {
        Log.d(LOG_TAG, "Unsubscribing.");
        Nearby.getMessagesClient(this).unsubscribe(mMessageListener);
    }

    /*
    private void publish() {
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

        Nearby.Messages.publish(mGoogleApiClient, mPubMessage, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(LOG_TAG, "Published successfully.");
                        } else {
                            Log.d(LOG_TAG,"Could not publish, status = " + status);
                        }
                    }
                });
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == 11) {
            if(!isOnline(context))
                dialogInternet();
        }
        if(resultCode == 12) {
            Toast.makeText(context, "Данные сохранены", Toast.LENGTH_LONG).show();
            CustomSharedPreferences.setDefaults("firstLaunchS", "false", context);
            finish();
            Intent newIntent = new Intent(V_MainActivity.this, V_MainActivity.class);
            startActivity(newIntent);
        }
        if(resultCode == 22) {
            //Toast.makeText(context, "Данные сохранены", Toast.LENGTH_LONG).show();
            //CustomSharedPreferences.setDefaults("firstLaunch", "false", context);
            CustomSharedPreferences.setDefaults("mode", "Coordinator", context);
            finish();
            Intent newIntent = new Intent(V_MainActivity.this, MainActivity.class);
            startActivity(newIntent);
        }
        //Log.d(LOG_TAG, "Here we are");
        // получаем новый курсор с данными
        //getSupportLoaderManager().getLoader(0).forceLoad();
        //loadDB();
        //listOperations.
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CM_DELETE_ID, 0, R.string.delete_record);
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == CM_DELETE_ID) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            Log.d(LOG_TAG, adapter.getItem(info.position));
            db.deleteFiledData("Operations", "_id", Long.toString(adapter.getItemId(info.position)));
            fillList();
            adapter = new CustomAdapter(V_MainActivity.this, arrayMissions);
            listOperations.setAdapter(new CustomAdapter(V_MainActivity.this, arrayMissions));
            buildGoogleApiClient();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    public class CustomAdapter extends BaseAdapter {
        Context context;
        LayoutInflater lInflater;
        ArrayList<String> list;

        CustomAdapter(Context context, ArrayList<String> objects) {
            this.context = context;
            list = objects;
            lInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        // кол-во элементов
        @Override
        public int getCount() {
            return list.size();
        }

        // элемент по позиции
        @Override
        public String getItem(int position) {
            return list.get(position);
        }

        // id по позиции
        @Override
        public long getItemId(int position) {
            long id = 0;
            String item = getItem(position);
            Log.d(LOG_TAG, item.substring(item.indexOf(": ") + 2));
            Cursor cursor = db.getFieldData("Operations", "info", item.substring(
                    item.indexOf(": ") + 2));
            if(cursor.moveToFirst()) {
                // определяем номера столбцов по имени в выборке
                int idColIndex = cursor.getColumnIndex("_id");
                do {
                    Log.d(LOG_TAG, cursor.getString(idColIndex));
                    id = Long.parseLong(cursor.getString(idColIndex));
                } while (cursor.moveToNext());
            } else
                Log.d(LOG_TAG, "Operations - 0 rows");
            cursor.close();
            return id;
        }

        // пункт списка
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // используем созданные, но не используемые view
            View view = convertView;
            if (view == null) {
                view = lInflater.inflate(R.layout.v_list_mission, parent, false);
            }

            TextView textView = (TextView) view.findViewById(R.id.list_operation_text_view);
            textView.setText(getItem(position).toString());

            return view;
        }
    }

    public void fillList()  {
        arrayMissions = new ArrayList<String>();
        Cursor cursor = db.getAllData("Operations");
        if(cursor.moveToFirst()) {

            // определяем номера столбцов по имени в выборке
            int dateColIndex = cursor.getColumnIndex("date");
            int infoColIndex = cursor.getColumnIndex("info");

            do {
                String date = cursor.getString(dateColIndex);
                String info = cursor.getString(infoColIndex);
                arrayMissions.add(date + ": " + info);
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "Operations - 0 rows");
        cursor.close();
    }

    @Override
    public void onStop() {
        if(mPubMessage != null)
            Nearby.getMessagesClient(this).unpublish(mPubMessage);
        if(mMessageListener != null)
            Nearby.getMessagesClient(this).unsubscribe(mMessageListener);
        super.onStop();
        //Nearby.Messages.unpublish(mGoogleApiClient, mPubMessage);
        //super.onStop();
    }

    @Override
    public void onResume() {
        if(!isOnline(context))
            dialogInternet();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if(mPubMessage != null)
            Nearby.getMessagesClient(this).unpublish(mPubMessage);
        if(mMessageListener != null)
            Nearby.getMessagesClient(this).unsubscribe(mMessageListener);
        // закрываем подключение при выходе
        if(db != null)
            db.close();
        super.onDestroy();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        return new CustomCursorLoader(this, db);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        scAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    static class CustomCursorLoader extends CursorLoader {

        V_DataBase db;

        public CustomCursorLoader(Context context, V_DataBase db) {
            super(context);
            this.db = db;
        }

        @Override
        public Cursor loadInBackground() {
            Cursor cursor = db.getAllData("Operations");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return cursor;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(LOG_TAG, "GoogleApiClient connected");
        Toast.makeText(context, "Данные операций принимаются", Toast.LENGTH_LONG).show();
        subscribe();
        /*
        switch (mode) {
            case 1:
                subscribe();
                break;
            case 2:
                publish();
                break;
            default:
                break;
        }*/
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
}


/** А вот и внутрений класс который делает запросы, если вы не читали статьи у меня в блоге про отдельные
 * потоки советую почитать */
/*
public class SiteParser extends AsyncTask<String, Void, String> {

    // Метод выполняющий запрос в фоне, в версиях выше 4 андроида, запросы в главном потоке выполнять
    // нельзя, поэтому все что вам нужно выполнять - выносите в отдельный тред
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected String doInBackground(String... arg) {

        Document doc;
        try {
            //String url = "http://lizaalert.org/forum/";
            String url = "http://lizaalert.org/forum/viewforum.php?f=276";

            doc = Jsoup.connect(url)
                    .data("query", "Java")
                    .userAgent("Mozilla")
                    .cookie("auth", "token")
                    .timeout(3000)
                    .post();

            ArrayList<St
            Elements elems = doc.getElementsByClass("row bg1");
            for (Element elem : elems) {
                String lostForumFIO = elem.getElementsByAttribute("title").text();
                if(lostForumFIO.contains("Пропал")) {
                    String lostForumText = elem.getElementsByAttribute("title").html();
                    String[] strs = new String[100];
                    strs = lostForumText.split("&amp;");
                    String lostForumURL = "http://lizaalert.org/forum" + strs[0].substring(10) + "&" +strs[1];
                    String lostForumID = strs[1];
                    String[] var = new String[3];
                    var[0] = lostForumFIO;
                    var[1] = lostForumURL;
                    var[2] = lostForumID;
                    lostForumInfo.add(var);
                }
            }

            elems = doc.getElementsByClass("row bg2");
            for (Element elem : elems) {
                String lostForumFIO = elem.getElementsByAttribute("title").text();
                if(lostForumFIO.contains("Пропал")) {
                    String lostForumText = elem.getElementsByAttribute("title").html();
                    String[] strs = new String[100];
                    strs = lostForumText.split("&amp;");
                    String lostForumURL = "http://lizaalert.org/forum" + strs[0].substring(10) + "&" +strs[1];
                    String lostForumID = strs[1];
                    String[] var = new String[3];
                    var[0] = lostForumFIO;
                    var[1] = lostForumURL;
                    var[2] = lostForumID;
                    lostForumInfo.add(var);
                }
            }

            Document lostDoc;
            String lostUrl;

            for(int i = 0; i < lostForumInfo.size(); i++) {

                lostUrl = lostForumInfo.get(i)[1];
                lostDoc = Jsoup.connect(lostUrl)
                        .data("query", "Java")
                        .userAgent("Mozilla")
                        .cookie("auth", "token")
                        .timeout(3000)
                        .post();

                String lostID = (lostForumInfo.get(i)[2]).substring(2);

                elems.clear();

                Log.d(LOG_TAG, Integer.toString(i));
                Log.d(LOG_TAG, lostID);
                Log.d(LOG_TAG, lostDoc.getElementsByClass("content").text());

                String forumDesc = lostDoc.getElementsByClass("content").text();

                String regexp = "(Координаты|КООРДИНАТЫ|координаты).{0,10}\\d{2}\\.\\d{4,6}.{0,5}\\d{2}\\.\\d{4,6}";
                Pattern pattern = Pattern.compile(regexp);
                Matcher matcher = pattern.matcher(forumDesc);
                String forumCoords;
                if(matcher.find()){
                    forumCoords = matcher.group();
                    //Log.d(LOG_TAG, matcher.group());
                }

                String desc;
                if(forumDesc.contains("---"))
                    desc = forumDesc.substring(0, forumDesc.indexOf("---"));
                else
                    desc = forumDesc;

                Log.d(LOG_TAG, desc);
                //получена строка с описанием пропавшего, далее происходит обработка строки
                //и приведение ее к подходящему виду

                String lowDesc = desc.toLowerCase();
                ArrayList<Integer> textPos = new ArrayList<>();
                if(lowDesc.contains("табл"))
                    textPos.add(lowDesc.indexOf("табл"));
                if(lowDesc.contains("ориентировк"))
                    textPos.add(lowDesc.indexOf("ориентировк"));
                if(lowDesc.contains("карт"))
                    textPos.add(lowDesc.indexOf("карт"));
                if(lowDesc.contains("резерв"))
                    textPos.add(lowDesc.indexOf("резерв"));
                if(lowDesc.contains("готов"))
                    textPos.add(lowDesc.indexOf("готов"));
                if(lowDesc.contains("нахожусь"))
                    textPos.add(lowDesc.indexOf("нахожусь"));
                Collections.sort(textPos, new Comparator<Integer>() {
                    @Override
                    public int compare(Integer a, Integer b) {
                        return a < b ? -1 : (a > b) ? 1 : 0;
                    }
                });

                if(textPos.size() > 0)
                    desc = desc.substring(0, textPos.get(0));
                Log.d(LOG_TAG, desc);

                //далее необходимо занести полученнную инфу в бд
                ArrayList<String> info = new ArrayList<>();
                info.add(lostID);
                DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
                String date = df.format(Calendar.getInstance().getTime());
                info.add(date);
                if(desc.toLowerCase().contains("приметы")) {
                    info.add(desc.substring(0, desc.toLowerCase().indexOf("приметы")));
                    info.add(desc.substring(desc.toLowerCase().indexOf("приметы")));
                }
                else {
                    info.add(desc);
                    info.add("-");
                }
                String photosPaths = "";
                    /*
                    if(photos.size() != 0)
                        for(int i = 0; i < photos.size(); i++) {
                            photosPaths += photos.get(i) + " ";
                        }
                        */
/*
                info.add(photosPaths);
                db.addRec("Operations", info);
                //db.addRec("O");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
*/



