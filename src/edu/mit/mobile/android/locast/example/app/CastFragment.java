package edu.mit.mobile.android.locast.example.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;
import android.widget.ProgressBar;
import android.widget.Toast;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageLoaderAdapter;
import edu.mit.mobile.android.locast.Constants;
import edu.mit.mobile.android.locast.app.DeleteDialogFragment;
import edu.mit.mobile.android.locast.app.DeleteDialogFragment.OnDeleteListener;
import edu.mit.mobile.android.locast.data.Authorable;
import edu.mit.mobile.android.locast.data.CastMedia;
import edu.mit.mobile.android.locast.data.tags.Taggable;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.accounts.Authenticator;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.sync.LocastSyncService;
import edu.mit.mobile.android.locast.widget.TagButton;
import edu.mit.mobile.android.locast.widget.TagListView;
import edu.mit.mobile.android.locast.widget.TagsLoaderCallbacks;

public abstract class CastFragment extends Fragment implements LoaderCallbacks<Cursor> {

    public static final String ARG_INTENT_ACTION = "action";
    public static final String ARG_CAST_URI = "cast";
    public static final String ARG_CAST_DIR_URI = "cast_dir";
    public static final String ARG_CAST_MEDIA_URI = "cast_media";
    private static final int LOADER_CAST = 1000;

    private static final int LOADER_CAST_MEDIA = 1001;
    private static final String TAG = CastFragment.class.getSimpleName();
    private static final int LOADER_TAGS = 2000;

    private ImageCache mImageCache;

    private Gallery mCastMediaView;
    private CastMediaAdapter mCastMediaAdapter;
    private Uri mCastMedia;
    private Uri mCast;
    private OnDeleteListener mOnDeleteListener;

    // tags
    private TagListView mTags;
    private TagsLoaderCallbacks mTagsLoader;
    private final OnClickListener mOnTagClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            final String tag = ((TagButton) v).getText().toString();

            startActivity(new Intent(Intent.ACTION_VIEW, Taggable.getItemMatchingTags(
                    Cast.CONTENT_URI, tag)));
        }
    };

    protected abstract String[] getCastProjection();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args == null
                || (!args.containsKey(ARG_CAST_URI) && !args.containsKey(ARG_CAST_DIR_URI))) {
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
        mCastMediaView.setOnItemClickListener(mOnItemClickListener);

        mCastMediaAdapter = new CastMediaAdapter(getActivity(), null, 0);

        mCastMediaView.setEmptyView(view.findViewById(android.R.id.empty));

        mCastMediaView.setAdapter(new ImageLoaderAdapter(mCastMediaAdapter, mImageCache,
                CastMediaAdapter.IMAGE_IDS, 300, 300));

        registerForContextMenu(mCastMediaView);

        mTags = (TagListView) view.findViewById(R.id.tags);

        mTags.setOnTagClickListener(mOnTagClickListener);

        mTagsLoader = new TagsLoaderCallbacks(getActivity(), mTags);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mTagsLoader = null;
    }

    protected void restartLoaders() {
        final Bundle loaderArgs = new Bundle(getArguments());
        final Uri castUri = loaderArgs.getParcelable(ARG_CAST_URI);

        if (castUri != null) {

            final Uri castMedia = Cast.CAST_MEDIA.getUri(castUri);
            loaderArgs.putParcelable(ARG_CAST_MEDIA_URI, castMedia);

            getLoaderManager().restartLoader(LOADER_CAST, loaderArgs, this);

            getLoaderManager().restartLoader(LOADER_CAST_MEDIA, loaderArgs, this);

            final Bundle tagsArgs = new Bundle(1);
            tagsArgs.putParcelable(TagsLoaderCallbacks.ARGS_URI, Cast.TAGS.getUri(castUri));

            getLoaderManager().restartLoader(LOADER_TAGS, tagsArgs, mTagsLoader);

            LocastSyncService.startExpeditedAutomaticSync(getActivity(), castUri);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        restartLoaders();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        switch (v.getId()) {
            case R.id.cast_media: {
                getActivity().getMenuInflater().inflate(R.menu.context_cast_media, menu);
                final Cursor c = mCastMediaAdapter.getCursor();
                if (c == null) {
                    return;
                }
                AdapterView.AdapterContextMenuInfo info;
                try {
                    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                } catch (final ClassCastException e) {
                    Log.e(TAG, "bad menuInfo", e);
                    return;
                }

                c.moveToPosition(info.position);

                final String myUserUri = Authenticator.getUserUri(getActivity(),
                        Authenticator.ACCOUNT_TYPE);

                final boolean isEditable = Authorable.canEdit(myUserUri, c);

                menu.findItem(R.id.delete).setVisible(isEditable);

            }
                break;
            default:
                super.onCreateContextMenu(menu, v, menuInfo);
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (final ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        final Uri itemUri = ContentUris.withAppendedId(mCastMedia, info.id);

        switch (item.getItemId()) {
            case R.id.delete:
                showDeleteDialog(itemUri);
                return true;

            case R.id.view: {
                final Cursor c = mCastMediaAdapter.getCursor();
                c.moveToPosition(info.position);
                final Intent i = CastMedia.showMedia(getActivity(), c, mCastMedia);
                if (i != null) {
                    startActivity(i);
                }
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showDeleteDialog(Uri item) {
        final DeleteDialogFragment del = DeleteDialogFragment.newInstance(item, getActivity()
                .getString(R.string.delete_cast_media),
                getString(R.string.delete_cast_media_confirm_message));
        del.registerOnDeleteListener(mOnDeleteListener);
        del.show(getFragmentManager(), "delete-item-dialog");
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
                mCast = cast;
                return new CursorLoader(getActivity(), cast, getCastProjection(), null, null, null);

            case LOADER_CAST_MEDIA:
                final Uri cast_media = args.getParcelable(ARG_CAST_MEDIA_URI);
                mCastMedia = cast_media;
                return new CursorLoader(getActivity(), cast_media, CastMediaAdapter.PROJECTION,
                        null, null, null);
            default:
                throw new IllegalArgumentException("unhandled loader ID " + id);
        }
    }

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            new ResolveIntentTask(getActivity(), mCastMedia,
                    (ProgressBar) view.findViewById(R.id.progress))
                    .execute((Cursor) mCastMediaAdapter.getItem(position));
        }
    };

    /**
     * Makes a few attempts to resolve the media and runs startActivity if it finds something.
     *
     */
    private static class ResolveIntentTask extends AsyncTask<Cursor, Void, Intent> {

        private Activity mContext;
        private final Uri mCastMediaDir;
        private ProgressBar mProgress;

        public ResolveIntentTask(Activity context, Uri castMediaDir, ProgressBar progress) {
            mCastMediaDir = castMediaDir;
            mContext = context;
            mProgress = progress;
        }

        @Override
        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected Intent doInBackground(Cursor... params) {
            final Intent showMedia = CastMedia.showMedia(mContext, params[0], mCastMediaDir);
            return showMedia;
        }

        @Override
        protected void onPostExecute(Intent result) {
            mProgress.setVisibility(View.GONE);
            mProgress = null;
            try {
                if (result == null) {
                    throw new ActivityNotFoundException();
                }
                mContext.startActivity(result);
            } catch (final ActivityNotFoundException e) {
                Toast.makeText(mContext, R.string.err_cast_media_no_activities, Toast.LENGTH_LONG)
                        .show();
            }
            mContext = null;
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
