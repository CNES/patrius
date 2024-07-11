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
* VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2900:15/11/2021:[PATRIUS] Possibilite de desactiver les eclipses pour la SRP 
* VERSION:4.8:DM:DM-2899:15/11/2021:[PATRIUS] Autres corps occultants que la Terre pour la SRP 
* VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.7:FA:FA-2897:18/05/2021:Alignement Soleil-Sat-Terre non supporté pour la SRP 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:439:12/06/2015:Corrected partial derivatives computation for PRS
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class SolarRadiationPressureCircularTest {

    /** Parameter name for absorption coefficient. */
    public static final String ABSORPTION_COEFFICIENT = "absorption coefficient";

    /** Parameter name for reflection coefficient. */
    public static final String SPECULAR_COEFFICIENT = "specular reflection coefficient";

    /** Parameter name for diffusion coefficient. */
    public static final String DIFFUSION_COEFFICIENT = "diffusion reflection coefficient";

    /** ka. */
    private static final Parameter KA = new Parameter(ABSORPTION_COEFFICIENT, 0.);

    /** List of the parameters names. */
    private static final ArrayList<Parameter> parameters;
    static {
        parameters = new ArrayList<Parameter>();
        parameters.add(KA);
        parameters.add(new Parameter(SPECULAR_COEFFICIENT, 0.));
        parameters.add(new Parameter(DIFFUSION_COEFFICIENT, 0.));
    }

    /** Default name for SimpleMassModel. */
    private static final String DEFAULT = "default";

    /**
     * Test SRP with occulting body different from Earth (here Moon). Two cases are tested:
     * <ul>
     * <li>Occulting body = Moon, state centered on Moon, state hidden behind Moon</li>
     * <li>Occulting body = Moon, state centered on Earth, state hidden behind Moon</li>
     * </ul>
     */
    @Test
    public void testOtherOccultingBody() throws PatriusException {
        // Initialization with occulting body = Moon
        final CelestialBody sun  = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final SolarRadiationPressureCircular srp = new SolarRadiationPressureCircular(sun,
            1500000, moon.getInertialEquatorFrame(), new SphericalSpacecraft(50.0, 0.5, 1, 0., 0.,
                DEFAULT), false);
        final MassProvider massModel = new SimpleMassModel(1500., DEFAULT);

        // Case 1: occulting body = Moon, state centered on Moon, state hidden behind Moon: acc = 0
        final Vector3D pos1 = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, moon.getInertialEquatorFrame()).getPosition().scalarMultiply(-0.001);
        final SpacecraftState state1 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos1, Vector3D.ZERO), moon.getInertialEquatorFrame(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU), massModel);
        final Vector3D actualAcc = srp.computeAcceleration(state1);
        Assert.assertEquals(0, actualAcc.getNorm(), 0.);
        
        // Case 2: occulting body = Moon, state centered on Earth, state hidden behind Moon: acc = 0
        final Vector3D pos2 = moon.getInertialEquatorFrame().getTransformTo(FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH).transformPosition(pos1);
        final SpacecraftState state2 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos2, Vector3D.ZERO), FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU), massModel);
        final Vector3D actualAcc2 = srp.computeAcceleration(state2);
        Assert.assertEquals(0, actualAcc2.getNorm(), 0.);
    }

    /**
     * Test SRP with multiple occulting bodies. 4 cases are tested:
     * <ul>
     * <li>Satellite behind first body</li>
     * <li>Satellite behind second body</li>
     * <li>Satellite not behind any body</li>
     * <li>Satellite behind first and second body</li>
     * </ul>
     */
    @Test
    public void testMultipleOccultingBodies() throws PatriusException {
        // Case 1, 2, 3: two separate occulting bodies, 3 configuration:
        // - Satellite behind first body
        // - Satellite behind second body
        // - Satellite not behind any body
        // Initialization with occulting body = Moon
        final CelestialBody sun  = CelestialBodyFactory.getSun();
        final CelestialBody body1 = CelestialBodyFactory.getEarth();
        final CelestialBody body2 = CelestialBodyFactory.getMoon();
        final SolarRadiationPressureCircular srp = new SolarRadiationPressureCircular(sun,
            6378000, new SphericalSpacecraft(50.0, 0.5, 1, 0., 0.,
                DEFAULT));
        srp.addOccultingBody(1500000, body2.getInertialEquatorFrame());
        final MassProvider massModel = new SimpleMassModel(1500., DEFAULT);

        // Case 1: state behind first body: acc = 0
        final Vector3D pos1 = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, body1.getInertialEquatorFrame()).getPosition().scalarMultiply(-0.001);
        final SpacecraftState state1 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos1, Vector3D.ZERO), body1.getInertialEquatorFrame(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU), massModel);
        final Vector3D actualAcc = srp.computeAcceleration(state1);
        Assert.assertEquals(0, actualAcc.getNorm(), 0.);
        
        // Case 2: state behind second body: acc = 0
        final Vector3D pos2 = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, body2.getInertialEquatorFrame()).getPosition().scalarMultiply(-0.001);
        final SpacecraftState state2 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos2, Vector3D.ZERO), body2.getInertialEquatorFrame(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU), massModel);
        final Vector3D actualAcc2 = srp.computeAcceleration(state2);
        Assert.assertEquals(0, actualAcc2.getNorm(), 0.);
        
        // Case 3: state not behind any body: acc != 0
        final Vector3D pos3 = Vector3D.PLUS_I.scalarMultiply(1E8);
        final SpacecraftState state3 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos3, Vector3D.ZERO), body1.getInertialEquatorFrame(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU), massModel);
        final Vector3D actualAcc3 = srp.computeAcceleration(state3);
        Assert.assertFalse(actualAcc3.getNorm() == 0);
        
        // Case 4: state behind two bodies (same body): acc = 0
        final CelestialBody body3 = CelestialBodyFactory.getEarth();
        final SolarRadiationPressureCircular srp2 = new SolarRadiationPressureCircular(sun,
                6378000, new SphericalSpacecraft(50.0, 0.5, 1, 0., 0.,
                    DEFAULT));
        srp2.addOccultingBody(6378000, body3.getInertialEquatorFrame());

        final Vector3D pos4 = sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, body1.getInertialEquatorFrame()).getPosition().scalarMultiply(-0.001);
        final SpacecraftState state4 = new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos4, Vector3D.ZERO), body1.getInertialEquatorFrame(), AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU), massModel);
        final Vector3D actualAcc4 = srp2.computeAcceleration(state4);
        Assert.assertEquals(0, actualAcc4.getNorm(), 0.);        
    }

    @Test
    public void testLightning() throws PatriusException {
        // Initialization

        final SolarRadiationPressureCircular SRP = new SolarRadiationPressureCircular(this.sun,
            this.earth.getEquatorialRadius(), new SphericalSpacecraft(50.0, 0.5, 0.5, 0.5, 0.,
                DEFAULT));
        final SolarRadiationPressureCircular SRP2 = new SolarRadiationPressureCircular(this.sun,
                this.earth.getEquatorialRadius(), new SphericalSpacecraft(50.0, 0.5, 0.5, 0.5, 0.,
                    DEFAULT));
        SRP2.setEclipsesComputation(false);
        Assert.assertFalse(SRP2.isEclipseComputation());
        Assert.assertEquals(1, SRP.getParameters().size());

        final double period = 2 * FastMath.PI
            * MathLib.sqrt(this.orbit.getA() * this.orbit.getA() * this.orbit.getA() / this.orbit.getMu());
        Assert.assertEquals(86164, period, 1);

        // creation of the propagator
        final KeplerianPropagator k = new KeplerianPropagator(this.orbit);

        // intermediate variables
        AbsoluteDate currentDate;
        double changed = 1;
        int count = 0;

        for (int t = 1; t < 3 * period; t += 1000) {
            currentDate = this.date.shiftedBy(t);
            try {

                final double ratio = SRP.getLightningRatio(k.propagate(currentDate)
                    .getPVCoordinates().getPosition(), FramesFactory.getEME2000(), currentDate);
                final double ratio2 = SRP2.getLightningRatio(k.propagate(currentDate)
                        .getPVCoordinates().getPosition(), FramesFactory.getEME2000(), currentDate);

                if (MathLib.floor(ratio) != changed) {
                    changed = MathLib.floor(ratio);
                    if (changed == 0) {
                        count++;
                    }
                }
                
                // When eclipses are disabled, lightning ratio should always be one
                Assert.assertEquals(1., ratio2, 0.);
            } catch (final PatriusException e) {
                e.printStackTrace();
            }
        }
        Assert.assertTrue(3 == count);
    }

    /**
     * Check that lightning ratio in case of Earth exactly between Sun and satellite returns 0.
     */
    @Test
    public void testLightningAlignment() throws PatriusException {
        // Initialization
        final SolarRadiationPressureCircular srp = new SolarRadiationPressureCircular(this.sun,
            this.earth.getEquatorialRadius(), new SphericalSpacecraft(50.0, 0.5, 0.5, 0.5, 0.,
                DEFAULT));
        
        // Define aligned spacecraft (Earth exactly between Sun and satellite)
        final Vector3D position = this.sun.getPVCoordinates(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF()).getPosition().scalarMultiply(-1);

        // Check lightning ratio is equal to 0
        Assert.assertEquals(0, srp.getLightningRatio(position, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH), 0.);
    }

    @Test
    public void testRoughOrbitalModifs() throws ParseException, PatriusException,
                                        FileNotFoundException {

        // initialization
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
            new TimeComponents(13, 59, 27.816), TimeScalesFactory.getUTC());
        final Orbit orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3, MathLib.tan(0.001745329)
            * MathLib.cos(2 * FastMath.PI / 3), MathLib.tan(0.001745329)
            * MathLib.sin(2 * FastMath.PI / 3), 0.1, PositionAngle.TRUE,
            FramesFactory.getEME2000(), date, this.mu);
        final double period = orbit.getKeplerianPeriod();
        Assert.assertEquals(86164, period, 1);

        // creation of the force model
        final SolarRadiationPressureCircular SRP = new SolarRadiationPressureCircular(this.sun,
            this.earth.getEquatorialRadius(), new SphericalSpacecraft(500.0, 0.7, 0.7, 0.7, 0.,
                DEFAULT));

        // creation of the propagator
        final double[] absTolerance = { 0.1, 1.0e-9, 1.0e-9, 1.0e-5, 1.0e-5, 1.0e-5 };
        final double[] relTolerance = { 1.0e-4, 1.0e-4, 1.0e-4, 1.0e-6, 1.0e-6, 1.0e-6 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(900.0, 60000,
            absTolerance, relTolerance);
        integrator.setInitialStepSize(3600);
        final NumericalPropagator calc = new NumericalPropagator(integrator);
        calc.addForceModel(SRP);

        // Step Handler
        calc.setMasterMode(MathLib.floor(period), new SolarStepHandler());
        final AbsoluteDate finalDate = date.shiftedBy(10 * period);
        final MassProvider massModel = new SimpleMassModel(1500., "default");
        calc.setInitialState(new SpacecraftState(orbit, massModel));
        calc.setMassProviderEquation(massModel);
        calc.propagate(finalDate);
        Assert.assertTrue(calc.getCalls() < 7100);
    }

    public static void checkRadius(final double radius, final double min, final double max) {
        Assert.assertTrue(radius >= min);
        Assert.assertTrue(radius <= max);
    }

    // Test exception thrown in case of unsupported parameter (Kref)
    // test the addDAccDParam method of SolarRadiationPressure throws an exception.
    @Test(expected = PatriusException.class)
    public void testException() throws PatriusException {
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(10.0, 1.7, 0, 0, 0, DEFAULT);
        final Parameter kRef = new Parameter("toto", 1.);
        final SolarRadiationPressureCircular prs = new SolarRadiationPressureCircular(kRef, this.sun,
            this.earth.getEquatorialRadius(), spacecraft);
        prs.addDAccDParam(null, kRef, new double[] {});
        Assert.assertFalse(prs.supportsJacobianParameter(kRef));
    }

    // test the DPRSAccDParam and DPRSAccDState methods.
    @Test
    public void testDPRSAccDerivatives() throws PatriusException {
        // this class represents a mock RadiationSensitive object:
        class MockRadiationSensitive implements RadiationSensitive {

            @Override
            public Vector3D radiationPressureAcceleration(final SpacecraftState state,
                                                          final Vector3D flux) throws PatriusException {
                return Vector3D.ZERO;
            }

            @Override
            public void addDSRPAccDParam(final SpacecraftState s, final Parameter param,
                                         final double[] dAccdParam, final Vector3D satSunVector)
                                                                                                throws PatriusException {
                dAccdParam[0] = 1.0;
                dAccdParam[1] = 2.0;
                dAccdParam[2] = 3.0;
            }

            @Override
            public void addDSRPAccDState(final SpacecraftState s, final double[][] dAccdPos,
                                         final double[][] dAccdVel, final Vector3D satSunVector)
                                                                                                throws PatriusException {
                dAccdPos[0][0] = 1.0;
                dAccdPos[0][1] = 0.1;
                dAccdVel[0][0] = 1.0;
                dAccdVel[1][1] = 1.1;
                dAccdVel[2][1] = 2.1;
            }

            /** {@inheritDoc} */
            @Override
            public ArrayList<Parameter> getJacobianParameters() {
                // return parameters
                return parameters;
            }

        }

        final MockRadiationSensitive spacecraft = new MockRadiationSensitive();
        final SolarRadiationPressureCircular prs = new SolarRadiationPressureCircular(this.sun,
            this.earth.getEquatorialRadius(), spacecraft);
        final double[] dAccdParam = new double[3];
        prs.addDAccDParam(new SpacecraftState(this.orbit), KA, dAccdParam);
        Assert.assertTrue(KA.getName().contains(ABSORPTION_COEFFICIENT));
        Assert.assertEquals(2.0, dAccdParam[1] / dAccdParam[0], 0.0);
        Assert.assertEquals(3.0, dAccdParam[2] / dAccdParam[0], 0.0);

        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        prs.addDAccDState(new SpacecraftState(this.orbit), dAccdPos, dAccdVel);
        Assert.assertEquals(0.1, dAccdPos[0][1] / dAccdPos[0][0], 0.0);
        Assert.assertEquals(0.0, dAccdVel[0][0], 0.0);
        Assert.assertEquals(0.0, dAccdVel[2][1], 0.0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link SolarRadiationPressureCircular#SolarRadiationPressure(PVCoordinatesProvider, double, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressureCircular#SolarRadiationPressure(Parameter, PVCoordinatesProvider, double, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressureCircular#SolarRadiationPressure(double, double, PVCoordinatesProvider, double, RadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressureCircular#computeGradientPosition()}
     * @testedMethod {@link SolarRadiationPressureCircular#computeGradientVelocity()}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link SolarRadiationPressureCircular}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at
     *                   construction
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        // Instance
        final SphericalSpacecraft spacecraft = new SphericalSpacecraft(10.0, 1.7, 0, 0, 0, DEFAULT);
        final SolarRadiationPressureCircular prs = new SolarRadiationPressureCircular(
            new Parameter("toto", 1.), this.sun, this.earth.getEquatorialRadius(), FramesFactory.getGCRF(), spacecraft, false);
        final SolarRadiationPressureCircular prs2 = new SolarRadiationPressureCircular(this.sun,
            this.earth.getEquatorialRadius(), FramesFactory.getGCRF(), spacecraft, false);
        final SolarRadiationPressureCircular prs3 = new SolarRadiationPressureCircular(1, 1, this.sun,
            this.earth.getEquatorialRadius(), FramesFactory.getGCRF(), spacecraft, false);

        // Spacecraft state
        final Orbit orbit = new KeplerianOrbit(7E7, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Check partial derivatives are not computed
        Assert.assertFalse(prs.computeGradientPosition());
        Assert.assertFalse(prs.computeGradientVelocity());

        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        final double[][] dAccdPos2 = new double[3][3];
        final double[][] dAccdVel2 = new double[3][3];
        final double[][] dAccdPos3 = new double[3][3];
        final double[][] dAccdVel3 = new double[3][3];

        // Compute partial derivatives
        prs.addDAccDState(state, dAccdPos, dAccdVel);
        prs2.addDAccDState(state, dAccdPos2, dAccdVel2);
        prs3.addDAccDState(state, dAccdPos3, dAccdVel3);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdPos2[i][j], 0);
                Assert.assertEquals(0, dAccdPos3[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
                Assert.assertEquals(0, dAccdVel2[i][j], 0);
                Assert.assertEquals(0, dAccdVel3[i][j], 0);
            }
        }
    }

    /**
     * @throws PatriusException problem at the model creation
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
    public void testGetters() throws PatriusException {
        final RadiationSensitive spacecraftIn = new SphericalSpacecraft(500.0, 0.7, 0.7, 0.7, 0., DEFAULT);
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        final SolarRadiationPressureCircular model = new SolarRadiationPressureCircular(new Parameter(
            "k", 2), sun, Constants.CNES_STELA_AE, spacecraftIn);
        Assert.assertEquals(2, model.getkRefValue(), 0);
        Assert.assertEquals(
            CelestialBodyFactory.getSun()
                .getPVCoordinates(AbsoluteDate.J2000_EPOCH, FramesFactory.getICRF())
                .getPosition().getNorm(),
            model.getSun().getPVCoordinates(AbsoluteDate.J2000_EPOCH, FramesFactory.getICRF())
                .getPosition().getNorm(), 0);
        Assert.assertEquals(Constants.CNES_STELA_AE, model.getEquatorialRadius().get(0), 0);
    }

    /**
     * @throws PatriusException
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
    public void testEnrichParameterDescriptors() throws PatriusException {
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final SolarRadiationPressureCircular forceModel = new SolarRadiationPressureCircular(sun,
                1500000, moon.getInertialEquatorFrame(), new SphericalSpacecraft(50.0, 0.5, 1,
                        0., 0., DEFAULT), false);

        // Check that the force model has some parameters (otherwise this test isn't needed and the
        // enrichParameterDescriptors method shouldn't be called in the force model)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }
    }

    private final double mu = 3.98600E14;

    /** The Sun. */
    private PVCoordinatesProvider sun;

    /** The Earth. */
    private OneAxisEllipsoid earth;

    /** The orbit. */
    private Orbit orbit;

    /** The date. */
    private AbsoluteDate date;

    private static class SolarStepHandler implements PatriusFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = -2346826010279512941L;

        private SolarStepHandler() {
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        @Override
        public void handleStep(final SpacecraftState currentState, final boolean isLast) {
            final double dex = currentState.getEquinoctialEx() - 0.01071166;
            final double dey = currentState.getEquinoctialEy() - 0.00654848;
            final double alpha = MathLib.toDegrees(MathLib.atan2(dey, dex));
            Assert.assertTrue(alpha > 100.0);
            Assert.assertTrue(alpha < 112.0);
            checkRadius(MathLib.sqrt(dex * dex + dey * dey), 0.003524, 0.003541);
        }

    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");

        this.sun = CelestialBodyFactory.getSun();

        this.earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765, FramesFactory.getITRF());

        this.date = new AbsoluteDate(new DateComponents(1970, 3, 21),
            new TimeComponents(13, 59, 27.816), TimeScalesFactory.getUTC());
        this.orbit = new EquinoctialOrbit(42164000, 10e-3, 10e-3, MathLib.tan(0.001745329)
            * MathLib.cos(2 * FastMath.PI / 3), MathLib.tan(0.001745329)
            * MathLib.sin(2 * FastMath.PI / 3), 0.1, PositionAngle.TRUE,
            FramesFactory.getEME2000(), this.date, this.mu);
    }

}
