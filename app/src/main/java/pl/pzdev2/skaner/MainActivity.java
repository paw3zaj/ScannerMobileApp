package pl.pzdev2.skaner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.pzdev2.skaner.kody.IntentIntegrator;
import pl.pzdev2.skaner.kody.IntentResult;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SQLiteDatabase db;
    private Cursor cursor;
    private SimpleCursorAdapter listAdapter;

    SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    private List<String> barcodesList = new ArrayList<>();

    private ListView listView;

    //PRODUCTION IP
//    public static final String URL = "http://153.19.70.197:7323/receive-books-barcode";
    private int requestCode;
    private int resultCode;
//    @Nullable
    private Intent intent;
    //DEVELOPER IP
    public static final String URL = "http://153.19.70.138:8080/receive-books-barcode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sendButton = (Button) findViewById(R.id.send_btn);
        sendButton.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.list_books);

        surfaceView = (SurfaceView) findViewById(R.id.barcode_sv);

        updateListView();

        scanBarcode();
        //
//        updateListView();

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        this.requestCode = requestCode;
        this.resultCode = resultCode;
        this.intent = intent;

//        pobranie wyniku za pomocą klasy IntentResult
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

        updateListView();
        Toast toast = Toast.makeText(this, "Test Wysyłania", Toast.LENGTH_SHORT);
        toast.show();


//        List<String> barcodeList = new ArrayList<String>();
//
//        try {
////                SQLiteDatabase db = databaseHelper.getReadableDatabase();
//            cursor = db.query("BORROWED", new String[]{"BARCODE"}, null, null, null, null, null);
//            if (cursor.moveToFirst()) {
//                do {
//                    //calling the method to save the barcodes to MySQL
//                    barcodeList.add(cursor.getString(0));
//
//                } while (cursor.moveToNext());
//                sendBarcode(barcodeList);
//
//            }
//        } catch (SQLException e) {
//            Toast toast = Toast.makeText(this, "Dane nieosiągalne", Toast.LENGTH_SHORT);
//            toast.show();
//        }
    }

    private void sendBarcode(final List<String> barCode) {

        // Define the POST request
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.POST, URL, new JSONArray(barCode),
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

//                        db.delete("BORROWED", null, null);

                        try {
                            deleteBarcodeVirtua(response);
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

    public void deleteBarcodeVirtua(JSONArray res) throws JSONException {

        for (int i = 0; i < res.length(); i++) {
            JSONObject jsonObject = res.getJSONObject(i);
            String barcode = jsonObject.getString("barCode");

            db.delete("BORROWED",
                    "BARCODE = ?",
                    new String[]{barcode});
        }
    }

    public void updateListView() {

        //Add the listener to the list view
//        listView = (ListView) findViewById(R.id.list_books);

        //Create a cursor
        SQLiteOpenHelper databaseHepler = new DatabaseHelper(this);
        try {
            db = databaseHepler.getReadableDatabase();
            cursor = db.query("BORROWED",
                    new String[]{"_id", "BARCODE"},
                    null, null, null, null, "_id DESC");
            listAdapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_1,
                    cursor,
                    new String[]{"BARCODE"},
                    new int[]{android.R.id.text1},
                    0);
            listView.setAdapter(listAdapter);
//            listAdapter.isEmpty();

        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void onCleanup() {
        if (cursor.getCount() != 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title)
                    .setMessage(R.string.dialog_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deleteListBarcodes();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    private void deleteListBarcodes() {

        DatabaseHelper.deleteAll(db);
        updateListView();

        Toast toast = Toast.makeText(this, "Kody kreskowe usunięte", Toast.LENGTH_SHORT);
        toast.show();
    }

    private void scanBarcode() {

        barcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.CODE_39 | Barcode.EAN_13)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try{
                    if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Toast.makeText(getApplicationContext(), "Aby zapobiec wyciekowi pamięci, skaner kodów kreskowych został zatrzymany", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
//                final List<String> barcodesList = new ArrayList<>();

                if(barcodes.size() != 0) {

                    listView.post(new Runnable() {
                        @Override
                        public void run() {

                            Barcode thisCode = barcodes.valueAt(0);
                            String barcode = thisCode.rawValue;

                            if(!barcodesList.contains(barcode)) {
                                barcodesList.add(barcode);
                                DatabaseHelper.insertBook(db, barcode);
                            }
                            updateListView();
//
//                            Toast.makeText(getApplicationContext(), "size: " + barcodes.size(), Toast.LENGTH_SHORT)
//                                    .show();

                        }
                    });
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
//Sprawdzanie czy został kliknięty przycisk
        switch (v.getId()) {
            case R.id.send_btn:
                onSend();
                break;
        }
    }
}
