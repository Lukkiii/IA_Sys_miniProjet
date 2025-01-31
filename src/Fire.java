import java.util.Random;
public class Fire {

    private FireGrid fireGrid;
    private Random random = new Random();

    public Fire(FireGrid fireGrid) {
        this.fireGrid = fireGrid;
        this.random = new Random();
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
        // Essayer de créer un feu à une position aléatoire (10 tentatives)
        while (attempts < maxAttempts) {
            int x = random.nextInt(fireGrid.getWidth());
            int y = random.nextInt(fireGrid.getHeight());
            // Ne pas créer de feu près du quartier général
            if (!isNearHQ(x, y)) {
                // 80% à 100% de l'intensité initiale
                double initialIntensity = fireGrid.getInitialIntensity() * (0.8 + random.nextDouble() * 0.2);
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
                if (currentIntensities[i][j] > fireGrid.getIntensityThreshold()) {
                    hasActiveFire = true;
                    // Renforcer le feu existant avec une probabilité de 30%
                    if (random.nextDouble() < 0.3) {
                        // Augmenter l'intensité du feu mais pas au-dessus de la valeur maximale
                        newGrid[i][j] = Math.min(fireGrid.getMaxIntensity(), currentIntensities[i][j] + (random.nextDouble() * 20 - 5));
                    }
                }
            }
        }

        for (int i = 0; i < fireGrid.getWidth(); i++) {
            for (int j = 0; j < fireGrid.getHeight(); j++) {
                if (currentIntensities[i][j] > fireGrid.getIntensityThreshold()) {
                    // Propager le feu à partir des cellules actives
                    spreadToNeighbors(i, j, newGrid, currentIntensities[i][j]);
                }
            }
        }

        // Créer un nouveau feu si aucun feu actif n'est présent
        if (!hasActiveFire) {
            createNewFire();
        }

        // Mettre à jour la grille d'intensité du feu
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
                    if (newGrid[nx][ny] < fireGrid.getIntensityThreshold() && random.nextDouble() < fireGrid.getSpreadProbability()) {

                        // Intensité de propagation du feu (60% à 90% de l'intensité source)
                        double spreadIntensity = sourceIntensity * (0.6 + random.nextDouble() * 0.3);
                        // Limiter l'intensité de propagation du feu
                        spreadIntensity = Math.max(fireGrid.getIntensityThreshold() + 10, Math.min(fireGrid.getMaxIntensity(), spreadIntensity));
                        newGrid[nx][ny] = spreadIntensity;
                    }
                }
            }
        }
    }

    // Vérifier si la cellule est proche de quartier général
    private boolean isNearHQ(int x, int y) {
        int hqX = HeadQuarters.HQ_X;
        int hqY = HeadQuarters.HQ_Y;
        int safeDistance = 5;
        double distance = Math.sqrt(Math.pow(x - hqX, 2) + Math.pow(y - hqY, 2));
        return distance < safeDistance;
    }

    public FireGrid getFireGrid() {
        return this.fireGrid;
    }

    // Récupérer la carte d'intensité du feu
    public double[][] getIntensityMap() {
        return fireGrid.getIntensityGrid();
    }
}