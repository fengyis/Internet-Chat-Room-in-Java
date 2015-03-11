
import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.*;


public class server {

    
    public static ArrayList<User> UserPool = new ArrayList<User>();



 

    public static void main(String[] args) throws IOException  {

        int portNumber = Integer.parseInt(args[0]);
    	//int portNumber = Integer.parseInt("8080");
    	boolean flag=true;
        OPERATION server = new OPERATION();

        OPERATION.AddShutdownHookSample shutdownhandler = server.new AddShutdownHookSample();
        shutdownhandler.attachShutDownHook();

        ServerSocket	serverSocket = new ServerSocket(portNumber);

        String[] allUserName = LoadUsers();

        System.out.println("Listen to port "+portNumber);
        while (flag) {

            try {
                Socket loginlistenSocket = serverSocket.accept();

                InetAddress inetAddress = loginlistenSocket.getInetAddress();
                String targetIP = inetAddress.getHostAddress();

                // System.out.println("connection established client");
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        loginlistenSocket.getInputStream()));
                PrintWriter out = new PrintWriter(
                    loginlistenSocket.getOutputStream());

                String rawmessage = in.readLine();

                String[] message;
                String sourceusername="";
                String command;
                message = rawmessage.split(" ");
                sourceusername = message[0];
                try {
                    // invalid start message;
                	 
                    command =message[1];

                } catch(ArrayIndexOutOfBoundsException e) {
                    loginlistenSocket.close();
                    command="JumpToIllegalCommand";

                } catch (Exception e) {
                    loginlistenSocket.close();
                    continue;
                }


                ArrayList<User> online = GetOnlineUser(UserPool, "online");

                User sourceuser = Searchlist(online, sourceusername);



                // sourceuser==null for unlive user
                // if(sourceuser==null){

                // }
                try {


                    if (command.equals("Authentication")) {// login check
                        String username = message[2];

                        String password = message[3];
                        String targetport = message[4];
                        Calendar logintime = Calendar.getInstance();

                        User checkuser = Searchlist(UserPool, username);

                        boolean block = false;
                       
                        if (checkuser != null && checkuser.blocktime- logintime.getTimeInMillis() > 0) {
                        	//check whether the user has been blocked for log in
                            block = true;
                        }

                        int result = Authentication(username, password, allUserName);

                        if (result == 1 && !block) { //not blocked
                            // sucess

                            User newuser = Searchlist(UserPool, username);
                            User checkexist=Searchlist(online,username);
                            if(checkexist!=null && newuser.getName().equals(checkexist.getName())) {
                            	//check whether this user is already online, if so, force out 
                                OPERATION.RouteMessage write = server.new RouteMessage(
                                                                   "###forceout###", checkexist.getIP(),
                                                                   checkexist.getPersonalport());// automatically down
                                write.start();

                            }
                            newuser.initialize(username, password, targetport,
                                               targetIP, logintime, "online");
                            String suffix = "n";
                            int a = 0;
                            if (newuser.offlinemessage[0] != null) {
                            	//has offline message
                                a = 1;
                                suffix = "y";

                            }

                            OPERATION.Send(out, "success " + suffix);
                            String omsg = "";
                            if (a == 1) {
                                for (int i = 0; i < 200; i++) {
                                    if (newuser.offlinemessage[i] != null) {
                                    	//catch offlien message and send, saperated by "/"
                                        omsg = omsg + newuser.offlinemessage[i]
                                               + "/";
                                    } else
                                        break;
                                }
                                OPERATION.Send(out, omsg);
                                newuser.clearofflinemsg();
                            }

                        } else if (block) {
                            OPERATION.Send(out, "block");
                        } else {
                            OPERATION.Send(out, "invalid");

                        }

                    } else if (command.equals("invalid")) {
                        // block for 60s
                        User blockuser = Searchlist(UserPool, sourceusername);
                        if(blockuser!=null) {
                            Calendar blocktime = Calendar.getInstance();
                            blockuser.blocktime = blocktime.getTimeInMillis() + 60000;
                            System.out.println("This is the blocked user"
                                               + blockuser.getName() + "blocktime: "
                                               + blockuser.blocktime);
                        }

                    } else if (command.equals("online")) {

                        // show online users

                        int j = 0;
                        int k;
                        String[] namelist = new String[online.size()];
                        System.out.println(online.size());
                        for (int i = 0; i < online.size(); i++) {
                            User onlineuser=online.get(i);
                            //get online user
                            namelist[j++] = onlineuser.getName();
                            k=0;
                            while(onlineuser.blocklist[k]!=null&& onlineuser.blocklist[k].equals(sourceusername)) {
                                k++;
                                namelist[--j]=null;
                            }
                        }

                        System.out.println("show online users "
                                           + Arrays.toString(namelist));
                        User targetuser = Searchlist(online, sourceusername);

                        OPERATION.RouteMessage write = server.new RouteMessage(
                                                           Arrays.toString(namelist), targetuser.getIP(),
                                                           targetuser.getPersonalport());// automatically down
                        write.start();

                    } else if (command.equals("ALIVE")) {
                        // System.out.println(rawmessage);
                        Calendar refreshtime = Calendar.getInstance();
                        sourceuser.setlogintime(refreshtime);
                        for (int i = 0; i < UserPool.size(); i++) {
                        	//receive ALIVE signal in 30 seconds to stay online
                            if (refreshtime.getTimeInMillis()
                                    - UserPool.get(i).getlogintime() > 30000) {
                            	User offuser=UserPool.get(i);
                            	if(UserPool.get(i).Status!=null && UserPool.get(i).Status.equals("online")){
                                     for (int k = 0; k < online.size(); k++) {
                                             String msg = offuser.getName() + " is down " ;
                                             OPERATION.RouteMessage write = server.new RouteMessage(
                                                                                msg, online.get(k).getIP(), online.get(k)
                                                                                .getPersonalport());// automatically
                                             // down
                                             write.start();
                                     }
                            	}
                            	UserPool.get(i).Status = "offline";
                                
                              
                            }
                        }

                    }

                    else if (command.equals("message")) {
                        // route the message to correct user

                        String msg;
                        String targetusername = message[2];
                        if(Searchlist(UserPool,targetusername)==null) {
                            throw new ArrayIndexOutOfBoundsException();
                        }
                        User targetuser = Searchlist(online, targetusername);
                        if (targetuser == null) {
                        	//if targetuser is offline, save offine message for him
                            int offset = message[0].length() + command.length()
                                         + message[2].length() + 2;
                            msg = sourceusername + ":"
                                  + rawmessage.substring(offset);
                            User revuser = Searchlist(UserPool, targetusername);
                            revuser.saveOfflinemsg(msg);
                            System.out.println("offline message" + msg + "for"
                                               + revuser.getName() + "saved");
                        }

                        else {//route message
                            System.out.println(sourceusername + "and "
                                               + targetusername);
                            boolean block = targetuser
                                            .Checkblocklist(sourceusername);

                            if (!block) {
                            	//if not blocked by sourceuser
                                int offset = message[0].length()
                                             + command.length() + message[2].length()
                                             + 2;
                                msg = sourceusername + ":"
                                      + rawmessage.substring(offset);

                                OPERATION.RouteMessage write = server.new RouteMessage(
                                                                   msg, targetuser.getIP(),
                                                                   targetuser.getPersonalport());// automatically
                                // down
                                write.start();

                            } else {
                            	//blocked by source user
                                msg = "You have been blocked by "
                                      + targetuser.getName();
                                OPERATION.RouteMessage write = server.new RouteMessage(
                                                                   msg, sourceuser.getIP(),
                                                                   sourceuser.getPersonalport());// automatically
                                // down
                                write.start();
                            }
                        }
                    } else if (command.equals("broadcast")) {
                        // broadcast message
                        int offset = message[0].length() + command.length() + 1;
                        String tmpmsg = rawmessage.substring(offset);
                        boolean hasblock=false;
                        for (int i = 0; i < online.size(); i++) {
                            if(!online.get(i).Checkblocklist(sourceusername)) {
                                String msg = sourceusername + ": " + tmpmsg;
                                OPERATION.RouteMessage write = server.new RouteMessage(
                                                                   msg, online.get(i).getIP(), online.get(i)
                                                                   .getPersonalport());// automatically
                                // down
                                write.start();
                            } else {
                                hasblock=true;
                            }
                        }//end of for loop
                            if(hasblock){
                                OPERATION.RouteMessage write_2 = server.new RouteMessage(
                                                                     "Some recipents blocked you", sourceuser.getIP(), sourceuser
                                                                     .getPersonalport());// automatically
                                write_2.start();
                            }
                            
                       
                    }

                    else if (command.equals("block")) {
                        // block user

                        String targetusername = message[2];

                        User targetuser = Searchlist(online, targetusername);
                        sourceuser.block(targetuser.getName());
                        String msg = "You have been blocked by " + sourceusername;
                        OPERATION.RouteMessage write = server.new RouteMessage(msg,
                                                       targetuser.getIP(), targetuser.getPersonalport());// automatically
                        // down
                        write.start();
                    } else if (command.equals("unblock")) {
                        // unblock user
                        String targetusername = message[2];
                        User targetuser = Searchlist(online, targetusername);
                        sourceuser.unblock(targetuser.getName());
                        String msg = "You have been unblocked by " + sourceusername;
                        OPERATION.RouteMessage write = server.new RouteMessage(msg,
                                                       targetuser.getIP(), targetuser.getPersonalport());// automatically
                        // down
                        write.start();
                    } else if (command.equals("getaddress")) {
                        // get user address
                        String targetusername = message[2];
                        User targetuser = Searchlist(online, targetusername);
                        String msg;
                        if (targetuser==null) {
                            msg=targetusername+" is offline. Request failed!";
                        } else {
                            msg = "Port:" + targetuser.getPersonalport() + " "
                                  + "IP address:" + targetuser.getIP() + "@"
                                  + targetusername;
                        }
                        OPERATION.RouteMessage write = server.new RouteMessage(msg,
                                                       sourceuser.getIP(), sourceuser.getPersonalport());// automatically
                        // down
                        write.start();
                    } else if (command.equals("logout")) {
                       
                        
                        String msg = "You are offline now";
                        System.out.println(sourceuser.getName() + "is off");
                        OPERATION.RouteMessage write = server.new RouteMessage(msg,
                                                       sourceuser.getIP(), sourceuser.getPersonalport());// automatically
                        // down
                        write.start();
                        sourceuser.Status = "offline";
                       

                    } else {
                        throw new ArrayIndexOutOfBoundsException();
                    }

                } catch(ArrayIndexOutOfBoundsException e) {
                	//catch illegal command

                    OPERATION.RouteMessage write = server.new RouteMessage("Illegal command. Please resend",
                                                   sourceuser.getIP(), sourceuser.getPersonalport());// automatically

                    write.start();

                }
                loginlistenSocket.close();
            } catch(Exception e) {

                e.printStackTrace();
                continue;
            }


        }// end of outer while loop

        serverSocket.close();




    }



    public static int Authentication(String username, String password,
                                     String[] allusers) {

        for (int i = 0; i < UserPool.size(); i++) {
            if (UserPool.get(i).getName().equals(username)
                    && UserPool.get(i).getPassword().equals(password))
                return 1;

        }
        return 0;

    }

    public static User Searchlist(ArrayList<User> UserPool, String name) {
        User target=null;
        for (int i = 0; i < UserPool.size(); i++) {
            if (UserPool.get(i).getName().equals(name)) {
                target=UserPool.get(i);

            }

        }

        return target;
    }

    public static class UserNotFindException extends Exception {
        UserNotFindException() {
            super();
            System.out.println("user not found!");
        }

    }

    public static ArrayList<User> GetOnlineUser(ArrayList<User> UserPool,
            String status) {

        ArrayList<User> onlineuser = new ArrayList<User>();

        for (int i = 0; i < UserPool.size(); i++) {
            if (UserPool.get(i).getStatus() != null
                    && UserPool.get(i).getStatus().equals("online")) {

                onlineuser.add(UserPool.get(i));
            }
        }
        return onlineuser;
    }

    public static String[] LoadUsers() throws FileNotFoundException {

        File file = new File("credentials.txt");
        Scanner scan = new Scanner(file);

        while (scan.hasNext()) {
            String tmpName = scan.next();
            Calendar currentime = Calendar.getInstance();
            User tmpUser = new User(tmpName, scan.next(), currentime);
            UserPool.add(tmpUser);
        }

        scan.close();

        String[] allUserName = new String[UserPool.size()];
        for (int i = 0; i < UserPool.size(); i++) {

            allUserName[i] = UserPool.get(i).getName();
        }

        return allUserName;
    }

    public static class User {
        public String userName;

        public long loginTime;
        private String passWord;
        public String personalport;
        public String IP;
        public String Status;
        public long blocktime;

        public String[] offlinemessage = new String[200];
        public String[] blocklist = new String[200];

        public User(String userName, String passWord, Calendar login) {
            this.userName = userName;
            this.passWord = passWord;
            this.blocktime = login.getTimeInMillis();

        }

        public User(String userName, String passWord, String personalport,
                    String IP, Calendar login, String status) {
            this.userName = userName;
            this.passWord = passWord;
            this.personalport = personalport;
            this.IP = IP;
            this.loginTime = login.getTimeInMillis();
            this.Status = status;

        }

        public void initialize(String userName, String passWord,
                               String personalport, String IP, Calendar login, String status) {
            this.userName = userName;
            this.passWord = passWord;
            this.personalport = personalport;
            this.IP = IP;
            this.loginTime = login.getTimeInMillis();
            this.Status = status;
        }

        public String getName() {
            String Name;
            Name = userName;
            return Name;
        }

        public String getPassword() {
            return this.passWord;
        }

        public String getPersonalport() {
            return this.personalport;
        }

        public String getIP() {
            return this.IP;
        }

        public String getStatus() {
            return this.Status;
        }

        public void setlogintime(Calendar refreshtime) {

            this.loginTime = refreshtime.getTimeInMillis();
        }

        public long getlogintime() {
            return this.loginTime;
        }

        public void block(String name) {

            for (int i = 0; i < Array.getLength(blocklist); i++) {

                if(name.equals(blocklist[i])) {
                    break;
                } else if (blocklist[i] == null) {
                    blocklist[i] = name;
                    break;
                }

            }
        }

        public void unblock(String name) {
            int i;
            for (i = 0; i < Array.getLength(blocklist); i++) {
                if (blocklist[i] != null && blocklist[i] == name) {
                    System.out.println(blocklist[i]);
                    blocklist[i] = null;

                    break;
                }

            }
            for (int j = i; j < Array.getLength(blocklist); j++) {
                if (blocklist[j + 1] != null) {
                    blocklist[j] = blocklist[j + 1];
                    System.out.println(Arrays.toString(blocklist));
                } else {
                    blocklist[j]=null;
                    break;
                }
            }
        }

        public boolean Checkblocklist(String name) {
            boolean result = false;

            for (int i = 0; i < Array.getLength(this.blocklist); i++) {
                if (blocklist[i] != null && blocklist[i].equals(name)) {
                    result = true;
                    return result;
                }

            }
            return result;

        }

        public void saveOfflinemsg(String msg) {
            int i = 0;

            while (offlinemessage != null && offlinemessage[i] != null) {
                i++;
            }
            offlinemessage[i] = msg;
        }

        public void clearofflinemsg() {
            this.offlinemessage = new String[200];
        }

    }

    /*
     * public class SchedueTask extends TimerTask { public ArrayList<User>
     * Userpool; public String Username;
     *
     * public SchedueTask(ArrayList<User> userpool, String username){
     * this.Userpool=userpool; this.Username=username;
     *
     *
     *
     * } public void run(){ User targetuser=server.Searchlist(this.Userpool,
     * this.Username); targetuser.Status="offline"; return; }
     *
     *
     * }
     */

}
