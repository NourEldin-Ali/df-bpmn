package org.openbpmn.bpmn.discovery.model;

public class Pair<S, T> {
    private S first;
    private T second;

    public Pair(S first, T second) {
        this.first = first;
        this.second = second;
    }

    public S getSource() {
        return first;
    }

    public void setFirst(S first) {
        this.first = first;
    }

    public T getTarget() {
        return second;
    }

    public void setSecond(T second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "source=" + first +
                ", target=" + second +
                '}';
    }
}
