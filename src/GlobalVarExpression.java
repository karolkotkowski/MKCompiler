public class GlobalVarExpression implements Expression {
    private DataType dataType;
    private String name;
    private int index;
    private ObjectType objectType;

    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name, int index) {
        this.dataType = dataType;
        this.name = name;
        this.index = index;
        this.objectType = objectType;
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

    public void increaseIndex() {
        index++;
    }
}
