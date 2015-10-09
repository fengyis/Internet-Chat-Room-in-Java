# Internet-Chat-Room-in-Java

##Description:
This Internet Chat Room Service utilize none-persistent socket connection. Each time the server or client makes the connection, the connection will close immediately after they accomplish their task, e.g. send message or ALIVE signal. The server plays a role like a message center which could provide users with online status, forward message and store offline messages.

The server should be constructed with a predefined portnumber by which each client will be able to connect. After initialization, the server would listen to each call from the clients either for authentication, broadcast or message etc. The client side, on the other hand, should be constructed with destination IP address and portnumber defined by the server. Each client has its own portnumber starting from 4000(arbitrary) to 8080. If the multiple clients attempt to start within the same IP address, the client would avoid conflicts and increment their port number by one until no conflicts have been caught.

Basically, the message contains four segnments. First one is the sourceuser, from which the message is sent. The second is the command, which indicates what the sourceuser wants, e.g. message, getaddress and etc. The server will process the rawmessage and peform the function according to the command. The detailed commands and how it operates is listed below.

The structure of this program is divided into three parts, server, client and OPERATION. The roles of server and client are obvious. OPERATION here is a class which provide every method or function that server or client might need. The purpose here is to avoid duplication code for the same function such as send/receive messages and listen thread.

The server has a arraylist of USERs which store all of information for a single USER including IP address, Portnumber, online status, username, password, offline message and etc. Thereby, whatever client wants or request, the server would just consult the arraylist and return the requested contents. Also note that the server could exit gracefully by hitting ctrl+C. This is the only way for server to quit. 

The client side also has a arraylist of records which record every peer information to which they requested the address information and prepare to private chat.


*Message Center Initialization: java server XXX

XXX is the portnumber specified by administarator

*Client Initialization: java client XXX YYY

XXX is the destination IP address
YYY is the portnumber defined by server.

##Functionality 

###a. User Authentication
If a user try the same user name three times and failed, such username will be blocked in 30 seconds. Eventhough log in another IP address, this username will still be unavailable. 
If a user try the same username when such username is already online, the old user will be forced to logout and new user will be prompted to log in.
displayed on his console, if not, this message will be saved and will be delivered next time target user logs in. 

###b. Heartbeat and Timeout
Every client will automatically send server a ALIVE signal in 20 seconds. The sever will record this signal and indicate that the user is online. If the server has not received the signal in 30 seconds(with 10 senconds graceful extention)， this user will be treated as offline.

###c.User message forwarding 
The client could exchange messages by prefix of "message." By doing this, server acts like a message center which forward what it receives from client to the destination address. 
**command: message username XXX 
XXX is the message that you want to send. The contend will be delivered to the user name through server. If it is online, the message will be delivered. If not, message will be saved as offline message and will be prompted out when the user is back.

###d.Block/unclock
The client could maintain a list of users which they would not like to hear any information from such like private, broadcast and presence.
**command: block username
**command: unblock username
Username is the name that you want to block/remove from your block list.

###e. Online
The client coud get a list of online users just by typing online.
**command: online
This will return you the list of current online users

###f. Getaddress targetusername
In order to establish peer to peer connection, the user should get address priori to private command. 
After getting the online list, the "getaddress username" will give you the IP address and Port number of this user and next step, you will be able to build P2P connection to exchange message to this user without routing from the server.
**command: getaddress XXX
XXX is the username which you want to chat privately.

###g. Private chat
Client is able to send private message without routing from message center. That means, eventough the message center is down, the online users could still exachange messages for those who already caught the address.
**command: private username XXX
This will send private message to the target user name without passing through the server.

###h.Broadcast message
Client could broadcast message to any online clients including himself. 
**command: broadcast XXX
XXX is the message content. This command will lead to every online user see the message including user itself.

###i. Logout
This command will let the user logout the chat room and broadcast the message indicating his log off message.
**command: logout

###j. shutdown control
If any user press ctrl+c, the program will exit with message "ByeBye"

