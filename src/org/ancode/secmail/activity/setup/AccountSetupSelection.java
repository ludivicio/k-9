package org.ancode.secmail.activity.setup;

import java.util.ArrayList;

import org.ancode.secmail.R;
import org.ancode.secmail.activity.Accounts;
import org.ancode.secmail.activity.K9Activity;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AccountSetupSelection extends K9Activity implements OnItemClickListener {

	private static final String PACKAGE_NAME = "org.ancode.secmail";
	private ListView mListView;
	private MenuItem importSettings;
	private EmailProviderAdapter adapter;
	private String[] emailTypes = null;

	public static void actionChooseEmailProvider(Context context) {
		Intent i = new Intent(context, AccountSetupSelection.class);
		context.startActivity(i);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_setup_selection);
		mListView = (ListView) findViewById(R.id.lv_choose_email);
		emailTypes = getResources().getStringArray(R.array.account_email_providers);
		
		ArrayList<Integer> drawableIds = new ArrayList<Integer>();
		for(int i = 0; i < emailTypes.length; i ++) {
			drawableIds.add(getResources().getIdentifier(emailTypes[i], "drawable", PACKAGE_NAME));
		}
		
		adapter = new EmailProviderAdapter(drawableIds.toArray(new Integer[]{}));
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(this);
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.accounts_setup_selection_option, menu);
        importSettings = menu.findItem(R.id.import_settings);
        return true;
	}

	class EmailProviderAdapter extends BaseAdapter {

		Integer[] providers = null;
		
		public EmailProviderAdapter(Integer[] data) {
			providers = data;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView != null) {
				view = convertView;
			} else {
				view = getLayoutInflater().inflate(R.layout.account_email_type_list_item, parent, false);
			}

			ViewHolder holder = (ViewHolder) view.getTag();

			if (holder == null) {
				holder = new ViewHolder();
				holder.mEmailType = (TextView) view.findViewById(R.id.tv_email_provider);
				view.setTag(holder);
			}
			holder.mEmailType.setBackgroundResource(((Integer)getItem(position)).intValue());
			return view;
		}

		class ViewHolder {
			public TextView mEmailType;
		}

		@Override
		public int getCount() {
			return providers.length;
		}

		@Override
		public Object getItem(int position) {
			return providers[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		AccountSetupBasics.actionNewAccount(this, emailTypes[position]);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.import_settings) {
			Accounts.importSettings(this);
		} else {
			return super.onOptionsItemSelected(item);
		}
		return false;
	
	}
}
