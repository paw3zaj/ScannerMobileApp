package pl.pzdev2.skaner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import pl.pzdev2.skaner.kody.IntentIntegrator;
import pl.pzdev2.skaner.kody.IntentResult;

public class MainActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private Cursor cursor;

    private Button scanBtn;
    private TextView formatTxt, contentTxt;

    private List<String> barcodeList = new ArrayList<String>();

    public static final String URL = "http://153.19.70.138:8080/receive-list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Add the listener to the list view
        ListView listView = (ListView) findViewById(R.id.list_books);

        //Create a cursor
        SQLiteOpenHelper databaseHepler = new DatabaseHelper(this);
        try {
            db = databaseHepler.getReadableDatabase();
            cursor = db.query("BORROWED",
                    new String[]{"_id", "BARCODE"},
                    null, null, null, null, null);
            SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_1,
                    cursor,
                    new String[]{"BARCODE"},
                    new int[]{android.R.id.text1},
                    0);
            listView.setAdapter(listAdapter);

        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }

    public void onScan(View view) {
        //Sprawdzanie czy został kliknięty przycisk skanowania
        if (view.getId() == R.id.scan_btn) {
            //instantiate ZXing integration class
            IntentIntegrator scanIntegrator = new IntentIntegrator(this);
            //start scanning
            scanIntegrator.initiateScan();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //pobranie wyniku za pomocą klasy IntentResult
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        //sprawdzenie czy mamy poprawny wynik
        if (scanningResult != null) {
            //pobieramy wynik skanowania
            String scanContent = scanningResult.getContents();
            //zapisuje w SQLite
            DatabaseHelper.insertBook(db, scanContent); //, scanFormat);
        } else {
            //złe dane zostały pobrane z ZXing
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void onSend(View view) {

        //Sprawdzanie czy został kliknięty przycisk wysyłania
        if (view.getId() == R.id.send_btn) {

            //getting all the barcodes
            SQLiteOpenHelper databaseHelper = new DatabaseHelper(this);
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query("BORROWED", new String[]{"BARCODE"}, null, null, null, null, null);
                if (cursor.moveToFirst()) {
                    do {
                        //calling the method to save the barcodes to MySQL
                        barcodeList.add(cursor.getString(0));

                    } while (cursor.moveToNext());
                    sendBarcode(barcodeList);

//                    db.delete("BORROWED", null, null);
                }
            } catch (SQLException e) {
                Toast toast = Toast.makeText(this, "Dane nieosiągalne", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    private void sendBarcode(final List<String> barcode) {

        this.barcodeList = barcode;

        // Define the POST request
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.POST, URL, new JSONArray(barcode),
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        db.delete("BORROWED", null, null);
                        Toast.makeText(getApplicationContext(), "Dane przesłane na serwer", Toast.LENGTH_LONG).show();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Błąd przesyłu danych!!!", Toast.LENGTH_LONG).show();
            }
        });

        // Add the request object to the queue to be executed
        MySingleton.getInstance(this).addToRequestQueue(req);
    }
}
