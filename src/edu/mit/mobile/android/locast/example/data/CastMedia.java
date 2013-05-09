package edu.mit.mobile.android.locast.example.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.DBForeignKeyColumn;
import edu.mit.mobile.android.locast.data.ResourcesSync;
import edu.mit.mobile.android.locast.data.SyncMap;
import edu.mit.mobile.android.locast.data.VideoContent;
import edu.mit.mobile.android.locast.data.interfaces.Authorable;
import edu.mit.mobile.android.locast.data.interfaces.AuthorableUtils;
import edu.mit.mobile.android.locast.net.NetworkProtocolException;

@UriPath(CastMedia.PATH)
public class CastMedia extends VideoContent implements Authorable {

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

    // currently, cast media can be either a video or an image. This combines them.
    public static class CombinedResourcesSync extends VideoResourcesSync {
        public CombinedResourcesSync() {

        }

        @Override
        protected void fromResourcesJSON(Context context, Uri localItem, ContentValues cv,
                JSONObject resources) throws NetworkProtocolException, JSONException {
            // TODO Auto-generated method stub
            super.fromResourcesJSON(context, localItem, cv, resources);

            addToContentValues(cv, "thumbnail", resources, COL_SCREENSHOT, null, false);

        }
    }

    public static final ItemSyncMap SYNC_MAP = new ItemSyncMap();

	public static final String TYPE_DIR = "vnd.android.cursor.dir/vnd.edu.mit.mobile.android.locast.misti.castmedia";

    public static class ItemSyncMap extends VideoContent.ItemSyncMap {
        /**
         *
         */
        private static final long serialVersionUID = 2603776320366624920L;

        public ItemSyncMap() {
            // this is where you could put other fields. eg:
            // put(COL_LOCAL_COLUMN, new SyncFieldMap("remote_key", SyncFieldMap.STRING));
            putAll(AuthorableUtils.SYNC_MAP);
        }

        @Override
        public ResourcesSync getResourcesSync() {
            return new CombinedResourcesSync();
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
