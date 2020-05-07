import com.sun.corba.se.spi.ior.ObjectKey;
import com.sun.javaws.jnl.RContentDesc;

import java.util.*;

public class LLVMActions extends MKBaseListener {
    private HashMap<String, GlobalVarExpression> globalVariables = new HashMap<String, GlobalVarExpression>();
    private LLVMGenerator generator = new LLVMGenerator(this, globalVariables);
    private Stack<Expression> expressionStack = new Stack<Expression>();
    private Configuration configuration = new Configuration();
    private int line = 0;
    private String fileName;
    private boolean inFunction = false; //flag for checking if currently in function body
    private boolean returning = false;  //flag for checking if a return statement occured in a function
    private HashMap<String, GlobalVarExpression> functions = new HashMap<String, GlobalVarExpression>();

    public LLVMActions(String fileName) {
        this.fileName = fileName;
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

        int argumentsCount = 0;
        if (childCount > 2)
            argumentsCount = (int) (childCount * 0.5 - 0.5);

        if (inFunction)
            printError("defining function not allowed inside another function");
        if (functions.containsKey(name))
            printError("defining already existing function");

        List<Expression> arguments = new ArrayList<Expression>(argumentsCount);
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
            printError("missing give back statement in function body");
        inFunction = false;
        returning = false;
        generator.endFunctionDefinition();
    }

    @Override
    public void exitReturning(MKParser.ReturningContext context) {
        line = context.getStart().getLine();
        Expression expression = expressionStack.pop();

        if (!inFunction)
            printError("give back statement used while not in function body");
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

        Expression expression = null;
        if (globalVariables.containsKey(name)) {
            expression = globalVariables.get(name);
        } else {
            printError("using non-existing lady " + name);
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
        GlobalVarExpression array = null;

        if (globalVariables.containsKey(name)) {
            array = globalVariables.get(name);
            expressionStack.push(new GlobalVarExpression(ObjectType.ARRAY_ELEMENT, array.getDataType(), name, index));
        } else
            printError("using non-existing array lady " + name);


    }

    @Override
    public void exitVariable_declaration(MKParser.Variable_declarationContext context) {
        line = context.getStart().getLine();

        String name = context.NAME().getText();
        ObjectType objectType = ObjectType.VARIABLE;

        if (context.ASSIGN() == null) {
            generator.declareVariable(new GlobalVarExpression(objectType, DataType.NONE, name, 0));
        } else {
            Expression rightExpression = expressionStack.pop();
            DataType dataType = rightExpression.getDataType();

            GlobalVarExpression leftExpression = new GlobalVarExpression(objectType, dataType, name, 0);
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
        Expression expression = null;

        if (globalVariables.containsKey(name)) {
            expression = globalVariables.get(name);
        } else {
            printError("getting non-existing lady " + name + " to hear");
        }

        generator.scan(dataType, (GlobalVarExpression) expression);
    }

    @Override
    public void exitInt_array_declaration(MKParser.Int_array_declarationContext context) {
        line = context.getStart().getLine();
        DataType dataType = DataType.INT;
        MKParser.Array_declaration1Context context1 = context.getChild(MKParser.Array_declaration1Context.class, 0);
        //System.out.println(context.getChild(MKParser.Array_declaration1Context.class, 0));

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

        List<Expression> elements = new ArrayList<Expression>(arrayLength);
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

