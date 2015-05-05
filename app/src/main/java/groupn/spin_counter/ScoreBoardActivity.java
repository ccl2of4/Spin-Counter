package groupn.spin_counter;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import groupn.spin_counter.model.DataRepository;
import groupn.spin_counter.model.User;


public class ScoreBoardActivity extends ActionBarActivity {

    private static final String TAG = "ScoreBoardActivity";

    private TableLayout mTableLayout;
    private DataRepository mDataRepository;
    private GestureDetector mGestureDetector;
    private Typeface font;
    private int black;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_board);

        mDataRepository = DataRepository.getInstance (DataRepository.Type.Global, getApplicationContext ());

        mTableLayout = (TableLayout)findViewById (R.id.table_layout);

        populateTable ();

        mGestureDetector = new GestureDetector(this, new GestureListener());
        if(findViewById(R.id.scoreboard).getTag().equals("tablet_screen")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }

        font = Typeface.createFromAsset(getAssets(), "fonts/orangejuice.otf");

        // customize action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        View view = LayoutInflater.from(this).inflate(R.layout.abs_layout_score_board, null);
        ((TextView)view.findViewById(R.id.mytext)).setTypeface(font);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);
        getSupportActionBar().setCustomView(view);
        black = Color.parseColor("#ff000000");
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

        return super.onOptionsItemSelected(item);
    }

    private void populateTable () {
        mDataRepository.getLeaderboard(new DataRepository.Callback<List<User>>() {
            @Override
            public void success(List<User> users) {
                //set font and color of first row
                ((TextView)findViewById(R.id.row1_col1)).setTypeface(font);
                ((TextView)findViewById(R.id.row1_col1)).setTextColor(black);
                ((TextView)findViewById(R.id.row1_col2)).setTypeface(font);
                ((TextView)findViewById(R.id.row1_col2)).setTextColor(black);
                ((TextView)findViewById(R.id.row1_col3)).setTypeface(font);
                ((TextView)findViewById(R.id.row1_col3)).setTextColor(black);
                ((TextView)findViewById(R.id.row1_col4)).setTypeface(font);
                ((TextView)findViewById(R.id.row1_col4)).setTextColor(black);
                ((TextView)findViewById(R.id.row1_col5)).setTypeface(font);
                ((TextView)findViewById(R.id.row1_col5)).setTextColor(black);
                ((TextView)findViewById(R.id.row1_col6)).setTypeface(font);
                ((TextView)findViewById(R.id.row1_col6)).setTextColor(black);
                for (User user : users) {
                    TableRow tableRow = new TableRow (ScoreBoardActivity.this);

                    List<String> attributes = new LinkedList<String> ();
                    attributes.add ( user.username );
                    attributes.add ( Integer.toString(user.maxSpins) );
                    attributes.add ( Integer.toString(user.getGamesPlayed()) );
                    attributes.add ( Integer.toString(user.gamesWon) );
                    attributes.add ( Integer.toString(user.gamesLost) );
                    attributes.add ( Integer.toString(user.gamesTied) );

                    for (String attribute : attributes) {
                        TextView textView = new TextView (ScoreBoardActivity.this);
                        textView.setTypeface(font);
                        textView.setTextColor(black);
                        textView.setText (attribute);
                        tableRow.addView (textView);
                    }

                    mTableLayout.addView (tableRow);
                }
            }

            @Override
            public void failure(boolean networkError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in,R.anim.push_right_out);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //Returns true if the GestureDetector.OnGestureListener consumed the event, else false.
        boolean eventConsumed=mGestureDetector.onTouchEvent(event);
        if (eventConsumed)
        {
            Log.d("SWIPE", ""+GestureListener.swipeDirection);
            if(GestureListener.swipeDirection == 1){
                Log.d("SWIPED", "RIGHT");
                startActivity(new Intent(ScoreBoardActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                overridePendingTransition(R.anim.push_right_in,R.anim.push_right_out);
            }
            else if(GestureListener.swipeDirection == 0){
                Log.d("SWIPED", "LEFT");
                //startActivity(new Intent(ScoreBoardActivity.this, BluetoothBrawlActivity.class));
                //overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
            }
            return true;
        }
        else
            return false;
    }
}
