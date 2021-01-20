package mirglab.liza_alert_app;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.os.AsyncTask;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static final int CREATE_MISSION = 10;

    String mode;

    Button btnCreateMission, btnDownloadMissions;
    ListView listOperations;
    DataBase db;
    SimpleCursorAdapter scAdapter;
    SiteParser siteParser;
    ProgressBar progressBar;
    Context context = this;
    String[] from;
    int[] to;
    ArrayList<String> arrayMissions;
    CustomAdapter adapter;
    AlertDialog.Builder internetDialog;
    //Bitmap photoGlobal;

    // благодоря этому классу мы будет разбирать данные на куски
    public Elements title;
    // то в чем будем хранить данные пока не передадим адаптеру
    public ArrayList<String> titleList = new ArrayList<String>();
    // Listview Adapter для вывода данных
    // List view
    final String LOG_TAG = "____";
    private static final int CM_DELETE_ID = 1;

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

        super.onCreate(savedInstanceState);

        /*
        internetDialog = new AlertDialog.Builder(context)
                .setMessage("Для работы приложения требуется подключение к интернету")
                .setCancelable(false)
                .setPositiveButton("Открыть настройки", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 11);
                    }
                });
        internetDialog.create();
        */

        mode = CustomSharedPreferences.getDefaults("mode", context);
        if(mode == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setMessage("Выберите режим работы")
                    .setCancelable(false)
                    .setNegativeButton("Координатор", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CustomSharedPreferences.setDefaults("mode", "Coordinator", context);
                            finish();
                            Intent newIntent = new Intent(MainActivity.this, MainActivity.class);
                            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(newIntent);
                        }
                    })
                    .setPositiveButton("Поисковик", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CustomSharedPreferences.setDefaults("mode", "Searcher", context);
                            finish();
                            Intent newIntent = new Intent(MainActivity.this, MainActivity.class);
                            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(newIntent);
                        }
                    });
            builder.create();
            builder.show();
        } else {
            if(mode.equals("Searcher")) {
                Intent intent = new Intent(MainActivity.this, V_MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                this.finish();
            } else if(mode.equals("Coordinator")) {

                setContentView(R.layout.activity_main);

                if (!isOnline(context))
                    dialogInternet();

                //CustomSharedPreferences.setDefaults("firstLaunch", "false", context);
                String firstLaunchC = CustomSharedPreferences.getDefaults("firstLaunchC", context);
                if (firstLaunchC == null) {
                    startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 12);
                } else {
                    //progressBar = (ProgressBar) findViewById(R.id.mainProgressBar);
                    //progressBar.setVisibility(View.INVISIBLE);

                    if ((ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) ||
                            (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                            (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS) != PackageManager.PERMISSION_GRANTED) ||
                            (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) ||
                            (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS, Manifest.permission.ACCESS_NETWORK_STATE,
                                    Manifest.permission.ACCESS_WIFI_STATE}, 10);
                        }
                    }

                    //this.deleteDatabase("Liza_Alert_DB");

                    // открываем подключение к БД
                    db = new DataBase(MainActivity.this);
                    db.open();

                    listOperations = (ListView) findViewById(R.id.listOperations);
                    registerForContextMenu(listOperations);

                    fillList();
                    adapter = new CustomAdapter(MainActivity.this, arrayMissions);
                    listOperations.setAdapter(new CustomAdapter(MainActivity.this, arrayMissions));

                    listOperations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            id = adapter.getItemId(position);
                            Log.d(LOG_TAG, "itemClick: position = " + position + ", id = " + id);
                            Intent intent = new Intent(MainActivity.this, MissionActivity.class);
                            intent.putExtra("id", Long.toString(id));
                            if (siteParser != null)
                                siteParser.cancel(true);
                            startActivity(intent);
                        }
                    });

                    // Добавляем данные для ListView
                    //adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.product_name, titleList);

                                        /*
                                        btnDownloadMissions = findViewById(R.id.btnDownloadMissions);
                                        btnDownloadMissions.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                downloadMissions();
                                            }
                                        });
                                        */

                    btnCreateMission = findViewById(R.id.btnCreateMission);
                    btnCreateMission.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MainActivity.this, CreateMissionActivity.class);
                            startActivityForResult(intent, CREATE_MISSION);
                        }
                    });
                }
            }
        }
    }

    public void downloadMissions() {

        try {
            siteParser = new SiteParser();
            siteParser.execute();
        } catch (Exception anyError) {
            Log.d(LOG_TAG, "Parsing problems!");
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setMessage("Не удается скачать данные с форума, проверьте подключение к интернету")
                    .setCancelable(false)
                    .setPositiveButton("Повторить", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            downloadMissions();
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        if(!isOnline(context))
            dialogInternet();
        super.onResume();

        if(mode == "Coordinator") {}
        else if(mode == "Searcher") {
            Intent intent = new Intent(MainActivity.this, V_MainActivity.class);
            startActivity(intent);
            this.finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(LOG_TAG, "Here we are");
        if(resultCode == 10) {
            finish();
            Intent newIntent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(newIntent);
        }
        if(resultCode == 11) {
            if(!isOnline(context))
                dialogInternet();
        }
        if(resultCode == 12) {
            //Toast.makeText(context, "Данные сохранены", Toast.LENGTH_LONG).show();
            //CustomSharedPreferences.setDefaults("firstLaunch", "false", context);
            finish();
            Intent newIntent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(newIntent);
        }
        if(resultCode == 22) {
            //Toast.makeText(context, "Данные сохранены", Toast.LENGTH_LONG).show();
            //CustomSharedPreferences.setDefaults("firstLaunch", "false", context);
            CustomSharedPreferences.setDefaults("mode", "Searcher", context);
            finish();
            Intent newIntent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(newIntent);
        }
        // получаем новый курсор с данными
        //loaderManager.initLoader(0, null, this).forceLoad();
        //loadDB();
        //listOperations.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onClickSettings(MenuItem item) {
        startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 12);
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
            adapter = new CustomAdapter(MainActivity.this, arrayMissions);
            listOperations.setAdapter(new CustomAdapter(MainActivity.this, arrayMissions));
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // закрываем подключение при выходе
        if(db != null)
            db.close();
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
                view = lInflater.inflate(R.layout.list_mission, parent, false);
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

        //ImageView lostPhoto = findViewById(R.id.lostPhoto);
        //lostPhoto.setImageBitmap(photoGlobal);
    }

    /** А вот и внутрений класс который делает запросы, если вы не читали статьи у меня в блоге про отдельные
     * потоки советую почитать */
    public class SiteParser extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //tvInfo.setText("Begin");
            progressBar.setVisibility(View.VISIBLE);
        }

        // Метод выполняющий запрос в фоне, в версиях выше 4 андроида, запросы в главном потоке выполнять
        // нельзя, поэтому все что вам нужно выполнять - выносите в отдельный тред
        @Override
        protected String doInBackground(String... arg) {

            Document doc;
            try {
                //String url = "http://lizaalert.org/forum/";
                //String url = "http://lizaalert.org/forum/viewforum.php?f=276";
                String url = CustomSharedPreferences.getDefaults("settingsForum", context);

                for(int page = 0; page < 3; page++) {
                    if(page > 0)
                        url += "&start=" + Integer.toString(25*page);

                    doc = Jsoup.connect(url)
                            .data("query", "Java")
                            .userAgent("Mozilla")
                            .cookie("auth", "token")
                            .timeout(30000)
                            .post();

                    ArrayList<String[]> lostForumInfo = new ArrayList<>();

                    Elements elems = doc.getElementsByClass("row bg1");
                    for (Element elem : elems) {
                        String lostForumFIO = elem.getElementsByAttribute("title").text();
                        //Log.d(LOG_TAG, elem.getElementsByTag("img").text());
                        if (lostForumFIO.contains("Пропал")) {
                            String lostForumText = elem.getElementsByAttribute("title").html();
                            String[] strs = new String[100];
                            strs = lostForumText.split("&amp;");
                            String lostForumURL = "http://lizaalert.org/forum" + strs[0].substring(10) + "&" + strs[1];
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
                        //Log.d(LOG_TAG, elem.getElementsByTag("img").text());
                        if (lostForumFIO.contains("Пропал")) {
                            String lostForumText = elem.getElementsByAttribute("title").html();
                            String[] strs = new String[100];
                            strs = lostForumText.split("&amp;");
                            String lostForumURL = "http://lizaalert.org/forum" + strs[0].substring(10) + "&" + strs[1];
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

                    for (int i = 0; i < lostForumInfo.size(); i++) {

                        lostUrl = lostForumInfo.get(i)[1];
                        lostDoc = Jsoup.connect(lostUrl)
                                .data("query", "Java")
                                .userAgent("Mozilla")
                                .cookie("auth", "token")
                                .timeout(30000)
                                .post();

                        String lostID = (lostForumInfo.get(i)[2]).substring(2);

                        elems.clear();

                        Log.d(LOG_TAG, Integer.toString(i));
                        Log.d(LOG_TAG, lostID);
                        Log.d(LOG_TAG, lostDoc.getElementsByClass("content").text());
                        //Log.d(LOG_TAG, lostDoc.getElementsByAttributeValue("alt", "Изображение").text());
                        //Log.d(LOG_TAG, lostDoc.getElementsByTag("img").text());
                        String imgParse = lostDoc.getElementsByClass("content").html();
                        if((imgParse.contains("img src=")) && (imgParse.contains(" alt="))) {
                            imgParse = imgParse.substring(imgParse.indexOf("img src=") + 9, imgParse.indexOf(" alt=") - 1);
                            if(!imgParse.contains("https:"))
                                imgParse = "https" + imgParse.substring(4);
                            Log.d(LOG_TAG, imgParse);
                        }

                        String forumDesc = lostDoc.getElementsByClass("content").text();

                        String regexp = "(Координаты|КООРДИНАТЫ|координаты).{0,10}\\d{2}\\.\\d{4,6}.{0,5}\\d{2}\\.\\d{4,6}";
                        Pattern pattern = Pattern.compile(regexp);
                        Matcher matcher = pattern.matcher(forumDesc);
                        String forumCoords;
                        if (matcher.find()) {
                            forumCoords = matcher.group();
                            Log.d(LOG_TAG, matcher.group());
                        }

                        String desc;
                        if (forumDesc.contains("---"))
                            desc = forumDesc.substring(0, forumDesc.indexOf("---"));
                        else
                            desc = forumDesc;

                        Log.d(LOG_TAG, desc);
                        //получена строка с описанием пропавшего, далее происходит обработка строки
                        //и приведение ее к подходящему виду

                        String lowDesc = desc.toLowerCase();
                        ArrayList<Integer> textPos = new ArrayList<>();
                        if (lowDesc.contains("табл"))
                            textPos.add(lowDesc.indexOf("табл"));
                        if (lowDesc.contains("ориентировк"))
                            textPos.add(lowDesc.indexOf("ориентировк"));
                        if (lowDesc.contains("карт"))
                            textPos.add(lowDesc.indexOf("карт"));
                        if (lowDesc.contains("резерв"))
                            textPos.add(lowDesc.indexOf("резерв"));
                        if (lowDesc.contains("готов"))
                            textPos.add(lowDesc.indexOf("готов"));
                        if (lowDesc.contains("нахожусь"))
                            textPos.add(lowDesc.indexOf("нахожусь"));
                        Collections.sort(textPos, new Comparator<Integer>() {
                            @Override
                            public int compare(Integer a, Integer b) {
                                return a < b ? -1 : (a > b) ? 1 : 0;
                            }
                        });

                        if (textPos.size() > 0)
                            desc = desc.substring(0, textPos.get(0));
                        Log.d(LOG_TAG, desc);

                        //далее необходимо занести полученнную инфу в бд
                        ArrayList<String> info = new ArrayList<>();
                        info.add(lostID);
                        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
                        String date = df.format(Calendar.getInstance().getTime());
                        info.add(date);
                        if (desc.toLowerCase().contains("приметы")) {
                            info.add(desc.substring(0, desc.toLowerCase().indexOf("приметы")));
                            info.add(desc.substring(desc.toLowerCase().indexOf("приметы")));
                        } else {
                            info.add(desc);
                            info.add("-");
                        }
                        //String photosPaths = "";
                    /*
                    if(photos.size() != 0)
                        for(int i = 0; i < photos.size(); i++) {
                            photosPaths += photos.get(i) + " ";
                        }
                        */
                        //info.add(photosPaths);
                        if((imgParse != null) && (!imgParse.equals("")) && (imgParse.contains("https"))) {
                            //Bitmap photo = getBitmapFromURL(imgParse);
                            InputStream in = null;
                            try {
                                in = new java.net.URL(imgParse).openStream();
                            } catch (java.net.MalformedURLException e) {
                                Log.d(LOG_TAG, "Photo error");
                            }
                            if(in != null) {
                                //Log.d(LOG_TAG, in.toString());
                                Bitmap photo = BitmapFactory.decodeStream(in);
                                //photoGlobal = BitmapFactory.decodeStream(in);
                                //Bitmap photo = Picasso.with(context).load(imgParse).get();
                                String photoString = BitmapUtility.bitMapToString(photo);
                                info.add(photoString);
                                info.add(imgParse);
                            } else {
                                info.add("");
                                info.add("");
                            }
                        } else {
                            info.add("");
                            info.add("");
                        }
                        db.addRec("Operations", info);
                        //db.addRec("O");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        Bitmap getBitmapFromURL(String src) {
            try {
                java.net.URL url = new java.net.URL(src);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            fillList();
            adapter = new CustomAdapter(MainActivity.this, arrayMissions);
            listOperations.setAdapter(new CustomAdapter(MainActivity.this, arrayMissions));
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, String, Bitmap> {

        String url;

        public DownloadImageTask(String url) {
            this.url = url;
        }

        protected Bitmap doInBackground(String... strings) {
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bmp;
        }

        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
        }
    }
}


