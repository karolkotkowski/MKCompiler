public enum DataType {
    INT, REAL, CHAR;

    public String toLLVM() {
        switch (this) {
            case INT:
                return "i32";
            case REAL:
                return "double";
            default:
                return null;
        }
    }
}
