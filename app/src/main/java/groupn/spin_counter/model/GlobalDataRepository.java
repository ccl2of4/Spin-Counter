package groupn.spin_counter.model;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by connor on 4/27/15.
 */

class GlobalDataRepository extends DataRepository {

    @Override
    public void getUserInfo(final Callback<User> callback) {
        getService().getUser(getMacAddress(), new retrofit.Callback<User>() {
            @Override
            public void success(User user, Response response) {
                callback.success(user);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error.getKind().equals(RetrofitError.Kind.NETWORK));
            }
        });
    }

    @Override
    public void registerUsername(String username, final Callback<User> callback) {
        getService().postSignup(getMacAddress(), username, new retrofit.Callback<User>() {
            @Override
            public void success(User result, Response response) {
                callback.success(result);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error.getKind().equals(RetrofitError.Kind.NETWORK));
            }
        });
    }

    @Override
    public void changeUsername(String username, final Callback<User> callback) {
        getService().postChangeUsername(getMacAddress(), username, new retrofit.Callback<User>() {
            @Override
            public void success(User result, Response response) {
                callback.success(result);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error.getKind().equals(RetrofitError.Kind.NETWORK));
            }
        });
    }

    @Override
    public void getLeaderboard(final Callback<List<User>> callback) {
        getService().getLeaderboard(new retrofit.Callback<List<User>>() {
            @Override
            public void success(List<User> users, Response response) {
                callback.success(users);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error.getKind().equals(RetrofitError.Kind.NETWORK));
            }
        });
    }

    @Override
    public void reportSpins(int spins) {
        mService.postSpin(getMacAddress(), spins, new retrofit.Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.d (TAG, "report success");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d (TAG, "report fail");
            }
        });
    }

    @Override
    public void reportGame(final User opponent, boolean won) {
        getUserInfo(new Callback<User>() {
            @Override
            public void success(User thisUser) {
                getService().postGame(thisUser.userId, opponent.userId, new retrofit.Callback<Response>() {
                    @Override
                    public void success(Response response, Response response2) {

                    }

                    @Override
                    public void failure(RetrofitError error) {

                    }
                });
            }

            @Override
            public void failure(boolean networkError) {
                // maybe this should be reported
            }
        });
    }

    String getMacAddress () {
        WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String macAddress = wInfo.getMacAddress();
        return macAddress;
    }

    GlobalService getService () {
        if (mService == null) {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint("http://166.78.0.158/api")
                    .build ();
            mService = restAdapter.create (GlobalService.class);
        }
        return mService;
    }

    private static String TAG = "GlobalDataRepository";
    GlobalService mService;
}
