package v7;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

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
		return "(" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ")";
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
		if (o3 == 0 && onSegment(p2, q2, p1))
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
	public static List<Edge> findMST(List<Point> vertices, VisibilityChecker checker, Map<Point, Node> pointToNodeMap) {
		List<Edge> allEdges = new ArrayList<>();

		for (int i = 0; i < vertices.size(); i++) {
			for (int j = i + 1; j < vertices.size(); j++) {
				Point p1 = vertices.get(i);
				Point p2 = vertices.get(j);

				if (checker.isVisible(p1, p2)) {
					double distance = p1.distanceTo(p2);
					double weight = distance * distance;
					Node n1 = pointToNodeMap.get(p1);
					Node n2 = pointToNodeMap.get(p2);

					if (n1 != null && n2 != null) {
						allEdges.add(new Edge(n1, n2, weight));
					}
				}
			}
		}

		allEdges.sort(Comparator.comparingDouble(e -> e.weight));
		List<Edge> mst = new ArrayList<>();
		UnionFind unionFind = new UnionFind(new ArrayList<>(pointToNodeMap.values()));

		for (Edge edge : allEdges) {
			if (unionFind.union(edge.source, edge.target)) {
				mst.add(edge);
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

@SuppressWarnings("serial")
class TreeGraphPanel extends JPanel {
	private final List<Edge> mstEdges;
	private final Map<Point, Node> pointToNodeMap;
	private final Node specifiedRoot;

	private final List<Point> allPoints2;
	private final List<LineSegment> obstacles;
	private final Map<Point, Integer> pointNumberMap;

	public TreeGraphPanel(Map<Point, Node> pointToNodeMap, List<Edge> mstEdges, Node specifiedRoot, List<Point> allPoints2, List<LineSegment> obstacles, Map<Point, Integer> pointNumberMap) {
		this.pointToNodeMap = pointToNodeMap;
		this.mstEdges = mstEdges;
		this.specifiedRoot = specifiedRoot;
		this.allPoints2 = allPoints2;
		this.obstacles = obstacles;
		this.pointNumberMap = pointNumberMap;
		setPreferredSize(new Dimension(1700, 1000));
		setBackground(Color.WHITE);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		int xOffset = 250;
		int networkXOffset = 850;

		Map<Node, Point> initialLayout = BuchheimTreeLayout.calculateLayout(pointToNodeMap, mstEdges, 500, getHeight(), specifiedRoot);
		
		Map<Node, Point> nodeLayout = new HashMap<>();
		double offsetX = 0;
		if (specifiedRoot != null && initialLayout.containsKey(specifiedRoot)) {
			double originalRootX = initialLayout.get(specifiedRoot).x;
			offsetX = 205 - originalRootX;
		}
		
		for (Map.Entry<Node, Point> entry : initialLayout.entrySet()) {
			Point originalP = entry.getValue();
			nodeLayout.put(entry.getKey(), new Point(originalP.x + offsetX, originalP.y));
		}
		
		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(2));
		for (Edge edge : mstEdges) {
			Point p1 = nodeLayout.get(edge.source);
			Point p2 = nodeLayout.get(edge.target);
			if (p1 != null && p2 != null) {
				g2d.drawLine((int) (p1.x + xOffset), (int) p1.y, (int) (p2.x + xOffset), (int) p2.y);
			}
		}

		g2d.setColor(Color.BLACK);
		for (Map.Entry<Node, Point> entry : nodeLayout.entrySet()) {
			Point p = entry.getValue();
			g2d.fillOval((int) (p.x - 5 + xOffset), (int) p.y - 5, 10, 10);
			g2d.drawString("x=" + String.valueOf((int) (p.x + xOffset)), (int) (p.x - 5 + xOffset), (int) p.y + 15);
			Integer pointNumber = pointNumberMap.get(entry.getKey().point);
			if (pointNumber != null) {
				g2d.drawString(String.valueOf(pointNumber), (int) (p.x + 10 + xOffset), (int) p.y);
			} else {
				g2d.drawString(entry.getKey().point.toString(), (int) (p.x + 10 + xOffset), (int) p.y);
			}
		}

		if (specifiedRoot != null && nodeLayout.containsKey(specifiedRoot)) {
			g2d.setColor(Color.ORANGE);
			Point rootP = nodeLayout.get(specifiedRoot);
			if (rootP != null) {
				g2d.fillOval((int) (rootP.x - 8 + xOffset), (int) rootP.y - 8, 16, 16);
				g2d.setColor(Color.BLACK);
				g2d.setFont(new Font("新細明體", Font.BOLD, 14));
				g2d.drawString("Root", (int) (rootP.x + 10 + xOffset), (int) rootP.y - 20);
			}
		}
		
		paint2(g2d, networkXOffset);

		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("新細明體", Font.BOLD, 14));
		g2d.drawString("藍色線: 最小生成樹 (樹狀佈局)", 10, 880);
	}

	void paint2(Graphics2D g2d, int xOffset) {

		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(3));
		for (LineSegment obstacle : obstacles) {
			g2d.drawLine((int) (obstacle.p1.x * 50 + xOffset), (int) (obstacle.p1.y * 50), (int) (obstacle.p2.x * 50 + xOffset), (int) (obstacle.p2.y * 50));
		}

		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(3));
		for (Edge edge : mstEdges) {
			Point p1 = edge.source.point;
			Point p2 = edge.target.point;
			g2d.drawLine((int) (p1.x * 50 + xOffset), (int) (p1.y * 50), (int) (p2.x * 50 + xOffset), (int) (p2.y * 50));
		}

		g2d.setColor(Color.BLACK);
		for (Point p : allPoints2) {
			g2d.fillOval((int) (p.x * 50 - 5 + xOffset), (int) (p.y * 50 - 5), 10, 10);
			Integer pointNumber = pointNumberMap.get(p);
			if (pointNumber != null) {
				g2d.drawString(String.valueOf(pointNumber), (int) (p.x * 50 + 10 + xOffset), (int) (p.y * 50));
			} else {
				g2d.drawString(p.toString(), (int) (p.x * 50 + 10 + xOffset), (int) (p.y * 50));
			}
		}

		if (specifiedRoot != null) {
			g2d.setColor(Color.ORANGE);
			Point rootP = specifiedRoot.point;
			if (rootP != null) {
				g2d.fillOval((int) (rootP.x * 50 - 8 + xOffset), (int) (rootP.y * 50 - 8), 16, 16);
				g2d.setColor(Color.BLACK);
				g2d.setFont(new Font("新細明體", Font.BOLD, 14));
				g2d.drawString("Root", (int) (rootP.x * 50 + 10 + xOffset), (int) (rootP.y * 50 - 10));
			}
		}
	}
}

