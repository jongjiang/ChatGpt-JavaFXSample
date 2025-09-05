import java.util.*;

public class BuchheimTreeLayout {

	public static Map<Node, Point> calculateLayout(Map<Point, Node> pointToNodeMap, List<Edge> edges, int panelWidth, int panelHeight, Node specifiedRoot) {

		Map<Node, Point> layoutMap = new HashMap<>();
		Set<Node> visited = new HashSet<>();
		List<Node> roots = new ArrayList<>();

		Map<Node, List<Node>> adjacencyList = new HashMap<>();
		for (Edge edge : edges) {
			adjacencyList.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge.target);
			adjacencyList.computeIfAbsent(edge.target, k -> new ArrayList<>()).add(edge.source);
		}

		if (specifiedRoot != null && pointToNodeMap.values().contains(specifiedRoot)) {
			roots.add(specifiedRoot);
			markComponent(specifiedRoot, adjacencyList, visited);
		}

		for (Node node : pointToNodeMap.values()) {
			if (!visited.contains(node)) {
				roots.add(node);
				markComponent(node, adjacencyList, visited);
			}
		}

		double currentX = 400;
		double verticalSpacing = 30;
		double horizontalSpacing = 20;

		for (Node root : roots) {
			Map<Node, NodeData> nodeDataMap = new HashMap<>();
			Map<Node, List<Node>> tree = buildTreeFromGraph(root, adjacencyList);

			// 核心演算法的第一遍遍歷
			firstWalk(root, tree, nodeDataMap, horizontalSpacing);

			// 計算根節點的最終偏移量
			NodeData rootData = nodeDataMap.get(root);
			double totalWidth = rootData.subtreeWidth;
			double startX = currentX + (panelWidth - totalWidth) / 4;

			// 核心演算法的第二遍遍歷，計算最終座標
			secondWalk(root, tree, nodeDataMap, layoutMap, startX - rootData.x, verticalSpacing);

			currentX += totalWidth + 50;
		}

		return layoutMap;
	}

	// ------------------- Buchheim 演算法的核心運算 -------------------

	/**
	 * 第一遍遍歷：計算每個節點的初步 x 座標和子樹的輪廓
	 */
	private static void firstWalk(Node node, Map<Node, List<Node>> tree, Map<Node, NodeData> nodeDataMap, double hSpacing) {
		NodeData data = new NodeData();
		nodeDataMap.put(node, data);

		List<Node> children = tree.getOrDefault(node, Collections.emptyList());
		if (children.isEmpty()) {
			data.x = 0;
			data.subtreeWidth = hSpacing;
			return;
		}

		Node previousChild = null;
		for (Node child : children) {
			firstWalk(child, tree, nodeDataMap, hSpacing);
			data.subtreeWidth += nodeDataMap.get(child).subtreeWidth;

			if (previousChild != null) {
				// 修正：使用 getShift 函數來正確處理子樹合併
				double shift = getShift(child, previousChild, tree, nodeDataMap);
				NodeData childData = nodeDataMap.get(child);
				childData.x += shift;
				childData.mod += shift;
			}
			previousChild = child;
		}

		// 將根節點置中於其子樹
		double firstChildX = nodeDataMap.get(children.get(0)).x;
		double lastChildX = nodeDataMap.get(children.get(children.size() - 1)).x;
		data.x = (firstChildX + lastChildX) / 2.0;
	}

	/**
	 * 計算合併兩個相鄰子樹所需的最小平移量
	 */
	private static double getShift(Node right, Node left, Map<Node, List<Node>> tree, Map<Node, NodeData> nodeDataMap) {
		double shift = 0;

		// 遍歷左右子樹的輪廓線，找到最大的重疊部分
		Queue<Node> leftQueue = new LinkedList<>();
		leftQueue.add(left);
		Queue<Node> rightQueue = new LinkedList<>();
		rightQueue.add(right);

		while (!leftQueue.isEmpty() && !rightQueue.isEmpty()) {
			Node leftNode = leftQueue.poll();
			Node rightNode = rightQueue.poll();

			NodeData leftData = nodeDataMap.get(leftNode);
			NodeData rightData = nodeDataMap.get(rightNode);

			double neededShift = (leftData.x + leftData.subtreeWidth) - rightData.x;
			shift = Math.max(shift, neededShift);

			// 將左右子樹的下一層子節點加入佇列
			List<Node> leftChildren = tree.getOrDefault(leftNode, Collections.emptyList());
			if (!leftChildren.isEmpty()) {
				leftQueue.add(leftChildren.get(leftChildren.size() - 1));
			}

			List<Node> rightChildren = tree.getOrDefault(rightNode, Collections.emptyList());
			if (!rightChildren.isEmpty()) {
				rightQueue.add(rightChildren.get(0));
			}
		}

		return shift;
	}

	/**
	 * 第二遍遍歷：根據第一遍的結果，計算每個節點的最終座標
	 */
	private static void secondWalk(Node node, Map<Node, List<Node>> tree, Map<Node, NodeData> nodeDataMap, Map<Node, Point> layoutMap, double x, double y) {
		NodeData data = nodeDataMap.get(node);
		double finalX = x + data.x + data.mod;

		layoutMap.put(node, new Point(finalX, y));

		for (Node child : tree.getOrDefault(node, Collections.emptyList())) {
			secondWalk(child, tree, nodeDataMap, layoutMap, x + data.mod, y + 50);
		}
	}

	// ------------------- 資料結構與輔助方法 -------------------

	private static class NodeData {
		double x = 0;
		double mod = 0;
		double subtreeWidth = 0;
	}

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
					tree.computeIfAbsent(child, k -> new ArrayList<>());
					queue.add(child);
				}
			}
		}
		return tree;
	}
}