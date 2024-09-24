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
 * @history created 12/09/18
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This test is made in order to highlight an error when computing the PV coordinates of a celestial body. Two binary
 * files are available in the test resources (unxp1950.405 and unxp2000.405).
 * 
 * For the two dates "1999/12/31 0h00" and "2049/12/31 0h00" the computation works normally.
 * 
 * For the dates in the interval [1999/12/31 0h00, 2002/12/31 0h00] an error occurs with the message
 * "org.orekit.errors.TimeStampedCacheException: impossible de générer des donnée après le 2000-01-20T23:58:55.816 at
 * org.orekit.utils.TimeStampedCache$Slot.appendAtEnd(TimeStampedCache.java:764) at
 * org.orekit.utils.TimeStampedCache$Slot.getNeighbors(TimeStampedCache.java:593) at
 * org.orekit.utils.TimeStampedCache.getNeighbors(TimeStampedCache.java:282) at
 * org.orekit.bodies.JPLEphemeridesLoader$JPLCelestialBody.getPVCoordinates(JPLEphemeridesLoader.java:1047) at
 * fr.cnes.sirius.bugs.TimeStampedCacheBug.test(TimeStampedCacheBug.java:85)"
 * 
 * Corrected by Orekit patch applied on JPLEphemeridesLoader in Orekit (issue 113).
 * 
 * @author mercadierg
 * 
 * @version $Id: TimeStampedCacheBug.java 17916 2017-09-11 12:40:47Z bignon $
 * 
 * @since 1.2
 * 
 */
public class TimeStampedCacheBug {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Time Stamped Cache Bug
         * 
         * @featureDescription Elliptic field of view to be used in sensors description
         * 
         * @coveredRequirements
         */
        TIMESTAMPEDCACHE
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TIMESTAMPEDCACHE}
     * 
     * @testedMethod {@link AbsoluteDate}
     * 
     * @description test of time stamped cache bug
     * 
     * @input none
     * 
     * @output none
     * 
     * @testPassCriteria none
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void test() throws PatriusException {
        /**
         * Orekit data initialisation
         */
        Utils.setDataRoot("regular-dataCNES-2003");

        final CelestialBody moon = CelestialBodyFactory.getMoon();

        // 1999/12/31 0h00
        final AbsoluteDate initDate = new AbsoluteDate(1999, 12, 31, 00, 00, 00, TimeScalesFactory.getUTC());
        // PV coordinates
        System.out.println("PV Coordinates (1999/12/31 0h00) = "
            + moon.getPVCoordinates(initDate, FramesFactory.getGCRF()));

        // 2049/12/31 0h00
        final AbsoluteDate otherDate = new AbsoluteDate(2049, 12, 31, 00, 00, 00, TimeScalesFactory.getUTC());
        // PV coordinates
        System.out.println("PV Coordinates (2049/12/31 0h00) = "
            + moon.getPVCoordinates(otherDate, FramesFactory.getGCRF()));

        // 3 years from initDate
        AbsoluteDate currentDate = initDate;

        for (int i = 0; i < 1096; i++) {
            currentDate = currentDate.shiftedBy(86400.);
            moon.getPVCoordinates(currentDate, FramesFactory.getGCRF());
        }
    }
}
