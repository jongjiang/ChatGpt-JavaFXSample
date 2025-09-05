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

//8. 樹狀佈局計算器：計算每個節點在面板上的 (x, y) 座標（加入方案A：後處理壓縮）
class TreeLayoutCalculator {

	/**
	 * 計算所有節點的位置，用於樹狀佈局顯示 內含「方案A：後處理壓縮（horizontal compaction）」
	 *
	 * @param pointToNodeMap 點到節點的映射
	 * @param edges          MST 邊集合
	 * @param panelWidth     面板寬度
	 * @param panelHeight    面板高度
	 * @param specifiedRoot  指定根節點 (可為 null)
	 * @return Node -> Point 座標的映射
	 */
	public static Map<Node, Point> calculateLayout(Map<Point, Node> pointToNodeMap, List<Edge> edges, int panelWidth, int panelHeight, Node specifiedRoot) {

		// 1) 建立鄰接表：每個節點 → 鄰居清單 (雙向)
		Map<Node, List<Node>> adjacencyList = new HashMap<>();
		for (Edge edge : edges) {
			adjacencyList.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge.target); //有source就加target
			adjacencyList.computeIfAbsent(edge.target, k -> new ArrayList<>()).add(edge.source); //有target就加source
		}

		// 2) 儲存每個節點的最終座標
		Map<Node, Point> layoutMap = new HashMap<>();

		// 3) 用於 BFS 記錄已拜訪過的節點
		Set<Node> visited = new HashSet<>();

		// 4) 儲存每棵樹的根節點
		List<Node> roots = new ArrayList<>();

		// 5) 所有節點
		Set<Node> allNodes = new HashSet<>(pointToNodeMap.values());

		// 6) 如果有指定根節點，先處理它（把同一連通元件標記 visited）
		if (specifiedRoot != null && allNodes.contains(specifiedRoot)) {
			roots.add(specifiedRoot);
			markComponent(specifiedRoot, adjacencyList, visited);
		}

		// 7) 其他未拜訪節點（其他連通元件）也當作新樹的根
		for (Node node : allNodes) {
			if (!visited.contains(node)) {
				roots.add(node);
				markComponent(node, adjacencyList, visited);
			}
		}

		// 8) 佈局起點與參數
		double currentX = 50.0;
		double startY = 20;
		double minGap = 50.0; // 後處理壓縮時的最小水平間距（可依字型大小調整）

		// 9) 逐一處理每棵樹
		for (Node root : roots) {
			// 9.1 由 MST 建立以 root 為根的單向樹
			Map<Node, List<Node>> tree = buildTreeFromGraph(root, adjacencyList);

			// 9.2 計算每個節點的子樹寬度
			Map<Node, Double> subtreeWidths = new HashMap<>();
			calculateSubtreeWidths(root, tree, subtreeWidths);

			// 9.3 計算整棵樹的最大深度（只用來估算高度置中）
			Map<Node, Integer> depths = new HashMap<>();
			int maxDepth = calculateDepth(root, tree, depths);

			// 9.4 計算整棵樹的總寬度
			double treeWidth = subtreeWidths.getOrDefault(root, 50.0);

			// 9.5 計算水平方向的壓縮比例 (spacingFactor)
			double availableWidth = 50; // panelWidth - 100.0; // 依原始程式保留
			double spacingFactor = 1.0;
			if (treeWidth > 0 && treeWidth < availableWidth) {
				spacingFactor = availableWidth / treeWidth;
			}

			// 9.6 垂直置中
			double finalY = startY;
			double treeHeight = maxDepth * 50;
			double availableHeight = panelHeight - startY - 20;
			if (treeHeight < availableHeight) {
				finalY += (availableHeight - treeHeight) / 2.0;
			}

			// 9.7 根據壓縮後寬度調整起始 X，使整棵樹居中
			double startX = currentX + 450 + (availableWidth - treeWidth * spacingFactor) / 2.0;

			// 9.8 指定節點座標（原演算法）
			assignPositions(root, tree, layoutMap, startX, finalY, subtreeWidths, spacingFactor);

			// 9.9 方案A：後處理壓縮（將各子樹在不重疊下盡量靠攏）
			Map<Node, Integer> depthMap = new HashMap<>();
			computeDepths(root, tree, 0, depthMap);
			horizontalCompaction(root, tree, layoutMap, depthMap, minGap);

			// 9.10 更新下一棵樹的起始 X（以壓縮前 treeWidth 為準；若希望考慮壓縮後寬度，可在此改為 computeWidth）
			currentX += treeWidth * spacingFactor;
		}

