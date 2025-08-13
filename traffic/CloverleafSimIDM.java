import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Cloverleaf Interchange Traffic Simulation — Intelligent Driver Model IDM + MOBIL + QuadTree + Multithread + GIF export Java8 + Swing demo.
 * 
 * 三葉草立體交流道交通模擬 — 使用 IDM（智慧駕駛模型）+ MOBIL（變換車道模型）+ QuadTree（空間索引）+ 多執行緒運算 + 轉出 GIF。
 * 
 * 編譯：javac CloverleafSimIDM.java
 * 執行：java CloverleafSimIDM
 * 
 * 操作說明：
 *  Space：暫停／繼續
 *  R：重置
 *  + / -：增加／減少車輛生成頻率
 *  L：切換是否啟用車道變換（MOBIL）
 *  G：開始／停止 GIF 錄製（動畫 GIF 會存到工作目錄）
 *  S：儲存單張 PNG 畫面（frames/yyyymmdd_hhmmss.png）
 *  方向鍵上下：縮放
 *  方向鍵左右：平移（按住 Shift 速度更快）
 */
@SuppressWarnings("serial")
public class CloverleafSimIDM extends JFrame {
	public static void main(String[] args) {
		// Swing 元件需在 Event Dispatch Thread 建立與更新
		SwingUtilities.invokeLater(() -> {
			CloverleafSimIDM f = new CloverleafSimIDM();
			f.setVisible(true);
		});
	}

	public CloverleafSimIDM() {
		super("Cloverleaf Interchange — IDM + MOBIL + QuadTree + Multithread + GIF");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setSize(1200, 900);
		setLocationRelativeTo(null); // 視窗置中
		SimPanel panel = new SimPanel(1200, 900);
		setContentPane(panel); // 將模擬面板設為內容
	}

	// ================================ Panel ================================
	static class SimPanel extends JPanel implements KeyListener, MouseWheelListener {
		final int W, H;           // 畫布寬高（畫面座標）
		final World world;       // 模擬世界（道路、車輛、演算法）
		Timer timer; // Swing 計時器（驅動 60 FPS 更新）
		boolean paused = false;  // 是否暫停
		double dt = 1.0 / 60.0;   // 每幀時間（秒）
		double zoom = 1.0;        // 視圖縮放
		double offsetX = 0, offsetY = 0; // 視圖平移
		boolean mobilEnabled = true;     // 是否啟用 MOBIL 車道變換模型
		final VideoRecorder recorder = new VideoRecorder(); // GIF 錄製器

		public SimPanel(int w, int h) {
			this.W = w;
			this.H = h;
			setBackground(new Color(30, 34, 44)); // 深色背景
			setFocusable(true); // 接收鍵盤事件
			addKeyListener(this);
			addMouseWheelListener(this);
			world = new World(W, H);
			// 以固定 dt 觸發 tick（近似 60 FPS）
			timer = new Timer((int) (1000 * dt), e -> tick());
			timer.start();
		}

		/** 每幀更新：運動學更新→重繪→如正在錄影則擷取一幀 */
		void tick() {
			if (!paused)
				world.update(dt, mobilEnabled);
			repaint();
			// 若正在錄影：以目前面板內容產生一張 BufferedImage 存至 GIF 序列
			if (recorder.isRecording()) {
				BufferedImage frame = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = frame.createGraphics();
				paintComponent(g2); // 直接呼叫繪製邏輯將視圖畫到影像上
				g2.dispose();
				recorder.addFrame(frame, (int) (dt * 1000)); // 指定該幀停留毫秒數
			}
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // 抗鋸齒

			// 建立當前轉換矩陣副本，繪完地圖後要還原
			AffineTransform at = g2.getTransform();
			// 視圖：先移動到面板中心，再套用使用者平移與縮放，最後把世界中心對齊
			g2.translate(W / 2 + offsetX, H / 2 + offsetY);
			g2.scale(zoom, zoom);
			g2.translate(-world.cx, -world.cy);

			world.render(g2); // 繪道路與車輛

			g2.setTransform(at); // 還原轉換矩陣
			drawHUD(g2); // 疊加 HUD 文字
		}

		/** 繪製畫面左上角資訊與快捷鍵提示 */
		void drawHUD(Graphics2D g) {
			g.setColor(new Color(235, 240, 250));
			g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
			String info = String.format("Cars:%d  Spawn:%.2fs  FPS:60  MOBIL:%s  %s  GIF:%s", world.cars.size(), world.spawnEverySec, (mobilEnabled ? "ON" : "OFF"), (paused ? "PAUSED" : "RUNNING"), (recorder.isRecording() ? "REC" : "idle"));
			g.drawString(info, 16, 24);
			g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
			g.drawString("Space=Pause  R=Reset  +/-=Spawn  L=LaneChange  G=GIF  S=SavePNG  Arrows=Pan/Zoom", 16, 44);
		}

		// ---------------- controls -----------------
		@Override
		public void keyTyped(KeyEvent e) {}

		@Override
		public void keyPressed(KeyEvent e) {
			// 鍵盤快捷鍵對應功能
			if (e.getKeyCode() == KeyEvent.VK_SPACE)
				paused = !paused; // 暫停／繼續
			else if (e.getKeyCode() == KeyEvent.VK_R)
				world.reset();
			else if (e.getKeyCode() == KeyEvent.VK_L)
				mobilEnabled = !mobilEnabled; // 切換 MOBIL
			else if (e.getKeyCode() == KeyEvent.VK_G)
				toggleGIF();
			else if (e.getKeyCode() == KeyEvent.VK_S)
				savePNG();
			else if (e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_EQUALS)
				world.spawnEverySec = Math.max(0.2, world.spawnEverySec - 0.1); // 提高出生頻率
			else if (e.getKeyCode() == KeyEvent.VK_MINUS)
				world.spawnEverySec = Math.min(3.0, world.spawnEverySec + 0.1); // 降低出生頻率
			else if (e.getKeyCode() == KeyEvent.VK_UP)
				zoom *= 1.1; // 放大
			else if (e.getKeyCode() == KeyEvent.VK_DOWN)
				zoom /= 1.1; // 縮小
			else if (e.getKeyCode() == KeyEvent.VK_LEFT)
				offsetX += (e.isShiftDown() ? 30 : 10); // 左移（實際視覺向左是 offsetX 正）
			else if (e.getKeyCode() == KeyEvent.VK_RIGHT)
				offsetX -= (e.isShiftDown() ? 30 : 10); // 右移
		}

		@Override
		public void keyReleased(KeyEvent e) {}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			// 滑鼠滾輪縮放
			if (e.getWheelRotation() < 0)
				zoom *= 1.1;
			else
				zoom /= 1.1;
		}

