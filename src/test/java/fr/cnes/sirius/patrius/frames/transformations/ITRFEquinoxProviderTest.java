/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class ITRFEquinoxProviderTest {

    @Test
    public void testEquinoxVersusCIO() throws PatriusException {
        final Frame itrfEquinox = FramesFactory.getITRFEquinox();
        final Frame itrfCIO = FramesFactory.getITRF();
        final AbsoluteDate start = new AbsoluteDate(2011, 4, 10, TimeScalesFactory.getUTC());
        final AbsoluteDate end = new AbsoluteDate(2011, 7, 4, TimeScalesFactory.getUTC());
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(10000)) {
            final double angularOffset =
                itrfEquinox.getTransformTo(itrfCIO, date).getRotation().getAngle();
            Assert.assertEquals(0, angularOffset / Constants.ARC_SECONDS_TO_RADIANS, 0.07);
        }

        // Cover the getTransform methods of ITRFEquinoxProvider class:
        final Transform transform = new ITRFEquinoxProvider().getTransform(start);
        Assert.assertNotNull(transform);
        final Transform transform2 = new ITRFEquinoxProvider().getTransform(start, false);
        Assert.assertNotNull(transform2);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("rapid-data-columns");

        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
