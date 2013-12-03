package org.ancode.secmail.helper;

import android.content.Context;
import org.ancode.secmail.R;

public class SizeFormatter {
    /*
     * Formats the given size as a String in bytes, kB, MB or GB with a single digit
     * of precision. Ex: 12,315,000 = 12.3 MB
     */
    public static String formatSize(Context context, long size) {
        if (size > 1024000000) {
            return ((float)(size / 102400000) / 10) + context.getString(R.string.abbrev_gigabytes);
        }
        if (size > 1024000) {
            return ((float)(size / 102400) / 10) + context.getString(R.string.abbrev_megabytes);
        }
        if (size > 1024) {
            return ((float)(size / 102) / 10) + context.getString(R.string.abbrev_kilobytes);
        }
        return size + context.getString(R.string.abbrev_bytes);
    }

}


