package org.ancode.secmail.mail.crypto.v2;


import org.ancode.secmail.R;
import org.ancode.secmail.view.CryptoguardDialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

public class CryptoguardUiHelper {

	/**
	 * Open the protect function or not.
	 * 
	 * @param success
	 */
	public static void openProtectDialog(final Context context) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(getString(context,
				R.string.account_cryptoguard_apply_success));
		builder.setMessage(getString(context,
				R.string.account_cryptoguard_openup_tip));
		builder.setPositiveButton(
				getString(context, R.string.account_cryptoguard_open_now),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						showProtectInfoDialog(context);
					}
				});
		builder.setNegativeButton(
				getString(context, R.string.account_cryptoguard_nexttime), null);
		builder.create().show();

	}

	public static void showProtectInfoDialog(final Context context) {

		CryptoguardDialog dialog = new CryptoguardDialog(context);

		PostResultV2 pr = new PostResultV2();
		dialog.setPostResult(pr);
		dialog.show();

		Log.i("lxc", "resultCode: " + pr.getResultCode());

		if (pr != null && pr.isSuccess()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(getString(context,
					R.string.account_cryptoguard_apply_success));
			builder.setMessage(getString(context,
					R.string.account_cryptoguard_activate_tip));
			builder.setPositiveButton(
					getString(context, R.string.account_cryptoguard_ok), null);
			builder.create().show();
		}

	}

	private static String getString(Context context, int id) {
		if (context != null) {
			return context.getString(id);
		}
		return null;
	}
}
