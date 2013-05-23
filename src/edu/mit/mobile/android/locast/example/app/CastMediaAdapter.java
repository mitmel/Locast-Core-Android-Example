package edu.mit.mobile.android.locast.example.app;

import android.content.Context;
import android.database.Cursor;

import com.stackoverflow.ArrayUtils;

import edu.mit.mobile.android.imagecache.SimpleThumbnailCursorAdapter;
import edu.mit.mobile.android.locast.nfftt.R;
import edu.mit.mobile.android.locast.example.data.CastMedia;

public class CastMediaAdapter extends SimpleThumbnailCursorAdapter {

    public static final int[] IMAGE_IDS = new int[] { R.id.thumbnail };

    public static final String[] FROM = new String[] { CastMedia.COL_THUMB_LOCAL,
            CastMedia.COL_SCREENSHOT };

    public static final String[] PROJECTION = ArrayUtils.concat(FROM, new String[] { CastMedia._ID,
            CastMedia.COL_MIME_TYPE, CastMedia.COL_LOCAL_URL, CastMedia.COL_MEDIA_URL,
            CastMedia.COL_AUTHOR_URI });

    public static final int[] TO = new int[] { R.id.thumbnail, R.id.thumbnail };

    public CastMediaAdapter(Context context, Cursor c, int flags) {
        this(context, R.layout.cast_media_item, c, flags);

    }

    public CastMediaAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, FROM, TO, IMAGE_IDS, flags);

    }

}
