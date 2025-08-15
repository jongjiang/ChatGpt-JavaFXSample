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
        
        int maxSize = Math.max(8, (rows * cols + 7) >> 3) << 1; // Interleaved storage
        maxSize = 1 << (32 - Integer.numberOfLeadingZeros(maxSize - 1)); // Power of 2
        int[] queue = new int[maxSize];
        int front = 0, rear = 0;
        
        queue[rear++] = sr;
        queue[rear++] = sc;
        image[sr][sc] = newColor;

        while (front < rear) {
            int row = queue[front++];
            int col = queue[front++];

            if (row + 1 < rows && image[row + 1][col] == originalColor) {
                image[row + 1][col] = newColor;
                queue[rear++] = row + 1;
                queue[rear++] = col;
            }
            if (row > 0 && image[row - 1][col] == originalColor) {
                image[row - 1][col] = newColor;
                queue[rear++] = row - 1;
                queue[rear++] = col;
            }
            if (col + 1 < cols && image[row][col + 1] == originalColor) {
                image[row][col + 1] = newColor;
                queue[rear++] = row;
                queue[rear++] = col + 1;
            }
            if (col > 0 && image[row][col - 1] == originalColor) {
                image[row][col - 1] = newColor;
                queue[rear++] = row;
                queue[rear++] = col - 1;
            }
            rear &= maxSize - 1;
            front &= maxSize - 1;
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

        // Optimized printing with exact StringBuilder capacity
        StringBuilder sb = new StringBuilder(image.length * image[0].length + (image.length - 1) + (image[0].length - 1));
        for (int i = 0, len = image.length - 1, width = image[0].length - 1; i <= len; i++) {
            for (int j = 0; j <= width; j++) {
                sb.append((char) (image[i][j] + '0'));
                if (j < width) sb.append(' ');
            }
            if (i < len) sb.append('\n');
        }
        System.out.print(sb);
    }
}