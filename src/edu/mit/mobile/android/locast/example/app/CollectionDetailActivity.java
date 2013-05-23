package edu.mit.mobile.android.locast.example.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import edu.mit.mobile.android.locast.nfftt.R;
import edu.mit.mobile.android.locast.example.data.Collection;

public class CollectionDetailActivity extends SherlockFragmentActivity {

    protected static final String[] PROJECTION_COLLECTION = new String[] { Collection._ID,
            Collection.COL_TITLE, Collection.COL_AUTHOR, Collection.COL_DESCRIPTION };

    protected static final String ARG_COLLECTION = "collection";

    private static final int LOADER_COLLECTION = 100;

    private Uri mCollection;
    private Uri mCollectionCasts;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setContentView(R.layout.activity_collection_detail);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            mCollection = getIntent().getData();

            final FragmentManager fm = getSupportFragmentManager();

            final Fragment castList = fm.findFragmentById(R.id.casts);

            final FragmentTransaction ft = fm.beginTransaction();

            mCollectionCasts = Collection.CASTS.getUri(mCollection);
            if (castList != null) {
                ft.attach(castList);
            } else {
                ft.add(R.id.casts, CastListFragment.instantiate(mCollectionCasts));
            }

            ft.commit();
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Bundle args = new Bundle(1);
        args.putParcelable(ARG_COLLECTION, mCollection);
        getSupportLoaderManager().restartLoader(LOADER_COLLECTION, args, mCollectionLoader);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.activity_collection_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.cast_new) {
            createNewCast();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void onCollectionLoaded(Loader<Cursor> loader, Cursor c) {
        setTitle(c.getString(c.getColumnIndexOrThrow(Collection.COL_TITLE)));

        getSupportActionBar().setSubtitle(
                c.getString(c.getColumnIndexOrThrow(Collection.COL_DESCRIPTION)));
    }

    private final LoaderCallbacks<Cursor> mCollectionLoader = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int arg0, Bundle args) {
            final Uri collection = args.getParcelable(ARG_COLLECTION);
            return new CursorLoader(CollectionDetailActivity.this, collection,
                    PROJECTION_COLLECTION, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            if (c.moveToFirst()) {
                onCollectionLoaded(loader, c);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> arg0) {

        }
    };

    private void createNewCast() {
        startActivity(new Intent(Intent.ACTION_INSERT, mCollectionCasts));
    }

}
