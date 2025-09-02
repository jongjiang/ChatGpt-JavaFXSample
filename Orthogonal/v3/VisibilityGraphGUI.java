package v3;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// --- 圖形運算與演算法部分 (與之前相同或微調) ---

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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

        if (o1 == 0 && onSegment(p1, p2, q1)) return true;
        if (o2 == 0 && onSegment(p1, q2, q1)) return true;
        if (o3 == 0 && onSegment(p2, p1, q2)) return true;
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;

        return false;
    }

    private static int orientation(Point p, Point q, Point r) {
        double val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
        if (Math.abs(val) < 1e-9) return 0;
        return (val > 0) ? 1 : 2;
    }

    private static boolean onSegment(Point p, Point q, Point r) {
        return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) &&
               q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
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
        return obstacles.stream()
                        .noneMatch(obstacle -> GeometryUtils.doIntersect(path, obstacle));
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

// 6. 可見性圖建構器 (已修正並優化)
class VisibilityGraphBuilder {
    private final List<Point> vertices;
    private final VisibilityChecker checker;
    private final Map<Point, Node> pointToNodeMap = new HashMap<>();

    public VisibilityGraphBuilder(List<Point> vertices, VisibilityChecker checker) {
        this.vertices = vertices;
        this.checker = checker;
    }

    public Graph build() {
        Graph graph = new Graph();
        vertices.forEach(p -> {
            Node node = new Node(p);
            pointToNodeMap.put(p, node);
            graph.adjacencies.put(node, new ArrayList<>());
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

    public Map<Point, Node> getPointToNodeMap() {
        return pointToNodeMap;
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
        path.addAll(target.shortestPath.stream().map(node -> node.point).collect(Collectors.toList()));
        path.add(target.point);
        return path;
    }
}

// --- Swing 繪圖部分 ---

class GraphPanel extends JPanel {
    private final Point startPoint;
    private final Point endPoint;
    private final List<LineSegment> obstacles;
    private final Graph graph;
    private final List<Point> shortestPath;
    private final Map<Point, Node> pointToNodeMap;

    public GraphPanel(Point startPoint, Point endPoint, List<LineSegment> obstacles, Graph graph, List<Point> shortestPath, Map<Point, Node> pointToNodeMap) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.obstacles = obstacles;
        this.graph = graph;
        this.shortestPath = shortestPath;
        this.pointToNodeMap = pointToNodeMap;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 繪製所有可見性圖的邊
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1));
        for (Node sourceNode : graph.adjacencies.keySet()) {
            for (Edge edge : graph.adjacencies.get(sourceNode)) {
                Point p1 = sourceNode.point;
                Point p2 = edge.target.point;
                g2d.drawLine((int)(p1.x * 50), (int)(p1.y * 50), (int)(p2.x * 50), (int)(p2.y * 50));
            }
        }

        // 繪製障礙物
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        for (LineSegment obstacle : obstacles) {
            g2d.drawLine((int)(obstacle.p1.x * 50), (int)(obstacle.p1.y * 50), (int)(obstacle.p2.x * 50), (int)(obstacle.p2.y * 50));
        }

        // 繪製最短路徑
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3));
        for (int i = 0; i < shortestPath.size() - 1; i++) {
            Point p1 = shortestPath.get(i);
            Point p2 = shortestPath.get(i + 1);
            g2d.drawLine((int)(p1.x * 50), (int)(p1.y * 50), (int)(p2.x * 50), (int)(p2.y * 50));
        }

        // 繪製所有節點 (障礙物頂點、起點、終點)
        g2d.setColor(Color.BLACK);
        for (Point p : pointToNodeMap.keySet()) {
            g2d.fillOval((int)(p.x * 50 - 5), (int)(p.y * 50 - 5), 10, 10);
            g2d.drawString(p.toString(), (int)(p.x * 50 + 10), (int)(p.y * 50));
        }

        // 特別標示起點和終點
        g2d.setColor(Color.GREEN);
        g2d.fillOval((int)(startPoint.x * 50 - 8), (int)(startPoint.y * 50 - 8), 16, 16);
        g2d.setColor(Color.ORANGE);
        g2d.fillOval((int)(endPoint.x * 50 - 8), (int)(endPoint.y * 50 - 8), 16, 16);

        // 提示文字
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("新細明體", Font.BOLD, 14));
        g2d.drawString("綠色點: 起點", 10, 520);
        g2d.drawString("橘色點: 終點", 10, 540);
        g2d.drawString("紅色線: 障礙物", 10, 560);
        g2d.drawString("灰色線: 可見性圖的邊", 10, 580);
        g2d.drawString("藍色線: 最短路徑", 10, 6100);
        g2d.drawString("最短路徑總長度: " + String.format("%.2f", pointToNodeMap.get(endPoint).distance), 10, 620);
    }
}

public class VisibilityGraphGUI {
    public static void main(String[] args) {
        // Swing 應用程式必須在事件分發執行緒（Event Dispatch Thread, EDT）上執行
        SwingUtilities.invokeLater(() -> {
            // 定義起點、終點和障礙物
            Point startPoint = new Point(1, 1);
            Point endPoint = new Point(9, 9);
            
            // 假設的障礙物列表
        		// 這裡的障礙物是線段，更完整的實現會使用多邊形
            List<LineSegment> obstacles = new ArrayList<>();
            obstacles.add(new LineSegment(new Point(1, 3), new Point(3, 2)));
            obstacles.add(new LineSegment(new Point(3, 2), new Point(9, 8)));
            
            //obstacles.add(new LineSegment(new Point(7, 3), new Point(7, 7)));
            //obstacles.add(new LineSegment(new Point(7, 7), new Point(3, 7)));
            //obstacles.add(new LineSegment(new Point(3, 7), new Point(3, 3)));

            // 收集所有頂點
            List<Point> vertices = new ArrayList<>();
            vertices.add(startPoint);
            vertices.add(endPoint);
//            vertices.add(new Point(3, 3));
//            vertices.add(new Point(7, 3));
//            vertices.add(new Point(7, 7));
//            vertices.add(new Point(3, 7));
            
            Random r = new Random(new Date().getTime());
            for (int i=1;i<=9;i+=2) {
            	for (int j=1;j<=9;j+=2) {
            		double d1 = r.nextInt(9) * 0.1;
            		double d2 = r.nextInt(9) * 0.1;
            		vertices.add(new Point(i + d1, j + d2));
            	}
            }

            // 建構可見性圖
            VisibilityChecker checker = new VisibilityChecker(obstacles);
            VisibilityGraphBuilder builder = new VisibilityGraphBuilder(vertices, checker);
            Graph graph = builder.build();
            Map<Point, Node> pointToNodeMap = builder.getPointToNodeMap();

            // 執行 Dijkstra 演算法
            Node startNode = pointToNodeMap.get(startPoint);
            Node endNode = pointToNodeMap.get(endPoint);
            DijkstraAlgorithm.computeShortestPath(graph, startNode);
            List<Point> shortestPath = DijkstraAlgorithm.getShortestPathTo(endNode);

            // 建立並顯示 Swing 視窗
            JFrame frame = new JFrame("可見性圖與最短路徑");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new GraphPanel(startPoint, endPoint, obstacles, graph, shortestPath, pointToNodeMap));
            frame.pack();
            frame.setLocationRelativeTo(null); // 視窗置中
            frame.setVisible(true);
        });
    }
}