package org.ancode.secmail.mail.crypto.v2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

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

import android.util.Log;
import android.util.Xml;

public class HttpPostServiceV2 {
	
	public static final String TAG = "HttpPostServiceV2";
	
	private static HttpClient httpClient;
	
	private static final String REG_REQUEST = "https://www.han2011.com/secmail/v2/reg_request";
	private static final String REG_CONFIRM = "https://www.han2011.com/secmail/v2/reg_confirm";
	private static final String REG_PROTECT = "https://www.han2011.com/secmail/v2/protect_enable";
	private static final String SEND_EMAIL = "https://www.han2011.com/secmail/v2/send";
	private static final String RECEIVE_EMAIL = "https://www.han2011.com/secmail/v2/get";

	private static HttpClient getHttpClient() {
		
		if( httpClient != null) {
			return httpClient;
		}
		
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			HttpConnectionParams.setConnectionTimeout(params, 5000);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

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

	/**
	 * Parse the post result.
	 * 
	 * @param is
	 *            the input
	 * @return the result
	 */
	public static PostResultV2 parsePostResult(InputStream is) {

		if( is == null )return null;
		
		PostResultV2 pr = new PostResultV2();
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
						eventType = parser.next();
						pr.setResultCode(parser.getText());
					} else if (tagName.equals("device")) {
						eventType = parser.next();
						pr.setDeviceUuid(parser.getText());
					} else if (tagName.equals("w")) {
						eventType = parser.next();
						pr.setInvalidKey(parser.getText());
					} else if (tagName.startsWith("uuid")) {
						eventType = parser.next();
						pr.getUuidMap().put(tagName, parser.getText());
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
		return pr;
	}

	/**
	 * Send the registration request to server.
	 * <p>
	 * <strong>post:</strong>https://www.gezimai1.com/secmail/v2/reg_request<br>
	 * <strong>mail:</strong>test@163.com<br>
	 * <strong>key:</strong>rsa_encrypt(rsa_publickey, AESKEY)
	 * </p>
	 * 
	 * @param mail
	 *            email address
	 * @param key
	 *            the encrypted key
	 * @return the post result
	 */
	public static PostResultV2 postRegRequest(String mail, String key) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("mail", mail));
		params.add(new BasicNameValuePair("key", key));
		InputStream is = post(REG_REQUEST, params);
		return parsePostResult(is);
	}

	/**
	 * Do post to send reg confirmation
	 * 
	 * <p>
	 * <strong>post:</strong>https://www.gezimai1.com/secmail/v2/reg_confirm<br>
	 * <strong>mail:</strong>test@163.com<br>
	 * <strong>key:</strong>aes256_encrypt(regCode+uuid, AESKEY)<br>
	 * <strong>device:</strong>deviceUuid
	 * </p>
	 * 
	 * @param mail
	 * @param key
	 * @param deviceUuid
	 * @return the post result
	 */
	public static PostResultV2 postRegConfirm(String mail, String key,
			String deviceUuid) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("mail", mail));
		params.add(new BasicNameValuePair("key", key));
		params.add(new BasicNameValuePair("device", deviceUuid));
		InputStream is = post(REG_CONFIRM, params);
		return parsePostResult(is);
	}

	/**
	 * Do post to send protect data.
	 * 
	 * <p>
	 * <strong>post:</strong>https://www.han2011.com/secmail/v2/protect_enable<br>
	 * <strong>mail:</strong>test@163.com<br>
	 * <strong>type:</strong>[mail|mobile]<br>
	 * <strong>protect:</strong>aes256_encrypt([other@163.com|1393115xxxx],
	 * AESKEY)<br>
	 * <strong>verify:</strong>aes256_encrypt(regCode+random(32),AESKEY)<br>
	 * <strong>device:</strong>deviceUuid
	 * </p>
	 * 
	 * @param mail
	 * @param type
	 * @param protect
	 * @param vertify
	 * @param deviceUuid
	 * @return
	 */
	public static PostResultV2 postProtectRequest(String mail, String type,
			String protect, String verify, String deviceUuid) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("mail", mail));
		params.add(new BasicNameValuePair("type", type));
		params.add(new BasicNameValuePair("protect", protect));
		params.add(new BasicNameValuePair("verify", verify));
		params.add(new BasicNameValuePair("device", deviceUuid));
		InputStream is = post(REG_PROTECT, params);
		return parsePostResult(is);
	}

	/**
	 * Do post to send email encrypt information.
	 * 
	 * <p>
	 * <strong>post:</strong>https://www.han2011.com/secmail/v2/send<br>
	 * <strong>from:</strong>alice@163.com<br>
	 * <strong>to:</strong>bob@163.com<br>
	 * <strong>verify:</strong>aes256_encrypt(regCode+random(32),AESKEY)<br>
	 * <strong>uuid1:</strong>7567-2342-23232-2323223<br>
	 * <strong>key1:</strong>aes256_encrypt(AESKEY1, AESKEY)<br>
	 * <strong>uuid2:</strong>7567-2342-23233-2323224<br>
	 * <strong>key2:</strong>aes256_encrypt(AESKEY2, AESKEY)<br>
	 * <strong>device:</strong>deviceUuid
	 * </p>
	 * 
	 * @deprecated
	 * @param from
	 * @param to
	 * @param aesKey
	 * @param regcode
	 * @param deviceUuid
	 * @param keys
	 * @return
	 */
	public static PostResultV2 postSendEmail(String from, String to,
			String aesKey, String regCode, String deviceUuid,
			List<AESKeyObject> keys) {

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("from", from));
		params.add(new BasicNameValuePair("to", to));
		try {
			AesCryptor cryptor = new AesCryptor(aesKey);
			String verify = regCode + AESKeyGenerator.generateAesKey();
			verify = cryptor.encrypt(verify);
			params.add(new BasicNameValuePair("verify", verify));
			for (int i = 0; i < keys.size(); i++) {
				AESKeyObject aesKeyOjbect = keys.get(i);
				params.add(new BasicNameValuePair("uuid" + (i + 1),
						aesKeyOjbect.getUuid()));
				String key = cryptor.encrypt(aesKeyOjbect.getAesKey());
				params.add(new BasicNameValuePair("key" + (i + 1), key));
			}
		} catch (CryptorException e) {
			e.printStackTrace();
		}
		params.add(new BasicNameValuePair("device", deviceUuid));
		InputStream is = post(SEND_EMAIL, params);
		return parsePostResult(is);
	}

	/**
	 * Do post to send email encrypt information
	 * 
	 * <p>
	 * <strong>post:</strong>https://www.han2011.com/secmail/v2/send<br>
	 * <strong>from:</strong>alice@163.com<br>
	 * <strong>to:</strong>bob@163.com<br>
	 * <strong>verify:</strong>aes256_encrypt(regCode+random(32),AESKEY)<br>
	 * <strong>uuid1:</strong>7567-2342-23232-2323223<br>
	 * <strong>key1:</strong>aes256_encrypt(AESKEY1, AESKEY)<br>
	 * <strong>uuid2:</strong>7567-2342-23233-2323224<br>
	 * <strong>key2:</strong>aes256_encrypt(AESKEY2, AESKEY)<br>
	 * <strong>device:</strong>deviceUuid
	 * </p>
	 * 
	 * @param from
	 * @param to
	 * @param verify
	 * @param deviceUuid
	 * @param uuids
	 * @param keys
	 * @return
	 */
	public static PostResultV2 postSendEmail(String from, String to,
			String verify, String deviceUuid, List<AESKeyObject> keyList) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("from", from));
		params.add(new BasicNameValuePair("to", to));
		params.add(new BasicNameValuePair("verify", verify));
		for (int i = 0; i < keyList.size(); i++) {
			params.add(new BasicNameValuePair("uuid" + (i + 1), keyList.get(i).getUuid()));
			params.add(new BasicNameValuePair("key" + (i + 1), keyList.get(i).getEncryptAesKey()));
		}
		params.add(new BasicNameValuePair("device", deviceUuid));
		InputStream is = post(SEND_EMAIL, params);
		return parsePostResult(is);
	}

	/**
	 * Do post to get email encrypt information
	 * 
	 * <p>
	 * uuid1=MIME-HEADER secmail-uuid1<br>
	 * uuid2=MIME-HEADER secmail-uuid2<br>
	 * <strong>post:</strong>https://www.han2011.com/secmail/v2/get<br>
	 * <strong>owner:</strong>bob@163.com<br>
	 * <strong>verify:</strong>aes256_encrypt(regCode+random(32),AESKEY)<br>
	 * <strong>uuid1:</strong>uuid1<br>
	 * <strong>uuid2:</strong>uuid2<br>
	 * <strong>device:</strong>deviceUuid
	 * </p>
	 * 
	 * @deprecated
	 * @param owner
	 * @param password
	 * @param regcode
	 * @param uuidList
	 * @return
	 */
	public static PostResultV2 postReceiveEmail(String owner, String aesKey,
			String regCode, String deviceUuid, List<String> uuidList) {

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("owner", owner));
		try {
			AesCryptor cryptor = new AesCryptor(aesKey);
			String verify = regCode + AESKeyGenerator.generateAesKey();
			verify = cryptor.encrypt(verify);
			Log.i(TAG, "verify:" + verify);
			params.add(new BasicNameValuePair("verify", verify));
			for (int i = 0; i < uuidList.size(); i++) {
				params.add(new BasicNameValuePair("uuid" + (i + 1), uuidList
						.get(i)));
			}
		} catch (CryptorException e) {
			e.printStackTrace();
		}
		params.add(new BasicNameValuePair("device", deviceUuid));
		InputStream is = post(RECEIVE_EMAIL, params);
		return parsePostResult(is);
	}

	/**
	 * Do post to get email encrypt information.
	 * 
	 * <p>
	 * uuid1=MIME-HEADER secmail-uuid1<br>
	 * uuid2=MIME-HEADER secmail-uuid2<br>
	 * <strong>post:</strong>https://www.han2011.com/secmail/v2/get<br>
	 * <strong>owner:</strong>bob@163.com<br>
	 * <strong>verify:</strong>aes256_encrypt(regCode+random(32),AESKEY)<br>
	 * <strong>uuid1:</strong>uuid1<br>
	 * <strong>uuid2:</strong>uuid2<br>
	 * <strong>device:</strong>deviceUuid
	 * </p>
	 * 
	 * @param owner
	 * @param verify
	 * @param deviceUuid
	 * @param uuidList
	 * @return
	 */
	public static PostResultV2 postReceiveEmail(String owner, String verify,
			String deviceUuid, List<String> uuidList) {

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("owner", owner));
		params.add(new BasicNameValuePair("verify", verify));
		for (int i = 0; i < uuidList.size(); i++) {
			params.add(new BasicNameValuePair("uuid" + (i + 1), uuidList.get(i)));
		}
		params.add(new BasicNameValuePair("device", deviceUuid));
		InputStream is = post(RECEIVE_EMAIL, params);
		return parsePostResult(is);
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
