package edu.mit.mobile.android.locast.example.app;

import java.io.IOException;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.Toast;
import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageCache.OnImageLoadListener;
import edu.mit.mobile.android.locast.data.Locatable;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.maps.GoogleStaticMapView;
import edu.mit.mobile.android.maps.OnMapUpdateListener;

public class LocatableItemMapActivity extends FragmentActivity implements LoaderCallbacks<Cursor>,
        OnImageLoadListener, OnMapUpdateListener {

    private static final String[] PROJECTION = new String[] { Locatable.Columns.COL_LATITUDE,
            Locatable.Columns.COL_LONGITUDE };
    private static final int LOCATABLE_LOADER = 100;
    private Bundle mLocatableArgs;

    private GoogleStaticMapView mMap;

    private ImageCache mImageCache;

    public static final String ARGS_LOCATABLE = "locatable";
    private static final String TAG = LocatableItemMapActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle arg0) {

        super.onCreate(arg0);

        setContentView(R.layout.activity_locatable_item_map);

        final String action = getIntent().getAction();

        final Uri data = getIntent().getData();

        if (Intent.ACTION_VIEW.equals(action)) {
            mLocatableArgs = new Bundle();
            mLocatableArgs.putParcelable(ARGS_LOCATABLE, data);
            loadContentFragment(getIntent());
        } else {
            Toast.makeText(this, "cannot handle intent", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mMap = (GoogleStaticMapView) findViewById(R.id.map);
        mImageCache = ImageCache.getInstance(this);
    }

    private void loadContentFragment(Intent intent) {
        final String type = intent.resolveType(getContentResolver());
        if (Cast.TYPE_ITEM.equals(type)) {
            final FragmentManager fm = getSupportFragmentManager();

            final String data = intent.getDataString();
            final Fragment f = fm.findFragmentByTag(data);
            if (f == null) {
                final FragmentTransaction ft = fm.beginTransaction();
                final CastDetailFragment cf = CastDetailFragment.getInstance(intent.getData());
                ft.replace(R.id.content, cf, data);
                ft.commit();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getSupportLoaderManager().restartLoader(LOCATABLE_LOADER, mLocatableArgs, this);

        mImageCache.registerOnImageLoadListener(this);
        mMap.setOnMapUpdateListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMap.setOnMapUpdateListener(null);
        mImageCache.unregisterOnImageLoadListener(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri locatable = args.getParcelable(ARGS_LOCATABLE);
        return new CursorLoader(this, locatable, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        if (c.moveToFirst()) {
            final float lat = c.getFloat(c.getColumnIndexOrThrow(Locatable.Columns.COL_LATITUDE));
            final float lon = c.getFloat(c.getColumnIndexOrThrow(Locatable.Columns.COL_LONGITUDE));

            mMap.setMap(lat, lon, false);

        } else {
            finish();
            return;
        }
    }

    @Override
    public void onMapUpdate(GoogleStaticMapView view, Uri mapUrl) {
        Drawable d;
        try {
            d = mImageCache.loadImage(R.id.map, mapUrl, mMap.getMapWidth(), mMap.getMapHeight());
            if (d != null) {
                mMap.setImageDrawable(d);
            }

        } catch (final IOException e) {
            Log.e(TAG, "error loading map", e);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onImageLoaded(long id, Uri imageUri, Drawable image) {
        if (R.id.map == id) {
            mMap.setImageDrawable(image);
        }
    }


}
