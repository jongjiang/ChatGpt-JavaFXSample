import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

@SuppressWarnings("serial")
public class HighwayInterchange2 extends JPanel {

	// Define constants for road styling
	private static final float MAIN_ROAD_WIDTH = 40f;
	private static final float RAMP_ROAD_WIDTH = 25f;
	private static final float EDGE_LINE_WIDTH = 2f;
	private static final float CENTER_LINE_WIDTH = 1.5f;

	// Define the dotted line pattern for the center line
	private static final float[] DASH_PATTERN = { 10.0f, 10.0f };
	private static final BasicStroke DOTTED_STROKE = new BasicStroke(CENTER_LINE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, DASH_PATTERN, 0.0f);

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;

		// Enable anti-aliasing for smooth drawing
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		// Set background
		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, getWidth(), getHeight());

		// Get canvas dimensions
		int w = getWidth();
		int h = getHeight();
		int cx = w / 2;
		int cy = h / 2;

		// Draw main roads and their edges/center lines
		drawMainRoad(g2d, cx, 0, cx, h, MAIN_ROAD_WIDTH); // Vertical
		drawMainRoad(g2d, 0, cy, w, cy, MAIN_ROAD_WIDTH); // Horizontal

		// Draw ramp roads and their edges/center lines
		// These coordinates are approximations based on the cloverleaf/stack logic.
		int offset = 80;
		int arcSize = 160;
		int arcOffset = 13;
		int arcExtenstion = 64;

		// --- Cloverleaf ramps (Arc2D) ---
		drawRamp(g2d, new Arc2D.Double(cx - offset, cy - offset, arcSize, arcSize, 90 + arcOffset, arcExtenstion, Arc2D.OPEN), RAMP_ROAD_WIDTH); // N2E
		drawRamp(g2d, new Arc2D.Double(cx - offset, cy - offset, arcSize, arcSize, 0 + arcOffset, arcExtenstion, Arc2D.OPEN), RAMP_ROAD_WIDTH); // S2W
		drawRamp(g2d, new Arc2D.Double(cx - offset, cy - offset, arcSize, arcSize, 180 + arcOffset, arcExtenstion, Arc2D.OPEN), RAMP_ROAD_WIDTH); // W2S
		drawRamp(g2d, new Arc2D.Double(cx - offset, cy - offset, arcSize, arcSize, 270 + arcOffset, arcExtenstion, Arc2D.OPEN), RAMP_ROAD_WIDTH); // W2N

		// --- Connector ramps (QuadCurve2D) ---
		// 進主線
		drawConnector(g2d, cx + 250, cy, cx, cy - 250, -50, 50, RAMP_ROAD_WIDTH, -20, -10, 0, 0,  10,  20, -60, -30, 3, 3,  30,  60); // E2N
		drawConnector(g2d, cx - 250, cy, cx, cy + 250, 50, -50, RAMP_ROAD_WIDTH,  20,  10, 0, 0, -10, -20,  60,  30, 3, 3, -30, -60); // W2S
		// 出主線
		drawConnector(g2d, cx + 250, cy, cx, cy + 250, -50, -50, RAMP_ROAD_WIDTH, -60,  30,  30,  30,  30, -60, -30,  10,  30, 10,  10, -30); // S2E
		drawConnector(g2d, cx - 250, cy, cx, cy - 250,  50,  50, RAMP_ROAD_WIDTH,  60, -30, -30, -30, -30,  60,  30, -10, -30, 10, -10,  30);// N2W
	}

	/**
	 * Draws a main road with edges and a center line.
	 * 
	 * @param g2d       The Graphics2D context.
	 * @param x1        The start x-coordinate.
	 * @param y1        The start y-coordinate.
	 * @param x2        The end x-coordinate.
	 * @param y2        The end y-coordinate.
	 * @param roadWidth The width of the road.
	 */
	private void drawMainRoad(Graphics2D g2d, int x1, int y1, int x2, int y2, float roadWidth) {
		// Draw the main road body
		g2d.setColor(Color.DARK_GRAY);
		g2d.setStroke(new BasicStroke(roadWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.drawLine(x1, y1, x2, y2);

		// Draw the white edge lines
		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(EDGE_LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		float halfRoadWidth = roadWidth / 2;
		if (x1 == x2) { // Vertical road
			g2d.drawLine(x1 - (int) halfRoadWidth, y1, x2 - (int) halfRoadWidth, y2);
			g2d.drawLine(x1 + (int) halfRoadWidth, y1, x2 + (int) halfRoadWidth, y2);
		} else { // Horizontal road
			g2d.drawLine(x1, y1 - (int) halfRoadWidth, x2, y2 - (int) halfRoadWidth);
			g2d.drawLine(x1, y1 + (int) halfRoadWidth, x2, y2 + (int) halfRoadWidth);
		}

		// Draw the white dotted center line
		g2d.setColor(Color.WHITE);
		g2d.setStroke(DOTTED_STROKE);
		g2d.drawLine(x1, y1, x2, y2);
	}

	/**
	 * Draws a ramp (Arc2D) with a center line.
	 * 
	 * @param g2d       The Graphics2D context.
	 * @param ramp      The Arc2D shape representing the ramp.
	 * @param roadWidth The width of the road.
	 */
	private void drawRamp(Graphics2D g2d, Arc2D ramp, float roadWidth) {
		// Draw the main road body
		g2d.setColor(Color.DARK_GRAY);
		g2d.setStroke(new BasicStroke(roadWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.draw(ramp);

		// Draw the white edge lines
		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(EDGE_LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		float halfRoadWidth = roadWidth / 2;
		double radius = ramp.getWidth() / 2;
		double centerX = ramp.getCenterX();
		double centerY = ramp.getCenterY();

		// Outer edge arc
		Arc2D outerArc = new Arc2D.Double(centerX - (radius + halfRoadWidth), centerY - (radius + halfRoadWidth), 2 * (radius + halfRoadWidth), 2 * (radius + halfRoadWidth), ramp.getAngleStart(), ramp.getAngleExtent(), Arc2D.OPEN);
		g2d.draw(outerArc);

		// Inner edge arc
		Arc2D innerArc = new Arc2D.Double(centerX - (radius - halfRoadWidth), centerY - (radius - halfRoadWidth), 2 * (radius - halfRoadWidth), 2 * (radius - halfRoadWidth), ramp.getAngleStart(), ramp.getAngleExtent(), Arc2D.OPEN);
		g2d.draw(innerArc);
	}

	private void drawConnector(Graphics2D g2d, int x1, int y1, int x2, int y2, int offsetX, int offsetY, float roadWidth, 
			                       int o1, int o2, int o3, int o4, int o5, int o6, 
			                       int i1, int i2, int i3, int i4, int i5, int i6) {
		g2d.setColor(Color.DARK_GRAY);
		g2d.setStroke(new BasicStroke(roadWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		QuadCurve2D curve = new QuadCurve2D.Float();
		int ctrlX = (x1 + x2) / 2;
		int ctrlY = (y1 + y2) / 2;
		curve.setCurve(x1, y1, ctrlX + offsetX, ctrlY + offsetY, x2, y2);
		g2d.draw(curve);
		
		// Draw the white edge lines
		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(EDGE_LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		// Approximate parallel curves by offsetting control points
		float halfRoadWidth = roadWidth / 2;

		double dx = curve.getX2() - curve.getX1();
		double dy = curve.getY2() - curve.getY1();
		double len = Math.sqrt(dx * dx + dy * dy);
		double nx = -dy / len;
		double ny = dx / len;

		QuadCurve2D outerCurve = new QuadCurve2D.Double(curve.getX1() + nx * halfRoadWidth+o1, curve.getY1() + ny * halfRoadWidth+o2, 
				                                            curve.getCtrlX() + nx * halfRoadWidth+o3, curve.getCtrlY() + ny * halfRoadWidth+o4, 
				                                            curve.getX2() + nx * halfRoadWidth+o5, curve.getY2() + ny * halfRoadWidth+o6);
		g2d.draw(outerCurve);	
		
		QuadCurve2D innerCurve = new QuadCurve2D.Double(curve.getX1() - nx * halfRoadWidth+i1, curve.getY1() - ny * halfRoadWidth+i2, 
				                                            curve.getCtrlX() - nx * halfRoadWidth+i3, curve.getCtrlY() - ny * halfRoadWidth+i4, 
				                                            curve.getX2() - nx * halfRoadWidth+i5, curve.getY2() - ny * halfRoadWidth+i6);
		g2d.draw(innerCurve);
		
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Highway Interchange");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(800, 800);
			frame.setContentPane(new HighwayInterchange2());
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}
