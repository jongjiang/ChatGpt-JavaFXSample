import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Cloverleaf Interchange Traffic Simulation (Java 8 + Swing)
 *
 * 中文版詳細註解：
 * - 這是一個用 Java Swing 繪製「苜蓿葉型交流道」(cloverleaf) 并模擬車流的單檔示範。
 * - 以固定 60FPS 左右的計時器推進模擬，車輛沿著離散的路徑點 (polyline) 前進。
 * - 路徑包含東西向、南北向直線車道以及四個豁口的弧形匝道，另有進入/退出的連接線段。
 * - 車輛使用極簡「跟車模型」：保持時間間隔、彎道降速、併道區域讓速。
 *
 * 編譯：javac CloverleafSim.java
 * 執行：java CloverleafSim
 *
 * 程式結構總覽：
 * - CloverleafSim：主視窗 JFrame，內含 SimulationPanel。
 * - SimulationPanel：管理 Swing 計時器、呼叫世界更新與繪製。
 * - World：幾何建構(路徑/匝道/連接段/併道區)、車輛生成、更新與渲染。
 * - MergeZone：簡易矩形併道區域，用於減速讓速判斷。
 * - Car：車輛狀態(所屬路徑、位置參數s、速度等)與取樣位置/朝向。
 * - RoadPath：以多段線(點列)表示的路徑，提供長度、取樣點、切線方向、曲率等運算。
 * - Point2D / Utils：幾何輔助。
 */
@SuppressWarnings("serial")
public class CloverleafSim extends JFrame {

