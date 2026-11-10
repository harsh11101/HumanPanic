package io.pants.humanpanic.interceptor;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.internal.PanicResponder;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;

/**
 * <b>Spring AOP integration.</b> Applies {@link HumanPanic} handling to annotated methods of
 * Spring beans. Registered by {@link io.pants.humanpanic.config.HumanPanicConfiguration}.
 *
 * <p>The usual Spring AOP proxy rules apply: only calls that go through the Spring proxy are
 * intercepted. Self-invocation ({@code this.method()}) and non-public methods are not.
 *
 * <p>Needs {@code spring-aop} and {@code aspectjweaver} on the classpath. Native AspectJ weaving
 * (ajc or load-time weaving) is not supported.
 */
@Aspect
public class HumanPanicAspect {

    private final PanicResponder responder;

    /**
     * Creates the aspect.
     *
     * @param crashReporter writes crash reports; must not be {@code null}
     * @param userNotifier  writes user messages; must not be {@code null}
     */
    public HumanPanicAspect(CrashReporter crashReporter, UserNotifier userNotifier) {
        this.responder = new PanicResponder(crashReporter, userNotifier);
    }

    /**
     * Around advice for {@code @HumanPanic} methods. The annotation is bound by the pointcut, so it
     * is found even when the bean is proxied through an interface.
     *
     * @param joinPoint  the intercepted call
     * @param humanPanic the annotation on the target method ({@code null} means defaults)
     * @return the method's result, or its default return value after a handled failure
     * @throws Throwable if the failure is a {@link VirtualMachineError}
     */
    @Around("@annotation(humanPanic)")
    public Object handlePanic(ProceedingJoinPoint joinPoint, HumanPanic humanPanic) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable failure) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object target = joinPoint.getTarget();
            if (method != null && target != null) {
                // With JDK proxies the signature names the interface method; report the implementation.
                method = AopUtils.getMostSpecificMethod(method, AopUtils.getTargetClass(target));
            }
            return responder.recover(failure, humanPanic, method, signature.getReturnType());
        }
    }
}
