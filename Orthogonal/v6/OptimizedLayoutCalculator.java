package v6;
import java.util.*;

// ... (假設 Point, Node, Edge, VisibilityChecker 等類別都已存在於專案中) ...

public class OptimizedLayoutCalculator {

	public static Map<Node, Point> calculateLayout(Map<Point, Node> pointToNodeMap, List<Edge> edges, int panelWidth, int panelHeight, Node specifiedRoot) {
		Map<Node, List<Node>> adjacencyList = buildAdjacencyList(edges);
		Set<Node> allNodes = new HashSet<>(pointToNodeMap.values());
		Map<Node, Point> layoutMap = new HashMap<>();

		// 步驟 1: 建立樹狀結構並初始化佈局
		Map<Node, List<Node>> tree = buildTree(specifiedRoot, adjacencyList, allNodes);
		initializeLayout(tree, layoutMap, specifiedRoot, panelWidth, panelHeight);

		// 步驟 2: 模擬分階段壓縮演算法
		int iterations = 100;
		for (int i = 0; i < iterations; i++) {
			// 根據迭代進度調整 γ 值
			double gamma = getGamma(i, iterations);

			// 執行一次迭代鬆弛
			relaxNodes(tree, layoutMap, gamma);
		}

		return layoutMap;
	}

	// 建構鄰接表
	private static Map<Node, List<Node>> buildAdjacencyList(List<Edge> edges) {
		Map<Node, List<Node>> adj = new HashMap<>();
		for (Edge edge : edges) {
			adj.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge.target);
			adj.computeIfAbsent(edge.target, k -> new ArrayList<>()).add(edge.source);
		}
		return adj;
	}

	// 根據根節點建構一棵樹
	private static Map<Node, List<Node>> buildTree(Node root, Map<Node, List<Node>> adjacencyList, Set<Node> allNodes) {
		Map<Node, List<Node>> tree = new HashMap<>();
		Queue<Node> queue = new LinkedList<>();
		Set<Node> visited = new HashSet<>();

		if (root != null && allNodes.contains(root)) {
			queue.add(root);
			visited.add(root);
			tree.put(root, new ArrayList<>());
		} else if (!allNodes.isEmpty()) {
			Node firstNode = allNodes.iterator().next();
			queue.add(firstNode);
			visited.add(firstNode);
			tree.put(firstNode, new ArrayList<>());
		}

		while (!queue.isEmpty()) {
			Node parent = queue.poll();
			for (Node child : adjacencyList.getOrDefault(parent, Collections.emptyList())) {
				if (!visited.contains(child)) {
					visited.add(child);
					tree.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
					queue.add(child);
				}
			}
		}
		return tree;
	}

	// 初始佈局：使用層次佈局來獲得一個合理的初始位置
	private static void initializeLayout(Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, Node root, int panelWidth, int panelHeight) {
		if (tree.isEmpty()) {
			return;
		}

		Map<Node, Integer> depths = new HashMap<>();
		Queue<Node> queue = new LinkedList<>();
		queue.add(root);
		depths.put(root, 0);

		List<List<Node>> levels = new ArrayList<>();
		levels.add(new ArrayList<>(Collections.singletonList(root)));

		// BFS 遍歷以計算深度
		while (!queue.isEmpty()) {
			Node node = queue.poll();
			int currentDepth = depths.get(node);

			for (Node child : tree.getOrDefault(node, new ArrayList<>())) {
				depths.put(child, currentDepth + 1);
				if (levels.size() <= currentDepth + 1) {
					levels.add(new ArrayList<>());
				}
				levels.get(currentDepth + 1).add(child);
				queue.add(child);
			}
		}

		// 分配初始座標
		double ySpacing = panelHeight / (double) levels.size();
		for (int i = 0; i < levels.size(); i++) {
			List<Node> levelNodes = levels.get(i);
			double xSpacing = panelWidth / (double) (levelNodes.size() + 1);
			for (int j = 0; j < levelNodes.size(); j++) {
				Node node = levelNodes.get(j);
				double x = (j + 1) * xSpacing;
				double y = (i + 0.5) * ySpacing;
				layoutMap.put(node, new Point(x, y));
			}
		}
	}

	// 模擬迭代鬆弛和壓縮
	private static void relaxNodes(Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, double gamma) {
		double repulsion = 50.0; // 模擬節點間的斥力
		double attraction = 10.0; // 模擬邊的引力
		double step = 0.05; // 每次迭代的移動步長

		// 暫存每次迭代的移動量
		Map<Node, Point> forces = new HashMap<>();
		for (Node node : layoutMap.keySet()) {
			forces.put(node, new Point(0, 0));
		}

		// 步驟 A: 計算「力」
		for (Node node1 : layoutMap.keySet()) {
			for (Node node2 : layoutMap.keySet()) {
				if (node1.equals(node2))
					continue;

				Point p1 = layoutMap.get(node1);
				Point p2 = layoutMap.get(node2);
				if (p1 == null || p2 == null)
					continue;

				double dx = p2.x - p1.x;
				double dy = p2.y - p1.y;
				double dist = Math.sqrt(dx * dx + dy * dy);

				// 計算斥力
				if (dist > 0) {
					double repulsiveForce = repulsion / (dist * dist);
					forces.get(node1).x -= dx / dist * repulsiveForce;
					forces.get(node1).y -= dy / dist * repulsiveForce;
				}

				// 計算引力 (只針對有邊相連的節點)
				if (tree.getOrDefault(node1, new ArrayList<>()).contains(node2)) {
					double idealDist = gamma * 10; // 模擬理想距離，w'i 這裡簡化為常數
					double attractiveForce = attraction * (dist - idealDist);
					forces.get(node1).x += dx / dist * attractiveForce;
					forces.get(node1).y += dy / dist * attractiveForce;
				}
			}
		}

		// 步驟 B: 根據力移動節點
		for (Node node : layoutMap.keySet()) {
			Point currentPos = layoutMap.get(node);
			Point force = forces.get(node);
			currentPos.x += force.x * step;
			currentPos.y += force.y * step;
		}

		// 步驟 C: 施加 y 座標的層次約束
		enforceLayering(tree, layoutMap, 100); // 這裡 100 是 y 軸間距
	}

	// 根據迭代進度調整 γ
	private static double getGamma(int currentIteration, int totalIterations) {
		int phase1End = totalIterations - 3;
		if (currentIteration < phase1End) {
			// 在第一階段逐漸減小 γ
			return 3.0 - 2.0 * ((double) currentIteration / phase1End);
		} else {
			// 最後三步使用 γ = 1
			return 1.0;
		}
	}

	// 確保同一層的節點保持在同一水平線上
	private static void enforceLayering(Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, double ySpacing) {
		if (tree.isEmpty())
			return;

		Map<Node, Integer> depths = new HashMap<>();
		Queue<Node> queue = new LinkedList<>();
		Node root = tree.keySet().iterator().next(); // 假設樹非空

		queue.add(root);
		depths.put(root, 0);

		int maxDepth = 0;

		while (!queue.isEmpty()) {
			Node node = queue.poll();
			int currentDepth = depths.get(node);
			maxDepth = Math.max(maxDepth, currentDepth);

			for (Node child : tree.getOrDefault(node, Collections.emptyList())) {
				depths.put(child, currentDepth + 1);
				queue.add(child);
			}
		}

		for (Node node : layoutMap.keySet()) {
			if (depths.containsKey(node)) {
				Point p = layoutMap.get(node);
				p.y = depths.get(node) * ySpacing + 50; // 50 是邊界偏移
			}
		}
	}
}