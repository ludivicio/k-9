package org.ancode.secmail.view;

import org.ancode.secmail.Account;
import org.ancode.secmail.R;
import org.ancode.secmail.mail.crypto.v2.AsyncHttpTools;
import org.ancode.secmail.mail.crypto.v2.PostResultV2;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public abstract class CaptchaDialog extends Dialog implements OnClickListener {

	private Button btnOk;
	private Button btnCancel;
	private EditText etCaptcha;

	private Context context;
	private Account account;

	private PostResultV2 postResult;

	public CaptchaDialog(Context context) {
		super(context);
		this.context = context;
		this.setTitle(getString(R.string.captcha_dialog_title));
		this.setCancelable(false);
	}

	public CaptchaDialog(Context context, Account account) {
		this(context);
		this.account = account;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.captcha_dailog_view);
		initView();
		setListener();
	}

	@Override
	public void onBackPressed() {
		// 返回按钮将不起作用
	}

	private void initView() {
		etCaptcha = (EditText) findViewById(R.id.et_captcha);
		btnOk = (Button) findViewById(R.id.bt_captcha_ok);
		btnCancel = (Button) findViewById(R.id.bt_captcha_cancel);
	}

	private void setListener() {
		btnOk.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == btnOk.getId()) {

			final String captcha = etCaptcha.getText().toString();

			if (captcha == null || "".equals(captcha)) {
				Toast.makeText(context, getString(R.string.captcha_dialog_not_input),
						Toast.LENGTH_SHORT).show();
				return;
			}

			// modified by lxc at 2013-11-11
			AsyncHttpTools.execute(new AsyncHttpTools.TaskListener() {

				@Override
				public PostResultV2 executeTask() {
					return onExecute(account, captcha);
				}

				@Override
				public void callBack(PostResultV2 result) {

					if (result == null) {
						Toast.makeText(context,
								context.getString(R.string.protect_network_anomaly),
								Toast.LENGTH_LONG).show();
						return;
					}

					if (result.isSuccess()) {
						CaptchaDialog.this.dismiss();
						onSuccess(captcha);
					} else {
						onFailed(captcha);
					}
				}
			});

		} else if (v.getId() == btnCancel.getId()) {
			CaptchaDialog.this.dismiss();
			onCancel();
		}
	}

	public abstract PostResultV2 onExecute(final Account account, final String captcha);

	public abstract void onSuccess(String arg0);

	public abstract void onFailed(String arg0);

	public abstract void onCancel();

	public void setAccount(Account account) {
		this.account = account;
	}

	public PostResultV2 getPostResult() {
		return postResult;
	}

	public void setPostResult(PostResultV2 postResult) {
		this.postResult = postResult;
	}

	private String getString(int id) {
		if (context != null) {
			return context.getString(id);
		}
		return null;
	}

}
