package lab2.sectionx.partb;

import java.net.*;

public final class WebServer {
    public static void main(String[] args) throws Exception {
        
        int port = 5555;
        ServerSocket serverSocket = new ServerSocket(port);
        Socket clientSocket = null;
        
        // Process HTTP service requests in an infinite loop.
        while (true) {
            // Listen for a TCP connection request to the port
            clientSocket = serverSocket.accept();
            
            // Construct an object to process the HTTP request message
            HttpRequest request = new HttpRequest(clientSocket);
            
            // Create a new thread to process the request
            Thread thread = new Thread(request);
            
            // Start the thread
            thread.start();
        }
    }
}
