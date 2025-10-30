package brotkrumen.graph;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class EdgeTest {

    @Test
    void testEdgeCreation() {
        Edge edgeOne = new Edge(1, 1, 2, 4.0);
        Edge edgeTwo = new Edge(2, 1, 2, 4.0, EnumSet.of(EdgeFlag.BLOCKED));

        assertEquals(1, edgeOne.getFrom());
        assertEquals(2, edgeOne.getTo());
        assertEquals(4.0, edgeOne.getCost());
        assertEquals(EnumSet.of(EdgeFlag.BLOCKED), edgeTwo.getFlags());
    }

    @Test
    void testEdgeEdit() {
        Edge edgeOne = new Edge(2, 1, 2, 4.0);

        edgeOne.setId(1);
        edgeOne.setFrom(3);
        edgeOne.setTo(4);
        edgeOne.setCost(5.0);
        edgeOne.setFlags(EnumSet.of(EdgeFlag.BLOCKED));

        assertEquals(1, edgeOne.getId());
        assertEquals(3, edgeOne.getFrom());
        assertEquals(4, edgeOne.getTo());
        assertEquals(5.0, edgeOne.getCost());
        assertEquals(EnumSet.of(EdgeFlag.BLOCKED), edgeOne.getFlags());
    }

    @Test
    void testEdgeToString() {
        Edge edge = new Edge(1, 2, 3, 4.0, EnumSet.of(EdgeFlag.BLOCKED));
        assertEquals("1 2->3 (4,00) [BLOCKED]", edge.toString());
    }

    @Test
    void testEdgeAttributeNotNull() {
        Edge edge = new Edge(1, 2, 3, 4.0, null);
        assertNotNull(edge.getFlags());

        edge.setFlags(null);
        assertNotNull(edge.getFlags());

        edge.setFlags(EnumSet.noneOf(EdgeFlag.class));
        assertNotNull(edge.getFlags());
    }

    @Test
    void testEdgeHasFlag() {
        Edge edge = new Edge(1, 2, 3, 4.0, EnumSet.of(EdgeFlag.BLOCKED));
        assertTrue(edge.hasFlag(EdgeFlag.BLOCKED));
    }
}
