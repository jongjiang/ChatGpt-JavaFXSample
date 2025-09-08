package v9;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// 定義計算子樹距離的邏輯
@FunctionalInterface
interface DistanceMetric {
	double calculate(List<Node> leaves1, List<Node> leaves2);
}

// 樹狀圖的資料模型
class Node {
	Optional<Node> left;
	Optional<Node> right;
	String id;
	List<Node> leaves;
	double xPosition;
	double yPosition;
	int depth;

	public Node(String id) {
		this(id, Optional.empty(), Optional.empty());
	}

	public Node(String id, Optional<Node> left, Optional<Node> right) {
		this.id = id;
		this.left = left;
		this.right = right;
		this.leaves = new ArrayList<>();
		left.ifPresent(l -> this.leaves.addAll(l.leaves));
		right.ifPresent(r -> this.leaves.addAll(r.leaves));
	}

	// 深度拷貝方法，用於排序
	public Node copy() {
		Node newLeft = this.left.isPresent() ? this.left.get().copy() : null;
		Node newRight = this.right.isPresent() ? this.right.get().copy() : null;
		Node newNode = new Node(this.id, Optional.ofNullable(newLeft), Optional.ofNullable(newRight));
		if (newLeft != null) {
			newNode.leaves.addAll(newLeft.leaves);
		}
		if (newRight != null) {
			newNode.leaves.addAll(newRight.leaves);
		}
		return newNode;
	}
}

// 排序演算法
class OptimalOrderer {
	public void order(Node node, DistanceMetric metric) {
		if (!node.left.isPresent() || !node.right.isPresent()) {
			return;
		}

		node.left.ifPresent(left -> order(left, metric));
		node.right.ifPresent(right -> order(right, metric));

		List<Node> leftLeaves = node.left.get().leaves;
		List<Node> rightLeaves = node.right.get().leaves;

		double distance1 = metric.calculate(leftLeaves, rightLeaves);
		double distance2 = metric.calculate(rightLeaves, leftLeaves);

		if (distance2 < distance1) {
			Node temp = node.left.get();
			node.left = node.right;
			node.right = Optional.of(temp);

			node.leaves.clear();
			node.left.ifPresent(l -> node.leaves.addAll(l.leaves));
			node.right.ifPresent(r -> node.leaves.addAll(r.leaves));
		}
	}
}

// 繪圖元件
class DendrogramPanel extends JPanel {
	private final Node originalRoot;
	private final Node orderedRoot;
	private static final double Y_SPACING = 50;
	private static final double X_SPACING = 50;

	public DendrogramPanel(Node originalRoot, Node orderedRoot) {
		this.originalRoot = originalRoot;
		this.orderedRoot = orderedRoot;
		setPreferredSize(new Dimension(800, 600));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 繪製原始樹
		g2d.setColor(Color.BLUE);
		g2d.drawString("原始樹", 50, 20);
		positionTree(originalRoot, 0);
		g2d.translate(20, 40);
		drawTree(g2d, originalRoot);

		// 繪製排序後的樹
		g2d.setColor(Color.RED);
		g2d.translate(400, -20);
		g2d.drawString("最佳化葉子排序", 50, 20);
		positionTree(orderedRoot, 0);
		g2d.translate(0, 20);
		drawTree(g2d, orderedRoot);
	}

	// 遞迴設定節點位置
	private void positionTree(Node node, int level) {
		if (node == null) {
			return;
		}
		node.depth = level;

		if (!node.left.isPresent() && !node.right.isPresent()) {
			int index = node.leaves.indexOf(node);
			node.xPosition = (index + 1) * X_SPACING;
			node.yPosition = (node.leaves.size()) * Y_SPACING;
			return;
		}

		positionTree(node.left.orElse(null), level + 1);
		positionTree(node.right.orElse(null), level + 1);

		double totalX = 0;
		int count = 0;
		if (node.left.isPresent()) {
			totalX += node.left.get().xPosition;
			count++;
		}
		if (node.right.isPresent()) {
			totalX += node.right.get().xPosition;
			count++;
		}
		node.xPosition = (count > 0) ? totalX / count : 0;
		node.yPosition = node.left.map(l -> l.yPosition).orElse(node.right.get().yPosition) - Y_SPACING;
	}

	// 遞迴繪製樹狀圖
	private void drawTree(Graphics2D g, Node node) {
		
		int yOffset = 150;
		
		if (node == null)
			return;

		g.setColor(Color.GRAY);
		if (node.left.isPresent()) {
			g.drawLine((int) node.xPosition, (int) node.yPosition + yOffset, (int) node.left.get().xPosition, (int) node.yPosition + yOffset);
			g.drawLine((int) node.left.get().xPosition, (int) node.left.get().yPosition + yOffset, (int) node.left.get().xPosition, (int) node.yPosition + yOffset);
			drawTree(g, node.left.get());
		}
		if (node.right.isPresent()) {
			g.drawLine((int) node.xPosition, (int) node.yPosition + yOffset, (int) node.right.get().xPosition, (int) node.yPosition + yOffset);
			g.drawLine((int) node.right.get().xPosition, (int) node.right.get().yPosition + yOffset, (int) node.right.get().xPosition, (int) node.yPosition + yOffset);
			drawTree(g, node.right.get());
		}

		g.setColor(Color.BLACK);
		g.fillOval((int) node.xPosition - 3, (int) node.yPosition - 3 + yOffset, 6, 6);
		g.drawString(node.id, (int) node.xPosition + 5, (int) node.yPosition - 5 + yOffset);
	}
}

// 主應用程式
public class OptimalLeafOrdering {
	public static void main(String[] args) {
		// 1. 建立一個包含葉子節點的原始樹
		Node leaf1 = new Node("A");
		Node leaf2 = new Node("B");
		Node leaf3 = new Node("C");
		Node leaf4 = new Node("D");

		Node node1 = new Node("E", Optional.of(leaf1), Optional.of(leaf3)); // 注意這裡的順序
		Node node2 = new Node("F", Optional.of(leaf2), Optional.of(leaf4));
		Node originalRoot = new Node("G", Optional.of(node1), Optional.of(node2));

		// 2. 複製原始樹，以進行排序
		Node orderedRoot = originalRoot.copy();

		// 3. 執行 Optimal Leaf Ordering
		OptimalOrderer orderer = new OptimalOrderer();
		DistanceMetric metric = (leaves1, leaves2) -> {
	    // 取得所有葉子ID，排序後合併成一個字串
	    String ids1 = leaves1.stream()
	                        .map(node -> node.id)
	                        .sorted()
	                        .collect(Collectors.joining());
	                        
	    String ids2 = leaves2.stream()
	                        .map(node -> node.id)
	                        .sorted()
	                        .collect(Collectors.joining());

	    // 比較兩個合併後的字串，返回一個 Double 值
	    return (double) ids1.compareTo(ids2);
	};
		
		orderer.order(orderedRoot, metric);

		// 4. 建立並顯示 Swing 視窗
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Optimal Leaf Ordering Demo");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new DendrogramPanel(originalRoot, orderedRoot));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}
