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
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:228:26/03/2014:Corrected partial derivatives computation
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::FA:295:24/10/2014:order null computation modified
 * VERSION::FA:530:02/02/2016:Corrected anomaly at creation of a Balmino model with order 0, degree 1
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1267:09/03/2018: Addition of getters for C and CS tables
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.HelmholtzPolynomial;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BalminoGravityModelTest {

    private final Parameter mu = new Parameter("mu", Constants.GRIM5C1_EARTH_MU);
    private final Parameter ae = new Parameter("ae", Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS);

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     * 
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testParamList() throws PatriusException, IOException, ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");

        final Frame itrf = FramesFactory.getITRF();

        // gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        final double[][] c = data.getC(6, 6, false);
        final double[][] s = data.getS(6, 6, false);

        final BalminoGravityModel att = new BalminoGravityModel(itrf, this.ae, this.mu, c, s);

        // check MU
        Assert.assertTrue(Precision.equals(Constants.GRIM5C1_EARTH_MU, att.getMu(), 0));

        // change mu value
        this.mu.setValue(1.5);
        Assert.assertTrue(Precision.equals(1.5, att.getMu(), 0));
    }

    /**
     * FA 93 : added test to ensure the list of "jacobian parameters" is empty.
     * 
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testJacobianList() throws PatriusException, IOException, ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");

        final Frame itrf = FramesFactory.getITRF();

        // gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        final double[][] c = data.getC(6, 6, false);
        final double[][] s = data.getS(6, 6, false);

        final BalminoGravityModel att = new BalminoGravityModel(itrf, this.ae, this.mu, c, s);
    }

    /**
     * Additional partial derviatives tests to ensure the jacobian is correctly taken into account.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPartialDerivatives() throws PatriusException, IOException, ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // date
        final AbsoluteDate date = new AbsoluteDate();

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        final double a = ae + 400e3;
        final double e = .001;
        final double i = .93;

        // orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, 0, 0, 0, PositionAngle.TRUE, gcrf,
            date, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        // gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        final double[][] c = data.getC(6, 6, false);
        final double[][] s = data.getS(6, 6, false);

        final BalminoGravityModel att = new BalminoGravityModel(itrf, ae, mu, c, s);
        att.setCentralTermContribution(false);

        final Transform t = gcrf.getTransformTo(itrf, date);

        // partial derivatives in ITRF frame
        final double[][] dAccdPos = att.computeDAccDPos(t.transformPosition(state.getPVCoordinates().getPosition()),
            state.getDate());

        /*
         * ====================================== finite diff _ DELTAS IN ITRF
         */
        final Vector3D pos = orbit.getPVCoordinates(itrf).getPosition();
        final Vector3D vel = orbit.getPVCoordinates(itrf).getVelocity();

        /* ===================================== */

        final double dh = .5;

        // positions
        final Vector3D ppx = pos.add(Vector3D.PLUS_I.scalarMultiply(dh));
        final Vector3D ppy = pos.add(Vector3D.PLUS_J.scalarMultiply(dh));
        final Vector3D ppz = pos.add(Vector3D.PLUS_K.scalarMultiply(dh));

        final Vector3D pmx = pos.add(Vector3D.PLUS_I.scalarMultiply(-dh));
        final Vector3D pmy = pos.add(Vector3D.PLUS_J.scalarMultiply(-dh));
        final Vector3D pmz = pos.add(Vector3D.PLUS_K.scalarMultiply(-dh));

        // pv coordinates
        final PVCoordinates pvpx = new PVCoordinates(ppx, vel);
        final PVCoordinates pvpy = new PVCoordinates(ppy, vel);
        final PVCoordinates pvpz = new PVCoordinates(ppz, vel);

        final PVCoordinates pvmx = new PVCoordinates(pmx, vel);
        final PVCoordinates pvmy = new PVCoordinates(pmy, vel);
        final PVCoordinates pvmz = new PVCoordinates(pmz, vel);
        
        // acc
        final Vector3D apx = att.computeNonCentralTermsAcceleration(pvpx.getPosition(), date);
        final Vector3D apy = att.computeNonCentralTermsAcceleration(pvpy.getPosition(), date);
        final Vector3D apz = att.computeNonCentralTermsAcceleration(pvpz.getPosition(), date);

        final Vector3D amx = att.computeNonCentralTermsAcceleration(pvmx.getPosition(), date);
        final Vector3D amy = att.computeNonCentralTermsAcceleration(pvmy.getPosition(), date);
        final Vector3D amz = att.computeNonCentralTermsAcceleration(pvmz.getPosition(), date);

        // pds
        final Vector3D pdx = apx.subtract(amx).scalarMultiply(1 / (2 * dh));
        final Vector3D pdy = apy.subtract(amy).scalarMultiply(1 / (2 * dh));
        final Vector3D pdz = apz.subtract(amz).scalarMultiply(1 / (2 * dh));

        final double[][] acc = { pdx.toArray(), pdy.toArray(), pdz.toArray() };
        final double[][] tacc = this.transpose(acc);

        final double[][] diff = new double[3][3];
        for (int ii = 0; ii < diff.length; ii++) {
            for (int j = 0; j < diff[ii].length; j++) {
                diff[ii][j] = (dAccdPos[ii][j] - tacc[ii][j]) / dAccdPos[ii][j];
                Assert.assertEquals(0, diff[ii][j], 5e-5);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link BalminoGravityModel#BalminoAttractionModel(Frame, double, double, double[][], double[][], double[][], double[][], boolean)}
     * @testedMethod {@link BalminoGravityModel#BalminoAttractionModel(Frame, Parameter, Parameter, double[][], double[][], double[][], double[][], boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link BalminoGravityModel}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at
     *                   construction : instantiation is done with null tabs of normalized
     *                   coefficients used for partial derivatives computation
     * 
     * @throws PatriusException when an Orekit error occurs
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException, IOException, ParseException {
        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // SpacecraftState
        final KeplerianOrbit orbit = new KeplerianOrbit(7E7, 0.001, 0.93, 0, 0, 0,
            PositionAngle.TRUE, gcrf, AbsoluteDate.J2000_EPOCH, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        // gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();
        final double[][] c = data.getC(6, 6, false);
        final double[][] s = data.getS(6, 6, false);
        final BalminoGravityModel model = new BalminoGravityModel(itrf, ae, mu, c, s, 0, 0);
        model.setCentralTermContribution(false);
        final BalminoGravityModel model2 = new BalminoGravityModel(itrf, new Parameter("ae",
            ae), new Parameter("mu", mu), c, s, 0, 0);
        model2.setCentralTermContribution(false);

        final Transform t = gcrf.getTransformTo(itrf, orbit.getDate());

        // Partial derivatives
        final double[][] dAccdPos = model.computeDAccDPos(t.transformPosition(state.getPVCoordinates().getPosition()),
            state.getDate());
        final double[][] dAccdPos2 = model2.computeDAccDPos(
            t.transformPosition(state.getPVCoordinates().getPosition()),
            state.getDate());

        // Check all derivatives are null
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 3; k++) {
                Assert.assertEquals(0, dAccdPos[j][k], 0);
                Assert.assertEquals(0, dAccdPos2[j][k], 0);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link BalminoGravityModel#addDAccDState(SpacecraftState, double[][], double[][])}
     * 
     * @description This test checks that:
     *              <ul>
     *              <li>The numerical propagation of a given orbit using instances of BalminoAttractionModel with fixed
     *              degree/order (60, 60) for acceleration but different degree/order (60, 60) and (59, 59) for partial
     *              derivatives lead to the same [position, velocity] state but slighty different state transition
     *              matrix.</li>
     *              <li>The partial derivatives of model (60, 60) for acceleration and (59, 59) for partial derivatives
     *              are the same than of model (59, 59) for acceleration and (59, 59) for partial derivatives.</li>
     *              <ul>
     * 
     * @input instances of {@link BalminoGravityModel}
     * 
     * @output positions, velocities of final orbits, partials derivatives
     * 
     * @testPassCriteria the [positions, velocities] must be equals, state transition matrix
     *                   "almost" the same (relative difference < 1E-5)
     * 
     * @throws PatriusException when an Orekit error occurs
     * @throws ParseException
     * @throws IOException
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testPropagationDifferentDegreeOrder() throws PatriusException, IOException,
                                                     ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // frames
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        // date
        final AbsoluteDate date = new AbsoluteDate();

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;

        // SpacecraftState
        final KeplerianOrbit orbit = new KeplerianOrbit(7E6, 0.001, 0.93, 0, 0, 0,
            PositionAngle.TRUE, gcrf, date, mu);
        SpacecraftState state1 = new SpacecraftState(orbit);
        SpacecraftState state2 = new SpacecraftState(orbit);
        final double t = orbit.getKeplerianPeriod();

        // gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        // Create 2 instances of BalminoModel with different degrees/orders
        final double[][] c1 = data.getC(60, 60, false);
        final double[][] s1 = data.getS(60, 60, false);
        final double[][] c2 = data.getC(59, 59, false);
        final double[][] s2 = data.getS(59, 59, false);
        final BalminoGravityModel model1 = new BalminoGravityModel(itrf, ae, mu, c1, s1, 60,
            60);
        final BalminoGravityModel model2 = new BalminoGravityModel(itrf, ae, mu, c1, s1, 59,
            59);
        final BalminoGravityModel model3 = new BalminoGravityModel(itrf, ae, mu, c2, s2, 59,
            59);

        // Propagators
        final double step = 60;
        final FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(step);
        final NumericalPropagator prop1 = new NumericalPropagator(integrator);
        final NumericalPropagator prop2 = new NumericalPropagator(integrator);

        final PartialDerivativesEquations eq1 = new PartialDerivativesEquations("partial", prop1);
        state1 = eq1.setInitialJacobians(state1);
        prop1.setInitialState(state1);
        prop1.addForceModel(new DirectBodyAttraction(model1));
        final PartialDerivativesEquations eq2 = new PartialDerivativesEquations("partial", prop2);
        state2 = eq2.setInitialJacobians(state2);
        prop2.setInitialState(state2);
        prop2.addForceModel(new DirectBodyAttraction(model2));

        // Propagation : final state
        final SpacecraftState FinalState1 = prop1.propagate(date.shiftedBy(t));
        final SpacecraftState FinalState2 = prop2.propagate(date.shiftedBy(t));

        // Positions and velocities must be the same whereas degrees/orders are different for each
        // model
        final Vector3D pos1 = FinalState1.getPVCoordinates().getPosition();
        final Vector3D pos2 = FinalState1.getPVCoordinates().getPosition();
        final Vector3D vel1 = FinalState2.getPVCoordinates().getVelocity();
        final Vector3D vel2 = FinalState2.getPVCoordinates().getVelocity();

        Assert.assertEquals(0., pos1.distance(pos2), 0.);
        Assert.assertEquals(0., vel1.distance(vel2), 0.);

        // Check that partial derivatives are different, but "nearly" the same
        final double epsilon = 8.6E-5;
        final double[] stm1 = FinalState1.getAdditionalState("partial");
        final double[] stm2 = FinalState2.getAdditionalState("partial");
        for (int i = 0; i < stm1.length; i++) {
            Assert.assertEquals(0., (stm1[i] - stm2[i]) / stm1[i], epsilon);
            Assert.assertFalse(stm1[i] == stm2[i]);
        }

        final Transform transform = gcrf.getTransformTo(itrf, orbit.getDate());

        // Check that different instances of BalminoModel returns same partial derivatives
        final double[][] dAccdPos = model2.computeDAccDPos(
            transform.transformPosition(state1.getPVCoordinates().getPosition()),
            state1.getDate());
        final double[][] dAccdPos2 = model3.computeDAccDPos(
            transform.transformPosition(state1.getPVCoordinates().getPosition()),
            state1.getDate());

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 3; k++) {
                Assert.assertEquals(dAccdPos[j][k], dAccdPos2[j][k], 0);
            }
        }

        // Check degree and order upper limit
        try {
            new BalminoGravityModel(itrf, ae, mu, c1, s1, 61, 60);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        try {
            new BalminoGravityModel(itrf, ae, mu, c1, s1, 60, 61);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    // private method to compute acceleration
    public Vector3D computeAcceleration(final SpacecraftState s, final int d, final int o,
                                        final double[][] cCoefs, final double[][] sCoefs) throws PatriusException {
        // Get the position in the body frame
        final Transform fromBodyFrame = FramesFactory.getITRF().getTransformTo(s.getFrame(),
            s.getDate());
        final Transform toBodyFrame = fromBodyFrame.getInverse();
        final Vector3D positionInBodyFrame = toBodyFrame.transformPosition(s.getPVCoordinates().getPosition());
        Vector3D gamma = GravityToolbox.computeBalminoAcceleration(positionInBodyFrame, cCoefs, sCoefs,
            this.mu.getValue(), this.ae.getValue(), d, o, new HelmholtzPolynomial(6, 6));
        // compute acceleration in inertial frame
        gamma = fromBodyFrame.transformVector(gamma);
        return gamma;
    }

    /**
     * FA 530 : Correct anomaly on Balmino model with order = 1 and degree = 0
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#BALMINO}
     * 
     * @testedMethod {@link BalminoGravityModel#BalminoAttractionModel(Frame, double, double, double[][], double[][])}
     *               {@link BalminoGravityModel#computeAcceleration(SpacecraftState)}
     * 
     * @description Test that the construction of an instance of the class is allowed with order = 1
     *              and degree = 0. Plus, the accelerations of a given spacecraft computed must be
     *              the same in the following cases : order 0/degree 0, order 1/degree 0 and order
     *              1/degree 1 (must be [0, 0, 0])
     * 
     * @testPassCriteria The constructor raises no exception and accelerations are the one expected
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testBalminoParticularCases() throws PatriusException, IOException, ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Non inertial frame
        final Frame nonInertialEarthFrame = FramesFactory.getITRF();

        // Date
        final AbsoluteDate date = new AbsoluteDate();

        // Constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = ae + 400e3;
        final double e = .001;
        final double i = .93;

        // Orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, 0, 0, 0, PositionAngle.TRUE,
            nonInertialEarthFrame, date, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // A provider for the GRGS data is created
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        // Degrees
        final int degree0 = 0;
        final int degree1 = 1;

        // Orders
        final int order0 = 0;
        final int order1 = 1;

        // unnormalized Cosine coefficients
        final double[][] unnormalizedC00 = provider.getC(degree0, order0, false);
        final double[][] unnormalizedC10 = provider.getC(degree1, order0, false);
        final double[][] unnormalizedC11 = provider.getC(degree1, order1, false);

        // unnormalized Sine coefficients
        final double[][] unnormalizedS00 = provider.getS(degree0, order0, false);
        final double[][] unnormalizedS10 = provider.getS(degree1, order0, false);
        final double[][] unnormalizedS11 = provider.getS(degree1, order1, false);

        // Balmino model : order = 0, degree = 0
        final BalminoGravityModel bal00 = new BalminoGravityModel(nonInertialEarthFrame,
            provider.getAe(), provider.getMu(), unnormalizedC00, unnormalizedS00);
        bal00.setCentralTermContribution(false);

        // Balmino model : order = 0, degree = 1
        // This construction should be allowed
        final BalminoGravityModel bal10 = new BalminoGravityModel(nonInertialEarthFrame,
            provider.getAe(), provider.getMu(), unnormalizedC10, unnormalizedS10);
        bal10.setCentralTermContribution(false);

        // Balmino model : order = 1, degree = 1
        final BalminoGravityModel bal11 = new BalminoGravityModel(nonInertialEarthFrame,
            provider.getAe(), provider.getMu(), unnormalizedC11, unnormalizedS11);
        bal11.setCentralTermContribution(false);

        // Compute the acceleration due to the different forces
        final Vector3D acc00 = bal00.computeNonCentralTermsAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());
        final Vector3D acc10 = bal10.computeNonCentralTermsAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());
        final Vector3D acc11 = bal11.computeNonCentralTermsAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate());

        // Check that all accelerations are the same : [0, 0, 0]
        Assert.assertEquals(0., acc00.getNorm(), 0.);
        Assert.assertEquals(0., acc10.getNorm(), 0.);
        Assert.assertEquals(0., acc11.getNorm(), 0.);
    }

    /**
     * FA 648 : Raise an exception if input order or degree for partial derivatives computation is
     * negative.
     * 
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#BALMINO}
     * 
     * @testedMethod {@link BalminoGravityModel#BalminoAttractionModel(Frame, Parameter, Parameter, double[][], double[][], int, int)}
     * 
     * @description Test to cover the case of a BalminoAttractionModel creation with either a
     *              negative order for partial derivatives computation or a negative degree : an
     *              exception should be raised.
     * 
     * @testPassCriteria An exception should be raised when calling the constructor with a bad
     *                   input.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBalminoNegativeOrderOrDegree() throws IOException, ParseException,
                                                  PatriusException {
        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Non inertial frame
        final Frame nonInertialEarthFrame = FramesFactory.getITRF();

        // Date
        final AbsoluteDate date = new AbsoluteDate();

        // Constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = ae + 400e3;
        final double e = .001;
        final double i = .93;

        // Orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, 0, 0, 0, PositionAngle.TRUE,
            nonInertialEarthFrame, date, mu);
        new SpacecraftState(orbit);

        // Gravity
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // A provider for the GRGS data is created
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        // Degree
        final int degree = 50;

        // Order
        final int order = 50;

        // unnormalized Cosine coefficients
        final double[][] unnormalizedC = provider.getC(degree, order, false);

        // unnormalized Sine coefficients
        final double[][] unnormalizedS = provider.getS(degree, order, false);

        // Balmino model : order for partial derivatives < 0, degree > 0
        final int degreePD = degree;
        final int orderPDNeg = -1;

        // An exception should be raised here !
        try {
            new BalminoGravityModel(
                nonInertialEarthFrame, new Parameter("ae", provider.getAe()), new Parameter(
                    "mu", provider.getMu()), unnormalizedC, unnormalizedS, degreePD,
                orderPDNeg);
        } catch (final IllegalArgumentException exc) {
            Assert.assertTrue(true);
        }

        // Balmino model : order for partial derivatives > 0, degree < 0
        final int orderPD = order;
        final int degreePDNeg = -1;

        new BalminoGravityModel(
            nonInertialEarthFrame, new Parameter("ae", provider.getAe()), new Parameter("mu",
                provider.getMu()), unnormalizedC, unnormalizedS, degreePDNeg, orderPD);
    }

    private static class AccelerationRetriever implements TimeDerivativesEquations {

        private static final long serialVersionUID = -4616792058307814184L;
        private Vector3D acceleration;

        @Override
        public void initDerivatives(final double[] yDot, final Orbit currentOrbit) {
        }

        @Override
        public void addXYZAcceleration(final double x, final double y, final double z) {
            this.acceleration = new Vector3D(x, y, z);
        }

        @Override
        public void addAcceleration(final Vector3D gamma, final Frame frame) {
        }

        public Vector3D getAcceleration() {
            return this.acceleration;
        }

        @Override
        public void addAdditionalStateDerivative(final String name, final double[] pDot) {
        }

    }

    void print(final double[][] d) {
        for (final double[] row : d) {
            for (final double e : row) {
                System.out.printf("%.16e\t", e);
            }
            System.out.println();
        }
    }

    double[][] transpose(final double[][] d) {

        final double[][] dt = new double[d[0].length][d.length];

        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                dt[j][i] = d[i][j];
            }
        }

        return dt;

    }

    @Test
    public void testAcceleration() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("normalized");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // c and s tables
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("EGNSTA02BS", true));
        final PotentialCoefficientsProvider pot = GravityFieldFactory.getPotentialProvider();

        final int degree = 70;
        final int order = 70;

        final double[][] cN = pot.getC(degree, order, true);
        final double[][] sN = pot.getS(degree, order, true);

        final double[][] cU = pot.getC(degree, order, false);
        final double[][] sU = pot.getS(degree, order, false);

        // pv
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate date = new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getTAI());
        final double mu = Constants.EIGEN5C_EARTH_MU;
        final double ae = Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS;

        final Vector3D pos = new Vector3D(-6590149.9269526824, 521546.44375059905,
            886362.25364358397);
        final PVCoordinates pv = new PVCoordinates(pos, new Vector3D(7000, 0, 0));

        // Helmholtz
        final CunninghamGravityModel model = new CunninghamGravityModel(itrf, ae, mu, cU, sU);
        final BalminoGravityModel model1 = new BalminoGravityModel(itrf, ae, mu, cN, sN);
        model1.setCentralTermContribution(false);

        final SpacecraftState spc = new SpacecraftState(new CartesianOrbit(pv,
            FramesFactory.getGCRF(), date, mu));

        final Transform t = itrf.getTransformTo(spc.getFrame(), date);
        final Vector3D cun = model.computeNonCentralTermsAcceleration(t.transformPosition(pv.getPosition()), date);
        final Vector3D cun1 = model.computeNonCentralTermsAcceleration(
            t.transformPosition(spc.getPVCoordinates().getPosition()),
            spc.getDate());
        final Vector3D nor = model1.computeNonCentralTermsAcceleration(t.transformPosition(pv.getPosition()), date);
        final Vector3D nor1 = model1.computeNonCentralTermsAcceleration(
            t.transformPosition(spc.getPVCoordinates().getPosition()),
            spc.getDate());

        Assert.assertEquals(0, (cun.getX() - nor.getX()) / cun.getX(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (cun.getY() - nor.getY()) / cun.getY(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (cun.getZ() - nor.getZ()) / cun.getZ(),
            Precision.DOUBLE_COMPARISON_EPSILON);

        Assert.assertEquals(0, (cun1.getX() - nor1.getX()) / cun.getX(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (cun1.getY() - nor1.getY()) / cun.getY(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (cun1.getZ() - nor1.getZ()) / cun.getZ(),
            Precision.DOUBLE_COMPARISON_EPSILON);

    }

    @Test
    public void testOtherMethods() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("normalized");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // c and s tables
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("EGNSTA02BS", true));
        final PotentialCoefficientsProvider pot = GravityFieldFactory.getPotentialProvider();

        final int degree = 70;
        final int order = 70;

        final double[][] cN = pot.getC(degree, order, true);
        final double[][] sN = pot.getS(degree, order, true);

        // pv
        final Frame itrf = FramesFactory.getITRF();
        final Parameter mu = new Parameter("mu", Constants.EIGEN5C_EARTH_MU);
        final Parameter ae = new Parameter("ae", Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS);

        // Helmholtz
        final BalminoGravityModel model = new BalminoGravityModel(itrf, ae, mu, cN, sN);

        final EventDetector[] list = (new DirectBodyAttraction(model)).getEventsDetectors();
        Assert.assertEquals(0, list.length);

        Assert.assertEquals(mu.getValue(), model.getMu(), Precision.EPSILON);

        mu.setValue(200.);
        Assert.assertEquals(200., model.getMu(), Precision.EPSILON);
    }

    // Check that the MU used during the propagation is the MU of the gravitational force (Balmino)
    // and not the MU
    // of the orbit.
    @Test
    public void testMu() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("normalized");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // c and s tables
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("EGNSTA02BS", true));
        final PotentialCoefficientsProvider pot = GravityFieldFactory.getPotentialProvider();

        final int degree = 70;
        final int order = 70;

        final double[][] cN = pot.getC(degree, order, true);
        final double[][] sN = pot.getS(degree, order, true);

        // pv
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate date = new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getTAI());
        final double mu = Constants.EIGEN5C_EARTH_MU;
        // use a slightly modified value for the MU of the orbit:
        final double mu_orbit = Constants.EIGEN5C_EARTH_MU + 5.0;
        final double ae = Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS;

        final Vector3D pos = new Vector3D(-6590149.9269526824, 521546.44375059905,
            886362.25364358397);
        final PVCoordinates pv = new PVCoordinates(pos, new Vector3D(7000, 0, 0));

        // Balmino force:
        final BalminoGravityModel model = new BalminoGravityModel(itrf, ae, mu, cN, sN);

        final SpacecraftState spc = new SpacecraftState(new CartesianOrbit(pv,
            FramesFactory.getGCRF(), date, mu_orbit));

        final double[] absTolerance = { 1.0e-6, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0., 10,
            absTolerance, relTolerance);
        integrator.setInitialStepSize(10);

        // First propagator: use a gravitational force
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator);
        propagator1.setInitialState(spc);
        propagator1.addForceModel(new DirectBodyAttraction(model));
        final SpacecraftState spc1 = propagator1.propagate(date.shiftedBy(100.0));
        // The MU used by the propagator is the MU of the orbit:
        Assert.assertEquals(mu_orbit, propagator1.getMu(), 0.0);

        // Second propagator: no gravitational force
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator);
        propagator2.setInitialState(spc);
        final SpacecraftState spc2 = propagator2.propagate(date.shiftedBy(100.0));
        // The MU used by the propagator is the MU of the orbit:
        Assert.assertEquals(mu_orbit, propagator2.getMu(), 0.0);

        // The two propagations should not have produced the same output!
        Assert.assertFalse(spc1.getPVCoordinates().getPosition().getNorm() == spc2
            .getPVCoordinates().getPosition().getNorm());
    }

    /**
     * 
     * @testType UT
     * 
     * @testedMethod {@link BalminoGravityModel#BalminoAttractionModel(Frame, double, double, double[][], double[][])}
     * @testedMethod {@link BalminoGravityModel#computeAcceleration(PVCoordinates)}
     * 
     * @description FT 295 : test with order null < degree
     * 
     * @input a {@link BalminoGravityModel}
     * 
     * @output the acceleration
     * 
     * @testPassCriteria the acceleration computed with Balmino model should be equal to Cunningham
     *                   acceleration
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testOrderNull() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("normalized");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // c and s tables
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("EGNSTA02BS", true));
        final PotentialCoefficientsProvider pot = GravityFieldFactory.getPotentialProvider();

        final int degree = 2;
        // with J2 but no more order for gravitational perturbations
        final int order = 0;

        final double[][] cN = pot.getC(degree, order, true);
        final double[][] sN = pot.getS(degree, order, true);

        final double[][] cU = pot.getC(degree, order, false);
        final double[][] sU = pot.getS(degree, order, false);

        // pv
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate date = new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getTAI());
        final double mu = Constants.EIGEN5C_EARTH_MU;
        final double ae = Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS;

        final Vector3D pos = new Vector3D(-6590149.9269526824, 521546.44375059905,
            886362.25364358397);
        final PVCoordinates pv = new PVCoordinates(pos, new Vector3D(0, 0, 0));

        // Helmholtz
        final CunninghamGravityModel model = new CunninghamGravityModel(itrf, ae, mu, cU, sU);
        final BalminoGravityModel model1 = new BalminoGravityModel(itrf, ae, mu, cN, sN);
        model1.setCentralTermContribution(false);

        final Vector3D cun = model.computeNonCentralTermsAcceleration(pv.getPosition(), date);
        final Vector3D nor = model1.computeNonCentralTermsAcceleration(pv.getPosition(), date);

        Assert.assertEquals(0, (cun.getX() - nor.getX()) / cun.getX(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (cun.getY() - nor.getY()) / cun.getY(),
            Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (cun.getZ() - nor.getZ()) / cun.getZ(),
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Test
    public void testCSTables() {
        // Tabs creation
        final double[][] cCoefs = new double[2][2];
        cCoefs[0][0] = 1;
        cCoefs[0][1] = 2;
        cCoefs[1][0] = 3;
        cCoefs[1][1] = 4;

        final double[][] sCoefs = new double[2][2];
        sCoefs[0][0] = 5;
        sCoefs[0][1] = 6;
        sCoefs[1][0] = 7;
        sCoefs[1][1] = 8;

        final BalminoGravityModel model = new BalminoGravityModel(FramesFactory.getGCRF(),
            Constants.CNES_STELA_AE, Constants.CNES_STELA_MU, cCoefs, sCoefs);
        // Get values
        Assert.assertEquals(2, model.getC().length, 0);
        Assert.assertEquals(2, model.getC()[0][1], 0);
        Assert.assertEquals(4, model.getC()[1][1], 0);
        Assert.assertEquals(2, model.getS()[0].length, 0);
        Assert.assertEquals(5, model.getS()[0][0], 0);
        Assert.assertEquals(7, model.getS()[1][0], 0);
    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {
        // Tabs creation
        final double[][] cCoefs = new double[2][2];
        cCoefs[0][0] = 1;
        cCoefs[0][1] = 2;
        cCoefs[1][0] = 3;
        cCoefs[1][1] = 4;

        final double[][] sCoefs = new double[2][2];
        sCoefs[0][0] = 5;
        sCoefs[0][1] = 6;
        sCoefs[1][0] = 7;
        sCoefs[1][1] = 8;
        final Frame frame = FramesFactory.getGCRF();
        final BalminoGravityModel model = new BalminoGravityModel(frame, Constants.CNES_STELA_AE,
            Constants.CNES_STELA_MU, cCoefs, sCoefs);
        Assert.assertEquals(true, model.getBodyFrame().getName().equals(frame.getName()));
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link BalminoGravityModel#computeAcceleration(SpacecraftState)}
     * 
     * @description compute acceleration with multiplicative factor k
     * 
     * @testPassCriteria acceleration with k = 5 = 5 * acceleration with k = 1
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testMultiplicativeFactor() throws PatriusException, IOException, ParseException {
        Utils.setDataRoot("normalized");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("EGNSTA02BS", true));
        final PotentialCoefficientsProvider pot = GravityFieldFactory.getPotentialProvider();
        final double[][] c = pot.getC(4, 4, true);
        final double[][] s = pot.getS(4, 4, true);
        final double ae = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.WGS84_EARTH_MU;
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7000000, 0, 0, 0, 0, 0,
            PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, mu));
        final BalminoGravityModel actualModel = new BalminoGravityModel(FramesFactory.getGCRF(), new Parameter("", ae),
                new Parameter("", mu), c, s, 4, 4);
        actualModel.setCentralTermContribution(false);
        final DirectBodyAttraction forceModel = new DirectBodyAttraction(actualModel);
        forceModel.setMultiplicativeFactor(5.);
        final BalminoGravityModel expectedModel = new BalminoGravityModel(FramesFactory.getGCRF(), new Parameter("", ae),
                new Parameter("", mu), c, s, 4, 4);
        expectedModel.setCentralTermContribution(false);
        
        // Acceleration
        final Vector3D actual = forceModel.computeAcceleration(state);
        final Vector3D expected = expectedModel.computeNonCentralTermsAcceleration(state.getPVCoordinates().getPosition(),
            state.getDate()).scalarMultiply(5.);
        Assert.assertEquals(expected, actual);
        // Partial derivatives
        final double[][] dAccdPosActual = new double[3][3];
        forceModel.addDAccDState(state.getPVCoordinates().getPosition(), state.getFrame(), state.getDate(), dAccdPosActual);
        final double[][] dAccdPosExpected = expectedModel.computeDAccDPos(state.getPVCoordinates().getPosition(),
            state.getDate());
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(dAccdPosActual[i][j], dAccdPosExpected[i][j] * 5., 0.);
            }
        }
        // K value
        Assert.assertEquals(5., forceModel.getMultiplicativeFactor(), 0.);
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws PatriusException, IOException,
            ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");

        // Gravity
        GravityFieldFactory
                .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();

        final double[][] c = data.getC(6, 6, false);
        final double[][] s = data.getS(6, 6, false);
        final Frame itrf = FramesFactory.getITRF();

        final BalminoGravityModel forceModel = new BalminoGravityModel(itrf, this.ae,
                this.mu, c, s);
    }
}
