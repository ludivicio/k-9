package org.ancode.secmail.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.ancode.secmail.Account;
import org.ancode.secmail.AccountStats;
import org.ancode.secmail.BaseAccount;
import org.ancode.secmail.FontSizes;
import org.ancode.secmail.K9;
import org.ancode.secmail.Preferences;
import org.ancode.secmail.R;
import org.ancode.secmail.activity.Accounts;
import org.ancode.secmail.activity.ActivityListener;
import org.ancode.secmail.activity.FolderInfoHolder;
import org.ancode.secmail.activity.MessageList;
import org.ancode.secmail.controller.MessagingController;
import org.ancode.secmail.helper.SizeFormatter;
import org.ancode.secmail.mail.Folder;
import org.ancode.secmail.mail.Message;
import org.ancode.secmail.mail.MessagingException;
import org.ancode.secmail.search.LocalSearch;
import org.ancode.secmail.search.SearchSpecification.Attribute;
import org.ancode.secmail.search.SearchSpecification.Searchfield;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnClosedListener;

public class MenuFragment extends SherlockFragment {

	private ListView mListView;
	private TextView mUserEmail;
	private TextView mAccountSetting;
	private ImageButton mEncryptButton;
	private MessageList mActivity;
	private Account mAccount;
	private FolderListAdapter mAdapter;
	private FolderListHandler mHandler = new FolderListHandler();
	private FontSizes mFontSizes = K9.getFontSizes();
	private LayoutInflater mInflater;
	private MessagingController mController;
	
	private String mCurFolderName = "INBOX";
	private String mFolderName = "INBOX";
	
	private static final int CHOOSE_FOLDER_EVENT = 0;
	private static final int ENCRYPT_EVENT = 1;
	private static final int DECRYPT_EVENT = 2;
	
	private int closedEvent = CHOOSE_FOLDER_EVENT;
	
	class FolderListHandler extends Handler {

		public void newFolders(final List<FolderInfoHolder> newFolders) {
			
			mHandler.post(new Runnable() {
				public void run() {
					mAdapter.mFolders.clear();
					mAdapter.mFolders.addAll(newFolders);
					mAdapter.mFilteredFolders = mAdapter.mFolders;
					mHandler.dataChanged();
				}
			});
		}

		public void workingAccount(final int res) {
			mHandler.post(new Runnable() {
				public void run() {
					String toastText = getString(res, mAccount.getDescription());
					Toast toast = Toast.makeText(getApplication(), toastText,
							Toast.LENGTH_SHORT);
					toast.show();
				}
			});
		}

		public void accountSizeChanged(final long oldSize, final long newSize) {
			mHandler.post(new Runnable() {
				public void run() {
					String toastText = getString(
							R.string.account_size_changed,
							mAccount.getDescription(),
							SizeFormatter.formatSize(getApplication(), oldSize),
							SizeFormatter.formatSize(getApplication(), newSize));

					Toast toast = Toast.makeText(getApplication(), toastText,
							Toast.LENGTH_LONG);

					toast.show();
				}
			});
		}

		public void folderLoading(final String folder, final boolean loading) {
			mHandler.post(new Runnable() {
				public void run() {
					FolderInfoHolder folderHolder = mAdapter.getFolder(folder);

					if (folderHolder != null) {
						folderHolder.loading = loading;
					}

				}
			});
		}

		public void progress(final boolean progress) {
			mHandler.post(new Runnable() {
				public void run() {

				}
			});
		}

