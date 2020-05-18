public class GlobalVarExpression implements Expression {
    private DataType dataType;
    private String name;
    private int numberOfArguments;
    private ObjectType objectType;
    private Expression length; //it is array index when (this) is array element

    //for variables
    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name) {
        this.dataType = dataType;
        this.name = name;
        this.objectType = objectType;
    }

    //for functions
    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name, int index) {
        this.dataType = dataType;
        this.name = name;
        this.numberOfArguments = index; //when function it is a number of arguments
        this.objectType = objectType;
    }

    //for arrays
    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name, Expression length) {
        this.dataType = dataType;
        this.name = name;
        this.numberOfArguments = numberOfArguments;
        this.objectType = objectType;
        this.length = length;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getName() {
        return name;
    }

    public int getNumberOfArguments() {
        return numberOfArguments;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public Expression getLength() { return length; }
}
