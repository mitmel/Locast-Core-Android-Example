package edu.mit.mobile.android.locast.example.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.TextView;
import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageLoaderAdapter;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.sync.LocastSyncService;

public class CastDetailFragment extends Fragment implements LoaderCallbacks<Cursor> {

    public static final String ARG_CAST_URI = "cast";
    public static final String ARG_CAST_MEDIA_URI = "cast_media";
    private static final int LOADER_CAST = 100;

    private static final String[] CAST_PROJECTION = new String[] { Cast._ID, Cast.COL_TITLE,
            Cast.COL_DESCRIPTION, Cast.COL_AUTHOR, Cast.COL_AUTHOR_URI, Cast.COL_PRIVACY };
    private static final int LOADER_CAST_MEDIA = 101;

    ImageCache mImageCache;

    public static CastDetailFragment getInstance(Uri cast) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_CAST_URI, cast);

        final CastDetailFragment f = new CastDetailFragment();
        f.setArguments(args);
        return f;
    }

    private Gallery mCastMediaView;
    private CastMediaAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_CAST_URI)) {
            throw new IllegalArgumentException("fragment requires a cast URI as an argument");
        }

        mImageCache = ImageCache.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cast_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCastMediaView = (Gallery) view.findViewById(R.id.cast_media);

        mAdapter = new CastMediaAdapter(getActivity(), null, 0);

        mCastMediaView.setAdapter(new ImageLoaderAdapter(mAdapter, mImageCache,
                CastMediaAdapter.IMAGE_IDS, 300, 300));

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_CAST:
                final Uri cast = args.getParcelable(ARG_CAST_URI);
                return new CursorLoader(getActivity(), cast, CAST_PROJECTION, null, null, null);

            case LOADER_CAST_MEDIA:
                final Uri cast_media = args.getParcelable(ARG_CAST_MEDIA_URI);
                return new CursorLoader(getActivity(), cast_media, CastMediaAdapter.PROJECTION,
                        null,
                        null, null);
            default:
                throw new IllegalArgumentException("unhandled loader ID " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {
            case LOADER_CAST:

                if (c.moveToFirst()) {
                    final View v = getView();

                    ((TextView) v.findViewById(R.id.description)).setText(c.getString(c
                            .getColumnIndexOrThrow(Cast.COL_DESCRIPTION)));

                    // ((TextView)v.findViewById(R.id.description)).setText(c.getString(c.getColumnIndexOrThrow(Cast.COL_DESCRIPTION)));
                }
                break;

            case LOADER_CAST_MEDIA:
                mAdapter.swapCursor(c);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_CAST_MEDIA:
                mAdapter.swapCursor(null);
                break;
        }

    }
}
