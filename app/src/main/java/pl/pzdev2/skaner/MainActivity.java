package pl.pzdev2.skaner;

import androidx.annotation.RequiresApi;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SQLiteDatabase db;
    private Cursor cursor;
    private SQLiteOpenHelper databaseHelper;
    private SimpleCursorAdapter listAdapter;
    private Button sendButton;

    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    private List<String> barcodesList;

    private ListView listView;

    private TextView txtView;

    //PRODUCTION IP
//    public static final String URL = "http://153.19.70.197:7323/receive-books-barcode";
    //DEVELOPER IP
    public static final String URL = "http://153.19.70.138:8080/receive-book";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        barcodesList = new ArrayList<>();
        databaseHelper = new DatabaseHelper(this);

        sendButton = (Button) findViewById(R.id.send_btn);
        sendButton.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.list_books);
        txtView = (TextView) findViewById(R.id.textView);

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

    private void onSend() throws JSONException {

        /*
        *

SomeClass obj1 = new SomeClass();
obj1.setValue("val1");
sList.add(obj1);

SomeClass obj2 = new SomeClass();
obj2.setValue("val2");
sList.add(obj2);

obj.put("list", sList);

JSONArray jArray = obj.getJSONArray("list");
for(int ii=0; ii < jArray.length(); ii++)
  System.out.println(jArray.getJSONObject(ii).getString("value"));
        * */
//        JSONObject obj = new JSONObject();
        List<ScannerLogs> barcodeList = new ArrayList<>();
    String b;
    String d;

//            List<String> stringList = new ArrayList<>();
        try {
//                SQLiteDatabase db = databaseHelper.getReadableDatabase();
            cursor = db.query("BORROWED", new String[]{"BARCODE", "CREATED_DATE"}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                disableButton();
                do {
                    //calling the method to save the barcodes to MySQL

//                    barcodeList.add(cursor.getString(0));
//                    barcodeList.add(cursor.getString(1));
                    barcodeList.add(new ScannerLogs(cursor.getString(0), cursor.getString(1)));
//                        stringList.add(cursor.getString(0));
//                        stringList.add(cursor.getString(1));
                    b = cursor.getString(0);
                    d = cursor.getString(1);

                } while (cursor.moveToNext());
                for(ScannerLogs log : barcodeList) {
                    log.toString();
                    System.out.println(log.getBarcode());
                    System.out.println(log.getCreatedDate());
                    System.out.println("w pętli");
                }
//                obj.put("", barcodeList);
//                sendBarcode(obj);
                sendBarcode(new ScannerLogs(b, d));
            }
        } catch (SQLException e) {
            Toast.makeText(this, "Dane nieosiągalne", Toast.LENGTH_SHORT).show();
            enableButton();
        }
    }

    private void sendBarcode(final ScannerLogs barCode) throws JSONException {

        JSONObject job = new JSONObject();
        job.put("barcode", barCode.getBarcode().toString());
        job.put("createdDate", barCode.getCreatedDate().toString());
        // Define the POST request
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, URL, job,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        DatabaseHelper.deleteAll(db);

                        updateListView();

                        Toast.makeText(getApplicationContext(), "Dane przesłane na serwer", Toast.LENGTH_LONG).show();

                        enableButton();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(getApplicationContext(), "Błąd przesyłu danych!!!", Toast.LENGTH_LONG).show();

                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    Toast.makeText(getApplicationContext(),
                            "TimeoutError or NoConnectionError",
                            Toast.LENGTH_LONG).show();
                } else if (error instanceof AuthFailureError) {
                    //TODO
                    Toast.makeText(getApplicationContext(),
                            "AuthFailureError",
                            Toast.LENGTH_LONG).show();
                } else if (error instanceof ServerError) {
                    //TODO
                    Toast.makeText(getApplicationContext(),
                            "ServerError",
                            Toast.LENGTH_LONG).show();
                } else if (error instanceof NetworkError) {
                    //TODO
                    Toast.makeText(getApplicationContext(),
                            "NetworkError",
                            Toast.LENGTH_LONG).show();
                } else if (error instanceof ParseError) {
                    //TODO
                    Toast.makeText(getApplicationContext(),
                            "ParseError",
                            Toast.LENGTH_LONG).show();
                } else if (error.getCause() instanceof SocketException) {
                    Toast.makeText(getApplicationContext(),
                            "SocketException",
                            Toast.LENGTH_LONG).show();
                }

//                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                txtView.setText(error.toString());
                enableButton();

            }
        });

        // Add the request object to the queue to be executed
        MySingleton.getInstance(this).addToRequestQueue(req);
    }

    public void updateListView() {

        //Create a cursor
//        SQLiteOpenHelper databaseHelper = new DatabaseHelper(this);
        try {
            db = databaseHelper.getReadableDatabase();
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

                            try {
                                long l =  Long.parseLong(barcode);
//                                if(barcode.length() == 12){
                            if(!barcodesList.contains(barcode)) {

                                    barcodesList.add(barcode);
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
//                            }
                            } catch (NumberFormatException e) {}

                            updateListView();
                        }
                    });
                }
            }
        });
    }

    private void enableButton() {
        sendButton.setEnabled(true);
        sendButton.setBackgroundColor(Color.parseColor("#ff99cc00"));
    }

    private void disableButton(){
        sendButton.setEnabled(false);
        sendButton.setBackgroundColor(Color.LTGRAY);
    }
    @Override
    public void onClick(View v) {
//Sprawdzanie czy został kliknięty przycisk
        switch (v.getId()) {
            case R.id.send_btn:
                try {
                    onSend();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
