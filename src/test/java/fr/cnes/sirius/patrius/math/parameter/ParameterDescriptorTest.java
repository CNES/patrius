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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.NullArgumentException;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Unit tests for {@link ParameterDescriptor}.
 *
 * @author Pierre Seimandi (GMV)
 */
public class ParameterDescriptorTest {

    /** Field descriptor representing the name of a parameter. */
    private static final FieldDescriptor<String> PARAMETER_NAME = StandardFieldDescriptors.PARAMETER_NAME;

    /** Field descriptor representing a string. */
    private static final FieldDescriptor<String> STRING = new FieldDescriptor<>("string",
            String.class);

    /** Field descriptor representing a date. */
    private static final FieldDescriptor<AbsoluteDate> DATE = new FieldDescriptor<>("date",
            AbsoluteDate.class);

    /** Field descriptor representing an orbital coordinate. */
    private static final FieldDescriptor<OrbitalCoordinate> COORDINATE = new FieldDescriptor<>(
            "coordinate", OrbitalCoordinate.class);

    /** Field descriptor representing a number. */
    private static final FieldDescriptor<Number> NUMBER = new FieldDescriptor<>("number",
            Number.class);

    /** Field descriptor representing an ID. */
    private static final FieldDescriptor<String> ID = new FieldDescriptor<>("identifier",
            String.class);

