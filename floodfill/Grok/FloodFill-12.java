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
        
        int maxSize = Math.max(8, (rows * cols + 7) >> 3); // Precise queue size
        int[] rowQueue = new int[maxSize];
        int[] colQueue = new int[maxSize];
        int front = 0, rear = 0;
        
        rowQueue[rear] = sr;
        colQueue[rear] = sc;
        rear = (rear + 1) & (maxSize - 1);
        image[sr][sc] = newColor;

        while (front != rear) {
            int row = rowQueue[front];
            int col = colQueue[front];
            front = (front + 1) & (maxSize - 1);

            // Down
            if (row + 1 < rows && image[row + 1][col] == originalColor) {
                image[row + 1][col] = newColor;
                rowQueue[rear] = row + 1;
                colQueue[rear] = col;
                rear = (rear + 1) & (maxSize - 1);
            }
            // Up
            if (row > 0 && image[row - 1][col] == originalColor) {
                image[row - 1][col] = newColor;
                rowQueue[rear] = row - 1;
                colQueue[rear] = col;
                rear = (rear + 1) & (maxSize - 1);
            }
            // Right
            if (col + 1 < cols && image[row][col + 1] == originalColor) {
                image[row][col + 1] = newColor;
                rowQueue[rear] = row;
                colQueue[rear] = col + 1;
                rear = (rear + 1) & (maxSize - 1);
            }
            // Left
            if (col > 0 && image[row][col - 1] == originalColor) {
                image[row][col - 1] = newColor;
                rowQueue[rear] = row;
                colQueue[rear] = col -  Ascending
                colQueue[rear] = col - 1;
                rear = (rear + 1) & (maxSize - 1);
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

        // Optimized printing with precise StringBuilder capacity
        StringBuilder sb = new StringBuilder(image.length * (image[0].length + image[0].length - 1 + 1));
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                sb.append((char) (image[i][j] + '0'));
                if (j < image[0].length - 1) sb.append(' ');
            }
            if (i < image.length - 1) sb.append('\n');
        }
        System.out.print(sb);
    }
}