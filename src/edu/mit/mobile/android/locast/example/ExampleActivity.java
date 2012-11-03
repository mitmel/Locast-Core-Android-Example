package edu.mit.mobile.android.locast.example;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

import edu.mit.mobile.android.locast.data.Authorable;
import edu.mit.mobile.android.locast.example.accounts.Authenticator;
import edu.mit.mobile.android.locast.example.app.CastListFragment;
import edu.mit.mobile.android.locast.example.app.CollectionListFragment;
import edu.mit.mobile.android.locast.example.app.NoAccountFragment;
import edu.mit.mobile.android.locast.example.data.Cast;
import edu.mit.mobile.android.locast.example.data.Collection;

public class ExampleActivity extends SherlockFragmentActivity implements TabListener {

    private boolean mIsLoggedIn;

    private static final String TAG_SPLASH = "splash";
    private static final String TAG_NEW = "new";
    private static final String TAG_COLLECTIONS = "collections";
    private static final String TAG_NEARBY = "nearby";
    private static final String TAG_UNPUBLISHED = "unpublished";
    private static final String TAG_MY = "my";

    private static final String INSTANCE_CURRENT_TAB = "edu.mit.mobile.android.locast.example.INSTANCE_CURRENT_TAB";

    private static final int TAB_NONE_SELECTED = ActionBar.Tab.INVALID_POSITION;
    private int mRestoreTab = TAB_NONE_SELECTED;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        if (savedInstanceState != null) {
            mRestoreTab = savedInstanceState.getInt(INSTANCE_CURRENT_TAB, TAB_NONE_SELECTED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        showSplashOrMain();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        final Tab t = getSupportActionBar().getSelectedTab();
        if (t != null) {
            outState.putInt(INSTANCE_CURRENT_TAB, t.getPosition());
        }
    }

    /**
     * Check to see if there's an account and shows either the splash screen or the main screen.
     * It's safe to call this even if the appropriate fragment is already showing - it'll just leave
     * it alone.
     */
    private void showSplashOrMain() {
        mIsLoggedIn = Authenticator.hasRealAccount(this, Authenticator.ACCOUNT_TYPE);

        if (mIsLoggedIn) {
            showMainScreen();
        } else {
            showSplash();
        }
        invalidateOptionsMenu();
    }

    /**
     * Replaces the current fragment with the splash screen. Removes any tabs.
     */
    private void showSplash() {
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        final Fragment f = fm.findFragmentById(android.R.id.content);

        if (f == null || !(f instanceof NoAccountFragment)) {
            final NoAccountFragment f2 = new NoAccountFragment();

            ft.replace(android.R.id.content, f2, TAG_SPLASH);
            ft.commit();
        }

        final ActionBar actionBar = getSupportActionBar();
        if (ActionBar.NAVIGATION_MODE_STANDARD != actionBar.getNavigationMode()) {
            actionBar.removeAllTabs();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
    }

    /**
     * Replaces the current fragment with the main interface.
     */
    private void showMainScreen() {

        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.findFragmentById(android.R.id.content);

        if (f != null && f instanceof NoAccountFragment) {
            final FragmentTransaction ft = fm.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            ft.remove(f);
            ft.commit();
        }

        final ActionBar actionBar = getSupportActionBar();
        if (ActionBar.NAVIGATION_MODE_TABS != actionBar.getNavigationMode()) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab().setText(R.string.main_tab_whats_new)
                    .setTabListener(this).setTag(TAG_NEW));
            actionBar.addTab(actionBar.newTab().setText(R.string.main_tab_collections)
                    .setTabListener(this).setTag(TAG_COLLECTIONS));

            if (mRestoreTab != TAB_NONE_SELECTED) {
                actionBar.setSelectedNavigationItem(mRestoreTab);
            }
        }
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        final FragmentManager fm = getSupportFragmentManager();

        Fragment f = fm.findFragmentById(android.R.id.content);

        final String tag = (String) tab.getTag();

        if (f != null && !tag.equals(f.getTag())) {
                ft.detach(f);
            f = null;
        }

        if (f == null) {
            f = fm.findFragmentByTag(tag);
        }

        if (f != null) {
            ft.attach(f);
        } else {
            ft.add(android.R.id.content, instantiateFragment(tag), tag);
        }
    }

    /**
     * Given a tag, creates a new fragment with the default arguments.
     *
     * @param tag
     * @return
     */
    private Fragment instantiateFragment(String tag) {
        Fragment f;
        if (TAG_MY.equals(tag)) {
            f = CastListFragment.instantiate(Authorable
                    .getAuthoredBy(Cast.CONTENT_URI,
                            Authenticator.getUserUri(this, Authenticator.ACCOUNT_TYPE)).buildUpon()
                    .appendQueryParameter(Cast.COL_DRAFT + "!", "1").build());

        } else if (TAG_NEW.equals(tag)) {
            f = CastListFragment.instantiate(Cast.CONTENT_URI.buildUpon()
                    .appendQueryParameter(Cast.COL_DRAFT + "!", "1").build());

        } else if (TAG_UNPUBLISHED.equals(tag)) {
            f = CastListFragment.instantiate(Cast.CONTENT_URI.buildUpon()
                    .appendQueryParameter(Cast.COL_DRAFT, "1").build());

        } else if (TAG_COLLECTIONS.equals(tag)) {
            f = CollectionListFragment.instantiate(Collection.CONTENT_URI.buildUpon()
                    .appendQueryParameter(Collection.COL_DRAFT + "!", "1").build());

        } else {
            throw new IllegalArgumentException("cannot instantiate fragment for tag " + tag);
        }
        return f;
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        final FragmentManager fm = getSupportFragmentManager();
        final String tag = (String) tab.getTag();
        final Fragment f = fm.findFragmentByTag(tag);
        if (f != null) {
            ft.detach(f);
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_example, menu);
        return true;
    }

}
