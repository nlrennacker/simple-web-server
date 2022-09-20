package web.util;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class Util {

    /**
     * Compares the supplied date time with the cached file date time
     * If the supplied date time is AFTER the file date time:
     * FALSE is returned
     * Otherwise
     * TRUE is returned
     * 
     * If the supplied date time does not match the correct formatting, false is
     * returned
     * 
     * @param file
     * @param unformattedDate
     * @return boolean value based on
     */
    public static boolean compareDateTime(File file, String unformattedDate) {

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
        try {
            ZonedDateTime dateToCompare = ZonedDateTime
                    .parse(unformattedDate, formatter);

            ZonedDateTime fileDateTime = Instant.ofEpochMilli(file.lastModified())
                    .atZone(ZoneId.of(ZonedDateTime.now().getOffset().toString()));

            System.out.println(dateToCompare + "\n" + fileDateTime);

            if (dateToCompare.isAfter(fileDateTime))
                return true;
             else return false;

        } catch (DateTimeParseException e) {
            System.out.println("Illegal Date Format");
            e.printStackTrace();
            return false;
        }
    }
}
