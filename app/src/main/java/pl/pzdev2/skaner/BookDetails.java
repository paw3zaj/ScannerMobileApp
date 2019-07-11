package pl.pzdev2.skaner;


import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class BookDetails extends AppCompatActivity {

    public static final String EXTRA_BORROWID = "borrowId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_details);

        int borrowId = (Integer)getIntent().getExtras().get(EXTRA_BORROWID);

        //Create a cursor
        SQLiteOpenHelper databaseHelper = new DatabaseHelper(this);
        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            Cursor cursor = db.query("BORROWED",
                    new String[] {"NAME", "DESCRIPTION"},
                    "_id = ?",
                    new String[] {Integer.toString(borrowId)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                String nameText = cursor.getString(0);
                String descriptionText = cursor.getString(1);

                TextView name = (TextView)findViewById(R.id.test_TV1);
                name.setText(nameText);

                TextView description = (TextView)findViewById(R.id.test_TV2);
                description.setText(descriptionText);
            }
            cursor.close();
            db.close();
        } catch (SQLException e) {
            Toast toast = Toast.makeText(this, "Dane nieosiągalne", Toast.LENGTH_SHORT);
            toast.show();
        }

    }
}
