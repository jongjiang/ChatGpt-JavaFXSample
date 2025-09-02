package v2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

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

// 3. 幾何運算工具類 (與之前相同)
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

// 4. 可見性檢查器 (與之前相同)
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
class Node implements Comparable<Node> {
	Point point;
	double distance = Double.MAX_VALUE;
	List<Node> shortestPath = new LinkedList<>();

	public Node(Point point) {
		this.point = point;
	}

	@Override
	public int compareTo(Node other) {
		return Double.compare(this.distance, other.distance);
	}
}

class Edge {
	Node target;
	double weight;

	public Edge(Node target, double weight) {
		this.target = target;
		this.weight = weight;
	}
}

class Graph {
	Map<Node, List<Edge>> adjacencies = new HashMap<>();

	public void addEdge(Node source, Node target, double weight) {
		adjacencies.computeIfAbsent(source, k -> new ArrayList<>()).add(new Edge(target, weight));
	}
}

//6. 可見性圖建構器
class VisibilityGraphBuilder {
 private final List<Point> vertices;
 private final VisibilityChecker checker;

 public VisibilityGraphBuilder(List<Point> vertices, VisibilityChecker checker) {
     this.vertices = vertices;
     this.checker = checker;
 }

 public Graph build() {
     Graph graph = new Graph();
     Map<Point, Node> pointToNodeMap = new HashMap<>();

     // 修正 1: 先將所有頂點都加入到圖的節點中
     vertices.forEach(p -> {
         Node node = new Node(p);
         pointToNodeMap.put(p, node);
         graph.adjacencies.put(node, new ArrayList<>()); // 預先將所有節點加入 adjacencies
     });

     for (int i = 0; i < vertices.size(); i++) {
         for (int j = i + 1; j < vertices.size(); j++) {
             Point p1 = vertices.get(i);
             Point p2 = vertices.get(j);

             if (checker.isVisible(p1, p2)) {
                 double distance = p1.distanceTo(p2);
                 Node n1 = pointToNodeMap.get(p1);
                 Node n2 = pointToNodeMap.get(p2);
                 graph.addEdge(n1, n2, distance);
                 graph.addEdge(n2, n1, distance);
             }
         }
     }
     return graph;
 }
}

// 7. Dijkstra 演算法
class DijkstraAlgorithm {
	public static void computeShortestPath(Graph graph, Node source) {
		source.distance = 0;
		PriorityQueue<Node> queue = new PriorityQueue<>();
		queue.add(source);

		while (!queue.isEmpty()) {
			Node u = queue.poll();

			for (Edge edge : graph.adjacencies.getOrDefault(u, Collections.emptyList())) {
				Node v = edge.target;
				double weight = edge.weight;

				if (u.distance + weight < v.distance) {
					queue.remove(v);
					v.distance = u.distance + weight;
					v.shortestPath.clear();
					v.shortestPath.addAll(u.shortestPath);
					v.shortestPath.add(u);
					queue.add(v);
				}
			}
		}
	}

	public static List<Point> getShortestPathTo(Node target) {
		List<Point> path = new ArrayList<>();
		path.addAll(target.shortestPath.stream().map(node -> node.point).toList());
		path.add(target.point);
		return path;
	}
}

// 8. 主程式：範例應用
public class VisibilityGraphDijkstra {
	public static void main(String[] args) {
		// 假設的障礙物 (由多個線段組成的多邊形)
		List<LineSegment> obstacles = new ArrayList<>();
		// 障礙物 1: 正方形
		obstacles.add(new LineSegment(new Point(3, 3), new Point(7, 3)));
		obstacles.add(new LineSegment(new Point(7, 3), new Point(7, 7)));
		obstacles.add(new LineSegment(new Point(7, 7), new Point(3, 7)));
		obstacles.add(new LineSegment(new Point(3, 7), new Point(3, 3)));

		// 定義起點、終點和障礙物頂點
		Point startPoint = new Point(1, 1);
		Point endPoint = new Point(9, 9);
		List<Point> vertices = new ArrayList<>();
		vertices.add(startPoint);
		vertices.add(endPoint);
		// 添加所有障礙物的頂點
		vertices.add(new Point(3, 3));
		vertices.add(new Point(7, 3));
		vertices.add(new Point(7, 7));
		vertices.add(new Point(3, 7));

		VisibilityChecker checker = new VisibilityChecker(obstacles);
		VisibilityGraphBuilder builder = new VisibilityGraphBuilder(vertices, checker);
		Graph graph = builder.build();

		// 找到起始節點和終點
		Node startNode = new Node(startPoint);
		Node endNode = new Node(endPoint);

		// 找到圖中對應的 Node 實例
		Node finalStartNode = graph.adjacencies.keySet().stream().filter(n -> n.point.equals(startNode.point))
				                                                     .findFirst()
				                                                     .orElseThrow();
		Node finalEndNode = graph.adjacencies.keySet().stream().filter(n -> n.point.equals(endNode.point))
				                                                   .findFirst()
				                                                   .orElseThrow();

		// 執行 Dijkstra 演算法
		DijkstraAlgorithm.computeShortestPath(graph, finalStartNode);

		// 取得最短路徑和總長度
		List<Point> shortestPath = DijkstraAlgorithm.getShortestPathTo(finalEndNode);
		double totalDistance = finalEndNode.distance;

		// 印出結果
		System.out.println("從 " + startPoint + " 到 " + endPoint + " 的最短路徑為：");
		System.out.println(shortestPath);
		System.out.println("最短路徑總長度： " + String.format("%.2f", totalDistance));
	}
}