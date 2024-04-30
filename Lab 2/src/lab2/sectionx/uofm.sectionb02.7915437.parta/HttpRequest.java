package lab2.sectionx.parta;

import java.io.*;
import java.net.*;
import java.util.*;


final class HttpRequest implements Runnable {
    
    final static String CRLF = "\r\n";
    Socket socket = null;
    
    // Constructor
    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }
    
    // Implement the run() method of the Runnable interface
    public void run() {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    // Does the actual processing of the request
    private void processRequest() throws Exception {
        // Get a reference to the socket's input and output streams
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        
        // Set up input stream filters
        InputStreamReader ir = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(ir);
        
        // Get the request line of the HTTP request message
        String requestLine = br.readLine();
        
        // Display the request line
        System.out.println();
        System.out.println(requestLine);
        
        // Get and display the header lines
        String headerLine = null;
        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
        }
        
        // Close the streams and socket
        os.close();
        br.close();
        socket.close(); 
    }
}
