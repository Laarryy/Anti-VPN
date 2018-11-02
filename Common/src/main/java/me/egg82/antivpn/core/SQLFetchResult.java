package me.egg82.antivpn.core;

public class SQLFetchResult {
    private final DataResult[] data;
    private final ConsensusResult[] consensus;
    private final String[] removedKeys;

    public SQLFetchResult(DataResult[] data, ConsensusResult[] consensus, String[] removedKeys) {
        this.data = data;
        this.consensus = consensus;
        this.removedKeys = removedKeys;
    }

    public DataResult[] getData() { return data; }

    public ConsensusResult[] getConsensus() { return consensus; }

    public String[] getRemovedKeys() { return removedKeys; }
}
