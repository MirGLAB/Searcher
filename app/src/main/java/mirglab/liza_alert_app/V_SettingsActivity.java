package mirglab.liza_alert_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class V_SettingsActivity extends AppCompatActivity {

    Button btnChangeMode, btnDelOperations, btnDeleteMarkers, btnSaveDB;
    mirglab.liza_alert_app.V_DataBase db;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Mint.initAndStartSession(this.getApplication(), "9b4fd61a");
        setContentView(R.layout.v_activity_settings);

        db = new mirglab.liza_alert_app.V_DataBase(this);
        db.open();

        btnChangeMode = findViewById(R.id.btnChangeMode);
        btnChangeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                CustomSharedPreferences.setDefaults("mode", "Coordinator", context);
                finish();
                Intent newIntent = new Intent(V_SettingsActivity.this, MainActivity.class);
                startActivity(newIntent);
                 */
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
                    File dir = new File(sdCard.getAbsolutePath() + "/Liza_Alert_Volunteer");
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
    }

    /*
    @Override
    public void onBackPressed() {
        finish();
        Intent intent = new Intent(V_SettingsActivity.this, V_MainActivity.class);
        startActivity(intent);
    }
     */
}
