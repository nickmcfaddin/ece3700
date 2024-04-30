package lab3.sectionx.yourname.alternatebitprotocol;

import lab3.sectionx.yourname.entity.Message;
import lab3.sectionx.yourname.entity.Packet;
import lab3.sectionx.yourname.logic.NetworkSimulator;

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
    
    private static boolean packageStatus = false;       //boolean indicating if the package is on the move (0 means no package transmitting)
    private int currSeqNum = 0;                         //sequence number that gets switched post successful data transfer
    private Message currMessage;                        //message saved here in case retransmission is required
    
    
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
        if (packageStatus == false){
            //Get the message that we want to send and start the timer
            System.out.println("A: Sending message #: " + currSeqNum);
            currMessage = message;
            startTimer(A, 100.0);
            
            //Determine checksum, send message along with checksum to receiver and change our packageStatus
            int checksum = message.getData().getBytes().length + currSeqNum;
            toLayer3(A, new Packet(currSeqNum, NAK, checksum, message.getData()));
            packageStatus = true;
        }
        else{
            System.out.println("A: Message received but another message already being transmitted");
        }
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        int packetSize = packet.getAcknum() + packet.getSeqnum() + packet.getPayload().getBytes().length;
        stopTimer(A);
        
        //Upon checksum error, retransmit packet
        if (packet.getChecksum() != packetSize){
            System.out.println("A: Checksum error... re-sending request now");
            packageStatus = false;
            aOutput(currMessage);
        }
        
        //No checksum error
        else{
            
            //Received an ACK
            if (packet.getAcknum() == ACK){
                System.out.println("A: ACK Received for message #: " + packet.getSeqnum());
                
                //Switch our currSeqNum variable to indicate successful data transfer
                if (currSeqNum == 0){
                    currSeqNum = 1;
                }
                else{
                    currSeqNum = 0;
                }
                packageStatus = false;
            }
            
            //Received a NAK, retransmit packet
            else if (packet.getAcknum() == NAK){
                System.out.println("A: NAK Received for message #: " + packet.getSeqnum());
                packageStatus = false;
                aOutput(currMessage);
            }
        }
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
        System.out.println("Timeout occurred, re-transmitting message now: " + currSeqNum);
        packageStatus = false;
        aOutput(currMessage);
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit(){}
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        //Packet received
        System.out.println("B: Received message #: " + packet.getSeqnum());
        int packetSize = packet.getAcknum() + packet.getSeqnum() + packet.getPayload().getBytes().length;
        
        //On corrupt message, we send a NAK
        if (packet.getChecksum() != packetSize){
            System.out.println("B: Checksum error, message is corrupted: sending NAK");
            toLayer3(B, new Packet(packet.getSeqnum(), NAK, packet.getSeqnum() + NAK));
        }
        else{
            System.out.println("B: Message's checksum is correct: sending ACK now");
            toLayer3(B, new Packet(packet.getSeqnum(), ACK, packet.getSeqnum() + ACK));
            toLayer5(packet.getPayload());
        }
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit(){}

    // Use to print final statistics
    protected void Simulation_done(){}	
}
