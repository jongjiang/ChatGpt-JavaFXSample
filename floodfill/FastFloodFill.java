public class FastFloodFill {

	public static void main(String[] args) {
		int size = 5000;
		int[][] image = new int[size][size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				image[i][j] = 0;
			}
		}

		// Block for visual testing
		image[100][100] = 0;
		image[100][101] = 0;

		long start = System.nanoTime();
		floodFill(image, 0, 0, 2);
		long end = System.nanoTime();

		System.out.println("Flood fill runtime: " + (end - start) / 1000000 + " ms");
	}
    
	//sr: start row, sc:start column
	public static void floodFill(int[][] image, int sr, int sc, int newColor) {
		int rows = image.length;
		int cols = image[0].length;
		int originalColor = image[sr][sc];
		if (originalColor == newColor)
			return;

		int[] stack = new int[rows * cols];
		int top = 0;
		stack[top++] = sr * cols + sc;
		image[sr][sc] = newColor;

		while (top > 0) {
			int index = stack[--top];
			int r = index / cols;
			int c = index % cols;

			// Up
			if (r > 0 && image[r - 1][c] == originalColor) {
				image[r - 1][c] = newColor;
				stack[top++] = (r - 1) * cols + c;
			}
			// Down
			if (r < rows - 1 && image[r + 1][c] == originalColor) {
				image[r + 1][c] = newColor;
				stack[top++] = (r + 1) * cols + c;
			}
			// Left
			if (c > 0 && image[r][c - 1] == originalColor) {
				image[r][c - 1] = newColor;
				stack[top++] = r * cols + (c - 1);
			}
			// Right
			if (c < cols - 1 && image[r][c + 1] == originalColor) {
				image[r][c + 1] = newColor;
				stack[top++] = r * cols + (c + 1);
			}
		}
	}

}
