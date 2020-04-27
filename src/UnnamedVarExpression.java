public class UnnamedVarExpression implements  Expression {
    private DataType dataType;
    private int index;
    private ObjectType objectType;

    public UnnamedVarExpression(ObjectType objectType, DataType dataType, int index) {
        this.dataType = dataType;
        this.index = index;
        this.objectType = objectType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public int getIndex() {
        return index;
    }

    public ObjectType getObjectType() {
        return objectType;
    }
}
