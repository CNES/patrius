/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class BackAndForthDetectorTest {

    @Test
    public void testBackAndForth() throws PatriusException {

        final TimeScale utc = TimeScalesFactory.getUTC();

        final AbsoluteDate date0 = new AbsoluteDate(2006, 12, 27, 12, 0, 0.0, utc);
        final AbsoluteDate date1 = new AbsoluteDate(2006, 12, 27, 22, 50, 0.0, utc);
        final AbsoluteDate date2 = new AbsoluteDate(2006, 12, 27, 22, 58, 0.0, utc);

        // Orbit
        final double a = 7274000.;
        final double e = 0.00127;
        final double i = MathLib.toRadians(90.);
        final double w = MathLib.toRadians(0.);
        final double raan = MathLib.toRadians(12.5);
        final double lM = MathLib.toRadians(60.);
        final Orbit iniOrb = new KeplerianOrbit(a, e, i, w, raan, lM,
            PositionAngle.MEAN, FramesFactory.getEME2000(), date0,
            Constants.WGS84_EARTH_MU);

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(iniOrb);

        // Station
        final GeodeticPoint stationPosition = new GeodeticPoint(MathLib.toRadians(0.), MathLib.toRadians(100.), 110.);
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF());
        final TopocentricFrame stationFrame = new TopocentricFrame(earth, stationPosition, "");

        // Detector
        final VisibilityDetector visiDetector = new VisibilityDetector(MathLib.toRadians(10.), stationFrame);
        propagator.addEventDetector(visiDetector);

        // Forward propagation (AOS + LOS)
        propagator.propagate(date1);
        propagator.propagate(date2);
        // Backward propagation (AOS + LOS)
        propagator.propagate(date1);
        propagator.propagate(date0);

        Assert.assertEquals(4, visiDetector.getVisiNb());

    }

    private static class VisibilityDetector extends ElevationDetector {
        private static final long serialVersionUID = 8739302131525333416L;
        private int _visiNb;

        public VisibilityDetector(final double elevation, final TopocentricFrame topo) {
            super(elevation, topo);
            this._visiNb = 0;
        }

        @Override
        public Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException
        {
            this._visiNb++;
            return Action.CONTINUE;
        }

        public int getVisiNb() {
            return this._visiNb;
        }
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
