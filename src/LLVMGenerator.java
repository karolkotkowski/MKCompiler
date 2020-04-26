import java.util.HashMap;

public class LLVMGenerator {

    private HashMap<String, Variable> globalVariables;
    private StringBuilder headerText = new StringBuilder();
    private StringBuilder bodyText = new StringBuilder();
    private int printableIndex = 0;
    private int scannableIndex = 0;
    private int varIndex = 1;
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

        String textValue = null;
        if (value.getClass() == ExplicitExpression.class) {
            textValue = "%" + ((ExplicitExpression) value).getName();
        } else {
            if (value.getClass() == Variable.class) {
                switch (type) {
                    case INT:
                        bodyText.append("  %" + varIndex + " = load i32, i32* @var_" + ((Variable) value).getName() + ((Variable) value).getMemoryIndex() + "\n");
                        break;
                    case REAL:
                        bodyText.append("  %" + varIndex + " = load double, double* @var_" + ((Variable) value).getName() + ((Variable) value).getMemoryIndex() + "\n");
                        break;
                }
                textValue = "%" + (varIndex++);
            }
            else
                textValue = value.toString();
        }
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

    public ExplicitExpression calculate(ExplicitExpression leftExpression, CalculationType calculationType, ExplicitExpression rightExpression) {
        leftExpression = makeCalculable(leftExpression);
        rightExpression = makeCalculable(rightExpression);

        VariableType leftType = leftExpression.getType();
        VariableType rightType = rightExpression.getType();

        boolean realCalculation = false;
        if (leftType == VariableType.REAL || rightType == VariableType.REAL || calculationType == CalculationType.DIV) {
            realCalculation = true;
            if (leftType != VariableType.REAL) leftExpression = cast(leftExpression, VariableType.REAL);
            if (rightType != VariableType.REAL) rightExpression = cast(rightExpression, VariableType.REAL);
        }

        String leftIndex = leftExpression.getName();
        String righIndex = rightExpression.getName();

        ExplicitExpression result = null;
        int resultIndex = varIndex;

        if (realCalculation) {
            bodyText.append("  %" + resultIndex + " = alloca double \n");

            bodyText.append("  %" + (varIndex++ + 1) + " = load double, double* %" + leftIndex + "\n");
            bodyText.append("  %" + (varIndex++ + 1) + " = load double, double* %" + righIndex + "\n");

        } else {
            bodyText.append("  %" + resultIndex + " = alloca i32 \n");

            bodyText.append("  %" + (varIndex++ + 1) + " = load i32, i32* %" + leftIndex + "\n");
            bodyText.append("  %" + (varIndex++ + 1) + " = load i32, i32* %" + righIndex + "\n");
        }

        if (realCalculation) {
            switch (calculationType) {
                case ADD:
                    bodyText.append("  %" + (varIndex++ + 1) + " = fadd double %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n\n");
                    break;
                case SUB:
                    bodyText.append("  %" + (varIndex++ + 1) + " = fsub double %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n\n");
                    break;
                case MUL:
                    bodyText.append("  %" + (varIndex++ + 1) + " = fmul double %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n\n");
                    break;
                case DIV:
                    bodyText.append("  %" + (varIndex++ + 1) + " = fdiv double %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n\n");
                    break;
            }
            result = new ExplicitExpression(new String("" + varIndex), VariableType.REAL);
        } else {
            switch (calculationType) {
                case ADD:
                    bodyText.append("  %" + (varIndex++ + 1) + " = add nsw i32 %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n");
                    break;
                case SUB:
                    bodyText.append("  %" + (varIndex++ + 1) + " = sub nsw i32 %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n");
                    break;
                case MUL:
                    bodyText.append("  %" + (varIndex++ + 1) + " = mul nsw i32 %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n");
                    break;
            }
            bodyText.append("  store i32 %" + varIndex + ", i32* %" + resultIndex + "\n");
            bodyText.append("  %" + (varIndex++ + 1) + " = load i32, i32* %" + resultIndex + "\n\n");
            result = new ExplicitExpression(new String("" + varIndex), VariableType.INT);
        }

        varIndex++;
        return result;
    }

