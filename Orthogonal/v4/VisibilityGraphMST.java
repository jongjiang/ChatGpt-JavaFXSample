package v4;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// 1. 資料結構：點
class Point {
	double x;
	double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double distanceTo(Point other) {
		return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Point point = (Point) o;
		return Double.compare(point.x, x) == 0 && Double.compare(point.y, y) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
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
	public static boolean doIntersect(LineSegment seg1, LineSegment seg2) {
		Point p1 = seg1.p1, q1 = seg1.p2;
		Point p2 = seg2.p1, q2 = seg2.p2;

		int o1 = orientation(p1, q1, p2);
		int o2 = orientation(p1, q1, q2);
		int o3 = orientation(p2, q2, p1);
		int o4 = orientation(p2, q2, q1);

		if (o1 != o2 && o3 != o4) {
			return true;
		}

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

	private static int orientation(Point p, Point q, Point r) {
		double val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
		if (Math.abs(val) < 1e-9)
			return 0;
		return (val > 0) ? 1 : 2;
	}

	private static boolean onSegment(Point p, Point q, Point r) {
		return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
	}
}

// 4. 可見性檢查器
class VisibilityChecker {
	private final List<LineSegment> obstacles;

	public VisibilityChecker(List<LineSegment> obstacles) {
		this.obstacles = obstacles;
	}

	public boolean isVisible(Point start, Point end) {
		LineSegment path = new LineSegment(start, end);
		return obstacles.stream().noneMatch(obstacle -> GeometryUtils.doIntersect(path, obstacle));
	}
}

// 5. 圖的資料結構
class Node {
	Point point;

	public Node(Point point) {
		this.point = point;
	}
}

class Edge {
	Node source;
	Node target;
	double weight;

	public Edge(Node source, Node target, double weight) {
		this.source = source;
		this.target = target;
		this.weight = weight;
	}
}

// 6. 最小生成樹 (MST) 演算法 - Kruskal
class KruskalMST {
	public static List<Edge> findMST(List<Point> vertices, VisibilityChecker checker) {
		List<Edge> allEdges = new ArrayList<>();
		Map<Point, Node> pointToNodeMap = vertices.stream().collect(Collectors.toMap(p -> p, Node::new));

		// 建立所有可見的邊
		for (int i = 0; i < vertices.size(); i++) {
			for (int j = i + 1; j < vertices.size(); j++) {
				Point p1 = vertices.get(i);
				Point p2 = vertices.get(j);

				if (checker.isVisible(p1, p2)) {
					double distance = p1.distanceTo(p2);
					Node n1 = pointToNodeMap.get(p1);
					Node n2 = pointToNodeMap.get(p2);
					allEdges.add(new Edge(n1, n2, distance));
				}
			}
		}

		// 依據權重對邊進行排序
		allEdges.sort(Comparator.comparingDouble(e -> e.weight));

		// 使用 Kruskal 演算法尋找 MST
		List<Edge> mst = new ArrayList<>();
		UnionFind unionFind = new UnionFind(new ArrayList<>(pointToNodeMap.values()));

		for (Edge edge : allEdges) {
			if (unionFind.union(edge.source, edge.target)) {
				mst.add(edge);
				if (mst.size() == vertices.size() - 1) {
					break;
				}
			}
		}
		return mst;
	}
}


//6. 最小生成樹 (MST) 演算法 - Kruskal2
class KruskalMST2 {
	public static List<Edge> findMST(List<Point> vertices, VisibilityChecker checker) {
		List<Edge> allEdges = new ArrayList<>();
		Map<Point, Node> pointToNodeMap = vertices.stream().collect(Collectors.toMap(p -> p, Node::new));

		// 建立所有可見的邊
		for (int i = 0; i < vertices.size(); i++) {
			for (int j = i + 1; j < vertices.size(); j++) {
				Point p1 = vertices.get(i);
				Point p2 = vertices.get(j);

				if (checker.isVisible(p1, p2)) {
					double distance = p1.distanceTo(p2);
					// 這裡就是修改的地方：將權重改為距離的平方
					double weight = distance * distance;
					Node n1 = pointToNodeMap.get(p1);
					Node n2 = pointToNodeMap.get(p2);
					allEdges.add(new Edge(n1, n2, weight));
				}
			}
		}

		// 依據權重對邊進行排序
		allEdges.sort(Comparator.comparingDouble(e -> e.weight));

		// ... (後續的 Kruskal 演算法邏輯保持不變)
		List<Edge> mst = new ArrayList<>();
		UnionFind unionFind = new UnionFind(new ArrayList<>(pointToNodeMap.values()));

		for (Edge edge : allEdges) {
			if (unionFind.union(edge.source, edge.target)) {
				mst.add(edge);
				if (mst.size() == vertices.size() - 1) {
					break;
				}
			}
		}
		return mst;
	}
}


// 7. Union-Find 資料結構 (用於 Kruskal)
class UnionFind {
	private final Map<Node, Node> parent = new HashMap<>();

	public UnionFind(List<Node> nodes) {
		for (Node node : nodes) {
			parent.put(node, node);
		}
	}

	public Node find(Node i) {
		if (parent.get(i) == i) {
			return i;
		}
		Node root = find(parent.get(i));
		parent.put(i, root);
		return root;
	}

	public boolean union(Node i, Node j) {
		Node rootI = find(i);
		Node rootJ = find(j);
		if (!rootI.equals(rootJ)) {
			parent.put(rootI, rootJ);
			return true;
		}
		return false;
	}
}

// --- Swing 繪圖部分 ---

class GraphPanel extends JPanel {
	private final List<LineSegment> obstacles;
	private final List<Edge> mstEdges;
	private final List<Point> allPoints;

	public GraphPanel(List<LineSegment> obstacles, List<Edge> mstEdges, List<Point> allPoints) {
		this.obstacles = obstacles;
		this.mstEdges = mstEdges;
		this.allPoints = allPoints;
		setPreferredSize(new Dimension(800, 600));
		setBackground(Color.WHITE);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 繪製障礙物
		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(3));
		for (LineSegment obstacle : obstacles) {
			g2d.drawLine((int) (obstacle.p1.x * 50), (int) (obstacle.p1.y * 50), (int) (obstacle.p2.x * 50), (int) (obstacle.p2.y * 50));
		}

		// 繪製最小生成樹的邊
		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(3));
		for (Edge edge : mstEdges) {
			Point p1 = edge.source.point;
			Point p2 = edge.target.point;
			g2d.drawLine((int) (p1.x * 50), (int) (p1.y * 50), (int) (p2.x * 50), (int) (p2.y * 50));
		}

		// 繪製所有點
		g2d.setColor(Color.BLACK);
		for (Point p : allPoints) {
			g2d.fillOval((int) (p.x * 50 - 5), (int) (p.y * 50 - 5), 10, 10);
			g2d.drawString(p.toString(), (int) (p.x * 50 + 10), (int) (p.y * 50));
		}

		// 提示文字
		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("新細明體", Font.BOLD, 14));
		g2d.drawString("紅色線: 障礙物", 10, 560);
		g2d.drawString("藍色線: 最小化總邊界的連接 (MST)", 10, 580);
	}
}

