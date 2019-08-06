package pl.pzdev2.skaner;


import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class BookDetails extends AppCompatActivity {

    public static final String EXTRA_BORROWID = "borrowId";

    public static final String URL = "http://153.19.70.138:8080/receive";

    private TextView textViewName;
//    private TextView description;

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

                textViewName = (TextView)findViewById(R.id.test_TV1);
                textViewName.setText(nameText);

//                description = (TextView)findViewById(R.id.test_TV2);
//                description.setText(descriptionText);
            }
            cursor.close();
            db.close();
        } catch (SQLException e) {
            Toast toast = Toast.makeText(this, "Dane nieosiągalne", Toast.LENGTH_SHORT);
            toast.show();
        }

        final TextView description = (TextView)findViewById(R.id.test_TV2);

        final String TAG = textViewName.getText().toString().trim();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        description.setText("poszło do serwera");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        description.setText("That didn't work!");
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("barecode", TAG);
                return params;
            }
        };

        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

}
