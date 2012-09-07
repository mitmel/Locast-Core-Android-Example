package edu.mit.mobile.android.locast.locastcoreexample.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.SimpleContentProvider;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.data.NoPublicPath;
import edu.mit.mobile.android.locast.data.SyncMap;
import edu.mit.mobile.android.locast.data.SyncMapException;
import edu.mit.mobile.android.locast.sync.SyncableProvider;

public class ExampleProvider extends SimpleContentProvider implements SyncableProvider {

    public static final String AUTHORITY = "edu.mit.mobile.android.locastcoreexample";


    private static final int DB_VER = 1;

    public ExampleProvider() {
        super(AUTHORITY, DB_VER);


    }

    @Override
    public SyncMap getSyncMap(ContentProviderClient provider, Uri toSync) throws RemoteException,
            SyncMapException {
        // TODO extract to more useful class
        final Class<? extends ContentItem> itemClass = getContentItem(toSync);

        try {
            final Field syncMapField = itemClass.getField("SYNC_MAP");
            if ((syncMapField.getModifiers() & Modifier.STATIC) == 0) {
                throw new SyncMapException("SYNC_MAP field in " + itemClass + " is not static");
            }
            if ((syncMapField.getModifiers() & Modifier.PUBLIC) == 0) {
                throw new SyncMapException("SYNC_MAP field in " + itemClass + " is not public");
            }

            return (SyncMap) syncMapField.get(null);

        } catch (final NoSuchFieldException e) {
            throw new SyncMapException(itemClass + " does not have a public static field SYNC_MAP",
                    e);
        } catch (final IllegalArgumentException e) {
            throw new SyncMapException("programming error in Locast Core", e);
        } catch (final IllegalAccessException e) {
            throw new SyncMapException("programming error in Locast Core", e);
        }
    }

    @Override
    public String getPublicPath(Context context, Uri uri) throws NoPublicPath {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canSync(Uri uri) {

        return true;
    }

    @Override
    public String getPostPath(Context context, Uri uri) throws NoPublicPath {

        // getWrappedContentItem(uri, c)
        return null;
    }

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }

    @Override
    public JsonSyncableItem getWrappedContentItem(Uri item, Cursor c) {
        // TODO extract to more useful class
        final Class<? extends JsonSyncableItem> itemClass = (Class<? extends JsonSyncableItem>) getContentItem(item);

        if (itemClass == null) {
            throw new RuntimeException("could not get content item; no mapping has been made");
        }

        try {
            final Constructor<? extends JsonSyncableItem> cons = itemClass.getConstructor(Cursor.class);
            return cons.newInstance(c);

        } catch (final NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
