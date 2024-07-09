/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2021 CNES
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
 */
package fr.cnes.sirius.patrius.covariance;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.PatriusUtils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.linear.CheckUtils;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.RealMatrixFormat;
import fr.cnes.sirius.patrius.math.linear.SymmetricPositiveMatrix;
import fr.cnes.sirius.patrius.math.parameter.FieldDescriptor;
import fr.cnes.sirius.patrius.math.parameter.ParameterDescriptor;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianCoordinate;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.OrbitalCoordinate;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@linkplain OrbitalCovariance}.
 *
 * @author Pierre Seimandi (GMV)
* HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
* END-HISTORY
 */
public class OrbitalCovarianceTest {

    /**
     * Initialization.
     *
     * @throws PatriusException
     *         if an error occurs during the initialization
     */
    @BeforeClass
    public static void init() throws PatriusException {
        // Locale
        Locale.setDefault(Locale.ENGLISH);

        // Patrius dataset
        PatriusUtils.clearDataProviders();
        final String foldername = "patriusdataset140/";
        final File folder = PatriusUtils.getSystemResource(foldername);
        PatriusUtils.addDatasetFolder(folder);

        // Frames configuration
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        FramesFactory.setConfiguration(builder.getConfiguration());
    }

    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0.;

    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /** GCRF frame. */
    private static final Frame GCRF = FramesFactory.getGCRF();

    /** Date of definition of the default orbits. */
    private static final AbsoluteDate DATE;

    /** Default Cartesian orbit used for the tests. */
    private static final Orbit CARTESIAN_ORBIT;

    /** Default Keplerian orbit used for the tests. */
    private static final Orbit KEPLERIAN_ORBIT;

    /** Default 6x6 covariance matrix used for the tests. */
    private static final SymmetricPositiveMatrix COVARIANCE_MATRIX_6X6;

    /** Default 9x9 covariance matrix used for the tests. */
    private static final SymmetricPositiveMatrix COVARIANCE_MATRIX_9X9;

    /**
     * Parameter descriptors associated with the default 9x9 covariance matrix (Cartesian
     * coordinates).
     */
    private static final List<ParameterDescriptor> CUSTOM_CARTESIAN_PARAMETER_DESCRIPTORS_9X9;

    /**
     * Parameter descriptors associated with the default 9x9 covariance matrix (Keplerian
     * parameters, true anomaly).
     */
    private static final List<ParameterDescriptor> CUSTOM_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY;

    /**
     * Parameter descriptors associated with the default 6x6 covariance matrix (Cartesian
     * coordinates).
     */
    private static final List<ParameterDescriptor> DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6;

    /**
     * Parameter descriptors associated with the default 9x9 covariance matrix (Cartesian
     * coordinates).
     */
    private static final List<ParameterDescriptor> DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9;

    /**
     * Parameter descriptors associated with the default 9x9 covariance matrix (Keplerian
     * parameters, true anomaly).
     */
    private static final List<ParameterDescriptor> DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY;

    /**
     * Parameter descriptors associated with the default 9x9 covariance matrix (Keplerian
     * parameters, eccentric anomaly).
     */
    private static final List<ParameterDescriptor> DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY;

    /** Expected message format for exceptions thrown when a null argument is detected. */
    private static final String NULL_ARGUMENT_FORMAT = "A non-null value is expected (%s)";

    /** Expected message format for exceptions thrown when a a covariance size is invalid. */
    private static final String INVALID_COVARIANCE_SIZE_FORMAT = "Invalid orbital coordinate descriptor: row number %d is mapped to %s (wrong %s: %s != %s)";

    static {
        double[][] data;
        List<ParameterDescriptor> parameterDescriptors;

        // Field descriptors
        final FieldDescriptor<OrbitalCoordinate> coordinateDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;
        final FieldDescriptor<String> parameterNameDescriptor = StandardFieldDescriptors.PARAMETER_NAME;

        // Default date
        DATE = new AbsoluteDate("2017-11-07T23:29:09.080", TimeScalesFactory.getTAI());

        // Default orbits
        final Vector3D position = new Vector3D(1123783.5101019042, 5726033.748189187,
                4006400.314678921);
        final Vector3D velocity = new Vector3D(2084.2377426112616, 3863.6251103548725,
                -6087.970527060892);
        final Vector3D acceleriation = new Vector3D(-1.820619869034308, -6.101556783658643,
                -4.503133105845696);
        final PVCoordinates pv = new PVCoordinates(position, velocity, acceleriation);
        CARTESIAN_ORBIT = new CartesianOrbit(pv, GCRF, DATE, Constants.EGM96_EARTH_MU);
        KEPLERIAN_ORBIT = new KeplerianOrbit(CARTESIAN_ORBIT);

        // Default covariance matrix (6x6)
        // This covariance matrix was retrieved from a CDM.
        data = new double[][] {
                { 50823.53083447366, 93983.94583101779, -148928.06893948335, -76.1091060156978,
                        -309.76206045144005, -222.34419118490644 },
                { 93983.94583101779, 173876.66266012395, -275502.8915656621, -140.80716084755107,
                        -573.0494019775061, -411.3280639406579 },
                { -148928.06893948335, -275502.8915656621, 436564.20106596744, 223.11861396813063,
                        908.0269961633179, 651.7725625198417 },
                { -76.1091060156978, -140.80716084755107, 223.11861396813063, 0.11403503131723135,
                        0.4640809062681965, 0.33311263212351044 },
                { -309.76206045144005, -573.0494019775061, 908.0269961633179, 0.4640809062681965,
                        1.8887215030585933, 1.3557001911913937 },
                { -222.34419118490644, -411.3280639406579, 651.7725625198417, 0.33311263212351044,
                        1.3557001911913937, 0.9731047946637145 } };
        COVARIANCE_MATRIX_6X6 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0.,
                0., 0., 0.);

