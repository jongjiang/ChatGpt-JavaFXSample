import java.util.ArrayDeque;
import java.util.Deque;

public class FloodFillRefined {

    /**
     * Performs a flood fill operation on a 2D integer array (image).
     * This implementation uses a span-based approach, which is an iterative
     * alternative to recursive flood fill, preventing StackOverflowErrors on
     * large images and potentially being more cache-friendly.
     *
     * @param image     The 2D array representing the image.
     * @param startRow  The row index of the starting pixel.
     * @param startCol  The column index of the starting pixel.
     * @param newColor  The new color to fill the region with.
     */
    public void floodFill(int[][] image, int startRow, int startCol, int newColor) {
        // Essential null and dimension checks for robustness.
        if (image == null || image.length == 0 || image[0].length == 0) {
            return;
        }

        int rows = image.length;
        int cols = image[0].length;

        // Initial check for the starting point being out of bounds.
        // Also, check if the color is already the target color to avoid unnecessary work.
        if (startRow < 0 || startRow >= rows || startCol < 0 || startCol >= cols) {
            return;
        }

        int originalColor = image[startRow][startCol];
        if (originalColor == newColor) {
            return;
        }

        // Use Deque as a stack for LIFO (Last-In, First-Out) behavior.
        // We store coordinates as a simple int array.
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startRow, startCol});

        while (!stack.isEmpty()) {
            int[] current = stack.pop();
            int y = current[0];
            int x = current[1];

            // 1. Find the left boundary of the current horizontal span.
            // This is a key step of the span-based algorithm.
            int left = x;
            while (left > 0 && image[y][left - 1] == originalColor) {
                left--;
            }

            // 2. Iterate through the span, fill it, and check neighbors.
            boolean hasSpanAbove = false;
            boolean hasSpanBelow = false;
            for (int i = left; i < cols && image[y][i] == originalColor; i++) {
                // Fill the current pixel.
                image[y][i] = newColor;

                // Check for new spans in the row above.
                if (y > 0) {
                    if (image[y - 1][i] == originalColor && !hasSpanAbove) {
                        stack.push(new int[]{y - 1, i});
                        hasSpanAbove = true;
                    } else if (image[y - 1][i] != originalColor) {
                        hasSpanAbove = false;
                    }
                }

                // Check for new spans in the row below.
                if (y < rows - 1) {
                    if (image[y + 1][i] == originalColor && !hasSpanBelow) {
                        stack.push(new int[]{y + 1, i});
                        hasSpanBelow = true;
                    } else if (image[y + 1][i] != originalColor) {
                        hasSpanBelow = false;
                    }
                }
            }
        }
    }
}