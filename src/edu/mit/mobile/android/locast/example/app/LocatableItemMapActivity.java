package edu.mit.mobile.android.locast.example.app;

import java.io.IOException;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageCache.OnImageLoadListener;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.data.interfaces.Locatable;
import edu.mit.mobile.android.locast.nfftt.BuildConfig;
import edu.mit.mobile.android.locast.nfftt.R;
import edu.mit.mobile.android.maps.GoogleStaticMapView;
import edu.mit.mobile.android.maps.OnMapUpdateListener;

public abstract class LocatableItemMapActivity extends SherlockFragmentActivity implements
        OnImageLoadListener, OnMapUpdateListener {

    protected static final String[] PROJECTION = new String[] { Locatable.COL_LATITUDE,
            Locatable.COL_LONGITUDE, JsonSyncableItem.COL_DRAFT };
    private static final int LOCATABLE_LOADER = 200;
    private Bundle mLocatableArgs;

    private GoogleStaticMapView mMap;
    private View mMapFrame;

    private boolean mShowMap = true;

    private ImageCache mImageCache;
    protected boolean mIsDraft;

    public static final String ARGS_LOCATABLE = "locatable";
    private static final String TAG = LocatableItemMapActivity.class.getSimpleName();

    private static final String INSTANCE_SHOW_MAP = "edu.mit.mobile.android.locast.example.app.LocatableItemMapActivity.SHOW_MAP";
    private static final String INSTANCE_LOCATABLE_ARGS = "edu.mit.mobile.android.locast.example.app.LocatableItemMapActivity.LOCATABLE_ARGS";

    @Override
    protected void onCreate(Bundle args) {

        super.onCreate(args);

        setContentView(R.layout.activity_locatable_item_map);

        mMap = (GoogleStaticMapView) findViewById(R.id.map);
        mMapFrame = findViewById(R.id.map_frame);


        mImageCache = ImageCache.getInstance(this);

        LoaderManager.enableDebugLogging(true);

        final Intent intent = getIntent();
        final Uri data = intent.getData();
        final String action = intent.getAction();

        if (args == null) {
            args = Bundle.EMPTY;
        }

        mShowMap = args.getBoolean(INSTANCE_SHOW_MAP, true);

        if (!mShowMap) {
            mMapFrame.setVisibility(View.GONE);
        }

        mLocatableArgs = args.getParcelable(INSTANCE_LOCATABLE_ARGS);
        if (mLocatableArgs == null) {
            mLocatableArgs = new Bundle(1);
        }

        if (Intent.ACTION_INSERT.equals(action)) {
            // we don't have a locatable yet. That's ok.
        } else {
            if (!mLocatableArgs.containsKey(ARGS_LOCATABLE)) {
                mLocatableArgs.putParcelable(ARGS_LOCATABLE, data);
            }
        }

        setDraft(true);

        if (!loadContentFragment(getIntent())) {
            return;
        }
    }

    @Override
    public void setIntent(Intent newIntent) {
        super.setIntent(newIntent);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setIntent(" + newIntent + ")");
        }

        final String type = newIntent.getType();

        if (type != null && type.startsWith(ProviderUtils.TYPE_ITEM_PREFIX)) {
            final Uri locatable = getLocatable();
            final Uri newLocatable = newIntent.getData();

            if (newLocatable != null && !newLocatable.equals(locatable)) {
                setLocatable(newLocatable);
            }
        }
    }

    /**
     * Implement this to handle the loading of the appropriate fragment given the provided intent.
     *
     * @param intent
     *            the intent describing the desired activity state
     * @param fm
     *            the working fragment manager
     * @param ft
     *            make any fragment changes on this transaction. It will be committed for you.
     * @param current
     *            the current fragment being displayed, if there is one.
     * @return
     */
    protected abstract boolean onLoadContentFragment(Intent intent, FragmentManager fm,
            FragmentTransaction ft, Fragment current);

    /**
     * Loads the appropriate fragment based on the given intent. The activity's intent will be set
     * to this intent upon successful fragment loading.
     *
     * @param intent
     * @return true if the fragment was loaded successfully
     */
    protected boolean loadContentFragment(Intent intent) {
        final FragmentManager fm = getSupportFragmentManager();

        final String data = intent.getDataString() + intent.getAction();
        final Fragment f = fm.findFragmentByTag(data);

        if (f == null) {
            final FragmentTransaction ft = fm.beginTransaction();
            if (onLoadContentFragment(intent, fm, ft, fm.findFragmentById(R.id.content))) {
                ft.commit();

            } else {
                Toast.makeText(this, R.string.err_unhandled_intent, Toast.LENGTH_LONG).show();
                finish();
                return false;
            }
        }
        setIntent(intent);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mImageCache.registerOnImageLoadListener(this);

        mMap.setOnMapUpdateListener(this);

        restartLoaders();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(INSTANCE_LOCATABLE_ARGS, mLocatableArgs);
        outState.putBoolean(INSTANCE_SHOW_MAP, mShowMap);
    }

    protected void setShowMap(boolean showMap) {
        if (showMap == mShowMap) {
            return;
        }

        mShowMap = showMap;

        if (showMap) {
            restartLoaders();
        } else {
            resetMap();
        }
    }

    protected void resetMap() {
        mMap.clearMap();
        final int hiddenState = mShowMap ? View.INVISIBLE : View.GONE;
        if (mMapFrame.getVisibility() != hiddenState) {
            mMapFrame.startAnimation(AnimationUtils.loadAnimation(this, R.anim.from_bottom_out));
            mMapFrame.setVisibility(hiddenState);
        }
    }

    protected void restartLoaders() {
        if (getLocatable() != null) {
            getSupportLoaderManager().restartLoader(LOCATABLE_LOADER, mLocatableArgs,
                    mLoaderCallbacks);
        } else {
            resetMap();
        }
    }

    protected Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content);
    }

    public boolean isDraft() {
        return mIsDraft;
    }

    public void setDraft(boolean isDraft) {
        mIsDraft = isDraft;
    }

    @Override
    protected void onPause() {
        super.onPause();

        getSupportLoaderManager().destroyLoader(LOCATABLE_LOADER);

        mMap.setOnMapUpdateListener(null);
        mImageCache.unregisterOnImageLoadListener(this);
    }

    public void setLocatable(Uri locatable) {
        mLocatableArgs.putParcelable(ARGS_LOCATABLE, locatable);
        restartLoaders();
    }

    public Uri getLocatable() {
        final Uri locatable = mLocatableArgs.getParcelable(ARGS_LOCATABLE);
        return locatable;
    }

    protected String[] getProjection() {
        return PROJECTION;
    }

    protected abstract void onCastLoaded(Loader<Cursor> loader, Cursor c);

    private final LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOCATABLE_LOADER:
                    final Uri locatable = args.getParcelable(ARGS_LOCATABLE);
                    if (locatable == null) {
                        throw new IllegalArgumentException("locatable was null");
                    }

                    return new CursorLoader(LocatableItemMapActivity.this, locatable,
                            getProjection(), null, null, null);
                default:
                    throw new IllegalArgumentException("invalid loader id");
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            switch (loader.getId()) {
                case LOCATABLE_LOADER:

                    if (c.moveToFirst()) {
                        final int latCol = c.getColumnIndexOrThrow(Locatable.COL_LATITUDE);
                        if (mShowMap && !c.isNull(latCol)) {
                            final float lat = c.getFloat(latCol);
                            final float lon = c.getFloat(c
                                    .getColumnIndexOrThrow(Locatable.COL_LONGITUDE));

                            mMap.setMap(lat, lon, false);
                        }

                        setDraft(JsonSyncableItem.isDraft(c));

                        onCastLoaded(loader, c);

                    } else {
                        finish();
                        return;
                    }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    @Override
    public void onMapUpdate(GoogleStaticMapView view, Uri mapUrl) {
        Drawable d;
        try {
            d = mImageCache.loadImage(R.id.map, mapUrl, mMap.getMapWidth(), mMap.getMapHeight());
            if (d != null) {
                mMapFrame.setVisibility(View.VISIBLE);
                mMap.setImageDrawable(d);
            }

        } catch (final IOException e) {
            Log.e(TAG, "error loading map", e);
        }
    }

    @Override
    public void onImageLoaded(int id, Uri imageUri, Drawable image) {
        if (R.id.map == id) {
            mMapFrame.setVisibility(View.VISIBLE);
            mMapFrame.startAnimation(AnimationUtils.loadAnimation(this, R.anim.from_bottom_in));
            mMap.setImageDrawable(image);
        }
    }
}
