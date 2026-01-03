import java.net.*;
import java.util.function.Consumer;

public class DiscoveryService {
    private static final int DISCOVERY_PORT = 8888; 
    private static final String DISCOVERY_MESSAGE = "STUDENT_PEER_DISCOVERY:";

    public static void startDiscovery(int myFilePort, Consumer<String> onPeerFound) {
        // Thread 1: Broadcast my existence
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                byte[] buffer = (DISCOVERY_MESSAGE + myFilePort).getBytes();
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
                        InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
                    socket.send(packet);
                    Thread.sleep(5000);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // Thread 2: Listen for others
        new Thread(() -> {
            try {
                // FIXED: Create an unbound socket first
                DatagramSocket socket = new DatagramSocket(null); 
                // FIXED: Set Reuse Address before binding
                socket.setReuseAddress(true); 
                socket.bind(new InetSocketAddress(DISCOVERY_PORT));

                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.startsWith(DISCOVERY_MESSAGE)) {
                        String peerPort = message.split(":")[1];
                        String peerAddress = packet.getAddress().getHostAddress() + ":" + peerPort;
                        onPeerFound.accept(peerAddress);
                    }
                }
            } catch (Exception e) { 
                System.err.println("Discovery Listener Error: " + e.getMessage());
            }
        }).start();
    }
}