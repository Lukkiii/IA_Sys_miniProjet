import java.util.Random;
public class Fire {
    private FireGrid fireGrid;
    private Random random = new Random();
    // Probabilité de propagation du feu
    private static final double SPREAD_PROBABILITY = 0.3;

    public Fire(int width, int height) {
        this.fireGrid = new FireGrid(width, height);
        initializeMultipleFires();
    }

    // Initialiser 2 ou 3 feux
    private void initializeMultipleFires() {
        int numFires = random.nextInt(2) + 2;
        for (int i = 0; i < numFires; i++) {
            createNewFire();
        }
    }

    // Créer un nouveau feu aléatoirement
    private void createNewFire() {
        int maxAttempts = 10;
        int attempts = 0;
        while (attempts < maxAttempts) {
            int x = random.nextInt(fireGrid.getWidth());
            int y = random.nextInt(fireGrid.getHeight());
            // Ne pas créer de feu près des headquarters
            if (!isNearHQ(x, y)) {
                // Intensité initiale du feu
                double initialIntensity = FireGrid.INITIAL_INTENSITY * (0.8 + random.nextDouble() * 0.2);
                fireGrid.setIntensityAt(x, y, initialIntensity);
                break;
            }
            attempts++;
        }
    }

    // Propager le feu
    public void spread() {
        double[][] currentIntensities = fireGrid.getIntensityGrid();
        double[][] newGrid = new double[fireGrid.getWidth()][fireGrid.getHeight()];
        boolean hasActiveFire = false;
        
        for (int i = 0; i < fireGrid.getWidth(); i++) {
            for (int j = 0; j < fireGrid.getHeight(); j++) {
                // Copier les intensités actuelles
                newGrid[i][j] = currentIntensities[i][j];
                if (currentIntensities[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    hasActiveFire = true;
                    // Renforcer le feu existant avec une probabilité de 30%
                    if (random.nextDouble() < 0.3) {
                        // Augmenter l'intensité du feu mais pas au-dessus de la valeur maximale
                        newGrid[i][j] = Math.min(FireGrid.MAX_INTENSITY, currentIntensities[i][j] + (random.nextDouble() * 20 - 5));
                    }
                }
            }
        }

        // Propager le feu
        for (int i = 0; i < fireGrid.getWidth(); i++) {
            for (int j = 0; j < fireGrid.getHeight(); j++) {
                if (currentIntensities[i][j] > FireGrid.INTENSITY_THRESHOLD) {
                    spreadToNeighbors(i, j, newGrid, currentIntensities[i][j]);
                }
            }
        }

        // Create new fire if none exists
        if (!hasActiveFire) {
            createNewFire();
        }
        
        fireGrid.updateGrid(newGrid);
    }

    // Propager le feu aux voisins
    private void spreadToNeighbors(int x, int y, double[][] newGrid, double sourceIntensity) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {

                // Ne pas propager le feu à la même cellule
                if (dx == 0 && dy == 0) continue;

                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < fireGrid.getWidth() && ny >= 0 && ny < fireGrid.getHeight()) {
                    if (newGrid[nx][ny] < FireGrid.INTENSITY_THRESHOLD && random.nextDouble() < SPREAD_PROBABILITY) {

                        // Intensité de propagation du feu (60% à 90% de l'intensité source)
                        double spreadIntensity = sourceIntensity * (0.6 + random.nextDouble() * 0.3);
                        // Limiter l'intensité de propagation du feu 
                        spreadIntensity = Math.max(FireGrid.INTENSITY_THRESHOLD + 10, Math.min(FireGrid.MAX_INTENSITY, spreadIntensity));
                        newGrid[nx][ny] = spreadIntensity;
                    }
                }
            }
        }
    }

    // Vérifier si la cellule est proche des headquarters
    private boolean isNearHQ(int x, int y) {
        int hqX = 15;
        int hqY = 15;
        int safeDistance = 5;
        double distance = Math.sqrt(Math.pow(x - hqX, 2) + Math.pow(y - hqY, 2));
        return distance < safeDistance;
    }

    public FireGrid getFireGrid() {
        return this.fireGrid;
    }

    // Récupérer la carte du feu sous forme de booléens
    public boolean[][] getFireMap() {
        boolean[][] boolMap = new boolean[fireGrid.getWidth()][fireGrid.getHeight()];
        double[][] intensityMap = fireGrid.getIntensityGrid();
        for (int i = 0; i < fireGrid.getWidth(); i++) {
            for (int j = 0; j < fireGrid.getHeight(); j++) {
                boolMap[i][j] = intensityMap[i][j] > FireGrid.INTENSITY_THRESHOLD;
            }
        }
        return boolMap;
    }

    // Récupérer la carte d'intensité du feu
    public double[][] getIntensityMap() {
        return fireGrid.getIntensityGrid();
    }
}