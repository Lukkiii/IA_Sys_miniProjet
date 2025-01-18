// Information d'un point de feu
class FireSpot {
    int x, y;
    double intensity;
    long discoveryTime;

    FireSpot(int x, int y, double intensity, long discoveryTime) {
        this.x = x;
        this.y = y;
        this.intensity = intensity;
        this.discoveryTime = discoveryTime;
    }
}
