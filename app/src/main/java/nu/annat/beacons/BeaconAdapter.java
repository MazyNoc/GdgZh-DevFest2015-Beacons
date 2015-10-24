package nu.annat.beacons;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BeaconAdapter extends RecyclerView.Adapter<BeaconVH> {

	private static final String TAG = BeaconAdapter.class.getSimpleName();
	List<EddyStoneRoom> items = new ArrayList<>();

	public BeaconAdapter() {
		setHasStableIds(true);
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).eddyStone.getInstance().hashCode();
	}

	@Override
	public BeaconVH onCreateViewHolder(ViewGroup parent, int viewType) {
		View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.eddy_stone_info, parent, false);
		return new BeaconVH(inflate);
	}

	@Override
	public void onBindViewHolder(BeaconVH holder, int position) {
		holder.setData(items.get(position));
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public void setData(List<EddyStoneRoom> beaconRoomData) {
		Log.d(TAG, "setData");
		items = beaconRoomData;
		sortItems();
		notifyDataSetChanged();
	}

	private void sortItems() {
		Collections.sort(items, new Comparator<EddyStoneRoom>() {
			@Override
			public int compare(EddyStoneRoom lhs, EddyStoneRoom rhs) {
				return Double.compare(lhs.eddyStone.getAgedDistance(), rhs.eddyStone.getAgedDistance());
			}
		});
	}
}
