/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:679:27/09/2016:Corrected orthodromic distance computation
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.projections;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class extends the {@link EllipsoidBodyShape} class and provides utility methods needed for projections.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @see EllipsoidBodyShape
 * 
 * @author Thomas Galpin
 * @version $Id$
 * @since 3.2
 * 
 * @deprecated as of 4.3. This class is replaced by {@link ProjectionEllipsoidUtils}
 */
@Deprecated
public class ProjectionEllipsoid extends ExtendedOneAxisEllipsoid {

    /** Local precision for geodetic problem computation (in meters). */
    public static final double GEODETIC_PRECISION = 1E-5;

    /** Local precision for azimuth problem computation (in meters). */
    private static final double AZIMUTH_PRECISION = 1E-8;

    /** Threshold for loxodromic problem computation. */
    private static final double LOXODROMIC_PRECISION = 1E-12;

    /** Default serial Id */
    private static final long serialVersionUID = 1L;

    /**
     * Maximum number of iterations used in method
     * {@link ProjectionEllipsoid#computePointAlongOrthodrome(GeodeticPoint, double, double)}.
     */
    private static final double MAX_ITER = 200;

    /**
     * Convergence threshod used in method
     * {@link ProjectionEllipsoid#computePointAlongOrthodrome(GeodeticPoint, double, double)}.
     */
    private static final double THRESHOLD = 1.0E-13;

    /** Constant used in the class. */
    private static final double C15 = 15.;

    /** Constant used in the class. */
    private static final double C16 = 16.;

    /** Constant used in the class. */
    private static final int C20 = 20;

    /** Constant used in the class. */
    private static final double C21 = 21.;

    /** Constant used in the class. */
    private static final double C27 = 27.;

    /** Constant used in the class. */
    private static final double C32 = 32.;

    /** Constant used in the class. */
    private static final double C35 = 35.;

    /** Constant used in the class. */
    private static final double C45 = 45.;

    /** Constant used in the class. */
    private static final double C47 = 47.;

    /** Constant used in the class. */
    private static final double C55 = 55.;

    /** Constant used in the class. */
    private static final double C64 = 64.;

    /** Constant used in the class. */
    private static final double C74 = 74.;

    /** Constant used in the class. */
    private static final double C96 = 96.;

    /** Constant used in the class. */
    private static final double C128 = 128.;

    /** Constant used in the class. */
    private static final double C151 = 151.;

    /** Constant used in the class. */
    private static final double C175 = 175.;

    /** Constant used in the class. */
    private static final double C256 = 256.;

    /** Constant used in the class. */
    private static final double C320 = 320.;

    /** Constant used in the class. */
    private static final double C512 = 512.;

    /** Constant used in the class. */
    private static final double C768 = 768.;

    /** Constant used in the class. */
    private static final double C1024 = 1024.;

    /** Constant used in the class. */
    private static final double C1097 = 1097.;

    /** Constant used in the class. */
    private static final double C3072 = 3072.;

    /** Constant used in the class. */
    private static final double C4096 = 4096.;

    /** Constant used in the class. */
    private static final double C16384 = 16384.;

    /** 7 */
    private static final int C_7 = 7;

    /** 8 */
    private static final int C_8 = 8;

    /** -3 */
    private static final int C_N3 = -3;

    /**
     * Ellipsoid series coefficients used to compute ellipsoid series
     * 
     * @see <a href="https://pubs.er.usgs.gov/publication/pp1395"> https://pubs.er.usgs.gov/publication/pp1395</a>
     */
    private double[] series;

    /** Boolean to indicate if series have been computed. */
    private boolean seriesComputed = false;

    /**
     * Constructor for the body ellipsoid.
     * 
     * @param ae
     *        equatorial radius
     * @param f
     *        the flattening (f = (a-b)/a)
     * @param bodyFrame
     *        body frame related to body shape
     * @param name
     *        the name of this shape
     */
    public ProjectionEllipsoid(final double ae, final double f, final Frame bodyFrame, final String name) {
        super(ae, f, bodyFrame, name);
    }

    /**
     * Compute series following Snyder method => "Map Projection, A working manual"
     * written by John P. Snyder, from page 17.
     * 
     * @see <a href="https://pubs.er.usgs.gov/publication/pp1395"> https://pubs.er.usgs.gov/publication/pp1395</a>
     * @since 3.2
     */
    private void computeSeries() {

        // SNYDER : direct method Snyder, equation 3-21 limited to sin(6 * lat)
        // eccentricity
        final double e = this.getEccentricity();
        final double es = e * e;

        // Coefficients used by Snyder method
        final double ap = 1. - es * (1. / 4. + es * (3. / C64 + es * (5. / C256)));
        final double bp2 = -es * (3. / 8. + es * (3. / C32 + es * (C45 / C1024)));
        final double cp2 = es * es * (C15 / C256 + es * (C45 / C1024));
        final double dp2 = -es * es * es * (C35 / C3072);

        // SNYDER : inverse method Snyder, equation 3-26
        double e1 = MathLib.sqrt(MathLib.max(0.0, 1. - es));
        e1 = (1. - e1) / (1. + e1);
        final double eSquare = e1 * e1;
        final double bp = e1 * (3. / 2. + eSquare * (-C27 / C32));
        final double cp = eSquare * (C21 / C16 + eSquare * (-C55 / C32));
        final double dp = e1 * eSquare * (C151 / C96);
        final double ep = eSquare * eSquare * (C1097 / C512);

        // return coefficients in an array
        final double[] seriesComp = { ap, bp2, cp2, dp2, 0., bp, cp, dp, ep };

        // Indicate that coefficients have already been computed
        this.series = seriesComp;
    }

