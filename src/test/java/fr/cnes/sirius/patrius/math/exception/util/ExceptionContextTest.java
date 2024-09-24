/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.exception.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Test for {@link ExceptionContext}.
 * 
 * @version $Id: ExceptionContextTest.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ExceptionContextTest {
    @Test
    public void testMessageChain() {
        final ExceptionContext c = new ExceptionContext(new Exception("oops"));
        final String sep = " | "; // Non-default separator.
        final String m1 = "column index (0)";
        c.addMessage(PatriusMessages.COLUMN_INDEX, 0);
        final String m2 = "got 1x2 but expected 3x4";
        c.addMessage(PatriusMessages.DIMENSIONS_MISMATCH_2x2, 1, 2, 3, 4);
        final String m3 = "It didn't work out";
        c.addMessage(PatriusMessages.SIMPLE_MESSAGE, m3);

        Assert.assertEquals(c.getMessage(Locale.US, sep),
            m1 + sep + m2 + sep + m3);
    }

    @Test
    public void testNoArgAddMessage() {
        final ExceptionContext c = new ExceptionContext(new Exception("hello"));
        c.addMessage(PatriusMessages.SIMPLE_MESSAGE);
        Assert.assertEquals(c.getMessage(), "{0}");
    }

    @Test
    public void testContext() {
        final Throwable thr = new Exception("bye");
        final ExceptionContext c = new ExceptionContext(thr);

        final String[] keys = { "Key 1", "Key 2" };
        final Object[] values = { "Value 1", Integer.valueOf(2) };

        for (int i = 0; i < keys.length; i++) {
            c.setValue(keys[i], values[i]);
        }

        // Check that all keys are present.
        Assert.assertTrue(c.getKeys().containsAll(Arrays.asList(keys)));

        // Check that all values are correctly stored.
        for (int i = 0; i < keys.length; i++) {
            Assert.assertEquals(values[i], c.getValue(keys[i]));
        }

        // Check behaviour on missing key.
        Assert.assertNull(c.getValue("xyz"));

        // Check throwable getter
        Assert.assertEquals(thr, c.getThrowable());
    }

    @Test
    public void testSerialize()
                               throws IOException,
                               ClassNotFoundException {
        final ExceptionContext cOut = new ExceptionContext(new Exception("Apache"));
        cOut.addMessage(PatriusMessages.COLUMN_INDEX, 0);
        cOut.setValue("Key 1", Integer.valueOf(0));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(cOut);

        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final ExceptionContext cIn = (ExceptionContext) ois.readObject();

        Assert.assertTrue(cOut.getMessage().equals(cIn.getMessage()));
        for (final String key : cIn.getKeys()) {
            Assert.assertTrue(cOut.getValue(key).equals(cIn.getValue(key)));
        }
    }

    @Test
    public void testSerializeUnserializable() throws Exception {
        final ExceptionContext cOut = new ExceptionContext(new Exception("Apache Commons Math"));
        cOut.addMessage(PatriusMessages.SIMPLE_MESSAGE, "OK");
        cOut.addMessage(PatriusMessages.SIMPLE_MESSAGE, new Unserializable());
        final String key = "Key 1";
        cOut.setValue(key, new Unserializable());

        {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(cOut);

            final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final ObjectInputStream ois = new ObjectInputStream(bis);
            final ExceptionContext cIn = (ExceptionContext) ois.readObject();

            final String nsObjStr = (String) cIn.getValue(key);
            Assert.assertTrue(nsObjStr.matches(".*could not be serialized.*"));
        }
    }

    /**
     * Class used by {@link #testSerializeUnserializable()}.
     */
    private static class Unserializable {
        Unserializable() {
        }
    }
}
