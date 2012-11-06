package edu.mit.mobile.android.locast.example.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import edu.mit.mobile.android.locast.example.R;
import edu.mit.mobile.android.locast.example.data.Collection;

public class CollectionDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setContentView(R.layout.activty_collection_detail);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            final Uri collection = getIntent().getData();

            final FragmentManager fm = getSupportFragmentManager();

            final Fragment castList = fm.findFragmentById(R.id.casts);

            final FragmentTransaction ft = fm.beginTransaction();

            if (castList != null) {
                ft.attach(castList);
            } else {
                ft.add(R.id.casts,
                        CastListFragment.instantiate(Collection.CASTS.getUri(collection)));
            }

            ft.commit();
        } else {
            finish();

        }
    }
}
