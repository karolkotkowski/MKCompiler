

import java.util.*;

public class LLVMGenerator {

    private HashMap<String, GlobalVarExpression> globalVariables;
    private HashMap<String, Set<String>> localVariables = new HashMap<String, Set<String>>();
    private LLVMBuilder llvm = new LLVMBuilder();
    private int varIndex = 1;
    private LLVMActions actions;
    private GlobalVarExpression currentFunction = null;

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
        text.append(llvm);
        System.out.println(text.toString());
    }

    public void declareFunction(GlobalVarExpression function, List<Expression> arguments) {
        DataType dataType = function.getDataType();
        String name = function.getName();
        int numberOfArguments = function.getIndex();

        currentFunction = function;
        varIndex = 0;

        StringBuilder definition = new StringBuilder();
        definition.append("\ndefine ");
        switch (dataType) {
            case INT:
                definition.append("i32 ");
                break;
            case REAL:
                definition.append("double ");
                break;
        }
        if ("main".equals(name))
            llvm.appendToMainDeclaration(definition);
        else
            llvm.append(definition);
        llvm.holdBuffer();

        localVariables.put(name, new HashSet<String>());
        StringBuilder types = new StringBuilder();
        Iterator<Expression> iterator = arguments.iterator();
        Expression argument;
        DataType argumentType;
        while (iterator.hasNext()) {
            argument = iterator.next();
            argumentType = argument.getDataType();

            declareVariable(argument);
            assignVariable(argument, new UnnamedVarExpression(ObjectType.VARIABLE, argumentType, varIndex));
            varIndex++;
            switch (argumentType) {
                case INT:
                    types.append("i32");
                    break;
                case REAL:
                    types.append("double");
                    break;
            }
            if (iterator.hasNext())
                types.append(", ");
        }
        varIndex++;
        String buffer = llvm.getBuffer();
        llvm.releaseBuffer();
        if (name.equals("main"))
            llvm.appendToMainDeclaration("@main(" + types.toString() + ") nounwind { \n" + buffer);
        else
            llvm.append("@func_" + name + "(" + types.toString() + ") nounwind { \n" + buffer);

    }

    public void doReturning(Expression expression) {
        DataType dataType = currentFunction.getDataType();

        int memoryIndex = varIndex;
        UnnamedVarExpression leftExpression = new UnnamedVarExpression(ObjectType.VARIABLE, expression.getDataType(), memoryIndex);
        varIndex++;
        assignVariable(leftExpression, expression);

        UnnamedVarExpression resultExpression = null;
        if (dataType != expression.getDataType())
            resultExpression = cast(leftExpression, dataType);
        else
            resultExpression = leftExpression;

        llvm.append("  %" + varIndex + " = load ", currentFunction);
        switch (dataType) {
            case INT:
                llvm.append("i32, i32* ", currentFunction);
                llvm.holdBuffer();
                llvm.append("  ret i32 ", currentFunction);
                break;
            case REAL:
                llvm.append("double, double* ", currentFunction);
                llvm.holdBuffer();
                llvm.append("  ret double ", currentFunction);
                break;
        }
        llvm.append("%" + varIndex + "\n", currentFunction);
        String buffer = llvm.getBuffer();
        llvm.releaseBuffer();
        llvm.append("%" + resultExpression.getIndex() + "\n" + buffer, currentFunction);
    }

    public void endFunctionDefinition() {
        llvm.append("} \n", currentFunction);
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
                                llvm.appendToHeader("@var_" + name + index + " = global i32 0\n");
                                break;
                            case REAL:
                                llvm.appendToHeader("@var_" + name + index + " = global double 0.0\n");
                                break;
                            case CHAR:
                                break;
                        }
                    }
                } else if (expressionClass.equals(NamedVarExpression.class)) {
                    name = ((NamedVarExpression) expression).getName();

                    if (currentFunction == null)
                        actions.printError("declaring local variable " + name + " outside a function body");

                    String functionName = currentFunction.getName();
                    Set<String> localNames = localVariables.get(functionName);

                    if (localNames.contains(name))
                        actions.printError("declaring already existing lady " + name + " in function " + functionName + "()");
                    localNames.add(name);

                    llvm.append("  %var_" + name + " = alloca ", currentFunction);
                    switch (dataType) {
                        case INT:
                            llvm.append("i32", currentFunction);
                            break;
                        case REAL:
                            llvm.append("double", currentFunction);
                            break;
                    }
                    llvm.append("\n");

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
                } else if (leftExpressionClass.equals(UnnamedVarExpression.class)) {
                    leftIndex = ((UnnamedVarExpression) leftExpression).getIndex();
                    leftFullName = "%" + leftIndex;
                    switch (rightDataType) {
                        case NONE:
                        case INT:
                            llvm.append("  %" + leftIndex + " = alloca i32 \n", currentFunction);
                            break;
                        case REAL:
                            llvm.append("  %" + leftIndex + " = alloca double \n", currentFunction);
                            break;
                        case CHAR:
                            break;
                    }
                } else if (leftExpressionClass.equals(NamedVarExpression.class)) {
                    leftFullName = "%var_" + ((NamedVarExpression) leftExpression).getName();
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
                        llvm.append("  %" + varIndex++ + " = load i32, i32* %" + leftIndex + "\n", currentFunction);
                        llvm.append("  %" + varIndex++ + " = sext i32 %" + (varIndex - 2) + " to i64 \n", currentFunction);
                        leftIndexStr = "%" + (varIndex - 1);
                        switch (leftDataType) {
                            case INT:
                                llvm.append("  %" + varIndex + " = getelementptr inbounds [" + arrayLength + " x i32], [" + arrayLength + " x i32]* " + arrayName + ", i64 0, i64 " + leftIndexStr + " \n", currentFunction);
                                break;
                            case REAL:
                                llvm.append("  %" + varIndex + " = getelementptr inbounds [" + arrayLength + " x double], [" + arrayLength + " x double]* " + arrayName + ", i64 0, i64 " + leftIndexStr + " \n", currentFunction);
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
                            llvm.append("  %" + varIndex + " = load i32, i32* @var_" + rightName + rightIndex + "\n", currentFunction);
                            llvm.append("  store i32 %" + varIndex + ", i32* " + leftFullName + "\n\n", currentFunction);
                            break;
                        case REAL:
                            llvm.append("  %" + varIndex + " = load double, double* @var_" + rightName + rightIndex + "\n", currentFunction);
                            llvm.append("  store double %" + varIndex + ", double* " + leftFullName + "\n\n", currentFunction);
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
                                llvm.append("  store i32 " + textValue + ", i32* " + leftFullName + "\n\n", currentFunction);
                                break;
                            case REAL:
                                llvm.append("  store double " + textValue + ", double* " + leftFullName + "\n\n", currentFunction);
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
                                    llvm.append("  store i32 %" + rightIndex + ", i32* " + leftFullName + "\n\n", currentFunction);
                                    break;
                                case REAL:
                                    llvm.append("  store double %" + rightIndex + ", double* " + leftFullName + "\n\n", currentFunction);
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
                    llvm.append("  %" + varIndex++ + " = load i32, i32* %" + ((UnnamedVarExpression) arrayIndex).getIndex() + " \n", currentFunction);
                    llvm.append("  %" + varIndex++ + " = sext i32 %" + (varIndex - 2) + " to i64 \n", currentFunction);

                    switch (rightDataType) {
                        case INT:
                            rightFullName = "getelementptr inbounds [" + arrayLength + " x i32], [" + arrayLength + " x i32]* @var_" + rightName + ", i64 0, i64 %" + (varIndex - 1) + "";
                            llvm.append("  %" + varIndex++ + " = " + rightFullName + "\n", currentFunction);
                            llvm.append("  %" + varIndex + " = load i32, i32* %" + (varIndex - 1) + "\n", currentFunction);
                            llvm.append("  store i32 %" + varIndex + ", i32* " + leftFullName + "\n\n", currentFunction);
                            break;
                        case REAL:
                            rightFullName = "getelementptr inbounds [" + arrayLength + " x double], [" + arrayLength + " x double]* @var_" + rightName + ", i64 0, i64 %" + (varIndex - 1) + "";
                            llvm.append("  %" + varIndex++ + " = " + rightFullName + "\n", currentFunction);
                            llvm.append("  %" + varIndex + " = load double, double* %" + (varIndex - 1) + "\n", currentFunction);
                            llvm.append("  store double %" + varIndex + ", double* " + leftFullName + "\n\n", currentFunction);
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
                    llvm.appendToHeader("@var_" + name + " = global [" + length + " x i32] zeroinitializer \n");
                    break;
                case REAL:
                    llvm.appendToHeader("@var_" + name + " = global [" + length + " x double] zeroinitializer \n");
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
            llvm.append("  %" + resultIndex + " = alloca double \n", currentFunction);

            llvm.append("  %" + varIndex++ + " = load double, double* %" + leftIndex + "\n", currentFunction);
            llvm.append("  %" + varIndex++ + " = load double, double* %" + rightIndex + "\n", currentFunction);

        } else {
            llvm.append("  %" + resultIndex + " = alloca i32 \n", currentFunction);

            llvm.append("  %" + varIndex++ + " = load i32, i32* %" + leftIndex + "\n", currentFunction);
            llvm.append("  %" + varIndex++ + " = load i32, i32* %" + rightIndex + "\n", currentFunction);
        }
        varIndex--;

        if (realCalculation) {
            switch (calculationType) {
                case ADD:
                    llvm.append("  %" + (varIndex++ + 1) + " = fadd double %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n\n", currentFunction);
                    break;
                case SUB:
                    llvm.append("  %" + (varIndex++ + 1) + " = fsub double %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n\n", currentFunction);
                    break;
                case MUL:
                    llvm.append("  %" + (varIndex++ + 1) + " = fmul double %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n\n", currentFunction);
                    break;
                case DIV:
                    llvm.append("  %" + (varIndex++ + 1) + " = fdiv double %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n\n", currentFunction);
                    break;
            }
            result = new UnnamedVarExpression(ObjectType.VARIABLE, DataType.REAL, varIndex);
        } else {
            switch (calculationType) {
                case ADD:
                    llvm.append("  %" + (varIndex++ + 1) + " = add nsw i32 %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n", currentFunction);
                    break;
                case SUB:
                    llvm.append("  %" + (varIndex++ + 1) + " = sub nsw i32 %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n", currentFunction);
                    break;
                case MUL:
                    llvm.append("  %" + (varIndex++ + 1) + " = mul nsw i32 %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n", currentFunction);
                    break;
            }
            llvm.append("  store i32 %" + varIndex++ + ", i32* %" + resultIndex + "\n", currentFunction);
            llvm.append("  %" + (varIndex) + " = load i32, i32* %" + resultIndex + "\n\n", currentFunction);
            result = new UnnamedVarExpression(ObjectType.VARIABLE, DataType.INT, varIndex);
        }

        varIndex++;
        return result;
    }

    public UnnamedVarExpression allocate(Expression expression) {
        ObjectType objectType = expression.getObjectType();
        Object expressionClass = expression.getClass();
        DataType dataType = expression.getDataType();
        String arrayName = null;
        int index = 0;
        String fullName = null;
        String textValue = null;

        int resultIndex = varIndex;
        varIndex++;

        switch (dataType) {
            case NONE:
            case INT:
                llvm.append("  %" + resultIndex + " = alloca i32 \n", currentFunction);
                break;
            case REAL:
                llvm.append("  %" + resultIndex + " = alloca double \n", currentFunction);
                break;
            case CHAR:
                break;
        }

        switch (objectType) {
            case VARIABLE:
                if (expressionClass.equals(GlobalVarExpression.class)) {
                    arrayName = ((GlobalVarExpression) expression).getName();
                    index = ((GlobalVarExpression) expression).getIndex();
                    fullName = "@var_" + arrayName + index;
                    textValue = "%" + varIndex;
                    if (!globalVariables.containsKey(arrayName))
                        actions.printError("using non-existing lady " + arrayName);
                    switch (dataType) {
                        case NONE:
                        case INT:
                            llvm.append("  %" + varIndex + " = load i32, i32* " + fullName + "\n", currentFunction);
                            break;
                        case REAL:
                            llvm.append("  %" + varIndex + " = load double, double* " + fullName + "\n", currentFunction);
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
                Expression array = null;
                int arrayLength = 0;
                if (expressionClass.equals(GlobalVarExpression.class)) {
                    arrayName = ((GlobalVarExpression) expression).getName();

                    if (!globalVariables.containsKey(arrayName))
                        actions.printError("using non-existing array lady " + arrayName);

                    array = globalVariables.get(arrayName);
                    arrayLength = ((GlobalVarExpression) array).getIndex();

                    index = ((UnnamedVarExpression) ((GlobalVarExpression) expression).getLength()).getIndex();
                    llvm.append("  %" + varIndex++ + " = load i32, i32* %" + index + "\n", currentFunction);
                    llvm.append("  %" + varIndex++ + " = sext i32 %" + (varIndex - 2) + " to i64 \n", currentFunction);
                    textValue = "%" + (varIndex + 1);

                    switch (dataType) {
                        case NONE:
                        case INT:
                            fullName = "getelementptr inbounds [" + arrayLength + " x i32], [" + arrayLength + " x i32]* @var_" + arrayName + ", i64 0, i64 %" + (varIndex - 1) + "";
                            llvm.append("  %" + varIndex++ + " = " + fullName + "\n", currentFunction);
                            llvm.append("  %" + varIndex + " = load i32, i32* %" + (varIndex - 1) + "\n", currentFunction);
                            break;
                        case REAL:
                            fullName = "getelementptr inbounds [" + arrayLength + " x double], [" + arrayLength + " x double]* @var_" + arrayName + ", i64 0, i64 %" + (varIndex - 1) + "";
                            llvm.append("  %" + varIndex++ + " = " + fullName + "\n", currentFunction);
                            llvm.append("  %" + varIndex + " = load double, double* %" + (varIndex - 1) + "\n", currentFunction);
                            break;
                        case CHAR:
                            break;
                    }
                    varIndex++;
                }
                break;
        }

        switch (dataType) {
            case NONE:
            case INT:
                llvm.append("  store i32 " + textValue + ", i32* %" + resultIndex + "\n\n", currentFunction);
                break;
            case REAL:
                llvm.append("  store double " + textValue + ", double* %" + resultIndex + "\n\n", currentFunction);
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
                llvm.append("  %" + resultIndex + " = alloca i32 \n");
                switch (previousType) {
                    case REAL:
                        llvm.append("  %" + (varIndex++) + " = load double, double* %" + index + "\n", currentFunction);
                        llvm.append("  %" + (varIndex++) + " = fptosi double %" + (varIndex - 2) + " to i32 \n", currentFunction);
                        llvm.append("  store i32 %" + (varIndex - 1) + ", i32* %" + resultIndex + "\n\n", currentFunction);
                        break;
                }
                break;
            case REAL:
                llvm.append("  %" + resultIndex + " = alloca double \n");
                switch (previousType) {
                    case INT:
                        llvm.append("  %" + (varIndex++) + " = load i32, i32* %" + index + "\n", currentFunction);
                        llvm.append("  %" + (varIndex++) + " = sitofp i32 %" + (varIndex - 2) + " to double \n", currentFunction);
                        llvm.append("  store double %" + (varIndex - 1) + ", double* %" + resultIndex + "\n\n", currentFunction);
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
                llvm.append("  %" + varIndex + " = load i32, i32* %" + memoryIndex + "\n", currentFunction);
                varIndex++;
                llvm.append("  %" + varIndex + " = call i32 (i8* , ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]*" + systemVariables.get("printInt") + ", i32 0, i32 0), i32 %" + (varIndex - 1) + ")\n\n", currentFunction);
                break;
            case REAL:
                llvm.append("  %" + varIndex + " = load double, double* %" + memoryIndex + "\n", currentFunction);
                varIndex++;
                llvm.append("  %" + varIndex + " = call i32 (i8* , ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]*" + systemVariables.get("printReal") + ", i32 0, i32 0), double %" + (varIndex - 1) + ")\n\n", currentFunction);
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
                    llvm.append(fullName + " = global i32 0", currentFunction);
                    llvm.append("  %" + varIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* " + systemVariables.get("scanInt") + ", i32 0, i32 0), i32* " + fullName + ")\n\n", currentFunction);
                    break;
                case REAL:
                    llvm.append(fullName + " = global double 0.0", currentFunction);
                    llvm.append("  %" + varIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* " + systemVariables.get("scanReal") + ", i32 0, i32 0), double* " + fullName + ")\n\n", currentFunction);
                    break;
                case CHAR:
                    break;
            }
        }
        varIndex++;
    }


}
