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
import edu.mit.mobile.android.locast.data.Locatable;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.maps.GoogleStaticMapView;
import edu.mit.mobile.android.maps.OnMapUpdateListener;

public abstract class LocatableItemMapActivity extends SherlockFragmentActivity implements
        OnImageLoadListener, OnMapUpdateListener {

    protected static final String[] PROJECTION = new String[] { Locatable.Columns.COL_LATITUDE,
            Locatable.Columns.COL_LONGITUDE };
    private static final int LOCATABLE_LOADER = 200;
    private Bundle mLocatableArgs;

    private GoogleStaticMapView mMap;
    private View mMapFrame;

    private ImageCache mImageCache;

    public static final String ARGS_LOCATABLE = "locatable";
    private static final String TAG = LocatableItemMapActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle arg0) {

        super.onCreate(arg0);

        setContentView(R.layout.activity_locatable_item_map);

        final String action = getIntent().getAction();

        LoaderManager.enableDebugLogging(true);

        final Uri data = getIntent().getData();

        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) {
            mLocatableArgs = new Bundle();
            mLocatableArgs.putParcelable(ARGS_LOCATABLE, data);
            loadContentFragment(getIntent());
        } else {
            Toast.makeText(this, R.string.err_unhandled_intent, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mMap = (GoogleStaticMapView) findViewById(R.id.map);
        mMapFrame = findViewById(R.id.map_frame);
        mMapFrame.setVisibility(View.INVISIBLE);

        mImageCache = ImageCache.getInstance(this);
    }

    protected abstract boolean onLoadContentFragment(Intent intent, FragmentTransaction ft);

    private void loadContentFragment(Intent intent) {
        final FragmentManager fm = getSupportFragmentManager();

        final String data = intent.getDataString() + intent.getAction();
        final Fragment f = fm.findFragmentByTag(data);

        if (f == null) {
            final FragmentTransaction ft = fm.beginTransaction();
            if (onLoadContentFragment(intent, ft)) {
                ft.commit();
            } else {
                Toast.makeText(this, R.string.err_unhandled_intent, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mImageCache.registerOnImageLoadListener(this);
        mMap.setOnMapUpdateListener(this);

        getSupportLoaderManager().initLoader(LOCATABLE_LOADER, mLocatableArgs, mLoaderCallbacks);

        // File f;
        // try {
        // f = new File("/sdcard/loadermanager.dump");
        // final FileOutputStream fos = new FileOutputStream(f);
        // final PrintWriter writer = new PrintWriter(fos);
        // getSupportLoaderManager().dump(TAG, fos.getFD(), writer, new String[] {});
        // Log.d(TAG, "dumped to " + f.getAbsolutePath());
        // writer.close();
        // fos.close();
        // } catch (final IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

    }

    @Override
    protected void onPause() {
        super.onPause();


        mMap.setOnMapUpdateListener(null);
        mImageCache.unregisterOnImageLoadListener(this);
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
                        final float lat = c.getFloat(c
                                .getColumnIndexOrThrow(Locatable.Columns.COL_LATITUDE));
                        final float lon = c.getFloat(c
                                .getColumnIndexOrThrow(Locatable.Columns.COL_LONGITUDE));

                        mMap.setMap(lat, lon, false);

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
            mMapFrame.startAnimation(AnimationUtils.makeInChildBottomAnimation(this));
            mMap.setImageDrawable(image);
        }
    }

}
