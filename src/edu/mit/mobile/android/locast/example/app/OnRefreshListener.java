package edu.mit.mobile.android.locast.example.app;

/**
 * Implement this to allow the system to refresh the content of the given item.
 * 
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 * 
 */
public interface OnRefreshListener {

    /**
     * Called when the user requests a refresh of the content.
     */
    public void onRefresh();
}
