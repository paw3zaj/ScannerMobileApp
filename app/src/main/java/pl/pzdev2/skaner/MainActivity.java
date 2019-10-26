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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.pzdev2.skaner.kody.IntentIntegrator;
import pl.pzdev2.skaner.kody.IntentResult;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SQLiteDatabase db;
    private Cursor cursor;
    private SimpleCursorAdapter listAdapter;

    private Button scanBtn;
    private TextView formatTxt, contentTxt;

    private ListView listView;

    public static final String URL = "http://153.19.70.138:8080/receive-books-barcode";
//public static final String URL = "http://192.168.0.109:8080/receive-books-barcode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sendButton = (Button) findViewById(R.id.send_btn);
        sendButton.setOnClickListener(this);
        Button cleanUpButton = (Button) findViewById(R.id.cleanup_btn);
        cleanUpButton.setOnClickListener(this);
        Button scanButton = (Button) findViewById(R.id.scan_btn);
        scanButton.setOnClickListener(this);

      updateListView();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }

    private void onScan() {

            //instantiate ZXing integration class
            IntentIntegrator scanIntegrator = new IntentIntegrator(this);
            //start scanning
            scanIntegrator.initiateScan();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //pobranie wyniku za pomocą klasy IntentResult
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        //sprawdzenie czy mamy poprawny wynik
        if (scanningResult != null) {
            //pobieramy wynik skanowania
            String scanContent = scanningResult.getContents();
            //zapisuje w SQLite
            if (scanContent != null) {
                DatabaseHelper.insertBook(db, scanContent); //, scanFormat);
            }
            updateListView();
        } else {
            //złe dane zostały pobrane z ZXing
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Błąd skanowania!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void onSend() {

        List<String> barcodeList = new ArrayList<String>();

            try {
//                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                cursor = db.query("BORROWED", new String[]{"BARCODE"}, null, null, null, null, null);
                if (cursor.moveToFirst()) {
                    do {
                        //calling the method to save the barcodes to MySQL
                        barcodeList.add(cursor.getString(0));

                    } while (cursor.moveToNext());
                    sendBarcode(barcodeList);

                }
            } catch (SQLException e) {
                Toast toast = Toast.makeText(this, "Dane nieosiągalne", Toast.LENGTH_SHORT);
                toast.show();
            }
    }

    private void sendBarcode(final List<String> barCode) {

//        this.barcodeList = barcode;

        // Define the POST request
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.POST, URL, new JSONArray(barCode),
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

//                        db.delete("BORROWED", null, null);

                        try {
                            deleteBarcodeFromVirtua(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        updateListView();

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

    public void deleteBarcodeFromVirtua(JSONArray res) throws JSONException {

        for (int i = 0; i < res.length(); i++) {
            JSONObject jsonObject = res.getJSONObject(i);
            String barcode = jsonObject.getString("barCode");

            db.delete("BORROWED",
                "BARCODE = ?",
                new String[] {barcode});
        }
    }

    public void updateListView() {

        //Add the listener to the list view
        listView = (ListView) findViewById(R.id.list_books);

        //Create a cursor
        SQLiteOpenHelper databaseHepler = new DatabaseHelper(this);
        try {
            db = databaseHepler.getReadableDatabase();
            cursor = db.query("BORROWED",
                    new String[]{"_id", "BARCODE"},
                    null, null, null, null, null);
            listAdapter = new SimpleCursorAdapter(this,
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

    private void onCleanup() {

        DatabaseHelper.deleteAll(db);
        updateListView();

        Toast toast = Toast.makeText(this, "Kody kreskowe usunięte", Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void onClick(View v) {
//Sprawdzanie czy został kliknięty przycisk skanowania
        switch (v.getId()) {
            case R.id.send_btn:
                onSend();
                break;
            case R.id.cleanup_btn:
                onCleanup();
                break;
            case  R.id.scan_btn:
                onScan();
                break;
        }
    }
}