        // Default covariance matrix (9x9)
        // This covariance matrix is random.
        data = new double[][] {
                { 6.05539, -0.59351, -0.4146, 0.98329, -0.7224, -0.30491, 1.51052, -1.52864,
                        -0.34775 },
                { -0.59351, 7.038, -1.35952, -0.46905, 0.19256, -0.70014, -0.06458, 1.25222,
                        -0.82201 },
                { -0.4146, -1.35952, 6.28385, 0.41193, -0.88719, 1.29538, -0.60869, 0.67723,
                        0.24138 },
                { 0.98329, -0.46905, 0.41193, 4.81552, 0.11198, 1.85299, 2.06513, 1.27783, -1.11462 },
                { -0.7224, 0.19256, -0.88719, 0.11198, 2.19887, -0.32198, 0.37129, -0.58917,
                        1.48552 },
                { -0.30491, -0.70014, 1.29538, 1.85299, -0.32198, 4.75056, 0.51745, -0.89962,
                        -0.55256 },
                { 1.51052, -0.06458, -0.60869, 2.06513, 0.37129, 0.51745, 3.75556, 0.00816, 1.08323 },
                { -1.52864, 1.25222, 0.67723, 1.27783, -0.58917, -0.89962, 0.00816, 4.66107,
                        -1.91075 },
                { -0.34775, -0.82201, 0.24138, -1.11462, 1.48552, -0.55256, 1.08323, -1.91075,
                        4.68624 } };
        COVARIANCE_MATRIX_9X9 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0.,
                0., 0., 0.);

        // Default parameter descriptors for a 6x6 covariance matrices (Cartesian coordinates)
        parameterDescriptors = ParameterUtils.buildDefaultParameterDescriptors(6);
        parameterDescriptors.get(0).addField(coordinateDescriptor, CartesianCoordinate.X);
        parameterDescriptors.get(1).addField(coordinateDescriptor, CartesianCoordinate.Y);
        parameterDescriptors.get(2).addField(coordinateDescriptor, CartesianCoordinate.Z);
        parameterDescriptors.get(3).addField(coordinateDescriptor, CartesianCoordinate.VX);
        parameterDescriptors.get(4).addField(coordinateDescriptor, CartesianCoordinate.VY);
        parameterDescriptors.get(5).addField(coordinateDescriptor, CartesianCoordinate.VZ);
        DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6 = parameterDescriptors;

        // Default parameter descriptors for a 9x9 covariance matrices (Cartesian coordinates)
        parameterDescriptors = ParameterUtils.buildDefaultParameterDescriptors(9);
        parameterDescriptors.get(0).addField(coordinateDescriptor, CartesianCoordinate.X);
        parameterDescriptors.get(1).addField(coordinateDescriptor, CartesianCoordinate.Y);
        parameterDescriptors.get(2).addField(coordinateDescriptor, CartesianCoordinate.Z);
        parameterDescriptors.get(3).addField(coordinateDescriptor, CartesianCoordinate.VX);
        parameterDescriptors.get(4).addField(coordinateDescriptor, CartesianCoordinate.VY);
        parameterDescriptors.get(5).addField(coordinateDescriptor, CartesianCoordinate.VZ);
        DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9 = parameterDescriptors;

        // Default parameter descriptors for a 9x9 covariance matrices (Keplerian parameters, true
        // anomaly)
        parameterDescriptors = ParameterUtils.buildDefaultParameterDescriptors(9);
        parameterDescriptors.get(0).addField(coordinateDescriptor,
                KeplerianCoordinate.SEMI_MAJOR_AXIS);
        parameterDescriptors.get(1)
                .addField(coordinateDescriptor, KeplerianCoordinate.ECCENTRICITY);
        parameterDescriptors.get(2).addField(coordinateDescriptor, KeplerianCoordinate.INCLINATION);
        parameterDescriptors.get(3).addField(coordinateDescriptor,
                KeplerianCoordinate.PERIGEE_ARGUMENT);
        parameterDescriptors.get(4).addField(coordinateDescriptor,
                KeplerianCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE);
        parameterDescriptors.get(5)
                .addField(coordinateDescriptor, KeplerianCoordinate.TRUE_ANOMALY);
        DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY = parameterDescriptors;

        // Default parameter descriptors for a 9x9 covariance matrices (Keplerian parameters, true
        // anomaly)
        parameterDescriptors = ParameterUtils.buildDefaultParameterDescriptors(9);
        parameterDescriptors.get(0).addField(coordinateDescriptor,
                KeplerianCoordinate.SEMI_MAJOR_AXIS);
        parameterDescriptors.get(1)
                .addField(coordinateDescriptor, KeplerianCoordinate.ECCENTRICITY);
        parameterDescriptors.get(2).addField(coordinateDescriptor, KeplerianCoordinate.INCLINATION);
        parameterDescriptors.get(3).addField(coordinateDescriptor,
                KeplerianCoordinate.PERIGEE_ARGUMENT);
        parameterDescriptors.get(4).addField(coordinateDescriptor,
                KeplerianCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE);
        parameterDescriptors.get(5).addField(coordinateDescriptor,
                KeplerianCoordinate.ECCENTRIC_ANOMALY);
        DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY = parameterDescriptors;

        // Custom parameter descriptors for a 9x9 covariance matrices (Cartesian coordinates)
        parameterDescriptors = copy(DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        parameterDescriptors.get(0).removeField(parameterNameDescriptor);
        parameterDescriptors.get(1).removeField(parameterNameDescriptor);
        parameterDescriptors.get(2).removeField(parameterNameDescriptor);
        parameterDescriptors.get(3).removeField(parameterNameDescriptor);
        parameterDescriptors.get(4).removeField(parameterNameDescriptor);
        parameterDescriptors.get(5).removeField(parameterNameDescriptor);
        parameterDescriptors.get(6).removeField(parameterNameDescriptor, "additional_parameter_1");
        parameterDescriptors.get(7).removeField(parameterNameDescriptor, "additional_parameter_2");
        parameterDescriptors.get(8).removeField(parameterNameDescriptor, "additional_parameter_3");
        CUSTOM_CARTESIAN_PARAMETER_DESCRIPTORS_9X9 = parameterDescriptors;

        // Custom parameter descriptors for a 6x6 covariance matrices (Keplerian parameters, true
        // anomaly)
        parameterDescriptors = copy(DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY);
        parameterDescriptors.get(0).removeField(parameterNameDescriptor);
        parameterDescriptors.get(1).removeField(parameterNameDescriptor);
        parameterDescriptors.get(2).removeField(parameterNameDescriptor);
        parameterDescriptors.get(3).removeField(parameterNameDescriptor);
        parameterDescriptors.get(4).removeField(parameterNameDescriptor);
        parameterDescriptors.get(5).removeField(parameterNameDescriptor);
        parameterDescriptors.get(6).removeField(parameterNameDescriptor, "additional_parameter_1");
        parameterDescriptors.get(7).removeField(parameterNameDescriptor, "additional_parameter_2");
        parameterDescriptors.get(8).removeField(parameterNameDescriptor, "additional_parameter_3");
        CUSTOM_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY = parameterDescriptors;
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} and checks the basic getters.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#getCovariance()}<br>
     * {@linkplain OrbitalCovariance#getCovarianceMatrix()}<br>
     * {@linkplain OrbitalCovariance#getOrbit()}<br>
     * {@linkplain OrbitalCovariance#getDate()}<br>
     * {@linkplain OrbitalCovariance#getFrame()}<br>
     * {@linkplain OrbitalCovariance#getOrbitType()}<br>
     * {@linkplain OrbitalCovariance#getPositionAngle()}<br>
     * </p>
     */
    @Test
    public void testConstructorFromCovariance() {
        OrbitalCovariance orbitalCovariance;

        // Frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame eme2000 = FramesFactory.getEME2000();

        // Covariance
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                CUSTOM_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY);

        // Create a new orbital covariance without specifying the frame, orbit type or position
        // angle type
        orbitalCovariance = new OrbitalCovariance(covariance, KEPLERIAN_ORBIT);
        Assert.assertEquals(covariance, orbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_9X9, orbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(CUSTOM_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY,
                orbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(KEPLERIAN_ORBIT, orbitalCovariance.getOrbit());
        Assert.assertEquals(DATE, orbitalCovariance.getDate());
        Assert.assertEquals(gcrf, orbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, orbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.TRUE, orbitalCovariance.getPositionAngle());

        // Create a new orbital covariance, specifying position angle type, but not the frame or
        // orbit type
        orbitalCovariance = new OrbitalCovariance(covariance, KEPLERIAN_ORBIT,
                PositionAngle.ECCENTRIC);
        Assert.assertEquals(covariance, orbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_9X9, orbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(CUSTOM_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY,
                orbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(KEPLERIAN_ORBIT, orbitalCovariance.getOrbit());
        Assert.assertEquals(DATE, orbitalCovariance.getDate());
        Assert.assertEquals(gcrf, orbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, orbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.ECCENTRIC, orbitalCovariance.getPositionAngle());

        // Create a new orbital covariance, specifying the frame, orbit type and position angle type
        orbitalCovariance = new OrbitalCovariance(covariance, CARTESIAN_ORBIT, eme2000,
                OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC);
        Assert.assertEquals(covariance, orbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_9X9, orbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(CUSTOM_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY,
                orbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(CARTESIAN_ORBIT, orbitalCovariance.getOrbit());
        Assert.assertEquals(DATE, orbitalCovariance.getDate());
        Assert.assertEquals(eme2000, orbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, orbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.ECCENTRIC, orbitalCovariance.getPositionAngle());
    }

    /**
     * Tests the constructors based on a {@linkplain SymmetricPositiveMatrix} and checks the basic
     * getters.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#getCovariance()}<br>
     * {@linkplain OrbitalCovariance#getCovarianceMatrix()}<br>
     * {@linkplain OrbitalCovariance#getOrbit()}<br>
     * {@linkplain OrbitalCovariance#getDate()}<br>
     * {@linkplain OrbitalCovariance#getFrame()}<br>
     * {@linkplain OrbitalCovariance#getOrbitType()}<br>
     * {@linkplain OrbitalCovariance#getPositionAngle()}<br>
     * </p>
     */
    @Test
    public void testConstructorFromSymmetricPositiveMatrix() {
        Covariance covariance;
        OrbitalCovariance orbitalCovariance;

        // Frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame eme2000 = FramesFactory.getEME2000();

        // Create a new orbital covariance without specifying the frame, orbit type or position
        // angle type
        orbitalCovariance = new OrbitalCovariance(COVARIANCE_MATRIX_9X9, KEPLERIAN_ORBIT);
        covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY);
        Assert.assertEquals(covariance, orbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_9X9, orbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY,
                orbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(KEPLERIAN_ORBIT, orbitalCovariance.getOrbit());
        Assert.assertEquals(DATE, orbitalCovariance.getDate());
        Assert.assertEquals(gcrf, orbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, orbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.TRUE, orbitalCovariance.getPositionAngle());

        // Create a new orbital covariance, specifying position angle type, but not the frame or
        // orbit type
        orbitalCovariance = new OrbitalCovariance(COVARIANCE_MATRIX_9X9, KEPLERIAN_ORBIT,
                PositionAngle.ECCENTRIC);
        covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY);
        Assert.assertEquals(covariance, orbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_9X9, orbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY,
                orbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(KEPLERIAN_ORBIT, orbitalCovariance.getOrbit());
        Assert.assertEquals(DATE, orbitalCovariance.getDate());
        Assert.assertEquals(gcrf, orbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, orbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.ECCENTRIC, orbitalCovariance.getPositionAngle());

        // Create a new orbital covariance, specifying the frame, orbit type and position angle type
        orbitalCovariance = new OrbitalCovariance(COVARIANCE_MATRIX_9X9, KEPLERIAN_ORBIT, eme2000,
                OrbitType.CARTESIAN, PositionAngle.ECCENTRIC);
        covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        Assert.assertEquals(covariance, orbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_9X9, orbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9,
                orbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(KEPLERIAN_ORBIT, orbitalCovariance.getOrbit());
        Assert.assertEquals(DATE, orbitalCovariance.getDate());
        Assert.assertEquals(eme2000, orbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.CARTESIAN, orbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.ECCENTRIC, orbitalCovariance.getPositionAngle());
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} when the provided covariance is
     * {@code null}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullCovariance() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "covariance");

        // Test data
        final Covariance covariance = null;

        // Constructor based on a covariance
        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                    PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain SymmetricPositiveMatrix} when the provided
     * covariance matrix is {@code null}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullCovarianceMatrix() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "covariance matrix");

        // Test data
        final SymmetricPositiveMatrix covarianceMatrix = null;

        // Constructor based on a covariance matrix
        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                    PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} or a
     * {@linkplain SymmetricPositiveMatrix} when the provided orbit is {@code null}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullOrbit() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "orbit");

        // Test data
        final Orbit orbit = null;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY);
        final SymmetricPositiveMatrix covarianceMatrix = COVARIANCE_MATRIX_9X9;

        // Constructors based on a covariance
        try {
            new OrbitalCovariance(covariance, orbit);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covariance, orbit, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covariance, orbit, GCRF, OrbitType.CARTESIAN, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructors based on a covariance matrix
        try {
            new OrbitalCovariance(covarianceMatrix, orbit);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covarianceMatrix, orbit, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covarianceMatrix, orbit, GCRF, OrbitType.CARTESIAN,
                    PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} or a
     * {@linkplain SymmetricPositiveMatrix} when the provided frame is {@code null}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, Frame, OrbitType, PositionAngle)}
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullFrame() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "frame");

        // Test data
        final Frame frame = null;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY);
        final SymmetricPositiveMatrix covarianceMatrix = COVARIANCE_MATRIX_9X9;

        // Constructor based on a covariance
        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, frame, OrbitType.CARTESIAN,
                    PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructor based on a covariance matrix
        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT, frame, OrbitType.CARTESIAN,
                    PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} or a
     * {@linkplain SymmetricPositiveMatrix} when the provided orbit type is {@code null}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, Frame, OrbitType, PositionAngle)}
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullOrbitType() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "orbit type");

        // Test data
        final OrbitType orbitType = null;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY);
        final SymmetricPositiveMatrix covarianceMatrix = COVARIANCE_MATRIX_9X9;

        // Constructor based on a covariance
        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, GCRF, orbitType, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructor based on a covariance matrix
        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT, GCRF, orbitType,
                    PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} or a
     * {@linkplain SymmetricPositiveMatrix} when the provided position angle type is {@code null}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, Frame, OrbitType, PositionAngle)}
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullPositionAngle() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "position angle type");

        // Test data
        final PositionAngle positionAngle = null;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY);
        final SymmetricPositiveMatrix covarianceMatrix = COVARIANCE_MATRIX_9X9;

        // Constructors based on a covariance
        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                    positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructors based on a covariance matrix
        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT, positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                    positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} or a
     * {@linkplain SymmetricPositiveMatrix} when the covariance matrix is not large enough to store
     * the orbital parameters.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, PositionAngle)}
     * <br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(SymmetricPositiveMatrix, Orbit, Frame, OrbitType, PositionAngle)}
     * </p>
     */
    @Test
    public void testConstructorInvalidCovarianceDimension() {
        // Expected exception message
        final String expectedMessage = "Invalid covariance matrix for 1 orbits: the matrix should be at least 6x6, not 5x5";

        // Test data
        final SymmetricPositiveMatrix covarianceMatrix = new ArrayRowSymmetricPositiveMatrix(5);
        final Covariance covariance = new Covariance(covarianceMatrix);

        // Constructors based on a covariance
        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, PositionAngle.ECCENTRIC);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                    PositionAngle.ECCENTRIC);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructors based on a covariance matrix
        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT, PositionAngle.ECCENTRIC);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        try {
            new OrbitalCovariance(covarianceMatrix, KEPLERIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                    PositionAngle.ECCENTRIC);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} when one of the parameter
     * descriptors related to the orbital parameters does not have an orbital coordinate descriptor.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * </p>
     */
    @Test
    public void testConstructorMissingParameterDescriptors() {
        // Format of the expected exception message
        final String expectedMessage = "No orbital coordinate descriptor on row number ";

        // Orbital coordinate descriptor
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;

        // No orbital coordinate descriptor
        for (int i = 0; i < 6; i++) {
            // Test data
            final List<ParameterDescriptor> parameterDescriptors = ParameterUtils
                    .buildDefaultParameterDescriptors(9);
            parameterDescriptors.get(0).addField(fieldDescriptor, CartesianCoordinate.X);
            parameterDescriptors.get(1).addField(fieldDescriptor, CartesianCoordinate.Y);
            parameterDescriptors.get(2).addField(fieldDescriptor, CartesianCoordinate.Z);
            parameterDescriptors.get(3).addField(fieldDescriptor, CartesianCoordinate.VX);
            parameterDescriptors.get(4).addField(fieldDescriptor, CartesianCoordinate.VY);
            parameterDescriptors.get(5).addField(fieldDescriptor, CartesianCoordinate.VZ);
            parameterDescriptors.get(i).removeField(fieldDescriptor);
            final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                    parameterDescriptors);

            // Constructors based on a covariance
            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage + i, e.getMessage());
            }

            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT, PositionAngle.ECCENTRIC);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage + i, e.getMessage());
            }

            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                        PositionAngle.ECCENTRIC);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage + i, e.getMessage());
            }
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} when one of the parameter
     * descriptors related to the orbital parameters have an invalid orbital coordinate descriptor
     * (wrong state vector index).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * </p>
     */
    @Test
    public void testConstructorWrongStateVectorIndex() {
        // Orbital coordinate descriptor
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;

        // No orbital coordinate descriptor
        for (int i = 0; i < 6; i++) {
            // Test data
            final List<ParameterDescriptor> parameterDescriptors = ParameterUtils
                    .buildDefaultParameterDescriptors(9);
            parameterDescriptors.get(0).addField(fieldDescriptor, CartesianCoordinate.X);
            parameterDescriptors.get(1).addField(fieldDescriptor, CartesianCoordinate.Y);
            parameterDescriptors.get(2).addField(fieldDescriptor, CartesianCoordinate.Z);
            parameterDescriptors.get(3).addField(fieldDescriptor, CartesianCoordinate.VX);
            parameterDescriptors.get(4).addField(fieldDescriptor, CartesianCoordinate.VY);
            parameterDescriptors.get(5).addField(fieldDescriptor, CartesianCoordinate.VZ);

            final int stateVectorIndex = (i + 1) % 6;
            final OrbitalCoordinate wrongCoordinate = CartesianCoordinate.valueOf(stateVectorIndex);
            parameterDescriptors.get(i).replaceField(fieldDescriptor, wrongCoordinate);
            final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                    parameterDescriptors);

            final String expectedMessage = String.format(INVALID_COVARIANCE_SIZE_FORMAT, i,
                    wrongCoordinate.toString(), "state vector index", stateVectorIndex, i);

            // Constructors based on a covariance
            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT, PositionAngle.ECCENTRIC);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                        PositionAngle.ECCENTRIC);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} when one of the parameter
     * descriptors related to the orbital parameters have an invalid orbital coordinate descriptor
     * (wrong orbit type).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#OrbitalCovariance(Covariance, Orbit, Frame, OrbitType, PositionAngle)}
     * </p>
     */
    @Test
    public void testConstructorWrongOrbitType() {
        // Orbital coordinate descriptor
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;

        // No orbital coordinate descriptor
        for (int i = 0; i < 6; i++) {
            // Test data
            final List<ParameterDescriptor> parameterDescriptors = ParameterUtils
                    .buildDefaultParameterDescriptors(9);
            parameterDescriptors.get(0).addField(fieldDescriptor, CartesianCoordinate.X);
            parameterDescriptors.get(1).addField(fieldDescriptor, CartesianCoordinate.Y);
            parameterDescriptors.get(2).addField(fieldDescriptor, CartesianCoordinate.Z);
            parameterDescriptors.get(3).addField(fieldDescriptor, CartesianCoordinate.VX);
            parameterDescriptors.get(4).addField(fieldDescriptor, CartesianCoordinate.VY);
            parameterDescriptors.get(5).addField(fieldDescriptor, CartesianCoordinate.VZ);

            final OrbitalCoordinate wrongCoordinate = KeplerianCoordinate.valueOf(i,
                    PositionAngle.TRUE);
            parameterDescriptors.get(i).replaceField(fieldDescriptor, wrongCoordinate);
            final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                    parameterDescriptors);

            final String expectedMessage = String.format(INVALID_COVARIANCE_SIZE_FORMAT, i,
                    wrongCoordinate.toString(), "orbit type", "KEPLERIAN", "CARTESIAN");

            // Constructors based on a covariance
            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT, PositionAngle.TRUE);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage, e.getMessage());
            }

            try {
                new OrbitalCovariance(covariance, CARTESIAN_ORBIT, GCRF, OrbitType.CARTESIAN,
                        PositionAngle.TRUE);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to another frame (GCRF).
     * <p>
     * The reference results were validated against Monte Carlo simulations.
     * </p>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToGCRF() throws PatriusException {
        OrbitalCovariance result;
        OrbitalCovariance expected;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_6X6,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrix
        final Frame destFrame = GCRF;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_6X6,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);

        // Specify the destination frame, orbit type and position angle type
        result = initialOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.TRUE);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.MEAN);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.ECCENTRIC);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        // Specify only the destination frame and orbit type
        result = initialOrbitalCovariance.transformTo(destFrame, initialOrbitType);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        // Specify only the destination frame
        result = initialOrbitalCovariance.transformTo(destFrame);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);
    }

    /**
     * Tests the methods that transform the orbital covariance to another frame (QSW).
     * <p>
     * The reference results were validated against Monte Carlo simulations.
     * </p>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToQSW() throws PatriusException {
        OrbitalCovariance result;
        OrbitalCovariance expected;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrix (non regression data)
        final double[][] data = {
                { +1.092531651223544E+01, -7.144897150701436E+02, +6.874856864378671E-01,
                        +7.517677184890204E-01, -1.279458604932415E-02, -2.043761429511903E-03 },
                { -7.144897150701436E+02, +6.612353482333787E+05, +6.953299068159686E+01,
                        -7.004894481851040E+02, +1.857716344216726E+00, +2.989392050549213E-01 },
                { +6.874856864378671E-01, +6.953299068159686E+01, +1.812101067407275E+01,
                        -8.610339545251833E-02, -6.321347593001292E-04, +4.570463932019209E-03 },
                { +7.517677184890204E-01, -7.004894481851040E+02, -8.610339545251833E-02,
                        +7.421595889005363E-01, -1.962354838798364E-03, -3.109330128845400E-04 },
                { -1.279458604932415E-02, +1.857716344216726E+00, -6.321347593001292E-04,
                        -1.962354838798364E-03, +1.668196595627780E-05, +2.679436564095861E-06 },
                { -2.043761429511903E-03, +2.989392050549213E-01, +4.570463932019209E-03,
                        -3.109330128845400E-04, +2.679436564095861E-06, +4.221982138851582E-06 } };

        final Frame destFrame = new LocalOrbitalFrame(GCRF, LOFType.QSW, initialOrbit, "QSW");
        final SymmetricPositiveMatrix covarianceMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);
        final Covariance covariance = new Covariance(covarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);

        // Specify the destination frame, orbit type and position angle type
        result = initialOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.TRUE);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.MEAN);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.ECCENTRIC);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        // Specify only the destination frame and orbit type
        result = initialOrbitalCovariance.transformTo(destFrame, initialOrbitType);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        // Specify only the destination frame
        result = initialOrbitalCovariance.transformTo(destFrame);
        expected = new OrbitalCovariance(covariance, initialOrbit, destFrame, OrbitType.CARTESIAN,
                initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);
    }

    /**
     * Tests the methods that transform the orbital covariance to a given type of local orbital
     * frame.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToLOF() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        OrbitalCovariance expected;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<LOFType, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // QSW (non regression data)
        data = new double[][] {
                { +1.092531651223544E+01, -7.144897150701436E+02, +6.874856864378671E-01,
                        +7.517677184890204E-01, -1.279458604932415E-02, -2.043761429511903E-03 },
                { -7.144897150701436E+02, +6.612353482333787E+05, +6.953299068159686E+01,
                        -7.004894481851040E+02, +1.857716344216726E+00, +2.989392050549213E-01 },
                { +6.874856864378671E-01, +6.953299068159686E+01, +1.812101067407275E+01,
                        -8.610339545251833E-02, -6.321347593001292E-04, +4.570463932019209E-03 },
                { +7.517677184890204E-01, -7.004894481851040E+02, -8.610339545251833E-02,
                        +7.421595889005363E-01, -1.962354838798364E-03, -3.109330128845400E-04 },
                { -1.279458604932415E-02, +1.857716344216726E+00, -6.321347593001292E-04,
                        -1.962354838798364E-03, +1.668196595627780E-05, +2.679436564095861E-06 },
                { -2.043761429511903E-03, +2.989392050549213E-01, +4.570463932019209E-03,
                        -3.109330128845400E-04, +2.679436564095861E-06, +4.221982138851582E-06 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.QSW, matrix);

        // TNW (non regression data)
        data = new double[][] {
                { +6.612320359045925E+05, +1.643373746823199E+03, +6.953388785073912E+01,
                        +8.331338198029812E-01, +7.167917727187817E+02, +2.989360390072591E-01 },
                { +1.643373746823199E+03, +1.423764529865002E+01, -5.898050944160786E-01,
                        +1.261478835886010E-02, +1.776350242902822E+00, +2.463709072067388E-03 },
                { +6.953388785073912E+01, -5.898050944160786E-01, +1.812101067407821E+01,
                        -7.385509678101698E-04, +8.781672552546382E-02, +4.570463932019209E-03 },
                { +8.331338198029812E-01, +1.261478835886010E-02, -7.385509678101698E-04,
                        +1.200246233090674E-05, +8.975307408100042E-04, +2.181894565092546E-06 },
                { +7.167917727187817E+02, +1.776350242902822E+00, +8.781672552546382E-02,
                        +8.975307408100042E-04, +7.771055904657258E-01, +3.183065019591730E-04 },
                { +2.989360390072591E-01, +2.463709072067388E-03, +4.570463932019209E-03,
                        +2.181894565092546E-06, +3.183065019591730E-04, +4.221982138858521E-06 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.TNW, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<LOFType, SymmetricPositiveMatrix> entry : expectedMatrices.entrySet()) {
            // LOF type
            final LOFType lofType = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            initialOrbitType, initialPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);

            // Expected frame name
            final String expectedFrameName = lofType.name();

            // Default orbit
            result = initialOrbitalCovariance.transformTo(lofType, false);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    initialOrbitType, initialPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());

            // Custom orbit
            result = initialOrbitalCovariance.transformTo(KEPLERIAN_ORBIT, lofType, false);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    initialOrbitType, initialPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to a given type of local orbital
     * frame.
     * <p>
     * This test specifies the initial frame as a non-pseudo inertial frame (ITRF). The orbit is
     * defined in a pseudo inertial frame (GCRF). The LOF should be built in GCRF, but the reference
     * orbit shouldn't be converted.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToLOFInertialFrame1st() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        OrbitalCovariance expected;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getITRF(); // Multi orb cov frame non-inertial
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<LOFType, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // QSW (non regression data)
        data = new double[][] {
                { 146.02738726639654, -8699.3265494894, -3620.9205181690486, 8.234545406134572,
                        -3.9377872130134506, 9.65047217389516 },
                { -8699.3265494894, 563644.7838152818, 234370.5133632676, -533.0695476569884,
                        254.3704417765034, -625.2292907960679 },
                { -3620.9205181690486, 234370.5133632676, 97473.58335801697, -221.68574040795843,
                        105.77839136706291, -259.9909326560004 },
                { 8.234545406134572, -533.0695476569884, -221.68574040795843, 0.5042431981220918,
                        -0.24059862233920465, 0.5913657473566217 },
                { -3.9377872130134506, 254.3704417765034, 105.77839136706291, -0.24059862233920465,
                        0.11481326117425839, -0.28217725236862246 },
                { 9.65047217389516, -625.2292907960679, -259.9909326560004, 0.5913657473566217,
                        -0.28217725236862246, 0.6935775908978105 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.QSW, matrix);

        // TNW (non regression data)
        data = new double[][] {
                { 563619.2301761967, 9490.894229362515, 234365.1954350346, 253.38157862435554,
                        547.3098458733017, -625.2151168837976 },
                { 9490.894229362515, 171.58102635169053, 3950.1605357902445, 4.278268508964686,
                        9.223408558282529, -10.528784474603682 },
                { 234365.1954350346, 3950.1605357902445, 97473.5833580169, 105.3694748785466,
                        227.61220778851867, -259.9909326560002 },
                { 253.38157862435554, 4.278268508964686, 105.3694748785466, 0.11392698574549287,
                        0.2460758978468004, -0.2810866447417963 },
                { 547.3098458733017, 9.223408558282529, 227.61220778851867, 0.2460758978468004,
                        0.5315628133785721, -0.6071757509501945 },
                { -625.2151168837976, -10.528784474603682, -259.9909326560002, -0.2810866447417963,
                        -0.6071757509501945, 0.6935775908978106 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.TNW, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<LOFType, SymmetricPositiveMatrix> entry : expectedMatrices.entrySet()) {
            // LOF type
            final LOFType lofType = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            initialOrbitType, initialPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);

            // Expected frame name
            final String expectedFrameName = lofType.name();

            result = initialOrbitalCovariance.transformTo(lofType, false);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    initialOrbitType, initialPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to a given type of local orbital
     * frame.
     * <p>
     * This test specifies the initial frame as a non-pseudo inertial frame (ITRF). The orbit is
     * also defined in a non-pseudo inertial frame (ITRF). The LOF should be built in GCRF and the
     * reference orbit should be converted in this frame before being stored in the LOF.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToLOFInertialFrame2nd() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        OrbitalCovariance expected;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getITRF(); // Multi orb cov frame non-inertial
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT.getType().convertOrbit(CARTESIAN_ORBIT,
                initialFrame);
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<LOFType, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // QSW (non regression data)
        data = new double[][] {
                { 146.02738726636744, -8699.326549489371, -3620.920518169012, 8.234545406134629,
                        -3.9377872130133937, 9.650472173895096 },
                { -8699.326549489371, 563644.7838152817, 234370.51336326762, -533.0695476569887,
                        254.37044177650353, -625.2292907960674 },
                { -3620.920518169012, 234370.51336326762, 97473.58335801701, -221.68574040795846,
                        105.77839136706298, -259.99093265600015 },
                { 8.234545406134629, -533.0695476569887, -221.68574040795846, 0.5042431981220924,
                        -0.24059862233920487, 0.5913657473566216 },
                { -3.9377872130133937, 254.37044177650353, 105.77839136706298,
                        -0.24059862233920487, 0.11481326117425883, -0.28217725236862234 },
                { 9.650472173895096, -625.2292907960674, -259.99093265600015, 0.5913657473566216,
                        -0.28217725236862234, 0.6935775908978097 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.QSW, matrix);

        // TNW (non regression data)
        data = new double[][] {
                { 563619.2301761966, 9490.894229362399, 234365.19543503472, 253.38157862435546,
                        547.3098458733018, -625.215116883797 },
                { 9490.894229362399, 171.58102635169053, 3950.1605357902154, 4.278268508964629,
                        9.223408558282443, -10.528784474603476 },
                { 234365.19543503472, 3950.1605357902154, 97473.58335801704, 105.36947487854658,
                        227.61220778851904, -259.9909326560003 },
                { 253.38157862435546, 4.278268508964629, 105.36947487854658, 0.11392698574549276,
                        0.24607589784680056, -0.28108664474179573 },
                { 547.3098458733018, 9.223408558282443, 227.61220778851904, 0.24607589784680056,
                        0.5315628133785723, -0.6071757509501942 },
                { -625.215116883797, -10.528784474603476, -259.9909326560003, -0.28108664474179573,
                        -0.6071757509501942, 0.6935775908978096 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.TNW, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<LOFType, SymmetricPositiveMatrix> entry : expectedMatrices.entrySet()) {
            // LOF type
            final LOFType lofType = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            initialOrbitType, initialPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);

            // Expected frame name
            final String expectedFrameName = lofType.name();

            result = initialOrbitalCovariance.transformTo(lofType, false);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    initialOrbitType, initialPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to a given type of local orbital
     * frame.
     * <p>
     * This test specifies the initial frame as a pseudo inertial frame (GCRF). The orbit is defined
     * in a non-pseudo inertial frame (ITRF). The LOF should be built in GCRF and the reference
     * orbit should be converted in this frame before being stored in the LOF.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToLOFInertialFrame3rd() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        OrbitalCovariance expected;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final Frame itrf = FramesFactory.getITRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT.getType().convertOrbit(CARTESIAN_ORBIT, itrf);
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<LOFType, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // QSW (non regression data)
        data = new double[][] {
                { 10.92531651223544, -714.48971507026, 0.687485686452419, 0.7517677184890204,
                        -0.012794586049324153, -0.002043761429540325 },
                { -714.48971507026, 661235.3482333787, 69.53299068169872, -700.4894481851038,
                        1.8577163442168967, 0.2989392050553903 },
                { 0.687485686452419, 69.53299068169872, 18.12101067406911, -0.08610339545266044,
                        -0.0006321347593001292, 0.004570463932017432 },
                { 0.7517677184890204, -700.4894481851038, -0.08610339545266044, 0.7421595889005361,
                        -0.00196235483879853, -0.00031093301288506736 },
                { -0.012794586049324153, 1.8577163442168967, -0.0006321347593001292,
                        -0.00196235483879853, 0.00001668196595649984, 0.00000267943656410974 },
                { -0.002043761429540325, 0.2989392050553903, 0.004570463932017432,
                        -0.00031093301288506736, 0.00000267943656410974, 0.00000422198213889668 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.QSW, matrix);

        // TNW (non regression data)
        data = new double[][] {
                { 661232.0359045924, 1643.3737468233157, 69.53388785084826, 0.8331338198028675,
                        716.7917727187818, 0.29893603900751486 },
                { 1643.3737468233157, 14.237645298679126, -0.5898050944451825,
                        0.012614788358803253, 1.776350242902879, 0.0024637090720887045 },
                { 69.53388785084826, -0.5898050944451825, 18.12101067406911,
                        -0.0007385509678243807, 0.0878167255255704, 0.00457046393201388 },
                { 0.8331338198028675, 0.012614788358803253, -0.0007385509678243807,
                        0.00001200246233057367, 0.0008975307408100042, 0.00000218189456507867 },
                { 716.7917727187818, 1.776350242902879, 0.0878167255255704, 0.0008975307408100042,
                        0.7771055904657252, 0.00031830650195936727 },
                { 0.29893603900751486, 0.0024637090720887045, 0.00457046393201388,
                        0.00000218189456507867, 0.00031830650195936727, 0.00000422198213884464 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.TNW, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<LOFType, SymmetricPositiveMatrix> entry : expectedMatrices.entrySet()) {
            // LOF type
            final LOFType lofType = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            initialOrbitType, initialPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);

            // Expected frame name
            final String expectedFrameName = lofType.name();

            result = initialOrbitalCovariance.transformTo(lofType, false);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    initialOrbitType, initialPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to a given type of local orbital
     * frame (frozen).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Orbit, LOFType, boolean)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToFrozenLOF() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        OrbitalCovariance expected;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<LOFType, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // QSW (non regression data)
        data = new double[][] {
                { +1.092531651223544E+01, -7.144897150701436E+02, +6.874856864378671E-01,
                        +1.509401000979381E+00, -1.266702493637695E-03, -6.143288784392809E-02 },
                { -7.144897150701436E+02, +6.612353482333787E+05, +6.953299068159686E+01,
                        -1.401652665533229E+03, +1.094303407398968E+00, +5.526150739548399E+01 },
                { +6.874856864378671E-01, +6.953299068159686E+01, +1.812101067407275E+01,
                        -1.598350487714200E-01, -1.409373318111307E-03, +1.035011825938526E-02 },
                { +1.509401000979381E+00, -1.401652665533229E+03, -1.598350487714200E-01,
                        +2.971236923794489E+00, -2.318417113112148E-03, -1.171347470903654E-01 },
                { -1.266702493637695E-03, +1.094303407398968E+00, -1.409373318111307E-03,
                        -2.318417113112148E-03, +1.941296368990209E-06, +9.109199803927770E-05 },
                { -6.143288784392809E-02, +5.526150739548399E+01, +1.035011825938526E-02,
                        -1.171347470903654E-01, +9.109199803927770E-05, +4.622463948681450E-03 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.QSW, matrix);

        // TNW (non regression data)
        data = new double[][] {
                { +6.612320359045925E+05, +1.643373746823199E+03, +6.953388785073912E+01,
                        -8.747366365361131E-01, +1.401649316294457E+03, +5.526136656632849E+01 },
                { +1.643373746823199E+03, +1.423764529865002E+01, -5.898050944160786E-01,
                        -2.082536279260694E-03, +3.478441044914462E+00, +1.390641676626174E-01 },
                { +6.953388785073912E+01, -5.898050944160786E-01, +1.812101067407821E+01,
                        -1.633908131182693E-03, +1.598329111700636E-01, +1.035011825937815E-02 },
                { -8.747366365361131E-01, -2.082536279260694E-03, -1.633908131182693E-03,
                        +1.291098807532087E-06, -1.855577469928149E-03, -7.345893124170721E-05 },
                { +1.401649316294457E+03, +3.478441044914462E+00, +1.598329111700636E-01,
                        -1.855577469928149E-03, +2.971237573992051E+00, +1.171347594758338E-01 },
                { +5.526136656632849E+01, +1.390641676626174E-01, +1.035011825937815E-02,
                        -7.345893124170721E-05, +1.171347594758338E-01, +4.622463948681498E-03 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.TNW, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<LOFType, SymmetricPositiveMatrix> entry : expectedMatrices.entrySet()) {
            // LOF type
            final LOFType lofType = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            OrbitType.CARTESIAN, initialPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);

            // Expected frame name
            final String expectedFrameName = "Frozen_" + lofType.name();

            // Default orbit
            result = initialOrbitalCovariance.transformTo(lofType, true);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    OrbitType.CARTESIAN, initialPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());

            // Custom orbit
            result = initialOrbitalCovariance.transformTo(KEPLERIAN_ORBIT, lofType, true);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    OrbitType.CARTESIAN, initialPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to another orbit type (Cartesian
     * coordinates).
     * <p>
     * The reference results were validated against Monte Carlo simulations.
     * </p>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#transformTo(PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToCartesian() throws PatriusException {
        OrbitalCovariance result;
        OrbitalCovariance expected;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrix
        final OrbitType destOrbitType = OrbitType.CARTESIAN;
        final Covariance covariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);

        // Specify the destination frame, orbit type and position angle type
        result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                PositionAngle.TRUE);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                PositionAngle.MEAN);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                PositionAngle.ECCENTRIC);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        // Specify only the destination frame and orbit type
        result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        // Specify only the destination orbit type and position angle type
        result = initialOrbitalCovariance.transformTo(destOrbitType, PositionAngle.TRUE);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(destOrbitType, PositionAngle.MEAN);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(destOrbitType, PositionAngle.ECCENTRIC);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        // Specify only the destination orbit type
        result = initialOrbitalCovariance.transformTo(destOrbitType);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        // Specify only the destination position angle type
        result = initialOrbitalCovariance.transformTo(PositionAngle.TRUE);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(PositionAngle.MEAN);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);

        result = initialOrbitalCovariance.transformTo(PositionAngle.ECCENTRIC);
        expected = new OrbitalCovariance(covariance, initialOrbit, initialFrame, destOrbitType,
                PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialOrbitalCovariance, result);
    }

    /**
     * Tests the methods that transform the orbital covariance to another orbit type (alternate
     * equinoctial parameters).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToAlternateEquinoctial() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<PositionAngle, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // True longitude argument (non regression data)
        data = new double[][] {
                { +2.762084775455959E-18, -6.089915292582324E-14, +7.611652258623638E-14,
                        +7.201706150192445E-15, +5.612030365064747E-15, +1.029453810921762E-13 },
                { -6.089915292582324E-14, +5.064982527144807E-09, -6.416810428089912E-09,
                        -5.905021879439040E-10, -4.652232873703560E-10, -8.601957285292227E-09 },
                { +7.611652258623638E-14, -6.416810428089912E-09, +8.129972152442285E-09,
                        +7.480144233646267E-10, +5.894182851759938E-10, +1.089803452284618E-08 },
                { +7.201706150192445E-15, -5.905021879439040E-10, +7.480144233646267E-10,
                        +6.919111563121408E-11, +5.403890947038201E-11, +1.003205234712622E-09 },
                { +5.612030365064747E-15, -4.652232873703560E-10, +5.894182851759938E-10,
                        +5.403890947038201E-11, +4.299871329641178E-11, +7.902262894929515E-10 },
                { +1.029453810921762E-13, -8.601957285292227E-09, +1.089803452284618E-08,
                        +1.003205234712622E-09, +7.902262894929515E-10, +1.461072756051098E-08 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.TRUE, matrix);

        // Mean longitude argument (non regression data)
        data = new double[][] {
                { +2.762084775455959E-18, -6.089915292582324E-14, +7.611652258623638E-14,
                        +7.201706150192445E-15, +5.612030365064747E-15, +2.977668045377885E-13 },
                { -6.089915292582324E-14, +5.064982527144807E-09, -6.416810428089912E-09,
                        -5.905021879439040E-10, -4.652232873703560E-10, -2.494054610298298E-08 },
                { +7.611652258623638E-14, -6.416810428089912E-09, +8.129972152442285E-09,
                        +7.480144233646267E-10, +5.894182851759938E-10, +3.159818131618400E-08 },
                { +7.201706150192445E-15, -5.905021879439040E-10, +7.480144233646267E-10,
                        +6.919111563121408E-11, +5.403890947038201E-11, +2.907900454414977E-09 },
                { +5.612030365064747E-15, -4.652232873703560E-10, +5.894182851759938E-10,
                        +5.403890947038201E-11, +4.299871329641178E-11, +2.290985079946917E-09 },
                { +2.977668045377885E-13, -2.494054610298298E-08, +3.159818131618400E-08,
                        +2.907900454414977E-09, +2.290985079946917E-09, +1.228139993794695E-07 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.MEAN, matrix);

        // Eccentric longitude argument (non regression data)
        data = new double[][] {
                { +2.762084775455959E-18, -6.089915292582324E-14, +7.611652258623638E-14,
                        +7.201706150192445E-15, +5.612030365064747E-15, +2.003658329203096E-13 },
                { -6.089915292582324E-14, +5.064982527144807E-09, -6.416810428089912E-09,
                        -5.905021879439040E-10, -4.652232873703560E-10, -1.677204960120900E-08 },
                { +7.611652258623638E-14, -6.416810428089912E-09, +8.129972152442285E-09,
                        +7.480144233646267E-10, +5.894182851759938E-10, +2.124911870986074E-08 },
                { +7.201706150192445E-15, -5.905021879439040E-10, +7.480144233646267E-10,
                        +6.919111563121408E-11, +5.403890947038201E-11, +1.955645881772410E-09 },
                { +5.612030365064747E-15, -4.652232873703560E-10, +5.894182851759938E-10,
                        +5.403890947038201E-11, +4.299871329641178E-11, +1.540678969261160E-09 },
                { +2.003658329203096E-13, -1.677204960120900E-08, +2.124911870986074E-08,
                        +1.955645881772410E-09, +1.540678969261160E-09, +5.554108859334365E-08 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.ECCENTRIC, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<PositionAngle, SymmetricPositiveMatrix> entry : expectedMatrices
                .entrySet()) {
            // Destination orbit type and position angle type
            final OrbitType destOrbitType = OrbitType.ALTERNATE_EQUINOCTIAL;
            final PositionAngle destPositionAngle = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);
            final OrbitalCovariance expected = new OrbitalCovariance(covariance, initialOrbit,
                    initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            // Specify only the destination orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);

                // Specify only the destination orbit type
                result = initialOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);
            }
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to another orbit type (equinoctial
     * parameters).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToEquinoctial() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<PositionAngle, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // True longitude argument (non regression data)
        data = new double[][] {
                { +5.482499741483480E+01, +2.713201570519186E-04, -3.391171448230423E-04,
                        -3.208530742771637E-05, -2.500292511266669E-05, -4.586460668790648E-04 },
                { +2.713201570519186E-04, +5.064982527144807E-09, -6.416810428089912E-09,
                        -5.905021879439040E-10, -4.652232873703560E-10, -8.601957285292227E-09 },
                { -3.391171448230423E-04, -6.416810428089912E-09, +8.129972152442285E-09,
                        +7.480144233646267E-10, +5.894182851759938E-10, +1.089803452284618E-08 },
                { -3.208530742771637E-05, -5.905021879439040E-10, +7.480144233646267E-10,
                        +6.919111563121408E-11, +5.403890947038201E-11, +1.003205234712622E-09 },
                { -2.500292511266669E-05, -4.652232873703560E-10, +5.894182851759938E-10,
                        +5.403890947038201E-11, +4.299871329641178E-11, +7.902262894929515E-10 },
                { -4.586460668790648E-04, -8.601957285292227E-09, +1.089803452284618E-08,
                        +1.003205234712622E-09, +7.902262894929515E-10, +1.461072756051098E-08 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.TRUE, matrix);

        // Mean longitude argument (non regression data)
        data = new double[][] {
                { +5.482499741483480E+01, +2.713201570519186E-04, -3.391171448230423E-04,
                        -3.208530742771637E-05, -2.500292511266669E-05, -1.326621673546885E-03 },
                { +2.713201570519186E-04, +5.064982527144807E-09, -6.416810428089912E-09,
                        -5.905021879439040E-10, -4.652232873703560E-10, -2.494054610298298E-08 },
                { -3.391171448230423E-04, -6.416810428089912E-09, +8.129972152442285E-09,
                        +7.480144233646267E-10, +5.894182851759938E-10, +3.159818131618400E-08 },
                { -3.208530742771637E-05, -5.905021879439040E-10, +7.480144233646267E-10,
                        +6.919111563121408E-11, +5.403890947038201E-11, +2.907900454414977E-09 },
                { -2.500292511266669E-05, -4.652232873703560E-10, +5.894182851759938E-10,
                        +5.403890947038201E-11, +4.299871329641178E-11, +2.290985079946917E-09 },
                { -1.326621673546885E-03, -2.494054610298298E-08, +3.159818131618400E-08,
                        +2.907900454414977E-09, +2.290985079946917E-09, +1.228139993794695E-07 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.MEAN, matrix);

        // Eccentric longitude argument (non regression data)
        data = new double[][] {
                { +5.482499741483480E+01, +2.713201570519186E-04, -3.391171448230423E-04,
                        -3.208530742771637E-05, -2.500292511266669E-05, -8.926772646902781E-04 },
                { +2.713201570519186E-04, +5.064982527144807E-09, -6.416810428089912E-09,
                        -5.905021879439040E-10, -4.652232873703560E-10, -1.677204960120900E-08 },
                { -3.391171448230423E-04, -6.416810428089912E-09, +8.129972152442285E-09,
                        +7.480144233646267E-10, +5.894182851759938E-10, +2.124911870986074E-08 },
                { -3.208530742771637E-05, -5.905021879439040E-10, +7.480144233646267E-10,
                        +6.919111563121408E-11, +5.403890947038201E-11, +1.955645881772410E-09 },
                { -2.500292511266669E-05, -4.652232873703560E-10, +5.894182851759938E-10,
                        +5.403890947038201E-11, +4.299871329641178E-11, +1.540678969261160E-09 },
                { -8.926772646902781E-04, -1.677204960120900E-08, +2.124911870986074E-08,
                        +1.955645881772410E-09, +1.540678969261160E-09, +5.554108859334365E-08 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.ECCENTRIC, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<PositionAngle, SymmetricPositiveMatrix> entry : expectedMatrices
                .entrySet()) {
            // Destination orbit type and position angle type
            final OrbitType destOrbitType = OrbitType.EQUINOCTIAL;
            final PositionAngle destPositionAngle = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);
            final OrbitalCovariance expected = new OrbitalCovariance(covariance, initialOrbit,
                    initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            // Specify only the destination orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);

                // Specify only the destination orbit type
                result = initialOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);
            }
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to another orbit type (circular
     * parameters).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToCircular() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<PositionAngle, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // True latitude argument (non regression data)
        data = new double[][] {
                { +5.482499741530046E+01, +2.462373200551316E-04, +3.577368477861997E-04,
                        +2.846103270249742E-05, -2.034449238039540E-05, -4.383015744986989E-04 },
                { +2.462373200551316E-04, +4.323422106372291E-09, +6.192792248060166E-09,
                        +4.877530245680829E-10, -3.446589386676029E-10, -7.602467313535437E-09 },
                { +3.577368477861997E-04, +6.192792248060166E-09, +8.871072026300305E-09,
                        +6.987102101564636E-10, -4.938176477341116E-10, -1.089026228796734E-08 },
                { +2.846103270249742E-05, +4.877530245680829E-10, +6.987102101564636E-10,
                        +5.515304483082634E-11, -3.878966723499599E-11, -8.580415245035822E-10 },
                { -2.034449238039540E-05, -3.446589386676029E-10, -4.938176477341116E-10,
                        -3.878966723499599E-11, +2.782975587174620E-11, +6.061598251727704E-10 },
                { -4.383015744986989E-04, -7.602467313535437E-09, -1.089026228796734E-08,
                        -8.580415245035822E-10, +6.061598251727704E-10, +1.337057815429368E-08 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.TRUE, matrix);

        // Mean latitude argument (non regression data)
        data = new double[][] {
                { +5.482499741530046E+01, +2.462373200551316E-04, +3.577368477861997E-04,
                        +2.846103270249742E-05, -2.034449238039540E-05, -1.306277181166354E-03 },
                { +2.462373200551316E-04, +4.323422106372291E-09, +6.192792248060166E-09,
                        +4.877530245680829E-10, -3.446589386676029E-10, -2.269763285886263E-08 },
                { +3.577368477861997E-04, +6.192792248060166E-09, +8.871072026300305E-09,
                        +6.987102101564636E-10, -4.938176477341116E-10, -3.251333459329668E-08 },
                { +2.846103270249742E-05, +4.877530245680829E-10, +6.987102101564636E-10,
                        +5.515304483082634E-11, -3.878966723499599E-11, -2.561124703966280E-09 },
                { -2.034449238039540E-05, -3.446589386676029E-10, -4.938176477341116E-10,
                        -3.878966723499599E-11, +2.782975587174620E-11, +1.809751660895674E-09 },
                { -1.306277181166354E-03, -2.269763285886263E-08, -3.251333459329668E-08,
                        -2.561124703966280E-09, +1.809751660895674E-09, +1.191666663018064E-07 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.MEAN, matrix);

        // Eccentric latitude argument (non regression data)
        data = new double[][] {
                { +5.482499741530046E+01, +2.462373200551316E-04, +3.577368477861997E-04,
                        +2.846103270249742E-05, -2.034449238039540E-05, -8.723327723098862E-04 },
                { +2.462373200551316E-04, +4.323422106372291E-09, +6.192792248060166E-09,
                        +4.877530245680829E-10, -3.446589386676029E-10, -1.515078715808843E-08 },
                { +3.577368477861997E-04, +6.192792248060166E-09, +8.871072026300305E-09,
                        +6.987102101564636E-10, -4.938176477341116E-10, -2.170285438567419E-08 },
                { +2.846103270249742E-05, +4.877530245680829E-10, +6.987102101564636E-10,
                        +5.515304483082634E-11, -3.878966723499599E-11, -1.709666285491197E-09 },
                { -2.034449238039540E-05, -3.446589386676029E-10, -4.938176477341116E-10,
                        -3.878966723499599E-11, +2.782975587174620E-11, +1.208014539452200E-09 },
                { -8.723327723098862E-04, -1.515078715808843E-08, -2.170285438567419E-08,
                        -1.709666285491197E-09, +1.208014539452200E-09, +5.309722975856750E-08 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.ECCENTRIC, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<PositionAngle, SymmetricPositiveMatrix> entry : expectedMatrices
                .entrySet()) {
            // Destination orbit type and position angle type
            final OrbitType destOrbitType = OrbitType.CIRCULAR;
            final PositionAngle destPositionAngle = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);
            final OrbitalCovariance expected = new OrbitalCovariance(covariance, initialOrbit,
                    initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            // Specify only the destination orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);

                // Specify only the destination orbit type
                result = initialOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);
            }
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to another orbit type (equatorial
     * parameters).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToEquatorial() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<PositionAngle, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // True anomaly (non regression data)
        data = new double[][] {
                { +5.482499741530046E+01, +4.197872319765747E-04, -7.644955953361432E-02,
                        -3.482998790860573E-05, -8.950136061516845E-06, +7.599091346676800E-02 },
                { +4.197872319765747E-04, +1.228404627090640E-08, -2.296831664065050E-06,
                        -9.965640663635276E-10, -2.614624224300985E-10, +2.283435503453955E-06 },
                { -7.644955953361432E-02, -2.296831664065050E-06, +4.295583007095372E-04,
                        +1.862703890857060E-07, +4.890051824339634E-08, -4.270535758307235E-04 },
                { -3.482998790860573E-05, -9.965640663635276E-10, +1.862703890857060E-07,
                        +8.151483984998667E-11, +2.092408787106948E-11, -1.851831181650256E-07 },
                { -8.950136061516845E-06, -2.614624224300985E-10, +4.890051824339634E-08,
                        +2.092408787106948E-11, +5.734624010636486E-12, -4.861539811341390E-08 },
                { +7.599091346676800E-02, +2.283435503453955E-06, -4.270535758307235E-04,
                        -1.851831181650256E-07, -4.861539811341390E-08, +4.245634616794752E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.TRUE, matrix);

        // Mean anomaly (non regression data)
        data = new double[][] {
                { +5.482499741530046E+01, +4.197872319765747E-04, -7.644955953361432E-02,
                        -3.482998790860573E-05, -8.950136061516845E-06, +7.512293786007262E-02 },
                { +4.197872319765747E-04, +1.228404627090640E-08, -2.296831664065050E-06,
                        -9.965640663635276E-10, -2.614624224300985E-10, +2.257990561197107E-06 },
                { -7.644955953361432E-02, -2.296831664065050E-06, +4.295583007095372E-04,
                        +1.862703890857060E-07, +4.890051824339634E-08, -4.222958822104848E-04 },
                { -3.482998790860573E-05, -9.965640663635276E-10, +1.862703890857060E-07,
                        +8.151483984998667E-11, +2.092408787106948E-11, -1.831189034794724E-07 },
                { -8.950136061516845E-06, -2.614624224300985E-10, +4.890051824339634E-08,
                        +2.092408787106948E-11, +5.734624010636486E-12, -4.807379972050120E-08 },
                { +7.512293786007262E-02, +2.257990561197107E-06, -4.222958822104848E-04,
                        -1.831189034794724E-07, -4.807379972050120E-08, +4.151562777108056E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.MEAN, matrix);

        // Eccentric anomaly (non regression data)
        data = new double[][] {
                { +5.482499741530046E+01, +4.197872319765747E-04, -7.644955953361432E-02,
                        -3.482998790860573E-05, -8.950136061516845E-06, +7.555688226898383E-02 },
                { +4.197872319765747E-04, +1.228404627090640E-08, -2.296831664065050E-06,
                        -9.965640663635276E-10, -2.614624224300985E-10, +2.270711789772300E-06 },
                { -7.644955953361432E-02, -2.296831664065050E-06, +4.295583007095372E-04,
                        +1.862703890857060E-07, +4.890051824339634E-08, -4.246744967395140E-04 },
                { -3.482998790860573E-05, -9.965640663635276E-10, +1.862703890857060E-07,
                        +8.151483984998667E-11, +2.092408787106948E-11, -1.841509099885823E-07 },
                { -8.950136061516845E-06, -2.614624224300985E-10, +4.890051824339634E-08,
                        +2.092408787106948E-11, +5.734624010636486E-12, -4.834457247555917E-08 },
                { +7.555688226898383E-02, +2.270711789772300E-06, -4.246744967395140E-04,
                        -1.841509099885823E-07, -4.834457247555917E-08, +4.198462338580872E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.ECCENTRIC, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<PositionAngle, SymmetricPositiveMatrix> entry : expectedMatrices
                .entrySet()) {
            // Destination orbit type and position angle type
            final OrbitType destOrbitType = OrbitType.EQUATORIAL;
            final PositionAngle destPositionAngle = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);
            final OrbitalCovariance expected = new OrbitalCovariance(covariance, initialOrbit,
                    initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            // Specify only the destination orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);

                // Specify only the destination orbit type
                result = initialOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);
            }
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to another orbit type (Keplerian
     * parameters).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToKeplerian() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<PositionAngle, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // True anomaly (non regression data)
        data = new double[][] {
                { +5.482499741483480E+01, +4.197872319767378E-04, +2.846103270259109E-05,
                        -7.642921504125866E-02, -2.034449238039887E-05, +7.599091346671116E-02 },
                { +4.197872319767378E-04, +1.228404627090639E-08, +8.222003190105723E-10,
                        -2.296250590418261E-06, -5.810736466989849E-10, +2.283435503453870E-06 },
                { +2.846103270259109E-05, +8.222003190105723E-10, +5.515304483118121E-11,
                        -1.536845357171475E-07, -3.878966723512115E-11, +1.528264941926409E-07 },
                { -7.642921504125866E-02, -2.296250590418261E-06, -1.536845357171475E-07,
                        +4.293411199829851E-04, +1.085764483832809E-07, -4.269456055421388E-04 },
                { -2.034449238039887E-05, -5.810736466989849E-10, -3.878966723512115E-11,
                        +1.085764483832809E-07, +2.782975587174612E-11, -1.079702885581099E-07 },
                { +7.599091346671116E-02, +2.283435503453870E-06, +1.528264941926409E-07,
                        -4.269456055421388E-04, -1.079702885581099E-07, +4.245634616794475E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.TRUE, matrix);

        // Mean anomaly (non regression data)
        data = new double[][] {
                { +5.482499741483480E+01, +4.197872319767378E-04, +2.846103270259109E-05,
                        -7.642921504125866E-02, -2.034449238039887E-05, +7.512293786010105E-02 },
                { +4.197872319767378E-04, +1.228404627090639E-08, +8.222003190105723E-10,
                        -2.296250590418261E-06, -5.810736466989849E-10, +2.257990561197031E-06 },
                { +2.846103270259109E-05, +8.222003190105723E-10, +5.515304483118121E-11,
                        -1.536845357171475E-07, -3.878966723512115E-11, +1.511234110131744E-07 },
                { -7.642921504125866E-02, -2.296250590418261E-06, -1.536845357171475E-07,
                        +4.293411199829851E-04, +1.085764483832809E-07, -4.221891155137270E-04 },
                { -2.034449238039887E-05, -5.810736466989849E-10, -3.878966723512115E-11,
                        +1.085764483832809E-07, +2.782975587174612E-11, -1.067666967223875E-07 },
                { +7.512293786010105E-02, +2.257990561197031E-06, +1.511234110131744E-07,
                        -4.221891155137270E-04, -1.067666967223875E-07, +4.151562777107848E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.MEAN, matrix);

        // Eccentric anomaly (non regression data)
        data = new double[][] {
                { +5.482499741483480E+01, +4.197872319767378E-04, +2.846103270259109E-05,
                        -7.642921504125866E-02, -2.034449238039887E-05, +7.555688226889856E-02 },
                { +4.197872319767378E-04, +1.228404627090639E-08, +8.222003190105723E-10,
                        -2.296250590418261E-06, -5.810736466989849E-10, +2.270711789772228E-06 },
                { +2.846103270259109E-05, +8.222003190105723E-10, +5.515304483118121E-11,
                        -1.536845357171475E-07, -3.878966723512115E-11, +1.519748694316495E-07 },
                { -7.642921504125866E-02, -2.296250590418261E-06, -1.536845357171475E-07,
                        +4.293411199829851E-04, +1.085764483832809E-07, -4.245671283056342E-04 },
                { -2.034449238039887E-05, -5.810736466989849E-10, -3.878966723512115E-11,
                        +1.085764483832809E-07, +2.782975587174612E-11, -1.073684338438322E-07 },
                { +7.555688226889856E-02, +2.270711789772228E-06, +1.519748694316495E-07,
                        -4.245671283056342E-04, -1.073684338438322E-07, +4.198462338580594E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.ECCENTRIC, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<PositionAngle, SymmetricPositiveMatrix> entry : expectedMatrices
                .entrySet()) {
            // Destination orbit type and position angle type
            final OrbitType destOrbitType = OrbitType.KEPLERIAN;
            final PositionAngle destPositionAngle = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);
            final OrbitalCovariance expected = new OrbitalCovariance(covariance, initialOrbit,
                    initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            // Specify only the destination orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);

                // Specify only the destination orbit type
                result = initialOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);
            }
        }
    }

    /**
     * Tests the methods that transform the orbital covariance to another orbit type (apsis
     * parameters).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToApsis() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Orbit initialOrbit = CARTESIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_6X6;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<PositionAngle, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // True anomaly (non regression data)
        data = new double[][] {
                { +6.100410218377672E+05, -6.158765280094757E+05, -5.793551211242479E-03,
                        +1.618334967789676E+01, +4.094246664139822E-03, -1.609304417270747E+01 },
                { -6.158765280094757E+05, +6.219313341708488E+05, +5.850473276645684E-03,
                        -1.633620810797927E+01, -4.134935648900610E-03, +1.624502599964092E+01 },
                { -5.793551211242479E-03, +5.850473276645684E-03, +5.515304482715315E-11,
                        -1.536845357115825E-07, -3.878966723369902E-11, +1.528264941871056E-07 },
                { +1.618334967789676E+01, -1.633620810797927E+01, -1.536845357115825E-07,
                        +4.293411199833321E-04, +1.085764483833269E-07, -4.269456055424753E-04 },
                { +4.094246664139822E-03, -4.134935648900610E-03, -3.878966723369902E-11,
                        +1.085764483833269E-07, +2.782975587174594E-11, -1.079702885581526E-07 },
                { -1.609304417270747E+01, +1.624502599964092E+01, +1.528264941871056E-07,
                        -4.269456055424753E-04, -1.079702885581526E-07, +4.245634616797736E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.TRUE, matrix);

        // Mean anomaly (non regression data)
        data = new double[][] {
                { +6.100410218377672E+05, -6.158765280094757E+05, -5.793551211242479E-03,
                        +1.618334967789676E+01, +4.094246664139822E-03, -1.591373616613788E+01 },
                { -6.158765280094757E+05, +6.219313341708488E+05, +5.850473276645684E-03,
                        -1.633620810797927E+01, -4.134935648900610E-03, +1.606398204185811E+01 },
                { -5.793551211242479E-03, +5.850473276645684E-03, +5.515304482715315E-11,
                        -1.536845357115825E-07, -3.878966723369902E-11, +1.511234110077004E-07 },
                { +1.618334967789676E+01, -1.633620810797927E+01, -1.536845357115825E-07,
                        +4.293411199833321E-04, +1.085764483833269E-07, -4.221891155140670E-04 },
                { +4.094246664139822E-03, -4.134935648900610E-03, -3.878966723369902E-11,
                        +1.085764483833269E-07, +2.782975587174594E-11, -1.067666967224290E-07 },
                { -1.591373616613788E+01, +1.606398204185811E+01, +1.511234110077004E-07,
                        -4.221891155140670E-04, -1.067666967224290E-07, +4.151562777111040E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.MEAN, matrix);

        // Eccentric anomaly (non regression data)
        data = new double[][] {
                { +6.100410218377672E+05, -6.158765280094757E+05, -5.793551211242479E-03,
                        +1.618334967789676E+01, +4.094246664139822E-03, -1.600338141427940E+01 },
                { -6.158765280094757E+05, +6.219313341708488E+05, +5.850473276645684E-03,
                        -1.633620810797927E+01, -4.134935648900610E-03, +1.615449517881729E+01 },
                { -5.793551211242479E-03, +5.850473276645684E-03, +5.515304482715315E-11,
                        -1.536845357115825E-07, -3.878966723369902E-11, +1.519748694261467E-07 },
                { +1.618334967789676E+01, -1.633620810797927E+01, -1.536845357115825E-07,
                        +4.293411199833321E-04, +1.085764483833269E-07, -4.245671283059742E-04 },
                { +4.094246664139822E-03, -4.134935648900610E-03, -3.878966723369902E-11,
                        +1.085764483833269E-07, +2.782975587174594E-11, -1.073684338438724E-07 },
                { -1.600338141427940E+01, +1.615449517881729E+01, +1.519748694261467E-07,
                        -4.245671283059742E-04, -1.073684338438724E-07, +4.198462338583821E-04 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(PositionAngle.ECCENTRIC, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<PositionAngle, SymmetricPositiveMatrix> entry : expectedMatrices
                .entrySet()) {
            // Destination orbit type and position angle type
            final OrbitType destOrbitType = OrbitType.APSIS;
            final PositionAngle destPositionAngle = entry.getKey();

            // Expected orbital covariance
            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix, parameterDescriptors);
            final OrbitalCovariance expected = new OrbitalCovariance(covariance, initialOrbit,
                    initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            // Specify only the destination orbit type and position angle type
            result = initialOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);

                // Specify only the destination orbit type
                result = initialOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialOrbitalCovariance, result);
            }
        }
    }

    /**
     * Tests the method that shifts the orbital covariance by a given duration.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain OrbitalCovariance#shiftedBy(double)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testShiftedBy6x6() throws PatriusException {
        // Initial covariance
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_6X6,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                CARTESIAN_ORBIT, GCRF, OrbitType.CARTESIAN, PositionAngle.TRUE);

        // Shifted orbit and covariance
        final double dt = 10000;
        final Orbit shiftedOrbit = initialOrbitalCovariance.getOrbit().shiftedBy(dt);
        final OrbitalCovariance result = initialOrbitalCovariance.shiftedBy(dt);

        // Expected covariance matrix (non regression data)
        final double[][] data = {
                { +4.198514093973674E+03, -7.013756985581294E+04, -1.945936405666918E+05,
                        -6.063914784371991E+01, -1.234754080394778E+02, +1.429606743371296E+02 },
                { -7.013756985581294E+04, +1.208386767865151E+06, +3.335492149636865E+06,
                        +1.040914105893418E+03, +2.123176442022494E+03, -2.448698159025487E+03 },
                { -1.945936405666918E+05, +3.335492149636865E+06, +9.215275287698925E+06,
                        +2.875026510875585E+03, +5.862640678334516E+03, -6.766103121678811E+03 },
                { -6.063914784371991E+01, +1.040914105893418E+03, +2.875026510875585E+03,
                        +8.970563471400084E-01, +1.829363553514043E+00, -2.110839752914856E+00 },
                { -1.234754080394778E+02, +2.123176442022494E+03, +5.862640678334516E+03,
                        +1.829363553514043E+00, +3.731006353314626E+00, -4.304175057491733E+00 },
                { +1.429606743371296E+02, -2.448698159025487E+03, -6.766103121678811E+03,
                        -2.110839752914856E+00, -4.304175057491733E+00, +4.967942779374653E+00 } };

        final SymmetricPositiveMatrix covarianceMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);
        final Covariance covariance = new Covariance(covarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_6X6);
        final OrbitalCovariance expected = new OrbitalCovariance(covariance, shiftedOrbit, GCRF,
                OrbitType.CARTESIAN, PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that transform the orbital covariance to a given type of local orbital
     * frame.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Orbit, LOFType, boolean)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform9x9ToLOF() throws PatriusException {
        double[][] data;
        OrbitalCovariance result;
        OrbitalCovariance expected;
        SymmetricPositiveMatrix matrix;

        // Initial covariance
        final Frame initialFrame = GCRF;
        final OrbitType initialOrbitType = OrbitType.KEPLERIAN;
        final PositionAngle initialPositionAngle = PositionAngle.ECCENTRIC;
        final Orbit initialOrbit = KEPLERIAN_ORBIT;
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_9X9;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                initialOrbit, initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrices
        final Map<LOFType, SymmetricPositiveMatrix> expectedMatrices = new LinkedHashMap<>();

        // QSW (non regression data)
        data = new double[][] {
                { +2.450687960631049E+13, -7.344117624702034E+13, +8.233604314027274E+12,
                        -9.512414763301047E+10, -5.125486492337776E+10, -1.111354698067017E+10,
                        +1.255921350887783E+05, -2.344373167821638E+06, +1.527573150260409E+06 },
                { -7.344117624702034E+13, +8.831192380949924E+14, +1.201957250473989E+12,
                        +2.869875322904252E+11, +1.567342231582066E+11, -1.063588654439234E+11,
                        +1.746481307121270E+07, +1.182446490849391E+07, -1.892003121672885E+07 },
                { +8.233604314027274E+12, +1.201957250473989E+12, +1.343110044227600E+14,
                        -3.183704505731112E+10, -6.247648126564954E+09, -8.669657477090283E+10,
                        -3.301744452086057E+05, -6.445174694364149E+05, +9.514256359014650E+06 },
                { -9.512414763301047E+10, +2.869875322904252E+11, -3.183704505731112E+10,
                        +3.692351264827745E+08, +1.989601990054245E+08, +4.262957939700904E+07,
                        -4.662983950858048E+02, +9.067784469514790E+03, -5.955749827661649E+03 },
                { -5.125486492337776E+10, +1.567342231582066E+11, -6.247648126564954E+09,
                        +1.989601990054245E+08, +1.081135848420428E+08, +1.614012342641424E+07,
                        -2.670734312715398E+02, +4.922224064902458E+03, -2.467992136849926E+03 },
                { -1.111354698067017E+10, -1.063588654439234E+11, -8.669657477090283E+10,
                        +4.262957939700904E+07, +1.614012342641424E+07, +3.356436728862142E+08,
                        +3.872963857400216E+03, -7.656605700979982E+03, +6.411581798691715E+03 },
                { +1.255921350887783E+05, +1.746481307121270E+07, -3.301744452086057E+05,
                        -4.662983950858048E+02, -2.670734312715398E+02, +3.872963857400216E+03,
                        +3.755560000000000E+00, +8.160000000000000E-03, +1.083230000000000E+00 },
                { -2.344373167821638E+06, +1.182446490849391E+07, -6.445174694364149E+05,
                        +9.067784469514790E+03, +4.922224064902458E+03, -7.656605700979982E+03,
                        +8.160000000000000E-03, +4.661070000000000E+00, -1.910750000000000E+00 },
                { +1.527573150260409E+06, -1.892003121672885E+07, +9.514256359014650E+06,
                        -5.955749827661649E+03, -2.467992136849926E+03, +6.411581798691715E+03,
                        +1.083230000000000E+00, -1.910750000000000E+00, +4.686240000000000E+00 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.QSW, matrix);

        // TNW (non regression data)
        data = new double[][] {
                { +8.829112035980091E+14, +7.464706344054100E+13, +1.213522628057713E+12,
                        +1.552245185823418E+11, -2.648657788921902E+11, -1.063743728020347E+11,
                        +1.746497226990803E+07, +1.182115986656439E+07, -1.891786661367438E+07 },
                { +7.464706344054100E+13, +2.471491410329420E+13, -8.231907680673282E+12,
                        +5.099981363180656E+10, -9.361444305714548E+10, +1.096412312881926E+10,
                        -1.010574497098898E+05, +2.360981857476548E+06, -1.554150494197723E+06 },
                { +1.213522628057713E+12, -8.231907680673282E+12, +1.343110044227600E+14,
                        -6.089415449532828E+09, +3.185815539087674E+10, -8.669657477090282E+10,
                        -3.301744452086075E+05, -6.445174694364162E+05, +9.514256359014655E+06 },
                { +1.552245185823418E+11, +5.099981363180656E+10, -6.089415449532828E+09,
                        +1.061433553241150E+08, -1.931459360998409E+08, +1.592968172272537E+07,
                        -2.652367319519863E+02, +4.876749485530401E+03, -2.438039983968617E+03 },
                { -2.648657788921902E+11, -9.361444305714548E+10, +3.185815539087674E+10,
                        -1.931459360998409E+08, +3.550788404157668E+08, -4.522943979521292E+07,
                        +8.965078401413738E+02, -8.769419428960182E+03, +5.485871864993559E+03 },
                { -1.063743728020347E+11, +1.096412312881926E+10, -8.669657477090282E+10,
                        +1.592968172272537E+07, -4.522943979521292E+07, +3.356436728862144E+08,
                        +3.872963857400218E+03, -7.656605700979983E+03, +6.411581798691715E+03 },
                { +1.746497226990803E+07, -1.010574497098898E+05, -3.301744452086075E+05,
                        -2.652367319519863E+02, +8.965078401413738E+02, +3.872963857400218E+03,
                        +3.755560000000000E+00, +8.160000000000000E-03, +1.083230000000000E+00 },
                { +1.182115986656439E+07, +2.360981857476548E+06, -6.445174694364162E+05,
                        +4.876749485530401E+03, -8.769419428960182E+03, -7.656605700979983E+03,
                        +8.160000000000000E-03, +4.661070000000000E+00, -1.910750000000000E+00 },
                { -1.891786661367438E+07, -1.554150494197723E+06, +9.514256359014655E+06,
                        -2.438039983968617E+03, +5.485871864993559E+03, +6.411581798691715E+03,
                        +1.083230000000000E+00, -1.910750000000000E+00, +4.686240000000000E+00 } };
        matrix = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0., 0., 0., 0.);
        expectedMatrices.put(LOFType.TNW, matrix);

        // Test the methods for different position angles
        for (final Map.Entry<LOFType, SymmetricPositiveMatrix> entry : expectedMatrices.entrySet()) {
            // LOF type
            final LOFType lofType = entry.getKey();

            // Expected orbital covariance
            final SymmetricPositiveMatrix covarianceMatrix = entry.getValue();
            final Covariance covariance = new Covariance(covarianceMatrix,
                    DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);

            // Expected frame name
            final String expectedFrameName = lofType.name();

            // Default orbit
            result = initialOrbitalCovariance.transformTo(lofType, false);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    OrbitType.CARTESIAN, initialPositionAngle);
            System.out.println(lofType);
            System.out.println(CheckUtils.printArray(result.getCovarianceMatrix().getData(),
                    CheckUtils.SCIENTIFIC_NUMBER_FORMAT));
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());

            // Custom orbit
            result = initialOrbitalCovariance.transformTo(KEPLERIAN_ORBIT, lofType, false);
            expected = new OrbitalCovariance(covariance, initialOrbit, result.getFrame(),
                    OrbitType.CARTESIAN, initialPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialOrbitalCovariance, result);
            Assert.assertEquals(expectedFrameName, result.getFrame().getName());
        }
    }

    /**
     * Tests to transform a covariance with an orbit defined at a different date (should fail).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#checkOrbit(Orbit)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     * @throws PatriusException if an unexpected error occurs during the test
     */
    @Test
    public void testTransformWrongOrbitDate() throws PatriusException {

        // Test data
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                KEPLERIAN_ORBIT, GCRF, OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC);
        final Orbit shiftedOrbit = KEPLERIAN_ORBIT.shiftedBy(1.);

        // Expected exception message
        final String expectedMessage = "The provided orbit is not defined at the same date as the covariance ("
                + shiftedOrbit.getDate() + " != " + KEPLERIAN_ORBIT.getDate() + ")";

        try {
            initialOrbitalCovariance.transformTo(shiftedOrbit, LOFType.TNW, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that shifts the orbital covariance by a given duration.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain OrbitalCovariance#shiftedBy(double)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testShiftedBy9x9() throws PatriusException {
        // Initial covariance
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY);
        final OrbitalCovariance initialOrbitalCovariance = new OrbitalCovariance(initialCovariance,
                KEPLERIAN_ORBIT, GCRF, OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC);

        // Shifted orbit and covariance
        final double dt = 10000;
        final Orbit shiftedOrbit = initialOrbitalCovariance.getOrbit().shiftedBy(dt);
        final OrbitalCovariance result = initialOrbitalCovariance.shiftedBy(dt);

        // Expected covariance matrix (non regression data)
        final double[][] data = {
                { +6.055389988831878E+00, -5.935099484835015E-01, -4.146000024499436E-01,
                        +9.832900049520112E-01, -7.224000019959324E-01, +6.364284608327401E-01,
                        +1.510520001457045E+00, -1.528639993718937E+00, -3.477500101874756E-01 },
                { -5.935099484835015E-01, +7.038000000001779E+00, -1.359520000000311E+00,
                        -4.690499999997085E-01, +1.925600000001337E-01, -1.186622319121578E+01,
                        -6.457999999978817E-02, +1.252220000000133E+00, -8.220100000001596E-01 },
                { -4.146000024499436E-01, -1.359520000000311E+00, +6.283849999999981E+00,
                        +4.119300000002387E-01, -8.871899999999969E-01, +3.453198487140023E+00,
                        -6.086900000000057E-01, +6.772300000000040E-01, +2.413799999999990E-01 },
                { +9.832900049520112E-01, -4.690499999997085E-01, +4.119300000002387E-01,
                        +4.815520000001111E+00, +1.119799999999083E-01, +2.598528099055713E+00,
                        +2.065130000000206E+00, +1.277830000000184E+00, -1.114620000000169E+00 },
                { -7.224000019959324E-01, +1.925600000001337E-01, -8.871899999999969E-01,
                        +1.119799999999083E-01, +2.198869999999996E+00, -6.277130773358998E-01,
                        +3.712899999999967E-01, -5.891700000000011E-01, +1.485520000000003E+00 },
                { +6.364284608327401E-01, -1.186622319121578E+01, +3.453198487140023E+00,
                        +2.598528099055713E+00, -6.277130773358998E-01, +2.469474257288859E+01,
                        +6.202938413338285E-01, -2.886905502282564E+00, +7.511123570974815E-01 },
                { +1.510520001457045E+00, -6.457999999978817E-02, -6.086900000000057E-01,
                        +2.065130000000206E+00, +3.712899999999967E-01, +6.202938413338285E-01,
                        +3.755560000000000E+00, +8.160000000000000E-03, +1.083230000000000E+00 },
                { -1.528639993718937E+00, +1.252220000000133E+00, +6.772300000000040E-01,
                        +1.277830000000184E+00, -5.891700000000011E-01, -2.886905502282564E+00,
                        +8.160000000000000E-03, +4.661070000000000E+00, -1.910750000000000E+00 },
                { -3.477500101874756E-01, -8.220100000001596E-01, +2.413799999999990E-01,
                        -1.114620000000169E+00, +1.485520000000003E+00, +7.511123570974815E-01,
                        +1.083230000000000E+00, -1.910750000000000E+00, +4.686240000000000E+00 } };
        final SymmetricPositiveMatrix covarianceMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, data, 0., 0., 0., 0.);
        final Covariance covariance = new Covariance(covarianceMatrix,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY);
        final OrbitalCovariance expected = new OrbitalCovariance(covariance, shiftedOrbit, GCRF,
                OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
    }

    /**
     * Tests the method that copies an orbital covariance.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain OrbitalCovariance#copy()}
     * </p>
     */
    @Test
    public void testCopy() {
        // Test data
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                CUSTOM_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        final OrbitalCovariance orbitalCovariance = new OrbitalCovariance(covariance,
                CARTESIAN_ORBIT);

        // Check the method
        final OrbitalCovariance copy = orbitalCovariance.copy();
        checkEquality(orbitalCovariance, copy, 0., 0.);
        Assert.assertFalse(copy == orbitalCovariance);
    }

    /**
     * @description Tests the equals and hashCode methods.
     *
     * @testedMethod {@link OrbitalCovariance#equals(Object)}
     * @testedMethod {@link OrbitalCovariance#hashCode()}
     *
     * @testPassCriteria The methods behaves as expected.
     */
    @Test
    public void testEqualsAndHashCode() {
        Covariance covariance;
        OrbitalCovariance other;

        // Test data
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final Frame frame = GCRF;
        final AbsoluteDate date = DATE;
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        final OrbitalCovariance instance = new OrbitalCovariance(initialCovariance,
                KEPLERIAN_ORBIT, frame, orbitType, positionAngle);

        // Check the hashCode consistency between calls
        final int hashCode = instance.hashCode();
        Assert.assertEquals(hashCode, instance.hashCode());

        // The compared object is null
        Assert.assertFalse(instance.equals(null));

        // Different class
        Assert.assertFalse(instance.equals(new Object()));

        // Same instance
        Assert.assertTrue(instance.equals(instance));

        // Same data, but different instances
        other = new OrbitalCovariance(initialCovariance, KEPLERIAN_ORBIT, frame, orbitType,
                positionAngle);
        Assert.assertTrue(instance.equals(other));
        Assert.assertTrue(other.equals(instance));
        Assert.assertTrue(instance.hashCode() == other.hashCode());

        // Different covariance matrix
        final ArrayRowSymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(9);
        covariance = new Covariance(matrix, DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        other = new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, frame, orbitType, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different parameter descriptors
        covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                CUSTOM_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        other = new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, frame, orbitType, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different orbit
        final Orbit orbit = new KeplerianOrbit(1E7, 0., 0., 0., 0., 0., PositionAngle.MEAN, frame,
                date, mu);
        covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        other = new OrbitalCovariance(covariance, orbit, frame, orbitType, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different frame
        covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        other = new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, FramesFactory.getEME2000(),
                orbitType, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different orbit type
        covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_TRUE_ANOMALY);
        other = new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, frame, OrbitType.KEPLERIAN,
                positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different position angle type
        covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_9X9);
        other = new OrbitalCovariance(covariance, KEPLERIAN_ORBIT, frame, orbitType,
                PositionAngle.ECCENTRIC);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }

    /**
     * Tests the method that returns a string representation of the orbital covariance.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain OrbitalCovariance#toString()}<br>
     * {@linkplain OrbitalCovariance#toString(RealMatrixFormat)}<br>
     * {@linkplain OrbitalCovariance#toString(RealMatrixFormat, TimeScale)}<br>
     * {@linkplain OrbitalCovariance#toString(RealMatrixFormat, TimeScale, String, String, boolean, boolean)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testToString() throws PatriusException {
        String result;
        StringBuilder builder;
        RealMatrixFormat format;

        // Orbital covariance
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_9X9,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_9X9_ECCENTRIC_ANOMALY);
        final OrbitalCovariance orbitalCovariance = new OrbitalCovariance(covariance,
                KEPLERIAN_ORBIT, GCRF, OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC);

        // Time scales
        final TimeScale utc = TimeScalesFactory.getUTC();

        // Default format
        result = orbitalCovariance.toString();
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0_SEMI_MAJOR_AXIS, p1_ECCENTRICITY, p2_INCLINATION, p3_PERIGEE_ARGUMENT, "
                + "p4_RIGHT_ASCENSION_OF_ASCENDING_NODE, p5_ECCENTRIC_ANOMALY, p6, p7, p8]");
        Assert.assertEquals(builder.toString(), result);

        // Null format
        format = null;
        result = orbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0_SEMI_MAJOR_AXIS, p1_ECCENTRICITY, p2_INCLINATION, p3_PERIGEE_ARGUMENT, "
                + "p4_RIGHT_ASCENSION_OF_ASCENDING_NODE, p5_ECCENTRIC_ANOMALY, p6, p7, p8]");
        Assert.assertEquals(builder.toString(), result);

        // Null format (UTC time scale)
        format = null;
        result = orbitalCovariance.toString(format, utc);
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:28:32.080 UTC; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0_SEMI_MAJOR_AXIS, p1_ECCENTRICITY, p2_INCLINATION, p3_PERIGEE_ARGUMENT, "
                + "p4_RIGHT_ASCENSION_OF_ASCENDING_NODE, p5_ECCENTRIC_ANOMALY, p6, p7, p8]");
        Assert.assertEquals(builder.toString(), result);

        // Java format
        format = MatrixUtils.JAVA_FORMAT;
        result = orbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0_SEMI_MAJOR_AXIS, p1_ECCENTRICITY, p2_INCLINATION, p3_PERIGEE_ARGUMENT, "
                + "p4_RIGHT_ASCENSION_OF_ASCENDING_NODE, p5_ECCENTRIC_ANOMALY, p6, p7, p8;");
        builder.append(System.lineSeparator());
        builder.append("                 ");
        builder.append("{{6.05539, -0.59351, -0.4146, 0.98329, -0.7224, -0.30491, 1.51052, -1.52864, -0.34775}, ");
        builder.append("{-0.59351, 7.038, -1.35952, -0.46905, 0.19256, -0.70014, -0.06458, 1.25222, -0.82201}, ");
        builder.append("{-0.4146, -1.35952, 6.28385, 0.41193, -0.88719, 1.29538, -0.60869, 0.67723, 0.24138}, ");
        builder.append("{0.98329, -0.46905, 0.41193, 4.81552, 0.11198, 1.85299, 2.06513, 1.27783, -1.11462}, ");
        builder.append("{-0.7224, 0.19256, -0.88719, 0.11198, 2.19887, -0.32198, 0.37129, -0.58917, 1.48552}, ");
        builder.append("{-0.30491, -0.70014, 1.29538, 1.85299, -0.32198, 4.75056, 0.51745, -0.89962, -0.55256}, ");
        builder.append("{1.51052, -0.06458, -0.60869, 2.06513, 0.37129, 0.51745, 3.75556, 0.00816, 1.08323}, ");
        builder.append("{-1.52864, 1.25222, 0.67723, 1.27783, -0.58917, -0.89962, 0.00816, 4.66107, -1.91075}, ");
        builder.append("{-0.34775, -0.82201, 0.24138, -1.11462, 1.48552, -0.55256, 1.08323, -1.91075, 4.68624}}]");
        Assert.assertEquals(builder.toString(), result);

        // Octave format
        format = MatrixUtils.OCTAVE_FORMAT;
        result = orbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0_SEMI_MAJOR_AXIS, p1_ECCENTRICITY, p2_INCLINATION, p3_PERIGEE_ARGUMENT, "
                + "p4_RIGHT_ASCENSION_OF_ASCENDING_NODE, p5_ECCENTRIC_ANOMALY, p6, p7, p8;");
        builder.append(System.lineSeparator());
        builder.append("                 ");
        builder.append("[6.05539, -0.59351, -0.4146, 0.98329, -0.7224, -0.30491, 1.51052, -1.52864, -0.34775; ");
        builder.append("-0.59351, 7.038, -1.35952, -0.46905, 0.19256, -0.70014, -0.06458, 1.25222, -0.82201; ");
        builder.append("-0.4146, -1.35952, 6.28385, 0.41193, -0.88719, 1.29538, -0.60869, 0.67723, 0.24138; ");
        builder.append("0.98329, -0.46905, 0.41193, 4.81552, 0.11198, 1.85299, 2.06513, 1.27783, -1.11462; ");
        builder.append("-0.7224, 0.19256, -0.88719, 0.11198, 2.19887, -0.32198, 0.37129, -0.58917, 1.48552; ");
        builder.append("-0.30491, -0.70014, 1.29538, 1.85299, -0.32198, 4.75056, 0.51745, -0.89962, -0.55256; ");
        builder.append("1.51052, -0.06458, -0.60869, 2.06513, 0.37129, 0.51745, 3.75556, 0.00816, 1.08323; ");
        builder.append("-1.52864, 1.25222, 0.67723, 1.27783, -0.58917, -0.89962, 0.00816, 4.66107, -1.91075; ");
        builder.append("-0.34775, -0.82201, 0.24138, -1.11462, 1.48552, -0.55256, 1.08323, -1.91075, 4.68624]]");
        Assert.assertEquals(builder.toString(), result);

        // Scilab format
        format = MatrixUtils.SCILAB_FORMAT;
        result = orbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0_SEMI_MAJOR_AXIS, p1_ECCENTRICITY, p2_INCLINATION, p3_PERIGEE_ARGUMENT, "
                + "p4_RIGHT_ASCENSION_OF_ASCENDING_NODE, p5_ECCENTRIC_ANOMALY, p6, p7, p8;");
        builder.append(System.lineSeparator());
        builder.append("                 ");
        builder.append(" [6.05539, -0.59351, -0.4146, 0.98329, -0.7224, -0.30491, 1.51052, -1.52864, -0.34775; ");
        builder.append("-0.59351, 7.038, -1.35952, -0.46905, 0.19256, -0.70014, -0.06458, 1.25222, -0.82201; ");
        builder.append("-0.4146, -1.35952, 6.28385, 0.41193, -0.88719, 1.29538, -0.60869, 0.67723, 0.24138; ");
        builder.append("0.98329, -0.46905, 0.41193, 4.81552, 0.11198, 1.85299, 2.06513, 1.27783, -1.11462; ");
        builder.append("-0.7224, 0.19256, -0.88719, 0.11198, 2.19887, -0.32198, 0.37129, -0.58917, 1.48552; ");
        builder.append("-0.30491, -0.70014, 1.29538, 1.85299, -0.32198, 4.75056, 0.51745, -0.89962, -0.55256; ");
        builder.append("1.51052, -0.06458, -0.60869, 2.06513, 0.37129, 0.51745, 3.75556, 0.00816, 1.08323; ");
        builder.append("-1.52864, 1.25222, 0.67723, 1.27783, -0.58917, -0.89962, 0.00816, 4.66107, -1.91075; ");
        builder.append("-0.34775, -0.82201, 0.24138, -1.11462, 1.48552, -0.55256, 1.08323, -1.91075, 4.68624]]");
        Assert.assertEquals(builder.toString(), result);

        // Visual format
        format = MatrixUtils.VISUAL_FORMAT;
        result = orbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0_SEMI_MAJOR_AXIS, p1_ECCENTRICITY, p2_INCLINATION, p3_PERIGEE_ARGUMENT, "
                + "p4_RIGHT_ASCENSION_OF_ASCENDING_NODE, p5_ECCENTRIC_ANOMALY, p6, p7, p8;");
        builder.append(System.lineSeparator());
        builder.append("                 [[      6.0554,    -0.59351,    -0.41460,     0.98329,"
                + "    -0.72240,    -0.30491,      1.5105,     -1.5286,    -0.34775]");
        builder.append(System.lineSeparator());
        builder.append("                  [    -0.59351,      7.0380,     -1.3595,    -0.46905,"
                + "     0.19256,    -0.70014,   -0.064580,      1.2522,    -0.82201]");
        builder.append(System.lineSeparator());
        builder.append("                  [    -0.41460,     -1.3595,      6.2839,     0.41193,"
                + "    -0.88719,      1.2954,    -0.60869,     0.67723,     0.24138]");
        builder.append(System.lineSeparator());
        builder.append("                  [     0.98329,    -0.46905,     0.41193,      4.8155,"
                + "     0.11198,      1.8530,      2.0651,      1.2778,     -1.1146]");
        builder.append(System.lineSeparator());
        builder.append("                  [    -0.72240,     0.19256,    -0.88719,     0.11198,"
                + "      2.1989,    -0.32198,     0.37129,    -0.58917,      1.4855]");
        builder.append(System.lineSeparator());
        builder.append("                  [    -0.30491,    -0.70014,      1.2954,      1.8530,"
                + "    -0.32198,      4.7506,     0.51745,    -0.89962,    -0.55256]");
        builder.append(System.lineSeparator());
        builder.append("                  [      1.5105,   -0.064580,    -0.60869,      2.0651,"
                + "     0.37129,     0.51745,      3.7556,   0.0081600,      1.0832]");
        builder.append(System.lineSeparator());
        builder.append("                  [     -1.5286,      1.2522,     0.67723,      1.2778,"
                + "    -0.58917,    -0.89962,   0.0081600,      4.6611,     -1.9108]");
        builder.append(System.lineSeparator());
        builder.append("                  [    -0.34775,    -0.82201,     0.24138,     -1.1146,"
                + "      1.4855,    -0.55256,      1.0832,     -1.9108,      4.6862]]]");
        Assert.assertEquals(builder.toString(), result);

        // Visual format (no class name)
        format = MatrixUtils.VISUAL_FORMAT;
        result = orbitalCovariance.toString(format, utc, Covariance.DEFAULT_NAME_SEPARATOR,
                Covariance.DEFAULT_FIELD_SEPARATOR, false, false);
        builder = new StringBuilder();
        builder.append("2017-11-07T23:28:32.080 UTC; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0_SEMI_MAJOR_AXIS, p1_ECCENTRICITY, p2_INCLINATION, p3_PERIGEE_ARGUMENT, "
                + "p4_RIGHT_ASCENSION_OF_ASCENDING_NODE, p5_ECCENTRIC_ANOMALY, p6, p7, p8;");
        builder.append(System.lineSeparator());
        builder.append("[[      6.0554,    -0.59351,    -0.41460,     0.98329,"
                + "    -0.72240,    -0.30491,      1.5105,     -1.5286,    -0.34775]");
        builder.append(System.lineSeparator());
        builder.append(" [    -0.59351,      7.0380,     -1.3595,    -0.46905,"
                + "     0.19256,    -0.70014,   -0.064580,      1.2522,    -0.82201]");
        builder.append(System.lineSeparator());
        builder.append(" [    -0.41460,     -1.3595,      6.2839,     0.41193,"
                + "    -0.88719,      1.2954,    -0.60869,     0.67723,     0.24138]");
        builder.append(System.lineSeparator());
        builder.append(" [     0.98329,    -0.46905,     0.41193,      4.8155,"
                + "     0.11198,      1.8530,      2.0651,      1.2778,     -1.1146]");
        builder.append(System.lineSeparator());
        builder.append(" [    -0.72240,     0.19256,    -0.88719,     0.11198,"
                + "      2.1989,    -0.32198,     0.37129,    -0.58917,      1.4855]");
        builder.append(System.lineSeparator());
        builder.append(" [    -0.30491,    -0.70014,      1.2954,      1.8530,"
                + "    -0.32198,      4.7506,     0.51745,    -0.89962,    -0.55256]");
        builder.append(System.lineSeparator());
        builder.append(" [      1.5105,   -0.064580,    -0.60869,      2.0651,"
                + "     0.37129,     0.51745,      3.7556,   0.0081600,      1.0832]");
        builder.append(System.lineSeparator());
        builder.append(" [     -1.5286,      1.2522,     0.67723,      1.2778,"
                + "    -0.58917,    -0.89962,   0.0081600,      4.6611,     -1.9108]");
        builder.append(System.lineSeparator());
        builder.append(" [    -0.34775,    -0.82201,     0.24138,     -1.1146,"
                + "      1.4855,    -0.55256,      1.0832,     -1.9108,      4.6862]]");
        Assert.assertEquals(builder.toString(), result);

        // Java format (reverse field descriptors order)
        format = MatrixUtils.JAVA_FORMAT;
        result = orbitalCovariance.toString(format, utc, Covariance.DEFAULT_NAME_SEPARATOR,
                Covariance.DEFAULT_FIELD_SEPARATOR, true, true);
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:28:32.080 UTC; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: SEMI_MAJOR_AXIS_p0, ECCENTRICITY_p1, INCLINATION_p2, PERIGEE_ARGUMENT_p3, "
                + "RIGHT_ASCENSION_OF_ASCENDING_NODE_p4, ECCENTRIC_ANOMALY_p5, p6, p7, p8;");
        builder.append(System.lineSeparator());
        builder.append("                 ");
        builder.append("{{6.05539, -0.59351, -0.4146, 0.98329, -0.7224, -0.30491, 1.51052, -1.52864, -0.34775}, ");
        builder.append("{-0.59351, 7.038, -1.35952, -0.46905, 0.19256, -0.70014, -0.06458, 1.25222, -0.82201}, ");
        builder.append("{-0.4146, -1.35952, 6.28385, 0.41193, -0.88719, 1.29538, -0.60869, 0.67723, 0.24138}, ");
        builder.append("{0.98329, -0.46905, 0.41193, 4.81552, 0.11198, 1.85299, 2.06513, 1.27783, -1.11462}, ");
        builder.append("{-0.7224, 0.19256, -0.88719, 0.11198, 2.19887, -0.32198, 0.37129, -0.58917, 1.48552}, ");
        builder.append("{-0.30491, -0.70014, 1.29538, 1.85299, -0.32198, 4.75056, 0.51745, -0.89962, -0.55256}, ");
        builder.append("{1.51052, -0.06458, -0.60869, 2.06513, 0.37129, 0.51745, 3.75556, 0.00816, 1.08323}, ");
        builder.append("{-1.52864, 1.25222, 0.67723, 1.27783, -0.58917, -0.89962, 0.00816, 4.66107, -1.91075}, ");
        builder.append("{-0.34775, -0.82201, 0.24138, -1.11462, 1.48552, -0.55256, 1.08323, -1.91075, 4.68624}}]");
        Assert.assertEquals(builder.toString(), result);

        // Java format (custom name and field separator)
        format = MatrixUtils.JAVA_FORMAT;
        result = orbitalCovariance.toString(format, utc, " | ", ".", true, false);
        builder = new StringBuilder();
        builder.append("OrbitalCovariance[2017-11-07T23:28:32.080 UTC; GCRF; KEPLERIAN; ECCENTRIC; "
                + "Parameters: p0.SEMI_MAJOR_AXIS | p1.ECCENTRICITY | p2.INCLINATION | p3.PERIGEE_ARGUMENT | "
                + "p4.RIGHT_ASCENSION_OF_ASCENDING_NODE | p5.ECCENTRIC_ANOMALY | p6 | p7 | p8;");
        builder.append(System.lineSeparator());
        builder.append("                 ");
        builder.append("{{6.05539, -0.59351, -0.4146, 0.98329, -0.7224, -0.30491, 1.51052, -1.52864, -0.34775}, ");
        builder.append("{-0.59351, 7.038, -1.35952, -0.46905, 0.19256, -0.70014, -0.06458, 1.25222, -0.82201}, ");
        builder.append("{-0.4146, -1.35952, 6.28385, 0.41193, -0.88719, 1.29538, -0.60869, 0.67723, 0.24138}, ");
        builder.append("{0.98329, -0.46905, 0.41193, 4.81552, 0.11198, 1.85299, 2.06513, 1.27783, -1.11462}, ");
        builder.append("{-0.7224, 0.19256, -0.88719, 0.11198, 2.19887, -0.32198, 0.37129, -0.58917, 1.48552}, ");
        builder.append("{-0.30491, -0.70014, 1.29538, 1.85299, -0.32198, 4.75056, 0.51745, -0.89962, -0.55256}, ");
        builder.append("{1.51052, -0.06458, -0.60869, 2.06513, 0.37129, 0.51745, 3.75556, 0.00816, 1.08323}, ");
        builder.append("{-1.52864, 1.25222, 0.67723, 1.27783, -0.58917, -0.89962, 0.00816, 4.66107, -1.91075}, ");
        builder.append("{-0.34775, -0.82201, 0.24138, -1.11462, 1.48552, -0.55256, 1.08323, -1.91075, 4.68624}}]");
        Assert.assertEquals(builder.toString(), result);
    }

    /**
     * Asserts the equality between two orbital covariances within the specified tolerance.
     *
     * @param expected
     *        the expected orbital covariance
     * @param actual
     *        the orbital covariance to be checked
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    public static void checkEquality(final OrbitalCovariance expected,
            final OrbitalCovariance actual, final double absTol, final double relTol) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            CheckUtils.checkEquality(expected.getCovarianceMatrix(), actual.getCovarianceMatrix(),
                    absTol, relTol);
            CheckUtils.checkEquality(expected.getParameterDescriptors(),
                    actual.getParameterDescriptors());
            Assert.assertEquals(expected.getOrbit(), actual.getOrbit());
            Assert.assertEquals(expected.getDate(), actual.getDate());
            Assert.assertEquals(expected.getFrame(), actual.getFrame());
            Assert.assertEquals(expected.getOrbitType(), actual.getOrbitType());
            Assert.assertEquals(expected.getPositionAngle(), actual.getPositionAngle());
        }
    }

    /**
     * Performs a copy of the provided parameter descriptors and returns them into a new list.
     * <p>
     * Note that the copy of the parameter descriptors is a shallow copy: the associated field
     * descriptors and the values mapped to them are copied by reference.
     * </p>
     *
     * @param parameterDescriptors
     *        the parameter descriptors to be copied
     * @return a new list containing a copy of the provided parameter descriptors, or {@code null}
     *         if the provided collection is {@code null}
     * @see ParameterDescriptor#copy()
     */
    private static List<ParameterDescriptor> copy(
            final Collection<ParameterDescriptor> parameterDescriptors) {
        List<ParameterDescriptor> copy = null;

        if (parameterDescriptors != null) {
            copy = new ArrayList<>(parameterDescriptors.size());

            for (final ParameterDescriptor parameterDescriptor : parameterDescriptors) {
                copy.add(parameterDescriptor.copy());
            }
        }

        return copy;
    }
}
