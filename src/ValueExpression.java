public class ValueExpression implements Expression {
    private ObjectType objectType;
    private DataType dataType;
    private Object value;

    public ValueExpression(ObjectType objectType, DataType dataType, Object value) {
        this.objectType = objectType;
        this.dataType = dataType;
        this.value = value;
    }

    @Override
    public ObjectType getObjectType() {
        return objectType;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    public Object getValue() {
        return value;
    }
}

