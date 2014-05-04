package ru.sawim.io;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ru.sawim.SawimApplication;
import ru.sawim.comm.StringConvertor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;


public final class Storage {
    private static final String TABLE_NAME = "recordstore";
    public static final String COLUMN_ID = "_id";
    private static final String COLUMN_DATA = "data";
    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private String name;

    public static String[] getList() {
        Context context = SawimApplication.getInstance();
        return context.databaseList();
    }

    public static void delete(String recordStoreName) {
        Context context = SawimApplication.getInstance();
        context.deleteDatabase(recordStoreName);
    }

    public void dropTable() {
        dbHelper.dropTable(db);
    }

    public Storage(String recordStoreName) {
        name = recordStoreName;
        Context context = SawimApplication.getContext();
        String SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATA + " BLOB)";
        dbHelper = new DatabaseHelper(context, recordStoreName, SQL_CREATE_ENTRIES);
    }

    public Storage(String recordStoreName, String sqlCreateEntries) {
        name = recordStoreName;
        Context context = SawimApplication.getContext();
        dbHelper = new DatabaseHelper(context, recordStoreName, sqlCreateEntries);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
        StorageConvertor.convertStorage(name, this);
    }

    public void close() {
        dbHelper.close();
    }

    public void addRecord(byte data[]) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA, data);
        db.insert(TABLE_NAME, null, values);
    }

    public void addRecord(String table, ContentValues values) {
        db.insert(table, null, values);
    }

    public byte[] getRecord(int id) {
        String where = COLUMN_ID + " = " + id;
        Cursor cursor = db.query(TABLE_NAME, null, where, null, null, null, null);
        cursor.moveToFirst();
        return cursor.getBlob(cursor.getColumnIndex(COLUMN_DATA));
    }

    public void setRecord(int id, byte data[]) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA, data);
        String where = COLUMN_ID + " = " + id;
        db.update(TABLE_NAME, values, where, null);
    }

    public void deleteRecord(int id) {
        String where = COLUMN_ID + " = " + id;
        db.delete(TABLE_NAME, where, null);
    }

    public int getNumRecords() {
        String selectCount = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Cursor cur = db.rawQuery(selectCount, null);
        if (cur.moveToFirst()) {
            return cur.getInt(0);
        }
        return 0;
    }

    public boolean exist() {
        String[] recordStores = Storage.getList();
        for (String recordStore : recordStores) {
            if (name.equals(recordStore)) {
                return true;
            }
        }
        return false;
    }

    public void saveListOfString(Vector<String> strings) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (String str : strings) {
                dos.writeUTF(StringConvertor.notNull(str));
                addRecord(baos.toByteArray());
                baos.reset();
            }
            baos.close();
        } catch (Exception ignored) {
        }
    }

    public Vector loadListOfString() {
        Vector<String> strings = new Vector<String>(getNumRecords());
        try {
            for (int i = 0; i < getNumRecords(); ++i) {
                byte[] data = getRecord(i + 1);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);
                strings.addElement(dis.readUTF());
                bais.close();
            }
        } catch (Exception ignored) {
        }
        return strings;
    }

    public void saveXStatuses(String[] titles, String[] descs) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (int i = 0; i < titles.length; ++i) {
                dos.writeUTF(StringConvertor.notNull(titles[i]));
                dos.writeUTF(StringConvertor.notNull(descs[i]));
            }
            putRecord(1, baos.toByteArray());
        } catch (Exception ignored) {
        }
    }

    public void loadXStatuses(String[] titles, String[] descs) {
        try {
            byte[] buf = getRecord(1);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream dis = new DataInputStream(bais);
            for (int i = 0; i < titles.length; ++i) {
                titles[i] = StringConvertor.notNull(dis.readUTF());
                descs[i] = StringConvertor.notNull(dis.readUTF());
            }
        } catch (Exception ignored) {
        }
    }

    private void initRecords(int count) throws Exception {
        if (getNumRecords() < count) {
            if ((1 < count) && (0 == getNumRecords())) {
                byte[] version = StringConvertor.stringToByteArrayUtf8(SawimApplication.VERSION);
                addRecord(version);
            }
            while (getNumRecords() < count) {
                addRecord(new byte[0]);
            }
        }
    }

    private void putRecord(int num, byte[] data) throws Exception {
        initRecords(num);
        setRecord(num, data);
    }

    private static final class DatabaseHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 3;
        private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;

        private String sqlCreateEntries;

        private DatabaseHelper(Context context, String recordStoreName, String sqlCreateEntries) {
            super(context, recordStoreName, null, DATABASE_VERSION);
            this.sqlCreateEntries = sqlCreateEntries;
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(sqlCreateEntries);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            dropTable(db);
        }

        public void dropTable(SQLiteDatabase db) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }
}

