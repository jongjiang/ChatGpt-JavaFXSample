import java.util.ArrayDeque;
import java.util.Deque;

public class FloodFillOptimized {
    public void floodFill(int[][] image, int sr, int sc, int newColor) {
        if (image == null || image.length == 0 || image[0].length == 0) {
            return;
        }

        int rows = image.length;
        int cols = image[0].length;

        // Check boundary conditions and if the color is already the new color.
        int originalColor = image[sr][sc];
        if (originalColor == newColor) {
            return;
        }

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sr, sc});

        while (!stack.isEmpty()) {
            int[] current = stack.pop();
            int y = current[0];
            int x = current[1];

            // Find the left boundary of the current span.
            int left = x;
            while (left > 0 && image[y][left - 1] == originalColor) {
                left--;
            }

            // Fill the span and check for new spans above and below.
            boolean hasSpanAbove = false;
            boolean hasSpanBelow = false;
            for (int i = left; i < cols && image[y][i] == originalColor; i++) {
                image[y][i] = newColor;

                // Check for a new span in the row above.
                if (y > 0) {
                    boolean isAboveSameColor = (image[y - 1][i] == originalColor);
                    if (isAboveSameColor && !hasSpanAbove) {
                        stack.push(new int[]{y - 1, i});
                        hasSpanAbove = true;
                    } else if (!isAboveSameColor) {
                        hasSpanAbove = false;
                    }
                }

                // Check for a new span in the row below.
                if (y < rows - 1) {
                    boolean isBelowSameColor = (image[y + 1][i] == originalColor);
                    if (isBelowSameColor && !hasSpanBelow) {
                        stack.push(new int[]{y + 1, i});
                        hasSpanBelow = true;
                    } else if (!isBelowSameColor) {
                        hasSpanBelow = false;
                    }
                }
            }
        }
    }
}