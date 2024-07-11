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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
 * VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.concurrency.propagation.numerical;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.SolarInputs97to05;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.NadirPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.atmospheres.DTM2000;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressure;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvLagrange;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.PVCoordinatesPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.DihedralFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger;
import fr.cnes.sirius.patrius.propagation.events.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.parallel.AbstractSimpleParallelTaskImpl;
import fr.cnes.sirius.patrius.tools.parallel.ParallelException;
import fr.cnes.sirius.patrius.tools.parallel.ParallelResult;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Extrapolation task for the multithreaded test.
 * 
 * @author cardosop
 * 
 * @version $Id: EphEventsTask.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class EphEventsTask extends AbstractSimpleParallelTaskImpl {

    static {
        SP = new ArrayList<>();
        try {
            setUp();
        } catch (final IOException e) {
            throw new ParallelException(e);
        } catch (final ParseException e) {
            throw new ParallelException(e);
        } catch (final PatriusException e) {
            throw new ParallelException(e);
        }
        computeStates();
    }

    /**
     * gravity field coefficients provider
     */
    private static PotentialCoefficientsProvider potentialProvider;

    /**
     * JPL ephemeris loader
     */
    private static JPLCelestialBodyLoader loaderEMB;
    /**
     * JPL ephemeris loader
     */
    private static JPLCelestialBodyLoader loaderSSB;
    /**
     * JPL ephemeris loader
     */
    private static JPLCelestialBodyLoader loaderSUN;
    /**
     * JPL ephemeris file
     */
    private static final String FJPLFILE = "unxp2000.405";
    /** Apside detector. */
    private static ApsideDetector apogeePergieePassages;
    /** Node detector. */
    private static NodeDetector nodesPassages;
    /** Eclipse detector. */
    private static EclipseDetector eclipse;
    /** Penumbra detector. */
    private static EclipseDetector penumbra;
    /** Station visibility detector. */
    private static CircularFieldOfViewDetector stationVisi35;
    /** Station visibility detector. */
    private static CircularFieldOfViewDetector stationVisi30;
    /** Station visibility detector. */
    private static DihedralFieldOfViewDetector stationVisi20;

    /** List of SpacecraftStates. */
    private static final List<SpacecraftState> SP;

    /**
     * Constructor.
     * 
     * @param iid
     *        Id of the task
     */
    public EphEventsTask(final int iid) {
        super(iid);
    }

    /**
     * Fills the list of SpacecraftStates.
     */
    private static void computeStates() {
        System.out.println("Computing states...");
        SpacecraftState initialState;
        final NumericalPropagator propagator;

        try {
            initialState = new SpacecraftState(getOrbitLEO());

            // 5 days propagation with potential, third bodies, SRP
            propagator = new NumericalPropagator(getDOPIntegrator());
            propagator.setEphemerisMode();
            propagator.setOrbitType(OrbitType.EQUINOCTIAL);
            propagator.setInitialState(initialState);
            final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101,
                FramesFactory.getITRF());
            propagator.setAttitudeProvider(new NadirPointing(earth));
            propagator.addForceModel(getEarthPotential());
            propagator.addForceModel(getMoonPotential());
            propagator.addForceModel(getSunPotential());
            propagator.addForceModel(getMarsPotential());
            propagator.addForceModel(getJupiterPotential());
            propagator.addForceModel(getVenusPotential());
            propagator.addForceModel(getSrp());
            propagator.addForceModel(getDTM2000Force());

            // Generate a list of SpacecraftStates
            final int days = 5;
            SpacecraftState curState = initialState;
            SP.add(curState);
            final int pointsPerDay = 30;
            final double step = 86400. / pointsPerDay;
            for (int i = 0; i < days * pointsPerDay; i++) {
                final SpacecraftState newState = propagator.propagate(
                    curState.getDate().shiftedBy(step));
                SP.add(newState);
                curState = newState;
            }

        } catch (final PropagationException e) {
            throw new ParallelException(e);
        } catch (final PatriusException e) {
            throw new ParallelException(e);
        } catch (final IOException e) {
            throw new ParallelException(e);
        } catch (final ParseException e) {
            throw new ParallelException(e);
        }

    }

    @Override
    public ParallelResult callImpl() {
        System.out.println("- BEGIN " + this.getTaskInfo());

        final AbsoluteDate startDate = SP.get(6).getDate();
        final AbsoluteDate endDate = SP.get(SP.size() - 7).getDate();

        // Load the contents of SP in a LagrangeEphemeris
        final SpacecraftState[] spArray = SP.toArray(new SpacecraftState[0]);
        Propagator lagEph = null;
        try {
            lagEph = new PVCoordinatesPropagator(new EphemerisPvLagrange(spArray, 8, null),
                SP.get(0).getDate(), SP.get(0).getMu(), SP.get(0).getFrame());
        } catch (final PatriusException e1) {
            e1.printStackTrace();
        }

        // Add event detectors
        final EventsLogger logEph = new EventsLogger();
        lagEph.addEventDetector(logEph.monitorDetector(apogeePergieePassages));
        lagEph.addEventDetector(logEph.monitorDetector(nodesPassages));
        lagEph.addEventDetector(logEph.monitorDetector(eclipse));
        lagEph.addEventDetector(logEph.monitorDetector(penumbra));
        lagEph.addEventDetector(logEph.monitorDetector(stationVisi35));
        lagEph.addEventDetector(logEph.monitorDetector(stationVisi30));
        lagEph.addEventDetector(logEph.monitorDetector(stationVisi20));

        // Perform event detection in this ephemeris
        final double safeShift = 0.001;
        try {
            lagEph.propagate(startDate.shiftedBy(safeShift), endDate.shiftedBy(-safeShift));
        } catch (final PropagationException e) {
            throw new ParallelException(e);
        }

        // Fill a result instance
        final EphEventsResult rez = new EphEventsResult();
        for (final LoggedEvent logged : logEph.getLoggedEvents()) {
            final String eventType = logged.getEventDetector().toString();
            final AbsoluteDate eventDate = logged.getState().getDate();
            rez.addEvt(eventType, eventDate);
        }

        System.out.println("-  END  " + this.getTaskInfo() + " - Nb.events : " + logEph.getLoggedEvents().size());
        return rez;
    }

    /**
     * DOP853 integrator with hmin=0.1s hmax=60s
     * 
     * @return DOP integrator
     * 
     */
    private static AdaptiveStepsizeIntegrator getDOPIntegrator() {
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8, 1e-9 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.1, 60., absTOL, relTOL);
        return integrator;
    }

    /**
     * get the earth potential
     * 
     * @return force
     * @throws PatriusException
     *         e
     * @throws IOException
     *         e
     * @throws ParseException
     *         e
     * 
     */
    private static ForceModel getEarthPotential() throws PatriusException, IOException, ParseException {

        // degree
        final int n = 60;
        // order
        final int m = 60;
        final double[][] C = potentialProvider.getC(n, m, false);
        final double[][] S = potentialProvider.getS(n, m, false);
        // return perturbing force (ITRF2008 central body frame)
        final ForceModel earthPotential = new DrozinerAttractionModel(FramesFactory.getITRF(),
            potentialProvider.getAe(), potentialProvider.getMu(), C, S);
        return earthPotential;
    }

    /**
     * Moon attraction
     * 
     * @return force
     * @throws PatriusException
     *         e
     * @throws IOException
     *         e
     * @throws ParseException
     *         e
     * 
     */
    private static ForceModel getMoonPotential() throws PatriusException, IOException, ParseException {
        CelestialBodyFactory.clearCelestialBodyLoaders();

        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(FJPLFILE,
            EphemerisType.MOON);
        final CelestialBody moon = loader.loadCelestialBody(CelestialBodyFactory.MOON);
        return new ThirdBodyAttraction(moon);
    }

    /**
     * Sun attraction
     * 
     * @return force
     * @throws PatriusException
     *         e
     * 
     */
    private static ForceModel getSunPotential() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(FJPLFILE,
            EphemerisType.SUN);
        final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);
        return new ThirdBodyAttraction(sun);
    }

    /**
     * Mars attraction
     * 
     * @return force
     * @throws PatriusException
     *         e
     * 
     */
    private static ForceModel getMarsPotential() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(FJPLFILE,
            EphemerisType.MARS);
        final CelestialBody mars = loader.loadCelestialBody(CelestialBodyFactory.MARS);
        return new ThirdBodyAttraction(mars);
    }

    /**
     * Jupiter attraction
     * 
     * @return force
     * @throws PatriusException
     *         e
     * 
     */
    private static ForceModel getJupiterPotential() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(FJPLFILE,
            EphemerisType.JUPITER);
        final CelestialBody jupiter = loader.loadCelestialBody(CelestialBodyFactory.JUPITER);
        return new ThirdBodyAttraction(jupiter);
    }

    /**
     * Venus attraction
     * 
     * @return force
     * @throws PatriusException
     *         e
     * 
     */
    private static ForceModel getVenusPotential() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(FJPLFILE,
            EphemerisType.VENUS);
        final CelestialBody venus = loader.loadCelestialBody(CelestialBodyFactory.VENUS);
        return new ThirdBodyAttraction(venus);
    }

    /**
     * Solar Radiation Pressure
     * 
     * @return force
     * @throws PatriusException
     *         e
     * @throws IOException
     *         e
     * @throws ParseException
     *         e
     * 
     */
    private static ForceModel getSrp() throws PatriusException, IOException, ParseException {

        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        // Earth equatorial radius from grim4s4_gr GRGS file
        final double requa = 6378136.0;

        // dRef reference distance for the solar radiation pressure (m)
        final double dRef = 149597870000.0;

        // pRef reference solar radiation pressure at dRef (N/m2)
        final double pRef = 4.5605E-6;

        // the spacecraft is a sphere (R=1m, absorption coefficient = 1)
        final RadiationSensitive vehicle = new SphericalSpacecraft(FastMath.PI, 0., 1., 0., 0., "default");

        return new SolarRadiationPressure(dRef, pRef, sun, requa, vehicle);
    }

    /**
     * Getter for DTM2000 force.
     * 
     * @return a DTM2000 based force.
     * @throws PatriusException
     *         should not happen
     */
    private static ForceModel getDTM2000Force() throws PatriusException {
        final Frame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        final DTM2000 atm = new DTM2000(in, sun, earth);
        final SphericalSpacecraft sf = new SphericalSpacecraft(5.0, 2.0, 0.0, 0.0, 0.0, "default");
        final ForceModel fm = new DragForce(atm, sf);
        return fm;
    }

    /**
     * get a LEO orbit.
     * 
     * @return orbit
     */
    private static Orbit getOrbitLEO() {
        final AbsoluteDate date;
        date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        // mu from grim4s4_gr potential file
        final double mu = potentialProvider.getMu();
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        final double a = 10000000;
        final Orbit orbit = new KeplerianOrbit(a, 0.001, MathLib.toRadians(40), MathLib.toRadians(10),
            MathLib.toRadians(15), MathLib.toRadians(20), PositionAngle.MEAN, referenceFrame, date, mu);
        return orbit;
    }

    /**
     * Orekit setup.
     * 
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws PatriusException
     *         should not happen
     */
    private static void setUp() throws IOException, ParseException, PatriusException {
        System.out.println("Setup...");
        Utils.setDataRoot("regular-dataCNES-2003:potentialCNES");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        // add a reader for gravity fields
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        // get the gravity field coefficients provider from the 'grim4s4_gr' file
        potentialProvider = GravityFieldFactory.getPotentialProvider();

        // JPL ephemeris
        loaderEMB = new JPLCelestialBodyLoader(FJPLFILE,
            EphemerisType.EARTH_MOON);
        loaderSSB = new JPLCelestialBodyLoader(FJPLFILE,
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);

        loaderSUN = new JPLCelestialBodyLoader(FJPLFILE, EphemerisType.SUN);
        final CelestialBody sun = loaderSUN.loadCelestialBody(CelestialBodyFactory.SUN);

        // Event detectors
        final Orbit initialOrbit = getOrbitLEO();

        // apogee perigee passages
        apogeePergieePassages = new ApsideDetector(initialOrbit, 2){
            private static final long serialVersionUID = 7149796307062112194L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // nodes passages
        nodesPassages = new NodeDetector(initialOrbit, initialOrbit.getFrame(), NodeDetector.ASCENDING_DESCENDING){
            private static final long serialVersionUID = 1528780196650676150L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.CONTINUE;
            }
        };

        final JPLCelestialBodyLoader loaderEarth = new JPLCelestialBodyLoader(FJPLFILE,
            EphemerisType.EARTH);

        final CelestialBody earth = loaderEarth.loadCelestialBody(CelestialBodyFactory.EARTH);

        final double re = Constants.EGM96_EARTH_EQUATORIAL_RADIUS;
        // eclipse
        eclipse = new EclipseDetector(sun, Constants.SUN_RADIUS, earth, re, 0, 300, 0.001){
            private static final long serialVersionUID = -2984027140864819559L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.CONTINUE;
            }
        };

        // penumbra
        penumbra = new EclipseDetector(sun, Constants.SUN_RADIUS, earth, re, 1, 300, 0.001){
            private static final long serialVersionUID = 5098112473308858265L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.CONTINUE;
            }
        };

        final int maxCheck = 120;

        final GeodeticPoint point1 = new GeodeticPoint(MathLib.toRadians(40), MathLib.toRadians(300), 0);
        final GeodeticPoint point2 = new GeodeticPoint(MathLib.toRadians(-30), MathLib.toRadians(250), 0);
        final GeodeticPoint point3 = new GeodeticPoint(MathLib.toRadians(-12), MathLib.toRadians(30), 0);

        final OneAxisEllipsoid earthBody = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765,
            FramesFactory.getITRF());

        final PVCoordinatesProvider station1 = new TopocentricFrame(earthBody, point1, "station 1");
        final PVCoordinatesProvider station2 = new TopocentricFrame(earthBody, point2, "station 2");
        final PVCoordinatesProvider station3 = new TopocentricFrame(earthBody, point3, "station 3");

        // station visibility
        stationVisi35 = new CircularFieldOfViewDetector(station1, Vector3D.PLUS_I, MathLib.toRadians(35), maxCheck){
            private static final long serialVersionUID = -54150076610577203L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.CONTINUE;
            }
        };

        stationVisi30 = new CircularFieldOfViewDetector(station2, Vector3D.PLUS_I, MathLib.toRadians(30), maxCheck){
            private static final long serialVersionUID = -7242813421915186858L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.CONTINUE;
            }
        };
        final Vector3D center = Vector3D.MINUS_I;
        final Vector3D axis1 = Vector3D.PLUS_K;
        final Vector3D axis2 = Vector3D.PLUS_J;
        stationVisi20 = new DihedralFieldOfViewDetector(station3, center, axis1, MathLib.toRadians(20), axis2,
            MathLib.toRadians(50), maxCheck){
            private static final long serialVersionUID = 1278789570580110865L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.CONTINUE;
            }
        };
    }

}
