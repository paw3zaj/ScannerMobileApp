package pl.pzdev2.skaner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    //DEVELOPER IP
    public static final String URL = "http://153.19.70.138:8080/receive-books-barcode";
    private int requestCode;
    private int resultCode;
    private Intent intent;


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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
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

        // Define the POST request
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.POST, URL, new JSONArray(barCode),
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        DatabaseHelper.deleteAll(db);

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

    public void updateListView() {

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
            Toast toast = Toast.makeText(this, "Baza danych SQLite niedostępna", Toast.LENGTH_SHORT);
            toast.show();
        }
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
