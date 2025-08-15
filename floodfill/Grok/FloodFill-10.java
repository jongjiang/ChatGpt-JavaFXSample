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
        
        int maxSize = (rows * cols) >> 2; // Tightened queue size
        int[] queue = new int[maxSize];
        int front = 0, rear = 0, size = 0;
        
        queue[rear] = (sr << 16) | sc; // Pack coordinates into single int
        rear = (rear + 1) % maxSize;
        size++;
        image[sr][sc] = newColor;

        while (size > 0) {
            int pos = queue[front];
            front = (front + 1) % maxSize;
            size--;
            
            int row = pos >> 16;
            int col = pos & 0xFFFF;

            // Down
            if (row + 1 < rows && image[row + 1][col] == originalColor) {
                image[row + 1][col] = newColor;
                queue[rear] = ((row + 1) << 16) | col;
                rear = (rear + 1) % maxSize;
                size++;
            }
            // Up
            if (row > 0 && image[row - 1][col] == originalColor) {
                image[row - 1][col] = newColor;
                queue[rear] = ((row - 1) << 16) | col;
                rear = (rear + 1) % maxSize;
                size++;
            }
            // Right
            if (col + 1 < cols && image[row][col + 1] == originalColor) {
                image[row][col + 1] = newColor;
                queue[rear] = (row << 16) | (col + 1);
                rear = (rear + 1) % maxSize;
                size++;
            }
            // Left
            if (col > 0 && image[row][col - 1] == originalColor) {
                image[row][col - 1] = newColor;
                queue[rear] = (row << 16) | (col - 1);
                rear = (rear + 1) % maxSize;
                size++;
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
        StringBuilder sb = new StringBuilder(image.length * (image[0].length * 2 - 1 + 1));
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