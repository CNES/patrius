/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class HelmertTransformationTest {

    @Test
    public void testHelmert20052008() throws PatriusException {
        FramesFactory.getITRF();
        AbsoluteDate date = new AbsoluteDate(2005, 1, 1, 12, 0, 0, TimeScalesFactory.getTT());

        Transform t = HelmertTransformationFactory.ITRF05TO08.getTransform(date).getInverse();

        final Vector3D pos2005 = new Vector3D(1234567.8, 2345678.9, 3456789.0);

        // check the Helmert transformation as per http://itrf.ign.fr/ITRF_solutions/2008/tp_08-05.php

        Vector3D pos2008 = t.transformPosition(pos2005);
        Vector3D generalOffset = pos2005.subtract(pos2008);
        Vector3D linearOffset = this.computeOffsetLinearly(-0.5, -0.9, -4.7, 0.000, 0.000, 0.000,
            0.3, 0.0, 0.0, 0.000, 0.000, 0.000,
            pos2005, 0.0);
        Vector3D error = generalOffset.subtract(linearOffset);
        Assert.assertEquals(0.0, error.getNorm(), MathLib.ulp(pos2005.getNorm()));

        date = date.shiftedBy(Constants.JULIAN_YEAR);
        t = HelmertTransformationFactory.ITRF05TO08.getTransform(date).getInverse();
        pos2008 = t.transformPosition(pos2005);
        generalOffset = pos2005.subtract(pos2008);
        linearOffset = this.computeOffsetLinearly(-0.5, -0.9, -4.7, 0.000, 0.000, 0.000,
            0.3, 0.0, 0.0, 0.000, 0.000, 0.000,
            pos2005, 1.0);
        error = generalOffset.subtract(linearOffset);
        Assert.assertEquals(0.0, error.getNorm(), MathLib.ulp(pos2005.getNorm()));

    }

    @Test
    public void testHelmert20002005() throws PatriusException {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        Transform t = HelmertTransformationFactory.ITRF05TO00.getTransform(date);

        final Vector3D pos2000 = new Vector3D(1234567.8, 2345678.9, 3456789.0);

        // check the Helmert transformation as per http://itrf.ign.fr/ITRF_solutions/2005/tp_05-00.php

        Vector3D pos2005 = t.transformPosition(pos2000);
        Vector3D generalOffset = pos2000.subtract(pos2005);
        Vector3D linearOffset = this.computeOffsetLinearly(0.1, -0.8, -5.8, 0.000, 0.000, 0.000,
            -0.2, 0.1, -1.8, 0.000, 0.000, 0.000,
            pos2000, 0.0);
        Vector3D error = generalOffset.subtract(linearOffset);
        Assert.assertEquals(0.0, error.getNorm(), MathLib.ulp(pos2000.getNorm()));

        date = date.shiftedBy(Constants.JULIAN_YEAR);
        t = HelmertTransformationFactory.ITRF05TO00.getTransform(date);
        pos2005 = t.transformPosition(pos2000);
        generalOffset = pos2000.subtract(pos2005);
        linearOffset = this.computeOffsetLinearly(0.1, -0.8, -5.8, 0.000, 0.000, 0.000,
            -0.2, 0.1, -1.8, 0.000, 0.000, 0.000,
            pos2000, 1.0);
        error = generalOffset.subtract(linearOffset);
        Assert.assertEquals(0.0, error.getNorm(), MathLib.ulp(pos2000.getNorm()));

    }

    @Test
    public void testHelmert19972000() throws PatriusException {
        AbsoluteDate date = new AbsoluteDate(1997, 1, 1, 12, 0, 0, TimeScalesFactory.getTT());

        Transform t = HelmertTransformationFactory.ITRF00TO97.getTransform(date);

        final Vector3D pos97 = new Vector3D(1234567.8, 2345678.9, 3456789.0);

        // check the Helmert transformation as per ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP

        // Transform t = HelmertTransformation.ITRF97TO00.getTransform(date);

        Vector3D pos2000 = t.transformPosition(pos97);
        Vector3D generalOffset = pos97.subtract(pos2000);
        Vector3D linearOffset = this.computeOffsetLinearly(6.7, 6.1, -18.5, 0.000, 0.000, 0.000,
            0.0, -0.6, -1.4, 0.000, 0.000, 0.002,
            pos2000, 0.0);
        Vector3D error = generalOffset.subtract(linearOffset);
        Assert.assertEquals(0.0, error.getNorm(), MathLib.ulp(pos97.getNorm()));

        date = date.shiftedBy(Constants.JULIAN_YEAR);
        t = HelmertTransformationFactory.ITRF00TO97.getTransform(date, false);
        pos2000 = t.transformPosition(pos97);
        generalOffset = pos97.subtract(pos2000);
        linearOffset = this.computeOffsetLinearly(6.7, 6.1, -18.5, 0.000, 0.000, 0.000,
            0.0, -0.6, -1.4, 0.000, 0.000, 0.002,
            pos2000, 1.0);
        error = generalOffset.subtract(linearOffset);
        Assert.assertEquals(0.0, error.getNorm(), MathLib.ulp(pos97.getNorm()));

    }

    @Test
    public void testHelmert19932000() throws PatriusException {
        final Vector3D pos93 = new Vector3D(1234567.8, 2345678.9, 3456789.0);

        // check the Helmert transformation as per ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP
        AbsoluteDate date = new AbsoluteDate(1988, 1, 1, 12, 0, 0, TimeScalesFactory.getTT());
        Vector3D pos2000 = HelmertTransformationFactory.ITRF00TO93.getTransform(date, FramesFactory.getConfiguration())
            .transformPosition(pos93);
        Vector3D generalOffset = pos93.subtract(pos2000);
        Vector3D linearOffset = this.computeOffsetLinearly(12.7, 6.5, -20.9, -0.39, 0.80, -1.14,
            -2.9, -0.2, -0.6, -0.11, -0.19, 0.07,
            pos2000, 0.0);
        Vector3D error = generalOffset.subtract(linearOffset);
        Assert.assertEquals(0.0, error.getNorm(), MathLib.ulp(pos93.getNorm()));

        date = date.shiftedBy(Constants.JULIAN_YEAR);
        pos2000 = HelmertTransformationFactory.ITRF00TO93.getTransform(date).transformPosition(pos93);
        generalOffset = pos93.subtract(pos2000);
        linearOffset = this.computeOffsetLinearly(12.7, 6.5, -20.9, -0.39, 0.80, -1.14,
            -2.9, -0.2, -0.6, -0.11, -0.19, 0.07,
            pos2000, 1.0);
        error = generalOffset.subtract(linearOffset);
        Assert.assertEquals(0.0, error.getNorm(), MathLib.ulp(pos93.getNorm()));

    }

    private Vector3D computeOffsetLinearly(final double t1, final double t2, final double t3,
                                           final double r1, final double r2, final double r3,
                                           final double t1Dot, final double t2Dot, final double t3Dot,
                                           final double r1Dot, final double r2Dot, final double r3Dot,
                                           final Vector3D p, final double dt) {
        final double t1U = (t1 + dt * t1Dot) * 1.0e-3;
        final double t2U = (t2 + dt * t2Dot) * 1.0e-3;
        final double t3U = (t3 + dt * t3Dot) * 1.0e-3;
        final double r1U = MathLib.toRadians((r1 + dt * r1Dot) / 3.6e6);
        final double r2U = MathLib.toRadians((r2 + dt * r2Dot) / 3.6e6);
        final double r3U = MathLib.toRadians((r3 + dt * r3Dot) / 3.6e6);
        return new Vector3D(t1U - r3U * p.getY() + r2U * p.getZ(),
            t2U + r3U * p.getX() - r1U * p.getZ(),
            t3U - r2U * p.getX() + r1U * p.getY());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
