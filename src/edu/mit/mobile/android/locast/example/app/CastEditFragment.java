package edu.mit.mobile.android.locast.example.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.data.Cast;

public class CastEditFragment extends CastFragment {

    private static final String[] CAST_PROJECTION = new String[] { Cast._ID, Cast.COL_TITLE,
            Cast.COL_DESCRIPTION, Cast.COL_AUTHOR_URI, Cast.COL_PRIVACY, Cast.COL_DRAFT,
            Cast.COL_LATITUDE, Cast.COL_LONGITUDE, Cast.COL_AUTHOR };

    private static final String INSTANCE_IS_LOADED = "edu.mit.mobile.android.locast.example.CastEditFragment.CAST_IS_LOADED";

    private TextView mTitle;
    private TextView mDescription;

    // this is recorded so that we only call loadCastFromCursor once.
    private boolean mIsLoaded = false;

    public static CastEditFragment getInstance(Uri cast) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_CAST_URI, cast);

        final CastEditFragment f = new CastEditFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsLoaded = savedInstanceState.getBoolean(INSTANCE_IS_LOADED, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(INSTANCE_IS_LOADED, mIsLoaded);
    }

    @Override
    protected View onCreateCastFragmentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cast_edit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTitle = (TextView) view.findViewById(R.id.title);
        mDescription = (TextView) view.findViewById(R.id.description);
    }

    @Override
    protected String[] getCastProjection() {
        return CAST_PROJECTION;
    }

    @Override
    protected void loadCastFromCursor(Loader<Cursor> loader, Cursor c) {
        if (!mIsLoaded) {
            mTitle.setText(c.getString(c.getColumnIndexOrThrow(Cast.COL_TITLE)));
            mDescription.setText(c.getString(c.getColumnIndexOrThrow(Cast.COL_DESCRIPTION)));
            mIsLoaded = true;
        }
    }

    public boolean save() {
        final Uri cast = getArguments().getParcelable(ARG_CAST_URI);

        final ContentValues cv = new ContentValues();

        cv.put(Cast.COL_TITLE, mTitle.getText().toString());
        cv.put(Cast.COL_DESCRIPTION, mDescription.getText().toString());

        return getActivity().getContentResolver().update(cast, cv, null, null) == 1;
    }
}
