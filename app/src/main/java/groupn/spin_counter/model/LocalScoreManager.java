package groupn.spin_counter.model;

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

    // ============
    //
    // Constants
    //
    // ============

    private static final String MostSpinsKey = "most spins key";
    private static final String GamesPlayedKey = "games played key";
    private static final String GamesWonKey = "games won key";

    // TODO figure out what file path we can write to
    private static final String DataFilePath = "";

    // ============
    //
    // i-vars
    //
    // ============

    private Map<String,Map<String,Object>> data;

    {
        data = retrieveData ();
        if (data == null) {
            data = new HashMap<String,Map<String,Object>> ();
        }
    }

    // ============
    //
    // Override
    //
    // ============

    @Override
    public void reportSpins (String user, int spins) {
        if (updateSpins (user, spins)) {
            storeData (data);
        }
    }

    @Override
    public void reportGame (String user, int spins, boolean won) {
        if (updateSpins (user, spins) || updateGames (user, won)) {
            storeData(data);
        }
    }

    @Override
    public Set<String> getAllUsers () {
        return data.keySet ();
    }

    @Override
    public int getMostSpins (String user) {
        return (Integer)data.get(user).get (MostSpinsKey);
    }

    @Override
    public int getGamesPlayed (String user) {
        return (Integer)data.get(user).get (GamesPlayedKey);
    }

    @Override
    public int getGamesWon (String user) {
        return (Integer)data.get(user).get (GamesWonKey);
    }

    // ============
    //
    // Private
    //
    // ============

    /**
     *
     * @param user
     * @param won
     * @return true if data was modified as a result of the call, false otherwise
     */
    private boolean updateGames (String user, boolean won) {
        Map<String,Object> userInfo = data.get (user);

        int gamesPlayed = (Integer)userInfo.get (GamesPlayedKey);
        int gamesWon = (Integer)userInfo.get (GamesWonKey);

        gamesPlayed += 1;
        gamesWon += won ? 1 : 0;

        userInfo.put (GamesPlayedKey, gamesPlayed);
        userInfo.put (GamesWonKey, gamesWon);

        return true;
    }

    /**
     *
     * @param user
     * @param spins
     * @return true if data was modified as a result of the call, false otherwise
     */
    private boolean updateSpins (String user, int spins) {
        Map<String,Object> userInfo = data.get (user);
        int mostSpins = (Integer)userInfo.get (MostSpinsKey);
        if (spins > mostSpins) {
            userInfo.put (MostSpinsKey, spins);
            return true;
        }
        return false;
    }

    /**
     *
     * @param data the map to be stored in the file at DataFilePath
     */
    private static void storeData (Map data) {

        try {
            FileOutputStream fileOut = new FileOutputStream(DataFilePath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(data);
            out.close();
            fileOut.close();

        // TODO error handling
        } catch(IOException i) {
        }

    }

    /**
     *
     * @return the map created from the file at DataFilePath
     */
    private static Map retrieveData () {

        Map result = null;

        try {
            FileInputStream fileIn = new FileInputStream(DataFilePath);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Object obj = in.readObject ();
            result = (Map) obj;
            in.close();
            fileIn.close();

        // TODO error handling
        } catch(IOException i) {
        } catch(ClassNotFoundException c) {
        }

        return result;
    }
}
