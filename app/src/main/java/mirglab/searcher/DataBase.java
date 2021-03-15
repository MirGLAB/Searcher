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

public class DataBase {

    private static final String DB_NAME = "SEARCHER_COORDINATOR_DB";
    private static final int DB_VERSION = 1;

    final String DB_lostInfo = "info";

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
                    "photoURL" + " text" +
                    ");";

    private static final String DB_CREATE_MEMBERS_TABLE =
            "create table " + "Members" + "(" +
                    "_id" + " integer primary key autoincrement, " +
                    "operation" + " text, " +
                    "number" + " text, " +
                    "name" + " text, " +
                    "region" + " text, "  +
                    "info" + " text, " +
                    "groupName" + " text, " +
                    "gotMessage " + " text" +
                    ");";

    private static final String DB_CREATE_GROUPS_TABLE =
            "create table " + "Groups" + "(" +
                    "_id" + " integer, " +
                    "operation" + " text, " +
                    "name" + " text, " +
                    "zone" + " text, " +
                    "color" + " text, " +
                    "markerColor" + " text" +
                    ");";

    private static final String DB_CREATE_MARKERS_TABLE =
            "create table " + "Markers" + "(" +
                    "_id" + " integer primary key autoincrement, " +
                    "operation" + " text, " +
                    "groupName" + " text, " +
                    "memberNumber" + " text, " +
                    "time" + " date, "  +
                    "lat" + " text, " +
                    "lng" + " text, " +
                    "info" + " text, " +
                    "onMap" + " text" +
                    ");";

    private static final String DB_CREATE_SETTINGS_TABLE =
            "create table " + "Settings" + "(" +
                    "_id" + " integer primary key autoincrement, " +
                    "number" + " text" +
                    ");";

    private final Context mCtx;

    private DBHelper mDBHelper;
    private SQLiteDatabase mDB;

    public DataBase(Context ctx) {
        mCtx = ctx;
    }

    // открыть подключение
    public void open() {
        mDBHelper = new DBHelper(mCtx, DB_NAME, null, DB_VERSION);
        mDB = mDBHelper.getWritableDatabase();
    }

    // закрыть подключение
    public void close() {
        if (mDBHelper!=null) mDBHelper.close();
    }

    // получить все данные из таблицы DB_TABLE
    public Cursor getAllData(String tableName) {
        Log.d(LOG_TAG, "from " + tableName + ": " + Integer.toString(mDB.query(tableName, null, null, null, null, null, null).getCount()));
        if(tableName.equals("Operations"))
            return mDB.query(tableName, null, null, null, null, null, "date DESC");
        else
            return mDB.query(tableName, null, null, null, null, null, null);
    }

