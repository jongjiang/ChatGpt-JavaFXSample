package v6;

import java.util.*;

//假設 Point, Node, Edge 等類別都已存在於專案中

public class OptimizedLayoutCalculator {

	public static Map<Node, Point> calculateLayout(Map<Point, Node> pointToNodeMap, List<Edge> edges, int panelWidth, int panelHeight, Node specifiedRoot) {
		Map<Node, List<Node>> adjacencyList = buildAdjacencyList(edges);
		Set<Node> allNodes = new HashSet<>(pointToNodeMap.values());
		Map<Node, Point> layoutMap = new HashMap<>();

		if (allNodes.isEmpty()) {
			return layoutMap;
		}

		Map<Node, List<Node>> tree = buildTree(specifiedRoot, adjacencyList, allNodes);
		Map<Node, Integer> depths = initializeLayout(tree, layoutMap, specifiedRoot, panelWidth, panelHeight);

		int iterations = 100;
		for (int i = 0; i < iterations; i++) {
			double gamma = getGamma(i, iterations);
			relaxNodes(tree, layoutMap, gamma, depths, panelWidth, allNodes.size());
		}

		// 新增步驟: 後處理佈局以確保美觀
		postProcessLayout(tree, layoutMap, specifiedRoot);

		return layoutMap;
	}

	private static Map<Node, List<Node>> buildAdjacencyList(List<Edge> edges) {
		Map<Node, List<Node>> adj = new HashMap<>();
		for (Edge edge : edges) {
			adj.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge.target);
			adj.computeIfAbsent(edge.target, k -> new ArrayList<>()).add(edge.source);
		}
		return adj;
	}

	private static Map<Node, List<Node>> buildTree(Node root, Map<Node, List<Node>> adjacencyList, Set<Node> allNodes) {
		Map<Node, List<Node>> tree = new HashMap<>();
		Queue<Node> queue = new LinkedList<>();
		Set<Node> visited = new HashSet<>();

		Node startNode = root;
		if (startNode == null || !allNodes.contains(startNode)) {
			startNode = allNodes.stream().findFirst().orElse(null);
		}

		if (startNode == null)
			return tree;

		queue.add(startNode);
		visited.add(startNode);
		tree.put(startNode, new ArrayList<>());

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

	private static Map<Node, Integer> initializeLayout(Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, Node root, int panelWidth, int panelHeight) {
		Map<Node, Integer> depths = new HashMap<>();
		List<List<Node>> levels = new ArrayList<>();
		Queue<Node> queue = new LinkedList<>();

		Node startNode = root;
		if (startNode == null || !tree.containsKey(startNode)) {
			startNode = tree.keySet().stream().findFirst().orElse(null);
		}

		if (startNode == null)
			return depths;

		queue.add(startNode);
		depths.put(startNode, 0);
		levels.add(new ArrayList<>(Collections.singletonList(startNode)));

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

		double ySpacing = (double) panelHeight / (maxDepth + 2);

		for (int i = 0; i < levels.size(); i++) {
			List<Node> levelNodes = levels.get(i);
			if (levelNodes.isEmpty())
				continue;

			double xSpacing = (double) panelWidth / (levelNodes.size() + 1);
			for (int j = 0; j < levelNodes.size(); j++) {
				Node node = levelNodes.get(j);
				double x = (j + 1) * xSpacing;
				double y = (i + 1) * ySpacing;
				layoutMap.put(node, new Point(x, y));
			}
		}
		return depths;
	}

	private static void relaxNodes(Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, double gamma, Map<Node, Integer> depths, int panelWidth, int totalNodes) {
		double repulsion = panelWidth * 0.5;
		double attraction = repulsion / 20.0;
		double step = 0.05;

		Map<Node, Double> xForces = new HashMap<>();
		for (Node node : layoutMap.keySet()) {
			xForces.put(node, 0.0);
		}

		for (Node node1 : layoutMap.keySet()) {
			for (Node node2 : layoutMap.keySet()) {
				if (node1.equals(node2))
					continue;

				Point p1 = layoutMap.get(node1);
				Point p2 = layoutMap.get(node2);
				if (p1 == null || p2 == null)
					continue;

				double dx = p2.x - p1.x;
				double dist = Math.abs(dx);
				if (dist < 1.0)
					dist = 1.0;

				double repulsiveForce = -repulsion / (dist * dist);
				xForces.put(node1, xForces.get(node1) - Math.signum(dx) * repulsiveForce);

				if (tree.getOrDefault(node1, Collections.emptyList()).contains(node2) || tree.getOrDefault(node2, Collections.emptyList()).contains(node1)) {
					double idealDist = (double) panelWidth / (totalNodes + 1) * gamma;
					double attractiveForce = attraction * (dist - idealDist);
					xForces.put(node1, xForces.get(node1) + Math.signum(dx) * attractiveForce);
				}
			}
		}

		for (Node node : layoutMap.keySet()) {
			Point currentPos = layoutMap.get(node);
			if (currentPos != null) {
				currentPos.x += xForces.get(node) * step;
			}
		}
	}

	private static double getGamma(int currentIteration, int totalIterations) {
		int phase1End = totalIterations - 3;
		if (currentIteration < phase1End) {
			return 3.0 - 2.0 * ((double) currentIteration / phase1End);
		} else {
			return 1.0;
		}
	}

	// ###最終美化與對齊

	/**
	 * 後處理佈局：調整節點的最終X位置，使其更美觀。 該方法主要解決迭代壓縮後，節點之間間距不均勻或父子節點未完美對齊的問題。
	 */
	private static void postProcessLayout(Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, Node specifiedRoot) {
		if (tree.isEmpty()) {
			return;
		}

		Node root = specifiedRoot;
		if (root == null || !tree.containsKey(root)) {
			root = tree.keySet().stream().findFirst().orElse(null);
		}
		if (root == null)
			return;

		// 從底部往上調整，確保子節點的排列能影響父節點的位置
		List<Node> postOrderTraversal = new ArrayList<>();
		Stack<Node> stack = new Stack<>();
		stack.push(root);

		while (!stack.empty()) {
			Node node = stack.pop();
			postOrderTraversal.add(0, node);
			for (Node child : tree.getOrDefault(node, Collections.emptyList())) {
				stack.push(child);
			}
		}

		// 遞迴調整節點的X位置
		adjustXPositions(root, tree, layoutMap);
	}

	private static void adjustXPositions(Node node, Map<Node, List<Node>> tree, Map<Node, Point> layoutMap) {
		List<Node> children = tree.getOrDefault(node, Collections.emptyList());
		if (children.isEmpty()) {
			return;
		}

		// 遞迴處理子節點
		for (Node child : children) {
			adjustXPositions(child, tree, layoutMap);
		}

		// 處理節點自身：將父節點的X位置設為其所有子節點的平均X位置
		double sumX = 0;
		for (Node child : children) {
			sumX += layoutMap.get(child).x;
		}
		double avgX = sumX / children.size();

		Point parentPos = layoutMap.get(node);
		if (parentPos != null) {
			parentPos.x = avgX;
		}
	}
}