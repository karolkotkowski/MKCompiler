public class GlobalVarExpression implements Expression {
    private DataType dataType;
    private String name;
    private int numberOfArguments;
    private ObjectType objectType;
    private Expression length; //it is array index when (this) is array element
    private Classy classy;

    //for variables
    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name, Classy classy) {
        this.dataType = dataType;
        this.name = name;
        this.objectType = objectType;
        this.classy = classy;
    }

    //for functions
    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name, int index, Classy classy) {
        this.dataType = dataType;
        this.name = name;
        this.numberOfArguments = index; //when function it is a number of arguments
        this.objectType = objectType;
        this.classy = classy;
    }

    //for arrays
    public GlobalVarExpression(ObjectType objectType, DataType dataType, String name, Expression length, Classy classy) {
        this.dataType = dataType;
        this.name = name;
        this.objectType = objectType;
        this.length = length;
        this.classy = classy;
    }

    public Classy getClassy() {
        return classy;
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
