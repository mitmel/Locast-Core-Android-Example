package edu.mit.mobile.android.locast.example.app;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.stackoverflow.ArrayUtils;
import com.stackoverflow.MediaUtils;

import edu.mit.mobile.android.locast.data.CastMedia;
import edu.mit.mobile.android.locast.data.CastMedia.CastMediaInfo;
import edu.mit.mobile.android.locast.data.MediaProcessingException;
import edu.mit.mobile.android.locast.data.PrivatelyAuthorable;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.accounts.Authenticator;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.sync.LocastSyncService;

public class CastViewEditActivity extends LocatableItemMapActivity {

    public static final String TAG = CastViewEditActivity.class.getSimpleName();

    private static final String TAG_EDIT = Intent.ACTION_EDIT;
    private static final String TAG_DETAIL = Intent.ACTION_VIEW;

    private static final int REQUEST_NEW_PHOTO = 100;

    private static final int REQUEST_NEW_VIDEO = 101;

    private static final int REQUEST_PICK_MEDIA = 102;

    private static final String PUBLIC_LOCAST_DIR = "locast";

    private static final String INSTANCE_CREATED_MEDIA_FILE = "edu.mit.mobile.android.locast.example.INSTANCE_CREATED_MEDIA_FILE";

    private boolean mCanEdit;

    private String[] mProjection;

    private GeoPoint mLocation;

    // stateful
    private File mCreatedMediaFile;

    private static String[] PROJECTION = new String[] { Cast.COL_PRIVACY, Cast.COL_TITLE,
            Cast.COL_AUTHOR_URI, Cast.COL_AUTHOR };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getSupportMenuInflater().inflate(R.menu.activity_cast_view, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle args) {
        super.onCreate(args);

        if (args != null) {
            mCreatedMediaFile = (File) args.getSerializable(INSTANCE_CREATED_MEDIA_FILE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(INSTANCE_CREATED_MEDIA_FILE, mCreatedMediaFile);
    }

    @Override
    protected void onCastLoaded(Loader<Cursor> loader, Cursor c) {
        mCanEdit = PrivatelyAuthorable.canEdit(
                Authenticator.getUserUri(this, Authenticator.ACCOUNT_TYPE), c);
        setTitle(Cast.getTitle(this, c));
        getSupportActionBar().setSubtitle(c.getString(c.getColumnIndexOrThrow(Cast.COL_AUTHOR)));
        invalidateOptionsMenu();
    }

    @Override
    protected String[] getProjection() {
        if (mProjection == null) {
            mProjection = ArrayUtils.concat(super.getProjection(), PROJECTION);
        }
        return mProjection;
    }

    private void editCast() {
        loadContentFragment(new Intent(Intent.ACTION_EDIT, getIntent().getData()));
    }

    private void onLeaveCastEdit(boolean save) {
        final FragmentManager fm = getSupportFragmentManager();

        final CastEditFragment edit = (CastEditFragment) fm.findFragmentByTag(TAG_EDIT);
        if (edit != null) {
            if (save) {
                if (edit.save()) {
                    Toast.makeText(this, R.string.notice_saved, Toast.LENGTH_SHORT).show();
                    LocastSyncService.startSync(this, getIntent().getData(), true);
                } else {
                    Toast.makeText(this, R.string.err_saving_cast, Toast.LENGTH_LONG).show();
                }
            }
        }

        loadContentFragment(new Intent(Intent.ACTION_VIEW, getIntent().getData()));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.edit).setVisible(mCanEdit);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit:
                editCast();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected boolean onLoadContentFragment(Intent intent, FragmentManager fm,
            FragmentTransaction ft, Fragment current) {

        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            if (current == null || !(current instanceof CastDetailFragment)) {
                ft.replace(R.id.content, CastDetailFragment.getInstance(intent.getData()),
                        TAG_DETAIL);
            }
            return true;

        } else if (intent.getAction().equals(Intent.ACTION_EDIT)) {
            if (current == null || !(current instanceof CastEditFragment)) {
                ft.replace(R.id.content, CastEditFragment.getInstance(intent.getData()), TAG_EDIT);
            }
            final ActionMode am = startActionMode(new CastEditActionMode());
            am.setTitle(R.string.edit_cast);
            return true;
        } else {
            return false;
        }
    }

    public void addMedia(Uri content) {
        final Uri cast = getIntent().getData();
        final Uri castMedia = Cast.CAST_MEDIA.getUri(cast);

        CastMediaInfo cmi;
        try {
            cmi = CastMedia.addMedia(this,
                    Authenticator.getFirstAccount(this, Authenticator.ACCOUNT_TYPE), castMedia,
                    content, new ContentValues());
            // if the current location is null, infer it from the first media that's added.
            if (mLocation == null && cmi.location != null) {
                setLocation(cmi.location);
            }
        } catch (final MediaProcessingException e) {
            Log.e(TAG, "could not add media", e);
            Toast.makeText(this, "Unable to add media: " + e.getLocalizedMessage(),
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

    private void setLocation(GeoPoint location) {
        mLocation = location;
    }

    private boolean onButtonPressed(int id) {
        switch (id) {
            case R.id.new_photo:

                takePicture();
                return true;

            case R.id.new_video:
                takeVideo();
                return true;

            default:
                return false;
        }
    }

    private void takeVideo() {

        final Intent i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        try {
            mCreatedMediaFile = createNewMedia(Environment.DIRECTORY_MOVIES, "mp4");
        } catch (final IOException e) {
            Log.e(TAG, "error making temporary file", e);
        }
        i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCreatedMediaFile));
        i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // high quality

        startActivityForResult(i, REQUEST_NEW_VIDEO);

    }

    private void takePicture() {
        Intent i2;
        try {

            i2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mCreatedMediaFile = createNewMedia(Environment.DIRECTORY_PICTURES, "jpeg");
            i2.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCreatedMediaFile));
            startActivityForResult(i2, REQUEST_NEW_PHOTO);
        } catch (final IOException e) {
            Log.e(TAG, "error making temporary file", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_CANCELED) {
            Log.d(TAG, "media adding cancelled");
            mCreatedMediaFile = null;
            return;
        }
        Uri mCreatedMediaUri = Uri.fromFile(mCreatedMediaFile);

        switch (requestCode) {
            case REQUEST_NEW_PHOTO:
                if (data != null) {
                    final Uri imageCaptureResult = MediaUtils.handleImageCaptureResult(this, data);
                    if (imageCaptureResult != null) {
                        mCreatedMediaUri = imageCaptureResult;
                    }
                }
                addMedia(mCreatedMediaUri);
                mCreatedMediaFile = null;
                break;
            case REQUEST_NEW_VIDEO:
                if (data.getData() != null) {
                    mCreatedMediaUri = data.getData();
                    Log.d(TAG,
                            "got a URL from the onActivityResult from adding a video: "
                                    + data.getDataString());
                }

                addMedia(mCreatedMediaUri);

                mCreatedMediaFile = null;
                break;
            case REQUEST_PICK_MEDIA:
                final Uri media = data.getData();
                if (media != null) {
                    addMedia(media);
                }
                break;
        }
    }

    private final class CastEditActionMode implements ActionMode.Callback {

        private boolean mCanceled = false;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getSupportMenuInflater().inflate(R.menu.activity_cast_edit, menu);
            mCanceled = false;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.d(TAG, "action item clicked: " + mode + "; " + item);
            final int id = item.getItemId();
            switch (id) {
                case R.id.cancel:
                    mCanceled = true;
                    mode.finish();
                    return true;
                default:
                    return onButtonPressed(id);

            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            onLeaveCastEdit(!mCanceled);
        }
    }
}
