package edu.mit.mobile.android.locast.example.data;

import android.database.Cursor;
import android.net.Uri;
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
        return null;
    }

    public static final ItemSyncMap SYNC_MAP = new ItemSyncMap();

    public static final String TYPE_DIR = "vnd.android.cursor.dir/vnd.edu.mit.mobile.android.locast.example.cast.#.media";

    public static class ItemSyncMap extends VideoContent.ItemSyncMap {
        /**
         *
         */
        private static final long serialVersionUID = 2603776320366624920L;

        public ItemSyncMap() {

        }
    }

    @Override
    public SyncMap getSyncMap() {
        return SYNC_MAP;
    }
}
