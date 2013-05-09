package edu.mit.mobile.android.locast.example.test;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import edu.mit.mobile.android.locast.example.accounts.AuthenticationService;
import edu.mit.mobile.android.locast.example.accounts.Authenticator;
import edu.mit.mobile.android.locast.net.ClientResponseException;
import edu.mit.mobile.android.locast.net.NetworkClient;
import edu.mit.mobile.android.locast.net.NetworkProtocolException;

public class FakeNetworkClient extends NetworkClient {

    public static HashMap<URI, Object> data = new HashMap<URI, Object>();

    private static HashMap<Account, Bundle> mUserData = new HashMap<Account, Bundle>();

    private static final String CAST_PATH = "cast/";

    private static final String COLLECTION_PATH = "collection/";

    private static final String USER_PATH = "user/";

    private static final String CAST_MEDIA_PATH = "media/";

    private static String BASE_URL = "http://example.org/api/";

    private static URI CAST_URI = resolveUrl(CAST_PATH);

    private static URI COLLECTION_URI = resolveUrl(COLLECTION_PATH);

    private static URI USER_URI = resolveUrl(USER_PATH);

    private int mCastId = 1;
    private int mCastMediaId = 1;
    private int mCollectionId = 1;

    private final Account mAccount;
    private static int mUserId = 1;

    public FakeNetworkClient(Context context, Account account) {
        super(context, account);
        mAccount = account;
    }

    public FakeNetworkClient(Context context, String displayName, String username)
            throws JSONException {
        this(context, createAccount(context, displayName, username));

        data.put(CAST_URI, new JSONArray());
        data.put(COLLECTION_URI, new JSONArray());
        data.put(USER_URI, new JSONArray());

    }

    public Account getAccount() {
        return mAccount;
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    private static Account createAccount(Context context, String displayName, String username)
            throws JSONException {
        final String userUri = generateUser(displayName);
        final Account account = new Account(username, Authenticator.ACCOUNT_TYPE);
        final AccountManager am = AccountManager.get(context);

        am.setUserData(account, AuthenticationService.USERDATA_DISPLAY_NAME, displayName);
        am.setUserData(account, AuthenticationService.USERDATA_LOCAST_API_URL, "");
        am.setUserData(account, AuthenticationService.USERDATA_USER_URI, userUri);
        am.setUserData(account, AuthenticationService.USERDATA_USERID, "1");

        final Bundle userdata = new Bundle();
        userdata.putString(AuthenticationService.USERDATA_DISPLAY_NAME, displayName);
        userdata.putString(AuthenticationService.USERDATA_USER_URI, userUri);

        mUserData.put(account, userdata);
        return account;
    }

    public String getUserUri(Account user) {
        return mUserData.get(user).getString(AuthenticationService.USERDATA_USER_URI);
    }

    private static URI resolveUrl(String path) {
        return URI.create(BASE_URL).resolve(path);
    }

    private static URI getContentUri(String base, int id) {
        return resolveUrl(base + id + "/");
    }

    public String createCast(String title, String authorUri) throws JSONException {
        final JSONObject jo = newSyncableItem();
        jo.put("title", title);

        jo.put("author", data.get(resolveUrl(authorUri)));
        jo.put("id", mCastId);
        final URI castUri = getContentUri(CAST_PATH, mCastId);
        jo.put("uri", castUri.getPath());
        jo.put("media", castUri.resolve(CAST_MEDIA_PATH).getPath());
        jo.put("privacy", "protected");
        jo.put("tags", new JSONArray());

        jo.put("resources", new JSONArray());

        mCastId++;

        data.put(castUri, jo);

        addToCasts(jo);

        return castUri.getPath();
    }

    private void addToCasts(JSONObject jo){
        final JSONArray ja = (JSONArray) data.get(CAST_URI);

        ja.put(jo);
    }

    private static JSONObject newSyncableItem() throws JSONException {
        final JSONObject jo = new JSONObject();
        jo.put("uuid", UUID.randomUUID());

        addCreatedModified(jo);

        return jo;
    }

    private static void addCreatedModified(JSONObject jo) throws JSONException {
        final String now = dateFormat.format(new Date());
        jo.put("created", now);
        jo.put("modified", now);
    }

    public String generateCastMedia(String cast, String mimeType, String media)
            throws JSONException {
        final JSONObject jo = (JSONObject) data.get(cast);
        final JSONArray res = jo.getJSONArray("resources");

        final String uri = cast + CAST_MEDIA_PATH + mCastMediaId + "/";

        // ??? data.put(uri,res);

        mCastMediaId++;

        return uri;
    }

    public String generateCollection(String title, String authorUri) throws JSONException {
        final JSONObject jo = newSyncableItem();
        jo.put("title", title);

        jo.put("author", data.get(resolveUrl(authorUri)));

        final URI uri = getContentUri(COLLECTION_PATH, mCollectionId);

        final URI casts = uri.resolve(CAST_PATH);
        jo.put("casts", casts.getPath());

        data.put(casts, new JSONArray());

        mCollectionId++;
        return uri.getPath();
    }

    public void addCastToCollection(String cast, String collection) throws JSONException {
        final JSONObject jo = (JSONObject) data.get(collection);

        final String castsUri = jo.getString("casts");

        final JSONArray casts = (JSONArray) data.get(castsUri);

        casts.put(data.get(cast));
    }

    public static String generateUser(String name) throws JSONException {
        final JSONObject jo = newSyncableItem();
        jo.put("display_name", name);
        final URI uri = getContentUri(USER_PATH, mUserId);
        jo.put("uri", uri.getPath());

        data.put(uri, jo);

        mUserId++;

        return uri.toString();
    }

    private void generateData() {
        try {

            final String steve = generateUser("Steve P.");
            final String leo = generateUser("Leo G.");

            final String kitten = createCast("a kitten", steve);
            final String lemur = createCast("a lemur", leo);

            final String cuteThings = generateCollection("cute things", steve);

            addCastToCollection(lemur, cuteThings);
            addCastToCollection(kitten, cuteThings);

        } catch (final JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public HttpResponse get(String path) throws IOException, JSONException,
            NetworkProtocolException, ClientResponseException {

        final Object o = data.get(resolveUrl(path));

        final BasicHttpResponse hr = new BasicHttpResponse(HttpVersion.HTTP_1_1, o != null ? 200
                : 404, o != null ? "OK" : "NOT FOUND");

        final String resp = o != null ? o.toString() : "";
        hr.setEntity(new StringEntity(resp));

        hr.setHeader("Date", DateUtils.formatDate(new Date()));

        return hr;
    }

    @Override
    protected synchronized HttpResponse put(String path, String jsonString) throws IOException,
            NetworkProtocolException {
        return null;
    }

    @Override
    public synchronized HttpResponse post(String path, String jsonString) throws IOException,
            NetworkProtocolException {
        return null;
    }

	public static void clearData() {
		data.clear();
		mUserData.clear();
	}

}