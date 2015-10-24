package nu.annat.beacons;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.TimeUnit;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

	private static final int REQUEST_ENABLE_BLUETOOTH = 1;
	private static final String TAG = MainActivityFragment.class.getSimpleName();
	public static final long DELAY_MILLIS = TimeUnit.SECONDS.toMillis(2);
	private BluetoothLeAdvertiser adv;
	private AdvertiseCallback advertiseCallback;
	private BroadcastReceiver newNameReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateRoomName();
		}
	};
	private BeaconAdapter adapter;
	private Handler handler;
	private BeaconListenerService myService;
	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			myService = ((BeaconListenerService.MyBinder) service).getService();
			updateBeacons();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	};
	Runnable updateMe = new Runnable() {
		@Override
		public void run() {
			if (handler != null) {
				handler.postDelayed(this, DELAY_MILLIS);
				updateBeacons();
			}
		}
	};

	public MainActivityFragment() {
	}

	private void updateBeacons() {
		if (adapter != null && myService != null) {
			adapter.setData(myService.getBeaconRoomData());
		}
	}

	private void updateRoomName() {
		SharedPreferences aDefault = getContext().getSharedPreferences("default", Context.MODE_PRIVATE);
		String lastRoom = aDefault.getString("lastRoom", null);
		getActivity().setTitle("You are in " + lastRoom);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == 2) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				initScan();
			} else {
				getActivity().finish();
			}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		updateRoomName();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_main, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (getActivity().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			initScan();
		} else {
			requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 2);
		}

		RecyclerView beaconlist = (RecyclerView) getView().findViewById(R.id.beaconlist);
		beaconlist.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = new BeaconAdapter();
		beaconlist.setAdapter(adapter);
	}

	@Override
	public void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(getContext()).registerReceiver(newNameReceiver, new IntentFilter("newRoom"));
		if (getActivity().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			initScan();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(newNameReceiver);
		Handler tmpHandler = handler;
		handler = null;
		tmpHandler.removeCallbacks(updateMe);
	}

	private void initScan() {
		getContext().bindService(new Intent(getContext(), BeaconListenerService.class), conn, Context.BIND_AUTO_CREATE);
		handler = new Handler();
		handler.postDelayed(updateMe, DELAY_MILLIS);

	}
}
