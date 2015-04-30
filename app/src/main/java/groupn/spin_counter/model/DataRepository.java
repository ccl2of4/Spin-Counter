package groupn.spin_counter.model;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by connor on 4/27/15.
 */

public abstract class DataRepository {

    // =======================
    //
    // Callbacks
    //
    // =======================

    public interface Callback<T> {
        public void success (T result);
        public void failure (boolean networkError);
    }


    // =======================
    //
    // Obtaining an instance
    //
    // =======================

    public enum Type {
        Local, // no longer supported
        Global
    }

    /**
     *
     * @param type
     * @param context an appropriate context (most like app context)
     * @return the appropriate singleton for the required type
     */
    public final static DataRepository getInstance (Type type, Context context) {
        DataRepository dataRepository = sInstances.get (type);
        dataRepository.setContext (context);
        return dataRepository;
    }

    // ============
    //
    // User Info
    //
    // ============

    /**
     *
     * @return the User instance representing the current user
     */
    public abstract void getUserInfo (Callback<User> callback);

    /**
     *
     * Registers a username and replaces the old username if one is present
     *
     * @param username the username wanted
     * @param callback success if the username was registered, failure otherwise (e.g. username is
     *                 already taken). callback carries new user data.
     */
    public abstract void registerUsername (String username, Callback<User> callback);

    /**
     *
     * @param username the new username
     * @param callback success if username was changed, failure otherwise (e.g. username is
     *                 already taken)
     */
    public abstract void changeUsername (String username, Callback<User> callback);

    // ============
    //
    // Retrieve (GET)
    //
    // ============

    public abstract void getLeaderboard (Callback<List<User>> callback);

    // ============
    //
    // Store (POST)
    //
    // ============

    /**
     *
     * @param spins the number of spins user achieved
     * @param callback completion handler
     */
    public abstract void reportSpins (int spins, Callback<Void> callback);

    /**
     *
     * Report a game. This will automatically report spins for both users as
     * well (using the data provided,)
     *
     * @param player1 either player (client or server)
     * @param player2 the other player
     * @param player1Spins number of spins for player1
     * @param player2Spins number of spins for player2
     * @param callback completion handler
     */
    public abstract void reportGame (User player1,
                                     User player2,
                                     int player1Spins,
                                     int player2Spins,
                                     Callback<Void> callback);


    /**
     *
     * @return the context used by the DataRepository. This probably needs to be set before calling
     * any methods
     */
    public final Context getContext () { return mContext; }
    private void setContext (Context context) { mContext = context; }

    // ============
    //
    // Private
    //
    // ============

    private final static Map<Type,DataRepository> sInstances = new HashMap<Type,DataRepository>();
    private Context mContext;

    static {
        sInstances.put (Type.Global, new GlobalDataRepository());
    }

}
