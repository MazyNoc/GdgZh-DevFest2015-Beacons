package nu.annat.beacons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class RoomFragment extends Fragment {

	private Room room;
	private TextView tv;
	private TextView[] c = new TextView[4];
	private BroadcastReceiver newDistance = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String instance = intent.getStringExtra("instance");
			double distance = intent.getDoubleExtra("distance", -1);
			if (distance >= 0) {
				updateDistance(instance, distance);
			}
		}
	};

	private void updateDistance(String instance, double distance) {
		room.updateInstance(instance, distance);
		Room.Point center = room.getCenter();
		if(center == null){
			tv.setText("outside");
			return;
		}
		tv.setText(String.format("x:%.2f, y:%.2f", center.x, center.y));

		for (int i = 0; i < room.corners.size(); i++) {
			c[i].setText(room.corners.get(i).toString());
		}


		CV cv = (CV) getView().findViewById(R.id.cv);
		cv.setPoint(room.p1, room.p2, room.p3, room.p4);

	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.room_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		tv = (TextView) view.findViewById(R.id.xy);

		c[0] = (TextView) view.findViewById(R.id.c1);
		c[1] = (TextView) view.findViewById(R.id.c2);
		c[2] = (TextView) view.findViewById(R.id.c3);
		c[3] = (TextView) view.findViewById(R.id.c4);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		room = new Room();
		room.addCorner(0, 0, "fc60f79b98a0");
		room.addCorner(0, 1, "e0871be89a43");
		room.addCorner(1, 1, "f580d973617e");
		room.addCorner(1, 0, "f53b2568ee23");
	}

	@Override
	public void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newDistance, new IntentFilter("newDistance"));
	}

	@Override
	public void onStop() {
		super.onStop();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newDistance);
	}

}
