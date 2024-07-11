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
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.TestUtils;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;

/**
 * Unit test for {@linkplain Parameter}.
 *
 * @author Pierre Seimandi (GMV)
 */
public class ParameterTest {

    /** Field descriptor representing an ID. */
    private static final FieldDescriptor<String> ID = new FieldDescriptor<>("identifier",
            String.class);

    /** Field descriptor representing a number. */
    private static final FieldDescriptor<Number> NUMBER = new FieldDescriptor<>("number",
            Number.class);

    /** Field descriptor representing an orbital coordinate. */
    private static final FieldDescriptor<OrbitalCoordinate> COORDINATE = new FieldDescriptor<>(
            "coordinate", OrbitalCoordinate.class);

    /**
     * Initialization.
     */
    @BeforeClass
    public static void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Tests the creation of a new instance when providing the name of the parameter and its value.
     */
    @Test
    public void testNameConstructor() {
        final Parameter parameter = new Parameter("name", 1.5);
        final ParameterDescriptor descriptor = new ParameterDescriptor(
                StandardFieldDescriptors.PARAMETER_NAME, "name");

        Assert.assertEquals(descriptor, parameter.getDescriptor());
        Assert.assertEquals(1.5, parameter.getValue(), 0.);
        Assert.assertEquals("name", parameter.getName());
    }

