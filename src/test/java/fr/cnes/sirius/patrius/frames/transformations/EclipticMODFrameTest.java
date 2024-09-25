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
 * @history creation 01/08/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for EOD frame.
 * 
 * @author Julie Anton
 * 
 * @version $Id: EODFrameTest.java 18088 2017-10-02 17:01:51Z bignon $
 * 
 * @since 1.2
 * 
 */
public class EclipticMODFrameTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle EOD frame
         * 
         * @featureDescription valdiation of the transformation between EOD and MOD
         * 
         * @coveredRequirements DV-MOD_470
         */
        EOD
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(EclipticMODFrameTest.class.getSimpleName(), "EOD frame");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EOD}
     * 
     * @testedMethod {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * 
     * @description Validation of the EOD frame with respect to a Scilab script which is part of a CNES toolbox for
     *              Scilab (fr_MOD2EOD.sci). The validation is done on 5 days from the date 01/01/2012 0h TT.
     * 
     * @testPassCriteria result is as expected (reference Celestlab, relative threshold 1E-6 due to different model: 
     *                   IAU 2006 for Celestlab, IAU 76 for PATRIUS).
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testEODtoMOD() throws PatriusException {

        final double eps = 1E-6;
        Report.printMethodHeader("testEODtoMOD", "Frame conversion", "Celestlab", eps, ComparisonType.RELATIVE);

        // MOD
        final Frame mod = FramesFactory.getMOD(false);
        // EOD
        final Frame eod = FramesFactory.getEclipticMOD(false);
  
        // orbit
        AbsoluteDate date = new AbsoluteDate(2012, 1, 1, TimeScalesFactory.getTT());
        final Orbit orbit = new KeplerianOrbit(7000000, 0.001, MathLib.toRadians(40.), MathLib.toRadians(20.), 0, 0,
            PositionAngle.MEAN, FramesFactory.getGCRF(), date, Constants.EIGEN5C_EARTH_MU);

        // transformation
        Transform t = mod.getTransformTo(eod, date);

        // date : 01/01/2012
        PVCoordinates pv = orbit.getPVCoordinates(date, FramesFactory.getMOD(true));
        pv = t.transformPVCoordinates(pv);

        Assert.assertEquals(0., MathLib.abs(6564533.8375737696 - pv.getPosition().getX()) / 6564533.8375737696,
                eps);
        Assert.assertEquals(0., MathLib.abs(2311729.176176156 - pv.getPosition().getY()) / 2311729.176176156, eps);
        Assert.assertEquals(0., MathLib.abs(681801.079028809 - pv.getPosition().getZ()) / 681801.079028809, eps);

        // date : 02/01/2012
        date = new AbsoluteDate(2012, 1, 2, 0, 0, 0, TimeScalesFactory.getTT());
        pv = orbit.getPVCoordinates(date, FramesFactory.getMOD(true));
        t = mod.getTransformTo(eod, date);
        pv = t.transformPVCoordinates(pv);

        Assert.assertEquals(0., MathLib.abs(5081983.6621631208 - pv.getPosition().getX()) / 5081983.6621631208,
                eps);
        Assert.assertEquals(0., MathLib.abs(-4608599.5805048168 - pv.getPosition().getY()) / 4608599.5805048168,
                eps);
        Assert.assertEquals(0., MathLib.abs(-1374984.0232519914 - pv.getPosition().getZ()) / 1374984.0232519914,
                eps);

        // date : 03/01/2012
        date = new AbsoluteDate(2012, 1, 3, 0, 0, 0, TimeScalesFactory.getTT());
        pv = orbit.getPVCoordinates(date, FramesFactory.getMOD(true));
        t = mod.getTransformTo(eod, date);
        pv = t.transformPVCoordinates(pv);

        Assert.assertEquals(0., MathLib.abs(-2034877.7615555268 - pv.getPosition().getX()) / 2034877.7615555268,
                eps);
        Assert.assertEquals(0., MathLib.abs(-6424526.8862352064 - pv.getPosition().getY()) / 6424526.8862352064,
                eps);
        Assert.assertEquals(0., MathLib.abs(-1908855.210624708 - pv.getPosition().getZ()) / 1908855.210624708, eps);

        // date : 04/01/2012
        date = new AbsoluteDate(2012, 1, 4, 0, 0, 0, TimeScalesFactory.getTT());
        pv = orbit.getPVCoordinates(date, FramesFactory.getMOD(true));
        t = mod.getTransformTo(eod, date);
        pv = t.transformPVCoordinates(pv);

        Assert.assertEquals(0., MathLib.abs(-6905803.1962236584 - pv.getPosition().getX()) / 6905803.1962236584,
                eps);
        Assert.assertEquals(0., MathLib.abs(-1138303.5015409282 - pv.getPosition().getY()) / 1138303.5015409282,
                eps);
        Assert.assertEquals(0., MathLib.abs(-332530.1324425994 - pv.getPosition().getZ()) / 332530.1324425994, eps);

        // date : 05/01/2012
        date = new AbsoluteDate(2012, 1, 5, 0, 0, 0, TimeScalesFactory.getTT());
        pv = orbit.getPVCoordinates(date, FramesFactory.getMOD(true));
        t = mod.getTransformTo(eod, date);
        pv = t.transformPVCoordinates(pv);

        Assert.assertEquals(0., MathLib.abs(-4150552.3219694328 - pv.getPosition().getX()) / 4150552.3219694328,
                eps);
        Assert.assertEquals(0., MathLib.abs(5404212.7512962184 - pv.getPosition().getY()) / 5404212.7512962184,
                eps);
        Assert.assertEquals(0., MathLib.abs(1610789.5880940722 - pv.getPosition().getZ()) / 1610789.5880940722, eps);

        Report.printToReport("Position", new Vector3D(-4150552.3219694328, 5404212.7512962184, 1610789.5880940722),
            pv.getPosition());
    }
}
