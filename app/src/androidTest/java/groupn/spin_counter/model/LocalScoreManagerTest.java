package groupn.spin_counter.model;

import android.test.AndroidTestCase;
import android.util.Log;

/**
 * Created by connor on 3/28/15.
 */
public class LocalScoreManagerTest extends AndroidTestCase {

    private LocalScoreManager mLocalScoreManager;
    private static final String TEST_DATA_FILE_NAME = "LocalScoreManagerTestDataFile";

    @Override
    public void setUp () {
        mLocalScoreManager = new LocalScoreManager ();
        mLocalScoreManager.setDataFileName (TEST_DATA_FILE_NAME);
        mLocalScoreManager.setContext (getContext ());
        mLocalScoreManager.clearData ();
    }

    public void testPreconditions () {
        assertEquals (0, mLocalScoreManager.getAllUsers().size ());
    }

    //
    // -reportGame
    //
    public void testReportGame1 () {
        mLocalScoreManager.reportGame ("Connor", 1, true);
        mLocalScoreManager.reportGame ("Connor", 2, false);

        assertEquals(1, mLocalScoreManager.getAllUsers().size());
    }

    public void testReportGame2 () {
        mLocalScoreManager.reportGame ("Connor", 1, true);
        mLocalScoreManager.reportGame ("Jack", 15, false);

        assertEquals(2, mLocalScoreManager.getAllUsers().size());
    }

    public void testReportGame3 () {
        mLocalScoreManager.reportGame("Connor", 4, true);

        assertEquals(1, mLocalScoreManager.getGamesPlayed("Connor"));
    }

    public void testReportGame4 () {
        mLocalScoreManager.reportGame ("Connor", 4, true);
        mLocalScoreManager.reportGame ("Connor", 4, true);

        assertEquals (2, mLocalScoreManager.getGamesPlayed ("Connor"));
    }

    public void testReportGame5 () {
        mLocalScoreManager.reportGame ("Connor", 4, true);
        mLocalScoreManager.reportGame ("Connor", 4, true);

        assertEquals (2, mLocalScoreManager.getGamesWon ("Connor"));
    }

    public void testReportGame6 () {
        mLocalScoreManager.reportGame ("Connor", 7, true);
        mLocalScoreManager.reportGame ("Connor", 4, true);

        assertEquals (7, mLocalScoreManager.getMostSpins("Connor"));
    }

    public void testReportGame7 () {
        mLocalScoreManager.reportGame ("Connor", 5, true);
        mLocalScoreManager.reportGame ("Connor", 12, true);

        assertEquals (12, mLocalScoreManager.getMostSpins ("Connor"));
    }

    //
    // -reportSpins
    //

    public void testReportSpins1 () {
        mLocalScoreManager.reportSpins ("Connor", 14);
        mLocalScoreManager.reportSpins ("Connor", 12);

        assertEquals (1, mLocalScoreManager.getAllUsers().size ());
    }

    public void testReportSpins2 () {
        mLocalScoreManager.reportSpins ("Connor", 14);
        mLocalScoreManager.reportSpins ("Sarah", 12);

        assertEquals (2, mLocalScoreManager.getAllUsers().size ());
    }

    public void testReportSpins3 () {
        mLocalScoreManager.reportSpins ("Connor", 14);
        mLocalScoreManager.reportSpins ("Sarah", 12);
        mLocalScoreManager.reportSpins ("Connor", 12);

        assertEquals (2, mLocalScoreManager.getAllUsers().size ());
    }

    public void testReportSpins4 () {
        mLocalScoreManager.reportSpins("Connor", 1);

        assertEquals (1, mLocalScoreManager.getMostSpins ("Connor"));
    }

    public void testReportSpins5 () {
        mLocalScoreManager.reportSpins ("Connor", 12);
        mLocalScoreManager.reportSpins ("Connor", 14);

        assertEquals (14, mLocalScoreManager.getMostSpins ("Connor"));
    }

    public void testReportSpins6 () {
        mLocalScoreManager.reportSpins ("Connor", 14);
        mLocalScoreManager.reportSpins ("Connor", 12);

        assertEquals (14, mLocalScoreManager.getMostSpins ("Connor"));
    }

    public void testReportSpins7 () {
        mLocalScoreManager.reportSpins ("Connor", 14);
        mLocalScoreManager.reportSpins ("Connor", 12);

        assertEquals (0, mLocalScoreManager.getGamesPlayed("Connor"));
    }
}