    /**
     * Tests the creation of a new instance when providing a parameter descriptor and the parameter
     * value.
     */
    @Test
    public void testDescriptorConstructor() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(NUMBER, 36977);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);

        final Parameter parameter = new Parameter(descriptor, 8.9);
        Assert.assertEquals(descriptor, parameter.getDescriptor());
        Assert.assertEquals(8.9, parameter.getValue(), 0.);
        Assert.assertEquals("X_36977_SAT1", parameter.getName());
    }

    /**
     * Tests the method:<br>
     * {@linkplain Parameter#setValue(double)}
     */
    @Test
    public void testSetValue() {
        final Parameter parameter = new Parameter("name", 3.7);
        Assert.assertEquals(3.7, parameter.getValue(), 0.);
        parameter.setValue(-9.8);
        Assert.assertEquals(-9.8, parameter.getValue(), 0.);
    }

    /**
     * Tests the methods:<br>
     * {@linkplain Parameter#getName()}<br>
     * {@linkplain Parameter#getName(boolean)}<br>
     * {@linkplain Parameter#getName(String, boolean)}<br>
     */
    @Test
    public void testGetName() {
        String expected;

        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(NUMBER, 36977);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        final Parameter parameter = new Parameter(descriptor, 8.9);

        // Default separator and order
        expected = "X_36977_SAT1";
        Assert.assertEquals(expected, parameter.getName());

        // Default separator and natural order
        expected = "SAT1_36977_X";
        Assert.assertEquals(expected, parameter.getName(false));

        // Default separator and reverse order
        expected = "X_36977_SAT1";
        Assert.assertEquals(expected, parameter.getName(true));

        // Custom separator and default order
        expected = "X|36977|SAT1";
        Assert.assertEquals(expected, parameter.getName("|"));

        // Custom separator and natural order
        expected = "SAT1|36977|X";
        Assert.assertEquals(expected, parameter.getName("|", false));

        // Custom separator and reverse order
        expected = "X|36977|SAT1";
        Assert.assertEquals(expected, parameter.getName("|", true));
    }

    /**
     * Tests the methods:<br>
     * {@linkplain Parameter#getName()}<br>
     * {@linkplain Parameter#getName(boolean)}<br>
     * {@linkplain Parameter#getName(String, boolean)}<br>
     * when the associated parameter descriptor is {@code null}.
     */
    @Test
    public void testGetNameNullDescriptor() {
        final Parameter parameter = new Parameter((ParameterDescriptor) null, 8.9);

        // Default separator and natural order
        Assert.assertNull(parameter.getName());

        // Default separator and reverse order
        Assert.assertNull(parameter.getName(true));

        // Custom separator and reverse order
        Assert.assertNull(parameter.getName("|", true));
    }

    /**
     * Tests the methods:<br>
     * {@linkplain Parameter#toString()}<br>
     * {@linkplain Parameter#toString(boolean)}<br>
     * {@linkplain Parameter#toString(String, String, boolean, boolean)}<br>
     */
    @Test
    public void testToString() {
        String expected;

        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(NUMBER, 36977);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        final Parameter parameter = new Parameter(descriptor, 8.9);

        // Default separators and order
        expected = "Parameter[X_36977_SAT1: 8.9]";
        Assert.assertEquals(expected, parameter.toString());

        // Default separators, natural order
        expected = "Parameter[SAT1_36977_X: 8.9]";
        Assert.assertEquals(expected, parameter.toString(false));

        // Default separators, reverse order
        expected = "Parameter[X_36977_SAT1: 8.9]";
        Assert.assertEquals(expected, parameter.toString(true));

        // Custom separators, natural order
        expected = "Parameter[SAT1|36977|X=8.9]";
        Assert.assertEquals(expected, parameter.toString("|", "=", true, false));

        // Custom separators, reverse order, no class name
        expected = "X|36977|SAT1=8.9";
        Assert.assertEquals(expected, parameter.toString("|", "=", false, true));
    }

    /**
     * Tests the method:<br>
     * {@linkplain Parameter#copy()}<br>
     */
    @Test
    public void testCopy() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(NUMBER, 36977);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        final Parameter parameter = new Parameter(descriptor, 8.9);

        final Parameter copy = parameter.copy();
        checkEquality(parameter, copy);
        Assert.assertNotSame(parameter, copy);
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#equals(Object)}<br>
     * {@linkplain ParameterDescriptor#hashCode()}
     */
    @Test
    public void testEqualsAndHashCode() {
        Parameter other;

        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(NUMBER, 36977);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        final Parameter parameter = new Parameter(descriptor, 8.9);

        // Check the hashCode consistency between calls
        final int hashCode = parameter.hashCode();
        Assert.assertEquals(hashCode, parameter.hashCode());

        // Compared object is null
        Assert.assertFalse(parameter.equals(null));

        // Compared object is a different class
        Assert.assertFalse(parameter.equals(new Object()));

        // Same instance
        Assert.assertTrue(parameter.equals(parameter));

        // Same data, but different instances
        // (must return false, since the default behavior was preserved)
        other = parameter.copy();
        Assert.assertFalse(parameter.equals(other));
        Assert.assertFalse(other.equals(parameter));
        Assert.assertFalse(parameter.hashCode() == other.hashCode());

        // Different parameter descriptor
        other = new Parameter("name", 8.9);
        Assert.assertFalse(parameter.equals(other));
        Assert.assertFalse(other.equals(parameter));
        Assert.assertFalse(parameter.hashCode() == other.hashCode());

        // Different value
        other = new Parameter(descriptor, 1.5);
        Assert.assertFalse(parameter.equals(other));
        Assert.assertFalse(other.equals(parameter));
        Assert.assertFalse(parameter.hashCode() == other.hashCode());
    }

    /**
     * Tests the serialization/deserialization.
     */
    @Test
    public void testSerialization() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(NUMBER, 36977);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        final Parameter parameter = new Parameter(descriptor, 8.9);

        final Parameter deserialized = TestUtils.serializeAndRecover(parameter);
        checkEquality(parameter, deserialized);
        Assert.assertNotSame(parameter, deserialized);

        // The hash code of the deserialized instance cannot be identical to the one of the initial
        // instance because distinct instances always have different hash codes, regardless of their
        // internal data.
        // Assert.assertEquals(parameter.hashCode(), deserialized.hashCode());
    }

    /**
     * Asserts that two parameters are equal and throws an {@linkplain AssertionError} if they are
     * not.
     *
     * @param expected
     *        the expected parameter
     * @param actual
     *        the actual parameter
     */
    private static void checkEquality(final Parameter expected, final Parameter actual) {
        Assert.assertEquals(expected.getDescriptor(), actual.getDescriptor());
        Assert.assertEquals(expected.getValue(), actual.getValue(), 0.);
    }
}
