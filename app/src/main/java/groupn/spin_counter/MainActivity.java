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
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
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
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import groupn.spin_counter.model.ScoreManager;
import groupn.spin_counter.view.SpinnerView;


public class MainActivity extends ActionBarActivity implements SpinCounter.SpinListener, SensorEventListener {
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
    private boolean mInSpinSession;
    private boolean mInCountdown;
    private ScoreManager mScoreManager;

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
        mScoreManager = ScoreManager.getInstance(ScoreManager.Type.Local);
        mScoreManager.setContext(getApplicationContext());

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
        mIsFirstTime = mPrefs.getBoolean("mIsFirstTime", true);
        mUsername = mPrefs.getString("mUsername", "New User");
        mIsMuted = mPrefs.getBoolean("mIsMuted", false);
        mUser = mUsername;

        mGestureDetector = new GestureDetector(this, new GestureListener());

        font = Typeface.createFromAsset(getAssets(), "fonts/orangejuice.otf");

        // =============
        //
        // View setup
        //
        // =============

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

        mMuteButton = (ImageButton)findViewById(R.id.mute_button);
        mMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsMuted = !mIsMuted;
                if (mIsMuted) {
                    mSounds.autoPause();
                    mMuteButton.setImageResource(R.drawable.mute);
                } else {
                    mMuteButton.setImageResource(R.drawable.unmute);
                }

                SharedPreferences.Editor ed = mPrefs.edit();
                ed.putBoolean("mIsMuted",mIsMuted);
                ed.apply();
            }
        });

        if (mIsMuted) {
            mSounds.autoPause();
            mMuteButton.setImageResource(R.drawable.mute);
        } else {
            mMuteButton.setImageResource(R.drawable.unmute);
        }

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
        if(findViewById(R.id.main).getTag().equals("tablet_screen")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }
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
            if (!mInSpinSession) {
                if (mInCountdown) {
                    done();
                }
                Log.d("SWIPE", "" + GestureListener.swipeDirection);
                if (GestureListener.swipeDirection == 1) {
                    Log.d("SWIPED", "RIGHT");
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
        if (mInSpinSession) {
            mScoreManager.reportSpins(mUsername,mCurrentNumberOfSpins);
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
