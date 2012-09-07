package edu.mit.mobile.android.locast.locastcoreexample.data;

import android.database.Cursor;
import android.net.Uri;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.DBForeignKeyColumn;
import edu.mit.mobile.android.locast.data.VideoContent;

@UriPath(CastMedia.PATH)
public class CastMedia extends VideoContent {

    public static final String PATH = "media";

    @DBForeignKeyColumn(parent = Cast.class)
    public static final String CAST = "cast";

    public CastMedia(Cursor c) {
        super(c);
    }

    @Override
    public Uri getContentUri() {
        return null;
    }

    public static final ItemSyncMap SYNC_MAP = new ItemSyncMap();

}
