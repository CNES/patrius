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
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.covariance;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
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
 * Unit tests for {@linkplain MultiOrbitalCovariance}.
 *
 * @author bonitt
 */
public class MultiOrbitalCovarianceTest {

    /** Default absolute tolerance used for the tests. */
    private static final double ABSTOL = 0.;

    /** Default relative tolerance used for the tests. */
    private static final double RELTOL = 1E-14;

    /** Date of definition of the default orbits. */
    private static AbsoluteDate DATE;

    /** Default first Cartesian orbit used for the tests. */
    private static Orbit CARTESIAN_ORBIT_1;

    /** Default second Cartesian orbit used for the tests. */
    private static Orbit CARTESIAN_ORBIT_2;

    /** Default orbits collection used for the tests. */
    private static List<Orbit> ORBITS;

    /** Default 12X12 covariance matrix used for the tests. */
    private static SymmetricPositiveMatrix COVARIANCE_MATRIX_12X12;

    /** Default 18X18 covariance matrix used for the tests. */
    private static SymmetricPositiveMatrix COVARIANCE_MATRIX_18X18;

    /**
     * Parameter descriptors associated with the default 12x12 covariance matrix (Cartesian
     * coordinates).
     */
    private static List<ParameterDescriptor> DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12;

    /**
     * Parameter descriptors associated with the default 18x18 covariance matrix (Cartesian
     * coordinates).
     */
    private static List<ParameterDescriptor> DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18;

    /**
     * Parameter descriptors associated with the default 12X12 covariance matrix (Keplerian
     * parameters, true anomaly).
     */
    private static List<ParameterDescriptor> DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_12X12_TRUE_ANOMALY;

    /** Expected message format for exceptions thrown when a null argument is detected. */
    private static final String NULL_ARGUMENT_FORMAT = "A non-null value is expected (%s)";

    /** Expected message format for exceptions thrown when a null collection is detected. */
    private static final String NULL_COLLECTION_FORMAT = "A non-null collection is expected (%s)";

    /** Expected message format for exceptions thrown when an empty collection is detected. */
    private static final String EMPTY_COLLECTION_FORMAT = "A non-empty collection is expected (%s)";

    /** Expected message format for exceptions thrown when a a covariance size is invalid. */
    private static final String INVALID_COVARIANCE_SIZE_FORMAT = "Invalid orbital coordinate descriptor: row number %d is mapped to %s (wrong %s: %s != %s)";

    @BeforeClass
    public static void setUpBeforeClass() {
        // Locale
        Locale.setDefault(Locale.ENGLISH);

        // Patrius dataset
        PatriusUtils.clearDataProviders();
        final String foldername = "patriusdataset140/";
        final File folder = PatriusUtils.getSystemResource(foldername);
        try {
            PatriusUtils.addDatasetFolder(folder);
        } catch (final PatriusException e) {
            e.printStackTrace();
        }

        // Frames configuration
        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        FramesFactory.setConfiguration(builder.getConfiguration());

        double[][] data;
        List<ParameterDescriptor> parameterDescriptors;

        // Field descriptors
        final FieldDescriptor<OrbitalCoordinate> coordinateDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;

        // Default date
        DATE = new AbsoluteDate("2017-11-07T23:29:09.080", TimeScalesFactory.getTAI());

        // Default orbits
        final Vector3D position1 = new Vector3D(1123783.5101019042, 5726033.748189187,
                4006400.314678921);
        final Vector3D velocity1 = new Vector3D(2084.2377426112616, 3863.6251103548725,
                -6087.970527060892);
        final Vector3D acceleriation1 = new Vector3D(-1.820619869034308, -6.101556783658643,
                -4.503133105845696);
        final PVCoordinates pv1 = new PVCoordinates(position1, velocity1, acceleriation1);
        CARTESIAN_ORBIT_1 = new CartesianOrbit(pv1, FramesFactory.getGCRF(), DATE, Constants.EGM96_EARTH_MU);

        final Vector3D position2 = position1.add(Vector3D.PLUS_I);
        final Vector3D velocity2 = velocity1.add(Vector3D.MINUS_J);
        final Vector3D acceleriation2 = acceleriation1;
        final PVCoordinates pv2 = new PVCoordinates(position2, velocity2, acceleriation2);
        CARTESIAN_ORBIT_2 = new CartesianOrbit(pv2, FramesFactory.getGCRF(), DATE, Constants.EGM96_EARTH_MU);

        ORBITS = new ArrayList<>();
        ORBITS.add(CARTESIAN_ORBIT_1);
        ORBITS.add(CARTESIAN_ORBIT_2);

        // Default covariance matrix (12x12)
        // This covariance matrix was built with a random generator.
        data = new double[][] {
                { 259.86, 136.45, 110.17, 113.80, 168.13, 146.05, 117.44, 177.67, 122.62, 85.204,
                        38.256, 97.704 },
                { 136.45, 245.65, 162.38, 168.30, 213.88, 142.47, 151.86, 144.55, 120.50, 156.87,
                        54.902, 99.309 },
                { 110.17, 162.38, 201.05, 157.60, 175.45, 151.72, 92.051, 72.361, 168.31, 127.19,
                        20.749, 146.50 },
                { 113.80, 168.30, 157.60, 244.84, 206.50, 138.34, 162.25, 126.17, 228.24, 115.13,
                        91.438, 156.41 },
                { 168.13, 213.88, 175.45, 206.50, 352.42, 163.31, 171.79, 154.16, 223.26, 117.69,
                        73.916, 182.56 },
                { 146.05, 142.47, 151.72, 138.34, 163.31, 280.27, 67.276, 195.39, 186.87, 86.425,
                        -32.051, 153.29 },
                { 117.44, 151.86, 92.051, 162.25, 171.79, 67.276, 208.05, 93.774, 123.41, 107.10,
                        101.11, 74.419 },
                { 177.67, 144.55, 72.361, 126.17, 154.16, 195.39, 93.774, 250.08, 129.29, 79.810,
                        24.577, 93.665 },
                { 122.62, 120.50, 168.31, 228.24, 223.26, 186.87, 123.41, 129.29, 292.39, 89.817,
                        70.426, 230.67 },
                { 85.204, 156.87, 127.19, 115.13, 117.69, 86.425, 107.10, 79.810, 89.817, 190.27,
                        43.504, 76.690 },
                { 38.256, 54.902, 20.749, 91.438, 73.916, -32.051, 101.11, 24.577, 70.426, 43.504,
                        99.617, 70.746 },
                { 97.704, 99.309, 146.50, 156.41, 182.56, 153.29, 74.419, 93.665, 230.67, 76.690,
                        70.746, 311.96 } };
        COVARIANCE_MATRIX_12X12 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0.,
                0., 0., 0.);

        // Default covariance matrix (12x12)
        // This covariance matrix was built with a random generator.
        data = new double[][] {
                { 538.70, 421.37, 397.12, 501.35, 470.82, 312.01, 291.04, 252.73, 444.07, 500.17,
                        309.81, 340.10, 404.92, 345.64, 307.42, 391.49, 357.33, 429.22 },
                { 421.37, 409.47, 345.13, 466.07, 426.12, 291.33, 287.86, 256.32, 398.81, 403.73,
                        298.03, 305.89, 359.73, 302.08, 310.10, 348.07, 322.83, 396.66 },
                { 397.12, 345.13, 506.64, 485.97, 408.77, 359.67, 316.30, 251.10, 400.80, 447.25,
                        351.48, 346.75, 415.77, 391.68, 304.60, 324.54, 354.41, 415.52 },
                { 501.35, 466.07, 485.97, 696.62, 539.48, 377.23, 402.81, 347.15, 505.25, 542.08,
                        375.03, 414.74, 501.40, 408.48, 343.41, 444.61, 397.92, 511.72 },
                { 470.82, 426.12, 408.77, 539.48, 609.16, 347.75, 350.04, 340.67, 469.49, 474.80,
                        328.36, 344.80, 471.78, 366.22, 353.02, 403.89, 364.11, 429.75 },
                { 312.01, 291.33, 359.67, 377.23, 347.75, 481.43, 296.15, 260.27, 385.46, 407.52,
                        321.20, 305.46, 326.22, 358.38, 251.47, 316.21, 308.26, 334.75 },
                { 291.04, 287.86, 316.30, 402.81, 350.04, 296.15, 381.76, 219.26, 358.60, 304.46,
                        246.25, 261.03, 332.40, 301.32, 271.56, 316.19, 211.45, 284.44 },
                { 252.73, 256.32, 251.10, 347.15, 340.67, 260.27, 219.26, 347.15, 329.39, 319.25,
                        194.42, 203.04, 290.17, 238.00, 248.15, 297.12, 199.52, 306.54 },
                { 444.07, 398.81, 400.80, 505.25, 469.49, 385.46, 358.60, 329.39, 567.02, 468.99,
                        320.94, 348.85, 389.27, 415.39, 335.25, 428.68, 303.80, 452.92 },
                { 500.17, 403.73, 447.25, 542.08, 474.80, 407.52, 304.46, 319.25, 468.99, 685.85,
                        374.79, 435.74, 496.25, 403.63, 323.12, 420.91, 385.98, 451.19 },
                { 309.81, 298.03, 351.48, 375.03, 328.36, 321.20, 246.25, 194.42, 320.94, 374.79,
                        377.48, 289.69, 313.66, 258.61, 254.05, 258.53, 259.99, 346.30 },
                { 340.10, 305.89, 346.75, 414.74, 344.80, 305.46, 261.03, 203.04, 348.85, 435.74,
                        289.69, 363.01, 383.97, 293.93, 251.93, 279.10, 252.16, 332.23 },
                { 404.92, 359.73, 415.77, 501.40, 471.78, 326.22, 332.40, 290.17, 389.27, 496.25,
                        313.66, 383.97, 496.92, 340.39, 312.96, 354.22, 299.28, 378.14 },
                { 345.64, 302.08, 391.68, 408.48, 366.22, 358.38, 301.32, 238.00, 415.39, 403.63,
                        258.61, 293.93, 340.39, 437.48, 274.94, 321.96, 293.98, 359.95 },
                { 307.42, 310.10, 304.60, 343.41, 353.02, 251.47, 271.56, 248.15, 335.25, 323.12,
                        254.05, 251.93, 312.96, 274.94, 323.48, 251.27, 240.39, 300.46 },
                { 391.49, 348.07, 324.54, 444.61, 403.89, 316.21, 316.19, 297.12, 428.68, 420.91,
                        258.53, 279.10, 354.22, 321.96, 251.27, 463.72, 268.97, 358.80 },
                { 357.33, 322.83, 354.41, 397.92, 364.11, 308.26, 211.45, 199.52, 303.80, 385.98,
                        259.99, 252.16, 299.28, 293.98, 240.39, 268.97, 395.78, 314.03 },
                { 429.22, 396.66, 415.52, 511.72, 429.75, 334.75, 284.44, 306.54, 452.92, 451.19,
                        346.30, 332.23, 378.14, 359.95, 300.46, 358.80, 314.03, 515.75 } };
        COVARIANCE_MATRIX_18X18 = new ArrayRowSymmetricPositiveMatrix(SymmetryType.LOWER, data, 0.,
                0., 0., 0.);

        // Default parameter descriptors for a 12x12 covariance matrices (Cartesian coordinates)
        parameterDescriptors = ParameterUtils.buildDefaultParameterDescriptors(12);
        parameterDescriptors.get(0).addField(coordinateDescriptor, CartesianCoordinate.X);
        parameterDescriptors.get(1).addField(coordinateDescriptor, CartesianCoordinate.Y);
        parameterDescriptors.get(2).addField(coordinateDescriptor, CartesianCoordinate.Z);
        parameterDescriptors.get(3).addField(coordinateDescriptor, CartesianCoordinate.VX);
        parameterDescriptors.get(4).addField(coordinateDescriptor, CartesianCoordinate.VY);
        parameterDescriptors.get(5).addField(coordinateDescriptor, CartesianCoordinate.VZ);
        parameterDescriptors.get(6).addField(coordinateDescriptor, CartesianCoordinate.X);
        parameterDescriptors.get(7).addField(coordinateDescriptor, CartesianCoordinate.Y);
        parameterDescriptors.get(8).addField(coordinateDescriptor, CartesianCoordinate.Z);
        parameterDescriptors.get(9).addField(coordinateDescriptor, CartesianCoordinate.VX);
        parameterDescriptors.get(10).addField(coordinateDescriptor, CartesianCoordinate.VY);
        parameterDescriptors.get(11).addField(coordinateDescriptor, CartesianCoordinate.VZ);
        DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12 = parameterDescriptors;

