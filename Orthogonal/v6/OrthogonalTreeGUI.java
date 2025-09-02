package v6;
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

// 8. 樹狀佈局計算器
class TreeLayoutCalculator {
	public static Map<Node, Point> calculateLayout(Map<Point, Node> pointToNodeMap, List<Edge> edges, int panelWidth, int panelHeight, Node specifiedRoot) {
		Map<Node, List<Node>> adjacencyList = new HashMap<>();

		for (Edge edge : edges) {
			adjacencyList.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge.target);
			adjacencyList.computeIfAbsent(edge.target, k -> new ArrayList<>()).add(edge.source);
		}

		Map<Node, Point> layoutMap = new HashMap<>();
		Set<Node> visited = new HashSet<>();
		List<Node> roots = new ArrayList<>();

		Set<Node> allNodes = new HashSet<>(pointToNodeMap.values());

		// 優先處理指定的根節點
		if (specifiedRoot != null && allNodes.contains(specifiedRoot)) {
			roots.add(specifiedRoot);
			Queue<Node> queue = new LinkedList<>();
			queue.add(specifiedRoot);
			visited.add(specifiedRoot);
			while (!queue.isEmpty()) {
				Node u = queue.poll();
				for (Node v : adjacencyList.getOrDefault(u, Collections.emptyList())) {
					if (!visited.contains(v)) {
						visited.add(v);
						queue.add(v);
					}
				}
			}
		}

		// 處理剩餘的連通分量
		for (Node node : allNodes) {
			if (!visited.contains(node)) {
				roots.add(node);
				Queue<Node> queue = new LinkedList<>();
				queue.add(node);
				visited.add(node);
				while (!queue.isEmpty()) {
					Node u = queue.poll();
					for (Node v : adjacencyList.getOrDefault(u, Collections.emptyList())) {
						if (!visited.contains(v)) {
							visited.add(v);
							queue.add(v);
						}
					}
				}
			}
		}

		// 計算並分配每個根節點的佈局
		double currentX = 50.0;
		double horizontalSpacing = 20.0;
		double verticalSpacing = 100.0;

		for (Node root : roots) {
			Map<Node, List<Node>> tree = buildTreeFromGraph(root, adjacencyList, allNodes);
			Map<Node, Double> xPositions = new HashMap<>();

			// 遞歸計算 x 位置
			assignPositions(root, tree, xPositions, 0.0, horizontalSpacing);

			// 調整 x 位置，使其從 currentX 開始
			double minX = xPositions.values().stream().min(Double::compare).orElse(0.0);
			for (Node node : xPositions.keySet()) {
				double adjustedX = xPositions.get(node) - minX + currentX;
				double y = 50 + getNodeDepth(node, tree, root) * verticalSpacing;
				layoutMap.put(node, new Point(adjustedX, y));
			}

			double maxX = xPositions.values().stream().max(Double::compare).orElse(0.0);
			currentX += (maxX - minX) + 50;
		}

		return layoutMap;
	}

	private static Map<Node, List<Node>> buildTreeFromGraph(Node root, Map<Node, List<Node>> adjacencyList, Set<Node> validNodes) {
		Map<Node, List<Node>> tree = new HashMap<>();
		Queue<Node> queue = new LinkedList<>();
		Set<Node> visited = new HashSet<>();

		queue.add(root);
		visited.add(root);
		tree.put(root, new ArrayList<>());

		while (!queue.isEmpty()) {
			Node parent = queue.poll();
			for (Node child : adjacencyList.getOrDefault(parent, Collections.emptyList())) {
				if (validNodes.contains(child) && !visited.contains(child)) {
					visited.add(child);
					tree.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
					queue.add(child);
				}
			}
		}
		return tree;
	}

