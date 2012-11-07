package edu.mit.mobile.android.locast.example.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import edu.mit.mobile.android.content.ForeignKeyDBHelper;
import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.m2m.M2MDBHelper;
import edu.mit.mobile.android.locast.data.JSONSyncableIdenticalChildFinder;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.data.NoPublicPath;
import edu.mit.mobile.android.locast.net.LocastApplicationCallbacks;
import edu.mit.mobile.android.locast.net.NetworkClient;
import edu.mit.mobile.android.locast.sync.SyncableProvider;
import edu.mit.mobile.android.locast.sync.SyncableSimpleContentProvider;

public class LocastProvider extends SyncableSimpleContentProvider implements SyncableProvider {

    public static final String AUTHORITY = "edu.mit.mobile.android.locast.example";


    private static final int DB_VER = 1;

    private static final String TAG = LocastProvider.class.getSimpleName();

    private String mBaseUrl;

    public LocastProvider() {
        super(AUTHORITY, DB_VER);

        final GenericDBHelper casts = new GenericDBHelper(Cast.class);

        final ForeignKeyDBHelper castsMedia = new ForeignKeyDBHelper(Cast.class, CastMedia.class,
                CastMedia.CAST);

        final GenericDBHelper collections = new GenericDBHelper(Collection.class);

        final M2MDBHelper collectionCasts = new M2MDBHelper(collections, casts,
                new JSONSyncableIdenticalChildFinder());

        // /cast/
        // /cast/1/
        addDirAndItemUri(casts, Cast.PATH);

        // /cast/1/media/
        // /cast/1/media/1/
        addChildDirAndItemUri(castsMedia, Cast.PATH, CastMedia.PATH);

        // /collection/
        // /collection/1/
        addDirAndItemUri(collections, Collection.PATH);

        // /collection/1/cast/
        // /collection/1/cast/1/
        addChildDirAndItemUri(collectionCasts, Collection.PATH, Cast.PATH);

        // /collection/1/cast/1/media/
        // /collection/1/cast/1/media/1/
        addChildDirAndItemUri(castsMedia, Collection.PATH + "/#/" + Cast.PATH, CastMedia.PATH);

    }

    @Override
    public boolean canSync(Uri uri) {

        return true;
    }

    /**
     * @param context
     * @param uri
     *            the URI of the item whose field should be queried
     * @param field
     *            the string name of the field
     * @return
     */
    private static String getPathFromField(Context context, Uri uri, String field) {
        String path = null;
        final String[] generalProjection = { JsonSyncableItem._ID, field };
        final Cursor c = context.getContentResolver().query(uri, generalProjection, null, null,
                null);
        try {
            if (c.getCount() == 1 && c.moveToFirst()) {
                final String storedPath = c.getString(c.getColumnIndex(field));
                path = storedPath;
            } else {
                throw new IllegalArgumentException("could not get path from field '" + field
                        + "' in uri " + uri);
            }
        } finally {
            c.close();
        }
        return path;
    }

    @Override
    public String getPostPath(Context context, Uri uri) throws NoPublicPath {
        return getPublicPath(context, uri);
    }

    @Override
    public String getPublicPath(Context context, Uri uri) throws NoPublicPath {
        Log.d(TAG, "getPublicPath " + uri);
        final String type = getType(uri);

        if (mBaseUrl == null) {
            final NetworkClient nc = ((LocastApplicationCallbacks) context.getApplicationContext())
                    .getNetworkClient(context, null);
            mBaseUrl = nc.getBaseUrl();
        }

        // TODO this is the only hard-coded URL. This should be removed eventually.
        if (Cast.TYPE_DIR.equals(type)) {
            if (uri.getPathSegments().size() == 1) {
                return mBaseUrl + "cast/";
            } else {
                return getPathFromField(context, ProviderUtils.removeLastPathSegment(uri),
                        Collection.COL_CASTS_URI);
            }

        } else if (CastMedia.TYPE_DIR.equals(type)) {
            return getPathFromField(context, CastMedia.getParent(uri), Cast.COL_MEDIA_PUBLIC_URL);

            // TODO this too
        } else if (Collection.TYPE_DIR.equals(type)) {
            return mBaseUrl + "collection/";

            // TODO find a way to make this generic. Inspect the SYNC_MAP somehow?
            // } else if (CastMedia.TYPE_DIR.equals(type)) {
            // return JsonSyncableItem.SyncChildRelation.getPathFromField(context,
            // CastMedia.getCard(uri), Card.COL_MEDIA_URL);
        } else {
            return super.getPublicPath(context, uri);
        }
    }

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }
}
