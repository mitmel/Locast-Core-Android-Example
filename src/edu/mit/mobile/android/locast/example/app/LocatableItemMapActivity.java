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

import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageCache.OnImageLoadListener;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.data.Locatable;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.maps.GoogleStaticMapView;
import edu.mit.mobile.android.maps.OnMapUpdateListener;

public abstract class LocatableItemMapActivity extends SherlockFragmentActivity implements
        OnImageLoadListener, OnMapUpdateListener {

    protected static final String[] PROJECTION = new String[] { Locatable.Columns.COL_LATITUDE,
            Locatable.Columns.COL_LONGITUDE, JsonSyncableItem.COL_DRAFT };
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

    @Override
    protected void onCreate(Bundle args) {

        super.onCreate(args);

        setContentView(R.layout.activity_locatable_item_map);

        mMap = (GoogleStaticMapView) findViewById(R.id.map);
        mMapFrame = findViewById(R.id.map_frame);
        mMapFrame.setVisibility(View.INVISIBLE);

        mImageCache = ImageCache.getInstance(this);

        LoaderManager.enableDebugLogging(true);

        final Uri data = getIntent().getData();
        final String action = getIntent().getAction();

        if (args == null) {
            args = Bundle.EMPTY;
        }

        mShowMap = args.getBoolean(INSTANCE_SHOW_MAP, true);

        mLocatableArgs = new Bundle(1);

        if (Intent.ACTION_INSERT.equals(action)) {
            // we don't have a locatable yet. That's ok.
        } else {
            mLocatableArgs.putParcelable(ARGS_LOCATABLE, data);
        }

        if (!loadContentFragment(getIntent())) {
            return;
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

        outState.putBoolean(INSTANCE_SHOW_MAP, mShowMap);
    }

    protected void setShowMap(boolean showMap) {
        if (showMap == mShowMap) {
            return;
        }

        if (showMap) {
            restartLoaders();
        } else {
            resetMap();
        }

        mShowMap = showMap;
    }

    protected void resetMap() {
        mMap.clearMap();
        if (mMapFrame.getVisibility() != View.INVISIBLE) {
            mMapFrame.startAnimation(AnimationUtils.loadAnimation(this, R.anim.from_bottom_out));
            mMapFrame.setVisibility(View.INVISIBLE);
        }
    }

    protected void restartLoaders() {
        if (mLocatableArgs.containsKey(ARGS_LOCATABLE)) {
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

    public void setIsDraft(boolean isDraft) {
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
                        final int latCol = c.getColumnIndexOrThrow(Locatable.Columns.COL_LATITUDE);
                        if (mShowMap && !c.isNull(latCol)) {
                            final float lat = c.getFloat(latCol);
                            final float lon = c.getFloat(c
                                    .getColumnIndexOrThrow(Locatable.Columns.COL_LONGITUDE));

                            mMap.setMap(lat, lon, false);
                        }

                        setIsDraft(JsonSyncableItem.isDraft(c));

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
    public void onImageLoaded(long id, Uri imageUri, Drawable image) {
        if (R.id.map == id) {
            mMapFrame.setVisibility(View.VISIBLE);
            mMapFrame.startAnimation(AnimationUtils.loadAnimation(this, R.anim.from_bottom_in));
            mMap.setImageDrawable(image);
        }
    }
}
