package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GraphTableTest {

    @Test
    @SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.CloseResource"})
    void updateFailureRollsBackAndRemainsVisibleToTheCaller() throws SQLException {
        final BrotkrumenConnectionProvider provider = mock(BrotkrumenConnectionProvider.class);
        final Connection connection = mock(Connection.class);
        final SQLException writeFailure = new SQLException("write failed");
        final GraphTable table = new GraphTable("graphs", mock(EdgeTable.class), mock(NodeTable.class));
        when(provider.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(writeFailure);

        final StorageException failure = assertThrows(StorageException.class,
                () -> table.saveGraph(provider, new Graph(1, "Broken")),
                "A failed graph update must not return successfully after rollback");

        assertSame(writeFailure, failure.getCause(), "The original write failure should remain observable");
        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
    }
}
