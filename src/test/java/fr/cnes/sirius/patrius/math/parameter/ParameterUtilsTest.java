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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.EquinoctialCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Unit tests for {@linkplain ParameterUtils}.
 *
 * @author Pierre Seimandi (GMV)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* END-HISTORY
 */
public class ParameterUtilsTest {

    /** Field descriptor representing an ID. */
    private static final FieldDescriptor<String> ID = new FieldDescriptor<>("identifier",
            String.class);

    /** Field descriptor representing an orbital coordinate. */
    private static final FieldDescriptor<OrbitalCoordinate> COORDINATE = new FieldDescriptor<>(
            "coordinate", OrbitalCoordinate.class);

    /** Field descriptor representing a date. */
    private static final FieldDescriptor<AbsoluteDate> DATE = new FieldDescriptor<>("date",
            AbsoluteDate.class);

    /** Field descriptor representing a number. */
    private static final FieldDescriptor<Number> NUMBER = new FieldDescriptor<>("number",
            Number.class);

    /**
     * Initialization.
     */
    @BeforeClass
    public static void init() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#addFieldToParameters(IParameterizable, FieldDescriptor, Object)}
     */
    @Test
    public void testAddFieldToParameterizable() {
        IParameterizable parameterizable;
        ParameterDescriptor expected;

        final ParameterDescriptor parameterDescriptor1 = new ParameterDescriptor();
        parameterDescriptor1.addField(ID, "SAT1");
        parameterDescriptor1.addField(COORDINATE, CartesianCoordinate.X);
        parameterDescriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);

        final ParameterDescriptor parameterDescriptor2 = new ParameterDescriptor();
        parameterDescriptor2.addField(ID, "SAT1");
        parameterDescriptor2.addField(COORDINATE, CartesianCoordinate.Y);

        final ParameterDescriptor parameterDescriptor3 = new ParameterDescriptor();
        parameterDescriptor3.addField(ID, "SAT1");
        parameterDescriptor3.addField(COORDINATE, CartesianCoordinate.Z);
        parameterDescriptor3.setMutability(false);

        final ParameterDescriptor parameterDescriptor4 = null;

        final Parameter parameter1 = new Parameter(parameterDescriptor1, 1.5);
        final Parameter parameter2 = new Parameter(parameterDescriptor2, 7.3);
        final Parameter parameter3 = new Parameter(parameterDescriptor3, 4.7);
        final Parameter parameter4 = new Parameter(parameterDescriptor4, 2.4);

        // The parameterizable instance is null
        parameterizable = null;
        ParameterUtils.addFieldToParameters(parameterizable, DATE, AbsoluteDate.CCSDS_EPOCH);

