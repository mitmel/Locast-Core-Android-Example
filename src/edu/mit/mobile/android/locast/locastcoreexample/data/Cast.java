package edu.mit.mobile.android.locast.locastcoreexample.data;

import android.database.Cursor;
import android.net.Uri;
import edu.mit.mobile.android.content.ForeignKeyManager;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.TextColumn;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;

@UriPath(Cast.PATH)
public class Cast extends JsonSyncableItem {
    public final static String TAG = Cast.class.getSimpleName();

    @DBColumn(type = TextColumn.class)
    public static final String _TITLE = "title";

    @DBColumn(type = TextColumn.class)
    public static final String _DESCRIPTION = "description";

    @DBColumn(type = TextColumn.class)
    public static final String _MEDIA_PUBLIC_URI = "public_uri";

    @DBColumn(type = TextColumn.class)
    public static final String _THUMBNAIL_URI = "thumbnail_uri";

    public static final ForeignKeyManager CAST_MEDIA = new ForeignKeyManager(CastMedia.class);

    public Cast(Cursor c) {
        super(c);
    }

    public static final String PATH = "cast";

    @Override
    public Uri getContentUri() {
        return CONTENT_URI;
    }

    public static final Uri CONTENT_URI = ProviderUtils.toContentUri(ExampleProvider.AUTHORITY,
            PATH);
}
