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
        
        int maxSize = Math.max(4, (rows * cols) >> 4) << 1; // Tighter size estimate
        int[] queue = new int[maxSize];
        int front = 0, rear = 0;
        
        queue[rear++] = sr;
        queue[rear++] = sc;
        image[sr][sc] = newColor;

        int[] offsets = {1, 0, -1, 0, 0, 1, 0, -1}; // Down, up, right, left

        while (front < rear) {
            int row = queue[front++], col = queue[front++];
            for (int i = 0; i < 8; i += 2) {
                int nr = row + offsets[i];
                int nc = col + offsets[i + 1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && image[nr][nc] == originalColor) {
                    image[nr][nc] = newColor;
                    queue[rear++] = nr;
                    queue[rear++] = nc;
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
        StringBuilder sb = new StringBuilder(image.length * (image[0].length + 1));
        for (int i = 0, len = image.length * image[0].length, w = image[0].length; i < len; i++) {
            sb.append((char) (image[i / w][i % w] + '0')).append(i % w < w - 1 ? ' ' : i < len - w ? '\n' : "");
        }
        System.out.print(sb);
    }
}