		// （可選）將所有 x 平移，讓最小 x >= 0
		normalizeToNonNegative(layoutMap);

		return layoutMap;
	}

	/** BFS 標記從 root 出發的所有連通節點 */
	private static void markComponent(Node root, Map<Node, List<Node>> graph, Set<Node> visited) {
		Queue<Node> q = new LinkedList<>();
		q.add(root);
		visited.add(root);
		while (!q.isEmpty()) {
			Node u = q.poll();
			for (Node v : graph.getOrDefault(u, Collections.emptyList())) {
				if (!visited.contains(v)) {
					visited.add(v);
					q.add(v);
				}
			}
		}
	}

	/** 計算節點深度（回傳最大深度） */
	private static int calculateDepth(Node node, Map<Node, List<Node>> tree, Map<Node, Integer> depths) {
		if (depths.containsKey(node))
			return depths.get(node);
		List<Node> children = tree.getOrDefault(node, new ArrayList<>());
		if (children.isEmpty()) {
			depths.put(node, 1);
			return 1;
		}
		int maxChildDepth = 0;
		for (Node child : children) {
			maxChildDepth = Math.max(maxChildDepth, calculateDepth(child, tree, depths));
		}
		depths.put(node, 1 + maxChildDepth);
		return 1 + maxChildDepth;
	}

	/** 自上而下標記深度（用於後處理壓縮的輪廓疊合） */
	private static void computeDepths(Node node, Map<Node, List<Node>> tree, int depth, Map<Node, Integer> depthMap) {
		depthMap.put(node, depth);
		for (Node child : tree.getOrDefault(node, List.of())) {
			computeDepths(child, tree, depth + 1, depthMap);
		}
	}

	/** 分配節點座標（原有演算法） */
	private static void assignPositions(Node node, Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, double x, double y, Map<Node, Double> subtreeWidths, double spacingFactor) {
		// 1. 設定目前節點的座標
		layoutMap.put(node, new Point(x, y));

		// 2. 若無子節點則返回
		List<Node> children = tree.getOrDefault(node, new ArrayList<>());
		if (children.isEmpty())
			return;

		// 3. 計算子節點總寬度
		double childrenTotalWidth = children.stream().mapToDouble(c -> subtreeWidths.getOrDefault(c, 50.0)).sum();

		// 4. 決定第一個子節點的起始 X（將整個孩子群置中於父 x）
		double currentChildX = x - (childrenTotalWidth * spacingFactor / 2.0);

		// 5. 遞迴分配每個子節點的位置
		for (Node child : children) {
			double childWidth = subtreeWidths.getOrDefault(child, 50.0) * spacingFactor;
			double childX = currentChildX + (childWidth / 2.0);
			assignPositions(child, tree, layoutMap, childX, y + 50, subtreeWidths, spacingFactor);
			currentChildX += childWidth;
		}
	}

	/** 計算每個節點的子樹寬度（原有演算法） */
	private static void calculateSubtreeWidths(Node node, Map<Node, List<Node>> tree, Map<Node, Double> widths) {
		List<Node> children = tree.getOrDefault(node, new ArrayList<>());
		if (children.isEmpty()) {
			widths.put(node, 50.0);
			return;
		}
		double totalWidth = 0.0;
		for (Node child : children) {
			calculateSubtreeWidths(child, tree, widths);
			totalWidth += widths.getOrDefault(child, 50.0);
		}
		widths.put(node, totalWidth);
	}

	/** 由 MST 建立單向樹 (從 root 出發) */
	private static Map<Node, List<Node>> buildTreeFromGraph(Node root, Map<Node, List<Node>> adjacencyList) {
		Map<Node, List<Node>> tree = new HashMap<>();
		Queue<Node> queue = new LinkedList<>();
		Set<Node> visited = new HashSet<>();

		queue.add(root);
		visited.add(root);
		tree.put(root, new ArrayList<>());

		while (!queue.isEmpty()) {
			Node parent = queue.poll();
			for (Node child : adjacencyList.getOrDefault(parent, Collections.emptyList())) {
				if (!visited.contains(child)) {
					visited.add(child);
					tree.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
					tree.computeIfAbsent(child, k -> new ArrayList<>()); // 確保每個節點有條目
					queue.add(child);
				}
			}
		}
		return tree;
	}

	// -------------------------
	// 方案A：後處理壓縮（核心）
	// -------------------------

	/**
	 * 將整棵子樹（以 root 為根）做水平壓縮，使各子樹在不重疊前提下盡量靠攏。 採用「逐層掃描 + 子樹輪廓」的近似演算法。
	 */
	private static void horizontalCompaction(Node root, Map<Node, List<Node>> tree, Map<Node, Point> layout, Map<Node, Integer> depthMap, double minGap) {
		// 依深度分組
		Map<Integer, List<Node>> byDepth = groupNodesByDepth(root, tree, depthMap);
		List<Integer> depths = new ArrayList<>(byDepth.keySet());
		Collections.sort(depths);

		// 逐層處理：將同層的子樹從左到右掃，碰到重疊就把右側子樹往左推
		for (int d : depths) {
			List<Node> level = new ArrayList<>(byDepth.get(d));
			level.sort(Comparator.comparingDouble(n -> layout.get(n).x));

			Contour acc = null; // 累積已放置之子樹的「右輪廓」

			for (Node u : level) {
				// 以 u 為根的子樹輪廓
				Contour cu = buildContour(u, tree, layout, depthMap);
				if (acc == null) {
					acc = cu;
					continue;
				}

				// 計算為避免重疊所需的左移量（負值表示往左移）
				double dx = overlapNeeded(acc, cu, minGap);
				if (dx < 0) {
					shiftSubtree(u, tree, layout, dx);
					cu = cu.shifted(dx);
				}
				acc = Contour.merge(acc, cu);
			}
		}
	}

	/** 將整棵子樹（u 為根）水平平移 dx */
	private static void shiftSubtree(Node u, Map<Node, List<Node>> tree, Map<Node, Point> layout, double dx) {
		Point p = layout.get(u);
		layout.put(u, new Point(p.x + dx, p.y));
		for (Node v : tree.getOrDefault(u, List.of())) {
			shiftSubtree(v, tree, layout, dx);
		}
	}

	/** 將所有 x 平移，使最小 x >= 0（可選） */
	private static void normalizeToNonNegative(Map<Node, Point> layout) {
		double minX = Double.POSITIVE_INFINITY;
		for (Point p : layout.values())
			minX = Math.min(minX, p.x);
		if (minX < 0) {
			double dx = -minX + 5; // 留 5px 邊距
			for (Map.Entry<Node, Point> e : layout.entrySet()) {
				Point p = e.getValue();
				e.setValue(new Point(p.x + dx, p.y));
			}
		}
	}

	/** 以 root 為根，建立每個節點所屬深度的分組 */
	private static Map<Integer, List<Node>> groupNodesByDepth(Node root, Map<Node, List<Node>> tree, Map<Node, Integer> depthMap) {
		Map<Integer, List<Node>> byDepth = new HashMap<>();
		Queue<Node> q = new LinkedList<>();
		Set<Node> vis = new HashSet<>();
		q.add(root);
		vis.add(root);
		while (!q.isEmpty()) {
			Node u = q.poll();
			int d = depthMap.getOrDefault(u, 0);
			byDepth.computeIfAbsent(d, k -> new ArrayList<>()).add(u);
			for (Node v : tree.getOrDefault(u, List.of())) {
				if (!vis.contains(v)) {
					vis.add(v);
					q.add(v);
				}
			}
		}
		return byDepth;
	}

	// -------------------------
	// 輪廓（contour）輔助結構與運算
	// -------------------------

	/**
	 * 子樹輪廓：紀錄各深度的最左/最右 x。 使用絕對深度（與 depthMap 一致），以便跨子樹比較。
	 */
	private static class Contour {
		final Map<Integer, Double> left = new HashMap<>();
		final Map<Integer, Double> right = new HashMap<>();

		void include(int depth, double x) {
			left.put(depth, Math.min(left.getOrDefault(depth, x), x));
			right.put(depth, Math.max(right.getOrDefault(depth, x), x));
		}

		Contour shifted(double dx) {
			Contour c = new Contour();
			for (Map.Entry<Integer, Double> e : left.entrySet())
				c.left.put(e.getKey(), e.getValue() + dx);
			for (Map.Entry<Integer, Double> e : right.entrySet())
				c.right.put(e.getKey(), e.getValue() + dx);
			return c;
		}

		static Contour merge(Contour a, Contour b) {
			Contour c = new Contour();
			// 合併左界
			Set<Integer> depths = new HashSet<>();
			depths.addAll(a.left.keySet());
			depths.addAll(b.left.keySet());
			for (int d : depths) {
				double la = a.left.getOrDefault(d, Double.POSITIVE_INFINITY);
				double lb = b.left.getOrDefault(d, Double.POSITIVE_INFINITY);
				c.left.put(d, Math.min(la, lb));
			}
			// 合併右界
			depths.clear();
			depths.addAll(a.right.keySet());
			depths.addAll(b.right.keySet());
			for (int d : depths) {
				double ra = a.right.getOrDefault(d, Double.NEGATIVE_INFINITY);
				double rb = b.right.getOrDefault(d, Double.NEGATIVE_INFINITY);
				c.right.put(d, Math.max(ra, rb));
			}
			return c;
		}
	}

	/** 建立以 u 為根之子樹輪廓 */
	private static Contour buildContour(Node u, Map<Node, List<Node>> tree, Map<Node, Point> layout, Map<Node, Integer> depthMap) {
		Contour c = new Contour();
		Deque<Node> stack = new ArrayDeque<>();
		stack.push(u);
		while (!stack.isEmpty()) {
			Node x = stack.pop();
			int d = depthMap.getOrDefault(x, 0);
			c.include(d, layout.get(x).x);
			List<Node> children = tree.getOrDefault(x, List.of());
			for (int i = children.size() - 1; i >= 0; --i) {
				stack.push(children.get(i));
			}
		}
		return c;
	}

	/**
	 * 計算使 cu 與 acc 不重疊所需的位移量（負值 = 往左移）。 overlap = max_over_common_depth (acc.right + minGap - cu.left) 若 overlap <= 0 → 無需移動，回傳 0；否則回傳 -overlap。
	 */
	private static double overlapNeeded(Contour acc, Contour cu, double minGap) {
		double need = Double.NEGATIVE_INFINITY;
		Set<Integer> depths = new HashSet<>(acc.right.keySet());
		depths.retainAll(cu.left.keySet());
		if (depths.isEmpty())
			return 0.0;
		for (int d : depths) {
			double r = acc.right.getOrDefault(d, Double.NEGATIVE_INFINITY);
			double l = cu.left.getOrDefault(d, Double.POSITIVE_INFINITY);
			need = Math.max(need, (r + minGap) - l);
		}
		if (need <= 0)
			return 0.0; // 不需要移動
		return -need; // 往左移
	}

	// （工具）計算目前佈局寬度（可用來比較不同策略的結果）
	static double computeWidth(Map<Node, Point> layout) {
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		for (Point p : layout.values()) {
			min = Math.min(min, p.x);
			max = Math.max(max, p.x);
		}
		return (layout.isEmpty() ? 0 : max - min);
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
	//新增：點編號的映射
	private final Map<Point, Integer> pointNumberMap; 

//修改建構子，接收點編號的映射
	public TreeGraphPanel(Map<Point, Node> pointToNodeMap, List<Edge> mstEdges, Node specifiedRoot, List<Point> allPoints2, List<LineSegment> obstacles, Map<Point, Integer> pointNumberMap) {
		this.pointToNodeMap = pointToNodeMap;
		this.mstEdges = mstEdges;
		this.specifiedRoot = specifiedRoot;
		this.allPoints2 = allPoints2;
		this.obstacles = obstacles;
		this.pointNumberMap = pointNumberMap; // 儲存點編號的映射
		setPreferredSize(new Dimension(1700, 1000));
		setBackground(Color.WHITE);
	}

	@Override
	protected void paintComponent(Graphics g) {

		int xOffset = 10;

		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 修改這裡：從 TreeLayoutCalculator 換成 BuchheimTreeLayout
    //Map<Node, Point> nodeLayout = BuchheimTreeLayout.calculateLayout(pointToNodeMap, mstEdges, 500, getHeight(), specifiedRoot);
    Map<Node, Point> nodeLayout = TreeLayoutCalculator.calculateLayout(pointToNodeMap, mstEdges, 500, getHeight(), specifiedRoot);

		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(2));
		for (Edge edge : mstEdges) {
			Point p1 = nodeLayout.get(edge.source);
			Point p2 = nodeLayout.get(edge.target);
			if (p1 != null && p2 != null) {
				g2d.drawLine((int) (p1.x + xOffset), (int) p1.y, (int) (p2.x + xOffset), (int) p2.y);
			}
		}

		// draw tree points
		g2d.setColor(Color.BLACK);
		for (Map.Entry<Node, Point> entry : nodeLayout.entrySet()) {
			Point p = entry.getValue();
			g2d.fillOval((int) (p.x - 5 + xOffset), (int) p.y - 5, 10, 10);
			g2d.drawString("x=" + String.valueOf((p.x - 5 + xOffset)), (int) (p.x - 5 + xOffset), (int) p.y + 15);
			
			//g2d.drawString(entry.getKey().point.toString(), (int) (p.x + 10 + +xOffset), (int) p.y);
			// 修改這裡：從點編號的映射中取得編號
      Integer pointNumber = pointNumberMap.get(entry.getKey().point);
      if (pointNumber != null) {
          g2d.drawString(String.valueOf(pointNumber), (int) (p.x + 10 + xOffset), (int) p.y);
      } else {
          // 如果找不到編號，仍舊顯示原始座標
          g2d.drawString(entry.getKey().point.toString(), (int) (p.x + 10 + xOffset), (int) p.y);
      }
		}

		// paint network
		paint2(g2d);

		// 新增：特別標示指定的根節點
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

		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("新細明體", Font.BOLD, 14));
		g2d.drawString("藍色線: 最小生成樹 (樹狀佈局)", 10, 880);
	}

	void paint2(Graphics2D g2d) {

		int xOffset = 850;

		// 繪製障礙物
		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(3));
		for (LineSegment obstacle : obstacles) {
			g2d.drawLine((int) (obstacle.p1.x * 50 + xOffset), (int) (obstacle.p1.y * 50), (int) (obstacle.p2.x * 50 + xOffset), (int) (obstacle.p2.y * 50));
		}

		// 繪製最小生成樹的邊
		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(3));
		for (Edge edge : mstEdges) {
			Point p1 = edge.source.point;
			Point p2 = edge.target.point;
			g2d.drawLine((int) (p1.x * 50 + xOffset), (int) (p1.y * 50), (int) (p2.x * 50 + xOffset), (int) (p2.y * 50));
		}

		// 繪製所有點
		g2d.setColor(Color.BLACK);
		for (Point p : allPoints2) {
			g2d.fillOval((int) (p.x * 50 - 5 + xOffset), (int) (p.y * 50 - 5), 10, 10);
			
			//g2d.drawString(p.toString(), (int) (p.x * 50 + 10 + xOffset), (int) (p.y * 50));
			// 修改這裡：從點編號的映射中取得編號
      Integer pointNumber = pointNumberMap.get(p);
      if (pointNumber != null) {
          g2d.drawString(String.valueOf(pointNumber), (int) (p.x * 50 + 10 + xOffset), (int) (p.y * 50));
      } else {
          // 如果找不到編號，仍舊顯示原始座標
          g2d.drawString(p.toString(), (int) (p.x * 50 + 10 + xOffset), (int) (p.y * 50));
      }
		}

		// 新增：特別標示指定的根節點
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

