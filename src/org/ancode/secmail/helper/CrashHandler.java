package org.ancode.secmail.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ancode.secmail.mail.crypto.v2.SSLSocketFactoryEx;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.Xml;

/**
 * 自定义的 异常处理类 , 实现了 UncaughtExceptionHandler接口
 * 
 */
public class CrashHandler implements UncaughtExceptionHandler {

	private static final String TAG = "CrashHelper";

	// 只有一个 MyCrash-Handler
	private static Context mContext;

	@SuppressLint("SimpleDateFormat")
	private final SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	private String appName;

	private String updateUrl;

	private CrashHandler() {
	}

	private static class SingletonHolder {
		public static CrashHandler INSTANCE = new CrashHandler();
	}

	public static CrashHandler getInstance(Context context) {
		mContext = context;
		return SingletonHolder.INSTANCE;
	}

	public void init(String appName, String url) {
		this.appName = appName;
		this.updateUrl = url;
	}

	@Override
	public void uncaughtException(Thread arg0, Throwable arg1) {

		try {
			// 1.获取当前程序的版本号. 版本的id
			String version = getVersionInfo();
			// 2.获取手机的硬件信息.
			String mobileInfo = getMobileInfo();
			// 3.把错误的堆栈信息 获取出来
			String errorinfo = getErrorInfo(arg1);
			// 4.把所有的信息 还有信息对应的时间 提交到服务器
			String date = dataFormat.format(new Date());
			StringBuilder sb = new StringBuilder();
			sb.append("==> CRASH LOG BEGIN <===============================================");
			sb.append("\n应用名称: " + appName);
			sb.append("\n崩溃时间: " + date);
			sb.append("\n版本信息: " + version);
			sb.append("\n手机信息: " + mobileInfo);
			sb.append("\n堆栈信息: " + errorinfo);
			sb.append("\n==> CRASH LOG END <===============================================");

			Log.e(TAG, sb.toString());

			new PostErrorInfoToServerTask().execute(sb.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 获取错误的信息
	 * 
	 * @param arg1
	 * @return
	 */
	private String getErrorInfo(Throwable arg1) {
		Writer writer = new StringWriter();
		PrintWriter pw = new PrintWriter(writer);
		arg1.printStackTrace(pw);
		pw.close();
		String error = writer.toString();
		return error;
	}

	/**
	 * 获取手机的硬件信息
	 * 
	 * @return
	 */
	private String getMobileInfo() {
		StringBuffer sb = new StringBuffer();
		// 通过反射获取系统的硬件信息
		try {
			Field[] fields = Build.class.getDeclaredFields();
			for (Field field : fields) {
				// 暴力反射 ,获取私有的信息
				field.setAccessible(true);
				String name = field.getName();
				String value = field.get(null).toString();
				sb.append(name + "=" + value);
				sb.append('\n');
			}
		} catch (Exception e) {
			sb.append("获取硬件信息失败!");
		}
		return sb.toString();
	}

	/**
	 * 获取手机的版本信息
	 * 
	 * @return
	 */
	private String getVersionInfo() {
		try {
			PackageManager pm = mContext.getPackageManager();
			PackageInfo info = pm.getPackageInfo(mContext.getPackageName(), 0);
			return info.versionName + " ---- " + info.versionCode;
		} catch (Exception e) {
			e.printStackTrace();
			return "获取版本信息失败";
		}
	}

	private class PostErrorInfoToServerTask extends AsyncTask<String, String, Boolean> {

		@Override
		protected Boolean doInBackground(String... parameters) {
			// TODO Auto-generated method stub

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("crash", parameters[0]));

			// 提交错误信息到服务器
			post(updateUrl, params);

			// InputStream is = post(updateUrl, params);

			// 将服务器返回的结果转换成字符串
			// String strResult = inputStreamToString(is);

			// 将服务器返回的结果进行解析，需要制定统一的接口
			// String parseResult = parse(is);

			// do something;
			// ...

			return true;

		}

	}

	private static HttpClient getHttpClient() {
		HttpClient httpClient;

		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			HttpConnectionParams.setConnectionTimeout(params, 5000);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			httpClient = new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			httpClient = new DefaultHttpClient();
		}

		return httpClient;
	}

	private static InputStream post(String url, List<NameValuePair> params) {

		HttpClient client = getHttpClient();
		HttpPost httpRequest = new HttpPost(url);
		HttpResponse httpResponse;
		try {
			httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			httpResponse = client.execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				InputStream is = httpResponse.getEntity().getContent();
				return is;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "post the request failed");
		}
		return null;
	}

	public static String parse(InputStream is) {

		if (is == null)
			return null;
		String result = null;
		XmlPullParser parser = Xml.newPullParser();
		try {
			parser.setInput(is, "UTF-8");
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					String tagName = parser.getName();
					if (tagName.equals("r")) {
						result = parser.getText();
						eventType = parser.next();
					}

					break;
				case XmlPullParser.END_TAG:
					break;
				}

				eventType = parser.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * Convert the InputStream to String.
	 * 
	 * @param in
	 *            the input
	 * @return the string
	 */
	public static String inputStreamToString(InputStream in) {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] data = new byte[1024];
		int count = -1;
		try {
			while ((count = in.read(data, 0, 1024)) != -1)
				outStream.write(data, 0, count);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Convert failed!");
		}
		return new String(outStream.toByteArray());
	}
}