public class VisibilityGraphMST {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			// 定義障礙物
			List<LineSegment> obstacles = new ArrayList<>();
			obstacles.add(new LineSegment(new Point(1, 3), new Point(3, 2)));
			obstacles.add(new LineSegment(new Point(3, 2), new Point(9, 8)));
			obstacles.add(new LineSegment(new Point(3, 8), new Point(4, 4)));

			// 隨機生成一系列點
			List<Point> points = new ArrayList<>();
			Random r = new Random(new Date().getTime());
			for (int i = 1; i <= 9; i += 2) {
				for (int j = 1; j <= 9; j += 2) {
					double d1 = r.nextInt(9) * 0.1;
					double d2 = r.nextInt(9) * 0.1;
					points.add(new Point(i + d1, j + d2));
				}
			}
			// add start, end
			points.add(new Point(0.5, 0.5));
			points.add(new Point(9.9, 9.9));

			// 建構可見性檢查器
			VisibilityChecker checker = new VisibilityChecker(obstacles);

			// 尋找最小生成樹
			List<Edge> mstEdges = KruskalMST2.findMST(points, checker);

			// 建立並顯示 Swing 視窗
			JFrame frame = new JFrame("可見性圖的最小生成樹");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new GraphPanel(obstacles, mstEdges, points));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}