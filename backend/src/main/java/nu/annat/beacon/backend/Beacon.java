package nu.annat.beacon.backend;

public class Beacon {
	public String nameSpace;
	public String instance;
	public String roomName;

	public Beacon(String nameSpace, String instance, String roomName) {
		this.nameSpace = nameSpace;
		this.instance = instance;
		this.roomName = roomName;
	}
}
