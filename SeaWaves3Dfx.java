import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class SeaWaves3Dfx extends Application {

	double anchorX, anchorY, anchorAngle;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		Group root = new Group();

		// Create a wave shape
		int nPoints = 30;
		int nWaves = 10;

		float[] coords = new float[(nPoints + 1) * nWaves * 3];
		int index = 0;
		for (int w = 0; w < nWaves; w++) {
			for (int i = 0; i <= nPoints; i++) {
				double angle = i * 2 * Math.PI / nPoints;
				double x = 10 * angle;
				double y = 100 * Math.sin(angle + w);
				double z = 100 * w;
				coords[index++] = (float) x;
				coords[index++] = (float) y;
				coords[index++] = (float) z;
			}
		}

		MeshView wave = new MeshView(new TriangleMesh());
		TriangleMesh mesh = (TriangleMesh) wave.getMesh();
		mesh.getPoints().setAll(coords);
		mesh.getTexCoords().setAll(0, 0); // texture array, only 0 and 1
		mesh.getFaces().setAll(createFaces(nPoints, nWaves));
		wave.setMaterial(new PhongMaterial(Color.BLUE));
		wave.setRotationAxis(Rotate.Y_AXIS);
		wave.setTranslateX(250);
		wave.setTranslateY(250);
		wave.setCullFace(CullFace.NONE);

		root.getChildren().add(wave);

		Scene scene = new Scene(root, 800, 800, true);

		scene.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				anchorX = event.getSceneX();
				anchorY = event.getSceneY();
				anchorAngle = wave.getRotate();
			}
		});

		scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				wave.setRotate(anchorAngle + anchorX - event.getSceneX());
			}
		});

		scene.setCamera(new PerspectiveCamera(false));
		stage.setScene(scene);
		stage.show();
	}

	private int[] createFaces(int nPoints, int nWaves) {
		int[] faces = new int[2 * 3 * nPoints * nWaves * 2];
//		System.out.println("faces="+(2 * 3 * nPoints * nWaves));
		int index = 0;
		for (int w = 0; w < nWaves - 1; w++) {
			for (int i = 0; i < nPoints; i++) {
				int p0 = w * (nPoints + 1) + i;
				int p1 = p0 + nPoints + 1;
				int p2 = p0 + 1;
				int p3 = p1 + 1;
				// clockwise
				faces[index++] = p2;  //p2
				faces[index++] = 0;   //t0
				faces[index++] = p0;  //p0
				faces[index++] = 0;   //t1
				faces[index++] = p1;  //p1
				faces[index++] = 0;   //t2

				faces[index++] = p2;  //p2
				faces[index++] = 0;   //t0
				faces[index++] = p1;  //p1
				faces[index++] = 0;   //t1
				faces[index++] = p3;  //p3
				faces[index++] = 0;   //t2
			}
		}
		return faces;
	}

}
