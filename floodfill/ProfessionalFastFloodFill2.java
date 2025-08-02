import java.util.Arrays;

public class ProfessionalFastFloodFill2 {

	// Custom high-performance span stack using int[] (y, leftX, rightX)
	static class SpanStack {
		private final int[] data;
		private int size = 0;

		SpanStack(int capacity) {
			data = new int[capacity * 3]; // 3 ints per span
		}

		void push(int y, int leftX, int rightX) {
			int idx = size * 3;
			data[idx] = y;
			data[idx + 1] = leftX;
			data[idx + 2] = rightX;
			size++;
		}

		boolean isEmpty() {
			return size == 0;
		}

		void pop(int[] span) {
			size--;
			int idx = size * 3;
			span[0] = data[idx]; // y
			span[1] = data[idx + 1]; // leftX
			span[2] = data[idx + 2]; // rightX
		}
	}

	public static void main(String[] args) {
		final int size = 5000;
		final int[][] image = new int[size][size];

		for (int i = 0; i < size; i++) {
			Arrays.fill(image[i], 1);
		}

		long start = System.nanoTime();
		floodFill(image, 0, 0, 2);
		long end = System.nanoTime();

		System.out.println("Fastest Flood Fill runtime: " + (end - start) / 1_000_000 + " ms");
	}

	public static void floodFill(int[][] image, int sr, int sc, int newColor) {
		final int rows = image.length;
		final int cols = image[0].length;
		final int targetColor = image[sr][sc];
		if (targetColor == newColor)
			return;

		final int maxSpans = rows * cols / 8; // estimated upper bound
		final SpanStack stack = new SpanStack(maxSpans);
		final int[] span = new int[3];

		stack.push(sr, sc, sc);

		while (!stack.isEmpty()) {
			stack.pop(span);
			final int y = span[0];
			int lx = span[1];
			int rx = span[2];

			// Expand left
			while (lx >= 0 && image[y][lx] == targetColor) {
				image[y][lx--] = newColor;
			}
			lx++;

			// Expand right
			while (rx < cols && image[y][rx] == targetColor) {
				image[y][rx++] = newColor;
			}
			rx--;

			// Above
			if (y > 0)
				scanLine(image, y - 1, lx, rx, targetColor, stack);
			// Below
			if (y < rows - 1)
				scanLine(image, y + 1, lx, rx, targetColor, stack);
		}
	}

	private static void scanLine(int[][] image, int y, int lx, int rx, int targetColor, SpanStack stack) {
		final int cols = image[0].length;
		int x = lx;
		while (x <= rx) {
			// Skip non-target pixels
			while (x <= rx && image[y][x] != targetColor)
				x++;
			if (x > rx)
				break;

			int startX = x;
			while (x < cols && image[y][x] == targetColor) {
				image[y][x++] = -1; // temp mark to avoid reprocessing
			}

			stack.push(y, startX, x - 1);
		}
	}
}
