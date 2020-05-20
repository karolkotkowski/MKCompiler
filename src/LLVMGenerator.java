

import java.util.*;

public class LLVMGenerator {
    private final HashMap<String, HashMap<String, NamedVarExpression>> localVariables = new HashMap<>();
    private final LLVMBuilder llvm = new LLVMBuilder();
    private int varIndex = 1;
    private final LLVMActions actions;
    private Method currentFunction = null;
    private int instructionIndex = 1;
    private final Stack<Integer> instructionStack = new Stack<>();
    private Instance currentInstance;
    private Set<String> thisVariablesSet = null;

    private final Configuration configuration = new Configuration();
    private final HashMap<String, String>  systemVariables = configuration.getSystemVariables();

    public void setCurrentInstance(Instance currentInstance) {
        this.currentInstance = currentInstance;
    }

    public LLVMBuilder getBuilder() {return llvm;}

    public LLVMGenerator (LLVMActions actions) {
        this.actions = actions;
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
        text.append(llvm);
        System.out.println(text.toString());
    }

    public void startInstruction(Expression leftExpression, CompareType compareType, Expression rightExpression) {
        llvm.append("  br label %compare" + instructionIndex + "\n\n", currentFunction);
        llvm.append(" compare" + instructionIndex + ":\n", currentFunction);

        leftExpression = allocate(leftExpression);
        rightExpression = allocate(rightExpression);

        DataType leftType = leftExpression.getDataType();
        DataType rightType = rightExpression.getDataType();

        boolean realComparison = false;
        if (leftType == DataType.REAL || rightType == DataType.REAL) {
            realComparison = true;
            if (leftType != DataType.REAL) leftExpression = cast((UnnamedVarExpression) leftExpression, DataType.REAL);
            if (rightType != DataType.REAL) rightExpression = cast((UnnamedVarExpression) rightExpression, DataType.REAL);
        }

        int leftIndex = ((UnnamedVarExpression) leftExpression).getIndex();
        int rightIndex = ((UnnamedVarExpression) rightExpression).getIndex();

        String llvmType;
        String llvmCompare;
        String compareTypeString = compareType.toString();
        if (realComparison) {
            llvmType = "double";
            llvmCompare = "fcmp";
            switch (compareTypeString) {
                case "eq":
                    compareTypeString = "oeq";
                    break;
                case "slt":
                    compareTypeString = "olt";
                    break;
                case "sle":
                    compareTypeString = "ole";
                    break;
                case "sge":
                    compareTypeString = "oge";
                    break;
                case "sgt":
                    compareTypeString = "ogt";
                    break;
                case "ne":
                    compareTypeString = "une";
                    break;
            }
        } else {
            llvmType = "i32";
            llvmCompare = "icmp";
        }

        llvm.append("  %" + varIndex++ + " = load " + llvmType + ", " + llvmType + "* %" + leftIndex + "\n", currentFunction);
        llvm.append("  %" + varIndex++ + " = load " + llvmType + ", " + llvmType + "* %" + rightIndex + "\n", currentFunction);
        llvm.append("  %" + varIndex + " = " + llvmCompare + " " + compareTypeString + " " + llvmType + " %" + (varIndex - 2) + ", %" + (varIndex - 1) + "\n", currentFunction);
        llvm.append("  br i1 %" + varIndex++ + ", label %then" + instructionIndex + ", label %end" + instructionIndex + "\n\n", currentFunction);
        llvm.append(" then" + instructionIndex + ":\n", currentFunction);

        instructionStack.push(instructionIndex++);
    }

    public void endInstruction(InstructionType instructionType) {
        int instructionToEnd = instructionStack.pop();
        String direction = "compare";
        if (instructionType.equals(InstructionType.IF))
            direction = "end";
        llvm.append("  br label %" + direction + instructionToEnd + "\n\n", currentFunction);
        llvm.append(" end" + instructionToEnd + ":\n", currentFunction);
    }

