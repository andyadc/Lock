package org.menagerie;

/**
 * Abstract Skeleton implementation of a ConnectionListener interface.
 *
 * <p>All methods in the ConnectionListener are implemented as no-ops, and it is the choice
 * of the concrete implementation to decide which actions to respond to, and which to ignore.
 *
 * @author Scott Fines
 *         Date: Apr 22, 2011
 *         Time: 9:26:20 AM
 */
public abstract class ConnectionListenerSkeleton implements ConnectionListener{

    @Override
    public void syncConnected() {
        //default no-op
    }

    @Override
    public void expired() {
        //default no-op
    }

    @Override
    public void disconnected() {
        //default no-op
    }
}
