/**
 *
 * Copyright 2011-2022 CNES
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
 * @history creation 18/10/2012
 *
 * HISTORY
 * VERSION:4.13.5:DM:DM-319:03/07/2024:[PATRIUS] Assurer la compatibilite ascendante de la v4.13
 * VERSION:4.13.2:DM:DM-222:08/03/2024:[PATRIUS] Assurer la compatibilité ascendante
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import static org.junit.Assert.assertArrayEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration.PatriusVersionCompatibility;

/**
 * Test class for {@link CIPCoordinatesGenerator}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: CIPCoordinatesGeneratorTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class CIPCoordinatesGeneratorTest {

    private CIPCoordinatesGenerator generator03;
    private CIPCoordinatesGenerator generator10;

    @Before
    public void testCIPCoordinatesGenerator() {
        this.generator03 = new CIPCoordinatesGenerator(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003), 12, 43200);
        this.generator10 = new CIPCoordinatesGenerator(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010), 12, 43200);
    }

    @Test
    public void testGenerateBackwards() {

        // Date
        final AbsoluteDate date = new AbsoluteDate(2010, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());

        final List<CIPCoordinates> list = this.generator10.generate(null, date);

        Assert.assertEquals(14, list.size());
        AbsoluteDate origin = new AbsoluteDate(2010, 3, 2, 0, 0, 0.0, TimeScalesFactory.getTAI());
        origin = new AbsoluteDate(origin.getComponents(TimeScalesFactory.getTAI()).getDate(), new TimeComponents(origin
            .getComponents(TimeScalesFactory.getTAI()).getTime().getHour(), 0, 0), TimeScalesFactory.getTAI());

        final Iterator<CIPCoordinates> it = list.iterator();
        while (it.hasNext()) {
            Assert.assertEquals(0, it.next().getDate().durationFrom(origin), Precision.EPSILON);
            origin = origin.shiftedBy(43200);
        }
    }

    @Test
    public void testGenerateForward() {

        // Date
        final AbsoluteDate date = new AbsoluteDate(2010, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        final CIPCoordinates dummy = new CIPCoordinates(date.shiftedBy(86400), 0, 0, 0, 0, 0, 0);
        final List<CIPCoordinates> list = this.generator03.generate(dummy, date);

        Assert.assertEquals(9, list.size());
    }

    @Test
    public void testComputePoleCoordinates() {
        
        PatriusConfiguration.setPatriusCompatibilityMode(PatriusVersionCompatibility.NEW_MODELS);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        // test against original implementation
        final double[] cip10ref = { 4.903997475506334E-4, 4.166125167417491E-5, -1.4432494003292058E-8 };
        final double[] cip03ref = { 4.903997402091521E-4, 4.1661387967743685E-5, -1.4432569047616784E-8 };
        assertArrayEquals(cip03ref, new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003)
            .getCIPCoordinates(date).getCIPMotion(), Precision.EPSILON);
        assertArrayEquals(cip10ref, new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010)
            .getCIPCoordinates(date).getCIPMotion(), Precision.EPSILON);

        final double[] cip03dvRef = { 5.937686116830366E-12, 6.514079689041986E-13, -3.7719714412691603E-17 };
        final double[] cip10dvRef = { 5.937685565984846E-12, 6.514070432335979E-13, -3.7719099455199287E-17 };
        assertArrayEquals(cip03dvRef, new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003)
            .getCIPCoordinates(date).getCIPMotionTimeDerivatives(), Precision.EPSILON);
        assertArrayEquals(cip10dvRef, new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010)
            .getCIPCoordinates(date).getCIPMotionTimeDerivatives(), Precision.EPSILON);
    }
}
