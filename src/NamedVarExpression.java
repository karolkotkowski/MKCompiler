public class NamedVarExpression implements  Expression {
    private DataType dataType;
    private String name;
    private ObjectType objectType;
    private int index;

    public NamedVarExpression(ObjectType objectType, DataType dataType, String name, int index) {
        this.dataType = dataType;
        this.name = name;
        this.objectType = objectType;
        this.index = index;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getName() {
        return name;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public int getIndex() {
        return index;
    }
}
