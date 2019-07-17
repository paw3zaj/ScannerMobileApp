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
                + "NAME TEXT, "
                + "DESCRIPTION TEXT);");
        insertBook(db, "Tomcio Paluch", "Dla dzieci");
        insertBook(db, "Stry człowiek i morze", "Dla wszystkich");
        insertBook(db, "O smokach", "Baśnie dla dzieci");
        insertBook(db, "O 2 takich co ...", "Książka Kornela Makuszyńskiego");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    //Metoda 'customer' dodaje rekord do SQLite
    public static void insertBook(SQLiteDatabase db, String name,
                                   String description) {

        ContentValues bookValue = new ContentValues();
        bookValue.put("NAME", name);
        bookValue.put("DESCRIPTION", description);
        db.insert("BORROWED", null, bookValue);
    }
}
