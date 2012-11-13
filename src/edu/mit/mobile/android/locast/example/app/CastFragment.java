package edu.mit.mobile.android.locast.example.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageLoaderAdapter;
import edu.mit.mobile.android.locast.Constants;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.sync.LocastSyncService;

public abstract class CastFragment extends Fragment implements LoaderCallbacks<Cursor> {

    public static final String ARG_CAST_URI = "cast";
    public static final String ARG_CAST_MEDIA_URI = "cast_media";
    private static final int LOADER_CAST = 1000;

    private static final int LOADER_CAST_MEDIA = 1001;
    private static final String TAG = CastFragment.class.getSimpleName();

    private ImageCache mImageCache;

    private Gallery mCastMediaView;
    private CastMediaAdapter mCastMediaAdapter;

    protected abstract String[] getCastProjection();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_CAST_URI)) {
            throw new IllegalArgumentException("fragment requires a cast URI as an argument");
        }

        mImageCache = ImageCache.getInstance(getActivity());
    }

    /**
     * Implement this to get the layout. Make sure to include a gallery view with R.id.cast_media
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    protected abstract View onCreateCastFragmentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return onCreateCastFragmentView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCastMediaView = (Gallery) view.findViewById(R.id.cast_media);

        mCastMediaAdapter = new CastMediaAdapter(getActivity(), null, 0);

        mCastMediaView.setAdapter(new ImageLoaderAdapter(mCastMediaAdapter, mImageCache,
                CastMediaAdapter.IMAGE_IDS, 300, 300));
    }

    @Override
    public void onResume() {
        super.onResume();

        final Bundle loaderArgs = new Bundle(getArguments());
        final Uri castUri = loaderArgs.getParcelable(ARG_CAST_URI);

        final Uri castMedia = Cast.CAST_MEDIA.getUri(castUri);
        loaderArgs.putParcelable(ARG_CAST_MEDIA_URI, castMedia);

        getLoaderManager().restartLoader(LOADER_CAST, loaderArgs, this);

        getLoaderManager().restartLoader(LOADER_CAST_MEDIA, loaderArgs, this);

        LocastSyncService.startExpeditedAutomaticSync(getActivity(), castUri);
        LocastSyncService.startExpeditedAutomaticSync(getActivity(), castMedia);
    }

    protected abstract void loadCastFromCursor(Loader<Cursor> loader, Cursor c);

    public CastMediaAdapter getCastMediaAdapter() {
        return mCastMediaAdapter;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_CAST:
                final Uri cast = args.getParcelable(ARG_CAST_URI);
                return new CursorLoader(getActivity(), cast, getCastProjection(), null, null, null);

            case LOADER_CAST_MEDIA:
                final Uri cast_media = args.getParcelable(ARG_CAST_MEDIA_URI);
                return new CursorLoader(getActivity(), cast_media, CastMediaAdapter.PROJECTION,
                        null, null, null);
            default:
                throw new IllegalArgumentException("unhandled loader ID " + id);
        }
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {
            case LOADER_CAST:

                if (c.moveToFirst()) {
                    if (Constants.DEBUG) {
                        ProviderUtils.dumpCursorToLog(c, getCastProjection());
                    }

                    loadCastFromCursor(loader, c);
                } else {
                    Log.e(TAG, "could not load content");
                }
                break;

            case LOADER_CAST_MEDIA:
                if (Constants.DEBUG) {
                    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                        ProviderUtils.dumpCursorToLog(c, CastMediaAdapter.PROJECTION);
                    }
                }

                mCastMediaAdapter.swapCursor(c);

                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_CAST_MEDIA:
                mCastMediaAdapter.swapCursor(null);
                break;
        }
    }
}
