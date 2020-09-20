import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class PartialHTTP1Server implements Runnable {
  
  public Socket clientSocket;
  public PartialHTTP1Server(Socket client)
  {
    clientSocket=client;
  }

    public static void main(String[] args) throws IOException {
        System.out.println(args[0]);
        
        //Bhavya check if there are sufficient arguments
        if(args.length!=1)
        {
          System.out.println("Error :: Invalid Inpu!"); 
        }
        
        //check if the arg[0] is int or not
        int portNumber=parseInputInteger(args[0]);
        if(portNumber==-1)
        {
          //error-port number not integer
          System.out.println("Error :: Port number not found!");
        }
       
        // create server socket

        // thread pool
        // ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        //ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(50);   

        try { // for initial server
          ServerSocket server= new ServerSocket(portNumber);
          System.out.println("Connected to initial server!");
          
          int connectionsEstablished= 0; //used for counting connections
         //setting up thread pool with all data
          ExecutorService threadPoolExecutor = new ThreadPoolExecutor(5, 50, 10L, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());
          
          // while loop goes here to handle up to 50 connections
          while(true)
          {
           Socket client= server.accept();
            try {// try thread pool connections
              
              
              //here new thread in java 
              threadPoolExecutor.execute(new PartialHTTP1Server(client));
            } 
            catch (Exception threadPoolFail) { // if thread pool fails
              
              try{//if connection established but still have some error
                  // PrintWriter for print error to client
                  //more than 50 connections then close server
              PrintWriter printWriter = new PrintWriter(client.getOutputStream(), true);
              printWriter.println("503 Service Unavailable");
              printWriter.close();
              server.close();
              }
              catch(Exception connectionFail)
              {//if connection is not established
              //then can't printWriter to client
                System.out.println("Error handling client");
              }
              
            }
          }
        //while loop end
        } catch (Exception initialServerFail) {
          System.out.println("Error :: Initial Server Failed");
      
            
        }
           
          
        
        /* if we did not get a connection request for more than 5 seconds 
        we can use 
        Set a timeout on blocking Socket operations:
            ServerSocket.accept();
            SocketInputStream.read();
              atagramSocket.receive();
              
        Sotimeout method. Socket.soTimeOut(int);*/
        
        
        
       
        
        
         
    }
    
    private static int parseInputInteger(String input) {
        try {
          return Integer.parseInt(input);
        } catch (Exception e) {
          return -1;
        }
    }
    
    
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
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public String parsingCommands(String command)
    {
      if (command == null || command.length() == 0) {
        return "HTTP/1.0 400 Bad Request";
      }
      //need to have 3 parts for it
      String[] array=command.split("\\s+");
      if (array.length != 3) {
        return "HTTP/1.0 400 Bad Request";
      }
      String httpMethod = array[0];
      String filePath = array[1];
      String httpVersion = array[2];
      if(httpMethod.equals(httpMethod.toUpperCase()) || filePath.charAt(0)!='/')
      {
        //display 
        return "HTTP/1.0 400 Bad Request";
      }
      else if(!httpVersion.equals("HTTP/1.0"))
      {
        return "HTTP/1.0 505 HTTP Version Not Supported";
      }
      
      //can only support GET,POST,HEAD
      //otherwise send 501 error
      if(httpMethod.equals("PUT") || httpMethod.equals("LINK") || httpMethod.equals("UNLINK") || httpMethod.equals("DELETE") )
      {
        return "HTTP/1.0 501 Not Implemented";
      }
      //for any other command, its bad/invalid request
      else if(!httpMethod.equals("GET") && !httpMethod.equals("POST") && !httpMethod.equals("HEAD"))
      {
        return "HTTP/1.0 400 Bad Request";
      }
      
      //check for filePath
      //if file not found 404
      File file=new File("."+filePath);
      if(!file.exists())
      {
        return "HTTP/1.0 404 Not Found";
      }
      
      //check if we can read the file or not
      
      //if forbidden 403
     // try{
        if(file.canRead())
      {
        // HTTP/1.0 200 OK
      }
      else 
      {
        return "HTTP/1.0 403 Forbidden";
      }
    /*  }
      catch(IOException fileNotOpen)
      {
        return "HTTP/1.0 500 Internal Server Error";
      }
      */
      return "";
      
      // success
    }
    
    // HTTP/1.0 505 HTTP Version Not Supported
    
    
    
    
    
    
    //because we used Runnable in thread pool, we have to create method run
    public void run()
    {
      // we have to read commands from client
      
      //use try catch , see knock knock
      try{
        //use bufferreader as shown in knock knock 
    
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String command = "";
        String modified = "";
        
        int line = 0;
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
        
        String parsedClintInput = parsingCommands(command);
        System.out.println("Parsed Client Input:" + parsedClintInput);
        
        
       
       // GET /index.html
       //string 0 - GET,POST,,, Capitalized
       
       
       //working test case
       
       //  GET /index.html HTTP/1.0
       
       //string 0 - GET capitalized
       //string 1- starts with a / and then file name
       //string 2 - its HTTP/1.0
       
       
      }
      catch(Exception e)
      {
        //exception in bufferReader
        
        
      }
      
      
    }
    
    
    
}

