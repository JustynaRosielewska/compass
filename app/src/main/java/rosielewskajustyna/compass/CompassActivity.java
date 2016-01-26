package rosielewskajustyna.compass;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;




public class CompassActivity extends Activity implements SensorEventListener {

    private ImageView mPointer, arrow;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;
    private float mLastDegree = 0f;
    private float arrowCurrentDegree = 0f;
    Switch switchLat, switchLong;
    LinearLayout pr;
    Button buttonGo;
    EditText editTextDegreesLat, editTextMinLat, editTextSecLat;
    EditText editTextDegreesLong, editTextMinLong, editTextSecLong;
    float bearing = 0;
    TextView textViewCurrent, textViewDegreeLat, textViewDegreeLong;
    GPSTracker gps;
    private GPSListener gpsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initSensors();
        initViews();
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        if(gps!=null) {
            gps.stopUsingGPS();
        }
    }

    private void initSensors() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if(mMagnetometer==null) {
            Toast.makeText(this, getResources().getString(R.string.no_magn), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        pr = (LinearLayout) findViewById(R.id.layoutProgress);
        mPointer = (ImageView) findViewById(R.id.pointer);
        arrow = (ImageView) findViewById(R.id.imageView);
        textViewCurrent = (TextView) findViewById(R.id.textViewCurrent);
        editTextDegreesLat = (EditText) findViewById(R.id.editTextDegreesLat);
        editTextMinLat = (EditText) findViewById(R.id.editTextMinLat);
        editTextSecLat = (EditText) findViewById(R.id.editTextSecLat);
        editTextDegreesLong = (EditText) findViewById(R.id.editTextDegreesLong);
        editTextMinLong = (EditText) findViewById(R.id.editTextMinLong);
        editTextSecLong = (EditText) findViewById(R.id.editTextSecLong);
        textViewDegreeLat = (TextView) findViewById(R.id.textViewDegreesLat);
        textViewDegreeLat.setText("\u00B0");
        textViewDegreeLong = (TextView) findViewById(R.id.textViewDegreesLong);
        textViewDegreeLong.setText("\u00B0");
        switchLat = (Switch) findViewById(R.id.switchLat);
        switchLong = (Switch) findViewById(R.id.switchLong);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchLat.setShowText(true);
            switchLong.setShowText(true);
        }

        buttonGo = (Button) findViewById(R.id.button);
        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (validationInsertCoordinations())
                    getAngleToDestination(getTargetLocation());
            }
        });
    }

    private Location getTargetLocation() {
        Location target = new Location("");
        double lat, lon;

        lat = getDouble(editTextDegreesLat) + (getDouble(editTextMinLat) * 60 + getDouble(editTextSecLat)) / 3600;
        lon = getDouble(editTextDegreesLong) + (getDouble(editTextMinLong) * 60 + getDouble(editTextSecLong)) / 3600;

        if (switchLat.isChecked())
            lat = -lat;
        if (!switchLong.isChecked())
            lon = -lon;

        target.setLatitude(lat);
        target.setLongitude(lon);

        return target;
    }

    private boolean validationInsertCoordinations() {
        boolean success = false;

        if (isAllDataFilled()) {
            if (getDouble(editTextDegreesLat) > 90) {
                Toast.makeText(this, getResources().getString(R.string.valid_degree_lat), Toast.LENGTH_LONG).show();
            } else if (getDouble(editTextDegreesLong) > 180) {
                Toast.makeText(this, getResources().getString(R.string.valid_degree_long), Toast.LENGTH_LONG).show();
            } else if (getDouble(editTextMinLat) > 59 || getDouble(editTextMinLong) > 59) {
                Toast.makeText(this, getResources().getString(R.string.valid_min), Toast.LENGTH_LONG).show();
            } else if (getDouble(editTextSecLat) > 60 || getDouble(editTextSecLong) > 60) {
                Toast.makeText(this, getResources().getString(R.string.valid_sec), Toast.LENGTH_LONG).show();
            } else {
                success = true;
            }
        }

        return success;
    }

    private boolean isAllDataFilled() {
        boolean success = false;

        if (editTextDegreesLat.length() > 0 && editTextMinLat.length() > 0 && editTextSecLat.length() > 0 && editTextDegreesLong.length() > 0
                && editTextMinLong.length() > 0 && editTextSecLong.length() > 0) {
            success = true;
        } else {
            Toast.makeText(this, getResources().getString(R.string.valid_fill_data), Toast.LENGTH_LONG).show();
        }

        return success;
    }

    private double getDouble(EditText editText) {
        double value;
        value = Double.parseDouble(editText.getText().toString());

        return value;
    }

    private void getAngleToDestination(final Location target) {
        gpsListener = new GPSListener() {
            @Override
            public void updateLocation(Location newLocation) {
                getBearing(newLocation, target);
            }
        };

        gps = new GPSTracker(this, gpsListener);
        Location currentLoc = gps.getLocation();
        if(currentLoc!=null) {
            getBearing(currentLoc, target);
        } else if (gps.canGetLocation()) {
            pr.setVisibility(View.VISIBLE);
        }
    }

    private void getBearing(Location currentLoc, Location target) {
        pr.setVisibility(View.GONE);
        setCurrentLocationText(currentLoc);
        float bearTo = currentLoc.bearingTo(target);

        if (bearTo < 0) {
            bearTo = bearTo + 360;
        }
        bearing = bearTo;
    }

    private void setCurrentLocationText(Location currentLoc) {
        String latDirection = " N ";
        String longDirection = " E";
        String strLatitude = currentLoc.convert(currentLoc.getLatitude(), currentLoc.FORMAT_SECONDS);
        String strLongitude = currentLoc.convert(currentLoc.getLongitude(), currentLoc.FORMAT_SECONDS);

        if (currentLoc.getLatitude() < 0)
            latDirection = " S ";
        if (currentLoc.getLongitude() < 0) {
            longDirection = " W";
        }
        StringBuilder msg = new StringBuilder();
        msg.append(getResources().getString(R.string.your_loc));
        msg.append("\n");
        msg.append(strLatitude.replace(":"," "));
        msg.append(latDirection);
        msg.append( strLongitude.replace(":"," "));
        msg.append(longDirection);
        textViewCurrent.setText(msg);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }

        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];

            float azimuthInDegress = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;

            if(mLastDegree==0 || Math.abs(azimuthInDegress-mLastDegree)>1) {
                mPointer.startAnimation(createAnimation(mCurrentDegree, -azimuthInDegress));
                mCurrentDegree = -azimuthInDegress;

                float direction = bearing + mCurrentDegree;
                arrow.startAnimation(createAnimation(arrowCurrentDegree, direction));
                arrowCurrentDegree = direction;
            }
            mLastDegree = azimuthInDegress;
        }
    }

    private RotateAnimation createAnimation(float startDegree, float endDegree) {
        RotateAnimation ra = new RotateAnimation(
                startDegree,
                endDegree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        ra.setDuration(120);
        ra.setFillAfter(true);
        return ra;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }
}