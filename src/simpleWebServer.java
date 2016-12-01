import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class simpleWebServer {

	public static void main(String[] args) throws IOException {
		new simpleWebServer();
	}
	
	private String getHeader(String status, String type){
		String header = "HTTP/1.1 " + status + "\n";
    	header += "Content-Type: " + type + "\n";
    	header += "\n";//empty line separates http header from message
    	return header;
	}
	
	public simpleWebServer() throws IOException{
		ServerSocket listenerSocket = new ServerSocket(8000);
		System.out.println("listening on port " + listenerSocket.getLocalPort());
		while (true) {
    		Socket connectionSocket = null;
			try {
				connectionSocket = listenerSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				
				//read the entire request into a single string
				String line;
	    		String request = "";
	    		try{
	    			while (!(line = inFromClient.readLine()).equals("")){
						request += line + "\n";//readline removes newlines so reinclude them here
					}
		    		System.out.println(request);
	    		}
	    		catch (NullPointerException e){
	    			e.printStackTrace();
	    			continue;
	    		}
	    		
	    		//search query string for particular page requested
	    		String pattern = "GET /([^ ]+) |POST /([^ ]+) ";//ASSUMTION:query string is in relative format
	    	    Pattern r = Pattern.compile(pattern);
	    	    Matcher m = r.matcher(request);
	    	    String requestedResource = "";
	    	    if (m.find()){
	    	    	if (m.group(1) != null){//java counts both groups as if they had captured something so can't just have m.group(1)
	    	    		requestedResource = m.group(1);
	    	    	}
	    	    	else if (m.group(2) != null){
	    	    		requestedResource = m.group(2);
	    	    	}
	    	    }
		    	
	    	    OutputStream outContent = connectionSocket.getOutputStream();
	    	    requestedResource.replaceAll("[^a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=]","");//whitelist characters to frustrate attacks
			    if (requestedResource.equals("")){
			    	File file = new File("index.html");
			    	Scanner scanner = new Scanner(file);
			    	String content = scanner.useDelimiter("\\Z").next();//\\Z delimiter is until end of input except terminator
			    	scanner.close();
			    	String header = getHeader("200 OK","text/html");
			    	outContent.write(header.getBytes());
			    	outContent.write(content.getBytes());
			    }
			    else{
			    	Path path = Paths.get(requestedResource);
			    	try {
			    		byte[] data = Files.readAllBytes(path);
			    		String header = "";
			    		switch (requestedResource.substring(requestedResource.lastIndexOf(".") + 1)){ 
			    			case ".ico": header = getHeader("200 OK", "image/x-icon"); break;
			    			case ".html": header = getHeader("200 OK", "text/html"); break;
			    			case ".jpg": header = getHeader("200 OK", "image/jpg"); break;
			    			case ".mp4": header = getHeader("200 OK", "video/mp4"); break;
			    		}
				    	outContent.write(header.getBytes());
				    	outContent.write(data);
			    	}
			    	catch(NoSuchFileException e){
			    		String header = getHeader("404 Not Found", "");
			    		outContent.write(header.getBytes());
			    	}
			    }
			    outContent.close();
			    connectionSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
