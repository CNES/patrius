/**
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
 * 
 * HISTORY
* VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:764:06/01/2017:Anomaly on events detection
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:remove PropulsiveProperty name attribute
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY

 * @history creation 23/03/2017
 */
package fr.cnes.sirius.patrius.events;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureCircular;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.GroundMaskElevationDetector;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Additional tests for event detectors: check that two intermingled eclipse detectors with different parameters
 * properly detects
 * expected events.
 */

public class TwoEclipseDetectorsTest {

    /** Count number of event occurrences. */
    private static int count = 0;

    static AbsoluteDate enterDate1 = null;
    static AbsoluteDate enterDate2 = null;
    static AbsoluteDate exitDate1 = null;
    static AbsoluteDate exitDate2 = null;
    static boolean resetState1 = false;
    static boolean resetState2 = false;
    static boolean resetState3 = false;

    /**
     * @testType UT
     * 
     * @description
     *              Check that two intermingled eclipse detectors with different parameters properly detects
     *              expected events (input and output of phenomenon). This check is performed in both propagation and
     *              retro-propagation.
     * 
     * @testPassCriteria 3 eclipses are detected (in and out) in both propagation and retro-propagation (hence 12
     *                   events).
     *                   Test fails if a part of the phenomenon is missing.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     **/
    @Test
    public void testTwoEclipseDetectors() throws IllegalArgumentException, PatriusException, IOException,
                                         ParseException {

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());

