/**
 *
 * Copyright 2011-2017 CNES
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
 * @history creation 18/10/2012
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.CIPCoordinatesGenerator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link CIPCoordinatesGenerator}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: TidalCorrectionGeneratorTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 */
public class TidalCorrectionGeneratorTest {

    private TidalCorrectionGenerator generator;

    @Before
    public void testCIPCoordinatesGenerator() throws PatriusException {
        this.generator = new TidalCorrectionGenerator(new IERS2010TidalCorrection(), 8, 3 / 32. * 86400);
    }

    @Test
    public void testGenerateBackwards() throws PatriusException {

        // Date
        final AbsoluteDate date = new AbsoluteDate(2010, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());

        final List<TidalCorrection> list = this.generator.generate(null, date);

        Assert.assertEquals(10, list.size());

        final TimeScale tai = TimeScalesFactory.getTAI();

        final double t = date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY - 37076.5;
        final double tCenter = (3 / 32. * 86400 / Constants.JULIAN_DAY)
            * MathLib.floor(t / (3 / 32. * 86400 / Constants.JULIAN_DAY));

        AbsoluteDate origin = new AbsoluteDate(AbsoluteDate.MODIFIED_JULIAN_EPOCH, (tCenter + 37076.5) *
            Constants.JULIAN_DAY - 3 / 32. * 86400 * 8 / 2, tai);

        final Iterator<TidalCorrection> it = list.iterator();
        while (it.hasNext()) {
            Assert.assertEquals(0, it.next().getDate().durationFrom(origin), Precision.EPSILON);
            origin = origin.shiftedBy(3 / 32. * 86400);
        }

    }

    @Test
    public void testGenerateForward() throws PatriusException {

        // Date
        final AbsoluteDate date = new AbsoluteDate(2010, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        final TidalCorrection dummy = new TidalCorrection(date, null, 0, 0);
        final List<TidalCorrection> list = this.generator.generate(dummy, date);

        Assert.assertEquals(5, list.size());

    }

}