		/** 開／關 GIF 錄影。開始時建立輸出檔案，結束時收尾序列寫入。 */
		void toggleGIF() {
			try {
				if (!recorder.isRecording()) {
					recorder.start(new File("traffic_" + timestamp() + ".gif"));
				} else {
					recorder.finish();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		/** 另存目前畫面為 PNG 檔（輸出到 frames 目錄）。 */
		void savePNG() {
			try {
				BufferedImage frame = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = frame.createGraphics();
				paintComponent(g2);
				g2.dispose();
				File dir = new File("frames");
				if (!dir.exists())
					dir.mkdirs();
				File f = new File(dir, timestamp() + ".png");
				ImageIO.write(frame, "png", f);
				System.out.println("Saved " + f.getAbsolutePath());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		/** 產生 yyyyMMdd_HHmmss 字串作為檔名用。 */
		static String timestamp() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			return sdf.format(new Date());
		}
	}

	// ================================ World ================================
	static class World {
		final int W, H;                   // 世界尺寸（畫布座標）
		final double cx, cy;              // 世界中心，用於視圖定位
		final Random rng = new Random(2); // 固定種子以利重現
		final List<RoadPath> paths = new ArrayList<>(); // 所有路徑（直線與弧線）
		final List<Car> cars = Collections.synchronizedList(new ArrayList<Car>()); // 車輛清單（同步）
		final List<Lane> lanes = new ArrayList<>(); // 車道包裝（對應到 RoadPath）

		// 車輛生成與上限
		double spawnEverySec = 0.7; // 每隔幾秒生成一輛
		double spawnAcc = 0.0;      // 積算器（達門檻則生成）
		int maxCars = 260;          // 車輛上限

		// IDM 參數（單位換算為畫素尺度，大致視覺合理即可）
		final double v0 = 36.0 * 3;   // 期望速度（px/s）
		final double T = 1.2;         // 期望時距（秒）
		final double aMax = 1.2 * 40; // 最大加速度（px/s^2）
		final double b = 1.5 * 40;    // 舒適減速度（px/s^2）
		final double s0 = 8.0;        // 最小頭距（px）
		final double delta = 4.0;     // 加速度指數

		// MOBIL 參數
		final double politeness = 0.3;            // 禮讓係數（他人效用的權重）
		final double aLaneChangeThreshold = 0.2 * 40; // 變道的最小淨效用門檻
		final double aSafe = -2.0 * 40;           // 目標車道後車允許的最大制動（安全約束）

		// 幾何設置
		final double laneWidth = 18;   // 車道寬
		final double highwayHalf = 160;// 中央隔離帶至邊緣半寬（用於畫中央實體）
		final double outerRadius = 200;// 外弧半徑（匝道）
		final double innerRadius = 140;// 內弧半徑（可用於其他幾何）

		// 空間索引（QuadTree）
		QuadTree qt;
		final Rectangle2D worldBounds;

		// 執行緒池（使用可用核心數-1，至少 2）
		final ExecutorService pool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 1));

		public World(int w, int h) {
			this.W = w;
			this.H = h;
			this.cx = W / 2.0;
			this.cy = H / 2.0;
			worldBounds = new Rectangle2D.Double(0, 0, W, H);
			buildGeometry(); // 建立道路幾何與車道
			buildRouteGraph(); // 建立路徑圖（entry→ramp→exit→目標直線）
		}

		/** 重置世界：清空車輛與生成計數器。 */
		void reset() {
			cars.clear();
			spawnAcc = 0;
		}

		// ------------------ Route Graph ------------------
		// 路徑圖：定義從某車道離開後，下一個允許接續的車道（entry→ramp→exit→目標直線）
		Map<Lane, List<Lane>> routeGraph = new HashMap<>();

		void buildRouteGraph() {
			routeGraph.clear();
			for (Lane l : lanes) routeGraph.put(l, new ArrayList<>());

			// EB -> NB
			connect("Connect_EB_R1", "Ramp_EB_to_NB");
			connect("Ramp_EB_to_NB", "Connect_R1_NB");
			connect("Connect_R1_NB", "NS_North_C_off");

			// NB -> WB
			connect("Connect_NB_R2", "Ramp_NB_to_WB");
			connect("Ramp_NB_to_WB", "Connect_R2_WB");
			connect("Connect_R2_WB", "EW_West_C_off");

			// WB -> SB
			connect("Connect_WB_R3", "Ramp_WB_to_SB");
			connect("Ramp_WB_to_SB", "Connect_R3_SB");
			connect("Connect_R3_SB", "NS_South_C_off");

			// SB -> EB
			connect("Connect_SB_R4", "Ramp_SB_to_EB");
			connect("Ramp_SB_to_EB", "Connect_R4_EB");
			connect("Connect_R4_EB", "EW_East_C_off");
		}

		/** 幫助函式：將名稱前綴相符的車道連成 from → to（以 Lane 為 key/value）。 */
		void connect(String fromName, String toName) {
			Lane from = findLaneByName(fromName);
			Lane to = findLaneByName(toName);
			if (from != null && to != null) {
				routeGraph.get(from).add(to);
			}
		}

		/** 以名稱前綴尋找第一個符合的 Lane。 */
		Lane findLaneByName(String namePrefix) {
			for (Lane l : lanes) {
				if (l.path.name.startsWith(namePrefix))
					return l;
			}
			return null;
		}

		/**
		 * 依決策與安全距離將車輛切換路段：
		 * - 若車輛在直線且 takeRamp=true，於末端附近嘗試切到 entry（通過 canMergeInto）
		 * - 若車輛已在 entry / ramp / exit，於段尾嘗試接到下一段（通過 canMergeInto）
		 */
		void followRoute(Car c) {
			// A) 已在 entry/ramp/exit/connector：用 routeGraph 串接
			List<Lane> chain = routeGraph.get(c.lane);
			if (chain != null && !chain.isEmpty()) {
				if (c.s >= 0.98) {
					Lane nxt = chain.get(0);

					// 用幾何交點決定併入 s
					Double sHit = mergeSAtIntersection(c.lane.path, nxt.path);

					if (sHit != null && gapOK(c, nxt, sHit)) { // 用你的 gap 檢查（見之前的 gapOK）
						c.lane = nxt;
						c.s = sHit;
						if (c.v < 8)
							c.v = 8; // 可選：給點初速
					} else {
						c.s = 0.985;
						c.v = 0; // 等下一個合流窗口
					}
				}
				return;
			}

			// B) 還在直線（決定是否上匝道）
			if (!c.lane.path.isStraight)
				return;

			if (c.takeRamp && c.s >= 0.65) {
				Lane entry = null;
				String n = c.lane.path.name;
				if (n.startsWith("EW_East_C"))
					entry = findLaneByName("Connect_EB_R1");
				else if (n.startsWith("NS_North_C"))
					entry = findLaneByName("Connect_NB_R2");
				else if (n.startsWith("EW_West_C"))
					entry = findLaneByName("Connect_WB_R3");
				else if (n.startsWith("NS_South_C"))
					entry = findLaneByName("Connect_SB_R4");

				if (entry != null) {
					Double sHit = mergeSAtIntersection(c.lane.path, entry.path); // 直線→入口的交點

					if (sHit != null && gapOK(c, entry, sHit)) {
						c.lane = entry;
						c.s = sHit;
						if (c.v < 8)
							c.v = 8;
					} else if (c.s >= 0.98) {
						c.s = 0.985;
						c.v = 0;
					}
				}
			}
		}



		/** 在目標車道 target 的 s≈sOnTarget 處，檢查前後最接近車是否留有足夠安全距離。 */
		boolean canMergeInto(Car me, Lane target, double sOnTarget) {
			Car ahead = null, behind = null;
			double bestAhead = Double.POSITIVE_INFINITY, bestBehind = Double.POSITIVE_INFINITY;

			synchronized (cars) {
				for (Car c : cars) if (c.lane == target && c != me) {
					double ds = c.s - sOnTarget;
					if (ds < 0) ds += 1.0;
					double dpx = ds * target.path.length;
					if (dpx >= 0 && dpx < bestAhead) { bestAhead = dpx; ahead = c; }

					double dsb = sOnTarget - c.s;
					if (dsb < 0) dsb += 1.0;
					double dpxb = dsb * target.path.length;
					if (dpxb >= 0 && dpxb < bestBehind) { bestBehind = dpxb; behind = c; }
				}
			}
			double vM = Math.max(1.0, me.v);
			double sStarMe = s0 + Math.max(0, vM * T);
			double sStarBehind = s0 + Math.max(0, (behind != null ? behind.v : vM) * T);
			boolean okAhead  = (ahead  == null) || (bestAhead  > sStarMe * 1.05); //1.2
			boolean okBehind = (behind == null) || (bestBehind > sStarBehind * 1.05); //1.2
			return okAhead && okBehind;
		}

		/** 建立道路幾何（主幹直線 + 四個匝道弧線 + 進出連接段），並建立對應的 Lane 與相鄰關係。 */
		void buildGeometry() {
			int edge = 60;
			// 主幹道：東西向、南北向，各方向兩車道（以中心線平移得到內外側）
			RoadPath eastCenter = RoadPath.straight(W-edge, cy + laneWidth*1.2, edge, cy + laneWidth*1.2, "EW_East_C");
			RoadPath eastLeft   = eastCenter.offset( laneWidth/2);   // 東向內側
			RoadPath eastRight  = eastCenter.offset(-laneWidth/2);   // 東向外側

			RoadPath westCenter = RoadPath.straight(edge, cy - laneWidth*1.2, W-edge, cy - laneWidth*1.2, "EW_West_C");
			RoadPath westLeft   = westCenter.offset( laneWidth/2);   // 西向內側
			RoadPath westRight  = westCenter.offset(-laneWidth/2);   // 西向外側

			RoadPath southCenter = RoadPath.straight(cx - laneWidth*1.2, edge, cx - laneWidth*1.2, H-edge, "NS_South_C");
			RoadPath southLeft   = southCenter.offset( laneWidth/2); // 南向內側
			RoadPath southRight  = southCenter.offset(-laneWidth/2); // 南向外側

			RoadPath northCenter = RoadPath.straight(cx + laneWidth*1.2, H-edge, cx + laneWidth*1.2, edge, "NS_North_C");
			RoadPath northLeft   = northCenter.offset( laneWidth/2); // 北向內側
			RoadPath northRight  = northCenter.offset(-laneWidth/2); // 北向外側

			// 四個匝道（外圓弧）
			RoadPath r1 = RoadPath.arc(cx + laneWidth, cy + laneWidth, outerRadius, 30, 60, true, "Ramp_EB_to_NB");   //Q4
			RoadPath r2 = RoadPath.arc(cx + laneWidth, cy - laneWidth, outerRadius, 300, 330, true, "Ramp_NB_to_WB"); //Q1
			RoadPath r3 = RoadPath.arc(cx - laneWidth, cy - laneWidth, outerRadius, 210, 240, true, "Ramp_WB_to_SB"); //Q2
			RoadPath r4 = RoadPath.arc(cx - laneWidth, cy + laneWidth, outerRadius, 120, 150, true, "Ramp_SB_to_EB"); //Q3

			// 各匝道的進入／離開連接線（簡化為直線）
			double a0 = Math.toRadians(30);
			double a1 = Math.toRadians(60);
			RoadPath r1Entry = RoadPath.straight(edge + (W - edge * 2)*0.7, cy + laneWidth*1.7, 
			                                     cx + laneWidth + outerRadius * Math.cos(a0), cy + laneWidth + outerRadius * Math.sin(a0), "Connect_EB_R1");
			RoadPath r1Exit  = RoadPath.straight(cx + laneWidth + outerRadius * Math.cos(a1), cy + laneWidth + outerRadius * Math.sin(a1), 
			                                     cx + laneWidth*1.7, edge + (H - edge * 2) * 0.7, "Connect_R1_NB");

			a0 = Math.toRadians(300); 
			a1 = Math.toRadians(330);
			RoadPath r2Entry = RoadPath.straight(cx + laneWidth*1.7, edge + (H - edge * 2) * 0.3,  
			                                     cx + laneWidth + outerRadius * Math.cos(a0), cy - laneWidth + outerRadius * Math.sin(a0), "Connect_NB_R2");
			RoadPath r2Exit  = RoadPath.straight(cx + laneWidth + outerRadius * Math.cos(a1), cy - laneWidth + outerRadius * Math.sin(a1),
			                                     edge + (W - edge * 2) * 0.7, cy - laneWidth*1.7, "Connect_R2_WB");

			a0 = Math.toRadians(210); 
			a1 = Math.toRadians(240);
			RoadPath r3Entry = RoadPath.straight(edge + (W - edge * 2)*0.3, cy - laneWidth*1.7,  
			                                     cx - laneWidth + outerRadius * Math.cos(a0), cy - laneWidth + outerRadius * Math.sin(a0), "Connect_WB_R3");
			RoadPath r3Exit  = RoadPath.straight(cx - laneWidth + outerRadius * Math.cos(a1), cy - laneWidth + outerRadius * Math.sin(a1), 
			                                     cx - laneWidth*1.7, edge + (H - edge * 2) * 0.3, "Connect_R3_SB");

			a0 = Math.toRadians(120); 
			a1 = Math.toRadians(150);
			RoadPath r4Entry = RoadPath.straight(cx - laneWidth*1.7, edge + (H - edge * 2) * 0.7, 
			                                     cx - laneWidth + outerRadius * Math.cos(a0), cy + laneWidth + outerRadius * Math.sin(a0), "Connect_SB_R4");
			RoadPath r4Exit  = RoadPath.straight(cx - laneWidth + outerRadius * Math.cos(a1), cy + laneWidth + outerRadius * Math.sin(a1), 
			                                     edge + (W - edge * 2)*0.3, cy + laneWidth*1.7, "Connect_R4_EB");

			// 將所有路徑加入清單（繪圖時用）
			Collections.addAll(paths, eastLeft, eastRight, westLeft, westRight,
					southLeft, southRight, northLeft, northRight,
					r1Entry, r1, r1Exit,
					r2Entry, r2, r2Exit,
					r3Entry, r3, r3Exit,
					r4Entry, r4, r4Exit);

			// ---- 建立 Lane 實例（保留參考，方便設 adjacents）----
			Lane lnEastLeft  = new Lane(eastLeft);   Lane lnEastRight  = new Lane(eastRight);
			Lane lnWestLeft  = new Lane(westLeft);   Lane lnWestRight  = new Lane(westRight);
			Lane lnSouthLeft = new Lane(southLeft);  Lane lnSouthRight = new Lane(southRight);
			Lane lnNorthLeft = new Lane(northLeft);  Lane lnNorthRight = new Lane(northRight);

			Lane lnR1Entry = new Lane(r1Entry); Lane lnR1 = new Lane(r1); Lane lnR1Exit = new Lane(r1Exit);
			Lane lnR2Entry = new Lane(r2Entry); Lane lnR2 = new Lane(r2); Lane lnR2Exit = new Lane(r2Exit);
			Lane lnR3Entry = new Lane(r3Entry); Lane lnR3 = new Lane(r3); Lane lnR3Exit = new Lane(r3Exit);
			Lane lnR4Entry = new Lane(r4Entry); Lane lnR4 = new Lane(r4); Lane lnR4Exit = new Lane(r4Exit);

			// 放入列表
			Collections.addAll(lanes,
					lnEastLeft, lnEastRight,
					lnWestLeft, lnWestRight,
					lnSouthLeft, lnSouthRight,
					lnNorthLeft, lnNorthRight,
					lnR1Entry, lnR1, lnR1Exit,
					lnR2Entry, lnR2, lnR2Exit,
					lnR3Entry, lnR3, lnR3Exit,
					lnR4Entry, lnR4, lnR4Exit);

			// ---- 設定直線雙線的左右相鄰（僅直線允許 MOBIL 變道）----
			lnEastLeft.adjRight = lnEastRight; lnEastRight.adjLeft = lnEastLeft;
			lnWestLeft.adjRight = lnWestRight; lnWestRight.adjLeft = lnWestLeft;
			lnSouthLeft.adjRight = lnSouthRight; lnSouthRight.adjLeft = lnSouthLeft;
			lnNorthLeft.adjRight = lnNorthRight; lnNorthRight.adjLeft = lnNorthLeft;
			// 匝道不設相鄰（不變道）
		}

		/**
		 * 世界更新單步：
		 * 1) 生成新車 → 2) 建立 QuadTree → 3) 搜尋前後鄰車 → 4) MOBIL 判斷變道 → 5) IDM 加速度與積分 → 6) 推進位置與回收。
		 */
		void update(double dt, boolean mobilEnabled) {
			
			for (Car c : cars) {
		    // NEW: 冷卻/動畫時間流逝
		    if (c.laneCooldown > 0) c.laneCooldown -= dt;
		    if (c.animFromLane != null) c.animT = Math.min(1.0, c.animT + dt / 0.35); // 0.35s 動畫
		    
		    c.advance(dt);
		    wrapOrRecycle(c);
		    followRoute(c);
			}
			
			// 1) 生成車輛（達時間門檻且未達上限）
			spawnAcc += dt;
			if (cars.size() < maxCars && spawnAcc >= spawnEverySec) {
				spawnAcc = 0;
				spawnCar();
			}

			// 2) 以目前車輛位置重建 QuadTree（便於區域查詢）
			qt = new QuadTree(worldBounds, 6, 8);
			synchronized (cars) {
				for (Car c : cars)
					qt.insert(new QTItem(c.position(), c));
			}

			// 3) 第一階段：使用 QuadTree 找出各車在同車道/左右車道的前車與後車（近似）
			Map<Car, NeighborInfo> neigh = new ConcurrentHashMap<>();
			List<Callable<Void>> tasks = new ArrayList<>();
			synchronized (cars) {
				for (Car c : cars) {
					tasks.add(() -> {
						neigh.put(c, findNeighbors(c));
						return null;
					});
				}
			}
			invokeAll(tasks); // 平行化鄰近搜尋

			// 4) 第二階段：MOBIL 變換車道（順序處理以避免衝突；隨機順序減少偏誤）
			if (mobilEnabled)
				laneChangeRound(neigh);

			// 5) 第三階段：依 IDM 計算加速度並積分速度與參數 s（平行化）
			tasks.clear();
			synchronized (cars) {
				for (Car c : cars) {
					tasks.add(() -> {
						stepIDM(c, neigh.get(c), dt);
						return null;
					});
				}
			}
			invokeAll(tasks);

			// 6) 推進位置並處理超出邊界；同時在這一步做路段接續/入口判斷
			synchronized (cars) {
				for (Car c : cars) {
					c.advance(dt);
					wrapOrRecycle(c);
					followRoute(c);
				}
			}
		}

		/** 將任務提交至執行緒池並等待完成。 */
		void invokeAll(List<Callable<Void>> tasks) {
			try {
				pool.invokeAll(tasks);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		/**
		 * 直線道：s 超出 [0,1) 時循環回包（實現無限長直線效果）。
		 * 匝道弧線：s>=1 時回到 0（形成回圈）。
		 */
		void wrapOrRecycle(Car c) {
	    // 只要這條 Lane 在 routeGraph 中「有」下一段，表示它是有限段（entry/ramp/exit/connector）
	    boolean finiteSegment = routeGraph.containsKey(c.lane) && !routeGraph.get(c.lane).isEmpty();

	    if (c.lane.path.isStraight) {
	        if (finiteSegment) {
	            // NEW: 有下一段 → 不 wrap，夾住在尾端附近等合流
	            if (c.s > 0.995) c.s = 0.995;
	        } else {
	            // 無下一段的無限直線（主線）才循環
	            if (c.s > 1.0) c.s = 0;
	            else if (c.s < 0) c.s += 1.0;
	        }
	    } else {
	        // 弧線（匝道本身通常也有下一段），同理：有下一段就別回繞
	        if (finiteSegment) {
	            if (c.s > 0.995) c.s = 0.995;
	        } else {
	            if (c.s > 1.0) c.s = 0.0; // 保留原本回圈行為
	        }
	    }
	}

		/** 隨機在主幹直線道生成一輛車（初始 s 落在 [0,0.2)）；並隨機決定是否偏好走匝道。 */
		void spawnCar() {
			List<Lane> spawnable = new ArrayList<>();
			for (Lane l : lanes)
				if (l.path.isStraight && (l.path.name.startsWith("EW") || l.path.name.startsWith("NS")))
					spawnable.add(l);
			Lane lane = spawnable.get(rng.nextInt(spawnable.size()));
			Car c = new Car(lane);
			c.s = rng.nextDouble() * 0.2;
			c.v = v0 * (0.4 + 0.2 * rng.nextDouble()); // 初速略低於期望速度
			c.color = randomColor(rng);
			// 60% 直行、40% 走匝道（可調整）
			c.takeRamp = (rng.nextDouble() < 0.4);
			synchronized (cars) {
				cars.add(c);
			}
		}

		/** 產生亮麗的 HSB 顏色當作車色。 */
		Color randomColor(Random r) {
			float h = r.nextFloat();
			float s = 0.6f + r.nextFloat() * 0.3f;
			float b = 0.75f + r.nextFloat() * 0.2f;
			return Color.getHSBColor(h, s, b);
		}

		/** 使用 QuadTree 在附近範圍找出可能的前後車（同車道、左右相鄰車道）。*/
		NeighborInfo findNeighbors(Car me) {
			Point2D p = me.position();
			// 以 me 為中心建立查詢矩形（寬高 120px）
			Rectangle2D rect = new Rectangle2D.Double(p.getX() - 60, p.getY() - 60, 120, 120);
			List<QTItem> list = qt.query(rect);

			Car leaderSame = null;
			double leaderSameDist = Double.POSITIVE_INFINITY;
			Car follSame = null;
			double follSameDist = Double.POSITIVE_INFINITY;
			Car leaderLeft = null, leaderRight = null, follLeft = null, follRight = null;
			double ld = 1e9, rd = 1e9, fld = 1e9, frd = 1e9;

			for (QTItem it : list) {
				Car other = (Car) it.payload;
				if (other == me)
					continue;
				// 同車道：找前車與後車（取最近者）
				if (other.lane == me.lane) {
					double d = laneDistanceAhead(me, other);
					double back = laneDistanceAhead(other, me);
					if (d >= 0 && d < leaderSameDist) {
						leaderSameDist = d;
						leaderSame = other;
					}
					if (back >= 0 && back < follSameDist) {
						follSameDist = back;
						follSame = other;
					}
				}
				// 只在直線道上考慮左右相鄰變道
				if (me.lane.adjLeft != null && other.lane == me.lane.adjLeft) {
					double d = laneDistanceAhead(me, other);
					if (d >= 0 && d < ld) {
						ld = d;
						leaderLeft = other;
					}
					double back = laneDistanceAhead(other, me);
					if (back >= 0 && back < fld) {
						fld = back;
						follLeft = me; // 簡化：僅記錄距離
					}
				}
				if (me.lane.adjRight != null && other.lane == me.lane.adjRight) {
					double d = laneDistanceAhead(me, other);
					if (d >= 0 && d < rd) {
						rd = d;
						leaderRight = other;
					}
					double back = laneDistanceAhead(other, me);
					if (back >= 0 && back < frd) {
						frd = back;
						follRight = me; // 簡化：僅記錄距離
					}
				}
			}
			return new NeighborInfo(leaderSame, follSame, leaderLeft, follLeft, leaderRight, follRight,
					leaderSameDist, follSameDist, ld, fld, rd, frd);
		}

		/** 回傳 a 車看見 b 車的前向距離（同車道、以 s 參數差轉換為長度）。*/
		double laneDistanceAhead(Car a, Car b) {
			if (a.lane != b.lane)
				return Double.NaN;
			double ds = b.s - a.s;
			if (ds < 0)
				ds += 1.0;
			return ds * a.lane.path.length;
		}

		// ------------------- MOBIL lane change -------------------
		/** 對所有車依隨機順序嘗試變道（先左後右），以減少競爭衝突。*/
		void laneChangeRound(Map<Car, NeighborInfo> neigh) {
			List<Car> order;
			synchronized (cars) {
				order = new ArrayList<>(cars);
			}
			Collections.shuffle(order, rng);

			for (Car c : order) {
				// NEW: 冷卻中就跳過
				if (c.laneCooldown > 0)
					continue;

				boolean changed = false;
				Lane left = c.lane.adjLeft;
				Lane right = c.lane.adjRight;

				if (left != null && considerLaneChange(c, left, neigh, true)) {
					startLaneChange(c, left); // NEW
					changed = true;
				} else if (right != null && considerLaneChange(c, right, neigh, false)) {
					startLaneChange(c, right); // NEW
					changed = true;
				}

				// NEW: 一幀只處理一次，避免連續兩次切換
				if (changed)
					continue;
			}
		}
		
		void startLaneChange(Car c, Lane target) {
			Lane from = c.lane;
			c.lane = target;
			c.laneCooldown = Math.max(c.laneCooldown, 1.8); // 約 1.8s 冷卻，可依喜好 1.2~3.0
			// 視覺補間：從舊線過渡到新線
			c.animFromLane = from;
			c.animT = 0.0;
		}

		/**
		 * 判斷是否由 me 變到 target 車道：
		 * 1) 安全性：目標車道後車的加速度不可低於 aSafe（避免急煞）。
		 * 2) 動機值：IDM 估算變道前後自身加速度差 + 禮讓加權他人影響 > 門檻。
		 */
		boolean considerLaneChange(Car me, Lane target, Map<Car, NeighborInfo> neigh, boolean toLeft) {
			
			boolean hasNext = routeGraph.containsKey(me.lane) && !routeGraph.get(me.lane).isEmpty();
			if (hasNext) return false; // 在 entry/ramp/exit 等有限段不上 MOBIL
			
	    if (!me.lane.path.isStraight || !target.path.isStraight) return false;
	    if (me.laneCooldown > 0) return false; // NEW: 冷卻
			
			if (!me.lane.path.isStraight || !target.path.isStraight)
				return false; // 簡化：僅在直線上變道
			// 安全性：計算目標車道之後車（newFollower）是否需過度煞車
			Car newFollower = nearestBehindInLane(me, target);
			Car newLeader = nearestAheadInLane(me, target);
			double aNewFollower = 0;
			if (newFollower != null)
				aNewFollower = accIDM(newFollower, newLeader, distAlong(newFollower, newLeader));
			if (newFollower != null && aNewFollower < aSafe)
				return false; // 不安全，放棄

			// 動機值：自己變道的收益（aGo - aStay）+ 禮讓乘子 ×（跟車者變化）
			NeighborInfo N = neigh.get(me);
			Car oldLeader = N.leaderSame;
			double sOld = N.leaderSameDist;
			double aOld = accIDM(me, oldLeader, sOld);
			double aStay = aOld; // 簡化：忽略他車隨後調整

			double sNew = distAlong(me, newLeader);
			double aGo = accIDM(me, newLeader, sNew);

			// 影響跟車者（近似）
			double aOldFollowerDelta = 0; // 簡化未計算舊後車
			double aNewFollowerDelta = 0;
			if (newFollower != null) {
				double aBefore = accIDM(newFollower, newLeader, distAlong(newFollower, newLeader));
				double aAfter = accIDM(newFollower, me, distAlong(newFollower, me));
				aNewFollowerDelta = aAfter - aBefore;
			}
			double incentive = (aGo - aStay) + politeness * (aOldFollowerDelta + aNewFollowerDelta);
			
	    // NEW: 遲滯（hysteresis）：提高觸發門檻，避免邊界振盪
	    double hysteresis = 0.15 * 40; // 約 6 px/s^2 的額外門檻
			
	    return incentive > (aLaneChangeThreshold + hysteresis);
		}

		/** 找出 ref 在指定車道上最近的前車。*/
		Car nearestAheadInLane(Car ref, Lane lane) {
			Car best = null;
			double bestDs = Double.POSITIVE_INFINITY;
			synchronized (cars) {
				for (Car c : cars)
					if (c.lane == lane && c != ref) {
						double ds = c.s - ref.s;
						if (ds < 0)
							ds += 1.0;
						if (ds >= 0 && ds < bestDs) {
							bestDs = ds;
							best = c;
						}
					}
			}
			return best;
		}

		/** 找出 ref 在指定車道上最近的後車。*/
		Car nearestBehindInLane(Car ref, Lane lane) {
			Car best = null;
			double bestDs = Double.POSITIVE_INFINITY;
			synchronized (cars) {
				for (Car c : cars)
					if (c.lane == lane && c != ref) {
						double ds = ref.s - c.s;
						if (ds < 0)
							ds += 1.0;
						if (ds >= 0 && ds < bestDs) {
							bestDs = ds;
							best = c;
						}
					}
			}
			return best;
		}

		/** 計算 rear 與 front 之間沿車道的距離（若非同車道或 front 不存在回傳無限大）。*/
		double distAlong(Car rear, Car front) {
			if (front == null)
				return Double.POSITIVE_INFINITY;
			if (rear.lane != front.lane)
				return Double.POSITIVE_INFINITY;
			double ds = front.s - rear.s;
			if (ds < 0)
				ds += 1.0;
			return ds * rear.lane.path.length;
		}

		// ------------------- IDM -------------------
		/** 依 IDM 計算加速度並以顯式歐拉積分更新速度與位置參數 s。*/
		void stepIDM(Car c, NeighborInfo N, double dt) {
			Car leader = (N != null ? N.leaderSame : null);
			double s = (N != null ? N.leaderSameDist : Double.POSITIVE_INFINITY); // 與前車距離（空間頭距）
			double a = accIDM(c, leader, s);
			// 積分 v 與 s（確保速度非負）
			c.v += a * dt;
			if (c.v < 0)
				c.v = 0;
			c.s += (c.v * dt) / c.lane.path.length;
		}

		/** IDM 加速度項：自由加速 - 跟車抑制。*/
		double accIDM(Car c, Car leader, double s) {
			double dv = 0;
			if (leader != null)
				dv = c.v - leader.v; // 與前車相對速度
			double sStar = s0 + Math.max(0, c.v * T + (c.v * dv) / (2 * Math.sqrt(aMax * b))); // 期望安全間距
			double termFree = Math.pow(c.v / v0, delta); // 自由路段加速趨近 0
			double termInt = (leader == null ? 0 : Math.pow(sStar / Math.max(1.0, s), 2)); // 跟車抑制項
			double a = aMax * (1 - termFree - termInt);
			return a;
		}

		/** 負責繪製道路與車輛（呼叫於 paintComponent 內）。*/
		void render(Graphics2D g) {
			// 道路底色（粗線）
			g.setStroke(new BasicStroke(24, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(64, 74, 90));
			for (RoadPath p : paths)
				g.draw(p.asPath2D());
			// 車道虛線標記
			g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] { 10, 14 }, 0));
			g.setColor(new Color(240, 220, 100));
			for (RoadPath p : paths)
				g.draw(p.asPath2D());
			// 十字中央實體（視覺裝飾）
			g.setColor(new Color(52, 60, 75));
			g.fillRect((int) (cx - highwayHalf), (int) (cy - 8), (int) (2 * highwayHalf), 16);
			g.fillRect((int) (cx - 8), (int) (cy - highwayHalf), 16, (int) (2 * highwayHalf));

			// 繪製所有車輛
			synchronized (cars) {
				for (Car c : cars)
					drawCar(g, c);
			}
		}

		/** 以車身長寬與朝向繪製一台小車。*/
		void drawCar(Graphics2D g, Car c) {
	    Point2D pt;
	    double heading;

	    if (c.animFromLane != null && c.animT < 1.0) {
	        // NEW: 兩條 Lane 在同一個 s 上做空間補間（僅用於視覺，物理仍依目標 lane）
	        Point2D p0 = c.animFromLane.path.pointAt(c.s);
	        Point2D p1 = c.lane.path.pointAt(c.s);
	        double x = RoadPath.lerp(p0.getX(), p1.getX(), c.animT);
	        double y = RoadPath.lerp(p0.getY(), p1.getY(), c.animT);
	        pt = new Point2D.Double(x, y);

	        double h0 = c.animFromLane.path.headingAt(c.s);
	        double h1 = c.lane.path.headingAt(c.s);
	        // 簡單線性補間方向（足夠平滑）
	        heading = h0 + (h1 - h0) * c.animT;

	        if (c.animT >= 1.0) c.animFromLane = null; // 收尾
	    } else {
	        pt = c.position();
	        heading = c.heading();
	        c.animFromLane = null; // 防衛
	    }

	    AffineTransform at = g.getTransform();
	    g.translate(pt.getX(), pt.getY());
	    g.rotate(heading);
	    g.setColor(c.color);
	    g.fillRoundRect(-10, -7, 20, 14, 6, 6);
	    g.setColor(new Color(220, 230, 240));
	    g.fillRoundRect(-5, -6, 9, 12, 6, 6);
	    g.setTransform(at);
		}
		
		// ---- 幾何工具：向量叉積 ----
		double cross(double ax, double ay, double bx, double by) {
		    return ax * by - ay * bx;
		}

		// ---- 兩線段相交：回傳是否命中與各自的參數 tA/tB（0~1）與交點 ----
		static class InterResult {
			boolean hit;
			double tA, tB;
			Point2D pt;

			InterResult(boolean h, double ta, double tb, Point2D p) {
				hit = h;
				tA = ta;
				tB = tb;
				pt = p;
			}
		}

		InterResult segIntersect(Point2D a1, Point2D a2, Point2D b1, Point2D b2) {
			double rX = a2.getX() - a1.getX(), rY = a2.getY() - a1.getY();
			double sX = b2.getX() - b1.getX(), sY = b2.getY() - b1.getY();
			double denom = cross(rX, rY, sX, sY);
			if (Math.abs(denom) < 1e-9)
				return new InterResult(false, 0, 0, null); // 平行或共線視為不交
			double qpx = b1.getX() - a1.getX(), qpy = b1.getY() - a1.getY();
			double t = cross(qpx, qpy, sX, sY) / denom; // a 上的參數
			double u = cross(qpx, qpy, rX, rY) / denom; // b 上的參數
			if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
				Point2D pt = new Point2D.Double(a1.getX() + t * rX, a1.getY() + t * rY);
				return new InterResult(true, t, u, pt);
			}
			return new InterResult(false, 0, 0, null);
		}

		// ---- 從 from 的尾端段 與 to 的起始段 找交點；回傳 to 上的 s（0~1），找不到就回傳投影 s ----
		Double mergeSAtIntersection(RoadPath from, RoadPath to) {
			// 掃描範圍：from 最後 24 段，to 前 48 段（可依需要調整）
			int na = from.pts.size();
			int nb = to.pts.size();
			int iStart = Math.max(0, na - 1 - 24);
			int jEnd = Math.min(nb - 1, 48);

			for (int i = iStart; i < na - 1; i++) {
				Point2D a1 = from.pts.get(i), a2 = from.pts.get(i + 1);
				for (int j = 1; j <= jEnd; j++) {
					Point2D b1 = to.pts.get(j - 1), b2 = to.pts.get(j);
					InterResult ir = segIntersect(a1, a2, b1, b2);
					if (ir.hit) {
						// 交點位於 to 的第 j-1 段，to 上對應的弧長 = segLen[j-1] + u * (segLen[j]-segLen[j-1])
						double segLen = to.segLen[j] - to.segLen[j - 1];
						double sLen = to.segLen[j - 1] + ir.tB * Math.max(1e-9, segLen);
						double s = sLen / Math.max(1e-9, to.length);
						// 為安全起見，把 s 限制在 to 起點一小段內
						return Math.max(0.0, Math.min(s, 0.25));
					}
				}
			}

			// 如果沒有精確交點，退而求其次：把 from 終點投影到 to 上，取最近點的 s
			Point2D tail = from.pts.get(na - 1);
			return projectPointToPathS(to, tail);
		}

		// ---- 將點投影到 polyline，回傳對應的 s（0~1） ----
		Double projectPointToPathS(RoadPath path, Point2D p) {
			int n = path.pts.size();
			double bestD2 = Double.POSITIVE_INFINITY;
			double bestSlen = 0.0;

			for (int i = 0; i < n - 1; i++) {
				Point2D a = path.pts.get(i), b = path.pts.get(i + 1);
				double vx = b.getX() - a.getX(), vy = b.getY() - a.getY();
				double wx = p.getX() - a.getX(), wy = p.getY() - a.getY();
				double vv = vx * vx + vy * vy;
				double t = (vv < 1e-9) ? 0.0 : (vx * wx + vy * wy) / vv; // 投影比例
				if (t < 0)
					t = 0;
				else if (t > 1)
					t = 1;
				double px = a.getX() + t * vx, py = a.getY() + t * vy;
				double dx = p.getX() - px, dy = p.getY() - py;
				double d2 = dx * dx + dy * dy;
				if (d2 < bestD2) {
					bestD2 = d2;
					double seg = path.segLen[i + 1] - path.segLen[i];
					bestSlen = path.segLen[i] + t * Math.max(1e-9, seg);
				}
			}
			double s = bestSlen / Math.max(1e-9, path.length);
			return Math.max(0.0, Math.min(s, 1.0));
		}
		
	// 掃描一個插入視窗（預設 0.00~0.15），找第一個安全的 s；找不到回傳 null
		Double findSafeMergeS(Car me, Lane target, double sStart, double sEnd, int samples) {
		    double bestS = -1;
		    for (int i = 0; i < samples; i++) {
		        double t = (samples == 1) ? 0.0 : (i / (double)(samples - 1));
		        double sCand = sStart + (sEnd - sStart) * t;

		        if (gapOK(me, target, sCand)) {
		            return sCand; // 找到就回傳
		        }
		    }
		    return null;
		}

		// 檢查在 target 車道的 sCand 處是否有足夠前後安全距離
		boolean gapOK(Car me, Lane target, double sCand) {
		    Car ahead = null, behind = null;
		    double bestAhead = Double.POSITIVE_INFINITY, bestBehind = Double.POSITIVE_INFINITY;

		    synchronized (cars) {
		        for (Car c : cars) if (c.lane == target && c != me) {
		            // 前方距離
		            double dsF = c.s - sCand;
		            if (dsF < 0) dsF += 1.0;
		            double dpxF = dsF * target.path.length;
		            if (dpxF >= 0 && dpxF < bestAhead) { bestAhead = dpxF; ahead = c; }

		            // 後方距離
		            double dsB = sCand - c.s;
		            if (dsB < 0) dsB += 1.0;
		            double dpxB = dsB * target.path.length;
		            if (dpxB >= 0 && dpxB < bestBehind) { bestBehind = dpxB; behind = c; }
		        }
		    }

		    // 動態安全距離：考慮相對速差，尾隨車更嚴格一點
		    double vM = Math.max(1.0, me.v);
		    double vB = (behind != null ? Math.max(1.0, behind.v) : vM);
		    double sStarMe     = s0 + Math.max(0, vM * T);
		    double sStarBehind = s0 + Math.max(0, vB * T);

		    // 放寬係數：前方 1.10、後方 1.20（避免插到後車鼻尖）
		    boolean okAhead  = (ahead  == null) || (bestAhead  > sStarMe * 1.10);
		    boolean okBehind = (behind == null) || (bestBehind > sStarBehind * 1.20);

		    // 若我幾乎停住（<= 2 px/s），允許稍微更緊湊的併入（像實務 zipper merge）
		    if (vM <= 2.0) {
		        okAhead  = (ahead  == null) || (bestAhead  > sStarMe * 1.05);
		        okBehind = (behind == null) || (bestBehind > sStarBehind * 1.10);
		    }
		    return okAhead && okBehind;
		}

		

	}

