package org.menagerie.election;

import java.util.concurrent.TimeUnit;

/**
 * Interface for synchronously managing Leader Elections across distributed networks.
 * <p><p>
 * A typical usage for synchronous leader election is very similar to that of distributed locking, and would be
 * <pre>
 * {@code
 *         LeaderElector elector = ...
          if(elector.nominateSelfForLeader()){
 *              try{
 *                  //do your elected stuff
                }finally{
                    elector.concede();
                }
          }else{
            //perform alternative action
          }}
   </pre>
 *
 * @author Scott Fines
 * @version 1.0
 */
public interface LeaderElector {

    /**
     * Attempts to become the leader, returning immediately with success or failure.
     * <p>
     * <p>
     * If this party can become the leader immediately, then this method completes and returns true. Otherwise,
     * another party is the current leader and this method will return false.
     * <p>
     * <p>
     *
     * @return true if this thread is the leader
     */
    public boolean nominateSelfForLeader();

    /**
     * Attempts to become the leader, waiting up to a maximum of {@code timeout} units.
     * <p>
     * If this party cannot become the leader, then the current thread becomes disabled for thread
     * scheduling purposes and lies dormant until one of three things happen:
     * <ol>
     *      <li> This thread becomes the leader
     *      <li> Some other thread interrupts the current thread
     *      <li> the specified waiting time elapses
     * </ol>
     * <br/>
     * If this party becomes the leader, the value true is returned.
     * <p><p>
     *
     * If the current thread
     * <ul>
     *      <li>has its interrupted status set on entry to this method
     *      <li> is interrupted while undergoing election
     * </ul>
     * <br/>
     * then an {@link InterruptedException} is thrown and the current thread's status is cleared. When this
     * occurs, it is no longer possible for this thread to become the leader.
     *
     * @param timeout the maximum time to wait to become leader
     * @param unit the time units to use
     * @return true if this thread is the leader
     * @throws InterruptedException if the thread is interrupted while waiting to determine the leadership
     */
    public boolean nominateSelfForLeader(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Concedes the election to another party.
     * <p>
     * This method may only be called by the party which <i>is</i> the current leader. Otherwise, an
     * {@link IllegalMonitorStateException} will be thrown.
     * <p>
     * After this method has been called, the results of all previous calls to {@link #getLeader()} will no longer
     * be valid.
     *
     * @throws IllegalMonitorStateException if this method is attempted to be called from a different thread than
     *              the caller of {@link #nominateSelfForLeader()}.
     */
    public void concede();

    /**
     * Gets a string representation of the leader node. Often this is an IP address, but implementations may differ
     * in what is returned.
     * <p>
     * Implementations should clearly indicate what in particular is being returned by this method.
     *
     * @return a String representation of the leader node
     */
    public String getLeader();

}
