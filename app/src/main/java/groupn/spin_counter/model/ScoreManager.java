package groupn.spin_counter.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by connor on 3/27/15.
 */
public abstract class ScoreManager {

    // =======================
    //
    // Obtaining an instance
    //
    // =======================

    public enum Type {
        Local,
        Global // not yet supported
    }

    /**
     *
     * @param type
     * @return the appropriate singleton for the required type
     */
    public final static ScoreManager getInstance (Type type) {
        ScoreManager scoreManager = instances.get (type);
        return scoreManager;
    }

    // ============
    //
    // Store
    //
    // ============

    /**
     *
     * @param user
     * @param spins the number of spins user achieved
     */
    public abstract void reportSpins (String user, int spins);

    /**
     *
     * @param user
     * @param spins the number of spins user achieved in the game
     * @param won true if the user won the game, false otherwise
     */
    public abstract void reportGame (String user, int spins, boolean won);


    // ============
    //
    // Retrieve
    //
    // ============

    /**
     *
     * @return a list of all users who have been added via -reportSpins or -reportGame
     */
    public abstract Set<String> getAllUsers ();

    /**
     *
     * @param user
     * @return the most spins user has ever achieved
     */
    public abstract int getMostSpins (String user);

    /**
     *
     * @param user
     * @return the number of games user has played
     */
    public abstract int getGamesPlayed (String user);

    /**
     *
     * @param user
     * @return the number of games user has won
     */
    public abstract int getGamesWon (String user);

    /**
     *
     * @param user
     * @return the number of games user has lost. Note: this is the same as (getGamesPlayed() - getGamesWon())
     */
    public final int getGamesList (String user) {
        return getGamesPlayed (user) - getGamesWon (user);
    }

    // ============
    //
    // Private
    //
    // ============

    private final static Map<Type,ScoreManager> instances = new HashMap<Type,ScoreManager> ();

    static {
        instances.put (Type.Local, new LocalScoreManager());
    }

}
