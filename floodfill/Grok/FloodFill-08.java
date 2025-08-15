import java.util.ArrayDeque;
import java.util.Deque;

public class FloodFill {
    public void floodFill(int[][] image, int sr, int sc, int newColor) {
        if (image == null || image.length == 0 || image[0].length == 0 ||
            sr < 0 || sr >= image.length || sc < 0 || sc >= image[0].length ||
            image[sr][sc] == newColor) {
            return;
        }

        int rows = image.length;
        int cols = image[0].length;
        int originalColor = image[sr][sc];
        
        Deque<int[]> queue = new ArrayDeque<>(Math.max(16, (rows * cols) >> 3)); // Optimized initial capacity
        int[] coord = {sr, sc};
        queue.offer(coord);
        image[sr][sc] = newColor;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1];

            // Down
            if (row + 1 < rows && image[row + 1][col] == originalColor) {
                image[row + 1][col] = newColor;
                queue.offer(new int[]{row + 1, col});
            }
            // Up
            if (row > 0 && image[row - 1][col] == originalColor) {
                image[row - 1][col] = newColor;
                queue.offer(new int[]{row - 1, col});
            }
            // Right
            if (col + 1 < cols && image[row][col + 1] == originalColor) {
                image[row][col + 1] = newColor;
                queue.offer(new int[]{row, col + 1});
            }
            // Left
            if (col > 0 && image[row][col - 1] == originalColor) {
                image[row][col - 1] = newColor;
                queue.offer(new int[]{row, col - 1});
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

        // Optimized printing with StringBuilder
        StringBuilder sb = new StringBuilder(image.length * image[0].length * 2);
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                sb.append(image[i][j]);
                if (j < image[0].length - 1) sb.append(' ');
            }
            sb.append('\n');
        }
        System.out.print(sb);
    }
}