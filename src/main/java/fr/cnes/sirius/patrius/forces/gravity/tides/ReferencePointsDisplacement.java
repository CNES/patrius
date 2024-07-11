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
 * @history creation 27/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:FA:FA-2855:18/05/2021:Erreur calcul colatitude DM 2622 
 * VERSION:4.6:DM:DM-2622:27/01/2021:Modelisation de la maree polaire dans Patrius 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

//QA exemption : Checkstyle disabled for the file
//CHECKSTYLE: stop MagicNumber

/**
 * This class provides the model describing the displacements of reference points
 * due to the effect of the solid Earth tides.
 * 
 * @description <p>
 *              Computes the displacement of reference points due to the effect of the solid Earth tides.
 *              </p>
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment is thread-safe if the celestial body is too.
 * 
 * @author ClaudeD
 * 
 * @version $Id: ReferencePointsDisplacement.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public final class ReferencePointsDisplacement {

    /** nominal second degree shida numbers */
    private static final double H20 = 0.6078;
    /** nominal second degree love numbers */
    private static final double L20 = 0.0847;
    /** nominal third degree shida numbers */
    private static final double H3 = 0.292;
    /** nominal third degree love numbers */
    private static final double L3 = 0.015;
    /** factor for sun */
    private static final double MASS_RATIO_SUN = 332946.0482;
    /** factor for moon */
    private static final double MASS_RATIO_MOON = 0.0123000371;
    /** equatorial radius */
    private static final double RE = 6378136.6;

    /** used in correction induced by the latitude dependence */
    private static final double L1D = 0.0012;
    /** used in correction induced by the latitude dependence */
    private static final double L1SD = 0.0024;

    /** used in out-of-phase correction induced by mantle inelasticity in diurnal band */
    private static final double DHIS = -0.0025;
    /** used in out-of-phase correction induced by mantle inelasticity in diurnal band */
    private static final double DLIS = -0.0007;

    /** used in out-of-phase correction induced by mantle inelasticity in semi-diurnal band */
    private static final double DHISD = -0.0022;
    /** used in out-of-phase correction induced by mantle inelasticity in semi-diurnal band */
    private static final double DLISD = -0.0007;

    /** used in step2LongPeriod method */
    private static final double[][] DATDILONG = { { 0., 0., 0., 1., 0., 0.47, 0.23, 0.16, 0.07 },
        { 0., 2, 0., 0., 0., -0.20, -0.12, -0.11, -0.05 }, { 1., 0., -1, 0., 0., -0.11, -0.08, -0.09, -0.04 },
        { 2., 0., 0., 0., 0., -0.13, -0.11, -0.15, -0.07 }, { 2., 0., 0., 1., 0., -0.05, -0.05, -0.06, -0.03 } };

    /** used in step2Diurnal method */
    private static final double[][] DATDI = { { -3., 0., 2., 0., 0., -0.01, 0., 0., 0. },
        { -3., 2., 0., 0., 0., -0.01, 0.0, 0.0, 0.0 }, { -2., 0., 1., -1., 0., -0.02, 0.0, 0.0, 0.0 },
        { -2., 0., 1., 0., 0., -0.08, 0.0, -0.01, 0.01 }, { -2., 2., -1., 0., 0., -0.02, 0.0, 0.0, 0.0 },
        { -1., 0., 0., -1., 0., -0.10, 0.0, 0.0, 0.0 }, { -1., 0., 0., 0., 0., -0.51, 0.0, -0.02, 0.03 },
        { -1., 2., 0., 0., 0., 0.01, 0.0, 0.0, 0.0 }, { 0., -2., 1., 0., 0., 0.01, 0.0, 0.0, 0.0 },
        { 0., 0., -1., 0., 0., 0.02, 0.0, 0.0, 0.0 }, { 0., 0., 1., 0., 0., 0.06, 0.0, 0.0, 0.0 },
        { 0., 0., 1., 1., 0., 0.01, 0.0, 0.0, 0.0 }, { 0., 2., -1., 0., 0., 0.01, 0.0, 0.0, 0.0 },
        { 1., -3., 0., 0., 1., -0.06, 0.0, 0.0, 0.0 }, { 1., -2., 0., -1., 0., 0.01, 0.0, 0.0, 0.0 },
        { 1., -2., 0., 0., 0., -1.23, -0.07, 0.06, 0.01 }, { 1., -1., 0., 0., -1., 0.02, 0.0, 0.0, 0.0 },
        { 1., -1., 0., 0., 1., 0.04, 0.0, 0.0, 0.0 }, { 1., 0., 0., -1., 0., -0.22, 0.01, 0.01, 0.0 },
        { 1., 0., 0., 0., 0., 12.00, -0.80, -0.67, -0.03 }, { 1., 0., 0., 1., 0., 1.73, -0.12, -0.10, 0.0 },
        { 1., 0., 0., 2., 0., -0.04, 0.0, 0.0, 0.0 }, { 1., 1., 0., 0., -1., -0.50, -0.01, 0.03, 0.0 },
        { 1., 1., 0., 0., 1., 0.01, 0.0, 0.0, 0.0 }, { 0., 1., 0., 1., -1., -0.01, 0.0, 0.0, 0.0 },
        { 1., 2., -2., 0., 0., -0.01, 0.0, 0.0, 0.0 }, { 1., 2., 0., 0., 0., -0.11, 0.01, 0.01, 0.0 },
        { 2., -2., 1., 0., 0., -0.01, 0.0, 0.0, 0.0 }, { 2., 0., -1., 0., 0., -0.02, 0.0, 0.0, 0.0 },
        { 3., 0., 0., 0., 0., 0.0, 0.0, 0.0, 0.0 }, { 3., 0., 0., 1., 0., 0.0, 0.0, 0.0, 0.0 } };

    /** 2*PI in degrees */
    private static final double PI2DEG = 360.;

    /** Milli arcsec to arcsec coefficient. */
    private static final double MILLIARCSEC_TO_ARCSEC = 1000.;

    /** Mm to m coefficient. */
    private static final double MILLIMETER_TO_M = 1000.;

    /**
     * Private constructor
     */
    private ReferencePointsDisplacement() {
    }

    /**
     * Computes the displacement of reference points due to the effect of the solid Earth tides.
     * 
     * <p>
     * See IERS conventions 2010, chapter 7, section 7.1.1 "Effects of the solid Earth tides"
     * ftp://tai.bipm.org/iers/conv2010/chapter7/tn36_c7.pdf
     * </p>
     * 
     * @param date
     *        date of computation in UTC
     * @param point
     *        geocentric position of the point (in ITRF Frame)
     * @param sun
     *        geocentric position of the sun (in ITRF Frame)
     * @param moon
     *        geocentric position of the moon (in ITRF Frame)
     * @return displacement (in ITRF frame)
     * @throws PatriusException
     *         if some data cannot be read or if some
     *         file content is corrupted
     */
    public static Vector3D solidEarthTidesCorrections(final AbsoluteDate date, final Vector3D point,
                                                      final Vector3D sun, final Vector3D moon) throws PatriusException {

        // Scalar product of point vector with sun/moon vector
        final double rPoint = point.getNorm();
        final double rSun = sun.getNorm();
        final double rMoon = moon.getNorm();
        final double scSun = MathLib.divide(Vector3D.dotProduct(point, sun), rPoint * rSun);
        final double scMoon = MathLib.divide(Vector3D.dotProduct(point, moon), rPoint * rMoon);
        final double scSun2 = scSun * scSun;
        final double scMoon2 = scMoon * scMoon;

        // Computation of new H2 and J2
        final double cosPhi = MathLib.divide(MathLib.sqrt(point.getX() * point.getX()
            + point.getY() * point.getY()), rPoint);
        final double cosPhi2 = cosPhi * cosPhi;
        final double h2 = H20 - 0.0006 * (1. - 3. / 2. * cosPhi2);
        final double l2 = L20 + 0.0002 * (1. - 3. / 2. * cosPhi2);

        // p2-term
        final double p2Sun = 3. * (h2 / 2. - l2) * scSun2 - h2 / 2.;
        final double p2Moon = 3. * (h2 / 2. - l2) * scMoon2 - h2 / 2.;

        // p3-term
        final double p3Sun = 5. / 2. * (H3 - 3. * L3) * scSun2 * scSun + 3. / 2. * (L3 - H3) * scSun;
        final double p3Moon = 5. / 2. * (H3 - 3. * L3) * scMoon2 * scMoon + 3. / 2. * (L3 - H3) * scMoon;

        // term in direction of SUN/MOON vector
        final double x2Sun = 3. * l2 * scSun;
        final double x2Moon = 3. * l2 * scMoon;
        final double x3Sun = 3. * L3 / 2. * (5. * scSun2 - 1.);
        final double x3Moon = 3. * L3 / 2. * (5. * scMoon2 - 1.);

        // factors for SUN/MOON
        final double reSun = MathLib.divide(RE, rSun);
        final double fac2Sun = MASS_RATIO_SUN * RE * (reSun * reSun * reSun);
        final double reMoon = MathLib.divide(RE, rMoon);
        final double fac2Moon = MASS_RATIO_MOON * RE * (reMoon * reMoon * reMoon);
        final double fac3Sun = fac2Sun * MathLib.divide(RE, rSun);
        final double fac3Moon = fac2Moon * MathLib.divide(RE, rMoon);

        // Total displacement
        final double x = fac2Sun * (MathLib.divide(x2Sun * sun.getX(), rSun)
            + MathLib.divide(p2Sun * point.getX(), rPoint)) + fac2Moon
            * (MathLib.divide(x2Moon * moon.getX(), rMoon)
            + MathLib.divide(p2Moon * point.getX(), rPoint)) + fac3Sun
            * (MathLib.divide(x3Sun * sun.getX(), rSun)
            + MathLib.divide(p3Sun * point.getX(), rPoint)) + fac3Moon
            * (MathLib.divide(x3Moon * moon.getX(), rMoon)
            + MathLib.divide(p3Moon * point.getX(), rPoint));
        final double y = fac2Sun * (MathLib.divide(x2Sun * sun.getY(), rSun)
            + MathLib.divide(p2Sun * point.getY(), rPoint)) + fac2Moon
            * (MathLib.divide(x2Moon * moon.getY(), rMoon)
            + MathLib.divide(p2Moon * point.getY(), rPoint)) + fac3Sun
            * (MathLib.divide(x3Sun * sun.getY(), rSun)
            + MathLib.divide(p3Sun * point.getY(), rPoint)) + fac3Moon
            * (MathLib.divide(x3Moon * moon.getY(), rMoon)
            + MathLib.divide(p3Moon * point.getY(), rPoint));
        final double z = fac2Sun * (MathLib.divide(x2Sun * sun.getZ(), rSun)
            + MathLib.divide(p2Sun * point.getZ(), rPoint)) + fac2Moon
            * (MathLib.divide(x2Moon * moon.getZ(), rMoon)
            + MathLib.divide(p2Moon * point.getZ(), rPoint)) + fac3Sun
            * (MathLib.divide(x3Sun * sun.getZ(), rSun)
            + MathLib.divide(p3Sun * point.getZ(), rPoint)) + fac3Moon
            * (MathLib.divide(x3Moon * moon.getZ(), rMoon)
            + MathLib.divide(p3Moon * point.getZ(), rPoint));

        Vector3D dTide = new Vector3D(x, y, z);

        // Corrections for the out-of-phase part of love numbers
        // First, for the diurnal band
        final Vector3D corDiurnal1 = step1Diurnal(point, sun, moon, fac2Sun, fac2Moon);
        dTide = dTide.add(corDiurnal1);

        // second, for the semi-diurnal band
        final Vector3D corSemiDiurnal = step1SemiDiurnal(point, sun, moon, fac2Sun, fac2Moon);
        dTide = dTide.add(corSemiDiurnal);

        // corrections for the latitude dependence of love numbers
        final Vector3D corLatitudeDependence = step1l1(point, sun, moon, fac2Sun, fac2Moon);
        dTide = dTide.add(corLatitudeDependence);

        // Step 2
        // compute hours in the day
        final double fhr = date.getComponents(TimeScalesFactory.getUTC()).getTime().getSecondsInDay() / 3600.;
        // compute the date in julian centuries
        final double jjm1 = date.getComponents(TimeScalesFactory.getUTC()).getDate().getMJD();

        double t = (-51544.5 + jjm1 + fhr / 24.) / Constants.JULIAN_DAY_CENTURY;
        final AbsoluteDate date0 = new AbsoluteDate(date, -fhr * 3600.);
        final double dtt = date0.getComponents(TimeScalesFactory.getTT()).getTime().getSecondsInDay();
        t = t + dtt / Constants.JULIAN_CENTURY;

        // Corrections for the diurnal band
        final Vector3D corDiurnal2 = step2Diurnal(fhr, t, point);
        dTide = dTide.add(corDiurnal2);

        // corrections for the long-period band
        final Vector3D corLongPeriod2 = step2LongPeriod(t, point);
        dTide = dTide.add(corLongPeriod2);

        return dTide;
    }

    /**
     * Computes the displacement of reference points due to the effect of the pole tides.
     * 
     * <p>
     * See IERS conventions 2010, chapter 7, section 7.1.4
     * "Rotational deformation due to polar motion: Secular polar motion and the pole tide"
     * ftp://tai.bipm.org/iers/conv2010/chapter7/tn36_c7.pdf
     * </p>
     * 
     * @param date
     *        date of computation in UTC
     * @param point
     *        geocentric position of the point (in ITRF Frame)
     * @return displacement (in ITRF frame)
     * @throws PatriusException thrown if pole computation failed
     */
    public static Vector3D poleTidesCorrections(final AbsoluteDate date, final Vector3D point) throws PatriusException {
        // Duration in years since J2000 epoch
        final double t = date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_YEAR;

        // Secular position of pole in arcsec
        final double xs = (55. + 1.677 * t) / MILLIARCSEC_TO_ARCSEC;
        final double ys = (320.5 + 3.460 * t) / MILLIARCSEC_TO_ARCSEC;
        
        // Current position of pole
        final double[] pole = FramesFactory.getConfiguration().getPolarMotion(date);
        final double xp = pole[0] / Constants.ARC_SECONDS_TO_RADIANS;
        final double yp = pole[1] / Constants.ARC_SECONDS_TO_RADIANS;
        
        // Delta in arcsec
        final double m1 = xp - xs;
        final double m2 = -(yp - ys);

        // Point in spherical coordinates
        final double lambda = MathLib.atan2(point.getY(), point.getX());
        final double theta = MathLib.acos(point.getZ() / point.getNorm());
        
        // Displacement in spherical coordinates
        final double[] sincosLambda = MathLib.sinAndCos(lambda);
        final double sinLambda = sincosLambda[0];
        final double cosLambda = sincosLambda[1];
        final double[] sincosTheta = MathLib.sinAndCos(theta);
        final double sinTheta = sincosTheta[0];
        final double cosTheta = sincosTheta[1];
        final double sTheta = -9 * (cosTheta * cosTheta - sinTheta * sinTheta) * (m1 * cosLambda + m2 * sinLambda);
        final double sLambda = -9 * cosTheta * (m1 * sinLambda - m2 * cosLambda);
        final double sr = -66 * sinTheta * cosTheta * (m1 * cosLambda + m2 * sinLambda);
        final double[] s = { sTheta, sLambda, sr };
        
        // Displacement in cartesian coordinates
        final double[][] matR = {
                { cosTheta * cosLambda, cosTheta * sinLambda, -sinTheta },
                { -sinLambda, cosLambda, 0 },
                { sinTheta * cosLambda, sinTheta * sinLambda, cosTheta }
        };
        return new Vector3D(new BlockRealMatrix(matR).transpose().operate(s)).scalarMultiply(1. / MILLIMETER_TO_M);
    }

    /**
     * Gives the corrections induced by the latitude dependence.
     * 
     * @param point
     *        geocentric position of the point (in ITRF Frame)
     * @param sun
     *        geocentric position of the sun (in ITRF Frame)
     * @param moon
     *        geocentric position of the moon (in ITRF Frame)
     * @param fac2Sun
     *        sun factor
     * @param fac2Moon
     *        moon factor
     * @return correction for diurnal band
     * 
     **/
    private static Vector3D step1l1(final Vector3D point, final Vector3D sun, final Vector3D moon,
                                    final double fac2Sun, final double fac2Moon) {

        final double rPoint = point.getNorm();
        // sine(phi):
        final double sinPhi = MathLib.divide(point.getZ(), rPoint);
        final double sinPhi2 = sinPhi * sinPhi;
        // cosine(phi):
        final double cosPhi = MathLib.divide(MathLib.sqrt(point.getX()
            * point.getX() + point.getY() * point.getY()), rPoint);
        final double cosPhi2 = cosPhi * cosPhi;
        final double sinLat = MathLib.divide(point.getY(), cosPhi * rPoint);
        final double cosLat = MathLib.divide(point.getX(), cosPhi * rPoint);
        final double rSun = sun.getNorm();
        final double rSun2 = rSun * rSun;
        final double rMoon = moon.getNorm();
        final double rMoon2 = rMoon * rMoon;

        // for the diurnal band
        double dnSun = -L1D * sinPhi2 * fac2Sun * sun.getZ()
            * MathLib.divide(sun.getX() * cosLat + sun.getY() * sinLat, rSun2);
        double dnMoon = -L1D * sinPhi2 * fac2Moon * moon.getZ()
            * MathLib.divide(moon.getX() * cosLat + moon.getY() * sinLat, rMoon2);

        double deSun = L1D * sinPhi * (cosPhi2 - sinPhi2) * fac2Sun * sun.getZ()
            * MathLib.divide(sun.getX() * sinLat - sun.getY() * cosLat, rSun2);
        double deMoon = L1D * sinPhi * (cosPhi2 - sinPhi2) * fac2Moon * moon.getZ()
            * MathLib.divide(moon.getX() * sinLat - moon.getY() * cosLat, rMoon2);
        double de = 3. * (deSun + deMoon);
        double dn = 3. * (dnSun + dnMoon);
        // first term correction:
        final Vector3D correction = new Vector3D(-de * sinLat - dn * sinPhi * cosLat, de * cosLat - dn * sinPhi
            * sinLat, dn * cosPhi);

        // for the semi-diurnal band
        final double cos2Lat = cosLat * cosLat - sinLat * sinLat;
        final double sin2Lat = 2. * cosLat * sinLat;
        final double sunX2 = sun.getX() * sun.getX() - sun.getY() * sun.getY();
        final double moonX2 = moon.getX() * moon.getX() - moon.getY() * moon.getY();

        dnSun = -L1SD / 2. * sinPhi * cosPhi * fac2Sun
            * MathLib.divide(sunX2 * cos2Lat + 2. * sun.getX() * sun.getY() * sin2Lat, rSun2);
        dnMoon = -L1SD / 2. * sinPhi * cosPhi * fac2Moon
            * MathLib.divide(moonX2 * cos2Lat + 2. * moon.getX() * moon.getY() * sin2Lat, rMoon2);
        deSun = -L1SD / 2. * sinPhi2 * cosPhi * fac2Sun
            * MathLib.divide(sunX2 * sin2Lat - 2. * sun.getX() * sun.getY() * cos2Lat, rSun2);
        deMoon = -L1SD / 2. * sinPhi2 * cosPhi * fac2Moon
            * MathLib.divide(moonX2 * sin2Lat - 2. * moon.getX() * moon.getY() * cos2Lat, rMoon2);

        de = 3. * (deSun + deMoon);
        dn = 3. * (dnSun + dnMoon);
        // second term correction:
        final Vector3D correction2 = new Vector3D(-de * sinLat - dn * sinPhi * cosLat, de * cosLat - dn * sinPhi
            * sinLat, dn * cosPhi);
        return correction.add(correction2);
    }

    /**
     * Gives the out-of-phase corrrections induced by mantle inelasticity in the diurnal band.
     * 
     * @param point
     *        geocentric position of the point (in ITRF Frame)
     * @param sun
     *        geocentric position of the sun (in ITRF Frame)
     * @param moon
     *        geocentric position of the moon (in ITRF Frame)
     * @param fac2Sun
     *        sun factor
     * @param fac2Moon
     *        moon factor
     * @return correction for diurnal band
     * */
    private static Vector3D step1Diurnal(final Vector3D point, final Vector3D sun, final Vector3D moon,
                                         final double fac2Sun, final double fac2Moon) {

        final double rPoint = point.getNorm();
        // sine(phi):
        final double sinPhi = MathLib.divide(point.getZ(), rPoint);
        final double sinPhi2 = sinPhi * sinPhi;
        // cosine(phi):
        final double cosPhi = MathLib.divide(MathLib.sqrt(point.getX() * point.getX()
            + point.getY() * point.getY()), rPoint);
        final double cosPhi2 = cosPhi * cosPhi;
        final double cosPhi2sinPhi2 = cosPhi2 - sinPhi2;
        final double sinLat = MathLib.divide(point.getY(), cosPhi * rPoint);
        final double cosLat = MathLib.divide(point.getX(), cosPhi * rPoint);
        final double rSun = sun.getNorm();
        final double rSun2 = rSun * rSun;
        final double rMoon = moon.getNorm();
        final double rMoon2 = rMoon * rMoon;
        // compute dr for Sun and Moon:
        final double drSun = -3. * DHIS * sinPhi * cosPhi * fac2Sun * sun.getZ()
            * MathLib.divide(sun.getX() * sinLat - sun.getY() * cosLat, rSun2);
        final double drMoon = -3. * DHIS * sinPhi * cosPhi * fac2Moon * moon.getZ()
            * MathLib.divide(moon.getX() * sinLat - moon.getY() * cosLat, rMoon2);
        // compute dn for Sun and Moon:
        final double dnSun = -3. * DLIS * cosPhi2sinPhi2 * fac2Sun * sun.getZ()
            * MathLib.divide(sun.getX() * sinLat - sun.getY() * cosLat, rSun2);
        final double dnMoon = -3. * DLIS * cosPhi2sinPhi2 * fac2Moon * moon.getZ()
            * MathLib.divide(moon.getX() * sinLat - moon.getY() * cosLat, rMoon2);
        // compute de for Sun and Moon:
        final double deSun = -3. * DLIS * sinPhi * fac2Sun * sun.getZ()
            * MathLib.divide(sun.getX() * cosLat + sun.getY() * sinLat, rSun2);
        final double deMoon = -3. * DLIS * sinPhi * fac2Moon * moon.getZ()
            * MathLib.divide(moon.getX() * cosLat + moon.getY() * sinLat, rMoon2);

        final double dr = drSun + drMoon;
        final double dn = dnSun + dnMoon;
        final double de = deSun + deMoon;
        // compute correction vector
        return new Vector3D(dr * cosLat * cosPhi - de * sinLat - dn * sinPhi * cosLat, dr * sinLat
            * cosPhi + de * cosLat - dn * sinPhi * sinLat, dr * sinPhi + dn * cosPhi);
    }

    /**
     * Gives the out-of-phase corrections induced by mantle inelasticity in the semi-diurnal band.
     * 
     * @param point
     *        geocentric position of the point (in ITRF Frame)
     * @param sun
     *        geocentric position of the sun (in ITRF Frame)
     * @param moon
     *        geocentric position of the moon (in ITRF Frame)
     * @param fac2Sun
     *        sun factor
     * @param fac2Moon
     *        moon factor
     * @return correction for semi-diurnal band
     * */
    private static Vector3D step1SemiDiurnal(final Vector3D point, final Vector3D sun, final Vector3D moon,
                                             final double fac2Sun, final double fac2Moon) {

        final double rPoint = point.getNorm();
        // sine(phi):
        final double sinPhi = MathLib.divide(point.getZ(), rPoint);
        // cosine(phi):
        final double cosPhi = MathLib.divide(MathLib.sqrt(point.getX() * point.getX() + point.getY() * point.getY()),
            rPoint);
        final double cosPhi2 = cosPhi * cosPhi;
        final double sinLat = MathLib.divide(point.getY(), cosPhi * rPoint);
        final double cosLat = MathLib.divide(point.getX(), cosPhi * rPoint);
        final double cos2Lat = cosLat * cosLat - sinLat * sinLat;
        final double sin2Lat = 2. * cosLat * sinLat;
        final double rSun = sun.getNorm();
        final double rSun2 = rSun * rSun;
        final double rMoon = moon.getNorm();
        final double rMoon2 = rMoon * rMoon;
        final double sunX2 = sun.getX() * sun.getX() - sun.getY() * sun.getY();
        final double moonX2 = moon.getX() * moon.getX() - moon.getY() * moon.getY();
        // compute dr for Sun and Moon:
        final double drSun = -3. / 4. * DHISD * cosPhi2 * fac2Sun
            * MathLib.divide(sunX2 * sin2Lat - 2. * sun.getX() * sun.getY() * cos2Lat, rSun2);
        final double drMoon = -3. / 4. * DHISD * cosPhi2 * fac2Moon
            * MathLib.divide(moonX2 * sin2Lat - 2. * moon.getX() * moon.getY() * cos2Lat, rMoon2);
        // compute dn for Sun and Moon:
        final double dnSun = 3. / 2. * DLISD * sinPhi * cosPhi * fac2Sun
            * MathLib.divide(sunX2 * sin2Lat - 2. * sun.getX() * sun.getY() * cos2Lat, rSun2);
        final double dnMoon = 3. / 2. * DLISD * sinPhi * cosPhi * fac2Moon
            * MathLib.divide(moonX2 * sin2Lat - 2. * moon.getX() * moon.getY() * cos2Lat, rMoon2);
        // compute de for Sun and Moon:
        final double deSun = -3. / 2. * DLISD * cosPhi * fac2Sun
            * MathLib.divide(sunX2 * cos2Lat + 2. * sun.getX() * sun.getY() * sin2Lat, rSun2);
        final double deMoon = -3. / 2. * DLISD * cosPhi * fac2Moon
            * MathLib.divide(moonX2 * cos2Lat + 2. * moon.getX() * moon.getY() * sin2Lat, rMoon2);

        final double dr = drSun + drMoon;
        final double dn = dnSun + dnMoon;
        final double de = deSun + deMoon;
        // compute correction vector
        return new Vector3D(dr * cosLat * cosPhi - de * sinLat - dn * sinPhi * cosLat, dr * sinLat
            * cosPhi + de * cosLat - dn * sinPhi * sinLat, dr * sinPhi + dn * cosPhi);
    }

    /**
     * Correction for diurnal band (step 2) to account for the frequency dependence of the love numbers.
     * 
     * @param fhr
     *        hr in the day
     * @param t
     *        date of computation in TT time (in julian epoch)
     * @param point
     *        geocentric position of the point (in ITRF Frame)
     * @return correction for diurnal band (step 2)
     * */
    private static Vector3D step2Diurnal(final double fhr, final double t, final Vector3D point) {

        // Distance
        final double rPoint = point.getNorm();
        // sine(phi):
        final double sinPhi = MathLib.divide(point.getZ(), rPoint);
        // cosine(phi):
        final double cosPhi = MathLib.divide(MathLib.sqrt(point.getX() * point.getX()
            + point.getY() * point.getY()), rPoint);
        // cosine(lat):
        final double cosLat = MathLib.divide(point.getX(), cosPhi * rPoint);
        // sine(lat):
        final double sinLat = MathLib.divide(point.getY(), cosPhi * rPoint);
        final double cosPhisinPhi2 = cosPhi * cosPhi - sinPhi * sinPhi;
        final double zLat = MathLib.atan2(point.getY(), point.getX());
        // variables initialization:
        double dr;
        double dn;
        double de;
        double thetaf;

        final double t2 = t * t;
        final double t3 = t2 * t;
        final double t4 = t3 * t;
        double s = 218.31664563 + 481267.88194 * t - 0.0014663889 * t2 + 0.00000185139 * t3;
        double tau = fhr * 15. + 280.4606184 + 36000.7700536 * t + 0.00038793 * t2 - 0.0000000258 * t3 - s;
        final double pr = 1.396971278 * t + 0.000308889 * t2 + 0.000000021 * t3 + 0.000000007 * t4;
        s = s + pr;
        double h = 280.46645 + 36000.7697489 * t + 0.00030322222 * t2 + 0.000000020 * t3 - 0.00000000654 * t4;
        double p = 83.35324312 + 4069.01363525 * t - 0.01032172222 * t2 - 0.0000124991 * t3 + 0.00000005263 * t4;
        double zns = 234.95544499 + 1934.13626197 * t - 0.00207561111 * t2 - 0.00000213944 * t3 + 0.00000001650 * t4;
        double ps = 282.93734098 + 1.71945766667 * t + 0.00045688889 * t2 - 0.00000001778 * t3 - 0.00000000334 * t4;

        // Variables modulo 360
        s = s % PI2DEG;
        tau = tau % PI2DEG;
        h = h % PI2DEG;
        p = p % PI2DEG;
        zns = zns % PI2DEG;
        ps = ps % PI2DEG;
        // correction initialization:
        Vector3D correction = Vector3D.ZERO;

        // Loop on all components
        for (int i = 0; i < 31; i++) {
            thetaf = (tau + DATDI[i][0] * s + DATDI[i][1] * h + DATDI[i][2] * p + DATDI[i][3] * zns + DATDI[i][4] * ps)
                * MathUtils.DEG_TO_RAD;
            final double thetafzLat = thetaf + zLat;
            final double[] sincos = MathLib.sinAndCos(thetafzLat);
            final double sin = sincos[0];
            final double cos = sincos[1];
            dr = DATDI[i][5] * 2. * sinPhi * cosPhi * sin + DATDI[i][6] * 2. * sinPhi * cosPhi * cos;
            dn = DATDI[i][7] * cosPhisinPhi2 * sin + DATDI[i][8] * cosPhisinPhi2 * cos;
            de = DATDI[i][7] * sinPhi * cos - DATDI[i][8] * sinPhi * sin;
            // compute correction vector:
            final Vector3D v = new Vector3D(dr * cosLat * cosPhi - de * sinLat - dn * sinPhi * cosLat, dr * sinLat
                * cosPhi + de * cosLat - dn * sinPhi * sinLat, dr * sinPhi + dn * cosPhi);
            // add correction vector:
            correction = correction.add(v);
        }

        // Return result
        return correction.scalarMultiply(1. / 1000.);
    }

    /**
     * Correction for diurnal band (step 2) to account for the frequency dependence of the love numbers.
     * 
     * @param t
     *        date of computation in TT time (in julian epoch)
     * @param point
     *        geocentric position of the point (in ITRF Frame)
     * @return correction for diurnal band (step 2)
     * */
    private static Vector3D step2LongPeriod(final double t, final Vector3D point) {

        final double rPoint = point.getNorm();
        // sine(phi):
        final double sinPhi = MathLib.divide(point.getZ(), rPoint);
        final double sinPhi2 = sinPhi * sinPhi;
        // cosine(phi):
        final double cosPhi = MathLib.divide(MathLib.sqrt(point.getX() * point.getX() + point.getY() * point.getY()),
            rPoint);
        // cosine(lat):
        final double cosLat = MathLib.divide(point.getX(), cosPhi * rPoint);
        // sine(lat):
        final double sinLat = MathLib.divide(point.getY(), cosPhi * rPoint);
        // variables initialization:
        double dr;
        double dn;
        double thetaf;

        final double t2 = t * t;
        final double t3 = t2 * t;
        final double t4 = t3 * t;
        double s = 218.31664563 + 481267.88194 * t - 0.0014663889 * t2 + 0.00000185139 * t3;
        final double pr = 1.396971278 * t + 0.000308889 * t2 + 0.000000021 * t3 + 0.000000007 * t4;
        s = s + pr;
        double h = 280.46645 + 36000.7697489 * t + 0.00030322222 * t2 + 0.000000020 * t3 - 0.00000000654 * t4;
        double p = 83.35324312 + 4069.01363525 * t - 0.01032172222 * t2 - 0.0000124991 * t3 + 0.00000005263 * t4;
        double zns = 234.95544499 + 1934.13626197 * t - 0.00207561111 * t2 - 0.00000213944 * t3 + 0.00000001650 * t4;
        double ps = 282.93734098 + 1.71945766667 * t + 0.00045688889 * t2 - 0.00000001778 * t3 - 0.00000000334 * t4;
        s = s % PI2DEG;
        h = h % PI2DEG;
        p = p % PI2DEG;
        zns = zns % PI2DEG;
        ps = ps % PI2DEG;
        // correction initialization:
        Vector3D correction = Vector3D.ZERO;

        for (int i = 0; i < 5; i++) {
            thetaf = (DATDILONG[i][0] * s + DATDILONG[i][1] * h + DATDILONG[i][2] * p + DATDILONG[i][3] * zns
                + DATDILONG[i][4] * ps) * MathUtils.DEG_TO_RAD;
            final double[] sincos = MathLib.sinAndCos(thetaf);
            final double sin = sincos[0];
            final double cos = sincos[1];
            dr = DATDILONG[i][5] * (3. * sinPhi2 - 1.) / 2. * cos + DATDILONG[i][7] * (3. * sinPhi2 - 1.) / 2. * sin;
            dn = DATDILONG[i][6] * (cosPhi * sinPhi * 2.) * cos + DATDILONG[i][8] * (cosPhi * sinPhi * 2.) * sin;
            // compute correction vector:
            final Vector3D v = new Vector3D(dr * cosLat * cosPhi - dn * sinPhi * cosLat, dr * sinLat * cosPhi - dn
                * sinPhi * sinLat, dr * sinPhi + dn * cosPhi);
            // add correction vector:
            correction = correction.add(v);
        }

        return correction.scalarMultiply(1. / 1000.);
    }
}
