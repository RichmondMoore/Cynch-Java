package main.java;

import java.util.List;

interface CynchCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
