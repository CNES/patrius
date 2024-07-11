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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.Locale;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Unit tests for {@link FieldDescriptor}
 *
 * @author Pierre Seimandi (GMV)
 */
public class FieldDescriptorTest {

    /**
     * Initialization.
     */
    @BeforeClass
    public static void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Tests the creation of new instances (no print function).
     */
    @Test
    public void testConstructor1() {
        final String name = "string";
        final Class<String> fieldClass = String.class;

        final FieldDescriptor<String> fieldDescriptor = new FieldDescriptor<>(name, fieldClass);
        Assert.assertEquals(name, fieldDescriptor.getName());
        Assert.assertEquals(fieldClass, fieldDescriptor.getFieldClass());
        Assert.assertNull(fieldDescriptor.getPrintFunction());

        // Ensure an exception is thrown if the name is null
        try {
            new FieldDescriptor<>(null, fieldClass);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (name)", e.getMessage());
        }

        // Ensure an exception is thrown if the field class is null
        try {
            new FieldDescriptor<>(name, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field class)", e.getMessage());
        }
    }

    /**
     * Tests the creation of new instances (with a print function).
     */
    @Test
    public void testConstructor2() {
        final String name = "date";
        final Class<AbsoluteDate> fieldClass = AbsoluteDate.class;
        final Function<AbsoluteDate, String> printFunction = (date) -> date.toString();

        final FieldDescriptor<AbsoluteDate> fieldDescriptor = new FieldDescriptor<>(name,
                fieldClass, printFunction);
        Assert.assertEquals(name, fieldDescriptor.getName());
        Assert.assertEquals(fieldClass, fieldDescriptor.getFieldClass());
        Assert.assertEquals(printFunction, fieldDescriptor.getPrintFunction());
        fieldDescriptor.setPrintFunction(null);
        Assert.assertNull(fieldDescriptor.getPrintFunction());

        // Ensure an exception is thrown if the name is null
        try {
            new FieldDescriptor<>(null, fieldClass, printFunction);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (name)", e.getMessage());
        }

        // Ensure an exception is thrown if the field class is null
        try {
            new FieldDescriptor<>(name, null, printFunction);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field class)", e.getMessage());
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain FieldDescriptor#printField(Object)}
     */
    @Test
    public void testPrintField() {
        // If no print function is defined, the default toString() method should be used.
        final FieldDescriptor<AbsoluteDate> fieldDescriptor = new FieldDescriptor<>("date",
                AbsoluteDate.class);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final String string1 = fieldDescriptor.printField(date);
        Assert.assertEquals("2000-01-01T11:59:27.816", string1);

        // If a print function is defined and the provided field is a date, it should be used
        // instead of the default toString() method.
        final Function<AbsoluteDate, String> printFunction = (datetime) -> "AbsoluteDate["
                + datetime.toString(6) + "]";
        fieldDescriptor.setPrintFunction(printFunction);
        final String string2 = fieldDescriptor.printField(date);
        Assert.assertEquals("AbsoluteDate[2000-01-01T11:59:27.816000]", string2);

        // If the class of the provided field does not match the class specified by the field
        // descriptor, the default toString() method
        // should be used even if a print function is defined.
        final String string3 = fieldDescriptor.printField(189.5481789393774949);
        Assert.assertEquals("189.5481789393775", string3);
    }

    /**
     * Tests the method:<br>
     * {@linkplain FieldDescriptor#toString()}
     */
    @Test
    public void testToString() {
        final FieldDescriptor<AbsoluteDate> fieldDescriptor = new FieldDescriptor<>("date",
                AbsoluteDate.class);
        Assert.assertEquals("FieldDescriptor[name: date; class: AbsoluteDate]",
                fieldDescriptor.toString());
    }

    /**
     * Tests the methods:<br>
     * {@linkplain FieldDescriptor#equals(Object)}<br>
     * {@linkplain FieldDescriptor#hashCode()}
     */
    @Test
    public void testEqualsAndHashCode() {
        FieldDescriptor<?> other;
        final FieldDescriptor<String> instance = new FieldDescriptor<>("name", String.class);
        final Function<String, String> printFunction = (string) -> string.toString();

        // Check the hashCode consistency between calls
        final int hashCode = instance.hashCode();
        Assert.assertEquals(hashCode, instance.hashCode());

        // Compared object is null
        Assert.assertFalse(instance.equals(null));

        // Compared object is a different class
        Assert.assertFalse(instance.equals(new Object()));

        // Same instance
        Assert.assertTrue(instance.equals(instance));

        // Same data, but different instances
        other = new FieldDescriptor<>("name", String.class);
        Assert.assertTrue(instance.equals(other));
        Assert.assertTrue(other.equals(instance));
        Assert.assertTrue(instance.hashCode() == other.hashCode());

        // Different name
        other = new FieldDescriptor<>("another name", String.class);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different class
        other = new FieldDescriptor<>("name", AbsoluteDate.class);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different print function
        other = new FieldDescriptor<>("name", String.class, printFunction);
        Assert.assertTrue(instance.equals(other));
        Assert.assertTrue(other.equals(instance));
        Assert.assertTrue(instance.hashCode() == other.hashCode());
    }

    /**
     * Tests the serialization/deserialization.
     */
    @Test
    public void testSerialization() {
        final Function<String, String> printFunction = (string) -> "value: " + string.toString();
        final FieldDescriptor<String> fieldDescriptor = new FieldDescriptor<>("string",
                String.class, printFunction);

        final FieldDescriptor<String> deserialized = TestUtils.serializeAndRecover(fieldDescriptor);
        Assert.assertNotSame(fieldDescriptor, deserialized);
        Assert.assertEquals(fieldDescriptor, deserialized);
        Assert.assertNull(deserialized.getPrintFunction());
        Assert.assertEquals(fieldDescriptor.hashCode(), deserialized.hashCode());
    }
}
