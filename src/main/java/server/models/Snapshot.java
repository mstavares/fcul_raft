package server.models;

import server.StateMachine;

public class Snapshot {

    private StateMachine stateMachine;
    private int lastIncludedIndex, lastIncludedTerm;

    public Snapshot(StateMachine stateMachine, int lastIncludedIndex, int lastIncludedTerm) {
        this.stateMachine = stateMachine;
        this.lastIncludedTerm = lastIncludedTerm;
        this.lastIncludedIndex = lastIncludedIndex;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public int getLastIncludedIndex() {
        return lastIncludedIndex;
    }

    public int getLastIncludedTerm() {
        return lastIncludedTerm;
    }

}
