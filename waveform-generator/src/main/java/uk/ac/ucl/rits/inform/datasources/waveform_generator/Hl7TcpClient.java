package uk.ac.ucl.rits.inform.datasources.waveform_generator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Hl7TcpClient implements AutoCloseable {
    private final Socket socket;

    /**
     * Connect over tcp.
     *
     * @param tcpHost connect to host
     * @param tcpPort connect to port
     * @throws IOException on connect error
     */
    public Hl7TcpClient(String tcpHost, int tcpPort) throws IOException {
        this.socket = new Socket(tcpHost, tcpPort);
    }

    /**
     * Send a message with CRLF.
     *
     * @param msg the byte array of the message to send, without the terminating CRLF
     * @throws IOException tcp error
     */
    public void sendMessage(byte[] msg) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(msg);
        os.write("\r\n".getBytes());
    }

    /**
     * Close connection.
     * @throws IOException tcp error
     */
    public void close() throws IOException {
        socket.getOutputStream().close();
        socket.close();
    }
}
