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
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:1448:20/04/2018:PATRIUS 4.0 minor corrections
 * VERSION::FA:1449:15/03/2018:remove TankProperty name attribute
 * VERSION::FA:1871:16/10/2018: new massModel update
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.models.utils.AssemblySphericalSpacecraft;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AnomalyDetector;
import fr.cnes.sirius.patrius.propagation.events.DistanceDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.ExtremaDistanceDetector;
import fr.cnes.sirius.patrius.propagation.events.LatitudeDetector;
import fr.cnes.sirius.patrius.propagation.events.NadirSolarIncidenceDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Validation class for {@link MultiNumericalPropagator}. This class tests multi-proagation performance.
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: MultiNumericalPropagatorPerfoValTest.java 17431 2017-04-11 09:06:20Z rodrigues $
 * 
 * @since 3.0
 * 
 */
public class MultiNumericalPropagatorPerfoValTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Multi-sat numerical propagation performance
         * 
         * @featureDescription Test multi-sat numerical propagation performance
         * 
         * @coveredRequirements
         */
        PERFORMANCE
    }

    /**
     * @throws OrekitException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     *
     * @testedFeature {@link features#PERFORMANCE}
     *
     * @testedMethod {@link MultiNumericalPropagator#propagate(AbsoluteDate)}
     *
     * @description Test multi-sat numerical propagation performance. Included features are:
     *              <ul>
     *              <li>Propagation in ephemeris mode</li>
     *              <li>Full force models</li>
     *              <li>Maneuvers (constant thrust)</li>
     *              <li>Events</li>
     *              <li>Events'logger</li>
     *              <li>Various mu</li>
     *              <li>Assembly</li>
     *              </ul>
     * 
     * @input MultiNumericalPropagator and its single propagators counter-parts
     *
     * @output final states, propagation durations
     *
     * @testPassCriteria final states and ephemeris are exactly the same, propagation durations are
     *                   of the same order of magnitude
     *
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    /**
     * @throws OrekitException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPerformance() throws PatriusException, IOException, ParseException {

        // PRS and adaptive step-size integrator lead to different results (normal)
        // - PRS: because eclipse events lead to reinitializing state
        // - DOPRI integrator: because error computation depends on state vector size

        // ====================== Initialization ======================

        Utils.setDataRoot("regular-dataPBASE");
        final int nPropagators = 10;

        // Mass providers
        final MassProvider[] massProviders1 = new MassProvider[nPropagators];
        final MassProvider[] massProviders2 = new MassProvider[nPropagators];
        final TankProperty[] tank1 = new TankProperty[nPropagators];
        final TankProperty[] tank2 = new TankProperty[nPropagators];

        for (int i = 0; i < nPropagators; i++) {
            final AssemblyBuilder builder1 = new AssemblyBuilder();
            builder1.addMainPart("Main" + i);
            builder1.addProperty(new MassProperty(1000.), "Main" + i);
            tank1[i] = new TankProperty(1000.);
            builder1.addPart("Tank" + i, "Main" + i, Transform.IDENTITY);
            builder1.addProperty(tank1[i], "Tank" + i);
            final Assembly assembly1 = builder1.returnAssembly();
            massProviders1[i] = new MassModel(assembly1);

            final AssemblyBuilder builder2 = new AssemblyBuilder();
            builder2.addMainPart("Main" + i);
            builder2.addProperty(new MassProperty(1000.), "Main" + i);
            tank2[i] = new TankProperty(1000.);
            builder2.addPart("Tank" + i, "Main" + i, Transform.IDENTITY);
            builder2.addProperty(tank2[i], "Tank" + i);
            final Assembly assembly2 = builder2.returnAssembly();
            massProviders2[i] = new MassModel(assembly2);
        }

        final double intergrationStep = 30.;
        final NumericalPropagator[] propagators = new NumericalPropagator[nPropagators];
        for (int i = 0; i < nPropagators; i++) {
            // final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator
            // (minstep, maxstep, absTolerance, relTolerance);
            final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(
                intergrationStep);
            propagators[i] = new NumericalPropagator(integrator);
            propagators[i].setEphemerisMode();
        }

        // final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator
        // (minstep, maxstep, absTolerance, relTolerance);
        final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(
            intergrationStep);
        final MultiNumericalPropagator multiNumericalPropagator = new MultiNumericalPropagator(
            integrator);
        multiNumericalPropagator.setEphemerisMode();

        // Initial state
        final AbsoluteDate initialDate = new AbsoluteDate(2002, 01, 02, TimeScalesFactory.getTAI());
        final AttitudeProvider attitudeProvider = new ConstantAttitudeLaw(FramesFactory.getCIRF(),
            Rotation.IDENTITY);
        for (int i = 0; i < nPropagators; i++) {
            final double mu = Constants.EGM96_EARTH_MU + i * 1E10;
            final Orbit initialOrbit = new KeplerianOrbit(7000E3, 0.001, 1.5, 0, 0, i * 10,
                PositionAngle.MEAN, FramesFactory.getGCRF(), initialDate, mu);
            final SpacecraftState initialState1 = new SpacecraftState(initialOrbit,
                massProviders1[i]);
            final SpacecraftState initialState2 = new SpacecraftState(initialOrbit,
                massProviders2[i]);

            propagators[i].setInitialState(initialState1);
            multiNumericalPropagator.addInitialState(initialState2, "state" + i);

            propagators[i].setAttitudeProvider(attitudeProvider);
            propagators[i].setMassProviderEquation(massProviders1[i]);
            // propagators[i].setAdditionalStateTolerance("MASS_Mass" + i, new double[] { 1. },
            // new double[] { 1. });
            multiNumericalPropagator.setAttitudeProvider(attitudeProvider, "state" + i);
            multiNumericalPropagator.setMassProviderEquation(massProviders2[i], "state" + i);
            // multiNumericalPropagator.setAdditionalStateTolerance("Tank" + i,
            // new double[] { 1. }, new double[] { 1. }, "state" + i);
        }

        // Force models (some depends on spacecraft)
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
            FramesFactory.getITRF());

        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        final double[][] C = provider.getC(60, 60, false);
        final double[][] S = provider.getS(60, 60, false);
        final ForceModel earthPotential = new DrozinerAttractionModel(FramesFactory.getITRF(),
            provider.getAe(), provider.getMu(), C, S);

        final ForceModel sunAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getSun());
        final ForceModel moonAttraction = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());

        for (int i = 0; i < nPropagators; i++) {
            final AssemblySphericalSpacecraft spacecraft = new AssemblySphericalSpacecraft(10.,
                2.2, 0.3, 0., 0.);
            final DragForce drag = new DragForce(new SimpleExponentialAtmosphere(earth, 0.0004,
                42000.0, 7500.0), spacecraft);
            // final ForceModel prs = new SolarRadiationPressureCircular(
            // CelestialBodyFactory.getSun(), earth.getEquatorialRadius(), spacecraft);

            propagators[i].addForceModel(earthPotential);
            propagators[i].addForceModel(sunAttraction);
            propagators[i].addForceModel(moonAttraction);
            propagators[i].addForceModel(drag);
            // propagators[i].addForceModel(prs);

            multiNumericalPropagator.addForceModel(earthPotential, "state" + i);
            multiNumericalPropagator.addForceModel(sunAttraction, "state" + i);
            multiNumericalPropagator.addForceModel(moonAttraction, "state" + i);
            multiNumericalPropagator.addForceModel(drag, "state" + i);
            // multiNumericalPropagator.addForceModel(prs, "state" + i);
        }

        // Add maneuver
        for (int i = 0; i < nPropagators; i++) {
            final double isp = 100 + i;
            propagators[i].addForceModel(new ContinuousThrustManeuver(initialDate.shiftedBy(1000),
                1000., new PropulsiveProperty(10, isp), Vector3D.PLUS_I, massProviders1[i],
                tank1[i]));

            multiNumericalPropagator.addForceModel(
                new ContinuousThrustManeuver(initialDate.shiftedBy(1000), 1000.,
                    new PropulsiveProperty(10, isp), Vector3D.PLUS_I, massProviders2[i],
                    tank2[i]), "state" + i);
        }

        // Add events (and loggers)
        final CodedEventsLogger[] loggers = new CodedEventsLogger[nPropagators];
        final CodedEventsLogger logger = new CodedEventsLogger();
        final EventDetector d1 = new AnomalyDetector(PositionAngle.MEAN, 0., 100., 1E-6,
            Action.CONTINUE);
        final EventDetector d2 = new DistanceDetector(CelestialBodyFactory.getMoon(), 384000E3,
            100., 1E-6, Action.CONTINUE);
        final EventDetector d3 = new LatitudeDetector(0., earth, LatitudeDetector.UP, 100., 1E-6,
            Action.CONTINUE);
        final EventDetector d4 = new ExtremaDistanceDetector(CelestialBodyFactory.getMoon(),
            ExtremaDistanceDetector.MIN, 100., 1E-6, Action.CONTINUE);
        final EventDetector d5 = new NadirSolarIncidenceDetector(1.2, earth, 100., 1E-6,
            Action.CONTINUE);
        for (int i = 0; i < nPropagators; i++) {
            // Events
            propagators[i].addEventDetector(d2);
            propagators[i].addEventDetector(d3);
            propagators[i].addEventDetector(d4);
            propagators[i].addEventDetector(d5);
            multiNumericalPropagator.addEventDetector(d2, "state" + i);
            multiNumericalPropagator.addEventDetector(d3, "state" + i);
            multiNumericalPropagator.addEventDetector(d4, "state" + i);
            multiNumericalPropagator.addEventDetector(d5, "state" + i);

            // Loggers (on 1st detector)
            loggers[i] = new CodedEventsLogger();
            final GenericCodingEventDetector gced = new GenericCodingEventDetector(d1, "Ascending",
                "Descending", true, "Anomaly");
            propagators[i].addEventDetector(loggers[i].monitorDetector(gced));

            final GenericCodingEventDetector mgced = new GenericCodingEventDetector(d1,
                "Ascending", "Descending", true, "Anomaly");
            multiNumericalPropagator.addEventDetector(logger.monitorDetector(mgced), "state" + i);
        }

        // ====================== Propagation ======================

        final AbsoluteDate finalDate = initialDate.shiftedBy(86400.);

        // n stand-alone propagations
        final List<SpacecraftState> res1 = new ArrayList<SpacecraftState>();
        final double t01 = System.currentTimeMillis();
        for (int i = 0; i < nPropagators; i++) {
            res1.add(propagators[i].propagate(finalDate));
        }
        final double duration1 = System.currentTimeMillis() - t01;

        // 1 multi-propagation
        final double t02 = System.currentTimeMillis();
        final Map<String, SpacecraftState> res2 = multiNumericalPropagator.propagate(finalDate);
        final double duration2 = System.currentTimeMillis() - t02;

        // ====================== Check ======================

        // Check duration
        System.out.println("Mono-sat duration: " + duration1 / 1000. + "s");
        System.out.println("Multi-sat duration: " + duration2 / 1000. + "s");
        Assert.assertTrue(duration2 < 2. * duration1);

        // Check final state
        for (int i = 0; i < nPropagators; i++) {
            final SpacecraftState state1 = res1.get(i);
            final SpacecraftState state2 = res2.get("state" + i);
            Assert.assertEquals(state1.getDate().durationFrom(state2.getDate()), 0., 0.);
            final PVCoordinates pv1 = state1.getPVCoordinates();
            final PVCoordinates pv2 = state2.getPVCoordinates();
            Assert.assertEquals(pv1.getPosition().getX(), pv2.getPosition().getX(), 0.);
            Assert.assertEquals(pv1.getPosition().getY(), pv2.getPosition().getY(), 0.);
            Assert.assertEquals(pv1.getPosition().getZ(), pv2.getPosition().getZ(), 0.);
            Assert.assertEquals(state1.getAttitude().getRotation().getQuaternion().getQ0(), state2
                .getAttitude().getRotation().getQuaternion().getQ0(), 0.);
            Assert.assertEquals(state1.getAttitude().getRotation().getQuaternion().getQ1(), state2
                .getAttitude().getRotation().getQuaternion().getQ1(), 0.);
            Assert.assertEquals(state1.getAttitude().getRotation().getQuaternion().getQ2(), state2
                .getAttitude().getRotation().getQuaternion().getQ2(), 0.);
            Assert.assertEquals(state1.getAttitude().getRotation().getQuaternion().getQ3(), state2
                .getAttitude().getRotation().getQuaternion().getQ3(), 0.);
            Assert.assertEquals(massProviders1[i].getTotalMass(), massProviders2[i].getTotalMass());
        }

        // Check ephemeris
        for (int i = 0; i < nPropagators; i++) {
            for (int j = 0; j < 86400; j += 10) {
                final BoundedPropagator bound1 = propagators[i].getGeneratedEphemeris();
                final BoundedPropagator bound2 = multiNumericalPropagator
                    .getGeneratedEphemeris("state" + i);
                final AbsoluteDate date = initialDate.shiftedBy(j);
                final PVCoordinates pv1 = bound1.propagate(date).getPVCoordinates();
                final PVCoordinates pv2 = bound2.propagate(date).getPVCoordinates();
                Assert.assertEquals(pv1.getPosition().distance(pv2.getPosition())
                    / pv2.getPosition().getNorm(), 0., 6E-14);
            }
        }

        // Check events'logger
        int nEvent = 0;
        for (int i = 0; i < nPropagators; i++) {
            nEvent += loggers[i].getCodedEventsList().getList().size();
        }
        Assert.assertEquals(nEvent, logger.getCodedEventsList().getList().size(), 0.);
    }
}
