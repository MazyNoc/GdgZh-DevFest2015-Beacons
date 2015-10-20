package nu.annat.beacons;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CV extends View {
	private Lerp.Point p1;
	private Lerp.Point p2;
	private Lerp.Point p3;
	private Lerp.Point p4;

	public CV(Context context) {
		super(context);
	}

	public CV(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CV(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		if (p1 != null && p2 != null && p3 != null && p4 != null) {
			canvas.drawLine(getPX(p1), getPY(p1), getPX(p2), getPY(p2), paint);
			canvas.drawLine(getPX(p3), getPY(p3), getPX(p4), getPY(p4), paint);
		}
	}

	private float getPY(Lerp.Point p1) {
		return (float) (getHeight() - (p1.y * getHeight()));
	}

	private float getPX(Lerp.Point p1) {
		return (float) (p1.x * getWidth());
	}

	public void setPoint(Lerp.Point p1, Lerp.Point p2, Lerp.Point p3, Lerp.Point p4) {
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.p4 = p4;
		invalidate();
	}
}
