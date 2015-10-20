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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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

	private static Logger log = Logger.getLogger("MyEndPoint");
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

	@ApiMethod(name = "summary.movingAverage")
	public List<MovingAverage> movingAverage() {
		List<MovingAverage> result = new ArrayList<>();
		List<UserPosition> positionList = getPositionList();
		long firstTime = positionList.get(0).startTime;
		long lastTime = positionList.get(positionList.size()-1).stopTime;
		for (long i = firstTime; i < lastTime; i += TimeUnit.MINUTES.toMillis(1)) {
			Map<String, Long> timesBetween = getTimesBetween(firstTime, firstTime + TimeUnit.MINUTES.toMillis(10), positionList);
			result.add(calculateAverage(firstTime + TimeUnit.MINUTES.toMillis(10), timesBetween));
		}
		return result;
	}

	private MovingAverage calculateAverage(long l, Map<String, Long> timesBetween) {
		MovingAverage result = new MovingAverage();
		result.time = l;
		float total = 0;
		for (Long aLong : timesBetween.values()) {
			total += aLong;
		}

		for (Map.Entry<String, Long> stringLongEntry : timesBetween.entrySet()) {
			RoomCoverage coverage = new RoomCoverage();
			coverage.name = stringLongEntry.getKey();
			coverage.percent = stringLongEntry.getValue() / total;
			result.roomCoverageList.add(coverage);
		}
		Collections.sort(result.roomCoverageList, new Comparator<RoomCoverage>() {
			@Override
			public int compare(RoomCoverage o1, RoomCoverage o2) {
				return o1.name.compareTo(o2.name);
			}
		});
		return result;
	}

	private Map<String, Long> getTimesBetween(long startTime, long endTime, List<UserPosition> positionList) {
		Map<String, Long> result = new HashMap<>();
		for (UserPosition userPosition : positionList) {
			if(userPosition.startTime<endTime && userPosition.stopTime>startTime){
				Long aLong = result.get(userPosition.roomName);
				if(aLong == null) {
					aLong = 0L;
				}
				aLong++;
				result.put(userPosition.roomName, aLong);
			}
		}
		log.info(result.toString());
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
