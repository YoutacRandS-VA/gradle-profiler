package org.gradle.profiler.client.protocol;

import org.gradle.profiler.client.protocol.messages.GradleInvocationParameters;
import org.gradle.profiler.client.protocol.messages.Message;
import org.gradle.profiler.client.protocol.messages.StudioAgentConnectionParameters;
import org.gradle.profiler.client.protocol.messages.StudioRequest;
import org.gradle.profiler.client.protocol.serialization.MessageProtocolHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * A singleton that runs inside a client process to communicate with the controller process.
 */
public enum Client {
    INSTANCE;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Object lock = new Object();
    private Connection connection;
    private MessageProtocolHandler protocolHandler;

    public void connect(int port) {
        synchronized (lock) {
            if (connection != null) {
                throw new IllegalStateException("This client is already connected.");
            }
            try {
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
                connection = new Connection(socket);
                protocolHandler = new MessageProtocolHandler("controller process", connection);
            } catch (IOException e) {
                throw new RuntimeException("Could not connect to controller process.", e);
            }
        }
    }

    public void send(Message message) {
        synchronized (lock) {
            protocolHandler.send(message);
        }
    }

    public GradleInvocationParameters receiveSyncParameters(Duration timeout) {
        synchronized (lock) {
            return protocolHandler.receive(GradleInvocationParameters.class, timeout);
        }
    }

    public StudioAgentConnectionParameters receiveConnectionParameters(Duration timeout) {
        synchronized (lock) {
            return protocolHandler.receive(StudioAgentConnectionParameters.class, timeout);
        }
    }

    public StudioRequest receiveStudioRequest(Duration timeout) {
        synchronized (lock) {
            return protocolHandler.receive(StudioRequest.class, timeout);
        }
    }

    public void listenAsync(Consumer<Client> runnable) {
        executor.execute(() -> runnable.accept(this));
    }

    public void disconnect() throws IOException {
        synchronized (lock) {
            try (MessageProtocolHandler protocolHandler = this.protocolHandler) {
                if (connection != null) {
                    connection.close();
                }
            } finally {
                connection = null;
                protocolHandler = null;
            }
        }
    }
}