    public static void main(String[] args) {
        // Swing 程式入口：將 UI 建立動作放到 Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            CloverleafSim f = new CloverleafSim();
            f.setVisible(true);
        });
    }

    public CloverleafSim() {
        super("Cloverleaf Interchange - Traffic Simulation (Java8 + Swing)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 800);            // 預設視窗大小
        setLocationRelativeTo(null);   // 視窗置中
        SimulationPanel panel = new SimulationPanel(1000, 800);
        setContentPane(panel);
        // 注意：螢幕上顯示的提示文字提及按 R 重置，但程式未綁定鍵盤事件；可自行延伸。
    }

    // ======================== Simulation Panel ===========================
		static class SimulationPanel extends JPanel {

        private final int W, H;              // 畫布寬高
        private final World world;           // 模擬世界
        private final Timer timer; // Swing 計時器 (約 60FPS)

        public SimulationPanel(int w, int h) {
            this.W = w;
            this.H = h;
            setBackground(new Color(32, 40, 52)); // 深色背景，襯托道路/車輛
            // 建構幾何與狀態
            world = new World(W, H);
            // 每 16ms 觸發一次：更新世界 + 重繪
            timer = new Timer(16, e -> {
                world.update(0.016); // 以秒為單位的固定時間步長 (約 1/60s)
                repaint();            // 要求 Swing 重繪 (非立即呼叫 paintComponent)
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // 一律以 Graphics2D 繪製，啟用反鋸齒由 World 控制
            world.render((Graphics2D) g);
        }
    }

    // ============================== World ================================
    static class World {
        final int W, H;                          // 畫布尺寸
        final List<RoadPath> paths = new ArrayList<>(); // 所有可行路徑 (直線/匝道/連接)
        final List<Car> cars = new ArrayList<>();       // 在場車輛
        final Random rng = new Random(1);                         // 固定種子以得到可重現結果

        // 生成控制
        double spawnAccumulator = 0.0;      // 時間累加器，超過 spawnEverySec 就試著生一輛
        final double spawnEverySec = 0.75;  // 平均每 0.75 秒生成一輛
        final int maxCars = 140;            // 場上車輛上限

        // 幾何參數 (以畫布中心為交流道中心)
        final double cx, cy;                // 中心座標
        final double laneWidth = 16;        // 單車道寬度 (繪圖尺度)
        final double highwayHalf = 140;     // 中央高架/分隔寬度的一半，用來畫十字的主幹
        final double outerRadius = 180;     // 苜蓿葉外圈半徑 (匝道弧線半徑)
        final double innerRadius = 120;     // 未直接使用，可延伸做雙車道/內外圈

        // 併道區：在匝道接入主線前的矩形區域，車輛經過時會額外降速
        final List<MergeZone> merges = new ArrayList<>();

        public World(int w, int h) {
            this.W = w;
            this.H = h;
            this.cx = W / 2;
            this.cy = H / 2;
            buildGeometry(); // 建立所有路徑與併道區
        }

        void buildGeometry() {
        	
            // 建構東西向與南北向的 4 條主線 (各 1 條車道，單向)：
            // 座標系：x 向右為正、y 向下為正 (Swing 預設)
	          // 東向 (由左往右) — y = cy - laneWidth (上方車道)
	          paths.add(RoadPath.straight(W - 40, cy - laneWidth, 40, cy - laneWidth, "EW_West"));
	          // 西向 (由右往左) — y = cy + laneWidth (下方車道)
	          paths.add(RoadPath.straight(40, cy + laneWidth, W - 40, cy + laneWidth, "EW_East"));
	
	          // 南向 (由上往下) — x = cx + laneWidth (右側車道)
	          paths.add(RoadPath.straight(cx + laneWidth, H - 40, cx + laneWidth, 40, "NS_North"));
	          // 北向 (由下往上) — x = cx - laneWidth (左側車道)
	          paths.add(RoadPath.straight(cx - laneWidth, 40, cx - laneWidth, H - 40, "NS_South"));

            // 苜蓿葉匝道：四個 1/4 圓弧 (標準 cloverleaf 旋向)： 螢幕與一般幾何不同
            // EB->NB, NB->WB, WB->SB, SB->EB
            //paths.addAll(RoadPath.leaf(cx + laneWidth, cy - laneWidth, outerRadius, 270, 290, true, "Ramp_EB_to_NB"));   // Q1 leaf
            paths.addAll(RoadPath.leaf(cx + laneWidth, cy + laneWidth, outerRadius, 30, 60, true, "Ramp_EB_to_NB"));   // Q1 leaf 
            paths.addAll(RoadPath.leaf(cx + laneWidth, cy - laneWidth, outerRadius, 300, 330, true, "Ramp_NB_to_WB"));    // Q4 leaf 
            paths.addAll(RoadPath.leaf(cx - laneWidth, cy - laneWidth, outerRadius, 210, 240, true, "Ramp_WB_to_SB"));  // Q3 leaf 
            paths.addAll(RoadPath.leaf(cx - laneWidth, cy + laneWidth, outerRadius, 120, 150, true, "Ramp_SB_to_EB")); // Q2 leaf 

            // 主線 <-> 匝道的連接段 (以二次貝茲曲線離散成多段線)
            addEntryExitConnectors();

            // 定義 4 個併道矩形區域 (在匝道接回主線處)
            defineMergeZones();
        }

        private void addEntryExitConnectors() {
            // 取得 4 條主線路徑
            RoadPath east = pathByName("EW_East");
            RoadPath west = pathByName("EW_West");
            RoadPath south = pathByName("NS_South");
            RoadPath north = pathByName("NS_North");

            // 進入匝道的連接：以起點(主線上 70% 位置) -> 控制點 -> 匝道入口終點 形成二次貝茲曲線
            // EB -> NB 匝道入口
            addConnectorToRampStart(east, 0.70, cx + laneWidth, cy + laneWidth, outerRadius, 30, "Conn_EB_to_Ramp");
            // NB -> WB 匝道入口
            addConnectorToRampStart(north, 0.70, cx + laneWidth, cy - laneWidth, outerRadius, 300, "Conn_NB_to_Ramp");
            // WB -> SB 匝道入口
            addConnectorToRampStart(west, 0.70, cx - laneWidth, cy - laneWidth, outerRadius, 210, "Conn_WB_to_Ramp");
            // SB -> EB 匝道入口
            addConnectorToRampStart(south, 0.70, cx - laneWidth, cy + laneWidth, outerRadius, 120, "Conn_SB_to_Ramp");

            // 匝道退出接回主線的連接段 (merge)
            // Ramp_EB_to_NB 接回 NS_North
            addMergeFromRampStart(cx + laneWidth, cy + laneWidth, outerRadius, 60, north, 0.30, "Merge_Ramp_to_North");
            // Ramp_NB_to_WB 接回 EW_West
            addMergeFromRampStart(cx + laneWidth, cy - laneWidth, outerRadius, 330, west, 0.30, "Merge_Ramp_to_West");
            // Ramp_WB_to_SB 接回 NS_South
            addMergeFromRampStart(cx - laneWidth, cy - laneWidth, outerRadius, 240, south, 0.30, "Merge_Ramp_to_South");
            // Ramp_SB_to_EB 接回 EW_East
            addMergeFromRampStart(cx - laneWidth, cy + laneWidth, outerRadius, 150, east, 0.30, "Merge_Ramp_to_East");
        }

        private void defineMergeZones() {
            // 在四個匝道並回主線的位置，放置簡單的矩形併道區域
        	merges.add(new MergeZone(new Rectangle((int) (cx + laneWidth - 30), (int) (cy + laneWidth - 30), 60, 60)));
        	merges.add(new MergeZone(new Rectangle((int) (cx + laneWidth - 30), (int) (cy - laneWidth - 30), 60, 60)));
        	merges.add(new MergeZone(new Rectangle((int) (cx - laneWidth - 30), (int) (cy - laneWidth - 30), 60, 60)));
        	merges.add(new MergeZone(new Rectangle((int) (cx - laneWidth - 30), (int) (cy + laneWidth - 30), 60, 60)));
        }

        RoadPath pathByName(String name) {
            for (RoadPath p : paths)
                if (p.name.equals(name))
                    return p;
            throw new IllegalArgumentException("No path: " + name);
        }

        void update(double dt) {
            // 1) 生成車輛：每隔 spawnEverySec 秒嘗試生一輛 (不超過 maxCars)
            spawnAccumulator += dt;
            if (cars.size() < maxCars && spawnAccumulator >= spawnEverySec) {
                spawnAccumulator = 0.0;
                spawnCar();
            }

            // 2) 依路徑分組，為每條路徑建立「前後車」關係，做簡易跟車控制
            Map<RoadPath, List<Car>> byPath = new HashMap<>();
            for (Car c : cars)
                byPath.computeIfAbsent(c.path, k -> new ArrayList<>()).add(c);
            for (List<Car> group : byPath.values())
                group.sort(Comparator.comparingDouble(c -> c.s)); // 依沿線參數 s (0~1) 排序

            // 針對每條路徑的車列，套用速度調整規則
            for (Map.Entry<RoadPath, List<Car>> e : byPath.entrySet()) {
                RoadPath rp = e.getKey();
                List<Car> group = e.getValue();
                int n = group.size();
                for (int i = 0; i < n; i++) {
                    Car me = group.get(i);
                    Car front = group.get((i + 1) % n); // 環狀考量：最後一台的前車是第一台 (視為在前一圈)
                    double gap = (front.s - me.s);
                    if (gap <= 0)
                        gap += 1.0; // 若前車已越過終點，將間距映回 [0,1)

                    // 目標速度：以車輛個別的 maxSpeed 為基準
                    double vTarget = me.maxSpeed;
                    // 彎道降速：曲率門檻 > 0.004 則將目標速降到 60%
                    if (rp.curvatureAt(me.s) > 0.004)
                        vTarget *= 0.6;

                    // 簡化的時間車頭時距(headway)控制：約 1 秒
                    // desiredGap 單位是路徑比例，與目前速度成正比 (速度/路徑長度)
                    double desiredGap = Math.max(0.05, me.speed * 1.0 / rp.length); // 至少保留 5% 的比例距離
                    if (gap < desiredGap)
                        // 若距離不足，進一步收緊目標速度；避免過猛降到太低，夾在 [0.2, 當前速度 - 誤差項]
                        vTarget = Math.min(vTarget, Math.max(0.2, me.speed - 0.6 * (desiredGap - gap)));

                    // 併道區額外讓速：降低至 75%
                    if (inMergeZone(me.position())) {
                        vTarget *= 0.75;
                    }

                    // 一階比例控制 (relaxation toward vTarget)
                    double accel = (vTarget - me.speed) * 1.8; // 調整係數 1.8 -> 加速度 (px/s^2)
                    me.speed = clamp(me.speed + accel * dt, 0.1, me.maxSpeed);
                }
            }

            // 3) 前進：將速度換算成路徑比例上的位移 (除以路徑長度)
            for (Car c : cars) {
                c.s = (c.s + (c.speed * dt) / c.path.length);
                if (c.s > 0.7 && c.s < 0.73) {
                    // 走到路徑末端：決定下一段路徑 (留在主線或轉入/轉出) ?
                    chooseNextPath(c);
                    //c.s -= 1; // 進入下一段後，將 s 回到 [0,1)
                    c.speed *= 40; //降速
                }
                
                if ((c.path.name.startsWith("EW") || c.path.name.startsWith("NS")) && c.s >= 1.0) {
                	c.s -= 1.0;
                }
            }
        }

        boolean inMergeZone(Point2D p) {
            // 檢查座標是否落在任一併道矩形內
            for (MergeZone mz : merges)
                if (mz.rect.contains(p.x, p.y))
                    return true;
            return false;
        }

        void chooseNextPath(Car c) {
            // 在路徑結束時決策：
            // - 若在主線接近中心，35% 機率進入匝道連接段 (Conn_* -> Ramp_* -> Merge_* -> 主線)
            // - 若在連接段/匝道/合流段，則按名稱規則尋找下一條合理的路徑
            String n = c.path.name;
            boolean takeRamp = rng.nextDouble() < 0.35; // 35% 嘗試走匝道
            
            if (n.equals("EW_East")) {
                c.path = takeRamp ? pickByPrefix("Conn_EB_to_Ramp") : c.path;
                if (!takeRamp) c.s = 0.73;
            } else if (n.equals("EW_West")) {
                c.path = takeRamp ? pickByPrefix("Conn_WB_to_Ramp") : c.path;
                if (!takeRamp) c.s = 0.73;
            } else if (n.equals("NS_South")) {
                c.path = takeRamp ? pickByPrefix("Conn_SB_to_Ramp") : c.path;
                if (!takeRamp) c.s = 0.73;
            } else if (n.equals("NS_North")) {
                c.path = takeRamp ? pickByPrefix("Conn_NB_to_Ramp") : c.path;
                if (!takeRamp) c.s = 0.73;
            } else if (n.contains("Conn_") || n.contains("Ramp_")) {
            	  // 連接段/匝道/合流段：依名稱推導下一段
                if (n.contains("to_Ramp")) {
                    // 由連接段轉入對應的匝道
                    RoadPath next = bestRampForConnector(n);
                    if (next != null) {
                        c.path = next;
                        c.s = 0.01;
                    }
                } else if (n.contains("Ramp_")) {
                    // 匝道之後接合流段 (Merge_*)，此處簡化：任取一個以 Merge_ 開頭的路徑
                    RoadPath m = pickByPrefix("Merge_");
                    if (m != null) {
                        c.path = m;
                        c.s = 0.01;
                    }
                };// this else if 怪怪的: else if (n.contains("Merge_")) {
                	
                if (n.contains("Merge_")) {
                    // 合流段結束後依名稱末尾決定回到哪一條主線
										if (n.endsWith("_North")) {
											c.path = pathByName("NS_North");
											c.s = 0.3;
											c.speed = 40;
										} else if (n.endsWith("_South")) {
											c.path = pathByName("NS_South");
											c.s = 0.3;
											c.speed = 40;
										} else if (n.endsWith("_East")) {
											c.path = pathByName("EW_East");
											c.s = 0.3;
											c.speed = 40;
										} else if (n.endsWith("_West")) {
											c.path = pathByName("EW_West");
											c.s = 0.3;
											c.speed = 40;
										}
                }
            }
        }

        private RoadPath bestRampForConnector(String connectorName) {
            // 依連接段名稱決定對應的匝道 (一一對應)
            if (connectorName.contains("EB_to_Ramp"))
                return pathByName("Ramp_EB_to_NB");
            if (connectorName.contains("NB_to_Ramp"))
                return pathByName("Ramp_NB_to_WB");
            if (connectorName.contains("WB_to_Ramp"))
                return pathByName("Ramp_WB_to_SB");
            if (connectorName.contains("SB_to_Ramp"))
                return pathByName("Ramp_SB_to_EB");
            return null;
        }

        private RoadPath pickByPrefix(String prefix) {
            // 由指定前綴挑選一條路徑 (若有多條，隨機取一)
            List<RoadPath> cands = new ArrayList<>();
            for (RoadPath p : paths)
                if (p.name.startsWith(prefix))
                    cands.add(p);
            if (cands.isEmpty())
                return null;
            return cands.get(rng.nextInt(cands.size()));
        }

        void spawnCar() {
            // 在 4 條主線中隨機選一條當出生路徑
            String[] mains = { "EW_East", "EW_West", "NS_South", "NS_North" };
            RoadPath p = pathByName(mains[rng.nextInt(mains.length)]);
            // 初始位置：在路徑前 20% 內隨機取一點，避免完全重疊
            double s0 = rng.nextDouble() * 0.2;
            Car c = new Car(p, s0);
            // 隨機車色與性能參數
            c.color = randomCarColor(rng);
            c.maxSpeed = 90 + rng.nextDouble() * 60; // 最高速 (px/s)
            c.length = 18 + rng.nextDouble() * 6;    // 車長 (像素)
            // 粗略避免生成瞬間重疊：若同一路徑已有車且 s 距離太近，放棄本次生成
            for (Car other : cars) {
                if (other.path == p && Math.abs(other.s - c.s) < 0.04) {
                    return; // 放棄當前 tick 的生成
                }
            }
            cars.add(c);
        }

        Color randomCarColor(Random r) {
            // 以 HSB 隨機產生較亮的車色
            float h = r.nextFloat();
            float s = 0.6f + r.nextFloat() * 0.3f;
            float b = 0.7f + r.nextFloat() * 0.3f;
            return Color.getHSBColor(h, s, b);
        }

        void render(Graphics2D g) {
            // 啟用反鋸齒，提升曲線/線段品質
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 分三層繪製：道路 -> 車輛 -> HUD
            drawRoads(g);
            drawCars(g);
            drawHUD(g);
        }
        
        void drawRoads(Graphics2D g) {
            // 道路底色 (粗線)
            g.setStroke(new BasicStroke(22, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(64, 74, 90));
            for (RoadPath p : paths) {
                Path2D path2 = p.asPath2D();
                g.draw(path2);
            }
            // 車道標線 (虛線)
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] { 10, 12 }, 0));
            g.setColor(new Color(240, 220, 100)); //yellow
            for (RoadPath p : paths) {
                Path2D path2 = p.asPath2D();
                g.draw(path2);
            }

            // 中央結構 (簡化的高架十字，僅做視覺提示)
            g.setColor(new Color(52, 60, 75));
            g.fillRect((int) (cx - highwayHalf), (int) (cy - 8), (int) (2 * highwayHalf), 16);
            g.fillRect((int) (cx - 8), (int) (cy - highwayHalf), 16, (int) (2 * highwayHalf));
        }

        void drawCars(Graphics2D g) {
            // 逐車繪製：先平移到車身中心，再依 heading 旋轉，畫出簡化車體
            for (Car c : cars) {
                Point2D p = c.position();
                double heading = c.heading();
                AffineTransform at = g.getTransform(); // 記錄原始轉換
                g.translate(p.x, p.y);
                g.rotate(heading);
                // 車身
                g.setColor(c.color);
                g.fillRoundRect((int) -c.length / 2, -7, (int) c.length, 14, 6, 6);
                // 擋風玻璃
                g.setColor(new Color(220, 230, 240));
                g.fillRoundRect(-2, -6, (int) (c.length * 0.35), 12, 6, 6);
                // 頭燈 (簡單的兩個小矩形)
                g.setColor(new Color(250, 250, 180));
                g.fillRect((int) (c.length / 2 - 2), -3, 2, 2);
                g.fillRect((int) (c.length / 2 - 2), 1, 2, 2);

                g.setTransform(at); // 還原座標系
            }
        }

        void drawHUD(Graphics2D g) {
            // 顯示一些狀態：車量、路徑數、生成頻率提示
            g.setColor(new Color(230, 235, 245));
            g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
            String txt = String.format("Cars: %d   Paths: %d   Spawn/%.2fs   Tip: Press R to reset", cars.size(), paths.size(), spawnEverySec);
            g.drawString(txt, 20, 24);
        }
        
    		void addConnectorToRampStart(RoadPath fromStraight, double sFrom, double arcCx, double arcCy, double r, double degStart, String name) {
    			// start point on straight
    			Point2D start = fromStraight.pointAt(sFrom);
    			double headingFrom = fromStraight.headingAt(sFrom); //0.7處
    			double txFrom = Math.cos(headingFrom), tyFrom = Math.sin(headingFrom);

    			// end point: exact ramp start point at degStart
    			double a0 = Math.toRadians(degStart);
    			Point2D end = new Point2D(arcCx + r * Math.cos(a0), arcCy + r * Math.sin(a0));
    			// ramp tangent at start (increasing angle dir)
    			// For counter-clockwise (our leaves use clockwise=false), tangent is rotated +90deg from radius
    			// derivative dir = (-sin a, cos a)
    			double txTo = -Math.sin(a0), tyTo = Math.cos(a0);

    			// control handle length heuristic: blend between distance and lane width, capped
    			double D = start.dist(end);
    			double L = Math.min(Math.max(80, 0.35 * D), 220);

    			Point2D c1 = BezierUtil.add(start, txFrom, tyFrom, L);
    			Point2D c2 = BezierUtil.add(end, txTo, tyTo, -L);

    			List<Point2D> pts = BezierUtil.cubicSamples(start, c1, c2, end, 28);
    			//paths.add(new RoadPath(pts, name));
    			paths.add(RoadPath.straight(start.x, start.y, end.x, end.y, name));
    		}
    		
				void addMergeFromRampStart(double arcCx, double arcCy, double r, double degStart, RoadPath toStraight, double sTo, String name) {
					// start on ramp at degStart
					double a0 = Math.toRadians(degStart);
					Point2D start = new Point2D(arcCx + r * Math.cos(a0), arcCy + r * Math.sin(a0));
					double txFrom = -Math.sin(a0), tyFrom = Math.cos(a0);

					// end on straight at sTo
					Point2D end = toStraight.pointAt(sTo);
					double headingTo = toStraight.headingAt(sTo);
					double txTo = Math.cos(headingTo), tyTo = Math.sin(headingTo);

					double D = start.dist(end);
					double L = Math.min(Math.max(80, 0.35 * D), 220);

					Point2D c1 = BezierUtil.add(start, txFrom, tyFrom, L);
					Point2D c2 = BezierUtil.add(end, txTo, tyTo, -L);

					List<Point2D> pts = BezierUtil.cubicSamples(start, c1, c2, end, 28);
					//paths.add(new RoadPath(pts, name));
					paths.add(RoadPath.straight(start.x, start.y, end.x, end.y, name));
				}
    }

    // =========================== Merge Zone ==============================
    static class MergeZone {
        final Rectangle rect; // 以 AWT Rectangle 表示的併道區域

        MergeZone(Rectangle r) {
            this.rect = r;
        }
    }

    // ============================= Car ===================================
    static class Car {
        RoadPath path;     // 所在路徑
        double s;          // 沿路徑的歸一化進度 [0,1)
        double speed = 70; // 當前速度 (px/s)
        double maxSpeed = 110; // 最高速度 (px/s)
        double length = 20;    // 車長，用於繪圖
        Color color = Color.CYAN; // 車色

        public Car(RoadPath path, double s) {
            this.path = path;
            this.s = s;
        }

        Point2D position() {
            // 依 s 參數在路徑上取樣世界座標
            return path.pointAt(s);
        }

        double heading() {
            // 取樣局部切線方向 (弧度)，供繪圖旋轉用
            return path.headingAt(s);
        }
    }

    // ============================ RoadPath ===============================
    static class RoadPath {
        final List<Point2D> pts;   // 離散點列 (多段線)
        final String name;         // 路徑名稱 (用於決策/除錯)
        final double length;       // 總長度 (像素)
        final double[] segLen;     // 各點到起點的累積長度 (prefix sums)

        public RoadPath(List<Point2D> pts, String name) {
            this.pts = pts;
            this.name = name;
            this.segLen = new double[pts.size()];
            double sum = 0.0;
            segLen[0] = 0.0;
            for (int i = 1; i < pts.size(); i++) {
                sum += pts.get(i - 1).dist(pts.get(i));
                segLen[i] = sum; // 累積長度至第 i 點
            }
            this.length = Math.max(1.0, sum); // 避免 0 長度 (保底為 1)
        }

        static RoadPath straight(double x1, double y1, double x2, double y2, String name) {
            // 直線以 41 個均勻取樣點表示 (n=40 -> 0..40)
            List<Point2D> l = new ArrayList<>();
            int n = 40;
            for (int i = 0; i <= n; i++) {
                double t = i / (double) n;
                l.add(new Point2D(lerp(x1, x2, t), lerp(y1, y2, t)));
            }
            return new RoadPath(l, name);
        }

        static List<RoadPath> leaf(double cx, double cy, double r, double degStart, double degEnd, boolean clockwise, String baseName) {
            // 生成 1 條弧線 (可延伸為內外兩條以模擬雙車道)
            List<RoadPath> res = new ArrayList<>();
            res.add(arc(cx, cy, r, degStart, degEnd, clockwise, baseName));
            return res;
        }

        static RoadPath arc(double cx, double cy, double r, double degStart, double degEnd, boolean clockwise, String name) {
            // 以固定步數離散圓弧 (步數越大越平滑)
            int steps = 120;
            double a0 = Math.toRadians(degStart);
            double a1 = Math.toRadians(degEnd);
            // 依順/逆時針調整終點角度，確保插值方向正確
//            if (clockwise && a1 > a0) //順時針 degEnd > degStart 
//                a1 -= Math.PI * 2;
//            if (!clockwise && a1 < a0)
//                a1 += Math.PI * 2;
            List<Point2D> l = new ArrayList<>();
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double a = a0 + (a1 - a0) * t;
                double x = cx + r * Math.cos(a);
                double y = cy + r * Math.sin(a);
                l.add(new Point2D(x, y));
            }
            return new RoadPath(l, name);
        }

        Point2D pointAt(double s01) {
            // 依 s01 [0..1) 對應到總長度，並在多段線中找出落在哪一段上，線性插值得到座標
            double target = s01 * length; // 目標弧長
            int hi = Arrays.binarySearch(segLen, target); // 二分搜累積長度
            if (hi >= 0)
                return pts.get(hi); // 剛好落在節點上
            hi = -hi - 1; // 轉為 upper_bound 位置
            int lo = Math.max(0, hi - 1);
            if (hi >= pts.size())
                return pts.get(pts.size() - 1); // 邊界保護
            double seg = segLen[hi] - segLen[lo];
            double t = seg > 1e-9 ? (target - segLen[lo]) / seg : 0; // 在 lo..hi 之間的內插比例
            Point2D a = pts.get(lo), b = pts.get(hi);
            return new Point2D(lerp(a.x, b.x, t), lerp(a.y, b.y, t));
        }

        double headingAt(double s01) {
            // 以微小位移前後取兩點，計算切線方向 (atan2)
            double eps = 1e-3;
            Point2D p1 = pointAt(Math.max(0, s01 - eps));
            Point2D p2 = pointAt(Math.min(1, s01 + eps));
            return Math.atan2(p2.y - p1.y, p2.x - p1.x);
        }

        double curvatureAt(double s01) {
            // 以三點近似曲率：k ≈ 2*三角形面積 / (a*b*c)
            double e = 1e-2; // 取樣間距
            Point2D p0 = pointAt(Math.max(0, s01 - e));
            Point2D p1 = pointAt(s01);
            Point2D p2 = pointAt(Math.min(1, s01 + e));
            double a = p0.dist(p1), b = p1.dist(p2), c = p0.dist(p2);
            double area2 = Math.abs((p1.x - p0.x) * (p2.y - p0.y) - (p1.y - p0.y) * (p2.x - p0.x)); // 2*三角形面積 (向量叉積絕對值)
            double denom = a * b * c;
            if (denom < 1e-6)
                return 0;
            return (2 * area2) / denom; // 近似曲率 (越大越彎)
        }

        Path2D asPath2D() {
            // 將離散點串成 Path2D，供 Graphics2D 繪出 (線段)
            Path2D p = new Path2D.Double();
            boolean first = true;
            for (Point2D pt : pts) {
                if (first) {
                    p.moveTo(pt.x, pt.y);
                    first = false;
                } else
                    p.lineTo(pt.x, pt.y);
            }
            return p;
        }

        static double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }
    }

    // ============================ Point2D ================================
    static class Point2D {
        final double x, y; // 平面座標

        Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        double dist(Point2D o) {
            double dx = x - o.x, dy = y - o.y;
            return Math.hypot(dx, dy); // sqrt(dx*dx + dy*dy)
        }
    }

    // ============================== Utils ================================
    static double lerp(double a, double b, double t) {
        return a + (b - a) * t; // 線性插值
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v)); // 夾取範圍
    }
    
		static class BezierUtil {
			static List<CloverleafSim.Point2D> cubicSamples(CloverleafSim.Point2D p0, CloverleafSim.Point2D c1, CloverleafSim.Point2D c2, CloverleafSim.Point2D p3, int n) {
				List<CloverleafSim.Point2D> pts = new ArrayList<>();
				for (int i = 0; i <= n; i++) {
					double t = i / (double) n;
					double it = 1 - t;
					double x = it * it * it * p0.x + 3 * it * it * t * c1.x + 3 * it * t * t * c2.x + t * t * t * p3.x;
					double y = it * it * it * p0.y + 3 * it * it * t * c1.y + 3 * it * t * t * c2.y + t * t * t * p3.y;
					pts.add(new CloverleafSim.Point2D(x, y));
				}
				return pts;
			}

			static CloverleafSim.Point2D add(CloverleafSim.Point2D a, double vx, double vy, double s) {
				return new CloverleafSim.Point2D(a.x + vx * s, a.y + vy * s);
			}
		}
		
}
