package v6;

import java.util.*;

//... (假設 Point, Node, Edge 等類別都已存在於專案中) ...

public class OptimizedLayoutCalculator {

	public static Map<Node, Point> calculateLayout(Map<Point, Node> pointToNodeMap, List<Edge> edges, int panelWidth, int panelHeight, Node specifiedRoot) {
		Map<Node, List<Node>> adjacencyList = buildAdjacencyList(edges);
		Set<Node> allNodes = new HashSet<>(pointToNodeMap.values());
		Map<Node, Point> layoutMap = new HashMap<>();

		// 步驟 1: 建立樹狀結構
		Map<Node, List<Node>> tree = buildTree(specifiedRoot, adjacencyList, allNodes);

		// 步驟 2: 進行分層佈局並初始化位置
		initializeLayout(tree, layoutMap, specifiedRoot, panelWidth, panelHeight);

		// 步驟 3: (可選) 模擬迭代壓縮
		// 為了確保從上而下的精確排列，我們在這裡不進行複雜的力導向迭代
		// 如果需要壓縮效果，可以啟用以下代碼，但會破壞嚴格的從上而下排列
		/*
		 * int iterations = 100; for (int i = 0; i < iterations; i++) { double gamma = getGamma(i, iterations); relaxNodes(tree, layoutMap, gamma); }
		 */

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
					// 為了確保唯一的父子關係，只將其作為一個父節點的子節點
					tree.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
					queue.add(child);
				}
			}
		}
		return tree;
	}

	/**
	 * 從上而下、由左而右地初始化樹狀佈局。 1. 進行廣度優先搜尋 (BFS) 以確定每個節點的深度。 2. 根據深度將節點分層。 3. 在每一層內，依照節點在 BFS 遍歷中的順序分配 X 座標。
	 */
	private static void initializeLayout(Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, Node root, int panelWidth, int panelHeight) {
		if (tree.isEmpty()) {
			return;
		}

		Map<Node, Integer> depths = new HashMap<>();
		List<List<Node>> levels = new ArrayList<>();
		Queue<Node> queue = new LinkedList<>();

		// 確保根節點存在，否則選擇一個隨機節點
		Node startNode = root;
		if (startNode == null || !tree.containsKey(startNode)) {
			startNode = tree.keySet().stream().findFirst().orElse(null);
		}

		if (startNode == null)
			return;

		queue.add(startNode);
		depths.put(startNode, 0);
		levels.add(new ArrayList<>(Collections.singletonList(startNode)));

		// BFS 遍歷以計算深度並將節點分層
		int maxDepth = 0;
		while (!queue.isEmpty()) {
			Node node = queue.poll();
			int currentDepth = depths.get(node);
			maxDepth = Math.max(maxDepth, currentDepth);

			for (Node child : tree.getOrDefault(node, Collections.emptyList())) {
				if (!depths.containsKey(child)) {
					depths.put(child, currentDepth + 1);
					if (levels.size() <= currentDepth + 1) {
						levels.add(new ArrayList<>());
					}
					levels.get(currentDepth + 1).add(child);
					queue.add(child);
				}
			}
		}

		// 根據分層結果分配 X, Y 座標
		double ySpacing = (double) panelHeight / (maxDepth + 2); // 為頂部和底部留出一些空間
		double totalWidth = (double) panelWidth;

		for (int i = 0; i < levels.size(); i++) {
			List<Node> levelNodes = levels.get(i);
			if (levelNodes.isEmpty())
				continue;

			double xSpacing = totalWidth / (levelNodes.size() + 1);
			for (int j = 0; j < levelNodes.size(); j++) {
				Node node = levelNodes.get(j);
				double x = (j + 1) * xSpacing;
				double y = (i + 1) * ySpacing;
				layoutMap.put(node, new Point(x, y));
			}
		}
	}
}