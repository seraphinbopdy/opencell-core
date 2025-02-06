package org.meveo.interceptor;

import java.io.Serializable;

import org.meveo.commons.utils.MethodCallingUtils;
import org.meveo.model.IEntity;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Concurrency lock implementation. See @ConcurrencyLock for use details.
 */
@ConcurrencyLock
@Interceptor
public class ConcurrencyLockInterceptor implements Serializable {

    private static final long serialVersionUID = -671251230091797949L;

    @AroundInvoke
    Object lockMethod(InvocationContext ctx) throws Exception {

        ConcurrencyLock lockConfig = ctx.getMethod().getAnnotation(ConcurrencyLock.class);

        Long lockBy = null;
        if (ctx.getParameters().length > lockConfig.lockParameter()) {

            Object parameterValue = ctx.getParameters()[lockConfig.lockParameter()];

            if (parameterValue instanceof String) {
                lockBy = generateLongForString((String) parameterValue);

            } else if (parameterValue instanceof Long) {
                lockBy = (Long) parameterValue;
            } else if (parameterValue instanceof IEntity) {
                lockBy = (Long) ((IEntity) parameterValue).getId();
            }
        }

        if (lockBy == null) {
            return ctx.proceed();
        }

        return MethodCallingUtils.executeFunctionLocked(lockBy, () -> ctx.proceed());

    }

    /**
     * Generates a long value from a string using hash.
     * 
     * @param input String value to convert to long.
     * @return A long value representing the input string.
     */
    private long generateLongForString(String input) {
        if (input == null) {
            return 0L;
        }
        long h = 1125899906842597L; // prime
        int len = input.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + input.charAt(i);
        }
        return h;
    }
}
