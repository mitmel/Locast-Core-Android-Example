package edu.mit.mobile.android.locast.example.accounts;

import edu.mit.mobile.android.locast.accounts.AbsRegisterActivity;
import edu.mit.mobile.android.locast.example.R;

public class RegisterActivity extends AbsRegisterActivity {

    @Override
    protected CharSequence getAppName() {
        return getText(R.string.app_name);
    }
}
