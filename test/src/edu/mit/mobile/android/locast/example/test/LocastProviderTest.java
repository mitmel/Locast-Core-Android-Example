package edu.mit.mobile.android.locast.example.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.http.client.HttpResponseException;
import org.json.JSONException;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
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
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.data.NoPublicPath;
import edu.mit.mobile.android.locast.data.SyncException;
import edu.mit.mobile.android.locast.data.interfaces.Authorable;
import edu.mit.mobile.android.locast.data.interfaces.PrivatelyAuthorableUtils;
import edu.mit.mobile.android.locast.data.tags.Tag;
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
        cv.put(Authorable.COL_AUTHOR, mUsers.get(uri));
        cv.put(Authorable.COL_AUTHOR_URI, uri);
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
        assertTrue(PrivatelyAuthorableUtils.canEdit(steve, c));
        assertFalse(PrivatelyAuthorableUtils.canEdit(nick, c));

        c.close();
    }

    private static final String[] CAST_PROJECTION = new String[] { Cast._ID, Cast.COL_TITLE };

    public void testTags() throws JSONException {
        final MockContentResolver cr = getMockContentResolver();
        final String taggy = createUser("Tag T.");

        ContentValues cv = JsonSyncableItem.newContentItem();

        final String cast1Title = "Tagged Cast";
        addCastCv(cv, taggy, cast1Title);

        cv.put(Tag.TAGS_SPECIAL_CV_KEY, "foo,bar");

        final Uri cast1 = cr.insert(Cast.CONTENT_URI, cv);

        assertNotNull(cast1);

        final long cast1Id = ContentUris.parseId(cast1);

        Cursor c;

        c = cr.query(cast1, null, null, null, null);

        try {
            assertTrue(c.moveToFirst());

            assertEquals(1, c.getCount());

        } finally {
            c.close();
        }

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 1,
                new long[] { ContentUris.parseId(cast1) }, new String[] { cast1Title }, "foo");

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 1,
                new long[] { ContentUris.parseId(cast1) }, new String[] { cast1Title }, "foo",
                "bar");

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 0, null, null, "foo", "baz");

        cv = JsonSyncableItem.newContentItem();

        final String cast2Title = "Tagged Cast 2";
        addCastCv(cv, taggy, cast2Title);

        cv.put(Tag.TAGS_SPECIAL_CV_KEY, "foo,baz");

        final Uri cast2 = cr.insert(Cast.CONTENT_URI, cv);

        assertNotNull(cast2);

        final long cast2Id = ContentUris.parseId(cast2);

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 1,
                new long[] { ContentUris.parseId(cast2) }, new String[] { cast2Title }, "baz");

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 2,
                new long[] { ContentUris.parseId(cast1), ContentUris.parseId(cast2) },
                new String[] { cast1Title, cast2Title }, "foo");

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 1,
                new long[] { ContentUris.parseId(cast2) }, new String[] { cast2Title }, "foo",
                "baz");

        // now replace the tags for one item with a new tag
        final ContentValues cv2 = new ContentValues();
        final HashSet<String> tags = new HashSet<String>();
        tags.add("newtag1");
        cv2.put(Tag.TAGS_SPECIAL_CV_KEY, Tag.toTagQuery(tags));
        cr.update(cast1, cv2, null, null);

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 1,
                new long[] { ContentUris.parseId(cast1) }, new String[] { cast1Title }, "newtag1");

        // as the tags have been updated, these shouldn't match this cast anymore

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 1, new long[] { cast2Id },
                new String[] { cast2Title }, "foo");

        assertTagsMatch(cr, CAST_PROJECTION, Cast.CONTENT_URI, 0, null, null, "bar");

    }

    private void assertTagsMatch(ContentResolver cr, String[] projection, Uri contentDir,
            int expectedCount, long[] expectedIds, String[] expectedTitles, String... tag) {

        final Cursor c = queryTags(cr, projection, contentDir, tag);

        int i = 0;
        try {
            assertEquals(expectedCount, c.getCount());

            while (c.moveToNext()) {
                assertEquals(expectedIds[i], c.getLong(c.getColumnIndex(Cast._ID)));
                assertEquals(expectedTitles[i], c.getString(c.getColumnIndex(Cast.COL_TITLE)));
                i++;
            }
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

    private Cursor queryTags(ContentResolver cr, String[] projection, Uri taggableDir,
            String... tags) {
        return cr.query(
                taggableDir.buildUpon().appendQueryParameter("tags", TextUtils.join(",", tags))
                        .build(), projection, null, null, null);
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