// 計算每個節點的深度
	private static int getNodeDepth(Node node, Map<Node, List<Node>> tree, Node root) {
		if (node.equals(root)) {
			return 0;
		}

		Queue<Node> q = new LinkedList<>();
		Map<Node, Integer> depths = new HashMap<>();
		q.add(root);
		depths.put(root, 0);

		while (!q.isEmpty()) {
			Node current = q.poll();
			if (current.equals(node)) {
				return depths.get(current);
			}
			for (Node child : tree.getOrDefault(current, Collections.emptyList())) {
				if (!depths.containsKey(child)) {
					depths.put(child, depths.get(current) + 1);
					q.add(child);
				}
			}
		}
		return -1; // 未找到
	}

	/**
	 * 遞歸地分配節點的 x 座標，確保子樹緊湊排列。 葉子節點首先被分配，然後父節點被置於其子節點的中心。
	 */
	private static void assignPositions(Node node, Map<Node, List<Node>> tree, Map<Node, Double> xPositions, double nextX, double horizontalSpacing) {
		List<Node> children = tree.getOrDefault(node, new ArrayList<>());

		if (children.isEmpty()) {
			// 葉子節點，分配一個新的 x 座標
			xPositions.put(node, nextX);
			return;
		}

		double childrenXSum = 0;
		for (Node child : children) {
			// 遞歸地分配子節點
			assignPositions(child, tree, xPositions, nextX, horizontalSpacing);
			nextX = xPositions.get(child) + horizontalSpacing;
			childrenXSum += xPositions.get(child);
		}

		if (children.size() > 1) {
			// 父節點置於其子節點的中心
			xPositions.put(node, childrenXSum / children.size());
		} else {
			// 只有一個子節點，與子節點 x 相同
			xPositions.put(node, xPositions.get(children.get(0)));
		}
	}
}

// --- Swing 繪圖部分 ---
@SuppressWarnings("serial")
class TreeGraphPanel extends JPanel {
	private final List<Edge> mstEdges;
	private final Map<Point, Node> pointToNodeMap;
	private final Node specifiedRoot;

	private final List<Point> allPoints2;
	private final List<LineSegment> obstacles;

	public TreeGraphPanel(Map<Point, Node> pointToNodeMap, List<Edge> mstEdges, Node specifiedRoot, List<Point> allPoints2, List<LineSegment> obstacles) {
		this.pointToNodeMap = pointToNodeMap;
		this.mstEdges = mstEdges;
		this.specifiedRoot = specifiedRoot;
		this.allPoints2 = allPoints2;
		this.obstacles = obstacles;
		setPreferredSize(new Dimension(1500, 1000));
		setBackground(Color.WHITE);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Map<Node, Point> nodeLayout = TreeLayoutCalculator.calculateLayout(pointToNodeMap, mstEdges, 500, getHeight(), specifiedRoot);
		//Map<Node, Point> nodeLayout = OptimizedLayoutCalculator.calculateLayout(pointToNodeMap, mstEdges, 500, getHeight(), specifiedRoot);

		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(2));
		for (Edge edge : mstEdges) {
			Point p1 = nodeLayout.get(edge.source);
			Point p2 = nodeLayout.get(edge.target);
			if (p1 != null && p2 != null) {
				g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
			}
		}

		// draw tree points
		g2d.setColor(Color.BLACK);
		for (Map.Entry<Node, Point> entry : nodeLayout.entrySet()) {
			Point p = entry.getValue();
			g2d.fillOval((int) p.x - 5, (int) p.y - 5, 10, 10);
			g2d.drawString(entry.getKey().point.toString(), (int) p.x + 10, (int) p.y);
		}

		// paint network
		paint2(g2d);

