package org.ancode.secmail.activity.setup;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import org.ancode.secmail.Account;
import org.ancode.secmail.EmailAddressValidator;
import org.ancode.secmail.K9;
import org.ancode.secmail.Preferences;
import org.ancode.secmail.R;
import org.ancode.secmail.activity.K9Activity;
import org.ancode.secmail.controller.MessagingController;
import org.ancode.secmail.helper.Utility;
import org.ancode.secmail.mail.AuthenticationFailedException;
import org.ancode.secmail.mail.CertificateValidationException;
import org.ancode.secmail.mail.Store;
import org.ancode.secmail.mail.Transport;
import org.ancode.secmail.mail.filter.Hex;
import org.ancode.secmail.mail.store.TrustManagerFactory;
import org.ancode.secmail.mail.store.WebDavStore;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Prompts the user for the email address and password. Attempts to lookup
 * default settings for the domain the user specified. If the domain is known
 * the settings are handed off to the AccountSetupCheckSettings activity. If no
 * settings are found the settings are handed off to the AccountSetupAccountType
 * activity.
 */
public class AccountSetupBasics extends K9Activity implements OnClickListener,
		TextWatcher {
	private final static String EXTRA_ACCOUNT = "org.ancode.secmail.AccountSetupBasics.account";
	private final static int DIALOG_NOTE = 1;
	private final static String STATE_KEY_PROVIDER = "org.ancode.secmail.AccountSetupBasics.provider";

	private EditText mEmailView;
	private EditText mPasswordView;
	private EditText mDescriptionView;
	private EditText mNameView;

	private Button mNextButton;
	private Button mManualSetupButton;

	private Account mAccount;
	private Provider mProvider;

	private Dialog mLoadingDialog;
	private ProgressBar mProgressBar;
	private TextView mLoadingMessageView;

	private boolean mCanceled;
	private boolean mDestroyed;

	private EmailAddressValidator mEmailValidator = new EmailAddressValidator();

	public static void actionNewAccount(Context context) {
		Intent i = new Intent(context, AccountSetupBasics.class);
		context.startActivity(i);
	}

	public static void actionNewAccount(Context context, String emailType) {
		Intent i = new Intent(context, AccountSetupBasics.class);
		i.putExtra("email_type", emailType);
		context.startActivity(i);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_setup_basics);

		mEmailView = (EditText) findViewById(R.id.account_email);
		mPasswordView = (EditText) findViewById(R.id.account_password);
		mDescriptionView = (EditText) findViewById(R.id.account_description);
		mNameView = (EditText) findViewById(R.id.account_name);

		mNextButton = (Button) findViewById(R.id.next);
		mManualSetupButton = (Button) findViewById(R.id.manual_setup);

		mNextButton.setOnClickListener(this);
		mManualSetupButton.setOnClickListener(this);

		mEmailView.addTextChangedListener(this);
		mPasswordView.addTextChangedListener(this);
		mNameView.addTextChangedListener(this);

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
			String accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
			mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
		}

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_KEY_PROVIDER)) {
			mProvider = (Provider) savedInstanceState
					.getSerializable(STATE_KEY_PROVIDER);
		}

		initializeEmailView();

		initializeLoadingDialog(this);
	}

	public void initializeLoadingDialog(Context context) {

		AlertDialog.Builder builder = new AlertDialog.Builder(
				AccountSetupBasics.this);
		builder.setTitle(getString(R.string.account_setup_check_settings_title));
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.account_setup_loading_dialog, null);// 得到加载view
		mProgressBar = (ProgressBar) v.findViewById(R.id.progressbar);
		mLoadingMessageView = (TextView) v.findViewById(R.id.tv_tip_message); // 提示文字
		builder.setCancelable(false);// 不可以用“返回键”取消
		builder.setView(v);
		builder.setNegativeButton(
				getString(R.string.account_setup_check_settings_cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mCanceled = true;
						setLoadingMessage(R.string.account_setup_check_settings_canceling_msg);// "正在取消...");
					}
				});

		mLoadingDialog = builder.create();
	}

	@Override
	public void onResume() {
		super.onResume();
		validateFields();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDestroyed = true;
		mCanceled = true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mAccount != null) {
			outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
		}
		if (mProvider != null) {
			outState.putSerializable(STATE_KEY_PROVIDER, mProvider);
		}
	}

	private void initializeEmailView() {
		String emailType = getIntent().getStringExtra("email_type");
		if (!TextUtils.isEmpty(emailType)) {
			emailType = emailType.substring(emailType.lastIndexOf("_") + 1);
		}
		if (!"other".equals(emailType)) {
			String content = "@" + emailType + ".com";
			mEmailView.requestFocus();
			mEmailView.setText(content);
			mEmailView.setSelection(0);
		}
	}

	public void afterTextChanged(Editable s) {
		validateFields();
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	private void validateFields() {
		String email = mEmailView.getText().toString();
		boolean valid = Utility.requiredFieldValid(mEmailView)
				&& Utility.requiredFieldValid(mPasswordView)
				&& Utility.requiredFieldValid(mNameView)
				&& mEmailValidator.isValidAddressOnly(email);

		mNextButton.setEnabled(valid);
		mManualSetupButton.setEnabled(valid);
		/*
		 * Dim the next button's icon to 50% if the button is disabled. TODO
		 * this can probably be done with a stateful drawable. Check into it.
		 * android:state_enabled
		 */
		Utility.setCompoundDrawablesAlpha(mNextButton,
				mNextButton.isEnabled() ? 255 : 128);
	}

	private String getOwnerName() {
		String name = null;
		try {
			name = getDefaultAccountName();
		} catch (Exception e) {
			Log.e(K9.LOG_TAG, "Could not get default account name", e);
		}

		if (name == null) {
			name = "";
		}
		return name;
	}

	private String getDefaultAccountName() {
		String name = null;
		Account account = Preferences.getPreferences(this).getDefaultAccount();
		if (account != null) {
			name = account.getName();
		}
		return name;
	}

	@Override
	public Dialog onCreateDialog(int id) {
		if (id == DIALOG_NOTE) {
			if (mProvider != null && mProvider.note != null) {
				return new AlertDialog.Builder(this)
						.setMessage(mProvider.note)
						.setPositiveButton(getString(R.string.okay_action),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										String email = mEmailView.getText()
												.toString();
										String password = mPasswordView
												.getText().toString();
										onAutoSetup(email, password);
									}
								})
						.setNegativeButton(getString(R.string.cancel_action),
								null).create();
			}
		}
		return null;
	}

	protected void onNext() {
		String email = mEmailView.getText().toString();
		String password = mPasswordView.getText().toString();
		String[] emailParts = splitEmail(email);
		String domain = emailParts[1];
		mProvider = findProviderForDomain(domain);
		if (mProvider == null) {
			/*
			 * We don't have default settings for this account, start the manual
			 * setup process.
			 */
			onManualSetup();
			return;
		}

		if (mProvider.note != null) {
			showDialog(DIALOG_NOTE);
		} else {

			showLoadingDialog(true);
			onAutoSetup(email, password);

		}
	}

	private void onAutoSetup(final String email, final String password) {

		final boolean mCheckIncoming = true;
		final boolean mCheckOutgoing = true;

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					configAccount(email, password);
					checkRemoteStore(mCheckIncoming, mCheckOutgoing);
				} catch (UnsupportedEncodingException enc) {
					Log.e("lxc", "Couldn't urlencode username or password.",
							enc);
				} catch (URISyntaxException use) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							onManualSetup();
						}
					});
				}
			}
		}).start();
	}

	private void configAccount(String email, String password)
			throws URISyntaxException, UnsupportedEncodingException {

		String[] emailParts = splitEmail(email);
		String user = emailParts[0];
		String domain = emailParts[1];
		URI incomingUri = null;
		URI outgoingUri = null;

		String userEnc = URLEncoder.encode(user, "UTF-8");
		String passwordEnc = URLEncoder.encode(password, "UTF-8");

		String incomingUsername = mProvider.incomingUsernameTemplate;
		incomingUsername = incomingUsername.replaceAll("\\$email", email);
		incomingUsername = incomingUsername.replaceAll("\\$user", userEnc);
		incomingUsername = incomingUsername.replaceAll("\\$domain", domain);

		URI incomingUriTemplate = mProvider.incomingUriTemplate;
		incomingUri = new URI(incomingUriTemplate.getScheme(), incomingUsername
				+ ":" + passwordEnc, incomingUriTemplate.getHost(),
				incomingUriTemplate.getPort(), null, null, null);

		String outgoingUsername = mProvider.outgoingUsernameTemplate;

		URI outgoingUriTemplate = mProvider.outgoingUriTemplate;

		if (outgoingUsername != null) {
			outgoingUsername = outgoingUsername.replaceAll("\\$email", email);
			outgoingUsername = outgoingUsername.replaceAll("\\$user", userEnc);
			outgoingUsername = outgoingUsername.replaceAll("\\$domain", domain);
			outgoingUri = new URI(outgoingUriTemplate.getScheme(),
					outgoingUsername + ":" + passwordEnc,
					outgoingUriTemplate.getHost(),
					outgoingUriTemplate.getPort(), null, null, null);

		} else {
			outgoingUri = new URI(outgoingUriTemplate.getScheme(), null,
					outgoingUriTemplate.getHost(),
					outgoingUriTemplate.getPort(), null, null, null);

		}
		if (mAccount == null) {
			mAccount = Preferences.getPreferences(AccountSetupBasics.this)
					.newAccount();
		}
		mAccount.setName(getOwnerName());
		mAccount.setEmail(email);
		mAccount.setStoreUri(incomingUri.toString());
		mAccount.setTransportUri(outgoingUri.toString());
		mAccount.setDraftsFolderName(getString(R.string.special_mailbox_name_drafts));
		mAccount.setTrashFolderName(getString(R.string.special_mailbox_name_trash));
		mAccount.setArchiveFolderName(getString(R.string.special_mailbox_name_archive));

		if (incomingUriTemplate.getHost().toLowerCase().endsWith(".yahoo.com")) {
			mAccount.setSpamFolderName("Bulk Mail");
		} else {
			mAccount.setSpamFolderName(getString(R.string.special_mailbox_name_spam));
		}

		mAccount.setSentFolderName(getString(R.string.special_mailbox_name_sent));

		if (incomingUri.toString().startsWith("imap")) {
			mAccount.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);
		} else if (incomingUri.toString().startsWith("pop3")) {
			mAccount.setDeletePolicy(Account.DELETE_POLICY_NEVER);
		}

	}

	private void checkRemoteStore(boolean mCheckIncoming, boolean mCheckOutgoing) {
		Store store = null;
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		try {

			final MessagingController ctrl = MessagingController
					.getInstance(getApplication());

			ctrl.clearCertificateErrorNotifications(AccountSetupBasics.this,
					mAccount, mCheckIncoming, mCheckOutgoing);

			if (mCheckIncoming) {
				store = mAccount.getRemoteStore();

				if (store instanceof WebDavStore) {
					setLoadingMessage(R.string.account_setup_check_settings_authenticate);
				} else {
					setLoadingMessage(R.string.account_setup_check_settings_check_incoming_msg);
				}

				store.checkSettings();

				if (store instanceof WebDavStore) {
					setLoadingMessage(R.string.account_setup_check_settings_fetch);
				}
			}

			if (mCheckOutgoing) {
				if (!(mAccount.getRemoteStore() instanceof WebDavStore)) {
					setLoadingMessage(R.string.account_setup_check_settings_check_outgoing_msg);
				}

				Transport transport = Transport.getInstance(mAccount);
				transport.close();
				transport.open();
				transport.close();
			}

			mHandler.sendEmptyMessage(0x123);

		} catch (final AuthenticationFailedException afe) {

			if (mCanceled) {
				showLoadingDialog(false);
				return;
			}
			showErrorDialog(R.string.account_setup_failed_dlg_auth_message_fmt);
		} catch (final CertificateValidationException cve) {

			if (mCanceled) {
				showLoadingDialog(false);
				return;
			}

			X509Certificate[] chain = cve.getCertChain();
			// Avoid NullPointerException in acceptKeyDialog()
			if (chain != null) {
				acceptKeyDialog(
						mCheckIncoming,
						mCheckOutgoing,
						R.string.account_setup_failed_dlg_certificate_message_fmt,
						cve);
			} else {
				showErrorDialog(R.string.account_setup_failed_dlg_server_message_fmt);
			}
		} catch (final Throwable t) {

			if (mCanceled) {
				showLoadingDialog(false);
				return;
			}

			showErrorDialog(R.string.account_setup_failed_dlg_server_message_fmt);

		}

	}

	private void showErrorDialog(final int msgResId) {
		mHandler.post(new Runnable() {
			public void run() {

				showLoadingDialog(false);
				mProgressBar.setIndeterminate(false);
				new AlertDialog.Builder(AccountSetupBasics.this)
						.setTitle(
								getString(R.string.account_setup_failed_dlg_title))
						.setMessage(getString(msgResId))
						.setCancelable(true)
						.setPositiveButton(
								getString(R.string.account_setup_failed_dlg_edit_details_action),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.cancel();
									}
								}).show();
			}
		});
	}

	private void acceptKeyDialog(final boolean mCheckIncoming,
			final boolean mCheckOutgoing, final int msgResId,
			final CertificateValidationException ex) {

		mHandler.post(new Runnable() {

			public void run() {

				showLoadingDialog(false);

				String exMessage = "Unknown Error";

				if (ex != null) {
					if (ex.getCause() != null) {
						if (ex.getCause().getCause() != null) {
							exMessage = ex.getCause().getCause().getMessage();

						} else {
							exMessage = ex.getCause().getMessage();
						}
					} else {
						exMessage = ex.getMessage();
					}
				}

				mProgressBar.setIndeterminate(false);
				StringBuilder chainInfo = new StringBuilder(100);
				MessageDigest sha1 = null;
				try {
					sha1 = MessageDigest.getInstance("SHA-1");
				} catch (NoSuchAlgorithmException e) {
					Log.e(K9.LOG_TAG, "Error while initializing MessageDigest",
							e);
				}

				final X509Certificate[] chain = ex.getCertChain();
				// We already know chain != null (tested before calling this
				// method)
				for (int i = 0; i < chain.length; i++) {
					// display certificate chain information
					// TODO: localize this strings
					chainInfo.append("Certificate chain[").append(i)
							.append("]:\n");
					chainInfo.append("Subject: ")
							.append(chain[i].getSubjectDN().toString())
							.append("\n");

					// display SubjectAltNames too
					// (the user may be mislead into mistrusting a certificate
					// by a subjectDN not matching the server even though a
					// SubjectAltName matches)
					try {
						final Collection<List<?>> subjectAlternativeNames = chain[i]
								.getSubjectAlternativeNames();
						if (subjectAlternativeNames != null) {
							// The list of SubjectAltNames may be very long
							// TODO: localize this string
							StringBuilder altNamesText = new StringBuilder();
							altNamesText.append("Subject has ")
									.append(subjectAlternativeNames.size())
									.append(" alternative names\n");

							// we need these for matching
							String storeURIHost = (Uri.parse(mAccount
									.getStoreUri())).getHost();
							String transportURIHost = (Uri.parse(mAccount
									.getTransportUri())).getHost();

							for (List<?> subjectAlternativeName : subjectAlternativeNames) {
								Integer type = (Integer) subjectAlternativeName
										.get(0);
								Object value = subjectAlternativeName.get(1);
								String name = "";
								switch (type.intValue()) {
								case 0:
									Log.w(K9.LOG_TAG,
											"SubjectAltName of type OtherName not supported.");
									continue;
								case 1: // RFC822Name
									name = (String) value;
									break;
								case 2: // DNSName
									name = (String) value;
									break;
								case 3:
									Log.w(K9.LOG_TAG,
											"unsupported SubjectAltName of type x400Address");
									continue;
								case 4:
									Log.w(K9.LOG_TAG,
											"unsupported SubjectAltName of type directoryName");
									continue;
								case 5:
									Log.w(K9.LOG_TAG,
											"unsupported SubjectAltName of type ediPartyName");
									continue;
								case 6: // Uri
									name = (String) value;
									break;
								case 7: // ip-address
									name = (String) value;
									break;
								default:
									Log.w(K9.LOG_TAG,
											"unsupported SubjectAltName of unknown type");
									continue;
								}

								// if some of the SubjectAltNames match the
								// store or transport -host,
								// display them
								if (name.equalsIgnoreCase(storeURIHost)
										|| name.equalsIgnoreCase(transportURIHost)) {
									// TODO: localize this string
									altNamesText.append("Subject(alt): ")
											.append(name).append(",...\n");
								} else if (name.startsWith("*.")
										&& (storeURIHost.endsWith(name
												.substring(2)) || transportURIHost
												.endsWith(name.substring(2)))) {
									// TODO: localize this string
									altNamesText.append("Subject(alt): ")
											.append(name).append(",...\n");
								}
							}
							chainInfo.append(altNamesText);
						}
					} catch (Exception e1) {
						// don't fail just because of subjectAltNames
						Log.w(K9.LOG_TAG,
								"cannot display SubjectAltNames in dialog", e1);
					}

					chainInfo.append("Issuer: ")
							.append(chain[i].getIssuerDN().toString())
							.append("\n");
					if (sha1 != null) {
						sha1.reset();
						try {
							char[] sha1sum = Hex.encodeHex(sha1.digest(chain[i]
									.getEncoded()));
							chainInfo.append("Fingerprint (SHA-1): ")
									.append(new String(sha1sum)).append("\n");
						} catch (CertificateEncodingException e) {
							Log.e(K9.LOG_TAG,
									"Error while encoding certificate", e);
						}
					}
				}

				new AlertDialog.Builder(AccountSetupBasics.this)
						.setTitle(
								getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
						.setMessage(
								getString(msgResId, exMessage) + " "
										+ chainInfo.toString())
						.setCancelable(true)
						.setPositiveButton(
								getString(R.string.account_setup_failed_dlg_invalid_certificate_accept),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {

										try {
											String alias = mAccount.getUuid();
											if (mCheckIncoming) {
												alias = alias + ".incoming";
											}
											if (mCheckOutgoing) {
												alias = alias + ".outgoing";
											}
											TrustManagerFactory
													.addCertificateChain(alias,
															chain);
										} catch (CertificateException e) {
											showErrorDialog(R.string.account_setup_failed_dlg_certificate_message_fmt);
										}

										// modified by lxc at 2014-01-16
										// Show loading dialog, and auto setup.
										showLoadingDialog(true);
										String email = mEmailView.getText().toString();
										String password = mPasswordView.getText().toString();
										onAutoSetup(email, password);

										// AccountSetupCheckSettings
										// .actionCheckSettings(
										// AccountSetupBasics.this,
										// mAccount,
										// mCheckIncoming,
										// mCheckOutgoing);
									}
								})
						.setNegativeButton(
								getString(R.string.account_setup_failed_dlg_invalid_certificate_reject),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										finish();
									}
								}).show();
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if(mAccount != null && !TextUtils.isEmpty(mAccount.getDescription())) {
				mAccount.setDescription(mAccount.getDescription());
			} else {
				mAccount.setDescription(mAccount.getEmail());
			}
			mAccount.save(Preferences.getPreferences(this));
			K9.setServicesEnabled(this);
			AccountSetupOptions.actionOptions(this, mAccount, false);
			// AccountSetupNames.actionSetNames(this, mAccount);
			finish();
		}
	}

	/**
	 * modified by lxc at 2014-02-22
	 * 用户点击返回按钮时，删除未配置完成的账户
	 */
	@Override
	public void onBackPressed() {
		if(mAccount != null) {
            try {
            	mAccount.getLocalStore().delete();
            } catch (Exception e) {
                // Ignore, this may lead to localStores on sd-cards that
                // are currently not inserted to be left
            }
            MessagingController.getInstance(getApplication()).notifyAccountCancel(AccountSetupBasics.this, mAccount);
            Preferences.getPreferences(AccountSetupBasics.this).deleteAccount(mAccount);
            K9.setServicesEnabled(AccountSetupBasics.this);
		}
		super.onBackPressed();
	}
	
	
	private void onManualSetup() {
		String email = mEmailView.getText().toString();
		String password = mPasswordView.getText().toString();
		String description = mDescriptionView.getText().toString();
		String[] emailParts = splitEmail(email);
		String user = emailParts[0];
		String domain = emailParts[1];

		if (mAccount == null) {
			mAccount = Preferences.getPreferences(this).newAccount();
		}
		mAccount.setName(getOwnerName());
		mAccount.setEmail(email);
		mAccount.setDescription(description);
		try {
			String userEnc = URLEncoder.encode(user, "UTF-8");
			String passwordEnc = URLEncoder.encode(password, "UTF-8");

			URI uri = new URI("placeholder", userEnc + ":" + passwordEnc,
					"mail." + domain, -1, null, null, null);
			mAccount.setStoreUri(uri.toString());
			mAccount.setTransportUri(uri.toString());
		} catch (UnsupportedEncodingException enc) {
			// This really shouldn't happen since the encoding is hardcoded to
			// UTF-8
			Log.e(K9.LOG_TAG, "Couldn't urlencode username or password.", enc);
		} catch (URISyntaxException use) {
			/*
			 * If we can't set up the URL we just continue. It's only for
			 * convenience.
			 */
		}
		mAccount.setDraftsFolderName(getString(R.string.special_mailbox_name_drafts));
		mAccount.setTrashFolderName(getString(R.string.special_mailbox_name_trash));
		mAccount.setSentFolderName(getString(R.string.special_mailbox_name_sent));
		mAccount.setArchiveFolderName(getString(R.string.special_mailbox_name_archive));
		// Yahoo! has a special folder for Spam, called "Bulk Mail".
		if (domain.endsWith(".yahoo.com")) {
			mAccount.setSpamFolderName("Bulk Mail");
		} else {
			mAccount.setSpamFolderName(getString(R.string.special_mailbox_name_spam));
		}

		AccountSetupAccountType.actionSelectAccountType(this, mAccount, false);

		// finish();
	}

	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.next) {
			onNext();
		} else if (id == R.id.manual_setup) {
			onManualSetup();
		}
	}

	public void showLoadingDialog(boolean show) {
		if (show) {
			mLoadingDialog.show();
		} else {
			mLoadingDialog.cancel();
		}
	}

	public void setLoadingMessage(final int resId) {
		mHandler.post(new Runnable() {
			public void run() {
				if (mDestroyed) {
					return;
				}
				mLoadingMessageView.setText(getString(resId));// message);
			}
		});
	}

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0x123) {
				showLoadingDialog(false);

				mAccount.setDescription(mAccount.getEmail());
				if (Utility.requiredFieldValid(mDescriptionView)) {
					mAccount.setDescription(mDescriptionView.getText()
							.toString());
				}
				mAccount.setName(mNameView.getText().toString());
				mAccount.save(Preferences
						.getPreferences(AccountSetupBasics.this));
				K9.setServicesEnabled(AccountSetupBasics.this);
				AccountSetupOptions.actionOptions(AccountSetupBasics.this,
						mAccount, false);
				// finish();
			}
		};
	};

	/**
	 * Attempts to get the given attribute as a String resource first, and if it
	 * fails returns the attribute as a simple String value.
	 * 
	 * @param xml
	 * @param name
	 * @return
	 */
	private String getXmlAttribute(XmlResourceParser xml, String name) {
		int resId = xml.getAttributeResourceValue(null, name, 0);
		if (resId == 0) {
			return xml.getAttributeValue(null, name);
		} else {
			return getString(resId);
		}
	}

	private Provider findProviderForDomain(String domain) {
		try {
			XmlResourceParser xml = getResources().getXml(R.xml.providers);
			int xmlEventType;
			Provider provider = null;
			while ((xmlEventType = xml.next()) != XmlResourceParser.END_DOCUMENT) {
				if (xmlEventType == XmlResourceParser.START_TAG
						&& "provider".equals(xml.getName())
						&& domain.equalsIgnoreCase(getXmlAttribute(xml,
								"domain"))) {
					provider = new Provider();
					provider.id = getXmlAttribute(xml, "id");
					provider.label = getXmlAttribute(xml, "label");
					provider.domain = getXmlAttribute(xml, "domain");
					provider.note = getXmlAttribute(xml, "note");
				} else if (xmlEventType == XmlResourceParser.START_TAG
						&& "incoming".equals(xml.getName()) && provider != null) {
					provider.incomingUriTemplate = new URI(getXmlAttribute(xml,
							"uri"));
					provider.incomingUsernameTemplate = getXmlAttribute(xml,
							"username");
				} else if (xmlEventType == XmlResourceParser.START_TAG
						&& "outgoing".equals(xml.getName()) && provider != null) {
					provider.outgoingUriTemplate = new URI(getXmlAttribute(xml,
							"uri"));
					provider.outgoingUsernameTemplate = getXmlAttribute(xml,
							"username");
				} else if (xmlEventType == XmlResourceParser.END_TAG
						&& "provider".equals(xml.getName()) && provider != null) {
					return provider;
				}
			}
		} catch (Exception e) {
			Log.e(K9.LOG_TAG, "Error while trying to load provider settings.",
					e);
		}
		return null;
	}

	private String[] splitEmail(String email) {
		String[] retParts = new String[2];
		String[] emailParts = email.split("@");
		retParts[0] = (emailParts.length > 0) ? emailParts[0] : "";
		retParts[1] = (emailParts.length > 1) ? emailParts[1] : "";
		return retParts;
	}

	static class Provider implements Serializable {
		private static final long serialVersionUID = 8511656164616538989L;

		public String id;

		public String label;

		public String domain;

		public URI incomingUriTemplate;

		public String incomingUsernameTemplate;

		public URI outgoingUriTemplate;

		public String outgoingUsernameTemplate;

		public String note;
	}
}
