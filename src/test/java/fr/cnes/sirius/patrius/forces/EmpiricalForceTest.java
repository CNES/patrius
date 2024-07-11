/**
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
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:133:18/11/2013:Javadoc improved
 * VERSION::FA:93:01/04/2014:changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:412:04/05/2015:changed partial derivatives formulas
 * VERSION::FA:440:06/06/2015:LOF type in partial derivatives
 * VERSION::FA:500:22/09/2015:New management of frames in acceleration computation
 * VERSION::FA:449:22/12/2015:Coverage test for new attitude handling
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.LinearFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the coverage of the class EmpiricalForce.
 * The validation tests are located in pbase-tools.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class EmpiricalForceTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Empirical force model.
         * 
         * @featureDescription Perform simple tests for coverage, the validation tests are located
         *                     in pbase-tools.
         * 
         * @coveredRequirements DV-MOD_160, DV-MOD_340
         */
        EMPIRICAL_FORCE_VALIDATION
    }

    /** The orbit used for the tests. */
    private Orbit orbit;

    /** The state used for the tests. */
    private SpacecraftState state;

    /** The CIRF2000 frame. */
    private Frame cirf;

    /** The pv coordinates n.1. */
    private PVCoordinates pv1;

    /** The pv coordinates n.2. */
    private PVCoordinates pv2;

    /** Empirical force model for N=1. */
    private EmpiricalForce forcesN1;

    /** Empirical force model for N=4. */
    private EmpiricalForce forcesN4;

    /** The reference direction for the empirical forces model N=4. */
    private Vector3D ref;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(EmpiricalForceTest.class.getSimpleName(), "Empirical force");
    }

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_MODEL}
     * 
     * @testedMethod {@link DragForce#getParameters()}
     * @testedMethod {@link DragForce#getParameter(String)}
     * @testedMethod {@link DragForce#setParameter()}
     * 
     * @description Test for the AeroModel parameters
     * 
     * @input nothing
     * 
     * @output the parameter list
     * 
     * @testPassCriteria there are 9 expected parameters
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testParamLists() throws PatriusException, IOException, ParseException {

        Assert.assertEquals(9, this.forcesN1.getParameters().size());
        Assert.assertFalse(this.forcesN1.supportsJacobianParameter(new Parameter("toto", 0.)));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @testedMethod {@link EmpiricalForce#EmpiricalForce(int, Vector3D, Vector3D, Vector3D, Vector3D, Frame)}
     * @testedMethod {@link EmpiricalForce#getLocalFrame()}
     * @testedMethod {@link EmpiricalForce#getEventsDetectors()}
     * 
     * @description test all the getters/setters of the class parameters
     * 
     * @input None
     * 
     * @output the output of the getParameter() method.
     * 
     * @testPassCriteria the output values are the expected ones.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testGetters() throws PatriusException {
        Assert.assertEquals(0, this.forcesN1.getEventsDetectors().length);
        Assert.assertNotSame(this.cirf, this.forcesN1.getLocalFrame());
        Assert.assertEquals(Vector3D.PLUS_K, this.forcesN1.getVectorS());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @testedMethod {@link EmpiricalForce#EmpiricalForce(int, Vector3D, Vector3D, Vector3D, Vector3D, Frame)}
     * @testedMethod {@link EmpiricalForce#EmpiricalForce(int, Vector3D, Vector3D, Vector3D, Vector3D, LOFType)}
     * @testedMethod {@link EmpiricalForce#computeCosSin(PVCoordinates, Vector3D)}
     * @testedMethod {@link EmpiricalForce#computeAcceleration(SpacecraftState)}
     * @testedMethod {@link EmpiricalForce#computeAcceleration(PVCoordinates, LocalOrbitalFrame, Vector3D, Frame, AbsoluteDate)}
     * @testedMethod {@link EmpiricalForce#addContribution(SpacecraftState, TimeDerivativesEquations)}
     * 
     * @description test the construction and the methods to compute the acceleration, without
     *              performing
     *              a propagation.
     * 
     * @input the empirical force models constructor parameters
     * 
     * @output two empirical force models and the output acceleration for some given parameters.
     * 
     * @testPassCriteria the output values of the methods to compute the cos/sin and the
     *                   acceleration
     *                   are the expected ones (OBELIX reference value).
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testComputeAcceleration() throws PatriusException {

        Report.printMethodHeader("testComputeAcceleration", "Acceleration computation", "Obelix",
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // test the cos(wnt) sin(wnt) computation for n = 1:
        final double[] actual = this.forcesN1.computeCosSin(this.pv1, Vector3D.PLUS_K);
        // comparison with OBELIX reference values:
        Assert.assertEquals(9.7412330135969649E-01, actual[0], 0.0);
        Assert.assertEquals(-2.2601724214777510E-01, actual[1], 0.0);
        // test the acceleration computation for n = 1:
        final Vector3D acc = this.forcesN1.computeAcceleration(new SpacecraftState(this.orbit));
        // comparison with OBELIX reference values:
        final Vector3D expected = new Vector3D(-8.3684551034724576E-08, -4.5718199936855544E-07,
                4.6022167111835094E-07);
        Assert.assertEquals(expected.getX(), acc.getX(), UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expected.getY(), acc.getY(), UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(expected.getZ(), acc.getZ(), UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Acceleration", expected, acc);

        // test the acceleration computation with the computeAcceleration method used for
        // validation:
        final Vector3D acc2 = this.forcesN1.computeAcceleration(this.orbit.getPVCoordinates(),
                new LocalOrbitalFrame(this.cirf, LOFType.QSW, this.orbit, "QSW"), Vector3D.PLUS_K,
                this.cirf, this.state);
        // comparison with the acceleration computed using the previous method:
        Assert.assertEquals(-8.3684551034724576E-08, acc2.getX(),
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-4.5718199936855544E-07, acc2.getY(),
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(4.6022167111835094E-07, acc2.getZ(),
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        // test the cos(wnt) sin(wnt) computation for n = 4:
        final double[] actual2 = this.forcesN4.computeCosSin(this.pv2, this.ref);
        // comparison with OBELIX reference values:
        Assert.assertEquals(-8.6926211704609102E-01, actual2[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(-4.9435146593142520E-01, actual2[1],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        // test the addContribution method
        NumericalPropagator prop1 = new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0,
                30.0, 0, 1.0e-5));
        NumericalPropagator prop2 = new NumericalPropagator(new GraggBulirschStoerIntegrator(10.0,
                30.0, 0, 1.0e-5));

        prop1.addForceModel(this.forcesN1);
        prop1.setInitialState(new SpacecraftState(this.orbit));
        prop2.setInitialState(new SpacecraftState(this.orbit));
        prop1.propagate(this.orbit.getDate().shiftedBy(1000));
        prop2.propagate(this.orbit.getDate().shiftedBy(1000));
        PVCoordinates finalPV1 = prop1.getPVCoordinates(this.orbit.getDate().shiftedBy(100),
                this.cirf);
        PVCoordinates finalPV2 = prop2.getPVCoordinates(this.orbit.getDate().shiftedBy(100),
                this.cirf);
        // test the final state is different when applying the empirical forces:
        Assert.assertNotSame(finalPV1.getPosition().getX(), finalPV2.getPosition().getX());
        Assert.assertNotSame(finalPV1.getVelocity().getZ(), finalPV2.getVelocity().getZ());

        // check the constructor with the LOF type and the propagation:
        // direction used to compute n.omega in the expression F = a.cos(n.omega.t) +
        // b.sin(n.omega.t) + c
        final Vector3D referenceDirection = new Vector3D(1, 0, 0);
        // vector along S
        final Vector3D c = new Vector3D(0, 1, 0);
        final Vector3D b = Vector3D.ZERO;
        final Vector3D a = Vector3D.ZERO;
        final ForceModel empiricalForce = new EmpiricalForce(1, referenceDirection, a, b, c,
                LOFType.TNW);
        // propagator construction:
        final double[] absTOL = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTOL = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final DormandPrince853Integrator integ = new DormandPrince853Integrator(0.1, 500, absTOL,
                relTOL);
        prop1 = new NumericalPropagator(integ);
        prop2 = new NumericalPropagator(integ);
        prop1.setOrbitType(OrbitType.EQUINOCTIAL);
        prop1.setInitialState(new SpacecraftState(this.orbit));
        prop2.setOrbitType(OrbitType.EQUINOCTIAL);
        prop2.setInitialState(new SpacecraftState(this.orbit));
        prop1.addForceModel(empiricalForce);
        finalPV1 = prop1.propagate(this.orbit.getDate().shiftedBy(1000)).getPVCoordinates();
        finalPV2 = prop2.propagate(this.orbit.getDate().shiftedBy(1000)).getPVCoordinates();
        // test the final state is different when applying the empirical forces:
        Assert.assertNotSame(finalPV1.getPosition().getX(), finalPV2.getPosition().getX());
        Assert.assertNotSame(finalPV1.getVelocity().getZ(), finalPV2.getVelocity().getZ());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @testedMethod {@link EmpiricalForce#addDAccDParam(SpacecraftState, String, double[])}
     * @testedMethod {@link EmpiricalForce#addDAccDState(SpacecraftState, double[][], double[][], double[])}
     * 
     * @description test the methods to compute the partial derivatives
     * 
     * @input the empirical force models
     * 
     * @output the output partial derivatives
     * 
     * @testPassCriteria the output derivatives of the methods are the expected ones (OBELIX
     *                   reference value).
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testComputeDacc() throws PatriusException {
        // computes the partial derivatives:
        double[] dAccdParam = new double[3];
        final Parameter ax = new Parameter("ax", 1.E-7);
        final Parameter ay = new Parameter("ay", 2.E-7);
        final Parameter az = new Parameter("az", 3.E-7);
        final Parameter bx = new Parameter("bx", 1.E-7);
        final Parameter by = new Parameter("by", 2.E-7);
        final Parameter bz = new Parameter("bz", 3.E-7);
        final Parameter cx = new Parameter("cx", 1.E-7);
        final Parameter cy = new Parameter("cy", 2.E-7);
        final Parameter cz = new Parameter("cz", 3.E-7);

        this.forcesN1 = new EmpiricalForce(1, Vector3D.PLUS_K, ax, ay, az, bx, by, bz, cx, cy, cz,
                LOFType.QSW);

        // here Ax is a simple Parameter handle with his name.
        // if Ax is a IParamDiffFunction, to acces to all paramaters of Ax we should do :
        for (final Parameter p : this.forcesN1.getAx().getParameters()) {
            this.forcesN1.addDAccDParam(this.state, p, dAccdParam);
        }
        Assert.assertEquals(-2.4727824050067477E-02, dAccdParam[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), ay, dAccdParam);
        Assert.assertEquals(-8.7176276689142929E-01, dAccdParam[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), az, dAccdParam);
        Assert.assertEquals(4.3397513665124027E-01, dAccdParam[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), bx, dAccdParam);
        Assert.assertEquals(5.7373790241036053E-03, dAccdParam[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), by, dAccdParam);
        Assert.assertEquals(2.0226742970309022E-01, dAccdParam[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), bz, dAccdParam);
        Assert.assertEquals(-1.0069142521250378E-01, dAccdParam[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), cx, dAccdParam);
        Assert.assertEquals(4.9600203966277895E-01, dAccdParam[1],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), cy, dAccdParam);
        Assert.assertEquals(2.0138283932106085E-01, dAccdParam[2],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), cz, dAccdParam);
        Assert.assertEquals(4.5399048247396340E-01, dAccdParam[2],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        // computes the partial derivative with respect to state parameters:
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        // this method does nothing:
        this.forcesN1.addDAccDState(new SpacecraftState(this.orbit), dAccdPos, dAccdVel);
        Assert.assertEquals(0.0, dAccdPos[2][1], 0);
        Assert.assertEquals(0.0, dAccdVel[0][1], 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @testedMethod {@link EmpiricalForce#addDAccDParam(SpacecraftState, String, double[])}
     * @testedMethod {@link EmpiricalForce#addDAccDState(SpacecraftState, double[][], double[][], double[])}
     * 
     * @description test the methods to compute the partial derivatives
     * 
     * @input the empirical force models defined with Linear function
     * 
     * @output the output partial derivatives
     * 
     * @testPassCriteria the output derivatives of the methods are the expected ones (analytical
     *                   computed reference).
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testComputeDacc2() throws PatriusException {
        // computes the partial derivatives:
        double[] dAccdParam = new double[3];
        final double d = 10;
        final AbsoluteDate t0 = this.orbit.getDate().shiftedBy(-d);
        final LinearFunction ax = new LinearFunction(t0, 1, 11);
        final LinearFunction ay = new LinearFunction(t0, 2, 12);
        final LinearFunction az = new LinearFunction(t0, 3, 13);
        final LinearFunction bx = new LinearFunction(t0, 4, 14);
        final LinearFunction by = new LinearFunction(t0, 5, 15);
        final LinearFunction bz = new LinearFunction(t0, 6, 16);
        final LinearFunction cx = new LinearFunction(t0, 7, 17);
        final LinearFunction cy = new LinearFunction(t0, 8, 18);
        final LinearFunction cz = new LinearFunction(t0, 9, 19);

        final Vector3D s = Vector3D.PLUS_K;
        this.forcesN1 = new EmpiricalForce(1, s, ax, ay, az, bx, by, bz, cx, cy, cz, this.cirf);

        final double[] cosSin = this.forcesN1.computeCosSin(this.state.getPVCoordinates(), s);
        // here Ax is a simple Parameter handle with his name.
        // if Ax is a IParamDiffFunction, to acces to all paramaters of Ax we should do :
        for (final Parameter p : this.forcesN1.getAx().getParameters()) {
            this.forcesN1.addDAccDParam(this.state, p, dAccdParam);
        }
        Assert.assertEquals(cosSin[0] * d + cosSin[0], dAccdParam[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[1], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[2], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        for (final Parameter p : this.forcesN1.getAy().getParameters()) {
            this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), p, dAccdParam);
        }
        Assert.assertEquals(0., dAccdParam[0], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(cosSin[0] * d + cosSin[0], dAccdParam[1],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[2], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        for (final Parameter p : this.forcesN1.getAz().getParameters()) {
            this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), p, dAccdParam);
        }
        Assert.assertEquals(0., dAccdParam[0], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[1], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(cosSin[0] * d + cosSin[0], dAccdParam[2],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        for (final Parameter p : this.forcesN1.getBx().getParameters()) {
            this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), p, dAccdParam);
        }
        Assert.assertEquals(cosSin[1] * d + cosSin[1], dAccdParam[0],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[1], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[2], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        for (final Parameter p : this.forcesN1.getBy().getParameters()) {
            this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), p, dAccdParam);
        }
        Assert.assertEquals(0., dAccdParam[0], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(cosSin[1] * d + cosSin[1], dAccdParam[1],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[2], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        for (final Parameter p : this.forcesN1.getBz().getParameters()) {
            this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), p, dAccdParam);
        }
        Assert.assertEquals(0., dAccdParam[0], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[1], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(cosSin[1] * d + cosSin[1], dAccdParam[2],
                UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        for (final Parameter p : this.forcesN1.getCx().getParameters()) {
            this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), p, dAccdParam);
        }
        Assert.assertEquals(d + 1, dAccdParam[0], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[1], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[2], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        for (final Parameter p : this.forcesN1.getCy().getParameters()) {
            this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), p, dAccdParam);
        }
        Assert.assertEquals(0., dAccdParam[0], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(d + 1, dAccdParam[1], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[2], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);

        dAccdParam = new double[3];
        for (final Parameter p : this.forcesN1.getCz().getParameters()) {
            this.forcesN1.addDAccDParam(new SpacecraftState(this.orbit), p, dAccdParam);
        }
        Assert.assertEquals(0., dAccdParam[0], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0., dAccdParam[1], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(d + 1, dAccdParam[2], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @description test the methods to compute the partial derivatives when no attitude data is
     *              provided
     * 
     * @input the empirical force models
     * 
     * @output the output partial derivatives
     * 
     * @testPassCriteria an exception is thrown => no attitude provided
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testComputeDacc3() throws PatriusException {
        // computes the partial derivatives:
        double[] dAccdParam = new double[3];
        final Parameter ax = new Parameter("ax", 1.E-7);
        final Parameter ay = new Parameter("ay", 2.E-7);
        final Parameter az = new Parameter("az", 3.E-7);
        final Parameter bx = new Parameter("bx", 1.E-7);
        final Parameter by = new Parameter("by", 2.E-7);
        final Parameter bz = new Parameter("bz", 3.E-7);
        final Parameter cx = new Parameter("cx", 1.E-7);
        final Parameter cy = new Parameter("cy", 2.E-7);
        final Parameter cz = new Parameter("cz", 3.E-7);

        final Frame nullFrame = null;
        final EmpiricalForce forceNullFrame = new EmpiricalForce(1, Vector3D.PLUS_K, ax, ay, az,
                bx, by, bz, cx, cy, cz, nullFrame);

        // Check an exception is thrown when calling addDAccDParam (no attitude provider)
        boolean testOk = false;
        try {
            dAccdParam = new double[3];
            forceNullFrame.addDAccDParam(new SpacecraftState(this.orbit), ax, dAccdParam);
            Assert.fail();
        } catch (final PatriusException e1) {
            testOk = true;
        }
        Assert.assertTrue(testOk);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @description test the methods to compute the partial derivatives when attitude data is
     *              provided
     *              on a simple case
     * @input the empirical force models
     * 
     * @output the output partial derivatives
     * 
     * @testPassCriteria The partial derivatives are the one expected
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */

    @Test
    public final void testComputeDacc4() throws PatriusException {

        // Simple case : rotation is identity, rotation rate and acceleration are null vectors
        final Attitude attForces = new Attitude(AbsoluteDate.J2000_EPOCH, this.cirf,
                new AngularCoordinates());
        final SpacecraftState s = new SpacecraftState(this.orbit, attForces);
        final PVCoordinates pv = s.getPVCoordinates();

        // computes the partial derivatives:
        final double[] dAccdParam = new double[3];
        final Parameter ax = new Parameter("ax", 1.E-7);
        final Parameter ay = new Parameter("ay", 2.E-7);
        final Parameter az = new Parameter("az", 3.E-7);
        final Parameter bx = new Parameter("bx", 1.E-7);
        final Parameter by = new Parameter("by", 2.E-7);
        final Parameter bz = new Parameter("bz", 3.E-7);
        final Parameter cx = new Parameter("cx", 1.E-7);
        final Parameter cy = new Parameter("cy", 2.E-7);
        final Parameter cz = new Parameter("cz", 3.E-7);

        final Frame nullFrame = null;
        final EmpiricalForce forceNullFrame = new EmpiricalForce(1, Vector3D.PLUS_K, ax, ay, az,
                bx, by, bz, cx, cy, cz, nullFrame);
        final double[] cosSin = forceNullFrame.computeCosSin(pv, Vector3D.PLUS_K);
        forceNullFrame.addDAccDParam(s, ax, dAccdParam);

        // Only the first partial derivative is not null
        Assert.assertEquals(dAccdParam[0], cosSin[0], UtilsPatrius.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(dAccdParam[1], 0.0, 0.0);
        Assert.assertEquals(dAccdParam[2], 0.0, 0.0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @testedMethod {@link EmpiricalForce#getAx()}
     * @testedMethod {@link EmpiricalForce#getAy()}
     * @testedMethod {@link EmpiricalForce#getAz()}
     * @testedMethod {@link EmpiricalForce#getBx()}
     * @testedMethod {@link EmpiricalForce#getBy()}
     * @testedMethod {@link EmpiricalForce#getBz()}
     * @testedMethod {@link EmpiricalForce#getCx()}
     * @testedMethod {@link EmpiricalForce#getCy()}
     * @testedMethod {@link EmpiricalForce#getCz()}
     * 
     * @description Test constructors and getters for coverage purpose
     * 
     * @testPassCriteria no exceptions raised
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public final void testConstructorsGetters() {
        final LinearFunction ax = new LinearFunction(new AbsoluteDate(), new Parameter("a", 2),
                new Parameter("a1", 1));
        final ConstantFunction ay = new ConstantFunction("ay", 1);
        final ConstantFunction az = new ConstantFunction(new Parameter("az", 0));
        final LinearFunction bx = new LinearFunction(new AbsoluteDate(), new Parameter("b", 2),
                new Parameter("b1", 1));
        final ConstantFunction by = new ConstantFunction(new Parameter("by", 1));
        final ConstantFunction bz = new ConstantFunction(new Parameter("bz", 0));
        final LinearFunction cx = new LinearFunction(new AbsoluteDate(), new Parameter("c", 2),
                new Parameter("c1", 1));
        final ConstantFunction cy = new ConstantFunction(new Parameter("cy", 1));
        final ConstantFunction cz = new ConstantFunction(new Parameter("cz", 0));
        final EmpiricalForce f = new EmpiricalForce(1, Vector3D.PLUS_K, ax, ay, az, bx, by, bz, cx,
                cy, cz, new LocalOrbitalFrame(this.cirf, LOFType.TNW, this.orbit, "TNW"));

        Assert.assertEquals(ax.hashCode(), f.getAx().hashCode());
        Assert.assertEquals(ay.hashCode(), f.getAy().hashCode());
        Assert.assertEquals(az.hashCode(), f.getAz().hashCode());
        Assert.assertEquals(bx.hashCode(), f.getBx().hashCode());
        Assert.assertEquals(by.hashCode(), f.getBy().hashCode());
        Assert.assertEquals(bz.hashCode(), f.getBz().hashCode());
        Assert.assertEquals(cx.hashCode(), f.getCx().hashCode());
        Assert.assertEquals(cy.hashCode(), f.getCy().hashCode());
        Assert.assertEquals(cz.hashCode(), f.getCz().hashCode());

        Assert.assertEquals(f.getVectorS(), Vector3D.PLUS_K);
        Assert.assertTrue(f.getAx().getParameters().get(0).getName().equals("EmpiricalForce_a"));
        Assert.assertTrue(f.getAy().getParameters().get(0).getName().equals("EmpiricalForce_ay"));
        Assert.assertTrue(f.getAz().getParameters().get(0).getName().equals("EmpiricalForce_az"));
        Assert.assertTrue(f.getBx().getParameters().get(0).getName().equals("EmpiricalForce_b"));
        Assert.assertTrue(f.getBy().getParameters().get(0).getName().equals("EmpiricalForce_by"));
        Assert.assertTrue(f.getBz().getParameters().get(0).getName().equals("EmpiricalForce_bz"));
        Assert.assertTrue(f.getCx().getParameters().get(0).getName().equals("EmpiricalForce_c"));
        Assert.assertTrue(f.getCy().getParameters().get(0).getName().equals("EmpiricalForce_cy"));
        Assert.assertTrue(f.getCz().getParameters().get(0).getName().equals("EmpiricalForce_cz"));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @testedMethod {@link EmpiricalForce#addDAccDParam(SpacecraftState, Parameter, double[]))}
     * 
     * @description Test exception thrown in case of unsupported parameter
     * 
     * @input an unsupported parameter
     * 
     * @output an exception
     * 
     * @testPassCriteria an exception should be raised
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test(expected = PatriusException.class)
    public final void testUnsupportedParam() throws PatriusException {
        final double[] dAccdParam = new double[3];
        this.forcesN1.addDAccDParam(this.state, new Parameter("toto", 1), dAccdParam);
    }

    /**
     * @throws ParseException
     * @throws IOException
     * @throws PatriusException
     * @testType UT
     * @testedMethod {@link EmpiricalForce#EmpiricalForce(int, Vector3D, Vector3D, Vector3D, Vector3D, Frame)}
     * @testedMethod {@link EmpiricalForce#EmpiricalForce(int, Vector3D, Vector3D, Vector3D, Vector3D, LOFType)
     *               )}
     * @description Test propagation with Empirical force in TNW frame following velocity in
     *              spacecraft frame is equal
     *              to propagation with Empirical force in TNW.
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testManeuverWithLOF() throws PatriusException, IOException, ParseException {

        // mass model
        final String thruster = "thruster";
        MassProvider massModel = new SimpleMassModel(1000., thruster);
        // initial orbit
        final double a = 6900e3;
        final double e = .001;
        final double i = 51.4 * FastMath.PI / 180;
        final double pa = 270 * FastMath.PI / 180;
        final double raan = 170 * FastMath.PI / 180;
        final double w = 30 * FastMath.PI / 180;

        // Echelle de temps TAI
        final TimeScale tai = TimeScalesFactory.getTAI();

        // Start date
        final AbsoluteDate date0 = new AbsoluteDate(2005, 1, 1, 6, 0, 0, tai);
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // inertial frame
        final Frame gcrf = FramesFactory.getGCRF();
        // initial orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE,
                gcrf, date0, mu);

        // keplerian period
        final double T = orbit.getKeplerianPeriod();
        // Final date
        final AbsoluteDate finalDate = date0.shiftedBy(T * 20);

        // attitude provider
        final AttitudeProvider attProv = new LofOffset(gcrf, LOFType.TNW);

        // attitude initiale
        final Attitude initialAttitude = attProv.getAttitude(orbit, date0, orbit.getFrame());

        // tol
        final double[][] tol = NumericalPropagator.tolerances(1, orbit, OrbitType.CARTESIAN);

        // integrateur
        final FirstOrderIntegrator dop853 = new DormandPrince853Integrator(.1, 7200, tol[0], tol[1]);

        // propagateur
        final NumericalPropagator prop1 = new NumericalPropagator(dop853);

        // bulletin initial
        final SpacecraftState etat = new SpacecraftState(orbit, initialAttitude, massModel);

        // initialisation du propagateur
        prop1.setInitialState(etat);
        prop1.clearEventsDetectors();
        prop1.setOrbitType(OrbitType.CARTESIAN);
        prop1.setAttitudeProvider(attProv);
        prop1.setMassProviderEquation(massModel);

        // Empricial force in LOF frame
        // direction used to compute n.omega in the expression F = a.cos(n.omega.t) +
        // b.sin(n.omega.t) + c
        final Vector3D referenceDirection = new Vector3D(1, 0, 0);
        // vector along S
        final Vector3D c = new Vector3D(0, 1, 0);
        final Vector3D b = Vector3D.ZERO;
        final Vector3D aa = Vector3D.ZERO;
        final ForceModel empiricalForce = new EmpiricalForce(1, referenceDirection, aa, b, c,
                LOFType.TNW);

        // Ajout manoeuvre impulsionnelle
        prop1.addForceModel(empiricalForce);

        // propagation
        final SpacecraftState endStateLofFrame = prop1.propagate(finalDate);

        // Empricial force with Sattelite frame
        // ----------------------------------
        massModel = new SimpleMassModel(1000., thruster);
        // propagateur
        final NumericalPropagator prop2 = new NumericalPropagator(dop853);

        // bulletin initial
        final SpacecraftState etat2 = new SpacecraftState(orbit, initialAttitude, massModel);

        // initialisation du propagateur
        prop2.setInitialState(etat2);
        prop2.clearEventsDetectors();
        prop2.setOrbitType(OrbitType.CARTESIAN);
        prop2.setAttitudeProvider(attProv);
        prop2.setMassProviderEquation(massModel);

        // impulsion
        final Frame nullFrame = null;
        final ForceModel empiricalForceSat = new EmpiricalForce(1, referenceDirection, aa, b, c,
                nullFrame);

        // Ajout empirical force
        prop2.addForceModel(empiricalForceSat);

        // propagation
        final SpacecraftState endStateSatFrame = prop2.propagate(finalDate);

        // Comparison
        // --------------------------------------------

        // Absolute difference !
        Assert.assertEquals(endStateSatFrame.getA(), endStateLofFrame.getA(), 1.0e-14);
        Assert.assertEquals(endStateSatFrame.getEquinoctialEx(),
                endStateLofFrame.getEquinoctialEx(), 1.0e-14);
        Assert.assertEquals(endStateSatFrame.getEquinoctialEy(),
                endStateLofFrame.getEquinoctialEy(), 1.0e-14);
        Assert.assertEquals(endStateSatFrame.getHx(), endStateLofFrame.getHx(), 1.0e-14);
        Assert.assertEquals(endStateSatFrame.getHy(), endStateLofFrame.getHy(), 1.0e-14);
        Assert.assertEquals(endStateSatFrame.getLM(), endStateLofFrame.getLM(), 1.0e-14);
        Assert.assertEquals(endStateSatFrame.getMass(thruster), endStateLofFrame.getMass(thruster),
                1.0e-14);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#EMPIRICAL_FORCE_VALIDATION}
     * 
     * @testedMethod {@link EmpiricalForce#computeGradientPosition()}
     * @testedMethod {@link EmpiricalForce#computeGradientVelocity()}
     * 
     * @description check that no acceleration partial derivatives are handled by this class
     * 
     * @input an instance of {@link EmpiricalForce}
     * 
     * @output booleans
     * 
     * @testPassCriteria since there are no partial derivatives computation, output booleans must be
     *                   false
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void isComputePDTest() {
        // instance
        final LinearFunction ax = new LinearFunction(new AbsoluteDate(), 2, 1);
        final ConstantFunction ay = new ConstantFunction("ay", 1);
        final ConstantFunction az = new ConstantFunction(new Parameter("az", 0));
        final LinearFunction bx = new LinearFunction(new AbsoluteDate(), new Parameter("b", 2),
                new Parameter("bx", 1));
        final ConstantFunction by = new ConstantFunction(new Parameter("by", 1));
        final ConstantFunction bz = new ConstantFunction(new Parameter("bz", 0));
        final LinearFunction cx = new LinearFunction(new AbsoluteDate(), new Parameter("b", 2),
                new Parameter("bz", 1));
        final ConstantFunction cy = new ConstantFunction(new Parameter("cy", 1));
        final ConstantFunction cz = new ConstantFunction(new Parameter("cz", 0));
        final EmpiricalForce f = new EmpiricalForce(1, Vector3D.PLUS_K, ax, ay, az, bx, by, bz, cx,
                cy, cz, new LocalOrbitalFrame(this.cirf, LOFType.TNW, this.orbit, "TNW"));

        Assert.assertFalse(f.computeGradientPosition());
        Assert.assertFalse(f.computeGradientVelocity());
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link ForceModel#enrichParameterDescriptors()}
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() {

        EmpiricalForce forceModel = this.forcesN1;

        // Check that the force model has some parameters (otherwise it shouldn't be enriched)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }

        // Also check this other constructor which also needs to call the enrichParameterDescriptors
        // method
        forceModel = new EmpiricalForce(1, Vector3D.PLUS_K, new LinearFunction(new AbsoluteDate(),
                new Parameter("ax", 1), new Parameter("bx", 2)), new ConstantFunction(
                new Parameter("ay", 1)), new ConstantFunction(new Parameter("az", 0)),
                new LinearFunction(new AbsoluteDate(), new Parameter("by", 1),
                        new Parameter("b", 2)), new ConstantFunction(new Parameter("by", 1)),
                new ConstantFunction(new Parameter("bz", 0)), new LinearFunction(
                        new AbsoluteDate(), new Parameter("bz", 1), new Parameter("b", 2)),
                new ConstantFunction(new Parameter("cy", 1)), new ConstantFunction(new Parameter(
                        "cz", 0)), LOFType.TNW);

        // Check that the force model has some parameters (otherwise it shouldn't be enriched)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }

        // Also check this other constructor which also needs to call the enrichParameterDescriptors
        // method
        final LinearFunction ax = new LinearFunction(new AbsoluteDate(), 1, 2);
        final ConstantFunction ay = new ConstantFunction("ay", 1);
        final ConstantFunction az = new ConstantFunction(new Parameter("az", 0));
        final LinearFunction bx = new LinearFunction(new AbsoluteDate(), new Parameter("bx", 1),
                new Parameter("b", 2));
        final ConstantFunction by = new ConstantFunction(new Parameter("by", 1));
        final ConstantFunction bz = new ConstantFunction(new Parameter("bz", 0));
        final LinearFunction cx = new LinearFunction(new AbsoluteDate(), new Parameter("bz", 1),
                new Parameter("b", 2));
        final ConstantFunction cy = new ConstantFunction(new Parameter("cy", 1));
        final ConstantFunction cz = new ConstantFunction(new Parameter("cz", 0));
        forceModel = new EmpiricalForce(1, Vector3D.PLUS_K, ax, ay, az, bx, by, bz, cx, cy, cz,
                new LocalOrbitalFrame(this.cirf, LOFType.TNW, this.orbit, "TNW"));

        // Check that the force model has some parameters (otherwise it shouldn't be enriched)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }
    }

    /**
     * Set up method before running the test.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");

        // test case n.1 - N=1:
        final Vector3D position1 = new Vector3D(-1.82669974206826097e+05, 3.56926390363572072e+06,
                6.24582796461802721e+06);
        final Vector3D velocity1 = new Vector3D(-6.66245723444786017e+03, -2.96132877876968496e+03,
                1.50462147504547397e+03);
        this.pv1 = new PVCoordinates(position1, velocity1);
        final double mu = 3.9860043770442000E+14;
        this.cirf = FramesFactory.getCIRF();
        this.orbit = new CartesianOrbit(this.pv1, this.cirf, AbsoluteDate.J2000_EPOCH, mu);
        this.state = new SpacecraftState(this.orbit);
        this.forcesN1 = new EmpiricalForce(1, Vector3D.PLUS_K, new Vector3D(1.E-7, 2.E-7, 3.E-7),
                new Vector3D(1.E-7, 2.E-7, 3.E-7), new Vector3D(1.E-7, 2.E-7, 3.E-7),
                new LocalOrbitalFrame(this.cirf, LOFType.QSW, this.orbit, "local"));

        // test case n.2 - N=4:
        final Vector3D position2 = new Vector3D(-3.89465042891014460e+06, 1.23913008523466252e+06,
                5.92795434638620354e+06);
        final Vector3D velocity2 = new Vector3D(-5.31192383064975274e+03, -4.55467063031813996e+03,
                -2.52882012658937629e+03);
        this.pv2 = new PVCoordinates(position2, velocity2);
        final Vector3D positionS = new Vector3D(9.6358466080300964E-01, -2.4550561484545036E-01,
                -1.0597921751223768E-01).scalarMultiply(1.4837898463744528E+11);
        final Vector3D n = this.pv2.getMomentum().normalize();
        this.ref = positionS.subtract(n.scalarMultiply(Vector3D.dotProduct(positionS, n)));
        this.ref = this.ref.normalize();
        this.forcesN4 = new EmpiricalForce(4, this.ref, new Vector3D(1.E-7, 2.E-7, 3.E-7),
                new Vector3D(1.E-7, 2.E-7, 3.E-7), new Vector3D(1.E-7, 2.E-7, 3.E-7),
                new LocalOrbitalFrame(this.cirf, LOFType.TNW, this.orbit, "TNW"));
    }
}