	// =============================== Lane ===============================
	/**
	 * Lane：車道是 RoadPath 的包裝，並可連結左右相鄰車道（用於變道）。
	 */
	static class Lane {
		final RoadPath path;
		Lane adjLeft, adjRight; // 左右相鄰（直線使用）

		public Lane(RoadPath p) {
			this.path = p;
		}
	}

	// =============================== Car ================================
	/**
	 * Car：以參數 s∈[0,1) 表示在當前 Lane 的位置；v 為像素/秒。
	 */
	static class Car {
		Lane lane;
		double s = 0; // 位置參數 path長的比例 [0 1）
		double v = 0; // 速度（px/s）
		Color color = Color.CYAN;
		boolean takeRamp = false; // 是否偏好走匝道
		double laneCooldown = 0;  // NEW: 變道冷卻秒數
		// NEW: 視覺平滑（補間）狀態
		Lane animFromLane = null;
		double animT = 0;         // 0->1 的補間進度

		public Car(Lane lane) {
			this.lane = lane;
		}

		Point2D position() {
			return lane.path.pointAt(s);
		}

		double heading() {
			return lane.path.headingAt(s);
		}

		// 依速度與 dt 推進 s；並做循環處理
		void advance(double dt) {
			s += (v * dt) / lane.path.length;
			if (s >= 1.0) {
				s = 0;
				v = 10;
			} else if (s < 0.0) {
				s += 1.0;
			}
		}
	}

