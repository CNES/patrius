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
 * @history creation 15/11/11
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::FA:485:12/11/2015: Coverage for modified constructors of DihedralFieldOfViewDetector
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.propagation.events;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.DihedralFieldOfViewDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for DihedralFieldOfViewDetector.<br>
 * Class to be merged with the existing DihedralFieldOfViewDetectorTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS follows different conventions regarding unit
 * tests.
 * 
 * @author cardosop
 * 
 * @version $Id: DihedralFieldOfViewDetectorTest.java 17917 2017-09-11 12:55:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class DihedralFieldOfViewDetectorTest {

    // Body mu
    private double mu;

    // Computation date
    private AbsoluteDate initDate;

    // Orbit
    private Orbit initialOrbit;

    // Reference frame = ITRF 2005
    private Frame itrf;

    // Earth center pointing attitude provider
    private BodyCenterPointing earthCenterAttitudeLaw;

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

            // Computation date
            // Satellite position as circular parameters
            this.mu = 3.9860047e14;

            this.initDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

            final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
            final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
            this.initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), this.initDate, this.mu);

            // Reference frame = ITRF 2005
            this.itrf = FramesFactory.getITRF();

            // Create earth center pointing attitude provider */
            this.earthCenterAttitudeLaw = new BodyCenterPointing(this.itrf);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    /*
     * ****
     * The unit tests below are to be merged within Orekit eventually.
     * Consider handling the SIRIUS specific data in some proper way.
     * ****
     */

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validation of the dihedral FOV detector
         * 
         * @featureDescription Validation of the dihedral FOV detector
         * 
         * @coveredRequirements DV-EVT_150
         */
        VALIDATION_DIHEDRAL_FOV_DETECTOR;
    }

    /**
     * Custom DihedralFieldOfViewDetector for some unit tests.
     * 
     * @return a DihedralFieldOfViewDetector instance
     * 
     * @throws PatriusException
     *         should not happen here
     */
    private DihedralFieldOfViewDetector customDFOVDetector() throws PatriusException {
        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.MINUS_J;
        final Vector3D axis1 = Vector3D.PLUS_K;
        final Vector3D axis2 = Vector3D.PLUS_I;
        final double aperture1 = MathLib.toRadians(28);
        final double aperture2 = MathLib.toRadians(120);

        final DihedralFieldOfViewDetector detector = new DihedralFieldOfViewDetector(sunPV, center, axis1, aperture1,
            axis2, aperture2, maxCheck);
        return detector;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DIHEDRAL_FOV_DETECTOR}
     * 
     * @testedMethod {@link DihedralFieldOfViewDetector#DihedralFieldOfViewDetector(PVCoordinatesProvider, Vector3D, Vector3D, double, Vector3D, double, double)}
     * 
     * @description unit test for DihedralFieldOfViewDetector constructor
     *              with vectors center and axis 1 not strictly orthogonal
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria an OrekitException should be risen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     * 
     * @throws PatriusException
     *         should happen
     */
    @Test(expected = PatriusException.class)
    public void testNonOrthogonalDihedralFieldOfViewAxis1() throws PatriusException {

        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.MINUS_J;
        final Vector3D axis1 = new Vector3D(0.0, -1.0E-20, 0.0);
        final Vector3D axis2 = Vector3D.PLUS_I;
        final double aperture1 = MathLib.toRadians(28);
        final double aperture2 = MathLib.toRadians(120);

        new DihedralFieldOfViewDetector(sunPV, center, axis1,
            aperture1, axis2, aperture2, maxCheck);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DIHEDRAL_FOV_DETECTOR}
     * 
     * @testedMethod {@link DihedralFieldOfViewDetector#DihedralFieldOfViewDetector(PVCoordinatesProvider, Vector3D, Vector3D, double, Vector3D, double, double)}
     * 
     * @description unit test for DihedralFieldOfViewDetector constructor
     *              with vectors center and axis 2 not strictly orthogonal
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria an OrekitException should be risen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     * 
     * @throws PatriusException
     *         should happen
     */
    @Test(expected = PatriusException.class)
    public void testNonOrthogonalDihedralFieldOfAxis2() throws PatriusException {

        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.MINUS_J;
        final Vector3D axis1 = Vector3D.PLUS_K;
        final Vector3D axis2 = new Vector3D(0.0, -1.0E-20, 0.0);
        final double aperture1 = MathLib.toRadians(28);
        final double aperture2 = MathLib.toRadians(120);

        new DihedralFieldOfViewDetector(sunPV, center, axis1,
            aperture1, axis2, aperture2, maxCheck);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DIHEDRAL_FOV_DETECTOR}
     * 
     * @testedMethod {@link DihedralFieldOfViewDetector#DihedralFieldOfViewDetector(PVCoordinatesProvider, Vector3D, Vector3D, double, Vector3D, double, double)}
     * 
     * @description unit test for DihedralFieldOfViewDetector constructor
     *              with vectors center and axis1 not orthogonal at given threshold
     * 
     * @input a {@link Double} instance : threshold against which vectors orthogonality is tested
     * 
     * @output none
     * 
     * @testPassCriteria an OrekitException should be risen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     * 
     * @throws PatriusException
     *         should happen
     */
    @Test(expected = PatriusException.class)
    public void testNonOrthogonalDihedralFieldOfViewEps1() throws PatriusException {

        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.MINUS_J;
        final Vector3D axis1 = new Vector3D(0.0, -1.0E-10, 0.0);
        final Vector3D axis2 = Vector3D.PLUS_I;
        final double aperture1 = MathLib.toRadians(28);
        final double aperture2 = MathLib.toRadians(120);
        final double threshold = 1.0E-12;

        new DihedralFieldOfViewDetector(sunPV, center, axis1,
            aperture1, axis2, aperture2, maxCheck, threshold);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DIHEDRAL_FOV_DETECTOR}
     * 
     * @testedMethod {@link DihedralFieldOfViewDetector#DihedralFieldOfViewDetector(PVCoordinatesProvider, Vector3D, Vector3D, double, Vector3D, double, double)}
     * 
     * @description unit test for DihedralFieldOfViewDetector constructor
     *              with vectors center and axis2 not orthogonal at given threshold
     * 
     * @input a {@link Double} instance : threshold against which vectors orthogonality is tested
     * 
     * @output none
     * 
     * @testPassCriteria an OrekitException should be risen
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     * 
     * @throws PatriusException
     *         should happen
     */
    @Test(expected = PatriusException.class)
    public void testNonOrthogonalDihedralFieldOfViewEps2() throws PatriusException {

        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.MINUS_J;
        final Vector3D axis1 = Vector3D.PLUS_K;
        final Vector3D axis2 = new Vector3D(0.0, -1.0E-10, 0.0);
        final double aperture1 = MathLib.toRadians(28);
        final double aperture2 = MathLib.toRadians(120);
        final double threshold = 1.0E-12;

        new DihedralFieldOfViewDetector(sunPV, center, axis1,
            aperture1, axis2, aperture2, maxCheck, threshold);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DIHEDRAL_FOV_DETECTOR}
     * 
     * @testedMethod {@link EventDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description unit test for eventOccured
     * 
     * @input a {@link SpacecraftState} instance
     * 
     * @output two return values of eventOccured for true and false as third parameters
     * 
     * @testPassCriteria the two return values are as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testEventOccured() throws PatriusException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), date, this.mu);
        final SpacecraftState someSPState = new SpacecraftState(orbit);
        final EventDetector detector = this.customDFOVDetector();
        // We check the two possible eventOccured outputs with integration direction forward and backward
        Action rez = detector.eventOccurred(someSPState, true, true);
        Assert.assertEquals(Action.CONTINUE, rez);
        rez = detector.eventOccurred(someSPState, false, true);
        Assert.assertEquals(Action.STOP, rez);
        rez = detector.eventOccurred(someSPState, true, false);
        Assert.assertEquals(Action.CONTINUE, rez);
        rez = detector.eventOccurred(someSPState, false, false);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_DIHEDRAL_FOV_DETECTOR}
     * 
     * @testedMethod {@link DihedralFieldOfViewDetector#getPVTarget()}
     * @testedMethod {@link DihedralFieldOfViewDetector#getCenter()}
     * @testedMethod {@link DihedralFieldOfViewDetector#getAxis1()}
     * @testedMethod {@link DihedralFieldOfViewDetector#getAxis2()}
     * @testedMethod {@link DihedralFieldOfViewDetector#getHalfAperture2()}
     * 
     * @description code coverage for several simple getters
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria returned values as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testDFOVDsimpleGetters() throws PatriusException {
        final DihedralFieldOfViewDetector detector = this.customDFOVDetector();
        // see customDFOVDetector for the expected values
        Assert.assertEquals(CelestialBodyFactory.getSun(), detector.getPVTarget());
        Assert.assertEquals(Vector3D.MINUS_J, detector.getCenter());
        Assert.assertEquals(Vector3D.PLUS_K, detector.getAxis1());
        Assert.assertEquals(MathLib.toRadians(28), detector.getHalfAperture1(), 0.);
        Assert.assertEquals(Vector3D.PLUS_I, detector.getAxis2());
        Assert.assertEquals(MathLib.toRadians(120), detector.getHalfAperture2(), 0.);

        final Vector3D targetPosSat = new Vector3D(-2, 50, 50);
        final Vector3D normalCenterPlane1 = new Vector3D(0, 1, 1);
        final Vector3D normalCenterPlane2 = new Vector3D(0, 1, -1);
        final Vector3D center = new Vector3D(1, 0, 0);
        // Compute the four angles from the four fov boundaries.
        final double angle1 = MathLib.atan2(Vector3D.dotProduct(targetPosSat, normalCenterPlane1),
            Vector3D.dotProduct(targetPosSat, center));
        final double angle2 = MathLib.atan2(Vector3D.dotProduct(targetPosSat, normalCenterPlane2),
            Vector3D.dotProduct(targetPosSat, center));
        final double g = MathLib.min(FastMath.PI / 2 - MathLib.abs(angle1), FastMath.PI / 2 - MathLib.abs(angle2));
        System.out.println(g);

    }
}
