package uk.ac.ucl.rits.inform.datasources.waveform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetConnectionSupport;
import org.springframework.integration.ip.tcp.connection.TcpNetConnection;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.TcpCodecs;
import org.springframework.messaging.Message;
import uk.ac.ucl.rits.inform.datasources.waveform.hl7parse.Hl7ParseException;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Listen on a TCP port for incoming HL7 messages.
 */
@Configuration
public class Hl7ListenerConfig {
    private final Logger logger = LoggerFactory.getLogger(Hl7ListenerConfig.class);

    private final Hl7ParseAndSend hl7ParseAndSend;

    public Hl7ListenerConfig(Hl7ParseAndSend hl7ParseAndSend) {
        this.hl7ParseAndSend = hl7ParseAndSend;
    }

    /**
     * Specify the server config.
     * @param listenPort port to listen on (inside container)
     * @param sourceAddressAllowList list of source addresses that are allowed to connect to us
     * @return connection factory
     */
    @Bean
    public TcpNetServerConnectionFactory serverConnectionFactory(
            @Value("${waveform.hl7.listen_port}") int listenPort,
            @Value("${waveform.hl7.source_address_allow_list}") List<String> sourceAddressAllowList) {
        TcpNetServerConnectionFactory connFactory = new TcpNetServerConnectionFactory(listenPort);
        connFactory.setSoSendBufferSize(10 * 1024 * 1024);
        connFactory.setSoReceiveBufferSize(10 * 1024 * 1024);
        connFactory.setSoTimeout(10_000);
        connFactory.setSoTcpNoDelay(false);
        connFactory.setSoKeepAlive(true);
        connFactory.setDeserializer(TcpCodecs.crlf(5_000_000));
        connFactory.setTcpNetConnectionSupport(new DefaultTcpNetConnectionSupport() {
            @Override
            public TcpNetConnection createNewConnection(
                    Socket socket,
                    boolean server,
                    boolean lookupHost,
                    ApplicationEventPublisher applicationEventPublisher,
                    String connectionFactoryName) {
                TcpNetConnection conn = super.createNewConnection(socket, server, lookupHost, applicationEventPublisher, connectionFactoryName);
                String sourceAddress = conn.getHostAddress();
                if (sourceAddressAllowList.contains(sourceAddress)
                        || sourceAddressAllowList.contains("ALL")) {
                    logger.info("connection accepted from {}:{}", sourceAddress, conn.getPort());
                } else {
                    logger.warn("CONNECTION REFUSED from {}:{}, allowlist = {}", sourceAddress, conn.getPort(), sourceAddressAllowList);
                    conn.close();
                }
                return conn;
            }
        });
        return connFactory;
    }

    /**
     * Routes the TCP connection to the message handling.
     * @param connectionFactory connection factory
     * @return adapter
     */
    @Bean
    public TcpReceivingChannelAdapter inbound(TcpNetServerConnectionFactory connectionFactory) {
        TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
        adapter.setConnectionFactory(connectionFactory);
        adapter.setOutputChannelName("hl7Stream");
        return adapter;
    }

    /**
     * Message handler. Source IP check has passed if we get here. No reply is expected.
     * @param msg the incoming message
     * @throws Hl7ParseException if HL7 is invalid or in a form that the ad hoc parser can't handle
     * @throws WaveformCollator.CollationException if the data has a logical error that prevents collation
     */
    @ServiceActivator(inputChannel = "hl7Stream")
    public void handler(Message<byte[]> msg) throws Hl7ParseException, WaveformCollator.CollationException {
        byte[] asBytes = msg.getPayload();
        String asStr = new String(asBytes, StandardCharsets.UTF_8);
        // parse message from HL7 to interchange message, send to internal queue
        hl7ParseAndSend.parseAndQueue(asStr);
    }
}
