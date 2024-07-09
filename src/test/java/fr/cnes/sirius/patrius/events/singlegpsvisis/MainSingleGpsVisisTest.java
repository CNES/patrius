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
 * @history Created 08/01/2015
 *
 * HISTORY
* VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:374:08/01/2015: wrong eclipse detection.
 * VERSION::FA:415:09/03/2015: Attitude discontinuity on event issue
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * VERSION::FA:1976:04/12/2018:Anomaly on events detection
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.events.singlegpsvisis;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureCircular;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Calcul de visibilités de satellites GPS - Mise en évidence de problème pour la détection
 * d'événements lors d'une discontinuité d'attitude.
 * 
 * @author François DESCLAUX
 */

public class MainSingleGpsVisisTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Test computation of GPS visibility with attitude discontinuity
         * 
         * @featureDescription
         * 
         * @coveredRequirements
         */
        SINGLE_GPS_VISIS,

        /**
         * @featureTitle Test computation of GPS visibility with attitude discontinuity on DV impulse
         * 
         * @featureDescription
         * 
         * @coveredRequirements
         */
        ATTITUDE_DISCONTINUITY_IMPULSE_DV
    }

    /** Result of testAttitudeDiscontOnDV(). */
    boolean res = false;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SINGLE_GPS_VISIS}
     * 
     * @testedMethod {@link NumericalPropagator#addEventDetector(fr.cnes.sirius.patrius.propagation.events.EventDetector)}
     * @testedMethod {@link NumericalPropagator#propagate(AbsoluteDate)}
     * 
     * @description Propagation with eclipse detection. Particular case.
     * 
     * @input a numerical propagator with an input eclipse detector and an input attitude discontinuity
     * 
     * @output eclipse detection
     * 
     * @testPassCriteria 2 eclipse in + 2 eclipse out should be detected
     * 
     * @comments FA 374
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testEclipseDetection() throws PatriusException,
                                      IOException, ParseException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());
        final AbsoluteDate endDate = initDate.shiftedBy(3 * 3600);
        final Orbit initialOrbit = new KeplerianOrbit(6700.e3, 0.01, FastMath.PI / 2.5, MathLib.toRadians(1.0),
            FastMath.PI / 4.0, 0.0, PositionAngle.TRUE,
            gcrf, initDate, Constants.WGS84_EARTH_MU);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        // Build propagator
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(0.1, 500,
            absTOL, relTOL);

        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(itrf);
        final NumericalPropagator propagator = new NumericalPropagator(dop);
        propagator.setInitialState(initialState);
        this.addForceAndAttitude(initialOrbit, earthPointingAtt, propagator, initialState);
        final MyEclipseDetector detector = this.myEclipseDetector();
        propagator.addEventDetector(detector);

        // Propagation
        propagator.propagate(initDate, endDate).getDate();

        // Check
        Assert.assertEquals(detector.getEnterDate().size(), 2);
        Assert.assertEquals(detector.getExitDate().size(), 2);
        Assert.assertEquals(detector.getEnterDate().get(0), "2008-01-01T01:26:07.391");
        Assert.assertEquals(detector.getEnterDate().get(1), "2008-01-01T02:57:02.831");
        Assert.assertEquals(detector.getExitDate().get(0), "2008-01-01T00:29:30.535");
        Assert.assertEquals(detector.getExitDate().get(1), "2008-01-01T02:00:24.607");
    }

    /**
     * Add force and attitude to propagator.
     */
    private void addForceAndAttitude(final Orbit initialOrbit,
                                     final AttitudeProvider attProv, final NumericalPropagator prop,
                                     final SpacecraftState initialState) throws IOException, ParseException,
                                                                        PatriusException {
        /*
         * 3) Ajout des forces naturelles
         */

        // a) Modèle de potentiel

        // add a reader for gravity fields
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader(
                "grim4s4_gr", true));
        // get the gravity field coefficients provider from the 'grim4s4_gr'
        // file
        final PotentialCoefficientsProvider provider = GravityFieldFactory
            .getPotentialProvider();
        // we get the data as extracted from the file
        final int n = 60; // degree
        final int m = 60; // order
        final double[][] C = provider.getC(n, m, false);
        final double[][] S = provider.getS(n, m, false);

        // return perturbing force (ITRF2008 central body frame)
        final ForceModel potentiel = new DrozinerAttractionModel(
            FramesFactory.getITRF(), provider.getAe(), provider.getMu(), C,
            S);

        // b) Attraction des troisièmes corps
        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLEphemeridesLoader loader = new JPLEphemeridesLoader(
            "unxp2000.405", JPLEphemeridesLoader.EphemerisType.SUN);

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader(
            "unxp2000.405", JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader(
            "unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(
            CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(
            CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        final CelestialBody sun = loader
            .loadCelestialBody(CelestialBodyFactory.SUN);
        final CelestialBody moon = loader
            .loadCelestialBody(CelestialBodyFactory.MOON);

        final ForceModel sunAttraction = new ThirdBodyAttraction(sun);
        final ForceModel moonAttraction = new ThirdBodyAttraction(moon);

        // c) Pression de radiation solaire
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46,
            1.0 / 298.25765, FramesFactory.getITRF());

        final Assembly sphericalSpacecraft = this.getSphericalVehicle(1000, 1, 2.3, 0.3, 0.4, 0.3);

        /* 5) Ajout de l'attitude, calcul de l'attitude initiale */
        prop.setAttitudeProvider(attProv);
        final Attitude initAtt = attProv.getAttitude(initialOrbit, initialOrbit.getDate(),
            initialOrbit.getFrame());

        /* 6) Initialisation de l'état : fournir un état complet Orbite, [Attitude, [massProvider]] */
        prop.setInitialState(new SpacecraftState(initialOrbit, initAtt));

        /*
         * Positionnement du véhicule sur le repère satellite courant (pour connexion de
         * l'assemblage véhicule à l'arbre des repères)
         */
        sphericalSpacecraft.initMainPartFrame(prop.getInitialState());

        final RadiationSensitive SRPmodel = new DirectRadiativeModel(
            sphericalSpacecraft);

        final ForceModel prs = new SolarRadiationPressureCircular(sun,
            earth.getEquatorialRadius(), SRPmodel);

        prop.addForceModel(potentiel);
        prop.addForceModel(sunAttraction);
        prop.addForceModel(moonAttraction);

        prop.addForceModel(prs);
    }

    /**
     * Eclipse detector.
     */
    private MyEclipseDetector myEclipseDetector() throws PatriusException {
        final double maxCheck = 60.;
        final double threshold = 1.0e-3;
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();

        return new MyEclipseDetector(sun, Constants.SUN_RADIUS, earth,
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, false, maxCheck, threshold);
    }

    /**
     * Private eclipse detector for test purpose.
     */
    private class MyEclipseDetector extends EclipseDetector {

        private static final long serialVersionUID = -812838231636201611L;

        private final List<String> enterDate = new ArrayList<String>();
        private final List<String> exitDate = new ArrayList<String>();

        public MyEclipseDetector(final PVCoordinatesProvider occulted,
            final double occultedRadius,
            final PVCoordinatesProvider occulting,
            final double occultingRadius,
            final boolean totalEclipse,
            final double maxCheck,
            final double threshold) {
            super(occulted, occultedRadius, occulting, occultingRadius, totalEclipse ? 0 : 1, maxCheck, threshold);
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward) throws PatriusException {
            // ATTENTION g positif hors éclipse, donc increasing en sortie d'éclipse
            if (!increasing) {
                this.enterDate.add(s.getDate().toString());
                System.out.println(s.getDate() + " ENTER Eclipse");
            } else {
                this.exitDate.add(s.getDate().toString());
                System.out.println(s.getDate() + " EXIT Eclipse");
            }
            return Action.CONTINUE;
        }

        public List<String> getEnterDate() {
            return this.enterDate;
        }

        public List<String> getExitDate() {
            return this.exitDate;
        }

    }

    /**
     * Returns a spherical spacecraft with parameters allowing to compute drag and SRP accelerations
     * 
     * @param mass
     *        vehicle mass
     * @param radius
     *        vehicle radius
     * @param normDragCoef
     *        vehicle normal drag force coefficient
     * @param tangentDragCoef
     *        vehicle tangential force coefficient
     * @param ka
     *        vehicle absorbed coef for SRP
     * @param ks
     *        vehicle specular reflectance coef for SRP
     * @param kd
     *        vehicle diffuse reflectance coef for SRP
     * @return
     * @throws PatriusException
     */
    private Assembly getSphericalVehicle(final double mass, final double radius, final double dragCoef,
                                         final double ka, final double ks, final double kd) throws PatriusException {

        final AssemblyBuilder builder = new AssemblyBuilder();

        builder.addMainPart("BODY");
        builder.addPart("Reservoir1", "BODY", Transform.IDENTITY);
        builder.addPart("Reservoir2", "BODY", Transform.IDENTITY);

        // MASSES : 50% BODY, 30% Reservoir1, 20% Reservoir2
        builder.addProperty(new MassProperty(0.5 * mass), "BODY");
        builder.addProperty(new MassProperty(0.3 * mass), "Reservoir1");
        builder.addProperty(new MassProperty(0.2 * mass), "Reservoir2");

        builder.addProperty(new AeroSphereProperty(radius, dragCoef), "BODY");

        builder.addProperty(new RadiativeSphereProperty(radius), "BODY");
        builder.addProperty(new RadiativeProperty(ka, ks, kd), "BODY");

        return builder.returnAssembly();

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_DISCONTINUITY_IMPULSE_DV}
     * 
     * @testedMethod {@link AbstractPropagator#propagate(AbsoluteDate)}
     * 
     * @description Propagation with attitude discontinuity on DV impulse with analytical propagator.
     * 
     * @input a analytical propagator with DV maneuver and discontinuous attitude laws
     * 
     * @output attitude
     * 
     * @testPassCriteria change of attitude law properly performed
     * 
     * @comments FA 415
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testAttitudeDiscontOnDV() throws PatriusException,
                                         IOException, ParseException {

        Utils.setDataRoot("regular-dataPBASE");

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate initDate = new AbsoluteDate("2008-01-01T00:00:00",
            TimeScalesFactory.getUTC());
        final double a = 42164.e3;
        final double e = 0.001;
        final double inc = MathLib.toRadians(0.0);
        final double pom = MathLib.toRadians(0.0);
        final double gom = MathLib.toRadians(0.0);
        final double M = 0.0;
        final Orbit initialOrbit = new KeplerianOrbit(a, e, inc, pom, gom, M, PositionAngle.TRUE,
            gcrf, initDate, Constants.WGS84_EARTH_MU);

        final AttitudeProvider lofPointingAtt = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addPart("Reservoir1", "Main", Transform.IDENTITY);
        builder.addProperty(new MassProperty(500), "Main");
        builder.addProperty(new MassProperty(500), "Reservoir1");
        final MassProvider massModel = new MassModel(builder.returnAssembly());

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, lofPointingAtt);

        // Detecteur visi centre terre
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final EventDetector visiCentreTerre = new CircularFieldOfViewDetector(earth, Vector3D.PLUS_J,
            MathLib.toRadians(3), 60.){
            private static final long serialVersionUID = 1L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                MainSingleGpsVisisTest.this.res = true;
                return Action.CONTINUE;
            }
        };
        // DateManoeuvre et impulsion
        final AbsoluteDate tman = initDate.shiftedBy(10.);
        final EventDetector tmanDetector = new DateDetector(tman, 60, 1e-6);
        final Vector3D deltaVSat = new Vector3D(-1000, 0, 0);
        final ImpulseManeuver impulse = new ImpulseManeuver(tmanDetector, deltaVSat,
            FramesFactory.getGCRF(), 300, massModel, "Reservoir1");
        propagator.addEventDetector(impulse);

        // Propagation
        propagator.setEphemerisMode();
        propagator.addEventDetector(visiCentreTerre);
        propagator.propagate(tman.shiftedBy(1000));

        Assert.assertTrue(this.res);
    }
}
