package com.vaadin.azure.starter.sessiontracker.backend;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RedisConnectorTest {
    String clusterKey;
    RedisConnectionFactory factory;
    RedisConnection connection;
    RedisConnector connector;

    @BeforeEach
    void setUp() {
        clusterKey = UUID.randomUUID().toString();

        factory = mock(RedisConnectionFactory.class);
        connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);

        connector = new RedisConnector(factory);
    }

    @Test
    void sendSession_sessionIsAdded() {
        SessionInfo sessionInfo = mock(SessionInfo.class);
        when(sessionInfo.getClusterKey()).thenReturn(clusterKey);
        byte[] data = new byte[] {'f','o','o'};
        when(sessionInfo.getData()).thenReturn(data);

        connector.sendSession(sessionInfo);

        verify(connection).set(aryEq(BackendUtil.b("session-" + clusterKey)),
                aryEq(data));
    }

    @Test
    void getSession_sessionIsRetrieved() {
        when(connection.exists(any(byte[].class))).thenReturn(false);

        connector.getSession(clusterKey);

        verify(connection).get(aryEq(BackendUtil.b("session-" + clusterKey)));
    }

    @Test
    void getSession_serializationInProgress_waitsForSerialization() {
        when(connection.exists(any(byte[].class))).thenReturn(true).thenReturn(false);

        connector.getSession(clusterKey);

        verify(connection, times(2)).exists(any(byte[].class));
    }

    @Test
    void markSerializationStarted_sessionLocked() {
        connector.markSerializationStarted(clusterKey);

        verify(connection).set(eq(BackendUtil.b("pending-" + clusterKey)),
                any());
    }

    @Test
    void markSerializationComplete_sessionNotLocked() {
        connector.markSerializationComplete(clusterKey);

        verify(connection).del(eq(BackendUtil.b("pending-" + clusterKey)));
    }
}