        // Default parameter descriptors for a 18X18 covariance matrices (Cartesian coordinates)
        parameterDescriptors = ParameterUtils.buildDefaultParameterDescriptors(18);
        parameterDescriptors.get(0).addField(coordinateDescriptor, CartesianCoordinate.X);
        parameterDescriptors.get(1).addField(coordinateDescriptor, CartesianCoordinate.Y);
        parameterDescriptors.get(2).addField(coordinateDescriptor, CartesianCoordinate.Z);
        parameterDescriptors.get(3).addField(coordinateDescriptor, CartesianCoordinate.VX);
        parameterDescriptors.get(4).addField(coordinateDescriptor, CartesianCoordinate.VY);
        parameterDescriptors.get(5).addField(coordinateDescriptor, CartesianCoordinate.VZ);
        parameterDescriptors.get(9).addField(coordinateDescriptor, CartesianCoordinate.X);
        parameterDescriptors.get(10).addField(coordinateDescriptor, CartesianCoordinate.Y);
        parameterDescriptors.get(11).addField(coordinateDescriptor, CartesianCoordinate.Z);
        parameterDescriptors.get(12).addField(coordinateDescriptor, CartesianCoordinate.VX);
        parameterDescriptors.get(13).addField(coordinateDescriptor, CartesianCoordinate.VY);
        parameterDescriptors.get(14).addField(coordinateDescriptor, CartesianCoordinate.VZ);
        DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18 = parameterDescriptors;

