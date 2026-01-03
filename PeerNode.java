import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerNode extends JFrame {
    private DefaultListModel<String> peerListModel = new DefaultListModel<>();
    private DefaultListModel<String> remoteFilesModel = new DefaultListModel<>();
    private ConcurrentHashMap<String, String> fileRegistry = new ConcurrentHashMap<>();

    public PeerNode(int myPort) {
        setTitle("P2P Student System | Port: " + myPort);
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 3, 10, 10)); // 3 columns

        // 1. LOCAL FILES
        JPanel p1 = new JPanel(new BorderLayout());
        p1.setBorder(BorderFactory.createTitledBorder("My Files"));
        DefaultListModel<String> localModel = new DefaultListModel<>();
        p1.add(new JScrollPane(new JList<>(localModel)), BorderLayout.CENTER);
        JButton upBtn = new JButton("Share File");
        p1.add(upBtn, BorderLayout.SOUTH);

        // 2. DISCOVERED PEERS
        JPanel p2 = new JPanel(new BorderLayout());
        p2.setBorder(BorderFactory.createTitledBorder("Active Peers"));
        JList<String> peerList = new JList<>(peerListModel);
        p2.add(new JScrollPane(peerList), BorderLayout.CENTER);
        JButton refreshFilesBtn = new JButton("View Peer's Files");
        p2.add(refreshFilesBtn, BorderLayout.SOUTH);

        // 3. REMOTE FILES
        JPanel p3 = new JPanel(new BorderLayout());
        p3.setBorder(BorderFactory.createTitledBorder("Available to Download"));
        JList<String> remoteList = new JList<>(remoteFilesModel);
        p3.add(new JScrollPane(remoteList), BorderLayout.CENTER);
        JButton downBtn = new JButton("Download Selected");
        p3.add(downBtn, BorderLayout.SOUTH);

        add(p1); add(p2); add(p3);

        // LOGIC: View Peer's Files
        refreshFilesBtn.addActionListener(e -> {
            String peer = peerList.getSelectedValue();
            if (peer == null) return;
            fetchRemoteFiles(peer);
        });

        // LOGIC: Download
        downBtn.addActionListener(e -> {
            String peer = peerList.getSelectedValue();
            String file = remoteList.getSelectedValue();
            if (peer != null && file != null) {
                new Thread(() -> downloadFile(peer, file)).start();
            }
        });

        // LOGIC: Share
        upBtn.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                fileRegistry.put(jfc.getSelectedFile().getName(), jfc.getSelectedFile().getAbsolutePath());
                localModel.addElement(jfc.getSelectedFile().getName());
            }
        });

        // Start Services
        new Thread(new FileServer(myPort, fileRegistry)).start();
        DiscoveryService.startDiscovery(myPort, (addr) -> {
            if (!peerListModel.contains(addr) && !addr.contains(String.valueOf(myPort))) {
                SwingUtilities.invokeLater(() -> peerListModel.addElement(addr));
            }
        });
    }

    private void fetchRemoteFiles(String peerAddr) {
        try (Socket s = new Socket(peerAddr.split(":")[0], Integer.parseInt(peerAddr.split(":")[1]));
             DataOutputStream dos = new DataOutputStream(s.getOutputStream());
             DataInputStream dis = new DataInputStream(s.getInputStream())) {
            
            dos.writeUTF("GET_FILE_LIST");
            String[] files = dis.readUTF().split(";");
            SwingUtilities.invokeLater(() -> {
                remoteFilesModel.clear();
                for (String f : files) if (!f.isEmpty()) remoteFilesModel.addElement(f);
            });
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Peer Offline"); }
    }

    private void downloadFile(String peerAddr, String fileName) {
        try (Socket s = new Socket(peerAddr.split(":")[0], Integer.parseInt(peerAddr.split(":")[1]));
             DataOutputStream dos = new DataOutputStream(s.getOutputStream());
             DataInputStream dis = new DataInputStream(s.getInputStream())) {

            dos.writeUTF("DOWNLOAD:" + fileName);
            long size = dis.readLong();
            if (size > 0) {
                try (FileOutputStream fos = new FileOutputStream("dl_" + fileName)) {
                    byte[] b = new byte[4096];
                    int r; long total = 0;
                    while (total < size && (r = dis.read(b)) != -1) {
                        fos.write(b, 0, r);
                        total += r;
                    }
                }
                JOptionPane.showMessageDialog(this, "Success! Saved as dl_" + fileName);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        String p = JOptionPane.showInputDialog("Enter Port:");
        if (p != null) new PeerNode(Integer.parseInt(p)).setVisible(true);
    }
}