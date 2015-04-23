package groupn.spin_counter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import groupn.spin_counter.model.ScoreManager;
import groupn.spin_counter.view.SpinnerView;


public class MainActivity extends ActionBarActivity implements SpinCounter.SpinListener {
    // constant for identifying the dialog
    private static final int DIALOG_ALERT = 10;
    //user name
    private String mUsername;
    private static String mUser;
    //stored data file
    private SharedPreferences mPrefs;
    //tracks if this is the first time the user has run the app
    private boolean mIsFirstTime;

    private SpinnerView mSpinnerView;

    private int mCurrentNumberOfSpins;

    private SpinCounter mSpinCounter;
    private ScoreManager mScoreManager;

    private GestureDetector mGestureDetector;

    private final String TAG = "MainActivity";
    private Typeface font;

    private Handler mTimeChecker;
    private boolean mIsTiming;
    private Runnable mStopSession;

    // constants
    private static final int DISQUALIFICATION = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // =============
        //
        // Model setup
        //
        // =============
        mScoreManager = ScoreManager.getInstance(ScoreManager.Type.Local);
        mScoreManager.setContext(getApplicationContext());

        mSpinCounter = new SpinCounter(this);
        mSpinCounter.registerListener(this);

        mTimeChecker = new Handler();
        mIsTiming = false;

        mCurrentNumberOfSpins = 0;

        mPrefs = getSharedPreferences("sc_prefs", MODE_PRIVATE);
        mIsFirstTime = mPrefs.getBoolean("mIsFirstTime", true);
        mUsername = mPrefs.getString("mUsername", "New User");
        mUser = mUsername;

        mGestureDetector = new GestureDetector(this, new GestureListener());

        font = Typeface.createFromAsset(getAssets(), "fonts/orangejuice.otf");

        // =============
        //
        // View setup
        //
        // =============

        Button scoreBoardButton = (Button)findViewById (R.id.scoreboard_button);
        scoreBoardButton.setTypeface(font);
        scoreBoardButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScoreBoardActivity.class));
                overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
            }
        });

        Button nfcButton = (Button)findViewById (R.id.nfc_button);
        nfcButton.setTypeface(font);
        nfcButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BluetoothBrawlActivity.class));
                overridePendingTransition(R.anim.push_right_in,R.anim.push_right_out);
            }
        });

        mSpinnerView = makeSpinnerView ();
        mSpinnerView.setCountdownListener(mCountdownListener);
        ((RelativeLayout)findViewById(R.id.main)).addView(mSpinnerView);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // prompt for username entry
        if(mIsFirstTime) {
            Log.d(mUsername, "First time running the app");
            showDialog(DIALOG_ALERT);
        }
        else
            Log.d("Username = ", mUsername);

        if(findViewById(R.id.main).getTag().equals("large_screen")){
            TextView title = (TextView)findViewById (R.id.textView);
            title.setTypeface(font);
        }
    }

    private SpinnerView makeSpinnerView () {
        SpinnerView result = new SpinnerView (this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule (RelativeLayout.CENTER_HORIZONTAL);
        params.addRule (RelativeLayout.CENTER_VERTICAL);
        result.setLayoutParams (params);
        return result;
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
                startActivity(new Intent(MainActivity.this, BluetoothBrawlActivity.class));
                overridePendingTransition(R.anim.push_right_in,R.anim.push_right_out);
            }
            else if(GestureListener.swipeDirection == 0){
                Log.d("SWIPED", "LEFT");
                startActivity(new Intent(MainActivity.this, ScoreBoardActivity.class));
                overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
            }
            return true;
        }
        else
            return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mSpinCounter.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);*/
        switch (id) {
            case R.id.change_username:
                showDialog(DIALOG_ALERT);
                return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ALERT:
                AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).
                setMessage("Please enter a username").
                setCancelable(true);
                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                input.setTextColor(Color.parseColor("#ffffffff"));
                Log.d("USERNAME HINT", mUsername);
                input.setHint(mUsername);
                input.setHintTextColor(Color.GRAY);
                builder.setView(input);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SharedPreferences.Editor ed = mPrefs.edit();
                        ed.putBoolean("mIsFirstTime",false);
                        mIsFirstTime = false;
                        String value = input.getText().toString();
                        if(value.length() == 0)
                            value = mPrefs.getString("mUsername", "New User");
                        mUsername = value;
                        mUser = mUsername;
                        ed.putString("mUsername", value);
                        Log.d("VALUE", value);
                        ed.apply();
                        Log.d("SAVING ", mIsFirstTime+" "+mUsername);
                        return;
                    }
                });
                builder.setNegativeButton("Nope", new CancelOnClickListener());
                AlertDialog dialog = builder.create();
                dialog.show();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onUpdate(float totalDegrees) {
        int newSpins = Math.abs((int)(totalDegrees/360.0f));
        if (newSpins <= mCurrentNumberOfSpins) {
            Log.d(TAG, "TRUE NEWSPINS " + newSpins + " oldspins: " + mCurrentNumberOfSpins );
            if (!mIsTiming) {
                mIsTiming = true;
                mStopSession = new Runnable() {
                    @Override
                    public void run() {
                        mIsTiming = false;
                        done();
                    }
                };
                mTimeChecker.postDelayed(mStopSession, DISQUALIFICATION);
            }
        } else
        {Log.d(TAG, "FALSE NEWSPINS " + newSpins + " oldspins: "+ mCurrentNumberOfSpins );
            if (mIsTiming) {
                mTimeChecker.removeCallbacks(mStopSession);
                mIsTiming = false;
            }
            mCurrentNumberOfSpins = newSpins;
            mSpinnerView.setNumberOfSpins(mCurrentNumberOfSpins);
            mSpinnerView.setRotation(-totalDegrees);
        }
    }

    @Override
    public void done() {
        mSpinCounter.stop();
        mSpinnerView.reset();
        mSpinnerView.setRotation(0);

        mScoreManager.reportSpins(mUsername,mCurrentNumberOfSpins);
    }

    private final SpinnerView.CountdownListener mCountdownListener = new SpinnerView.CountdownListener() {
        @Override
        public void countdownStarted() {
            mSpinCounter.prep();
        }
        @Override
        public void countdownFinished() {
            mSpinCounter.start();
        }
    };

    private final class CancelOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
        }
    }
}
