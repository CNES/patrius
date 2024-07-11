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
 * @history creation 19/10/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

public class NoEOP2000Test {

    private final double eps = Precision.EPSILON;

    @Test
    public void testIterator() {
        final Iterator<TimeStamped> it = new NoEOP2000History().iterator();

        assertFalse(it.hasNext());
        assertNull(it.next());

        // for coverage
        it.remove();
    }

    @Test
    public void testSize() {
        assertEquals(0, new NoEOP2000History().size());
    }

    @Test
    public void testGetEOPInterpolationMethod() {
        assertEquals(new NoEOP2000History().getEOPInterpolationMethod(), EOPInterpolators.LINEAR);
    }

    @Test
    public void testGetStartDate() {
        assertEquals(AbsoluteDate.PAST_INFINITY, new NoEOP2000History().getStartDate());
    }

    @Test
    public void testGetEndDate() {
        assertEquals(AbsoluteDate.FUTURE_INFINITY, new NoEOP2000History().getEndDate());
    }

    @Test
    public void testGetUT1MinusUTC() {
        Utils.setDataRoot("regular-data");
        assertEquals(0, new NoEOP2000History().getUT1MinusUTC(new AbsoluteDate()), this.eps);
    }

    @Test
    public void testGetLOD() {
        assertEquals(0, new NoEOP2000History().getLOD(new AbsoluteDate()), this.eps);
    }

    @Test
    public void testGetPoleCorrection() {
        assertEquals(PoleCorrection.NULL_CORRECTION,
            new NoEOP2000History().getPoleCorrection(AbsoluteDate.CCSDS_EPOCH));
    }

    @Test
    public void testGetNutationCorrection() {
        assertEquals(NutationCorrection.NULL_CORRECTION,
            new NoEOP2000History().getNutationCorrection(AbsoluteDate.CCSDS_EPOCH));
    }

    /**
     * Test method isActive().
     */
    @Test
    public void testGetters() {
        final EOP2000History history = new NoEOP2000History();
        Assert.assertEquals(false, history.isActive());
    }

}
