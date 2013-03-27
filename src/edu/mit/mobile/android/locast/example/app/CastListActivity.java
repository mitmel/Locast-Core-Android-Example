package edu.mit.mobile.android.locast.example.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import edu.mit.mobile.android.locast.example.R;

public class CastListActivity extends SherlockFragmentActivity {

    protected static final String ARG_CASTS = "casts";

    private Uri mCasts;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setContentView(R.layout.activity_cast_list);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            mCasts = getIntent().getData();

            final FragmentManager fm = getSupportFragmentManager();

            final Fragment castList = fm.findFragmentById(R.id.casts);

            final FragmentTransaction ft = fm.beginTransaction();

            if (castList != null) {
                ft.attach(castList);
            } else {
                ft.add(R.id.casts, CastListFragment.instantiate(mCasts));
            }

            ft.commit();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.activity_collection_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cast_new:
                createNewCast();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createNewCast() {
        startActivity(new Intent(Intent.ACTION_INSERT, mCasts));
    }
}
