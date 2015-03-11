import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class OPERATION {
    public static boolean flag;

    public OPERATION() {
        //
    }

    public static void Send(PrintWriter out, String message) throws IOException {
        out.println(message);
        out.flush();
    }

    public static String Receive(BufferedReader in) throws IOException {
        String message = in.readLine();
        return message;
    }

 
    public class heartbeat extends Thread {
        public boolean flag;
        public int portNumber;
        public String hostName;
        public String sourcename;

        public heartbeat(String hostName, String sourceuser, int portNumber) {
            flag = true;
            this.portNumber = portNumber;
            this.hostName = hostName;
            this.sourcename = sourceuser;

        }

        public void run() {
            try {
                while (flag) {
                    Socket HeartSocket = new Socket(hostName, portNumber);
                    PrintWriter out = new PrintWriter(
                        HeartSocket.getOutputStream());

                    Send(out, this.sourcename + " ALIVE");
                    sleep(20000);
                    // System.out.println("flag is "+this.flag);
                    // System.out.println("ALIVE");

                    HeartSocket.close();
                }

            } catch (Exception e) {
                //
            }
        }

        public void RequestStop() {
            this.flag = false;
            // System.out.println("flag is "+this.flag);
        }
    }

    public class ListenThread extends Thread {
        public ServerSocket ListenSocket;
        public int portnumber;
        public boolean flag;
        public OPERATION peer;
        public String msg;
        public ArrayList<client.Record> local_record;


        public ListenThread(ArrayList<client.Record> record, int portnumber,
                            ServerSocket server, String notes) {
            this.local_record = record;
            this.portnumber = portnumber;
            this.flag = true;
            this.ListenSocket = server;

        }

        public void run() {
            try {
                // ServerSocket waitSocket = new ServerSocket(portnumber);
                while (flag) {
                    // lock.lock();
                    // this.msg=null;
                    Socket standbySocket = ListenSocket.accept();// wait for
                  
                    BufferedReader tin = new BufferedReader(
                        new InputStreamReader(
                            standbySocket.getInputStream()));
                    String word = Receive(tin);


                    System.out.println(word);
                    if (word != null && word.split(":")[0].equals("Port")) {
                        String userrecordPort = word.substring(5, 9);
                        String tmp = word.split("IP address:")[1];
                        String userrecordIP = tmp.split("@")[0];
                        String userrecordName = word.split("@")[1];

                        client.Record user1 = new client.Record(userrecordName,
                                                                userrecordPort, userrecordIP);
                        local_record.add(user1);
                        System.out.println(local_record.get(0).personalport);
                    }


                    if (word.equals("###forceout###")) {

                        client.state="off";
                        System.out.println("this client is "+client.state);
                    }

                    standbySocket.close();
                    // lock.unlock();

                }
                ListenSocket.close();
            } catch (Exception e) {
                //
            }
        }

        public void RequestStop() {
            this.flag = false;
        }

        public String revmsg() {
            return this.msg;
        }
    }

    public class RouteMessage {

        public String message;
        public boolean flag;
        public String hostName;
        public int portNumber;

        public RouteMessage(String message, String hostName, String portNumber) {

            this.message = message;
            this.flag = true;
            this.hostName = hostName;
            this.portNumber = Integer.parseInt(portNumber);
        }

        public void start() {
            try {

           
                Socket writeSocket = new Socket(hostName, portNumber);
                PrintWriter tout = new PrintWriter(
                    writeSocket.getOutputStream());

                OPERATION.Send(tout, message);

                //System.out.println(message + " sent \n");
                writeSocket.close();
                //System.out.println("writeSocket closed");
            } catch (Exception e) {// }
            }
            // System.out.println("write is done");
        }

    }

    public class AddShutdownHookSample {
    	 public void attachShutDownHook(){
    		 
    	  Runtime.getRuntime().addShutdownHook(new Thread() {
    	   @Override
    	   public void run() {
    	    System.out.println("ByeBye!");
    	   }
    	  });
    	  System.out.println("Program Initiating......");
    	 }
    }
}
