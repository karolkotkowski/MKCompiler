public class Variable {
    private VariableType type;
    private String name;
    private Object value;
    private VariableScope scope;
    private int memoryIndex;

    public Variable(VariableScope scope, VariableType type, String name, Object value) {
        this.scope = scope;
        this.type = type;
        this.name = name;
        this.value = value;
        this.memoryIndex = 0;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getMemoryIndex() {
        return memoryIndex;
    }

    public void increaseMemoryIndex() {
        this.memoryIndex++;
    }

    public VariableScope getScope() {
        return scope;
    }

    public void setScope(VariableScope scope) {
        this.scope = scope;
    }

}