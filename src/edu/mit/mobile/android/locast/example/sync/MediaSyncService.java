package edu.mit.mobile.android.locast.example.sync;

import android.accounts.Account;
import android.net.Uri;
import edu.mit.mobile.android.locast.data.CastMedia;
import edu.mit.mobile.android.locast.data.SyncException;
import edu.mit.mobile.android.locast.sync.AbsMediaSync;

public class MediaSyncService extends AbsMediaSync {

    @Override
    public void enqueueUnpublishedMedia() throws SyncException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean getKeepOffline(Uri castMediaUri, CastMedia castMedia) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account getAccount() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri getTitledItemForCastMedia(Uri castMedia) {
        // TODO Auto-generated method stub
        return null;
    }

}