	// ============================= NeighborInfo =========================
	/**
	 * NeighborInfo：保存同車道與左右車道的前/後鄰車及其距離（近似）。
	 */
	static class NeighborInfo {
		final Car leaderSame, followerSame, leaderLeft, followerLeft, leaderRight, followerRight;
		final double leaderSameDist, followerSameDist, leaderLeftDist, followerLeftDist, leaderRightDist, followerRightDist;

		NeighborInfo(Car ls, Car fs, Car ll, Car fl, Car lr, Car fr, double lsd, double fsd, double lld, double fld, double lrd, double frd) {
			this.leaderSame = ls;
			this.followerSame = fs;
			this.leaderLeft = ll;
			this.followerLeft = fl;
			this.leaderRight = lr;
			this.followerRight = fr;
			this.leaderSameDist = lsd;
			this.followerSameDist = fsd;
			this.leaderLeftDist = lld;
			this.followerLeftDist = fld;
			this.leaderRightDist = lrd;
			this.followerRightDist = frd;
		}
	}

	// ============================== RoadPath ============================
	/**
	 * RoadPath：以多段線（或近似圓弧的折線）描述一條路徑。
	 * 提供弧長表、插值點與切線方向、偏移線與 Path2D 以供繪圖。
	 */
	static class RoadPath {
		final List<Point2D> pts;  // 節點序列
		final String name;        // 名稱（便於除錯與路徑圖連結）
		final double length;      // 總弧長
		final double[] segLen;    // 前綴弧長表，segLen[i] = 0..i 的長度
		final boolean isStraight; // 是否為直線（影響變道規則）

