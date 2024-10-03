public class LamportClock{
    public int counter;
    LamportClock(){
        counter = 0;
    }
    public void ReceivedAction(int incomingClockCounter){
        counter = Math.max(counter, incomingClockCounter);
        counter++;
    }

    public void UpdateTo(int counter) {
        this.counter = counter;
    }
}
