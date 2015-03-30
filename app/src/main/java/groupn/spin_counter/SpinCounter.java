package groupn.spin_counter;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike on 3/29/2015.
 */
public class SpinCounter implements SensorEventListener {

    private static final int UNDEFINED = -1;
    private static final int CLOCKWISE = 0;
    private static final int COUNTERCLOCKWISE = 1;
    private List<SpinListener> listeners;
    SensorManager sensorManager;
    private float[] rotationM = new float[16];
    private float[] orientationV = new float[3];
    private boolean spinning;
    private float initialDeg;
    private float lastDeg;
    private int direction;
    private boolean ready;

    public SpinCounter(Context context) {
        listeners = new ArrayList<SpinListener>();
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        ready = false;
    }

    public void registerListener(SpinListener listener) {
        listeners.add(listener);
    }

    public void unRegisterListener(SpinListener listener) {
        listeners.remove(listener);
    }

    public void prep() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                sensorManager.SENSOR_DELAY_FASTEST);
    }

    public float getLastDeg() {
        return lastDeg;
    }

    public void start() {
        direction = UNDEFINED;
        spinning = false;
        ready = true;

    }
    public void stop() {
        sensorManager.unregisterListener(this);
        ready = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationM, event.values);
                SensorManager.getOrientation(rotationM, orientationV);
                float azimuthInDegrees = (float) Math.toDegrees(orientationV[0]);
                float pitchInDegrees = (float) Math.toDegrees(orientationV[1]);
                float rollInDegrees = (float) Math.toDegrees(orientationV[2]);
                if (azimuthInDegrees < 0.0f) {
                    azimuthInDegrees += 360.0f;
                }
                if (pitchInDegrees < 0.0f) {
                    pitchInDegrees += 360.0f;
                }
                if (rollInDegrees < 0.0f) {
                    rollInDegrees += 360.0f;
                }
                if (ready) {
                    for (SpinListener listener : listeners) {
                        listener.onSensorUpdate(azimuthInDegrees);
                    }

                    if (!spinning) {
                        // Set the starting position
                        initialDeg = azimuthInDegrees;
                        spinning = true;
                    } else if (direction == UNDEFINED) {
                        //  Detect which direction the phone is spinning
                        if (Math.abs(initialDeg - azimuthInDegrees) < 180.0f) {
                            if (azimuthInDegrees > initialDeg) {
                                direction = CLOCKWISE;
                            } else {
                                direction = COUNTERCLOCKWISE;
                            }
                        } else {
                            if (azimuthInDegrees > initialDeg) {
                                direction = COUNTERCLOCKWISE;
                            } else {
                                direction = CLOCKWISE;
                            }
                        }
                    } else {
                        // Determine if the phone has made a rotation or has stopped spinning fast enough
                        float adjAzimuth = azimuthInDegrees - initialDeg;
                        float adjLastDeg = lastDeg - initialDeg;
                        if (adjAzimuth < 0.0f) {
                            adjAzimuth += 360.0f;
                        }
                        if (adjLastDeg < 0.0f) {
                            adjLastDeg += 360.0f;
                        }

                        if (direction == CLOCKWISE) {
                            if (adjLastDeg > adjAzimuth) {
                                if ((360.0f - adjLastDeg) + adjAzimuth < 180.0f) {
                                    for (SpinListener listener : listeners) {
                                        listener.onFullRotation();
                                    }
                                } else {
                                    for (SpinListener listener : listeners) {
                                        listener.done();
                                        return;
                                    }
                                }
                            }
                        } else {
                            if (adjLastDeg < adjAzimuth) {
                                if ((360.0f - adjAzimuth) + adjLastDeg < 180.f) {
                                    for (SpinListener listener : listeners) {
                                        listener.onFullRotation();
                                    }
                                } else {
                                    for (SpinListener listener : listeners) {
                                        listener.done();
                                        return;
                                    }
                                }
                            }
                        }
                    }

                    if ((pitchInDegrees > 80.0f && pitchInDegrees < 170.0f) ||
                            (rollInDegrees > 80.0f && rollInDegrees < 170.0f)) {
                        for (SpinListener listener : listeners) {
                            listener.done();
                            return;
                        }
                    }
                    //Log.i("SpinCounter", String.format("Orientation: %f", azimuthInDegrees));
                }
                lastDeg = azimuthInDegrees;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public interface SpinListener {
        public void onSensorUpdate(float azimuth);
        public void onFullRotation();
        public void done();
    }
}
