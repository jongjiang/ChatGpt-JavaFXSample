public class FloodFill {
    public void floodFill(int[][] image, int sr, int sc, int newColor) {
        // Input validation and edge case for 1x1 image
        if (image == null || image.length == 0 || image[0].length == 0 ||
            sr < 0 || sr >= image.length || sc < 0 || sc >= image[0].length ||
            image[sr][sc] == newColor) {
            return;
        }

        int originalColor = image[sr][sc];
        image[sr][sc] = newColor;

        // Direct recursive calls for adjacent pixels
        if (sr + 1 < image.length && image[sr + 1][sc] == originalColor) {
            floodFill(image, sr + 1, sc, newColor); // Down
        }
        if (sr - 1 >= 0 && image[sr - 1][sc] == originalColor) {
            floodFill(image, sr - 1, sc, newColor); // Up
        }
        if (sc + 1 < image[0].length && image[sr][sc + 1] == originalColor) {
            floodFill(image, sr, sc + 1, newColor); // Right
        }
        if (sc - 1 >= 0 && image[sr][sc - 1] == originalColor) {
            floodFill(image, sr, sc - 1, newColor); // Left
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