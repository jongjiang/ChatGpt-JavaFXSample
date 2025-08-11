import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
好的！下面給你一個Java 8 + Swing的完整範例：用「傳統幾何座標」（Y 向上）來指定圓弧中心、半徑、起始角與掃掠角，
但實際顯示在 Swing 的面板上。做法是對 Graphics2D 施加座標變換：把原點移到面板左下角，然後在 Y 軸做 scale(1, -1) 
翻轉。如此一來，你用的角度就是數學上慣用的：0° 在 +X 方向、角度正向為逆時針。

重點說明
translate(MARGIN, h - MARGIN); scale(1, -1); 這兩步把座標系改成「左下角為原點、Y 向上」。
在此座標系下，Arc2D 的 start 與 extent 就符合傳統幾何：0° 在 +X、正角為逆時針。
任何要「正常方向」顯示的 UI 文字，先用 g2.getTransform() 存起來，畫完幾何圖形後 setTransform(oldTx) 回到螢幕座標，再計算螢幕位置 (sx, sy) 來畫字即可。
**/

@SuppressWarnings("serial")
public class MathArcInSwing extends JPanel {
	private static final int MARGIN = 40;

	// 這裡用「傳統幾何」參數（Y 向上、逆時針為正）
	private double cx = 150; // 圓心 x
	private double cy = 100; // 圓心 y
	private double r = 80; // 半徑
	private double startDeg = 130; // 起始角（度）：期望在第一象限
	private double extentDeg = 70; // 掃掠角（度）：期望逆時針

	public MathArcInSwing() {
		setPreferredSize(new Dimension(520, 360));
		setBackground(Color.white);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int w = getWidth(), h = getHeight();
		AffineTransform oldTx = g2.getTransform();

		// === 建立「數學座標」：左下角為原點、Y 向上 ===
		g2.translate(MARGIN, h - MARGIN);
		g2.scale(1, -1);

		int usableW = w - 2 * MARGIN, usableH = h - 2 * MARGIN;

		// 背景格線 + 座標軸
		g2.setStroke(new BasicStroke(1f));
		g2.setColor(new Color(235, 235, 235));
		for (int x = 0; x <= usableW; x += 50)
			g2.drawLine(x, 0, x, usableH);
		for (int y = 0; y <= usableH; y += 50)
			g2.drawLine(0, y, usableW, y);
		g2.setColor(new Color(70, 70, 70));
		g2.setStroke(new BasicStroke(2f));
		g2.drawLine(0, 0, usableW, 0);
		g2.drawLine(0, 0, 0, usableH);

		// === 把「數學角」轉成 Arc2D 需要的角 ===
		double startJava = -startDeg; // 反號：把 30° 放回第一象限
		double extentJava = -extentDeg; // 反號：視覺上保持逆時針

		// 畫弧
		Arc2D.Double arc = new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, startJava, extentJava, Arc2D.OPEN);
		g2.setColor(new Color(30, 144, 255));
		g2.setStroke(new BasicStroke(3f));
		g2.draw(arc);

		// 畫圓心
		g2.setColor(new Color(220, 20, 60));
		double d = 6;
		g2.fill(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));

		// 起點/終點與方向（在數學座標下計算）
		double radS = Math.toRadians(startDeg);
		double radE = Math.toRadians(startDeg + extentDeg);
		double sx = cx + r * Math.cos(radS), sy = cy + r * Math.sin(radS);
		double ex = cx + r * Math.cos(radE), ey = cy + r * Math.sin(radE);

		g2.setStroke(new BasicStroke(2f));
		g2.setColor(new Color(34, 139, 34));
		g2.draw(new Line2D.Double(cx, cy, sx, sy)); // 起始半徑
		g2.setColor(new Color(178, 34, 34));
		g2.draw(new Line2D.Double(cx, cy, ex, ey)); // 終點半徑

		// 在面板座標系畫文字（要暫時還原變換）
		g2.setTransform(oldTx);
		g2.setColor(Color.BLACK);
		g2.setFont(g2.getFont().deriveFont(13f));

		int labelX = (int) Math.round(MARGIN + sx);
		int labelY = (int) Math.round(h - MARGIN - sy);
		g2.drawString("Start "+startJava+"°", labelX + 6, labelY - 6);

		int labelX2 = (int) Math.round(MARGIN + ex);
		int labelY2 = (int) Math.round(h - MARGIN - ey);
		g2.drawString("End "+(startJava + extentJava)+"°", labelX2 + 6, labelY2 - 6);

		g2.drawString(String.format("數學角度：start=%.0f°, extent=%.0f°（逆時針）", startDeg, extentDeg), 16, 20);

		g2.dispose();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame f = new JFrame("Arc with Math Coordinates (Y Up) - Fixed");
			f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			f.setContentPane(new MathArcInSwing());
			f.pack();
			f.setLocationRelativeTo(null);
			f.setVisible(true);
		});
	}
}
