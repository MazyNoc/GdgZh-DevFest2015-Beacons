package nu.annat.beacons;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class BeaconVH extends RecyclerView.ViewHolder {

	private final TextView roomName;
	private final TextView calculatedDistance;

	public BeaconVH(View itemView) {
		super(itemView);
		roomName = (TextView) itemView.findViewById(R.id.roomName);
		calculatedDistance = (TextView) itemView.findViewById(R.id.calculatedDistance);
	}


	public void setData(EddyStoneRoom eddyStoneRoom) {
		roomName.setText(eddyStoneRoom.roomName);
		calculatedDistance.setText(getDistanceText(eddyStoneRoom));
	}

	private String getDistanceText(EddyStoneRoom eddyStoneRoom) {
		double distance = eddyStoneRoom.eddyStone.getAgedDistance();
		if(distance<1){
			return String.format("%.2f cm", distance*100);
		} else {
			return String.format("%.2f m", distance);
		}
	}
}

