import java.util.ArrayDeque;
import java.util.Deque;

public class FastFloodFillScanline {

	public static void main(String[] args) {
		int size = 5000;
		int[][] image = new int[size][size];

		// Initialize a solid color image
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				image[i][j] = 0;
			}
		}

		long start = System.nanoTime();
		floodFill(image, 0, 0, 2);
		long end = System.nanoTime();

		System.out.println("Scanline Flood fill runtime: " + (end - start) / 1000000 + " ms");
	}

	public static void floodFill(int[][] image, int sr, int sc, int newColor) {
		int rows = image.length;
		int cols = image[0].length;
		int originalColor = image[sr][sc];

		if (originalColor == newColor) {
			return;
		}

		// Deque is more efficient than a LinkedList for stack/queue operations
		Deque<int[]> stack = new ArrayDeque<>();
		stack.push(new int[] { sr, sc });

		while (!stack.isEmpty()) {
			int[] point = stack.pop();
			int y = point[0];
			int x = point[1];

			// Fill left
			int fillLeftX = x;
			while (fillLeftX >= 0 && image[y][fillLeftX] == originalColor) {
				image[y][fillLeftX] = newColor;
				fillLeftX--;
			}
			fillLeftX++;

			// Fill right
			int fillRightX = x + 1;
			while (fillRightX < cols && image[y][fillRightX] == originalColor) {
				image[y][fillRightX] = newColor;
				fillRightX++;
			}
			fillRightX--;

			// Scan adjacent rows for new seeds
			for (int i = fillLeftX; i <= fillRightX; i++) {
				// Check row above
				if (y > 0 && image[y - 1][i] == originalColor) {
					stack.push(new int[] { y - 1, i });
					// Skip over the entire horizontal span to avoid redundant pushes
					while (i <= fillRightX && y > 0 && image[y - 1][i] == originalColor) {
						i++;
					}
					i--;
				}

				// Check row below
				if (y < rows - 1 && image[y + 1][i] == originalColor) {
					stack.push(new int[] { y + 1, i });
					// Skip over the entire horizontal span to avoid redundant pushes
					while (i <= fillRightX && y < rows - 1 && image[y + 1][i] == originalColor) {
						i++;
					}
					i--;
				}
			}
		}
	}
}