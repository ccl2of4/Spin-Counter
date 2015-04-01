package groupn.spin_counter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements SpinCounter.SpinListener {
    // constant for identifying the dialog
    private static final int DIALOG_ALERT = 10;
    //user name
    private String mUsername;
    //stored data file
    private SharedPreferences mPrefs;
    //tracks if this is the first time the user has run the app
    private boolean mIsFirstTime;

    private Button mSpinButton;
    private TextView mSpinCountTextView;
    private int mCurrentNumberOfSpins;

    private SpinCounter mSpinCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button scoreBoardButton = (Button)findViewById (R.id.scoreboard_button);
        scoreBoardButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScoreBoardActivity.class));
                overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
            }
        });

        Button nfcButton = (Button)findViewById (R.id.nfc_button);
        nfcButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NFCBrawlActivity.class));
                overridePendingTransition(R.anim.push_right_in,R.anim.push_right_out);
            }
        });

        mSpinCounter = new SpinCounter(this);
        mSpinCounter.registerListener(this);
        mSpinButton = (Button)findViewById(R.id.start_spin);
        mSpinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpinSession();
            }
        });

        mSpinCountTextView = (TextView)findViewById(R.id.spin_count);
        mCurrentNumberOfSpins = 0;

        mPrefs = getSharedPreferences("sc_prefs", MODE_PRIVATE);
        mIsFirstTime = mPrefs.getBoolean("mIsFirstTime", true);
        mUsername = mPrefs.getString("mUsername", "New User");
        if(mIsFirstTime) {
            Log.d(mUsername, "First time running the app");
            showDialog(DIALOG_ALERT);
        }
        else
            Log.d("Username = ", mUsername);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
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

    private void startSpinSession() {
        mSpinButton.setVisibility(View.GONE);
        mSpinCountTextView.setVisibility((View.VISIBLE));
        mSpinCountTextView.setRotation(0.0f);
        mSpinCounter.prep();
        mSpinCountTextView.setText("3");
        new CountDownTimer(3100, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mSpinCountTextView.setText(Long.toString(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                mSpinCountTextView.setText(R.string.go);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSpinCounter.start();
                    }
                }, 250);
            }
        }.start();
    }

    @Override
    public void onUpdate(float totalDegrees) {
        mCurrentNumberOfSpins = Math.abs((int)(totalDegrees/360.0f));
        mSpinCountTextView.setText(Integer.toString(mCurrentNumberOfSpins));
        mSpinCountTextView.setRotation(-totalDegrees);
    }

    @Override
    public void done() {
        mSpinCounter.stop();
        mSpinButton.setVisibility(View.VISIBLE);
        mSpinCountTextView.setVisibility((View.GONE));
    }

    private final class CancelOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
        }
    }
}
