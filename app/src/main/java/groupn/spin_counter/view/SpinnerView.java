package groupn.spin_counter.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import groupn.spin_counter.R;

/**
 * View to display spinning information to user
 */
public class SpinnerView extends RelativeLayout {

    public static abstract class CountdownListener {
        public abstract void countdownStarted ();
        public abstract void countdownFinished ();
    }

    // ===============
    //
    // Initialization
    //
    // ================
    public SpinnerView(Context context) {
        super(context);
        init(null, 0);
    }

    public SpinnerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SpinnerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        //setBackgroundColor (getResources().getColor(android.R.color.holo_blue_bright));

        mButton = makeStartButton ();
        mSpinsTextView = makeSpinsTextView ();
        mCountdownTextView = makeCountdownTextView ();
        mImageView = makeImageView ();
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "fonts/orangejuice.otf");
        mButton.setTypeface(font);
        mSpinsTextView.setTypeface(font);
        mCountdownTextView.setTypeface(font);
        if(Build.VERSION.SDK_INT >=16) {
            mButton.setBackground(getResources().getDrawable(R.drawable.button_skin));
            mSpinsTextView.setBackground(getResources().getDrawable(R.drawable.rectangle));
            mCountdownTextView.setBackground(getResources().getDrawable(R.drawable.rectangle));
        }
        else {
            Log.d(TAG,"API level 15; does not support setBackground method");
            new AlertDialog.Builder(getContext(),AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle(R.string.api_warning_title)
                    .setMessage(R.string.api_lvl)
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing
                        }
                    })
                    .show();
            mButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
        }
        
        addView(mButton);
        addView (mImageView);
        addView (mSpinsTextView);
        addView (mCountdownTextView);

        RelativeLayout.LayoutParams imageParams =
                (RelativeLayout.LayoutParams)mImageView.getLayoutParams();
        imageParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        imageParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                IMG_SIZE, getResources().getDisplayMetrics());
        imageParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                IMG_SIZE, getResources().getDisplayMetrics());
        mImageView.setLayoutParams(imageParams);

        RelativeLayout.LayoutParams spinsParams =
                (RelativeLayout.LayoutParams)mSpinsTextView.getLayoutParams();
        spinsParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        spinsParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TEXT_BOX_SIZE, getResources().getDisplayMetrics());
        spinsParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TEXT_BOX_SIZE, getResources().getDisplayMetrics());
        mSpinsTextView.setLayoutParams(spinsParams);


        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick (View c) {
                startCountdown ();
            }
        });

        reset ();
    }

    private Button makeStartButton () {
        Button result = new Button(getContext ());
        result.setBackgroundColor (getResources().getColor(android.R.color.transparent));
        result.setText (R.string.start_spin);
        result.setTextSize(FONT_SIZE);
        return result;
    }

    private TextView makeSpinsTextView () {
        TextView result = new TextView (getContext ());
        result.setGravity(Gravity.CENTER);
        result.setTextSize(FONT_SIZE);
        return result;
    }

    private TextView makeCountdownTextView () {
        TextView result = new TextView (getContext ());
        result.setTextSize(FONT_SIZE);
        return result;
    }

    private ImageView makeImageView () {
        ImageView result = new ImageView (getContext ());
        result.setImageResource(R.drawable.optical_illusion);
        return result;
    }

    /**
     *
     * @return the listener for the countdown action
     */
    public CountdownListener getCountdownListener () {
        return mCountdownListener;
    }

    public void setCountdownListener (CountdownListener listener) {
        mCountdownListener = listener;
    }

    /**
     * resets the state of SpinnerView to display the "Spin" button
     */
    public void reset () {
        mSpinsTextView.setVisibility (View.GONE);
        mCountdownTextView.setVisibility (View.GONE);
        mImageView.setVisibility (View.GONE);
        mButton.setVisibility (View.VISIBLE);
    }

    public void cancel () {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    /**
     *
     * @return true if enabled, false otherwise. being disabled prevents the user from
     * starting the countdown.
     */
    public boolean isEnabled () {
        return mButton.isEnabled ();
    }

    public void setEnabled (boolean enabled) {
        mButton.setEnabled (enabled);
    }

    /**
     *
     * @return the number of spins currently being displayed
     */
    public int getNumberOfSpins () {
        return Integer.parseInt (mSpinsTextView.getText().toString ());
    }

    public void setNumberOfSpins (int numSpins) {
        mSpinsTextView.setText (Integer.toString (numSpins));
    }

    /**
     * starts the countdown
     */
    private void startCountdown () {
        if (mCountdownListener != null) {
            mCountdownListener.countdownStarted ();
        }
        mButton.setVisibility (View.GONE);
        mCountdownTextView.setVisibility (View.VISIBLE);


        mCountDownTimer = new CountDownTimer (COUNTDOWN_DURATION, COUNTDOWN_TICKS) {
            @Override
            public void onTick (long millisUntilFinished) {
                mCountdownTextView.setText (Long.toString (millisUntilFinished/COUNTDOWN_TICKS));
            }

            @Override
            public void onFinish () {
                mCountdownTextView.setText ("Go!");

                Handler handler = new Handler();
                handler.postDelayed (new Runnable () {
                    @Override
                    public void run () {
                        mCountdownTextView.setVisibility (View.GONE);
                        mSpinsTextView.setVisibility (View.VISIBLE);
                        mImageView.setVisibility(View.VISIBLE);
                        setNumberOfSpins(0);

                        if (mCountdownListener != null) {
                            mCountdownListener.countdownFinished ();
                        }
                    }
                }, RUN_DELAY);
            }
        }.start ();
    }

    // instance variables
    private Button mButton;
    private TextView mSpinsTextView;
    private TextView mCountdownTextView;
    private ImageView mImageView;
    private CountdownListener mCountdownListener;
    private CountDownTimer mCountDownTimer;

    // logging
    private static final String TAG = "SpinnerView";

    // constants
    private static final int FONT_SIZE = 64;
    private static final int COUNTDOWN_DURATION = 3100;
    private static final int COUNTDOWN_TICKS = 1000;
    private static final int RUN_DELAY = 750;
    private static final int IMG_SIZE = 450;
    private static final int TEXT_BOX_SIZE = 95;
}
