import java.util.ArrayDeque;
import java.util.Deque;

public class ProfessionalFastFloodFill {

	// A lightweight data structure to store a horizontal span
	// Using a class is often clearer than an int array and the
	// overhead difference is negligible on a modern JVM.
	private static class Span {
		int y;
		int leftX;
		int rightX;

		public Span(int y, int leftX, int rightX) {
			this.y = y;
			this.leftX = leftX;
			this.rightX = rightX;
		}
	}

	public static void main(String[] args) {
		int size = 5000;
		int[][] image = new int[size][size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				image[i][j] = 1;
			}
		}

		long start = System.nanoTime();
		floodFill(image, 0, 0, 2);
		long end = System.nanoTime();

		System.out.println("Professional Scanline Flood Fill runtime: " + (end - start) / 1_000_000 + " ms");
	}

	/**
	 * Performs an optimized scanline flood fill. This method uses a stack-based
	 * approach to fill horizontal spans, which is significantly more efficient than
	 * filling pixel-by-pixel for large, contiguous regions.
	 *
	 * @param image    The 2D integer array representing the image.
	 * @param sr       The starting row for the fill.
	 * @param sc       The starting column for the fill.
	 * @param newColor The new color to fill with.
	 */
	public static void floodFill(int[][] image, int sr, int sc, int newColor) {
		int rows = image.length;
		int cols = image[0].length;
		int originalColor = image[sr][sc];

		// Edge case: if the new color is the same as the old, no work is needed.
		if (originalColor == newColor) {
			return;
		}

		// Using a Deque as a stack for optimal performance.
		Deque<Span> stack = new ArrayDeque<>();
		stack.push(new Span(sr, sc, sc));

		while (!stack.isEmpty()) {
			Span currentSpan = stack.pop();
			int y = currentSpan.y;
			int leftX = currentSpan.leftX;
			int rightX = currentSpan.rightX;

			// Fill leftward from the current span's starting point
			int fillLeftX = leftX;
			while (fillLeftX >= 0 && image[y][fillLeftX] == originalColor) {
				image[y][fillLeftX] = newColor;
				fillLeftX--;
			}
			fillLeftX++; // Adjust back to the start of the filled region

			// Fill rightward from the current span's starting point
			int fillRightX = rightX;
			while (fillRightX < cols && image[y][fillRightX] == originalColor) {
				image[y][fillRightX] = newColor;
				fillRightX++;
			}
			fillRightX--; // Adjust back to the end of the filled region

			// Now, scan the rows above and below the just-filled span for new seed points.
			// This is the core logic that makes the scanline algorithm fast.
			// We only need to check the row immediately above and below.

			// Scan row above
			if (y > 0) {
				scanForNewSpans(image, y - 1, fillLeftX, fillRightX, originalColor, stack);
			}

			// Scan row below
			if (y < rows - 1) {
				scanForNewSpans(image, y + 1, fillLeftX, fillRightX, originalColor, stack);
			}
		}
	}

	/**
	 * Helper method to scan a row for new seed points and add them to the stack.
	 * This logic is extracted into a separate method for clarity and to avoid code
	 * duplication.
	 */
	private static void scanForNewSpans(int[][] image, int y, int startX, int endX, int originalColor,
			Deque<Span> stack) {
		int cols = image[0].length;
		for (int x = startX; x <= endX; x++) {
			if (image[y][x] == originalColor) {
				// Found a new seed. Now find its full horizontal span.
				int newLeftX = x;
				while (newLeftX >= 0 && image[y][newLeftX] == originalColor) {
					newLeftX--;
				}
				newLeftX++;

				int newRightX = x;
				while (newRightX < cols && image[y][newRightX] == originalColor) {
					newRightX++;
				}
				newRightX--;

				// Push the entire new span onto the stack.
				stack.push(new Span(y, newLeftX, newRightX));

				// Important optimization: jump the loop past the newly found span
				// to avoid redundant checks.
				x = newRightX;
			}
		}
	}
}