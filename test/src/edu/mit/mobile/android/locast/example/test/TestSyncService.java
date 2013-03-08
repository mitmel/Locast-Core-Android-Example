package edu.mit.mobile.android.locast.example.test;

import android.test.IsolatedContext;
import android.test.ServiceTestCase;
import android.test.mock.MockContentResolver;
import edu.mit.mobile.android.locast.example.sync.SyncService;

public class TestSyncService extends ServiceTestCase<SyncService> {

    public TestSyncService() {
        super(SyncService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final MockContentResolver mcr = new MockContentResolver();
        setContext(new IsolatedContext(mcr, getContext()));

    }

    public void testSync() {
        // startService(new Intent(getContext(), SyncService.class));
        // final SyncService svc = getService();
        //
        // assertNotNull(svc);

    }


}
