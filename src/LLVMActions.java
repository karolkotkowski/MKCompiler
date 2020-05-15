import java.util.*;

public class LLVMActions extends MKBaseListener {
    private final HashMap<String, GlobalVarExpression> globalVariables = new HashMap<>();
    private final LLVMGenerator generator = new LLVMGenerator(this, globalVariables);
    private final Stack<Expression> expressionStack = new Stack<>();
    private int line = 0;
    private final String fileName;
    private boolean inFunction = false; //flag for checking if currently in function body
    private boolean returning = false;  //flag for checking if a return statement occurred in a function
    private final HashMap<String, GlobalVarExpression> functions = new HashMap<>();

    public LLVMActions(String fileName) {
        this.fileName = fileName;
    }

    private int countArguments(int childCount) {
        int argumentsCount = 0;
        if (childCount > 2)
            argumentsCount = (int) (childCount * 0.5 - 0.5);
        return argumentsCount;
    }

    private UnnamedVarExpression callFunction(String name, MKParser.Call_argumentsContext argumentsContext) {
        if (!functions.containsKey(name))
            printError("calling non-existing function " + name + "()");
        if ("main".equals(name))
            printError("calling main function not allowed");
        GlobalVarExpression function = functions.get(name);

        int expectedArgumentsCount = function.getIndex();
        int argumentsCount = countArguments(argumentsContext.getChildCount());
        if (expectedArgumentsCount != argumentsCount)
            printError("function " + name + "() called with " + argumentsCount + " argument(s). Expected: " + expectedArgumentsCount);

        List<Expression> arguments = new ArrayList<>(argumentsCount);
        for (int i = 0; i < argumentsCount; i++) {
            arguments.add(expressionStack.pop());
        }
        Collections.reverse(arguments);

        return generator.callFunction(function, arguments);
    }

    @Override
    public void exitFunction(MKParser.FunctionContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();
        MKParser.Call_argumentsContext argumentsContext = context.getChild(MKParser.Call_argumentsContext.class, 0);

        UnnamedVarExpression functionValue = callFunction(name, argumentsContext);
        expressionStack.push(functionValue);
    }

    @Override
    public void exitFunction_call(MKParser.Function_callContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();
        MKParser.Call_argumentsContext argumentsContext = context.getChild(MKParser.Call_argumentsContext.class, 0);

        callFunction(name, argumentsContext);
    }

    @Override
    public void exitInt_argument(MKParser.Int_argumentContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        NamedVarExpression argument = new NamedVarExpression(ObjectType.VARIABLE, DataType.INT, name, 0);
        expressionStack.push(argument);
    }

    @Override
    public void exitReal_argument(MKParser.Real_argumentContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        NamedVarExpression argument = new NamedVarExpression(ObjectType.VARIABLE, DataType.REAL, name, 0);
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

        int argumentsCount = countArguments(childCount);

        if (inFunction)
            printError("defining function not allowed inside another function");
        if (functions.containsKey(name))
            printError("defining already existing function" + name + "()");

        List<Expression> arguments = new ArrayList<>(argumentsCount);
        for (int i = 0; i < argumentsCount; i++) {
            arguments.add(expressionStack.pop());
        }
        Collections.reverse(arguments);

        inFunction = true;
        returning = false;
        GlobalVarExpression function = new GlobalVarExpression(ObjectType.FUNCTION, dataType, name, argumentsCount);
        functions.put(name, function);
        generator.declareFunction(function, arguments);
    }

    @Override
    public void exitFunction_declaration(MKParser.Function_declarationContext context) {
        line = context.getStart().getLine();
        if (!returning)
            printError("missing give statement in function body");
        inFunction = false;
        returning = false;
        generator.endFunctionDefinition();
    }

    @Override
    public void exitReturning(MKParser.ReturningContext context) {
        line = context.getStart().getLine();
        Expression expression = expressionStack.pop();

        if (!inFunction)
            printError("give statement used while not in function body");
        returning = true;

        generator.doReturning(expression);
    }

    @Override
    public void exitAdd(MKParser.AddContext context) {
        int line = context.getStart().getLine();

        calculate(CalculationType.ADD);
    }

