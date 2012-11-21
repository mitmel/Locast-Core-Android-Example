package edu.mit.mobile.android.locast.example.app;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.mit.mobile.android.locast.example.BuildConfig;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.accounts.AuthenticationService;
import edu.mit.mobile.android.locast.example.accounts.Authenticator;
import edu.mit.mobile.android.locast.example.data.Cast;

public class CastEditFragment extends CastFragment {

    private static final String[] CAST_PROJECTION = new String[] { Cast._ID, Cast.COL_TITLE,
            Cast.COL_DESCRIPTION, Cast.COL_AUTHOR_URI, Cast.COL_PRIVACY, Cast.COL_DRAFT,
            Cast.COL_LATITUDE, Cast.COL_LONGITUDE, Cast.COL_AUTHOR };

    private static final String INSTANCE_IS_LOADED = "edu.mit.mobile.android.locast.example.CastEditFragment.CAST_IS_LOADED";
    private static final String INSTANCE_IS_DRAFT = "edu.mit.mobile.android.locast.example.CastEditFragment.IS_DRAFT";

    private static final String TAG = CastEditFragment.class.getSimpleName();

    private TextView mTitle;
    private TextView mDescription;

    // stateful

    // this is recorded so that we only call loadCastFromCursor once.
    private boolean mIsLoaded;

    private boolean mIsDraft;

    /**
     * @param action
     *            {@link Intent#ACTION_EDIT} or {@link Intent#ACTION_INSERT}
     * @param cast
     *            a cast item or cast dir, respectively
     * @return
     */
    public static CastEditFragment getInstance(String action, Uri cast) {
        final Bundle args = new Bundle(2);
        if (Intent.ACTION_INSERT.equals(action)) {
            args.putParcelable(ARG_CAST_DIR_URI, cast);

        } else if (Intent.ACTION_EDIT.equals(action)) {
            args.putParcelable(ARG_CAST_URI, cast);
        }

        args.putString(ARG_INTENT_ACTION, action);

        final CastEditFragment f = new CastEditFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            savedInstanceState = Bundle.EMPTY;
        }

        mIsLoaded = savedInstanceState.getBoolean(INSTANCE_IS_LOADED, false);
        mIsDraft = savedInstanceState.getBoolean(INSTANCE_IS_DRAFT, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(INSTANCE_IS_LOADED, mIsLoaded);
        outState.putBoolean(INSTANCE_IS_DRAFT, mIsDraft);
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
            mIsDraft = Cast.isDraft(c);
            mIsLoaded = true;
        }
    }

    public Uri getCast() {
        return getArguments().getParcelable(ARG_CAST_URI);
    }

    public boolean validate() {

        // title is required
        if (mTitle.getText().toString().trim().length() == 0) {
            mTitle.setError(getActivity().getString(R.string.err_validate_please_enter_a_title));
            mTitle.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Validates and marks the cast not a draft if it succeeds. You must then call {@link #save()}.
     *
     * @return true if publishing succeeds
     */
    public boolean validateAndClearDraft() {
        if (!validate()) {
            return false;
        }

        mIsDraft = false;

        return true;
    }

    /**
     * Saves the state of the fragment to the database. If this is creating new content, the URI can
     * be retrieved with {@link #getCast()}.
     *
     * @return true if the save succeeded.
     */
    public boolean save() {
        final Bundle args = getArguments();
        Uri cast = args.getParcelable(ARG_CAST_URI);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "save() called with arguments " + args);
        }

        final ContentValues cv = new ContentValues();

        cv.put(Cast.COL_TITLE, mTitle.getText().toString());
        cv.put(Cast.COL_DESCRIPTION, mDescription.getText().toString());

        cv.put(Cast.COL_DRAFT, mIsDraft);

        if (cast == null) {
            cv.put(Cast.COL_AUTHOR_URI,
                    Authenticator.getUserUri(getActivity(), Authenticator.ACCOUNT_TYPE));

            cv.put(Cast.COL_AUTHOR, Authenticator.getUserData(getActivity(),
                    Authenticator.ACCOUNT_TYPE, AuthenticationService.USERDATA_DISPLAY_NAME));

            final Uri castDir = args.getParcelable(ARG_CAST_DIR_URI);
            cast = getActivity().getContentResolver().insert(castDir, cv);

            if (cast != null) {
                args.putParcelable(ARG_CAST_URI, cast);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Just created " + cast + " from CV " + cv);
                }
            }
            return cast != null;

        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "update cast " + cast + " with cv " + cv);
            }
            return getActivity().getContentResolver().update(cast, cv, null, null) == 1;

        }
    }
}
