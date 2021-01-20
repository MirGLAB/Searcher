package mirglab.searcher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class V_DataBase {

    private static final String DB_NAME = "Volunteer";
    private static final int DB_VERSION = 1;

    final String LOG_TAG = "____";

    private static final String DB_CREATE_OPERATIONS_TABLE =
            "create table " + "Operations" + "(" +
                    "_id" + " integer primary key, " +
                    "date" + " date, " +
                    "info" + " text, " +
                    "description" + " text, " +
                    "lostPhotos" + " text, " +
                    "centerLat" + " text, " +
                    "centerLng" + " text, " +
                    "number" + " text, " +
                    "groupName" + " text, " +
                    "groupZone" + " text, " +
                    "lat" + " text, " +
                    "lng" + " text" +
                    ");";

    private static final String DB_CREATE_USER_TABLE =
            "create table " + "User" + "(" +
                    "number" + " text, " +
                    "name" + " text, " +
                    "region" + " text, "  +
                    "info" + " text" +
                    ");";

    private static final String DB_CREATE_MARKERS_TABLE =
            "create table " + "Markers" + "(" +
                    "_id" + " integer primary key autoincrement, " +
                    "operation" + " text, " +
                    "time" + " text, "  +
                    "lat" + " text, " +
                    "lng" + " text, " +
                    "onMap" + " text" +
                    ");";

    private final Context mCtx;

    private DBHelper mDBHelper;
    private SQLiteDatabase mDB;

    public V_DataBase(Context ctx) {
        mCtx = ctx;
    }

    // открыть подключение
    public void open() {
        mDBHelper = new DBHelper(mCtx, DB_NAME, null, DB_VERSION);
        mDB = mDBHelper.getWritableDatabase();
    }

    // закрыть подключение
    public void close() {
        if (mDBHelper != null) mDBHelper.close();
    }

    // получить все данные из таблицы DB_TABLE
    public Cursor getAllData(String tableName) {
        if(tableName.equals("Operations"))
            return mDB.query(tableName, null, null, null, null, null, "date DESC");
        else
            return mDB.query(tableName, null, null, null, null, null, null);
    }

    public Cursor getIdData(String tableName, String id) {
        //return mDB.rawQuery("SELECT * FROM table_name=? WHERE _id=?", new String[] {table_name, id});
        //return mDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + table_name + "' AND _id = '" + id + "'", null);
        return mDB.rawQuery("SELECT * FROM " + tableName + " WHERE _id = " + id, null);
    }

    public Cursor getFieldData(String tableName, String field, String fieldValue) {
        if(tableName.equals("Markers")) {
            fieldValue = "'" + fieldValue + "'";
            Log.d(LOG_TAG, "SELECT * FROM " + tableName + " WHERE " + field + " = " + fieldValue);
            return mDB.rawQuery("SELECT * FROM " + tableName + " WHERE " + field + " = " + fieldValue +
                    " ORDER BY time ASC", null, null);
        } else {
            fieldValue = "'" + fieldValue + "'";
            Log.d(LOG_TAG, "SELECT * FROM " + tableName + " WHERE " + field + " = " + fieldValue);
            return mDB.rawQuery("SELECT * FROM " + tableName + " WHERE " + field + " = " + fieldValue, null);
        }
    }

    public void deleteFiledData(String tableName, String field, String fieldValue) {
        fieldValue = "'" + fieldValue + "'";
        Log.d(LOG_TAG, "DELETE FROM " + tableName + " WHERE " + field + " = " + fieldValue);
        mDB.delete(tableName, field + " = " + fieldValue, null);
    }

    public void modifyFieldData(String tableName, String param, String paramValue, String field, String fieldValue) {
        paramValue = "'" + paramValue + "'";
        Log.d(LOG_TAG, "Modifying data in " + tableName);
        ContentValues cv = new ContentValues();
        cv.put(field, fieldValue);
        mDB.update(tableName, cv, param + "=" + paramValue, null);
    }

    public void modifyFieldData(String tableName, String param, int paramValue, String field, String fieldValue) {
        Log.d(LOG_TAG, "Modifying data in " + tableName);
        ContentValues cv = new ContentValues();
        cv.put(field, fieldValue);
        mDB.update(tableName, cv, param + "=" + paramValue, null);
    }

    public void addFieldData(String tableName, String param, String paramValue, String field, String fieldValue) {
        Log.d(LOG_TAG, "Adding data in " + tableName);
        paramValue = "'" + paramValue + "'";
        Log.d(LOG_TAG, "SELECT * FROM " + tableName + " WHERE " + param + " = " + paramValue);
        Cursor cursor = mDB.rawQuery("SELECT * FROM " + tableName + " WHERE " + param + " = " + paramValue, null);
        String fieldOldValue = "";
        if(cursor.moveToFirst()) {
            int nameColIndex = cursor.getColumnIndex(field);
            do {
                fieldOldValue = cursor.getString(nameColIndex);
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "fieldOldValue - 0 rows");
        cursor.close();

        if(fieldOldValue == null)
            fieldOldValue = "";

        ContentValues cv = new ContentValues();
        cv.put(field, fieldOldValue + "" + fieldValue);
        Log.d(LOG_TAG, "fieldOldValue: " + fieldOldValue);
        Log.d(LOG_TAG, "fieldNewValue: " + fieldValue);
        mDB.update(tableName, cv, param + "=" + paramValue, null);
    }

    public void addFieldData(String tableName, String param, int paramValue, String field, String fieldValue) {
        Log.d(LOG_TAG, "Adding data in " + tableName);
        Log.d(LOG_TAG, "SELECT * FROM " + tableName + " WHERE " + param + " = " + paramValue);
        Cursor cursor = mDB.rawQuery("SELECT * FROM " + tableName + " WHERE " + param + " = " + paramValue, null);
        String fieldOldValue = "";
        if(cursor.moveToFirst()) {
            int nameColIndex = cursor.getColumnIndex(field);
            do {
                fieldOldValue = cursor.getString(nameColIndex);
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "fieldOldValue - 0 rows");
        cursor.close();

        if(fieldOldValue == null)
            fieldOldValue = "";

        ContentValues cv = new ContentValues();
        cv.put(field, fieldOldValue + "" + fieldValue);
        Log.d(LOG_TAG, "fieldOldValue: " + fieldOldValue);
        Log.d(LOG_TAG, "fieldNewValue: " + fieldValue);
        mDB.update(tableName, cv, param + "=" + paramValue, null);
    }

    public Cursor getMarkersData(String operationID, String lat, String lng) {
        Log.d(LOG_TAG, "get Markers data");
        return  mDB.rawQuery("SELECT * FROM Markers WHERE operation = '" + operationID +
                "' AND WHERE lat = '" + lat + "'" + "' AND WHERE lng = '" + lng + "'", null);
    }

    // добавить запись в DB_TABLE
    public void addRec(String tableName, ArrayList<String> info) {

        /*
        FIO = "Иванов Иван Иванович";
        location = "г. Зеленоград";
        age = "31 год";
        lastSaw = "ул. Юности, к.19";
        height = "167";
        body = "толстенький";
        eye = "голубые";
        hair = "темные";
        outfit = "белые брюки";
        signs = "розовые очки";
        */

        for (int k = 0; k < info.size(); k++)
            Log.d(LOG_TAG, info.get(k));

        //int id = Integer.parseInt(info.get(0));
        //if(getIdData(tableName, info.get(0)).getCount() == 0) {

        //создаем объект для данных
        //Класс ContentValues используется для указания полей таблицы и значений, которые мы в эти поля будем вставлять.
        ContentValues cv = new ContentValues();
        switch (tableName) {
            case "Operations":
                if(getIdData(tableName, info.get(0)).getCount() == 0) {
                    Toast.makeText(mCtx, "Получены данные новой операции", Toast.LENGTH_LONG).show();
                    cv.put("_id", Integer.parseInt(info.get(0)));
                    cv.put("date", info.get(1));
                    cv.put("info", info.get(2));
                    cv.put("description", info.get(3));
                    cv.put("lostPhotos", info.get(7));
                    cv.put("centerLat", info.get(4));
                    cv.put("centerLng", info.get(5));
                    cv.put("number", info.get(6));
                    cv.put("groupName", "-");
                    cv.put("groupZone", "");
                    cv.put("lat", "");
                    cv.put("lng", "");
                    long rowID = mDB.insert(tableName, null, cv);
                    Log.d(LOG_TAG, "row inserted in table " + tableName + ", ID = " + rowID);
                }
                break;
            case "User":
                delTable("User");
                mDB.execSQL(DB_CREATE_USER_TABLE);
                cv.put("number", info.get(0));
                cv.put("name", info.get(1));
                cv.put("region", info.get(2));
                cv.put("info", info.get(3));

                //mDB.execSQL("DROP TABLE IF EXISTS User");
                //mDB.execSQL(DB_CREATE_USER_TABLE);

                long rowID = mDB.insert(tableName, null, cv);
                Log.d(LOG_TAG, "row inserted in table " + tableName + ", ID = " + rowID);
                break;
            case "Markers":
                //int id = getFieldData("Markers", "operation", info.get(0)).getCount();
                //cv.put("_id", id);
                boolean add = true;
                Cursor cursor = getFieldData(tableName, "operation", info.get(0));
                if(cursor.moveToFirst()) {
                    int timeColIndex = cursor.getColumnIndex("time");
                    do {
                        String time = cursor.getString(timeColIndex);
                        if(time.equals(info.get(1))) {
                            add = false;
                            break;
                        }
                    } while(cursor.moveToNext());
                }
                cursor.close();
                if(add) {
                    cv.put("operation", info.get(0));
                    cv.put("time", info.get(1));
                    cv.put("lat", info.get(2));
                    cv.put("lng", info.get(3));
                    cv.put("onMap", "false");
                    rowID = mDB.insert(tableName, null, cv);
                    Log.d(LOG_TAG, "row inserted in table " + tableName + ", ID = " + rowID);
                } else {
                    Log.d(LOG_TAG, "this marker already in db");
                }
                break;
            default:
                Log.d(LOG_TAG, "Wrong table name");
                break;
        }
        cv.clear();
    }

    public void delTable(String tableName) {
        mDB.execSQL("DROP TABLE IF EXISTS " + tableName);
        Log.d(LOG_TAG, "Table " + tableName + " deleted");
    }

    // удалить запись из DB_TABLE
    public void delRec(String tableName, long id) {
        mDB.delete(tableName, "_id" + " = " + id, null);
    }

    public String getDBFullName() {
        Log.d(LOG_TAG, mDB.getPath());
        return mDB.getPath();
    }

    // класс по созданию и управлению БД
    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context, String name, CursorFactory factory,
                        int version) {
            super(context, name, factory, version);
        }

        // создаем и заполняем БД
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_CREATE_OPERATIONS_TABLE);
            db.execSQL(DB_CREATE_USER_TABLE);
            db.execSQL(DB_CREATE_MARKERS_TABLE);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {

            Log.d(LOG_TAG, "DB onOpen");

            //Log.d(LOG_TAG, Boolean.toString(isTableExists(db, "Operations")));
            //db.execSQL("DROP TABLE IF EXISTS Operations");
            //db.execSQL("DROP TABLE IF EXISTS User");
            Log.d(LOG_TAG, "Operations - " + Boolean.toString(isTableExists(db, "Operations")));
            Log.d(LOG_TAG, "User - " + Boolean.toString(isTableExists(db, "User")));
            //db.execSQL("DROP TABLE IF EXISTS User");
            //Log.d(LOG_TAG, "User - " + Boolean.toString(isTableExists(db, "User")));


            if(!isTableExists(db, "Operations")) {
                db.execSQL(DB_CREATE_OPERATIONS_TABLE);
            }

            if(!isTableExists(db, "User")) {
                db.execSQL(DB_CREATE_USER_TABLE);
            }

            if(!isTableExists(db, "Markers")) {
                db.execSQL(DB_CREATE_MARKERS_TABLE);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        public boolean isTableExists(SQLiteDatabase db, String tableName) {

            Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.close();
                    return true;
                }
                cursor.close();
            }
            return false;
        }
    }
}