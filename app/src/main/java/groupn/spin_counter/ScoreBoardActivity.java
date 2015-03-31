package groupn.spin_counter;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import groupn.spin_counter.model.ScoreManager;


public class ScoreBoardActivity extends ActionBarActivity {

    private static final String TAG = "ScoreBoardActivity";

    private TableLayout mTableLayout;
    private ScoreManager mScoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_board);

        mScoreManager = ScoreManager.getInstance (ScoreManager.Type.Local);
        mScoreManager.setContext (getApplicationContext ());

        mTableLayout = (TableLayout)findViewById (R.id.table_layout);

        populateTable ();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_score_board, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void populateTable () {
        Set<String> users = mScoreManager.getAllUsers ();

        for (String user : users) {
            TableRow tableRow = new TableRow (this);

            List<String> attributes = new LinkedList<String> ();
            attributes.add ( user );
            attributes.add ( Integer.toString (mScoreManager.getMostSpins (user)) );
            attributes.add ( Integer.toString (mScoreManager.getGamesPlayed (user)) );
            attributes.add ( Integer.toString (mScoreManager.getGamesWon (user)) );
            attributes.add ( Integer.toString (mScoreManager.getGamesLost (user)) );

            for (String attribute : attributes) {
                TextView textView = new TextView (this);
                textView.setText (attribute);
                tableRow.addView (textView);
            }

            mTableLayout.addView (tableRow);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in,R.anim.push_right_out);
    }
}
