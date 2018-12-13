/*
 * Copyright (C) 2016 Saxon State and University Library Dresden (SLUB)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.qucosa.camel.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {
    public static byte[] toJson(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsBytes(object);
    }

    /**
     * get timestamp (lastmod) for url/urlset
     * @return date in w3c datetime format as string
     */
    public static String getCurrentW3cDatetime() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date())+"+00:00";
    }

    // TODO conversion if needed
    public static String toW3cDatetime(String date) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String w3cDatetimeString = format.format(new Date())+"+00:00";

        String pattern = "2010-03-01T00:00:00-08:00";
        String dateString = "yyyy-MM-dd'T'HH:mm:ssZ";
        String datePattern = "yyyy-MM-dd'T'HH:mm:ss";

        DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
        DateTime dateTime = dtf.parseDateTime(dateString);
        System.out.println(dateTime); // 2010-03-01T04:00:00.000-04:00

        return "";
//        String dateString = "2010-03-01T00:00:00-08:00";
//        String pattern = "yyyy-MM-dd'T'HH:mm:ssZ";
//        DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
//        DateTime dateTime = dtf.parseDateTime(dateString);
//        System.out.println(dateTime); // 2010-03-01T04:00:00.000-04:00
    }

    public static boolean empty( final String s ) {
        return s == null || s.trim().isEmpty();
    }
}
