package br.edu.unasp.comandadigital.controller;

import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class SystemControllerTest {
    @Test void readyChecksConnectionAndClosesIt() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        var response = new SystemController(source).ready();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().status()).isEqualTo("READY");
        verify(connection).close();
    }
    @Test void invalidConnectionIsUnavailable() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(source.getConnection()).thenReturn(connection);
        assertThat(new SystemController(source).ready().getStatusCode().value()).isEqualTo(503);
        verify(connection).close();
    }
    @Test void connectionFailureDoesNotExposeException() throws Exception {
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("private JDBC details"));
        var response = new SystemController(source).ready();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().status()).isEqualTo("UNAVAILABLE");
        assertThat(response.getBody().toString()).doesNotContain("private JDBC details");
    }
}
