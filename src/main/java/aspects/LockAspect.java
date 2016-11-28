package aspects;

import beans.AuthenticatedRequestBean;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.commcare.modern.database.TableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

/**
 * Aspect for weaving locking for classes that require it
 */
@Aspect
public class LockAspect {

    @Autowired
    protected LockRegistry userLockRegistry;

    @Around(value = "@annotation(annotations.UserLock)")
    public Object beforeLock(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (!(args[0] instanceof AuthenticatedRequestBean)) {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                throw new RuntimeException(throwable);
            }
        }

        AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
        Lock lock = getLockAndBlock(TableBuilder.scrubName(bean.getUsername()));
        try {
            return joinPoint.proceed();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    protected Lock getLockAndBlock(String username){
        Lock lock = userLockRegistry.obtain(username);
        if (obtainLock(lock)) {
            return lock;
        } else {
            throw new RuntimeException("Timed out trying to obtain lock for username " + username  +
                    ". Please try your request again in a moment.");
        }
    }

    protected boolean obtainLock(Lock lock) {
        try {
            return lock.tryLock(60, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            return obtainLock(lock);
        }
    }
}