package org.ancode.secmail.activity;

import org.ancode.secmail.Identity;
import org.ancode.secmail.Preferences;
import org.ancode.secmail.R;

import android.content.Intent;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ManageIdentities extends ChooseIdentity {
    private boolean mIdentitiesChanged = false;
    public static final String EXTRA_IDENTITIES = "org.ancode.secmail.EditIdentity_identities";

    private static final int ACTIVITY_EDIT_IDENTITY = 1;

    @Override
    protected void setupClickListeners() {
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editItem(position);
            }
        });

        ListView listView = getListView();
        registerForContextMenu(listView);
    }

    private void editItem(int i) {
        Intent intent = new Intent(ManageIdentities.this, EditIdentity.class);
        intent.putExtra(EditIdentity.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(EditIdentity.EXTRA_IDENTITY, mAccount.getIdentity(i));
        intent.putExtra(EditIdentity.EXTRA_IDENTITY_INDEX, i);
        startActivityForResult(intent, ACTIVITY_EDIT_IDENTITY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.manage_identities_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
		if (itemId == R.id.new_identity) {
			Intent intent = new Intent(ManageIdentities.this, EditIdentity.class);
			intent.putExtra(EditIdentity.EXTRA_ACCOUNT, mAccount.getUuid());
			startActivityForResult(intent, ACTIVITY_EDIT_IDENTITY);
		} else {
			return super.onOptionsItemSelected(item);
		}
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.manage_identities_context_menu_title);
        getMenuInflater().inflate(R.menu.manage_identities_context, menu);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        int itemId = item.getItemId();
		if (itemId == R.id.edit) {
			editItem(menuInfo.position);
		} else if (itemId == R.id.up) {
			if (menuInfo.position > 0) {
                Identity identity = identities.remove(menuInfo.position);
                identities.add(menuInfo.position - 1, identity);
                mIdentitiesChanged = true;
                refreshView();
            }
		} else if (itemId == R.id.down) {
			if (menuInfo.position < identities.size() - 1) {
                Identity identity = identities.remove(menuInfo.position);
                identities.add(menuInfo.position + 1, identity);
                mIdentitiesChanged = true;
                refreshView();
            }
		} else if (itemId == R.id.top) {
			Identity identity = identities.remove(menuInfo.position);
			identities.add(0, identity);
			mIdentitiesChanged = true;
			refreshView();
		} else if (itemId == R.id.remove) {
			if (identities.size() > 1) {
                identities.remove(menuInfo.position);
                mIdentitiesChanged = true;
                refreshView();
            } else {
                Toast.makeText(this, getString(R.string.no_removable_identity),
                               Toast.LENGTH_LONG).show();
            }
		}
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
        //mAccount.refresh(Preferences.getPreferences(getApplication().getApplicationContext()));
        refreshView();
    }


    @Override
    public void onBackPressed() {
        saveIdentities();
        super.onBackPressed();
    }

    private void saveIdentities() {
        if (mIdentitiesChanged) {
            mAccount.setIdentities(identities);
            mAccount.save(Preferences.getPreferences(getApplication().getApplicationContext()));
        }
        finish();
    }
}
