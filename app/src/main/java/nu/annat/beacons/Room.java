package nu.annat.beacons;

import java.util.ArrayList;
import java.util.List;

public class Room {

	public class Point {
		double x;
		double y;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}

	class Corner {
		double x;
		double y;
		double distance = 0.5;
		String instance;

		public Corner(double x, double y, String instance) {
			this.x = x;
			this.y = y;
			this.instance = instance;
		}

		@Override
		public String toString() {
			return String.format("%.1f, %.1f : %.2f", x, y, distance);
		}
	}

	List<Corner> corners = new ArrayList<>();
	public Lerp.Point p1;
	public Lerp.Point p2;
	public Lerp.Point p3;
	public Lerp.Point p4;

	/**
	 * 1     2
	 * <p/>
	 * <p/>
	 * 0     3
	 * <p/>
	 * <p/>
	 * 0,1,2
	 * 1,2,3
	 * 2,3,0
	 * 3,0,1
	 */

	public Point getCenter() {

		int[] cornerPos = new int[]{0, 1, 2, 3};
		Point p1 = getCenter(cornerPos);

		return p1;

//		cornerPos = new int[]{1, 2, 3};
//		Point p2 = getCenter(cornerPos);
//
//		cornerPos = new int[]{2, 3, 0};
//		Point p3 = getCenter(cornerPos);
//
//		cornerPos = new int[]{3, 0, 1};
//		Point p4 = getCenter(cornerPos);
//
//		return new Point((p1.x + p2.x + p3.x + p4.x) / 4d, (p1.y + p2.y + p3.y + p4.y) / 4d);
	}

	private Point getCenter(int[] cornerPos) {

		Lerp lerp = new Lerp();
		for (int i : cornerPos) {
			Corner corner = corners.get(i);
			lerp.addCircle(corner.x, corner.y, corner.distance);
		}
		Lerp.Point center = lerp.getCenter();

		p1 = lerp.p1;
		p2 = lerp.p2;
		p3 = lerp.p3;
		p4 = lerp.p4;


		if(center == null) return null;
		return new Point(center.x, center.y);

//		Trilateration trilateration = new Trilateration();
//		for (int i : cornerPos) {
//			Corner corner = corners.get(i);
//			trilateration.addCircle(corner.x, corner.y, corner.distance);
//		}
//		Trilateration.Circle center = trilateration.findCenter();
//		return new Point(center.x, center.y);
	}

	public void addCorner(double x, double y, String instance) {
		corners.add(new Corner(x, y, instance));
	}

	public void updateInstance(String instance, double distance) {
		int length = corners.size();
		for (int i = 0; i < length; i++) {
			Corner corner = corners.get(i);
			if (corner.instance.equals(instance)) {
				corner.distance = distance;
				System.out.println(corner.instance + ", " + distance);
				return;
			}
		}
	}



}
