package brotkrumen.graph.search;

import brotkrumen.graph.Graph;

public class DijkstraSearch extends AbstractSearch {

    @Override protected double heuristic(Graph g, int a, int b) {
        return 0.0;
    }
}
