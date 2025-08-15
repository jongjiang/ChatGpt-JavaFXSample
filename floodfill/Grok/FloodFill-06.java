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
        
        Deque<int[]> queue = new ArrayDeque<>();
        int[] coord = new int[2];
        coord[0] = sr;
        coord[1] = sc;
        queue.offer(coord);
        image[sr][sc] = newColor;

        // Direction offsets: down, up, right, left
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0];
            int col = curr[1];

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols &&
                    image[newRow][newCol] == originalColor) {
                    image[newRow][newCol] = newColor;
                    int[] next = new int[2];
                    next[0] = newRow;
                    next[1] = newCol;
                    queue.offer(next);
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

        // Optimized printing
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                System.out.print(image[i][j] + (j < image[0].length - 1 ? " " : ""));
            }
            System.out.println();
        }
    }
}