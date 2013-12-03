package org.ancode.secmail.update.internal;

import org.ancode.secmail.update.ResponseParser;
import org.ancode.secmail.update.Version;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class SimpleJSONParser implements ResponseParser {

	@Override
	public Version parser(String response) {
		Version version = null;
		Log.v("secmail", response);
		try {
			JSONTokener jsonParser = new JSONTokener(response);
			JSONObject v = (JSONObject) jsonParser.nextValue();
			int versionCode = v.getInt("code");
			String versionName = v.getString("version");
			String releaseNote = v.getString("content");
			String releaseUrl = v.getString("downloadUrl");
			String releaseTime = v.getString("time");
			version = new Version(versionCode, versionName, releaseNote, releaseUrl, releaseTime);

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return version;
	}

}
