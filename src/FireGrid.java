
public class FireGrid {
    private final FireScenario.Parameters params;

    private double[][] grid;
    private int width;
    private int height;

    public FireGrid(int width, int height, FireScenario.Parameters params) {
        this.width = width;
        this.height = height;
        this.grid = new double[width][height];
        this.params = params;
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
        grid[x][y] = Math.min(params.maxIntensity, grid[x][y] + amount);
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
        grid[x][y] = Math.min(Math.max(intensity, 0.0), params.maxIntensity);
    }

    public double getIntensity(int newX, int newY) {
        return grid[newX][newY];
    }

    public double getMaxIntensity() { return params.maxIntensity; }
    public double getInitialIntensity() { return params.initialIntensity; }
    public double getIntensityThreshold() { return params.intensityThreshold; }
    public double getSpreadProbability() { return params.spreadProbability; }
    public String getScenarioDescription() { return params.description; }
}
