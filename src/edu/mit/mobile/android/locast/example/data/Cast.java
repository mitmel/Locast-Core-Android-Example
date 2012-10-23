package edu.mit.mobile.android.locast.example.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import edu.mit.mobile.android.content.ForeignKeyManager;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.TextColumn;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.data.Locatable;
import edu.mit.mobile.android.locast.data.PrivatelyAuthorable;
import edu.mit.mobile.android.locast.data.SyncMap;
import edu.mit.mobile.android.locast.data.Titled;
import edu.mit.mobile.android.locast.example.R;

@UriPath(Cast.PATH)
public class Cast extends JsonSyncableItem implements Titled.Columns, PrivatelyAuthorable.Columns,
        Locatable.Columns {
    public final static String TAG = Cast.class.getSimpleName();

    @DBColumn(type = TextColumn.class)
    public static final String COL_MEDIA_PUBLIC_URL = "media_url";

    @DBColumn(type = TextColumn.class)
    public static final String COL_THUMBNAIL_URL = "thumbnail_url";

    public static final ForeignKeyManager CAST_MEDIA = new ForeignKeyManager(CastMedia.class);

    public Cast(Cursor c) {
        super(c);
    }

    public static final String PATH = "cast";

    @Override
    public Uri getContentUri() {
        return CONTENT_URI;
    }

    public static final Uri CONTENT_URI = ProviderUtils
            .toContentUri(LocastProvider.AUTHORITY, PATH);

    public static class ItemSyncMap extends JsonSyncableItem.ItemSyncMap {
        /**
         *
         */
        private static final long serialVersionUID = -2194542380700398098L;

        public ItemSyncMap() {
            putAll(Titled.SYNC_MAP);
            putAll(PrivatelyAuthorable.SYNC_MAP);
            putAll(Locatable.SYNC_MAP);
        }
    }

    public static final ItemSyncMap SYNC_MAP = new ItemSyncMap();

    public static final String TYPE_DIR = "vnd.android.cursor.dir/vnd.edu.mit.mobile.android.locast.example.cast";

    @Override
    public SyncMap getSyncMap() {
        return SYNC_MAP;
    }

    public static CharSequence getTitle(Context context, Cursor c) {
        CharSequence title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
        if (title == null || title.length() == 0) {
            title = context.getText(R.string.untitled);
        }

        return title;
    }
}
