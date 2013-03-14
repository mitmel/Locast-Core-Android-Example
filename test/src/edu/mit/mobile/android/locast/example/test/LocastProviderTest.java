package edu.mit.mobile.android.locast.example.test;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.client.HttpResponseException;
import org.json.JSONException;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.test.IsolatedContext;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;
import android.text.TextUtils;
import edu.mit.mobile.android.locast.data.Authorable;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.data.NoPublicPath;
import edu.mit.mobile.android.locast.data.PrivatelyAuthorable;
import edu.mit.mobile.android.locast.data.SyncException;
import edu.mit.mobile.android.locast.data.Tag;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.example.data.LocastProvider;
import edu.mit.mobile.android.locast.net.LocastApplicationCallbacks;
import edu.mit.mobile.android.locast.net.NetworkClient;
import edu.mit.mobile.android.locast.net.NetworkProtocolException;
import edu.mit.mobile.android.locast.sync.SyncEngine;

public class LocastProviderTest extends ProviderTestCase2<LocastProvider> {

    HashMap<String, String> mUsers = new HashMap<String, String>();

    public LocastProviderTest() {
        super(LocastProvider.class, LocastProvider.AUTHORITY);
    }

    private String createUser(String name) throws JSONException {
        final String userDir = FakeNetworkClient.generateUser(name);

        mUsers.put(userDir, name);

        return userDir;
    }

    private void addUserCv(ContentValues cv, String uri) {
        cv.put(Authorable.Columns.COL_AUTHOR, mUsers.get(uri));
        cv.put(Authorable.Columns.COL_AUTHOR_URI, uri);
    }

    private Uri createCast(ContentResolver cr, String user, String title) {
        final ContentValues cv = JsonSyncableItem.newContentItem();

        cv.put(Cast.COL_TITLE, title);
        addUserCv(cv, user);

        final Uri cast = cr.insert(Cast.CONTENT_URI, cv);

        return cast;
    }

    private void addCastCv(ContentValues cv, String user, String title) {

        cv.put(Cast.COL_TITLE, title);
        addUserCv(cv, user);

    }

    public void testCreateCast() throws JSONException {
        final MockContentResolver cr = getMockContentResolver();


        final String steve = createUser("Steve P.");
        final String nick = createUser("Nick W.");

        final Uri kittenCast = createCast(cr, steve, "an impurrrrvious test");

        assertNotNull(kittenCast);

        final Cursor c = cr.query(kittenCast, null, null, null, null);
        assertTrue(c.moveToFirst());

        // test authorable
        assertTrue(PrivatelyAuthorable.canEdit(steve, c));
        assertFalse(PrivatelyAuthorable.canEdit(nick, c));

        c.close();
    }

    public void testTags() throws JSONException {
        final MockContentResolver cr = getMockContentResolver();
        final String taggy = createUser("Tag T.");

        final ContentValues cv = JsonSyncableItem.newContentItem();

        addCastCv(cv, taggy, "Tagged Cast");

        cv.put(Tag.TAGS_SPECIAL_CV_KEY, "foo,bar");

        final Uri cast = cr.insert(Cast.CONTENT_URI, cv);

        assertNotNull(cast);

        Cursor c;

        c = cr.query(cast, null, null, null, null);

        try {
            assertTrue(c.moveToFirst());

            assertEquals(1, c.getCount());

        } finally {
            c.close();
        }

        // one item should match
        c = queryTags(cr, Cast.CONTENT_URI, "foo");

        try {
            assertTrue(c.moveToFirst());

            assertEquals(1, c.getCount());

        } finally {
            c.close();
        }

        // one item should match
        c = queryTags(cr, Cast.CONTENT_URI, "foo", "bar");

        try {
            assertTrue(c.moveToFirst());

            assertEquals(1, c.getCount());

        } finally {
            c.close();
        }

        // shouldn't return anything
        c = queryTags(cr, Cast.CONTENT_URI, "foo", "baz");

        try {
            assertFalse(c.moveToFirst());

            assertEquals(0, c.getCount());

        } finally {
            c.close();
        }
    }

    @Override
    public IsolatedContext getMockContext() {
        return new LocastIsolatedContext(getMockContentResolver(), mContext);
    }

    private class LocastIsolatedContext extends IsolatedContext implements
            LocastApplicationCallbacks {

        public LocastIsolatedContext(ContentResolver resolver, Context targetContext) {
            super(resolver, targetContext);
        }

        @Override
        public NetworkClient getNetworkClientForAccount(Context context, Account account) {
            return new FakeNetworkClient(context, account);
        }
    }

    private Cursor queryTags(ContentResolver cr, Uri taggableDir, String... tags) {
        return cr.query(
                taggableDir.buildUpon().appendQueryParameter("tags", TextUtils.join(",", tags))
                        .build(), null, null, null, null);
    }

    public void testSyncEngine() throws HttpResponseException, RemoteException, SyncException,
            JSONException, IOException, NetworkProtocolException, NoPublicPath,
            OperationApplicationException, InterruptedException {
        // set up a mock context
        final LocastProvider provider = getProvider();
        final Context context = getMockContext();


        final FakeNetworkClient networkClient = new FakeNetworkClient(getContext(), "Steve P.",
                "spomeroy@mit.edu");
        final Account account = networkClient.getAccount();

        assertNotNull(account);

        final String steve = networkClient.getUserUri(account);

        assertNotNull(steve);

        final SyncEngine se = new SyncEngine(context, networkClient, provider);

        final SyncResult results = new SyncResult();
        final Uri uri = Cast.CONTENT_URI;

        sync(se, uri, account, results);

        Cast c;
        c = new Cast(provider.query(Cast.CONTENT_URI, null, null, null, null));


        assertEquals(0, c.getCount());

        c.close();

        final String newlyAddedCast = networkClient.createCast("My sweet cast", steve);

        sync(se, uri, account, results);

        c = new Cast(provider.query(Cast.CONTENT_URI, null, null, null, null));

        assertEquals(1, c.getCount());

        assertTrue(c.moveToFirst());

        assertEquals(newlyAddedCast, c.getPublicUrl());

        c.close();

    }

    private void sync(SyncEngine se, Uri uri, Account account, SyncResult results)
            throws HttpResponseException, RemoteException, SyncException, JSONException,
            IOException, NetworkProtocolException, NoPublicPath, OperationApplicationException,
            InterruptedException {
        final Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        se.sync(uri, account, b,
                getMockContentResolver().acquireContentProviderClient(LocastProvider.AUTHORITY),
                results);
    }
}