		RoadPath(List<Point2D> pts, String name, boolean straight) {
			this.pts = pts;
			this.name = name;
			this.isStraight = straight;
			this.segLen = new double[pts.size()];
			double sum = 0;
			segLen[0] = 0;
			for (int i = 1; i < pts.size(); i++) {
				sum += pts.get(i - 1).distance(pts.get(i));
				segLen[i] = sum;
			}
			this.length = Math.max(1.0, sum); // 避免除以 0
		}

		/** 建立兩點直線（以 120 段等分取樣成折線呈現，便於計算與繪圖）。*/
		static RoadPath straight(double x1, double y1, double x2, double y2, String name) {
			List<Point2D> l = new ArrayList<>();
			int n = 120;
			for (int i = 0; i <= n; i++) {
				double t = i / (double) n;
				l.add(new Point2D.Double(lerp(x1, x2, t), lerp(y1, y2, t)));
			}
			return new RoadPath(l, name, true);
		}

		/**
		 * 建立圓弧折線：中心 (cx,cy)、半徑 r、起訖角度（度）、順逆時針。
		 * 以 160 段近似圓弧，足以獲得平滑視覺。
		 */
		static RoadPath arc(double cx, double cy, double r, double degStart, double degEnd, boolean clockwise, String name) {
			int steps = 160;
			double a0 = Math.toRadians(degStart), a1 = Math.toRadians(degEnd);
			List<Point2D> l = new ArrayList<>();
			double a = 0;
			for (int i = 0; i <= steps; i++) {
				double t = i / (double) steps;
				a = a0 + (a1 - a) * 0 + (a1 - a0) * t; // 線性插值角度
				double x = cx + r * Math.cos(a);
				double y = cy + r * Math.sin(a);
				l.add(new Point2D.Double(x, y));
			}
			return new RoadPath(l, name, false);
		}

