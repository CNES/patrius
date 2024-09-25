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
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.11:DM:DM-3306:22/05/2023:[PATRIUS] Rayon du soleil dans le calcul de la PRS
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.concurrency.propagation.numerical;

import java.io.IOException;
import java.text.ParseException;

import fr.cnes.sirius.patrius.SolarInputs97to05;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.atmospheres.DTM2000;
import fr.cnes.sirius.patrius.forces.atmospheres.HarrisPriester;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressure;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.parallel.ParallelException;
import fr.cnes.sirius.patrius.tools.parallel.ParallelResult;
import fr.cnes.sirius.patrius.tools.parallel.ParallelTask;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Extrapolation task for the multithreaded test.
 *
 * @author cardosop
 *
 * @version $Id: ExtrapolTask.java 17911 2017-09-11 12:02:31Z bignon $
 *
 * @since 1.2
 *
 */
public class ExtrapolTask implements ParallelTask {

    /** Test mode. */
    public enum Mode {
        /** Mode 1 : default */
        MODE1,
        /** Mode 2 : different start date for propagation. */
        MODE2,
        /** Mode 3 : different ephemeris and atmospheric model. */
        MODE3
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
    private static String fJPLFile = "unxp2000.405";
    /**
     * JPL ephemeris file - alternative
     */
    private static String fJPLFileAlt = "unxp1800.406";

    /**
     * Chosen file.
     */
    private final String jplFile;

    /** Test mode. */
    private final Mode testMode;

    /** Number of days for the extrapolation. */
    private final int days;

    /** Id. */
    private final int id;

    /** Result. */
    private PVSResult result = null;

    /** Thread descriptor. */
    private String threadName = "none";

    /**
     * Constructor.
     *
     * @param iid
     *        Id of the task
     */
    public ExtrapolTask(final int iid) {
        // Default mode
        this(iid, Mode.MODE1, 5);
    }

    /**
     * Constructor with mode.
     *
     * @param iid
     *        Id of the task
     * @param mod
     *        mode
     * @param nbDays
     *        number of extrapolation days
     */
    public ExtrapolTask(final int iid, final Mode mod, final int nbDays) {
        this.id = iid;
        this.testMode = mod;
        if (mod == Mode.MODE3) {
            this.jplFile = fJPLFileAlt;
        } else {
            this.jplFile = fJPLFile;
        }
        // At least one day!
        this.days = MathLib.max(1, nbDays);
        try {
            this.setUp();
        } catch (final IOException e) {
            throw new ParallelException(e);
        } catch (final ParseException e) {
            throw new ParallelException(e);
        } catch (final PatriusException e) {
            throw new ParallelException(e);
        }
    }

    @Override
    public String getTaskLabel() {
        return "ExtrapolTask";
    }

    @Override
    public String getTaskInfo() {
        return this.getTaskLabel() + " mode " + this.testMode + " n°" + this.id + " on thread " + this.threadName;
    }

    @Override
    public ParallelResult call() {

        this.threadName = Thread.currentThread().getName();

        System.out.println("- BEGIN " + this.getTaskInfo());
        BoundedPropagator eph;

        SpacecraftState initialState;
        try {
            initialState = new SpacecraftState(getOrbitLEO(this.testMode));

            // 5 days propagation with potential, third bodies, SRP
            final NumericalPropagator propagator = new NumericalPropagator(getDOPIntegrator(),
                initialState.getFrame(), OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
            propagator.setEphemerisMode();
            // propagator.setOrbitType(OrbitType.EQUINOCTIAL);
            propagator.setInitialState(initialState);
            propagator.addForceModel(getEarthPotential());
            propagator.addForceModel(this.getMoonPotential());
            propagator.addForceModel(this.getSunPotential());
            propagator.addForceModel(this.getMarsPotential());
            propagator.addForceModel(this.getJupiterPotential());
            propagator.addForceModel(this.getVenusPotential());
            propagator.addForceModel(getSrp());
            if (this.testMode == Mode.MODE3) {
                propagator.addForceModel(getHarrisPriesterForce());
            } else {
                propagator.addForceModel(getDTM2000Force());
            }

            propagator.propagate(initialState.getDate().shiftedBy(this.days * 86400)).getPVCoordinates();

            // Sampling (30 points per day)
            eph = propagator.getGeneratedEphemeris();
            final int pointsPerDay = 30;
            final double sliceDuration = 86400. / pointsPerDay;

            // Create and fill a PVSResult object.
            // Note that the "PVSResult" knows its parent task
            this.result = new PVSResult(this);
            for (int slice = 0; slice < (this.days * pointsPerDay); slice++) {
                final PVCoordinates curPv = eph.getPVCoordinates(
                    new AbsoluteDate(initialState.getDate(),
                        slice * sliceDuration), FramesFactory.getGCRF());
                this.result.addPV(curPv);
            }

        } catch (final IOException e) {
            throw new ParallelException(e);
        } catch (final ParseException e) {
            throw new ParallelException(e);
        } catch (final PatriusException e) {
            throw new ParallelException(e);
        }

        System.out.println("-  END  " + this.getTaskInfo());
        return this.result;
    }

    @Override
    public ParallelResult getResult() {
        return this.result;
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
        final ForceModel earthPotential = new DirectBodyAttraction(new DrozinerGravityModel(FramesFactory.getITRF(),
            potentialProvider.getAe(), potentialProvider.getMu(), C, S));
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
    private ForceModel getMoonPotential() throws PatriusException, IOException, ParseException {
        CelestialBodyFactory.clearCelestialBodyLoaders();

        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(this.jplFile,
            EphemerisType.MOON);
        final CelestialBody moon = (CelestialBody) loader.loadCelestialPoint(CelestialBodyFactory.MOON);
        return new ThirdBodyAttraction(moon.getGravityModel());
    }

    /**
     * Sun attraction
     *
     * @return force
     * @throws PatriusException
     *         e
     *
     */
    private ForceModel getSunPotential() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(this.jplFile,
            EphemerisType.SUN);
        final CelestialBody sun = (CelestialBody) loader.loadCelestialPoint(CelestialBodyFactory.SUN);
        return new ThirdBodyAttraction(sun.getGravityModel());
    }

    /**
     * Mars attraction
     *
     * @return force
     * @throws PatriusException
     *         e
     *
     */
    private ForceModel getMarsPotential() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(this.jplFile,
            EphemerisType.MARS);
        final CelestialBody mars = (CelestialBody) loader.loadCelestialPoint(CelestialBodyFactory.MARS);
        return new ThirdBodyAttraction(mars.getGravityModel());
    }

