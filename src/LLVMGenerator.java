import java.util.HashMap;

public class LLVMGenerator {

    private HashMap<String, Variable> globalVariables;
    private StringBuilder headerText = new StringBuilder();
    private StringBuilder bodyText = new StringBuilder();
    private int printableIndex = 0;
    private int scannableIndex = 0;

    private Configuration configuration = new Configuration();
    private HashMap<String, String>  systemVariables = configuration.getSystemVariables();

    public LLVMGenerator (HashMap<String, Variable> globalVariables) {
        this.globalVariables = globalVariables;
    }

    public void generateOutput() {
        StringBuilder text = new StringBuilder();
        text.append("declare i32 @printf(i8*, ...)\n");
        text.append("declare i32 @scanf(i8*, ...)\n");
        text.append(systemVariables.get("printInt") + " = constant [4 x i8] c\"%d\\0A\\00\"\n");
        text.append(systemVariables.get("scanInt") + " = constant [3 x i8] c\"%d\\00\"\n");
        text.append("\n");
        text.append(headerText);
        text.append("\n");
        text.append("define i32 @main() nounwind{\n");
        text.append(bodyText);
        text.append("  ret i32 0 \n}\n");
        System.out.println(text.toString());
    }

    public void declareVariable(Variable variable) {
        VariableScope scope = variable.getScope();
        VariableType variableType = variable.getType();
        String name = variable.getName();
        Object value = variable.getValue();

        switch (variableType) {
            case INT:
                switch(scope) {
                    case GLOBAL:
                        headerText.append("@var_" + name + " = global i32 0\n");
                    break;
                }
                if (value != null) {
                    assignVariable(variable);
                }
                break;
            case REAL:

                break;
            case STR:
                String string = (String) value;
                int stringLength = string.length();
                String irStringSize = "[" + (stringLength+2) + " x i8]";
                headerText.append("@var_" + name + " = constant" + irStringSize + " c\"" + string + "\\0A\\00\"\n");
                break;
            case NONE:
                break;
        }
    }

    public void assignVariable(Variable variable) {
        VariableScope scope = variable.getScope();
        VariableType type = variable.getType();
        String name = variable.getName();
        Object value = variable.getValue();

        switch (type) {
            case INT:
                String textValue = value.toString();
                switch (scope) {
                    case GLOBAL:
                        bodyText.append("  store i32 " + textValue + ", i32* @var_" + name + "\n\n");
                        break;
                }
                break;
            case REAL:

                break;
            case STR:

                break;
        }
    }

    public void printStr(String string) {
        int stringLength = string.length();
        String irStringSize = "[" + (stringLength + 2) + " x i8]";
        headerText.append(systemVariables.get("printValue") + printableIndex + " = constant" + irStringSize + " c\"" + string + "\\0A\\00\"\n");
        bodyText.append("  call i32 (i8*, ...) @printf(i8* getelementptr inbounds ( " + irStringSize + ", " + irStringSize + "* " + systemVariables.get("printValue") + printableIndex + ", i32 0, i32 0))\n\n");
        printableIndex++;
    }

    public void printStr(Variable variable) {
        VariableScope scope = variable.getScope();
        String name = variable.getName();
        String string = (String) variable.getValue();
        int stringLength = string.length();
        String irStringSize = "[" + (stringLength + 2) + " x i8]";
        switch (scope) {
            case GLOBAL:
                if (globalVariables.containsKey(name)) {
                    bodyText.append("  call i32 (i8*, ...) @printf(i8* getelementptr inbounds ( " + irStringSize + ", " + irStringSize + "* @var_" +name + ", i32 0, i32 0))\n\n");
                }
                break;
        }
    }

    public void printInt(String text) {
        printStr(text);
    }

    public void printInt(Variable variable) {
        VariableScope scope = variable.getScope();
        String name = variable.getName();
        switch (scope) {
            case GLOBAL:
                if (globalVariables.containsKey(name)) {
                    bodyText.append("  %printable" + printableIndex + " = load i32, i32* @var_" + name + "\n");
                } else {
                    //error
                }
                break;
        }
        printableIndex++;
        bodyText.append("  %printable" + printableIndex + " = call i32 (i8* , ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]*" + systemVariables.get("printInt") + ", i32 0, i32 0), i32 %printable" + (printableIndex - 1) + ")\n\n");
        printableIndex++;
    }

    public void scanInt(Variable variable) {
        VariableScope scope = variable.getScope();
        String name = variable.getName();

        switch (scope) {
            case GLOBAL:
                bodyText.append("  %scannable" + scannableIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* " + systemVariables.get("scanInt") + ", i32 0, i32 0), i32* @var_" + name + ")\n\n");
                printableIndex++;
                break;
        }
    }
}
