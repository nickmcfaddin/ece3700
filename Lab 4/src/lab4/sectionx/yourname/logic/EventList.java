package lab4.sectionx.yourname.logic;

import lab4.sectionx.yourname.entity.Event;

public interface EventList
{
    public boolean add(Event e);
    public Event removeNext();
    public String toString();
    public Event removeTimer(int entity);
    public double getLastPacketTime(int entityTo);
}
