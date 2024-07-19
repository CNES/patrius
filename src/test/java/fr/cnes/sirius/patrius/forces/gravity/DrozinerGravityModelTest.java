/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
* VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
* VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
* VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
* VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
* VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
* VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Renamed Droziner to UnnormalizedDroziner
 * VERSION::FA:93:01/04/2014:changed partial derivatives API
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1267:09/03/2018: Addition of getters for C and CS tables
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.IOException;
import java.text.ParseException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressure;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class DrozinerGravityModelTest {

    // rough test to determine if J2 alone creates heliosynchronism
    @Test
    public void testHelioSynchronous() throws PatriusException {

        // initialization
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
            new TimeComponents(13, 59, 27.816), TimeScalesFactory.getUTC());
        final Transform itrfToEME2000 = this.ITRF2005.getTransformTo(FramesFactory.getEME2000(), date);
        final Vector3D pole = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        final Frame poleAligned = new Frame(FramesFactory.getEME2000(), new Transform(date,
            new Rotation(Vector3D.PLUS_K, pole)), "pole aligned", true);

        final double i = MathLib.toRadians(98.7);
        final double omega = MathLib.toRadians(93.0);
        final double OMEGA = MathLib.toRadians(15.0 * 22.5);
        final Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i, omega, OMEGA, 0,
            PositionAngle.MEAN, poleAligned, date, this.mu);

        this.propagator.addForceModel(new DirectBodyAttraction(new DrozinerGravityModel(this.ITRF2005, 6378136.460,
            this.mu, new double[][] { { 0.0 }, { 0.0 }, { this.c20 } }, new double[][] { { 0.0 }, { 0.0 },
                { 0.0 } })));

        // let the step handler perform the test
        this.propagator.setMasterMode(Constants.JULIAN_DAY, new SpotStepHandler());
        this.propagator.setInitialState(new SpacecraftState(orbit));
        this.propagator.propagate(date.shiftedBy(7 * Constants.JULIAN_DAY));
        Assert.assertTrue(this.propagator.getCalls() < 9200);

        // coverage tests:
        double[][] c = new double[3][1];
        double[][] s = new double[3][1];
        final DrozinerGravityModel model = new DrozinerGravityModel(this.ITRF2005, this.aeParam,
            this.muParam, c, s);
        this.muParam.setValue(5.);
        Assert.assertEquals(5., model.getMu(), 0.0);

        final PVCoordinates pv = new PVCoordinates(new Vector3D(0., 0., 8000000), Vector3D.ZERO);
        boolean rez = false;
        try {
            model.computeNonCentralTermsAcceleration(
                ITRF2005.getTransformTo(poleAligned, date).transformPosition(pv.getPosition()), date);
        } catch (final PatriusException e) {
            rez = true;
        }
        c = new double[1][0];
        s = new double[3][1];

        rez = false;
        try {
            new DrozinerGravityModel(this.ITRF2005, 6378136.460, this.mu, c, s);
        } catch (final IllegalArgumentException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // testing the second method
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06,
            -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03,
            -1.14097953925384523e+01);
        final SpacecraftState spacecraftState = new SpacecraftState(new CartesianOrbit(
            new PVCoordinates(pos, vel), FramesFactory.getGCRF(), new AbsoluteDate(2005, 3, 5,
                0, 24, 0.0, TimeScalesFactory.getTAI()), model.getMu()));
        try {
            model.computeNonCentralTermsAcceleration(ITRF2005.getTransformTo(spacecraftState.getFrame(), date)
                .transformPosition(spacecraftState.getPVCoordinates().getPosition()),
                spacecraftState.getDate());
        } catch (final PatriusException e) {
            rez = true;
        }

    }

    private static class SpotStepHandler implements PatriusFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = -3917769828973243346L;

        public SpotStepHandler() throws PatriusException {
            this.sun = CelestialBodyFactory.getSun();
            this.previous = Double.NaN;
        }

        private final PVCoordinatesProvider sun;
        private double previous;

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // nothing to do
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast)
            throws PropagationException {

            final AbsoluteDate current = currentState.getDate();
            Vector3D sunPos;
            try {
                sunPos = this.sun.getPVCoordinates(current, FramesFactory.getEME2000()).getPosition();
            } catch (final PatriusException e) {
                throw new PropagationException(e);
            }
            final Vector3D normal = currentState.getPVCoordinates().getMomentum();
            final double angle = Vector3D.angle(sunPos, normal);
            if (!Double.isNaN(this.previous)) {
                Assert.assertEquals(this.previous, angle, 0.0013);
            }
            this.previous = angle;
        }

    }

    // test the difference with the analytical extrapolator Eckstein Hechler
    @Test
    public void testEcksteinHechlerReference() throws PatriusException {

        // Definition of initial conditions with position and velocity
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        final Transform itrfToEME2000 = this.ITRF2005.getTransformTo(FramesFactory.getEME2000(), date);
        final Vector3D pole = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        final Frame poleAligned = new Frame(FramesFactory.getEME2000(), new Transform(date,
            new Rotation(Vector3D.PLUS_K, pole)), "pole aligned", true);

        final Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            poleAligned, date, this.mu);

        final SpacecraftState initialState = new SpacecraftState(initialOrbit);
        
        this.propagator = new NumericalPropagator(this.integrator, initialState.getFrame());
        this.propagator.addForceModel(new DirectBodyAttraction(new DrozinerGravityModel(this.ITRF2005, this.ae,
            this.mu, new double[][] { { 1.0 }, { 0.0 }, { this.c20 }, { this.c30 }, { this.c40 }, { this.c50 },
                { this.c60 }, }, new double[][] { { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 }, })));

        // let the step handler perform the test
        this.propagator.setMasterMode(20, new EckStepHandler(initialOrbit, this.ae, this.mu, this.c20, this.c30,
            this.c40, this.c50,
            this.c60));
        this.propagator.setInitialState(initialState);
        this.propagator.propagate(date.shiftedBy(50000));
        Assert.assertTrue(this.propagator.getCalls() < 1300);

    }

    private static class EckStepHandler implements PatriusFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = -7974453505641400294L;

        private EckStepHandler(final Orbit initialOrbit, final double ae, final double mu,
                               final double c20, final double c30, final double c40, final double c50,
                               final double c60) throws PatriusException {
            this.referencePropagator = new EcksteinHechlerPropagator(initialOrbit, ae, mu,
                initialOrbit.getFrame(), c20, c30, c40, c50, c60, ParametersType.OSCULATING);
        }

        private final EcksteinHechlerPropagator referencePropagator;

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            // nothing to do
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            try {

                final SpacecraftState EHPOrbit = this.referencePropagator.propagate(currentState
                    .getDate());
                final Vector3D posEHP = EHPOrbit.getPVCoordinates().getPosition();
                final Vector3D posDROZ = currentState.getPVCoordinates().getPosition();
                final Vector3D velEHP = EHPOrbit.getPVCoordinates().getVelocity();
                final Vector3D dif = posEHP.subtract(posDROZ);

                final Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
                final Vector3D W = EHPOrbit.getPVCoordinates().getMomentum().normalize();
                final Vector3D N = Vector3D.crossProduct(W, T);

                Assert.assertTrue(dif.getNorm() < 111);
                Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(dif, T)) < 111);
                Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(dif, N)) < 54);
                Assert.assertTrue(MathLib.abs(Vector3D.dotProduct(dif, W)) < 12);

            } catch (final PropagationException e) {
                e.printStackTrace();
            }

        }

    }

    // test the difference with the Cunningham model
    @Test
    public void testTesserealWithCunninghamReference() throws PatriusException {
        // initialization
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
            new TimeComponents(13, 59, 27.816), TimeScalesFactory.getUTC());
        final double i = MathLib.toRadians(98.7);
        final double omega = MathLib.toRadians(93.0);
        final double OMEGA = MathLib.toRadians(15.0 * 22.5);
        final Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i, omega, OMEGA, 0,
            PositionAngle.MEAN, FramesFactory.getEME2000(), date, this.mu);
        this.propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(100));
        this.propagator.addForceModel(new DirectBodyAttraction(new CunninghamGravityModel(this.ITRF2005, this.ae,
            this.mu, this.C, this.S)));
        this.propagator.setInitialState(new SpacecraftState(orbit));
        final SpacecraftState cunnOrb = this.propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        this.propagator.removeForceModels();
        this.propagator.addForceModel(new DirectBodyAttraction(new DrozinerGravityModel(this.ITRF2005, this.ae,
            this.mu, this.C, this.S)));

        this.propagator.setInitialState(new SpacecraftState(orbit));
        final SpacecraftState drozOrb = this.propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));

        final Vector3D dif = cunnOrb.getPVCoordinates().getPosition()
            .subtract(drozOrb.getPVCoordinates().getPosition());
        Assert.assertEquals(0, dif.getNorm(), 1.5e-6);
        Assert.assertTrue(this.propagator.getCalls() < 3500);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedMethod {@link SolarRadiationPressure#SolarRadiationPressure(PVCoordinatesProvider, BodyShape, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressure#SolarRadiationPressure(Parameter, PVCoordinatesProvider, BodyShape, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressure#SolarRadiationPressure(double, double, PVCoordinatesProvider, BodyShape, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressure#computeGradientPosition()}
     * @testedMethod {@link SolarRadiationPressure#computeGradientVelocity()}
     * @description compute acceleration partial derivatives wrt position
     * @input instances of {@link SolarRadiationPressure}
     * @output partial derivatives
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at
     *                   construction
     * @referenceVersion 3.2
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        // Instance
        final double[][] C = new double[3][3];
        final double[][] S = new double[3][3];
        final DrozinerGravityModel model = new DrozinerGravityModel(FramesFactory.getITRF(),
            6378000, Constants.EGM96_EARTH_MU, C, S);
        final DrozinerGravityModel model2 = new DrozinerGravityModel(FramesFactory.getITRF(),
            new Parameter("ae", 6378000), new Parameter("mu", Constants.EGM96_EARTH_MU), C, S);

        // Spacecraft state
        final Orbit orbit = new KeplerianOrbit(7E7, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
        SpacecraftState state = new SpacecraftState(orbit);
        state = state.addAdditionalState("gradient", new double[36]);

        // Compute partial derivatives
        final NumericalPropagator propagator = new NumericalPropagator(
            new ClassicalRungeKuttaIntegrator(30));
        propagator.addForceModel(new DirectBodyAttraction(model));
        propagator.addForceModel(new DirectBodyAttraction(model2));

        final PartialDerivativesEquations equations = new PartialDerivativesEquations("gradient",
            propagator);
        final TimeDerivativesEquations adder = new TimeDerivativesEquations(){
            /** Serializable UID. */
            private static final long serialVersionUID = 5663133068883693472L;

            @Override
            public void initDerivatives(final double[] yDot, final Orbit currentOrbit)
                throws PropagationException {
                // nothing to do
            }

            @Override
            public void addXYZAcceleration(final double x, final double y, final double z) {
                // nothing to do
            }

            @Override
            public void addAdditionalStateDerivative(final String name, final double[] pDot) {
                // Check all derivatives are null
                for (final double element : pDot) {
                    Assert.assertEquals(0., element, 0.);
                }
            }

            @Override
            public void addAcceleration(final Vector3D gamma, final Frame frame) throws PatriusException {
                // nothing to do
            }
        };
        equations.setInitialJacobians(state);
        equations.computeDerivatives(state, adder);
    }

    @Before
    public void setUp() throws PatriusException {
        this.ITRF2005 = null;
        this.propagator = null;
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        try {
            this.mu = 3.986004415e+14;
            this.muParam = new Parameter("mu", this.mu);
            this.ae = 6378136.460;
            this.aeParam = new Parameter("ae", this.ae);
            this.c20 = -1.08262631303e-3;
            this.c30 = 2.53248017972e-6;
            this.c40 = 1.61994537014e-6;
            this.c50 = 2.27888264414e-7;
            this.c60 = -5.40618601332e-7;
            this.ITRF2005 = FramesFactory.getITRF();
            final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
            final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
            this.integrator = new DormandPrince853Integrator(0.001,
                1000, absTolerance, relTolerance);
            this.integrator.setInitialStepSize(60);
            this.propagator = new NumericalPropagator(this.integrator);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
        this.ITRF2005 = null;
        this.propagator = null;
        this.integrator = null;
    }

    @Test
    public void testCSTables() {
        // Tabs creation
        final double[][] cCoefsT = new double[2][3];
        cCoefsT[0][0] = 9;
        cCoefsT[0][1] = 6;
        cCoefsT[1][0] = 4;
        cCoefsT[1][1] = 3;
        cCoefsT[0][2] = 10;
        cCoefsT[1][2] = 11;
        final double[][] sCoefsT = new double[2][3];
        sCoefsT[0][0] = 9;
        sCoefsT[0][1] = 3;
        sCoefsT[1][0] = 31;
        sCoefsT[1][1] = 1;
        sCoefsT[0][2] = 6;
        sCoefsT[1][2] = 2;

        final DrozinerGravityModel model = new DrozinerGravityModel(FramesFactory.getGCRF(),
            Constants.CNES_STELA_AE, Constants.CNES_STELA_MU, cCoefsT, sCoefsT);
        // Get values
        Assert.assertEquals(3, model.getC().length, 0);
        Assert.assertEquals(2, model.getC()[0].length, 0);
        Assert.assertEquals(4, model.getC()[0][1], 0);
        Assert.assertEquals(3, model.getC()[1][1], 0);
        Assert.assertEquals(6, model.getC()[1][0], 0);
        Assert.assertEquals(11, model.getC()[2][1], 0);

        Assert.assertEquals(2, model.getS()[0].length, 0);
        Assert.assertEquals(9, model.getS()[0][0], 0);
        Assert.assertEquals(3, model.getS()[1][0], 0);
        Assert.assertEquals(6, model.getS()[2][0], 0);
        Assert.assertEquals(1, model.getS()[1][1], 0);
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
        final DrozinerGravityModel model = new DrozinerGravityModel(frame, Constants.CNES_STELA_AE,
            Constants.CNES_STELA_MU, cCoefs, sCoefs);
        Assert.assertEquals(true, model.getBodyFrame().getName().equals(frame.getName()));

    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link DrozinerGravityModel#computeAcceleration(SpacecraftState)}
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
        final DrozinerGravityModel actualModel = new DrozinerGravityModel(FramesFactory.getGCRF(), new Parameter(
            "", ae), new Parameter("", mu), c, s);
        actualModel.setCentralTermContribution(false);
        final DirectBodyAttraction forceModel = new DirectBodyAttraction(actualModel);
        forceModel.setMultiplicativeFactor(5.);
        final Vector3D actual = forceModel.computeAcceleration(state);
        final Transform t = Transform.IDENTITY;
        final Vector3D expected = new DrozinerGravityModel(FramesFactory.getGCRF(), new Parameter("", ae),
            new Parameter("", mu), c, s).computeNonCentralTermsAcceleration(
            t.transformPosition(state.getPVCoordinates().getPosition()), state.getDate()).scalarMultiply(5.);
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(5., forceModel.getMultiplicativeFactor(), 0.);
    }

    /**
     * Test the partial derivatives computation: shall result in exceptions because Jacobian is not defined for Droziner
     * model.
     * 
     * @throws PatriusException when an error occurs
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testAddDAccDStateAndDParam() throws PatriusException, IOException, ParseException {

        Utils.setDataRoot("potential");

        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader(
            "GRGS_EIGEN_GL04S.txt", true));

        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        // Here, we get the data as extracted from the file.
        final int n = 100;
        final double[][] C = provider.getC(n, 100, false);
        final double[][] S = provider.getS(n, 100, false);

        Utils.setDataRoot("regular-data");
        final DrozinerGravityModel grav = new DrozinerGravityModel(FramesFactory.getITRF(), provider.getAe(),
            provider.getMu(), C, S);

        try {
            grav.computeDAccDPos(null, null);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected !
            Assert.assertEquals(PatriusMessages.UNAVAILABLE_JACOBIAN_FOR_DROZINER_MODEL.getSourceString(),
                e.getMessage());
        } catch (final RuntimeException e) {
            Assert.fail();
        }
    }

    private double mu;
    private double ae;
    private Parameter muParam;
    private Parameter aeParam;
    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;

    private final double[][] C = new double[][] {
        { 1.000000000000e+00 },
        { -1.863039013786e-09, -5.934448524722e-10 },
        { -1.082626313026e-03, -5.880684168557e-10, 5.454582196865e-06 },
        { 2.532480179720e-06, 5.372084926301e-06, 2.393880978120e-06, 1.908327022943e-06 },
        { 1.619945370141e-06, -1.608435522852e-06, 1.051465706331e-06, 2.972622682182e-06,
            -5.654946679590e-07 },
        { 2.278882644141e-07, -2.086346283172e-07, 2.162761961684e-06, -1.498655671702e-06,
            -9.794826452868e-07, 5.797035241535e-07 },
        { -5.406186013322e-07, -2.736882085330e-07, 1.754209863998e-07, 2.063640268613e-07,
            -3.101287736303e-07, -9.633248308263e-07, 3.414597413636e-08 } };
    private final double[][] S = new double[][] {
        { 0.000000000000e+00 },
        { 0.000000000000e+00, 1.953002572897e-10 },
        { 0.000000000000e+00, 3.277637296181e-09, -3.131184828481e-06 },
        { 0.000000000000e+00, 6.566367025901e-07, -1.637705321455e-06, 3.742073902553e-06 },
        { 0.000000000000e+00, -1.420694191113e-06, 1.987395414651e-06, -6.029325532200e-07,
            9.265045448070e-07 },
        { 0.000000000000e+00, -3.130219048314e-07, -1.072392243018e-06, -7.130099408898e-07,
            1.651623310985e-07, -2.220047616004e-06 },
        { 0.000000000000e+00, 9.562397128532e-08, -1.347688934659e-06, 3.220292843428e-08,
            -1.699735804354e-06, -1.934323349167e-06, -8.559943406892e-07 } };

    private Frame ITRF2005;
    private NumericalPropagator propagator;
    private AdaptiveStepsizeIntegrator integrator;

}
