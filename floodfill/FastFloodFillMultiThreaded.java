public class FastFloodFillMultiThreaded {

	// Custom lightweight stack for primitives
	static class IntStack {
		private int[] data;
		private int top;

		public IntStack(int capacity) {
			data = new int[capacity];
			top = 0;
		}

		public void push(int value) {
			if (top == data.length) {
				// Resize if needed, though with a 5000x5000 grid, this is unlikely
				// for a simple scanline fill.
				int[] newData = new int[data.length * 2];
				System.arraycopy(data, 0, newData, 0, data.length);
				data = newData;
			}
			data[top++] = value;
		}

		public int pop() {
			return data[--top];
		}

		public boolean isEmpty() {
			return top == 0;
		}
	}

	public static void main(String[] args) {
		int size = 5000;
		int[][] image = new int[size][size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				image[i][j] = 0;
			}
		}

		long start = System.nanoTime();
		floodFill(image, 0, 0, 2);
		long end = System.nanoTime();

		System.out.println("Final optimized runtime: " + (end - start) / 1000000 + " ms");
	}

	public static void floodFill(int[][] image, int sr, int sc, int newColor) {
		int rows = image.length;
		int cols = image[0].length;
		int originalColor = image[sr][sc];

		if (originalColor == newColor) {
			return;
		}

		// Using a single stack to store y * cols + x coordinates
		IntStack stack = new IntStack(rows * cols);
		stack.push(sr * cols + sc);

		while (!stack.isEmpty()) {
			int index = stack.pop();
			int y = index / cols;
			int x = index % cols;

			// Fill left and find new seeds
			int fillLeftX = x;
			while (fillLeftX >= 0 && image[y][fillLeftX] == originalColor) {
				image[y][fillLeftX] = newColor;
				fillLeftX--;
			}
			fillLeftX++;

			// Fill right and find new seeds
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
					stack.push((y - 1) * cols + i);
					while (i <= fillRightX && y > 0 && image[y - 1][i] == originalColor) {
						i++;
					}
					i--;
				}

				// Check row below
				if (y < rows - 1 && image[y + 1][i] == originalColor) {
					stack.push((y + 1) * cols + i);
					while (i <= fillRightX && y < rows - 1 && image[y + 1][i] == originalColor) {
						i++;
					}
					i--;
				}
			}
		}
	}
}