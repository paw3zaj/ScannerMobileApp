package pl.pzdev2.skaner;

import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketException;
import java.util.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SQLiteDatabase db;
    private Cursor cursor;
    private SQLiteOpenHelper databaseHelper;
    private Button sendButton;
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private Map<String, Integer> barcodeMap;
    private ListView listView;
    private static final String URL = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        barcodeMap = new HashMap<>();
        databaseHelper = new DatabaseHelper(this);

        sendButton = (Button) findViewById(R.id.send_btn);
        sendButton.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.list_books);
        surfaceView = (SurfaceView) findViewById(R.id.barcode_sv);

        updateListView();
        scanBarcode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeMap.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }

    private void onSend() {
        JSONArray jar = new JSONArray();
        try {
            cursor = db.query("BORROWED", new String[]{"BARCODE", "CREATED_DATE"}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                disableButton();
                do {
                    jar.put(new JSONObject(new HashMap<String, String>() {{
                        put("barcode", cursor.getString(0));
                        put("createdDate", cursor.getString(1));
                    }}));
                } while (cursor.moveToNext());
                sendBarcode(jar);
            }
        } catch (SQLException e) {
            Toast.makeText(this, "Dane nieosiągalne", Toast.LENGTH_SHORT).show();
            enableButton();
        }
    }

    private void sendBarcode(JSONArray barcodeList) {

        // Define the POST request
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.POST, URL, barcodeList,
                response -> {

                    DatabaseHelper.deleteAll(db);

                    updateListView();

                    Toast.makeText(getApplicationContext(), "Dane przesłane na serwer", Toast.LENGTH_LONG).show();

                    enableButton();

                }, error -> {

            if (error instanceof NoConnectionError) {
                Toast.makeText(getApplicationContext(),
                        "Sieć wifi niedostępna",
                        Toast.LENGTH_LONG).show();
            } else if (error instanceof TimeoutError) {
                Toast.makeText(getApplicationContext(),
                        "Przekroczony czas oczekiwania na połączenie z siecią",
                        Toast.LENGTH_LONG).show();
            } else if (error instanceof AuthFailureError) {
                Toast.makeText(getApplicationContext(),
                        "Błąd poświadczenia",
                        Toast.LENGTH_LONG).show();
            } else if (error instanceof ServerError) {
                Toast.makeText(getApplicationContext(),
                        "Błąd serwera",
                        Toast.LENGTH_LONG).show();
            } else if (error instanceof NetworkError) {
                Toast.makeText(getApplicationContext(),
                        "Błąd połączenia",
                        Toast.LENGTH_LONG).show();
            } else if (error instanceof ParseError) {
                Toast.makeText(getApplicationContext(),
                        "ParseError",
                        Toast.LENGTH_LONG).show();
            } else if (error.getCause() instanceof SocketException) {
                Toast.makeText(getApplicationContext(),
                        "SocketException",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
            }
            enableButton();
        });

        // avoid volley sending data twice bug
        req.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Add the request object to the queue to be executed
        MySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void updateListView() {
        try {
            db = databaseHelper.getReadableDatabase();
            cursor = db.query("BORROWED",
                    new String[]{"_id", "BARCODE"},
                    null, null, null, null, "_id DESC");
            SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_1,
                    cursor,
                    new String[]{"BARCODE"},
                    new int[]{android.R.id.text1},
                    0);
            listView.setAdapter(listAdapter);
        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(this, "Baza danych SQLite niedostępna", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void scanBarcode() {

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.CODE_39 | Barcode.EAN_13)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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

                if (barcodes.size() != 0) {

                    listView.post(() -> {

                        Barcode thisCode = barcodes.valueAt(0);
                        String barcode = thisCode.rawValue;

                        try {
                                if (counter(barcode)) {

                                    DatabaseHelper.insertBook(db, barcode, FormatDateTime.dateTime());

                                    Vibrator v = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);
                                    // Vibrate for 200 milliseconds
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                                    } else {
                                        //deprecated in API 26
                                        v.vibrate(200);
                                    }
                                }
                        } catch (NumberFormatException e) {
                            Toast.makeText(getApplicationContext(),
                                    "Problem z zeskanowaniem kodu kreskowego",
                                    Toast.LENGTH_LONG).show();
                        }
                        updateListView();
                    });
                }
            }
        });
    }

    private void enableButton() {
        sendButton.setEnabled(true);
        sendButton.setBackgroundColor(Color.parseColor("#ff99cc00"));
    }

    private void disableButton() {
        sendButton.setEnabled(false);
        sendButton.setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.send_btn) {
            onSend();
        }
    }

    private boolean counter(String barcode) {
        this.barcodeMap.merge(barcode, 1, Integer::sum);
        return this.barcodeMap.get(barcode) == 2;
    }
}