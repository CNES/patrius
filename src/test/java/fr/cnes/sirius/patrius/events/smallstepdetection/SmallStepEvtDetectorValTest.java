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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:105:21/11/2013: class creation.
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.smallstepdetection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.CunninghamAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation test for a propagation getting very short integration steps; we
 * check no events are lost, validating the varying event convergence.
 * 
 * @author cardosop
 * 
 * @version $Id: SmallStepEvtDetectorValTest.java 7379 2013-01-18 13:24:00Z
 *          CardosoP $
 * 
 * @since 2.1
 */
public class SmallStepEvtDetectorValTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Event detection with a dynamicaly varying event
         *               convergence
         * 
         * @featureDescription Event detection with a dynamically varying event
         *                     convergence when the integration step becomes too
         *                     small.
         * 
         * @coveredRequirements DV-EVT_63
         */
        DYNAMIC_CONVERGENCE
    }

    /** . */
    private static final double PROPDURATION = 0.5 * 86400.;
    /** Integrator abs tolerance. */
    private static final double[] ABSTOL = { 1.0e-5, 1.0e-5, 1.0e-5, 1.0e-10,
        1.0e-10, 1.0e-10 };
    /** Integrator rel tolerance. */
    private static final double[] RELTOL = { 1.0e-12, 1.0e-12, 1.0e-12,
        1.0e-12, 1.0e-12, 1.0e-12 };
    /** . */
    private static final double SUNRADIUS = 696000000.;
    /** . */
    private static final double EARTHRADIUS = 6378136.46;
    /** . */
    private double mu;
    /** . */
    private AbsoluteDate iniDate;
    /** . */
    private SpacecraftState initialState;
    /** . */
    private CelestialBody sun;
    /** . */
    private OneAxisEllipsoid earth;
    /** . */
    private MassProvider mass;

    /**
     * Method used to compute the reference values.
     * 
     * @throws PatriusException
     *         should not happen
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws URISyntaxException
     *         should not happen
     */
    public void createRefs() throws PatriusException, IOException,
                            ParseException, URISyntaxException {

        // Integrator and propagator
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(
            0., 60., ABSTOL, RELTOL);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(
            integrator);
        propagator.setInitialState(this.initialState);
        propagator.setOrbitType(OrbitType.CARTESIAN);

        // Forces
        this.addForcesToPropagator(propagator);

        // Event detector
        final EclipseDetectorWrapper ew = new EclipseDetectorWrapper(60., 1e-5,
            CelestialBodyFactory.getSun(), SUNRADIUS,
            CelestialBodyFactory.getEarth(), EARTHRADIUS);

        propagator.addEventDetector(ew);

        propagator.propagate(this.iniDate.shiftedBy(PROPDURATION));

        final Map<AbsoluteDate, Boolean> allEvts = ew.getAllEventDates();

        for (final Entry<AbsoluteDate, Boolean> entry : allEvts.entrySet()) {
            entry.getKey();
        }

    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#DYNAMIC_CONVERGENCE}
     * 
     * @testedMethod misc
     * 
     * @description runs event detection on a propagation that gets a very small
     *              integration step at some point, which triggers a dynamic
     *              decrease for the event's convergence parameter. No event
     *              shall be lost in this case.
     * 
     * @input integration parameters : initial orbit, forces ...
     * 
     * @output list of event dates
     * 
     * @testPassCriteria the events are close to those generated with an
     *                   extrapolation where the integration step remains
     *                   greater than convergence.
     * 
     * @referenceVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws URISyntaxException
     *         should not happen
     */
    @Test
    public void testEclipseToleranceBiggerThanSRP() throws PatriusException, IOException,
                                                   ParseException, URISyntaxException {

        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(
            1., 360., ABSTOL, RELTOL);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(
            integrator);
        propagator.setInitialState(this.initialState);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setMassProviderEquation(this.mass);
        // Forces
        this.addForcesToPropagator(propagator);

        // Event detector (100 * 0.001 -> 0.1 s de tolérance)
        final EclipseDetectorWrapper ew = new EclipseDetectorWrapper(10.,
            100 * CustomSolarRadiationPressure.TOLERANCE_ECLIPSE,
            CelestialBodyFactory.getSun(), SUNRADIUS,
            CelestialBodyFactory.getEarth(), EARTHRADIUS);

        propagator.addEventDetector(ew);

        propagator.propagate(this.iniDate
            .shiftedBy(PROPDURATION));

        // Le nombre d'entree et de sortie d'eclipse doit etre egal
        Assert.assertEquals("Pas le même nb d'entrée et de fin d'éclipse",
            ew.getOutEventDates().size(), ew.getInEventDates().size());

    }

    @Test
    public void testEclipseToleranceSmallerThanSRP() throws PatriusException, IOException,
                                                    ParseException, URISyntaxException {

        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(
            1., 360., ABSTOL, RELTOL);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(
            integrator);
        propagator.setInitialState(this.initialState);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setMassProviderEquation(this.mass);
        // Forces
        this.addForcesToPropagator(propagator);

        // Event detector (0.1 * 0.001 -> 0.0001 s de tolérance)
        final EclipseDetectorWrapper ew = new EclipseDetectorWrapper(10.,
            0.1 * CustomSolarRadiationPressure.TOLERANCE_ECLIPSE,
            CelestialBodyFactory.getSun(), SUNRADIUS,
            CelestialBodyFactory.getEarth(), EARTHRADIUS);

        propagator.addEventDetector(ew);

        propagator.propagate(this.iniDate
            .shiftedBy(PROPDURATION));

        // Le nombre d'entree et de sortie d'eclipse doit etre egal
        Assert.assertEquals("Pas le même nb d'entrée et de fin d'éclipse",
            ew.getOutEventDates().size(), ew.getInEventDates().size());

    }

    /**
     * Adds all forces to a propagator.
     * 
     * @param p
     *        numerical propagator
     * 
     * @throws IOException
     *         should not happen
     * @throws ParseException
     *         should not happen
     * @throws PatriusException
     *         should not happen
     */
    private void addForcesToPropagator(final NumericalPropagator p)
                                                                   throws IOException, ParseException, PatriusException {
        // Earth potential
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader(
                "GRGS_EIGEN_GL04S.txt", true));
        final PotentialCoefficientsProvider provider = GravityFieldFactory
            .getPotentialProvider();
        // Here, we get the data as extracted from the file.
        final int n = 100;
        final double[][] C = provider.getC(n, 100, false);
        final double[][] S = provider.getS(n, 100, false);

        // Solar radiation pressure

        final RadiationSensitive vehicule = new SphericalSpacecraft(5., 2.1, 0.4, 0.4, 0.2, "default");

        final ForceModel SRP = new CustomSolarRadiationPressure(this.sun,
            this.earth.getEquatorialRadius(), vehicule);

        // Atmosphere
        // final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        // final DTM2000 atm = new DTM2000(in, sun, earth);
        // final ForceModel atmDrag = new DragForce(atm, new AeroModel(AssemblyFactory.getSphericalSpacecraft()));

        // Add forces
        p.addForceModel(new CunninghamAttractionModel(FramesFactory.getITRF(),
            this.earth.getEquatorialRadius(), this.mu, C, S));
        p.addForceModel(SRP);
        // p.addForceModel(atmDrag);
    }

    /**
     * Setup.
     * 
     * @throws IOException
     *         snh
     * @throws PatriusException
     */
    @Before
    public void setUp() throws IOException, PatriusException {
        // Orekit initialization
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        try {
            // DataProvidersManager manager = DataProvidersManager.getInstance();
            // String path = "../PATRIUS_DATASET";
            // File f = new File(path);
            // try {
            // manager.addProvider(new DirectoryCrawler(f));
            // } catch (OrekitException e) {
            // System.out
            // .println("Configuration files not found. Check the path variable");
            // }

            this.mu = 3.9860047e14;

            this.sun = CelestialBodyFactory.getSun();

            this.earth = new OneAxisEllipsoid(EARTHRADIUS, 1.0 / 298.25765,
                FramesFactory.getITRF());

            this.iniDate = new AbsoluteDate(2001, 7, 28, 4, 0, 0.0,
                TimeScalesFactory.getTT());

            this.mass = new SimpleMassModel(1000.0, "default");
            new Vector3D(-6042438.668, 3392467.560,
                -25767.25680);

            // final Orbit orbat = new EquinoctialOrbit(new
            // PVCoordinates(position, velocity),
            // FramesFactory.getEME2000(), iniDate, mu);

            // keplerian parameters: {a: 6801621.090535456; e:
            // 0.018853048415303577;
            // i: 98.18622655464753; pa: 176.93085256562927; raan:
            // 150.6577488781153;
            // v: -177.14609413721;}
            // OK : 7325000 , DT 0.0011841071536764503
            final Orbit orbit = new KeplerianOrbit(7325000, 0.11,
                98.18622655464753, 176.93085256562927, 150.6577488781153,
                0., PositionAngle.MEAN, FramesFactory.getEME2000(),
                this.iniDate, this.mu);

            this.initialState = new SpacecraftState(orbit, this.mass);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    /**
     * Teardown.
     * 
     * @throws IOException
     *         snh
     * @throws URISyntaxException
     *         snh
     */
    @After
    public void tearDown() throws IOException, URISyntaxException {
        this.iniDate = null;
        this.initialState = null;

    }

    /**
     * Utility class : wrapper for event detector.
     */
    private static final class EclipseDetectorWrapper implements EventDetector {

        private static final long serialVersionUID = 6559571242489434L;

        private final EclipseDetector ecld;

        private final List<Double> inEventDates = new ArrayList<Double>();
        private final List<Double> outEventDates = new ArrayList<Double>();

        /**
         * @return the inEventDates
         */
        public List<Double> getInEventDates() {
            return this.inEventDates;
        }

        /**
         * @return the outEventDates
         */
        public List<Double> getOutEventDates() {
            return this.outEventDates;
        }

        private final Map<AbsoluteDate, Boolean> allEventDates = new TreeMap<AbsoluteDate, Boolean>();

        public EclipseDetectorWrapper(final double maxChk, final double thr,
            final PVCoordinatesProvider octed, final double octedRad,
            final PVCoordinatesProvider octing, final double octingRad) {
            this.ecld = new EclipseDetector(octed, octedRad, octing, octingRad, 0,
                maxChk, thr){
                private static final long serialVersionUID = 1L;

                @Override
                public Action eventOccurred(final SpacecraftState s,
                                            final boolean increasing, final boolean forward) throws PatriusException {
                    // return increasing ? Action.CONTINUE : Action.STOP;

                    // System.err.println("UMBRA " + inout + " (user eclipse detector)\t"+s.getDate());
                    // System.out.println(s.getPVCoordinates().getPosition().getNorm());
                    return Action.CONTINUE;
                }
            };
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.ecld.init(s0, t);
        }

        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            return this.ecld.g(s);
        }

        @Override
        public boolean shouldBeRemoved() {
            return false;
        }

        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {
            final double pseudoDate = s.getDate().durationFrom(
                AbsoluteDate.J2000_EPOCH);
            final List<Double> eventDates = increasing ? this.outEventDates
                : this.inEventDates;
            eventDates.add(pseudoDate);
            // if (increasing) {
            // System.out.print("\nDeb " + s.getDate());
            // } else {
            // System.out.print(" " + s.getDate() + " Fin");
            // }
            this.allEventDates.put(s.getDate(), increasing);
            return this.ecld.eventOccurred(s, increasing, forward);
        }

        @Override
        public SpacecraftState resetState(final SpacecraftState oldState)
                                                                         throws PatriusException {
            return this.ecld.resetState(oldState);
        }

        @Override
        public double getThreshold() {
            return this.ecld.getThreshold();
        }

        @Override
        public double getMaxCheckInterval() {
            return this.ecld.getMaxCheckInterval();
        }

        @Override
        public int getMaxIterationCount() {
            return this.ecld.getMaxIterationCount();
        }

        @Override
        public int getSlopeSelection() {
            return this.ecld.getSlopeSelection();
        }

        public Map<AbsoluteDate, Boolean> getAllEventDates() {
            return this.allEventDates;
        }

        @Override
        public EventDetector copy() {
            return null;
        }

    }

}
