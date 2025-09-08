package v8;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

// 1. Node 類：樹狀圖的資料模型
class Node {
	Node left;
	Node right;
	String id;
	double xPosition;
	double yPosition; // 新增 Y 軸位置

	public Node(String id) {
		this(id, null, null);
	}

	public Node(String id, Node left, Node right) {
		this.id = id;
		this.left = left;
		this.right = right;
	}
}

// 2. AveragePositioner 類：計算節點位置
class AveragePositioner {
	private static final double X_SPACING = 50;
	private static final double Y_SPACING = 50;

	// 初始化所有葉子節點的 X, Y 軸位置
	public void initializeLeafPositions(Node node, AtomicInteger xCounter, int yLevel) {
		if (node == null)
			return;
		if (node.left == null && node.right == null) {
			node.xPosition = xCounter.getAndIncrement() * X_SPACING; // 50是間距
			node.yPosition = yLevel * Y_SPACING;
			return;
		}
		initializeLeafPositions(node.left, xCounter, yLevel + 1);
		initializeLeafPositions(node.right, xCounter, yLevel + 1);
		node.yPosition = yLevel * Y_SPACING;
	}

	// 遞迴計算所有節點的平均 X 軸位置
	public void positionNodes(Node node) {
		if (node == null)
			return;

		positionNodes(node.left);
		positionNodes(node.right);

		if (node.left != null && node.right != null) {
			node.xPosition = (node.left.xPosition + node.right.xPosition) / 2.0;
		}
	}
}

// 3. DendrogramPanel 類：繪圖元件
@SuppressWarnings("serial")
class DendrogramPanel extends JPanel {
	private final Node root;

	public DendrogramPanel(Node root) {
		this.root = root;
		setPreferredSize(new Dimension(800, 600)); // 設定視窗大小
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.translate(20, 20); // 調整繪圖原點，留出邊界

		drawTree(g2d, root);
	}

	// 遞迴繪製樹狀圖
	private void drawTree(Graphics2D g, Node node) {
		if (node == null)
			return;

		// 繪製與子節點的連線
		if (node.left != null) {
			g.drawLine((int) node.xPosition, (int) node.yPosition, (int) node.left.xPosition, (int) node.yPosition);
			g.drawLine((int) node.left.xPosition, (int) node.left.yPosition, (int) node.left.xPosition, (int) node.yPosition);
			drawTree(g, node.left);
		}
		if (node.right != null) {
			g.drawLine((int) node.xPosition, (int) node.yPosition, (int) node.right.xPosition, (int) node.yPosition);
			g.drawLine((int) node.right.xPosition, (int) node.right.yPosition, (int) node.right.xPosition, (int) node.yPosition);
			drawTree(g, node.right);
		}

		// 繪製節點ID
		g.drawString(node.id, (int) node.xPosition - 5, (int) node.yPosition + 15);
	}
}

// 4. SwingDendrogramApp 類：主應用程式
public class SwingDendrogramApp {

	public static void main(String[] args) {
		// 1. 建立樹狀圖結構
		Node leafH = new Node("H");
		Node leafI = new Node("I");
		Node leafJ = new Node("J");
		Node leafK = new Node("K");

		Node leafD = new Node("D");
		Node nodeE = new Node("E", leafH, leafI);
		Node nodeB = new Node("B", leafD, nodeE);

		Node nodeF = new Node("F", leafJ, leafK);
		Node leafG = new Node("G");
		Node nodeC = new Node("C", nodeF, leafG);

		Node rootA = new Node("A", nodeB, nodeC);

		// 2. 執行定位演算法
		AveragePositioner positioner = new AveragePositioner();
		AtomicInteger xCounter = new AtomicInteger(1);
		int yLevel = 1;

		positioner.initializeLeafPositions(rootA, xCounter, yLevel);
		positioner.positionNodes(rootA);

		// 3. 建立並顯示 Swing 視窗
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Average Positioning Dendrogram");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new DendrogramPanel(rootA));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}