public class OrthogonalTreeGUI {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			List<LineSegment> obstacles = new ArrayList<>();
			obstacles.add(new LineSegment(new Point(1, 3), new Point(3, 2)));
			// obstacles.add(new LineSegment(new Point(3, 2), new Point(9, 8)));
			obstacles.add(new LineSegment(new Point(3, 2), new Point(8, 2)));
			obstacles.add(new LineSegment(new Point(3, 8), new Point(4, 4)));

			List<Point> points = new ArrayList<>();
			// 由於 Kruskal 演算法會對 points 排序，我們需要一個原始的列表來保持點的順序。
			List<Point> originalPoints = new ArrayList<>();
			Random r = new Random(System.currentTimeMillis());

			// 新增：點與編號的映射
			Map<Point, Integer> pointNumberMap = new HashMap<>();
			int pointCounter = 1;
			
			// 5 x 5 grids
			int maxI = 9;
			int maxJ = 13;
			for (int i = 1; i <= maxI; i += 2) {
				for (int j = 1; j <= maxJ; j += 2) {
					double d1 = r.nextInt(9) * 0.1;
					double d2 = r.nextInt(9) * 0.1;
					Point pt = new Point(i + d1, j + d2);
					Point pt2 = new Point(pt.x, pt.y);
					points.add(new Point(i + d1, j + d2));
					originalPoints.add(pt2); // 儲存原始順序的點
					pointNumberMap.put(pt, pointCounter++); // 關聯編號
				}
			}

