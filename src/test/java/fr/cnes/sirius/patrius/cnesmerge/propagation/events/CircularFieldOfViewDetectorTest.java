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
 * @history creation 14/11/11
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
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
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class CircularFieldOfViewDetectorTest {

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
         * @featureTitle Validation of the circular field of view detector
         * 
         * @featureDescription Validation of the circular field of view detector
         * 
         * @coveredRequirements DV-EVT_150
         */
        VALIDATION_CIRCULAR_FOV_DETECTOR;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_CIRCULAR_FOV_DETECTOR}
     * 
     * @testedMethod {@link CircularFieldOfViewDetector#getMaxCheckInterval()}
     * @testedMethod {@link CircularFieldOfViewDetector#getPVTarget()}
     * @testedMethod {@link CircularFieldOfViewDetector#getCenter()}
     * @testedMethod {@link CircularFieldOfViewDetector#getHalfAperture()}
     * 
     * @description test for the simple getters of {@link CircularFieldOfViewDetector}
     * 
     * @input constructor parameters
     * 
     * @output getters' results from a {@link CircularFieldOfViewDetector} instance
     * 
     * @testPassCriteria getters' results as expected
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testCFOVDetectorSimpleGetters() throws PatriusException {
        final double maxCheck = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.PLUS_I;
        final double aperture = MathLib.toRadians(35);
        final CircularFieldOfViewDetector detector =
            new CircularFieldOfViewDetector(sunPV, center, aperture, maxCheck);
        Assert.assertEquals(maxCheck, detector.getMaxCheckInterval(), 0.);
        Assert.assertEquals(sunPV, detector.getPVTarget());
        Assert.assertEquals(center, detector.getCenter());
        Assert.assertEquals(aperture, detector.getHalfAperture(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#VALIDATION_CIRCULAR_FOV_DETECTOR}
     * 
     * @testedMethod {@link CircularFieldOfViewDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description unit test for eventOccured
     * 
     * @input constructor parameters and a {@link SpacecraftState} instance
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
    public final void testEventOccured() throws PatriusException {
        final double maxCheck = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.PLUS_I;
        final double aperture = MathLib.toRadians(35);
        final CircularFieldOfViewDetector detector =
            new CircularFieldOfViewDetector(sunPV, center, aperture, maxCheck);
        final SpacecraftState someSPState = new SpacecraftState(this.initialOrbit);
        // We check the two possible eventOccured outputs
        Action rez = detector.eventOccurred(someSPState, true, true);
        Assert.assertEquals(Action.CONTINUE, rez);
        rez = detector.eventOccurred(someSPState, false, true);
        Assert.assertEquals(Action.STOP, rez);
    }

}
