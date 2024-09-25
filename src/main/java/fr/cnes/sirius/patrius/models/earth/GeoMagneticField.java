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
 */

/* Copyright 2011-2012 Space Applications Services
 *
 * HISTORY
* VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au 
 *          lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC) 
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:92:27/03/2014:Made constructor and setters public
 * VERSION::FA:261:13/10/2014:JE V2.2 corrections (changed GeoMagneticField javadoc)
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Used to calculate the geomagnetic field at a given geodetic point on earth.
 * The calculation is estimated using spherical harmonic expansion of the
 * geomagnetic potential with coefficients provided by an actual geomagnetic
 * field model (e.g. IGRF, WMM).
 * <p>
 * Based on original software written by Manoj Nair from the National Geophysical Data Center, NOAA, as part of the WMM
 * 2010 software release (WMM_SubLibrary.c)
 * </p>
 *
 * @see <a href="http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml">World Magnetic Model Overview</a>
 * @see <a href="http://www.ngdc.noaa.gov/geomag/WMM/soft.shtml">WMM Software Downloads</a>
 * @author Thomas Neidhart
 */
public class GeoMagneticField {

    /** Km to m. */
    private static final double KM_TO_M = 1000.;

    /** Threshold for accurate computation. */
    private static final double THRESHOLD = 1.0e-10;

    /** Semi major-axis of WGS-84 ellipsoid in km. */
    private static final double A = Constants.WGS84_EARTH_EQUATORIAL_RADIUS / KM_TO_M;

    /** The first eccentricity squared. */
    private static final double EPSSQ = 0.0066943799901413169961;

    /** Mean radius of IAU-66 ellipsoid, in km. */
    private static final double ELLIPSOID_RADIUS = 6371.2;

    /** 100 years modulo. */
    private static final int MOD_100 = 100;

    /** 400 years modulo. */
    private static final int MOD_400 = 400;

    /** Number of days in one year. */
    private static final double DAYS_IN_YEAR = 365.0;

    /** The model name. */
    private final String modelName;

    /** Base time of magnetic field model epoch (yrs). */
    private final double epoch;

    /** C - Gauss coefficients of main geomagnetic model (nT). */
    private final double[] g;

    /** C - Gauss coefficients of main geomagnetic model (nT). */
    private final double[] h;

    /** CD - Gauss coefficients of secular geomagnetic model (nT/yr). */
    private final double[] dg;

    /** CD - Gauss coefficients of secular geomagnetic model (nT/yr). */
    private final double[] dh;

    /** Maximum degree of spherical harmonic model. */
    private final int maxN;

    /** Maximum degree of spherical harmonic secular variations. */
    private final int maxNSec;

    /** The validity start of this magnetic field model. */
    private final double validityStart;

    /** The validity end of this magnetic field model. */
    private final double validityEnd;

    /** Pre-calculated ratio between gauss-normalized and schmidt quasi-normalized associated Legendre functions. */
    private final double[] schmidtQuasiNorm;