        // Default parameter descriptors for a 12X12 covariance matrices (Keplerian parameters, true
        // anomaly)
        parameterDescriptors = ParameterUtils.buildDefaultParameterDescriptors(12);
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
        parameterDescriptors.get(6).addField(coordinateDescriptor,
                KeplerianCoordinate.SEMI_MAJOR_AXIS);
        parameterDescriptors.get(7)
                .addField(coordinateDescriptor, KeplerianCoordinate.ECCENTRICITY);
        parameterDescriptors.get(8).addField(coordinateDescriptor, KeplerianCoordinate.INCLINATION);
        parameterDescriptors.get(9).addField(coordinateDescriptor,
                KeplerianCoordinate.PERIGEE_ARGUMENT);
        parameterDescriptors.get(10).addField(coordinateDescriptor,
                KeplerianCoordinate.RIGHT_ASCENSION_OF_ASCENDING_NODE);
        parameterDescriptors.get(11).addField(coordinateDescriptor,
                KeplerianCoordinate.TRUE_ANOMALY);
        DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_12X12_TRUE_ANOMALY = parameterDescriptors;
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} and checks the basic getters.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain MultiOrbitalCovariance#getCovariance()}<br>
     * {@linkplain MultiOrbitalCovariance#getCovarianceMatrix()}<br>
     * {@linkplain MultiOrbitalCovariance#getOrbits()}<br>
     * {@linkplain MultiOrbitalCovariance#getDate()}<br>
     * {@linkplain MultiOrbitalCovariance#getFrame()}<br>
     * {@linkplain MultiOrbitalCovariance#getOrbitType()}<br>
     * {@linkplain MultiOrbitalCovariance#getPositionAngle()}<br>
     * </p>
     */
    @Test
    public void testConstructorFromCovariance() {
        MultiOrbitalCovariance multiOrbitalCovariance;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Covariance
        Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);

        // Number of additional parameters for each orbit
        int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Create a new multi orbital covariance, specifying the frame, orbit type and position
        // angle type
        multiOrbitalCovariance = new MultiOrbitalCovariance(covariance, ORBITS,
                nbAdditionalParameters, eme2000, OrbitType.CARTESIAN, PositionAngle.TRUE);

        Assert.assertEquals(covariance, multiOrbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_12X12, multiOrbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12,
                multiOrbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(ORBITS, multiOrbitalCovariance.getOrbits());
        Assert.assertEquals(DATE, multiOrbitalCovariance.getDate());
        Assert.assertEquals(eme2000, multiOrbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.CARTESIAN, multiOrbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.TRUE, multiOrbitalCovariance.getPositionAngle());

        // Evaluation with the 18x18 covariance (2 orbits and additional parameters, checked for
        // compatibility)
        // Covariance
        covariance = new Covariance(COVARIANCE_MATRIX_18X18,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);

        // Number of additional parameters for each orbit
        nbAdditionalParameters = new int[] { 3, 3 };

        // Create a new multi orbital covariance, specifying the frame, orbit type and position
        // angle type
        multiOrbitalCovariance = new MultiOrbitalCovariance(covariance, ORBITS,
                nbAdditionalParameters, eme2000, OrbitType.CARTESIAN, PositionAngle.TRUE);

        Assert.assertEquals(covariance, multiOrbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_18X18, multiOrbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18,
                multiOrbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(ORBITS, multiOrbitalCovariance.getOrbits());
        Assert.assertEquals(DATE, multiOrbitalCovariance.getDate());
        Assert.assertEquals(eme2000, multiOrbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.CARTESIAN, multiOrbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.TRUE, multiOrbitalCovariance.getPositionAngle());
    }

    /**
     * Tests the constructors based on a {@linkplain SymmetricPositiveMatrix} and checks the basic
     * getters.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain MultiOrbitalCovariance#getCovariance()}<br>
     * {@linkplain MultiOrbitalCovariance#getCovarianceMatrix()}<br>
     * {@linkplain MultiOrbitalCovariance#getOrbits()}<br>
     * {@linkplain MultiOrbitalCovariance#getDate()}<br>
     * {@linkplain MultiOrbitalCovariance#getFrame()}<br>
     * {@linkplain MultiOrbitalCovariance#getOrbitType()}<br>
     * {@linkplain MultiOrbitalCovariance#getPositionAngle()}<br>
     * </p>
     */
    @Test
    public void testConstructorFromSymmetricPositiveMatrix() {
        MultiOrbitalCovariance multiOrbitalCovariance;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 3, 3 };

        // Create a new orbital covariance, specifying the frame, orbit type and position angle type
        multiOrbitalCovariance = new MultiOrbitalCovariance(COVARIANCE_MATRIX_18X18, ORBITS,
                nbAdditionalParameters, eme2000, OrbitType.CARTESIAN, PositionAngle.ECCENTRIC);
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_18X18,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);

        Assert.assertEquals(covariance, multiOrbitalCovariance.getCovariance());
        Assert.assertEquals(COVARIANCE_MATRIX_18X18, multiOrbitalCovariance.getCovarianceMatrix());
        Assert.assertEquals(DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18,
                multiOrbitalCovariance.getParameterDescriptors());
        Assert.assertEquals(ORBITS, multiOrbitalCovariance.getOrbits());
        Assert.assertEquals(DATE, multiOrbitalCovariance.getDate());
        Assert.assertEquals(eme2000, multiOrbitalCovariance.getFrame());
        Assert.assertEquals(OrbitType.CARTESIAN, multiOrbitalCovariance.getOrbitType());
        Assert.assertEquals(PositionAngle.ECCENTRIC, multiOrbitalCovariance.getPositionAngle());
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} when the provided covariance is
     * {@code null}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullCovariance() {

        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "covariance");

        // Test data
        final Covariance covariance = null;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Constructor based on a covariance
        try {
            new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, PositionAngle.TRUE);
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
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullCovarianceMatrix() {

        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "covariance matrix");

        // Test data
        final SymmetricPositiveMatrix covarianceMatrix = null;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 3, 3 };

        // Constructor based on a covariance matrix
        try {
            new MultiOrbitalCovariance(covarianceMatrix, ORBITS, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, PositionAngle.ECCENTRIC);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} or a
     * {@linkplain SymmetricPositiveMatrix} when the provided orbits list contains a {@code null}
     * element.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullOrbit() {

        // Test data
        final List<Orbit> orbitList1 = null;
        final List<Orbit> orbitList2 = new ArrayList<>();
        orbitList2.add(CARTESIAN_ORBIT_1);
        orbitList2.add(null);
        final List<Orbit> orbitList3 = new ArrayList<>();

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Covariance
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Constructors based on a covariance
        try {
            new MultiOrbitalCovariance(covariance, orbitList1, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(String.format(NULL_COLLECTION_FORMAT, "orbits list"),
                    e.getMessage());
        }
        try {
            new MultiOrbitalCovariance(covariance, orbitList2, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(String.format(NULL_ARGUMENT_FORMAT, "orbit"), e.getMessage());
        }
        try {
            new MultiOrbitalCovariance(covariance, orbitList3, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(String.format(EMPTY_COLLECTION_FORMAT, "orbits list"),
                    e.getMessage());
        }
    }

    /**
     * Tests the constructors based on a {@linkplain Covariance} or a
     * {@linkplain SymmetricPositiveMatrix} when the provided frame is {@code null}.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullFrame() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "frame");

        // Test data
        final Frame frame = null;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final SymmetricPositiveMatrix covarianceMatrix = COVARIANCE_MATRIX_12X12;

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Constructor based on a covariance
        try {
            new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, frame,
                    OrbitType.CARTESIAN, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructor based on a covariance matrix
        try {
            new MultiOrbitalCovariance(covarianceMatrix, ORBITS, nbAdditionalParameters, frame,
                    OrbitType.CARTESIAN, PositionAngle.TRUE);
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
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullOrbitType() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "orbit type");

        // Test data
        final OrbitType orbitType = null;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final SymmetricPositiveMatrix covarianceMatrix = COVARIANCE_MATRIX_12X12;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Constructor based on a covariance
        try {
            new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, eme2000,
                    orbitType, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructor based on a covariance matrix
        try {
            new MultiOrbitalCovariance(covarianceMatrix, ORBITS, nbAdditionalParameters, eme2000,
                    orbitType, PositionAngle.TRUE);
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
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorFromCovarianceNullPositionAngle() {
        // Expected exception message
        final String expectedMessage = String.format(NULL_ARGUMENT_FORMAT, "position angle type");

        // Test data
        final PositionAngle positionAngle = null;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final SymmetricPositiveMatrix covarianceMatrix = COVARIANCE_MATRIX_12X12;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Constructors based on a covariance
        try {
            new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructors based on a covariance matrix
        try {
            new MultiOrbitalCovariance(covarianceMatrix, ORBITS, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, positionAngle);
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
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorInvalidCovarianceDimension() {
        // Expected exception message
        final String expectedMessage = "Invalid covariance matrix for 2 orbits: the matrix should be at least 12x12, not 11x11";

        // Test data
        final SymmetricPositiveMatrix covarianceMatrix = new ArrayRowSymmetricPositiveMatrix(11);
        final Covariance covariance = new Covariance(covarianceMatrix);

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Constructors based on a covariance
        try {
            new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, PositionAngle.TRUE);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }

        // Constructors based on a covariance matrix
        try {
            new MultiOrbitalCovariance(covarianceMatrix, ORBITS, nbAdditionalParameters, eme2000,
                    OrbitType.CARTESIAN, PositionAngle.TRUE);
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
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorMissingParameterDescriptors() {

        // Format of the expected exception message
        final String expectedMessage = "No orbital coordinate descriptor on row number ";

        // Orbital coordinate descriptor
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // No orbital coordinate descriptor
        for (int i = 0; i < 6; i++) {
            // Test data
            final List<ParameterDescriptor> parameterDescriptors = ParameterUtils
                    .buildDefaultParameterDescriptors(12);
            parameterDescriptors.get(0).addField(fieldDescriptor, CartesianCoordinate.X);
            parameterDescriptors.get(1).addField(fieldDescriptor, CartesianCoordinate.Y);
            parameterDescriptors.get(2).addField(fieldDescriptor, CartesianCoordinate.Z);
            parameterDescriptors.get(3).addField(fieldDescriptor, CartesianCoordinate.VX);
            parameterDescriptors.get(4).addField(fieldDescriptor, CartesianCoordinate.VY);
            parameterDescriptors.get(5).addField(fieldDescriptor, CartesianCoordinate.VZ);
            parameterDescriptors.get(6).addField(fieldDescriptor, CartesianCoordinate.X);
            parameterDescriptors.get(7).addField(fieldDescriptor, CartesianCoordinate.Y);
            parameterDescriptors.get(8).addField(fieldDescriptor, CartesianCoordinate.Z);
            parameterDescriptors.get(9).addField(fieldDescriptor, CartesianCoordinate.VX);
            parameterDescriptors.get(10).addField(fieldDescriptor, CartesianCoordinate.VY);
            parameterDescriptors.get(11).addField(fieldDescriptor, CartesianCoordinate.VZ);
            parameterDescriptors.get(i).removeField(fieldDescriptor);
            final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                    parameterDescriptors);

            // Constructors based on a covariance
            try {
                new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, eme2000,
                        OrbitType.CARTESIAN, PositionAngle.TRUE);
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
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorWrongStateVectorIndex() {
        // Orbital coordinate descriptor
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // No orbital coordinate descriptor
        for (int i = 0; i < 6; i++) {
            // Test data
            final List<ParameterDescriptor> parameterDescriptors = copy(DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
            final int stateVectorIndex = (i + 1) % 6;
            final OrbitalCoordinate wrongCoordinate = CartesianCoordinate.valueOf(stateVectorIndex);
            parameterDescriptors.get(i).replaceField(fieldDescriptor, wrongCoordinate);
            final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                    parameterDescriptors);

            final String expectedMessage = String.format(INVALID_COVARIANCE_SIZE_FORMAT, i,
                    wrongCoordinate.toString(), "state vector index", stateVectorIndex, i);

            // Constructors based on a covariance
            try {
                new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, eme2000,
                        OrbitType.CARTESIAN, PositionAngle.TRUE);
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
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)}
     * <br>
     * </p>
     */
    @Test
    public void testConstructorWrongOrbitType() {
        // Orbital coordinate descriptor
        final FieldDescriptor<OrbitalCoordinate> fieldDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // No orbital coordinate descriptor
        for (int i = 0; i < 6; i++) {
            // Test data
            final List<ParameterDescriptor> parameterDescriptors = copy(DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
            final OrbitalCoordinate wrongCoordinate = KeplerianCoordinate.valueOf(i,
                    PositionAngle.TRUE);
            parameterDescriptors.get(i).replaceField(fieldDescriptor, wrongCoordinate);
            final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                    parameterDescriptors);

            final String expectedMessage = String.format(INVALID_COVARIANCE_SIZE_FORMAT, i,
                    wrongCoordinate.toString(), "orbit type", "KEPLERIAN", "CARTESIAN");

            // Constructors based on a covariance
            try {
                new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, eme2000,
                        OrbitType.CARTESIAN, PositionAngle.TRUE);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals(expectedMessage, e.getMessage());
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another frame (GCRF).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain OrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToGCRF() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Collection<Orbit> initialOrbits = ORBITS;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(indices1), ORBITS.get(0), initialFrame,
                initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(indices2), ORBITS.get(1), initialFrame,
                initialOrbitType, initialPositionAngle);

        // Expected covariance matrix
        final Frame destFrame = FramesFactory.getGCRF();
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);

        // Specify the destination frame, orbit type and position angle type
        result = initialMultiOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.TRUE);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                destFrame, OrbitType.CARTESIAN, PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame, initialOrbitType,
                PositionAngle.TRUE);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame, initialOrbitType,
                PositionAngle.TRUE);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.MEAN);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                destFrame, OrbitType.CARTESIAN, PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame, initialOrbitType,
                PositionAngle.MEAN);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame, initialOrbitType,
                PositionAngle.MEAN);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.ECCENTRIC);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                destFrame, OrbitType.CARTESIAN, PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame, initialOrbitType,
                PositionAngle.ECCENTRIC);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame, initialOrbitType,
                PositionAngle.ECCENTRIC);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        // Specify only the destination frame and orbit type
        result = initialMultiOrbitalCovariance.transformTo(destFrame, initialOrbitType);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                destFrame, OrbitType.CARTESIAN, initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame, initialOrbitType);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame, initialOrbitType);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        // Specify only the destination frame
        result = initialMultiOrbitalCovariance.transformTo(destFrame);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                destFrame, OrbitType.CARTESIAN, initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another frame (QSW).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToQSW() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Collection<Orbit> initialOrbits = ORBITS;
        final Orbit initialOrbit = ORBITS.get(0);
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_12X12;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        final Frame destFrame = new LocalOrbitalFrame(FramesFactory.getGCRF(), LOFType.QSW, initialOrbit, "QSW");

        // Expected covariance
        final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);

        int i = 0;
        final int[] startIndices = new int[] { 0, 6 };
        for (final Orbit orbit : ORBITS) {
            final RealMatrix subJacobians = orbit.getJacobian(initialFrame, destFrame,
                    initialOrbitType, initialOrbitType, initialPositionAngle, initialPositionAngle);
            final int startIndex = startIndices[i];
            jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
            i++;
        }
        final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);

        // Specify the destination frame, orbit type and position angle type
        result = initialMultiOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.TRUE);
        expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                destFrame, initialOrbitType, PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame, initialOrbitType,
                PositionAngle.TRUE);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame, initialOrbitType,
                PositionAngle.TRUE);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.MEAN);
        expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                destFrame, initialOrbitType, PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame, initialOrbitType,
                PositionAngle.MEAN);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame, initialOrbitType,
                PositionAngle.MEAN);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(destFrame, initialOrbitType,
                PositionAngle.ECCENTRIC);
        expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                destFrame, initialOrbitType, PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame, initialOrbitType,
                PositionAngle.ECCENTRIC);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame, initialOrbitType,
                PositionAngle.ECCENTRIC);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        // Specify only the destination frame and orbit type
        result = initialMultiOrbitalCovariance.transformTo(destFrame, initialOrbitType);
        expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                destFrame, initialOrbitType, initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame, initialOrbitType);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame, initialOrbitType);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        // Specify only the destination frame
        result = initialMultiOrbitalCovariance.transformTo(destFrame);
        expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                destFrame, initialOrbitType, initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destFrame);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destFrame);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

    }

    /**
     * Tests the methods that transform the multi orbital covariance to a given type of local
     * orbital frame.
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(int, LOFType, boolean)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToLOF() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Collection<Orbit> initialOrbits = ORBITS;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_12X12;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        final List<LOFType> evaluatedLOF = new ArrayList<>();
        evaluatedLOF.add(LOFType.QSW);
        evaluatedLOF.add(LOFType.TNW);

        // Test the methods for different LOF types
        for (final LOFType lofType : evaluatedLOF) {
            // Test on each orbit
            for (final Orbit orbit : ORBITS) {

                final String expectedFrameName = lofType.name();
                final Frame destFrame = new LocalOrbitalFrame(FramesFactory.getGCRF(), lofType, orbit,
                        expectedFrameName);

                final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
                int i = 0;
                final int[] startIndices = new int[] { 0, 6 };
                for (final Orbit orbitBis : ORBITS) {
                    final RealMatrix subJacobians = orbitBis.getJacobian(initialFrame, destFrame,
                            initialOrbitType, initialOrbitType, initialPositionAngle,
                            initialPositionAngle);
                    final int startIndex = startIndices[i];
                    jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                    i++;
                }

                // Expected covariance
                final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                        .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                                initialOrbitType, initialPositionAngle);
                final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                        parameterDescriptors);

                // Default orbit
                result = initialMultiOrbitalCovariance.transformTo(orbit, lofType, false);
                expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                        result.getFrame(), initialOrbitType, initialPositionAngle);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                Assert.assertEquals(expectedFrameName, result.getFrame().getName());
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(orbit, lofType, false);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(orbit, lofType, false);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL, false);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to a given type of local
     * orbital frame.
     * <p>
     * This test specifies the initial frame as a non-pseudo inertial frame (ITRF). All the orbits
     * are defined in a pseudo inertial frame (GCRF). The LOF should be built in GCRF, but the
     * reference orbit shouldn't be converted.
     * </p>
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToLOFInertialFrame1st() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getITRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final Collection<Orbit> initialOrbits = ORBITS;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_12X12;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        final List<LOFType> evaluatedLOF = new ArrayList<>();
        evaluatedLOF.add(LOFType.QSW);
        evaluatedLOF.add(LOFType.TNW);

        // Test the methods for different LOF types
        for (final LOFType lofType : evaluatedLOF) {
            // Test on each orbit
            for (final Orbit orbit : ORBITS) {

                final String expectedFrameName = lofType.name();
                final Frame destFrame = new LocalOrbitalFrame(FramesFactory.getGCRF(), lofType, orbit,
                        expectedFrameName);

                final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
                int i = 0;
                final int[] startIndices = new int[] { 0, 6 };
                for (final Orbit orbitBis : ORBITS) {
                    final RealMatrix subJacobians = orbitBis.getJacobian(initialFrame, destFrame,
                            initialOrbitType, initialOrbitType, initialPositionAngle,
                            initialPositionAngle);
                    final int startIndex = startIndices[i];
                    jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                    i++;
                }

                // Expected covariance
                final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                        .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                                initialOrbitType, initialPositionAngle);
                final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                        parameterDescriptors);

                // Default orbit
                result = initialMultiOrbitalCovariance.transformTo(orbit, lofType, false);
                expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                        result.getFrame(), initialOrbitType, initialPositionAngle);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                Assert.assertEquals(expectedFrameName, result.getFrame().getName());
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(orbit, lofType, false);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(orbit, lofType, false);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL, false);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to a given type of local
     * orbital frame.
     * <p>
     * This test specifies the initial frame as a non-pseudo inertial frame (ITRF). All the orbits
     * are also defined in a non-pseudo inertial frame (ITRF). The LOF should be built in GCRF and
     * the reference orbit should be converted in this frame before being stored in the LOF.
     * </p>
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToLOFInertialFrame2nd() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getITRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final List<Orbit> initialOrbits = new ArrayList<>();
        initialOrbits.add(ORBITS.get(0).getType().convertOrbit(ORBITS.get(0), initialFrame));
        initialOrbits.add(ORBITS.get(1).getType().convertOrbit(ORBITS.get(1), initialFrame));
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_12X12;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }),
                initialOrbits.get(0), initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                initialOrbits.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        final List<LOFType> evaluatedLOF = new ArrayList<>();
        evaluatedLOF.add(LOFType.QSW);
        evaluatedLOF.add(LOFType.TNW);

        // Test the methods for different LOF types
        for (final LOFType lofType : evaluatedLOF) {
            // Test on each orbit
            for (final Orbit orbit : initialOrbits) {

                final String expectedFrameName = lofType.name();

                // Orbit converted back in GCRF (pseudo-inertial frame)
                final Orbit orbitInertial = orbit.getType().convertOrbit(orbit, FramesFactory.getGCRF());
                final Frame destFrame = new LocalOrbitalFrame(FramesFactory.getGCRF(), lofType, orbitInertial,
                        expectedFrameName);

                final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
                int i = 0;
                final int[] startIndices = new int[] { 0, 6 };
                for (final Orbit orbitBis : ORBITS) {
                    final RealMatrix subJacobians = orbitBis.getJacobian(initialFrame, destFrame,
                            initialOrbitType, initialOrbitType, initialPositionAngle,
                            initialPositionAngle);
                    final int startIndex = startIndices[i];
                    jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                    i++;
                }

                // Expected covariance
                final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                        .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                                initialOrbitType, initialPositionAngle);
                final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                        parameterDescriptors);

                // Default orbit
                result = initialMultiOrbitalCovariance.transformTo(orbit, lofType, false);
                expected = new MultiOrbitalCovariance(covariance, initialOrbits,
                        nbAdditionalParameters, destFrame, initialOrbitType, initialPositionAngle);

                checkEquality(expected, result, ABSTOL, RELTOL, false);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                Assert.assertEquals(expectedFrameName, result.getFrame().getName());
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(orbit, lofType, false);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(orbit, lofType, false);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL, false);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to a given type of local
     * orbital frame.
     * <p>
     * This test specifies the initial frame as a pseudo inertial frame (GCRF). All the orbits are
     * defined in a non-pseudo inertial frame (ITRF). The LOF should be built in GCRF and the
     * reference orbit should be converted in this frame before being stored in the LOF.
     * </p>
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToLOFInertialFrame3rd() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final List<Orbit> initialOrbits = new ArrayList<>();
        initialOrbits.add(ORBITS.get(0).getType().convertOrbit(ORBITS.get(0), itrf));
        initialOrbits.add(ORBITS.get(1).getType().convertOrbit(ORBITS.get(1), itrf));
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_12X12;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }),
                initialOrbits.get(0), initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                initialOrbits.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        final List<LOFType> evaluatedLOF = new ArrayList<>();
        evaluatedLOF.add(LOFType.QSW);
        evaluatedLOF.add(LOFType.TNW);

        // Test the methods for different LOF types
        for (final LOFType lofType : evaluatedLOF) {
            // Test on each orbit
            for (final Orbit orbit : initialOrbits) {

                final String expectedFrameName = lofType.name();

                // Orbit converted back in GCRF (pseudo-inertial frame)
                final Orbit orbitInertial = orbit.getType().convertOrbit(orbit, initialFrame);
                final Frame destFrame = new LocalOrbitalFrame(FramesFactory.getGCRF(), lofType, orbitInertial,
                        expectedFrameName);

                final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
                int i = 0;
                final int[] startIndices = new int[] { 0, 6 };
                for (final Orbit orbitBis : initialOrbits) {
                    final RealMatrix subJacobians = orbitBis.getJacobian(initialFrame, destFrame,
                            initialOrbitType, initialOrbitType, initialPositionAngle,
                            initialPositionAngle);
                    final int startIndex = startIndices[i];
                    jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                    i++;
                }

                // Expected covariance
                final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                        .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                                initialOrbitType, initialPositionAngle);
                final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                        parameterDescriptors);

                // Default orbit
                result = initialMultiOrbitalCovariance.transformTo(orbit, lofType, false);
                expected = new MultiOrbitalCovariance(covariance, initialOrbits,
                        nbAdditionalParameters, destFrame, initialOrbitType, initialPositionAngle);
                checkEquality(expected, result, ABSTOL, RELTOL, false);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                Assert.assertEquals(expectedFrameName, result.getFrame().getName());
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(orbit, lofType, false);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(orbit, lofType, false);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL, false);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to a given type of local
     * orbital frame (frozen).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Orbit, LOFType, boolean)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToFrozenLOF() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final List<Orbit> initialOrbits = ORBITS;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_12X12;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }),
                initialOrbits.get(0), initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                initialOrbits.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        final List<LOFType> evaluatedLOF = new ArrayList<>();
        evaluatedLOF.add(LOFType.QSW);
        evaluatedLOF.add(LOFType.TNW);

        // Test the methods for different LOF types
        for (final LOFType lofType : evaluatedLOF) {
            // Test on each orbit
            for (int i = 0; i < ORBITS.size(); i++) {

                final Orbit orbit = ORBITS.get(i);

                final Frame destFrame = new LocalOrbitalFrame(FramesFactory.getGCRF(), lofType, orbit, lofType.name())
                        .getFrozenFrame(initialFrame, orbit.getDate(), "Frozen_" + lofType.name());

                final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
                int j = 0;
                final int[] startIndices = new int[] { 0, 6 };
                for (final Orbit orbitBis : ORBITS) {
                    final RealMatrix subJacobians = orbitBis.getJacobian(initialFrame, destFrame,
                            initialOrbitType, initialOrbitType, initialPositionAngle,
                            initialPositionAngle);
                    final int startIndex = startIndices[j];
                    jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                    j++;
                }

                // Expected covariance
                final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                        .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                                initialOrbitType, initialPositionAngle);
                final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                        parameterDescriptors);

                // Expected frame name
                final String expectedFrameName = "Frozen_" + lofType.name();

                // Default orbit
                result = initialMultiOrbitalCovariance.transformTo(i, lofType, true);
                expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                        result.getFrame(), initialOrbitType, initialPositionAngle);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                Assert.assertEquals(expectedFrameName, result.getFrame().getName());
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(orbit, lofType, true);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(orbit, lofType, true);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL, false);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another orbit type
     * (Cartesian coordinates).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType, PositionAngle)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToCartesian() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final List<Orbit> initialOrbits = ORBITS;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }),
                initialOrbits.get(0), initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                initialOrbits.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        // Expected covariance matrix
        final OrbitType destOrbitType = OrbitType.CARTESIAN;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);

        // Specify the destination frame, orbit type and position angle type
        result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                PositionAngle.TRUE);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                PositionAngle.TRUE);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                PositionAngle.TRUE);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                PositionAngle.MEAN);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                PositionAngle.MEAN);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                PositionAngle.MEAN);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                PositionAngle.ECCENTRIC);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                PositionAngle.ECCENTRIC);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                PositionAngle.ECCENTRIC);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        // Specify only the destination frame and orbit type
        result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        // Specify only the destination orbit type and position angle type
        result = initialMultiOrbitalCovariance.transformTo(destOrbitType, PositionAngle.TRUE);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                PositionAngle.TRUE);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                PositionAngle.TRUE);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(destOrbitType, PositionAngle.MEAN);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                PositionAngle.MEAN);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                PositionAngle.MEAN);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(destOrbitType, PositionAngle.ECCENTRIC);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                PositionAngle.ECCENTRIC);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                PositionAngle.ECCENTRIC);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        // Specify only the destination orbit type
        result = initialMultiOrbitalCovariance.transformTo(destOrbitType);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, initialPositionAngle);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        // Specify only the destination position angle type
        result = initialMultiOrbitalCovariance.transformTo(PositionAngle.TRUE);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(PositionAngle.TRUE);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(PositionAngle.TRUE);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(PositionAngle.MEAN);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.MEAN);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(PositionAngle.MEAN);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(PositionAngle.MEAN);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);

        result = initialMultiOrbitalCovariance.transformTo(PositionAngle.ECCENTRIC);
        expected = new MultiOrbitalCovariance(covariance, initialOrbits, nbAdditionalParameters,
                initialFrame, destOrbitType, PositionAngle.ECCENTRIC);
        checkEquality(expected, result, ABSTOL, RELTOL);
        Assert.assertNotSame(initialMultiOrbitalCovariance, result);
        orbitalCovariance1 = initialOrbitalCovariance1.transformTo(PositionAngle.ECCENTRIC);
        orbitalCovariance2 = initialOrbitalCovariance2.transformTo(PositionAngle.ECCENTRIC);
        checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                indices2, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another orbit type
     * (alternate equinoctial parameters).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToAlternateEquinoctial() throws PatriusException {
        MultiOrbitalCovariance result;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, initialFrame, initialOrbitType,
                initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        // Destination orbit type
        final OrbitType destOrbitType = OrbitType.ALTERNATE_EQUINOCTIAL;

        // Test the methods for different destination position angles
        for (final PositionAngle destPositionAngle : PositionAngle.values()) {

            // Expected covariance
            final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
            int i = 0;
            final int[] startIndices = new int[] { 0, 6 };
            for (final Orbit orbit : ORBITS) {
                final RealMatrix subJacobians = orbit.getJacobian(initialFrame, initialFrame,
                        initialOrbitType, destOrbitType, initialPositionAngle, destPositionAngle);
                final int startIndex = startIndices[i];
                jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                i++;
            }

            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                    parameterDescriptors);
            final MultiOrbitalCovariance expected = new MultiOrbitalCovariance(covariance, ORBITS,
                    nbAdditionalParameters, initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            // Specify only the destination orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame,
                        destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame,
                        destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);

                // Specify only the destination orbit type
                result = initialMultiOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another orbit type
     * (equinoctial parameters).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToEquinoctial() throws PatriusException {
        MultiOrbitalCovariance result;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, initialFrame, initialOrbitType,
                initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        // Destination orbit type
        final OrbitType destOrbitType = OrbitType.EQUINOCTIAL;

        // Test the methods for different destination position angles
        for (final PositionAngle destPositionAngle : PositionAngle.values()) {

            // Expected covariance
            final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
            int i = 0;
            final int[] startIndices = new int[] { 0, 6 };
            for (final Orbit orbit : ORBITS) {
                final RealMatrix subJacobians = orbit.getJacobian(initialFrame, initialFrame,
                        initialOrbitType, destOrbitType, initialPositionAngle, destPositionAngle);
                final int startIndex = startIndices[i];
                jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                i++;
            }

            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                    parameterDescriptors);
            final MultiOrbitalCovariance expected = new MultiOrbitalCovariance(covariance, ORBITS,
                    nbAdditionalParameters, initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            // Specify only the destination orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame,
                        destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame,
                        destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);

                // Specify only the destination orbit type
                result = initialMultiOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another orbit type (circular
     * parameters).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToCircular() throws PatriusException {
        MultiOrbitalCovariance result;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, initialFrame, initialOrbitType,
                initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        // Destination orbit type
        final OrbitType destOrbitType = OrbitType.CIRCULAR;

        // Test the methods for different destination position angles
        for (final PositionAngle destPositionAngle : PositionAngle.values()) {

            // Expected covariance
            final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
            int i = 0;
            final int[] startIndices = new int[] { 0, 6 };
            for (final Orbit orbit : ORBITS) {
                final RealMatrix subJacobians = orbit.getJacobian(initialFrame, initialFrame,
                        initialOrbitType, destOrbitType, initialPositionAngle, destPositionAngle);
                final int startIndex = startIndices[i];
                jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                i++;
            }

            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                    parameterDescriptors);
            final MultiOrbitalCovariance expected = new MultiOrbitalCovariance(covariance, ORBITS,
                    nbAdditionalParameters, initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            // Specify only the destination orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame,
                        destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame,
                        destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);

                // Specify only the destination orbit type
                result = initialMultiOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another orbit type
     * (equatorial parameters).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToEquatorial() throws PatriusException {
        MultiOrbitalCovariance result;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, initialFrame, initialOrbitType,
                initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        // Destination orbit type
        final OrbitType destOrbitType = OrbitType.EQUATORIAL;

        // Test the methods for different destination position angles
        for (final PositionAngle destPositionAngle : PositionAngle.values()) {

            // Expected covariance
            final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
            int i = 0;
            final int[] startIndices = new int[] { 0, 6 };
            for (final Orbit orbit : ORBITS) {
                final RealMatrix subJacobians = orbit.getJacobian(initialFrame, initialFrame,
                        initialOrbitType, destOrbitType, initialPositionAngle, destPositionAngle);
                final int startIndex = startIndices[i];
                jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                i++;
            }

            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                    parameterDescriptors);
            final MultiOrbitalCovariance expected = new MultiOrbitalCovariance(covariance, ORBITS,
                    nbAdditionalParameters, initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            // Specify only the destination orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame,
                        destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame,
                        destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);

                // Specify only the destination orbit type
                result = initialMultiOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another orbit type
     * (Keplerian parameters).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToKeplerian() throws PatriusException {
        MultiOrbitalCovariance result;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, initialFrame, initialOrbitType,
                initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        // Destination orbit type
        final OrbitType destOrbitType = OrbitType.KEPLERIAN;

        // Test the methods for different destination position angles
        for (final PositionAngle destPositionAngle : PositionAngle.values()) {

            // Expected covariance
            final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
            int i = 0;
            final int[] startIndices = new int[] { 0, 6 };
            for (final Orbit orbit : ORBITS) {
                final RealMatrix subJacobians = orbit.getJacobian(initialFrame, initialFrame,
                        initialOrbitType, destOrbitType, initialPositionAngle, destPositionAngle);
                final int startIndex = startIndices[i];
                jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                i++;
            }

            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                    parameterDescriptors);
            final MultiOrbitalCovariance expected = new MultiOrbitalCovariance(covariance, ORBITS,
                    nbAdditionalParameters, initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            // Specify only the destination orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame,
                        destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame,
                        destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);

                // Specify only the destination orbit type
                result = initialMultiOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the methods that transform the multi orbital covariance to another orbit type (apsis
     * parameters).
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType, PositionAngle)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Frame, OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(OrbitType, PositionAngle)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform6x6ToApsis() throws PatriusException {
        MultiOrbitalCovariance result;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, initialFrame, initialOrbitType,
                initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5 };
        final int[] indices2 = new int[] { 6, 7, 8, 9, 10, 11 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 0, 1, 2, 3, 4, 5 }), ORBITS.get(0),
                initialFrame, initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(new int[] { 6, 7, 8, 9, 10, 11 }),
                ORBITS.get(1), initialFrame, initialOrbitType, initialPositionAngle);

        // Destination orbit type
        final OrbitType destOrbitType = OrbitType.APSIS;

        // Test the methods for different destination position angles
        for (final PositionAngle destPositionAngle : PositionAngle.values()) {

            // Expected covariance
            final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
            int i = 0;
            final int[] startIndices = new int[] { 0, 6 };
            for (final Orbit orbit : ORBITS) {
                final RealMatrix subJacobians = orbit.getJacobian(initialFrame, initialFrame,
                        initialOrbitType, destOrbitType, initialPositionAngle, destPositionAngle);
                final int startIndex = startIndices[i];
                jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                i++;
            }

            final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                    .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                            destOrbitType, destPositionAngle);
            final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                    parameterDescriptors);
            final MultiOrbitalCovariance expected = new MultiOrbitalCovariance(covariance, ORBITS,
                    nbAdditionalParameters, initialFrame, destOrbitType, destPositionAngle);

            // Specify the destination frame, orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame, destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            // Specify only the destination orbit type and position angle type
            result = initialMultiOrbitalCovariance.transformTo(destOrbitType, destPositionAngle);
            checkEquality(expected, result, ABSTOL, RELTOL);
            Assert.assertNotSame(initialMultiOrbitalCovariance, result);
            orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType,
                    destPositionAngle);
            orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType,
                    destPositionAngle);
            checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                    indices2, ABSTOL, RELTOL);

            if (destPositionAngle.equals(initialPositionAngle)) {
                // Specify only the destination frame and orbit type
                result = initialMultiOrbitalCovariance.transformTo(initialFrame, destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(initialFrame,
                        destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(initialFrame,
                        destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);

                // Specify only the destination orbit type
                result = initialMultiOrbitalCovariance.transformTo(destOrbitType);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(destOrbitType);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(destOrbitType);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL);
            }
        }
    }

    /**
     * Tests the method that shifts the multi orbital covariance by a given duration.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#shiftedBy(double)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testShiftedBy6x6() throws PatriusException {

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;

        // Initial covariance
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, frame, orbitType, positionAngle);

        // Shifted orbit and covariance
        final double dt = 10000;
        final List<Orbit> shiftedOrbits = new ArrayList<Orbit>();
        for (final Orbit orbit : ORBITS) {
            shiftedOrbits.add(orbit.shiftedBy(dt));
        }
        final MultiOrbitalCovariance result = initialMultiOrbitalCovariance.shiftedBy(dt);

        // Expected covariance matrix
        final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(12);
        int i = 0;
        final int[] startIndices = new int[] { 0, 6 };
        for (final Orbit orbit : ORBITS) {
            final RealMatrix subJacobians = orbit.getJacobian(frame, frame, orbitType, orbitType,
                    positionAngle, positionAngle);
            final int startIndex = startIndices[i];
            jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
            i++;
        }
        final Covariance shiftedCovariance = initialCovariance.quadraticMultiplication(jacobians,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);

        final MultiOrbitalCovariance expected = new MultiOrbitalCovariance(shiftedCovariance,
                shiftedOrbits, nbAdditionalParameters, FramesFactory.getGCRF(), OrbitType.CARTESIAN,
                PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
    }

    /**
     * Tests the methods that transform the multi orbital covariance to a given type of local
     * orbital
     * frame.
     * <p>
     * The transformed multi orbital covariance is also evaluated against the associated transformed
     * orbital covariances.
     * </p>
     * 
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(LOFType, boolean)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Orbit, LOFType, boolean)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransform9x9ToLOF() throws PatriusException {
        MultiOrbitalCovariance result;
        MultiOrbitalCovariance expected;
        OrbitalCovariance orbitalCovariance1;
        OrbitalCovariance orbitalCovariance2;

        // Initial covariance
        final Frame initialFrame = FramesFactory.getGCRF();
        final OrbitType initialOrbitType = OrbitType.CARTESIAN;
        final PositionAngle initialPositionAngle = PositionAngle.ECCENTRIC;
        final Collection<Orbit> initialOrbits = ORBITS;
        final int[] nbAdditionalParameters = new int[] { 3, 3 };
        final SymmetricPositiveMatrix initialCovarianceMatrix = COVARIANCE_MATRIX_18X18;
        final Covariance initialCovariance = new Covariance(initialCovarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, initialOrbits, nbAdditionalParameters, initialFrame,
                initialOrbitType, initialPositionAngle);

        final int[] indices1 = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
        final int[] indices2 = new int[] { 9, 10, 11, 12, 13, 14, 15, 16, 17 };
        final OrbitalCovariance initialOrbitalCovariance1 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(indices1), ORBITS.get(0), initialFrame,
                initialOrbitType, initialPositionAngle);
        final OrbitalCovariance initialOrbitalCovariance2 = new OrbitalCovariance(
                initialCovariance.getSubCovariance(indices2), ORBITS.get(1), initialFrame,
                initialOrbitType, initialPositionAngle);

        final List<LOFType> evaluatedLOF = new ArrayList<>();
        evaluatedLOF.add(LOFType.QSW);
        evaluatedLOF.add(LOFType.TNW);

        // Test the methods for different LOF types
        for (final LOFType lofType : evaluatedLOF) {
            // Test on each orbit
            for (final Orbit orbit : ORBITS) {

                final String expectedFrameName = lofType.name();
                final Frame destFrame = new LocalOrbitalFrame(FramesFactory.getGCRF(), lofType, orbit,
                        expectedFrameName);

                final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(18);
                int i = 0;
                final int[] startIndices = new int[] { 0, 9 };
                for (final Orbit orbitBis : ORBITS) {
                    final RealMatrix subJacobians = orbitBis.getJacobian(initialFrame, destFrame,
                            initialOrbitType, initialOrbitType, initialPositionAngle,
                            initialPositionAngle);
                    final int startIndex = startIndices[i];
                    jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
                    i++;
                }

                // Expected orbital covariance
                final List<ParameterDescriptor> parameterDescriptors = AbstractOrbitalCovariance
                        .convertParameterDescriptors(initialCovariance.getParameterDescriptors(),
                                initialOrbitType, initialPositionAngle);
                final Covariance covariance = initialCovariance.quadraticMultiplication(jacobians,
                        parameterDescriptors);

                // Default orbit
                result = initialMultiOrbitalCovariance.transformTo(orbit, lofType, false);
                expected = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters,
                        result.getFrame(), initialOrbitType, initialPositionAngle);
                checkEquality(expected, result, ABSTOL, RELTOL);
                Assert.assertNotSame(initialMultiOrbitalCovariance, result);
                Assert.assertEquals(expectedFrameName, result.getFrame().getName());
                orbitalCovariance1 = initialOrbitalCovariance1.transformTo(orbit, lofType, false);
                orbitalCovariance2 = initialOrbitalCovariance2.transformTo(orbit, lofType, false);
                checkMultiOrbCovVSOrbCov(result, orbitalCovariance1, orbitalCovariance2, indices1,
                        indices2, ABSTOL, RELTOL, false);
            }
        }
    }

    /**
     * Tests to transform a covariance with an orbit defined at a different date (should fail).
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#checkOrbit(Orbit)}<br>
     * {@linkplain MultiOrbitalCovariance#transformTo(Orbit, LOFType, boolean)}<br>
     * </p>
     * @throws PatriusException if an unexpected error occurs during the test
     */
    @Test
    public void testTransformWrongOrbitDate() throws PatriusException {

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Test data
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, FramesFactory.getGCRF(), OrbitType.CARTESIAN,
                PositionAngle.ECCENTRIC);

        final Orbit shiftedOrbit = ORBITS.get(0).shiftedBy(1.);

        // Expected exception message
        final String expectedMessage = "The provided orbit is not defined at the same date as the covariance ("
                + shiftedOrbit.getDate() + " != " + ORBITS.get(0).getDate() + ")";

        try {
            initialMultiOrbitalCovariance.transformTo(shiftedOrbit, LOFType.TNW, false);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the method that shifts the multi orbital covariance by a given duration.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#shiftedBy(double)}
     * </p>
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testShiftedBy9x9() throws PatriusException {

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 3, 3 };

        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;

        // Initial covariance
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_18X18,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);
        final MultiOrbitalCovariance initialMultiOrbitalCovariance = new MultiOrbitalCovariance(
                initialCovariance, ORBITS, nbAdditionalParameters, frame, orbitType, positionAngle);

        // Shifted orbit and covariance
        final double dt = 10000;
        final List<Orbit> shiftedOrbits = new ArrayList<Orbit>();
        for (final Orbit orbit : ORBITS) {
            shiftedOrbits.add(orbit.shiftedBy(dt));
        }
        final MultiOrbitalCovariance result = initialMultiOrbitalCovariance.shiftedBy(dt);

        // Expected covariance matrix
        final RealMatrix jacobians = MatrixUtils.createRealIdentityMatrix(18);
        int i = 0;
        final int[] startIndices = new int[] { 0, 9 };
        for (final Orbit orbit : ORBITS) {
            final RealMatrix subJacobians = orbit.getJacobian(frame, frame, orbitType, orbitType,
                    positionAngle, positionAngle);
            final int startIndex = startIndices[i];
            jacobians.setSubMatrix(subJacobians.getData(false), startIndex, startIndex);
            i++;
        }
        final Covariance shiftedCovariance = initialCovariance.quadraticMultiplication(jacobians,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);

        final MultiOrbitalCovariance expected = new MultiOrbitalCovariance(shiftedCovariance,
                shiftedOrbits, nbAdditionalParameters, FramesFactory.getGCRF(), OrbitType.CARTESIAN,
                PositionAngle.TRUE);
        checkEquality(expected, result, ABSTOL, RELTOL);
    }

    /**
     * Tests the method that extract the relative covariance between two orbits.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#getRelativeCovariance(int, int)}
     * </p>
     */
    @Test
    public void testRelativeCovariance() {

        // Test data
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 3, 3 };
        final SymmetricPositiveMatrix covarianceMatrix = COVARIANCE_MATRIX_18X18;
        final Covariance covariance = new Covariance(covarianceMatrix,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);
        final MultiOrbitalCovariance multiOrbitalCovariance = new MultiOrbitalCovariance(
                covariance, ORBITS, nbAdditionalParameters, frame, orbitType, positionAngle);

        final Covariance relativeCovariance = multiOrbitalCovariance.getRelativeCovariance(0, 1);

        // Build the expected relative covariance from the initial data
        final FieldDescriptor<OrbitalCoordinate> coordinateDescriptor = StandardFieldDescriptors.ORBITAL_COORDINATE;
        final List<ParameterDescriptor> expectedParameterDescriptors = new ArrayList<>();
        expectedParameterDescriptors.add(new ParameterDescriptor(coordinateDescriptor,
                CartesianCoordinate.X));
        expectedParameterDescriptors.add(new ParameterDescriptor(coordinateDescriptor,
                CartesianCoordinate.Y));
        expectedParameterDescriptors.add(new ParameterDescriptor(coordinateDescriptor,
                CartesianCoordinate.Z));
        expectedParameterDescriptors.add(new ParameterDescriptor(coordinateDescriptor,
                CartesianCoordinate.VX));
        expectedParameterDescriptors.add(new ParameterDescriptor(coordinateDescriptor,
                CartesianCoordinate.VY));
        expectedParameterDescriptors.add(new ParameterDescriptor(coordinateDescriptor,
                CartesianCoordinate.VZ));

        // Expected covariance matrix (non-regression)
        final double[][] expectedData = new double[][] {
                { 224.21, 82.620, 45.510, 50.600, 54.010, -79.810 },
                { 82.620, 190.89, -22.550, 44.970, 54.290, -85.920 },
                { 45.510, -22.550, 176.15, 39.430, -33.780, 1.5400 },
                { 50.600, 44.970, 39.430, 190.74, -0.39000, 20.560 },
                { 54.010, 54.290, -33.780, -0.39000, 314.20, -88.710 },
                { -79.810, -85.920, 1.5400, 20.560, -88.710, 301.97 } };
        final ArrayRowSymmetricPositiveMatrix expectedCovarianceMatrix = new ArrayRowSymmetricPositiveMatrix(
                SymmetryType.LOWER, expectedData);
        final Covariance expectedCovariance = new Covariance(expectedCovarianceMatrix,
                expectedParameterDescriptors);

        // Evaluate the result
        Assert.assertTrue(relativeCovariance.getParameterDescriptors().equals(
                expectedCovariance.getParameterDescriptors()));
        Assert.assertTrue(relativeCovariance.getCovarianceMatrix().equals(
                expectedCovariance.getCovarianceMatrix(), 1e-12, 1e-12));
    }

    /**
     * Tests the method that extract the orbital covariance associated to one orbit.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#getOrbitalCovariance(int)}
     * </p>
     */
    @Test
    public void testGetOrbitalCovariance() {

        // Test data
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 3, 3 };
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_18X18,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);
        final MultiOrbitalCovariance multiOrbitalCovariance = new MultiOrbitalCovariance(
                covariance, ORBITS, nbAdditionalParameters, frame, orbitType, positionAngle);

        final OrbitalCovariance orbitalCovariance1 = multiOrbitalCovariance.getOrbitalCovariance(0);
        final OrbitalCovariance orbitalCovariance2 = multiOrbitalCovariance.getOrbitalCovariance(1);

        // Build the expected covariance from the initial data
        final SymmetricPositiveMatrix expectedCovarianceMatrix1 = COVARIANCE_MATRIX_18X18
                .getSubMatrix(0, 8);
        final SymmetricPositiveMatrix expectedCovarianceMatrix2 = COVARIANCE_MATRIX_18X18
                .getSubMatrix(9, 17);

        final List<ParameterDescriptor> expectedParameterDescriptors1 = new ArrayList<>();
        final List<ParameterDescriptor> expectedParameterDescriptors2 = new ArrayList<>();
        for (int i = 0; i < DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18.size(); i++) {
            final ParameterDescriptor currentParam = DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18
                    .get(i);
            if (i < 9) {
                expectedParameterDescriptors1.add(currentParam);
            } else {
                expectedParameterDescriptors2.add(currentParam);
            }
        }

        final OrbitalCovariance expectedOrbitalCovariance1 = new OrbitalCovariance(new Covariance(
                expectedCovarianceMatrix1, expectedParameterDescriptors1), ORBITS.get(0));
        final OrbitalCovariance expectedOrbitalCovariance2 = new OrbitalCovariance(new Covariance(
                expectedCovarianceMatrix2, expectedParameterDescriptors2), ORBITS.get(1));

        // Check the results
        checkEquality(expectedOrbitalCovariance1, orbitalCovariance1, ABSTOL, RELTOL);
        checkEquality(expectedOrbitalCovariance2, orbitalCovariance2, ABSTOL, RELTOL);
    }

    /**
     * Tests the method that extract a stored orbit.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#getOrbit(int))}
     * </p>
     */
    @Test
    public void testGetOrbit() {

        // Test data
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 3, 3 };
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_18X18,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);
        final MultiOrbitalCovariance multiOrbitalCovariance = new MultiOrbitalCovariance(
                covariance, ORBITS, nbAdditionalParameters, frame, orbitType, positionAngle);

        Assert.assertTrue(multiOrbitalCovariance.getOrbit(0).equals(ORBITS.get(0)));
        Assert.assertTrue(multiOrbitalCovariance.getOrbit(1).equals(ORBITS.get(1)));
    }

    /**
     * Tests the exception cases of the method that extract a orbit.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#getOrbit(int))}
     * </p>
     */
    @Test
    public void testGetOrbitException() {

        // Test data
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 3, 3 };
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_18X18,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);
        final MultiOrbitalCovariance multiOrbitalCovariance = new MultiOrbitalCovariance(
                covariance, ORBITS, nbAdditionalParameters, frame, orbitType, positionAngle);

        // Try to get an orbit with an invalid index
        try {
            multiOrbitalCovariance.getOrbit(-1);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Invalid orbit index: -1 is not between 0 and 1", e.getMessage());
        }
        try {
            multiOrbitalCovariance.getOrbit(2);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Invalid orbit index: 2 is not between 0 and 1", e.getMessage());
        }
    }

    /**
     * Tests the exception case when we try to build a multi orbital covariance with orbits defined
     * at different dates.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)
     * )}
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)
     * )}
     * </p>
     */
    @Test
    public void testOrbitsDateException() {

        // Test data
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 3, 3 };
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_18X18,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);

        final List<Orbit> orbits = new ArrayList<>();
        orbits.add(ORBITS.get(0));
        orbits.add(ORBITS.get(1).shiftedBy(1.));

        // Expected exception message
        final String expectedMessage = "Invalid orbit date ("
                + ORBITS.get(1).shiftedBy(1.).getDate()
                + "): the orbits must all be defined at the same date (" + ORBITS.get(0).getDate()
                + ")";

        // Try to build a multi orbital covariance with orbits defined at different dates
        try {
            new MultiOrbitalCovariance(covariance, orbits, nbAdditionalParameters, frame,
                    orbitType, positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
        try {
            new MultiOrbitalCovariance(COVARIANCE_MATRIX_18X18, orbits, nbAdditionalParameters,
                    frame, orbitType, positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Tests the exception case when we try to build a multi orbital covariance with invalid
     * additional parameters array information.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(Covariance, Collection, int[], Frame, OrbitType, PositionAngle)
     * )}
     * {@linkplain MultiOrbitalCovariance#MultiOrbitalCovariance(SymmetricPositiveMatrix, Collection, int[], Frame, OrbitType, PositionAngle)
     * )}
     * </p>
     */
    @Test
    public void testAdditionalParametersArrayException() {

        // Test data
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_18X18,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_18X18);

        // Try to build a multi orbital covariance with 2 orbits and 3 elements in its additional
        // parameters array
        try {
            new MultiOrbitalCovariance(covariance, ORBITS, new int[3], frame, orbitType,
                    positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Invalid additional parameters count array: its length of "
                    + "does not match the number of orbits (3 != 2)", e.getMessage());
        }
        try {
            new MultiOrbitalCovariance(COVARIANCE_MATRIX_18X18, ORBITS, new int[3], frame,
                    orbitType, positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Invalid additional parameters count array: its length of "
                    + "does not match the number of orbits (3 != 2)", e.getMessage());
        }

        // Try to build a multi orbital covariance with a negative elements in its additional
        // parameters array
        try {
            new MultiOrbitalCovariance(covariance, ORBITS, new int[] { 3, -1 }, frame, orbitType,
                    positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Negative additional parameters count: "
                    + "-1 was supplied for orbit number {1}", e.getMessage());
        }
        try {
            new MultiOrbitalCovariance(COVARIANCE_MATRIX_18X18, ORBITS, new int[] { 3, -1 }, frame,
                    orbitType, positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Negative additional parameters count: "
                    + "-1 was supplied for orbit number {1}", e.getMessage());
        }

        // Total size described by the additional parameters array not compatible with the
        // covariance size
        try {
            new MultiOrbitalCovariance(covariance, ORBITS, new int[] { 3, 4 }, frame, orbitType,
                    positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Invalid additional parameters count array: "
                    + "total count is inconsistent with the size of the covariance "
                    + "matrix (19 != 18)", e.getMessage());
        }
        try {
            new MultiOrbitalCovariance(COVARIANCE_MATRIX_18X18, ORBITS, new int[] { 3, 4 }, frame,
                    orbitType, positionAngle);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Invalid additional parameters count array: "
                    + "total count is inconsistent with the size of the covariance "
                    + "matrix (19 != 18)", e.getMessage());
        }
    }

    /**
     * Tests the method that copies a multi orbital covariance.
     *
     * <p>
     * Tested method:<br>
     * {@linkplain MultiOrbitalCovariance#copy()}
     * </p>
     */
    @Test
    public void testCopy() {

        // Frame
        final Frame eme2000 = FramesFactory.getEME2000();

        // Covariance
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);

        // Number of additional parameters for each orbit
        final int[] nbAdditionalParameters = new int[] { 0, 0 };

        // Test data
        final MultiOrbitalCovariance multiOrbitalCovariance = new MultiOrbitalCovariance(
                covariance, ORBITS, nbAdditionalParameters, eme2000, OrbitType.CARTESIAN,
                PositionAngle.TRUE);

        // Check the method
        final MultiOrbitalCovariance copy = multiOrbitalCovariance.copy();
        checkEquality(multiOrbitalCovariance, copy, 0., 0.);
        Assert.assertFalse(copy == multiOrbitalCovariance);
    }

    /**
     * @description Tests the equals and hashCode methods.
     *
     * @testedMethod {@link MultiOrbitalCovariance#equals(Object)}
     * @testedMethod {@link MultiOrbitalCovariance#hashCode()}
     *
     * @testPassCriteria The methods behaves as expected.
     */
    @Test
    public void testEqualsAndHashCode() {
        Covariance covariance;
        MultiOrbitalCovariance other;

        // Test data
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance initialCovariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance instance = new MultiOrbitalCovariance(initialCovariance,
                ORBITS, nbAdditionalParameters, frame, orbitType, positionAngle);

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
        other = new MultiOrbitalCovariance(initialCovariance, ORBITS, nbAdditionalParameters,
                frame, orbitType, positionAngle);
        Assert.assertTrue(instance.equals(other));
        Assert.assertTrue(other.equals(instance));
        Assert.assertTrue(instance.hashCode() == other.hashCode());

        // Different covariance matrix
        final ArrayRowSymmetricPositiveMatrix matrix = new ArrayRowSymmetricPositiveMatrix(12);
        covariance = new Covariance(matrix, DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        other = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, frame,
                orbitType, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different parameter descriptors
        final List<ParameterDescriptor> parameterDescriptors = copy(DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        parameterDescriptors.get(0).replaceField(StandardFieldDescriptors.PARAMETER_NAME, "custom");
        covariance = new Covariance(COVARIANCE_MATRIX_12X12, parameterDescriptors);
        other = new MultiOrbitalCovariance(covariance, ORBITS, nbAdditionalParameters, frame,
                orbitType, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different orbit
        List<Orbit> orbitsCustom = new ArrayList<>();
        orbitsCustom.add(ORBITS.get(0));
        orbitsCustom.add(ORBITS.get(0));
        other = new MultiOrbitalCovariance(initialCovariance, orbitsCustom, nbAdditionalParameters,
                frame, orbitType, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different frame
        other = new MultiOrbitalCovariance(initialCovariance, ORBITS, nbAdditionalParameters,
                FramesFactory.getEME2000(), orbitType, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different orbit type
        covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_KEPLERIAN_PARAMETER_DESCRIPTORS_12X12_TRUE_ANOMALY);
        orbitsCustom = new ArrayList<>();
        orbitsCustom.add(new KeplerianOrbit(ORBITS.get(0)));
        orbitsCustom.add(new KeplerianOrbit(ORBITS.get(1)));
        other = new MultiOrbitalCovariance(covariance, orbitsCustom, nbAdditionalParameters, frame,
                OrbitType.KEPLERIAN, positionAngle);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());

        // Different position angle type
        other = new MultiOrbitalCovariance(initialCovariance, ORBITS, nbAdditionalParameters,
                frame, orbitType, PositionAngle.ECCENTRIC);
        Assert.assertFalse(instance.equals(other));
        Assert.assertFalse(other.equals(instance));
        Assert.assertFalse(instance.hashCode() == other.hashCode());
    }

    /**
     * Tests the method that returns a string representation of the multi orbital covariance.
     *
     * <p>
     * Tested methods:<br>
     * {@linkplain MultiOrbitalCovariance#toString()}<br>
     * {@linkplain MultiOrbitalCovariance#toString(RealMatrixFormat)}<br>
     * {@linkplain MultiOrbitalCovariance#toString(RealMatrixFormat, TimeScale)}<br>
     * {@linkplain MultiOrbitalCovariance#toString(RealMatrixFormat, TimeScale, String, String, boolean, boolean)}
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
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final int[] nbAdditionalParameters = new int[] { 0, 0 };
        final Covariance covariance = new Covariance(COVARIANCE_MATRIX_12X12,
                DEFAULT_CARTESIAN_PARAMETER_DESCRIPTORS_12X12);
        final MultiOrbitalCovariance multiOrbitalCovariance = new MultiOrbitalCovariance(
                covariance, ORBITS, nbAdditionalParameters, frame, orbitType, positionAngle);

        // Time scales
        final TimeScale utc = TimeScalesFactory.getUTC();

        // Default format
        result = multiOrbitalCovariance.toString();
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0_X, p1_Y, p2_Z, p3_VX, p4_VY, p5_VZ, p6_X, p7_Y, p8_Z, p9_VX, p10_VY, p11_VZ]");
        Assert.assertEquals(builder.toString(), result);

        // Null format
        format = null;
        result = multiOrbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0_X, p1_Y, p2_Z, p3_VX, p4_VY, p5_VZ, p6_X, p7_Y, p8_Z, p9_VX, p10_VY, p11_VZ]");
        Assert.assertEquals(builder.toString(), result);

        // Null format (UTC time scale)
        format = null;
        result = multiOrbitalCovariance.toString(format, utc);
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:28:32.080 UTC; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0_X, p1_Y, p2_Z, p3_VX, p4_VY, p5_VZ, p6_X, p7_Y, p8_Z, p9_VX, p10_VY, p11_VZ]");
        Assert.assertEquals(builder.toString(), result);

        // Java format
        format = MatrixUtils.JAVA_FORMAT;
        result = multiOrbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0_X, p1_Y, p2_Z, p3_VX, p4_VY, p5_VZ, p6_X, p7_Y, p8_Z, p9_VX, p10_VY, p11_VZ;");
        builder.append(System.lineSeparator());
        builder.append("                      ");
        builder.append("{{259.86, 136.45, 110.17, 113.8, 168.13, 146.05, 117.44, 177.67, 122.62, 85.204, 38.256, 97.704}, "
                + "{136.45, 245.65, 162.38, 168.3, 213.88, 142.47, 151.86, 144.55, 120.5, 156.87, 54.902, 99.309}, "
                + "{110.17, 162.38, 201.05, 157.6, 175.45, 151.72, 92.051, 72.361, 168.31, 127.19, 20.749, 146.5}, "
                + "{113.8, 168.3, 157.6, 244.84, 206.5, 138.34, 162.25, 126.17, 228.24, 115.13, 91.438, 156.41}, "
                + "{168.13, 213.88, 175.45, 206.5, 352.42, 163.31, 171.79, 154.16, 223.26, 117.69, 73.916, 182.56}, "
                + "{146.05, 142.47, 151.72, 138.34, 163.31, 280.27, 67.276, 195.39, 186.87, 86.425, -32.051, 153.29}, "
                + "{117.44, 151.86, 92.051, 162.25, 171.79, 67.276, 208.05, 93.774, 123.41, 107.1, 101.11, 74.419}, "
                + "{177.67, 144.55, 72.361, 126.17, 154.16, 195.39, 93.774, 250.08, 129.29, 79.81, 24.577, 93.665}, "
                + "{122.62, 120.5, 168.31, 228.24, 223.26, 186.87, 123.41, 129.29, 292.39, 89.817, 70.426, 230.67}, "
                + "{85.204, 156.87, 127.19, 115.13, 117.69, 86.425, 107.1, 79.81, 89.817, 190.27, 43.504, 76.69}, "
                + "{38.256, 54.902, 20.749, 91.438, 73.916, -32.051, 101.11, 24.577, 70.426, 43.504, 99.617, 70.746}, "
                + "{97.704, 99.309, 146.5, 156.41, 182.56, 153.29, 74.419, 93.665, 230.67, 76.69, 70.746, 311.96}}]");
        Assert.assertEquals(builder.toString(), result);

        // Octave format
        format = MatrixUtils.OCTAVE_FORMAT;
        result = multiOrbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0_X, p1_Y, p2_Z, p3_VX, p4_VY, p5_VZ, p6_X, p7_Y, p8_Z, p9_VX, p10_VY, p11_VZ;");
        builder.append(System.lineSeparator());
        builder.append("                      ");
        builder.append("[259.86, 136.45, 110.17, 113.8, 168.13, 146.05, 117.44, 177.67, 122.62, 85.204, 38.256, 97.704; "
                + "136.45, 245.65, 162.38, 168.3, 213.88, 142.47, 151.86, 144.55, 120.5, 156.87, 54.902, 99.309; "
                + "110.17, 162.38, 201.05, 157.6, 175.45, 151.72, 92.051, 72.361, 168.31, 127.19, 20.749, 146.5; "
                + "113.8, 168.3, 157.6, 244.84, 206.5, 138.34, 162.25, 126.17, 228.24, 115.13, 91.438, 156.41; "
                + "168.13, 213.88, 175.45, 206.5, 352.42, 163.31, 171.79, 154.16, 223.26, 117.69, 73.916, 182.56; "
                + "146.05, 142.47, 151.72, 138.34, 163.31, 280.27, 67.276, 195.39, 186.87, 86.425, -32.051, 153.29; "
                + "117.44, 151.86, 92.051, 162.25, 171.79, 67.276, 208.05, 93.774, 123.41, 107.1, 101.11, 74.419; "
                + "177.67, 144.55, 72.361, 126.17, 154.16, 195.39, 93.774, 250.08, 129.29, 79.81, 24.577, 93.665; "
                + "122.62, 120.5, 168.31, 228.24, 223.26, 186.87, 123.41, 129.29, 292.39, 89.817, 70.426, 230.67; "
                + "85.204, 156.87, 127.19, 115.13, 117.69, 86.425, 107.1, 79.81, 89.817, 190.27, 43.504, 76.69; "
                + "38.256, 54.902, 20.749, 91.438, 73.916, -32.051, 101.11, 24.577, 70.426, 43.504, 99.617, 70.746; "
                + "97.704, 99.309, 146.5, 156.41, 182.56, 153.29, 74.419, 93.665, 230.67, 76.69, 70.746, 311.96]]");
        Assert.assertEquals(builder.toString(), result);

        // Scilab format
        format = MatrixUtils.SCILAB_FORMAT;
        result = multiOrbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0_X, p1_Y, p2_Z, p3_VX, p4_VY, p5_VZ, p6_X, p7_Y, p8_Z, p9_VX, p10_VY, p11_VZ;");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[259.86, 136.45, 110.17, 113.8, 168.13, 146.05, 117.44, 177.67, 122.62, 85.204, 38.256, 97.704; "
                + "136.45, 245.65, 162.38, 168.3, 213.88, 142.47, 151.86, 144.55, 120.5, 156.87, 54.902, 99.309; "
                + "110.17, 162.38, 201.05, 157.6, 175.45, 151.72, 92.051, 72.361, 168.31, 127.19, 20.749, 146.5; "
                + "113.8, 168.3, 157.6, 244.84, 206.5, 138.34, 162.25, 126.17, 228.24, 115.13, 91.438, 156.41; "
                + "168.13, 213.88, 175.45, 206.5, 352.42, 163.31, 171.79, 154.16, 223.26, 117.69, 73.916, 182.56; "
                + "146.05, 142.47, 151.72, 138.34, 163.31, 280.27, 67.276, 195.39, 186.87, 86.425, -32.051, 153.29; "
                + "117.44, 151.86, 92.051, 162.25, 171.79, 67.276, 208.05, 93.774, 123.41, 107.1, 101.11, 74.419; "
                + "177.67, 144.55, 72.361, 126.17, 154.16, 195.39, 93.774, 250.08, 129.29, 79.81, 24.577, 93.665; "
                + "122.62, 120.5, 168.31, 228.24, 223.26, 186.87, 123.41, 129.29, 292.39, 89.817, 70.426, 230.67; "
                + "85.204, 156.87, 127.19, 115.13, 117.69, 86.425, 107.1, 79.81, 89.817, 190.27, 43.504, 76.69; "
                + "38.256, 54.902, 20.749, 91.438, 73.916, -32.051, 101.11, 24.577, 70.426, 43.504, 99.617, 70.746; "
                + "97.704, 99.309, 146.5, 156.41, 182.56, 153.29, 74.419, 93.665, 230.67, 76.69, 70.746, 311.96]]");
        Assert.assertEquals(builder.toString(), result);

        // Visual format
        format = MatrixUtils.VISUAL_FORMAT;
        result = multiOrbitalCovariance.toString(format);
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:29:09.080 TAI; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0_X, p1_Y, p2_Z, p3_VX, p4_VY, p5_VZ, p6_X, p7_Y, p8_Z, p9_VX, p10_VY, p11_VZ;");
        builder.append(System.lineSeparator());
        builder.append("                      ");
        builder.append("[[      259.86,      136.45,      110.17,      113.80,      168.13,      146.05,      117.44,      177.67,      122.62,      85.204,      38.256,      97.704]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      136.45,      245.65,      162.38,      168.30,      213.88,      142.47,      151.86,      144.55,      120.50,      156.87,      54.902,      99.309]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      110.17,      162.38,      201.05,      157.60,      175.45,      151.72,      92.051,      72.361,      168.31,      127.19,      20.749,      146.50]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      113.80,      168.30,      157.60,      244.84,      206.50,      138.34,      162.25,      126.17,      228.24,      115.13,      91.438,      156.41]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      168.13,      213.88,      175.45,      206.50,      352.42,      163.31,      171.79,      154.16,      223.26,      117.69,      73.916,      182.56]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      146.05,      142.47,      151.72,      138.34,      163.31,      280.27,      67.276,      195.39,      186.87,      86.425,     -32.051,      153.29]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      117.44,      151.86,      92.051,      162.25,      171.79,      67.276,      208.05,      93.774,      123.41,      107.10,      101.11,      74.419]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      177.67,      144.55,      72.361,      126.17,      154.16,      195.39,      93.774,      250.08,      129.29,      79.810,      24.577,      93.665]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      122.62,      120.50,      168.31,      228.24,      223.26,      186.87,      123.41,      129.29,      292.39,      89.817,      70.426,      230.67]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      85.204,      156.87,      127.19,      115.13,      117.69,      86.425,      107.10,      79.810,      89.817,      190.27,      43.504,      76.690]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      38.256,      54.902,      20.749,      91.438,      73.916,     -32.051,      101.11,      24.577,      70.426,      43.504,      99.617,      70.746]");
        builder.append(System.lineSeparator());
        builder.append("                       ");
        builder.append("[      97.704,      99.309,      146.50,      156.41,      182.56,      153.29,      74.419,      93.665,      230.67,      76.690,      70.746,      311.96]]]");
        Assert.assertEquals(builder.toString(), result);

        // Visual format (no class name)
        format = MatrixUtils.VISUAL_FORMAT;
        result = multiOrbitalCovariance.toString(format, utc, Covariance.DEFAULT_NAME_SEPARATOR,
                Covariance.DEFAULT_FIELD_SEPARATOR, false, false);
        builder = new StringBuilder();

        builder.append("2017-11-07T23:28:32.080 UTC; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0_X, p1_Y, p2_Z, p3_VX, p4_VY, p5_VZ, p6_X, p7_Y, p8_Z, p9_VX, p10_VY, p11_VZ;");
        builder.append(System.lineSeparator());
        builder.append("[[      259.86,      136.45,      110.17,      113.80,      168.13,      146.05,      117.44,      177.67,      122.62,      85.204,      38.256,      97.704]");
        builder.append(System.lineSeparator());
        builder.append(" [      136.45,      245.65,      162.38,      168.30,      213.88,      142.47,      151.86,      144.55,      120.50,      156.87,      54.902,      99.309]");
        builder.append(System.lineSeparator());
        builder.append(" [      110.17,      162.38,      201.05,      157.60,      175.45,      151.72,      92.051,      72.361,      168.31,      127.19,      20.749,      146.50]");
        builder.append(System.lineSeparator());
        builder.append(" [      113.80,      168.30,      157.60,      244.84,      206.50,      138.34,      162.25,      126.17,      228.24,      115.13,      91.438,      156.41]");
        builder.append(System.lineSeparator());
        builder.append(" [      168.13,      213.88,      175.45,      206.50,      352.42,      163.31,      171.79,      154.16,      223.26,      117.69,      73.916,      182.56]");
        builder.append(System.lineSeparator());
        builder.append(" [      146.05,      142.47,      151.72,      138.34,      163.31,      280.27,      67.276,      195.39,      186.87,      86.425,     -32.051,      153.29]");
        builder.append(System.lineSeparator());
        builder.append(" [      117.44,      151.86,      92.051,      162.25,      171.79,      67.276,      208.05,      93.774,      123.41,      107.10,      101.11,      74.419]");
        builder.append(System.lineSeparator());
        builder.append(" [      177.67,      144.55,      72.361,      126.17,      154.16,      195.39,      93.774,      250.08,      129.29,      79.810,      24.577,      93.665]");
        builder.append(System.lineSeparator());
        builder.append(" [      122.62,      120.50,      168.31,      228.24,      223.26,      186.87,      123.41,      129.29,      292.39,      89.817,      70.426,      230.67]");
        builder.append(System.lineSeparator());
        builder.append(" [      85.204,      156.87,      127.19,      115.13,      117.69,      86.425,      107.10,      79.810,      89.817,      190.27,      43.504,      76.690]");
        builder.append(System.lineSeparator());
        builder.append(" [      38.256,      54.902,      20.749,      91.438,      73.916,     -32.051,      101.11,      24.577,      70.426,      43.504,      99.617,      70.746]");
        builder.append(System.lineSeparator());
        builder.append(" [      97.704,      99.309,      146.50,      156.41,      182.56,      153.29,      74.419,      93.665,      230.67,      76.690,      70.746,      311.96]]");
        Assert.assertEquals(builder.toString(), result);

        // Java format (reverse field descriptors order)
        format = MatrixUtils.JAVA_FORMAT;
        result = multiOrbitalCovariance.toString(format, utc, Covariance.DEFAULT_NAME_SEPARATOR,
                Covariance.DEFAULT_FIELD_SEPARATOR, true, true);
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:28:32.080 UTC; GCRF; CARTESIAN; TRUE; "
                + "Parameters: X_p0, Y_p1, Z_p2, VX_p3, VY_p4, VZ_p5, X_p6, Y_p7, Z_p8, VX_p9, VY_p10, VZ_p11;");
        builder.append(System.lineSeparator());
        builder.append("                      ");
        builder.append("{{259.86, 136.45, 110.17, 113.8, 168.13, 146.05, 117.44, 177.67, 122.62, 85.204, 38.256, 97.704}, "
                + "{136.45, 245.65, 162.38, 168.3, 213.88, 142.47, 151.86, 144.55, 120.5, 156.87, 54.902, 99.309}, "
                + "{110.17, 162.38, 201.05, 157.6, 175.45, 151.72, 92.051, 72.361, 168.31, 127.19, 20.749, 146.5}, "
                + "{113.8, 168.3, 157.6, 244.84, 206.5, 138.34, 162.25, 126.17, 228.24, 115.13, 91.438, 156.41}, "
                + "{168.13, 213.88, 175.45, 206.5, 352.42, 163.31, 171.79, 154.16, 223.26, 117.69, 73.916, 182.56}, "
                + "{146.05, 142.47, 151.72, 138.34, 163.31, 280.27, 67.276, 195.39, 186.87, 86.425, -32.051, 153.29}, "
                + "{117.44, 151.86, 92.051, 162.25, 171.79, 67.276, 208.05, 93.774, 123.41, 107.1, 101.11, 74.419}, "
                + "{177.67, 144.55, 72.361, 126.17, 154.16, 195.39, 93.774, 250.08, 129.29, 79.81, 24.577, 93.665}, "
                + "{122.62, 120.5, 168.31, 228.24, 223.26, 186.87, 123.41, 129.29, 292.39, 89.817, 70.426, 230.67}, "
                + "{85.204, 156.87, 127.19, 115.13, 117.69, 86.425, 107.1, 79.81, 89.817, 190.27, 43.504, 76.69}, "
                + "{38.256, 54.902, 20.749, 91.438, 73.916, -32.051, 101.11, 24.577, 70.426, 43.504, 99.617, 70.746}, "
                + "{97.704, 99.309, 146.5, 156.41, 182.56, 153.29, 74.419, 93.665, 230.67, 76.69, 70.746, 311.96}}]");
        Assert.assertEquals(builder.toString(), result);

        // Java format (custom name and field separator)
        format = MatrixUtils.JAVA_FORMAT;
        result = multiOrbitalCovariance.toString(format, utc, " | ", ".", true, false);
        builder = new StringBuilder();
        builder.append("MultiOrbitalCovariance[2017-11-07T23:28:32.080 UTC; GCRF; CARTESIAN; TRUE; "
                + "Parameters: p0.X | p1.Y | p2.Z | p3.VX | p4.VY | p5.VZ | p6.X | p7.Y | p8.Z | p9.VX | p10.VY | p11.VZ;");
        builder.append(System.lineSeparator());
        builder.append("                      ");
        builder.append("{{259.86, 136.45, 110.17, 113.8, 168.13, 146.05, 117.44, 177.67, 122.62, 85.204, 38.256, 97.704}, "
                + "{136.45, 245.65, 162.38, 168.3, 213.88, 142.47, 151.86, 144.55, 120.5, 156.87, 54.902, 99.309}, "
                + "{110.17, 162.38, 201.05, 157.6, 175.45, 151.72, 92.051, 72.361, 168.31, 127.19, 20.749, 146.5}, "
                + "{113.8, 168.3, 157.6, 244.84, 206.5, 138.34, 162.25, 126.17, 228.24, 115.13, 91.438, 156.41}, "
                + "{168.13, 213.88, 175.45, 206.5, 352.42, 163.31, 171.79, 154.16, 223.26, 117.69, 73.916, 182.56}, "
                + "{146.05, 142.47, 151.72, 138.34, 163.31, 280.27, 67.276, 195.39, 186.87, 86.425, -32.051, 153.29}, "
                + "{117.44, 151.86, 92.051, 162.25, 171.79, 67.276, 208.05, 93.774, 123.41, 107.1, 101.11, 74.419}, "
                + "{177.67, 144.55, 72.361, 126.17, 154.16, 195.39, 93.774, 250.08, 129.29, 79.81, 24.577, 93.665}, "
                + "{122.62, 120.5, 168.31, 228.24, 223.26, 186.87, 123.41, 129.29, 292.39, 89.817, 70.426, 230.67}, "
                + "{85.204, 156.87, 127.19, 115.13, 117.69, 86.425, 107.1, 79.81, 89.817, 190.27, 43.504, 76.69}, "
                + "{38.256, 54.902, 20.749, 91.438, 73.916, -32.051, 101.11, 24.577, 70.426, 43.504, 99.617, 70.746}, "
                + "{97.704, 99.309, 146.5, 156.41, 182.56, 153.29, 74.419, 93.665, 230.67, 76.69, 70.746, 311.96}}]");
        Assert.assertEquals(builder.toString(), result);
    }

    /**
     * Asserts the equality between two multi orbital covariances within the specified tolerance.
     *
     * @param expected
     *        the expected multi orbital covariance
     * @param actual
     *        the multi orbital covariance to be checked
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    private static void checkEquality(final MultiOrbitalCovariance expected,
            final MultiOrbitalCovariance actual, final double absTol, final double relTol) {
        checkEquality(expected, actual, absTol, relTol, true);
    }

    /**
     * Asserts the equality between two multi orbital covariances within the specified tolerance.
     *
     * @param expected
     *        the expected multi orbital covariance
     * @param actual
     *        the multi orbital covariance to be checked
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     * @param checkFrameSameInstance
     *        indicate if the frame in each multi orbital covariance should be the same instance or
     *        if only the frame's name and parent's name should be check
     */
    private static void checkEquality(final MultiOrbitalCovariance expected,
            final MultiOrbitalCovariance actual, final double absTol, final double relTol,
            final boolean checkFrameSameInstance) {
        if (expected == null ^ actual == null) {
            Assert.assertEquals(expected, actual);
        } else if (expected != actual) {
            CheckUtils.checkEquality(expected.getCovarianceMatrix(), actual.getCovarianceMatrix(),
                    absTol, relTol);
            CheckUtils.checkEquality(expected.getParameterDescriptors(),
                    actual.getParameterDescriptors());
            Assert.assertEquals(expected.getOrbits().size(), actual.getOrbits().size());
            for (int i = 0; i < expected.getOrbits().size(); i++) {
                Assert.assertTrue(expected.getOrbits().get(i).equals(actual.getOrbits().get(i)));
            }
            Assert.assertEquals(expected.getDate(), actual.getDate());
            if (checkFrameSameInstance) {
                Assert.assertEquals(expected.getFrame(), actual.getFrame());
            } else {
                Assert.assertEquals(expected.getFrame().getName(), actual.getFrame().getName());
                Assert.assertEquals(expected.getFrame().getParent().getName(), actual.getFrame()
                        .getParent().getName());
            }
            Assert.assertEquals(expected.getOrbitType(), actual.getOrbitType());
            Assert.assertEquals(expected.getPositionAngle(), actual.getPositionAngle());
        }
    }

    /**
     * Asserts the equality between a multi orbital covariances (expected to be defined on 2 orbits)
     * and two associated orbital covariances within the specified tolerance.
     * 
     * @param result
     *        the multi orbital covariance to evaluate
     * @param covariance1
     *        the first orbital covariance to check against the multi orbital covariance
     * @param covariance2
     *        the second orbital covariance to check against the multi orbital covariance
     * @param indices1
     *        the rows/columns indices of the first orbital covariance in the multi orbital
     *        covariance
     * @param indices2
     *        the rows/columns indices of the second orbital covariance in the multi orbital
     *        covariance
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     */
    private static void checkMultiOrbCovVSOrbCov(final MultiOrbitalCovariance result,
            final OrbitalCovariance covariance1, final OrbitalCovariance covariance2,
            final int[] indices1, final int[] indices2, final double absTol, final double relTol) {
        checkMultiOrbCovVSOrbCov(result, covariance1, covariance2, indices1, indices2, absTol,
                relTol, true);
    }

    /**
     * Asserts the equality between a multi orbital covariances (expected to be defined on 2 orbits)
     * and two associated orbital covariances within the specified tolerance.
     * 
     * @param result
     *        the multi orbital covariance to evaluate
     * @param covariance1
     *        the first orbital covariance to check against the multi orbital covariance
     * @param covariance2
     *        the second orbital covariance to check against the multi orbital covariance
     * @param indices1
     *        the rows/columns indices of the first orbital covariance in the multi orbital
     *        covariance
     * @param indices2
     *        the rows/columns indices of the second orbital covariance in the multi orbital
     *        covariance
     * @param absTol
     *        the absolute tolerance
     * @param relTol
     *        the relative tolerance
     * @param checkFrameSameInstance
     *        indicate if the frame in each multi orbital covariance should be the same instance or
     *        if only the frame's name and parent's name should be check
     */
    private static void checkMultiOrbCovVSOrbCov(final MultiOrbitalCovariance result,
            final OrbitalCovariance covariance1, final OrbitalCovariance covariance2,
            final int[] indices1, final int[] indices2, final double absTol, final double relTol,
            final boolean checkFrameSameInstance) {
        if (result == null || covariance1 == null || covariance1 == covariance2) {
            Assert.fail();
        } else {
            CheckUtils.checkEquality(result.getCovarianceMatrix().getSubMatrix(indices1),
                    covariance1.getCovarianceMatrix(), absTol, relTol);
            CheckUtils.checkEquality(result.getCovarianceMatrix().getSubMatrix(indices2),
                    covariance2.getCovarianceMatrix(), absTol, relTol);
            CheckUtils.checkEquality(
                    result.getParameterDescriptors().subList(indices1[0],
                            indices1[indices1.length - 1] + 1),
                    covariance1.getParameterDescriptors());
            CheckUtils.checkEquality(
                    result.getParameterDescriptors().subList(indices2[0],
                            indices2[indices2.length - 1] + 1),
                    covariance2.getParameterDescriptors());
            Assert.assertTrue(result.getOrbits().get(0).equals(covariance1.getOrbit()));
            Assert.assertTrue(result.getOrbits().get(1).equals(covariance2.getOrbit()));
            Assert.assertEquals(result.getDate(), covariance1.getDate());
            Assert.assertEquals(result.getDate(), covariance2.getDate());
            if (checkFrameSameInstance) {
                Assert.assertEquals(result.getFrame(), covariance1.getFrame());
                Assert.assertEquals(result.getFrame(), covariance2.getFrame());
            } else {
                Assert.assertEquals(result.getFrame().getName(), covariance1.getFrame().getName());
                Assert.assertEquals(result.getFrame().getName(), covariance2.getFrame().getName());
                Assert.assertEquals(result.getFrame().getParent().getName(), covariance1.getFrame()
                        .getParent().getName());
                Assert.assertEquals(result.getFrame().getParent().getName(), covariance2.getFrame()
                        .getParent().getName());
            }
            Assert.assertEquals(result.getOrbitType(), covariance1.getOrbitType());
            Assert.assertEquals(result.getOrbitType(), covariance2.getOrbitType());
            Assert.assertEquals(result.getPositionAngle(), covariance1.getPositionAngle());
            Assert.assertEquals(result.getPositionAngle(), covariance2.getPositionAngle());
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
}
