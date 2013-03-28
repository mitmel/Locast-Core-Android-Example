package edu.mit.mobile.android.locast.example.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.mit.mobile.android.locast.data.tags.TaggableUtils;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.widget.TagButton;

public class CastDetailFragment extends CastFragment {

    private static final String[] CAST_PROJECTION = new String[] { Cast._ID, Cast.COL_TITLE,
            Cast.COL_DESCRIPTION, Cast.COL_AUTHOR, Cast.COL_AUTHOR_URI, Cast.COL_PRIVACY };

    private final OnClickListener mOnTagClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            final String tag = ((TagButton) v).getText().toString();

            startActivity(new Intent(Intent.ACTION_VIEW, TaggableUtils.getItemMatchingTags(
                    Cast.CONTENT_URI, tag)));
        }
    };

    public static CastDetailFragment getInstance(Uri cast) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_CAST_URI, cast);

        final CastDetailFragment f = new CastDetailFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    protected View onCreateCastFragmentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cast_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTags.setOnTagClickListener(mOnTagClickListener);
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
