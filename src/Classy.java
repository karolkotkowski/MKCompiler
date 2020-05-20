import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Classy {
    private String name;
    private Map<String, GlobalVarExpression> fields = new HashMap();
    private Map<String, Method> methods = new HashMap<>();
    private List<GeneratorMethod> generatorMethods = new ArrayList<>();

    public Classy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addField(GlobalVarExpression expression) {
        fields.put(expression.getName(), expression);
    }

    public GlobalVarExpression getField(String name) {
        return fields.get(name);
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public void addMethod(String name, Method method) {
        methods.put(name, method);
    }

    public Method getMethod(String name) {
        return methods.get(name);
    }

    public boolean hasMethod(String name) {
        return methods.containsKey(name);
    }

    public List<GeneratorMethod> getGeneratorMethods() {
        return generatorMethods;
    }

    public void addGeneratorMethod(GeneratorMethod generatorMethod) {
        generatorMethods.add(generatorMethod);
    }
}
