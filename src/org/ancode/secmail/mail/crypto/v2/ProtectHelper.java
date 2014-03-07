package org.ancode.secmail.mail.crypto.v2;

import org.ancode.secmail.Account;
import org.ancode.secmail.Preferences;
import org.ancode.secmail.R;
import org.ancode.secmail.view.CaptchaDialog;
import org.ancode.secmail.view.ProtectDialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class ProtectHelper {

	/**
	 * 用户申请加密服务成功后，提示用户是否开通密码保护功能
	 * 
	 * @param context
	 * @param account
	 */
	public static void showApplyProtectDialog(final Context context, final Account account) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(getString(context, R.string.apply_encrypt_service_success));
		builder.setMessage(getString(context, R.string.apply_open_protect_tip));

		builder.setPositiveButton(getString(context, R.string.button_open_protect_now),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						showEnableProtectDialog(context, account);
					}
				});

		builder.setNegativeButton(getString(context, R.string.button_open_protect_nexttime), null);
		builder.create().show();

	}

	/**
	 * 显示密码保护申请窗口
	 * 
	 * @param context
	 * @param account
	 */
	public static void showEnableProtectDialog(final Context context, final Account account) {

		ProtectDialog dialog = new ProtectDialog(context, account) {

			@Override
			public void onSuccess(String arg0) {

				// 设定特殊的标志，因为还不知道用户是否已经进行了激活
				account.setVerification("#" + arg0);
				showActiveProtectDialog(context, account);
			}

			@Override
			public void onFailed(String resultCode) {

				if ("protect_enabled".equals(resultCode)) {
					
					showDisableProtectDialog(context, account);
					
					return;
				}
				showToast(context, R.string.protect_apply_failed);
			}

		};

		dialog.show();

	}

	/**
	 * 显示取消密码保护窗口
	 * 
	 * @param context
	 * @param account
	 */
	public static void showDisableProtectDialog(final Context context, final Account account) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(getString(context, R.string.cancel_dialog_title));
		builder.setMessage(getString(context, R.string.cancel_dialog_message));

		builder.setPositiveButton(getString(context, R.string.protect_button_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();

						AsyncHttpTools.execute(new AsyncHttpTools.TaskListener() {

							@Override
							public PostResultV2 executeTask() {
								return HttpPostUtil.postDisableProtectRequest(account);
							}

							@Override
							public void callBack(PostResultV2 result) {
								
								if (result == null) {
									Log.i("lxc", "依旧是空值");
									showToast(context, R.string.protect_network_anomaly);
									return;
								}

								Log.i("lxc", result.toString());
								
								if (result.isSuccess()) {
									showCancelProtectDialog(context, account);
								} else {

									if ("protect_disabled".equals(result.getResultCode())) {
										showToast(context, R.string.protect_already_cancel);
										return;
									}
									showToast(context, R.string.protect_cancel_failed);
								}
							}
						});
					}
				});

		builder.setNegativeButton(getString(context, R.string.button_open_protect_nexttime), null);

		builder.create().show();
	}

	/**
	 * 显示验证码输入窗口
	 * 
	 * @param context
	 * @param account
	 */
	private static void showActiveProtectDialog(final Context context, final Account account) {

		CaptchaDialog dialog = new CaptchaDialog(context, account) {

			@Override
			public PostResultV2 onExecute(Account account, String captcha) {
				return HttpPostUtil.postActiveProtectRequest(account, captcha);
			}

			@Override
			public void onSuccess(String arg0) {
				String protect = account.getVerification();
				if (TextUtils.isEmpty(protect)) {
					return;
				}

				if (protect.startsWith("#")) {
					protect = protect.substring(1);
				}

				account.setVerification(protect);
				account.save(Preferences.getPreferences(context));
				showToast(context, R.string.protect_active_success);
			}

			@Override
			public void onFailed(String arg0) {
				account.setVerification(null);
				account.save(Preferences.getPreferences(context));
				showToast(context, R.string.protect_verify_failed);
			}

			@Override
			public void onCancel() {
				account.setVerification(null);
				account.save(Preferences.getPreferences(context));
				showToast(context, R.string.protect_active_verify_failed);
			}

		};

		dialog.show();

	}

	/**
	 * 显示验证码输入窗口
	 * 
	 * @param context
	 * @param account
	 */
	private static void showCancelProtectDialog(final Context context, final Account account) {

		CaptchaDialog dialog = new CaptchaDialog(context, account) {

			@Override
			public PostResultV2 onExecute(Account account, String captcha) {
				return HttpPostUtil.postCancelProtectRequest(context, account, captcha);
			}

			@Override
			public void onSuccess(String arg0) {
				account.setVerification(null);
				account.save(Preferences.getPreferences(context));
				showToast(context, R.string.protect_cancel_success);
			}

			@Override
			public void onFailed(String arg0) {
				showToast(context, R.string.protect_verify_failed);
			}

			@Override
			public void onCancel() {
				showToast(context, R.string.protect_cancel_verify_failed);
			}

		};

		dialog.show();

	}

	private static String getString(Context context, int id) {
		if (context != null) {
			return context.getString(id);
		}
		return null;
	}

	private static void showToast(Context context, int id) {
		if (context == null) {
			return;
		}

		Toast.makeText(context, context.getString(id), Toast.LENGTH_LONG).show();
	}

}
