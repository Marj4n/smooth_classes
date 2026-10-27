package org.marj4n.smooth_classes.runtime;

/** Small immutable result returned by server-side ability executions. */
public record ExecutionResult(boolean success, int affectedTargets, String detail) {
    public static ExecutionResult success(int affectedTargets, String detail) {
        return new ExecutionResult(true, affectedTargets, detail);
    }

    public static ExecutionResult failure(String detail) {
        return new ExecutionResult(false, 0, detail);
    }
}
