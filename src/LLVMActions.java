import java.util.*;

public class LLVMActions extends MKBaseListener {
    private final LLVMGenerator generator = new LLVMGenerator(this);
    private final Stack<Expression> expressionStack = new Stack<>();
    private int line = 0;
    private final String fileName;
    private boolean inFunction = false; //flag for checking if currently in function body
    private boolean returning = false;  //flag for checking if a return statement occurred in a function
    private final Map<String, Classy> classies = new HashMap<>();
    private Classy currentClassy;
    private final Map<String, Instance> instances = new HashMap<>();
    private Map<String, NamedVarExpression> localVariables;

    public LLVMActions(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void exitInstance_declaration(MKParser.Instance_declarationContext context) {
        line = context.getStart().getLine();
        String instanceName = context.NAME(0).getText();
        String classyName = context.NAME(1).getText();

        if (instances.containsKey(instanceName))
            printError("instance with name " + instanceName + " already exists");
        if (!classies.containsKey(classyName))
            printError("trying to create an instance of non-existing classy " + classyName);
        if (classies.containsKey(instanceName))
            printError("trying to hide classy " + instanceName);
        if ("Main".equals(instanceName) || "main".equals(instanceName))
            printError("invalid instance name: " + instanceName);

        declareInstance(instanceName, classyName);
    }

    private void declareInstance(String instanceName, String classyName) {
        Classy classy = classies.get(classyName);
        Instance instance = new Instance(instanceName, classy);
        instances.put(instanceName, instance);

        generator.getBuilder().setInInstanceDeclaration(true);
        generator.setCurrentInstance(instance);

        Iterator<GeneratorMethod> iterator = classy.getGeneratorMethods().iterator();
        GeneratorMethod generatorMethod;
        GeneratorMethodType type;
        List<Object> arguments;
        while (iterator.hasNext()) {
            generatorMethod = iterator.next();
            type = generatorMethod.getType();
            arguments = generatorMethod.getArguments();
            switch (type) {
                case START_INSTRUCTION:
                    generator.startInstruction((Expression) arguments.get(0), (CompareType) arguments.get(1), (Expression) arguments.get(2));
                    break;
                case END_INSTRUCTION:
                    generator.endInstruction((InstructionType) arguments.get(0));
                    break;
                case CALL_FUNCTION:
                    generator.callFunction((Instance) arguments.get(0), (Method) arguments.get(1), (List<Expression>) arguments.get(2));
                    break;
                case DECLARE_FUNCTION:
                    generator.declareFunction((Method) arguments.get(0), (List<Expression>) arguments.get(1));
                    break;
                case END_FUNCTION_DEFINITION:
                    generator.endFunctionDefinition();
                    break;
                case DO_RETURNING:
                    generator.doReturning((Expression) arguments.get(0));
                    break;
                case CALCULATION:
                    generator.calculate((Expression) arguments.get(0), (CalculationType) arguments.get(1), (Expression) arguments.get(2), (boolean) arguments.get(3));
                    break;
                case DECLARE_VARIABLE:
                    generator.declareVariable((Expression) arguments.get(0));
                    break;
                case ASSIGN_VARIABLE:
                    generator.assignVariable((Expression) arguments.get(0), (Expression) arguments.get(1));
                    break;
                case PRINT:
                    generator.print((Expression) arguments.get(0));
                    break;
                case SCAN:
                    generator.scan((DataType) arguments.get(0), (Expression) arguments.get(1));
                    break;
                case DECLARE_ARRAY:
                    generator.declareArray((Expression) arguments.get(0), (int) arguments.get(1), (List<Expression>) arguments.get(2));
                    break;
            }
        }

        generator.getBuilder().setInInstanceDeclaration(false);

        if (classy.hasMethod("initialize")) {
            GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.CALL_FUNCTION, instance, classy.getMethod("initialize"), new ArrayList<>());
            currentClassy.addGeneratorMethod(gm);
        }
    }

