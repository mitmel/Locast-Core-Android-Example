package edu.mit.mobile.android.locast.example.app;

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
import edu.mit.mobile.android.locast.widget.TagListView;

public class CastDetailFragment extends CastFragment {

    private static final String[] CAST_PROJECTION = new String[] { Cast._ID, Cast.COL_TITLE,
            Cast.COL_DESCRIPTION, Cast.COL_AUTHOR, Cast.COL_AUTHOR_URI, Cast.COL_PRIVACY };

    public static CastDetailFragment getInstance(Uri cast) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_CAST_URI, cast);

        final CastDetailFragment f = new CastDetailFragment();
        f.setArguments(args);
        return f;
    }

    private TagListView mTags;

    @Override
    protected View onCreateCastFragmentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cast_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTags = (TagListView) view.findViewById(R.id.tags);
    }

    @Override
    protected String[] getCastProjection() {
        return CAST_PROJECTION;
    }

    @Override
    protected void loadCastFromCursor(Loader<Cursor> loader, Cursor c) {
        final View v = getView();

        ((TextView) v.findViewById(R.id.description)).setText(c.getString(c
                .getColumnIndexOrThrow(Cast.COL_DESCRIPTION)));

    }

}
