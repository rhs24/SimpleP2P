import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;

public class FileClient implements Runnable {
    private String host;
    private int port;
    private String fileName;

    public FileClient(String host, int port, String fileName) {
        this.host = host;
        this.port = port;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF(fileName);
            long fileSize = dis.readLong();

            if (fileSize > 0) {
                try (FileOutputStream fos = new FileOutputStream("downloaded_" + fileName)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    long totalRead = 0;
                    while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                    }
                }
                JOptionPane.showMessageDialog(null, "Download Complete: downloaded_" + fileName);
            } else {
                JOptionPane.showMessageDialog(null, "File not found on peer.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Connection Failed: " + e.getMessage());
        }
    }
}