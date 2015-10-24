package nu.annat.beacons;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nu.annat.beacon.backend.myApi.MyApi;
import nu.annat.beacon.backend.myApi.model.Beacon;
import nu.annat.beacon.backend.myApi.model.BeaconCollection;
import nu.annat.beacon.backend.myApi.model.StoredPosition;
import nu.annat.beacon.backend.myApi.model.UserPosition;

public class BeaconListenerService extends Service {
    private static final String TAG = BeaconListenerService.class.getSimpleName() + "a";

    public class MyBinder extends Binder {
        public BeaconListenerService getService() {
            return BeaconListenerService.this;
        }
    }

    IBinder myBinder = new MyBinder();
    private MyApi myApiService;
    private EvictList eddyStoneList = new EvictList();
    private long lastSent = 0;
    private Handler handler;
    private BeaconCollection beaconCollection;
    private String userId;
    public Runnable removeRunnable = new Runnable() {
        @Override
        public void run() {
            sendClosestBeacon(null);
        }
    };
    private BluetoothLeScanner scanner;

    public BeaconListenerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getNameList();
        handler = new Handler();
        SharedPreferences beacon = this.getSharedPreferences("default", MODE_PRIVATE);
        userId = beacon.getString("userId", null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            beacon.edit().putString("userId", userId).apply();
        }
        initScan();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "stop".equals(intent.getAction())) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private void initScan() {
        BluetoothManager manager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanner = btAdapter.getBluetoothLeScanner();

            if (scanner == null) {
                Toast.makeText(this, "You must enable Bluetooth for this app to work", Toast.LENGTH_LONG).show();
                stopSelf();
                return;
            }

            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
            List<ScanFilter> filters = new ArrayList<>();
            scanner.startScan(filters, settings, new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (result != null && result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                        if (result.getScanRecord().getServiceUuids().contains(EddyStone.EDDYSTONE_SERVICE_UUID)) {
                            try {
                                EddyStone newStone = new EddyStone(result);
                                if (findBeacon(newStone, beaconCollection) != null) {
                                    sendDistance(newStone.getInstance(), newStone.getDistance());
                                    synchronized (eddyStoneList) {
                                        addNewStone(newStone);
                                        EddyStone closest = eddyStoneList.getClosest();
                                        eddyStoneList.evict(TimeUnit.SECONDS.toMillis(30));
                                        if (closest != null) {
                                            sendClosestBeacon(closest);
                                        }
                                    }
                                }
                            } catch (Exception ignore) {
                            }
                        }
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.e("StartScanner", "" + errorCode);
                }
            });
        } else {
            btAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    try {
                        EddyStone newStone = new EddyStone(rssi, scanRecord);
                        if (findBeacon(newStone, beaconCollection) != null) {
                            sendDistance(newStone.getInstance(), newStone.getDistance());
                            synchronized (eddyStoneList) {
                                addNewStone(newStone);
                                EddyStone closest = eddyStoneList.getClosest();
                                eddyStoneList.evict(TimeUnit.SECONDS.toMillis(30));
                                if (closest != null) {
                                    sendClosestBeacon(closest);
                                }
                            }
                        }
                    } catch (Exception ignore) {
                    }
                }
            });
            return;
        }
    }

    private void addNewStone(EddyStone newStone) {
        Beacon beacon = findBeacon(newStone, beaconCollection);
        if (beacon != null) {
            eddyStoneList.addStone(newStone);
        }
    }

    private void sendDistance(String instance, double distance) {
        Intent newDistance = new Intent("newDistance");
        newDistance.putExtra("instance", instance);
        newDistance.putExtra("distance", distance);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(newDistance);
    }

    private void sendClosestBeacon(final EddyStone closest) {

        if (beaconCollection == null) return;

        handler.removeCallbacks(removeRunnable);
        if (closest != null) {
            handler.postDelayed(removeRunnable, TimeUnit.SECONDS.toMillis(30));
            //	Log.d(TAG, "Adding timer");
        }

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                initApi();
                try {
                    if ((getLastInstance() != null && closest == null) || !closest.getInstance().equals(getLastInstance())) {

                        lastSent = System.currentTimeMillis();

                        eddyStoneList.evict(TimeUnit.SECONDS.toMillis(30));

                        if (getLastKey() != 0) {
                            myApiService.position().update(getLastKey()).execute();
                        }

                        if (closest != null) {
                            Beacon closesBeacon = findBeacon(closest, beaconCollection);
                            if (closesBeacon != null) {
                                UserPosition userPosition = new UserPosition();
                                userPosition.setUserUUID(userId);
                                userPosition.setRoomName(closesBeacon.getRoomName());
                                userPosition.setOldId(getLastKey());
                                StoredPosition storedPosition = myApiService.position().store(userPosition).execute();
                                setLastKey(storedPosition.getId());
                                setLastRoom(userPosition.getRoomName());
                                setLastInstance(closest.getInstance());
                            }
                        } else {
                            if (getLastKey() != 0) {
                                myApiService.position().update(getLastKey()).execute();
                                setLastKey(0);
                                setLastRoom(null);
                                setLastInstance(null);
                            }
                        }
                    }
                    //Log.d(TAG, "Room updated: " + closest==null?"rooms removed":closest.getInstance());
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean aVoid) {
                super.onPostExecute(aVoid);
                if (aVoid) {
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("newRoom"));
                }
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Nullable
    private Beacon findBeacon(EddyStone newStone, BeaconCollection execute) {
        if (execute == null) return null;
        for (Beacon beacon : execute.getItems()) {
            if (beacon.getNameSpace().toLowerCase().equals(newStone.getNameSpace()) &&
                beacon.getInstance().toLowerCase().equals(newStone.getInstance())) {
                return beacon;
            }
        }
        return null;
    }

    private void getNameList() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    initApi();
                    beaconCollection = myApiService.beacons().list().execute();
                    //Log.d(TAG, "name collection downloaded");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public long getLastKey() {
        SharedPreferences aDefault = getSharedPreferences("default", MODE_PRIVATE);
        return aDefault.getLong("lastKey", 0);
    }

    private void setLastKey(long instance) {
        SharedPreferences aDefault = getSharedPreferences("default", MODE_PRIVATE);
        aDefault.edit().putLong("lastKey", instance).apply();
    }

    public String getLastInstance() {
        SharedPreferences aDefault = getSharedPreferences("default", MODE_PRIVATE);
        return aDefault.getString("lastInstance", null);
    }

    private void setLastInstance(String instance) {
        SharedPreferences aDefault = getSharedPreferences("default", MODE_PRIVATE);
        aDefault.edit().putString("lastInstance", instance).apply();
    }

    private void setLastRoom(String roomName) {
        SharedPreferences aDefault = getSharedPreferences("default", MODE_PRIVATE);
        aDefault.edit().putString("lastRoom", roomName).apply();
    }

    private void initApi() {
        if (myApiService == null) {
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), null)
                .setRootUrl("https://ch-zhgdg-mikael-becons.appspot.com/_ah/api/");
            myApiService = builder.build();
        }
    }

    public List<EddyStoneRoom> getBeaconRoomData() {
        List<EddyStoneRoom> list = new ArrayList<>();
        synchronized (eddyStoneList) {
            for (EddyStone eddyStone : eddyStoneList.keySet()) {
                Beacon beacon = findBeacon(eddyStone, beaconCollection);
                if (beacon != null) {
                    list.add(new EddyStoneRoom(eddyStone, beacon.getRoomName()));
                }
            }
        }
        return list;
    }
}
