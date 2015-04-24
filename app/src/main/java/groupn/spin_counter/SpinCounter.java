package groupn.spin_counter;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike on 3/29/2015.
 */
public class SpinCounter implements SensorEventListener {

    private List<SpinListener> listeners;
    SensorManager sensorManager;
    private float lastDeg;
    private boolean ready;
    private float[] gravity = new float[3];
    private boolean gravityInit;
    private float[] geomagnetic = new float[3];
    private boolean geomagneticInit;
    private float totalDegrees;


    public SpinCounter(Context context) {
        listeners = new ArrayList<SpinListener>();
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        ready = false;
        gravityInit = false;
        geomagneticInit = false;
    }

    public void registerListener(SpinListener listener) {
        listeners.add(listener);
    }

    public void unRegisterListener(SpinListener listener) {
        listeners.remove(listener);
    }

    public void prep() {
        if(!sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                sensorManager.SENSOR_DELAY_UI)) {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    sensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    sensorManager.SENSOR_DELAY_UI);
        }
    }
    public void start() {
        ready = true;
        totalDegrees = 0.0f;

    }
    public void stop() {
        sensorManager.unregisterListener(this);
        ready = false;
        geomagneticInit = false;
        gravityInit = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            float[] orientationV = new float[3];
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] rotationM = new float[16];
                SensorManager.getRotationMatrixFromVector(rotationM, event.values);
                SensorManager.getOrientation(rotationM, orientationV);
            } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                for(int i=0; i<3; i++){
                    gravity[i] =  event.values[i];
                }
                gravityInit = true;
                if (geomagneticInit) {
                    float rotationM[] = new float[9];
                    float rotationI[] = new float[9];
                    SensorManager.getRotationMatrix(rotationM, rotationI, gravity, geomagnetic);
                    SensorManager.getOrientation(rotationM, orientationV);
                } else {
                    return;
                }
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                for(int i=0; i<3; i++){
                    geomagnetic[i] =  event.values[i];
                }
                geomagneticInit = true;
                if (gravityInit) {
                    float rotationM[] = new float[9];
                    float rotationI[] = new float[9];
                    SensorManager.getRotationMatrix(rotationM, rotationI, gravity, geomagnetic);
                    SensorManager.getOrientation(rotationM, orientationV);
                } else {
                    return;
                }
            } else {
                return;
            }

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
                float delta = azimuthInDegrees - lastDeg;
                if (Math.abs(delta) > 180.0f) {
                    if (delta < 0.0f) {
                        delta += 360.0f;
                    } else {
                        delta -= 360.0f;
                    }
                }
                totalDegrees += delta;
                for (SpinListener listener : listeners) {
                    listener.onUpdate(totalDegrees);
                }
                if ((pitchInDegrees > 60.0f && pitchInDegrees < 300.0f) ||
                        (rollInDegrees > 60.0f && rollInDegrees < 300.0f)) {
                    for (SpinListener listener : listeners) {
                        listener.done();
                        return;
                    }
                }
            }
            lastDeg = azimuthInDegrees;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public interface SpinListener {
        public void onUpdate(float totalDegrees);
        public void done();
    }
}