        // Initialization
        final AbsoluteDate initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 350.2e3, 0.001,
            MathLib.toRadians(98),
            MathLib.toRadians(3), 0, 0, PositionAngle.TRUE, FramesFactory.getGCRF(), initDate,
            Constants.WGS84_EARTH_MU);
        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(FramesFactory.getITRF());

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);

        builder.addProperty(new MassProperty(0.5 * 1000), "BODY");
        builder.addProperty(new MassProperty(0.3 * 1000), "Reservoir1");
        builder.addProperty(new MassProperty(0.2 * 1000), "Reservoir2");
        builder.addProperty(new AeroSphereProperty(1., 2.3), "BODY");
        builder.addProperty(new RadiativeSphereProperty(1), "BODY");
        builder.addProperty(new RadiativeProperty(0.3, 0.4, 0.3), "BODY");
        final Assembly spacecraft = builder.returnAssembly();

        final MassProvider massModel = new MassModel(spacecraft);
        final NumericalPropagator propagator = new TwoEclipseDetectorsTest().new NumTestPropagator(initialOrbit,
            spacecraft, massModel, earthPointingAtt);
        spacecraft.initMainPartFrame(propagator.getInitialState());

        //
        // Calcul précis (sans bug) avec des seuils faibles : Maxcheck 10 s, Threshold 0.001 s. donne :
        // 2008-01-01T00:30:16.543 EclipsePatrius ENTER
        // 2008-01-01T00:31:35.095 Duree 78.55184283060589 s.
        // 2008-01-01T00:31:35.095 EclipsePatrius EXIT
        // 2008-01-01T02:01:53.976 EclipsePatrius ENTER
        // 2008-01-01T02:02:54.029 Duree 60.053453231326785 s.
        // 2008-01-01T02:02:54.029 EclipsePatrius EXIT
        // 2008-01-01T03:33:31.417 EclipsePatrius ENTER
        // 2008-01-01T03:34:13.156 Duree 41.73833626042688 s.
        // 2008-01-01T03:34:13.156 EclipsePatrius EXIT
        //
        // BUG DE DETECTION : une sortie d'éclipse n'est pas détectée : Maxcheck 100 s, Threshold 1 s. donne:
        // 2008-01-01T00:30:16.544 EclipsePatrius ENTER
        // 2008-01-01T00:31:35.095 Duree 78.55180697594733 s.
        // 2008-01-01T00:31:35.095 EclipsePatrius EXIT
        // 2008-01-01T02:02:54.010 EclipsePatrius ENTER
        // 2008-01-01T03:34:13.127 EclipsePatrius 2eme ENTER !!!!!!!!!!!!!
        // Sortie de la 2eme éclipse vers 02:02:54.029 non détectée.
        //
        // A Noter qu'en passant le "threshold de 1 sec. à 0.001 sec. le problème subsiste, mais différemment.
        // Maxcheck 100 s, Threshold 0.001 s. donne :
        // 2008-01-01T00:30:16.544 EclipsePatrius ENTER
        // 2008-01-01T00:31:35.095 Duree 78.55180634788826 s.
        // 2008-01-01T00:31:35.095 EclipsePatrius EXIT
        // 2008-01-01T02:02:54.029 Duree 5557.485685950909 s.
        // 2008-01-01T02:02:54.029 EclipsePatrius 2eme EXIT !!!!!!!!!!!!!
        // L'entrée à 02:01:53.976 n'est pas détectée.

        final EventDetector eclipseDet = new EclipseDetector(CelestialBodyFactory.getSun(), Constants.SUN_RADIUS,
            CelestialBodyFactory.getEarth(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 1.0, 100, 1){
            boolean inEclipse = false;
            AbsoluteDate enterDate = null;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (increasing) {
                    if (this.enterDate != null) {
                        System.out.println(s.getDate() + " Duree " + s.getDate().durationFrom(this.enterDate)
                            + " s.");
                        if (!this.inEclipse) {
                            Assert.fail();
                        }
                    }
                    this.inEclipse = false;
                    count++;
                    System.out.println(s.getDate() + " EclipsePatrius EXIT ");
                } else {
                    if (this.inEclipse) {
                        Assert.fail();
                    }
                    this.inEclipse = true;
                    this.enterDate = s.getDate();
                    count++;
                    System.out.println(s.getDate() + " EclipsePatrius ENTER");
                }
                return Action.CONTINUE;
            }
        };
        propagator.addEventDetector(eclipseDet);
        propagator.setEphemerisMode();

        // Forward propagation
        System.out.println("    =========    PROPAGATION START    =========");
        System.out.println("Date : " + initDate);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(3600 * 3.7));
        System.out.println("    =========    PROPAGATION END      =========");
        System.out.println("Date : " + finalState.getDate());

        propagator.propagate(initDate);

        // Check
        Assert.assertEquals(10, count);

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    /**
     * @testType UT
     * 
     * @description
     *              Check events detection with a ground mask elevation detector (enter and exit with 10s inbetween):
     *              - Propagation with maxCheck = 10s: detects both events
     *              - Propagation with maxCheck = 100s: detects no events
     *              - Propagation with maxCheck = 100s and reset state in the middle: detects both events
     * 
     * @testPassCriteria 1st propagation and 3rd propagation detects same events (accuracy = detector threshold =
     *                   0.001s)
     *                   2nd propagation detects no events.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     **/
    @Test
    public void testGroundMaskWithResetState() throws IllegalArgumentException, PatriusException, IOException,
                                              ParseException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(new FramesConfigurationBuilder().getConfiguration());

        final AbsoluteDate initDate = new AbsoluteDate("2014-06-01T00:00:00.000", TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(98), 0,
            MathLib.toRadians(141.52), 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
            initDate, Constants.WGS84_EARTH_MU);
        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(FramesFactory.getITRF());

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);
        builder.addProperty(new MassProperty(0.5 * 3500), "BODY");
        final TankProperty reservoir1 = new TankProperty(0.3 * 3500);
        builder.addProperty(reservoir1, "Reservoir1");
        builder.addProperty(new TankProperty(0.2 * 3500), "Reservoir2");
        builder.addProperty(new AeroSphereProperty(5., 2.3), "BODY");
        builder.addProperty(new RadiativeSphereProperty(5.), "BODY");
        builder.addProperty(new RadiativeProperty(0.5, 0.5, 0.0), "BODY");
        final Assembly spacecraft = builder.returnAssembly();
        final MassProvider massModel = new MassModel(spacecraft);

        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel,
            earthPointingAtt);

        spacecraft.initMainPartFrame(propagator.getInitialState());
        final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 0,
            FramesFactory.getTIRF(), "STATION");
        final TopocentricFrame topoStation = new TopocentricFrame(earth, new GeodeticPoint(MathLib.toRadians(40), 0,
            100.), "STATION");
        final double[][] maskAzEl = new double[][] { { MathLib.toRadians(0), MathLib.toRadians(10) },
            { MathLib.toRadians(360), MathLib.toRadians(10) } };

        // Build detectors
        final EventDetector visiDetMaxcheck100 = new GroundMaskElevationDetector(maskAzEl, topoStation, 100,
            0.001){
            private static final long serialVersionUID = -2709670796726672757L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (increasing) {
                    System.out.println(s.getDate() + " ENTER STATION ");
                    enterDate1 = s.getDate();
                } else {
                    System.out.println(s.getDate() + " EXIT STATION ");
                    exitDate1 = s.getDate();
                }
                return Action.CONTINUE;
            }
        };
        final EventDetector visiDetMaxcheck10 = new GroundMaskElevationDetector(maskAzEl, topoStation, 10,
            0.001){
            private static final long serialVersionUID = 805259594347477761L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (increasing) {
                    System.out.println(s.getDate() + " ENTER STATION ");
                    enterDate2 = s.getDate();
                } else {
                    System.out.println(s.getDate() + " EXIT STATION ");
                    exitDate2 = s.getDate();
                }
                return Action.CONTINUE;
            }
        };

        // Propagations
        System.out.println("    =========    SANS MANOEUVRE, Maxcheck visi station = 10 s. (Evts REELS) ========= ");
        propagator.addEventDetector(visiDetMaxcheck10);
        propagator.propagate(initDate, initDate.shiftedBy(3600 * 24 * 0.2));

        System.out.println("    =========    SANS MANOEUVRE, Maxcheck visi station = 100 s. ========= ");
        enterDate1 = null;
        exitDate1 = null;
        propagator.clearEventsDetectors();
        propagator.addEventDetector(visiDetMaxcheck100);
        propagator.propagate(initDate, initDate.shiftedBy(3600 * 24 * 0.2));
        Assert.assertTrue(enterDate1 == null);
        Assert.assertTrue(exitDate1 == null);

        System.out
            .println("    =========    AVEC  MANOEUVRE (t0=03:52:12.000) et Maxcheck visi station = 100 s.   ========= ");
        final AbsoluteDate manDate = new AbsoluteDate("2014-06-01T03:52:12.000", TimeScalesFactory.getTAI());
        final ContinuousThrustManeuver man =
            new ContinuousThrustManeuver(manDate, 1e6, new PropulsiveProperty(1., 1600), Vector3D.PLUS_I,
                massModel, reservoir1, LOFType.TNW);
        propagator.clearEventsDetectors();
        propagator.addEventDetector(visiDetMaxcheck100);
        propagator.addForceModel(man);
        propagator.propagate(initDate, initDate.shiftedBy(3600 * 24 * 0.2));

        // Check
        Assert.assertEquals(0., enterDate1.durationFrom(enterDate2), 0.001);
        Assert.assertEquals(0., exitDate1.durationFrom(exitDate2), 0.001);

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    /**
     * @testType UT
     * 
     * @description
     *              Check events detection with a ground mask elevation detector (enter and exit with reset states
     *              actions with 10s inbetween):
     *              - Propagation with maxCheck = 10s: detects both events (reset state action)
     *              - Propagation with maxCheck = 100s and reset state in the middle: detects both events (reset state
     *              action)
     * 
     * @testPassCriteria 1st propagation and 3rd propagation detects same events (accuracy = detector threshold =
     *                   0.001s)
     *                   All reset states (ground mask events + reset state in the middle) have been properly triggered
     *                   and at correct date.
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     **/
    @Test
    public void testGroundMaskWith2ResetState() throws IllegalArgumentException, PatriusException, IOException,
                                               ParseException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(new FramesConfigurationBuilder().getConfiguration());

        final AbsoluteDate initDate = new AbsoluteDate("2014-06-01T00:00:00.000", TimeScalesFactory.getTAI());
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(98), 0,
            MathLib.toRadians(141.52), 0, PositionAngle.TRUE, FramesFactory.getGCRF(),
            initDate, Constants.WGS84_EARTH_MU);
        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(FramesFactory.getITRF());

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);
        builder.addProperty(new MassProperty(0.5 * 3500), "BODY");
        builder.addProperty(new MassProperty(0.3 * 3500), "Reservoir1");
        builder.addProperty(new MassProperty(0.2 * 3500), "Reservoir2");
        builder.addProperty(new AeroSphereProperty(5., 2.3), "BODY");
        builder.addProperty(new RadiativeSphereProperty(5.), "BODY");
        builder.addProperty(new RadiativeProperty(0.5, 0.5, 0.0), "BODY");
        final Assembly spacecraft = builder.returnAssembly();
        final MassProvider massModel = new MassModel(spacecraft);

        final NumericalPropagator propagator = new NumTestPropagator(initialOrbit, spacecraft, massModel,
            earthPointingAtt);

        spacecraft.initMainPartFrame(propagator.getInitialState());
        final EllipsoidBodyShape earth = new ExtendedOneAxisEllipsoid(
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 0,
            FramesFactory.getTIRF(), "STATION");
        final TopocentricFrame topoStation = new TopocentricFrame(earth, new GeodeticPoint(MathLib.toRadians(40), 0,
            100.), "STATION");
        final double[][] maskAzEl = new double[][] { { MathLib.toRadians(0), MathLib.toRadians(10) },
            { MathLib.toRadians(360), MathLib.toRadians(10) } };

        // Build detectors
        final EventDetector visiDetMaxcheck100 = new GroundMaskElevationDetector(maskAzEl, topoStation, 100,
            0.001){
            private static final long serialVersionUID = -2709670796726672757L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (increasing) {
                    System.out.println(s.getDate() + " ENTER STATION ");
                    enterDate1 = s.getDate();
                    return Action.RESET_STATE;
                } else {
                    System.out.println(s.getDate() + " EXIT STATION ");
                    exitDate1 = s.getDate();
                    return Action.RESET_STATE;
                }
            }

            @Override
            public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
                resetState1 = true;
                return oldState;
            }
        };
        final EventDetector visiDetMaxcheck10 = new GroundMaskElevationDetector(maskAzEl, topoStation, 10,
            0.001){
            private static final long serialVersionUID = 805259594347477761L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                if (increasing) {
                    System.out.println(s.getDate() + " ENTER STATION ");
                    enterDate2 = s.getDate();
                } else {
                    System.out.println(s.getDate() + " EXIT STATION ");
                    exitDate2 = s.getDate();
                }
                return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
                resetState2 = true;
                return oldState;
            }
        };

        final EventDetector detectorRS = new EventDetector(){
            @Override
            public boolean shouldBeRemoved() {
                return false;
            }

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public double getThreshold() {
                return 0.001;
            }

            @Override
            public int getSlopeSelection() {
                return 0;
            }

            @Override
            public int getMaxIterationCount() {
                return 100;
            }

            @Override
            public double getMaxCheckInterval() {
                return 10;
            }

            @Override
            public double g(final SpacecraftState s) throws PatriusException {
                final AbsoluteDate manDate = new AbsoluteDate("2014-06-01T03:52:12.000", TimeScalesFactory.getTAI());
                return s.getDate().durationFrom(manDate);
            }

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
                Assert.assertEquals(
                    0.,
                    oldState.getDate().durationFrom(
                        new AbsoluteDate("2014-06-01T03:52:12.000", TimeScalesFactory.getTAI())), 0.001);
                resetState3 = true;
                return oldState;
            }

            @Override
            public EventDetector copy() {
                return null;
            }
        };

        // Add another detector to check it is properly handled
        final EventDetector dateDetector = new DateDetector(new AbsoluteDate("2014-06-01T03:52:13.000",
            TimeScalesFactory.getTAI())){

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                Assert.assertEquals(
                    0.,
                    s.getDate().durationFrom(
                        new AbsoluteDate("2014-06-01T03:52:13.000", TimeScalesFactory.getTAI())), 0.001);
                return Action.CONTINUE;
            }
        };

        // Propagations
        System.out.println("    =========    SANS MANOEUVRE, Maxcheck visi station = 10 s. (Evts REELS) ========= ");
        propagator.addEventDetector(visiDetMaxcheck10);
        propagator.propagate(initDate, initDate.shiftedBy(3600 * 24 * 0.2));

        System.out
            .println("    =========    AVEC  MANOEUVRE (t0=03:52:12.000) et Maxcheck visi station = 100 s.   ========= ");
        propagator.clearEventsDetectors();
        propagator.addEventDetector(visiDetMaxcheck100);
        propagator.addEventDetector(detectorRS);
        propagator.addEventDetector(dateDetector);
        propagator.propagate(initDate, initDate.shiftedBy(3600 * 24 * 0.2));

        // Check
        Assert.assertEquals(0., enterDate1.durationFrom(enterDate2), 0.001);
        Assert.assertEquals(0., exitDate1.durationFrom(exitDate2), 0.001);
        Assert.assertTrue(resetState1);
        Assert.assertTrue(resetState2);
        Assert.assertTrue(resetState3);

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    private class NumTestPropagator extends NumericalPropagator {

        private static final long serialVersionUID = -2902349006522333152L;

        /**
         * Propagateur numerique.
         */
        public NumTestPropagator(final Orbit initialOrbit, final Assembly spacecraft, final MassProvider massProvider,
            final AttitudeProvider attProv) throws PatriusException, IOException, ParseException {
            /* 0) Creation initiale */
            super(new DormandPrince853Integrator(1e-5, 500,
                new double[] { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 },
                new double[] { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 }));

            /* 3) Gestion de la masse : fourniture du mass model au propagateur */
            this.setMassProviderEquation(massProvider);
            // Donner les valeurs de tolérance pour l'intégration des états additionnels
            // Ici trois parts ont la propriété de masse
            this.setAdditionalStateTolerance("MASS_BODY", new double[] { 1e-06 }, new double[] { 1e-09 });
            this.setAdditionalStateTolerance("MASS_Reservoir1", new double[] { 1e-06 }, new double[] { 1e-09 });
            this.setAdditionalStateTolerance("MASS_Reservoir2", new double[] { 1e-06 }, new double[] { 1e-09 });

            /* 4) Ajout des forces naturelles et manoeuvres */
            // a) Modele de potentiel
            GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
            final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
            final double[][] C = provider.getC(60, 60, false);
            final double[][] S = provider.getS(60, 60, false);
            this.addForceModel(new DrozinerAttractionModel(FramesFactory.getITRF(), provider.getAe(), provider.getMu(),
                C, S));

            // b) Attraction des troisiemes corps
            CelestialBodyFactory.clearCelestialBodyLoaders();
            final JPLEphemeridesLoader loader = new JPLEphemeridesLoader("unxp2000.405",
                JPLEphemeridesLoader.EphemerisType.SUN);
            final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader("unxp2000.405",
                JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
            final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader("unxp2000.405",
                JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);
            CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
            CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
            final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);
            final CelestialBody moon = loader.loadCelestialBody(CelestialBodyFactory.MOON);
            this.addForceModel(new ThirdBodyAttraction(sun));
            this.addForceModel(new ThirdBodyAttraction(moon));

            // c) Pression de radiation solaire
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765, FramesFactory.getITRF());
            final RadiationSensitive radiativeModel = new DirectRadiativeModel(spacecraft);
            this.addForceModel(new SolarRadiationPressureCircular(sun, earth.getEquatorialRadius(), radiativeModel));

            // d) Frottement atmospherique
            final SimpleExponentialAtmosphere atm = new SimpleExponentialAtmosphere(earth, 0.0004, 42000.0, 7500.0);
            final AeroModel aeroModel = new AeroModel(spacecraft);
            this.addForceModel(new DragForce(atm, aeroModel));

            /* 5) Ajout de l'attitude, calcul de l'attitude initiale */
            this.setAttitudeProvider(attProv);
            final Attitude initAtt = attProv.getAttitude(initialOrbit, initialOrbit.getDate(), initialOrbit.getFrame());

            /* 6) Initialisation de l'état : fournir un état complet Orbite, Attitude, Masse via massProvider */
            this.setInitialState(new SpacecraftState(initialOrbit, initAtt, massProvider));
        }
    }

    /**
     * @testType UT
     * 
     * @description
     *              Check node events are properly detected if a reset state is performed in-between with a larger
     *              maxCheck.
     *              This test is the simplified version for analytical propagators of test
     *              {@link #testGroundMaskWithResetState()}.
     * 
     * @testPassCriteria propagations with reset state detects both node events (before and after reset state)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     **/
    @Test
    public void testAnalytical() throws IllegalArgumentException, PatriusException, IOException, ParseException {

        // Propagation
        final List<AbsoluteDate> actual = new ArrayList<AbsoluteDate>();

        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, 0.1, 0, 0, FastMath.PI / 2., PositionAngle.TRUE,
            FramesFactory.getGCRF(),
            initDate, Constants.WGS84_EARTH_MU);
        final Propagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.addEventDetector(new NodeDetector(FramesFactory.getGCRF(), 2, initialOrbit.getKeplerianPeriod(),
            0.001){
            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                super.eventOccurred(s, increasing, forward);
                actual.add(s.getDate());
                return Action.CONTINUE;
            }
        });
        propagator.addEventDetector(new DateDetector(AbsoluteDate.J2000_EPOCH.shiftedBy(2000.), 100, 0.001,
            Action.RESET_STATE));
        propagator.propagate(initDate.shiftedBy(initialOrbit.getKeplerianPeriod()));

        // Check
        Assert.assertEquals(0.,
            actual.get(0).durationFrom(new AbsoluteDate("2000-01-01T12:23:44.945", TimeScalesFactory.getTAI())),
            0.001);
        Assert.assertEquals(0.,
            actual.get(1).durationFrom(new AbsoluteDate("2000-01-01T13:12:19.203", TimeScalesFactory.getTAI())),
            0.001);
    }
}