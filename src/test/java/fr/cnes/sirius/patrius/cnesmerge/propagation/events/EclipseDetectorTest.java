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
 * Copyright 2002-2011 CS Communication & Systèmes
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.propagation.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.ConstantVectorDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for EclipseDetector.<br>
 * Class to be merged with the existing EclipseDetectorTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS
 * follows different conventions regarding unit tests.
 * 
 * @author cardosop
 * 
 * @version $Id: EclipseDetectorTest.java 17917 2017-09-11 12:55:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class EclipseDetectorTest {

    /**
     * Mu.
     */
    private double mu;
    /**
     * iniDate.
     */
    private AbsoluteDate iniDate;
    /**
     * initialState.
     */
    private SpacecraftState initialState;
    /**
     * Propagator.
     */
    private NumericalPropagator propagator;

    /**
     * sunRadius.
     */
    private final double sunRadius = 696000000.;
    /**
     * earthRadius.
     */
    private final double earthRadius = 6400000.;

    /**
     * Existing unit test setup, DO NOT MERGE within Orekit.
     */
    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils
                .getIERS2003Configuration(true));

            this.mu = 3.9860047e14;
            final Vector3D position = new Vector3D(-6142438.668, 3492467.560,
                -25767.25680);
            final Vector3D velocity = new Vector3D(505.8479685, 942.7809215,
                7435.922231);
            this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0,
                TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(
                position, velocity), FramesFactory.getEME2000(), this.iniDate,
                this.mu);
            this.initialState = new SpacecraftState(orbit);
            final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6,
                1.0e-6, 1.0e-6 };
            final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7,
                1.0e-7, 1.0e-7 };
            final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(
                0.001, 1000, absTolerance, relTolerance);
            integrator.setInitialStepSize(60);
            this.propagator = new NumericalPropagator(integrator);
            this.propagator.setInitialState(this.initialState);
        } catch (final PatriusException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    /**
     * Existing unit test teardown, DO NOT MERGE within Orekit.
     */
    @After
    public void tearDown() {
        this.iniDate = null;
        this.initialState = null;
        this.propagator = null;
    }

    /*
     * ****
     * The unit tests below are to be merged within Orekit eventually. Consider
     * handling the SIRIUS specific data in some proper way. ****
     */

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of the Eclipse detector
         * 
         * @featureDescription Validation of the Eclipse detector
         * 
         * @coveredRequirements DV-EVT_140
         */
        VALIDATION_ECLIPSE_DETECTOR;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#EclipseDetector(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, double, fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, double, double, double, double)}
     * 
     * @description code coverage for the second EclipseDetector constructor
     * 
     * @input misc
     * 
     * @output an {@link EclipseDetector} instance
     * 
     * @testPassCriteria the instance is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testEclipseDetectorCtor3() throws PatriusException {
        final double expectedMaxCheck = 543.21;
        final double expectedThreshold = 1.e-5;
        final EclipseDetector epct3 = new EclipseDetector(
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1,
            expectedMaxCheck, expectedThreshold);
        Assert.assertEquals(expectedMaxCheck, epct3.getMaxCheckInterval(), 0.);
        Assert.assertEquals(expectedThreshold, epct3.getThreshold(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ECLIPSE_DETECTOR}
     * 
     * @testedMethod {@link EclipseDetector#getOccultingRadius()}
     * @testedMethod {@link EclipseDetector#getOcculted()}
     * @testedMethod {@link EclipseDetector#getOccultedRadius()}
     * @testedMethod {@link EclipseDetector#isTotalEclipse()}
     * 
     * @description code coverage for several {@link EclipseDetector} simple
     *              getters not covered by unit tests
     * 
     * @input misc
     * 
     * @output an {@link EclipseDetector} instance
     * 
     * @testPassCriteria got matching values from the instance
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testEclipseDetectorMiscGetters() throws PatriusException {
        final EclipseDetector epmisc = new EclipseDetector(
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1,
            AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD);
        final PVCoordinatesProvider occulting = epmisc.getOcculting();
        Assert.assertEquals(CelestialBodyFactory.getEarth(), occulting);
        final PVCoordinatesProvider occulted = epmisc.getOcculted();
        Assert.assertEquals(CelestialBodyFactory.getSun(), occulted);
        final double occultedRadius = epmisc.getOccultedRadius();
        Assert.assertEquals(this.sunRadius, occultedRadius, 0.);
        final boolean totEc = epmisc.isTotalEclipse();
        Assert.assertEquals(false, totEc);
    }

    /**
     * @throws PatriusException
     *         problem to the eclipse detector creation
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
        final EclipseDetector detector = new EclipseDetector(
            new ConstantVectorDirection(new Vector3D(1, 30, 1),
                FramesFactory.getGCRF()),
            CelestialBodyFactory.getSun(), 120000, 1e-10, 1e-9);

        Assert.assertEquals(
            120000.0,
            detector.getOccultingRadiusProvider().getApparentRadius(new ConstantPVCoordinatesProvider(new Vector3D(1, 1, 1), FramesFactory.getGCRF()),
                AbsoluteDate.J2000_EPOCH, CelestialBodyFactory.getSun(), PropagationDelayType.INSTANTANEOUS), 0);
    }
}
