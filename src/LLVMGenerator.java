import java.util.HashMap;

public class LLVMGenerator {

    private HashMap<String, Variable> globalVariables;
    private StringBuilder headerText = new StringBuilder();
    private StringBuilder bodyText = new StringBuilder();
    private int printableIndex = 0;
    private int scannableIndex = 0;
    private LLVMActions actions;

    private Configuration configuration = new Configuration();
    private HashMap<String, String>  systemVariables = configuration.getSystemVariables();

    public LLVMGenerator (LLVMActions actions, HashMap<String, Variable> globalVariables) {
        this.actions = actions;
        this.globalVariables = globalVariables;
    }

    public void generateOutput() {
        StringBuilder text = new StringBuilder();
        text.append("declare i32 @printf(i8*, ...)\n");
        text.append("declare i32 @scanf(i8*, ...)\n");
        text.append(systemVariables.get("printInt") + " = constant [4 x i8] c\"%d\\0A\\00\"\n");
        text.append(systemVariables.get("printReal") + " = constant [4 x i8] c\"%f\\0A\\00\"\n");
        text.append(systemVariables.get("scanInt") + " = constant [3 x i8] c\"%d\\00\"\n");
        text.append(systemVariables.get("scanReal") + " = constant [4 x i8] c\"%lf\\00\"\n");
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
        int memoryIndex = variable.getMemoryIndex();

        switch (variableType) {
            case INT:
                switch (scope) {
                    case GLOBAL:
                        headerText.append("@var_" + name + memoryIndex + " = global i32 0\n");
                        break;
                }
                break;
            case REAL:
                switch (scope) {
                    case GLOBAL:
                        headerText.append("@var_" + name + memoryIndex + " = global double 0.0\n");
                        break;
                }
                break;
            case STR:
                String string = (String) value;
                int stringLength = string.length();
                String irStringSize = "[" + (stringLength+2) + " x i8]";
                headerText.append("@var_" + name + memoryIndex + " = constant" + irStringSize + " c\"" + string + "\\0A\\00\"\n");
                break;
            case NONE:
                switch (scope) {
                    case GLOBAL:
                        headerText.append("@var_" + name + memoryIndex + " = global i32 0\n");
                        break;
                }
                break;
        }
    }

    public void assignVariable(Variable variable) {
        VariableScope scope = variable.getScope();
        VariableType type = variable.getType();
        String name = variable.getName();
        Object value = variable.getValue();
        int memoryIndex = variable.getMemoryIndex();

        String textValue = value.toString();
        switch (type) {
            case INT:
                switch (scope) {
                    case GLOBAL:
                        bodyText.append("  store i32 " + textValue + ", i32* @var_" + name + memoryIndex + "\n\n");
                        break;
                }
                break;
            case REAL:
                switch (scope) {
                    case GLOBAL:
                        bodyText.append("  store double " + textValue + ", double* @var_" + name + memoryIndex + "\n\n");
                }
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
        int memoryIndex = variable.getMemoryIndex();

        int stringLength = string.length();
        String irStringSize = "[" + (stringLength + 2) + " x i8]";
        switch (scope) {
            case GLOBAL:
                if (globalVariables.containsKey(name)) {
                    bodyText.append("  call i32 (i8*, ...) @printf(i8* getelementptr inbounds ( " + irStringSize + ", " + irStringSize + "* @var_" + name + memoryIndex + ", i32 0, i32 0))\n\n");
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
        int memoryIndex = variable.getMemoryIndex();

        switch (scope) {
            case GLOBAL:
                bodyText.append("  %printable" + printableIndex + " = load i32, i32* @var_" + name + memoryIndex + "\n");
                break;
        }
        printableIndex++;

        bodyText.append("  %printable" + printableIndex + " = call i32 (i8* , ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]*" + systemVariables.get("printInt") + ", i32 0, i32 0), i32 %printable" + (printableIndex - 1) + ")\n\n");
        printableIndex++;
    }

    public void printReal(String text) {
        printStr(text);
    }

    public void printReal(Variable variable) {
        VariableScope scope = variable.getScope();
        String name = variable.getName();
        int memoryIndex = variable.getMemoryIndex();

        switch (scope) {
            case GLOBAL:
                bodyText.append("  %printable" + printableIndex + " = load double, double* @var_" + name + memoryIndex + "\n");
                break;
        }
        printableIndex++;

        bodyText.append("  %printable" + printableIndex + " = call i32 (i8* , ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]*" + systemVariables.get("printReal") + ", i32 0, i32 0), double %printable" + (printableIndex - 1) + ")\n\n");
        printableIndex++;
    }

    public void scanInt(Variable variable) {
        VariableScope scope = variable.getScope();
        String name = variable.getName();
        int memoryIndex = variable.getMemoryIndex();

        switch (scope) {
            case GLOBAL:
                bodyText.append("  %scannable" + scannableIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* " + systemVariables.get("scanInt") + ", i32 0, i32 0), i32* @var_" + name + memoryIndex + ")\n\n");
                break;
        }
        scannableIndex++;
    }

    public void scanReal(Variable variable) {
        VariableScope scope = variable.getScope();
        String name = variable.getName();
        int memoryIndex = variable.getMemoryIndex();

        switch (scope) {
            case GLOBAL:
                bodyText.append("  %scannable" + scannableIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* " + systemVariables.get("scanReal") + ", i32 0, i32 0), double* @var_" + name + memoryIndex + ")\n\n");
                break;
        }
        scannableIndex++;
    }

    private Variable cast(Variable variable, VariableType newType) {
        VariableScope scope = variable.getScope();
        VariableType previousType = variable.getType();
        String name = variable.getName();

        String previousValue = variable.getValue().toString();
        Object newValue = null;

        try {
            switch (newType) {
                case INT:
                    newValue = Integer.parseInt(previousValue);
                    break;
                case REAL:
                    newValue = Double.parseDouble(previousValue);
                    break;
                case STR:
                    newValue = previousValue;
                    break;
            }
        } catch(java.lang.NumberFormatException e) {
            actions.printError("cannot parse lady from " + previousType + " to " + newType);
        }

        Variable newVariable = new Variable(scope, newType, name, newValue);

        switch (scope) {
            case GLOBAL:
                globalVariables.put(name, newVariable);
                break;
        }

        return newVariable;
    }
}