// 主程式 (main) 保持不變
public class OrthogonalTreeGUI {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			List<LineSegment> obstacles = new ArrayList<>();
			obstacles.add(new LineSegment(new Point(1, 3), new Point(3, 2)));
			//obstacles.add(new LineSegment(new Point(3, 2), new Point(9, 8)));
			obstacles.add(new LineSegment(new Point(3, 2), new Point(8, 2)));
			obstacles.add(new LineSegment(new Point(3, 8), new Point(4, 4)));

			List<Point> points = new ArrayList<>();
			List<Point> originalPoints = new ArrayList<>();
			Random r = new Random(System.currentTimeMillis());

			Map<Point, Integer> pointNumberMap = new HashMap<>();
			int pointCounter = 1;

			int maxI = 9;
			int maxJ = 9;
			for (int i = 1; i <= maxI; i += 2) {
				for (int j = 1; j <= maxJ; j += 2) {
					double d1 = r.nextInt(9) * 0.1;
					double d2 = r.nextInt(9) * 0.1;
					Point pt = new Point(i + d1, j + d2);
					Point pt2 = new Point(pt.x, pt.y);
					points.add(new Point(i + d1, j + d2));
					originalPoints.add(pt2);
					pointNumberMap.put(pt, pointCounter++);
				}
			}

			Point pt_1st = new Point(0.5, 0.5);
			Point pt_last = new Point(maxI + 0.9, maxJ + 0.9);
			points.add(pt_1st);
			points.add(pt_last);
			pointNumberMap.put(pt_1st, 0);
			pointNumberMap.put(pt_last, pointCounter++);
			Point customRootPoint = points.get(11);

			Map<Point, Node> pointToNodeMap = points.stream().collect(Collectors.toMap(p -> p, Node::new, (existing, replacement) -> existing));

			VisibilityChecker checker = new VisibilityChecker(obstacles);
			List<Edge> mstEdges = KruskalMST.findMST(points, checker, pointToNodeMap);

			Node specifiedRoot = pointToNodeMap.get(customRootPoint);

			JFrame frame = new JFrame("可見性圖的最小生成樹 (樹狀佈局)");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new JScrollPane(new TreeGraphPanel(pointToNodeMap, mstEdges, specifiedRoot, originalPoints, obstacles, pointNumberMap)));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}