    /**
     * Jupiter attraction
     *
     * @return force
     * @throws PatriusException
     *         e
     *
     */
    private ForceModel getJupiterPotential() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(this.jplFile,
            EphemerisType.JUPITER);
        final CelestialBody jupiter = (CelestialBody) loader.loadCelestialPoint(CelestialBodyFactory.JUPITER);
        return new ThirdBodyAttraction(jupiter.getGravityModel());
    }

    /**
     * Venus attraction
     *
     * @return force
     * @throws PatriusException
     *         e
     *
     */
    private ForceModel getVenusPotential() throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();
        final JPLCelestialBodyLoader loader = new JPLCelestialBodyLoader(this.jplFile,
            EphemerisType.VENUS);
        final CelestialBody venus = (CelestialBody) loader.loadCelestialPoint(CelestialBodyFactory.VENUS);
        return new ThirdBodyAttraction(venus.getGravityModel());
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

        return new SolarRadiationPressure(dRef, pRef, sun, 6.95E8, requa, vehicle);
    }

    /**
     * Getter for DTM2000 force.
     *
     * @return a DTM2000 based force.
     * @throws PatriusException
     *         should not happen
     */
    private static ForceModel getDTM2000Force() throws PatriusException {
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        final DTM2000 atm = new DTM2000(in, sun, earth);
        final SphericalSpacecraft sf = new SphericalSpacecraft(5.0, 2.0, 0.0, 0.0, 0.0, "default");
        final ForceModel fm = new DragForce(atm, sf);
        return fm;
    }

    /**
     * Getter for a Harris-Priester atmospheric model force.
     *
     * @return a Harris-Priester atmospheric model based force.
     * @throws PatriusException
     *         should not happen
     */
    private static ForceModel getHarrisPriesterForce() throws PatriusException {
        final CelestialBodyFrame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        final HarrisPriester hp = new HarrisPriester(sun, earth);
        final SphericalSpacecraft sf = new SphericalSpacecraft(5.0, 2.0, 0.0, 0.0, 0.0, "default");
        final ForceModel fm = new DragForce(hp, sf);
        return fm;
    }

    /**
     * get a LEO orbit.
     *
     * @param mod
     *        test mode
     * @return orbit
     */
    private static Orbit getOrbitLEO(final Mode mod) {
        final AbsoluteDate date;
        if (mod == Mode.MODE2) {
            date = new AbsoluteDate(2005, 8, 02, 12, 12, 0.0, TimeScalesFactory.getTAI());
        } else {
            date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        }
        // mu from grim4s4_gr potential file
        final double mu = potentialProvider.getMu();
        // GCRF reference frame
        final Frame referenceFrame = FramesFactory.getGCRF();
        // pos-vel from ZOOM ephemeris reference
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06,
            -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03,
            -1.14097953925384523e+01);
        final PVCoordinates pvCoordinates = new PVCoordinates(pos, vel);
        final Orbit orbit = new CartesianOrbit(pvCoordinates, referenceFrame, date, mu);
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
    private void setUp() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003:potentialCNES");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        // add a reader for gravity fields
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        // get the gravity field coefficients provider from the 'grim4s4_gr' file
        potentialProvider = GravityFieldFactory.getPotentialProvider();

        // JPL ephemeris
        loaderEMB = new JPLCelestialBodyLoader(this.jplFile,
            EphemerisType.EARTH_MOON);
        loaderSSB = new JPLCelestialBodyLoader(this.jplFile,
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);

        loaderSUN = new JPLCelestialBodyLoader(this.jplFile, EphemerisType.SUN);
        loaderSUN.loadCelestialPoint(CelestialBodyFactory.SUN);
    }

}