    /**
     * Initialization.
     */
    @BeforeClass
    public static void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Test the creation of new instances.
     */
    @Test
    public void testConstructors() {
        ParameterDescriptor descriptor;
        Map<FieldDescriptor<?>, Object> fieldDescriptorsMap;

        // No initial field descriptor
        descriptor = new ParameterDescriptor();
        fieldDescriptorsMap = new LinkedHashMap<>();
        Assert.assertTrue(descriptor.isMutable());
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Only a name is provided
        descriptor = new ParameterDescriptor("SAT1");
        fieldDescriptorsMap = new LinkedHashMap<>();
        fieldDescriptorsMap.put(PARAMETER_NAME, "SAT1");
        Assert.assertTrue(descriptor.isMutable());
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Single initial field descriptor
        descriptor = new ParameterDescriptor(PARAMETER_NAME, "SAT1");
        fieldDescriptorsMap = new LinkedHashMap<>();
        fieldDescriptorsMap.put(PARAMETER_NAME, "SAT1");
        Assert.assertTrue(descriptor.isMutable());
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Multiple initial field descriptors
        fieldDescriptorsMap = new LinkedHashMap<>();
        fieldDescriptorsMap.put(PARAMETER_NAME, "SAT1");
        fieldDescriptorsMap.put(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor = new ParameterDescriptor(fieldDescriptorsMap);
        Assert.assertTrue(descriptor.isMutable());
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertNotSame(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Empty initial field descriptors map
        fieldDescriptorsMap = new LinkedHashMap<>();
        descriptor = new ParameterDescriptor(fieldDescriptorsMap);
        Assert.assertTrue(descriptor.isMutable());
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertNotSame(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Null initial field descriptors map
        fieldDescriptorsMap = null;
        descriptor = new ParameterDescriptor(fieldDescriptorsMap);
        fieldDescriptorsMap = new LinkedHashMap<>();
        Assert.assertTrue(descriptor.isMutable());
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertNotSame(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Ensure an exception is thrown if a null parameter name is provided
        try {
            new ParameterDescriptor((String) null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field value)", e.getMessage());
        }

        // Ensure an exception is thrown if a null field descriptor is provided
        try {
            new ParameterDescriptor((FieldDescriptor<String>) null, "SAT1");
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptor)", e.getMessage());
        }

        // Ensure an exception is thrown if a null field value is provided
        try {
            new ParameterDescriptor(PARAMETER_NAME, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field value)", e.getMessage());
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#setMutability(boolean)}
     */
    @Test
    public void testSetMutablity() {
        final ParameterDescriptor descriptor = new ParameterDescriptor("SAT1");
        Assert.assertTrue(descriptor.isMutable());

        descriptor.setMutability(false);
        Assert.assertFalse(descriptor.isMutable());

        descriptor.setMutability(true);
        Assert.assertTrue(descriptor.isMutable());

        // Ensure the same instance is returned by the method
        Assert.assertSame(descriptor, descriptor.setMutability(true));
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#contains(FieldDescriptor)}<br>
     * {@linkplain ParameterDescriptor#contains(FieldDescriptor, Object)}
     */
    @Test
    public void testContains() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "parameter");
        descriptor.addField(COORDINATE, CartesianCoordinate.X);

        Assert.assertTrue(descriptor.contains(STRING));
        Assert.assertTrue(descriptor.contains(COORDINATE));
        Assert.assertFalse(descriptor.contains(DATE));
        Assert.assertTrue(descriptor.contains(STRING, "parameter"));
        Assert.assertFalse(descriptor.contains(STRING, "wrong-name"));
        Assert.assertTrue(descriptor.contains(COORDINATE, CartesianCoordinate.X));
        Assert.assertFalse(descriptor.contains(COORDINATE, CartesianCoordinate.Y));
        Assert.assertFalse(descriptor.contains(DATE, AbsoluteDate.J2000_EPOCH));
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#addField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#addFieldIfAbsent(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#addUntypedField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#addUntypedFields(Map)}<br>
     */
    @Test
    public void testAddField() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        final Map<FieldDescriptor<?>, Object> fieldDescriptorsMap = new LinkedHashMap<>();

        Object previousValue;

        // Add a typed field descriptor
        previousValue = descriptor.addField(ID, "SAT1");
        Assert.assertNull(previousValue);
        fieldDescriptorsMap.put(ID, "SAT1");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Add a typed field descriptor which is already associated with the parameter descriptor
        previousValue = descriptor.addField(ID, "SATELITTE-1");
        Assert.assertEquals("SAT1", previousValue);
        fieldDescriptorsMap.replace(ID, "SATELITTE-1");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Add a typed field descriptor (if absent)
        previousValue = descriptor.addFieldIfAbsent(NUMBER, 1);
        Assert.assertNull(previousValue);
        fieldDescriptorsMap.put(NUMBER, 1);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Try to add a typed field descriptor which is already associated with the parameter descriptor with the if absent feature
        previousValue = descriptor.addFieldIfAbsent(NUMBER, 2);
        Assert.assertEquals(1, previousValue);
        // Nothing to replace in the map which stay unchanged
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Add an untyped field descriptor
        previousValue = descriptor.addUntypedField(DATE, AbsoluteDate.J2000_EPOCH);
        Assert.assertNull(previousValue);
        fieldDescriptorsMap.put(DATE, AbsoluteDate.J2000_EPOCH);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Add an untyped field descriptor which is already associated with the parameter descriptor
        previousValue = descriptor.addUntypedField(DATE, AbsoluteDate.CCSDS_EPOCH);
        Assert.assertEquals(AbsoluteDate.J2000_EPOCH, previousValue);
        fieldDescriptorsMap.replace(DATE, AbsoluteDate.CCSDS_EPOCH);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Add multiple untyped field descriptors, some of which are already
        // associated with the parameter descriptor but mapped to different values
        fieldDescriptorsMap.replace(ID, "SAT2");
        fieldDescriptorsMap.replace(DATE, AbsoluteDate.JULIAN_EPOCH);
        fieldDescriptorsMap.put(COORDINATE, CartesianCoordinate.X);
        descriptor.addUntypedFields(fieldDescriptorsMap);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // The provided field descriptors map is null (the parameter descriptor should remain
        // unchanged)
        descriptor.addUntypedFields(null);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
    }

    /**
     * Ensures an exception is thrown when the provided field descriptor is {@code null}.
     * <p>
     * Tested methods:<br>
     * {@linkplain ParameterDescriptor#addField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#addUntypedField(FieldDescriptor, Object)}<br>
     * </p>
     */
    @Test
    public void testAddFieldNullFieldDescriptor() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();

        try {
            descriptor.addField(null, "SAT1");
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptor)", e.getMessage());
        }

        try {
            descriptor.addUntypedField(null, "SAT1");
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptor)", e.getMessage());
        }
    }

    /**
     * Ensures an exception is thrown when the provided field value is {@code null}.
     * <p>
     * Tested methods:<br>
     * {@linkplain ParameterDescriptor#addField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#addUntypedField(FieldDescriptor, Object)}<br>
     * </p>
     */
    @Test
    public void testAddFieldNullFieldValue() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();

        try {
            descriptor.addField(ID, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field value)", e.getMessage());
        }

        try {
            descriptor.addUntypedField(ID, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field value)", e.getMessage());
        }
    }

    /**
     * Ensures an exception is thrown when attempting to add a new field descriptor to an immutable
     * parameter descriptor.
     * <p>
     * Tested methods:<br>
     * {@linkplain ParameterDescriptor#addField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#addUntypedField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#addUntypedFields(Map)}<br>
     * </p>
     */
    @Test
    public void testAddToImmutableInstance() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.setMutability(false);

        final Map<FieldDescriptor<?>, Object> fieldDescriptorsMap = new LinkedHashMap<>();
        fieldDescriptorsMap.put(ID, "SAT1");
        fieldDescriptorsMap.put(DATE, AbsoluteDate.J2000_EPOCH);

        // Typed method (single field descriptor)
        try {
            descriptor.addField(ID, "SAT1");
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        // Untyped method (single field descriptor)
        try {
            descriptor.addUntypedField(ID, "SAT1");
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        // Untyped method (null field descriptors map)
        try {
            descriptor.addUntypedFields(null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        // Untyped method (multiple field descriptors)
        try {
            descriptor.addUntypedFields(fieldDescriptorsMap);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#replaceField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceField(FieldDescriptor, Object, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceUntypedField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceUntypedField(FieldDescriptor, Object, Object)}<br>
     */
    @Test
    public void testReplaceField() {
        Object replacedObject;
        boolean replaced;

        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");

        final Map<FieldDescriptor<?>, Object> fieldDescriptorsMap = descriptor
                .getAssociatedFields();

        // Null typed field descriptor
        replacedObject = descriptor.replaceField(null, "new-value");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertNull(replacedObject);

        // Null untyped field descriptor
        replacedObject = descriptor.replaceUntypedField((FieldDescriptor<?>) null, "new-value");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertNull(replacedObject);

        // Replace a typed field descriptor
        replacedObject = descriptor.replaceField(STRING, "new-value");
        fieldDescriptorsMap.replace(STRING, "new-value");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertEquals("initial-value", replacedObject);

        // Replace a typed field descriptor which is not associated with the parameter descriptor
        replacedObject = descriptor.replaceField(COORDINATE, CartesianCoordinate.Z);
        Assert.assertNull(replacedObject);

        // Replace an untyped field descriptor
        replacedObject = descriptor.replaceUntypedField(DATE, AbsoluteDate.CCSDS_EPOCH);
        fieldDescriptorsMap.replace(DATE, AbsoluteDate.CCSDS_EPOCH);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertEquals(AbsoluteDate.J2000_EPOCH, replacedObject);

        // Replace a typed field descriptor if the mapped value matches the specified value
        replaced = descriptor.replaceField(null, 10, 20);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(replaced);

        replaced = descriptor.replaceField(NUMBER, null, 20);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(replaced);

        replaced = descriptor.replaceField(NUMBER, 20, 50);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(replaced);

        replaced = descriptor.replaceField(NUMBER, 10, 20);
        fieldDescriptorsMap.replace(NUMBER, 20);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertTrue(replaced);

        // Replace an untyped field descriptor if the mapped value matches the specified value
        replaced = descriptor.replaceUntypedField(null, "SAT1", "SAT2");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(replaced);

        replaced = descriptor.replaceUntypedField(ID, null, "SAT2");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(replaced);

        replaced = descriptor.replaceUntypedField(ID, 20, "SAT2");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(replaced);

        replaced = descriptor.replaceUntypedField(ID, "SAT2", "SAT2");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(replaced);

        replaced = descriptor.replaceUntypedField(ID, "SAT1", "SAT2");
        fieldDescriptorsMap.replace(ID, "SAT2");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertTrue(replaced);
    }

    /**
     * Ensures an exception is thrown when the provided field value is {@code null}.
     * <p>
     * Tested methods:<br>
     * {@linkplain ParameterDescriptor#replaceField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceField(FieldDescriptor, Object, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceUntypedField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceUntypedField(FieldDescriptor, Object, Object)}<br>
     * </p>
     */
    @Test
    public void testReplaceFieldNullFieldValue() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");

        try {
            descriptor.replaceField(ID, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field value)", e.getMessage());
        }

        try {
            descriptor.replaceField(ID, null, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field value)", e.getMessage());
        }

        try {
            descriptor.replaceUntypedField(ID, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field value)", e.getMessage());
        }

        try {
            descriptor.replaceUntypedField(ID, null, null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field value)", e.getMessage());
        }
    }

    /**
     * Ensures an exception is thrown when attempting to replace the value mapped to a field
     * descriptor to an immutable parameter descriptor.
     * <p>
     * Tested methods:<br>
     * {@linkplain ParameterDescriptor#replaceField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceField(FieldDescriptor, Object, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceUntypedField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#replaceUntypedField(FieldDescriptor, Object, Object)}<br>
     * </p>
     */
    @Test
    public void testReplaceInImmutableInstance() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");
        descriptor.setMutability(false);

        // Typed method
        try {
            descriptor.replaceField(ID, "SAT2");
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        try {
            descriptor.replaceField(ID, "SAT1", "SAT2");
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        // Untyped method
        try {
            descriptor.replaceUntypedField(ID, "SAT2");
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        try {
            descriptor.replaceUntypedField(ID, "SAT1", "SAT2");
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#removeField(FieldDescriptor)}<br>
     * {@linkplain ParameterDescriptor#removeField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#removeUntypedField(FieldDescriptor, Object)}<br>
     */
    @Test
    public void testRemoveField() {
        Object removedObject;
        boolean removed;

        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");

        final Map<FieldDescriptor<?>, Object> fieldDescriptorsMap = descriptor
                .getAssociatedFields();

        // Null typed field descriptor
        removedObject = descriptor.removeField(null);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertNull(removedObject);

        // Null untyped field descriptor
        removedObject = descriptor.removeField((FieldDescriptor<?>) null);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertNull(removedObject);

        // Remove a typed field descriptor
        removedObject = descriptor.removeField(STRING);
        fieldDescriptorsMap.remove(STRING);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertEquals("initial-value", removedObject);

        // Remove a typed field descriptor which is not associated with the parameter descriptor
        removedObject = descriptor.removeField(STRING);
        Assert.assertNull(removedObject);

        // Remove an untyped field descriptor
        removedObject = descriptor.removeField((FieldDescriptor<?>) DATE);
        fieldDescriptorsMap.remove(DATE);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertEquals(AbsoluteDate.J2000_EPOCH, removedObject);

        // Remove a typed field descriptor if the mapped value matches the specified value
        removed = descriptor.removeField(null, 10);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(removed);

        removed = descriptor.removeField(NUMBER, null);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(removed);

        removed = descriptor.removeField(NUMBER, 20);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(removed);

        removed = descriptor.removeField(NUMBER, 10);
        fieldDescriptorsMap.remove(NUMBER);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertTrue(removed);

        // Remove an untyped field descriptor if the mapped value matches the specified value
        removed = descriptor.removeUntypedField(null, "SAT1");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(removed);

        removed = descriptor.removeUntypedField(ID, null);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(removed);

        removed = descriptor.removeUntypedField(ID, 20);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(removed);

        removed = descriptor.removeUntypedField(ID, "SAT2");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertFalse(removed);

        removed = descriptor.removeUntypedField(ID, "SAT1");
        fieldDescriptorsMap.remove(ID);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
        Assert.assertTrue(removed);
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#removeUntypedFields(Collection)}
     */
    @Test
    public void testRemoveFields() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");

        // Initial field descriptors map
        Map<FieldDescriptor<?>, Object> fieldDescriptorsMap = descriptor.getAssociatedFields();

        // Null field descriptors list (the parameter descriptor should remain unchanged)
        descriptor.removeUntypedFields((FieldDescriptor<?>[]) null);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        descriptor.removeUntypedFields((Collection<FieldDescriptor<?>>) null);
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Expected remaining field descriptors
        descriptor.removeUntypedFields(Arrays.asList(STRING, DATE, null));
        fieldDescriptorsMap = new LinkedHashMap<>();
        fieldDescriptorsMap.put(COORDINATE, CartesianCoordinate.X);
        fieldDescriptorsMap.put(NUMBER, 10);
        fieldDescriptorsMap.put(ID, "SAT1");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());

        // Expected remaining field descriptors
        descriptor.removeUntypedFields(COORDINATE, NUMBER, null);
        fieldDescriptorsMap = new LinkedHashMap<>();
        fieldDescriptorsMap.put(ID, "SAT1");
        checkEquality(fieldDescriptorsMap, descriptor.getAssociatedFields());
    }

    /**
     * Ensures an exception is thrown when attempting to remove a field descriptor from an immutable
     * parameter descriptor.
     * <p>
     * Tested methods:<br>
     * {@linkplain ParameterDescriptor#removeField(FieldDescriptor)}<br>
     * {@linkplain ParameterDescriptor#removeField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#removeUntypedField(FieldDescriptor, Object)}<br>
     * {@linkplain ParameterDescriptor#removeUntypedFields(Collection)}<br>
     * </p>
     */
    @Test
    public void testRemoveFromImmutableInstance() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");
        descriptor.setMutability(false);

        // Typed method (single field descriptor)
        try {
            descriptor.removeField(COORDINATE);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        // Typed method (single field descriptor and specific field value)
        try {
            descriptor.removeField(COORDINATE, CartesianCoordinate.X);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        // Untyped method (single field descriptor and specific field value)
        try {
            descriptor.removeUntypedField(COORDINATE, CartesianCoordinate.X);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        // Untyped method (null collection of field descriptors)
        try {
            descriptor.removeUntypedFields((FieldDescriptor<?>[]) null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        try {
            descriptor.removeUntypedFields((Collection<FieldDescriptor<?>>) null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        // Untyped method (multiple field descriptors)
        try {
            descriptor.removeUntypedFields(ID, COORDINATE);
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }

        try {
            descriptor.removeUntypedFields(Arrays.asList(ID, COORDINATE));
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#clear()}
     */
    @Test
    public void testClear() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);

        descriptor.clear();
        Assert.assertTrue(descriptor.getAssociatedFields().isEmpty());
    }

    /**
     * Ensure an exception is thrown when attempting to clear the field descriptors map of an
     * immutable parameter descriptor.
     * <p>
     * Tested method:<br>
     * {@linkplain ParameterDescriptor#clear()}
     * </p>
     */
    @Test
    public void testClearImmutableInstance() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.setMutability(false);

        try {
            descriptor.clear();
            Assert.fail();
        } catch (final IllegalStateException e) {
            Assert.assertEquals(
                    "Operation not allowed: this parameter descriptor is currently immutable",
                    e.getMessage());
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#isEmpty()}
     */
    @Test
    public void testIsEmpty() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        Assert.assertTrue(descriptor.isEmpty());
        descriptor.addField(ID, "SAT1");
        Assert.assertFalse(descriptor.isEmpty());
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#getAssociatedFieldDescriptors()}
     */
    @Test
    public void testGetAssociatedFieldDescriptors() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");

        final Set<FieldDescriptor<?>> fieldDescriptors = new LinkedHashSet<>();
        fieldDescriptors.add(STRING);
        fieldDescriptors.add(DATE);
        fieldDescriptors.add(COORDINATE);
        fieldDescriptors.add(NUMBER);
        fieldDescriptors.add(ID);

        checkEquality(fieldDescriptors, descriptor.getAssociatedFieldDescriptors());
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#getFieldValue(FieldDescriptor)}
     */
    @Test
    public void testGetFieldValue() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);

        Assert.assertEquals("initial-value", descriptor.getFieldValue(STRING));
        Assert.assertEquals(AbsoluteDate.J2000_EPOCH, descriptor.getFieldValue(DATE));
        Assert.assertEquals(CartesianCoordinate.X, descriptor.getFieldValue(COORDINATE));
        Assert.assertNull(descriptor.getFieldValue(NUMBER));
        Assert.assertNull(descriptor.getFieldValue(ID));

        try {
            descriptor.getFieldValue(null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptor)", e.getMessage());
        }
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#extractSubset(FieldDescriptor...)}<br>
     * {@linkplain ParameterDescriptor#extractSubset(Collection)}<br>
     */
    @Test
    public void testExtractSubset() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        descriptor.addField(NUMBER, 10);
        descriptor.setMutability(false);

        final ParameterDescriptor expected = new ParameterDescriptor();
        expected.addField(COORDINATE, CartesianCoordinate.X);
        expected.addField(ID, "SAT1");
        expected.setMutability(false);

        ParameterDescriptor subset;

        subset = descriptor.extractSubset(COORDINATE, ID, STRING);
        Assert.assertEquals(expected, subset);
        checkEquality(expected.getAssociatedFields(), subset.getAssociatedFields());
        Assert.assertEquals(expected.isMutable(), subset.isMutable());

        subset = descriptor.extractSubset(Arrays.asList(COORDINATE, ID, STRING));
        Assert.assertEquals(expected, subset);
        checkEquality(expected.getAssociatedFields(), subset.getAssociatedFields());
        Assert.assertEquals(expected.isMutable(), subset.isMutable());
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#mergeWith(ParameterDescriptor)}
     */
    @Test
    public void testMergeWith() {
        final ParameterDescriptor descriptor1 = new ParameterDescriptor();
        descriptor1.addField(ID, "SAT1");
        descriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor1.setMutability(false);

        final ParameterDescriptor descriptor2 = new ParameterDescriptor();
        descriptor2.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        descriptor2.addField(NUMBER, 10);
        descriptor2.setMutability(true);

        ParameterDescriptor merged;
        ParameterDescriptor expected;

        // Standard merge
        merged = descriptor1.mergeWith(descriptor2);
        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        expected.addField(NUMBER, 10);
        expected.setMutability(false);
        Assert.assertEquals(expected, merged);
        checkEquality(expected.getAssociatedFields(), merged.getAssociatedFields());
        Assert.assertEquals(expected.isMutable(), merged.isMutable());

        // The other parameter descriptor is null
        merged = descriptor1.mergeWith(null);
        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(DATE, AbsoluteDate.J2000_EPOCH);
        expected.setMutability(false);
        Assert.assertEquals(expected, merged);
        checkEquality(expected.getAssociatedFields(), merged.getAssociatedFields());
        Assert.assertEquals(expected.isMutable(), merged.isMutable());
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#intersectionWith(ParameterDescriptor)}
     */
    @Test
    public void testIntersectionWith() {
        final ParameterDescriptor descriptor1 = new ParameterDescriptor();
        descriptor1.addField(ID, "SAT1");
        descriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor1.addField(COORDINATE, CartesianCoordinate.X);
        descriptor1.addField(NUMBER, 10);
        descriptor1.setMutability(false);

        final ParameterDescriptor descriptor2 = new ParameterDescriptor();
        descriptor2.addField(ID, "SAT1");
        descriptor2.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        descriptor2.addField(COORDINATE, CartesianCoordinate.Y);
        descriptor2.addField(NUMBER, 10);
        descriptor2.setMutability(true);

        ParameterDescriptor expected;
        ParameterDescriptor intersection;

        // Standard intersection
        intersection = descriptor1.intersectionWith(descriptor2);
        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(NUMBER, 10);
        expected.setMutability(false);
        Assert.assertEquals(expected, intersection);
        checkEquality(expected.getAssociatedFields(), intersection.getAssociatedFields());
        Assert.assertEquals(expected.isMutable(), intersection.isMutable());

        // The other parameter descriptor is null
        intersection = descriptor1.intersectionWith(null);
        expected = new ParameterDescriptor();
        expected.setMutability(false);
        Assert.assertEquals(expected, intersection);
        checkEquality(expected.getAssociatedFields(), intersection.getAssociatedFields());
        Assert.assertEquals(expected.isMutable(), intersection.isMutable());
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#getName()}<br>
     * {@linkplain ParameterDescriptor#getName(String)}<br>
     * {@linkplain ParameterDescriptor#getName(boolean)}<br>
     * {@linkplain ParameterDescriptor#getName(String, boolean)}<br>
     */
    @Test
    public void testGetName() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);

        String expected;

        // Default separator and order
        expected = "X_2000-01-01T11:59:27.816_SAT1";
        Assert.assertEquals(expected, descriptor.getName());

        // Custom separator, default order
        expected = "X|2000-01-01T11:59:27.816|SAT1";
        Assert.assertEquals(expected, descriptor.getName("|"));

        // Default separator, natural order
        expected = "SAT1_2000-01-01T11:59:27.816_X";
        Assert.assertEquals(expected, descriptor.getName(false));

        // Default separator, reverse order
        expected = "X_2000-01-01T11:59:27.816_SAT1";
        Assert.assertEquals(expected, descriptor.getName(true));

        // Custom separator, natural order
        expected = "SAT1|2000-01-01T11:59:27.816|X";
        Assert.assertEquals(expected, descriptor.getName("|", false));

        // Custom separator, reverse order
        expected = "X|2000-01-01T11:59:27.816|SAT1";
        Assert.assertEquals(expected, descriptor.getName("|", true));
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#getName()}<br>
     * {@linkplain ParameterDescriptor#getName(boolean)}<br>
     */
    @Test
    public void testGetNameWithEmptyStrings() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(new FieldDescriptor<>("", String.class), "string-1");
        descriptor.addField(new FieldDescriptor<>(" ", String.class), "string-2");
        descriptor.addField(new FieldDescriptor<>("name-1", String.class), "");
        descriptor.addField(new FieldDescriptor<>("name-2", String.class), " ");

        String expected;

        // Default order (reverse order)
        expected = "string-2_string-1";
        Assert.assertEquals(expected, descriptor.getName());

        // Natural order
        expected = "string-1_string-2";
        Assert.assertEquals(expected, descriptor.getName(false));

        // Reverse order
        expected = "string-2_string-1";
        Assert.assertEquals(expected, descriptor.getName(true));

        // Custom separator
        expected = "string-2|string-1";
        Assert.assertEquals(expected, descriptor.getName("|"));

        expected = "string-1|string-2";
        Assert.assertEquals(expected, descriptor.getName("|", false));

        expected = "string-2|string-1";
        Assert.assertEquals(expected, descriptor.getName("|", true));
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#toString()}<br>
     * {@linkplain ParameterDescriptor#toString(boolean)}<br>
     */
    @Test
    public void testToString() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);

        String expected;

        // Default order
        expected = "ParameterDescriptor[coordinate: X; date: 2000-01-01T11:59:27.816; identifier: SAT1]";
        Assert.assertEquals(expected, descriptor.toString());

        // Natural order
        expected = "ParameterDescriptor[identifier: SAT1; date: 2000-01-01T11:59:27.816; coordinate: X]";
        Assert.assertEquals(expected, descriptor.toString(false));

        // Revere order
        expected = "ParameterDescriptor[coordinate: X; date: 2000-01-01T11:59:27.816; identifier: SAT1]";
        Assert.assertEquals(expected, descriptor.toString(true));
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#toString()}<br>
     * {@linkplain ParameterDescriptor#toString(boolean)}<br>
     */
    @Test
    public void testToStringWithEmptyStrings() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(new FieldDescriptor<>("", String.class), "");
        descriptor.addField(new FieldDescriptor<>("", String.class), "string-1");
        descriptor.addField(new FieldDescriptor<>(" ", String.class), "string-2");
        descriptor.addField(new FieldDescriptor<>("name-1", String.class), "");
        descriptor.addField(new FieldDescriptor<>("name-2", String.class), " ");

        String expected;

        // Default order
        expected = "ParameterDescriptor[name-2: ; name-1: ; : string-2; : string-1]";
        Assert.assertEquals(expected, descriptor.toString());

        // Natural order
        expected = "ParameterDescriptor[: string-1; : string-2; name-1: ; name-2: ]";
        Assert.assertEquals(expected, descriptor.toString(false));

        // Revere order
        expected = "ParameterDescriptor[name-2: ; name-1: ; : string-2; : string-1]";
        Assert.assertEquals(expected, descriptor.toString(true));
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#toString(String, String, boolean, boolean, boolean, boolean)}<br>
     */
    @Test
    public void testToCustomString() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(ID, "SAT1");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);

        String expected;

        // Print everything
        expected = "ParameterDescriptor[identifier=SAT1 | date=2000-01-01T11:59:27.816 | coordinate=X]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, true, true, false));

        expected = "ParameterDescriptor[coordinate=X | date=2000-01-01T11:59:27.816 | identifier=SAT1]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, true, true, true));

        // Do not print the name of the class
        expected = "identifier=SAT1 | date=2000-01-01T11:59:27.816 | coordinate=X";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", false, true, true, false));

        expected = "coordinate=X | date=2000-01-01T11:59:27.816 | identifier=SAT1";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", false, true, true, true));

        // Do not print the field values
        expected = "ParameterDescriptor[identifier | date | coordinate]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, true, false, false));

        expected = "ParameterDescriptor[coordinate | date | identifier]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, true, false, true));

        // Do not print the name of the field descriptors
        expected = "ParameterDescriptor[SAT1 | 2000-01-01T11:59:27.816 | X]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, false, true, false));

        expected = "ParameterDescriptor[X | 2000-01-01T11:59:27.816 | SAT1]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, false, true, true));
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#toString(String, String, boolean, boolean, boolean, boolean)}<br>
     */
    @Test
    public void testToCustomStringWithEmptyStrings() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(new FieldDescriptor<>("", String.class), "string-1");
        descriptor.addField(new FieldDescriptor<>(" ", String.class), "string-2");
        descriptor.addField(new FieldDescriptor<>("name-1", String.class), "");
        descriptor.addField(new FieldDescriptor<>("name-2", String.class), " ");

        String expected;

        expected = "ParameterDescriptor[=string-1 | =string-2 | name-1= | name-2=]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, true, true, false));

        expected = "ParameterDescriptor[name-2= | name-1= | =string-2 | =string-1]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, true, true, true));

        // Do not print the name of the class
        expected = "=string-1 | =string-2 | name-1= | name-2=";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", false, true, true, false));

        expected = "name-2= | name-1= | =string-2 | =string-1";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", false, true, true, true));

        // Do not print the field values
        expected = "ParameterDescriptor[name-1 | name-2]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, true, false, false));

        expected = "ParameterDescriptor[name-2 | name-1]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, true, false, true));

        // Do not print the name of the field descriptors
        expected = "ParameterDescriptor[string-1 | string-2]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, false, true, false));

        expected = "ParameterDescriptor[string-2 | string-1]";
        Assert.assertEquals(expected, descriptor.toString("=", " | ", true, false, true, true));
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterDescriptor#copy()}<br>
     */
    @Test
    public void testCopy() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");
        descriptor.setMutability(false);

        final ParameterDescriptor copy = descriptor.copy();
        checkEquality(copy.getAssociatedFields(), descriptor.getAssociatedFields());
        Assert.assertNotSame(copy.getAssociatedFields(), descriptor.getAssociatedFields());
        Assert.assertEquals(copy.isMutable(), descriptor.isMutable());
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#equals(Object)}<br>
     * {@linkplain ParameterDescriptor#hashCode()}
     */
    @Test
    public void testEqualsAndHashCode() {
        ParameterDescriptor other;

        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");
        descriptor.setMutability(true);

        // Check the hashCode consistency between calls
        final int hashCode = descriptor.hashCode();
        Assert.assertEquals(hashCode, descriptor.hashCode());

        // Compared object is null
        Assert.assertFalse(descriptor.equals(null));

        // Compared object is a different class
        Assert.assertFalse(descriptor.equals(new Object()));

        // Same instance
        Assert.assertTrue(descriptor.equals(descriptor));

        // Same data, but different instances
        other = descriptor.copy();
        Assert.assertTrue(descriptor.equals(other));
        Assert.assertTrue(other.equals(descriptor));
        Assert.assertTrue(descriptor.hashCode() == other.hashCode());

        // Same field descriptors, but different values
        other = descriptor.copy();
        other.addField(STRING, "new-value");
        Assert.assertFalse(descriptor.equals(other));
        Assert.assertFalse(other.equals(descriptor));
        Assert.assertFalse(descriptor.hashCode() == other.hashCode());

        // Different field descriptors
        other = new ParameterDescriptor();
        Assert.assertFalse(descriptor.equals(other));
        Assert.assertFalse(other.equals(descriptor));
        Assert.assertFalse(descriptor.hashCode() == other.hashCode());

        // Different mutability state
        other = descriptor.copy();
        other.setMutability(false);
        Assert.assertTrue(descriptor.equals(other));
        Assert.assertTrue(other.equals(descriptor));
        Assert.assertTrue(descriptor.hashCode() == other.hashCode());
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterDescriptor#areEqual(ParameterDescriptor, ParameterDescriptor, FieldDescriptor)}<br>
     * {@linkplain ParameterDescriptor#areEqual(ParameterDescriptor, ParameterDescriptor, Collection)}<br>
     */
    @Test
    public void testAreEquals() {
        final ParameterDescriptor descriptor1 = new ParameterDescriptor();
        descriptor1.addField(ID, "SAT1");
        descriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor1.addField(COORDINATE, CartesianCoordinate.X);
        descriptor1.addField(NUMBER, 10);
        descriptor1.setMutability(true);

        final ParameterDescriptor descriptor2 = new ParameterDescriptor();
        descriptor2.addField(ID, "SAT2");
        descriptor2.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor2.addField(COORDINATE, CartesianCoordinate.X);
        descriptor2.setMutability(false);

        // Same instance
        Assert.assertTrue(descriptor1.equals(descriptor1, ID));
        Assert.assertTrue(ParameterDescriptor.areEqual(descriptor1, descriptor1, ID));

        // One of the instances is null
        Assert.assertFalse(descriptor1.equals(null, ID));
        Assert.assertFalse(ParameterDescriptor.areEqual(null, descriptor2, ID));
        Assert.assertFalse(ParameterDescriptor.areEqual(descriptor1, null, ID));

        // Both instances are null
        Assert.assertTrue(ParameterDescriptor.areEqual(null, null, ID));

        // The specified field descriptor is not associated with one of the instances
        Assert.assertFalse(descriptor1.equals(descriptor2, NUMBER));
        Assert.assertFalse(descriptor2.equals(descriptor1, NUMBER));
        Assert.assertFalse(ParameterDescriptor.areEqual(descriptor1, descriptor2, NUMBER));

        // The specified field descriptor is associated with both instances, but not mapped to the
        // same value
        Assert.assertFalse(descriptor1.equals(descriptor2, ID));
        Assert.assertFalse(descriptor2.equals(descriptor1, ID));
        Assert.assertFalse(ParameterDescriptor.areEqual(descriptor1, descriptor2, ID));

        // Compare multiple field descriptors
        Assert.assertTrue(descriptor1.equals(descriptor2, DATE, COORDINATE));
        Assert.assertTrue(descriptor2.equals(descriptor1, DATE, COORDINATE));
        Assert.assertTrue(ParameterDescriptor.areEqual(descriptor1, descriptor2, DATE, COORDINATE));

        Assert.assertFalse(descriptor1.equals(descriptor2, ID, DATE, COORDINATE));
        Assert.assertFalse(descriptor2.equals(descriptor1, ID, DATE, COORDINATE));
        Assert.assertFalse(ParameterDescriptor.areEqual(descriptor1, descriptor2, ID, DATE,
                COORDINATE));

        Assert.assertTrue(descriptor1.equals(descriptor2, Arrays.asList(DATE, COORDINATE)));
        Assert.assertTrue(descriptor2.equals(descriptor1, Arrays.asList(DATE, COORDINATE)));
        Assert.assertTrue(ParameterDescriptor.areEqual(descriptor1, descriptor2,
                Arrays.asList(DATE, COORDINATE)));

        Assert.assertFalse(descriptor1.equals(descriptor2, Arrays.asList(ID, DATE, COORDINATE)));
        Assert.assertFalse(descriptor2.equals(descriptor1, Arrays.asList(ID, DATE, COORDINATE)));
        Assert.assertFalse(ParameterDescriptor.areEqual(descriptor1, descriptor2,
                Arrays.asList(ID, DATE, COORDINATE)));

        // The specified field descriptor is null
        try {
            descriptor1.equals(descriptor2, (FieldDescriptor<?>) null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptor)", e.getMessage());
        }

        try {
            ParameterDescriptor.areEqual(descriptor1, descriptor2, (FieldDescriptor<?>) null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptor)", e.getMessage());
        }

        // The specified collection of field descriptors is null
        try {
            descriptor1.equals(descriptor2, (Collection<FieldDescriptor<?>>) null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptors)", e.getMessage());
        }

        try {
            ParameterDescriptor.areEqual(descriptor1, descriptor2,
                    (Collection<FieldDescriptor<?>>) null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptors)", e.getMessage());
        }

        try {
            descriptor1.equals(descriptor2, (FieldDescriptor<?>[]) null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptors)", e.getMessage());
        }

        try {
            ParameterDescriptor.areEqual(descriptor1, descriptor2, (FieldDescriptor<?>[]) null);
            Assert.fail();
        } catch (final NullArgumentException e) {
            Assert.assertEquals("A non-null value is expected (field descriptors)", e.getMessage());
        }
    }

    /**
     * Tests the serialization/deserialization.
     */
    @Test
    public void testSerialization() {
        final ParameterDescriptor descriptor = new ParameterDescriptor();
        descriptor.addField(STRING, "initial-value");
        descriptor.addField(DATE, AbsoluteDate.J2000_EPOCH);
        descriptor.addField(COORDINATE, CartesianCoordinate.X);
        descriptor.addField(NUMBER, 10);
        descriptor.addField(ID, "SAT1");

        final ParameterDescriptor deserialized = TestUtils.serializeAndRecover(descriptor);
        Assert.assertEquals(descriptor, deserialized);
        Assert.assertNotSame(descriptor, deserialized);
        Assert.assertEquals(descriptor.hashCode(), deserialized.hashCode());
    }

    /**
     * Checks if two collections are equal (order matters), and throws an exception if that's not
     * the case.
     *
     * @param <T>
     *        the type of the elements in the collections
     * @param expected
     *        the expected collection
     * @param actual
     *        the collection tested
     */
    private static <T> void checkEquality(final Collection<T> expected, final Collection<T> actual) {
        if ((expected == null) ^ (actual == null)) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            Assert.assertEquals(expected.size(), actual.size());

            final Iterator<T> iteratorE = expected.iterator();
            final Iterator<T> iteratorA = actual.iterator();

            while (iteratorE.hasNext() && iteratorA.hasNext()) {
                final T itemE = iteratorE.next();
                final T itemA = iteratorA.next();
                Assert.assertEquals(itemE, itemA);
            }
        }
    }

    /**
     * Checks if two maps are equal (order matters), and throws an exception if that's not the case.
     *
     * @param expected
     *        the expected map
     * @param actual
     *        the map tested
     * @param <K>
     *        the type of the map's keys
     * @param <V>
     *        the type of the map's values
     */
    private static <K, V> void checkEquality(final Map<K, V> expected, final Map<K, V> actual) {
        if ((expected == null) ^ (actual == null)) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            Assert.assertEquals(expected.size(), actual.size());

            final Iterator<Map.Entry<K, V>> iteratorE = expected.entrySet().iterator();
            final Iterator<Map.Entry<K, V>> iteratorA = actual.entrySet().iterator();

            while (iteratorE.hasNext() && iteratorA.hasNext()) {
                final Map.Entry<K, V> entryE = iteratorE.next();
                final Map.Entry<K, V> entryA = iteratorA.next();
                Assert.assertEquals(entryE.getKey(), entryA.getKey());
                Assert.assertEquals(entryE.getValue(), entryA.getValue());
            }
        }
    }
}
