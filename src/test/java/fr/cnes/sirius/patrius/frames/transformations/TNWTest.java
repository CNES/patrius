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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1867:22/10/2018: Build the validation test - Fix the TNW frame's rotation
 * issue and update the LOFType class
 * END-HISTORY
 */

/**
 */
package fr.cnes.sirius.patrius.frames.transformations;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Check the transform from inertial frame into both QSW and TNW frames compared with a test case
 * values.
 * 
 * @author Thibaut BONIT
 */

public class TNWTest {

    /** Inertial Frame */
    private static final Frame INERTIAL_FRAME = FramesFactory.getGCRF();

    /** Gravitational constant */
    private static final double MU = Constants.WGS84_EARTH_MU;

    /** Tolerance precision on Celestlab values. */
    private static final double E_5 = 1E-5;

    @Test
    public void test() throws PatriusException {
        final PVCoordinates pvReal = new PVCoordinates(new Vector3D(-6342397.708343451,
            3405466.844336187, 35258.43978766135), new Vector3D(-1125.0841972798553,
            -2177.280104481554, 7099.273397402214));
        final PVCoordinates pvCata = new PVCoordinates(new Vector3D(-6342410.144442565,
            3405548.162864295, 35060.597555117565), new Vector3D(-1125.2412780977793,
            -2177.1583118007948, 7099.232814795427));

        final Orbit realOrbit = new KeplerianOrbit(pvReal, INERTIAL_FRAME, new AbsoluteDate(), MU);
        final Orbit cataOrbit = new CartesianOrbit(pvCata, INERTIAL_FRAME, new AbsoluteDate(), MU);

        final LocalOrbitalFrame tnw = new LocalOrbitalFrame(realOrbit.getFrame(), LOFType.TNW,
            realOrbit, "TNW");

        final PVCoordinates pvRelTNW = cataOrbit.getPVCoordinates(tnw);

        final LocalOrbitalFrame qsw = new LocalOrbitalFrame(realOrbit.getFrame(), LOFType.QSW,
            realOrbit, "QSW");

        final PVCoordinates pvRelQSW = cataOrbit.getPVCoordinates(qsw);

        // Test case results from Celestlab
        final PVCoordinates expectedPvRelTNW = new PVCoordinates(new Vector3D(-208.72371,
            -48.344949, -2.4676235), new Vector3D(-0.0996476, 0.0179723, 0.0176348));
        final PVCoordinates expectedPvRelQSW = new PVCoordinates(new Vector3D(48.455408,
            -208.69809, -2.4676235), new Vector3D(-0.0219208, -0.1005861, 0.0176348));

        // Output and comparison
        // System.out.println("TNW PATRIUS : " + pvRelTNW.toString());
        // System.out.println("TNW CELEST  : " + expectedPvRelTNW.toString() + "\n");
        // System.out.println("QSW PATRIUS : " + pvRelQSW.toString());
        // System.out.println("QSW CELEST  : " + expectedPvRelQSW.toString());

        final Vector3D pvRelTNWPos = pvRelTNW.getPosition();
        final Vector3D expectedPvRelTNWPos = expectedPvRelTNW.getPosition();
        final Vector3D pvRelTNWVel = pvRelTNW.getVelocity();
        final Vector3D expectedPvRelTNWVel = expectedPvRelTNW.getVelocity();
        final Vector3D pvRelQSWPos = pvRelQSW.getPosition();
        final Vector3D expectedPvRelQSWPos = expectedPvRelQSW.getPosition();
        final Vector3D pvRelQSWVel = pvRelQSW.getVelocity();
        final Vector3D expectedPvRelQSWVel = expectedPvRelQSW.getVelocity();

        Assert.assertEquals(pvRelTNWPos.getX(), expectedPvRelTNWPos.getX(), E_5);
        Assert.assertEquals(pvRelTNWPos.getY(), expectedPvRelTNWPos.getY(), E_5);
        Assert.assertEquals(pvRelTNWPos.getZ(), expectedPvRelTNWPos.getZ(), E_5);

        Assert.assertEquals(pvRelTNWVel.getX(), expectedPvRelTNWVel.getX(), E_5);
        Assert.assertEquals(pvRelTNWVel.getY(), expectedPvRelTNWVel.getY(), E_5);
        Assert.assertEquals(pvRelTNWVel.getZ(), expectedPvRelTNWVel.getZ(), E_5);

        Assert.assertEquals(pvRelQSWPos.getX(), expectedPvRelQSWPos.getX(), E_5);
        Assert.assertEquals(pvRelQSWPos.getY(), expectedPvRelQSWPos.getY(), E_5);
        Assert.assertEquals(pvRelQSWPos.getZ(), expectedPvRelQSWPos.getZ(), E_5);

        Assert.assertEquals(pvRelQSWVel.getX(), expectedPvRelQSWVel.getX(), E_5);
        Assert.assertEquals(pvRelQSWVel.getY(), expectedPvRelQSWVel.getY(), E_5);
        Assert.assertEquals(pvRelQSWVel.getZ(), expectedPvRelQSWVel.getZ(), E_5);
    }

}
