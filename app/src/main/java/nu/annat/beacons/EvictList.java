package nu.annat.beacons;

import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class EvictList extends HashMap<EddyStone, Long> {

	private static final String TAG = EvictList.class.getSimpleName();

	public void addStone(EddyStone stone) {
		Log.d(TAG, "Updating stone " + stone.getInstance());
		super.remove(stone);
		super.put(stone, System.currentTimeMillis());
	}

	public void evict(long olderThanTime) {
		long now = System.currentTimeMillis();
		Iterator<Entry<EddyStone, Long>> iterator = entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<EddyStone, Long> next = iterator.next();
			if (now - next.getValue() > olderThanTime) {
				iterator.remove();
			}
		}
	}

	public EddyStone getClosest() {
		Set<EddyStone> eddyStones = keySet();
		EddyStone current = null;
		for (EddyStone eddyStone : eddyStones) {
			if (current == null || eddyStone.getAgedDistance() < current.getAgedDistance()) {
				current = eddyStone;
			}
		}
		return current;
	}
}
