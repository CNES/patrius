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
 * HISTORY
 * VERSION:4.13:DM:DM-30:08/12/2023:[PATRIUS] Deplacement et modification des
 * classes TimeStampedString et TimeStampedDouble
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

public class TimeStampedDoubleTest {

    @Test
    public void testTimeStampedDouble() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double[] doubles = new double[] { 1.2, 3.5, 4.7 };

        final TimeStampedDouble timeStamped = new TimeStampedDouble(doubles, date);
        Assert.assertEquals(date, timeStamped.getDate());
        Assert.assertEquals(doubles, timeStamped.getDoubles());
    }
    
    @Test
    public void testTimeStampedDouble_oneDouble() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double double1 =  3.5 ;

        final TimeStampedDouble timeStamped = new TimeStampedDouble(double1, date);
        Assert.assertEquals(date, timeStamped.getDate());
        Assert.assertEquals(double1, timeStamped.getDouble(),0);
    }
}
