package edu.mit.mobile.android.locast.example.accounts;

import android.accounts.Account;
import android.content.Intent;
import edu.mit.mobile.android.locast.accounts.AbsLocastAuthenticatorActivity;
import edu.mit.mobile.android.locast.nfftt.R;
import edu.mit.mobile.android.locast.example.data.LocastProvider;

public class AuthenticatorActivity extends AbsLocastAuthenticatorActivity {

    @Override
    protected CharSequence getAppName() {
        return getText(R.string.app_name);
    }

    @Override
    protected boolean isEmailAddressLogin() {
        return true;
    }

    @Override
    protected Account createAccount(String username) {
        return new Account(username, Authenticator.ACCOUNT_TYPE);
    }

    @Override
    protected String getAuthority() {
        return LocastProvider.AUTHORITY;
    }

    @Override
    protected Intent getSignupIntent() {
        return new Intent(this, RegisterActivity.class);
    }

    @Override
    protected String getAccountType() {
        return Authenticator.ACCOUNT_TYPE;
    }

    @Override
    protected String getAuthtokenType() {
        return Authenticator.ACCOUNT_TYPE;
    }
}
