import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
 
class JavaTask2 {
    private int port;
    private ServerSocket ss; 
    private Thread ServerThread; 
	BlockingQueue<SocketConnect> q = new LinkedBlockingQueue<SocketConnect>();
	
    public static void main(String[] args) throws IOException {
        new JavaTask2(1234).run();
    }	
 
    public JavaTask2(int port) throws IOException {
        ss = new ServerSocket(port);
        this.port = port;
    }
 
    void run() {
        ServerThread = Thread.currentThread();
        while (true) { 
            Socket s = getNewConn();
            if (ServerThread.isInterrupted()) {
                break;
            } else {
				if (s != null){
					try {
						final SocketConnect processor = new SocketConnect(s);
						final Thread thread = new Thread(processor);
						thread.setDaemon(true);
						thread.start();
						q.offer(processor);
					} 
					catch (IOException ignored) {
					}
				}
			}
        }
    }
 
    private Socket getNewConn() {
        Socket s = null;
        try {
            s = ss.accept();
        } catch (IOException ignored) {
        }
        return s;
    }
 
    private class SocketConnect implements Runnable{
        Socket s;
        BufferedReader br;
        BufferedWriter bw;
 
        SocketConnect(Socket socketParam) throws IOException {
            s = socketParam;
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
        }
 
        public void run() {
            while (!s.isClosed()) {
                String line = null;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    close();
                }
 
                if (line == null) {
                    close();
                } else {
                    for (SocketConnect sp:q) {
                        sp.send(line);
                    }
                }
            }
        }
 
        public synchronized void send(String line) {
            try {
                bw.write(line);
                bw.flush();
            } catch (IOException e) {
                close();
            }
        }
 
        public synchronized void close() {
            q.remove(this); 
            if (!s.isClosed()) {
                try {
                    s.close();
                } catch (IOException ignored) {}
            }
        }
    }
}