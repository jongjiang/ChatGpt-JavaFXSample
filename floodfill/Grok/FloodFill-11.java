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
        
        int maxSize = Math.max(8, (rows * cols) >> 3); // Tightened queue size
        maxSize = 1 << (32 - Integer.numberOfLeadingZeros(maxSize - 1)); // Next power of 2
        int[] queue = new int[maxSize];
        int front = 0, rear = 0;
        
        queue[rear] = (sr << 15) | sc; // Pack coordinates (15 bits row, 15 bits col)
        rear = (rear + 1) & (maxSize - 1);
        image[sr][sc] = newColor;

        while (front != rear) {
            int pos = queue[front];
            front = (front + 1) & (maxSize - 1);
            
            int row = pos >> 15;
            int col = pos & 0x7FFF;

            // Down
            if (row + 1 < rows && image[row + 1][col] == originalColor) {
                image[row + 1][col] = newColor;
                queue[rear] = ((row + 1) << 15) | col;
                rear = (rear + 1) & (maxSize - 1);
            }
            // Up
            if (row > 0 && image[row - 1][col] == originalColor) {
                image[row - 1][col] = newColor;
                queue[rear] = ((row - 1) << 15) | col;
                rear = (rear + 1) & (maxSize - 1);
            }
            // Right
            if (col + 1 < cols && image[row][col + 1] == originalColor) {
                image[row][col + 1] = newColor;
                queue[rear] = (row << 15) | (col + 1);
                rear = (rear + 1) & (maxSize - 1);
            }
            // Left
            if (col > 0 && image[row][col - 1] == originalColor) {
                image[row][col - 1] = newColor;
                queue[rear] = (row << 15) | (col - 1);
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
        StringBuilder sb = new StringBuilder(image.length * image[0].length + (image.length - 1) + image[0].length - 1);
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