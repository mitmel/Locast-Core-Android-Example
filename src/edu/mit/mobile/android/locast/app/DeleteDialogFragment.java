package edu.mit.mobile.android.locast.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.misti.R;
import edu.mit.mobile.android.locast.sync.LocastSyncService;

/**
 * <p>
 * A delete confirmation dialog that can delete any JsonSyncableItem.
 * </p>
 *
 * <p>
 * To use, call {@link #newInstance(Uri, CharSequence)} with the uri of the item you wish to delete
 * as well as the message to display when prompting the user to delete the item.
 * </p>
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 *
 */
public class DeleteDialogFragment extends DialogFragment implements OnClickListener {

    private static final String ARG_ITEM_URI = "uri";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_TITLE = "title";
    private static final String TAG = DeleteDialogFragment.class.getSimpleName();

    private OnDeleteListener mOnDeleteListener;

    private Uri mItem;
    private CharSequence mMessage;
    private CharSequence mTitle;

    public static DeleteDialogFragment newInstance(Uri item, CharSequence title,
            CharSequence message) {
        final DeleteDialogFragment f = new DeleteDialogFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_ITEM_URI, item);
        args.putCharSequence(ARG_TITLE, title);
        args.putCharSequence(ARG_MESSAGE, message);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        mMessage = args.getCharSequence(ARG_MESSAGE);
        mTitle = args.getCharSequence(ARG_TITLE);
        final Uri item = args.getParcelable(ARG_ITEM_URI);
        mItem = item;

        final String type = getActivity().getContentResolver().getType(mItem);
        if (type == null || !type.startsWith(ProviderUtils.TYPE_ITEM_PREFIX)) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Cannot handle the requested content type", Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOnDeleteListener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_launcher)
                .setTitle(mTitle).setPositiveButton(R.string.delete, this).setMessage(mMessage)
                .setNegativeButton(android.R.string.cancel, this).setCancelable(true).create();
    }

    public void registerOnDeleteListener(OnDeleteListener listener) {
        mOnDeleteListener = listener;
    }

    public void unregisterOnDeleteListener(OnDeleteListener listener) {
        mOnDeleteListener = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE: {
                final int count = JsonSyncableItem.markDeleted(getActivity().getContentResolver(),
                        mItem, true, null, null);
                dialog.dismiss();
                if (count >= 1) {
                    LocastSyncService.startSync(getActivity(), mItem, true);
                } else {
                    Log.w(TAG, "Failed to delete  " + mItem + ". Count from markDeleted() was "
                            + count);
                }
                if (mOnDeleteListener != null) {
                    mOnDeleteListener.onDelete(mItem, count >= 1); // it should only ever be 1,
                                                                   // but...
                }
            }
                break;

            case AlertDialog.BUTTON_NEGATIVE:
                dialog.cancel();
                if (mOnDeleteListener != null) {
                    mOnDeleteListener.onDelete(mItem, false);
                }
                break;
        }

    }

    public static interface OnDeleteListener {
        public void onDelete(Uri item, boolean deleted);
    }
}
