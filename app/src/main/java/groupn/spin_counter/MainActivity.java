package groupn.spin_counter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


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
    private float mOffset;
    private boolean mOnce;

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
            }
        });

        Button nfcButton = (Button)findViewById (R.id.nfc_button);
        nfcButton.setOnClickListener (new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NFCBrawlActivity.class));
            }
        });

        mSpinCounter = new SpinCounter(this);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Please enter a username");
                builder.setCancelable(true);
                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                builder.setView(input);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SharedPreferences.Editor ed = mPrefs.edit();
                        ed.putBoolean("mIsFirstTime",false);
                        mIsFirstTime = false;
                        String value = input.getText().toString();
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
        mSpinCounter.prep();
        mOnce = true;
        mSpinCountTextView.setText("3");
        new CountDownTimer(3100, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished <= 2000) {
                    if (mOnce) {
                        mOffset = mSpinCounter.getLastDeg();
                        mOnce = false;
                    }
                }
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
    public void onSensorUpdate(float azimuth) {
        float adjAzimuth = azimuth + mOffset;
        if (adjAzimuth >= 360.0f) {
            adjAzimuth -= 360.0f;
        }
        if (adjAzimuth < 180.0f) {
            mSpinCountTextView.setRotation(adjAzimuth + 180.0f);
        } else {
            mSpinCountTextView.setRotation(adjAzimuth - 180.0f);
        }
    }

    @Override
    public void onFullRotation() {
        mCurrentNumberOfSpins++;
        mSpinCountTextView.setText(Integer.toString(mCurrentNumberOfSpins));
    }

    @Override
    public void done() {
        mSpinCounter.stop();
    }

    private final class CancelOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
        }
    }
}
