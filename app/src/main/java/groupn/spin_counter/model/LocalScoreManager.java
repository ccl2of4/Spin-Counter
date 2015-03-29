package groupn.spin_counter.model;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by connor on 3/27/15.
 */
class LocalScoreManager extends ScoreManager {

    @Override
    public void reportSpins (String user, int spins) {
        if (updateSpins (user, spins)) {
            storeData (getData ());
        }
    }

    @Override
    public void reportGame (String user, int spins, boolean won) {
        //use the bitwise or to avoid short-circuit evaluation
        if (updateSpins (user, spins) | updateGames (user, won)) {
            storeData(getData ());
        }
    }

    @Override
    public Set<String> getAllUsers () {
        return getData().keySet();
    }

    @Override
    public int getMostSpins (String user) {
        return (Integer)getData().get(user).get (MOST_SPINS_KEY);
    }

    @Override
    public int getGamesPlayed (String user) {
        return (Integer)getData().get(user).get (GAMES_PLAYED_KEY);
    }

    @Override
    public int getGamesWon (String user) {
        return (Integer)getData().get(user).get (GAMES_WON_KEY);
    }

    /**
     * clear all score data
     */
    void clearData () {
        Map data = getData ();
        data.clear ();
        storeData (data);
    }

    /**
     *
     * @return the file name for persistent storage
     */
    String getDataFileName () {
        if (mDataFileName == null) {
            mDataFileName = DEFAULT_DATA_FILE_NAME;
        }
        return mDataFileName;
    }

    void setDataFileName (String dataFileName) {
        mDataFileName = dataFileName;
    }

    // ============
    //
    // Private
    //
    // ============

    private static final String MOST_SPINS_KEY = "most spins key";
    private static final String GAMES_PLAYED_KEY = "games played key";
    private static final String GAMES_WON_KEY = "games won key";
    private static final String DEFAULT_DATA_FILE_NAME = "scoreboard_data";

    // self-encapsulated fields
    private Map<String,Map<String,Object>> mData;
    private String mDataFileName;

    /**
     *
     * @param user
     * @param won
     * @return true if data was modified as a result of the call, false otherwise
     */
    private boolean updateGames (String user, boolean won) {
        boolean newUser = addUser (user);
        Map<String,Object> userInfo = getData().get (user);

        int gamesPlayed = (Integer)userInfo.get (GAMES_PLAYED_KEY);
        int gamesWon = (Integer)userInfo.get (GAMES_WON_KEY);

        gamesPlayed += 1;
        gamesWon += won ? 1 : 0;

        userInfo.put (GAMES_PLAYED_KEY, gamesPlayed);
        userInfo.put (GAMES_WON_KEY, gamesWon);

        // data is always changed
        return newUser || true;
    }

    /**
     *
     * @param user
     * @param spins
     * @return true if data was modified as a result of the call, false otherwise
     */
    private boolean updateSpins (String user, int spins) {
        boolean newUser = addUser (user);

        Map<String,Object> userInfo = getData().get(user);
        int mostSpins = (Integer)userInfo.get (MOST_SPINS_KEY);
        if (spins > mostSpins) {
            userInfo.put (MOST_SPINS_KEY, spins);
            return true;
        }

        return newUser;
    }

    /**
     *
     * @param user the user to be added
     * @return true if user was added, false otherwise (ex. user was added at an earlier date)
     */
    private boolean addUser (String user) {
        Map<String,Object> userInfo = getData().get(user);

        if (userInfo != null) {
            return false;
        }

        userInfo = new HashMap<String,Object> ();
        userInfo.put (MOST_SPINS_KEY, 0);
        userInfo.put (GAMES_PLAYED_KEY, 0);
        userInfo.put (GAMES_WON_KEY, 0);

        getData().put (user, userInfo);

        return true;
    }

    /**
     *
     * @param data the map to be stored in the file at DataFilePath
     */
    private void storeData (Map data) {

        try {
            File file = new File (getContext().getFilesDir(), getDataFileName());
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(data);
            out.close();
            fileOut.close();

        // TODO error handling
        } catch(IOException i) {
            throw new AssertionError ();
        }

    }

    /**
     *
     * @return the map created from the file at DataFilePath
     */
    private Map retrieveData () {

        Map result = null;

        try {
            File file = new File (getContext().getFilesDir(), getDataFileName());
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Object obj = in.readObject ();
            result = (Map) obj;
            in.close();
            fileIn.close();

        // TODO error handling
        } catch(IOException i) {
        } catch(ClassNotFoundException c) {
            throw new AssertionError ();
        }

        return result;
    }

    /**
     *
     * @return data, using lazy instantiation
     */
    private Map<String,Map<String,Object>> getData () {
        if (mData != null) {
            return mData;
        }

        mData = retrieveData ();
        if (mData == null) {
            mData = new HashMap<String,Map<String,Object>> ();
        }

        return mData;
    }
}
