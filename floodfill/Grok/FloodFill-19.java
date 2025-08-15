import java.util.ArrayDeque;
import java.util.Deque;

public class FloodFill {
    public void floodFill(int[][] image, int sr, int sc, int newColor) {
        if (image == null || image.length == 0 || image[0].length == 0 ||
            sr < 0 || sr >= image.length || sc < 0 || sc >= image[0].length ||
            image[sr][sc] == newColor) {
            return;
        }

        int originalColor = image[sr][sc];
        int rows = image.length;
        int cols = image[0].length;

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sr, sc});

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int y = p[0];
            int x = p[1];

            // Expand left
            while (x > 0 && image[y][x - 1] == originalColor) {
                x--;
            }

            boolean spanAbove = false;
            boolean spanBelow = false;

            // Fill span and check above/below
            for (int i = x; i < cols && image[y][i] == originalColor; i++) {
                image[y][i] = newColor;

                if (y > 0) {
                    if (image[y - 1][i] == originalColor) {
                        if (!spanAbove) {
                            stack.push(new int[]{y - 1, i});
                            spanAbove = true;
                        }
                    } else {
                        spanAbove = false;
                    }
                }

                if (y < rows - 1) {
                    if (image[y + 1][i] == originalColor) {
                        if (!spanBelow) {
                            stack.push(new int[]{y + 1, i});
                            spanBelow = true;
                        }
                    } else {
                        spanBelow = false;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        int[][] image = {
            {1, 1, 1},
            {1, 1, 0},
            {1, 0, 1}
        };

        FloodFill ff = new FloodFill();
        ff.floodFill(image, 1, 1, 2);

        // Optimized printing with exact capacity
        StringBuilder sb = new StringBuilder(image.length * image[0].length + image.length + image[0].length - 2);
        for (int i = 0, len = image.length * image[0].length; i < len; i++) {
            int r = i / image[0].length, c = i % image[0].length;
            sb.append((char) (image[r][c] + '0')).append(c < image[0].length - 1 ? ' ' : i < len - image[0].length ? '\n' : "");
        }
        System.out.print(sb);
    }
}