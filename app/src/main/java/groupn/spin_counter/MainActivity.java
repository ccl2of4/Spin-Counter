package groupn.spin_counter;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import groupn.spin_counter.model.DataRepository;
import groupn.spin_counter.model.User;
import groupn.spin_counter.view.SpinnerView;


public class MainActivity extends ActionBarActivity implements SpinCounter.SpinListener, SensorEventListener {

    private SpinnerView mSpinnerView;

    private TextView mScore;
    private TextView mHighScore;

    private int mCurrentNumberOfSpins;

    private SpinCounter mSpinCounter;
    private boolean mInSpinSession;
    private boolean mInCountdown;
    private DataRepository mDataRepository;

    private GestureDetector mGestureDetector;

    private Typeface font;

    private Handler mTimeChecker;
    private boolean mIsTiming;
    private Runnable mStopSession;

    private Button mNfcButton, mScoreBoardButton;
    private ImageButton mMuteButton;

    private SoundPool mSounds;
    private int[] mSoundIds;
    private int[] mPlayingIds;

    // constants
    private static final int DISQUALIFICATION = 2500;

    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // =============
        //
        // Model setup
        //
        // =============

        mDataRepository = DataRepository.getInstance(DataRepository.Type.Global, getApplicationContext ());

        mSpinCounter = new SpinCounter(this);
        mInSpinSession = false;
        mInCountdown = false;
        mSpinCounter.registerListener(this);

        mSounds = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        mSoundIds = new int[3];
        mPlayingIds = new int[3];
        mSoundIds[0] = mSounds.load(this, R.raw.countdown, 1);
        mSoundIds[1] = mSounds.load(this, R.raw.swoosh, 1);

        mTimeChecker = new Handler();
        mIsTiming = false;

        mCurrentNumberOfSpins = 0;

        font = Typeface.createFromAsset(getAssets(), "fonts/orangejuice.otf");

        // =============
        //
        // View setup
        //
        // =============

        // display username as actionbar title
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        mGestureDetector = new GestureDetector(this, new GestureListener());

        // fonts
        ((TextView)findViewById(R.id.ui_separator)).setTypeface(font);
        ((TextView)findViewById(R.id.highscore)).setTypeface(font);
        ((TextView)findViewById(R.id.score)).setTypeface(font);

