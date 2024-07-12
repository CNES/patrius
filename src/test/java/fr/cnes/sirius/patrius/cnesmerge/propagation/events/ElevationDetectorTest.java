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
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
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
import fr.cnes.sirius.patrius.propagation.events.ElevationDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for ElevationDetector.<br>
 * Class to be merged with the existing ElevationDetectorTest in Orekit,
 * <strong>only for the added unit tests themselves</strong>, since SIRIUS follows
 * different conventions regarding unit tests.
 * 
 * @author cardosop
 * 
 * @version $Id: ElevationDetectorTest.java 17917 2017-09-11 12:55:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ElevationDetectorTest {

    private double mu;
    private double ae;

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        this.mu = 3.9860047e14;
        this.ae = 6.378137e6;
    }

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
         * @featureTitle Validation of the Elevation detector
         * 
         * @featureDescription Validation of the Elevation detector
         * 
         * @coveredRequirements DV-EVT_130
         */
        VALIDATION_ELEVATION_DETECTOR;
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
        final BodyShape earth = new OneAxisEllipsoid(this.ae, f, ITRF2005);
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(48.833),
            MathLib.toRadians(2.333),
            0.0);
        final TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");
        return topo;
    }

    /**
     * custom SpacecraftState for some UT.
     * 
     * @return a custom SpacecraftState
     * @throws PatriusException
     *         should not happen
     */
    private SpacecraftState customSpacecraftState() throws PatriusException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), date, this.mu);
        final SpacecraftState state = new SpacecraftState(orbit);
        return state;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ELEVATION_DETECTOR}
     * 
     * @testedMethod {@link ElevationDetector#ElevationDetector(double, TopocentricFrame, double, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters
     * 
     * @output an {@link ElevationDetector}
     * 
     * @testPassCriteria the {@link ElevationDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testElevationDetectorDoubleDoubleDoubleFrame() throws PatriusException {
        final double expectedMaxCheck = 543.21;
        final double expectedThr = 0.00054321;
        final double expectedElevation = MathLib.toRadians(0.0);
        final TopocentricFrame topo = this.customTopoFrame();
        final ElevationDetector detector =
            new ElevationDetector(expectedElevation, topo, expectedMaxCheck, expectedThr);
        Assert.assertEquals(expectedMaxCheck, detector.getMaxCheckInterval(), 0.);
        Assert.assertEquals(expectedThr, detector.getThreshold(), 0.);
        Assert.assertEquals(expectedElevation, detector.getElevation(), 0.);
        Assert.assertEquals(topo, detector.getTopocentricFrame());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ELEVATION_DETECTOR}
     * 
     * @testedMethod {@link ElevationDetector#ElevationDetector(double, TopocentricFrame, double)}
     * 
     * @description simple constructor test
     * 
     * @input constructor parameters
     * 
     * @output an {@link ElevationDetector}
     * 
     * @testPassCriteria the {@link ElevationDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testElevationDetectorDoubleDoubleFrame() throws PatriusException {
        final double expectedMaxCheck = 543.21;
        final double expectedThr = 1.e-6;
        final double expectedElevation = MathLib.toRadians(0.0);
        final TopocentricFrame topo = this.customTopoFrame();
        final ElevationDetector detector =
            new ElevationDetector(expectedElevation, topo, expectedMaxCheck);
        Assert.assertEquals(expectedMaxCheck, detector.getMaxCheckInterval(), 0.);
        Assert.assertEquals(expectedThr, detector.getThreshold(), 0.);
        Assert.assertEquals(expectedElevation, detector.getElevation(), 0.);
        Assert.assertEquals(topo, detector.getTopocentricFrame());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_ELEVATION_DETECTOR}
     * 
     * @testedMethod {@link ElevationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description tests {@link ElevationDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @input constructor parameters
     * 
     * @output an {@link ElevationDetector}
     * 
     * @testPassCriteria the {@link ElevationDetector} is successfully created
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testEventOccurred() throws PatriusException {
        final Action expectedAction = Action.STOP;
        final boolean increasing = false;
        final boolean forward = true;
        final double maxCheck = 543.21;
        final double elevation = MathLib.toRadians(0.0);
        final TopocentricFrame topo = this.customTopoFrame();
        final SpacecraftState state = this.customSpacecraftState();
        final ElevationDetector detector =
            new ElevationDetector(elevation, topo, maxCheck);
        Assert.assertEquals(expectedAction, detector.eventOccurred(state, increasing, forward));
    }

}
