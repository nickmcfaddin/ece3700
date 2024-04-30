package lab4.sectionx.yourname.gobackn;

import lab4.sectionx.yourname.entity.Message;
import lab4.sectionx.yourname.entity.Packet;
import lab4.sectionx.yourname.logic.NetworkSimulator;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */
    
    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
  
    private static final int ACK = 1;
    private static final int NAK = 0;
    
    private int windowUpperBound;
    private int windowLowerBound;
    private boolean checksumError;
    private boolean timerOn;
    
    private MessageHolder[] messages;                //All saved messages, window moves through this array
    private int messagesSaved;                       //Tracks unsent messages in "messages" array
    private int currPlacement;                       //Tracks where received messages will be put in "messages" array
    private int currAck;                             //Used to track ACK expected to be received at B
    private int transmittingPackets = 0;             //Number of packets in transmission
    
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = winsize+1;
	RxmtInterval = delay;
    }

    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        //Places each message inside the buffer
        messages[currPlacement] = new MessageHolder(message,currPlacement);
        messagesSaved++;
        System.out.println("A: Received packet. Placing in buffer at #" + currPlacement);
        
        //Once array is maxed out, we reset currPlacement
        if (currPlacement == messages.length - 1){
            currPlacement = 0;
        }
        else{
            currPlacement++;
        }
        
        //Start timer when the number of saved messages is equal to window size
        if (messagesSaved == WindowSize){
            startTimer(A, 50);
            timerOn = true;
            
            if (windowLowerBound < windowUpperBound){
                //Create and send each packet in the window
                for (int i = windowLowerBound; i < windowUpperBound; i++){
                    System.out.println("A: Sending packet #" + i);
                    
                    //Create packet
                    int checksum = i + ACK + messages[i].getMessage().getData().getBytes().length;
                    Packet ourPacket = new Packet(i, ACK, checksum,messages[i].getMessage().getData());
                    
                    //Send packet and increment/decrement variables as needed
                    toLayer3(A,ourPacket);                  
                    messages[i].SentStatus();
                    messagesSaved--;
                    transmittingPackets++;
                }
            }
            else{
                //When the window's lower bound is on the last entry of "messages" array
                if (windowLowerBound == messages.length - 1){
                    
                    //Get message from array, and create the packet
                    Message sentMessage = messages[windowLowerBound].getMessage();
                    int checksum = windowLowerBound + ACK + sentMessage.getData().getBytes().length;
                    Packet ourPacket = new Packet(windowLowerBound, ACK, checksum,sentMessage.getData());
                    
                    //Send packet and increment/decrement variables as needed
                    toLayer3(A, ourPacket);
                    messagesSaved--;
                    transmittingPackets++;
                    messages[windowLowerBound].SentStatus();
                    System.out.println("A: Sending packet #" + windowLowerBound);
                    
                    //Create and send each remaining packet in the window
                    for (int i = 0; i < windowUpperBound; i++){
                        System.out.println("A: Sending packet #" + i);
                        
                        //Create packet
                        sentMessage = messages[i].getMessage();
                        checksum = i + 1 + sentMessage.getData().getBytes().length;
                        ourPacket = new Packet(i, 0,checksum,sentMessage.getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                        messagesSaved--;
                        transmittingPackets++;
                    }
                }
                
                //If upper bound of window at top of "messages" array
                else if (windowUpperBound == messages.length - 1){
                    //Create and send each packet in the window
                    for (int i = windowLowerBound; i < windowUpperBound; i++){
                        //Create packet
                        System.out.println("A: Sending packet #" + i);
                        Message sentMessage = messages[i].getMessage();
                        int checksum = i + ACK + sentMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                        messagesSaved--;//
                        transmittingPackets++;
                    }
                }
                
                else{
                    
                    //Each variable from lower bound of window to top of "messages" array
                    for (int i = windowLowerBound; i < (messages.length); i ++){
                        //Create packet
                        System.out.println("A: Sending packet #" + i);
                        Message sentMessage = messages[i].getMessage();
                        int checksum = i + ACK + sentMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                        messagesSaved--;
                        transmittingPackets++;
                    }
                    
                    //Each variable from bottom of "messages" array to upper bound of window
                    for (int i = 0; i < windowUpperBound; i++){
                        //Create packet
                        System.out.println("A: Sending packet #" + i);
                        Message sentMessage = messages[i].getMessage();
                        int checksum = i + ACK + sentMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                        messagesSaved--;
                        transmittingPackets++;
                    }
                }
            }
        }
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        //Stop the timer if it is on before continuing
        if (timerOn){
            stopTimer(A);
            timerOn = false;
        }
        //Data was properly sent
        if (packet.getChecksum() == packet.getAcknum()+packet.getSeqnum()){
            
            //Checks if the packet was the right sequence number and not a NAK
            if (packet.getSeqnum() == messages[windowLowerBound].getSeqNum() && packet.getAcknum() != NAK){
                
                //Packet out of transit
                transmittingPackets--;   
                
                //Stop the timer if it is on before continuing
                if (timerOn){
                    stopTimer(A);
                    timerOn = false;
                }
                
                //Start a new timer
                startTimer(A, 50);
                timerOn = true;
                
                //Set packet to received
                messages[windowLowerBound].received();
                System.out.println("A: Received expected ACK #" + packet.getSeqnum());
                
                //Upper window bound is at top of array
                if (windowUpperBound == messages.length - 1){
                    
                    //Alter window bounds
                    windowUpperBound = 0;
                    windowLowerBound++;
                    
                    //If the element contains data and has not been sent
                    if (messages[windowUpperBound] != null && messages[windowUpperBound].SentStatus()){
                        //Create packet
                        int seqNum = messages[windowUpperBound].getSeqNum();
                        Message ourMessage = messages[windowUpperBound].getMessage();
                        int ourChecksum = seqNum + ACK + ourMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(seqNum, ACK, ourChecksum,ourMessage.getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A, ourPacket);
                        System.out.println("A: Sending packet #" + seqNum);
                        messagesSaved--;
                        transmittingPackets++;
                        messages[windowUpperBound].SentStatus();
                    }
                }
                
                //Lower window bound is at top of array
                else if (windowLowerBound == messages.length - 1){
          
                    //Alter window bounds
                    windowLowerBound = 0;
                    windowUpperBound++;
                    
                    //If the element contains data and has not been sent
                    if (messages[windowUpperBound] != null && messages[windowUpperBound].SentStatus()){
                        //Create packet
                        int seqNum = messages[windowUpperBound].getSeqNum();
                        Message ourMessage = messages[windowUpperBound].getMessage();
                        int ourChecksum = seqNum + ACK + ourMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(seqNum,ACK,ourChecksum,ourMessage.getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A,ourPacket);
                        System.out.println("A: Sending packet #" + seqNum);
                        messagesSaved--;
                        transmittingPackets++;
                        messages[windowUpperBound].SentStatus();
                    }
                }
                else{
                    //Neither window bound is at top of array, thus increment
                    windowLowerBound++;
                    windowUpperBound++;
                    
                    //If the element contains data and has not been sent
                    if (messages[windowUpperBound] != null && messages[windowUpperBound].SentStatus()){
                        //Create packet
                        int seqNum = messages[windowUpperBound].getSeqNum();
                        Message ourMessage = messages[windowUpperBound].getMessage();
                        int ourChecksum = seqNum + ACK + ourMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(seqNum, ACK, ourChecksum,ourMessage.getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A, ourPacket);
                        System.out.println("A: Sending packet #" + seqNum);
                        messagesSaved--;
                        transmittingPackets++;
                        messages[windowUpperBound].SentStatus();
                    }
                }
                
            }
            //NAK received
            else if (packet.getAcknum() == NAK){
                System.out.println("A: NAK received. Resending window");
                
                //Stop and restart timeout timer
                if (timerOn){
                    stopTimer(A);
                }
                startTimer(A, 50);
                timerOn = true;
                
                //Lower window bound less than upper window bound
                if (windowLowerBound < windowUpperBound){
                    //Resend each packet
                    for (int i = windowLowerBound; i < windowUpperBound; i++){
                        //Create packet
                        System.out.println("A: Resending packet #" + i);
                        int checksum = i + ACK + messages[i].getMessage().getData().getBytes().length;
                        Packet ourPacket = new Packet(i, ACK, checksum,messages[i].getMessage().getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                        transmittingPackets++;
                    }
                }
                else{
                    //Lower window bound at top of "messages" array
                    if (windowLowerBound == messages.length - 1){
                        //Create packet
                        Message sentMessage = messages[windowLowerBound].getMessage();
                        int checksum = windowLowerBound + ACK + sentMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(windowLowerBound, ACK, checksum,sentMessage.getData());
                        
                        //Send packet and increment/decrement variables as needed
                        toLayer3(A, ourPacket);
                        messages[windowLowerBound].SentStatus();
                        transmittingPackets++;
                        
                        System.out.println("A: Resending packet #" + windowLowerBound);
                        
                        //For elements in "messages" array to upper bound of window
                        for (int i = 0; i < windowUpperBound && !messages[i].SentStatus(); i++){
                            //Create packet
                            System.out.println("A: Sending packet #" + i);
                            sentMessage = messages[i].getMessage();
                            checksum = i + ACK + sentMessage.getData().getBytes().length;
                            ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                            
                            //Send packet and increment/decrement variables as needed
                            toLayer3(A, ourPacket);
                            messages[i].SentStatus();
                            transmittingPackets++;
                        }
                    }
                    
                    //Upper bound of window at top of "messages" array
                    else if (windowUpperBound == messages.length - 1){
                        for (int i = windowLowerBound; i < windowUpperBound; i++){
                            //Create packet
                            System.out.println("A: Resending packet #" + i);
                            Message sentMessage = messages[i].getMessage();
                            int checksum = i + ACK + sentMessage.getData().getBytes().length;
                            Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                            
                            //Send packet and increment/decrement variables as needed
                            toLayer3(A, ourPacket);
                            messages[i].SentStatus();
                            transmittingPackets++;
                        }
                    }
                    else{
                        //Values in first half of window
                        for (int i = windowLowerBound; i < (messages.length); i ++){
                            //Create packet
                            System.out.println("A: Resending packet #" + i);
                            Message sentMessage = messages[i].getMessage();
                            int checksum = i + ACK + sentMessage.getData().getBytes().length;
                            Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                            
                            //Send packet and increment/decrement variables as needed
                            toLayer3(A, ourPacket);
                            messages[i].SentStatus();
                            transmittingPackets++;
                        }
                        
                        //Values in second half of window
                        for (int i = 0; i < windowUpperBound; i++){
                            //Create packet
                            System.out.println("A: Resending packet #" + i);
                            Message sentMessage = messages[i].getMessage();
                            int checksum = i + ACK + sentMessage.getData().getBytes().length;
                            Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                            
                            //Send packet and increment/decrement variables as needed
                            toLayer3(A, ourPacket);
                            messages[i].SentStatus();
                            transmittingPackets++;
                        }
                    }
                }
            }
        }
        else{
            System.out.println("A: ERROR incorrect checksum. Ignoring packet");
        }
        
        //Turn timer off in no packets are transmitting
        if (transmittingPackets == 0 && timerOn){
            stopTimer(A);
            timerOn = false;
        }
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
        //Restart timer
        if (timerOn){
            timerOn = false;
        }
        startTimer(A, 50);
        
        //Lower window bound less than upper window bound
        if (windowLowerBound < windowUpperBound){
            timerOn = true;
            
            //Each value in the window
            for (int i = windowLowerBound; i < windowUpperBound; i++){
                
                //If the element contains data and has not been received
                if (messages[i] != null && !messages[i].getReceivedStatus()){
                    //Create packet
                    System.out.println("A: Timeout. Resending packet #" + i);
                    int checksum = i + ACK + messages[i].getMessage().getData().getBytes().length;
                    Packet ourPacket = new Packet(i, ACK, checksum,messages[i].getMessage().getData());
                    
                    //Send packet
                    toLayer3(A, ourPacket);
                    messages[i].SentStatus();
                }
            }
        }
        else{
            timerOn = true;
            
            //Lower window bound at top of "messages" array
            if (windowLowerBound == messages.length - 1){
                
                //If the element contains data and has not been received
                if (messages[windowLowerBound] != null && !messages[windowLowerBound].getReceivedStatus()){
                    //Create packet
                    Message sentMessage = messages[windowLowerBound].getMessage();
                    int checksum = windowLowerBound + ACK + sentMessage.getData().getBytes().length;
                    Packet ourPacket = new Packet(windowLowerBound, ACK, checksum,sentMessage.getData());
                    
                    //Resend packet
                    toLayer3(A, ourPacket);
                    messages[windowLowerBound].SentStatus();
                    System.out.println("A: Timeout. Resending packet #" + windowLowerBound);
                    
                    //Each value in window that is remaining
                    for (int i = 0; i < windowUpperBound && !messages[i].getReceivedStatus(); i++){
                        //Create packet
                        System.out.println("A: Timeout. Resending packet #" + i);
                        sentMessage = messages[i].getMessage();
                        checksum = i + ACK + sentMessage.getData().getBytes().length;
                        ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                        
                        //Send packet
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                    }
                }
            }
            
            //Upper bound of window is at top of "messages" array
            else if (windowUpperBound == messages.length){
                //Each element in the window that has not yet been received
                for (int i = windowLowerBound; i < windowUpperBound && !messages[i].getReceivedStatus(); i++){//for each variable in the window and if the message at i is not sent
                    
                    //If the element contains data and has not been sent
                    if (messages[i] != null && !messages[i].SentStatus()){
                        //Create packet
                        System.out.println("A: Timeout. Resending packet #" + i);
                        Message sentMessage = messages[i].getMessage();
                        int checksum = i + ACK + sentMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                        
                        //Send packet
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                    }
                }
            }
            else{
                
                //Values in first half of window that have not been received
                for (int i = windowLowerBound; i < messages.length - 1 && !messages[i].getReceivedStatus(); i ++){
                    
                    //If the element contains data and has not been sent
                    if (messages[i] != null && !messages[i].SentStatus()){
                        //Create packet
                        System.out.println("A: Timeout. Resending packet #" + i);
                        Message sentMessage = messages[i].getMessage();
                        int checksum = i + ACK + sentMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                        
                        //Send packet
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                    }
                }
                
                //Each variable in second half of window if it has not been received yet
                for (int i = 0; i < windowUpperBound && !messages[i].getReceivedStatus(); i++){
                    
                    //If the element contains data and has not been sent
                    if (messages[i] != null && !messages[i].SentStatus()){
                        //Create packet
                        System.out.println("A: Timeout. Resending packet #" + i);
                        Message sentMessage = messages[i].getMessage();
                        int checksum = i + ACK + sentMessage.getData().getBytes().length;
                        Packet ourPacket = new Packet(i, ACK, checksum,sentMessage.getData());
                        
                        //Send packet
                        toLayer3(A, ourPacket);
                        messages[i].SentStatus();
                    }
                }
            }
        }
        
        //No transmitting packets, turn timer off
        if (transmittingPackets == 0 && timerOn){
            stopTimer(A);
            timerOn = false;
        }
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        //Set upper and lower window bounds
        windowLowerBound = 0;
        windowUpperBound = WindowSize;
        
        //Initialize int counter values to 0
        currPlacement = 0;
        messagesSaved = 0;
        
        //Go back N has initial window size of 2N+1
        messages = new MessageHolder[WindowSize*2 + 1];
        
        //Set timer
        timerOn = true;
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        //Output is expected ACK
        int outputValue = currAck;
        
        //Set output to 0 if ACK is "messages" full array size
        if (currAck == WindowSize * 2 + 1){
            outputValue = 0;
        }
        
        System.out.print("B: Received Packet #"+ packet.getSeqnum() + " and expected packet #"+ outputValue);
        
        //If our values are equal and match, we accept packet, else we reject
        if (packet.getSeqnum() != outputValue){
            System.out.print(" - ignoring sent packet\n");
        }
        else{
            System.out.print(" - accepting sent packet\n");
        }
        
        //Get packet's sequence number and verify against checksum and expected ACK
        int packetSeqNum = packet.getSeqnum();
        if (checksumError && packet.getSeqnum() == currAck){
            checksumError = false;
        }
        
        //If the packet was as expected
        if (packetSeqNum == currAck){
            //No corruption
            if (packet.getChecksum() == (packet.getAcknum() + packetSeqNum + packet.getPayload().getBytes().length)){
                System.out.println("B: Message received correctly. Sending ACK #" + packetSeqNum);
                
                //Increment packet checksum
                int packetChecksum = currAck + ACK;
                
                //Create packet
                Packet ourPacket = new Packet(packetSeqNum, ACK, packetChecksum);
                
                //Send packet and increment ACK
                toLayer3(B, ourPacket);
                toLayer5(packet.getPayload());
                currAck++;
            }
            else{
                System.out.println("B: CHECKSUM ERROR - sending NAK #" + packetSeqNum);
                
                //Expecting same ACK, no increment
                int packetChecksum = packetSeqNum + NAK;
                
                //Create packet
                Packet ourPacket = new Packet(packetSeqNum, NAK, packetChecksum);
                
                //Send packet indicating NAK and checksum error
                toLayer3(B, ourPacket);
                checksumError = true;
            }
        }
        
        //Current ACK is expected and we are at the bottom of the "messages" array
        else if (currAck == 2 * WindowSize + 1 && packetSeqNum == 0){
            //No corruption
            if (packet.getChecksum() == (packet.getAcknum() + packetSeqNum + packet.getPayload().getBytes().length)){
                System.out.println("B: Message received correctly. Sending ACK #" + packetSeqNum);
                
                //Increment packet checksum
                int packetChecksum = 0 + ACK;
                
                //Create packet
                Packet ourPacket = new Packet(0, ACK, packetChecksum);
                
                //Send packet and set next expected ACK
                toLayer3(B, ourPacket);
                toLayer5(packet.getPayload());
                currAck = 1;
            }
            else{
                System.out.println("B: CHECKSUM ERROR - sending NAK #" + packetSeqNum);
                
                //Expecting same ACK, no increment
                int packetChecksum = packetSeqNum + NAK;
                
                //Create packet
                Packet ourPacket = new Packet(packetSeqNum, NAK, packetChecksum);
                
                //Send packet indicating NAK and checksum error
                toLayer3(B, ourPacket);
                checksumError = true;
            }
        }
        
        //Receive duplicate ACK
        else if (packetSeqNum < currAck){
            
            //No checksum error
            if (!checksumError){
                System.out.println("B: Duplicate packet - sending ACK #" + packetSeqNum);
                
                //Increment packet checksum
                int packetChecksum = packetSeqNum + ACK;
                
                //Create packet
                Packet ourPacket = new Packet(packetSeqNum, ACK, packetChecksum);
                
                //Send packet
                toLayer3(B, ourPacket);
            }
        } 
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        //Set up ACK and checksum variables
        currAck = 0;
        checksumError = false;
    }

    // Use to print final statistics
    protected void Simulation_done(){}	
}

// This class will be used to hold the recieved messages
class MessageHolder{
    private Message ourMessage;             
    private boolean sent;               //If message was sent successfully
    private boolean received;           //If message was received successfully
    private int seqNum;                 //Message sequence number
    
    //Constructor
    public MessageHolder(Message ourMessage,int seqNum){
        this.ourMessage = ourMessage;
        sent = false;
        this.seqNum = seqNum; 
        received = false;
    }
    
    //Indicates sent message
    public void messageSent(){
        sent = true;
    }
    
    //Indicates received
    public void received(){
        received = true;
    }	
    
    //GETTERS
    public int getSeqNum(){
        return seqNum;
    }
    public Message getMessage(){
        return ourMessage;
    }
    public boolean SentStatus(){
        return sent;
    }
    public boolean getReceivedStatus(){
        return received;
    }
}