    @Override
    public void exitMultiply(MKParser.MultiplyContext context) {
        int line = context.getStart().getLine();

        calculate(CalculationType.MUL);
    }

    @Override
    public void exitDivide(MKParser.DivideContext context) {
        int line = context.getStart().getLine();

        calculate(CalculationType.DIV);
    }

    @Override
    public void exitSubtract(MKParser.SubtractContext context) {
        int line = context.getStart().getLine();

        calculate(CalculationType.SUB);
    }

    private void calculate(CalculationType calculationType) {
        Expression rightExpression = expressionStack.pop();
        Expression leftExpression = expressionStack.pop();

        UnnamedVarExpression result = generator.calculate(leftExpression, calculationType, rightExpression);
        expressionStack.push(result);
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
    public void exitName(MKParser.NameContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        Expression expression = generator.getLocalVariable(name);
        if (expression == null) {
            if (globalVariables.containsKey(name)) {
                expression = globalVariables.get(name);
            } else {
                printError("using non-existing lady " + name);
            }
        }

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

        if (globalVariables.containsKey(name)) {
            GlobalVarExpression array = globalVariables.get(name);
            expressionStack.push(new GlobalVarExpression(ObjectType.ARRAY_ELEMENT, array.getDataType(), name, index));
        } else
            printError("using non-existing array lady " + name);


    }

    @Override
    public void exitVariable_declaration(MKParser.Variable_declarationContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();
        ObjectType objectType = ObjectType.VARIABLE;
        DataType dataType = DataType.NONE;
        Expression leftExpression;

        if (context.ASSIGN() == null) {
            if (inFunction)
                leftExpression = new NamedVarExpression(objectType, dataType, name, 0);
            else
                leftExpression = new GlobalVarExpression(objectType, dataType, name, 0);

            generator.declareVariable(leftExpression);

            if (inFunction)
                generator.assignVariable(leftExpression, new ValueExpression(ObjectType.VARIABLE, DataType.INT, 0));
        } else {
            Expression rightExpression = expressionStack.pop();
            dataType = rightExpression.getDataType();

            if (inFunction)
                leftExpression = new NamedVarExpression(objectType, dataType, name, 0);
            else
                leftExpression = new GlobalVarExpression(objectType, dataType, name, 0);

            generator.declareVariable(leftExpression);

            generator.assignVariable(leftExpression, rightExpression);
        }

    }

    @Override
    public void exitVariable_assignment(MKParser.Variable_assignmentContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();

        Expression rightExpression = expressionStack.pop();
        GlobalVarExpression leftExpression = new GlobalVarExpression(ObjectType.VARIABLE, DataType.NONE, name, 0);

        generator.assignVariable(leftExpression, rightExpression);
    }

    @Override
    public void exitPrint(MKParser.PrintContext context) {
        line = context.getStart().getLine();

        Expression expression = expressionStack.pop();
        generator.print(expression);
    }

    @Override
    public void exitScan_int(MKParser.Scan_intContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        scan(DataType.INT, name);
    }

    @Override
    public void exitScan_real(MKParser.Scan_realContext context) {
        line = context.getStart().getLine();
        String name = context.NAME().getText();

        scan(DataType.REAL, name);
    }

    private void scan(DataType dataType, String name) {
        Expression expression;

        expression = generator.getLocalVariable(name);
        if (expression == null) {
            if (globalVariables.containsKey(name)) {
                expression = globalVariables.get(name);
            } else {
                printError("getting non-existing lady " + name + " to hear");
            }
        }

        generator.scan(dataType, expression);
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

        GlobalVarExpression array = new GlobalVarExpression(ObjectType.ARRAY, dataType, name, 0);
        generator.declareArray(array, arrayLength, elements);
    }

    @Override
    public void exitArray_assignment(MKParser.Array_assignmentContext context) {
        line = context.getStart().getLine();

        Expression rightExpression = expressionStack.pop();
        Expression leftExpression = expressionStack.pop();

        generator.assignVariable(leftExpression, rightExpression);
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

