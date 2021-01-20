package mirglab.searcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.splunk.mint.Mint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



public class SettingsActivity extends AppCompatActivity {

    Button btnChangeMode, btnDelOperations, btnDelMembers, btnDelGroups, btnFillMembers, btnSave,
            btnDeleteMarkers, btnSaveDB;
    EditText number, lizaAlertForum;
    DataBase db;
    SharedPreferences sPref;
    final Context context = this;
    final String LOG_TAG = "____";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
        setContentView(R.layout.activity_settings);

        db = new DataBase(this);
        db.open();

        btnChangeMode = findViewById(R.id.btnChangeMode);
        btnChangeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CustomSharedPreferences.setDefaults("mode", "Searcher", context);
                //finish();
                //Intent newIntent = new Intent(SettingsActivity.this, MainActivity.class);
                //startActivity(newIntent);
                Intent intent = new Intent();
                setResult(22, intent);
                finish();
            }
        });

        btnDelOperations = findViewById(R.id.btnDeleteOperations);
        btnDelOperations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delTable("Operations");
            }
        });

        btnDelMembers = findViewById(R.id.btnDeleteMembers);
        btnDelMembers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delTable("Members");
            }
        });

        btnDelGroups = findViewById(R.id.btnDeleteGroups);
        btnDelGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delTable("Groups");
            }
        });


        btnDeleteMarkers = findViewById(R.id.btnDeleteMarkers);
        btnDeleteMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delTable("Markers");
            }
        });

        btnSaveDB = findViewById(R.id.btnSaveDB);
        btnSaveDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String appDbPath = db.getDBFullName();
                //String sdFolder =  Environment.getExternalStorageDirectory().getAbsolutePath() + "/Liza_Alert_Volunteer";

                InputStream input = null;
                OutputStream output = null;
                try {

                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath() + "/Liza_Alert_Coordinator");
                    dir.mkdirs();

                    String fileName = String.format("%d", System.currentTimeMillis());
                    File outFile = new File(dir, fileName);

                    //Open your local db as the input stream
                    input = new FileInputStream(appDbPath);
                    //Open the empty db as the output stream
                    output = new FileOutputStream(outFile);

                    //transfer bytes from the inputfile to the outputfile
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = input.read(buffer))>0){
                        output.write(buffer, 0, length);
                    }
                } catch (IOException e) {
                    Toast.makeText(context, "Не удалось сохранить базу данных", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } finally {
                    Toast.makeText(context, "База данных сохранена", Toast.LENGTH_LONG).show();
                    try {
                        //Close the streams
                        if(output!=null){
                            output.flush();
                            output.close();
                        }
                        if(input!=null){
                            input.close();
                        }
                    } catch (IOException e) { }
                }
            }
        });

        number = findViewById(R.id.coordinatorNumber);
        lizaAlertForum = findViewById(R.id.lizaAlertForum);

        String savedText = CustomSharedPreferences.getDefaults("settingsNumber", context);
        if(savedText != null) {
            if (!savedText.equals(""))
                number.setText(savedText);
        }
        savedText = CustomSharedPreferences.getDefaults("settingsForum", context);
        Log.d(LOG_TAG, "Well then its " + savedText);
        if(savedText != null) {
            if (!savedText.equals("")) {
                lizaAlertForum.setText(savedText);
            } else {
                savedText = "http://lizaalert.org/forum/viewforum.php?f=276";
                lizaAlertForum.setText(savedText);
            }
        } else {
            savedText = "http://lizaalert.org/forum/viewforum.php?f=276";
            lizaAlertForum.setText(savedText);
        }
        /*
        sPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String savedText = sPref.getString("number", "");
        if (!savedText.equals(""))
            number.setText(savedText);
            */

        CustomSharedPreferences.setDefaults("firstLaunchC", "false", context);

        btnSave = findViewById(R.id.settingsSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                sPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = sPref.edit();
                ed.putString("number", number.getText().toString());
                ed.apply();
                */
                String numberStr = number.getText().toString();
                if(!numberStr.contains("+7")||numberStr.length() != 12)
                    Toast.makeText(context, "Номер введен в неверном формате", Toast.LENGTH_LONG).show();
                else {
                    CustomSharedPreferences.setDefaults("settingsNumber", numberStr, context);
                    String forumStr = lizaAlertForum.getText().toString();
                    if(!forumStr.equals(""))
                        CustomSharedPreferences.setDefaults("settingsForum", forumStr, context);
                    Intent intent = new Intent();
                    setResult(12, intent);
                    finish();
                }
            }
        });
    }

    /*
    @Override
    public void onBackPressed() {
        finish();
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
    }
     */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            setResult(12, intent);
            finish();
            return true;
        }
        return false;
    }

    /*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return false;
    }
    */
}