    /**
     * Create a new geomagnetic field model with the given parameters.<br>
     * Internal structures are initialized according to the specified degrees of the main and secular variations.
     *
     * @param modelNameIn
     *        the model name
     * @param epochIn
     *        the base time of magnetic field model epoch (yrs)
     * @param maxNIn
     *        the maximum degree of the main model
     * @param maxNSecIn
     *        the maximum degree of the secular variations
     * @param validityStartIn
     *        validity start of this model
     * @param validityEndIn
     *        validity end of this model
     */
    public GeoMagneticField(final String modelNameIn, final double epochIn, final int maxNIn, final int maxNSecIn,
                            final double validityStartIn, final double validityEndIn) {

        this.modelName = modelNameIn;
        this.epoch = epochIn;
        this.maxN = maxNIn;
        this.maxNSec = maxNSecIn;

        this.validityStart = validityStartIn;
        this.validityEnd = validityEndIn;

        // initialize main and secular field coefficient arrays
        final int maxMainFieldTerms = (maxNIn + 1) * (maxNIn + 2) / 2;
        this.g = new double[maxMainFieldTerms];
        this.h = new double[maxMainFieldTerms];

        final int maxSecularFieldTerms = (maxNSecIn + 1) * (maxNSecIn + 2) / 2;
        this.dg = new double[maxSecularFieldTerms];
        this.dh = new double[maxSecularFieldTerms];

        // pre-calculate the ratio between gauss-normalized and schmidt quasi-normalized
        // associated Legendre functions as they depend only on the degree of the model.

        this.schmidtQuasiNorm = new double[maxMainFieldTerms + 1];
        this.schmidtQuasiNorm[0] = 1.0;

        int index;
        int index1;
        for (int n = 1; n <= maxNIn; n++) {
            index = n * (n + 1) / 2;
            index1 = (n - 1) * n / 2;

            // for m = 0
            this.schmidtQuasiNorm[index] = this.schmidtQuasiNorm[index1] * (2 * n - 1) / n;

            for (int m = 1; m <= n; m++) {
                index = n * (n + 1) / 2 + m;
                index1 = n * (n + 1) / 2 + m - 1;
                this.schmidtQuasiNorm[index] =
                    this.schmidtQuasiNorm[index1]
                            * MathLib.sqrt((double) ((n - m + 1) * (m == 1 ? 2 : 1)) / (double) (n + m));
            }
        }
    }

    /**
     * Getter for the epoch for this magnetic field model.
     *
     * @return the epoch
     */
    public double getEpoch() {
        return this.epoch;
    }

    /**
     * Getter for the model name.
     *
     * @return the model name
     */
    public String getModelName() {
        return this.modelName;
    }

    /**
     * Getter for the start of the validity period for this model.
     *
     * @return the validity start as decimal year
     */
    public double validFrom() {
        return this.validityStart;
    }

    /**
     * Getter for the end of the validity period for this model.
     *
     * @return the validity end as decimal year
     */
    public double validTo() {
        return this.validityEnd;
    }

    /**
     * Indicates whether this model supports time transformation or not.
     *
     * @return <code>true</code> if this model can be transformed within its validity period, <code>false</code>
     *         otherwise
     */
    public boolean supportsTimeTransform() {
        return this.maxNSec > 0;
    }

    /**
     * Setter for the given main field coefficients.
     *
     * @param n
     *        the degree
     * @param m
     *        the order
     * @param gnm
     *        the g coefficient at position n,m
     * @param hnm
     *        the h coefficient at position n,m
     */
    public void setMainFieldCoefficients(final int n, final int m, final double gnm, final double hnm) {
        final int index = n * (n + 1) / 2 + m;
        this.g[index] = gnm;
        this.h[index] = hnm;
    }

    /**
     * Setter for the given secular variation coefficients.
     *
     * @param n
     *        the degree
     * @param m
     *        the order
     * @param dgnm
     *        the dg coefficient at position n,m
     * @param dhnm
     *        the dh coefficient at position n,m
     */
    public void setSecularVariationCoefficients(final int n, final int m, final double dgnm, final double dhnm) {
        final int index = n * (n + 1) / 2 + m;
        this.dg[index] = dgnm;
        this.dh[index] = dhnm;
    }

    /**
     * Calculate the magnetic field at the specified latitude, longitude and height.
     *
     * @param latitude
     *        the latitude in decimal degrees
     * @param longitude
     *        the longitude in decimal degrees
     * @param altitude
     *        the altitude in kilometers above mean sea level
     * @return the {@link GeoMagneticElements} at the given geodetic point
     */
    public GeoMagneticElements calculateField(final double latitude, final double longitude, final double altitude) {
        return calculateFieldInternal(MathLib.toRadians(latitude), MathLib.toRadians(longitude), altitude * KM_TO_M);
    }

    /**
     * Calculate the magnetic field at the specified ellipsoid point identified by latitude, longitude and height.
     *
     * @param point
     *        ellipsoid point
     * @return the {@link GeoMagneticElements} at the given ellipsoid point
     */
    public GeoMagneticElements calculateField(final EllipsoidPoint point) {
        return calculateFieldInternal(point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(),
            point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude(),
            point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight());
    }

