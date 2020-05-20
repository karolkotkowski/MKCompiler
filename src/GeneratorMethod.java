import java.util.Arrays;
import java.util.List;

public class GeneratorMethod {
    private GeneratorMethodType type;
    private List<Object> arguments;

    public GeneratorMethod(GeneratorMethodType type, Object ...arguments) {
        this.type = type;
        if (arguments == null)
            this.arguments = null;
        else
            this.arguments = Arrays.asList(arguments);
    }

    public GeneratorMethodType getType() {
        return type;
    }

    public List<Object> getArguments() {
        return arguments;
    }
}
