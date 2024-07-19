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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.performance.frames;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.SortedMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.validation.externaltools.EphemerisDataLoader;
import fr.cnes.sirius.patrius.validation.externaltools.EphemerisDataLoaderStepByStep;

/**
 * <p>
 * Frame performances test
 * </p>
 * <p>
 * This test aims at assessing the performances of Orekit when it comes to transform a state vector (position and
 * velocity) from a frame to another frame.
 * 
 * @author Julie Anton
 * 
 * @version $Id: FramePerfValTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class FramePerfValTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle frame transformation performances
         * 
         * @featureDescription assess the performances of frame conversions
         * 
         * @coveredRequirements DV-REPERES_20
         */
        CONVERSION_PERFORMANCES
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#CONVERSION_PERFORMANCES}
     * 
     * @testedMethod {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * @testedMethod {@link Transform#transformPVCoordinates(PVCoordinates)}
     * 
     * @description the test assess the performances (execution time) of the transformation of a {@link PVCoordinates
     *              position velocity set} from the ITRF frame to the GCRF frame
     * 
     * @input files spot1-ITRF.eph and spot1-GCRF.eph provided by ZOOM (see the file frame-validation of the directory
     *        resources)
     * 
     * @output execution time
     * 
     * @testPassCriteria if the difference between the results and the reference (spot1-GCRF) is above 10 cm in position
     *                   and 1mm/s in velocity then an error is raised.
     * 
     * @comments spot1-ITRF.eph and spot1-GCRF.eph files (from ZOOM)
     * 
     * @exception IOException
     *            if data can't be read
     * @exception ParseException
     *            if data can't be parsed
     * @exception PatriusException
     *            if some data is missing
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testPerfITRFtoGCRF() throws PatriusException, IOException, ParseException {

        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(false));

        // the references : ephemeris obtained with ZOOM in the GCRF frame after a conversion
        // from ITRF to GCRF
        final EphemerisDataLoader loaderGCRF = new EphemerisDataLoader("spot1-GCRF.eph", new AbsoluteDate(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00, TimeScalesFactory.getTAI()));
        loaderGCRF.loadData();
        final SortedMap<AbsoluteDate, PVCoordinates> resultRef = loaderGCRF.getEphemeris();

        // tic
        final long start = System.currentTimeMillis();

        /*
         * the result : ephemeris obtained with Orekit in the GCRF frame after a conversion from ITRF to GCRF. The
         * transformation is done in the loader at the same time as the loading of the bulletin. The result of the
         * transformation is then stored in a sorted map, that is returned by getEphemeris.
         */
        final EphemerisDataLoaderStepByStep loaderITRF = new EphemerisDataLoaderStepByStep("spot1-ITRF.eph",
            FramesFactory.getITRF(), FramesFactory.getGCRF(), new AbsoluteDate(DateComponents.FIFTIES_EPOCH,
                TimeComponents.H00, TimeScalesFactory.getTAI()));
        loaderITRF.loadData();
        final SortedMap<AbsoluteDate, PVCoordinates> result = loaderITRF.getEphemeris();

        // toc
        final long duree = System.currentTimeMillis() - start;
        // display
        final long h = duree / 3600000;
        final long mn = (duree % 3600000) / 60000;
        final long sec = (duree % 60000) / 1000;
        final long ms = (duree % 1000);

        System.out.println("execution time = " + h + "h:" + mn + "mn:" + sec + "s:" + ms + "ms");

        // Comparison between the reference and the result

        PVCoordinates pvRef;
        AbsoluteDate date;
        PVCoordinates pv;

        // size of the array containing the results
        final int n = result.size();

        for (int i = 0; i < n; i++) {
            // date
            date = result.firstKey();
            // position velocity coordinates from the result
            pv = result.get(date);
            // position velocity coordinates from ZOOM
            pvRef = resultRef.get(date);
            // comparison between the result and the reference
            if (!equals(pv, pvRef, 0.1, 0.001)) {
                Assert.fail("the ephemeris are not the same");
            }
            // the ephemeris which have just been compared with each other are removed from the lists
            result.remove(date);
            resultRef.remove(date);
        }
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#CONVERSION_PERFORMANCES}
     * 
     * @testedMethod {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * @testedMethod {@link Transform#transformPVCoordinates(PVCoordinates)}
     * 
     * @description the test assess the performances (execution time) of the transformation of a {@link PVCoordinates
     *              position velocity set} from the GCRF frame to the ITRF frame
     * 
     * @input files spot1-ITRF.eph and spot1-GCRF.eph provided by ZOOM (see the file frame-validation of the directory
     *        resources)
     * 
     * @output execution time
     * 
     * @testPassCriteria if the difference between the results and the reference (spot1-ITRF) is above 10 cm in position
     *                   and 1mm/s in velocity then an error is raised.
     * 
     * @comments spot1-ITRF.eph and spot1-GCRF.eph files (from ZOOM)
     * 
     * @exception IOException
     *            if data can't be read
     * @exception ParseException
     *            if data can't be parsed
     * @exception PatriusException
     *            if some data is missing
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testPerfGCRFtoITRF() throws PatriusException, IOException, ParseException {

        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(false));

        // the references : ephemeris obtained with ZOOM in the ITRF frame after a conversion
        // from ITRF to GCRF
        final EphemerisDataLoader loaderITRF = new EphemerisDataLoader("spot1-ITRF.eph", new AbsoluteDate(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00, TimeScalesFactory.getTAI()));
        loaderITRF.loadData();
        final SortedMap<AbsoluteDate, PVCoordinates> resultRef = loaderITRF.getEphemeris();

        // tic
        final long start = System.currentTimeMillis();

        /*
         * the result : ephemeris obtained with Orekit in the ITRF frame after a conversion from GCRF to ITRF. The
         * transformation is done in the loader at the same time as the loading of the bulletin. The result of the
         * transformation is then stored in a sorted map, that is returned by getEphemeris.
         */
        final EphemerisDataLoaderStepByStep loaderGCRF = new EphemerisDataLoaderStepByStep("spot1-GCRF.eph",
            FramesFactory.getGCRF(), FramesFactory.getITRF(), new AbsoluteDate(DateComponents.FIFTIES_EPOCH,
                TimeComponents.H00, TimeScalesFactory.getTAI()));
        loaderGCRF.loadData();

        final SortedMap<AbsoluteDate, PVCoordinates> result = loaderGCRF.getEphemeris();

        // toc
        final long duree = System.currentTimeMillis() - start;
        // display
        final long h = duree / 3600000;
        final long mn = (duree % 3600000) / 60000;
        final long sec = (duree % 60000) / 1000;
        final long ms = (duree % 1000);

        System.out.println("execution time = " + h + "h:" + mn + "mn:" + sec + "s:" + ms + "ms");

        // Comparison between the reference and the result

        PVCoordinates pvRef;
        AbsoluteDate date;
        PVCoordinates pv;

        // size of the array containing the results
        final int n = result.size();

        for (int i = 0; i < n; i++) {
            // date
            date = result.firstKey();
            // position velocity coordinates from the result
            pv = result.get(date);
            // position velocity coordinates from ZOOM
            pvRef = resultRef.get(date);
            // comparison between the result and the reference
            if (!equals(pv, pvRef, 0.1, 0.001)) {
                Assert.fail("the ephemeris are not the same");
            }
            // the ephemeris which have just been compared with each other are removed from the lists
            result.remove(date);
            resultRef.remove(date);
        }
    }

    /**
     * Compare two position velocity coordinates with given thresholds (one for the position, the other one for the
     * velocity)
     * 
     * @param pv1
     *        : first position velocity coordinates
     * @param pv2
     *        : second position velocity coordinates
     * @param eps1
     *        : position threshold (absolute comparison between each component)
     * @param eps2
     *        : velocity threshold (absolute comparison between each component)
     * @return true if the two sets of position velocity are equals taken into account a threshold, false otherwise
     */
    private static
        boolean
            equals(final PVCoordinates pv1, final PVCoordinates pv2, final double eps1, final double eps2) {
        // position components of the first position velocity coordinates set
        final double x1 = pv1.getPosition().getX();
        final double y1 = pv1.getPosition().getY();
        final double z1 = pv1.getPosition().getZ();
        // velocity components of the first position velocity coordinates set
        final double v1 = pv1.getVelocity().getX();
        final double u1 = pv1.getVelocity().getY();
        final double w1 = pv1.getVelocity().getZ();

        // position components of the second position velocity coordinates set
        final double x2 = pv2.getPosition().getX();
        final double y2 = pv2.getPosition().getY();
        final double z2 = pv2.getPosition().getZ();
        // velocity components of the second position velocity coordinates set
        final double v2 = pv2.getVelocity().getX();
        final double u2 = pv2.getVelocity().getY();
        final double w2 = pv2.getVelocity().getZ();

        // absolute comparisons
        if (MathLib.abs(x1 - x2) > eps1) {
            return false;
        }
        if (MathLib.abs(y1 - y2) > eps1) {
            return false;
        }
        if (MathLib.abs(z1 - z2) > eps1) {
            return false;
        }
        if (MathLib.abs(v1 - v2) > eps2) {
            return false;
        }
        if (MathLib.abs(u1 - u2) > eps2) {
            return false;
        }
        if (MathLib.abs(w1 - w2) > eps2) {
            return false;
        }
        return true;
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#CONVERSION_PERFORMANCES}
     * 
     * @testedMethod {@link Frame#getTransformTo(Frame, AbsoluteDate)}
     * 
     * @description This test focuses on the performances of the CIRF2000 frame. It performs 100 conversions with dates
     *              that are in the buffer in the first case, out of the buffer in the second case.
     * 
     * @input none
     * 
     * @output computing time
     * 
     * @testPassCriteria the computing time with the points outside of the buffer is more than 10 times higher than with
     *                   the points inside the buffer.
     * 
     * @see org.orekit.frames.CIRF2000FrameTest
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public void testPerfBuffer() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(false));

        final int pointNumber = 2000;
        /*
         * build the series of dates
         */
        final double gap1 = 15;
        final double gap2 = 6 * 86400;
        final AbsoluteDate refDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 500);

        // build the series of dates inside of the buffer
        final ArrayList<AbsoluteDate> dates1 = new ArrayList<>();
        for (int i = 0; i < pointNumber; i++) {
            dates1.add(new AbsoluteDate(refDate, i * gap1));
        }

        // build the series of dates outside of the buffer
        final ArrayList<AbsoluteDate> dates2 = new ArrayList<>();
        for (int i = 0; i < pointNumber; i++) {
            dates2.add(new AbsoluteDate(refDate, i * gap2));
        }

        /*
         * build the series of PVCoordinates
         */
        final Vector3D refPosition = new Vector3D(-0.8e6, -0.6e6, -7e6);
        final Vector3D refVelocity = new Vector3D(-0.4e4, 0.6e4, -2e2);
        final PVCoordinates refState = new PVCoordinates(refPosition, refVelocity);

        final Vector3D shiftPosition = new Vector3D(-0.4e3, 0.4e3, -2e1);
        final Vector3D shiftVelocity = new Vector3D(-1, 0.5, -1e-2);
        final PVCoordinates shiftState = new PVCoordinates(shiftPosition, shiftVelocity);

        final ArrayList<PVCoordinates> stateVectors = new ArrayList<>();

        for (int i = 0; i < pointNumber; i++) {
            stateVectors.add(new PVCoordinates(1.0, refState, i, shiftState));
        }

        // get the frames
        final Frame itrf = FramesFactory.getITRF();
        final Frame gcrf = FramesFactory.getGCRF();

        /*
         * compute the transformations inside of the buffer
         */
        Transform transform;

        // tic
        long start = System.currentTimeMillis();

        for (int i = 0; i < pointNumber; i++) {
            transform = itrf.getTransformTo(gcrf, dates1.get(i));
            transform.transformPVCoordinates(stateVectors.get(i));
        }

        // toc
        final long duree1 = System.currentTimeMillis() - start;
        // display
        long h = duree1 / 3600000;
        long mn = (duree1 % 3600000) / 60000;
        long sec = (duree1 % 60000) / 1000;
        long ms = (duree1 % 1000);

        System.out.println("execution time = " + h + "h:" + mn + "mn:" + sec + "s:" + ms + "ms");

        // compute the transformations outside of the buffer
        // tic
        start = System.currentTimeMillis();

        for (int i = 0; i < pointNumber; i++) {
            transform = itrf.getTransformTo(gcrf, dates2.get(i));
            transform.transformPVCoordinates(stateVectors.get(i));
        }

        // toc
        final long duree2 = System.currentTimeMillis() - start;
        // display
        h = duree2 / 3600000;
        mn = (duree2 % 3600000) / 60000;
        sec = (duree2 % 60000) / 1000;
        ms = (duree2 % 1000);

        System.out.println("execution time = " + h + "h:" + mn + "mn:" + sec + "s:" + ms + "ms");

        // compare the results
        Assert.assertTrue(duree2 > 10 * duree1);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("frame-validation");
    }
}
