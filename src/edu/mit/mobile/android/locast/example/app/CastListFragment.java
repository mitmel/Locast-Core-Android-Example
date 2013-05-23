package edu.mit.mobile.android.locast.example.app;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.stackoverflow.ArrayUtils;

import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageLoaderAdapter;
import edu.mit.mobile.android.imagecache.SimpleThumbnailCursorAdapter;
import edu.mit.mobile.android.locast.nfftt.R;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.sync.LocastSyncService;

public class CastListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        OnItemClickListener, OnRefreshListener {

    // public interface

    public static final String ARG_CAST_DIR_URI = "uri";

    // private

    // columns to pull data from...
    private static final String[] FROM = { Cast.COL_TITLE, Cast.COL_PREVIEW_IMAGE_URL };

    // ... mapping to views
    private static final int[] TO = { R.id.title, R.id.thumbnail };

    // ... and a list of the views that should be processed by the ImageCache
    private static final int[] IMAGE_IDS = new int[] { R.id.thumbnail };

    // add in the _ID when querying
    private static final String[] PROJECTION = ArrayUtils.concat(new String[] { Cast._ID,
            Cast.COL_DRAFT }, FROM);

    private Uri mCasts;

    private ImageCache mImageCache;

    private SimpleThumbnailCursorAdapter mAdapter;

    // methods

    /**
     * Create a new CastListFragment displaying the given casts
     *
     * @param castDir
     * @return
     */
    public static CastListFragment instantiate(Uri castDir) {
        final Bundle b = new Bundle();
        b.putParcelable(ARG_CAST_DIR_URI, castDir);
        final CastListFragment f = new CastListFragment();
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if (args != null) {
            final Uri newUri = args.getParcelable(ARG_CAST_DIR_URI);

            // only set mCasts if there's a new value from newUri
            mCasts = newUri != null ? newUri : mCasts;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.cast_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        getListView().addFooterView(
                getLayoutInflater(savedInstanceState).inflate(R.layout.list_footer, null), null,
                false);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        mImageCache = ImageCache.getInstance(getActivity());

        mAdapter = new SimpleThumbnailCursorAdapter(getActivity(), R.layout.cast_list_item, null,
                FROM, TO, IMAGE_IDS, 0) {
            @Override
            public void bindView(View v, Context context, Cursor c) {
                super.bindView(v, context, c);
                ((TextView) v.findViewById(R.id.title)).setText(Cast.getTitle(context, c));
            }
        };

        // the numerical values below should match the layout default.
        setListAdapter(new ImageLoaderAdapter(getActivity(), mAdapter, mImageCache, IMAGE_IDS, 133,
                100, ImageLoaderAdapter.UNIT_DIP));

        getListView().setOnItemClickListener(this);

        getLoaderManager().initLoader(0, null, this);
        LocastSyncService.startExpeditedAutomaticSync(getActivity(), mCasts.buildUpon().query(null)
                .build());
        registerForContextMenu(getListView());

        // mDensity = getActivity().getResources().getDisplayMetrics().density;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), mCasts, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        mAdapter.swapCursor(c);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Uri item = ContentUris.withAppendedId(mCasts, id).buildUpon().query(null).build();
        final Cursor c = mAdapter.getCursor();
        c.moveToPosition(position);
        if (Cast.isDraft(c)) {
            startActivity(new Intent(Intent.ACTION_EDIT, item));
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, item));
        }
    }

    @Override
    public void onRefresh() {
        LocastSyncService.startSync(getActivity(), mCasts, true);

    }
}
