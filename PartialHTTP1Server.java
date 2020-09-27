import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.nio.file.*;

public class PartialHTTP1Server implements Runnable {

  public Socket clientSocket;
  public byte[] fileBytes;

  public PartialHTTP1Server(Socket client)
  {
    clientSocket=client;
  }

    public static void main(String[] args) throws IOException {
        System.out.println(args[0]);

        //Bhavya check if there are sufficient arguments
        if(args.length!=1)
        {
          System.out.println("Error :: Invalid Input!");
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




    public String parsingCommands(String command, String modified)
    {
      System.out.println("Command lenght is"+command.length());
      System.out.println();
      if (command == null || command.length() == 0) {
        return "HTTP/1.0 400 Bad Request";
      }
      //need to have 3 parts for it
      String[] array=command.split("\\s+");
      System.out.println("arraylenght is"+array.length);
      System.out.println();

      if (array.length != 3) {
        return "HTTP/1.0 400 Bad Request";
      }
      String httpMethod = array[0];
      String filePath = array[1];
      String httpVersion = array[2];
      System.out.println("http is"+array[0]);
      System.out.println("file name is"+array[1]);


      if(!httpMethod.equals(httpMethod.toUpperCase()) || filePath.charAt(0)!='/')
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



      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
      simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

      //if file has not been modified since "If-Modified-Since" input date, return "HTTP/1.0 304 Not Modified"
      String ifModifiedSince = "If-Modified-Since:";
      if (modified.contains(ifModifiedSince) && httpMethod.equals("GET")) {
        String ifModifiedSinceDate = modified.substring(modified.indexOf(' ') + 1);
        String fileDate = simpleDateFormat.format(file.lastModified());
        try {
          Date parseIfModifiedSinceDate = simpleDateFormat.parse(ifModifiedSinceDate);
          Date parseFileDate = simpleDateFormat.parse(fileDate);
          if (parseIfModifiedSinceDate.compareTo(parseFileDate) > 0) { // "If-Modified-Since" date is after file date
            return "HTTP/1.0 304 Not Modified";
          }

        } catch (Exception parseModifiedDateException) {
          System.out.println("Error processing data");

        }
      }



      //check if we can read the file or not

      //if forbidden 403
     // try{


       Path p=Paths.get("."+filePath);
       try
       {
         fileBytes=Files.readAllBytes(p);
         String output= header(file,filePath);
         return output;
       }
       catch (AccessDeniedException accessDenied)
       {
         return "HTTP/1.0 403 Forbidden";
       }
       catch(IOException io)
       {

         return "HTTP/1.0 500 Internal Server Error";
       }



      // success
    }

    // HTTP/1.0 505 HTTP Version Not Supported



    public String header(File file,String fName)
    {
      String message="HTTP/1.0 200 OK"+'\r'+'\n';
      String extention= "";

      //file has valid extention
      if(fName.indexOf('.')!=-1)
      {
        //get extention
       extention=fName.substring(fName.indexOf('.')+1);

      }
      SimpleDateFormat date=new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
        date.setTimeZone(TimeZone.getTimeZone("GMT"));
        message += "Content-Type: "+ getContentType(fName)+'\r'+'\n'; // getContentType(fName)
        message += "Content-Length: "+ file.length()+'\r'+'\n';

        message+="Last-Modified: "+date.format(file.lastModified())+'\r'+'\n';
        message+="Content-Encoding: identity" + '\r' + '\n';
        message+="Allow: GET, POST, HEAD"+'\r'+'\n';

        //Date today=new Date();
        Calendar tomorrow =Calendar.getInstance();
        tomorrow.add(Calendar.HOUR,24);
        message+="Expires: "+date.format(tomorrow.getTime())+'\r'+'\n'+'\r'+'\n';
       // message+='\r\n';
      return message;
    }


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
        // global variable

        //to send data to client
        DataOutputStream ClientOutput = new DataOutputStream(clientSocket.getOutputStream());
        int line = 0;

        try
        {
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
        System.out.println("Parsed Client Input:" + parsedClintInput);

        //convert header response to byte
       byte[] response=parsedClintInput.getBytes();
       //send byte response to client
        ClientOutput.write(response);
        //flush and clean output stream
        ClientOutput.flush();

       //if GET or POST requests are successful, then we have to send file info

       if(parsedClintInput.contains("200 OK"))
       {
         if(command.contains("GET") || command.contains("POST"))
         {
           ClientOutput.write(fileBytes);
         }

       }


        }
        catch(SocketTimeoutException timeout)
        {
          byte[] socketTimer="HTTP/1.0 408 Request Timeout".getBytes();
          ClientOutput.write(socketTimer);
        }


       //closing input and output streams
         ClientOutput.close();
      in.close();
      //closing server as no more requests
      clientSocket.close();

      }
      catch(Exception e)
      {
        //exception in bufferReader
        System.out.println("Error: handling client input");

      }



    }



}
