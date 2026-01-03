import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileServer implements Runnable {
    private int port;
    private ConcurrentHashMap<String, String> fileRegistry;

    public FileServer(int port, ConcurrentHashMap<String, String> registry) {
        this.port = port;
        this.fileRegistry = registry;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleRequest(socket)).start(); // Multithreaded handling
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleRequest(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            String request = dis.readUTF();

            if (request.equals("GET_FILE_LIST")) {
                // Return all keys (filenames) from our in-memory map
                String list = String.join(";", fileRegistry.keySet());
                dos.writeUTF(list);
            } else if (request.startsWith("DOWNLOAD:")) {
                String fileName = request.replace("DOWNLOAD:", "");
                String filePath = fileRegistry.get(fileName);
                if (filePath != null) {
                    File file = new File(filePath);
                    dos.writeLong(file.length());
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = fis.read(buffer)) != -1) dos.write(buffer, 0, read);
                    }
                } else {
                    dos.writeLong(-1); // File not found
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}