    public void callFunction(Instance instance, Method method, List<Expression> arguments) {
        DataType dataType = method.getDataType();
        String name = method.getName();
        List<DataType> thisArguments = method.getArguments();

        Iterator<Expression> argumentsIterator = arguments.iterator();
        Expression argument;
        DataType argumentType;
        DataType expectedType;
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while (argumentsIterator.hasNext()) {
            argument = argumentsIterator.next();
            argumentType = argument.getDataType();

            expectedType = thisArguments.get(i);
            if (!expectedType.equals(argumentType))
                actions.printError("argument no. " + (i + 1) + " type is " + argumentType + ". Expected: " + expectedType);

            allocate(argument);
            llvm.append("  %" + varIndex + " = load " + argumentType.toLLVM() + ", " + argumentType.toLLVM() + "* %" + (varIndex - 2) + "\n\n", currentFunction);
            buffer.append(argumentType.toLLVM() + " %" + varIndex);
            if (argumentsIterator.hasNext())
                buffer.append(", ");
            varIndex++;
        }

        String fullName = "ret_" + instance.getName() + "_" + name;
        if (!thisVariablesSet.contains(fullName)) {
            llvm.append("  %var_" + fullName + " = alloca " + dataType.toLLVM() + "\n", currentFunction);
            thisVariablesSet.add(fullName);
        }
        llvm.append("  %" + fullName + varIndex + " = call " + dataType.toLLVM() + " @func_" + instance.getName() + "_" + name + "(" + buffer + ")\n", currentFunction);
        llvm.append("  store " + dataType.toLLVM() + " %" + fullName + varIndex + ", " + dataType.toLLVM() + "* %var_" + fullName + "\n\n", currentFunction);
    }

    public void declareFunction(Method method, List<Expression> arguments) {
        DataType dataType = method.getDataType();
        String name = method.getName();
        Instance instance = currentInstance;

        currentFunction = method;
        thisVariablesSet = new HashSet<>();
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

        localVariables.put(name, new HashMap<>());

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
            llvm.append("@func_" + instance.getName() + "_" + name + "(" + types.toString() + ") nounwind { \n" + buffer);

    }

