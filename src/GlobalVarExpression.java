public class GlobalVarExpression implements Expression {
    private DataType dataType;
    private String name;
    private int index;
    private ObjectType objectType;
    private Expression length; //it is array index when (this) is array element

    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name, int index) {
        this.dataType = dataType;
        this.name = name;
        this.index = index;
        this.objectType = objectType;
    }

    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name, Expression length) {
        this.dataType = dataType;
        this.name = name;
        this.index = index;
        this.objectType = objectType;
        this.length = length;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public Expression getLength() { return length; }
}
