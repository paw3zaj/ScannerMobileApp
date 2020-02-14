package pl.pzdev2.skaner;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FormatDateTime {

    private static final String DATE_FORMATTER = "yyyy-MM-dd HH:mm:ss";

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String dateTime() {

        LocalDateTime localDateTime = LocalDateTime.now(); // get current date time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMATTER);

        return localDateTime.format(formatter);
    }
}
