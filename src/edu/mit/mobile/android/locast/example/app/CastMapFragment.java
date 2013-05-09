package edu.mit.mobile.android.locast.example.app;
/*
 * Copyright (C) 2012-2013  MIT Mobile Experience Lab
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.stackoverflow.ArrayUtils;

import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageCache.OnImageLoadListener;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.maps.LocatableMapFragment;

public class CastMapFragment extends LocatableMapFragment {

    public static String[] CAST_PROJECTION = ArrayUtils.concat(LocatableMapFragment.PROJECTION,
			new String[] { Cast.COL_PREVIEW_IMAGE_URL, Cast.COL_PRIVACY, Cast.COL_AUTHOR });

    private static final String TAG = CastMapFragment.class.getSimpleName();

    public static CastMapFragment instantiate(Uri cardDir, boolean showMyLocation) {
        final Bundle args = new Bundle(1);

        args.putParcelable(ARG_LOCATABLE_DIR, cardDir);
        args.putBoolean(ARG_SHOW_MY_LOCATION, showMyLocation);

        final CastMapFragment f = new CastMapFragment();

        f.setArguments(args);

        return f;
    }

    private ImageCache mImageCache;

    private CastInfoWindowAdapter mInfoWindowAdapter;

    private final OnImageLoadListener mOnImageLoadListener = new OnImageLoadListener() {

        @Override
        public void onImageLoaded(int id, Uri imageUri, Drawable image) {
            final Marker m = mPendingLoads.get(id);

            if (m != null) {
                m.showInfoWindow();
            }
        }
    };

    private void clearPendingLoads() {
        mPendingLoads.clear();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageCache = ImageCache.getInstance(getActivity());
    };

    @Override
    public void onPause() {
        super.onPause();

        mImageCache.unregisterOnImageLoadListener(mOnImageLoadListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        mImageCache.registerOnImageLoadListener(mOnImageLoadListener);
    };

    private final SparseArray<Marker> mPendingLoads = new SparseArray<Marker>();

    private Drawable loadMarkerImage(Marker marker, Uri image) {
        final int id = mImageCache.getNewID();

        Drawable imageDrawable = null;
        try {
            imageDrawable = mImageCache.loadImage(id, image, 320, 240);
            if (imageDrawable == null) {
                mPendingLoads.put(id, marker);
            }
        } catch (final IOException e) {
            Log.e(TAG, "error loading image", e);
        }

        return imageDrawable;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mImageCache = ImageCache.getInstance(getActivity());

        final boolean showMyLocation = getArguments().getBoolean(ARG_SHOW_MY_LOCATION, false);
        setShowMyLocation(showMyLocation);

        final GoogleMap map = getMap();

        // bail if there's no map
        if (map == null) {
            return;
        }

        mInfoWindowAdapter = new CastInfoWindowAdapter(getActivity(), this);
		map.setInfoWindowAdapter(mInfoWindowAdapter);

        // the map doesn't automatically snap to our current location, so we need to do that
        // somehow.
        if (showMyLocation) {
            final LocationManager lm = (LocationManager) getActivity().getSystemService(
                    Context.LOCATION_SERVICE);
            final Location myLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            // XXX this returns null
            // final Location myLoc = getMap().getMyLocation();
            if (myLoc != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myLoc.getLatitude(),
                        myLoc.getLongitude()), 16));
            }
        }
    }

    @Override
    protected String[] getProjection() {
        return CAST_PROJECTION;
    }

    public static class CastInfoWindowAdapter implements InfoWindowAdapter {

        private static final String[] PROJECTION_INFO_WINDOW = new String[] {
                Cast.COL_PREVIEW_IMAGE_URL, Cast.COL_PRIVACY, Cast.COL_AUTHOR };
        private final CastMapFragment mCastMapFragment;
        private final View mContent;
        private final ContentResolver mCr;

        public CastInfoWindowAdapter(Context context, CastMapFragment cmf) {
            mCastMapFragment = cmf;
            final LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContent = inflater.inflate(R.layout.cast_map_info_window, null);
            mCr = context.getContentResolver();

        }

        @Override
        public View getInfoContents(Marker marker) {
            final View v = mContent;

            mCastMapFragment.clearPendingLoads();

            final Uri item = mCastMapFragment.getItem(marker);
            final Cursor c = mCr.query(item, PROJECTION_INFO_WINDOW, null, null, null);

            try {
                String thumb;


                if (c.moveToFirst()) {
                    thumb = c.getString(c.getColumnIndex(Cast.COL_PREVIEW_IMAGE_URL));

                    if (thumb != null) {
                        final Drawable d = mCastMapFragment.loadMarkerImage(marker,
                                Uri.parse(thumb));

                        final ImageView imgView = (ImageView) v
                                .findViewById(R.id.cast_media_thumbnail);
                        if (d != null) {
                            imgView.setImageDrawable(d);
                        } else {
                            imgView.setImageResource(R.drawable.ic_placeholder);
                        }
                    }

                    ((TextView) v.findViewById(R.id.author)).setText(c.getString(c
                            .getColumnIndex(Cast.COL_AUTHOR)));
                }
            } finally {
                c.close();
            }

            ((TextView) v.findViewById(R.id.title)).setText(marker.getTitle());

            return v;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

    }
}
