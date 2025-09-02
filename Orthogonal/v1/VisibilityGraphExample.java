package v1;
import java.util.List;

import v1.GeometryUtils;
import v1.LineSegment;
import v1.Point;
import v1.VisibilityChecker;

import java.util.ArrayList;

// 1. 資料結構：點
class Point {
	double x;
	double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}

// 2. 資料結構：線段
class LineSegment {
	Point p1;
	Point p2;

	public LineSegment(Point p1, Point p2) {
		this.p1 = p1;
		this.p2 = p2;
	}
}

// 3. 幾何運算工具類
class GeometryUtils {
	/**
	 * 判斷兩個線段是否相交。 這裡使用跨乘法（cross product）來判斷方向，這是一種常見的幾何運算技巧。
	 *
	 * @param seg1 第一個線段
	 * @param seg2 第二個線段
	 * @return 如果相交，返回 true；否則返回 false。
	 */
	public static boolean doIntersect(LineSegment seg1, LineSegment seg2) {
		Point p1 = seg1.p1, q1 = seg1.p2;
		Point p2 = seg2.p1, q2 = seg2.p2;

		int o1 = orientation(p1, q1, p2);
		int o2 = orientation(p1, q1, q2);
		int o3 = orientation(p2, q2, p1);
		int o4 = orientation(p2, q2, q1);

		// 一般情況
		if (o1 != o2 && o3 != o4) {
			return true;
		}

		// 處理特殊情況：共線且相交
		if (o1 == 0 && onSegment(p1, p2, q1))
			return true;
		if (o2 == 0 && onSegment(p1, q2, q1))
			return true;
		if (o3 == 0 && onSegment(p2, p1, q2))
			return true;
		if (o4 == 0 && onSegment(p2, q1, q2))
			return true;

		return false;
	}

	// 輔助方法：計算三點的方位（orientation）
	private static int orientation(Point p, Point q, Point r) {
		double val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
		if (Math.abs(val) < 1e-9)
			return 0; // 共線（collinear）
		return (val > 0) ? 1 : 2; // 順時針（clockwise）或逆時針（counterclockwise）
	}

	// 輔助方法：檢查點 q 是否在線段 pr 上（共線情況）
	private static boolean onSegment(Point p, Point q, Point r) {
		return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
	}
}

// 4. 核心邏輯：可見性檢查器
class VisibilityChecker {
	private final List<LineSegment> obstacles;

	public VisibilityChecker(List<LineSegment> obstacles) {
		this.obstacles = obstacles;
	}

	/**
	 * 使用 Java 8 Stream API 檢查兩點之間是否可見。
	 *
	 * @param start 起點
	 * @param end   終點
	 * @return 如果兩點之間沒有障礙物阻擋，返回 true；否則返回 false。
	 */
	public boolean isVisible(Point start, Point end) {
		LineSegment path = new LineSegment(start, end);

		// 使用 Stream API 判斷是否存在任何障礙物與路徑相交
		return obstacles.stream().noneMatch(obstacle -> GeometryUtils.doIntersect(path, obstacle));
	}
}

// 主程式：範例應用
public class VisibilityGraphExample {
	public static void main(String[] args) {
		// 假設的障礙物列表
		// 這裡的障礙物是線段，更完整的實現會使用多邊形
		List<LineSegment> obstacles = new ArrayList<>();
		obstacles.add(new LineSegment(new Point(3, 3), new Point(7, 3))); // 障礙物 1
		obstacles.add(new LineSegment(new Point(3, 3), new Point(3, 7))); // 障礙物 2
		obstacles.add(new LineSegment(new Point(7, 3), new Point(7, 7))); // 障礙物 3
		obstacles.add(new LineSegment(new Point(3, 7), new Point(7, 7))); // 障礙物 4
		// 這個範例中，這四條線段構成了一個正方形障礙物

		VisibilityChecker checker = new VisibilityChecker(obstacles);

		Point start = new Point(0, 0);
		Point end1 = new Point(10, 10); // 可見
		Point end2 = new Point(5, 5); // 不可見 (在障礙物內部)
		Point end3 = new Point(5, 0); // 不可見 (路徑穿過障礙物)

		// 使用 Java 8 Lambda 表達式來打印結果
		System.out.println("從 " + start + " 到 " + end1 + " 是否可見？ " + checker.isVisible(start, end1));
		System.out.println("從 " + start + " 到 " + end2 + " 是否可見？ " + checker.isVisible(start, end2));
		System.out.println("從 " + start + " 到 " + end3 + " 是否可見？ " + checker.isVisible(start, end3));
	}
}