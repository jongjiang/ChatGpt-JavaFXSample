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
        
        Deque<int[]> queue = new ArrayDeque<>((rows * cols) >> 2); // Pre-allocate capacity
        int[] coord = {sr, sc};
        queue.offer(coord);
        image[sr][sc] = newColor;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0];
            int col = curr[1];

            // Down
            if (row + 1 < rows && image[row + 1][col] == originalColor) {
                image[row + 1][col] = newColor;
                curr[0] = row + 1;
                curr[1] = col;
                queue.offer(curr.clone());
            }
            // Up
            if (row - 1 >= 0 && image[row - 1][col] == originalColor) {
                image[row - 1][col] = newColor;
                curr[0] = row - 1;
                curr[1] = col;
                queue.offer(curr.clone());
            }
            // Right
            if (col + 1 < cols && image[row][col + 1] == originalColor) {
                image[row][col + 1] = newColor;
                curr[0] = row;
                curr[1] = col + 1;
                queue.offer(curr.clone());
            }
            // Left
            if (col - 1 >= 0 && image[row][col - 1] == originalColor) {
                image[row][col - 1] = newColor;
                curr[0] = row;
                curr[1] = col - 1;
                queue.offer(curr.clone());
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                sb.append(image[i][j]).append(j < image[0].length - 1 ? " " : "");
            }
            sb.append("\n");
        }
        System.out.print(sb);
    }
}