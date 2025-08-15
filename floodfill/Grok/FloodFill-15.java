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
        
        int maxSize = 1 << (32 - Integer.numberOfLeadingZeros((rows * cols) >> 3)); // Power-of-2 size
        int[] queue = new int[maxSize];
        int front = 0, rear = 0;
        
        queue[rear++] = (sr << 16) | sc; // Pack row and col into one int
        image[sr][sc] = newColor;

        int[] dr = {1, -1, 0, 0}; // Direction offsets: down, up, right, left
        int[] dc = {0, 0, 1, -1};

        while (front < rear) {
            int pos = queue[front++];
            int row = pos >> 16;
            int col = pos & 0xFFFF;

            for (int i = 0; i < 4; i++) {
                int nr = row + dr[i];
                int nc = col + dc[i];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && image[nr][nc] == originalColor) {
                    image[nr][nc] = newColor;
                    queue[rear++] = (nr << 16) | nc;
                    if (rear == maxSize) rear = 0; // Circular wraparound
                }
            }
            if (front == maxSize) front = 0; // Circular wraparound
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
            if ((i + 1) % image[0].length > 0) sb.append(' ');
            else if (i < len - 1) sb.append('\n');
        }
        System.out.print(sb);
    }
}