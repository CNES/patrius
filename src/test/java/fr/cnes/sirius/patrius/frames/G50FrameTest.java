/**
 * Copyright 2011-2023 CNES
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
 * VERSION:4.13:DM:DM-68:08/12/2023:[PATRIUS] Ajout du repere G50 CNES
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980Entry;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOP1980History;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistory;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPInterpolators;
import fr.cnes.sirius.patrius.frames.transformations.G50Provider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link G50Provider}.
 */
public class G50FrameTest {

    /**
     * Check that G50 frame returns the same result as Veis 1950 frame when UT1 - UTC shift is 0.
     */
    @Test
    public void testVsVeis() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        // Initialization
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 04, 06),
            new TimeComponents(07, 51, 28.386009),
            TimeScalesFactory.getUTC());
        
        // Set UT1 - UTC shift to 0
        final FramesConfigurationBuilder builder = 
                new FramesConfigurationBuilder(FramesConfigurationFactory.getIERS2003Configuration(true));
        final EOPHistory eopHistory = new EOP1980History(EOPInterpolators.LINEAR);
        final double ut1MinusUtc = 0.;
        eopHistory.addEntry(new EOP1980Entry(date.shiftedBy(-1.), ut1MinusUtc, 0, 0, 0, 0, 0));
        eopHistory.addEntry(new EOP1980Entry(date, ut1MinusUtc, 0, 0, 0, 0, 0));
        eopHistory.addEntry(new EOP1980Entry(date.shiftedBy(1.), ut1MinusUtc, 0, 0, 0, 0, 0));
        builder.setEOPHistory(eopHistory);
        FramesFactory.setConfiguration(builder.getConfiguration());

        // Transform
        final Transform t = FramesFactory.getVeis1950().getTransformTo(FramesFactory.getG50(), date);

        // Check
        Assert.assertEquals(Rotation.distance(t.getRotation(), Rotation.IDENTITY), 0., 0.);
    }
}
