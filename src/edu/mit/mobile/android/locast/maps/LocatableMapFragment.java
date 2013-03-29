package edu.mit.mobile.android.locast.maps;
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

import java.util.HashMap;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.locast.data.interfaces.Locatable;
import edu.mit.mobile.android.locast.data.interfaces.Titled;

public class LocatableMapFragment extends SupportMapFragment implements OnMarkerClickListener,
        OnInfoWindowClickListener {

    /**
     * Pass this a {@link Uri} of the dir of some items that are {@link Locatable} and
     * {@link Titled}.
     */
    public static final String ARG_LOCATABLE_DIR = "edu.mit.mobile.android.locast.maps.ARG_LOCATABLE_DIR";
    protected static final String[] PROJECTION = new String[] { ContentItem._ID,
            Locatable.COL_LATITUDE, Locatable.COL_LONGITUDE, Titled.COL_TITLE,
            Titled.COL_DESCRIPTION };
    private static final int LOCATABLE_LOADER = 100;

    public static final LocatableMapFragment instantiate(Uri locatableDir) {

        final LocatableMapFragment f = new LocatableMapFragment();

        final Bundle args = new Bundle(1);

        args.putParcelable(ARG_LOCATABLE_DIR, locatableDir);

        f.setArguments(args);

        return f;
    }

    private Uri mLocatable;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final GoogleMap gm = getMap();

        if (gm != null) {
            gm.setOnInfoWindowClickListener(this);
        }

        mLocatable = getArguments().getParcelable(ARG_LOCATABLE_DIR);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    public void setShowMyLocation(boolean enabled) {
        final GoogleMap m = getMap();
        if (m != null) {
            m.setMyLocationEnabled(enabled);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getMap() != null) {
            getLoaderManager().restartLoader(LOCATABLE_LOADER, getArguments(), mLocatableLoader);
        }
    }

    protected String[] getProjection() {
        return PROJECTION;
    }

    protected MarkerOptions getMarker(Cursor c) {
        final MarkerOptions mo = new MarkerOptions();
        mo.position(new LatLng(c.getDouble(c.getColumnIndex(Locatable.COL_LATITUDE)), c.getDouble(c
                .getColumnIndex(Locatable.COL_LONGITUDE))));
        mo.title(c.getString(c.getColumnIndex(Titled.COL_TITLE)));
        mo.snippet(c.getString(c.getColumnIndex(Titled.COL_DESCRIPTION)));

        return mo;
    }

    public Uri getItem(Marker marker) {
        final Long id = mMarkerMapping.get(marker);
        if (id != null) {
            return ContentUris.withAppendedId(mLocatable, id);
        }
        return null;
    }

    private final HashMap<Marker, Long> mMarkerMapping = new HashMap<Marker, Long>();

    private final LoaderCallbacks<Cursor> mLocatableLoader = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final Uri uri = args.getParcelable(ARG_LOCATABLE_DIR);
            return new CursorLoader(getActivity(), uri, getProjection(), null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            final GoogleMap gm = getMap();

            gm.clear();

            mMarkerMapping.clear();

            final int idCol = c.getColumnIndexOrThrow(ContentItem._ID);

            while (c.moveToNext()) {
                final Marker m = gm.addMarker(getMarker(c));
                mMarkerMapping.put(m, c.getLong(idCol));
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> arg0) {
            final GoogleMap m = getMap();
            if (m != null) {
                m.clear();
            }

            mMarkerMapping.clear();
        }
    };

    @Override
    public boolean onMarkerClick(Marker marker) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Called when the info window for a marker is clicked
     *
     * @param marker
     * @param item
     *            the content item
     */
    protected void onInfoWindowClick(Marker marker, Uri item) {
        startActivity(new Intent(Intent.ACTION_VIEW, item));
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final Long id = mMarkerMapping.get(marker);
        if (id != null) {
            onInfoWindowClick(marker, ContentUris.withAppendedId(mLocatable, id));
        }
    }
}