    private ExplicitExpression makeCalculable(ExplicitExpression expression) {
        VariableType type = expression.getType();
        Variable variable = expression.getVariable();
        String textValue = null;

        String name = null;
        int memoryIndex = 0;
        if (variable != null) {
            name = variable.getName();
            memoryIndex = variable.getMemoryIndex();
        } else {
            textValue = expression.getValue().toString();
        }

        int resultIndex = varIndex;

        switch(type) {
            case INT:
                bodyText.append("  %" + resultIndex + " = alloca i32 \n");
                if (variable == null) {
                    if (expression.isCalculable()) {
                        bodyText.append("  store i32 %" + expression.getName() + ", i32* %" + resultIndex + "\n\n");
                    } else {
                        bodyText.append("  store i32 " + textValue + ", i32* %" + resultIndex + "\n\n");
                    }
                } else {
                    varIndex++;
                    bodyText.append("  %" + varIndex + " = load i32, i32* @var_" + name + memoryIndex + "\n");
                    bodyText.append("  store i32 %" + varIndex + ", i32* %" + resultIndex + "\n\n");
                }
                break;
            case REAL:
                bodyText.append("  %" + resultIndex + " = alloca double \n");
                if (variable == null) {
                    if (expression.isCalculable()) {
                        bodyText.append("  store double %" + expression.getName() + ", double* %" + resultIndex + "\n\n");
                    } else {
                        bodyText.append("  store double " + textValue + ", double* %" + resultIndex + "\n\n");
                    }
                } else {
                    varIndex++;
                    bodyText.append("  %" + varIndex + " = load double, double* @var_" + name + memoryIndex + "\n");
                    bodyText.append("  store double %" + varIndex + ", double* %" + resultIndex + "\n\n");
                }
                break;
        }
        varIndex++;

        return new ExplicitExpression(new String("" + resultIndex), type);
    }

    private ExplicitExpression cast(ExplicitExpression explicitExpression, VariableType newType) {
        VariableType previousType = explicitExpression.getType();
        String name = explicitExpression.getName();
        String castedIndex = explicitExpression.getName();

        int resultIndex = varIndex++;

        switch (newType) {
            case INT:

                break;
            case REAL:
                bodyText.append("  %" + resultIndex + " = alloca double \n");
                switch (previousType) {
                    case INT:
                        bodyText.append("  %" + (varIndex++) + " = load i32, i32* %" + name + "\n");
                        bodyText.append("  %" + (varIndex++) + " = sitofp i32 %" + (varIndex - 2) + " to double \n");
                        bodyText.append("  store double %" + (varIndex - 1) + ", double* %" + resultIndex + "\n\n");
                        break;
                }
                break;
            case STR:

                break;
        }

        return new ExplicitExpression(("" + resultIndex), newType);
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

    public void scan(Variable variable) {
        VariableScope scope = variable.getScope();
        VariableType type = variable.getType();
        String name = variable.getName();
        int memoryIndex = variable.getMemoryIndex();

        switch (type) {
            case INT:
                switch (scope) {
                    case GLOBAL:
                        bodyText.append("  %scannable" + scannableIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* " + systemVariables.get("scanInt") + ", i32 0, i32 0), i32* @var_" + name + memoryIndex + ")\n\n");
                        break;
                }
                break;
            case REAL:
                switch (scope) {
                    case GLOBAL:
                        bodyText.append("  %scannable" + scannableIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* " + systemVariables.get("scanReal") + ", i32 0, i32 0), double* @var_" + name + memoryIndex + ")\n\n");
                        break;
                }
                break;
        }
        scannableIndex++;
    }


}
