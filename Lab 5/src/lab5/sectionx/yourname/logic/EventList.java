package lab5.sectionx.yourname.logic;


import lab5.sectionx.yourname.entity.Event;

public interface EventList
{
    public boolean add(Event e);
    public Event removeNext();
    public String toString();
    public double getLastPacketTime(int entityFrom, int entityTo);
}
