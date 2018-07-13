package org.menagerie;

import java.io.*;

/**
 * Serializer based on Java Serialization.
 *
 * @author Scott Fines
 * @version 1.0
 *          Date: 20-Jan-2011
 *          Time: 08:45:40
 */
public class JavaSerializer<T extends Serializable> implements Serializer<T> {

    @Override
    @SuppressWarnings({"unchecked"})
    public T deserialize(byte[] data) {
        try {
            ObjectInputStream inputStream  = new ObjectInputStream(new ByteArrayInputStream(data));
            return (T)inputStream.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            //should never happen, since AbstractMap.SimpleEntry is part of the JDK
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] serialize(T instance) {
         try {
            ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
            ObjectOutputStream arrayOutput = new ObjectOutputStream(byteArrayStream);
            arrayOutput.writeObject(instance);
            arrayOutput.flush();

            byte[] bytes = byteArrayStream.toByteArray();
            arrayOutput.close();
            return bytes;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
