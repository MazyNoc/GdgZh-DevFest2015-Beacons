package nu.annat.beacons;

import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class EddyStone {

	private static final char[] HEX = "0123456789abcdef".toCharArray();
	public static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
	public static final ParcelUuid EDDYSTONE_TELEMETRY_UUID = ParcelUuid.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	private final int rssi;
	private final int txPower;
	private final String nameSpace;
	private final String instance;
	private final long time;

	EddyStone(ScanResult result) {
		byte[] serviceData = result.getScanRecord().getServiceData(EDDYSTONE_SERVICE_UUID);
		rssi = result.getRssi();
		txPower = serviceData[1];

		byte[] namespaceBytes = Arrays.copyOfRange(serviceData, 2, 12);
		nameSpace = toHexString(namespaceBytes);
		byte[] instanceBytes = Arrays.copyOfRange(serviceData, 12, 18);
		instance = toHexString(instanceBytes);

		time = System.currentTimeMillis();
	}

	public static String toHexString(byte[] bytes) {
		char[] chars = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int c = bytes[i] & 0xFF;
			chars[i * 2] = HEX[c >>> 4];
			chars[i * 2 + 1] = HEX[c & 0x0F];
		}
		return new String(chars).toLowerCase();
	}

	public String getNameSpace() {
		return nameSpace;
	}

	public String getInstance() {
		return instance;
	}

	public double getDistance() {
		return Math.pow(10, ((txPower - rssi) - 41) / 20.0);
	}

	public long getAge(){
		return System.currentTimeMillis() - time;
	}

	/**
	 *
	 * @return distance + one centimeter per second.
	 */
	public double getAgedDistance() {
		return getDistance() + 0.02*TimeUnit.MILLISECONDS.toSeconds(getAge());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EddyStone eddyStone = (EddyStone) o;

		if (!nameSpace.equals(eddyStone.nameSpace)) return false;
		return instance.equals(eddyStone.instance);
	}

	@Override
	public int hashCode() {
		int result = nameSpace.hashCode();
		result = 31 * result + instance.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return String.format("Rssi: %d, txPower: %d, distance: %.2f, %.2f, %s", rssi, txPower, getDistance(), getAgedDistance(), instance);
	}
}