		// 新增：特別標示指定的根節點
		if (specifiedRoot != null && nodeLayout.containsKey(specifiedRoot)) {
			g2d.setColor(Color.ORANGE);
			Point rootP = nodeLayout.get(specifiedRoot);
			if (rootP != null) {
				g2d.fillOval((int) rootP.x - 8, (int) rootP.y - 8, 16, 16);
				g2d.setColor(Color.BLACK);
				g2d.setFont(new Font("新細明體", Font.BOLD, 14));
				g2d.drawString("Root", (int) rootP.x + 10, (int) rootP.y - 20);
			}
		}

		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("新細明體", Font.BOLD, 14));
		g2d.drawString("藍色線: 最小生成樹 (樹狀佈局)", 10, 580);
	}

	void paint2(Graphics2D g2d) {

		// 繪製障礙物
		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(3));
		for (LineSegment obstacle : obstacles) {
			g2d.drawLine((int) (obstacle.p1.x * 50 + 650), (int) (obstacle.p1.y * 50), (int) (obstacle.p2.x * 50 + 650), (int) (obstacle.p2.y * 50));
		}

		// 繪製最小生成樹的邊
		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(3));
		for (Edge edge : mstEdges) {
			Point p1 = edge.source.point;
			Point p2 = edge.target.point;
			g2d.drawLine((int) (p1.x * 50 + 650), (int) (p1.y * 50), (int) (p2.x * 50 + 650), (int) (p2.y * 50));
		}

		// 繪製所有點
		g2d.setColor(Color.BLACK);
		for (Point p : allPoints2) {
			g2d.fillOval((int) (p.x * 50 - 5 + 650), (int) (p.y * 50 - 5), 10, 10);
			g2d.drawString(p.toString(), (int) (p.x * 50 + 10 + 650), (int) (p.y * 50));
		}

		// 新增：特別標示指定的根節點
		if (specifiedRoot != null) {
			g2d.setColor(Color.ORANGE);
			Point rootP = specifiedRoot.point;
			if (rootP != null) {
				g2d.fillOval((int) (rootP.x * 50 - 8 + 650), (int) (rootP.y * 50 - 8), 16, 16);
				g2d.setColor(Color.BLACK);
				g2d.setFont(new Font("新細明體", Font.BOLD, 14));
				g2d.drawString("Root", (int) (rootP.x * 50 + 10 + 650), (int) (rootP.y * 50 - 10));
			}
		}

	}
}

public class OrthogonalTreeGUI {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			List<LineSegment> obstacles = new ArrayList<>();
			obstacles.add(new LineSegment(new Point(1, 3), new Point(3, 2)));
			obstacles.add(new LineSegment(new Point(3, 2), new Point(9, 8)));
			obstacles.add(new LineSegment(new Point(3, 2), new Point(8, 2)));
			obstacles.add(new LineSegment(new Point(3, 8), new Point(4, 4)));

			List<Point> points = new ArrayList<>();
			List<Point> points2 = new ArrayList<>();
			Random r = new Random(System.currentTimeMillis());

			// 5 x 5 grids
			for (int i = 1; i <= 9; i += 2) {
				for (int j = 1; j <= 9; j += 2) {
					double d1 = r.nextInt(9) * 0.1;
					double d2 = r.nextInt(9) * 0.1;
					Point pt = new Point(i + d1, j + d2);
					Point pt2 = new Point(pt.x, pt.y);
					points.add(new Point(i + d1, j + d2));
					points2.add(pt2);
				}
			}

			Point customRootPoint = new Point(0.5, 0.5);
			points.add(customRootPoint);
			points.add(new Point(9.9, 9.9));
			points2.add(new Point(0.5, 0.5));
			points2.add(new Point(9.9, 9.9));

			Map<Point, Node> pointToNodeMap = points.stream().collect(Collectors.toMap(p -> p, Node::new, (existing, replacement) -> existing));

			VisibilityChecker checker = new VisibilityChecker(obstacles);
			List<Edge> mstEdges = KruskalMST.findMST(points, checker, pointToNodeMap);

			// 指定根節點
			Node specifiedRoot = pointToNodeMap.get(customRootPoint);

			JFrame frame = new JFrame("可見性圖的最小生成樹 (樹狀佈局)");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new TreeGraphPanel(pointToNodeMap, mstEdges, specifiedRoot, points2, obstacles));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}