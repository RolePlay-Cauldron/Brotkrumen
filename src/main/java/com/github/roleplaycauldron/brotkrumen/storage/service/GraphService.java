package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;

import java.util.Optional;
import java.util.Set;

public interface GraphService {

    Optional<Graph> loadGraphById(int graphId);

    Optional<Graph> loadGraphByName(String name);

    Set<Graph> getAllGraphs();

    void saveGraph(Graph graph);

    void deleteGraph(int graphId);
}
