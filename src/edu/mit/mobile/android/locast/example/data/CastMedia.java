package edu.mit.mobile.android.locast.example.data;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.DBForeignKeyColumn;
import edu.mit.mobile.android.locast.data.SyncMap;
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
        return Cast.CAST_MEDIA.getUri(ContentUris.withAppendedId(Cast.CONTENT_URI,
                getLong(getColumnIndexOrThrow(CAST))));
    }

    public static final ItemSyncMap SYNC_MAP = new ItemSyncMap();

    public static final String TYPE_DIR = "vnd.android.cursor.dir/vnd.edu.mit.mobile.android.locast.example.castmedia";

    public static class ItemSyncMap extends VideoContent.ItemSyncMap {
        /**
         *
         */
        private static final long serialVersionUID = 2603776320366624920L;

        public ItemSyncMap() {
            // this is where you could put other fields. eg:
            // put(COL_LOCAL_COLUMN, new SyncFieldMap("remote_key", SyncFieldMap.STRING));
        }
    }

    public static Uri getParent(Uri castMedia) {
        return ProviderUtils.removeLastPathSegment(castMedia);
    }

    @Override
    public SyncMap getSyncMap() {
        return SYNC_MAP;
    }
}
