package edu.mit.mobile.android.locast.example.sync;

import android.accounts.Account;
import android.net.Uri;

import com.stackoverflow.ArrayUtils;

import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.locast.data.CastMedia;
import edu.mit.mobile.android.locast.data.SyncException;
import edu.mit.mobile.android.locast.example.accounts.Authenticator;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.sync.AbsMediaSync;

public class MediaSyncService extends AbsMediaSync {

    private String[] mCastMediaProjection;

    @Override
    public void enqueueUnpublishedMedia() throws SyncException {

        enqueueUnpublishedMedia(Cast.CAST_MEDIA.getAll(Cast.CONTENT_URI));

    }

    @Override
    public String[] getCastMediaProjection() {
        if (mCastMediaProjection == null) {
            mCastMediaProjection = ArrayUtils
                    .concat(new String[] { '"' + edu.mit.mobile.android.locast.example.data.CastMedia.CAST + '"' },
                            super.getCastMediaProjection());
        }
        return mCastMediaProjection;

    }

    @Override
    public boolean getKeepOffline(Uri castMediaUri, CastMedia castMedia) {
        return false;
    }

    @Override
    public Account getAccount() {
        return Authenticator.getFirstAccount(this, Authenticator.ACCOUNT_TYPE);
    }

    @Override
    public Uri getTitledItemForCastMedia(Uri castMedia) {
        // XXX this is inspecting the URL. Maybe generalize
        return ProviderUtils.removeLastPathSegments(castMedia, 2);
    }

}