			Point pt_1st = new Point(0.5, 0.5);
			Point pt_last = new Point(maxI + 0.9, maxJ + 0.9);
			points.add(pt_1st);
			points.add(pt_last);
			originalPoints.add(new Point(0.5, 0.5));
			originalPoints.add(new Point(maxI + 0.9, maxJ + 0.9));
			pointNumberMap.put(pt_1st, 0);
			pointNumberMap.put(pt_last, pointCounter++);
			Point customRootPoint = points.get(r.nextInt(points.size()));

//			Point customRootPoint = new Point(0.5, 0.5);
//			points.add(customRootPoint);
//			points.add(new Point(9.9, 9.9));
//			originalPoints.add(new Point(0.5, 0.5));
//			originalPoints.add(new Point(9.9, 9.9));

			Map<Point, Node> pointToNodeMap = points.stream().collect(Collectors.toMap(p -> p, Node::new, (existing, replacement) -> existing));

			VisibilityChecker checker = new VisibilityChecker(obstacles);
			List<Edge> mstEdges = KruskalMST.findMST(points, checker, pointToNodeMap);

			// 指定根節點
			Node specifiedRoot = pointToNodeMap.get(customRootPoint);

			JFrame frame = new JFrame("可見性圖的最小生成樹 (樹狀佈局)");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			// 傳入 pointNumberMap 和原始 points 列表
			frame.add(new JScrollPane(new TreeGraphPanel(pointToNodeMap, mstEdges, specifiedRoot, originalPoints, obstacles, pointNumberMap)));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}