		public void dataChanged() {

			mHandler.post(new Runnable() {
				public void run() {
					mAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void handleMessage(android.os.Message msg) {

			onOpenFolder(String.valueOf(msg.obj));

			super.handleMessage(msg);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mActivity = (MessageList) getActivity();
		mAccount = mActivity.getAccount();
		
		if(mAccount == null) {
			return super.onCreateView(inflater, container, savedInstanceState);
		}
		
		mController = MessagingController.getInstance(getApplication());

		mInflater = inflater;

		View v = inflater.inflate(R.layout.slide_menu_content, container, false);
		mListView = (ListView) v.findViewById(R.id.menu_listview);

		mUserEmail = (TextView) v.findViewById(R.id.tv_user_email);
		if(mAccount != null && !TextUtils.isEmpty(mAccount.getEmail())) {
			mUserEmail.setText(mAccount.getEmail());
		} 
		
		mEncryptButton = (ImageButton) v.findViewById(R.id.ib_account_encrypt);
		
		if (mAccount.getRegCode() == null || mAccount.getRegCode().trim().equals("")) {
			mEncryptButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_button_unlock));
		} else {
			mEncryptButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_button_lock));
		}
		
		mEncryptButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				if (mAccount.getRegCode() == null || mAccount.getRegCode().trim().equals("")) {
					closedEvent = ENCRYPT_EVENT;
				} else {
					closedEvent = DECRYPT_EVENT;
				}
				
				mActivity.toggle();
			}
			
		});
		
		mAccountSetting = (TextView) v.findViewById(R.id.tv_account_setting);
		mAccountSetting.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onAccounts();
			}
		});
		
		SlidingMenu slidingMenu = mActivity.getSlidingMenu();
		slidingMenu.setOnClosedListener(onClosedListener);
		
		mListView.setFastScrollEnabled(true);
		mListView.setItemsCanFocus(false);
		mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);

		mListView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mFolderName = ((FolderInfoHolder) mAdapter.getItem(position)).name;
				closedEvent = CHOOSE_FOLDER_EVENT;
				mActivity.toggle();
			}
		});

		mAdapter = new FolderListAdapter();
		mListView.setAdapter(mAdapter);

		mController.addListener(mAdapter.mListener);
		mController.getAccountStats(getApplication(), mAccount,
				mAdapter.mListener);
		mController.notifyAccountCancel(getApplication(), mAccount);

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		onRefresh(true);
	}

	@Override
	public void onPause() {
		super.onPause();
		MessagingController.getInstance(getApplication()).removeListener(mAdapter.mListener);
	}

	private void onAccounts() {
        Accounts.listAccounts(mActivity);
    }
	
	private Application getApplication() {
		if (mActivity != null) {
			return mActivity.getApplication();
		}
		return null;
	}

	private void onOpenFolder(String folder) {
		LocalSearch search = new LocalSearch(folder);
		search.addAccountUuid(mAccount.getUuid());
		search.addAllowedFolder(folder);
		MessageList.actionDisplaySearch(mActivity, search, false, false);
	}

	private void onRefresh(final boolean forceRemote) {
		mController.listFolders(mAccount, forceRemote, mAdapter.mListener);
	}

	private OnClosedListener onClosedListener = new OnClosedListener() {
		
		@Override
		public void onClosed() {
			if(closedEvent == CHOOSE_FOLDER_EVENT) {
				if(mCurFolderName != null && !mCurFolderName.equals(mFolderName)) {
					mCurFolderName = mFolderName;
					onOpenFolder(mFolderName);
					closedEvent = -1;
				} 
			} else if(closedEvent == ENCRYPT_EVENT) {
				closedEvent = -1;
				mActivity.registEncryptService(mEncryptButton);
			} else if(closedEvent == DECRYPT_EVENT) {
				closedEvent = -1;
				mActivity.registDecryptService(mEncryptButton);
			}
		}
	};
	
	
	class FolderListAdapter extends BaseAdapter implements Filterable {

		private ArrayList<FolderInfoHolder> mFolders = new ArrayList<FolderInfoHolder>();

		private List<FolderInfoHolder> mFilteredFolders = Collections.unmodifiableList(mFolders);

		private Filter mFilter = new FolderListFilter();

		public Object getItem(long position) {
			return getItem((int) position);
		}

		public Object getItem(int position) {
			return mFilteredFolders.get(position);
		}

		public long getItemId(int position) {
			return mFilteredFolders.get(position).folder.getName().hashCode();
		}

		public int getCount() {
			return mFilteredFolders.size();
		}

		@Override
		public boolean isEnabled(int item) {
			return true;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		public int getFolderIndex(String folder) {
			FolderInfoHolder searchHolder = new FolderInfoHolder();
			searchHolder.name = folder;
			return mFilteredFolders.indexOf(searchHolder);
		}

		public FolderInfoHolder getFolder(String folder) {
			FolderInfoHolder holder = null;

			int index = getFolderIndex(folder);
			if (index >= 0) {
				holder = (FolderInfoHolder) getItem(index);
				if (holder != null) {
					return holder;
				}
			}
			return null;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		public boolean isItemSelectable(int position) {
			return true;
		}

		public void setFilter(final Filter filter) {
			this.mFilter = filter;
		}

		public Filter getFilter() {
			return mFilter;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position <= getCount()) {
				return getItemView(position, convertView, parent);
			} else {
				Log.e("lxc", "getView with illegal positon=" + position
						+ " called! count is only " + getCount());
				return null;
			}
		}

		public View getItemView(int itemPosition, View convertView, ViewGroup parent) {
			FolderInfoHolder folder = (FolderInfoHolder) getItem(itemPosition);
			View view;
			if (convertView != null) {
				view = convertView;
			} else {
				view = mInflater.inflate(R.layout.slide_menu_folder_list_item, parent, false);
			}

			FolderViewHolder holder = (FolderViewHolder) view.getTag();

			if (holder == null) {
				holder = new FolderViewHolder();
				
				holder.folderListItemLayout = (LinearLayout) view.findViewById(R.id.folder_list_item_layout);
				holder.folderIcon = (ImageView) view.findViewById(R.id.folder_icon);
				holder.folderName = (TextView) view.findViewById(R.id.folder_name);
				holder.folderStatus = (TextView) view.findViewById(R.id.folder_status);
				
				holder.activeIcons = (RelativeLayout) view.findViewById(R.id.active_icons);
				holder.newMessageCountWrapper = (View) view.findViewById(R.id.new_message_count_wrapper);
				holder.newMessageCount = (TextView) view.findViewById(R.id.new_message_count);
				
				holder.flaggedMessageCountWrapper = (View) view.findViewById(R.id.flagged_message_count_wrapper);
				holder.flaggedMessageCountIcon = (View) view.findViewById(R.id.flagged_message_count_icon);
				holder.flaggedMessageCount = (TextView) view.findViewById(R.id.flagged_message_count);
				
				holder.rawFolderName = folder.name;
				view.setTag(holder);
			}

			if (folder == null) {
				return view;
			}

			final String folderStatus;

			if (folder.loading) {
				folderStatus = getString(R.string.status_loading);
			} else if (folder.status != null) {
				folderStatus = folder.status;
			} else if (folder.lastChecked != 0) {
				long now = System.currentTimeMillis();
				int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
				CharSequence formattedDate;

				if (Math.abs(now - folder.lastChecked) > DateUtils.WEEK_IN_MILLIS) {
					formattedDate = getString(R.string.preposition_for_date,
							DateUtils.formatDateTime(getApplication(),
									folder.lastChecked, flags));
				} else {
					formattedDate = DateUtils.getRelativeTimeSpanString(
							folder.lastChecked, now,
							DateUtils.MINUTE_IN_MILLIS, flags);
				}

				folderStatus = getString(
						folder.pushActive ? R.string.last_refresh_time_format_with_push
								: R.string.last_refresh_time_format,
						formattedDate);
			} else {
				folderStatus = null;
			}

			holder.folderName.setText(folder.displayName);
			if (folderStatus != null) {
				holder.folderStatus.setText(folderStatus);
				holder.folderStatus.setVisibility(View.VISIBLE);
			} else {
				holder.folderStatus.setVisibility(View.GONE);
			}

			if (folder.unreadMessageCount == -1) {
				folder.unreadMessageCount = 0;
				try {
					folder.unreadMessageCount = folder.folder.getUnreadMessageCount();
				} catch (Exception e) {
					Log.e("lxc", "Unable to get unreadMessageCount for "
							+ mAccount.getDescription() + ":" + folder.name);
				}
			}
			
			if (folder.unreadMessageCount > 0) {
				
				// modified by lxc at 2013-12-06
				String unreadMessageText = "";
				if(folder.unreadMessageCount < 100) {
					unreadMessageText = folder.unreadMessageCount + "";
				} else {
					unreadMessageText = "99+";
				}
				holder.newMessageCount.setText(unreadMessageText);
				
				holder.newMessageCountWrapper.setOnClickListener(createUnreadSearch(mAccount, folder));
				holder.newMessageCountWrapper.setVisibility(View.VISIBLE);
			} else {
				holder.newMessageCountWrapper.setVisibility(View.GONE);
			}

			if (folder.flaggedMessageCount == -1) {
				folder.flaggedMessageCount = 0;
				try {
					folder.flaggedMessageCount = folder.folder.getFlaggedMessageCount();
				} catch (Exception e) {
					Log.e(K9.LOG_TAG, "Unable to get flaggedMessageCount for "
							+ mAccount.getDescription() + ":" + folder.name);
				}

			}

			// Hide the layout.
			holder.flaggedMessageCountWrapper.setVisibility(View.GONE);
			
			holder.activeIcons.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
//					Toast toast = Toast.makeText(getApplication(),
//							getString(R.string.tap_hint), Toast.LENGTH_SHORT);
//					toast.show();
				}
			});

			holder.folderIcon.setImageResource(folder.folderIcon);
			
			mFontSizes.setViewTextSize(holder.folderName, mFontSizes.getFolderName());

			if (K9.wrapFolderNames()) {
				holder.folderName.setEllipsize(null);
				holder.folderName.setSingleLine(false);
			} else {
				holder.folderName.setEllipsize(TruncateAt.START);
				holder.folderName.setSingleLine(true);
			}
			mFontSizes.setViewTextSize(holder.folderStatus,
					mFontSizes.getFolderStatus());

			return view;
		}
		
		private OnClickListener createUnreadSearch(Account account, FolderInfoHolder folder) {
			String searchTitle = getString(
					R.string.search_title,
					getString(R.string.message_list_title,
							account.getDescription(), folder.displayName),
					getString(R.string.unread_modifier));

			LocalSearch search = new LocalSearch(searchTitle);
			search.and(Searchfield.READ, "1", Attribute.NOT_EQUALS);

			search.addAllowedFolder(folder.name);
			search.addAccountUuid(account.getUuid());

			return new FolderClickListener(search);
		}


		/**
		 * Filter to search for occurences of the search-expression in any place
		 * of the folder-name instead of doing jsut a prefix-search.
		 * 
		 * @author Marcus@Wolschon.biz
		 */
		public class FolderListFilter extends Filter {
			private CharSequence mSearchTerm;

			public CharSequence getSearchTerm() {
				return mSearchTerm;
			}

			/**
			 * Do the actual search. {@inheritDoc}
			 * 
			 * @see #publishResults(CharSequence, FilterResults)
			 */
			@Override
			protected FilterResults performFiltering(CharSequence searchTerm) {
				mSearchTerm = searchTerm;
				FilterResults results = new FilterResults();

				if ((searchTerm == null) || (searchTerm.length() == 0)) {
					ArrayList<FolderInfoHolder> list = new ArrayList<FolderInfoHolder>(
							mFolders);
					results.values = list;
					results.count = list.size();
				} else {
					final String searchTermString = searchTerm.toString()
							.toLowerCase();
					final String[] words = searchTermString.split(" ");
					final int wordCount = words.length;

					final ArrayList<FolderInfoHolder> newValues = new ArrayList<FolderInfoHolder>();

					for (final FolderInfoHolder value : mFolders) {
						if (value.displayName == null) {
							continue;
						}
						final String valueText = value.displayName
								.toLowerCase();

						for (int k = 0; k < wordCount; k++) {
							if (valueText.contains(words[k])) {
								newValues.add(value);
								break;
							}
						}
					}

					results.values = newValues;
					results.count = newValues.size();
				}

				return results;
			}

			/**
			 * Publish the results to the user-interface. {@inheritDoc}
			 */
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				mFilteredFolders = Collections
						.unmodifiableList((ArrayList<FolderInfoHolder>) results.values);
				notifyDataSetChanged();
			}
		}

		private ActivityListener mListener = new ActivityListener() {
			
			@Override
			public void informUserOfStatus() {

			}

			@Override
			public void accountStatusChanged(BaseAccount account,
					AccountStats stats) {
				if (!account.equals(mAccount)) {
					return;
				}
				if (stats == null) {
					return;
				}
			}

			@Override
			public void listFoldersStarted(Account account) {
				super.listFoldersStarted(account);
			}

			@Override
			public void listFoldersFailed(Account account, String message) {
				super.listFoldersFailed(account, message);
			}

			@Override
			public void listFoldersFinished(Account account) {
				if (account.equals(mAccount)) {
					mHandler.progress(false);
					mController.removeListener(mAdapter.mListener);
				}
				super.listFoldersFinished(account);
			}

			@Override
			public void listFolders(Account account, Folder[] folders) {

				if (account.equals(mAccount)) {

					List<FolderInfoHolder> newFolders = new LinkedList<FolderInfoHolder>();
					List<FolderInfoHolder> topFolders = new LinkedList<FolderInfoHolder>();

					Account.FolderMode aMode = account.getFolderDisplayMode();
					Preferences prefs = Preferences.getPreferences(getApplication().getApplicationContext());
					for (Folder folder : folders) {
						try {
							folder.refresh(prefs);
							Folder.FolderClass fMode = folder.getDisplayClass();
							if ((aMode == Account.FolderMode.FIRST_CLASS && fMode != Folder.FolderClass.FIRST_CLASS)
									|| (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS
											&& fMode != Folder.FolderClass.FIRST_CLASS && fMode != Folder.FolderClass.SECOND_CLASS)
									|| (aMode == Account.FolderMode.NOT_SECOND_CLASS && fMode == Folder.FolderClass.SECOND_CLASS)) {
								continue;
							}
						} catch (MessagingException me) {
							Log.e("lxc",
									"Couldn't get prefs to check for displayability of folder "
											+ folder.getName(), me);
						}

						FolderInfoHolder holder = null;

						int folderIndex = getFolderIndex(folder.getName());
						if (folderIndex >= 0) {
							holder = (FolderInfoHolder) getItem(folderIndex);
						}

						if (holder == null) {
							holder = new FolderInfoHolder(getApplication(), folder, mAccount, -1);
						} else {
							holder.populate(getApplication(), folder, mAccount, -1);
						}
						
						if (folder.isInTopGroup()) {
							topFolders.add(holder);
						} else {
							newFolders.add(holder);
						}
					}

					Collections.sort(newFolders);
					Collections.sort(topFolders);
					topFolders.addAll(newFolders);
					mHandler.newFolders(topFolders);
				}

				super.listFolders(account, folders);
			}

			@Override
			public void synchronizeMailboxStarted(Account account, String folder) {
				super.synchronizeMailboxStarted(account, folder);
				if (account.equals(mAccount)) {

					mHandler.progress(true);
					mHandler.folderLoading(folder, true);
				}
			}

			@Override
			public void synchronizeMailboxFinished(Account account,
					String folder, int totalMessagesInMailbox,
					int numNewMessages) {
				super.synchronizeMailboxFinished(account, folder,
						totalMessagesInMailbox, numNewMessages);
				if (account.equals(mAccount)) {
					mHandler.progress(false);
					mHandler.folderLoading(folder, false);

					refreshFolder(account, folder);
				}

			}

			private void refreshFolder(Account account, String folderName) {

				Folder localFolder = null;
				try {
					if (account != null && folderName != null) {
						if (!account.isAvailable(getApplication())) {
							Log.i(K9.LOG_TAG,
									"not refreshing folder of unavailable account");
							return;
						}
						localFolder = account.getLocalStore().getFolder(
								folderName);
						FolderInfoHolder folderHolder = getFolder(folderName);
						if (folderHolder != null) {
							folderHolder.populate(getApplication(),
									localFolder, mAccount, -1);
							folderHolder.flaggedMessageCount = -1;
							mHandler.dataChanged();
						}
					}
				} catch (Exception e) {
					Log.e(K9.LOG_TAG, "Exception while populating folder", e);
				} finally {
					if (localFolder != null) {
						localFolder.close();
					}
				}

			}

			@Override
			public void synchronizeMailboxFailed(Account account,
					String folder, String message) {
				super.synchronizeMailboxFailed(account, folder, message);
				if (!account.equals(mAccount)) {
					return;
				}

				mHandler.progress(false);
				mHandler.folderLoading(folder, false);
				FolderInfoHolder holder = getFolder(folder);

				if (holder != null) {
					holder.lastChecked = 0;
				}

				mHandler.dataChanged();
			}

			@Override
			public void setPushActive(Account account, String folderName,
					boolean enabled) {
				if (!account.equals(mAccount)) {
					return;
				}
				FolderInfoHolder holder = getFolder(folderName);

				if (holder != null) {
					holder.pushActive = enabled;

					mHandler.dataChanged();
				}
			}

			@Override
			public void messageDeleted(Account account, String folder,
					Message message) {
				synchronizeMailboxRemovedMessage(account, folder, message);
			}

			@Override
			public void emptyTrashCompleted(Account account) {
				if (account.equals(mAccount)) {
					refreshFolder(account, mAccount.getTrashFolderName());
				}
			}

			@Override
			public void folderStatusChanged(Account account, String folderName,
					int unreadMessageCount) {
				if (account.equals(mAccount)) {
					refreshFolder(account, folderName);
					informUserOfStatus();
				}
			}

			@Override
			public void sendPendingMessagesCompleted(Account account) {
				super.sendPendingMessagesCompleted(account);
				if (account.equals(mAccount)) {
					refreshFolder(account, mAccount.getOutboxFolderName());
				}
			}

			@Override
			public void sendPendingMessagesStarted(Account account) {
				super.sendPendingMessagesStarted(account);

				if (account.equals(mAccount)) {
					mHandler.dataChanged();
				}
			}

			@Override
			public void sendPendingMessagesFailed(Account account) {
				super.sendPendingMessagesFailed(account);
				if (account.equals(mAccount)) {
					refreshFolder(account, mAccount.getOutboxFolderName());
				}
			}

			@Override
			public void accountSizeChanged(Account account, long oldSize,
					long newSize) {
				if (account.equals(mAccount)) {
					mHandler.accountSizeChanged(oldSize, newSize);
				}
			}
		};
	}

	static class FolderViewHolder {
		public TextView folderName;
		public TextView folderStatus;
		public TextView newMessageCount;
		public TextView flaggedMessageCount;
		public View flaggedMessageCountIcon;
		public View newMessageCountWrapper;
		public View flaggedMessageCountWrapper;

		public RelativeLayout activeIcons;
		public String rawFolderName;
		public ImageView folderIcon;
		public LinearLayout folderListItemLayout;
	}

	private class FolderClickListener implements OnClickListener {

		final LocalSearch search;

		FolderClickListener(LocalSearch search) {
			this.search = search;
		}

		@Override
		public void onClick(View v) {
			MessageList.actionDisplaySearch(getApplication(), search, true,
					false);
		}
	}
	
}
