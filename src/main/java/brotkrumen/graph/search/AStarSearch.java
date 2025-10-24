package brotkrumen.graph.search;

import brotkrumen.graph.Graph;
import brotkrumen.graph.Node;

public class AStarSearch extends AbstractSearch {

    @Override
    protected double heuristic(Graph g, int a, int b) {
        Node wa = g.getNodeById(a), wb = g.getNodeById(b);
        double dx = wa.getX() - wb.getX(), dy = wa.getY() - wb.getY(), dz = wa.getZ() - wb.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
