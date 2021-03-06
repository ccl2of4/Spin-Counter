package groupn.spin_counter;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import groupn.spin_counter.model.User;

/**
 * Created by connor on 4/29/15.
 */
public class SpinCounterApplication extends Application {

    /* user global state */
    public User getUser () {
        return mUser;
    }
    public void setUser (User user) {
        mUser = user;
        savePreferences();
    }

    /* muted global state */
    public boolean isMuted () {
        return mMuted;
    }
    public void setMuted (boolean muted) {
        mMuted = muted;
        savePreferences();
    }

    /* server global state */
    public boolean ismServer () {
        return mServer;
    }
    public void setmServer (boolean server) {
        mServer = server;
        savePreferences();
    }

    /* first time global state */
    public boolean isFirstTime () {
        return mIsFirstTime;
    }
    public void setFirstTime (boolean t) {
        mIsFirstTime = t;
        savePreferences();
    }

    /* mode global state */
    public int getMode () {
        return mMode;
    }
    public void setMode (int m) {
        mMode = m;
        savePreferences();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        loadPreferences();
    }

    private void loadPreferences () {
        if (mPrefs == null) {
            mPrefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        }

        mMuted = mPrefs.getBoolean(MUTED_KEY, false);
        mUser = User.deserialize(mPrefs.getString(USER_KEY, null));
        mMode = mPrefs.getInt(MODE_KEY, 1);
    }

    private void savePreferences () {
        SharedPreferences.Editor e = mPrefs.edit();

        e.putBoolean(MUTED_KEY, mMuted);
        e.putString(USER_KEY, mUser.serialize());
        e.putBoolean(SERVER_KEY, mServer);
        e.putBoolean(FIRST_KEY,mIsFirstTime);
        e.putInt(MODE_KEY,mMode);

        e.apply();
    }

    // static members
    private static final String MUTED_KEY = "muted";
    private static final String USER_KEY = "user";
    private static final String PREFERENCES_NAME = "sc_prefs";
    private static final String TAG = "SpinCounterApplication";
    private static final String SERVER_KEY = "server";
    private static final String FIRST_KEY="firstTime";
    private static final String MODE_KEY="mode";

    // i-vars
    private User mUser;
    private boolean mMuted;
    private SharedPreferences mPrefs;
    private boolean mServer;
    private boolean mIsFirstTime;
    private int mMode;
}
