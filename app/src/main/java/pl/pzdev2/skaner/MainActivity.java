package pl.pzdev2.skaner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
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

import pl.pzdev2.skaner.kody.IntentIntegrator;
import pl.pzdev2.skaner.kody.IntentResult;

public class MainActivity extends AppCompatActivity { //} implements View.OnClickListener {

    private SQLiteDatabase db;
    private Cursor cursor;

    private Button scanBtn;
    private TextView formatTxt, contentTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        ---------------------------------------------
        //instantiate UI items
        scanBtn = (Button)findViewById(R.id.scan_btn);
        formatTxt = (TextView)findViewById(R.id.barcode_tv);
        contentTxt = (TextView)findViewById(R.id.bookname_tv);

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
                    new String[] {"_id", "NAME"},
                    null, null, null, null, null);
            SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_1,
                    cursor,
                    new String[]{"NAME"},
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
            String scanFormat = scanningResult.getFormatName();
            //wyświetlamy na ekranie aplikacji
            formatTxt.setText("FORMAT: "+scanFormat);
            contentTxt.setText("CONTENT: "+scanContent);
            //zapisuje w SQLite
            DatabaseHelper.insertBook(db, scanContent, scanFormat);
        }
        else{
            //złe dane zostały pobrane z ZXing
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

//    @Override
//    public void onPointerCaptureChanged(boolean hasCapture) {
//
//    }

}
