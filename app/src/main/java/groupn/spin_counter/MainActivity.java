package groupn.spin_counter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import groupn.spin_counter.model.DataRepository;
import groupn.spin_counter.model.User;
import groupn.spin_counter.view.SpinnerView;


public class MainActivity extends ActionBarActivity implements SpinCounter.SpinListener, SensorEventListener {

    // constants for identifying the dialog
    private static final int DIALOG_NO_USERNAME = 0;
    private static final int DIALOG_NO_USERNAME_NETWORK_ERROR = 1;
    private static final int DIALOG_NO_USERNAME_USERNAME_TAKEN = 2;
    private static final int DIALOG_CHANGE_USERNAME = 3;
    private static final int DIALOG_CHANGE_USERNAME_NETWORK_ERROR = 4;
    private static final int DIALOG_CHANGE_USERNAME_USERNAME_TAKEN = 5;

    //stored data file
    private SharedPreferences mPrefs;
    //tracks if this is the first time the user has run the app

    private User mUser;

    private boolean mHasSpun;

    private SpinnerView mSpinnerView;

    private TextView mScore;
    private TextView mHighscore;

    private int mCurrentNumberOfSpins;

    private SpinCounter mSpinCounter;
    private boolean mInSpinSession;
    private boolean mInCountdown;
    private DataRepository mDataRepository;

    private GestureDetector mGestureDetector;

    private final String TAG = "MainActivity";
    private Typeface font;

    private Handler mTimeChecker;
    private boolean mIsTiming;
    private Runnable mStopSession;

    private Button mNfcButton, mScoreBoardButton;
    private ImageButton mMuteButton;

    private SoundPool mSounds;
    private boolean mIsMuted;
    private int[] mSoundIds;
    private int[] mPlayingIds;

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

        mPrefs = getSharedPreferences("sc_prefs", MODE_PRIVATE);
        mHasSpun = mPrefs.getBoolean("mHasSpun", false);
        mIsMuted = mPrefs.getBoolean("mIsMuted", false);

        font = Typeface.createFromAsset(getAssets(), "fonts/orangejuice.otf");

        // =============
        //
        // View setup
        //
        // =============

