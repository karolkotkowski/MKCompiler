public class LLVMBuilder {
    private StringBuilder builder = new StringBuilder();
    private StringBuilder buffer = new StringBuilder();
    private StringBuilder headerBuilder = new StringBuilder();
    private StringBuilder mainBuilder = new StringBuilder();
    private StringBuilder mainDeclarationBuilder = new StringBuilder();
    private boolean bufferHeld = false;

    public void holdBuffer() {
        bufferHeld = true;
    }

    public String getBuffer() {
        return buffer.toString();
    }

    public void releaseBuffer() {
        buffer = new StringBuilder();
        bufferHeld = false;
    }

    public void append(Object object) {
        if (bufferHeld) {
            buffer.append(object);
        } else {
            builder.append(object);
        }
    }

    public void append(Object object, GlobalVarExpression currentFunction) {
        String currentFunctionName;
        if (currentFunction == null)
            currentFunctionName = "main";
        else
            currentFunctionName = currentFunction.getName();;
        if ("main".equals(currentFunctionName))
            appendToMain(object);
        else
            append(object);
    }

    public void appendToHeader(Object object) {
        if (bufferHeld)
            buffer.append(object);
        else
            headerBuilder.append(object);
    }

    public void appendToMain(Object object) {
        if (bufferHeld)
            buffer.append(object);
        else
            mainBuilder.append(object);
    }

    public void appendToMainDeclaration(Object object) {
        if (bufferHeld)
            buffer.append(object);
        else
            mainDeclarationBuilder.append(object);
    }

    @Override
    public String toString() {
        return headerBuilder.toString() + mainDeclarationBuilder.toString() + mainBuilder.toString() + builder.toString();
    }
}
