package groupn.spin_counter;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import org.w3c.dom.Text;

import java.util.List;

import groupn.spin_counter.R;
import groupn.spin_counter.bluetooth.BluetoothService;
import groupn.spin_counter.bluetooth.Constants;
import groupn.spin_counter.bluetooth.DeviceListActivity;
import groupn.spin_counter.model.DataRepository;
import groupn.spin_counter.model.User;
import groupn.spin_counter.view.SpinnerView;

public class BluetoothBrawlActivity extends ActionBarActivity {

    private static final String TAG = "BluetoothBrawlActivity";
    private boolean isServer;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    private SpinCounter mSpinCounter;
    private SpinnerView mSpinnerView;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the communication services
     */
    private BluetoothService mBluetoothService = null;

    public String mSavedBluetoothAdapterName;
    private SharedPreferences mPrefs;

    //start of message codes
    private static String START_CODE = "1:";
    private static String DONE_CODE = "2:";
    private static String USER_CODE = "3:";
    private static String MODE_CODE = "4:";

    //score result variables
    private int mEnemyScore = -1;
    private int mMyScore = -1;

    private String mUsername;
    private GestureDetector mGestureDetector;
    private Typeface font;
    private int mDiscoverable;

    private DataRepository mDataRepository;

    // constant for identifying the dialog
    private static final int DIALOG_ALERT = 11;

    private User mEnemy;

