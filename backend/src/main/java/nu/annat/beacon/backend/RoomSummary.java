package nu.annat.beacon.backend;

public class RoomSummary {
	public String roomName;
	public int timesVisited;
	public long firstSeen;
	public long lastSeen;
	public long totalTime;

	public void add(RoomSummary currentRoomSummary) {
		timesVisited++;
		totalTime+=(currentRoomSummary.lastSeen-currentRoomSummary.firstSeen);
		firstSeen = Math.min(firstSeen, currentRoomSummary.firstSeen);
		lastSeen = Math.min(lastSeen, currentRoomSummary.lastSeen);
	}

	public long getDuration(){
		return lastSeen-firstSeen;
	}
}
