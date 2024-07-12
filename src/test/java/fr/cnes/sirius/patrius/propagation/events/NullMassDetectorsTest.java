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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class NullMassDetectorsTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Validation of the null mass detector
         * 
         * @featureDescription test the null mass detector
         * 
         * @coveredRequirements
         */
        NULL_MASS_DETECTOR,

        /**
         * @featureTitle Validation of the null mass part detector
         * 
         * @featureDescription test the null mass part detector
         * 
         * @coveredRequirements
         */
        NULL_MASS_PART_DETECTOR;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#NULL_MASS_DETECTOR}
     * 
     * @testedMethod {@link NullMassDetector#NullMassDetector(double, double, MassProvider)}
     * @testedMethod {@link NullMassDetector#NullMassDetector(int, double, double, MassProvider)}
     * 
     * @description cover the NullMassDetector methods
     * 
     * @input NullMassDetector instances
     * 
     * @output
     * 
     * @testPassCriteria coverage
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testNullMassDetector() {
        final MassProvider mass = new SimpleMassModel(100.0, "Satellite");
        NullMassDetector detector = new NullMassDetector(AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, mass);
        final NullMassDetector detector2 = (NullMassDetector) detector.copy();
        Assert.assertNotNull(detector2);

        detector = new NullMassDetector(0, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, mass);
        Assert.assertNotNull(detector);
    }

    /**
     * @throws PatriusException
     * @throws IllegalArgumentException
     * @testType UT
     * 
     * @testedFeature {@link features#NULL_MASS_PART_DETECTOR}
     * 
     * @testedMethod {@link NullMassPartDetector#NullMassPartDetector(double, double, MassProvider, String)}
     * @testedMethod {@link NullMassPartDetector#NullMassPartDetector(int, double, double, MassProvider, String)}
     * @testedMethod {@link NullMassPartDetector#getPartName()}
     * @testedMethod {@link NullMassPartDetector#resetState(fr.cnes.sirius.patrius.propagation.SpacecraftState)}
     * 
     * @description cover the NullMassPartDetector methods
     * 
     * @input NullMassPartDetector instances
     * 
     * @output
     * 
     * @testPassCriteria coverage
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test
    public void testNullMassPartDetector() throws IllegalArgumentException, PatriusException {
        final String part = "Satellite";
        final MassProvider mass = new SimpleMassModel(100.0, part);
        NullMassPartDetector detector = new NullMassPartDetector(AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, mass, part);
        final NullMassPartDetector detector2 = (NullMassPartDetector) detector.copy();
        Assert.assertNotNull(detector2);

        detector = new NullMassPartDetector(0, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD, mass, part);
        Assert.assertNotNull(detector);

        Assert.assertEquals(part, detector.getPartName());

        // resetState coverage
        final Frame gcrf = FramesFactory.getGCRF();
        final double a = Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 300e3;
        final double e = 0.001;
        final double inc = MathLib.toRadians(45);
        final double pom = MathLib.toRadians(3);
        final double gom = MathLib.toRadians(2);
        final double M = MathLib.toRadians(1);
        final Orbit orbit = new KeplerianOrbit(a, e, inc, pom, gom, M, PositionAngle.TRUE,
            gcrf, AbsoluteDate.J2000_EPOCH, Constants.WGS84_EARTH_MU);
        final SpacecraftState state = detector.resetState(new SpacecraftState(orbit, mass));
        // check mass is null
        Assert.assertEquals(0., state.getMass(part));
        Assert.assertEquals(0., mass.getMass(part));

        // test eventOccurred coverage
        Assert.assertEquals(Action.RESET_STATE, detector.eventOccurred(state, true, true));
    }

}
