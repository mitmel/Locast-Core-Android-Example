package edu.mit.mobile.android.locast.example.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import edu.mit.mobile.android.content.ForeignKeyDBHelper;
import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.m2m.M2MDBHelper;
import edu.mit.mobile.android.locast.data.NoPublicPath;
import edu.mit.mobile.android.locast.net.NetworkClient;
import edu.mit.mobile.android.locast.sync.SyncableProvider;
import edu.mit.mobile.android.locast.sync.SyncableSimpleContentProvider;

public class LocastProvider extends SyncableSimpleContentProvider implements SyncableProvider {

    public static final String AUTHORITY = "edu.mit.mobile.android.locast.example";


    private static final int DB_VER = 1;

    private static final String TAG = LocastProvider.class.getSimpleName();

    public LocastProvider() {
        super(AUTHORITY, DB_VER);

        final GenericDBHelper casts = new GenericDBHelper(Cast.class);

        final ForeignKeyDBHelper castsMedia = new ForeignKeyDBHelper(Cast.class, CastMedia.class,
                CastMedia.CAST);

        final GenericDBHelper collections = new GenericDBHelper(Collection.class);

        final M2MDBHelper collectionCasts = new M2MDBHelper(collections, casts);

        addDirAndItemUri(casts, Cast.PATH);

        addChildDirAndItemUri(castsMedia, Cast.PATH, CastMedia.PATH);

        addChildDirAndItemUri(collectionCasts, Collection.PATH, Cast.PATH);

    }

    @Override
    public boolean canSync(Uri uri) {

        return true;
    }

    @Override
    public String getPostPath(Context context, Uri uri) throws NoPublicPath {
        return getPublicPath(context, uri);
    }

    @Override
    public String getPublicPath(Context context, Uri uri) throws NoPublicPath {
        Log.d(TAG, "getPublicPath " + uri);
        final String type = getType(uri);

        // TODO this is the only hard-coded URL. This should be removed eventually.
        if (Cast.TYPE_DIR.equals(type)) {
            return NetworkClient.getBaseUrlFromManifest(context) + "cast/";

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
