package org.menagerie.collections;

import org.apache.zookeeper.data.ACL;
import org.menagerie.Beta;
import org.menagerie.Serializer;
import org.menagerie.ZkSessionManager;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * ZooKeeper iterator which uses a Read-Write lock to enforce ZooKeeper-based synchronization.
 *
 * @author Scott Fines
 * @version 1.0
 *          Date: 20-Jan-2011
 *          Time: 08:31:16
 */
final class ZkReadWriteIterator<E> extends ZkIterator<E>{
    private final ReadWriteLock safety;

    public ZkReadWriteIterator(String baseNode,
                               Serializer<E> eSerializer,
                               ZkSessionManager zkSessionManager,
                               List<ACL> privileges, String iteratorPrefix,
                               char iteratorDelimiter, ReadWriteLock safety) {
        super(baseNode, eSerializer, zkSessionManager, privileges, iteratorPrefix, iteratorDelimiter);
        this.safety = safety;
    }

    @Override
    public boolean hasNext() {
        Lock readLock = safety.readLock();
        readLock.lock();
        try{
            return super.hasNext();
        }finally{
            readLock.unlock();
        }
    }


    @Override
    public void remove() {
        Lock writeLock = safety.writeLock();
        writeLock.lock();
        try{
            super.remove();
        }finally{
            writeLock.unlock();
        }
    }
}
