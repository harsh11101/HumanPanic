package io.pants.humanpanic.interceptor;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

@Aspect
public class HumanPanicAspect {

    @Autowired
    private static CrashReporter crashReporter;
    @Autowired
    private static UserNotifier userNotifier;

    @Around("@annotation(io.pants.humanpanic.HumanPanic)")
    public Object handlePanic(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            HumanPanic annotation = method.getAnnotation(HumanPanic.class);

            handleException(throwable, annotation, method);

            if (annotation.exitCode() != 0) {
                System.exit(annotation.exitCode());
            }

            return getDefaultReturnValue(signature.getReturnType());
        }
    }

    private void handleException(Throwable throwable, HumanPanic annotation, Method method) {
        if (annotation.createCrashReport()) {
            String reportPath = crashReporter.createReport(throwable, method);
            if (!annotation.silent()) {
                userNotifier.notifyWithReport(annotation.message(), reportPath);
            }
        } else if (!annotation.silent()) {
            userNotifier.notify(annotation.message(), throwable);
        }

        if (annotation.printStackTrace()) {
            throwable.printStackTrace();
        }
    }

    private Object getDefaultReturnValue(Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) return null;
        if (returnType.isPrimitive()) {
            if (returnType == boolean.class) return false;
            if (returnType == byte.class) return (byte) 0;
            if (returnType == short.class) return (short) 0;
            if (returnType == int.class) return 0;
            if (returnType == long.class) return 0L;
            if (returnType == float.class) return 0.0f;
            if (returnType == double.class) return 0.0d;
            if (returnType == char.class) return '\u0000';
        }
        return null;
    }
}
