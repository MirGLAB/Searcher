package mirglab.searcher;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;



public class CreateMissionActivity extends AppCompatActivity {

    public static final int PICK_IMAGE = 1;

    EditText newLostInfo, newLostDescription;
    Button btnLoadPhoto, btnCreate;
    ArrayList<ImageView> newLostPhotos;
    ArrayList<String> photos;
    //ArrayList<Uri> lostPhotosUri;
    //DBHelper dbHelper;
    //SQLiteDatabase db;
    DataBase db;
    final String LOG_TAG = "____";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_mission);

        //dbHelper = new DBHelper(this);
        db = new DataBase(this);
        db.open();

        newLostInfo = (EditText)findViewById(R.id.newLostInfo);
        newLostDescription = (EditText)findViewById(R.id.newLostDescription);
        newLostPhotos = new ArrayList<>();
        //lostPhotosUri = new ArrayList<>();

        newLostPhotos.add((ImageView)findViewById(R.id.newLostPhoto1));
        newLostPhotos.add((ImageView)findViewById(R.id.newLostPhoto2));
        newLostPhotos.add((ImageView)findViewById(R.id.newLostPhoto3));

        btnLoadPhoto = findViewById(R.id.btnLoadPhoto);
        btnLoadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });

        btnCreate = findViewById(R.id.btnCreate);
        //Сохранение данных о новом пропавшем в бд
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //заполнение в одтдельном потоке - сомнителная идея, т.к. нельзя продолжить работу,
                //пока не будет заполнена инфа в бд
                //new DBWriter().execute();

                // получаем данные из полей ввода
                ArrayList<String> info = new ArrayList<>();
                String id;
                id = "1" + (new SimpleDateFormat("ddMMyyyyHHmmssS")).format(Calendar.getInstance().getTime());
                Log.d("WWW", "ID = " + id);
                info.add(id);
                DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
                String date = df.format(Calendar.getInstance().getTime());
                info.add(date);
                info.add(newLostInfo.getText().toString());
                info.add(newLostDescription.getText().toString());
                String photosPaths = "";
                if(photos != null)
                    for(int i = 0; i < photos.size(); i++) {
                        photosPaths += photos.get(i) + " ";
                    }
                info.add(photosPaths);

                //Операции нельзя создавать пока что, нужно разрбраться с присвоением ID
                //можно брать ID последней записи и прибоавлять 1
                db.addRec("Operations", info);

                //setResult(RESULT_OK);
                Intent intent = new Intent();
                setResult(10, intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        db.close();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == Activity.RESULT_OK) {
            if (intent.getData() != null) {
                try {
                    Uri uri = intent.getData();
                    photos = new ArrayList<>();
                    photos.add(getRealPathFromURI(this, uri));
                    //photos.add(uri.toString());
                    Log.d(LOG_TAG, "photo path = " + photos.get(0));
                    newLostPhotos.get(0).setImageURI(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    photos = new ArrayList<>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri uri = item.getUri();
                        photos.add(getRealPathFromURI(this, uri));
                        Log.d(LOG_TAG, "photo № " + Integer.toString(i+1) +" path = " + photos.get(0));
                        newLostPhotos.get(i).setImageURI(uri);
                    }
                }
            }
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
