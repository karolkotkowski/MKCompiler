

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class LLVMGenerator {

    private HashMap<String, GlobalVarExpression> globalVariables;
    private StringBuilder headerText = new StringBuilder();
    private StringBuilder bodyText = new StringBuilder();
    private int varIndex = 1;
    private LLVMActions actions;

    private Configuration configuration = new Configuration();
    private HashMap<String, String>  systemVariables = configuration.getSystemVariables();

    public LLVMGenerator (LLVMActions actions, HashMap<String, GlobalVarExpression> globalVariables) {
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

    public void declareVariable(Expression expression) {
        Class expressionClass = expression.getClass();
        ObjectType objectType = expression.getObjectType();
        DataType dataType = expression.getDataType();
        String name = null;
        int index = 0;

        switch (objectType) {
            case VARIABLE:
                if (expressionClass.equals(GlobalVarExpression.class)) {
                    name = ((GlobalVarExpression) expression).getName();
                    index = ((GlobalVarExpression) expression).getIndex();

                    if (globalVariables.containsKey(name) && index == 0)
                        actions.printError("declaring already existing lady " + name);
                    globalVariables.put(name, (GlobalVarExpression) expression);

                    if (index > 0 || dataType == DataType.NONE) {
                        switch (dataType) {
                            case NONE:
                            case INT:
                                headerText.append("@var_" + name + index + " = global i32 0\n");
                                break;
                            case REAL:
                                headerText.append("@var_" + name + index + " = global double 0.0\n");
                                break;
                            case CHAR:
                                break;
                        }
                    }
                }
                break;
            case CONSTANT:
                break;
        }
    }

    public void assignVariable(Expression leftExpression, Expression rightExpression) {
        Class leftExpressionClass = leftExpression.getClass();
        ObjectType leftObjectType = leftExpression.getObjectType();
        DataType leftDataType = leftExpression.getDataType();
        String leftName = null;
        int leftIndex = 0;
        String leftFullName = null;

        Class rightExpressionClass = rightExpression.getClass();
        ObjectType rightObjectType = rightExpression.getObjectType();
        DataType rightDataType = rightExpression.getDataType();
        String rightName = null;
        int rightIndex = 0;
        String rightFullName = null;
        String textValue = null;

        switch (leftObjectType) {
            case VARIABLE:
                if (leftExpressionClass.equals(GlobalVarExpression.class)) {
                    leftName = ((GlobalVarExpression) leftExpression).getName();
                    //leftIndex = ((GlobalVarExpression) leftExpression).getIndex();
                    if (globalVariables.containsKey(leftName)) {
                        leftIndex = globalVariables.get(leftName).getIndex();
                        leftIndex++;
                        leftExpression = new GlobalVarExpression(leftObjectType, rightDataType, leftName, leftIndex);
                        declareVariable(leftExpression);
                        leftFullName = "@var_" + leftName + leftIndex;
                    } else
                        actions.printError("assigning to non-existing lady " + leftName);
                } else {
                    if (leftExpressionClass.equals(UnnamedVarExpression.class)) {
                        leftIndex = ((UnnamedVarExpression) leftExpression).getIndex();
                        leftFullName = "%" + leftIndex;
                        switch(rightDataType) {
                            case NONE:
                            case INT:
                                bodyText.append("  %" + leftIndex + " = alloca i32 \n");
                                break;
                            case REAL:
                                bodyText.append("  %" + leftIndex + " = alloca double \n");
                                break;
                            case CHAR:
                                break;
                        }
                    }
                }
                break;
            case CONSTANT:
                break;
            case ARRAY_ELEMENT:
                if (leftExpressionClass.equals(GlobalVarExpression.class)) {
                    leftName = ((GlobalVarExpression) leftExpression).getName();
                    UnnamedVarExpression indexExpression = ((UnnamedVarExpression) ((GlobalVarExpression) leftExpression).getLength());
                    String leftIndexStr;

                    if (!globalVariables.containsKey(leftName))
                        actions.printError("assigning to non-existing array lady " + leftName);

                    GlobalVarExpression array = globalVariables.get(leftName);
                    String arrayName = "@var_" + array.getName();
                    int arrayLength = array.getIndex(); //using GlobalVarExpression.index to store array length

                    if (array.getDataType() != rightDataType)
                        actions.printError("array element type " + rightDataType + " not matching array type " + array.getDataType());

                    if (indexExpression == null) {
                        leftIndex = ((GlobalVarExpression) leftExpression).getIndex();
                        leftIndexStr = "" + leftIndex;
                        switch (leftDataType) {
                            case INT:
                                leftFullName = "getelementptr inbounds ([" + arrayLength + " x i32], [" + arrayLength + " x i32]* " + arrayName + ", i64 0, i64 " + leftIndexStr + ")";
                                break;
                            case REAL:
                                leftFullName = "getelementptr inbounds ([" + arrayLength + " x double], [" + arrayLength + " x double]* " + arrayName + ", i64 0, i64 " + leftIndexStr + ")";
                                break;
                            case CHAR:
                                break;
                        }
                    } else {
                        leftIndex = indexExpression.getIndex();
                        bodyText.append("  %" + varIndex++ + " = load i32, i32* %" + leftIndex + "\n");
                        bodyText.append("  %" + varIndex++ + " = sext i32 %" + (varIndex - 2) + " to i64 \n");
                        leftIndexStr = "%" + (varIndex - 1);
                        switch (leftDataType) {
                            case INT:
                                bodyText.append("  %" + varIndex + " = getelementptr inbounds [" + arrayLength + " x i32], [" + arrayLength + " x i32]* " + arrayName + ", i64 0, i64 " + leftIndexStr + " \n");
                                break;
                            case REAL:
                                bodyText.append("  %" + varIndex + " = getelementptr inbounds [" + arrayLength + " x double], [" + arrayLength + " x double]* " + arrayName + ", i64 0, i64 " + leftIndexStr + " \n");
                                break;
                            case CHAR:
                                break;
                        }
                        leftFullName = "%" + varIndex++;
                    }


                }
                break;
        }

        switch (rightObjectType) {
            case VARIABLE:
                if (rightExpressionClass.equals(GlobalVarExpression.class)) {
                    rightName = ((GlobalVarExpression) rightExpression).getName();
                    rightIndex = ((GlobalVarExpression) rightExpression).getIndex();
                    if (!globalVariables.containsKey(rightName))
                        actions.printError("assigning value from non-existing lady " + rightName);
                    switch (rightDataType) {
                        case NONE:
                        case INT:
                            bodyText.append("  %" + varIndex + " = load i32, i32* @var_" + rightName + rightIndex + "\n");
                            bodyText.append("  store i32 %" + varIndex + ", i32* " + leftFullName + "\n\n");
                            break;
                        case REAL:
                            bodyText.append("  %" + varIndex + " = load double, double* @var_" + rightName + rightIndex + "\n");
                            bodyText.append("  store double %" + varIndex + ", double* " + leftFullName + "\n\n");
                            break;
                        case CHAR:
                            break;
                    }
                    varIndex++;
                } else {
                    if (rightExpressionClass.equals(ValueExpression.class)) {
                        textValue = ((ValueExpression) rightExpression).getValue().toString();
                        switch (rightDataType) {
                            case NONE:
                            case INT:
                                bodyText.append("  store i32 " + textValue + ", i32* " + leftFullName + "\n\n");
                                break;
                            case REAL:
                                bodyText.append("  store double " + textValue + ", double* " + leftFullName + "\n\n");
                                break;
                            case CHAR:
                                break;
                        }
                    } else {
                        if (rightExpressionClass.equals(UnnamedVarExpression.class)) {
                            rightIndex = ((UnnamedVarExpression) rightExpression).getIndex();
                            switch (rightDataType) {
                                case NONE:
                                case INT:
                                    bodyText.append("  store i32 %" + rightIndex + ", i32* " + leftFullName + "\n\n");
                                    break;
                                case REAL:
                                    bodyText.append("  store double %" + rightIndex + ", double* " + leftFullName + "\n\n");
                                    break;
                                case CHAR:
                                    break;
                            }
                        }
                    }
                }
                break;
            case CONSTANT:
                break;
            case ARRAY:
                break;
            case ARRAY_ELEMENT:
                Expression arrayIndex = null;
                Expression array = null;
                int arrayLength = 0;
                if (rightExpressionClass.equals(GlobalVarExpression.class)) {
                    rightName = ((GlobalVarExpression) rightExpression).getName();
                    if (!globalVariables.containsKey(rightName))
                        actions.printError("assigning value from non-existing array lady " + rightName);
                    array = globalVariables.get(rightName);
                    arrayLength = ((GlobalVarExpression) array).getIndex();

                    arrayIndex = ((GlobalVarExpression) rightExpression).getLength();
                    bodyText.append("  %" + varIndex++ + " = load i32, i32* %" + ((UnnamedVarExpression) arrayIndex).getIndex() + " \n");
                    bodyText.append("  %" + varIndex++ + " = sext i32 %" + (varIndex - 2) + " to i64 \n");

                    switch (rightDataType) {
                        case INT:
                            rightFullName = "getelementptr inbounds [" + arrayLength + " x i32], [" + arrayLength + " x i32]* @var_" + rightName + ", i64 0, i64 %" + (varIndex - 1) + "";
                            bodyText.append("  %" + varIndex++ + " = " + rightFullName + "\n");
                            bodyText.append("  %" + varIndex + " = load i32, i32* %" + (varIndex - 1) + "\n");
                            bodyText.append("  store i32 %" + varIndex + ", i32* " + leftFullName + "\n\n");
                            break;
                        case REAL:
                            rightFullName = "getelementptr inbounds [" + arrayLength + " x double], [" + arrayLength + " x double]* @var_" + rightName + ", i64 0, i64 %" + (varIndex - 1) + "";
                            bodyText.append("  %" + varIndex++ + " = " + rightFullName + "\n");
                            bodyText.append("  %" + varIndex + " = load double, double* %" + (varIndex - 1) + "\n");
                            bodyText.append("  store double %" + varIndex + ", double* " + leftFullName + "\n\n");
                            break;
                        case CHAR:
                            break;
                    }
                    varIndex++;
                }
                break;
        }
    }

    public void declareArray(Expression array, int length, List<Expression> elements) {
        Class expressionClass = array.getClass();
        DataType dataType = array.getDataType();
        String name = null;

        if (expressionClass.equals(GlobalVarExpression.class)) {
            name = ((GlobalVarExpression) array).getName();
            if (globalVariables.containsKey(name))
                actions.printError("declaring already existing array lady " + name);
            array = new GlobalVarExpression(ObjectType.ARRAY, dataType, name, length); //using GlobalVarExpression.index to store array length
            globalVariables.put(name, (GlobalVarExpression) array);
            switch (dataType) {
                case INT:
                    headerText.append("@var_" + name + " = global [" + length + " x i32] zeroinitializer \n");
                    break;
                case REAL:
                    headerText.append("@var_" + name + " = global [" + length + " x double] zeroinitializer \n");
                    break;
                case CHAR:
                    break;
            }
        }

        Expression leftExpression = null;
        Expression rightExpression = null;
        Iterator<Expression> expressionIterator = elements.iterator();
        int index = 0;
        while (expressionIterator.hasNext()) {
            rightExpression = expressionIterator.next();
            if (expressionClass.equals(GlobalVarExpression.class))
                leftExpression = new GlobalVarExpression(ObjectType.ARRAY_ELEMENT, dataType, name, index);//new UnnamedVarExpression(ObjectType.VARIABLE, DataType.INT, index));
            assignVariable(leftExpression, rightExpression);
            index++;
        }
    }

    public UnnamedVarExpression calculate(Expression leftExpression, CalculationType calculationType, Expression rightExpression) {
        leftExpression = allocate(leftExpression);
        rightExpression = allocate(rightExpression);

        DataType leftType = leftExpression.getDataType();
        DataType rightType = rightExpression.getDataType();

        boolean realCalculation = false;
        if (leftType == DataType.REAL || rightType == DataType.REAL || calculationType == CalculationType.DIV) {
            realCalculation = true;
            if (leftType != DataType.REAL) leftExpression = cast((UnnamedVarExpression) leftExpression, DataType.REAL);
            if (rightType != DataType.REAL) rightExpression = cast((UnnamedVarExpression) rightExpression, DataType.REAL);
        }

        int leftIndex = ((UnnamedVarExpression) leftExpression).getIndex();
        int rightIndex = ((UnnamedVarExpression) rightExpression).getIndex();

        UnnamedVarExpression result = null;
        int resultIndex = varIndex;
        varIndex++;

        if (realCalculation) {
            bodyText.append("  %" + resultIndex + " = alloca double \n");

            bodyText.append("  %" + varIndex++ + " = load double, double* %" + leftIndex + "\n");
            bodyText.append("  %" + varIndex++ + " = load double, double* %" + rightIndex + "\n");

        } else {
            bodyText.append("  %" + resultIndex + " = alloca i32 \n");

            bodyText.append("  %" + varIndex++ + " = load i32, i32* %" + leftIndex + "\n");
            bodyText.append("  %" + varIndex++ + " = load i32, i32* %" + rightIndex + "\n");
        }
        varIndex--;

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
            result = new UnnamedVarExpression(ObjectType.VARIABLE, DataType.REAL, varIndex);
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
            bodyText.append("  store i32 %" + varIndex++ + ", i32* %" + resultIndex + "\n");
            bodyText.append("  %" + (varIndex) + " = load i32, i32* %" + resultIndex + "\n\n");
            result = new UnnamedVarExpression(ObjectType.VARIABLE, DataType.INT, varIndex);
        }

        varIndex++;
        return result;
    }

    public UnnamedVarExpression allocate(Expression expression) {
        ObjectType objectType = expression.getObjectType();
        Object expressionClass = expression.getClass();
        DataType dataType = expression.getDataType();
        String name = null;
        int index = 0;
        String fullName = null;
        String textValue = null;

        int resultIndex = varIndex;
        varIndex++;

        switch (dataType) {
            case NONE:
            case INT:
                bodyText.append("  %" + resultIndex + " = alloca i32 \n");
                break;
            case REAL:
                bodyText.append("  %" + resultIndex + " = alloca double \n");
                break;
            case CHAR:
                break;
        }

        switch (objectType) {
            case VARIABLE:
                if (expressionClass.equals(GlobalVarExpression.class)) {
                    name = ((GlobalVarExpression) expression).getName();
                    index = ((GlobalVarExpression) expression).getIndex();
                    fullName = "@var_" + name + index;
                    textValue = "%" + varIndex;
                    if (!globalVariables.containsKey(name))
                        actions.printError("using non-existing lady " + name);
                    switch (dataType) {
                        case NONE:
                        case INT:
                            bodyText.append("  %" + varIndex + " = load i32, i32* " + fullName + "\n");
                            break;
                        case REAL:
                            bodyText.append("  %" + varIndex + " = load double, double* " + fullName + "\n");
                            break;
                        case CHAR:
                            break;
                    }
                    varIndex++;
                } else {
                    if (expressionClass.equals(ValueExpression.class)) {
                        textValue = ((ValueExpression) expression).getValue().toString();
                    } else {
                        if (expressionClass.equals(UnnamedVarExpression.class)) {
                            index = ((UnnamedVarExpression) expression).getIndex();
                            textValue = "%" + index;
                        }
                    }
                }
                break;
            case CONSTANT:
                break;
            case ARRAY:
                break;
            case ARRAY_ELEMENT:
                break;
        }

        switch (dataType) {
            case NONE:
            case INT:
                bodyText.append("  store i32 " + textValue + ", i32* %" + resultIndex + "\n\n");
                break;
            case REAL:
                bodyText.append("  store double " + textValue + ", double* %" + resultIndex + "\n\n");
                break;
            case CHAR:
                break;
        }

        return new UnnamedVarExpression(ObjectType.VARIABLE, dataType, resultIndex);
    }

    private UnnamedVarExpression cast(UnnamedVarExpression expression, DataType newType) {
        DataType previousType = expression.getDataType();
        int index = expression.getIndex();

        int resultIndex = varIndex++;

        switch (newType) {
            case INT:

                break;
            case REAL:
                bodyText.append("  %" + resultIndex + " = alloca double \n");
                switch (previousType) {
                    case INT:
                        bodyText.append("  %" + (varIndex++) + " = load i32, i32* %" + index + "\n");
                        bodyText.append("  %" + (varIndex++) + " = sitofp i32 %" + (varIndex - 2) + " to double \n");
                        bodyText.append("  store double %" + (varIndex - 1) + ", double* %" + resultIndex + "\n\n");
                        break;
                }
                break;
        }

        return new UnnamedVarExpression(ObjectType.VARIABLE, newType, resultIndex);
    }

    public void print(Expression expression) {
        DataType dataType = expression.getDataType();

        int memoryIndex = varIndex;
        UnnamedVarExpression leftExpression = new UnnamedVarExpression(ObjectType.VARIABLE, dataType, memoryIndex);
        varIndex++;
        assignVariable(leftExpression, expression);

        switch (dataType) {
            case NONE:
            case INT:
                bodyText.append("  %" + varIndex + " = load i32, i32* %" + memoryIndex + "\n");
                varIndex++;
                bodyText.append("  %" + varIndex + " = call i32 (i8* , ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]*" + systemVariables.get("printInt") + ", i32 0, i32 0), i32 %" + (varIndex - 1) + ")\n\n");
                break;
            case REAL:
                bodyText.append("  %" + varIndex + " = load double, double* %" + memoryIndex + "\n");
                varIndex++;
                bodyText.append("  %" + varIndex + " = call i32 (i8* , ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]*" + systemVariables.get("printReal") + ", i32 0, i32 0), double %" + (varIndex - 1) + ")\n\n");
                break;
            case CHAR:
                break;
        }
        varIndex++;
    }

    /*public void printStr(String string) {
        int stringLength = string.length();
        String irStringSize = "[" + (stringLength + 2) + " x i8]";
        headerText.append(systemVariables.get("printValue") + printableIndex + " = constant" + irStringSize + " c\"" + string + "\\0A\\00\"\n");
        bodyText.append("  call i32 (i8*, ...) @printf(i8* getelementptr inbounds ( " + irStringSize + ", " + irStringSize + "* " + systemVariables.get("printValue") + printableIndex + ", i32 0, i32 0))\n\n");
        printableIndex++;
    }
    */

    public void scan(DataType dataType, Expression expression) {
        Object expressionClass = expression.getClass();

        if (expressionClass.equals(GlobalVarExpression.class)) {
            String name = ((GlobalVarExpression) expression).getName();
            int index = ((GlobalVarExpression) expression).getIndex() + 1;
            String fullName = "@var_" + name + index;
            GlobalVarExpression globalVarExpression = new GlobalVarExpression(ObjectType.VARIABLE, dataType, name, index);
            globalVariables.put(name, globalVarExpression);

            switch (dataType) {
                case INT:
                    headerText.append(fullName + " = global i32 0");
                    bodyText.append("  %" + varIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* " + systemVariables.get("scanInt") + ", i32 0, i32 0), i32* " + fullName + ")\n\n");
                    break;
                case REAL:
                    headerText.append(fullName + " = global double 0.0");
                    bodyText.append("  %" + varIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* " + systemVariables.get("scanReal") + ", i32 0, i32 0), double* " + fullName + ")\n\n");
                    break;
                case CHAR:
                    break;
            }
        }
        varIndex++;
    }


}
