/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
package fr.cnes.sirius.patrius.frames;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @author cardosop
 * 
 */
public class ITRF2008ToGCRFPerfTest {

    @Test
    public void performanceTest() throws PatriusException {
        final List<Vector3D> rezList = new ArrayList<Vector3D>();
        for (int i = 0; i < 20000; i++) {
            // create a bogus position vector
            final Vector3D bogusPos = new Vector3D(i * 123.456, i * -0.25, i * 33.12 * FastMath.PI);
            // convert it from ITRF2008 Frame to GCRF Frame
            final Transform t = FramesFactory.getITRF().getTransformTo(FramesFactory.getGCRF(),
                AbsoluteDate.J2000_EPOCH.shiftedBy(3250 * i));
            final Vector3D convBogusPos = t.transformPosition(bogusPos);
            // save
            rezList.add(convBogusPos);
        }
        // print first
        System.out.println(rezList.get(0));
        // print last
        System.out.println(rezList.get(rezList.size() - 1));
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

}
