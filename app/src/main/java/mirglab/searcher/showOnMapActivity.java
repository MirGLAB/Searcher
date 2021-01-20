package mirglab.searcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.splunk.mint.Mint;



public class showOnMapActivity extends AppCompatActivity {

    private EditText login, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_on_map);
        login = findViewById(R.id.login);
        password = findViewById(R.id.password);
        login.setText("1");
        password.setText("2");
        Mint.initAndStartSession(this.getApplication(), "9b4fd61a");

    }

    public void onClickSignIn(View view)  {

        String log = login.getText().toString();
        String pass = password.getText().toString();
        if (log.equals("1") &&(pass.equals("2")))
        {
            logtrue();
        }
        else
        {
            Toast.makeText(this, "Неверный логин и(или) пароль.", Toast.LENGTH_SHORT).show();
        }
    }

    public void logtrue() {
        Intent intent = new Intent(showOnMapActivity.this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        //при переходе на активити с картой, останавливается отправка смс
       // Intent stopIntent = new Intent(showOnMapActivity.this, SendSmsAndGpsService.class);
       // stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
       // stopService(stopIntent);
       // stopService(new Intent(showOnMapActivity.this, SendSmsAndGpsService.class));
    }
}