    public Cursor getIdData(String tableName, String id) {
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

    public Cursor getFieldData(String tableName, String field, int fieldValue) {
        //fieldValue = "'" + fieldValue + "'";
        Log.d(LOG_TAG, "SELECT * FROM " + tableName + " WHERE " + field + " = " + fieldValue);
        return mDB.rawQuery("SELECT * FROM " + tableName + " WHERE " + field + " = " + fieldValue, null);
    }

    public Cursor getColumnFieldData(String tableName, String column, String field, String fieldValue) {
        fieldValue = "'" + fieldValue + "'";
        Log.d(LOG_TAG, "SELECT * FROM " + tableName + " WHERE " + field + " = " + fieldValue);
        return mDB.rawQuery("SELECT " + column + " FROM " + tableName + " WHERE " + field + " = " + fieldValue, null);
    }

    public void deleteFiledData(String tableName, String field, String fieldValue) {
        fieldValue = "'" + fieldValue + "'";
        Log.d(LOG_TAG, "DELETE FROM " + tableName + " WHERE " + field + " = " + fieldValue);
        mDB.delete(tableName, field + " = " + fieldValue, null);
    }

    public void modifyOperationsFieldData(String tableName, String param, String paramValue, String field, String fieldValue) {
        paramValue = "'" + paramValue + "'";
        Log.d(LOG_TAG, "Modifying data in " + tableName);
        ContentValues cv = new ContentValues();
        cv.put(field, fieldValue);
        mDB.update(tableName, cv, param + "=" + paramValue, null);
        //mDB.update("Groups", cv, "operation" + "=" + operationID + " AND name=" + groupName, null);
    }

    public void modifyFieldData(String tableName, String operationID, String param, String paramValue, String field, String fieldValue) {
        paramValue = "'" + paramValue + "'";
        Log.d(LOG_TAG, "Modifying data in " + tableName);
        ContentValues cv = new ContentValues();
        cv.put(field, fieldValue);
        mDB.update(tableName, cv, "operation" + "=" + operationID + " AND " + param + "=" + paramValue, null);
        //mDB.update("Groups", cv, "operation" + "=" + operationID + " AND name=" + groupName, null);
    }

    public void modifyFieldData(String tableName, String operationID, String param, int paramValue, String field, String fieldValue) {
        Log.d(LOG_TAG, "Modifying data in " + tableName);
        ContentValues cv = new ContentValues();
        cv.put(field, fieldValue);
        mDB.update(tableName, cv, "operation" + "=" + operationID + " AND " + param + "=" + paramValue, null);
    }

    public Cursor getMarkersData(String operationID, String groupName) {
        Log.d(LOG_TAG, "get Markers data");
        return  mDB.rawQuery("SELECT * FROM Markers WHERE operation = '" + operationID +
                "' AND WHERE groupName = '" + groupName + "'", null);
    }

    public void addFieldData(String tableName, String param, String paramValue, String operationID, String field, String fieldValue) {
        Log.d(LOG_TAG, "Adding data in " + tableName);
        paramValue = "'" + paramValue + "'";
        Log.d(LOG_TAG, "SELECT * FROM " + tableName + " WHERE " + param + " = " + paramValue);
        Cursor cursor = mDB.rawQuery("SELECT * FROM " + tableName + " WHERE " + param + " = " + paramValue
                + " AND operation = " + operationID, null);
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
        mDB.update(tableName, cv, param + "=" + paramValue + " AND operation=" + operationID, null);
    }

    public void removeZoneFromGroup(String operationID, String groupName, String zone) {
        //Log.d(LOG_TAG, "Adding data in " + tableName);
        //Log.d(LOG_TAG, "SELECT * FROM " + tableName + " WHERE " + param + " = " + paramValue);
        groupName = "'" + groupName + "'";
        Cursor cursor = mDB.rawQuery("SELECT * FROM Groups WHERE operation = " + operationID + " AND name = " + groupName, null);
        String fieldOldValue = "";
        if(cursor.moveToFirst()) {
            int zoneColIndex = cursor.getColumnIndex("zone");
            do {
                fieldOldValue = cursor.getString(zoneColIndex);
            } while (cursor.moveToNext());
        } else
            Log.d(LOG_TAG, "fieldOldValue - 0 rows");
        cursor.close();

        if(fieldOldValue == null)
            fieldOldValue = "";

        String[] zones = fieldOldValue.split("\\@");
        for (int i = 0; i < zones.length; i++) {
            if (zone.equals(zones[i])) {
                zones[i] = "";
            }
        }

        String fieldValue = "";
        for (int i = 0; i < zones.length; i++) {
            if(!zones[i].equals(""))
                fieldValue += zones[i] + "@";
        }

        ContentValues cv = new ContentValues();
        cv.put("zone", fieldValue);
        Log.d(LOG_TAG, "fieldOldValue: " + fieldOldValue);
        Log.d(LOG_TAG, "fieldNewValue: " + fieldValue);
        mDB.update("Groups", cv, "operation" + "=" + operationID + " AND name=" + groupName, null);
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

        //int id = Integer.parseInt(info.get(0));

        //создаем объект для данных
        //Класс ContentValues используется для указания полей таблицы и значений, которые мы в эти поля будем вставлять.
        String message = "";
        for(int i = 0;i < info.size()-1; i++)
            message += info.get(i) + ", ";
        message += info.get(info.size()-1);
        Log.d(LOG_TAG, "Adding rec {" + message + "} to " + tableName);

        ContentValues cv = new ContentValues();
        long rowID;
        switch (tableName) {
            case "Operations":
                if(getIdData(tableName, info.get(0)).getCount() == 0) {
                    cv.put("_id", Long.parseLong(info.get(0)));
                    //cv.put("_id", Integer.parseInt(info.get(0)));
                    cv.put("date", info.get(1));
                    cv.put("info", info.get(2));
                    cv.put("description", info.get(3));
                    cv.put("lostPhotos", info.get(4));
                    //cv.put("photoURL", info.get(5));
                    cv.put("centerLat", "-");
                    cv.put("centerLng", "-");
                    rowID = mDB.insert(tableName, null, cv);
                    Log.d(LOG_TAG, "row inserted in table " + tableName + ", ID = " + rowID);
                }
                break;
            case "Members":
                if(getFieldData(tableName, "number", info.get(1)).getCount() == 0) {
                    Toast.makeText(mCtx, "Получены данные о новом участнике операции", Toast.LENGTH_LONG).show();
                    cv.put("operation", info.get(0));
                    cv.put("number", info.get(1));
                    cv.put("name", info.get(2));
                    cv.put("region", info.get(3));
                    cv.put("info", info.get(4));
                    cv.put("groupName", "-");
                    cv.put("gotMessage", "false");
                    rowID = mDB.insert(tableName, null, cv);
                    Log.d(LOG_TAG, "row inserted in table " + tableName + ", ID = " + rowID);
                } else {
                    boolean inDB = false;
                    Cursor cursor = getFieldData(tableName, "number", info.get(1));
                    if(cursor.moveToFirst()) {
                        do {
                            if (cursor.getString(cursor.getColumnIndex("operation")).equals(info.get(0)))
                                inDB = true;
                        } while(cursor.moveToNext());
                    }
                    cursor.close();
                    if(!inDB) {
                        Toast.makeText(mCtx, "Получены данные о новом участнике операции", Toast.LENGTH_LONG).show();
                        cv.put("operation", info.get(0));
                        cv.put("number", info.get(1));
                        cv.put("name", info.get(2));
                        cv.put("region", info.get(3));
                        cv.put("info", info.get(4));
                        cv.put("groupName", "-");
                        cv.put("gotMessage", "false");
                        rowID = mDB.insert(tableName, null, cv);
                        Log.d(LOG_TAG, "row inserted in table " + tableName + ", ID = " + rowID);
                    }
                }
                break;
            case "Groups":
                int groupCount = getFieldData("Groups", "operation", info.get(0)).getCount();
                cv.put("_id", groupCount + 1);
                cv.put("operation", info.get(0));
                cv.put("name", info.get(1) + "-" + Integer.toString(groupCount+1));
                cv.put("color", "-");
                cv.put("markerColor", "-");
                rowID = mDB.insert(tableName, null, cv);
                Log.d(LOG_TAG, "row inserted in table " + tableName + ", ID = " + rowID);
                break;
            case "Markers":
                boolean add = true;
                Cursor cursor = getFieldData(tableName, "operation", info.get(0));
                if(cursor.moveToFirst()) {
                    int groupColIndex = cursor.getColumnIndex("groupName");
                    int timeColIndex = cursor.getColumnIndex("time");
                    do {
                        String group = cursor.getString(groupColIndex);
                        String time = cursor.getString(timeColIndex);
                        if(group.equals(info.get(1)) && time.equals(info.get(3))) {
                            add = false;
                            break;
                        }
                    } while(cursor.moveToNext());
                }
                cursor.close();
                if(add) {
                    cv.put("operation", info.get(0));
                    cv.put("groupName", info.get(1));
                    cv.put("memberNumber", info.get(2));
                    cv.put("time", info.get(3));
                    cv.put("lat", info.get(4));
                    cv.put("lng", info.get(5));
                    cv.put("info", info.get(6));
                    cv.put("onMap", "false");
                    rowID = mDB.insert(tableName, null, cv);
                    Log.d(LOG_TAG, "row inserted in table " + tableName + ", ID = " + rowID);
                } else{
                    Log.d(LOG_TAG, "this marker already in db");
                }
                break;
            default:
                Log.d(LOG_TAG, "Wrong table name");
                break;
        }
        cv.clear();
    }

    // удалить запись из DB_TABLE
    public void delRec(String tableName, long id) {
        mDB.delete(tableName, "_id" + " = " + id, null);
    }

    public void delTable(String tableName) {
        mDB.execSQL("DROP TABLE IF EXISTS " + tableName);
        Log.d(LOG_TAG, "Table " + tableName + " deleted");
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
            db.execSQL(DB_CREATE_MEMBERS_TABLE);
            db.execSQL(DB_CREATE_GROUPS_TABLE);
            db.execSQL(DB_CREATE_MARKERS_TABLE);
            //db.execSQL(DB_CREATE_SETTINGS_TABLE);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {

            Log.d(LOG_TAG, "DB onOpen");

            //Log.d(LOG_TAG, Boolean.toString(isTableExists(db, "Operations")));
            //db.execSQL("DROP TABLE IF EXISTS Operations");
            Log.d(LOG_TAG, Boolean.toString(isTableExists(db, "Operations")));
            //db.execSQL("DROP TABLE IF EXISTS Members");
            //db.execSQL("DROP TABLE IF EXISTS Operations");

            if(!isTableExists(db, "Operations")) {
                db.execSQL(DB_CREATE_OPERATIONS_TABLE);
            }

            if(!isTableExists(db, "Members")) {
                db.execSQL(DB_CREATE_MEMBERS_TABLE);
            }

            if(!isTableExists(db, "Groups")) {
                db.execSQL(DB_CREATE_GROUPS_TABLE);
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