    @Override
    public void exitClass_declaration(MKParser.Class_declarationContext context) {
        //line = context.getStop().getLine();

        if ("Main".equals(currentClassy.getName())) {
            declareInstance("main", "Main");
        }

        currentClassy = null;
    }

    @Override
    public void exitClass_declaration1(MKParser.Class_declaration1Context context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        if (classies.containsKey(name))
            printError("declaring already existing classy " + name);
        Classy classy = new Classy(name);
        classies.put(name, classy);
        currentClassy = classy;
    }

    @Override
    public void exitInstruction1(MKParser.Instruction1Context context) {
        line = context.getStart().getLine();
        Expression rightExpression = expressionStack.pop();
        Expression leftExpression = expressionStack.pop();
        String compareStatement = context.COMPARESTATEMENT().toString();

        if (!inFunction)
            printError("instruction not allowed outside a function body");

        CompareType compareType;
        switch (compareStatement) {
            case "==":
                compareType = CompareType.EQ;
                break;
            case "<":
                compareType = CompareType.SLT;
                break;
            case "<=":
                compareType = CompareType.SLE;
                break;
            case ">=":
                compareType = CompareType.SGE;
                break;
            case ">":
                compareType = CompareType.SGT;
                break;
            case "!=":
                compareType = CompareType.NE;
                break;
            default:
                compareType = null;
        }

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.START_INSTRUCTION, leftExpression, compareType, rightExpression);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitIf_instruction(MKParser.If_instructionContext context) {
        line = context.getStart().getLine();
        InstructionType instructionType = InstructionType.IF;

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.END_INSTRUCTION, instructionType);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitWhile_instruction(MKParser.While_instructionContext context) {
        line = context.getStart().getLine();
        InstructionType instructionType = InstructionType.WHILE;

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.END_INSTRUCTION, instructionType);
        currentClassy.addGeneratorMethod(gm);
    }

    private int countArguments(int childCount) {
        int argumentsCount = 0;
        if (childCount > 2)
            argumentsCount = (int) (childCount * 0.5 - 0.5);
        return argumentsCount;
    }

    private NamedVarExpression callFunction(String instanceName, String methodName, MKParser.Call_argumentsContext argumentsContext) {
        if (!inFunction)
            printError("calling a method not allowed outside a method body");
        if ("main".equals(methodName))
            printError("calling main() method not allowed");
        if (!instances.containsKey(instanceName))
            printError("instance " + instanceName + " does not exist");

        Instance instance = instances.get(instanceName);
        Classy classy = instance.getClassy();
        if (!classy.hasMethod(methodName))
            printError("calling non-existing method " + instanceName + "." + methodName + "()");

        Method method = classy.getMethod(methodName);
        List<DataType> methodArguments = method.getArguments();

        int expectedArgumentsCount = methodArguments.size();
        int argumentsCount = countArguments(argumentsContext.getChildCount());
        if (expectedArgumentsCount != argumentsCount)
            printError("method " + instanceName + "." + methodName + "() called with " + argumentsCount + " argument(s). Expected: " + expectedArgumentsCount);

        List<Expression> arguments = new ArrayList<>(argumentsCount);
        for (int i = 0; i < argumentsCount; i++) {
            arguments.add(expressionStack.pop());
        }
        Collections.reverse(arguments);

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.CALL_FUNCTION, instance, method, arguments);
        currentClassy.addGeneratorMethod(gm);
        return new NamedVarExpression(ObjectType.VARIABLE, method.getDataType(), "ret_" + instanceName + "_" + methodName);
    }

    @Override
    public void exitFunction(MKParser.FunctionContext context) {
        line = context.getStart().getLine();
        String instanceName = context.NAME(0).getText();
        String methodName = context.NAME(1).getText();
        MKParser.Call_argumentsContext argumentsContext = context.getChild(MKParser.Call_argumentsContext.class, 0);

        NamedVarExpression functionValue = callFunction(instanceName, methodName, argumentsContext);
        expressionStack.push(functionValue);
    }

    @Override
    public void exitFunction_call(MKParser.Function_callContext context) {
        line = context.getStart().getLine();
        String instanceName = context.NAME(0).getText();
        String methodName = context.NAME(1).getText();
        MKParser.Call_argumentsContext argumentsContext = context.getChild(MKParser.Call_argumentsContext.class, 0);

        callFunction(instanceName, methodName, argumentsContext);
    }

    @Override
    public void exitInt_argument(MKParser.Int_argumentContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        NamedVarExpression argument = new NamedVarExpression(ObjectType.VARIABLE, DataType.INT, name);
        expressionStack.push(argument);
    }

    @Override
    public void exitReal_argument(MKParser.Real_argumentContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        NamedVarExpression argument = new NamedVarExpression(ObjectType.VARIABLE, DataType.REAL, name);
        expressionStack.push(argument);
    }

    @Override
    public void exitInt_function_declaration(MKParser.Int_function_declarationContext context) {
        line = context.getStart().getLine();
        MKParser.Function_declaration2Context context2 = context.getChild(MKParser.Function_declaration2Context.class, 0);

        declareFunction(DataType.INT, context2);
    }

    @Override
    public void exitReal_function_declaration(MKParser.Real_function_declarationContext context) {
        line = context.getStart().getLine();
        MKParser.Function_declaration2Context context2 = context.getChild(MKParser.Function_declaration2Context.class, 0);

        declareFunction(DataType.REAL, context2);
    }

    private void declareFunction(DataType dataType, MKParser.Function_declaration2Context context2) {
        String name = context2.NAME().getText();
        MKParser.ArgumentsContext context3 = context2.getChild(MKParser.ArgumentsContext.class, 0);
        int childCount = context3.getChildCount();
        localVariables = new HashMap<>();

        int argumentsCount = countArguments(childCount);

        if (inFunction)
            printError("defining method not allowed inside another method");
        if (currentClassy.hasMethod(name))
            printError("defining already existing method " + name + "()");

        List<NamedVarExpression> arguments = new ArrayList<>(argumentsCount);
        List<DataType> argumentsTypes = new ArrayList<>(argumentsCount);
        Expression expression;
        for (int i = 0; i < argumentsCount; i++) {
            expression = expressionStack.pop();
            arguments.add((NamedVarExpression) expression);
            argumentsTypes.add(expression.getDataType());
            localVariables.put(((NamedVarExpression) expression).getName(), (NamedVarExpression) expression);
        }
        Collections.reverse(arguments);
        Collections.reverse(argumentsTypes);

        inFunction = true;
        returning = false;
        Method method = new Method(name, dataType, argumentsTypes);//GlobalVarExpression(ObjectType.FUNCTION, dataType, name, argumentsCount, currentClassy);
        currentClassy.addMethod(name, method);

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.DECLARE_FUNCTION, method, arguments);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitFunction_declaration(MKParser.Function_declarationContext context) {
        line = context.getStart().getLine();
        if (!returning)
            printError("missing give statement in method body");
        inFunction = false;
        returning = false;

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.END_FUNCTION_DEFINITION, null);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitReturning(MKParser.ReturningContext context) {
        line = context.getStart().getLine();
        Expression expression = expressionStack.pop();

        if (!inFunction)
            printError("give statement used while not in method body");
        returning = true;

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.DO_RETURNING, expression);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitAdd(MKParser.AddContext context) {
        calculate(CalculationType.ADD);
    }

    @Override
    public void exitMultiply(MKParser.MultiplyContext context) {
        calculate(CalculationType.MUL);
    }

    @Override
    public void exitDivide(MKParser.DivideContext context) {
        calculate(CalculationType.DIV);
    }

    @Override
    public void exitSubtract(MKParser.SubtractContext context) {
        calculate(CalculationType.SUB);
    }

    private void calculate(CalculationType calculationType) {
        Expression rightExpression = expressionStack.pop();
        DataType rightType = rightExpression.getDataType();
        Expression leftExpression = expressionStack.pop();
        DataType leftType = leftExpression.getDataType();

        boolean realCalculation = false;
        String resultName = "res_int";
        DataType dataType = DataType.INT;
        if (leftType == DataType.REAL || rightType == DataType.REAL || calculationType == CalculationType.DIV) {
            realCalculation = true;
            resultName = "res_real";
            dataType = DataType.REAL;
        }

        expressionStack.push(new NamedVarExpression(ObjectType.VARIABLE, dataType, resultName));

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.CALCULATION, leftExpression, calculationType, rightExpression, realCalculation);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitInt(MKParser.IntContext context) {
        line = context.getStart().getLine();

        Integer value = Integer.parseInt(context.INT().getText());

        ValueExpression expression = new ValueExpression(ObjectType.VARIABLE, DataType.INT, value);
        expressionStack.push(expression);
    }

    @Override
    public void exitReal(MKParser.RealContext context) {
        line = context.getStart().getLine();

        Double value = Double.parseDouble(context.REAL().getText());

        ValueExpression expression = new ValueExpression(ObjectType.VARIABLE, DataType.REAL, value);
        expressionStack.push(expression);
    }

    /*@Override
    public void exitStr(MKParser.StrContext context) {
        line = context.getStart().getLine();

        String value = context.STR().getText();

        ValueExpression expression = new ValueExpression(ObjectType.VARIABLE, DataType.STR, value);
        expressionStack.push(expression);
    }*/

    @Override
    public void exitVariable(MKParser.VariableContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        Expression expression = null;
        if (!localVariables.containsKey(name)) {
            if (currentClassy.hasField(name)) {
                expression = currentClassy.getField(name);
            } else {

                    printError("using non-existing lady " + name);
            }
        } else
            expression = localVariables.get(name);

        expressionStack.push(expression);
    }

    public void exitArray_index(MKParser.Array_indexContext context) {
        line = context.getStart().getLine();

        Expression index = expressionStack.pop();
        if (index.getDataType() != DataType.INT)
            printError("array index or length must be an integer");
        index = generator.allocate(index);
        expressionStack.push(index);
    }

    @Override
    public void exitArray_element1(MKParser.Array_element1Context context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();
        Expression index = expressionStack.pop();

        if (currentClassy.hasField(name)) {
            GlobalVarExpression array = currentClassy.getField(name);
            expressionStack.push(new GlobalVarExpression(ObjectType.ARRAY_ELEMENT, array.getDataType(), name, index, currentClassy));
        } else
            printError("using non-existing array lady " + name);
    }

    @Override
    public void exitInt_variable_declaration(MKParser.Int_variable_declarationContext context) {
        declareVariable(DataType.INT, context.getChild(MKParser.Variable_declaration1Context.class, 0));
    }

    @Override
    public void exitReal_variable_declaration(MKParser.Real_variable_declarationContext context) {
        declareVariable(DataType.REAL, context.getChild(MKParser.Variable_declaration1Context.class, 0));
    }

    private void declareVariable(DataType dataType, MKParser.Variable_declaration1Context context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();
        ObjectType objectType = ObjectType.VARIABLE;
        Expression leftExpression;

        if (inFunction) {
            if (localVariables.containsKey(name))
                printError("declaring already existing variable " + name);
            leftExpression = new NamedVarExpression(objectType, dataType, name);
            localVariables.put(name, (NamedVarExpression) leftExpression);
        } else {
            if (currentClassy.hasField(name))
                printError("declaring already existing variable " + name);
            leftExpression = new GlobalVarExpression(objectType, dataType, name, currentClassy);
            currentClassy.addField((GlobalVarExpression) leftExpression);
        }

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.DECLARE_VARIABLE, leftExpression);
        currentClassy.addGeneratorMethod(gm);

        if (inFunction) {
            switch (dataType) {
                case INT:
                    gm = new GeneratorMethod(GeneratorMethodType.ASSIGN_VARIABLE, leftExpression, new ValueExpression(ObjectType.VARIABLE, dataType, 0));
                    currentClassy.addGeneratorMethod(gm);
                    break;
                case REAL:
                    gm = new GeneratorMethod(GeneratorMethodType.ASSIGN_VARIABLE, leftExpression, new ValueExpression(ObjectType.VARIABLE, dataType, 0.0));
                    currentClassy.addGeneratorMethod(gm);
                    break;
            }
        }
    }

    @Override
    public void exitVariable_assignment(MKParser.Variable_assignmentContext context) {
        line = context.getStart().getLine();

        if (!inFunction)
            printError("assigning to a variable not allowed outside a method body");

        String name = context.NAME().getText();
        Expression leftExpression = null;
        if (!localVariables.containsKey(name)) {
            if (currentClassy.hasField(name))
                leftExpression = currentClassy.getField(name);
            else
                printError("trying to assign to non-existing lady " + name);
        } else
            leftExpression = localVariables.get(name);

        Expression rightExpression = expressionStack.pop();

        if (leftExpression.getDataType() != rightExpression.getDataType())
            printError("types mismatch");

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.ASSIGN_VARIABLE, leftExpression, rightExpression);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitPrint(MKParser.PrintContext context) {
        line = context.getStart().getLine();

        if (!inFunction)
            printError("whispering not allowed outside a function body");

        Expression expression = expressionStack.pop();

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.PRINT, expression);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitScan(MKParser.ScanContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        if (!inFunction)
            printError("hearing not allowed outside a function body");

        Expression expression = null;
        if (!localVariables.containsKey(name)) {
            if (currentClassy.hasField(name)) {
                expression = currentClassy.getField(name);
            } else {
                printError("using non-existing lady " + name);
            }
        } else
            expression = localVariables.get(name);

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.SCAN, expression.getDataType(), expression);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitInt_array_declaration(MKParser.Int_array_declarationContext context) {
        line = context.getStart().getLine();
        DataType dataType = DataType.INT;
        MKParser.Array_declaration1Context context1 = context.getChild(MKParser.Array_declaration1Context.class, 0);

        declareArray(context1, dataType);
    }

    @Override
    public void exitReal_array_declaration(MKParser.Real_array_declarationContext context) {
        line = context.getStart().getLine();
        DataType dataType = DataType.REAL;
        MKParser.Array_declaration1Context context1 = context.getChild(MKParser.Array_declaration1Context.class, 0);

        declareArray(context1, dataType);
    }

    private void declareArray(MKParser.Array_declaration1Context context, DataType dataType) {
        String name = context.NAME().getText();
        int arrayLength = 0;

        if (context.getChild(MKParser.Array_lengthContext.class, 0) != null)
            arrayLength = Integer.parseInt(context.getChild(MKParser.Array_lengthContext.class, 0).INT().getText());

        List<Expression> elements = new ArrayList<>(arrayLength);
        if (context.ASSIGN() != null) {
            int elementCount = 0;
            while (!expressionStack.empty()) {
                elements.add(expressionStack.pop());
                elementCount++;
            }
            Collections.reverse(elements);
            if (arrayLength == 0)
                arrayLength = elementCount;
            else if (elementCount > arrayLength)
                printError("assigning more elements to an array lady than declared");
        }

        GlobalVarExpression array = new GlobalVarExpression(ObjectType.ARRAY, dataType, name, 0, currentClassy);
        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.DECLARE_ARRAY, array, arrayLength, elements);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitArray_assignment(MKParser.Array_assignmentContext context) {
        line = context.getStart().getLine();

        Expression rightExpression = expressionStack.pop();
        Expression leftExpression = expressionStack.pop();

        GeneratorMethod gm = new GeneratorMethod(GeneratorMethodType.ASSIGN_VARIABLE, leftExpression, rightExpression);
        currentClassy.addGeneratorMethod(gm);
    }

    @Override
    public void exitFile(MKParser.FileContext context) {
        line = context.getStart().getLine();

        generator.generateOutput();
    }


    public void printError(String message) {
        System.err.println("Compilation error at line " + line + " - " + message + " in " + fileName);
        System.exit(1);
    }
}

