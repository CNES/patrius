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
 * @history creation 14/11/11
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.cnesmerge.propagation.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.ApparentElevationDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for ApparentElevationDetector.<br>
 * Class to be merged with the existing ApparentElevationDetectorTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS follows different conventions regarding unit
 * tests.
 * 
 * @author cardosop
 * 
 * @version $Id: ApparentElevationDetectorTest.java 17917 2017-09-11 12:55:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ApparentElevationDetectorTest {

    /*
     * The following code is lifted form Orekit.
     * It should NOT BE MERGED BACK into Orekit!
     */

    /** UT data. */
    private double mu;
    /** UT data. */
    private double ae;

    /**
     * Existing unit test setup, DO NOT MERGE within Orekit.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        this.mu = 3.9860047e14;
        this.ae = 6.378137e6;
    }

    /**
     * Existing unit test teardown, DO NOT MERGE within Orekit.
     */
    @After
    public void tearDown() {
        this.mu = Double.NaN;
        this.ae = Double.NaN;
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
         * @featureTitle Validation of the apparent elevation detector
         * 
         * @featureDescription Validation of the apparent elevation detector
         * 
         * @coveredRequirements DV-EVT_130
         */
        VALIDATION_APPARENT_ELEVATION_DETECTOR;
    }

    /**
     * custom TopocentricFrame for some UT.
     * 
     * @return a custom TopocentricFrame
     * @throws PatriusException
     *         should not happen
     */
    private TopocentricFrame customTopoFrame() throws PatriusException {
        // Earth and frame
        // equatorial radius in meters
        this.ae = 6378137.0;
        // flattening
        final double f = 1.0 / 298.257223563;
        // terrestrial frame at an arbitrary date
        final Frame ITRF2005 = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(this.ae, f, ITRF2005);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(48.833), MathLib.toRadians(2.333), 0., "");
        final TopocentricFrame topo = new TopocentricFrame(point, "Gstation");
        return topo;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_APPARENT_ELEVATION_DETECTOR}
     * 
     * @testedMethod {@link ApparentElevationDetector#ApparentElevationDetector(double, TopocentricFrame, double)}
     * 
     * @description code coverage for the second ApparentElevationDetector constructor
     * 
     * @input misc
     * 
     * @output an {@link ApparentElevationDetector} instance
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
    public void testAEDetectorCtor2() throws PatriusException {
        final double expectedMaxCheck = 543.21;
        final TopocentricFrame topo = this.customTopoFrame();
        final ApparentElevationDetector detector =
            new ApparentElevationDetector(MathLib.toRadians(0.0), topo, expectedMaxCheck);
        Assert.assertEquals(expectedMaxCheck, detector.getMaxCheckInterval(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_APPARENT_ELEVATION_DETECTOR}
     * 
     * @testedMethod {@link ApparentElevationDetector#ApparentElevationDetector(double, TopocentricFrame, double, double)}
     * 
     * @description code coverage for the third ApparentElevationDetector constructor
     * 
     * @input misc
     * 
     * @output an {@link ApparentElevationDetector} instance
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
    public void testAEDetectorCtor3() throws PatriusException {
        final double expectedMaxCheck = 543.21;
        final double expectedThr = 0.00054321;
        final TopocentricFrame topo = this.customTopoFrame();
        final ApparentElevationDetector detector =
            new ApparentElevationDetector(MathLib.toRadians(0.0), topo, expectedMaxCheck, expectedThr);
        Assert.assertEquals(expectedMaxCheck, detector.getMaxCheckInterval(), 0.);
        Assert.assertEquals(expectedThr, detector.getThreshold(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_APPARENT_ELEVATION_DETECTOR}
     * 
     * @testedMethod {@link ApparentElevationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description unit test for eventOccured
     * 
     * @input a {@link SpacecraftState} instance
     * 
     * @output two return values of eventOccured
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
        final TopocentricFrame topo = this.customTopoFrame();
        final ApparentElevationDetector detector =
            new ApparentElevationDetector(MathLib.toRadians(0.0), topo);
        final SpacecraftState someSPState = new SpacecraftState(orbit);
        // We check the two possible eventOccured outputs
        Action rez = detector.eventOccurred(someSPState, true, true);
        Assert.assertEquals(Action.CONTINUE, rez);
        rez = detector.eventOccurred(someSPState, false, true);
        Assert.assertEquals(Action.STOP, rez);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_APPARENT_ELEVATION_DETECTOR}
     * 
     * @testedMethod {@link ApparentElevationDetector#getElevation()}
     * @testedMethod {@link ApparentElevationDetector#getTopocentricFrame()}
     * @testedMethod {@link ApparentElevationDetector#getPressure()}
     * @testedMethod {@link ApparentElevationDetector#getTemperature()}
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
    public void testAEDsimpleGetters() throws PatriusException {
        final double expectedMaxCheck = 543.21;
        final double expectedThr = 0.00054321;
        final double expectedElevation = MathLib.toRadians(0.0);
        final TopocentricFrame topo = this.customTopoFrame();
        final ApparentElevationDetector detector =
            new ApparentElevationDetector(expectedElevation, topo, expectedMaxCheck, expectedThr);
        Assert.assertEquals(expectedElevation, detector.getElevation(), 0.);
        Assert.assertEquals(topo, detector.getTopocentricFrame());
        Assert.assertEquals(ApparentElevationDetector.DEFAULT_PRESSURE, detector.getPressure(), 0.);
        Assert.assertEquals(ApparentElevationDetector.DEFAULT_TEMPERATURE, detector.getTemperature(), 0.);
    }
}
