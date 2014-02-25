package org.ancode.secmail.update.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class IgnorePersistent {

	public static final String VERSION_PERSISTENT_NAME = "auto_update_setting";
	private static final String INGORE_TIME = "ingore_time";
	private SharedPreferences shared;

	public IgnorePersistent(Context context) {
		shared = context.getSharedPreferences(VERSION_PERSISTENT_NAME, Context.MODE_PRIVATE);
	}

	public void save(long time) {
		Editor editor = shared.edit();
		editor.clear();
		editor.putLong(INGORE_TIME, time);
		editor.commit();
	}

	public void clear() {
		Editor editor = shared.edit();
		editor.clear();
		editor.commit();
	}

	public long load() {
		if (shared.contains(INGORE_TIME)) {
			long time = shared.getLong(INGORE_TIME, 0);
			return time;
		}
		return 0;
	}

}
