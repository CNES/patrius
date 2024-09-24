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
 * @history creation 12/03/2012
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.tides.TerrestrialTides;
import fr.cnes.sirius.patrius.frames.FactoryManagedFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Test class for the radiation pressure model.
 *              </p>
 * 
 * @author Gerald Mercadier
 * 
 * @version $Id: RediffusedRadiationPressureTest.java 17911 2017-09-11 12:02:31Z
 *          bignon $
 * 
 * @since 1.1
 * 
 */
public class RediffusedRadiationPressureTest {

    /** Parameter name for K0 albedo global coefficient. */
    public static final String K0ALBEDO_COEFFICIENT = "K0 albedo coefficient";
    /** Parameter name for K0 infrared global coefficient. */
    public static final String K0IR_COEFFICIENT = "K0 infrared coefficient";
    /** Parameter name for absorption coefficient. */
    public static final String ABSORPTION_COEFFICIENT = "absorption coefficient";
    /** Parameter name for specular coefficient. */
    public static final String SPECULAR_COEFFICIENT = "specular coefficient";
    /** Parameter name for diffusion coefficient. */
    public static final String DIFFUSION_COEFFICIENT = "diffusion coefficient";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Radiative model.
         * 
         * @featureDescription Computation of the rediffused radiation pressure
         *                     acceleration.
         * 
         * @coveredRequirements DV-VEHICULE_410, DV-VEHICULE_430, DV-MOD_280
         */
        REDIFFUSED_RADIATIVE_MODEL
    }

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#RADIATIVE_MODEL}
     * 
     * @testedMethod {@link DragForce#getParametersNames()}
     * @testedMethod {@link DragForce#getParameter(String)}
     * @testedMethod {@link DragForce#setParameter()}
     * 
     * @description Test for the parameters
     * 
     * @input a parameter
     * 
     * @output its value
     * 
     * @testPassCriteria the parameter value is as expected exactly (0 ulp
     *                   difference)
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testParamList() throws PatriusException {

        // radiative model
        final IEmissivityModel model = new KnockeRiesModel();
        final FactoryManagedFrame itrfFrame = FramesFactory.getITRF();
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final RediffusedRadiativeTestModel radiativeModel = new RediffusedRadiativeTestModel(
            true, true, 1, 1);
        final RediffusedRadiationPressure prs = new RediffusedRadiationPressure(
            sun, itrfFrame, 15, 5, model, radiativeModel);

        Assert.assertEquals(5, prs.getParameters().size());
        Assert.assertFalse(prs.supportsJacobianParameter(new Parameter("toto",
            0.)));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiationPressure#computeAcceleration(SpacecraftState)}
     * @testedMethod {@link RediffusedRadiationPressure#getEventsDetectors()}
     * 
     * @description testing the redistributed radiation pressure acceleration.
     * 
     * @input rediffused radiative model.
     * 
     * @output the redistributed radiation pressure acceleration.
     * 
     * @testPassCriteria the computed acceleration is the expected one.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void radiationPressureAccelerationTest() {

        /**
         * Test on a model with a sphere and one facet
         */
        try {
            // radiative model
            final IEmissivityModel model = new KnockeRiesModel();
            final FactoryManagedFrame itrfFrame = FramesFactory.getITRF();
            final CelestialBody sun = CelestialBodyFactory.getSun();
            final RediffusedRadiativeTestModel radiativeModel = new RediffusedRadiativeTestModel(
                true, true, 1, 1);
            final RediffusedRadiationPressure r = new RediffusedRadiationPressure(
                sun, itrfFrame, 15, 5, model, radiativeModel);

            // spacecraft
            final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0,
                TimeScalesFactory.getTAI());
            // mu from grim4s4_gr model
            final double mu = 0.39860043770442e+15;
            // GCRF reference frame
            final Frame referenceFrame = FramesFactory.getGCRF();
            // pos-vel
            final Vector3D pos = new Vector3D(4.05228560172917172e+07,
                -1.17844795966431592e+07, -6.58338151580381091e+05);
            final Vector3D vel = new Vector3D(8.57448611492193891e+02,
                2.94919910671677371e+03, -4.06888496702080431e+01);
            final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
            final Orbit orbit = new CartesianOrbit(pvCoordinates,
                referenceFrame, date, mu);
            // creation of the spacecraft with no attitude
            final SpacecraftState spacecraftState = new SpacecraftState(orbit,
                new Attitude(date, referenceFrame, Rotation.IDENTITY,
                    Vector3D.ZERO));

            // compute radiation pressure acceleration
            final Vector3D computedAcc = r.computeAcceleration(spacecraftState);
            Assert.assertEquals(76.e-10, computedAcc.getX(),
                Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(76.e-10, computedAcc.getY(),
                Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(76.e-10, computedAcc.getZ(),
                Precision.DOUBLE_COMPARISON_EPSILON);

            r.getEventsDetectors();

            final double[][] dAccdPos = new double[3][3];
            final double[][] dAccdVel = new double[3][3];
            final double[] dAccdParam = new double[3];

            r.addDAccDState(spacecraftState, dAccdPos, dAccdVel);
            Assert.assertEquals(1., dAccdPos[0][0],
                Precision.DOUBLE_COMPARISON_EPSILON);
            Assert.assertEquals(0., dAccdVel[0][0],
                Precision.DOUBLE_COMPARISON_EPSILON);

            r.addDAccDParam(spacecraftState, radiativeModel.k0Albedo,
                dAccdParam);
            Assert.assertEquals(0., dAccdParam[0],
                Precision.DOUBLE_COMPARISON_EPSILON);

            // call the methods with a different SpacecraftState, for coverage
            // purposes:
            final Orbit orbit2 = new CartesianOrbit(pvCoordinates,
                referenceFrame, date, mu);
            final SpacecraftState scs = new SpacecraftState(orbit2);
            r.addDAccDState(scs, dAccdPos, dAccdVel);

            final Orbit orbit3 = new CartesianOrbit(pvCoordinates,
                referenceFrame, date.shiftedBy(0.5), mu);
            final SpacecraftState scs3 = new SpacecraftState(orbit3);
            r.addDAccDParam(scs3, radiativeModel.k0Albedo, dAccdParam);

            // expected force on the sphere
        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link TerrestrialTides#TerrestrialTides(fr.cnes.sirius.patrius.frames.Frame, double, double)}
     * 
     * @description add force contribution to the numerical propagator
     * 
     * @input corrections related to terrestrial tides (third body attraction on
     *        an anelastic crust up to degree 2 and 3, frequential correction of
     *        Love numbers, ellipticity correction).
     * 
     * @output each corrective component due to terrestrial tides
     * 
     * @testPassCriteria the tested methods run with no error.
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testContrib() throws PatriusException {

        // date 20153 1320
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 06, 00, 22, 0.0,
            TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = date.shiftedBy(Constants.JULIAN_DAY);

        // mu from grim4s4_gr GRGS file (Earth)
        final double muEarth = 3.9860043770442E+14;

        // radiative model
        final IEmissivityModel model = new KnockeRiesModel();
        final FactoryManagedFrame itrfFrame = FramesFactory.getITRF();
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final RediffusedRadiativeTestModel radiativeModelAlb = new RediffusedRadiativeTestModel(
            true, false, 1, 1);
        final RediffusedRadiativeTestModel radiativeModelIR = new RediffusedRadiativeTestModel(
            false, true, 1, 1);
        final RediffusedRadiationPressure rIR = new RediffusedRadiationPressure(
            sun, itrfFrame, 15, 5, model, radiativeModelIR);
        final RediffusedRadiationPressure rAlb = new RediffusedRadiationPressure(
            sun, itrfFrame, 15, 5, model, radiativeModelAlb);

        // test addContribution method
        final PVCoordinates pv = new PVCoordinates(new Vector3D(
            2.70303160815657163e+06, 6.15588486808402184e+06,
            -1.16119700511837618e+04), new Vector3D(
            -7.06109645777311016e+03, 3.08016738885103905e+03,
            1.36108059143140654e+01));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(),
            date, muEarth);
        final SpacecraftState scr = new SpacecraftState(orbit);

        final NumericalPropagator calc = new NumericalPropagator(
            new GraggBulirschStoerIntegrator(10.0, 30.0, 0, 1.0e-5));
        calc.addForceModel(rAlb);
        calc.addForceModel(rIR);

        calc.setInitialState(scr);
        calc.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(scr.getMu())));
        calc.propagate(finalDate);
        final PVCoordinates finalPV = calc.getPVCoordinates(finalDate,
            FramesFactory.getGCRF());
        final double[] actualPV = { finalPV.getPosition().getX(),
            finalPV.getPosition().getY(), finalPV.getPosition().getZ() };

        // OREKIT results
        final double[] expectedPV = { 5686316.720630774, -3631643.594037052,
            -9956.098649910384 };

        Assert.assertArrayEquals(expectedPV, actualPV, 3E-6);

        Assert.assertTrue(rIR.getParameters().size() == 5);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#REDIFFUSED_RADIATIVE_MODEL}
     * 
     * @testedMethod {@link RediffusedRadiationPressure#addDAccDParam(SpacecraftState, Parameter, double[])
     *               )}
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
    public void testUnsupportedParam() throws PatriusException {
        final double[] dAccdParam = new double[3];
        // date 20153 1320
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 06, 00, 22, 0.0,
            TimeScalesFactory.getTAI());

        // mu from grim4s4_gr GRGS file (Earth)
        final double muEarth = 3.9860043770442E+14;

        // radiative model
        final IEmissivityModel model = new KnockeRiesModel();
        final FactoryManagedFrame itrfFrame = FramesFactory.getITRF();
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final RediffusedRadiativeTestModel radiativeModelIR = new RediffusedRadiativeTestModel(
            false, true, 1, 1);
        final RediffusedRadiationPressure rIR = new RediffusedRadiationPressure(
            sun, itrfFrame, 15, 5, model, radiativeModelIR);

        final PVCoordinates pv = new PVCoordinates(new Vector3D(
            2.70303160815657163e+06, 6.15588486808402184e+06,
            -1.16119700511837618e+04), new Vector3D(
            -7.06109645777311016e+03, 3.08016738885103905e+03,
            1.36108059143140654e+01));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(),
            date, muEarth);
        final SpacecraftState scr = new SpacecraftState(orbit);

        // call method
        rIR.addDAccDParam(scr, new Parameter("toto", 1), dAccdParam);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link RediffusedRadiationPressure#RediffusedRadiationPressure(CelestialBody, Frame, int, int, IEmissivityModel, RediffusedRadiationSensitive, boolean)}
     * @testedMethod {@link SolarRadiationPressure#computeGradientPosition()}
     * @testedMethod {@link SolarRadiationPressure#computeGradientVelocity()}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link SolarRadiationPressure}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation
     *                   is deactivated at construction
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        // Instance
        final IEmissivityModel model = new KnockeRiesModel();
        final FactoryManagedFrame itrfFrame = FramesFactory.getITRF();
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final RediffusedRadiativeTestModel radiativeModel = new RediffusedRadiativeTestModel(
            true, true, 1, 1);
        final RediffusedRadiationPressure prs = new RediffusedRadiationPressure(
            sun, itrfFrame, 15, 5, model, radiativeModel, false);

        // Spacecraft state
        final Orbit orbit = new KeplerianOrbit(7E7, 0, 0, 0, 0, 0,
            PositionAngle.TRUE, FramesFactory.getGCRF(),
            AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Check partial derivatives are not computed
        Assert.assertFalse(prs.computeGradientPosition());
        Assert.assertFalse(prs.computeGradientVelocity());

        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];

        // Compute partial derivatives
        prs.addDAccDState(state, dAccdPos, dAccdVel);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
            }
        }
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() throws PatriusException {

        final IEmissivityModel model = new KnockeRiesModel();
        final RediffusedRadiativeTestModel radiativeModel = new RediffusedRadiativeTestModel(
            true, true, 1, 1);
        final RediffusedRadiationPressure otherInstance = new RediffusedRadiationPressure(
            CelestialBodyFactory.getSun(), FramesFactory.getGCRF(), 0, 0,
            model, radiativeModel);

        final AssemblyBuilder builder = new AssemblyBuilder();
        final String mainPart = "Satellite";
        final String array1 = "array1";
        final String array2 = "array2";
        builder.addMainPart(mainPart);
        builder.addPart(array1, mainPart, new Transform(
            AbsoluteDate.J2000_EPOCH, new Rotation(Vector3D.PLUS_I, 0.25)));
        builder.addPart(array2, mainPart, new Transform(
            AbsoluteDate.J2000_EPOCH, new Rotation(Vector3D.PLUS_K, 0.18)));
        builder.addProperty(new MassProperty(10), array1);

        // adding of a simple mass property
        final SolidShape sphere = new Sphere(Vector3D.ZERO, 1);
        final IPartProperty shapeProp = new GeometricProperty(sphere);
        builder.addProperty(shapeProp, mainPart);

        final RediffusedRadiationPressure rediffusedRadPressure = new RediffusedRadiationPressure(
            otherInstance, builder.returnAssembly());

        Assert.assertEquals(CelestialBodyFactory.getSun().getName(),
            rediffusedRadPressure.getInSun().getName());
        Assert.assertEquals("Satellite", rediffusedRadPressure.getAssembly()
            .getMainPart().getName());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws PatriusException {
        final IEmissivityModel model = new KnockeRiesModel();
        final FactoryManagedFrame itrfFrame = FramesFactory.getITRF();
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final RediffusedRadiativeTestModel radiativeModel = new RediffusedRadiativeTestModel(true,
                true, 1, 1);
        final RediffusedRadiationPressure forceModel = new RediffusedRadiationPressure(sun,
                itrfFrame, 15, 5, model, radiativeModel);

        // Check that the force model has some parameters
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL, RediffusedRadiationPressure.class));
        }
    }

    /**
     * General set up method.
     * 
     * @throws PatriusException
     *         if an Orekit error occurs
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataCNES-2003");
    }
}