        // The list of parameters is not null, but it contains a null value and an immutable
        // instance
        final List<Parameter> parameters = Arrays.asList(parameter1, parameter2, parameter3,
                parameter4, null);
        parameterizable = new SimpleParameterizable(parameters);
        ParameterUtils.addFieldToParameters(parameterizable, DATE, AbsoluteDate.CCSDS_EPOCH);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.X);
        expected.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        Assert.assertEquals(expected, parameter1.getDescriptor());
        Assert.assertEquals(1.5, parameter1.getValue(), 0.);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.Y);
        expected.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        Assert.assertEquals(expected, parameter2.getDescriptor());
        Assert.assertEquals(7.3, parameter2.getValue(), 0.);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.Z);
        Assert.assertEquals(expected, parameter3.getDescriptor());
        Assert.assertEquals(4.7, parameter3.getValue(), 0.);

        expected = null;
        Assert.assertEquals(expected, parameter4.getDescriptor());
        Assert.assertEquals(2.4, parameter4.getValue(), 0.);
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#addFieldToParameters(Collection, FieldDescriptor, Object)}
     */
    @Test
    public void testAddFieldToParameters() {
        List<Parameter> parameters;
        ParameterDescriptor expected;

        final ParameterDescriptor parameterDescriptor1 = new ParameterDescriptor();
        parameterDescriptor1.addField(ID, "SAT1");
        parameterDescriptor1.addField(COORDINATE, CartesianCoordinate.X);
        parameterDescriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);

        final ParameterDescriptor parameterDescriptor2 = new ParameterDescriptor();
        parameterDescriptor2.addField(ID, "SAT1");
        parameterDescriptor2.addField(COORDINATE, CartesianCoordinate.Y);

        final ParameterDescriptor parameterDescriptor3 = new ParameterDescriptor();
        parameterDescriptor3.addField(ID, "SAT1");
        parameterDescriptor3.addField(COORDINATE, CartesianCoordinate.Z);
        parameterDescriptor3.setMutability(false);

        final Parameter parameter1 = new Parameter(parameterDescriptor1, 1.5);
        final Parameter parameter2 = new Parameter(parameterDescriptor2, 7.3);
        final Parameter parameter3 = new Parameter(parameterDescriptor3, 4.7);
        final Parameter parameter4 = new Parameter((ParameterDescriptor) null, 2.4);

        // The list of parameters is null.
        parameters = null;
        ParameterUtils.addFieldToParameters(parameters, DATE, AbsoluteDate.CCSDS_EPOCH);

        // The list of parameters is not null, but it contains a null value and an immutable
        // instance
        parameters = Arrays.asList(parameter1, parameter2, parameter3, parameter4, null);
        ParameterUtils.addFieldToParameters(parameters, DATE, AbsoluteDate.CCSDS_EPOCH);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.X);
        expected.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        Assert.assertEquals(expected, parameter1.getDescriptor());
        Assert.assertEquals(1.5, parameter1.getValue(), 0.);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.Y);
        expected.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        Assert.assertEquals(expected, parameter2.getDescriptor());
        Assert.assertEquals(7.3, parameter2.getValue(), 0.);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.Z);
        Assert.assertEquals(expected, parameter3.getDescriptor());
        Assert.assertEquals(4.7, parameter3.getValue(), 0.);

        expected = null;
        Assert.assertEquals(expected, parameter4.getDescriptor());
        Assert.assertEquals(2.4, parameter4.getValue(), 0.);
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#addFieldToParameterDescriptors(Collection, FieldDescriptor, Object)}
     */
    @Test
    public void testAddFieldToParameterDescriptors() {
        List<ParameterDescriptor> parameterDescriptors;
        ParameterDescriptor expected;

        final ParameterDescriptor parameterDescriptor1 = new ParameterDescriptor();
        parameterDescriptor1.addField(ID, "SAT1");
        parameterDescriptor1.addField(COORDINATE, CartesianCoordinate.X);
        parameterDescriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);

        final ParameterDescriptor parameterDescriptor2 = new ParameterDescriptor();
        parameterDescriptor2.addField(ID, "SAT1");
        parameterDescriptor2.addField(COORDINATE, CartesianCoordinate.Y);

        final ParameterDescriptor parameterDescriptor3 = new ParameterDescriptor();
        parameterDescriptor3.addField(ID, "SAT1");
        parameterDescriptor3.addField(COORDINATE, CartesianCoordinate.Z);
        parameterDescriptor3.setMutability(false);

        // The list of parameter descriptors is null.
        parameterDescriptors = null;
        ParameterUtils.addFieldToParameterDescriptors(parameterDescriptors, DATE,
                AbsoluteDate.CCSDS_EPOCH);

        // The list of parameter descriptors is not null, but it contains a null value and an
        // immutable instance
        parameterDescriptors = Arrays.asList(parameterDescriptor1, parameterDescriptor2,
                parameterDescriptor3, null);
        ParameterUtils.addFieldToParameterDescriptors(parameterDescriptors, DATE,
                AbsoluteDate.CCSDS_EPOCH);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.X);
        expected.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        Assert.assertEquals(expected, parameterDescriptor1);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.Y);
        expected.addField(DATE, AbsoluteDate.CCSDS_EPOCH);
        Assert.assertEquals(expected, parameterDescriptor2);

        expected = new ParameterDescriptor();
        expected.addField(ID, "SAT1");
        expected.addField(COORDINATE, CartesianCoordinate.Z);
        Assert.assertEquals(expected, parameterDescriptor3);
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterUtils#buildDefaultParameterDescriptors(int)}<br>
     * {@linkplain ParameterUtils#buildDefaultParameterDescriptors(int, int)}<br>
     */
    @Test
    public void testBuildDefaultParameterDescriptors() {
        List<ParameterDescriptor> actual;
        List<ParameterDescriptor> expected;

        actual = ParameterUtils.buildDefaultParameterDescriptors(0);
        expected = new ArrayList<>();
        Assert.assertEquals(expected, actual);

        actual = ParameterUtils.buildDefaultParameterDescriptors(0, 5);
        expected = new ArrayList<>();
        Assert.assertEquals(expected, actual);

        actual = ParameterUtils.buildDefaultParameterDescriptors(0, -5);
        expected = new ArrayList<>();
        Assert.assertEquals(expected, actual);

        actual = ParameterUtils.buildDefaultParameterDescriptors(5);
        expected = new ArrayList<>();
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p0"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p1"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p2"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p3"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p4"));
        Assert.assertEquals(expected, actual);

        actual = ParameterUtils.buildDefaultParameterDescriptors(5, 4);
        expected = new ArrayList<>();
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p4"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p5"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p6"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p7"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p8"));
        Assert.assertEquals(expected, actual);

        actual = ParameterUtils.buildDefaultParameterDescriptors(5, -5);
        expected = new ArrayList<>();
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p-5"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p-4"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p-3"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p-2"));
        expected.add(new ParameterDescriptor(StandardFieldDescriptors.PARAMETER_NAME, "p-1"));
        Assert.assertEquals(expected, actual);

        try {
            actual = ParameterUtils.buildDefaultParameterDescriptors(-1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Illegal Capacity: -1", e.getMessage());
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#buildOrbitalParameterDescriptors(OrbitType, PositionAngle)}<br>
     * for Cartesian coordinates.
     */
    @Test
    public void testBuildCartesianParameterDescriptors() {
        List<ParameterDescriptor> actual;
        List<ParameterDescriptor> expected;

        for (final PositionAngle positionAngle : PositionAngle.values()) {
            expected = new ArrayList<>();
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.X));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.Y));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.Z));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.VX));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.VY));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.VZ));

            actual = ParameterUtils.buildOrbitalParameterDescriptors(OrbitType.CARTESIAN,
                    positionAngle);
            Assert.assertEquals(expected, actual);
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#buildOrbitalParameterDescriptors(OrbitType, PositionAngle)}<br>
     * for Keplerian elements.
     */
    @Test
    public void testBuildKeplerianParameterDescriptors() {
        List<ParameterDescriptor> actual;
        List<ParameterDescriptor> expected;

        for (final PositionAngle positionAngle : PositionAngle.values()) {
            final KeplerianCoordinate expectedAnomalyType;
            switch (positionAngle) {
                case TRUE:
                    expectedAnomalyType = KeplerianCoordinate.TRUE_ANOMALY;
                    break;
                case MEAN:
                    expectedAnomalyType = KeplerianCoordinate.MEAN_ANOMALY;
                    break;
                case ECCENTRIC:
                    expectedAnomalyType = KeplerianCoordinate.ECCENTRIC_ANOMALY;
                    break;
                default:
                    throw new EnumConstantNotPresentException(PositionAngle.class,
                            positionAngle.name());
            }

            expected = new ArrayList<>();
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.SEMI_MAJOR_AXIS));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.ECCENTRICITY));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.INCLINATION));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.PERIGEE_ARGUMENT));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    expectedAnomalyType));

            actual = ParameterUtils.buildOrbitalParameterDescriptors(OrbitType.KEPLERIAN,
                    positionAngle);
            Assert.assertEquals(expected, actual);
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#buildOrbitalParameterDescriptors(OrbitType, PositionAngle)}<br>
     * for equinoctial elements.
     */
    @Test
    public void testBuildEquinoctialParameterDescriptors() {
        List<ParameterDescriptor> actual;
        List<ParameterDescriptor> expected;

        for (final PositionAngle positionAngle : PositionAngle.values()) {
            final EquinoctialCoordinate expectedLongitudeType;
            switch (positionAngle) {
                case TRUE:
                    expectedLongitudeType = EquinoctialCoordinate.TRUE_LONGITUDE_ARGUMENT;
                    break;
                case MEAN:
                    expectedLongitudeType = EquinoctialCoordinate.MEAN_LONGITUDE_ARGUMENT;
                    break;
                case ECCENTRIC:
                    expectedLongitudeType = EquinoctialCoordinate.ECCENTRIC_LONGITUDE_ARGUMENT;
                    break;
                default:
                    throw new EnumConstantNotPresentException(PositionAngle.class,
                            positionAngle.name());
            }

            expected = new ArrayList<>();
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.SEMI_MAJOR_AXIS));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.E_X));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.E_Y));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.H_X));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.H_Y));
            expected.add(new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    expectedLongitudeType));

            actual = ParameterUtils.buildOrbitalParameterDescriptors(OrbitType.EQUINOCTIAL,
                    positionAngle);
            Assert.assertEquals(expected, actual);
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#buildOrbitalParameters(OrbitType, PositionAngle)}<br>
     * for Cartesian coordinates.
     */
    @Test
    public void testBuildCartesianParameters() {
        List<Parameter> actual;
        List<Parameter> expected;
        ParameterDescriptor descriptor;

        for (final PositionAngle positionAngle : PositionAngle.values()) {
            expected = new ArrayList<>();
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.X);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.Y);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.Z);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.VX);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.VY);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    CartesianCoordinate.VZ);
            expected.add(new Parameter(descriptor, 0.));

            actual = ParameterUtils.buildOrbitalParameters(OrbitType.CARTESIAN, positionAngle);
            checkEquality(expected, actual);
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#buildOrbitalParameters(OrbitType, PositionAngle)}<br>
     * for Keplerian elements.
     */
    @Test
    public void testBuildKeplerianParameters() {
        List<Parameter> actual;
        List<Parameter> expected;
        ParameterDescriptor descriptor;

        for (final PositionAngle positionAngle : PositionAngle.values()) {
            final KeplerianCoordinate expectedAnomalyType;
            switch (positionAngle) {
                case TRUE:
                    expectedAnomalyType = KeplerianCoordinate.TRUE_ANOMALY;
                    break;
                case MEAN:
                    expectedAnomalyType = KeplerianCoordinate.MEAN_ANOMALY;
                    break;
                case ECCENTRIC:
                    expectedAnomalyType = KeplerianCoordinate.ECCENTRIC_ANOMALY;
                    break;
                default:
                    throw new EnumConstantNotPresentException(PositionAngle.class,
                            positionAngle.name());
            }

            expected = new ArrayList<>();
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.SEMI_MAJOR_AXIS);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.ECCENTRICITY);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.INCLINATION);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.PERIGEE_ARGUMENT);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    KeplerianCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    expectedAnomalyType);
            expected.add(new Parameter(descriptor, 0.));

            actual = ParameterUtils.buildOrbitalParameters(OrbitType.KEPLERIAN, positionAngle);
            checkEquality(expected, actual);
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#buildOrbitalParameters(OrbitType, PositionAngle)}<br>
     * for equinoctial elements.
     */
    @Test
    public void testBuildEquinoctialParameters() {
        List<Parameter> actual;
        List<Parameter> expected;
        ParameterDescriptor descriptor;

        for (final PositionAngle positionAngle : PositionAngle.values()) {
            final EquinoctialCoordinate expectedLongitudeType;
            switch (positionAngle) {
                case TRUE:
                    expectedLongitudeType = EquinoctialCoordinate.TRUE_LONGITUDE_ARGUMENT;
                    break;
                case MEAN:
                    expectedLongitudeType = EquinoctialCoordinate.MEAN_LONGITUDE_ARGUMENT;
                    break;
                case ECCENTRIC:
                    expectedLongitudeType = EquinoctialCoordinate.ECCENTRIC_LONGITUDE_ARGUMENT;
                    break;
                default:
                    throw new EnumConstantNotPresentException(PositionAngle.class,
                            positionAngle.name());
            }

            expected = new ArrayList<>();
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.SEMI_MAJOR_AXIS);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.E_X);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.E_Y);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.H_X);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    EquinoctialCoordinate.H_Y);
            expected.add(new Parameter(descriptor, 0.));
            descriptor = new ParameterDescriptor(StandardFieldDescriptors.ORBITAL_COORDINATE,
                    expectedLongitudeType);
            expected.add(new Parameter(descriptor, 0.));

            actual = ParameterUtils.buildOrbitalParameters(OrbitType.EQUINOCTIAL, positionAngle);
            checkEquality(expected, actual);
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#concatenateParameterNames(Collection, String, String, boolean)}<br>
     */
    @Test
    public void testConcatenateParametersNames() {
        final ParameterDescriptor parameterDescriptor1 = new ParameterDescriptor();
        parameterDescriptor1.addField(ID, "SAT1");
        parameterDescriptor1.addField(COORDINATE, CartesianCoordinate.X);
        parameterDescriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);

        final ParameterDescriptor parameterDescriptor2 = new ParameterDescriptor();
        parameterDescriptor2.addField(ID, "SAT1");
        parameterDescriptor2.addField(COORDINATE, CartesianCoordinate.Y);

        final ParameterDescriptor parameterDescriptor3 = new ParameterDescriptor();
        parameterDescriptor3.addField(ID, "SAT1");
        parameterDescriptor3.addField(COORDINATE, CartesianCoordinate.Z);
        parameterDescriptor3.setMutability(false);

        final ParameterDescriptor parameterDescriptor4 = null;

        final Parameter parameter1 = new Parameter(parameterDescriptor1, 1.5);
        final Parameter parameter2 = new Parameter(parameterDescriptor2, 7.3);
        final Parameter parameter3 = new Parameter(parameterDescriptor3, 4.7);
        final Parameter parameter4 = new Parameter(parameterDescriptor4, 2.4);

        // The list of parameters is not null, but it contains a null value and a parameter
        // associated with a null parameter descriptor
        final List<Parameter> parameters = Arrays.asList(parameter1, parameter2, parameter3,
                parameter4, null);
        final String expected = "SAT1_X_2000-01-01T11:59:27.816|SAT1_Y|SAT1_Z";
        final String actual = ParameterUtils.concatenateParameterNames(parameters, "|", "_", false);
        Assert.assertEquals(expected, actual);

        // The list of parameters is null
        try {
            ParameterUtils.concatenateParameterNames(null, "|", "_", false);
            Assert.fail();
        } catch (final NullPointerException e) {
            // Don't check the exception message since the exception thrown is not a custom one
        }
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#concatenateParameterNames(Collection, String, String, boolean)}<br>
     */
    @Test
    public void testConcatenateParameterDescriptorsNames() {
        final ParameterDescriptor parameterDescriptor1 = new ParameterDescriptor();
        parameterDescriptor1.addField(ID, "SAT1");
        parameterDescriptor1.addField(COORDINATE, CartesianCoordinate.X);
        parameterDescriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);

        final ParameterDescriptor parameterDescriptor2 = new ParameterDescriptor();
        parameterDescriptor2.addField(ID, "SAT1");
        parameterDescriptor2.addField(COORDINATE, CartesianCoordinate.Y);

        final ParameterDescriptor parameterDescriptor3 = new ParameterDescriptor();
        parameterDescriptor3.addField(ID, "SAT1");
        parameterDescriptor3.addField(COORDINATE, CartesianCoordinate.Z);
        parameterDescriptor3.setMutability(false);

        // The list of parameter descriptors is not null, but it contains a null value
        final List<ParameterDescriptor> parameterDescriptors = Arrays.asList(parameterDescriptor1,
                parameterDescriptor2, parameterDescriptor3, null);
        final String expected = "SAT1_X_2000-01-01T11:59:27.816|SAT1_Y|SAT1_Z";
        final String actual = ParameterUtils.concatenateParameterDescriptorNames(
                parameterDescriptors, "|", "_", false);
        Assert.assertEquals(expected, actual);

        // The list of parameter descriptors is null
        try {
            ParameterUtils.concatenateParameterDescriptorNames(null, "|", "_", false);
            Assert.fail();
        } catch (final NullPointerException e) {
            // Don't check the exception message since the exception thrown is not a custom one
        }
    }

    /**
     * Tests the methods:<br>
     * {@linkplain ParameterUtils#extractParameters(Collection, FieldDescriptor)}<br>
     * {@linkplain ParameterUtils#extractParameters(Collection, FieldDescriptor, Predicate)}<br>
     */
    @Test
    public void testExtractParameters() {
        List<Parameter> parameters;
        List<Parameter> actual;
        List<Parameter> expected;

        final ParameterDescriptor parameterDescriptor1 = new ParameterDescriptor();
        parameterDescriptor1.addField(ID, "SAT1");
        parameterDescriptor1.addField(COORDINATE, CartesianCoordinate.X);
        parameterDescriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);

        final ParameterDescriptor parameterDescriptor2 = new ParameterDescriptor();
        parameterDescriptor2.addField(ID, "SAT1");
        parameterDescriptor2.addField(COORDINATE, CartesianCoordinate.Y);
        parameterDescriptor2.addField(NUMBER, 10248);

        final ParameterDescriptor parameterDescriptor3 = new ParameterDescriptor();
        parameterDescriptor3.addField(ID, "SAT2");
        parameterDescriptor3.addField(COORDINATE, CartesianCoordinate.Z);
        parameterDescriptor3.addField(NUMBER, 72994);
        parameterDescriptor3.setMutability(false);

        final ParameterDescriptor parameterDescriptor4 = null;

        final Parameter parameter1 = new Parameter(parameterDescriptor1, 1.5);
        final Parameter parameter2 = new Parameter(parameterDescriptor2, 7.3);
        final Parameter parameter3 = new Parameter(parameterDescriptor3, 4.7);
        final Parameter parameter4 = new Parameter(parameterDescriptor4, 2.4);

        // The list of parameters is null
        expected = new ArrayList<>();
        parameters = null;
        actual = ParameterUtils.extractParameters(parameters, COORDINATE);
        checkEquality(expected, actual);
        actual = ParameterUtils.extractParameters(parameters, COORDINATE,
                (coordinate) -> coordinate.equals(CartesianCoordinate.Y));
        expected = new ArrayList<>();
        checkEquality(expected, actual);

        // The list of parameters is not null, but contains a null parameter and a parameter
        // associated with a null parameter descriptor
        parameters = Arrays.asList(parameter1, parameter2, parameter3, parameter4, null);

        expected = Arrays.asList(parameter1, parameter2, parameter3);
        actual = ParameterUtils.extractParameters(parameters, COORDINATE);
        checkEquality(expected, actual);
        actual = ParameterUtils.extractParameters(parameters, COORDINATE, null);
        checkEquality(expected, actual);

        expected = Arrays.asList(parameter1);
        actual = ParameterUtils.extractParameters(parameters, DATE);
        checkEquality(expected, actual);
        actual = ParameterUtils.extractParameters(parameters, DATE, null);
        checkEquality(expected, actual);

        expected = Arrays.asList(parameter2, parameter3);
        actual = ParameterUtils.extractParameters(parameters, NUMBER);
        checkEquality(expected, actual);
        actual = ParameterUtils.extractParameters(parameters, NUMBER, null);
        checkEquality(expected, actual);

        // Extract the parameter descriptors associated with a specific field descriptor and which
        // matches a given predicate
        expected = Arrays.asList(parameter1, parameter2);
        actual = ParameterUtils.extractParameters(parameters, ID, (id) -> id.equals("SAT1"));
        checkEquality(expected, actual);

        expected = Arrays.asList(parameter1);
        actual = ParameterUtils.extractParameters(parameters, COORDINATE,
                (coordinate) -> coordinate.equals(CartesianCoordinate.X));
        checkEquality(expected, actual);
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#extractParameterDescriptors(Collection)}
     */
    @Test
    public void testExtractParameterDescriptors1() {
        List<Parameter> parameters;
        List<ParameterDescriptor> actual;
        List<ParameterDescriptor> expected;

        final ParameterDescriptor parameterDescriptor1 = new ParameterDescriptor();
        parameterDescriptor1.addField(ID, "SAT1");
        parameterDescriptor1.addField(COORDINATE, CartesianCoordinate.X);
        parameterDescriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);

        final ParameterDescriptor parameterDescriptor2 = new ParameterDescriptor();
        parameterDescriptor2.addField(ID, "SAT1");
        parameterDescriptor2.addField(COORDINATE, CartesianCoordinate.Y);
        parameterDescriptor2.addField(NUMBER, 10248);

        final ParameterDescriptor parameterDescriptor3 = new ParameterDescriptor();
        parameterDescriptor3.addField(ID, "SAT2");
        parameterDescriptor3.addField(COORDINATE, CartesianCoordinate.Z);
        parameterDescriptor3.addField(NUMBER, 72994);
        parameterDescriptor3.setMutability(false);

        final ParameterDescriptor parameterDescriptor4 = null;

        final Parameter parameter1 = new Parameter(parameterDescriptor1, 1.5);
        final Parameter parameter2 = new Parameter(parameterDescriptor2, 7.3);
        final Parameter parameter3 = new Parameter(parameterDescriptor3, 4.7);
        final Parameter parameter4 = new Parameter(parameterDescriptor4, 2.4);

        // The list of parameters is null
        parameters = null;
        expected = new ArrayList<>();
        actual = ParameterUtils.extractParameterDescriptors(parameters);
        Assert.assertEquals(expected, actual);

        // The list of parameter descriptors is not null, but contains a null parameter and a
        // parameter associated with a null parameter descriptor
        parameters = Arrays.asList(parameter1, parameter2, parameter3, parameter4, null);
        actual = ParameterUtils.extractParameterDescriptors(parameters);
        expected = Arrays.asList(parameterDescriptor1, parameterDescriptor2, parameterDescriptor3,
                parameterDescriptor4);
        Assert.assertEquals(expected, actual);
    }

    /**
     * Tests the method:<br>
     * {@linkplain ParameterUtils#extractParameterDescriptors(Collection, FieldDescriptor)}<br>
     * {@linkplain ParameterUtils#extractParameterDescriptors(Collection, FieldDescriptor, Predicate)}
     * <br>
     */
    @Test
    public void testExtractParameterDescriptors2() {
        List<ParameterDescriptor> parameterDescriptors;
        List<ParameterDescriptor> actual;
        List<ParameterDescriptor> expected;

        final ParameterDescriptor parameterDescriptor1 = new ParameterDescriptor();
        parameterDescriptor1.addField(ID, "SAT1");
        parameterDescriptor1.addField(COORDINATE, CartesianCoordinate.X);
        parameterDescriptor1.addField(DATE, AbsoluteDate.J2000_EPOCH);

        final ParameterDescriptor parameterDescriptor2 = new ParameterDescriptor();
        parameterDescriptor2.addField(ID, "SAT1");
        parameterDescriptor2.addField(COORDINATE, CartesianCoordinate.Y);
        parameterDescriptor2.addField(NUMBER, 10248);

        final ParameterDescriptor parameterDescriptor3 = new ParameterDescriptor();
        parameterDescriptor3.addField(ID, "SAT2");
        parameterDescriptor3.addField(COORDINATE, CartesianCoordinate.Z);
        parameterDescriptor3.addField(NUMBER, 72994);
        parameterDescriptor3.setMutability(false);

        // The list of parameter descriptors is null
        expected = new ArrayList<>();
        parameterDescriptors = null;
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, COORDINATE);
        Assert.assertEquals(expected, actual);
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, COORDINATE, null);
        Assert.assertEquals(expected, actual);

        // The list of parameter descriptors is not null, but contains a null parameter and a
        // parameter associated with a null parameter descriptor
        parameterDescriptors = Arrays.asList(parameterDescriptor1, parameterDescriptor2,
                parameterDescriptor3, null);
        expected = Arrays.asList(parameterDescriptor1, parameterDescriptor2, parameterDescriptor3);
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, COORDINATE);
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, COORDINATE, null);
        Assert.assertEquals(expected, actual);

        expected = Arrays.asList(parameterDescriptor1);
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, DATE);
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, DATE, null);
        Assert.assertEquals(expected, actual);

        expected = Arrays.asList(parameterDescriptor2, parameterDescriptor3);
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, NUMBER);
        Assert.assertEquals(expected, actual);
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, NUMBER, null);
        Assert.assertEquals(expected, actual);

        // Extract the parameter descriptors associated with a specific field descriptor and which
        // matches a given predicate
        actual = ParameterUtils.extractParameterDescriptors(parameterDescriptors, ID,
                (id) -> id.equals("SAT1"));
        expected = Arrays.asList(parameterDescriptor1, parameterDescriptor2);
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that two parameters are equal and throws an {@linkplain AssertionError} if they are not.
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

    /**
     * Asserts that two collections of parameters are equal (order matters) and throws an
     * {@linkplain AssertionError} if they are not.
     *
     * @param expected
     *        the expected collection of parameters
     * @param actual
     *        the actual collection of parameters
     */
    private static void checkEquality(final Collection<Parameter> expected,
            final Collection<Parameter> actual) {
        if ((expected == null) ^ (actual == null)) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            Assert.assertEquals(expected.size(), actual.size());

            final Iterator<Parameter> iteratorE = expected.iterator();
            final Iterator<Parameter> iteratorA = actual.iterator();

            while (iteratorE.hasNext() && iteratorA.hasNext()) {
                final Parameter itemE = iteratorE.next();
                final Parameter itemA = iteratorA.next();
                checkEquality(itemE, itemA);
            }
        }
    }

    /**
     * Simple implementation of {@linkplain IParameterizable} for testing purposes.
     */
    private class SimpleParameterizable implements IParameterizable {

        /** Serial version UID. */
        private static final long serialVersionUID = 4143834239447009517L;

        /** Parameters associated with this instance. */
        private final ArrayList<Parameter> parameters;

        /**
         * Constructor.
         *
         * @param initialParameters
         *        the parameters initially associated with this instance
         */
        public SimpleParameterizable(final Collection<Parameter> initialParameters) {
            this.parameters = new ArrayList<>();
            if (initialParameters != null) {
                this.parameters.addAll(initialParameters);
            }
        }

        /**
         * Checks if a parameter is supported.
         *
         * @return {@code true} if the parameter is currently associated with this instance,
         *         {@code false} otherwise
         */
        @Override
        public boolean supportsParameter(final Parameter parameter) {
            return this.parameters.contains(parameter);
        }

        /**
         * Gets the supported parameters.
         *
         * @return the parameters currently associated with this instance
         */
        @Override
        public ArrayList<Parameter> getParameters() {
            return new ArrayList<>(this.parameters);
        }
    }
}
