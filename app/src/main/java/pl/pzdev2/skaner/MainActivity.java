package pl.pzdev2.skaner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
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
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.pzdev2.skaner.kody.IntentIntegrator;
import pl.pzdev2.skaner.kody.IntentResult;

public class MainActivity extends AppCompatActivity { //} implements View.OnClickListener {

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

//        ---------------------------------------------
        //instantiate UI items
        scanBtn = (Button)findViewById(R.id.scan_btn);
//        formatTxt = (TextView)findViewById(R.id.barcode_tv);
//        contentTxt = (TextView)findViewById(R.id.bookname_tv);

        //listen for clicks
//        scanBtn.setOnClickListener(this);
//        -----------------------------------------------

        //Add the listener to the list view
        ListView listView = (ListView) findViewById(R.id.list_books);

        //Create a cursor
        SQLiteOpenHelper databaseHepler = new DatabaseHelper(this);
        try {
            db = databaseHepler.getReadableDatabase();
            cursor = db.query("BORROWED",
                    new String[] {"_id", "BARCODE"},
                    null, null, null, null, null);
            SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_1,
                    cursor,
                    new String[]{"BARCODE"},
                    new int[]{android.R.id.text1},
                    0);
            listView.setAdapter(listAdapter);

        } catch(SQLiteException e) {
            Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }

        //Create an OnItemClickListener
        AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> listView, View itemView, int position, long id) {

                //Pass the drink the user clicks on to BookDetails
                Intent intent = new Intent(MainActivity.this,
                        BookDetails.class);
                intent.putExtra(BookDetails.EXTRA_BORROWID, (int) id);
                startActivity(intent);

//                if(position == 0) {
//                    Intent intent = new Intent(MainActivity.this, BookDetails.class);
//                    startActivity(intent);
//                }
            }
        };
        listView.setOnItemClickListener(itemClickListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }

//    --------------------------------------------------------------------------

//    public void onClick(View v){
//        //Sprawdzanie czy został kliknięty przycisk skanowania
//        if(v.getId()==R.id.scan_btn){
//            //instantiate ZXing integration class
//            IntentIntegrator scanIntegrator = new IntentIntegrator(this);
//            //start scanning
//            scanIntegrator.initiateScan();
//        }
//    }

    public void onScan(View view) {
        //Sprawdzanie czy został kliknięty przycisk skanowania
        if(view.getId()==R.id.scan_btn) {
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
            //pobieramy format kodu skanowania
//            String scanFormat = scanningResult.getFormatName();
            //wyświetlamy na ekranie aplikacji
//            formatTxt.setText("FORMAT: "+scanFormat);
//            contentTxt.setText("CONTENT: "+scanContent);
            //zapisuje w SQLite
            DatabaseHelper.insertBook(db, scanContent); //, scanFormat);
        }
        else{
            //złe dane zostały pobrane z ZXing
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void onSend(View view) {



        //Sprawdzanie czy został kliknięty przycisk wysyłania
        if (view.getId() == R.id.send_btn) {

            //getting all the unsynced names
            SQLiteOpenHelper databaseHelper = new DatabaseHelper(this);
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
            Cursor cursor = db.query("BORROWED",new String[] {"BARCODE"}, null,null, null, null,null);
            if (cursor.moveToFirst()) {
                do {
                    //calling the method to save the unsynced name to MySQL
                    // sendBarcode(
//                            cursor.getString(cursor.getColumnIndex(String.valueOf(DatabaseHelper.BARCODE)))
                        barcodeList.add(cursor.getString(0));
                        
                } while (cursor.moveToNext());
//            sendRequest();!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                sendBarcode(barcodeList);

            }
            } catch (SQLException e) {
                Toast toast = Toast.makeText(this, "Dane nieosiągalne", Toast.LENGTH_SHORT);
                toast.show();
            }
        }

    }
        private void sendBarcode(final List<String> barcode) {

            this.barcodeList = barcode;

            // POST params to be sent to the server
            // Map<String, String> params = new HashMap<String, String>();
            // params.put("barcode", barcode);

            // Define the POST request
            JsonArrayRequest req = new JsonArrayRequest(Request.Method.POST, URL, new JSONArray(barcode),
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {

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