    public void doReturning(Expression expression) {
        DataType dataType = currentFunction.getDataType();

        if (dataType != expression.getDataType())
            actions.printError("types mismatch in give statement");

        int memoryIndex = varIndex;
        UnnamedVarExpression leftExpression = new UnnamedVarExpression(ObjectType.VARIABLE, expression.getDataType(), memoryIndex);
        varIndex++;
        assignVariable(leftExpression, expression);

        UnnamedVarExpression resultExpression;
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
        String name;

        switch (objectType) {
            case VARIABLE:
                if (expressionClass.equals(GlobalVarExpression.class)) {
                    name = ((GlobalVarExpression) expression).getName();
                    Instance instance = currentInstance;

                    switch (dataType) {
                        case INT:
                            llvm.appendToHeader("@var_" + instance.getName() + name + " = global i32 0\n");
                            break;
                        case REAL:
                            llvm.appendToHeader("@var_" + instance.getName() + name + " = global double 0.0\n");
                            break;
                        case CHAR:
                            break;
                    }

                } else if (expressionClass.equals(NamedVarExpression.class)) {
                    name = ((NamedVarExpression) expression).getName();

                    if (currentFunction == null)
                        actions.printError("declaring local variable " + name + " outside a function body");

                    String functionName = currentFunction.getName();
                    Map<String, NamedVarExpression> thisVariables = localVariables.get(functionName);

                    if (thisVariables.containsKey(name))
                        actions.printError("declaring already existing lady " + name + " in function " + functionName + "()");
                    thisVariables.put(name, (NamedVarExpression) expression);

                    llvm.append("  %var_" + name + " = alloca ", currentFunction);
                    switch (dataType) {
                        case INT:
                            llvm.append("i32", currentFunction);
                            break;
                        case REAL:
                            llvm.append("double", currentFunction);
                            break;
                    }
                    llvm.append("\n", currentFunction);

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
        String leftName;
        int leftIndex;
        String leftFullName = null;
        Instance leftInstance;

        Class rightExpressionClass = rightExpression.getClass();
        ObjectType rightObjectType = rightExpression.getObjectType();
        DataType rightDataType = rightExpression.getDataType();
        String rightName;
        int rightIndex;
        String rightFullName;
        String textValue;
        Instance rightInstance;

        switch (leftObjectType) {
            case VARIABLE:
                if (leftExpressionClass.equals(GlobalVarExpression.class)) {
                    leftName = ((GlobalVarExpression) leftExpression).getName();
                    leftInstance = currentInstance;
                    leftFullName = "@var_" + leftInstance.getName() + leftName;
                } else if (leftExpressionClass.equals(UnnamedVarExpression.class)) {
                    leftIndex = ((UnnamedVarExpression) leftExpression).getIndex();
                    leftFullName = "%" + leftIndex;
                    switch (rightDataType) {
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
                    leftName = ((NamedVarExpression) leftExpression).getName();
                    Map<String, NamedVarExpression> thisVariables = localVariables.get(currentFunction.getName());

                    if (thisVariables.containsKey(leftName)) {
                        leftFullName = "%var_" + leftName;
                    } else
                        actions.printError("assigning to non-existing lady " + leftName);

                }
                break;
            case CONSTANT:
                break;
            case ARRAY_ELEMENT:
                if (leftExpressionClass.equals(GlobalVarExpression.class)) {
                    leftName = ((GlobalVarExpression) leftExpression).getName();
                    UnnamedVarExpression indexExpression = ((UnnamedVarExpression) ((GlobalVarExpression) leftExpression).getLength());
                    String leftIndexStr;
                    leftInstance = currentInstance;

                    GlobalVarExpression array = (GlobalVarExpression) leftExpression;
                    String arrayName = "@var_" + leftInstance.getName() + array.getName();
                    int arrayLength = array.getNumberOfArguments(); //using GlobalVarExpression.index to store array length

                    if (array.getDataType() != rightDataType)
                        actions.printError("array element type " + rightDataType + " not matching array type " + array.getDataType());

                    if (indexExpression == null) {
                        leftIndex = ((GlobalVarExpression) leftExpression).getNumberOfArguments();
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
                    rightInstance = currentInstance;

                    switch (rightDataType) {
                        case INT:
                            llvm.append("  %" + varIndex + " = load i32, i32* @var_" + rightInstance.getName() + rightName + "\n", currentFunction);
                            llvm.append("  store i32 %" + varIndex + ", i32* " + leftFullName + "\n\n", currentFunction);
                            break;
                        case REAL:
                            llvm.append("  %" + varIndex + " = load double, double* @var_" + rightInstance.getName() + rightName + "\n", currentFunction);
                            llvm.append("  store double %" + varIndex + ", double* " + leftFullName + "\n\n", currentFunction);
                            break;
                        case CHAR:
                            break;
                    }
                    varIndex++;

                } else if (rightExpressionClass.equals(NamedVarExpression.class)) {
                  rightName = ((NamedVarExpression) rightExpression).getName();
                  Map<String, NamedVarExpression> thisVariables = localVariables.get(currentFunction.getName());

                    switch (rightDataType) {
                        case INT:
                            llvm.append("  %" + varIndex + " = load i32, i32* %var_" + rightName + "\n", currentFunction);
                            llvm.append("  store i32 %" + varIndex + ", i32* " + leftFullName + "\n\n", currentFunction);
                            break;
                        case REAL:
                            llvm.append("  %" + varIndex + " = load double, double* %var_" + rightName + "\n", currentFunction);
                            llvm.append("  store double %" + varIndex + ", double* " + leftFullName + "\n\n", currentFunction);
                            break;
                        case CHAR:
                            break;
                    }
                    varIndex++;

                } else if (rightExpressionClass.equals(ValueExpression.class)) {
                        textValue = ((ValueExpression) rightExpression).getValue().toString();
                        switch (rightDataType) {
                            case INT:
                                llvm.append("  store i32 " + textValue + ", i32* " + leftFullName + "\n\n", currentFunction);
                                break;
                            case REAL:
                                llvm.append("  store double " + textValue + ", double* " + leftFullName + "\n\n", currentFunction);
                                break;
                            case CHAR:
                                break;
                        }
                } else if (rightExpressionClass.equals(UnnamedVarExpression.class)) {
                            rightIndex = ((UnnamedVarExpression) rightExpression).getIndex();
                            switch (rightDataType) {
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
                break;
            case ARRAY_ELEMENT:
                Expression arrayIndex;
                GlobalVarExpression array;
                int arrayLength;
                if (rightExpressionClass.equals(GlobalVarExpression.class)) {
                    rightName = ((GlobalVarExpression) rightExpression).getName();
                    rightInstance = currentInstance;

                    array = (GlobalVarExpression) rightExpression;
                    arrayLength = array.getNumberOfArguments();

                    arrayIndex = ((GlobalVarExpression) rightExpression).getLength();
                    llvm.append("  %" + varIndex++ + " = load i32, i32* %" + ((UnnamedVarExpression) arrayIndex).getIndex() + " \n", currentFunction);
                    llvm.append("  %" + varIndex++ + " = sext i32 %" + (varIndex - 2) + " to i64 \n", currentFunction);

                    switch (rightDataType) {
                        case INT:
                            rightFullName = "getelementptr inbounds [" + arrayLength + " x i32], [" + arrayLength + " x i32]* @var_" + rightInstance.getName() + rightName + ", i64 0, i64 %" + (varIndex - 1) + "";
                            llvm.append("  %" + varIndex++ + " = " + rightFullName + "\n", currentFunction);
                            llvm.append("  %" + varIndex + " = load i32, i32* %" + (varIndex - 1) + "\n", currentFunction);
                            llvm.append("  store i32 %" + varIndex + ", i32* " + leftFullName + "\n\n", currentFunction);
                            break;
                        case REAL:
                            rightFullName = "getelementptr inbounds [" + arrayLength + " x double], [" + arrayLength + " x double]* @var_" + rightInstance.getName() + rightName + ", i64 0, i64 %" + (varIndex - 1) + "";
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
        Instance instance;

        if (expressionClass.equals(GlobalVarExpression.class)) {
            name = ((GlobalVarExpression) array).getName();
            instance = currentInstance;

            switch (dataType) {
                case INT:
                    llvm.appendToHeader("@var_" + instance.getName() + name + " = global [" + length + " x i32] zeroinitializer \n");
                    break;
                case REAL:
                    llvm.appendToHeader("@var_" + instance.getName() + name + " = global [" + length + " x double] zeroinitializer \n");
                    break;
                case CHAR:
                    break;
            }

            Expression leftExpression = null;
            Expression rightExpression;
            Iterator<Expression> expressionIterator = elements.iterator();
            int index = 0;
            while (expressionIterator.hasNext()) {
                rightExpression = expressionIterator.next();
                if (expressionClass.equals(GlobalVarExpression.class))
                    leftExpression = new GlobalVarExpression(ObjectType.ARRAY_ELEMENT, dataType, name, index, currentInstance.getClassy());//new UnnamedVarExpression(ObjectType.VARIABLE, DataType.INT, index));
                assignVariable(leftExpression, rightExpression);
                index++;
            }
        }
    }

    public void calculate(Expression leftExpression, CalculationType calculationType, Expression rightExpression, boolean realCalculation) {
        leftExpression = allocate(leftExpression);
        rightExpression = allocate(rightExpression);

        DataType leftType = leftExpression.getDataType();
        DataType rightType = rightExpression.getDataType();

        if (realCalculation) {
            if (leftType != DataType.REAL) leftExpression = cast((UnnamedVarExpression) leftExpression, DataType.REAL);
            if (rightType != DataType.REAL) rightExpression = cast((UnnamedVarExpression) rightExpression, DataType.REAL);
        }

        int leftIndex = ((UnnamedVarExpression) leftExpression).getIndex();
        int rightIndex = ((UnnamedVarExpression) rightExpression).getIndex();

        UnnamedVarExpression result;
        String resultName;

        if (realCalculation) {
            resultName = "res_real";
            if (!thisVariablesSet.contains(resultName)) {
                llvm.append("  %var_" + resultName + " = alloca double \n", currentFunction);
                thisVariablesSet.add(resultName);
            }

            llvm.append("  %" + varIndex++ + " = load double, double* %" + leftIndex + "\n", currentFunction);
            llvm.append("  %" + varIndex++ + " = load double, double* %" + rightIndex + "\n", currentFunction);

        } else {
            resultName = "res_int";
            if (!thisVariablesSet.contains(resultName)) {
                llvm.append("  %var_" + resultName + " = alloca i32 \n", currentFunction);
                thisVariablesSet.add(resultName);
            }

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
            llvm.append("  store double %" + varIndex + ", double* %var_" + resultName + "\n", currentFunction);
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
            llvm.append("  store i32 %" + varIndex + ", i32* %var_" + resultName + "\n", currentFunction);
        }
        varIndex++;
    }

    public UnnamedVarExpression allocate(Expression expression) {
        ObjectType objectType = expression.getObjectType();
        Object expressionClass = expression.getClass();
        DataType dataType = expression.getDataType();
        String arrayName;
        int index;
        String fullName;
        String textValue = null;

        int resultIndex = varIndex;
        varIndex++;

        switch (dataType) {
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
                    Instance instance = currentInstance;
                    fullName = "@var_" + instance.getName() + arrayName;
                    textValue = "%" + varIndex;

                    switch (dataType) {
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
                } else if (expressionClass.equals(NamedVarExpression.class)) {
                    arrayName = ((NamedVarExpression) expression).getName();
                    fullName = "%var_" + arrayName;
                    textValue = "%" + varIndex;
                    Map<String, NamedVarExpression> thisVariables = localVariables.get(currentFunction.getName());
                    switch (dataType) {
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
                } else if (expressionClass.equals(ValueExpression.class)) {
                    llvm.append("  %" + varIndex++ + " = alloca i32 \n", currentFunction);
                    textValue = ((ValueExpression) expression).getValue().toString();
                } else if (expressionClass.equals(UnnamedVarExpression.class)) {
                    llvm.append("  %" + varIndex++ + " = alloca i32 \n", currentFunction);
                    index = ((UnnamedVarExpression) expression).getIndex();
                    textValue = "%" + index;
                }
                break;
            case CONSTANT:
                break;
            case ARRAY:
                break;
            case ARRAY_ELEMENT:
                GlobalVarExpression array;
                int arrayLength;
                if (expressionClass.equals(GlobalVarExpression.class)) {
                    arrayName = ((GlobalVarExpression) expression).getName();
                    Instance instance = currentInstance;

                    array = (GlobalVarExpression) expression;
                    arrayLength = array.getNumberOfArguments();

                    index = ((UnnamedVarExpression) ((GlobalVarExpression) expression).getLength()).getIndex();
                    llvm.append("  %" + varIndex++ + " = load i32, i32* %" + index + "\n", currentFunction);
                    llvm.append("  %" + varIndex++ + " = sext i32 %" + (varIndex - 2) + " to i64 \n", currentFunction);
                    textValue = "%" + (varIndex + 1);

                    switch (dataType) {
                        case INT:
                            fullName = "getelementptr inbounds [" + arrayLength + " x i32], [" + arrayLength + " x i32]* @var_" + instance.getName() + arrayName + ", i64 0, i64 %" + (varIndex - 1) + "";
                            llvm.append("  %" + varIndex++ + " = " + fullName + "\n", currentFunction);
                            llvm.append("  %" + varIndex + " = load i32, i32* %" + (varIndex - 1) + "\n", currentFunction);
                            break;
                        case REAL:
                            fullName = "getelementptr inbounds [" + arrayLength + " x double], [" + arrayLength + " x double]* @var_" + instance.getName() + arrayName + ", i64 0, i64 %" + (varIndex - 1) + "";
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
                llvm.append("  %" + resultIndex + " = alloca i32 \n", currentFunction);
                switch (previousType) {
                    case REAL:
                        llvm.append("  %" + (varIndex++) + " = load double, double* %" + index + "\n", currentFunction);
                        llvm.append("  %" + (varIndex++) + " = fptosi double %" + (varIndex - 2) + " to i32 \n", currentFunction);
                        llvm.append("  store i32 %" + (varIndex - 1) + ", i32* %" + resultIndex + "\n\n", currentFunction);
                        break;
                }
                break;
            case REAL:
                llvm.append("  %" + resultIndex + " = alloca double \n", currentFunction);
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
        String name;
        String fullName = null;

        if (expressionClass.equals(GlobalVarExpression.class)) {
            name = ((GlobalVarExpression) expression).getName();
            Instance instance = currentInstance;
            fullName = "@var_" + instance.getName() + name;

        } else if (expressionClass.equals(NamedVarExpression.class)) {
            name = ((NamedVarExpression) expression).getName();
            fullName = "%var_" + name;
        }

        switch (dataType) {
            case INT:
                llvm.append("  %" + varIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* " + systemVariables.get("scanInt") + ", i32 0, i32 0), i32* " + fullName + ")\n\n", currentFunction);
                break;
            case REAL:
                llvm.append("  %" + varIndex + " = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* " + systemVariables.get("scanReal") + ", i32 0, i32 0), double* " + fullName + ")\n\n", currentFunction);
                break;
            case CHAR:
                break;
        }

        varIndex++;
    }


}
