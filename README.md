# Brotkrumen

A waypoint and pathfinding plugin for Paper servers, built on mathematical graph theory.
It lets you define networks of interconnected nodes across your world, then guide players through them on the 
shortest path.

The name is German for "breadcrumb", which is roughly what the guided path visualization looks like in practice.

> Check out the Documentation on [our website](https://roleplay-cauldron.github.io/brotkrumen/)

---

## What it does

Brotkrumen gives server administrators a tool to build navigable waypoint graphs. A graph is a collection of nodes
(locations) connected by edges. Edges can be plain walking connections or teleport links, either within a graph or
between different graphs. Players can then be guided along the shortest path between two points using the `/bk resolve`
command, which visualizes the route with particles or block displays and optionally handles teleports automatically.

This is mostly aimed at roleplay or survival servers that need a structured way to connect locations,
for example, town navigation, quest routing, or a guided tour system.

---

## Requirements

- Paper 1.21+
- Java 21
- (optional) MySQL / MariaDB for a shared database

---

## Pathfinding

Two algorithms are available and are selected automatically based on the traversal types involved:

- **A*** — faster, heuristic-based. Only used for pure walking routes without any teleports.
- **Dijkstra** — handles all traversal types including local teleports, inter-graph teleports, and warps. Always finds
  the lowest-cost path.

Edge costs are configurable. Warps count as virtual edges in the graph, so they participate in pathfinding normally.

---

## Visualization

The guided path is rendered in-world using either particles or block display entities, depending on the configured
preset. The visible window moves with the player, a configurable number of nodes ahead and optionally behind are shown
at any time. A goal marker appears at the destination. When a player completes a segment or reaches the goal, a
configurable notification plays (title, message, sound).

If a player strays too far from the path, they receive a warning. After a grace period the guidance cancels.

Visual presets are defined in `presets.yml` and can be changed per graph with `/bkeditor preset <name>`.

---

## Inter-graph travel

Graphs can be linked with inter-graph edges, allowing pathfinding to cross graph boundaries transparently. These links
are managed in the editor and can be enabled or disabled independently. Multiple linked graphs form a network, and the
resolver treats the whole network as one unified graph when calculating routes.

---

## Database

By default, Brotkrumen stores everything in a local SQLite file. For servers that need shared storage across instances,
MySQL and MariaDB are supported via the `config.yml`. The schema is managed with migrations, and table names can be
prefixed to avoid conflicts.

---

## Language support

The plugin ships with multiple message translations. See
[here](https://github.com/Roleplay-Cauldron/Brotkrumen/tree/master/src/main/resources/language)
for a list of available languages. The default locale is set in `config.yml`.

---

## License

[GNU GPLv3](LICENSE)
