package org.ancode.secmail.helper;

import org.ancode.secmail.K9;
import org.ancode.secmail.R;
import org.ancode.secmail.activity.misc.ContactPictureLoader;

import android.content.Context;
import android.util.TypedValue;

public class ContactPicture {

    public static ContactPictureLoader getContactPictureLoader(Context context) {
        final int defaultBgColor;
        if (!K9.isColorizeMissingContactPictures()) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.contactPictureFallbackDefaultBackgroundColor,
                    outValue, true);
            defaultBgColor = outValue.data;
        } else {
            defaultBgColor = 0;
        }

        return new ContactPictureLoader(context, defaultBgColor);
    }
}
