package mirglab.liza_alert_app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.splunk.mint.Mint;

import java.util.ArrayList;

public class V_InfoActivity extends AppCompatActivity {

    EditText phone, name, region, info;
    Button btnSave;
    Spinner spinner;
    mirglab.liza_alert_app.V_DataBase db;
    final String LOG_TAG = "____";
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        context = this;
        db = new mirglab.liza_alert_app.V_DataBase(this);
        db.open();
        Mint.initAndStartSession(this.getApplication(), "0ff11f04");
        setContentView(R.layout.v_activity_info);
        btnSave = findViewById(R.id.btnSave);
        phone = findViewById(R.id.userPhone);
        name = findViewById(R.id.userName);
        region = findViewById(R.id.userRegion);
        info = findViewById(R.id.userInfo);
        //spinner =  findViewById(R.id.spinner);

        Cursor cursor = null;
        cursor = db.getAllData("User");
        if (cursor.moveToFirst()) {
            do {
                phone.setText(cursor.getString(cursor.getColumnIndex("number")));
                name.setText(cursor.getString(cursor.getColumnIndex("name")));
                region.setText(cursor.getString(cursor.getColumnIndex("region")));
                info.setText(cursor.getString(cursor.getColumnIndex("info")));
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "0 rows");
        cursor.close();

        CustomSharedPreferences.setDefaults("firstLaunchS", "false", context);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ArrayList<String> userInfo = new ArrayList<>();
                userInfo.add(phone.getText().toString());
                userInfo.add(name.getText().toString());
                userInfo.add(region.getText().toString());
                userInfo.add(info.getText().toString());
                db.addRec("User", userInfo);

                //Intent intent = new Intent();
                //setResult(12, intent);
                //finish();
                //CustomSharedPreferences.setDefaults("firstLaunch", "false", context);
                startActivity(new Intent(mirglab.liza_alert_app.V_InfoActivity.this, mirglab.liza_alert_app.V_MainActivity.class));

                //Toast.makeText(context, "Данные сохранены", Toast.LENGTH_LONG).show();
                //CustomSharedPreferences.setDefaults("firstLaunch", "false", context);
                //Toast.makeText(context, "Lf", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            Intent intent = new Intent(V_InfoActivity.this, V_MainActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