		/**
		 * 以粗略法（節點法）產生偏移路徑：取局部切線，往其左法向平移固定距離。
		 * 視覺上可接受，非幾何嚴格偏移。
		 */
		RoadPath offset(double orthogonal) {
			List<Point2D> out = new ArrayList<>();
			for (int i = 0; i < pts.size(); i++) {
				Point2D p = pts.get(i);
				Point2D t0 = pts.get(Math.max(0, i - 1));
				Point2D t1 = pts.get(Math.min(pts.size() - 1, i + 1));
				double dx = t1.getX() - t0.getX();
				double dy = t1.getY() - t0.getY();
				double len = Math.hypot(dx, dy);
				if (len < 1e-6)
					len = 1;
				dx /= len;
				dy /= len;
				// 左法線
				double nx = -dy, ny = dx;
				out.add(new Point2D.Double(p.getX() + nx * orthogonal, p.getY() + ny * orthogonal));
			}
			return new RoadPath(out, name + "_off", this.isStraight);
		}

		/** 將節點序列轉為 Path2D 以供 Graphics2D 畫線。*/
		Shape asPath2D() {
			Path2D p = new Path2D.Double();
			boolean first = true;
			for (Point2D q : pts) {
				if (first) {
					p.moveTo(q.getX(), q.getY());
					first = false;
				} else
					p.lineTo(q.getX(), q.getY());
			}
			return p;
		}

