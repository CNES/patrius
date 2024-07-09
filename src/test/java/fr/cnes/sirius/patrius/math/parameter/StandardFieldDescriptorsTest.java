/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2017 CNES
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
 */
package fr.cnes.sirius.patrius.math.parameter;

import java.io.File;
import java.util.Locale;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.PatriusUtils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@linkplain StandardFieldDescriptors}.
 *
 * @author Pierre Seimandi (GMV)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* END-HISTORY
 */
public class StandardFieldDescriptorsTest {

    /**
     * Initialization.
     *
     * @throws PatriusException
     *         if an error occurs during the initialization
     */
    @BeforeClass
    public static void initPatrius() throws PatriusException {
        // Set the default locale
        Locale.setDefault(Locale.ENGLISH);

        // Clear the data providers
        PatriusUtils.clearDataProviders();

        // Add the JPL ephemerides to the dataset folders
        // (required to build third body attraction forces)
        final File folder = PatriusUtils.getSystemResource("jplEphemeris/");
        PatriusUtils.addDatasetFolder(folder);
    }

    /**
     * Tests {@linkplain StandardFieldDescriptors#PARAMETER_NAME}.
     */
    @Test
    public void testParameterName() {
        final FieldDescriptor<String> descriptor = StandardFieldDescriptors.PARAMETER_NAME;
        final Function<String, String> initialPrintFunction = descriptor.getPrintFunction();

        Assert.assertEquals("parameter_name", descriptor.getName());
        Assert.assertEquals(String.class, descriptor.getFieldClass());
        Assert.assertNull(descriptor.getPrintFunction());

        Object field;
        String expectedString;

        // The field is not a string
        field = 1.2;
        expectedString = "1.2";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is a string
        field = "first string";
        expectedString = "first string";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is a string and a custom print function was set
        descriptor.setPrintFunction((object) -> object.getClass().getSimpleName() + ": "
                + object.toString());
        field = "second string";
        expectedString = "String: second string";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // Reset the print function to its original value
        descriptor.setPrintFunction(initialPrintFunction);
    }

    /**
     * Tests {@linkplain StandardFieldDescriptors#FORCE_MODEL}.
     *
     * @throws PatriusException
     *         if an error occurs while building the force model used for testing
     */
    @Test
    public void testForceModel() throws PatriusException {
        final FieldDescriptor<Class<? extends ForceModel>> descriptor = StandardFieldDescriptors.FORCE_MODEL;
        final Function<Class<? extends ForceModel>, String> initialPrintFunction = descriptor
                .getPrintFunction();

        Assert.assertEquals("force_model", descriptor.getName());
        Assert.assertEquals(Class.class, descriptor.getFieldClass());
        Assert.assertNotNull(descriptor.getPrintFunction());

        final ThirdBodyAttraction force = new ThirdBodyAttraction(CelestialBodyFactory.getEarth());

        Object field;
        String expectedString;

        // The field is not a force model
        field = 1.2;
        expectedString = "1.2";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is a force model
        field = force;
        expectedString = "fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction@";
        Assert.assertTrue(descriptor.printField(field).startsWith(expectedString));

        // The field is a class of an object which is not a force model
        field = Double.class;
        expectedString = "Double";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is the class of a force model
        field = force.getClass();
        expectedString = "ThirdBodyAttraction";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is a force model and a custom print function was set
        descriptor.setPrintFunction((object) -> "Force model: " + object.getSimpleName());
        field = force.getClass();
        expectedString = "Force model: ThirdBodyAttraction";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // Reset the print function to its original value
        descriptor.setPrintFunction(initialPrintFunction);
    }

    /**
     * Tests {@linkplain StandardFieldDescriptors#DATE}.
     */
    @Test
    public void testDate() {
        final FieldDescriptor<AbsoluteDate> descriptor = StandardFieldDescriptors.DATE;
        final Function<AbsoluteDate, String> initialPrintFunction = descriptor.getPrintFunction();

        Assert.assertEquals("date", descriptor.getName());
        Assert.assertEquals(AbsoluteDate.class, descriptor.getFieldClass());
        Assert.assertNull(descriptor.getPrintFunction());

        Object field;
        String expectedString;

        // The field is not a date
        field = 1.2;
        expectedString = "1.2";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is a date
        field = AbsoluteDate.J2000_EPOCH;
        expectedString = "2000-01-01T11:59:27.816";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is a date and a custom print function was set
        descriptor.setPrintFunction((object) -> object.getClass().getSimpleName() + ": "
                + object.toString(0));
        field = AbsoluteDate.CCSDS_EPOCH;
        expectedString = "AbsoluteDate: 1958-01-01T00:00:00";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // Reset the print function to its original value
        descriptor.setPrintFunction(initialPrintFunction);
    }

    /**
     * Tests {@linkplain StandardFieldDescriptors#DATE_INTERVAL}.
     */
    @Test
    public void testDateInterval() {
        final FieldDescriptor<AbsoluteDateInterval> descriptor = StandardFieldDescriptors.DATE_INTERVAL;
        final Function<AbsoluteDateInterval, String> initialPrintFunction = descriptor
                .getPrintFunction();

        Assert.assertEquals("date_interval", descriptor.getName());
        Assert.assertEquals(AbsoluteDateInterval.class, descriptor.getFieldClass());
        Assert.assertNotNull(descriptor.getPrintFunction());

        Object field;
        String expectedString;

        // The field is not a date interval
        field = 1.2;
        expectedString = "1.2";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is a date interval
        field = new AbsoluteDateInterval(AbsoluteDate.CCSDS_EPOCH, AbsoluteDate.J2000_EPOCH);
        expectedString = "1958-01-01T00:00:00.000@2000-01-01T11:59:27.816";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is a date interval and a custom print function was set
        descriptor.setPrintFunction((object) -> object.getClass().getSimpleName() + ": "
                + object.toString());
        field = new AbsoluteDateInterval(AbsoluteDate.CCSDS_EPOCH, AbsoluteDate.J2000_EPOCH);
        expectedString = "AbsoluteDateInterval: [ 1958-01-01T00:00:00.000 ; 2000-01-01T11:59:27.816 ]";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // Reset the print function to its original value
        descriptor.setPrintFunction(initialPrintFunction);
    }

    /**
     * Tests {@linkplain StandardFieldDescriptors#ORBITAL_COORDINATE}.
     */
    @Test
    public void testOrbitalCoordinate() {
        final FieldDescriptor<OrbitalCoordinate> descriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;
        final Function<OrbitalCoordinate, String> initialPrintFunction = descriptor
                .getPrintFunction();

        Assert.assertEquals("orbital_coordinate", descriptor.getName());
        Assert.assertEquals(OrbitalCoordinate.class, descriptor.getFieldClass());
        Assert.assertNull(descriptor.getPrintFunction());

        Object field;
        String expectedString;

        // The field is not an orbital coordinate
        field = "test string";
        expectedString = "test string";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is an orbital coordinate
        field = CartesianCoordinate.X;
        expectedString = "X";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // The field is an orbital coordinate and a custom print function was set
        descriptor.setPrintFunction((object) -> object.getClass().getSimpleName() + ": "
                + object.toString());
        field = CartesianCoordinate.VY;
        expectedString = "CartesianCoordinate: VY";
        Assert.assertEquals(expectedString, descriptor.printField(field));

        // Reset the print function to its original value
        descriptor.setPrintFunction(initialPrintFunction);
    }
}
