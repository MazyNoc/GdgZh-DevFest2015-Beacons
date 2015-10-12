package nu.annat.beacons;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

	private static final int REQUEST_ENABLE_BLUETOOTH = 1;
	private static final String TAG = MainActivityFragment.class.getSimpleName();
	private BluetoothLeAdvertiser adv;
	private AdvertiseCallback advertiseCallback;
	private BroadcastReceiver newNameReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateRoomName();
		}
	};

	private void updateRoomName() {
		SharedPreferences aDefault = getContext().getSharedPreferences("default", Context.MODE_PRIVATE);
		String lastRoom = aDefault.getString("lastRoom", null);
		getActivity().setTitle(lastRoom);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		updateRoomName();
	}

	public MainActivityFragment() {
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
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
	}

	private void initScan() {
		Intent intent = new Intent(getActivity(), BeaconListenerService.class);
		getActivity().startService(intent);
	}

	@Override
	public void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(getContext()).registerReceiver(newNameReceiver, new IntentFilter("newRoom"));
	}

	@Override
	public void onStop() {
		super.onStop();
		LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(newNameReceiver);
	}
}
