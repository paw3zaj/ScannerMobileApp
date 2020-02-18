package pl.pzdev2.skaner;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatDateTime {

    private static final String DATE_FORMATTER = "yyyy-MM-dd HH:mm:ss";

    public static String dateTime() {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMATTER);
        // get current date time
        return simpleDateFormat.format(new Date());
    }




}
