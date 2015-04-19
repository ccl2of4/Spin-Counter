package groupn.spin_counter.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
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
        setBackgroundColor (getResources().getColor(android.R.color.holo_blue_bright));

        mButton = makeStartButton ();
        mSpinsTextView = makeSpinsTextView ();
        mCountdownTextView = makeCountdownTextView ();
        mImageView = makeImageView ();
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "fonts/orangejuice.otf");
        mButton.setTypeface(font);
        mSpinsTextView.setTypeface(font);
        mCountdownTextView.setTypeface(font);
        
        addView (mButton);
        addView (mSpinsTextView);
        addView (mCountdownTextView);
        addView (mImageView);

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
        mButton.setVisibility (View.VISIBLE);
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
        mButton.setVisibility (View.GONE);
        mCountdownTextView.setVisibility (View.VISIBLE);

        if (mCountdownListener != null) {
            mCountdownListener.countdownStarted ();
        }

        new CountDownTimer (COUNTDOWN_DURATION, COUNTDOWN_TICKS) {
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
                        setNumberOfSpins (0);

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

    // logging
    private static final String TAG = "SpinnerView";

    // constants
    private static final int FONT_SIZE = 64;
    private static final int COUNTDOWN_DURATION = 3100;
    private static final int COUNTDOWN_TICKS = 1000;
    private static final int RUN_DELAY = 250;
}
