import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

@SuppressWarnings("serial")
public class HighwayInterchange1 extends JPanel {

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		drawHighwayInterchange((Graphics2D) g);
	}

	private void drawHighwayInterchange(Graphics2D g2d) {
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Set background
		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, getWidth(), getHeight());

		// Set road color
		g2d.setColor(Color.DARK_GRAY);
		g2d.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		int w = getWidth();
		int h = getHeight();
		int cx = w / 2;
		int cy = h / 2;
		int offset = 80;

		// Draw vertical main highway (North-South)
		g2d.drawLine(cx, 0, cx, h);

		// Draw horizontal main highway (East-West)
		g2d.drawLine(0, cy, w, cy);

		// Draw curved ramps (cloverleaf)
		// 進主線
		drawRamp(g2d, cx - offset, cy - offset, -90, 90); // W2N
		drawRamp(g2d, cx - offset, cy - offset, 90, 90); // E2S
		// 出主線
		drawRamp(g2d, cx - offset, cy - offset, 180, 90); // N2E
		drawRamp(g2d, cx - offset, cy - offset, 0, 90); // S2W

		// Draw direct connector ramps (flyovers)
		// 進主線
		drawConnector(g2d, cx + 220, cy, cx, cy - 220, -60, 60); // E2N
		drawConnector(g2d, cx - 220, cy, cx, cy + 220, 60, -60); // W2S
		// 出主線
		drawConnector(g2d, cx + 220, cy, cx, cy + 220, -60, -60); // S2E
		drawConnector(g2d, cx - 220, cy, cx, cy - 220, 60, 60); // N2W
	}

	private void drawRamp(Graphics2D g2d, int x, int y, int startAngle, int arcAngle) {
		int size = 160;
		Arc2D ramp = new Arc2D.Double(x, y, size, size, startAngle, arcAngle, Arc2D.OPEN);
		g2d.draw(ramp);
	}

	private void drawConnector(Graphics2D g2d, int x1, int y1, int x2, int y2, int offsetX, int offsetY) {
		QuadCurve2D curve = new QuadCurve2D.Float();
		int ctrlX = (x1 + x2) / 2;
		int ctrlY = (y1 + y2) / 2;
		curve.setCurve(x1, y1, ctrlX + offsetX, ctrlY + offsetY, x2, y2);
		g2d.draw(curve);
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Highway Interchange - Stack/Clover Hybrid");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);
		frame.setContentPane(new HighwayInterchange1());
		frame.setVisible(true);
	}
}
