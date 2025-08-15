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
        
        floodFillHelper(image, sr, sc, originalColor, newColor);
    }
    
    private void floodFillHelper(int[][] image, int row, int col, int originalColor, int newColor) {
        // Boundary and color check
        if (row < 0 || row >= image.length || 
            col < 0 || col >= image[0].length || 
            image[row][col] != originalColor) {
            return;
        }
        
        // Replace color
        image[row][col] = newColor;
        
        // Process all adjacent pixels
        for (int[] dir : DIRECTIONS) {
            floodFillHelper(image, row + dir[0], col + dir[1], originalColor, newColor);
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
        
        // Print result
        for (int[] row : image) {
            for (int pixel : row) {
                System.out.print(pixel + " ");
            }
            System.out.println();
        }
    }
}