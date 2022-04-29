package tnorm;

public class Counters {
    private int obli01counter;
    private int rTAACounter;
    private int paymentCounter;

    Counters(){
        this.obli01counter = 1;
        this.rTAACounter = 1;
        this.paymentCounter = 1;
    }

    public int getObli01counter() {
        return obli01counter;
    }
    public void addOneToObli01counter(){
        obli01counter += 1;
    }

    public int getRTAACounter() {
        return rTAACounter;
    }
    public void addOneToRTAACounter(){
        this.rTAACounter +=1;
    }

    public int getPaymentCounter() {
        return paymentCounter;
    }

    public void addOneToPaymentCounter() {
        this.paymentCounter +=1;
    }
}
