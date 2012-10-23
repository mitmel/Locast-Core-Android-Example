package edu.mit.mobile.android.locast.example.sync;

import java.io.IOException;

import org.apache.http.client.HttpResponseException;
import org.json.JSONException;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import edu.mit.mobile.android.locast.data.NoPublicPath;
import edu.mit.mobile.android.locast.data.SyncException;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.example.data.LocastProvider;
import edu.mit.mobile.android.locast.net.NetworkProtocolException;
import edu.mit.mobile.android.locast.sync.AbsLocastAccountSyncService;
import edu.mit.mobile.android.locast.sync.SyncEngine;

public class SyncService extends AbsLocastAccountSyncService {

    @Override
    public void syncDefaultItems(SyncEngine syncEngine, Account account, Bundle extras,
            ContentProviderClient provider, SyncResult syncResult) throws HttpResponseException,
            RemoteException, SyncException, JSONException, IOException, NetworkProtocolException,
            NoPublicPath, OperationApplicationException, InterruptedException {
        syncEngine.sync(Cast.CONTENT_URI, account, extras, provider, syncResult);

    }

    @Override
    public String getAuthority() {

        return LocastProvider.AUTHORITY;
    }

}
