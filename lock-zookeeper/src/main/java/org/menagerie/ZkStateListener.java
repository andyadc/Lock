package org.menagerie;

/**
 * @author Scott Fines
 * Date: Apr 22, 2011
 * Time: 9:09:14 AM
 */
public interface ZkStateListener {

    void syncConnected();

    void sessionExpired();

    void disconnected();
}
