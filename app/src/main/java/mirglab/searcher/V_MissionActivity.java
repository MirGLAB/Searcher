package mirglab.searcher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.google.gson.Gson;
import com.splunk.mint.Mint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;



public class V_MissionActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Button btnStartService, btnStopService, btnSave, btnStartMissionMap, btnSendAgain;
    SeekBar seekBar;
    TextView seekBarText, missionLtd, missionLng, missionGroup, missionZone, missionNodesProgress;
    SharedPreferences sPref;
    final String LOG_TAG = "____";
    String operationID;
    final Context context = this;
    TextView lostInfo, lostDescription;
    ImageView lostPhoto1, lostPhoto2, lostPhoto3;
    ArrayList<ImageView> lostPhotos;
    String photosPaths;
    String phone = "", name = "", region = "", userInfo = "", groupName = "", zone = "";
    ArrayList<String> lat = new ArrayList<>();
    ArrayList<String> lng = new ArrayList<>();
    boolean inGroup = false;
    boolean isImageFitToScreen = false;

    V_DataBase db;

    private static final Gson gson = new Gson();

    private static final int TTL_IN_SECONDS = 30 * 60; // Three minutes.

    // Key used in writing to and reading from SharedPreferences.
    private static final String KEY_UUID = "key_uuid";

    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to three
     * minutes in this sample.
     */
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder().setTtlSeconds(TTL_IN_SECONDS).build();

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

    Button btnStartReciever;

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.v_activity_mission);
        //seekBarText = findViewById(R.id.seekBarText);
        //seekBar = findViewById(R.id.seekBar);
        Mint.initAndStartSession(this.getApplication(), "0ff11f04");
        //btnStartService = findViewById(R.id.btnStartService);
        btnStartMissionMap = findViewById(R.id.btnStartMissionMap);
        btnStartMissionMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inGroup) {
                    Intent intent = new Intent(V_MissionActivity.this, V_MissionMapSidebarActivity.class);
                    intent.putExtra("id", operationID);
                    intent.putExtra("groupName", groupName);
                    startActivity(intent);
                } else {
                    Toast.makeText(context, "Отсутсвуют данные о группе", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnSendAgain = findViewById(R.id.btnSendAgain);
        btnSendAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buildGoogleApiClient();
            }
        });

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            }
        }

        db = new V_DataBase(this);
        db.open();

        operationID = getIntent().getStringExtra("id");

        lostInfo = (TextView) findViewById(R.id.lostInfo);
        lostDescription = (TextView) findViewById(R.id.lostDescription);
        lostPhoto1 = (ImageView) findViewById(R.id.lostPhoto1);
        lostPhoto2 = (ImageView) findViewById(R.id.lostPhoto2);
        lostPhoto3 = (ImageView) findViewById(R.id.lostPhoto3);

        lostPhotos = new ArrayList<>();
        //lostPhotosUri = new ArrayList<>();

        lostPhotos.add((ImageView) findViewById(R.id.lostPhoto1));
        lostPhotos.add((ImageView) findViewById(R.id.lostPhoto2));
        lostPhotos.add((ImageView) findViewById(R.id.lostPhoto3));

        missionLtd = (TextView) findViewById(R.id.missionLtd);
        missionLng = (TextView) findViewById(R.id.missionLng);

        missionGroup = (TextView) findViewById(R.id.missionGroup);
        //missionZone = (TextView) findViewById(R.id.missionZone);
        //missionNodesProgress = (TextView) findViewById(R.id.missionNodesProgress);

        Cursor cursor = null;
        cursor = db.getIdData("Operations", operationID);
        if (cursor.moveToFirst()) {

            // определяем номера столбцов по имени в выборке
            int lostInfoColIndex = cursor.getColumnIndex("info");
            int lostDescriptionColIndex = cursor.getColumnIndex("description");
            int lostPhotosColIndex = cursor.getColumnIndex("lostPhotos");
            int latIndex = cursor.getColumnIndex("centerLat");
            int lngIndex = cursor.getColumnIndex("centerLng");
            int groupNameColIndex = cursor.getColumnIndex("groupName");
            int groupZoneColIndex = cursor.getColumnIndex("groupZone");

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
                missionLtd.setText(cursor.getString(latIndex));
                missionLng.setText(cursor.getString(lngIndex));
                groupName = cursor.getString(groupNameColIndex);
                zone = cursor.getString(groupZoneColIndex);
                if (!groupName.equals("-")) {
                    inGroup = true;
                    missionGroup.setText("Ваша группа: " + groupName);
                }
                if (!zone.equals("")) {
                    //missionZone.setText("Ваш участок: данные загружены");
                }
                photosPaths = cursor.getString(lostPhotosColIndex);
                Log.d(LOG_TAG, "1 " + photosPaths);
                if (!photosPaths.equals("")) {
                    if (photosPaths.contains("http")) {
                        try {
                            new ImageParser(photosPaths).execute();
                        } catch (Exception anyError) {
                            Toast.makeText(context, "Не удалось загрузить фото пропавшего. Проверьте подключение к интернету.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        final Bitmap photo = BitmapUtility.getImage(photosPaths);
                        lostPhoto1.setImageBitmap(photo);
                        lostPhoto1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (isImageFitToScreen) {
                                    isImageFitToScreen = false;
                                    lostPhoto1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                    lostPhoto1.setAdjustViewBounds(true);
                                } else {
                                    isImageFitToScreen = true;
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
                }
                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "0 rows");
        cursor.close();

        cursor = null;
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

        mPubMessage = new Message(gson.toJson(operationID + "|" + phone + "|" + name + "|" + region +
                "|" + userInfo).getBytes(Charset.forName("UTF-8")));
        if (!inGroup)
            buildGoogleApiClient();

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.d(LOG_TAG, "Message found: " + new String(message.getContent()));
                int nodesInfo = 0;
                //Toast.makeText(context, "", Toast.LENGTH_LONG).show();
                String messageFull = new String(message.getContent());
                messageFull = messageFull.substring(1, messageFull.length() - 1);
                //Boolean
                //if(messageFull.contains(phone)) {
                if (messageFull.contains(phone)) {
                    //messageFull = messageFull.substring(1, messageFull.length() - 1);
                    String[] messageParts = new String[3];
                    messageParts = messageFull.split("\\|");
                    groupName = messageParts[1];
                    zone = messageParts[2];
                    Log.d(LOG_TAG, "Ваша группа: " + groupName);
                    missionGroup.setText("Ваша группа: " + groupName);
                    db.modifyFieldData("Operations", "_id", Long.parseLong(operationID),
                            "groupName", groupName);
                    db.modifyFieldData("Operations", "_id", Long.parseLong(operationID),
                            "groupZone", zone);
                    inGroup = true;
                    unpublish();
                    mPubMessage = new Message(gson.toJson(operationID + "|" + phone + "|" + groupName + "|" + "ok")
                            .getBytes(Charset.forName("UTF-8")));
                    publish();
                }
            }

            @Override
            public void onLost(Message message) {
                Log.d(LOG_TAG, "Lost sight of message: " + new String(message.getContent()));
            }
        };

        /*
        sPref = getPreferences(MODE_PRIVATE);
        int savedTime = sPref.getInt("time", 0);
        if(savedTime != 0) {
            Log.d("Saved time", Integer.toString(savedTime));
            seekBar.setProgress(savedTime);
            seekBarText.setText("Отправлять каждые " + savedTime + " минут");
        }
        else {
            seekBar.setProgress(1);
            seekBarText.setText("Отправлять каждые 1 минут");
        }
        seekBar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener) this);
        */
    }

    public void downloadImage(Bitmap image) {

        FileOutputStream outStream = null;
        // Write to SD Card
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/Searcher_Volunteer");
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.v_menu_main, menu);
        return true;
    }

    public void onClickinfo(MenuItem item)
    {
        startActivity(new Intent(V_MissionActivity.this, V_InfoActivity.class));
    }

    public void onClickLocationSettings(MenuItem item)
    {
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }
    */

    public void onClickStartSendService(View view) {
        unpublish();
        unsubscribe();
        mGoogleApiClient.disconnect();
        Intent startIntent = new Intent(V_MissionActivity.this, V_SendSmsAndGpsService.class);
        startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startIntent.putExtra("timer", seekBar.getProgress());
        startIntent.putExtra("id", operationID);
        //startIntent.putExtra("groupName", groupName);
        startService(startIntent);
    }

    public void onClickStopSendService(View view) {
        Intent stopIntent = new Intent(V_MissionActivity.this, V_SendSmsAndGpsService.class);
        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        stopService(stopIntent);
        stopService(new Intent(V_MissionActivity.this, V_SendSmsAndGpsService.class));
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

        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(LOG_TAG, "Ожидайте завершения распределения по группам");
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                    .setMessage("Не удается получить данные, проверьте подключение к интернету")
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
                            Toast.makeText(context, "Ваши данные отправлены координатору", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG, "Published successfully.");
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                    .setMessage("Не удается отправить заявку, проверьте подключение к интернету")
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
                            //Toast.makeText(context, "Для отправки ваших данных координатору требуется подключение к интернету", Toast.LENGTH_LONG).show();
                            Log.d(LOG_TAG,"Could not publish, status = " + status);
                        }
                    }
                });
    }

    public void unpublish() {
        if (mPubMessage != null) {
            Nearby.getMessagesClient(this).unpublish(mPubMessage);
            mPubMessage = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        seekBarText.setText("Отправлять каждые " + String.valueOf(seekBar.getProgress())+" минут");
        if(seekBar.getProgress() == 0)
            seekBar.setProgress(1);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected void onDestroy() {
        unpublish();
        unsubscribe();
        db.close();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        unpublish();
        unsubscribe();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(LOG_TAG, "GoogleApiClient connected");
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
        //Toast.makeText(context, "Для отправки ваших данных координатору требуется подключение к интернету", Toast.LENGTH_LONG).show();
        Log.d(LOG_TAG,"Could not publish, status = все плохо");
    }

    public class ImageParser extends AsyncTask<String, Void, String> {

        String imgParse = "";
        String imageString = "";

        public ImageParser(String photosPaths) {
            this.imgParse = photosPaths;
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d(LOG_TAG, "2 " + imgParse);
            InputStream in = null;
            try {
                in = new java.net.URL(imgParse).openStream();
                Bitmap photo = BitmapFactory.decodeStream(in);
                imageString = BitmapUtility.bitMapToString(photo);
                db.modifyFieldData("Operations", "_id", operationID, "lostPhotos", imageString);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(LOG_TAG, "3 " + result);
            Log.d(LOG_TAG, "4 " + imageString);
            if((!imageString.equals("")) && (imageString != null) && (imgParse.contains("https"))) {
                final Bitmap photo = BitmapUtility.getImage(imageString);
                lostPhoto1.setImageBitmap(photo);
                lostPhoto1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isImageFitToScreen) {
                            isImageFitToScreen = false;
                            lostPhoto1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            lostPhoto1.setAdjustViewBounds(true);
                        } else {
                            isImageFitToScreen = true;
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
            }
        }
    }
}
