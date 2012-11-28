package edu.mit.mobile.android.widget;

/*
 * LocationLink.java
 * Copyright (C) 2010 MIT Mobile Experience Lab
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.utils.AddressUtils;

/**
 * A button that shows a reverse-geocoded location. This can toggle between two different displays:
 * the current location or a saved location.
 *
 * @author steve
 *
 */
public class LocationLink extends Button {
    public static final String TAG = LocationLink.class.getSimpleName();

    private Location mLocation;
    private static String LAT_LON_FORMAT = "%.4f, %.4f";
    private String mGeocodedName;
    private final int mNoLocationResId = R.string.location_link_no_location;
    private boolean mShowAccuracy = true;
    private boolean mShowLatLon = true;

    // these should sync up with the drawable's xml
    private static final int LEVEL_OFF = 0;
    private static final int LEVEL_SEARCHING = 1;
    private static final int LEVEL_FOUND = 2;
    private static final int LEVEL_SAVED = 3;

    public static final int STATE_OFF = LEVEL_OFF;
    public static final int STATE_SEARCHING = LEVEL_SEARCHING;
    public static final int STATE_FOUND = LEVEL_FOUND;

    private final OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mWrappedOnClickListener != null) {
                mWrappedOnClickListener.onClick(v);
            }
            setShowSaved(!mShowSaved);
        }
    };

    private OnClickListener mWrappedOnClickListener;

    ConnectivityManager mCm;

    private Location mSavedLocation;

    private boolean mShowSaved;

    private int mState;

    private String mSavedGeocodedName;

    /**
     * Set this to true to disable updating the label after changing the data via accessors.
     */
    private boolean mRestoring = false;

    public LocationLink(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public LocationLink(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LocationLink(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setText(mNoLocationResId);
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        super.setOnClickListener(mOnClickListener);

        // initially unclickable until a saved location is loaded.
        setClickable(false);
    }

    /**
     * Sets the level of the compound drawable, if there is one.
     *
     * @param level
     */
    private void setDrawableLevel(int level) {
        if (mShowSaved) {
            level = LEVEL_SAVED;
        }

        final Drawable left = getCompoundDrawables()[0];
        if (left != null) {
            left.setLevel(level);
        }
        postInvalidate();
    }

    /**
     * Sets the location that is displayed on the link. Will display accuracy information if present
     * and if showAccuracy is set.
     *
     * @param location
     */
    public void setLocation(Location location) {
        // only update if it's a distinctly new location.
        if (this.mLocation == null || location == null || this.mLocation.distanceTo(location) != 0) {

            this.mLocation = location;
            mGeocodedName = null; // invalidated

            updateLabel();
        }
    }

    public void setLocation(Location location, int state) {
        setLocation(location);
        setLocationState(state);
    }

    @Override
    public int getMinimumHeight() {

        int drawableH = 0;
        final Drawable l = getCompoundDrawables()[0];
        if (l != null) {
            drawableH = l.getIntrinsicHeight() + getCompoundPaddingTop()
                    + getCompoundPaddingBottom();
        }

        return Math.max(super.getMinimumHeight(), drawableH);
    }

    /**
     * Sets the saved location.
     *
     * @param saved
     */
    public void setSavedLocation(Location saved) {
        // only update if it's a distinctly new location.
        if (this.mSavedLocation == null || saved == null
                || this.mSavedLocation.distanceTo(saved) != 0) {

            this.mSavedLocation = saved;
            mSavedGeocodedName = null; // invalidated

            final boolean hasSaved = mSavedLocation != null;

            setClickable(hasSaved);

            // only hide the shown saved if it gets set to null.
            if (!hasSaved) {
                setShowSaved(false);
            }
        }
    }

    public void setLocationState(int state) {
        switch (state) {
            case STATE_FOUND:
            case STATE_OFF:
            case STATE_SEARCHING:
                mState = state;
                setDrawableLevel(state);
                break;

            default:
                throw new IllegalArgumentException("Invalid state: " + state);
        }
    }

    public Location getShownLocation() {
        return mShowSaved ? mSavedLocation : mLocation;
    }

    public void setShowSaved(boolean showSaved) {
        if (mSavedLocation == null || showSaved == mShowSaved) {
            return;
        }

        mShowSaved = showSaved;

        setDrawableLevel(mState);
        setSelected(showSaved);
        updateLabel();
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mWrappedOnClickListener = l;
    }

    /**
     * Sets the location that is displayed on the link. Will display accuracy information if present
     * and if showAccuracy is set.
     *
     * @param latitude
     * @param longitude
     */
    public void setLocation(double latitude, double longitude) {
        final Location l = new Location("internal");
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        setLocation(l);
    }

    public void setShowLatLon(boolean showLatLon) {
        mShowLatLon = showLatLon;
    }

    public Location getLocation() {
        return mLocation;
    }

    /**
     * Shows the accuracy of the location.
     *
     * @param showAccuracy
     */
    public void setShowAccuracy(boolean showAccuracy) {
        this.mShowAccuracy = showAccuracy;
    }

    public boolean getShowAccuracy() {
        return mShowAccuracy;
    }

    private void setGeocodedName(String placename) {
        mGeocodedName = placename;
        updateLabel();
    }

    private void setSavedGeocodedName(String placename) {
        mSavedGeocodedName = placename;
        updateLabel();
    }

    private void updateLabel() {
        if (mRestoring) {
            return;
        }

        Location location = mLocation;
        String placename = mGeocodedName;

        if (mShowSaved) {
            location = mSavedLocation;
            placename = mSavedGeocodedName;
        }

        if (location == null) {
            setText(mNoLocationResId);
        } else {
            String accuracy = "";
            if (mShowAccuracy && location.hasAccuracy()) {
                final float accuracyVal = location.getAccuracy();
                if (accuracyVal <= 500) {
                    accuracy = " "
                            + getResources().getString(R.string.location_link_accuracy_meter,
                                    accuracyVal);
                } else {
                    accuracy = " "
                            + getResources().getString(R.string.location_link_accuracy_kilometer,
                                    accuracyVal / 1000.0);
                }
            }

            if (placename == null) {
                if (mShowLatLon) {
                    placename = String.format(LAT_LON_FORMAT, location.getLatitude(),
                            location.getLongitude());
                } else {
                    placename = getResources().getString(R.string.location_link_location_found);
                }
                final NetworkInfo activeNet = mCm.getActiveNetworkInfo();
                final boolean hasNetConnection = activeNet != null && activeNet.isConnected();
                if (hasNetConnection) {
                    final GeocoderTask t = new GeocoderTask(mShowSaved);
                    t.execute(location);
                }
            }

            setText(placename + accuracy);
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mRestoring = true;

        this.mLocation = ss.location;
        this.mGeocodedName = ss.geocodedName;

        setSavedLocation(ss.savedLocation);
        this.mSavedGeocodedName = ss.savedGeocodedName;

        setLocationState(this.mState);

        setShowSaved(ss.showSaved);

        mRestoring = false;

        updateLabel();

    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        final SavedState ss = new SavedState(superState);
        ss.location = mLocation;
        ss.geocodedName = mGeocodedName;
        ss.savedLocation = mSavedLocation;
        ss.savedGeocodedName = mSavedGeocodedName;
        ss.state = mState;
        ss.showSaved = mShowSaved;
        return ss;
    }

    /**
     * This will preserve the looked-up placename, preventing unnecessary geocoder lookups.
     *
     * @author stevep
     *
     */
    private class SavedState extends BaseSavedState {
        Location location;
        String geocodedName;
        Location savedLocation;
        String savedGeocodedName;
        int state;
        boolean showSaved;

        public SavedState(Parcel in) {
            super(in);

            location = Location.CREATOR.createFromParcel(in);
            geocodedName = in.readString();
            savedLocation = Location.CREATOR.createFromParcel(in);
            savedGeocodedName = in.readString();
            state = in.readInt();
            showSaved = in.readInt() != 0;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeParcelable(location, flags);
            dest.writeString(geocodedName);
            dest.writeParcelable(savedLocation, flags);
            dest.writeString(savedGeocodedName);
            dest.writeInt(state);
            dest.writeInt(showSaved ? 1 : 0);
        }
    }

    private class GeocoderTask extends AsyncTask<Location, Long, String> {
        private final boolean mSaved;

        public GeocoderTask(boolean saved) {
            mSaved = saved;
        }

        @Override
        protected String doInBackground(Location... params) {
            final Location location = params[0];
            final Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try {
                final List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                if (addresses.size() > 0) {
                    final Address thisLocation = addresses.get(0);
                    // TODO fixme
                    // return thisLocation.toString();
                    return AddressUtils.addressToName(thisLocation);
                }
            } catch (final IOException e) {
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                if (mSaved) {
                    setSavedGeocodedName(result);
                } else {
                    setGeocodedName(result);
                }
            }
        }
    }
}
