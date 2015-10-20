package nu.annat.beacons;

import java.util.ArrayList;
import java.util.List;

public class Lerp {

	public static class Circle {
		public double x;
		public double y;
		public double r;

		public Circle(double x, double y, double r) {
			this.x = x;
			this.y = y;
			this.r = r;
		}
	}

	public class Point {
		double x;
		double y;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}

	private List<Circle> circles = new ArrayList<>();
	public Point p1;
	public Point p2;
	public Point p3;
	public Point p4;

	public void addCircle(double x, double y, double radius) {
		circles.add(new Circle(x, y, radius));
	}

	public Point getCenter() {




		p1 = lerp(circles.get(0), circles.get(1));
		p2 = lerp(circles.get(2), circles.get(3));
		p3 = lerp(circles.get(1), circles.get(2));
		p4 = lerp(circles.get(0), circles.get(3));

		return intersection(p1, p2, p3, p4);

	}

	private Point lerp(Circle c1, Circle c2) {
		double deltaX = c2.x - c1.x;
		double deltaY = c2.y - c1.y;
		double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

		double measuredDist = dist/(c1.r + c2.r);
		double percentFromC1 = c1.r * measuredDist;

		return new Point(
			c1.x + (deltaX * percentFromC1),
			c1.y + (deltaY * percentFromC1));
	}

	public Point intersection(Point p1, Point p2, Point p3, Point p4) {
		double d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x);
		if (d == 0) return null;

		double xi = ((p3.x - p4.x) * (p1.x * p2.y - p1.y * p2.x) - (p1.x - p2.x) * (p3.x * p4.y - p3.y * p4.x)) / d;
		double yi = ((p3.y - p4.y) * (p1.x * p2.y - p1.y * p2.x) - (p1.y - p2.y) * (p3.x * p4.y - p3.y * p4.x)) / d;

		Point p = new Point(xi, yi);
		if (xi < Math.min(p1.x,p2.x) || xi > Math.max(p1.x, p2.x)) return null;
		if (xi < Math.min(p3.x, p4.x) || xi > Math.max(p2.x, p4.x)) return null;
		return p;
	}
}
