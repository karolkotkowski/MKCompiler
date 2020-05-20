import java.util.List;

public class Method {
    private String name;
    private DataType dataType;
    private List<DataType> arguments;

    public Method(String name, DataType dataType, List<DataType> arguments) {
        this.name = name;
        this.dataType = dataType;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public List<DataType> getArguments() {
        return arguments;
    }
}
