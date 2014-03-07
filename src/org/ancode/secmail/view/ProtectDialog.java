package org.ancode.secmail.view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ancode.secmail.Account;
import org.ancode.secmail.R;
import org.ancode.secmail.mail.crypto.v2.AsyncHttpTools;
import org.ancode.secmail.mail.crypto.v2.HttpPostUtil;
import org.ancode.secmail.mail.crypto.v2.PostResultV2;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public abstract class ProtectDialog extends Dialog implements OnClickListener,
		OnItemSelectedListener {

	private static final String EMAIL = "mail";
	private static final String PHONE = "mobile";

	private Button btnOk;
	private Button btnCancel;
	private Spinner spType;
	private EditText etProtect;
	private TextView tvType;

	private Context context;
	private Account account;

	public ProtectDialog(Context context) {
		super(context);
		this.context = context;
		this.setTitle(getString(R.string.protect_dialog_title));
	}

	public ProtectDialog(Context context, Account account) {
		this(context);
		this.account = account;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.protect_dialog_view);
		initView();
		setListener();
	}

	private void initView() {
		btnOk = (Button) findViewById(R.id.bt_protect_ok);
		btnCancel = (Button) findViewById(R.id.bt_protect_cancel);
		spType = (Spinner) findViewById(R.id.sp_protect_type);
		etProtect = (EditText) findViewById(R.id.et_protect);
		tvType = (TextView) findViewById(R.id.tv_protect_type);
	}

	private void setListener() {
		btnOk.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
		spType.setOnItemSelectedListener(this);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

		String type = getProtectType();
		if (EMAIL.equals(type)) {
			// Choose mail.
			tvType.setText(getString(R.string.protect_dialog_input_mail));
			etProtect.setText("");
			etProtect.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

		} else if (PHONE.equals(type)) {
			// Choose mobile.
			tvType.setText(getString(R.string.protect_dialog_input_mobile));
			etProtect.setText("");
			etProtect.setInputType(InputType.TYPE_TEXT_VARIATION_PHONETIC);
		}

	}

	private String getProtectType() {
		String selected = spType.getSelectedItem() + "";
		String[] types = context.getResources().getStringArray(R.array.account_protect_types);
		if (types[0].equals(selected)) {
			return EMAIL;
		} else if (types[1].equals(selected)) {
			return PHONE;
		}
		return "";

	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == btnOk.getId()) {

			final String protect = etProtect.getText().toString();

			if (protect == null || "".equals(protect)) {
				Toast.makeText(context, getString(R.string.protect_dialog_not_input),
						Toast.LENGTH_SHORT).show();
				return;
			}

			final String type = getProtectType();

			if (EMAIL.equals(type)) {
				if (!verifyEmail(protect)) {
					Toast.makeText(context, getString(R.string.protect_dialog_invalid_mail),
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (protect.toLowerCase().equals(account.getEmail().toLowerCase())) {
					Toast.makeText(context, getString(R.string.protect_dialog_other_mail),
							Toast.LENGTH_SHORT).show();
					return;
				}
			} else if (PHONE.equals(type)) {
				if (!verifyPhone(protect)) {
					Toast.makeText(context, getString(R.string.protect_dialog_invalid_mobile),
							Toast.LENGTH_SHORT).show();
					return;
				}
			}

			// modified by lxc at 2013-11-11
			AsyncHttpTools.execute(new AsyncHttpTools.TaskListener() {

				@Override
				public PostResultV2 executeTask() {
					return HttpPostUtil.postEnableProtectRequest(account, type, protect);
				}

				@Override
				public void callBack(PostResultV2 result) {

					if (result == null) {
						Toast.makeText(context,
								context.getString(R.string.protect_network_anomaly),
								Toast.LENGTH_LONG).show();
						return;
					}

					Log.i("lxc", result.toString());
					
					if (result.isSuccess()) {
						onSuccess(protect);
					} else {
						onFailed(result.getResultCode());
					}
				}
			});
			
			ProtectDialog.this.dismiss();

		} else if (v.getId() == btnCancel.getId()) {
			ProtectDialog.this.dismiss();
		}
	}

	public abstract void onSuccess(String arg0);

	public abstract void onFailed(String resultCode);

	public static boolean verifyPhone(String phoneNumber) {
		Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$");
		Matcher m = p.matcher(phoneNumber);
		return m.matches();
	}

	public static boolean verifyEmail(String email) {
		String str = "^([a-zA-Z0-9]*[\\.-_]?[a-zA-Z0-9]+)*@([a-zA-Z0-9]*[-_]?[a-zA-Z0-9]+)+[\\.][A-Za-z]{2,3}([\\.][A-Za-z]{2})?$";
		Pattern p = Pattern.compile(str);
		Matcher m = p.matcher(email);
		return m.matches();
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	private String getString(int id) {
		if (context != null) {
			return context.getString(id);
		}
		return null;
	}

}
