package pl.pzdev2.skaner;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "statistics";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE BORROWED ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "BARCODE TEXT, "
                + "CREATED_DATE TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    //Metoda 'customer' dodaje rekord do SQLite
    public static void insertBook(SQLiteDatabase db, String barcode, String createdDate) {

        ContentValues bookValue = new ContentValues();
        bookValue.put("BARCODE", barcode);
        bookValue.put("CREATED_DATE", createdDate);
        db.insert("BORROWED", null, bookValue);
    }

    public static void deleteAll(SQLiteDatabase db) {
        db.execSQL("delete from BORROWED");
    }

}
