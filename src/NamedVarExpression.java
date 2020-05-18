public class NamedVarExpression implements  Expression {
    private DataType dataType;
    private String name;
    private ObjectType objectType;

    public NamedVarExpression(ObjectType objectType, DataType dataType, String name) {
        this.dataType = dataType;
        this.name = name;
        this.objectType = objectType;
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

}
