package main.java;

import java.util.List;

class CynchFunction implements CynchCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    CynchFunction(Stmt.Function declaration, Environment closure) {
        this.closure = closure;
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme,
                               arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch(Return returnValue) {
            return returnValue.value;
        }
        return null;
    }
}