    /**
     * Compute the bearing (azimuth) between two geodetic Points.
     * 
     * @param gv1
     *        geodetic point 1
     * @param gv2
     *        geodetic point 2
     * @return the azimuth angle as a double. Convention used : azimuth is angle from the north direction
     *         to the current direction in CLOCKWISE sense.
     * @throws PatriusException
     *         if points are too close from each other
     */
    public final double computeBearing(final GeodeticPoint gv1, final GeodeticPoint gv2) throws PatriusException {

        // Local variables
        final double gv1Lat = gv1.getLatitude();
        final double gv2Lat = gv2.getLatitude();
        final double gv1Long = gv1.getLongitude();
        final double gv2Long = gv2.getLongitude();

        // Test that points are different (latitude and longitude only)
        if (MathLib.abs(gv1Lat - gv2Lat) < Precision.EPSILON &&
                MathLib.abs(gv1Long - gv2Long) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.PDB_POINTS_TOO_CLOSE);
        }

        // compute delta of longitude ( -PI <= deltaLon < PI)
        final double lon1 = MathUtils.normalizeAngle(gv1Long, 0.);
        final double lon2 = MathUtils.normalizeAngle(gv2Long, 0.);
        final double deltaLon = lon2 - lon1;
        // final double geodeticLat1 = gv1Lat;
        // final double geodeticLat2 = gv2Lat;

        // angle from North direction in trigo sense
        double bearing = 0;

        // test if points are along a meridian (simplified problem)
        if (MathLib.abs(deltaLon) < Precision.EPSILON && gv1Lat > gv2Lat) {
            bearing = FastMath.PI;
        }

        final Double dY = MathLib.abs(gv2Lat - gv1Lat);

        if (dY > Precision.EPSILON) {
            // Compute mercator Y for each latitude
            final double mercatorLat2 = this.computeMercatorLatitude(gv2Lat);
            final double mercatorLat1 = this.computeMercatorLatitude(gv1Lat);
            final double deltaLat = mercatorLat2 - mercatorLat1;
            // compute bearing (from North)
            bearing = MathLib.atan2(deltaLon, deltaLat);
        } else {
            // along a parallel (general method has 0/0 indetermination)
            if (deltaLon > 0.) {
                // East
                bearing = FastMath.PI / 2.0;
            } else {
                // West
                bearing = -FastMath.PI / 2.0;
            }
        }

