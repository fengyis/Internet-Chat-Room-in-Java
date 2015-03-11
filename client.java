
import java.util.*;
import java.io.*;
import java.net.*;



public class client {
    public static ArrayList<Record> UserRecord = new ArrayList<Record>();
    public static String state="on";

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        int personalPort;
        OPERATION client = new OPERATION();
        OPERATION.AddShutdownHookSample shutdownhandler = client.new AddShutdownHookSample();
        shutdownhandler.attachShutDownHook();
        for (personalPort = 4000; personalPort < 8080; personalPort++) {

            try {
            	String hostName=args[0];
            	int Hostportnumber = Integer.parseInt(args[1]);
               // String hostName = "127.0.0.1";
                //int Hostportnumber = Integer.parseInt("8080");

                

                String username = "";
                String[] h= new String [3];
                int h_index=0;

                String userrecordName = "";
                boolean login = false;
                ServerSocket ListenSocket = new ServerSocket(personalPort);
                OPERATION.ListenThread listen = client.new ListenThread(UserRecord, personalPort, ListenSocket,state);
                boolean flag=true;
                while(flag) {
                    //while (input_times < 3) {

                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(System.in));

                    System.out.println("Username: ");
                    username = br.readLine().trim ();
                    System.out.println("Password: ");
                    String password = br.readLine().trim ();

                    System.out.println("Connect to 8080");

                    Socket clientSocket = new Socket(hostName, Hostportnumber);
                    PrintWriter out = new PrintWriter(
                        clientSocket.getOutputStream());// write out
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                    OPERATION.Send(out, username + " " + "Authentication "
                                   + username + " " + password + " " + personalPort);

                    String feedback = OPERATION.Receive(in);

                    if (feedback.equals("invalid")) {
                        System.out.println("invalide username and password");
                        h[h_index%3]=username;
                        h_index++;

                        if (h[0].equals(h[1]) && h[1].equals(h[2])) {
                            flag=false;
                            OPERATION.RouteMessage write_2 = client.new RouteMessage(username + " invalid 60s", hostName, "8080");// automatically
                            // down
                            write_2.start();
                            System.out.println(username
                                               + " account has been bloced for 60 seconds due to multiple failures");
                            // notify server to blockout
                        }
                    } else if (feedback.equals("success n")) {
                        login = true;
                        break;

                    } else if (feedback.equals("success y")) {
                        login = true;
                        String offlinemessage = OPERATION.Receive(in);
                        System.out.println("offline message: " + offlinemessage);
                        break;

                    } else if (feedback.equals("block")) {
                        System.out.println("You have been blocked. Please try later");
                    }


                    clientSocket.close();

                } // end of while loop
                OPERATION.heartbeat beat = client.new heartbeat(hostName,
                                           username, Hostportnumber);// initialize heart beat
                if (login) {
                    // listen to server call
                    System.out.println("Welcome to the chat room");
                    listen.start();

                    beat.start();// start heat beat
                    Socket presencebroad = new Socket(hostName, Hostportnumber);
                    PrintWriter lout = new PrintWriter(
                        presencebroad.getOutputStream());// write out
                    OPERATION.Send(lout, username + " broadcast " + username+ " arrived!");
                    presencebroad.close();

                }


                while (login && state.equals("on")) {// input message
                    try {

                        System.out.println(username+": ");
                        BufferedReader tmpmsg = new BufferedReader(
                            new InputStreamReader(System.in));
                        String msg;
                        try {
                            msg = tmpmsg.readLine();
                        } catch (Exception e) {
                            System.out.println("Message invalid. Please reenter");
                            continue;
                        }
                        if (msg.equals("logout")) {
                        	//msg="boadcast "+username+ " left!!";
                            
                            System.out.println("You have successfully log out");

                            Socket leavebroad = new Socket(hostName, Hostportnumber);
                            PrintWriter lout = new PrintWriter(
                                leavebroad.getOutputStream());// write out
                            OPERATION.Send(lout, username + " broadcast " + username+ " left!");
                          
                            leavebroad.close();
                            Socket leavebroad_2 = new Socket(hostName, Hostportnumber);
                            PrintWriter lout_2 = new PrintWriter(
                                leavebroad_2.getOutputStream());// write out
                            OPERATION.Send(lout_2, username + " logout");
                           
                            leavebroad_2.close();
                            login = false;
                            
                            
                        } else if (msg.split(" ")[0].equals("private")) {
                            if(msg.split(" ")[1]!=null) {
                                String peername = msg.split(" ")[1];
                                String peerIP = "";
                                String peerport = "";
                                boolean done=false;
                                //System.out.println(UserRecord.get(0).userName);
                                for (int i = 0; i < UserRecord.size(); i++) {
                                    if (UserRecord.get(i) != null
                                            && UserRecord.get(i).userName
                                            .equals(peername)) {
                                        peerIP = UserRecord.get(i).IP;
                                        peerport = UserRecord.get(i).personalport;
                                        System.out.println(peerport);
                                        System.out.println(peerIP);
                                        Socket writeSocket = new Socket(peerIP,
                                                                        Integer.parseInt(peerport));
                                        PrintWriter pout = new PrintWriter(
                                            writeSocket.getOutputStream());// write out
                                        OPERATION.Send(pout, username + " " + msg);
                                        writeSocket.close();
                                        done=true;
                                    }
                                }
                                if (!done) System.out.println("Can not find "+peername+" on your peerlist");


                            } else {
                                System.out.println("invalid peername");
                            }
                        }  //end of private

                        else{
                        Socket writeSocket = new Socket(hostName, Hostportnumber);
                        PrintWriter lout = new PrintWriter(
                            writeSocket.getOutputStream());// write out
                        OPERATION.Send(lout, username + " " + msg);
                        writeSocket.close();
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("invalid message");

                    }
                    catch (ConnectException e){
                    	System.out.println("connection refused");
                    	
                    }
                    
                    //System.out.println("this is client state" + state);
                }//end of while


              

                beat.RequestStop();
                beat.join();
                ListenSocket.close();
                System.out.println("ListenSocket closed");
               

            } catch (BindException e) {
                continue;
            } 
            catch (ConnectException e){
            	System.out.println("Server is Down");
            	 
            	
            }
            catch (Exception e) {

                e.printStackTrace();
               
            }
            break;
        }// end of for loop
        
        System.out.println("end");

    }

    public static class Record {
        public String personalport;
        public String IP;
        public String userName;
        public String Status;

        public Record(String userName, String personalport, String IP) {
            this.userName = userName;
            this.IP = IP;
            this.personalport = personalport;

        }

    }

}
