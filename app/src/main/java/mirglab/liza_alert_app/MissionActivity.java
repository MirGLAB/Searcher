package mirglab.liza_alert_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.database.Cursor;
import android.net.Uri;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.gson.Gson;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MissionActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LoaderManager.LoaderCallbacks<Cursor> {

    //Режим подписки или публикации сообщения через Google Nearby API
    //1 - подписоваться (по умолчанию)
    //2 - публиковать
    int mode = 2;

    private static final int TTL_IN_SECONDS = 300 * 60; // Three minutes.

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

    private Message mPubMessage, pubPhotoMessage;

    private MessageListener subscribeMessageListener, publishMessageListener;

    TextView lostInfo, lostDescription, missionMembers;
    ImageView lostPhoto1, lostPhoto2, lostPhoto3;
    ArrayList<ImageView> lostPhotos;
    String photosPaths;
    Button btnStartRequestService, btnCreateGroups, missionBtnGetCoords;
    ListView listMembers;
    SimpleCursorAdapter scAdapter;
    Intent intentMaps;
    EditText etLtd, etLng;
    String spFirstCreateGroupKey, spMissionContinuationKey;
    SharedPreferences sPref;
    String firstCreateGroup;
    final String LOG_TAG = "____";
    String operationID;
    DataBase db;
    LocationManager locationManager;
    LocationListener locationListener;
    LatLng centerCoords;
    boolean isImageFitToScreen;

    MessageListener messageListener;

    final Context context = this;

    private static final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        operationID = getIntent().getStringExtra("id");
        db = new DataBase(this);
        db.open();

        Log.d(LOG_TAG, "ID = " + operationID);
        String missionRestart = CustomSharedPreferences.getDefaults(operationID + "restart", context);
        String missionStarted = CustomSharedPreferences.getDefaults(operationID + "started", context);

        /*
        Intent intent = new Intent(MissionActivity.this, MissionMapSidebarActivity.class);
        intent.putExtra("id", operationID);
        startActivity(intent);
        */
        if(missionStarted == null)
            missionStarted = "";
        if(missionRestart == null)
            missionRestart = "";

        Log.d(LOG_TAG, "missionRestart " + missionRestart);
        Log.d(LOG_TAG, "missionStarted " + missionStarted);

        if ((missionStarted.equals("true"))&&(!missionRestart.equals("true"))) {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.activity_mission_prepared);

            if (mGoogleApiClient != null)
                mGoogleApiClient.disconnect();
            unsubscribe();
            unpublish();

            isImageFitToScreen = false;

            TextView latTextView = findViewById(R.id.missionLtdTextView);
            TextView lngTextView = findViewById(R.id.missionLngTextView);

            lostInfo = findViewById(R.id.lostInfo);
            lostDescription = findViewById(R.id.lostDescription);
            lostPhoto1 = findViewById(R.id.lostPhoto1);
            lostPhoto2 = findViewById(R.id.lostPhoto2);
            lostPhoto3 = findViewById(R.id.lostPhoto3);

            lostPhotos = new ArrayList<>();
            //lostPhotosUri = new ArrayList<>();

            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto1));
            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto2));
            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto3));

            Cursor cursor = null;
            cursor = db.getIdData("Operations", operationID);
            if (cursor.moveToFirst()) {

                // определяем номера столбцов по имени в выборке
                int lostInfoColIndex = cursor.getColumnIndex("info");
                int lostDescriptionColIndex = cursor.getColumnIndex("description");
                int lostPhotosColIndex = cursor.getColumnIndex("lostPhotos");

                int latIndex = cursor.getColumnIndex("centerLat");
                int lngIndex = cursor.getColumnIndex("centerLng");

                do {
                    lostInfo.setText(cursor.getString(lostInfoColIndex));
                    lostDescription.setText(cursor.getString(lostDescriptionColIndex));
                    /*
                    photosPaths = cursor.getString(lostPhotosColIndex);
                    Log.d(LOG_TAG, "photosPaths = " + photosPaths);
                    String[] paths = photosPaths.split(" ");
                    for (int i = 0; i < paths.length; i++) {
                        Log.d(LOG_TAG, "paths [" + i + 1 + "] = " + paths[i]);
                        lostPhotos.get(i).setImageURI(Uri.fromFile(new File(paths[i])));
                    }
                    */
                    photosPaths = cursor.getString(lostPhotosColIndex);
                    if(!photosPaths.equals("")) {
                        //byte[] photoBytes = photosPaths.getBytes();
                        //final Bitmap photo = BitmapUtility.getImage(photosPaths);
                        //lostPhoto1.setImageBitmap(photo);
                        File imgFile = new  File(photosPaths);
                        Bitmap photo = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        lostPhoto1.setImageBitmap(photo);
                        lostPhoto1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(isImageFitToScreen) {
                                    isImageFitToScreen=false;
                                    lostPhoto1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                    lostPhoto1.setAdjustViewBounds(true);
                                }else{
                                    isImageFitToScreen=true;
                                    lostPhoto1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                    lostPhoto1.setScaleType(ImageView.ScaleType.FIT_XY);
                                }
                            }
                        });

                        /*
                        lostPhoto1.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                        .setMessage("Сохранить изображение?")
                                        .setCancelable(false)
                                        .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                downloadImage(photo);
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
                                return false;
                            }
                        });

                         */
                    }
                    String lat = cursor.getString(latIndex);
                    String lng = cursor.getString(lngIndex);
                    Log.d(LOG_TAG, lat + " " + lng);
                    if(!lat.equals("-") && !lng.equals("-")) {
                        latTextView.setText(lat);
                        lngTextView.setText(lng);
                    }
                    // переход на следующую строку
                    // а если следующей нет (текущая - последняя), то false - выходим из цикла
                } while (cursor.moveToNext());
            } else
                Log.d(LOG_TAG, "0 rows");
            cursor.close();

            Button btnStartMission = findViewById(R.id.btnStartMission);
            btnStartMission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Intent intent = new Intent(MissionActivity.this, MapsActivity.class);
                    Intent intent = new Intent(MissionActivity.this, MissionMapSidebarActivity.class);
                    intent.putExtra("id", operationID);
                    startActivity(intent);
                }
            });

            Button btnRestartMission = findViewById(R.id.btnRestartMission);
            btnRestartMission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = getIntent();
                    //intent.putExtra("restart", "true");
                    CustomSharedPreferences.setDefaults(operationID + "restart", "true", context);
                    finish();
                    startActivity(intent);
                }
            });
        }
        else {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_mission);

            //db = new DataBase(this);
            //db.open();
            CustomSharedPreferences.setDefaults(operationID + "started", "false", context);
            CustomSharedPreferences.setDefaults(operationID + "restart", "false", context);

            isImageFitToScreen = false;

            lostInfo = findViewById(R.id.lostInfo);
            lostDescription = findViewById(R.id.lostDescription);
            missionMembers = findViewById(R.id.missionMembers);
            lostPhoto1 = findViewById(R.id.lostPhoto1);
            lostPhoto2 = findViewById(R.id.lostPhoto2);
            lostPhoto3 = findViewById(R.id.lostPhoto3);

            lostPhotos = new ArrayList<>();
            //lostPhotosUri = new ArrayList<>();

            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto1));
            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto2));
            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto3));

            etLtd = findViewById(R.id.missionLtd);
            etLng = findViewById(R.id.missionLng);

            int membersCount = db.getFieldData("Members", "operation", operationID).getCount();
            String membersCountString = "Участники: " + Integer.toString(membersCount);
            missionMembers.setText(membersCountString);

            Cursor cursor = null;
            cursor = db.getIdData("Operations", operationID);
            if (cursor.moveToFirst()) {

                // определяем номера столбцов по имени в выборке
                int lostInfoColIndex = cursor.getColumnIndex("info");
                int lostDescriptionColIndex = cursor.getColumnIndex("description");
                int lostPhotosColIndex = cursor.getColumnIndex("lostPhotos");
                int latIndex = cursor.getColumnIndex("centerLat");
                int lngIndex = cursor.getColumnIndex("centerLng");

                do {
                    lostInfo.setText(cursor.getString(lostInfoColIndex));
                    lostDescription.setText(cursor.getString(lostDescriptionColIndex));
                    photosPaths = cursor.getString(lostPhotosColIndex);
                    Log.d(LOG_TAG, "photosPaths = " + photosPaths);
                    if(!photosPaths.equals("")) {
                        //byte[] photoBytes = photosPaths.getBytes();
                        //File imgFile = new File(photosPaths);
                        final Bitmap photo = BitmapFactory.decodeFile("/storage/emulated/0/VK/Downloads/TOUkLwy6Xt0(1).jpg");
                        //lostPhoto1.setImageBitmap(photo);
                        lostPhoto1.setImageURI(Uri.parse(photosPaths));
                        lostPhoto1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(isImageFitToScreen) {
                                    isImageFitToScreen=false;
                                    lostPhoto1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                    lostPhoto1.setAdjustViewBounds(true);
                                }else{
                                    isImageFitToScreen=true;
                                    lostPhoto1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                    lostPhoto1.setScaleType(ImageView.ScaleType.FIT_XY);
                                }
                            }
                        });

                        // Загрузка Фото
                        /*
                        lostPhoto1.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                        .setMessage("Сохранить изображение?")
                                        .setCancelable(false)
                                        .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                downloadImage(photo);
                                            }
                                        })
                                        .setNegativeButton("Отмен", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                builder.create();
                                builder.show();
                                return false;
                            }
                        });
                        */
                    }
                    String lat = cursor.getString(latIndex);
                    String lng = cursor.getString(lngIndex);
                    Log.d(LOG_TAG, lat + " " + lng);
                    if(!lat.equals("-") && !lng.equals("-")) {
                        etLtd.setText(lat);
                        etLng.setText(lng);
                    }
                    // переход на следующую строку
                    // а если следующей нет (текущая - последняя), то false - выходим из цикла
                } while (cursor.moveToNext());
            } else
                Log.d(LOG_TAG, "0 rows");
            cursor.close();

            subscribeMessageListener = new MessageListener() {
                @Override
                public void onFound(Message message) {
                    Log.d(LOG_TAG, "Message found: " + new String(message.getContent()));
                    String messageFull = new String(message.getContent());
                    if(messageFull.contains(operationID) && !messageFull.contains("ok")) {
                        //Toast.makeText(context, "Получены данные о новом участнике операции", Toast.LENGTH_LONG).show();
                        messageFull = messageFull.substring(1, messageFull.length() - 1);
                        String[] messageParts = new String[5];
                        messageParts = messageFull.split("\\|");
                        for (int j = 1; j < messageParts.length; j++)
                            Log.d(LOG_TAG, Integer.toString(j) + " - " + messageParts[j]);

                        ArrayList<String> memberInfo = new ArrayList<>();
                        memberInfo.add(operationID);
                        memberInfo.add(messageParts[1]);
                        memberInfo.add(messageParts[2]);
                        memberInfo.add(messageParts[3]);
                        memberInfo.add(messageParts[3]);

                        db.addRec("Members", memberInfo);

                        int membersCount = db.getFieldData("Members", "operation", operationID).getCount();
                        String membersCountString = "Участники: " + Integer.toString(membersCount);
                        missionMembers.setText(membersCountString);
                    }
                }

                @Override
                public void onLost(Message message) {
                    Log.d(LOG_TAG, "Lost sight of message: " + new String(message.getContent()));
                }
            };

            missionBtnGetCoords = findViewById(R.id.missionBtnGetCoords);
            missionBtnGetCoords.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    locationListener = new CustomLocationListener();
                    if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
                        }
                    }
                    if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                        Toast.makeText(context, "Для получения текущих координат включите GPS", Toast.LENGTH_SHORT).show();
                    try {
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                    } catch(SecurityException e){
                        e.printStackTrace();
                    }
                }
            });

            btnStartRequestService = findViewById(R.id.btnStartRequestService);
            btnStartRequestService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String etLtdString = etLtd.getText().toString(), etLngString = etLng.getText().toString();
                    Boolean etLtdEx = false, etLngEx = false;

                    String regexp = "\\d{2}\\.\\d{4,20}";
                    Pattern pattern = Pattern.compile(regexp);
                    Matcher matcher = pattern.matcher(etLtdString);
                    if (matcher.find())
                        etLtdEx = true;
                    matcher = pattern.matcher(etLngString);
                    if (matcher.find())
                        etLngEx = true;

                    if (etLtdEx && etLngEx) {

                        db.modifyOperationsFieldData("Operations", "_id", operationID, "centerLat", etLtd.getText().toString());
                        db.modifyOperationsFieldData("Operations", "_id", operationID, "centerLng", etLng.getText().toString());

                        String date = "-";
                        String photo = "";
                        String photoURL = "";
                        Cursor cursor = db.getIdData("Operations", operationID);

                        if (cursor.moveToFirst()) {
                            date = cursor.getString(cursor.getColumnIndex("date"));
                            photo = cursor.getString(cursor.getColumnIndex("lostPhotos"));
                            photoURL = cursor.getString(cursor.getColumnIndex("photoURL"));
                        }
                        cursor.close(); // that's important too, otherwise you're gonna leak cursors

                        String number = CustomSharedPreferences.getDefaults("settingsNumber", context);
                        try {
                            byte[] bytePhoto = GZIPCompression.compress(photo);
                            photo = Arrays.toString(bytePhoto);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        /*
                        try {
                            //photo = new String(GZIPCompression.compress(photo), "UTF-8");
                            //photo = new String(GZIPCompression.compress(photo));
                            //photo = Base64.encodeToString(GZIPCompression.compress(photo), Base64.DEFAULT);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        if (!number.equals("")) {
                            mPubMessage = new Message(gson.toJson("Operation " + operationID + "|" + date + "|" + lostInfo.getText() +
                                    "|" + lostDescription.getText() + "|" + etLtdString + "|" + etLngString +
                                    "|" + number + "|" + photoURL).getBytes(Charset.forName("UTF-8")));
                            Log.d(LOG_TAG, mPubMessage.toString());
                            buildGoogleApiClient();
                        } else {
                            Toast.makeText(context, "Укажите номер телефона в настройках", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Координаты штаба введены в неверном формате", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //listMembers = (ListView)findViewById(R.id.listMembers);

            String[] from = new String[]{"name", "info"};
            int[] to = new int[] {R.id.listMemberName, R.id.listMemberInfo};
            scAdapter = new SimpleCursorAdapter(this, R.layout.list_members, null, from, to,0);
            //listMembers.setAdapter(scAdapter);
            // добавляем контекстное меню к списку
            //registerForContextMenu(listMembers);
            // создаем лоадер для чтения данных
            //getSupportLoaderManager().initLoader(0, null, this);

            /*
            listMembers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(LOG_TAG, "Members: itemClick: position = " + position + ", id = " + id);
                    //Intent intent = new Intent(MainActivity.this, MissionActivity.class);
                    //intent.putExtra("id", Long.toString(id));
                    //startActivity(intent);
                }
            });
            */

            btnCreateGroups = findViewById(R.id.btnCreateGroups);
            btnCreateGroups.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String etLtdString = etLtd.getText().toString(), etLngString = etLng.getText().toString();
                    Boolean etLtdEx = false, etLngEx = false;

                    String regexp = "\\d{2}\\.\\d{4,20}";
                    Pattern pattern = Pattern.compile(regexp);
                    Matcher matcher = pattern.matcher(etLtdString);
                    if (matcher.find())
                        etLtdEx = true;
                    matcher = pattern.matcher(etLngString);
                    if (matcher.find())
                        etLngEx = true;

                    if (etLtdEx && etLngEx) {

                        db.modifyOperationsFieldData("Operations", "_id", operationID, "centerLat", etLtd.getText().toString());
                        db.modifyOperationsFieldData("Operations", "_id", operationID, "centerLng", etLng.getText().toString());

                        centerCoords = new LatLng(Double.parseDouble(etLtd.getText().toString()), Double.parseDouble(etLng.getText().toString()));

                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                        LayoutInflater inflater = getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.dialog_group_count, null);
                        final EditText groupCountText = dialogView.findViewById(R.id.dialogGroupCount);
                        groupCountText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                groupCountText.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        InputMethodManager inputMethodManager = (InputMethodManager) MissionActivity
                                                .this.getSystemService(Context.INPUT_METHOD_SERVICE);
                                        inputMethodManager.showSoftInput(groupCountText, InputMethodManager.SHOW_IMPLICIT);
                                    }
                                });
                            }
                        });

                        int groupCount = db.getFieldData("Groups", "operation", operationID).getCount();

                        groupCountText.requestFocus();
                        if(groupCount > 0) {
                            dialogBuilder.setTitle("Текущее число групп: " + Integer.toString(groupCount));
                            dialogBuilder.setMessage("Чтобы изменить число групп, введите новое значение и нажмите ОК. " +
                                    "Чтобы отсавить число групп прежним, оставьте поле ввода пустым и нажмиите ок");
                        } else
                            dialogBuilder.setTitle("Введите число групп");
                        dialogBuilder.setView(dialogView);
                        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String groupCount;
                                if(!groupCountText.getText().toString().equals(""))
                                    groupCount = groupCountText.getText().toString();
                                else
                                    groupCount = "0";
                                Log.d(LOG_TAG, groupCount);
                                //Intent intent = new Intent(MissionActivity.this, GroupsActivity.class);
                                Intent intent = new Intent(MissionActivity.this, DistributeGroupsActivity.class);
                                intent.putExtra("groupCount", groupCount);
                                intent.putExtra("id", operationID);
                                //intent.putExtra("id", Long.toString(id));
                                startActivityForResult(intent, 1);
                            }
                        });
                        AlertDialog alertDialog = dialogBuilder.create();
                        alertDialog.show();
                        /*
                        Intent intent = new Intent(MissionActivity.this, GroupsActivity.class);
                        intent.putExtra("id", operationID);
                        startActivityForResult(intent, 1);
                        */

                        /*
                        sPref = getPreferences(MODE_PRIVATE);
                        String savedText = sPref.getString(spFirstCreateGroupKey, "");
                        if (!savedText.equals(""))
                            firstCreateGroup = savedText;
                        else firstCreateGroup = "true";
                        */

                        /*
                        if (firstCreateGroup.equals("true")) {
                            sPref = getPreferences(MODE_PRIVATE);
                            SharedPreferences.Editor ed = sPref.edit();
                            firstCreateGroup = "false";
                            ed.putString(spFirstCreateGroupKey, firstCreateGroup);
                            ed.apply();

                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                            LayoutInflater inflater = getLayoutInflater();
                            View dialogView = inflater.inflate(R.layout.dialog_group_count, null);
                            final EditText groupCountText = (EditText) dialogView.findViewById(R.id.dialogGroupCount);
                            groupCountText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                @Override
                                public void onFocusChange(View v, boolean hasFocus) {
                                    groupCountText.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            InputMethodManager inputMethodManager = (InputMethodManager) MissionActivity
                                                    .this.getSystemService(Context.INPUT_METHOD_SERVICE);
                                            inputMethodManager.showSoftInput(groupCountText, InputMethodManager.SHOW_IMPLICIT);
                                        }
                                    });
                                }
                            });
                            groupCountText.requestFocus();
                            dialogBuilder.setTitle("Введите число групп");
                            dialogBuilder.setView(dialogView);
                            dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String groupCount = groupCountText.getText().toString();
                                    Log.d(LOG_TAG, groupCount);
                                    Intent intent = new Intent(MissionActivity.this, GroupsActivity.class);
                                    intent.putExtra("groupCount", groupCount);
                                    intent.putExtra("id", operationID);
                                    //intent.putExtra("id", Long.toString(id));
                                    startActivityForResult(intent, 1);
                                }
                            });
                            AlertDialog alertDialog = dialogBuilder.create();
                            alertDialog.show();
                        } else {
                            Intent intent = new Intent(MissionActivity.this, GroupsActivity.class);
                            intent.putExtra("id", operationID);
                            startActivityForResult(intent, 1);
                        }
                        */
                    }
                    else
                        Toast.makeText(context, "Координаты штаба введены в неверном формате", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if(requestCode == resultCode) {
            setContentView(R.layout.activity_mission_prepared);

            CustomSharedPreferences.setDefaults(operationID + "started", "true", context);

            isImageFitToScreen = false;

            Log.d(LOG_TAG, requestCode + " " + resultCode);

            TextView latTextView = findViewById(R.id.missionLtdTextView);
            TextView lngTextView = findViewById(R.id.missionLngTextView);

            lostInfo = findViewById(R.id.lostInfo);
            lostDescription = findViewById(R.id.lostDescription);
            lostPhoto1 = findViewById(R.id.lostPhoto1);
            lostPhoto2 = findViewById(R.id.lostPhoto2);
            lostPhoto3 = findViewById(R.id.lostPhoto3);

            lostPhotos = new ArrayList<>();
            //lostPhotosUri = new ArrayList<>();

            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto1));
            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto2));
            lostPhotos.add((ImageView) findViewById(R.id.lostPhoto3));

            Cursor cursor = null;
            cursor = db.getIdData("Operations", operationID);
            if (cursor.moveToFirst()) {

                // определяем номера столбцов по имени в выборке
                int lostInfoColIndex = cursor.getColumnIndex("info");
                int lostDescriptionColIndex = cursor.getColumnIndex("description");
                int lostPhotosColIndex = cursor.getColumnIndex("lostPhotos");
                int latIndex = cursor.getColumnIndex("centerLat");
                int lngIndex = cursor.getColumnIndex("centerLng");

                do {
                    lostInfo.setText(cursor.getString(lostInfoColIndex));
                    lostDescription.setText(cursor.getString(lostDescriptionColIndex));
                    photosPaths = cursor.getString(lostPhotosColIndex);
                    if(!photosPaths.equals("")) {
                        //byte[] photoBytes = photosPaths.getBytes();
                        final Bitmap photo = BitmapUtility.getImage(photosPaths);
                        lostPhoto1.setImageBitmap(photo);
                        lostPhoto1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(isImageFitToScreen) {
                                    isImageFitToScreen=false;
                                    lostPhoto1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                    lostPhoto1.setAdjustViewBounds(true);
                                }else{
                                    isImageFitToScreen=true;
                                    lostPhoto1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                    lostPhoto1.setScaleType(ImageView.ScaleType.FIT_XY);
                                }
                            }
                        });

                        lostPhoto1.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                        .setMessage("Сохранить изображение?")
                                        .setCancelable(false)
                                        .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                downloadImage(photo);

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
                                return false;
                            }
                        });
                    }

                    String lat = cursor.getString(latIndex);
                    String lng = cursor.getString(lngIndex);
                    Log.d(LOG_TAG, lat + " " + lng);
                    if(!lat.equals("-") && !lng.equals("-")) {
                        latTextView.setText(lat);
                        lngTextView.setText(lng);
                    }
                    // переход на следующую строку
                    // а если следующей нет (текущая - последняя), то false - выходим из цикла
                } while (cursor.moveToNext());
            } else
                Log.d(LOG_TAG, "0 rows");
            cursor.close();

            Button btnStartMission = findViewById(R.id.btnStartMission);
            btnStartMission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Intent intent = new Intent(MissionActivity.this, MapsActivity.class);
                    Intent intent = new Intent(MissionActivity.this, MissionMapSidebarActivity.class);
                    intent.putExtra("id", operationID);
                    startActivity(intent);
                }
            });

            Button btnRestartMission = findViewById(R.id.btnRestartMission);
            btnRestartMission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = getIntent();
                    //intent.putExtra("restart", "true");
                    CustomSharedPreferences.setDefaults(operationID + "restart", "true", context);
                    finish();
                    startActivity(intent);
                }
            });

        }
    }

    public void downloadImage(Bitmap image) {

        FileOutputStream outStream = null;
        // Write to SD Card
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/Liza_Alert_Coordinator");
            dir.mkdirs();

            String fileName = String.format("%d.jpg", System.currentTimeMillis());
            File outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            image.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();

            Toast.makeText(context, "Изображение сохранено", Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "onPictureTaken - wrote to " + outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mission, menu);
        return true;
    }

    public void onClickFillMembers(MenuItem item) {
        Log.d(LOG_TAG, "Fill table Members");
        ArrayList<String> memberInfo = new ArrayList<>();
        memberInfo.add(operationID);
        memberInfo.add("+79998887766");
        memberInfo.add("Л. А. Поисковиков");
        memberInfo.add("г. Поисковый");
        memberInfo.add("новичок");
        for(int i = 0; i < 10; i++)
            db.addRec("Members", memberInfo);
        Log.d(LOG_TAG, "Success!");
    }

    public void onClickDelSP(MenuItem item) {
        sPref = getPreferences(MODE_PRIVATE);
        sPref.edit().clear().apply();
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

    /*
    private void publish(String message) {
        Log.d(LOG_TAG, "Publishing message: " + message);
        //activeMessage = new Message(message.getBytes());
        activeMessage = new Message(gson.toJson(message).getBytes(Charset.forName("UTF-8")));
        Nearby.getMessagesClient(this).publish(activeMessage);
        Log.d(LOG_TAG, "ok");
    }
    */

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
                            Toast.makeText(context, "Данные операции отправлены", Toast.LENGTH_LONG).show();
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
                            //Toast.makeText(context, "Для отправки данных об операции требуется подключение к интернету", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG,"Could not publish, status = " + status);
                        }
                    }
                });
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, subscribeMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Toast.makeText(context, "Данные волонтеров принимаются", Toast.LENGTH_LONG).show();
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
                            //Toast.makeText(context, "Для получения данных волонтеров требуется подключение к интернету", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG,"Could not subscribe, status = " + status);
                        }
                    }
                });
    }

    private void unsubscribe() {
        Log.d(LOG_TAG, "Unsubscribing.");
        if((mGoogleApiClient != null) && (subscribeMessageListener != null)) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, subscribeMessageListener);
        }
    }

    public void unpublish() {
        if (mPubMessage != null) {
            Nearby.getMessagesClient(this).unpublish(mPubMessage);
            mPubMessage = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //Nearby.getMessagesClient(this).publish(mMessage);
        //Nearby.getMessagesClient(this).subscribe(mMessageListener);
    }

    @Override
    public void onStop() {
        if(mPubMessage != null)
            Nearby.getMessagesClient(this).unpublish(mPubMessage);
        if(subscribeMessageListener != null)
            Nearby.getMessagesClient(this).unsubscribe(subscribeMessageListener);
        super.onStop();
        //Nearby.Messages.unpublish(mGoogleApiClient, mPubMessage);
        //super.onStop();
    }

    @Override
    public void onDestroy() {
        if(mPubMessage != null)
            Nearby.getMessagesClient(this).unpublish(mPubMessage);
        if(subscribeMessageListener != null)
            Nearby.getMessagesClient(this).unsubscribe(subscribeMessageListener);
        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(LOG_TAG, "GoogleApiClient connected");
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
        }
        */
        publish();
        subscribe();
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
    public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        return new CustomCursorLoader(this, db, operationID);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        scAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    static class CustomCursorLoader extends CursorLoader {

        DataBase db;
        String operationID;

        public CustomCursorLoader(Context context, DataBase db, String operationID) {
            super(context);
            this.db = db;
            this.operationID = operationID;
        }

        @Override
        public Cursor loadInBackground() {
            Cursor cursor = db.getFieldData("Members", "operation", operationID);
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return cursor;
        }
    }

    class CustomLocationListener implements LocationListener {

        @SuppressLint("SetTextI18n")
        @Override
        public void onLocationChanged(Location location) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            Log.d(LOG_TAG, "Location is " + lat + " " + lng);
            etLtd.setText(Double.toString(lat));
            etLng.setText(Double.toString(lng));
            db.modifyOperationsFieldData("Operations", "_id", operationID, "centerLat", Double.toString(lat));
            db.modifyOperationsFieldData("Operations", "_id", operationID, "centerLng", Double.toString(lng));
            //locationManager.removeUpdates(locationListener);
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}