		/**
		 * 依 s∈[0,1] 取對應弧長位置的座標：
		 * 先用二分查表找落在哪個線段，再線性插值端點。
		 */
		Point2D pointAt(double s01) {
			double target = s01 * length;
			int hi = Arrays.binarySearch(segLen, target);
			if (hi >= 0)
				return pts.get(hi);
			hi = -hi - 1;
			int lo = Math.max(0, hi - 1);
			if (hi >= pts.size())
				return pts.get(pts.size() - 1);
			double seg = segLen[hi] - segLen[lo];
			double t = seg > 1e-9 ? (target - segLen[lo]) / seg : 0;
			Point2D a = pts.get(lo), b = pts.get(hi);
			return new Point2D.Double(lerp(a.getX(), b.getX(), t), lerp(a.getY(), b.getY(), t));
		}

		/** 由相鄰兩點估計該處切線方向（弧度）。*/
		double headingAt(double s01) {
			double e = 1e-3;
			Point2D p1 = pointAt(Math.max(0, s01 - e));
			Point2D p2 = pointAt(Math.min(1, s01 + e));
			return Math.atan2(p2.getY() - p1.getY(), p2.getX() - p1.getX());
		}

		static double lerp(double a, double b, double t) {
			return a + (b - a) * t;
		}
	}

