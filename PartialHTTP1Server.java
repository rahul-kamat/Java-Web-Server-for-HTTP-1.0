import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.nio.file.*;

public class PartialHTTP1Server implements Runnable {

  public Socket clientSocket;
  public byte[] fileBytes;

  public PartialHTTP1Server(Socket client) {
    clientSocket = client;
  }

  public static void main(String[] args) throws IOException {
      // check if there are sufficient arguments
      if (args.length!=1) {
        System.out.println("Error :: Invalid Input!");
      }

      // check if the arg[0] is an integer
      int portNumber = parseInputInteger(args[0]);
      if (portNumber == -1) {
        // port number is not an integer
        System.out.println("Error :: Port number not found!");
      }

      // create server socket

      try { // for initial server
        ServerSocket server= new ServerSocket(portNumber);
        System.out.println("Connected to initial server!");

        int connectionsEstablished = 0; //used for counting connections

        //setting up thread pool with all data
        ExecutorService threadPoolExecutor = new ThreadPoolExecutor(5, 50, 10L, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());

        // while loop goes here to handle up to 50 connections
        while (true) {
          Socket client= server.accept();
          try { // for thread pool connections
            // new thread in java
            threadPoolExecutor.execute(new PartialHTTP1Server(client));
          }
          catch (Exception threadPoolFail) { // if thread pool fails
            try{
              //if connection is established but error exists
              //Close the server for more than 50 connections
              PrintWriter printWriter = new PrintWriter(client.getOutputStream(), true); // PrintWriter for print error to client
              printWriter.println("503 Service Unavailable");
              printWriter.close();
              server.close();
            } catch(Exception connectionFail) { //if connection is not established,then printWriter can't print to client
              System.out.println("Error handling client");
            }
          }
        }
      } catch (Exception initialServerFail) {
        System.out.println("Error :: Initial Server Failed!");
      }
  }

  // returns input string parsed as an Integer and -1 if error parsing input
  private static int parseInputInteger(String input) {
      try {
        return Integer.parseInt(input);
      } catch (Exception e) {
        return -1;
      }
  }

  // get Content-Type header field value by MIME type
  private String getContentType(String fileName) {
    int indexOfPeriod = fileName.lastIndexOf(".");
    if (indexOfPeriod == -1) {
      return "application/octet-stream";
    }

    String fileExtension = fileName.substring(indexOfPeriod+1).toLowerCase();

    if (fileExtension.equals("html") || fileExtension.equals("plain")) {
      return "text/" + fileExtension;
    } else if (fileExtension.equals("gif") || fileExtension.equals("jpeg") || fileExtension.equals("png")) {
      return "image/" + fileExtension;
    } else if (fileExtension.equals("octet-stream") || fileExtension.equals("pdf") || fileExtension.equals("x-gzip") || fileExtension.equals("zip")) {
      return "application/" + fileExtension;
    }

    return "application/octet-stream";
  }

