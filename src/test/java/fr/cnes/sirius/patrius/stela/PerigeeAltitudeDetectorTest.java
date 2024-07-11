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
 * HISTORY
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: test modification
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link PerigeeAltitudeDetector}.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class PerigeeAltitudeDetectorTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Perigee altitude detector
         * 
         * @featureDescription Perigee altitude detector
         * 
         * @coveredRequirements
         */
        PERIG_ALT_DETECTOR
    }

    /** Position epsilon. */
    private static final double POS_EPS = 1e-12;
    /** Initial state. */
    private static SpacecraftState initialState;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#PerigeeAltitudeDetector(double, double)}
     * @testedMethod {@link PerigeeAltitudeDetector#getAltitude()}
     * @testedMethod {@link PerigeeAltitudeDetector#getEarthRadius()}
     * 
     * @description unit test for a constructor
     * 
     * @input constructor attributes
     * 
     * @output detector instance
     * 
     * @testPassCriteria values of constructor attributes are preserved with perfect accuracy.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testPerigeeAltitudeDetectorDoubleDouble() {
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = (PerigeeAltitudeDetector) new PerigeeAltitudeDetector(refAlt, refRad).copy();
        Assert.assertEquals(refAlt, ed.getAltitude(), 0.);
        Assert.assertEquals(refRad, ed.getEarthRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#PerigeeAltitudeDetector(double, double, double)}
     * @testedMethod {@link PerigeeAltitudeDetector#getAltitude()}
     * @testedMethod {@link PerigeeAltitudeDetector#getEarthRadius()}
     * 
     * @description unit test for a constructor
     * 
     * @input constructor attributes
     * 
     * @output detector instance
     * 
     * @testPassCriteria values of constructor attributes are preserved with perfect accuracy.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testPerigeeAltitudeDetectorDoubleDoubleDouble() {
        final double refChk = 7.;
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refChk, refAlt, refRad);
        Assert.assertEquals(refChk, ed.getMaxCheckInterval(), 0.);
        Assert.assertEquals(refAlt, ed.getAltitude(), 0.);
        Assert.assertEquals(refRad, ed.getEarthRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#PerigeeAltitudeDetector(double, double, double, Action, Action)}
     * @testedMethod {@link PerigeeAltitudeDetector#getAltitude()}
     * @testedMethod {@link PerigeeAltitudeDetector#getEarthRadius()}
     * 
     * @description unit test for a constructor
     * 
     * @input constructor attributes
     * 
     * @output detector instance
     * 
     * @testPassCriteria values of constructor attributes are preserved with perfect accuracy.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testPerigeeAltitudeDetectorDoubleDoubleDoubleActionAction() {
        final double refChk = 7.;
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refChk,
            10.e-10, refAlt, refRad, Action.CONTINUE, Action.STOP);
        Assert.assertEquals(refChk, ed.getMaxCheckInterval(), 0.);
        Assert.assertEquals(refAlt, ed.getAltitude(), 0.);
        Assert.assertEquals(refRad, ed.getEarthRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#PerigeeAltitudeDetector(double, double, double, double)}
     * @testedMethod {@link PerigeeAltitudeDetector#getAltitude()}
     * @testedMethod {@link PerigeeAltitudeDetector#getEarthRadius()}
     * 
     * @description unit test for a constructor
     * 
     * @input constructor attributes
     * 
     * @output detector instance
     * 
     * @testPassCriteria values of constructor attributes are preserved with perfect accuracy.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testPerigeeAltitudeDetectorDoubleDoubleDoubleDouble() {
        final double refThr = 6.;
        final double refChk = 7.;
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refChk, refThr, refAlt, refRad);
        Assert.assertEquals(refThr, ed.getThreshold(), 0.);
        Assert.assertEquals(refChk, ed.getMaxCheckInterval(), 0.);
        Assert.assertEquals(refAlt, ed.getAltitude(), 0.);
        Assert.assertEquals(refRad, ed.getEarthRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#PerigeeAltitudeDetector(double, double, double, double, OrbitNatureConverter)}
     * @testedMethod {@link PerigeeAltitudeDetector#getAltitude()}
     * @testedMethod {@link PerigeeAltitudeDetector#getEarthRadius()}
     * 
     * @description unit test for a constructor
     * 
     * @input constructor attributes
     * 
     * @output detector instance
     * 
     * @testPassCriteria values of constructor attributes are preserved with perfect accuracy.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testPerigeeAltitudeDetectorDoubleDoubleDoubleDoubleOrbitNatureConverter() {
        final OrbitNatureConverter onc =
            new OrbitNatureConverter(new ArrayList<StelaForceModel>());
        final double refThr = 6.;
        final double refChk = 7.;
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refChk, refThr, refAlt, refRad, onc);
        Assert.assertEquals(refThr, ed.getThreshold(), 0.);
        Assert.assertEquals(refChk, ed.getMaxCheckInterval(), 0.);
        Assert.assertEquals(refAlt, ed.getAltitude(), 0.);
        Assert.assertEquals(refRad, ed.getEarthRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#PerigeeAltitudeDetector(double, double, double, Action, Action)}
     * @testedMethod {@link PerigeeAltitudeDetector#getAltitude()}
     * @testedMethod {@link PerigeeAltitudeDetector#getEarthRadius()}
     * 
     * @description unit test for a constructor
     * 
     * @input constructor attributes
     * 
     * @output detector instance
     * 
     * @testPassCriteria values of constructor attributes are preserved with perfect accuracy.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testPerigeeAltitudeDetectorDoubleDoubleActionAction() {
        final double refThr = 6.;
        final double refChk = 7.;
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refChk,
            refThr, refAlt, refRad, Action.CONTINUE, Action.STOP);
        Assert.assertEquals(refThr, ed.getThreshold(), 0.);
        Assert.assertEquals(refChk, ed.getMaxCheckInterval(), 0.);
        Assert.assertEquals(refAlt, ed.getAltitude(), 0.);
        Assert.assertEquals(refRad, ed.getEarthRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#PerigeeAltitudeDetector(double, double, double, double, OrbitNatureConverter)}
     * @testedMethod {@link PerigeeAltitudeDetector#getAltitude()}
     * @testedMethod {@link PerigeeAltitudeDetector#getEarthRadius()}
     * 
     * @description unit test for a constructor
     * 
     * @input constructor attributes
     * 
     * @output detector instance
     * 
     * @testPassCriteria values of constructor attributes are preserved with perfect accuracy.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public final void testPerigeeAltitudeDetectorDoubleDoubleDoubleDoubleOrbitNatureConverterActionAction() {
        final OrbitNatureConverter onc =
            new OrbitNatureConverter(new ArrayList<StelaForceModel>());
        final double refThr = 6.;
        final double refChk = 7.;
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refChk, refThr, refAlt, refRad, onc,
            Action.CONTINUE, Action.STOP);
        Assert.assertEquals(refThr, ed.getThreshold(), 0.);
        Assert.assertEquals(refChk, ed.getMaxCheckInterval(), 0.);
        Assert.assertEquals(refAlt, ed.getAltitude(), 0.);
        Assert.assertEquals(refRad, ed.getEarthRadius(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     * 
     * @description unit test for the eventOccured method
     * 
     * @input constructor attributes
     * 
     * @output detector instance
     * 
     * @testPassCriteria returned values of eventOccurred() are as expected.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testEventOccurred() throws PatriusException {
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refAlt, refRad);
        Action act = ed.eventOccurred(null, true, true);
        Assert.assertEquals(Action.CONTINUE, act);
        act = ed.eventOccurred(null, false, true);
        Assert.assertEquals(Action.STOP, act);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#g(SpacecraftState)}
     * 
     * @description unit test for the g method
     * 
     * @input constructor attributes, and a spacecraft state
     * 
     * @output detector instance
     * 
     * @testPassCriteria returned value of g() is as expected.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testG() throws PatriusException {
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refAlt, refRad);
        final double ref = 6557983;
        final double val = ed.g(initialState);
        Assert.assertEquals(ref, val, POS_EPS);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#PERIG_ALT_DETECTOR}
     * 
     * @testedMethod {@link PerigeeAltitudeDetector#g(SpacecraftState)}
     * 
     * @description unit test for the g method, with an orbit nature converter available
     * 
     * @input constructor attributes, including an orbit nature converter, and a spacecraft state
     * 
     * @output detector instance
     * 
     * @testPassCriteria returned value of g() is as expected.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGOsc() throws PatriusException {
        final OrbitNatureConverter onc =
            new OrbitNatureConverter(new ArrayList<StelaForceModel>());
        final double refThr = 6.;
        final double refChk = 7.;
        final double refAlt = 8.;
        final double refRad = 9.;
        final PerigeeAltitudeDetector ed = new PerigeeAltitudeDetector(refChk, refThr, refAlt, refRad, onc);
        final double ref = 6557982.999999999;
        final double val = ed.g(initialState);
        Assert.assertEquals(ref, val, POS_EPS);
    }

    /**
     * Setup before class.
     * 
     * @throws PatriusException
     *         should not happen
     */
    @BeforeClass
    public static void setupBeforeClass() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.),
            TimeScalesFactory.getTAI());
        final double rt = 6378 * 1000;
        final Orbit orbit = new ApsisOrbit(180000.0 + rt, 36000000 + rt, MathLib.toRadians(10.),
            MathLib.toRadians(30.), MathLib.toRadians(20.), MathLib.toRadians(45.),
            PositionAngle.MEAN, FramesFactory.getCIRF(), initDate, Constants.EGM96_EARTH_MU);
        initialState = new SpacecraftState(orbit);
    }

}
