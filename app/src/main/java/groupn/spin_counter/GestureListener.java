package groupn.spin_counter;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
/**
 * Created by Seth on 4/18/2015.
 */
class GestureListener extends GestureDetector.SimpleOnGestureListener
{
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    static int swipeDirection;

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffY = e2.getRawY() - e1.getRawY();
            float diffX = e2.getRawX() - e1.getRawX();
            if ((Math.abs(diffX) - Math.abs(diffY)) > SWIPE_THRESHOLD) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        //SwipeRight
                        swipeDirection = 1;
                        return true;
                    } else {
                        //SwipeLeft
                        swipeDirection = 0;
                        return true;
                    }
                }
            }
        } catch (Exception e) {

        }
        return result;
    }
}