/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package nu.annat.beacon.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

/**
 * An endpoint class we are exposing
 */
@Api(
	name = "myApi",
	version = "v1",
	namespace = @ApiNamespace(
		ownerDomain = "backend.beacon.annat.nu",
		ownerName = "backend.beacon.annat.nu",
		packagePath = ""
	)
)
public class MyEndpoint {

	private static final double MIN_STORED_DURATION = TimeUnit.MINUTES.toMillis(1);

	/**
	 * A simple endpoint method that takes a name and says Hi back
	 */
	@ApiMethod(name = "sayHi")
	public MyBean sayHi(@Named("name") String name) {
		MyBean response = new MyBean();
		response.setData("Hi, " + name);

		return response;
	}

	@ApiMethod(name = "beacons.list")
	public List<Beacon> getBeaconList() {
		List<Beacon> result = new ArrayList<>();
		result.add(new Beacon("edd1ebeac04e5defa017", "f8ca7b174716", "Office"));
		result.add(new Beacon("edd1ebeac04e5defa017", "cc33b9ec1e1b", "LivingRoom"));
		result.add(new Beacon("edd1ebeac04e5defa017", "ee64bd972de3", "Sleeping"));
		return result;
	}

	@ApiMethod(name = "position.update")
	public void updateUserPosition(@Named("entryKey") long entryKey) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
			try {
				Entity oldUserPosition = datastoreService.get(KeyFactory.createKey("UserPosition", entryKey));
				oldUserPosition.setProperty("stopTime", System.currentTimeMillis());
				datastoreService.put(oldUserPosition);
			} catch (EntityNotFoundException e) {
				e.printStackTrace();
			}
	}
	@ApiMethod(name = "position.store")
	public StoredPosition storeUserPosition(UserPosition userPosition) {
		DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
		Entity userPositionEntity = new Entity("UserPosition");
		userPositionEntity.setProperty("userUuid", userPosition.userUUID);
		userPositionEntity.setProperty("startTime", System.currentTimeMillis());
		userPositionEntity.setProperty("stopTime", System.currentTimeMillis());
		userPositionEntity.setProperty("room", userPosition.roomName);
		Key put = datastoreService.put(userPositionEntity);
		StoredPosition storedPosition = new StoredPosition();
		storedPosition.id = put.getId();
		return storedPosition;
	}

	@ApiMethod(name = "position.list")
	public List<UserPosition> getPositionList() {
		DatastoreService dataStore = DatastoreServiceFactory.getDatastoreService();
		Query query = new Query("UserPosition");
		query.addSort("startTime");
		List<Entity> entities = dataStore.prepare(query).asList(FetchOptions.Builder.withDefaults());
		List<UserPosition> userPositions = new ArrayList<>(entities.size());
		for (Entity entity : entities) {
			UserPosition userPosition = new UserPosition();
			userPosition.roomName = (String) entity.getProperty("room");
			userPosition.startTime = (Long) entity.getProperty("startTime");
			userPosition.stopTime = (Long) entity.getProperty("stopTime");
			userPositions.add(userPosition);
		}
		return userPositions;
	}

	@ApiMethod(name = "summary.list")
	public List<RoomSummary> getPositionSummaryList() {
		List<UserPosition> positionList = getPositionList();
		List<RoomSummary> result = new ArrayList<>();

		String lastRoom = null;
		RoomSummary currentRoomSummary = null;
		for (UserPosition userPosition : positionList) {
			if (!userPosition.roomName.equals(lastRoom)) {
				if (currentRoomSummary != null) {
					mergeRoom(result, currentRoomSummary);
				}
				currentRoomSummary = new RoomSummary();
				currentRoomSummary.roomName = userPosition.roomName;
				currentRoomSummary.firstSeen = userPosition.startTime;
				lastRoom = currentRoomSummary.roomName;
			}
			currentRoomSummary.lastSeen = userPosition.stopTime;
		}
		return result;
	}

	private void mergeRoom(List<RoomSummary> result, RoomSummary currentRoomSummary) {
		if(currentRoomSummary.getDuration()> MIN_STORED_DURATION) {
		for (RoomSummary roomSummary : result) {
			if (roomSummary.roomName.equals(currentRoomSummary.roomName)) {
				roomSummary.add(currentRoomSummary);
				return;
			}
		}
			currentRoomSummary.timesVisited++;
			currentRoomSummary.totalTime = currentRoomSummary.getDuration();
			result.add(currentRoomSummary);
		}
	}
}