	// ============================== QuadTree ============================
	/**
	 * QuadTree：以四分樹加速範圍查詢；此處採用點型索引（車輛位置）。
	 */
	static class QTItem {
		final Point2D point;  // 空間位置
		final Object payload; // 實際物件（這裡是 Car）

		QTItem(Point2D p, Object o) {
			this.point = p;
			this.payload = o;
		}
	}

	static class QuadTree {
		final Rectangle2D bounds; // 節點邊界
		final int maxItems;       // 觸發分裂的最大物件數
		final int maxDepth;       // 最大深度
		List<QTItem> items;       // 此節點持有的項目
		QuadTree[] child;         // 四個子節點（NE/NW/SE/SW）
		int depth = 0;            // 當前深度

		QuadTree(Rectangle2D b, int maxItems, int maxDepth) {
			this(b, maxItems, maxDepth, 0);
		}

		QuadTree(Rectangle2D b, int maxItems, int maxDepth, int depth) {
			this.bounds = (Rectangle2D) b.clone();
			this.maxItems = maxItems;
			this.maxDepth = maxDepth;
			this.depth = depth;
			this.items = new ArrayList<>();
		}

		/** 插入一個點；必要時遞迴插入子節點。*/
		void insert(QTItem it) {
			if (child != null) {
				int idx = childIndex(it.point);
				if (idx >= 0) {
					child[idx].insert(it);
					return;
				}
			}
			items.add(it);
			if (items.size() > maxItems && depth < maxDepth)
				subdivide();
		}

		/** 節點分裂為四個子節點，並將可完全容納的項目下放。*/
		void subdivide() {
			child = new QuadTree[4];
			double x = bounds.getX(), y = bounds.getY(), w = bounds.getWidth() / 2, h = bounds.getHeight() / 2;
			child[0] = new QuadTree(new Rectangle2D.Double(x, y, w, h), maxItems, maxDepth, depth + 1);
			child[1] = new QuadTree(new Rectangle2D.Double(x + w, y, w, h), maxItems, maxDepth, depth + 1);
			child[2] = new QuadTree(new Rectangle2D.Double(x, y + h, w, h), maxItems, maxDepth, depth + 1);
			child[3] = new QuadTree(new Rectangle2D.Double(x + w, y + h, w, h), maxItems, maxDepth, depth + 1);
			Iterator<QTItem> it = items.iterator();
			while (it.hasNext()) {
				QTItem q = it.next();
				int idx = childIndex(q.point);
				if (idx >= 0) {
					child[idx].insert(q);
					it.remove();
				}
			}
		}

		/** 判斷點 p 應屬於哪個子節點（若剛好落在邊界則回傳 -1 代表留在父節點）。*/
		int childIndex(Point2D p) {
			double mx = bounds.getX() + bounds.getWidth() / 2;
			double my = bounds.getY() + bounds.getHeight() / 2;
			int i = (p.getY() < my ? 0 : 2) + (p.getX() < mx ? 0 : 1);
			Rectangle2D b = child[i].bounds;
			return b.contains(p) ? i : -1;
		}

		/** 以矩形 r 查詢所有落在其中的項目。*/
		List<QTItem> query(Rectangle2D r) {
			List<QTItem> out = new ArrayList<>();
			query(r, out);
			return out;
		}

		void query(Rectangle2D r, List<QTItem> out) {
			if (!bounds.intersects(r))
				return;
			if (child != null) {
				for (QuadTree c : child)
					c.query(r, out);
			}
			for (QTItem it : items) {
				if (r.contains(it.point))
					out.add(it);
			}
		}
	}

	// ============================ VideoRecorder (Animated GIF) ==========
	/**
	 * VideoRecorder：以 ImageIO 的序列寫入功能輸出動畫 GIF。
	 * 注意：JRE 內建 GIF 編碼器簡易可用，但色彩索引與壓縮非最佳；本範例著重示意。
	 */
	static class VideoRecorder {
		private ImageWriter gifWriter;      // GIF 編碼器
		private ImageWriteParam imageWriteParam; // 這裡維持預設（可為 null）
		private IIOMetadata imageMeta;      // 用於設定每幀延遲與迴圈
		private ImageOutputStream output;   // 檔案輸出串流
		private boolean recording = false;  // 是否正在錄影
		private int frameCount = 0;         // 累積幀數

		public boolean isRecording() {
			return recording;
		}

		/** 開始錄影：尋找 GIF Writer、開啟輸出檔，並準備寫入序列。*/
		public void start(File file) throws IOException {
			Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
			if (!writers.hasNext())
				throw new IOException("No GIF writer available");
			gifWriter = writers.next();
			output = ImageIO.createImageOutputStream(file);
			gifWriter.setOutput(output);
			gifWriter.prepareWriteSequence(null);
			recording = true;
			frameCount = 0;
			System.out.println("Recording GIF to " + file.getAbsolutePath());
		}

		/** 新增一幀到 GIF 序列；首次呼叫時建立並設定中繼資料（延遲、循環）。*/
		public void addFrame(RenderedImage img, int delayMs) {
			if (!recording)
				return;
			try {
				if (imageMeta == null) {
					ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
					imageMeta = gifWriter.getDefaultImageMetadata(type, imageWriteParam);
					String metaFormat = imageMeta.getNativeMetadataFormatName();
					IIOMetadataNode root = (IIOMetadataNode) imageMeta.getAsTree(metaFormat);
					// 設定每幀延遲（GraphicControlExtension）
					IIOMetadataNode gce = getNode(root, "GraphicControlExtension");
					gce.setAttribute("disposalMethod", "none");
					gce.setAttribute("userInputFlag", "FALSE");
					gce.setAttribute("transparentColorFlag", "FALSE");
					int centi = Math.max(1, delayMs / 10); // 以 1/100 秒計
					gce.setAttribute("delayTime", Integer.toString(centi));
					gce.setAttribute("transparentColorIndex", "0");
					// 設定 Netscape 2.0 擴展以實現無限循環
					IIOMetadataNode appExts = getNode(root, "ApplicationExtensions");
					IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
					app.setAttribute("applicationID", "NETSCAPE");
					app.setAttribute("authenticationCode", "2.0");
					byte[] loopBytes = new byte[] { 0x1, 0x0, 0x0 }; // 迴圈無限次
					app.setUserObject(loopBytes);
					appExts.appendChild(app);
					imageMeta.setFromTree(metaFormat, root);
				}
				IIOImage frame = new IIOImage(img, null, imageMeta);
				gifWriter.writeToSequence(frame, imageWriteParam);
				frameCount++;
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		/** 取得或建立指定名稱的中繼資料節點。*/
		private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
			int nNodes = rootNode.getLength();
			for (int i = 0; i < nNodes; i++) {
				if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
					return (IIOMetadataNode) rootNode.item(i);
				}
			}
			IIOMetadataNode node = new IIOMetadataNode(nodeName);
			rootNode.appendChild(node);
			return node;
		}

		/** 結束錄影：關閉序列並釋放資源。*/
		public void finish() {
			if (!recording)
				return;
			try {
				gifWriter.endWriteSequence();
				output.close();
				System.out.println("GIF saved. Frames=" + frameCount);
			} catch (IOException ignored) {
			}
			recording = false;
			frameCount = 0;
			gifWriter = null;
			imageMeta = null;
			output = null;
		}
	}
}
