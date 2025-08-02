import java.util.LinkedList;
import java.util.Queue;

public class FastFloodFillOptimized {

	public static void main(String[] args) {
		int size = 5000;
		int[][] image = new int[size][size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				image[i][j] = 0;
			}
		}

		image[100][100] = 0;
		image[100][101] = 0;

		long start = System.nanoTime();
		floodFill(image, 0, 0, 2);
		long end = System.nanoTime();

		System.out.println("Flood fill runtime: " + (end - start) / 1000000 + " ms");
	}

	public static void floodFill(int[][] image, int sr, int sc, int newColor) {
		int rows = image.length;
		int cols = image[0].length;
		int originalColor = image[sr][sc];

		if (originalColor == newColor) {
			return;
		}

		// Using a Queue for Breadth-First Search (BFS) is often more cache-friendly
		// and can be faster than a stack for large, contiguous regions.
		Queue<int[]> queue = new LinkedList<>();
		queue.add(new int[] { sr, sc });
		image[sr][sc] = newColor;

		int[] dx = { 0, 0, 1, -1 };
		int[] dy = { 1, -1, 0, 0 };

		while (!queue.isEmpty()) {
			int[] cell = queue.poll();
			int r = cell[0];
			int c = cell[1];

			for (int i = 0; i < 4; i++) {
				int nr = r + dx[i];
				int nc = c + dy[i];

				if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && image[nr][nc] == originalColor) {
					image[nr][nc] = newColor;
					queue.add(new int[] { nr, nc });
				}
			}
		}
	}
}