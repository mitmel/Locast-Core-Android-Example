package edu.mit.mobile.android.locast.example.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
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

    private CastMediaHelper mCastMediaHelper;

    private CastEditActionMode mCastEditActionMode;

    private static String[] PROJECTION = new String[] { Cast.COL_PRIVACY, Cast.COL_TITLE,
            Cast.COL_AUTHOR_URI, Cast.COL_AUTHOR };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getSupportMenuInflater().inflate(R.menu.activity_cast_view, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle args) {
        super.onCreate(args);



        mCastMediaHelper = new CastMediaHelper(this, args);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mCastMediaHelper.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCastMediaHelper.onActivityResult(getCast(), requestCode, resultCode, data);
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
        loadContentFragment(new Intent(Intent.ACTION_EDIT, getIntent().getData()));
    }

    /**
     * @return the working cast, or null if it's currently being created.
     */
    private Uri getCast() {
        Uri cast = null;
        // only set the cast when it's actually a cast item.
        if (Cast.TYPE_ITEM.equals(getIntent().resolveType(getContentResolver()))) {
            cast = getIntent().getData();
        }

        return cast;
    }

    private void onLeaveCastEdit(boolean save) {
        mCastEditActionMode = null;

        if (save && !save()) {
            Log.e(TAG, "Error saving cast");
            Toast.makeText(this, R.string.err_saving_cast, Toast.LENGTH_LONG).show();
            return;
        }

        if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
            if (save) {
                final Uri cast = getCast();

                loadContentFragment(new Intent(Intent.ACTION_VIEW, cast));

            } else {
                setResult(RESULT_CANCELED);
                finish();
            }

        } else {
            if (save) {
                Toast.makeText(this, R.string.notice_saved, Toast.LENGTH_SHORT).show();
                LocastSyncService.startSync(this, getIntent().getData(), true);

            }
            loadContentFragment(new Intent(Intent.ACTION_VIEW, getIntent().getData()));
        }
    }

    /**
     * @return true if the cast was saved. False if there was an error.
     */
    private boolean saveIfMissing() {
        if (getCast() == null) {
            return save();
        }
        return true;
    }

    /**
     * If an edit fragment is showing, saves the content either creating a new cast or updating the
     * existing one. This updates this activity's intent's from an INSERT to an EDIT if it was just
     * created.
     *
     * @return
     */
    private boolean save() {
        final FragmentManager fm = getSupportFragmentManager();

        final CastEditFragment edit = (CastEditFragment) fm.findFragmentByTag(TAG_EDIT);
        if (edit == null) {
            Log.e(TAG, "save() was called, but no edit fragments were found");
            return false;
        }

        final Uri cast = getCast();

        if (!edit.save()) {
            return false;
        }

        final Uri newCast = edit.getCast();

        if (!newCast.equals(cast)) {
            setIntent(new Intent(Intent.ACTION_EDIT, newCast));
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.edit).setVisible(mCanEdit);
        menu.findItem(R.id.new_photo).setVisible(mCanEdit);
        menu.findItem(R.id.new_video).setVisible(mCanEdit);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        switch (id) {
            case R.id.edit:
                editCast();
                return true;

            default:

                if (onButtonPressed(id)) {
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
        }
    }

    @Override
    protected boolean onLoadContentFragment(Intent intent, FragmentManager fm,
            FragmentTransaction ft, Fragment current) {
        final String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            if (current == null || !(current instanceof CastDetailFragment)) {
                ft.replace(R.id.content, CastDetailFragment.getInstance(intent.getData()),
                        TAG_DETAIL);
            }
            setShowMap(true);
            return true;

        } else if (Intent.ACTION_EDIT.equals(action)) {
            if (current == null || !(current instanceof CastEditFragment)) {
                ft.replace(R.id.content, CastEditFragment.getInstance(action, intent.getData()),
                        TAG_EDIT);
            }
            final CastEditActionMode amc = new CastEditActionMode();
            amc.setDraft(isDraft());
            mCastEditActionMode = amc;
            final ActionMode am = startActionMode(amc);
            am.setTitle(R.string.edit_cast);

            setShowMap(false);
            return true;

        } else if (Intent.ACTION_INSERT.equals(action)) {
            if (current == null || !(current instanceof CastEditFragment)) {
                ft.replace(R.id.content, CastEditFragment.getInstance(action, intent.getData()),
                        TAG_EDIT);
            }
            final CastEditActionMode amc = new CastEditActionMode();
            amc.setDraft(isDraft());
            mCastEditActionMode = amc;
            final ActionMode am = startActionMode(amc);
            am.setTitle(R.string.edit_cast);
            setShowMap(false);
            return true;

        } else {

            return false;
        }
    }

    private boolean onButtonPressed(int id) {
        switch (id) {
            case R.id.new_photo:
                takePicture();
                return true;

            case R.id.new_video:
                takeVideo();
                return true;

            default:
                return false;
        }
    }

    private void takePicture() {
        saveIfMissing();
        mCastMediaHelper.takePicture();
    }

    private void takeVideo() {
        saveIfMissing();
        mCastMediaHelper.takeVideo();
    }

    @Override
    public void setDraft(boolean draft) {
        super.setDraft(draft);
        if (mCastEditActionMode != null) {
            mCastEditActionMode.setDraft(draft);
        }
    }

    private boolean publish() {
        final Fragment f = getCurrentFragment();
        if (!(f instanceof CastEditFragment)) {
            Log.e(TAG, "publish() called, but not showing an edit fragment");
            return false;
        }

        final boolean valid = ((CastEditFragment) f).validateAndClearDraft();

        return valid;
    }

    private final class CastEditActionMode implements ActionMode.Callback {

        private View mPublish;

        private boolean mCanceled = false;

        private final OnClickListener mOnClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (publish()) {
                    mCanceled = false;
                    mMode.finish();
                }
            }
        };

        private boolean mIsDraft = true;

        private ActionMode mMode;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getSupportMenuInflater().inflate(R.menu.activity_cast_edit, menu);

            final View v = getLayoutInflater().inflate(R.layout.action_mode_cast_edit, null);
            mode.setCustomView(v);
            mPublish = v.findViewById(R.id.publish);
            mPublish.setOnClickListener(mOnClickListener);
            mMode = mode;

            mCanceled = false;
            return true;
        }

        public void setDraft(boolean isDraft) {
            if (mIsDraft != isDraft) {
                mIsDraft = isDraft;
                if (mMode != null) {
                    mMode.invalidate();
                }
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mPublish.setVisibility(mIsDraft ? View.VISIBLE : View.GONE);


            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.d(TAG, "action item clicked: " + mode + "; " + item);
            final int id = item.getItemId();
            switch (id) {
                case R.id.cancel:
                    mCanceled = true;
                    mode.finish();
                    return true;
                default:
                    return onButtonPressed(id);

            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mMode = null;
            onLeaveCastEdit(!mCanceled);
        }
    }
}
