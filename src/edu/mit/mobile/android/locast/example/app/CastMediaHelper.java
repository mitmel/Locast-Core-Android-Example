package edu.mit.mobile.android.locast.example.app;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.stackoverflow.MediaUtils;

import edu.mit.mobile.android.locast.data.Authorable;
import edu.mit.mobile.android.locast.data.CastMedia;
import edu.mit.mobile.android.locast.data.CastMedia.CastMediaInfo;
import edu.mit.mobile.android.locast.data.MediaProcessingException;
import edu.mit.mobile.android.locast.example.accounts.Authenticator;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.sync.LocastSyncService;

/**
 * A helper to make it easy to add media to a cast. This provides {@link #takePicture()} and
 * {@link #takeVideo()} which will fire off the appropriate intents needed to record a picture
 * and/or video. To use, hook into your activity's lifecycle as described in
 * {@link #CastMediaHelper(Activity, Bundle, Uri)}.
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 *
 */
public class CastMediaHelper {

    private static final String TAG = CastMediaHelper.class.getSimpleName();

    private static final int REQUEST_NEW_PHOTO = 100;

    private static final int REQUEST_NEW_VIDEO = 101;

    private static final int REQUEST_PICK_MEDIA = 102;

    private static final String PUBLIC_LOCAST_DIR = "locast";

    private static final String INSTANCE_CREATED_MEDIA_FILE = "edu.mit.mobile.android.locast.example.INSTANCE_CREATED_MEDIA_FILE";
    private static final String INSTANCE_CAST_LOCATION = "edu.mit.mobile.android.locast.example.INSTANCE_CAST_LOCATION";

    private final Activity mContext;

    private Location mLocation;

    // stateful
    private File mCreatedMediaFile;

    /**
     * Hook this in by calling {@link #onSaveInstanceState(Bundle)} and
     * {@link #onActivityResult(int, int, Intent)} from your activity's equivalent methods.
     *
     * @param context
     * @param args
     *            arguments from the activity's onCreate()
     * @param cast
     */
    public CastMediaHelper(Activity context, Bundle args) {
        mContext = context;

        if (args != null) {
            mCreatedMediaFile = (File) args.getSerializable(INSTANCE_CREATED_MEDIA_FILE);
            mLocation = args.getParcelable(INSTANCE_CAST_LOCATION);
        }
    }

    protected void onSaveInstanceState(Bundle outState) {

        outState.putSerializable(INSTANCE_CREATED_MEDIA_FILE, mCreatedMediaFile);
        outState.putParcelable(INSTANCE_CAST_LOCATION, mLocation);
    }

    /**
     * Adds the provided media to the cast. If there's location information associated with this
     * media file and there is no current location set, it will be added. This will show a toast if
     * there is a problem.
     *
     * @param content
     */
    public void addMedia(Uri cast, Uri content) {
        final Uri castMedia = Cast.CAST_MEDIA.getUri(cast);

        CastMediaInfo cmi;
        try {
            final Account me = Authenticator.getFirstAccount(mContext, Authenticator.ACCOUNT_TYPE);
            final ContentValues cv = new ContentValues();

            Authorable.putAuthorInformation(mContext, me, cv);

            cmi = CastMedia.addMedia(mContext, me, castMedia, content, cv);
            // if the current location is null, infer it from the first media that's added.
            if (mLocation == null && cmi.location != null) {
                setLocation(cmi.location);
            }

            // bump cast so it'll be marked dirty
            mContext.getContentResolver().update(cast, new ContentValues(), null, null);

            LocastSyncService.startSync(mContext, cast, true);
        } catch (final MediaProcessingException e) {
            Log.e(TAG, "could not add media", e);
            // TODO translate
            Toast.makeText(mContext, "Unable to add media: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private File createNewMedia(String publicDir, String extension) throws IOException {
        // Create an image file name
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String mediaFileName = timeStamp + "_";
        final File outdir = getLocastDir(publicDir);
        outdir.mkdirs();
        final File image = File.createTempFile(mediaFileName, "." + extension, outdir);

        return image;
    }

    private File getLocastDir(String publicDir) {
        return new File(Environment.getExternalStoragePublicDirectory(publicDir), PUBLIC_LOCAST_DIR);
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    public void setLocation(GeoPoint location) {
        mLocation = new Location("manual");
    }

    public Location getLocation() {
        return mLocation;
    }

    /**
     * <p>
     * Takes a video, adding it to this helper's cast.
     * </p>
     * <p>
     * This uses {@link Activity#startActivityForResult(Intent, int)}, so make sure to call
     * {@link #onActivityResult(int, int, Intent)} from your activity.
     * </p>
     */
    public void takeVideo() {

        final Intent i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        try {
            mCreatedMediaFile = createNewMedia(Environment.DIRECTORY_MOVIES, "mp4");
            i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCreatedMediaFile));
            i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // high quality

            mContext.startActivityForResult(i, REQUEST_NEW_VIDEO);
        } catch (final IOException e) {
            Log.e(TAG, "error making temporary file", e);
        }
    }

    /**
     * <p>
     * Takes a picture, adding it to this helper's cast.
     * </p>
     * <p>
     * This uses {@link Activity#startActivityForResult(Intent, int)}, so make sure to call
     * {@link #onActivityResult(int, int, Intent)} from your activity.
     * </p>
     */
    public void takePicture() {
        Intent i2;
        try {

            i2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mCreatedMediaFile = createNewMedia(Environment.DIRECTORY_PICTURES, "jpeg");
            i2.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCreatedMediaFile));
            mContext.startActivityForResult(i2, REQUEST_NEW_PHOTO);
        } catch (final IOException e) {
            Log.e(TAG, "error making temporary file", e);
        }
    }

    /**
     * Call this from your activity's {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param cast
     *            the cast to which the media should be added
     * @param requestCode
     * @param resultCode
     * @param data
     * @return true if the result was handled by this class
     */
    protected boolean onActivityResult(Uri cast, int requestCode, int resultCode, Intent data) {

        if ((requestCode == REQUEST_NEW_PHOTO || requestCode == REQUEST_NEW_VIDEO || requestCode == REQUEST_PICK_MEDIA)
                && resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "media adding cancelled");
            mCreatedMediaFile = null;
            return true;
        }
        Uri mCreatedMediaUri = Uri.fromFile(mCreatedMediaFile);

        switch (requestCode) {
            case REQUEST_NEW_PHOTO:
                if (data != null) {
                    final Uri imageCaptureResult = MediaUtils.handleImageCaptureResult(mContext,
                            data);
                    if (imageCaptureResult != null) {
                        mCreatedMediaUri = imageCaptureResult;
                    }
                }
                addMedia(cast, mCreatedMediaUri);
                mCreatedMediaFile = null;
                return true;

            case REQUEST_NEW_VIDEO:
                if (data.getData() != null) {
                    mCreatedMediaUri = data.getData();
                    Log.d(TAG,
                            "got a URL from the onActivityResult from adding a video: "
                                    + data.getDataString());
                }

                addMedia(cast, mCreatedMediaUri);

                mCreatedMediaFile = null;
                return true;

            case REQUEST_PICK_MEDIA:
                final Uri media = data.getData();
                if (media != null) {
                    addMedia(cast, media);
                }
                return true;

            default:
                return false;
        }
    }
}
