# IA_Sys_miniProjet

### Fonctionnalités

- **Interface Graphique** : Affiche la propagation du feu sur une grille et fournit des informations sur la simulation.
- **Logique de Propagation du Feu** : Simule la propagation du feu avec des intensités et des probabilités variables.
- **Protection du Quartier Général** : Assure que le feu ne commence pas près du quartier général.
- **Robots Extincteurs** : Des robots se déplacent sur la grille pour éteindre les feux.

### Composants

1. **SimulationGUI.java** : Gère l'interface graphique pour la simulation, y compris le panneau d'affichage principal et un panneau d'information.
2. **Simulation.java** : Contrôle la boucle principale de la simulation, mettant à jour la propagation du feu et l'interface graphique à intervalles réguliers.
3. **FireGrid.java** : Représente la grille où le feu se propage, gérant l'intensité du feu dans chaque cellule.
4. **Fire.java** : Gère la logique pour initialiser et propager le feu à travers la grille.
5. **Robot.java** : Gère les robots qui se déplacent et éteignent les feux.

### Comment Exécuter

1. Compiler les fichiers Java :
    ```sh
    javac -d class src/*.java
    ```

2. Exécuter la simulation :
    ```sh
    java -cp class Simulation
    ```

### Détails de la Simulation

- La taille de la grille est de 30x30 cellules.
- Le quartier général est situé au centre de la grille (15, 15).
- Le feu commence à des emplacements aléatoires et se propage en fonction d'un facteur de probabilité.
- L'intensité du feu est visualisée avec différentes couleurs.
- Les robots se déplacent pour éteindre les feux et retournent à l'état de recherche une fois le feu éteint.
