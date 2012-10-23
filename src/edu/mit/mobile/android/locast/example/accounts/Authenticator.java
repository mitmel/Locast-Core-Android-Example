package edu.mit.mobile.android.locast.example.accounts;

import android.content.Context;
import android.content.Intent;
import edu.mit.mobile.android.locast.accounts.AbsLocastAuthenticator;
import edu.mit.mobile.android.locast.example.data.LocastProvider;

public class Authenticator extends AbsLocastAuthenticator {

    public static final String ACCOUNT_TYPE = LocastProvider.AUTHORITY;

    public Authenticator(Context context) {
        super(context);
    }

    @Override
    public Intent getAuthenticator(Context context) {
        return new Intent(context, AuthenticatorActivity.class);
    }

    @Override
    public String getAccountType() {
        return ACCOUNT_TYPE;
    }

    @Override
    public String getAuthTokenType() {

        return ACCOUNT_TYPE;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

}
