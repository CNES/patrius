/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:10/03/2016:Serialization test utils
 * END-HISTORY
 */

package fr.cnes.sirius.patrius;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;

/**
 * @version $Id: TestUtils.java 7721 2013-02-14 14:07:13Z CardosoP $
 */
public class TestUtils {

    /**
     * Serializes an object to a bytes array and then recovers the object from the bytes array.
     * Returns the deserialized object.
     * 
     * @param o
     *        object to serialize and recover
     * @return
     * @return the recovered, deserialized object
     */
    public static <T> T serializeAndRecover(final T o) {
        try {
            // serialize the Object
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream so = new ObjectOutputStream(bos);
            so.writeObject(o);

            // deserialize the Object
            final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final ObjectInputStream si = new ObjectInputStream(bis);
            return (T) si.readObject();
        } catch (final IOException ioe) {
            System.out.println("error ioe serializeAndRecover ");
            ioe.printStackTrace();
            return null;
        } catch (final ClassNotFoundException cnfe) {
            System.out.println("error cnfe serializeAndRecover ");
            cnfe.printStackTrace();
            return null;
        }
    }

    /**
     * Verifies that serialization preserves equals and hashCode.
     * Serializes the object, then recovers it and checks equals and hash code.
     * 
     * @param object
     *        the object to serialize and recover
     */
    public static void checkSerializedEquality(final Object object) {
        final Object object2 = serializeAndRecover(object);
        Assert.assertEquals("Equals check", object, object2);
        Assert.assertEquals("HashCode check", object.hashCode(), object2.hashCode());
    }
}
