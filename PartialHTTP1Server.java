import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class PartialHTTP1Server {
  
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
            catch (Exception e) { // if thread pool fails
              
              try{//if connection established but still have some error
                  // PrintWriter for print error to client
                  //more than 50 connections then close server
              PrintWriter printWriter = new PrintWriter(client.getOutputStream(), true);
              printWriter.println("503 Service Unavailable");
              printWriter.close();
              server.close();
              }
              catch(Exception e)
              {//if connection is not established
              //then can't printWriter to client
                System.out.println("Error handling client");
              }
              
            }
          }
        //while loop end
        } catch (Expection e) {
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
}