        // score board button
        mScoreBoardButton = (Button)findViewById (R.id.scoreboard_button);
        mScoreBoardButton.setTypeface(font);
        mScoreBoardButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInSpinSession || mInCountdown) {
                    done();
                }
                startActivity(new Intent(MainActivity.this, ScoreBoardActivity.class));
                overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
            }
        });

        // bluetooth button
        mNfcButton = (Button)findViewById (R.id.nfc_button);
        mNfcButton.setTypeface(font);
        mNfcButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( BluetoothAdapter.getDefaultAdapter() != null) {
                    if (mInSpinSession || mInCountdown) {
                        done();
                    }
                    startActivity(new Intent(MainActivity.this, BluetoothBrawlActivity.class));
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                }
                else{
                    new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("No Bluetooth Detected")
                            .setMessage("This device doesn't have Bluetooth: 2-Player Brawling is disabled.")
                            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Do nothing
                                }
                            })
                            .show();
                }
            }
        });

        // mute button
        mMuteButton = (ImageButton)findViewById(R.id.mute_button);
        mMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean muted = getSpinCounterApplication().isMuted();
                getSpinCounterApplication().setMuted(!muted);
                updateMuted();
            }
        });

        // spinnerview
        mSpinnerView = makeSpinnerView ();
        mSpinnerView.setCountdownListener(mCountdownListener);
        ((RelativeLayout)findViewById(R.id.main)).addView(mSpinnerView);

        // score/high score
        mScore = (TextView)findViewById(R.id.score);
        mHighScore = (TextView)findViewById(R.id.highscore);

        // screen setup
        if(findViewById(R.id.main).getTag().equals("large_screen")){
            TextView title = (TextView)findViewById (R.id.textView);
            title.setTypeface(font);
        }

        // handles text scaling down for smaller screens
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.d("SCREEN SIZE", "WIDTH: " + width + " HEIGHT: "+ height);
        if(width <= 520){
            ((TextView) findViewById(R.id.ui_separator)).setText(R.string.dashed_line_short);
            ((TextView) findViewById(R.id.highscore)).setTextSize(35);
        }

        // rotate to landscape if necessary
        if(findViewById(R.id.main).getTag().equals("tablet_screen")){
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                //finish();
                return;
            }
        }

        findSensors();
        updateUI();
    }

    private SpinCounterApplication getSpinCounterApplication () {
        return (SpinCounterApplication)getApplication();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!checkForUserData()) {
            goToLoginActivity(LoginActivity.PURPOSE_FIND_USER);
            return;
        }

        // update the UI with the data that we have, then synchronize our data with
        // the server (as a sanity check/to ensure data integrity), then update the UI
        // again. if everything goes nicely, the high score will really only appear to be
        // updated once
        updateUI();
        mDataRepository.getUserInfo(new DataRepository.Callback<User>() {
            @Override
            public void success(User user) {
                getSpinCounterApplication().setUser(user);
                updateUI();
            }
            @Override
            public void failure(boolean networkError) {
                if (networkError) {
                    Toast.makeText(MainActivity.this, R.string.synchronize_failure, Toast.LENGTH_SHORT).show();
                }

                // this code will run if the server can't find your account (e.g. it was deleted)
                else {
                    goToLoginActivity(LoginActivity.PURPOSE_FIND_USER);
                }
            }
        });
    }

    private boolean checkForUserData () {
        return getSpinCounterApplication().getUser() != null;
    }

    private void goToLoginActivity (int purpose) {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.putExtra(LoginActivity.PURPOSE, purpose);
        startActivity(intent);
    }

    private void findSensors() {
        SensorManager s = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if(s.registerListener(this,
                s.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                s.SENSOR_DELAY_NORMAL)) {
            s.unregisterListener(this);
        } else {
            new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("No Gyroscope detected")
                    .setMessage("This device has no gyroscope. Your spin detection may be buggy or inaccurate")
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing
                        }
                    })
                    .show();
        }
    }

    private void updateUI () {
        updateHighScore();
        updateMuted();
        updateUsername();
    }

    private void updateMuted() {
        if (getSpinCounterApplication().isMuted()) {
            mSounds.autoPause();
            mMuteButton.setImageResource(R.drawable.mute);
        } else {
            mSounds.autoResume();
            mMuteButton.setImageResource(R.drawable.unmute);
        }
    }

    private void updateHighScore () {
        User user = getSpinCounterApplication().getUser();
        if (user != null) {
            mHighScore.setText("Your Highscore: " + user.maxSpins);
        }
    }

    private void updateUsername(){
        View view = LayoutInflater.from(this).inflate(R.layout.abs_layout, null);
        if(getSpinCounterApplication().getUser() != null)
            ((TextView)view.findViewById(R.id.mytext)).setText("  " + getSpinCounterApplication().getUser().username + " ");
        ((TextView)view.findViewById(R.id.mytext)).setTypeface(font);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);
        getSupportActionBar().setCustomView(view);
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
    public void onResume(){
        super.onResume();
        mScore.setVisibility(View.GONE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //Returns true if the GestureDetector.OnGestureListener consumed the event, else false.
        boolean eventConsumed=mGestureDetector.onTouchEvent(event);
        if (eventConsumed)
        {
            if (!mInSpinSession) {
                if (mInCountdown) {
                    done();
                }
                Log.d("SWIPE", "" + GestureListener.swipeDirection);
                if (GestureListener.swipeDirection == 1) {
                    Log.d("SWIPED", "RIGHT");
                    //error handling for emulators without bluetooth adapters
                    if (BluetoothAdapter.getDefaultAdapter() != null) {
                        startActivity(new Intent(MainActivity.this, BluetoothBrawlActivity.class));
                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    } else {
                        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("No Bluetooth Detected")
                                .setMessage("This device doesn't have Bluetooth: 2-Player Brawling is disabled.")
                                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Do nothing
                                    }
                                })
                                .show();
                    }
                } else if (GestureListener.swipeDirection == 0) {
                    Log.d("SWIPED", "LEFT");
                    startActivity(new Intent(MainActivity.this, ScoreBoardActivity.class));
                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                }
            }
            return true;
        }
        else
            return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mInSpinSession || mInCountdown) {
            done();
        }
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

        switch (id) {
            case R.id.change_username:
                goToLoginActivity(LoginActivity.PURPOSE_CHANGE_USERNAME);
                return true;
        }
        return false;
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
            if (!getSpinCounterApplication().isMuted()) {
                mPlayingIds[1] = mSounds.play(mSoundIds[1], 1, 1, 1, 0, 1.0f);
            }
            mCurrentNumberOfSpins = newSpins;
            mSpinnerView.setNumberOfSpins(mCurrentNumberOfSpins);
        }
        mSpinnerView.setRotation(-totalDegrees);
    }

    @Override
    public void done() {
        mSpinCounter.stop();
        if(getSpinCounterApplication().getUser().maxSpins < mCurrentNumberOfSpins) {
            mHighScore.setText("Your Highscore: " + mCurrentNumberOfSpins);
        }
        mHighScore.setVisibility(View.VISIBLE);
        if (mInSpinSession) {
            mDataRepository.reportSpins(mCurrentNumberOfSpins, new DataRepository.Callback<Void>() {
                @Override
                public void success(Void result) {}
                @Override
                public void failure(boolean networkError) {
                    Toast.makeText(MainActivity.this, R.string.synchronize_failure, Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (mInCountdown) {
            mSounds.stop(mPlayingIds[0]);
            mSpinnerView.cancel();
        }
        mInSpinSession = false;
        mInCountdown = false;
        mScoreBoardButton.setVisibility(View.VISIBLE);
        mNfcButton.setVisibility(View.VISIBLE);
        mSpinnerView.reset();
        mSpinnerView.setRotation(0);

        mScore.setVisibility(View.VISIBLE);
        Log.d(TAG,"SETTING SCORE " + mCurrentNumberOfSpins);
        if (mIsTiming) {
            Log.d(TAG,"CANCEL TIMER");
            mTimeChecker.removeCallbacks(mStopSession);
            mIsTiming = false;
        }

        mScore.setText("Score: " + mCurrentNumberOfSpins);

        mCurrentNumberOfSpins = 0;
    }

    private final SpinnerView.CountdownListener mCountdownListener = new SpinnerView.CountdownListener() {
        @Override
        public void countdownStarted() {
            if (!getSpinCounterApplication().isMuted()) {
                mPlayingIds[0] = mSounds.play(mSoundIds[0], 1, 1, 1, 0, 1.0f);
            }
            mInCountdown = true;
            mSpinCounter.prep();
        }
        @Override
        public void countdownFinished() {
            mSpinCounter.start();
            mInCountdown = false;
            mInSpinSession = true;
            mScoreBoardButton.setVisibility(View.GONE);
            mNfcButton.setVisibility(View.GONE);
            mScore.setVisibility(View.GONE);
            mHighScore.setVisibility(View.GONE);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Do nothing
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Do nothing
    }

    private final class CancelOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
        }
    }
}
