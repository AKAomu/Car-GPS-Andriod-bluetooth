package com.example.user.googleplaylocation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity  implements View.OnClickListener {

    private GoogleApiClient mGoogleApi;
    public TextView mTextLat;
    public TextView mTextLon;
    public double mLastLat = 0;
    public double mLastLon = 0;
    public String data;
    public TextView mTextStaus;
    public TextView mTextDataLat;
    public TextView mTextDataLon;
    public TextView mTextDataStatus;

    /** RFCOMM socket */
    //public static final int TYPE_RFCOMM = 1;

    // Constants that indicate the current connection state
    //public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public Button connectBluetooth;
    public Button disconnectBluetooth;
    public Button startRecieve;
    public TextView mTextDataReceive;
    public BluetoothAdapter mAdapter;
    //public String device_name = "1C:DA:27:F5:27:29";
    public String device_name = "80:30:DC:30:69:1E";
    //public String device_name = "vivo 1713";
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream inStream;
    private OutputStream outputStream;
    private BluetoothSocket mSocket = null;
    private BluetoothDevice device = null;

    public enum BT_STATE {UNKNOWN_STATE, CONNECTED_STATE, DISCOVERY_START_STATE, FAILURE_STATE, NULL_ADAPTER};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Car Services: Location");
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
              }
        });

        mTextLat = (TextView)findViewById(R.id.textView_lat);
        mTextLon = (TextView)findViewById(R.id.textView_lon);

        mTextStaus = (TextView) findViewById(R.id.textStaus);
        mTextDataLat = (TextView) findViewById(R.id.textView_dataLat);
        mTextDataLon = (TextView) findViewById(R.id.textView_dataLon);
        mTextDataStatus = (TextView) findViewById(R.id.textView_DataStatus);
        mTextDataReceive = (TextView) findViewById(R.id.textView_dataRecieve);
        findViewById(R.id.button_connectBluetooth).setOnClickListener(this);
        findViewById(R.id.button_disconnectBluetooth).setOnClickListener(this);
        findViewById(R.id.button_start).setOnClickListener(this);

        toggleButton(false);
        startCancelButton(true);

        mGoogleApi = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mCallback)
                .addOnConnectionFailedListener(mOnFailed)
                .build();

        mAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public void onClick(View view) {
        WriteRead writeRead;

        switch (view.getId()) {
            case R.id.button_disconnectBluetooth:
                mAdapter.cancelDiscovery();
                break;
            case R.id.button_connectBluetooth:
                buttonSendClick();
                break;
            case R.id.button_start:
                writeRead = new WriteRead(mSocket,1);
                new Thread(writeRead).start();
                break;
            default:
                Log.i("MyApp", "unknown click event");
        }
    }

    private void toggleButton(boolean flag) {
        findViewById(R.id.button_start).setEnabled(flag);
    }

    private void startCancelButton(boolean flag) {
        if (flag) {
            findViewById(R.id.button_disconnectBluetooth).setEnabled(false);
            findViewById(R.id.button_connectBluetooth).setEnabled(true);
        } else {
            findViewById(R.id.button_disconnectBluetooth).setEnabled(true);
            findViewById(R.id.button_connectBluetooth).setEnabled(false);
        }
    }

    private GoogleApiClient.ConnectionCallbacks mCallback =
          new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                      LocationRequest request = new LocationRequest()    //.create()
                              .setInterval(500)
                              .setFastestInterval(500)
                              .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                      try {
                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                    mGoogleApi, request, mLocationListener);
                      } catch(SecurityException ex) { }
                }

                @Override
                public void onConnectionSuspended(int i) {

                }
          };

    private GoogleApiClient.OnConnectionFailedListener mOnFailed =
          new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {

                }
          };

    private LocationListener mLocationListener =
        new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            //ถ้าละติจูดและลองจิจูดไม่เท่ากับค่าเดิมจึงจะแสดงตำแหน่ง
            if(lat != mLastLat && lon  != mLastLon) {
                String strLat = "\n" + lat;
                String strLon = "\n" + lon;
                mTextDataLat.setText(strLat);
                mTextDataLon.setText(strLon);
                mLastLat = lat;
                mLastLon = lon;
                data = "https://suyama-project.appspot.com/location?lat="+lat+"&lon="+lon;
                //data = "http://192.168.0.6:8080/location?lat="+lat+"&lon="+lon;
                new DataConnect().execute(data);
                //mTextStaus.append(" OK Location");
            }
                }
          };

    @Override
    public void onStart() {
        if(mGoogleApi != null) {
              mGoogleApi.connect();
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        if(mGoogleApi != null && mGoogleApi.isConnected()) {
              mGoogleApi.disconnect();
        }
        super.onStop();
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
        if(id == R.id.action_settings) {
              return true;
        }

        return super.onOptionsItemSelected(item);
    }


    class DataConnect extends AsyncTask<String, Void, String>{
        @Override
        public String doInBackground(String... args) {
            String response = "";
            try {
                URL url = new URL(args[0]);
                HttpURLConnection httpConnect = (HttpURLConnection)url.openConnection();
                httpConnect.setRequestMethod("GET");
                //httpConnect.setDoOutput(true);
                httpConnect.connect();
                //mTextDataStatus.setText(httpConnect.getResponseMessage());

            } catch (Exception e) {   }
            return response;
        }
    }

    //@RequiresApi(api = Build.VERSION_CODES.M)
    public void buttonSendClick() {

        if(mAdapter == null) {
            return;
        } else if(!mAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1234);
        } else if(mAdapter.isEnabled()) {
            //mTextDataStatus.setText(mAdapter.getName());
            //bluetoothConnect();
            mAdapter.cancelDiscovery();
            mAdapter.startDiscovery();
            updateState(BT_STATE.DISCOVERY_START_STATE);

            device = mAdapter.getRemoteDevice(device_name);

            try {

                Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                mSocket = (BluetoothSocket) m.invoke(device, 3);
                //mSocket = device.createRfcommSocketToServiceRecord(uuid);
                Log.i("MyApp", "Created Socket!");
                updateState(BT_STATE.CONNECTED_STATE);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("error", "Failed to create temporary socket");
                updateState(BT_STATE.FAILURE_STATE);
            }

            try {
                mSocket.connect();
                Log.i("MyApp", "Connected!");
                updateState(BT_STATE.CONNECTED_STATE);
            } catch (IOException connectException) {
                Log.e("error", "Connection through socket failed: " + connectException);
                updateState(BT_STATE.FAILURE_STATE);
                try {
                    mSocket.close();
                    Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    mSocket = (BluetoothSocket) m.invoke(device, 1);
                    mSocket.connect();
                    Log.i("MyApp", "Connected Again!");
                    updateState(BT_STATE.CONNECTED_STATE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("error", "Connection through socket failed again: " + connectException);
                    updateState(BT_STATE.FAILURE_STATE);
                }
                try {
                    outputStream = mSocket.getOutputStream();
                    inStream = mSocket.getInputStream();
                    Log.i("MyApp", "Data In/Out!");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mAdapter.cancelDiscovery();
                //mTextDataStatus.setText("Socket : Connected");
            }
        } else {
            //mTextDataStatus.setText("Bluetooth is disabled.");
            Log.e("error", "Bluetooth is disabled.");
            updateState(BT_STATE.NULL_ADAPTER);
        }
    }

    private void updateState(BT_STATE target) {
        switch(target) {
            case CONNECTED_STATE:
                toggleButton(true);
                startCancelButton(false);
                mTextDataStatus.setText("Connected");
                break;
            case DISCOVERY_START_STATE:
                mTextDataStatus.setText("Discovery start");
                startCancelButton(false);
                break;
            case FAILURE_STATE:
                toggleButton(false);
                startCancelButton(true);
                mTextDataStatus.setText("Failure");
                break;
            case NULL_ADAPTER:
                toggleButton(false);
                startCancelButton(true);
                mTextDataStatus.setText("Null Adapter");
                break;
            case UNKNOWN_STATE:
                toggleButton(false);
                startCancelButton(true);
                mTextDataStatus.setText("Unknown");
                break;
        }
    }

    /*private void bluetoothConnect() {
        mAdapter.cancelDiscovery();
        mAdapter.startDiscovery();
        try {
            device = mAdapter.getRemoteDevice(device_name);
            Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            mSocket = (BluetoothSocket) m.invoke(device, 1);
            //_socket = target.createRfcommSocketToServiceRecord(UUID.fromString(SERIAL_SERVICE));
            mSocket.connect();
            Log.i("MyApp", "Connected!");
            mTextDataStatus.setText("Socket : Connected");
            toggleButton(true);
        } catch(Exception exception) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            exception.printStackTrace();
            mTextDataStatus.setText("Bluetooth cannot connect.");
            Log.e("error", "Bluetooth cannot connect.");
        }
    }*/

    class WriteRead implements Runnable {
        public final String LOG_TAG = getClass().getName();

        private int _start = 0;
        private final BluetoothSocket _socket;

        private Reader reader;
        private Writer writer;

        public TextView _tvReceive;

        private final StringBuilder _stringBuilder = new StringBuilder();

        WriteRead(BluetoothSocket socket, int start) {
            _socket = socket;
            _start = start;
        }

        public String getResponse() {
            return _stringBuilder.toString();
        }

        public void run() {
            try {
                reader = new InputStreamReader(_socket.getInputStream(), "UTF-8");
                writer = new OutputStreamWriter(_socket.getOutputStream(), "UTF-8");

                switch(_start) {
                    case 1:
                        Log.i("MyApp", "write green");
                        writer.write("green\n");
                        writer.flush();
                        break;
                }

                final char[] buffer = new char[8];
                while (true) {
                    int size = reader.read(buffer);
                    if (size < 0) {
                        break;
                    } else {
                        _stringBuilder.append(buffer, 0, size);
                        mTextDataReceive.setText(buffer,0,size);
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

}


