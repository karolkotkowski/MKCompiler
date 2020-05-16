public enum CompareType {
    EQ, SLT, SLE, SGE, SGT, NE;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
