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
        
        int maxSize = Math.max(4, (rows * cols) >> 3) << 1; // Tighter interleaved queue size
        int[] queue = new int[maxSize];
        int front = 0, rear = 0;
        
        queue[rear++] = sr;
        queue[rear++] = sc;
        image[sr][sc] = newColor;

        while (front < rear) {
            int row = queue[front++];
            int col = queue[front++];

            // Check and process all directions in one block
            if (row + 1 < rows && image[row + 1][col] == originalColor) {
                image[row + 1][col] = newColor;
                if (rear + 2 <= maxSize) {
                    queue[rear++] = row + 1;
                    queue[rear++] = col;
                }
            }
            if (row > 0 && image[row - 1][col] == originalColor) {
                image[row - 1][col] = newColor;
                if (rear + 2 <= maxSize) {
                    queue[rear++] = row - 1;
                    queue[rear++] = col;
                }
            }
            if (col + 1 < cols && image[row][col + 1] == originalColor) {
                image[row][col + 1] = newColor;
                if (rear + 2 <= maxSize) {
                    queue[rear++] = row;
                    queue[rear++] = col + 1;
                }
            }
            if (col > 0 && image[row][col - 1] == originalColor) {
                image[row][col - 1] = newColor;
                if (rear + 2 <= maxSize) {
                    queue[rear++] = row;
                    queue[rear++] = col - 1;
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

        // Optimized printing with exact StringBuilder capacity
        StringBuilder sb = new StringBuilder(image.length * image[0].length + image.length + image[0].length - 2);
        for (int i = 0, len = image.length * image[0].length; i < len; i++) {
            sb.append((char) (image[i / image[0].length][i % image[0].length] + '0'));
            if ((i + 1) % image[0].length != 0) sb.append(' ');
            else if (i < len - 1) sb.append('\n');
        }
        System.out.print(sb);
    }
}