        // Azimuth is centered around PI
        return MathUtils.normalizeAngle(bearing, FastMath.PI);
    }

    /**
     * Compute the spherical azimuth (clock wise) between two points.
     * 
     * @param p1
     *        first point
     * @param p2
     *        second point
     * @return the spherical azimuth (clock wise)
     */
    public double computeSphericalAzimuth(final GeodeticPoint p1, final GeodeticPoint p2) {
        // Intermediate variables
        // Lat/Lon
        final double phi1 = p1.getLatitude();
        final double phi2 = p2.getLatitude();
        final double lambda1 = p1.getLongitude();
        final double lambda2 = p2.getLongitude();
        final double[] sincosPhi1 = MathLib.sinAndCos(phi1);
        final double sinphi1 = sincosPhi1[0];
        final double cosphi1 = sincosPhi1[1];
        final double[] sincosPhi2 = MathLib.sinAndCos(phi2);
        final double sinphi2 = sincosPhi2[0];
        final double cosphi2 = sincosPhi2[1];
        final double ldiff = lambda2 - lambda1;
        // calculate spherical azimuth(centered around PI)
        final double azimuth = MathLib.atan2(cosphi2 * (float) MathLib.sin(ldiff),
            cosphi1 * (float) sinphi2 - sinphi1 * cosphi2 * (float) MathLib.cos(ldiff));
        // Normalize angle
        return MathUtils.normalizeAngle(azimuth, FastMath.PI);
    }

    /**
     * Compute crescent latitude (Le) at a given geodetic latitude.
     * also called Mercator latitude. See following link for demonstration of formula :
     * 
     * @see <a href="http://cartes-martinique.pagesperso-orange.fr/LatitudesCroissantes.htm">
     *      http://cartes-martinique.pagesperso-orange.fr/LatitudesCroissantes.htm</a>
     * @param geodeticLat
     *        geodetic latitude
     * @return crescent latitude (Le) also called Mercator latitude
     */
    public double computeMercatorLatitude(final double geodeticLat) {
        double lat = geodeticLat;
        if (MathLib.abs(geodeticLat) > Mercator.MAX_LATITUDE) {
            lat = MathLib.signum(geodeticLat) * Mercator.MAX_LATITUDE;
        }
        final double e = this.getEccentricity();
        final double eSL = e * MathLib.sin(lat);
        final double k = MathLib.pow((1.0 - eSL) / (1.0 + eSL), e / 2.);
        final double cotZ2 = k * MathLib.tan(FastMath.PI / 4.0 + lat / 2.);
        return MathLib.log(cotZ2);
    }

    /**
     * Compute radius of curvature section East/West (also called M or Re).
     * Distance to the Intersection of normal to ellipsoid at the given latitude with pole axis.
     * 
     * @param geodeticLat
     *        geodetic latitude must between : - PI/2 and PI/2
     * @return radius of curvatureEast/West (also called M or Re)
     */
    public double computeRadiusEastWest(final double geodeticLat) {
        final double sinLat = MathLib.sin(geodeticLat);
        final double ecc = this.getEccentricity();
        return MathLib.divide(this.getEquatorialRadius(),
            MathLib.sqrt(MathLib.max(0.0, 1. - ecc * ecc * sinLat * sinLat)));
    }

    /**
     * Loxodromic distance between P1 and P2.This is the distance of constant bearing (or along a line in Mercator).
     * This method comes from libSpace.
     * 
     * @param p1
     *        Point 1
     * @param p2
     *        Point 2
     * @return distance in meters along loxodromic
     * @throws PatriusException
     *         if latitude of one point is not between -/+ 89.999 deg
     */
    public double computeLoxodromicDistance(final GeodeticPoint p1, final GeodeticPoint p2) throws PatriusException {
        final double long2 = MathUtils.normalizeAngle(p2.getLongitude(), 0.);
        final double long1 = MathUtils.normalizeAngle(p1.getLongitude(), 0.);
        final double deltaLon = MathUtils.normalizeAngle(long2 - long1, 0.);
        final double geodeticLat1 = p1.getLatitude();
        final double geodeticLat2 = p2.getLatitude();

        // along a meridian (simplified problem)
        if (MathLib.abs(deltaLon) < Precision.EPSILON) {
            return MathLib.abs(this.computeMeridionalDistance(geodeticLat2)
                    - this.computeMeridionalDistance(geodeticLat1));
        }

        if (MathLib.abs(geodeticLat1) > Mercator.MAX_LATITUDE || MathLib.abs(geodeticLat2) > Mercator.MAX_LATITUDE) {
            throw new PatriusException(PatriusMessages.PDB_LATITUDE_CLOSE_90);
        }
        final double x = MathLib.abs(deltaLon);
        final double dY = MathLib.abs(geodeticLat2 - geodeticLat1);

        // result
        double loxDist = 0.;
        if (dY > LOXODROMIC_PRECISION) {
            // Compute mercator Y for each latitude
            final double y1 = this.computeMercatorLatitude(geodeticLat1);
            final double y2 = this.computeMercatorLatitude(geodeticLat2);

            final double k = MathLib.atan2(deltaLon, y2 - y1);
            final double dL =
                this.computeMeridionalDistance(geodeticLat2) - this.computeMeridionalDistance(geodeticLat1);
            loxDist = MathLib.abs(MathLib.divide(dL, MathLib.cos(k)));
        } else {
            // along a parallel (general method has 0/0 indetermination)
            final double[] sincos = MathLib.sinAndCos(geodeticLat1);
            final double sinLat = sincos[0];
            final double cosLat = sincos[1];
            final double eSL = this.getEccentricity() * sinLat;
            loxDist = MathLib.divide(this.getEquatorialRadius() * x * cosLat,
                MathLib.sqrt(MathLib.max(0.0, 1. - eSL * eSL)));
        }
        // return distance in meters along loxodromic
        return loxDist;
    }

    /**
     * Compute the distance from a given geodetic latitude to the equator, along a meridian.
     * 
     * @param geodeticLat
     *        geodetic latitude must between : - PI/2 and PI/2
     * @return meridionalDistance (in meters)
     */
    public final double computeMeridionalDistance(final double geodeticLat) {
        final double[] seriesComp = this.getSeries();
        final double coefAP = seriesComp[0];
        final double coefBP = seriesComp[1];
        final double coefCP = seriesComp[2];
        final double coefDP = seriesComp[3];
        final double coefEP = seriesComp[4];
        return this.getEquatorialRadius() * (coefAP * geodeticLat +
                this.evalSinEven(geodeticLat, coefBP, coefCP, coefDP, coefEP));
    }

    /**
     * Compute the geodetic latitude, from a distance from the equator.
     * 
     * @param distance
     *        distance from the equator
     * @return geodetic latitude
     */
    public final double computeInverseMeridionalDistance(final double distance) {
        final double[] seriesComp = this.getSeries();
        final double coefAP = seriesComp[0];
        return this.computeInverseRectifyingLatitude(MathLib.divide(distance, this.getEquatorialRadius() * coefAP));
    }

    /**
     * Compute the point coordinates from an origin point, an azimuth and a distance along the rhumb
     * line (Loxodrome). The computation is done on the geoid associated to the geodetic point. The method is issue from
     * Snyder, Map Projection, A working manuel, p46/47.
     * The method comes from libSpace
     * The precision is
     * <ul>
     * <li>4 cm for 1000 km</li>
     * <li>4 mm for 100 km</li>
     * <li><1 mm for 1 km</li>
     * </ul>
     * but is slower than the simplified method (about 20%)
     * 
     * @see <a href="https://pubs.er.usgs.gov/publication/pp1395"> https://pubs.er.usgs.gov/publication/pp1395</a>
     * @param p1
     *        initial point
     * @param distance
     *        distance along the rhumb line
     * @param azimuth
     *        Convention used : azimuth is angle from the north direction
     *        to the current direction in CLOCKWISE sense.
     * @return the resulting geodetic point
     * @throws PatriusException
     *         if computed latitudes are out of range
     */
    public final GeodeticPoint
        computePointAlongLoxodrome(final GeodeticPoint p1,
                                   final double distance, final double azimuth) throws PatriusException {

        // the minus sign of "-FastMath.sin(azimuth)" is due to the convention used for azimuth angle (CLOCKWISE sense)
        final double[] sincos = MathLib.sinAndCos(azimuth);
        final double sinAz = sincos[0];
        final double cosAz = sincos[1];

        // geodetic lat
        final double lat1 = p1.getLatitude();
        final double lat2;
        double lon2 = p1.getLongitude();

        if (MathLib.abs(sinAz) < AZIMUTH_PRECISION) {
            // along a meridian
            lat2 = this.computeInverseMeridionalDistance(this.computeMeridionalDistance(lat1) + cosAz * distance);
        } else if (MathLib.abs(cosAz) > AZIMUTH_PRECISION) {
            // general case, cos(bearing) != 0
            final double dLat = MathLib.divide(cosAz * distance, this.getEquatorialRadius());
            // errors if computed latitudes are out of range
            if (MathLib.abs(lat1) > Mercator.MAX_LATITUDE) {
                throw new PatriusException(PatriusMessages.PDB_LATITUDE_OUT_OF_RANGE, MathUtils.RAD_TO_DEG * lat1,
                    MathUtils.RAD_TO_DEG * Mercator.MAX_LATITUDE);
            } else if (MathLib.abs(lat1 + dLat) > Mercator.MAX_LATITUDE) {
                throw new PatriusException(PatriusMessages.PDB_LATITUDE_OUT_OF_RANGE,
                    MathUtils.RAD_TO_DEG * (lat1 + dLat), MathUtils.RAD_TO_DEG * Mercator.MAX_LATITUDE);
            }

            final double y1 = this.computeMercatorLatitude(lat1);
            // M = M(geodeticLat) + dist * cos(Az) and then use M to solve for final geodeticLat
            // A) Simplified solution : Spherical earth
            // geodeticLat += dLat;
            // B) Ellipsoid solution by power series
            lat2 =
                this.computeInverseMeridionalDistance(dLat * this.getEquatorialRadius()
                        + this.computeMeridionalDistance(lat1));
            // C) Ellipsoid solution by elliptic integral evaluation
            // geodeticLat = inverseMeridionalDistance(dLat * _A + meridionalDistance(geodeticLat));
            final double y2 = this.computeMercatorLatitude(lat2);
            double dLon = MathLib.abs(MathLib.divide((y2 - y1) * sinAz, cosAz));
            if (sinAz < 0) {
                dLon = -dLon;
            }
            lon2 += dLon * MathLib.signum(distance);
        } else {
            // along a parallel (general method has 0/0 indetermination)
            lat2 = lat1;
            final double[] sincosLat = MathLib.sinAndCos(lat1);
            final double sinLat = sincosLat[0];
            final double cosLat = sincosLat[1];
            final double eSL = this.getEccentricity() * sinLat;
            double dLon = MathLib.divide(distance * MathLib.sqrt(MathLib.max(0.0, 1. - eSL * eSL)),
                this.getEquatorialRadius() * cosLat);
            // if((sinLat > 0 && sinAz < 0) || (sinLat < 0 && sinAz > 0) ){
            if (sinAz < 0) {
                dLon = -dLon;
            }
            lon2 += dLon;
        }
        return new GeodeticPoint(lat2, lon2, 0.);
    }

    /**
     * compute geodetic latitude at a given rectifying latitude.
     * rectifying latitude -> giving sphere that has correct distances along the meridians
     * 
     * @param rectifyingLat
     *        rectifying latitude
     * @return geodetic latitude (rad)
     */
    public double computeInverseRectifyingLatitude(final double rectifyingLat) {
        final double[] seriesComp = this.getSeries();
        final double coefbp = seriesComp[5];
        final double coefcp = seriesComp[6];
        final double coefdp = seriesComp[C_7];
        final double coefep = seriesComp[C_8];
        return rectifyingLat + this.evalSinEven(rectifyingLat, coefbp, coefcp, coefdp, coefep);
    }

    /**
     * Compute the orthodromic distance between two points.
     * This code is issue from <b>Vincenty's</b> works.
     * If points are equal it returns 0, else it calls the the <b>Vincenty</b> method, with more elementary parameters.
     * 
     * @see <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf"> http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf</a>
     * @param p1
     *        first point
     * @param p2
     *        second point
     * @return orthodromic distance on ellipsoid.
     */
    public final double computeOrthodromicDistance(final GeodeticPoint p1, final GeodeticPoint p2) {
        double orthodromicDistance = 0;

        // Local variables
        final double p1Lat = p1.getLatitude();
        final double p2Lat = p2.getLatitude();
        final double p1Long = p1.getLongitude();
        final double p2Long = p2.getLongitude();

        // Test that points are different (latitude and longitude only)
        if (MathLib.abs(p1Lat - p2Lat) > Precision.EPSILON ||
                MathLib.abs(p1Long - p2Long) > Precision.EPSILON) {
            orthodromicDistance = this.computeOrthodromicDistance(p1Lat, p1Long,
                p2Lat, p2Long);
        }
        return orthodromicDistance;
    }

    /**
     * Compute the orthodromic distance. This code is issue from <b>Vincenty</b>'s works.
     * 
     * @see <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf"> http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf</a>
     * @param lat1
     *        latitude point 1.
     * @param lon1
     *        longitude point 1.
     * @param lat2
     *        latitude point 2.
     * @param lon2
     *        longitude point 2.
     * @return Orthodromic distance on ellipsoid.
     */
    public final double computeOrthodromicDistance(final double lat1, final double lon1,
                                                   final double lat2, final double lon2) {

        // Initialization
        double s = 0;

        // Ellipsoid variables :
        final double a = this.getEquatorialRadius();
        final double f = this.getFlattening();
        final double b = a * (1. - f);

        // calculations
        final double a2 = a * a;
        final double b2 = b * b;
        final double a2b2b2 = MathLib.divide(a2 - b2, b2);

        // least difference between longitudes. It handles the discontinuity at 180 deg
        double delta = MathLib.abs(lon2 - lon1);
        if (delta > FastMath.PI) {
            delta -= 2 * FastMath.PI;
        }
        // lon2 - lon1;
        final double omega = delta;

        final double tanphi1 = MathLib.tan(lat1);
        final double tanU1 = (1.0 - f) * tanphi1;
        final double u11 = MathLib.atan(tanU1);
        final double[] sincosU1 = MathLib.sinAndCos(u11);
        final double sinU1 = sincosU1[0];
        final double cosU1 = sincosU1[1];

        final double tanphi2 = MathLib.tan(lat2);
        final double tanU2 = (1.0 - f) * tanphi2;
        final double u22 = MathLib.atan(tanU2);
        final double[] sincosU2 = MathLib.sinAndCos(u22);
        final double sinU2 = sincosU2[0];
        final double cosU2 = sincosU2[1];

        final double sinU1sinU2 = sinU1 * sinU2;
        final double cosU1sinU2 = cosU1 * sinU2;
        final double sinU1cosU2 = sinU1 * cosU2;
        final double cosU1cosU2 = cosU1 * cosU2;

        // eq. 13
        double lambda = omega;

        // intermediates we'll need to compute 's'
        double a3 = 0.0;
        double b3 = 0.0;
        double sigma = 0.0;
        double deltasigma = 0.0;
        double lambda0;

        for (int i = 0; i < C20; i++) {
            lambda0 = lambda;

            final double[] sincosL = MathLib.sinAndCos(lambda);
            final double sinlambda = sincosL[0];
            final double coslambda = sincosL[1];

            // eq. 14
            final double sin2sigma = cosU2 * sinlambda * cosU2 * sinlambda + (cosU1sinU2 - sinU1cosU2 * coslambda) *
                    (cosU1sinU2 - sinU1cosU2 * coslambda);
            final double sinsigma = MathLib.sqrt(sin2sigma);

            // eq. 15
            final double cossigma = sinU1sinU2 + cosU1cosU2 * coslambda;

            // eq. 16
            sigma = MathLib.atan2(sinsigma, cossigma);

            // eq. 17 Careful! sin2sigma might be almost 0!
            final double sinalpha = sin2sigma == 0 ? 0.0 : MathLib.divide(cosU1cosU2 * sinlambda, sinsigma);
            final double alpha = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, sinalpha)));
            final double cosalpha = MathLib.cos(alpha);
            final double cos2alpha = cosalpha * cosalpha;

            // eq. 18 Careful! cos2alpha might be almost 0!
            final double cos2sigmam = cos2alpha == 0.0 ? 0.0 : cossigma - MathLib.divide(2 * sinU1sinU2, cos2alpha);
            final double u2 = cos2alpha * a2b2b2;

            final double cos2sigmam2 = cos2sigmam * cos2sigmam;

            // eq. 3
            a3 = 1.0 + u2 / C16384 * (C4096 + u2 * (-C768 + u2 * (C320 - C175 * u2)));

            // eq. 4
            b3 = u2 / C1024 * (C256 + u2 * (-C128 + u2 * (C74 - C47 * u2)));

            // eq. 6
            deltasigma = b3 * sinsigma * (cos2sigmam + b3 / 4 * (cossigma * (-1 + 2 * cos2sigmam2) -
                    b3 / 6 * cos2sigmam * (C_N3 + 4 * sin2sigma) * (C_N3 + 4 * cos2sigmam2)));

            // eq. 10
            final double c = f / C16 * cos2alpha * (4 + f * (4 - 3 * cos2alpha));

            // eq. 11 (modified)
            lambda = omega + (1 - c) * f * sinalpha * (sigma + c * sinsigma *
                    (cos2sigmam + c * cossigma * (-1 + 2 * cos2sigmam2)));

            // see how much improvement we got
            final double change;
            if (lambda == 0) {
                change = Double.NaN;
            } else {
                change = MathLib.abs(MathLib.divide(lambda - lambda0, lambda));
            }

            if (i > 1 && change < Precision.EPSILON) {
                // converged = true;
                break;
            }
        }

        // eq. 19
        s = b * a3 * (sigma - deltasigma);

        return s;

        // Following code was in the initial code retrieved from LibKernel but no used in this method
        // boolean converged = false;
        // double alpha1 = 0d;
        // double alpha2 = 0d;
        //
        // // didn't converge? must be N/S
        // if (!converged) {
        // if (lat1 > lat2) {
        // alpha1 = FastMath.PI;
        // alpha2 = 0.0;
        // }
        // else if (lat1 < lat2) {
        // alpha1 = 0.0;
        // alpha2 = FastMath.PI;
        // }
        // else {
        // alpha1 = Double.NaN;
        // alpha2 = Double.NaN;
        // }
        // }
        // else {
        // // else, it converged, so do the math eq. 20
        // double radians = FastMath.atan2(cosU2 * FastMath.sin(lambda),
        // cosU1sinU2 - sinU1cosU2 * FastMath.cos(lambda));
        // if (radians < 0.0) {
        // radians += 2 * FastMath.PI;
        // }
        // alpha1 = radians;
        //
        // // eq. 21
        // radians = FastMath.atan2(cosU1 * FastMath.sin(lambda),
        // -sinU1cosU2 + cosU1sinU2 * FastMath.cos(lambda)) + FastMath.PI;
        // if (radians < 0.0) {
        // radians += 2 * FastMath.PI;
        // alpha2 = radians;
        // }
        // }
        //
        // if (alpha1 >= 2 * FastMath.PI) {
        // alpha1 -= 2 * FastMath.PI;
        // }
        // if (alpha2 >= 2 * FastMath.PI) {
        // alpha2 -= 2 * FastMath.PI;
        // }

    }

    /**
     * Compute a geodetic point along orthodrome, from a point <code>p<sub>1</sub></code>, at a distance <code>d</code>,
     * in a direction defined from an azimuth. This is the direct geodetic problem.
     * This code is issue from <b>Vincenty</b>'s works.
     * 
     * @see <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf"> http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf</a>
     * @param p1
     *        Origin point.
     * @param distance
     *        Distance to compute resulting point.
     * @param azimuthDirection
     *        azimuth direction, convention used : azimuth is angle from the north direction
     *        to the current direction in CLOCKWISE sense.
     * @return the geodetic point
     */
    public final GeodeticPoint computePointAlongOrthodrome(final GeodeticPoint p1,
                                                           final double distance,
                                                           final double azimuthDirection) {
        // Ellipsoid variables
        final double a = this.getEquatorialRadius();
        final double f = this.getFlattening();
        final double b = a * (1. - f);

        final double aSquared = a * a;
        final double bSquared = b * b;
        final double phi1 = p1.getLatitude();
        // Alpha1 = Azimuth clockwise
        final double alpha1 = MathUtils.normalizeAngle(azimuthDirection, 0);
        final double[] sincosAlpha = MathLib.sinAndCos(alpha1);
        final double sinAlpha1 = sincosAlpha[0];
        final double cosAlpha1 = sincosAlpha[1];
        final double s = distance;
        final double tanU1 = (1.0 - f) * MathLib.tan(phi1);
        final double cosU1 = 1.0 / MathLib.sqrt(1.0 + tanU1 * tanU1);
        final double sinU1 = tanU1 * cosU1;

        // eq. 1
        final double sigma1 = MathLib.atan2(tanU1,
            cosAlpha1);

        // eq. 2
        final double sinAlpha = cosU1 * sinAlpha1;

        final double sin2Alpha = sinAlpha * sinAlpha;
        final double cos2Alpha = 1 - sin2Alpha;
        final double uSquared = MathLib.divide(cos2Alpha * (aSquared - bSquared), bSquared);

        // eq. 3
        final double a3 = 1 + uSquared / C16384 * (C4096 + uSquared * (-C768 + uSquared * (C320 - C175 * uSquared)));

        // eq. 4
        final double b3 = uSquared / C1024 * (C256 + uSquared * (-C128 + uSquared * (C74 - C47 * uSquared)));

        // iterate until there is a negligible change in sigma
        double deltaSigma;
        final double sOverbA = MathLib.divide(s, b * a3);
        double sigma = sOverbA;
        double sinSigma;
        double prevSigma = sOverbA;
        double sigmaM2;
        double cosSigmaM2;
        double cos2SigmaM2;

        // break after converging to tolerance (a maximum number of iteration has been set)
        for (int i = 0; i < MAX_ITER; i++) {
            // eq. 5
            sigmaM2 = 2.0 * sigma1 + sigma;
            cosSigmaM2 = MathLib.cos(sigmaM2);
            cos2SigmaM2 = cosSigmaM2 * cosSigmaM2;
            final double[] sincosSigma = MathLib.sinAndCos(sigma);
            sinSigma = sincosSigma[0];
            final double cosSignma = sincosSigma[1];

            // eq. 6
            deltaSigma = b3 * sinSigma * (cosSigmaM2 + b3 / 4.0 * (cosSignma * (-1 + 2 * cos2SigmaM2) -
                    b3 / 6.0 * cosSigmaM2 * (C_N3 + 4 * sinSigma * sinSigma) * (C_N3 + 4 * cos2SigmaM2)));

            // eq. 7
            sigma = sOverbA + deltaSigma;
            // break after converging to tolerance
            if (MathLib.abs(sigma - prevSigma) < THRESHOLD) {
                break;
            }
            prevSigma = sigma;
        }
        sigmaM2 = 2.0 * sigma1 + sigma;
        cosSigmaM2 = MathLib.cos(sigmaM2);
        cos2SigmaM2 = cosSigmaM2 * cosSigmaM2;

        final double[] sincosSigma2 = MathLib.sinAndCos(sigma);
        final double cosSigma = sincosSigma2[1];
        sinSigma = sincosSigma2[0];

        // eq. 8
        final double phi2 = MathLib.atan2(sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1,
            (1.0 - f) * MathLib.sqrt(sin2Alpha + MathLib.pow(sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1,
                2.0)));

        // eq. 9
        // This fixes the pole crossing defect spotted by Matt Feemster. When a
        // path passes a pole and essentially crosses a line of latitude twice -
        // once in each direction - the longitude calculation got messed up.
        // Using atan2 instead of atan fixes the defect. The change is in the
        // next 3 lines.
        // double tanLambda = sinSigma * sinAlpha1 / (cosU1 * cosSigma - sinU1 *
        // sinSigma * cosAlpha1);
        // double lambda = FastMath.atan(tanLambda);
        final double lambda = MathLib.atan2(sinSigma * sinAlpha1,
            cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1);

        // eq. 10
        final double c = f / C16 * cos2Alpha * (4 + f * (4 - 3 * cos2Alpha));

        // eq. 11
        final double l = lambda - (1 - c) * f * sinAlpha *
                (sigma + c * sinSigma * (cosSigmaM2 + c * cosSigma * (-1 + 2 * cos2SigmaM2)));

        // eq. 12
        // double alpha2 = FastMath.atan2(sinAlpha, -sinU1 * sinSigma + cosU1 *
        // cosSigma * cosAlpha1);

        // build result
        final double latitudeOut = phi2;
        final double longitudeOut = p1.getLongitude() + l;

        return new GeodeticPoint(latitudeOut, longitudeOut, 0d);
    }

    /**
     * Discretize a rhumb line into <i>N</i> segments, between two points. The result returned contains the first and
     * the last point.
     * 
     * @param from
     *        first geodetic point
     * @param to
     *        ending geodetic point
     * @param maxLength
     *        This distance is used when a discretization occurs : i.e :
     *        when the line property is not coherent with
     *        the projection ( {@link EnumLineProperty}). This parameter represent a <b>distance</b> expressed in
     *        meters. This will be the maximal distance between two points of the projected polygon. If you set the
     *        parameter to a value <=0 no discretization will be done.<br>
     * @return a list of geodetic points
     * @throws PatriusException
     *         if points are too close from each other
     */
    public final List<GeodeticPoint>
        discretizeRhumbLine(final GeodeticPoint from,
                            final GeodeticPoint to, final double maxLength) throws PatriusException {

        if (maxLength <= 0.) {
            // no discretization. the two points are returned
            final ArrayList<GeodeticPoint> result = new ArrayList<GeodeticPoint>(1);
            result.add(from);
            return result;
        }

        // Compute full length :
        final double fullLength = this.computeLoxodromicDistance(from, to);

        // compute number of intermediate points
        final double maximalDistance = MathLib.abs(maxLength);
        final int nbPoint = (int) MathLib.divide(fullLength, maximalDistance);

        // Compute effective delta to add :
        final double delta = MathLib.divide(fullLength, nbPoint);
        // Initialization :
        GeodeticPoint pointToAdd = null;
        GeodeticPoint tmpPoint = from;
        // Get the azimuth :
        final double az = this.computeBearing(from, to);
        // Resulting point list :
        final List<GeodeticPoint> list = new ArrayList<GeodeticPoint>();
        // Add first point :
        list.add(from);
        // One segment only : add final point :
        if (nbPoint == 1) {
            list.add(to);
        } else {
            // Discretize :
            for (int i = 1; i < nbPoint; i++) {
                // Compute new point position :
                pointToAdd = this.computePointAlongLoxodrome(tmpPoint, delta, az);
                // Add it :
                list.add(pointToAdd);
                tmpPoint = pointToAdd;
                // Ending condition : Too far : add last point and break :
                if ((i + 1) * delta > fullLength) {
                    list.add(to);
                    break;
                }
            }
        }
        return list;
    }

    /**
     * Discretize a great circle into <i>N</i> segments, between two points. The result returned contains the first and
     * the last point.
     * 
     * @param from
     *        first geodetic point
     * @param to
     *        ending geodetic point
     * @param maxLength
     *        This distance is used when a discretization occurs : i.e :
     *        when the line property is not coherent with
     *        the projection ( {@link EnumLineProperty}). This parameter represent a <b>distance</b> expressed in
     *        meters. This will be the maximal distance between two points of the projected polygon. If you set the
     *        parameter to a value <=0, no discretization will be done.<br>
     * @return a list of geodetic points
     */
    public final List<GeodeticPoint> discretizeGreatCircle(final GeodeticPoint from, final GeodeticPoint to,
                                                           final double maxLength) {

        if (maxLength <= 0.) {
            // no discretization. the two points are returned
            final ArrayList<GeodeticPoint> result = new ArrayList<GeodeticPoint>(2);
            result.add(from);
            result.add(to);
            return result;
        }

        // Compute full length :
        final double fullLength = this.computeOrthodromicDistance(from, to);
        // compute number of intermediate points
        final double maximalDistance = MathLib.abs(maxLength);
        final int nbPoint = (int) MathLib.divide(fullLength, maximalDistance);

        // Compute effective delta to add :
        final double delta = MathLib.divide(fullLength, nbPoint);
        // Initialization :
        GeodeticPoint pointToAdd = null;
        GeodeticPoint tmpPoint = from;
        // Get initial spherical azimuth (clockwise)
        double az = this.computeSphericalAzimuth(from, to);
        // Resulting point list :
        final List<GeodeticPoint> list = new ArrayList<GeodeticPoint>();
        // Add first point :
        list.add(from);
        // One segment only : add final point :
        if (nbPoint == 1) {
            list.add(to);
        } else {
            // Discretize :
            for (int i = 1; i < nbPoint; i++) {
                // Compute new point position :
                pointToAdd = this.computePointAlongOrthodrome(tmpPoint, delta, az);
                // Add it :
                list.add(pointToAdd);
                tmpPoint = pointToAdd;
                // Compute new azimuth :
                az = this.computeSphericalAzimuth(pointToAdd, to);
                // Ending condition : Too far : add last point and break :
                if ((i + 1) * delta > fullLength) {
                    list.add(to);
                    break;
                }
            }
        }
        return list;
    }

    /**
     * Evaluation of A*sin(2x) + B*sin(4x) + C*sin(6x) + D*sin(8x)
     * 
     * @param xCoef
     *        double x
     * @param aCoef
     *        A coefficient
     * @param bCoef
     *        B coefficient
     * @param cCoef
     *        C coefficient
     * @param dCoef
     *        D coefficient
     * @return the evaluation of A*sin(2x) + B*sin(4x) + C*sin(6x) + D*sin(8x)
     */
    private double evalSinEven(final double xCoef, final double aCoef, final double bCoef,
                               final double cCoef, final double dCoef) {
        final double x2 = xCoef + xCoef;
        final double[] sincos = MathLib.sinAndCos(x2);
        final double sin2x = sincos[0];
        double cos2x2 = sincos[1];
        cos2x2 += cos2x2;
        return sin2x * (aCoef - cCoef + cos2x2 * (bCoef - 2. * dCoef + cos2x2 * (cCoef + dCoef * cos2x2)));
    }

    /**
     * Get the eccentricity.
     * 
     * @return the eccentricity
     */
    public double getEccentricity() {
        final double f = this.getFlattening();
        return MathLib.sqrt(MathLib.max(0.0, f * (2.0 - f)));
    }

    /**
     * Getter for series.
     * 
     * @return the series
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[] getSeries() {
        if (!this.seriesComputed) {
            this.computeSeries();
            this.seriesComputed = true;
        }
        return this.series;
    }
}