    private ShowcaseView sv;
    private Handler mTimeChecker;
    private boolean mIsTiming;
    private Runnable mStopSession;
    private int mMode;
    private static final int lvl2 = 10000;
    private static final int lvl3 = 5000;
    private static final int lvl4 = 2500;
    private static final int lvl5 = 1500;
    private boolean myTurn;
    private int whichView;
    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcbrawl);

        // hide activity name on actionbar
        //getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);

        //instantiate score manager
        mDataRepository = DataRepository.getInstance(DataRepository.Type.Global, getApplicationContext());

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mSavedBluetoothAdapterName = mBluetoothAdapter.getName();
        mPrefs = getSharedPreferences("sc_prefs", MODE_PRIVATE);
        Log.d(TAG, getSpinCounterApplication().getUser().username);
        mBluetoothAdapter.setName(getSpinCounterApplication().getUser().username);
        mUsername = mBluetoothAdapter.getName();
        Log.d("USERNAME", mUsername);

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        mDiscoverable = mPrefs.getInt("mDiscoverable",300);

        mSpinnerView = makeSpinnerView ();
        mSpinnerView.setCountdownListener (mCountdownListener);
        RelativeLayout layout = (RelativeLayout)findViewById (R.id.bluetooth_main_layout);
        layout.addView (mSpinnerView);
        mSpinnerView.setEnabled(false);

        mSpinCounter = new SpinCounter (this);
        mSpinCounter.registerListener (mSpinListener);
        isServer=true;
        getSpinCounterApplication().setmServer(true);
        Log.d(TAG,"isServer");

        mTimeChecker = new Handler();
        mIsTiming = false;

        mGestureDetector = new GestureDetector(this, new GestureListener());

        font = Typeface.createFromAsset(getAssets(), "fonts/orangejuice.otf");
        TextView nfcButton = (TextView)findViewById (R.id.textView);
        nfcButton.setTypeface(font);
        ((TextView)findViewById (R.id.you_score)).setTypeface(font);
        ((TextView)findViewById (R.id.them_score)).setTypeface(font);

        // handles text scaling down for smaller screens
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.d("SCREEN SIZE", "WIDTH: " + width + " HEIGHT: "+ height);
        if(width < 540){
            ((TextView) findViewById(R.id.textView)).setTextSize(40);
        }
        SharedPreferences settings = getSharedPreferences("mprefs", 0);
        if (settings.getBoolean("firstTimeBT", true)) {
            tutorial();
            settings.edit().putBoolean("firstTimeBT", false).commit();
        }

        mMode = getSpinCounterApplication().getMode();
        updateModeIcon();
        myTurn = false;
    }

    private void tutorial(){
        final Target bt = new ViewTarget(R.id.anchor2, this);
        whichView = 0;
        sv = new ShowcaseView.Builder(this, false)
                .setTarget(Target.NONE)
                .setContentTitle(R.string.main_bluetooth)
                .setContentText(R.string.bt_text2)
                .setStyle(5)
                .build();
        sv.setButtonText("OK.");
        if (findViewById(R.id.bluetooth).getTag().equals("tablet_screen") || findViewById(R.id.bluetooth).getTag().equals("large_screen")) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            sv.setButtonPosition(params);
        }
        sv.overrideButtonClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(whichView==0) {
                    sv.setShowcase(bt, true);
                    sv.setContentTitle(getString(R.string.main_bluetooth));
                    sv.setContentText(getString(R.string.bt_text3));
                }
                if(whichView==2) {
                    sv.hide();
                }
                whichView++;
            }
        });
    }

    public void updateModeIcon(){
        view = LayoutInflater.from(this).inflate(R.layout.abs_layout_bt, null);
        ImageView imageView = (ImageView)view.findViewById(R.id.lvl_icon);

        if(mMode == 1)
            imageView.setImageResource(R.drawable.lvl1);
        else if(mMode ==lvl2)
            imageView.setImageResource(R.drawable.lvl2);
        else if(mMode ==lvl3)
            imageView.setImageResource(R.drawable.lvl3);
        else if(mMode ==lvl4)
            imageView.setImageResource(R.drawable.lvl4);
        else if(mMode ==lvl5)
            imageView.setImageResource(R.drawable.lvl5);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isServer & !mIsTiming & myTurn)
                    pickMode();
                else
                    Toast.makeText(BluetoothBrawlActivity.this, R.string.cant_pick, Toast.LENGTH_SHORT).show();
            }
        });
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);
        getSupportActionBar().setCustomView(view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bluetooth_chat, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mBluetoothService == null) {
            setupChat();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.setName(mSavedBluetoothAdapterName);
        Log.d(TAG, "BluetoothBrawlActivity Destroying");
        disconnect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                //isServer = false;
                //Log.d(TAG,"isClient");
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                //mSpinnerView.setEnabled(true);
                return true;
            }
            case R.id.discoverable: {
                isServer = true;
                //mSpinnerView.setEnabled(false);
                Log.d(TAG,"isServer");
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.discoverable_length:
                showDialog(DIALOG_ALERT);
                return true;
            case R.id.tutorial2:
                tutorial();
                return true;
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
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
                //startActivity(new Intent(BluetoothBrawlActivity.this, ScoreBoardActivity.class));
                //overridePendingTransition(R.anim.push_right_in,R.anim.push_right_out);
            }
            else if(GestureListener.swipeDirection == 0){
                Log.d("SWIPED", "LEFT");
                startActivity(new Intent(BluetoothBrawlActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
            }
            return true;
        }
        else
            return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //isServer=true;
        overridePendingTransition(R.anim.push_left_in,R.anim.push_left_out);
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBluetoothService.connect(device);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {

        // Initialize the BluetoothService to perform bluetooth connections
        mBluetoothService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }


    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, mDiscoverable);
            startActivity(discoverableIntent);
        }
    }


    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = this;
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = this;
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            isServer = getSpinCounterApplication().ismServer();
                            if(!isServer) {
                                Log.d(TAG, "isClient");
                                myTurn = true;
                                pickMode();
                                mSpinnerView.setEnabled(true);
                            }
                            sendUser();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;

                // user just made a move
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;

                // opponent just made a move
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    interpretMessage(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(BluetoothBrawlActivity.this, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    if(msg.getData().getString(Constants.TOAST).equals("Unable to connect device") || msg.getData().getString(Constants.TOAST).equals("Device connection was lost")){
                        Log.d(TAG,"isServer");
                        isServer=true;
                        mSpinnerView.setEnabled(false);
                        //onBackPressed();
                        disconnect();
                    }
                    Toast.makeText(BluetoothBrawlActivity.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void pickMode(){
        AlertDialog.Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        b.setTitle("Pick a difficulty for your game!");
        String[] types = {"Too Easy", "Casual", "Just Right", "Too Pro", "Literally Impossible"};
        b.setItems(types, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                switch(which){
                    case 0:
                        mMode = 1;
                        sendMessage(MODE_CODE+mMode);
                        updateModeIcon();
                        break;
                    case 1:
                        mMode = lvl2;
                        sendMessage(MODE_CODE+mMode);
                        updateModeIcon();
                        break;
                    case 2:
                        mMode = lvl3;
                        sendMessage(MODE_CODE+mMode);
                        updateModeIcon();
                        break;
                    case 3:
                        mMode = lvl4;
                        sendMessage(MODE_CODE+mMode);
                        updateModeIcon();
                        break;
                    case 4:
                        mMode = lvl5;
                        sendMessage(MODE_CODE+mMode);
                        updateModeIcon();
                        break;
                }
            }

        });

        b.show();
    }
    private void sendUser(){
        sendMessage(USER_CODE + getSpinCounterApplication().getUser().serialize());
    }
    private void interpretMessage(String msg){
        if(msg.startsWith(START_CODE)){
            Log.d(TAG, "Starting");
            mSpinnerView.setEnabled(true);
            myTurn = true;
            Toast.makeText(BluetoothBrawlActivity.this, "YOUR TURN! GO!", Toast.LENGTH_SHORT).show();
        }
        else if(msg.startsWith(DONE_CODE)){
            Log.d(TAG,"Done");
            mEnemyScore = Integer.parseInt(msg.substring(2));
            Toast.makeText(BluetoothBrawlActivity.this, "Their Score was: " + mEnemyScore, Toast.LENGTH_SHORT).show();
            if(!isServer){
                mSpinnerView.setEnabled(true);
                sendMessage(DONE_CODE + mMyScore);
                myTurn = true;
            }
            reportResult();
        }
        else if(msg.startsWith(USER_CODE)){
            Log.d(TAG,"Enemy User");
            mEnemy = User.deserialize(msg.substring(2));
            Log.d(TAG, mEnemy.username + " " + mEnemy.macAddress);
        }
        else if(msg.startsWith(MODE_CODE)){
            Log.d(TAG,"Setting Mode");
            mMode = Integer.parseInt(msg.substring(2));
            updateModeIcon();
        }
    }

    private void reportResult(){
        ((TextView)findViewById(R.id.you_score)).setText(" You: " + mMyScore + " ");
        ((TextView)findViewById(R.id.them_score)).setText(" " + mConnectedDeviceName + ": " +mEnemyScore + " ");

        if(mMyScore > mEnemyScore) {
            Toast.makeText(BluetoothBrawlActivity.this, "You WON! " + mMyScore + " to " + mEnemyScore, Toast.LENGTH_SHORT).show();
            ((TextView)findViewById(R.id.you_score)).setBackgroundResource(R.drawable.win);
            ((TextView)findViewById(R.id.them_score)).setBackgroundResource(R.drawable.lose);
        }else if(mMyScore < mEnemyScore) {
            Toast.makeText(BluetoothBrawlActivity.this, "You LOST! " + mMyScore + " to " + mEnemyScore, Toast.LENGTH_SHORT).show();
            ((TextView)findViewById(R.id.you_score)).setBackgroundResource(R.drawable.lose);
            ((TextView)findViewById(R.id.them_score)).setBackgroundResource(R.drawable.win);
        }else {
            Toast.makeText(BluetoothBrawlActivity.this, "TIE! " + mMyScore + " to " + mEnemyScore, Toast.LENGTH_SHORT).show();
            ((TextView)findViewById(R.id.you_score)).setBackgroundResource(R.drawable.tie);
            ((TextView)findViewById(R.id.them_score)).setBackgroundResource(R.drawable.tie);
        }
        if(isServer) {
            mDataRepository.reportGame(getSpinCounterApplication().getUser(), mEnemy, mMyScore, mEnemyScore, new DataRepository.Callback<Void>() {
                @Override
                public void success(Void result) {
                }

                @Override
                public void failure(boolean networkError) {
                    Toast.makeText(BluetoothBrawlActivity.this, R.string.synchronize_failure, Toast.LENGTH_SHORT).show();
                }
            });
        }

        mEnemyScore = -1;
        mMyScore = -1;
    }

    // ==================
    //
    // SpinCounter logic
    //
    // ==================

    private final SpinCounter.SpinListener mSpinListener = new SpinCounter.SpinListener() {
        private int mCurrentNumberOfSpins;

        @Override
        public void onUpdate (float totalDegrees) {
            mCurrentNumberOfSpins = Math.abs((int)(totalDegrees/360.0f));
            mSpinnerView.setNumberOfSpins(mCurrentNumberOfSpins);
            mSpinnerView.setRotation(-totalDegrees);
        }
        @Override
        public void done () {
            mSpinCounter.stop();
            mSpinnerView.reset();
            mSpinnerView.setRotation(0);
            myTurn = false;
            if (mIsTiming) {
                Log.d(TAG,"CANCEL TIMER");
                mTimeChecker.removeCallbacks(mStopSession);
                mIsTiming = false;
            }
            if(!isServer) {
                sendMessage(START_CODE);
            }
            else {
                sendMessage(DONE_CODE + mCurrentNumberOfSpins);
            }
            mSpinnerView.setEnabled(false);
            mMyScore = mCurrentNumberOfSpins;
        }
    };

    // ==================
    //
    // SpinnerView logic
    //
    // ==================

    private SpinnerView makeSpinnerView () {
        SpinnerView result = new SpinnerView(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        result.setLayoutParams(params);
        return result;
    }

    private final SpinnerView.CountdownListener mCountdownListener = new SpinnerView.CountdownListener() {
        @Override
        public void countdownStarted() {
            mSpinCounter.prep();
        }
        @Override
        public void countdownFinished() {
            if (!mIsTiming) {
                mIsTiming = true;
                mStopSession = new Runnable() {
                    @Override
                    public void run() {
                        mIsTiming = false;
                        mSpinListener.done();
                    }
                };
                if(mMode != 1)
                    mTimeChecker.postDelayed(mStopSession, mMode);
                mSpinCounter.start();
            }
        }
    };

    // ==================
    //
    // Miscellaneous
    //
    // ==================

    private boolean deviceHasGyroscope () {
        SensorManager mgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = mgr.getSensorList(Sensor.TYPE_ALL);
        return (mgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null);
    }

    private void disconnect(){
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    private SpinCounterApplication getSpinCounterApplication () {
        return (SpinCounterApplication)getApplication();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ALERT:
                AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).
                        setMessage("Choose How Long Bluetooth Stays Discoverable:").
                        setCancelable(true);
                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setInputType(3);
                input.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                input.setFilters(new InputFilter[]{new InputFilterMinMax(0, 300)});
                input.setTextColor(Color.parseColor("#ffffffff"));
                Log.d("Discoverable Length", "0-300 seconds");
                input.setHint("0-300 seconds");
                input.setHintTextColor(Color.GRAY);
                builder.setView(input);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SharedPreferences.Editor ed = mPrefs.edit();
                        int value = Integer.parseInt(input.getText().toString());
                        mDiscoverable = value;
                        ed.putInt("mDiscoverable", value);
                        Log.d("VALUE", "" + value);
                        ed.apply();
                        return;
                    }
                });
                builder.setNegativeButton("Nope", new CancelOnClickListener());
                AlertDialog dialog = builder.create();
                dialog.show();
        }
        return super.onCreateDialog(id);
    }

    private final class CancelOnClickListener implements
            DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
        }
    }
}
