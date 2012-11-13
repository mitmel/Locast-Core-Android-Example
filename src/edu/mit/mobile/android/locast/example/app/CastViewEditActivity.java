package edu.mit.mobile.android.locast.example.app;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.stackoverflow.ArrayUtils;

import edu.mit.mobile.android.locast.data.PrivatelyAuthorable;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.accounts.Authenticator;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.sync.LocastSyncService;

public class CastViewEditActivity extends LocatableItemMapActivity {

    public static final String TAG = CastViewEditActivity.class.getSimpleName();

    private static final String TAG_EDIT = Intent.ACTION_EDIT;
    private static final String TAG_DETAIL = Intent.ACTION_VIEW;

    private boolean mCanEdit;

    private String[] mProjection;

    private static String[] PROJECTION = new String[] { Cast.COL_PRIVACY, Cast.COL_TITLE,
            Cast.COL_AUTHOR_URI, Cast.COL_AUTHOR };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getSupportMenuInflater().inflate(R.menu.activity_cast_view, menu);
        return true;
    }

    @Override
    protected void onCastLoaded(Loader<Cursor> loader, Cursor c) {
        mCanEdit = PrivatelyAuthorable.canEdit(
                Authenticator.getUserUri(this, Authenticator.ACCOUNT_TYPE), c);
        setTitle(Cast.getTitle(this, c));
        getSupportActionBar().setSubtitle(c.getString(c.getColumnIndexOrThrow(Cast.COL_AUTHOR)));
        invalidateOptionsMenu();
    }

    @Override
    protected String[] getProjection() {
        if (mProjection == null) {
            mProjection = ArrayUtils.concat(super.getProjection(), PROJECTION);
        }
        return mProjection;
    }

    private void editCast() {
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        onLoadContentFragment(new Intent(Intent.ACTION_EDIT, getIntent().getData()), ft);
        final ActionMode am = startActionMode(new CastEditActionMode());
        am.setTitle(R.string.edit_cast);
        ft.commit();
    }

    private void onLeaveCastEdit(boolean save) {
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        final CastEditFragment edit = (CastEditFragment) fm.findFragmentByTag(TAG_EDIT);
        if (edit != null) {
            if (save) {
                if (edit.save()) {
                    Toast.makeText(this, R.string.notice_saved, Toast.LENGTH_SHORT).show();
                    LocastSyncService.startSync(this, getIntent().getData(), true);
                } else {
                    Toast.makeText(this, R.string.err_saving_cast, Toast.LENGTH_LONG).show();
                }
            }
        }

        onLoadContentFragment(new Intent(Intent.ACTION_VIEW, getIntent().getData()), ft);
        ft.commit();

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.edit).setVisible(mCanEdit);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit:
                editCast();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showEditFragment(Intent intent, FragmentTransaction ft) {
        ft.replace(R.id.content, CastEditFragment.getInstance(intent.getData()), TAG_EDIT);
    }

    private void showDetailsFragment(Intent intent, FragmentTransaction ft) {
        ft.replace(R.id.content, CastDetailFragment.getInstance(intent.getData()), TAG_DETAIL);
    }

    @Override
    protected boolean onLoadContentFragment(Intent intent, FragmentTransaction ft) {

        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            showDetailsFragment(intent, ft);
            return true;

        } else if (intent.getAction().equals(Intent.ACTION_EDIT)) {
            showEditFragment(intent, ft);
            return true;
        } else {
            return false;
        }
    }

    private final class CastEditActionMode implements ActionMode.Callback {

        private boolean mCanceled = false;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getSupportMenuInflater().inflate(R.menu.activity_cast_edit, menu);
            mCanceled = false;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.d(TAG, "action item clicked: " + mode + "; " + item);
            switch (item.getItemId()) {
                case R.id.cancel:
                    mCanceled = true;
                    mode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            onLeaveCastEdit(!mCanceled);
        }
    }
}