    /**
     * Internal method to calculate the magnetic field at the specified latitude, longitude and altitude (code
     * mutualization).
     *
     * @param latitude
     *        the latitude in decimal radians
     * @param longitude
     *        the longitude in decimal radians
     * @param altitude
     *        the altitude in meters above mean sea level
     * @return the {@link GeoMagneticElements} at the given geodetic point
     */
    private GeoMagneticElements calculateFieldInternal(final double latitude, final double longitude,
                                                       final double altitude) {
        final SphericalCoordinates sph = transformToSpherical(latitude, longitude, altitude);
        final SphericalHarmonicVars vars = new SphericalHarmonicVars(sph);
        final LegendreFunction legendre = new LegendreFunction(MathLib.sin(sph.phi));

        // sum up the magnetic field vector components
        final Vector3D magFieldSph = summation(sph, vars, legendre);
        // rotate the field to geodetic coordinates
        final Vector3D magFieldGeo = rotateMagneticVector(sph, latitude, magFieldSph);
        // return the magnetic elements
        return new GeoMagneticElements(magFieldGeo);
    }

    /**
     * Calculate the magnetic field at the specified point identified by the coordinates of the point and the reference
     * point.
     *
     * @param point
     *        cartesian point
     * @param frame
     *        frame in which cartesian point is expressed
     * @param date
     *        date in which cartesian point is given
     * @return the {@link GeoMagneticElements} at the given cartesian point
     * @throws PatriusException
     *         if point cannot be converted to body frame
     *         if the specified year is outside the validity period
     *         if getDecimalYear() error occurred
     */
    public GeoMagneticElements calculateField(final Vector3D point, final Frame frame, final AbsoluteDate date)
        throws PatriusException {

        final GeoMagneticField transformedField = this.transformModel(getDecimalYear(date));
        final OneAxisEllipsoid body =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getITRF());
        final EllipsoidPoint ep = body.buildPoint(point, frame, date, BodyPointName.DEFAULT);
        // return the magnetic elements
        return transformedField.calculateField(ep);
    }

    /**
     * Time transform the model coefficients from the base year of the model using secular variation coefficients.
     *
     * @param year
     *        the year to which the model shall be transformed
     * @return a time-transformed magnetic field model
     * @throws PatriusException
     *         if the specified year is outside the validity period of the model or the model does not support time
     *         transformations (i.e. no secular variations available)
     */
    public GeoMagneticField transformModel(final double year) throws PatriusException {

        if (!supportsTimeTransform()) {
            // Exception
            throw new PatriusException(PatriusMessages.UNSUPPORTED_TIME_TRANSFORM, this.modelName,
                String.valueOf(this.epoch));
        }

        // Compute time delta in years
        final double dt = year - this.epoch;
        final int maxSecIndex = this.maxNSec * (this.maxNSec + 1) / 2 + this.maxNSec;

        // Initialize transformed magnetic field model
        final GeoMagneticField transformed =
            new GeoMagneticField(this.modelName, year, this.maxN, this.maxNSec, this.validityStart, this.validityEnd);

        for (int n = 1; n <= this.maxN; n++) {
            for (int m = 0; m <= n; m++) {
                final int index = n * (n + 1) / 2 + m;
                if (index <= maxSecIndex) {
                    transformed.h[index] = this.h[index] + dt * this.dh[index];
                    transformed.g[index] = this.g[index] + dt * this.dg[index];
                    // we need a copy of the secular var coef to calculate secular change
                    transformed.dh[index] = this.dh[index];
                    transformed.dg[index] = this.dg[index];
                } else {
                    // just copy the parts that do not have corresponding secular variation coefficients
                    transformed.h[index] = this.h[index];
                    transformed.g[index] = this.g[index];
                }
            }
        }

        // Return result
        return transformed;
    }

    /**
     * Time transform the model coefficients from the base year of the model using a linear interpolation with a second
     * model. The second model is required to have an adjacent validity period.
     *
     * @param otherModel
     *        the other magnetic field model
     * @param year
     *        the year to which the model shall be transformed
     * @return a time-transformed magnetic field model
     * @throws PatriusException
     *         if the specified year is outside the validity period of the model or the model does not support time
     *         transformations (i.e. no secular variations available)
     */
    public GeoMagneticField transformModel(final GeoMagneticField otherModel, final double year)
        throws PatriusException {

        // the model can only be transformed within its validity period
        if (year < this.validityStart || year > this.validityEnd) {
            // Exception
            throw new PatriusException(PatriusMessages.OUT_OF_RANGE_TIME_TRANSFORM, this.modelName,
                String.valueOf(this.epoch), year, this.validityStart, this.validityEnd);
        }

        final double factor = (year - this.epoch) / (otherModel.epoch - this.epoch);
        final int maxNCommon = MathLib.min(this.maxN, otherModel.maxN);
        final int maxNCommonIndex = maxNCommon * (maxNCommon + 1) / 2 + maxNCommon;

        final int newMaxN = MathLib.max(this.maxN, otherModel.maxN);

        // Initialize model
        final GeoMagneticField transformed = new GeoMagneticField(this.modelName, year, newMaxN, 0, this.validityStart,
            this.validityEnd);

        // Loop on order and degree
        for (int n = 1; n <= newMaxN; n++) {
            for (int m = 0; m <= n; m++) {
                final int index = n * (n + 1) / 2 + m;
                if (index <= maxNCommonIndex) {
                    transformed.h[index] = this.h[index] + factor * (otherModel.h[index] - this.h[index]);
                    transformed.g[index] = this.g[index] + factor * (otherModel.g[index] - this.g[index]);
                } else {
                    if (this.maxN < otherModel.maxN) {
                        transformed.h[index] = factor * otherModel.h[index];
                        transformed.g[index] = factor * otherModel.g[index];
                    } else {
                        transformed.h[index] = this.h[index] + factor * -this.h[index];
                        transformed.g[index] = this.g[index] + factor * -this.g[index];
                    }
                }
            }
        }

        // Return result
        return transformed;
    }

    /**
     * Utility function to get a decimal year for a given AbsoluteDate.
     *
     * @param date
     *        date in AbsoluteDate format
     * @return the decimal year represented by the given day
     * @throws PatriusException
     *         if TimeScalesFactory error occurred
     */
    public static double getDecimalYear(final AbsoluteDate date) throws PatriusException {
        final TimeScale t = TimeScalesFactory.getUTC();
        final int day = date.getComponents(t).getDate().getDay();
        final int month = date.getComponents(t).getDate().getMonth();
        final int year = date.getComponents(t).getDate().getYear();
        return getDecimalYear(day, month, year);
    }

    /**
     * Utility function to get a decimal year for a given day.
     *
     * @param day
     *        the day (1-31)
     * @param month
     *        the month (1-12)
     * @param year
     *        the year
     * @return the decimal year represented by the given day
     */
    public static double getDecimalYear(final int day, final int month, final int year) {
        final int[] days = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
        final int leapYear = year % 4 == 0 && (year % MOD_100 != 0 || year % MOD_400 == 0) ? 1 : 0;

        final double dayInYear = days[month - 1] + day + (month > 2 ? leapYear : 0);
        return year + dayInYear / (DAYS_IN_YEAR + leapYear);
    }

    /**
     * Transform geodetic coordinates to spherical coordinates.
     *
     * @param latitude
     *        the latitude
     * @param longitude
     *        the longitude
     * @param altitude
     *        the altitude
     * @return the spherical coordinates wrt to the reference ellipsoid of the model
     */
    private static SphericalCoordinates transformToSpherical(final double latitude, final double longitude,
                                                             final double altitude) {

        // Convert geodetic coordinates (defined by the WGS-84 reference ellipsoid)
        // to Earth Centered Earth Fixed Cartesian coordinates, and then to spherical coordinates.

        final double heightAboveEllipsoid = altitude / KM_TO_M;

        final double[] sincos = MathLib.sinAndCos(latitude);
        final double sinLat = sincos[0];
        final double cosLat = sincos[1];

        // compute the local radius of curvature on the reference ellipsoid
        final double rc = A / MathLib.sqrt(MathLib.max(0.0, 1.0d - EPSSQ * sinLat * sinLat));

        // compute ECEF Cartesian coordinates of specified point (for longitude=0)
        final double xp = (rc + heightAboveEllipsoid) * cosLat;
        final double zp = (rc * (1.0d - EPSSQ) + heightAboveEllipsoid) * sinLat;

        // compute spherical radius and angle lambda and phi of specified point
        final double r = MathLib.hypot(xp, zp);
        return new SphericalCoordinates(r, longitude, MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, zp / r))));
    }

    /**
     * Rotate the magnetic vectors to geodetic coordinates.
     *
     * @param sph
     *        the spherical coordinates
     * @param latitude
     *        the latitude
     * @param field
     *        the magnetic field in spherical coordinates
     * @return the magnetic field in geodetic coordinates
     */
    private static Vector3D rotateMagneticVector(final SphericalCoordinates sph, final double latitude,
                                                 final Vector3D field) {

        // difference between the spherical and geodetic latitudes
        final double psi = sph.phi - latitude;

        final double[] sincos = MathLib.sinAndCos(psi);
        final double sin = sincos[0];
        final double cos = sincos[1];

        // rotate spherical field components to the geodetic system
        final double bz = field.getX() * sin + field.getZ() * cos;
        final double bx = field.getX() * cos - field.getZ() * sin;
        final double by = field.getY();

        return new Vector3D(bx, by, bz);
    }

    /**
     * Computes Geomagnetic Field Elements X, Y and Z in spherical coordinate
     * system using spherical harmonic summation.
     * The vector Magnetic field is given by -grad V, where V is geomagnetic
     * scalar potential. The gradient in spherical coordinates is given by:
     *
     * <pre>
     *          dV ^   1 dV ^       1    dV ^
     * grad V = -- r + - -- t + -------- -- p
     *          dr     r dt     r sin(t) dp
     * </pre>
     *
     * @param sph
     *        the spherical coordinates
     * @param vars
     *        the spherical harmonic variables
     * @param legendre
     *        the legendre function
     * @return the magnetic field vector in spherical coordinates
     */
    private Vector3D summation(final SphericalCoordinates sph, final SphericalHarmonicVars vars,
                               final LegendreFunction legendre) {

        int index;
        double bx = 0.0;
        double by = 0.0;
        double bz = 0.0;

        for (int n = 1; n <= this.maxN; n++) {
            for (int m = 0; m <= n; m++) {
                index = n * (n + 1) / 2 + m;

                /**
                 * <pre>
                 *       nMax               (n+2)   n    m            m           m
                 * Bz = -SUM (n + 1) * (a/r)     * SUM [g cos(m p) + h sin(m p)] P (sin(phi))
                 *       n=1                       m=0   n            n           n
                 * </pre>
                 *
                 * Equation 12 in the WMM Technical report. Derivative with respect to radius.
                 */
                bz -= vars.relativeRadiusPower[n]
                        * (this.g[index] * vars.cmLambda[m] + this.h[index] * vars.smLambda[m]) * (1d + n)
                        * legendre.mP[index];

                /**
                 * <pre>
                 *      nMax     (n+2)   n    m            m            m
                 * By = SUM (a/r)     * SUM [g cos(m p) + h sin(m p)] dP (sin(phi))
                 *      n=1             m=0   n            n            n
                 * </pre>
                 *
                 * Equation 11 in the WMM Technical report. Derivative with respect to longitude, divided by radius.
                 */
                by += vars.relativeRadiusPower[n]
                        * (this.g[index] * vars.smLambda[m] - this.h[index] * vars.cmLambda[m]) * m
                        * legendre.mP[index];
                /**
                 * <pre>
                 *        nMax     (n+2)   n    m            m            m
                 * Bx = - SUM (a/r)     * SUM [g cos(m p) + h sin(m p)] dP (sin(phi))
                 *        n=1             m=0   n            n            n
                 * </pre>
                 *
                 * Equation 10 in the WMM Technical report. Derivative with respect to latitude, divided by radius.
                 */
                bx -= vars.relativeRadiusPower[n]
                        * (this.g[index] * vars.cmLambda[m] + this.h[index] * vars.smLambda[m])
                        * legendre.mPDeriv[index];
            }
        }

        final double cosPhi = MathLib.cos(sph.phi);
        if (MathLib.abs(cosPhi) > THRESHOLD) {
            by = by / cosPhi;
        } else {
            // special calculation for component - By - at geographic poles.
            // To avoid using this function, make sure that the latitude is not
            // exactly +/-90.
            by = summationSpecial(sph, vars);
        }

        return new Vector3D(bx, by, bz);
    }

    /**
     * Special calculation for the component By at geographic poles.
     *
     * @param sph
     *        the spherical coordinates
     * @param vars
     *        the spherical harmonic variables
     * @return the By component of the magnetic field
     */
    private double summationSpecial(final SphericalCoordinates sph, final SphericalHarmonicVars vars) {

        double k;
        final double sinPhi = MathLib.sin(sph.phi);
        final double[] mPcupS = new double[this.maxN + 1];
        mPcupS[0] = 1;
        double by = 0.0;

        for (int n = 1; n <= this.maxN; n++) {
            final int index = n * (n + 1) / 2 + 1;
            if (n == 1) {
                mPcupS[n] = mPcupS[n - 1];
            } else {
                k = (double) ((n - 1) * (n - 1) - 1) / (double) ((2 * n - 1) * (2 * n - 3));
                mPcupS[n] = sinPhi * mPcupS[n - 1] - k * mPcupS[n - 2];
            }

            /**
             * <pre>
             *      nMax     (n+2)   n    m            m            m
             * By = SUM (a/r)     * SUM [g cos(m p) + h sin(m p)] dP (sin(phi))
             *      n=1             m=0   n            n            n
             * </pre>
             *
             * Equation 11 in the WMM Technical report. Derivative with respect to longitude, divided by radius.
             */
            by += vars.relativeRadiusPower[n] * (this.g[index] * vars.smLambda[1] - this.h[index] * vars.cmLambda[1])
                    * mPcupS[n] * this.schmidtQuasiNorm[index];
        }

        return by;
    }

    /** Utility class to hold spherical coordinates. */
    private static final class SphericalCoordinates {

        /** The radius. */
        private final double r;

        /** The azimuth angle. */
        private final double lambda;

        /** The polar angle. */
        private final double phi;

        /**
         * Create a new spherical coordinate object.
         *
         * @param rIn
         *        the radius
         * @param lambdaIn
         *        the lambda angle
         * @param phiIn
         *        the phi angle
         */
        private SphericalCoordinates(final double rIn, final double lambdaIn, final double phiIn) {

            this.r = rIn;
            this.lambda = lambdaIn;
            this.phi = phiIn;
        }
    }

    /** Utility class to compute certain variables for magnetic field summation. */
    private final class SphericalHarmonicVars {

        /** (Radius of Earth / Spherical radius r)^(n+2). */
        private final double[] relativeRadiusPower;

        /** cos(m*lambda). */
        private final double[] cmLambda;

        /** sin(m*lambda). */
        private final double[] smLambda;

        /**
         * Calculates the spherical harmonic variables for a given spherical coordinate.
         *
         * @param sph
         *        the spherical coordinate
         */
        private SphericalHarmonicVars(final SphericalCoordinates sph) {

            this.relativeRadiusPower = new double[GeoMagneticField.this.maxN + 1];

            // Compute a table of (EARTH_REFERENCE_RADIUS_KM / radius)^n for i in
            // 0 .. maxN (this is much faster than calling FastMath.pow maxN+1 times).

            final double p = ELLIPSOID_RADIUS / sph.r;
            this.relativeRadiusPower[0] = p * p;
            for (int n = 1; n <= GeoMagneticField.this.maxN; n++) {
                this.relativeRadiusPower[n] = this.relativeRadiusPower[n - 1] * (ELLIPSOID_RADIUS / sph.r);
            }

            // Compute tables of sin(lon * m) and cos(lon * m) for m = 0 .. maxN
            // this is much faster than calling FastMath.sin and FastMath.cos maxN+1 times.

            this.cmLambda = new double[GeoMagneticField.this.maxN + 1];
            this.smLambda = new double[GeoMagneticField.this.maxN + 1];

            this.cmLambda[0] = 1.0d;
            this.smLambda[0] = 0.0d;

            final double[] sincos = MathLib.sinAndCos(sph.lambda);
            final double cosLambda = sincos[1];
            final double sinLambda = sincos[0];
            this.cmLambda[1] = cosLambda;
            this.smLambda[1] = sinLambda;

            for (int m = 2; m <= GeoMagneticField.this.maxN; m++) {
                this.cmLambda[m] = this.cmLambda[m - 1] * cosLambda - this.smLambda[m - 1] * sinLambda;
                this.smLambda[m] = this.cmLambda[m - 1] * sinLambda + this.smLambda[m - 1] * cosLambda;
            }
        }
    }

    /** Utility class to compute a table of Schmidt-semi normalized associated Legendre functions. */
    private final class LegendreFunction {

        /** The vector of all associated Legendre polynomials. */
        private final double[] mP;

        /** The vector of derivatives of the Legendre polynomials wrt latitude. */
        private final double[] mPDeriv;

        /**
         * Calculate the Schmidt-semi normalized Legendre function.
         * <p>
         * <b>Note:</b> In geomagnetism, the derivatives of ALF are usually found with respect to the colatitudes. Here
         * the derivatives are found with respect to the latitude. The difference is a sign reversal for the derivative
         * of the Associated Legendre Functions.
         * </p>
         *
         * @param x
         *        sinus of the spherical latitude (or cosinus of the spherical colatitude)
         */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: Commons-Math code kept as such
        private LegendreFunction(final double x) {
            // CHECKSTYLE: resume CyclomaticComplexity check

            final int numTerms = (GeoMagneticField.this.maxN + 1) * (GeoMagneticField.this.maxN + 2) / 2;

            this.mP = new double[numTerms + 1];
            this.mPDeriv = new double[numTerms + 1];

            this.mP[0] = 1.0;
            this.mPDeriv[0] = 0.0;

            // sin (geocentric latitude) - sin_phi
            final double z = MathLib.sqrt(MathLib.max(0.0, (1.0d - x) * (1.0d + x)));

            int index;
            int index1;
            int index2;

            // First, compute the Gauss-normalized associated Legendre functions
            for (int n = 1; n <= GeoMagneticField.this.maxN; n++) {
                for (int m = 0; m <= n; m++) {
                    index = n * (n + 1) / 2 + m;
                    if (n == m) {
                        index1 = (n - 1) * n / 2 + m - 1;
                        this.mP[index] = z * this.mP[index1];
                        this.mPDeriv[index] = z * this.mPDeriv[index1] + x * this.mP[index1];
                    } else if (n == 1 && m == 0) {
                        index1 = (n - 1) * n / 2 + m;
                        this.mP[index] = x * this.mP[index1];
                        this.mPDeriv[index] = x * this.mPDeriv[index1] - z * this.mP[index1];
                    } else if (n > 1 && n != m) {
                        index1 = (n - 2) * (n - 1) / 2 + m;
                        index2 = (n - 1) * n / 2 + m;
                        if (m > n - 2) {
                            this.mP[index] = x * this.mP[index2];
                            this.mPDeriv[index] = x * this.mPDeriv[index2] - z * this.mP[index2];
                        } else {
                            final double k = (double) ((n - 1) * (n - 1) - m * m)
                                    / (double) ((2 * n - 1) * (2 * n - 3));

                            this.mP[index] = x * this.mP[index2] - k * this.mP[index1];
                            this.mPDeriv[index] = x * this.mPDeriv[index2] - z * this.mP[index2] - k
                                    * this.mPDeriv[index1];
                        }
                    }

                }
            }

            // Converts the Gauss-normalized associated Legendre functions to the Schmidt quasi-normalized
            // version using pre-computed relation stored in the variable schmidtQuasiNorm

            for (int n = 1; n <= GeoMagneticField.this.maxN; n++) {
                for (int m = 0; m <= n; m++) {
                    index = n * (n + 1) / 2 + m;

                    this.mP[index] = this.mP[index] * GeoMagneticField.this.schmidtQuasiNorm[index];
                    // The sign is changed since the new WMM routines use derivative with
                    // respect to latitude instead of co-latitude
                    this.mPDeriv[index] = -this.mPDeriv[index] * GeoMagneticField.this.schmidtQuasiNorm[index];
                }
            }
        }
    }
}