        getSupportActionBar().setDisplayShowTitleEnabled(false);
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
                updateMuted(!mIsMuted);
            }
        });
        updateMuted(mIsMuted);

        // spinnerview
        mSpinnerView = makeSpinnerView ();
        mSpinnerView.setCountdownListener(mCountdownListener);
        ((RelativeLayout)findViewById(R.id.main)).addView(mSpinnerView);


        // score/high score
        mScore = (TextView)findViewById(R.id.score);
        mHighscore = (TextView)findViewById(R.id.highscore);

        // screen setup
        if(findViewById(R.id.main).getTag().equals("large_screen")){
            TextView title = (TextView)findViewById (R.id.textView);
            title.setTypeface(font);
        }
        if(findViewById(R.id.main).getTag().equals("tablet_screen")){
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                return;
            }
        }

        // user info
        mDataRepository.getUserInfo(new DataRepository.Callback<User> () {
            // user exists in DB
            @Override
            public void success(User user) {
                mUser = user;
                changedLogin();
                finishedLaunching();
            }

            // user doesn't exist in DB. we need to create an entry by asking the user for a
            // username
            @Override
            public void failure(boolean networkError) {
                if (networkError) {
                    Log.d (TAG, "network error finding user in DB. exiting.");
                    System.exit(1);
                }
                else {
                    showDialog(DIALOG_NO_USERNAME);
                }
            }
        });

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
    }

    private void finishedLaunching() {
        findSensors();
    }

    private void changedLogin() {
        updateHighScore();
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

    private void updateMuted(boolean muted) {
        mIsMuted = muted;
        if (mIsMuted) {
            mSounds.autoPause();
            mMuteButton.setImageResource(R.drawable.mute);
        } else {
            mMuteButton.setImageResource(R.drawable.unmute);
        }
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putBoolean("mIsMuted", mIsMuted);
        ed.apply();
    }

    private void updateHighScore () {
        if (mUser != null) {
            mHighscore.setText ("Your Highscore: " + mUser.maxSpins);
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
    public void onResume(){
        super.onResume();
        mScore.setVisibility(View.GONE);
    }

    @Override
    protected Dialog onCreateDialog(final int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).
                setCancelable(true);
        final EditText input = new EditText(this);
        input.setTextColor(Color.parseColor("#ffffffff"));
        String usernameHint = (mUser == null) ? "" : mUser.username;
        input.setHint(usernameHint);
        input.setHintTextColor(Color.GRAY);
        builder.setView(input);
        String acceptButtonTitle = null;

        switch (id) {
            case DIALOG_NO_USERNAME: {
                builder.setMessage(R.string.no_username);
                acceptButtonTitle = getString (R.string.no_username_accept_button);
                break;
            }
            case DIALOG_NO_USERNAME_USERNAME_TAKEN : {
                builder.setMessage(R.string.no_username_username_taken);
                acceptButtonTitle = getString (R.string.no_username_accept_button);
                break;
            }
            case DIALOG_NO_USERNAME_NETWORK_ERROR : {
                builder.setMessage(R.string.no_username_network_error);
                acceptButtonTitle = getString (R.string.no_username_accept_button);
                break;
            }
            case DIALOG_CHANGE_USERNAME : {
                builder.setMessage(R.string.change_username);
                acceptButtonTitle = getString (R.string.change_username_accept_button);
                builder.setNegativeButton(R.string.username_cancel_button, new CancelOnClickListener());
                break;
            }
            case DIALOG_CHANGE_USERNAME_USERNAME_TAKEN : {
                builder.setMessage(R.string.change_username_username_taken);
                acceptButtonTitle = getString (R.string.change_username_accept_button);
                builder.setNegativeButton(R.string.username_cancel_button, new CancelOnClickListener());
                break;
            }
            case DIALOG_CHANGE_USERNAME_NETWORK_ERROR : {
                builder.setMessage(R.string.change_username_network_error);
                acceptButtonTitle = getString (R.string.change_username_accept_button);
                builder.setNegativeButton(R.string.username_cancel_button, new CancelOnClickListener());
                break;
            }
        }

        final List<Integer> noUsernameCases = Arrays.asList(
                DIALOG_NO_USERNAME,
                DIALOG_NO_USERNAME_USERNAME_TAKEN,
                DIALOG_NO_USERNAME_NETWORK_ERROR);

        final List<Integer> changeUsernameCases = Arrays.asList(
                 DIALOG_CHANGE_USERNAME,
                DIALOG_CHANGE_USERNAME_USERNAME_TAKEN,
                DIALOG_CHANGE_USERNAME_NETWORK_ERROR);

        if (noUsernameCases.contains(id)) {
            builder.setPositiveButton(acceptButtonTitle, new NoUsernameClickListener(input));
        } else if (changeUsernameCases.contains(id)) {
            builder.setPositiveButton(acceptButtonTitle, new ChangeUsernameClickListener(input));
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        return super.onCreateDialog(id);
    }

    private class ChangeUsernameClickListener implements DialogInterface.OnClickListener {
        private EditText mInput;
        public ChangeUsernameClickListener (EditText input) {
            mInput = input;
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            mDataRepository.changeUsername(mInput.getText().toString(), new DataRepository.Callback<User>() {
                @Override
                public void success(User result) {
                    mUser = result;
                    changedLogin();
                }

                @Override
                public void failure(boolean networkError) {
                    if (networkError) {
                        showDialog(DIALOG_CHANGE_USERNAME_NETWORK_ERROR);
                    } else {
                        showDialog(DIALOG_CHANGE_USERNAME_USERNAME_TAKEN);
                    }
                }
            });
        }
    }

    private class NoUsernameClickListener implements DialogInterface.OnClickListener {
        private EditText mInput;
        public NoUsernameClickListener (EditText input) {
            mInput = input;
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            mDataRepository.registerUsername(mInput.getText().toString(), new DataRepository.Callback<User>() {
                @Override
                public void success(User result) {
                    mUser = result;
                    finishedLaunching();
                    changedLogin();
                }

                @Override
                public void failure(boolean networkError) {
                    if (networkError) {
                        showDialog(DIALOG_NO_USERNAME_NETWORK_ERROR);
                    } else {
                        showDialog(DIALOG_NO_USERNAME_USERNAME_TAKEN);
                    }
                }
            });
        }
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

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);*/
        switch (id) {
            case R.id.change_username:
                showDialog(DIALOG_CHANGE_USERNAME);
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
            if (!mIsMuted) {
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
        if(!mHasSpun || mUser.maxSpins < mCurrentNumberOfSpins) {
            Log.d(TAG,"NEW HIGHSCORE: " + mCurrentNumberOfSpins);
            mHighscore.setVisibility(View.VISIBLE);
            mHighscore.setText("Your Highscore: " + mCurrentNumberOfSpins);
        }
        else{
            mHighscore.setVisibility(View.VISIBLE);
        }
        if (mInSpinSession) {
            Log.d (TAG, "reporting spins");
            mDataRepository.reportSpins(mCurrentNumberOfSpins);
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
        mScore.setText("Score: " + mCurrentNumberOfSpins);
        if(!mHasSpun){
            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putBoolean("mHasSpun", true);
            ed.apply();
            mHasSpun = true;
        }
        mCurrentNumberOfSpins = 0;
    }

    private final SpinnerView.CountdownListener mCountdownListener = new SpinnerView.CountdownListener() {
        @Override
        public void countdownStarted() {
            if (!mIsMuted) {
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
            mHighscore.setVisibility(View.GONE);
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
