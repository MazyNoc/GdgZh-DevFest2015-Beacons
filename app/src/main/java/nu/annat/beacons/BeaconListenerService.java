package nu.annat.beacons;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nu.annat.beacon.backend.myApi.MyApi;
import nu.annat.beacon.backend.myApi.model.Beacon;
import nu.annat.beacon.backend.myApi.model.BeaconCollection;
import nu.annat.beacon.backend.myApi.model.StoredPosition;
import nu.annat.beacon.backend.myApi.model.UserPosition;

public class BeaconListenerService extends Service {
	private static final String TAG = BeaconListenerService.class.getSimpleName();
	private MyApi myApiService;
	private EvictList eddyStoneList = new EvictList();
	private long lastSent = 0;

	public BeaconListenerService() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && "stop".equals(intent.getAction())) {
			stopSelf();
			return Service.START_NOT_STICKY;
		}
		initScan();
		return Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private void initScan() {
		BluetoothManager manager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter btAdapter = manager.getAdapter();

		BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
		ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
		List<ScanFilter> filters = new ArrayList<>();
		scanner.startScan(filters, settings, new ScanCallback() {

			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				super.onScanResult(callbackType, result);
				Log.d(TAG, "scanresult ");
				if (result != null && result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
					Log.d(TAG, result.getDevice().getAddress());
					if (result.getScanRecord().getServiceUuids().contains(EddyStone.EDDYSTONE_SERVICE_UUID)) {
						sendClosestBeacon(new EddyStone(result));
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
	}

	private void sendClosestBeacon(final EddyStone newStone) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				initApi();
				try {

					BeaconCollection execute = myApiService.beacons().list().execute();
					Beacon beacon = findBeacon(newStone, execute);
					if (beacon != null) {
						eddyStoneList.addStone(newStone);

						final EddyStone closest = eddyStoneList.getClosest();

						if ( (getLastInstance() != null && closest == null)
							|| !closest.getInstance().equals(getLastInstance())) {

							lastSent = System.currentTimeMillis();

							for (EddyStone eddyStone : eddyStoneList.keySet()) {
								System.out.println(eddyStone);
							}

							eddyStoneList.evict(TimeUnit.SECONDS.toMillis(30));

							if(getLastKey()!=0){
								myApiService.position().update(getLastKey()).execute();
							}

							setLastKey(0);
							setLastRoom(null);
							setLastInstance(null);

							if (closest != null) {
								Beacon closesBeacon = findBeacon(closest, execute);
								if (closesBeacon != null) {
									System.out.println(execute.getItems());
									UserPosition userPosition = new UserPosition();
									userPosition.setUserUUID("74c73cd5-c219-4b6b-93e7-e9311a952821");
									userPosition.setRoomName(closesBeacon.getRoomName());
									System.out.println(userPosition);
									userPosition.setOldId(getLastKey());
									StoredPosition storedPosition = myApiService.position().store(userPosition).execute();
									setLastKey(storedPosition.getId());
									setLastRoom(userPosition.getRoomName());
									setLastInstance(closesBeacon.getInstance());
								}
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("newRoom"));
			}

			@Nullable
			private Beacon findBeacon(EddyStone newStone, BeaconCollection execute) {
				for (Beacon beacon : execute.getItems()) {
					if (beacon.getNameSpace().toLowerCase().equals(newStone.getNameSpace()) &&
						beacon.getInstance().toLowerCase().equals(newStone.getInstance())) {
						return beacon;
					}
				}
				return null;
			}
		}.execute();
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
}
