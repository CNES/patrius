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
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class TEMEProviderTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TEMEProviderTest.class.getSimpleName(), "TEME frame");
    }

    @Test
    public void testValladoTEMEofDate() throws PatriusException {

        Report.printMethodHeader("testAASReferenceLEO", "Frame conversion", "Vallado paper", 0.025,
            ComparisonType.ABSOLUTE);

        // this reference test has been extracted from Vallado's book:
        // Fundamentals of Astrodynamics and Applications
        // David A. Vallado, Space Technology Library, 2007
        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2000, 182),
            new TimeComponents(0.78495062 * Constants.JULIAN_DAY),
            TimeScalesFactory.getUTC());

        // TEME
        final PVCoordinates pvTEME =
            new PVCoordinates(new Vector3D(-9060473.73569, 4658709.52502, 813686.73153),
                new Vector3D(-2232.832783, -4110.453490, -3157.345433));

        // reference position in EME2000
        // note that Valado's book gives
        // PVCoordinates pvEME2000Ref =
        // new PVCoordinates(new Vector3D(-9059941.3786, 4659697.2000, 813958.8875),
        // new Vector3D(-2233.348094, -4110.136162, -3157.394074));
        // the values we use here are slightly different, they were computed using
        // Vallado's C++ companion code to the book, using the teme_j2k function with
        // all 106 nutation terms and the 2 corrections elements of the equation of the equinoxes
        final PVCoordinates pvEME2000Ref =
            new PVCoordinates(new Vector3D(-9059941.5224999374914, 4659697.1225837596648, 813957.72947647583351),
                new Vector3D(-2233.3476939179299769, -4110.1362849403413335, -3157.3941963060194738));

        final Transform t = FramesFactory.getTEME().getTransformTo(FramesFactory.getEME2000(), t0);

        final PVCoordinates pvEME2000Computed = t.transformPVCoordinates(pvTEME);
        final PVCoordinates delta = new PVCoordinates(pvEME2000Computed, pvEME2000Ref);
        Assert.assertEquals(0.0, delta.getPosition().getNorm(), 0.025);
        Assert.assertEquals(0.0, delta.getVelocity().getNorm(), 1.0e-4);

        Report.printToReport("Position", pvEME2000Ref.getPosition(), pvEME2000Computed.getPosition());
        Report.printToReport("Velocity", pvEME2000Ref.getVelocity(), pvEME2000Computed.getVelocity());

        // Cover the getTransform methods of the TEMEProvider class:
        Assert.assertNotNull(new TEMEProvider().getTransform(t0));
        Assert.assertNotNull(new TEMEProvider().getTransform(t0, false));
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
