public class FloodFill {
    // Define directions for adjacent pixels (down, up, right, left)
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    
    public void floodFill(int[][] image, int sr, int sc, int newColor) {
        // Input validation
        if (image == null || image.length == 0 || image[0].length == 0 ||
            sr < 0 || sr >= image.length || sc < 0 || sc >= image[0].length) {
            return;
        }
        
        int originalColor = image[sr][sc];
        // If original color is same as new color, no need to process
        if (originalColor == newColor) {
            return;
        }
        
        // Replace color
        image[sr][sc] = newColor;
        
        // Process all adjacent pixels
        for (int[] dir : DIRECTIONS) {
            int newRow = sr + dir[0];
            int newCol = sc + dir[1];
            
            // Inline bounds and color check
            if (newRow >= 0 && newRow < image.length && 
                newCol >= 0 && newCol < image[0].length && 
                image[newRow][newCol] == originalColor) {
                floodFill(image, newRow, newCol, newColor);
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
        
        // Print result using StringBuilder for efficiency
        StringBuilder sb = new StringBuilder();
        for (int[] row : image) {
            for (int pixel : row) {
                sb.append(pixel).append(" ");
            }
            sb.append("\n");
        }
        System.out.print(sb.toString());
    }
}