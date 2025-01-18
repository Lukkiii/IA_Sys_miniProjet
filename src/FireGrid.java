
public class FireGrid {
    // Valeur maximale de l'intensité du feu
    public static final double MAX_INTENSITY = 100.0;
    // Intensité initiale du feu
    public static final double INITIAL_INTENSITY = 80.0;
    // Seuil d'intensité pour déterminer si une case est en feu
    public static final double INTENSITY_THRESHOLD = 10.0;

    private double[][] grid;
    private int width;
    private int height;

    public FireGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new double[width][height];
    }

    // Mettre à jour la grille avec de nouvelles intensités
    public void updateGrid(double[][] newGrid) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                this.grid[i][j] = newGrid[i][j];
            }
        }
    }

    public void decreaseIntensity(int x, int y, double amount) {
        grid[x][y] = Math.max(0.0, grid[x][y] - amount);
    }

    public void increaseIntensity(int x, int y, double amount) {
        grid[x][y] = Math.min(MAX_INTENSITY, grid[x][y] + amount);
    }

    // ====== Getter et setter pour la grille d'intensité du feu ======
    public double[][] getIntensityGrid() {
        double[][] copy = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                copy[i][j] = grid[i][j];
            }
        }
        return copy;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public double getIntensityAt(int x, int y) {
        return grid[x][y];
    }

    public void setIntensityAt(int x, int y, double intensity) {
        grid[x][y] = Math.min(Math.max(intensity, 0.0), MAX_INTENSITY);
    }

    public double getIntensity(int newX, int newY) {
        return grid[newX][newY];
    }
}