  public String parsingCommands(String command, String modified) {
    if (command == null || command.length() == 0) {
      return "HTTP/1.0 400 Bad Request";
    }

    // there are 3 parts to the input command
    String[] array=command.split("\\s+");

    if (array.length != 3) { // input is not in the correct format
      return "HTTP/1.0 400 Bad Request";
    }

    String httpMethod = array[0];
    String filePath = array[1];
    String httpVersion = array[2];

    // input command is not correct
    if (!httpMethod.equals(httpMethod.toUpperCase()) || filePath.charAt(0)!='/') {
      return "HTTP/1.0 400 Bad Request";
    } else if (!httpVersion.equals("HTTP/1.0")) { // only handle HTTP/1.0 request command
      return "HTTP/1.0 505 HTTP Version Not Supported";
    }

    // valid HTTP/1.0 request command that is not supported
    if (httpMethod.equals("PUT") || httpMethod.equals("LINK") || httpMethod.equals("UNLINK") || httpMethod.equals("DELETE") ) {
      return "HTTP/1.0 501 Not Implemented";
    }
    // invalid HTTP/1.0 request command
    else if (!httpMethod.equals("GET") && !httpMethod.equals("POST") && !httpMethod.equals("HEAD")) {
      return "HTTP/1.0 400 Bad Request";
    }

    // if file not found, return 404
    File file = new File("."+filePath);
    if (!file.exists()) {
      return "HTTP/1.0 404 Not Found";
    }

    // used to format and parse dates in a locale-sensitive manner
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    // if file has not been modified since "If-Modified-Since" input date, return "HTTP/1.0 304 Not Modified"
    String ifModifiedSince = "If-Modified-Since:";
    if (modified.contains(ifModifiedSince) && httpMethod.equals("GET")) {
      String ifModifiedSinceDate = modified.substring(modified.indexOf(' ') + 1);
      String fileDate = simpleDateFormat.format(file.lastModified());

      try {
        Date parseIfModifiedSinceDate = simpleDateFormat.parse(ifModifiedSinceDate);
        Date parseFileDate = simpleDateFormat.parse(fileDate);

        if (parseIfModifiedSinceDate.compareTo(parseFileDate) > 0) { // "If-Modified-Since" date is after file date
          Calendar tomorrow =Calendar.getInstance();
          tomorrow.add(Calendar.HOUR,24);
          return "HTTP/1.0 304 Not Modified" + '\r' + '\n' + "Expires: " + simpleDateFormat.format(tomorrow.getTime()) + '\r' + '\n';
        }
      } catch (Exception parseModifiedDateException) {
        System.out.println("Error :: Cannot process date!");
      }
    }


   Path p = Paths.get("."+filePath);
   try {
     fileBytes = Files.readAllBytes(p);
     String output = header(file,filePath);
     return output;
   }
   // support additional HTTP 1.0 status codes
   catch (AccessDeniedException accessDenied) {
     return "HTTP/1.0 403 Forbidden";
   }
   catch(IOException io) {
     return "HTTP/1.0 500 Internal Server Error";
   }



    // success!
  }

  // build response header
  public String header(File file,String fName) {
    String message = "HTTP/1.0 200 OK" + '\r' + '\n';
    String extention = "";

    // file has valid extention
    if (fName.indexOf('.') != -1) {
      // get extention
      extention=fName.substring(fName.indexOf('.')+1);
    }

    // formatting for date -> text and parsing for text -> date conversions
    SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
    date.setTimeZone(TimeZone.getTimeZone("GMT"));

    // Expires header field
    Calendar tomorrow = Calendar.getInstance();
    tomorrow.add(Calendar.HOUR,24);

    message += "Content-Type: " + getContentType(fName) + '\r' + '\n';
    message += "Content-Length: " + file.length() + '\r' + '\n';
    message += "Last-Modified: " + date.format(file.lastModified()) + '\r' + '\n';
    message += "Content-Encoding: identity" + '\r' + '\n';
    message += "Allow: GET, POST, HEAD" + '\r' + '\n';
    message += "Expires: "+date.format(tomorrow.getTime()) + '\r' + '\n' + '\r' + '\n';

    return message;
  }


  // create method `run` because Runnable is used in thread pool
  public void run() {
    // read commands from client
    // use try-catch , see knock knock
    try{
      // use BufferedReader as shown in knock knock
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      // input line 1 is command and line 2 is modified
      String command = "";
      String modified = "";

      // used to send data to client
      DataOutputStream ClientOutput = new DataOutputStream(clientSocket.getOutputStream());
      int line = 0;

      try {
        clientSocket.setSoTimeout(5000);
        while (line < 2) {
          String readLine = in.readLine();
          if (line == 0) {
            command = readLine;
          } else {
            modified = readLine;
          }
          line++;
        }

        System.out.println("Command: " + command);
        System.out.println("Modified: " + modified);

        String parsedClintInput = parsingCommands(command, modified);
        System.out.println("Parsed Client Input: " + parsedClintInput);

        //convert header response to byte
        byte[] response=parsedClintInput.getBytes();
        //send byte response to client
        ClientOutput.write(response);
        //flush and clean output stream
        ClientOutput.flush();

       //if GET or POST requests are successful, then send file info
       if (parsedClintInput.contains("200 OK")) {
         if (command.contains("GET") || command.contains("POST")) {
           ClientOutput.write(fileBytes);
         }
       }
    } catch(SocketTimeoutException timeout) {
        System.out.println("HTTP/1.0 408 Request Timeout");
        byte[] socketTimer="HTTP/1.0 408 Request Timeout".getBytes();
        ClientOutput.write(socketTimer);
    }

    // close input and output streams
    ClientOutput.close();
    in.close();

    // close server as there are no more requests
    clientSocket.close();
    } catch(Exception e) { // exception in BufferReader
      System.out.println("Error :: handling client input");
    }
  }
}
