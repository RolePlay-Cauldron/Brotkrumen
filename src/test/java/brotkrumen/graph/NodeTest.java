package brotkrumen.graph;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NodeTest {

    @Test
    void nodeSetTest() {
        Node node = new Node(2, 2, 3, 4);

        node.setId(1);
        node.setX(10);
        node.setY(-20);
        node.setZ(100);

        assertEquals(1, node.getId());
        assertEquals(10, node.getX());
        assertEquals(-20, node.getY());
        assertEquals(100, node.getZ());
    }

    @Test
    void nodeLocationTest() {
        Location loc = mock(Location.class);
        when(loc.getBlockX()).thenReturn(1);
        when(loc.getBlockY()).thenReturn(2);
        when(loc.getBlockZ()).thenReturn(3);

        Node nodeOne = new Node(1, loc);
        Node nodeTwo = new Node(2, 2, 3, 4);

        assertEquals(1, nodeOne.getId());
        assertEquals(1, nodeOne.getX());
        assertEquals(2, nodeOne.getY());
        assertEquals(3, nodeOne.getZ());

        assertEquals(2, nodeTwo.getId());
        assertEquals(2, nodeTwo.getX());
        assertEquals(3, nodeTwo.getY());
        assertEquals(4, nodeTwo.getZ());
    }
}
