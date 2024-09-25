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
 * HISTORY
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2998:15/11/2021:[PATRIUS] Discontinuite methode computeBearing de la classe ProjectionEllipsoid...
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.5:DM:DM-2473:27/05/2020:methode computeCenterPointAlongLoxodrome dans la classe ProjectionEllipsoidUtils
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:FA:FA-2113:15/05/2019:Usage des exceptions pour gerer un if/else dans Ellipsoid.getPointLocation()
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.projections;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class provides utility methods needed for projections.
 *
 * @concurrency not thread-safe
 *
 * @see EllipsoidBodyShape
 *
 * @author Thomas Galpin
 * @version $Id$
 * @since 4.3
 */
public final class ProjectionEllipsoidUtils {

    /** Local precision for geodetic problem computation (in meters). */
    public static final double GEODETIC_PRECISION = 1E-5;

    /** Local precision for azimuth problem computation (in meters). */
    private static final double AZIMUTH_PRECISION = 1E-8;

    /** Threshold for loxodromic problem computation. */
    private static final double LOXODROMIC_PRECISION = 1E-12;

    /**
     * Maximum number of iterations used in method
     * {@link ProjectionEllipsoidUtils#computePointAlongOrthodrome(EllipsoidPoint, double, double)}.
     */
    private static final double MAX_ITER = 200;

    /**
     * Convergence threshod used in method
     * {@link ProjectionEllipsoidUtils#computePointAlongOrthodrome(EllipsoidPoint, double, double)}.
     */
    private static final double THRESHOLD = 1.0E-13;

    /** Constant used in the class. */
    private static final int C_N3 = -3;

    /** Constant used in the class. */
    private static final int C_7 = 7;

    /** Constant used in the class. */
    private static final int C_8 = 8;

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

    /**
     * Ellipsoid series coefficients used to compute ellipsoid series.
     *
     * @see <a href="https://pubs.er.usgs.gov/publication/pp1395"> https://pubs.er.usgs.gov/publication/pp1395</a>
     */
    private static double[] series;

    /** Boolean to indicate if series have been computed. */
    private static EllipsoidBodyShape ellipsoidCache = null;

    /**
     * Private constructor (utility class).
     */
    private ProjectionEllipsoidUtils() {
        // Private constructor
    }

    /**
     * Compute series following Snyder method => "Map Projection, A working manual" written by John P. Snyder, from page
     * 17.
     *
     * @param shape
     *        Body shape
     *
     * @see <a href="https://pubs.er.usgs.gov/publication/pp1395"> https://pubs.er.usgs.gov/publication/pp1395</a>
     * @since 3.2
     */
    private static void computeSeries(final OneAxisEllipsoid shape) {

        // SNYDER : direct method Snyder, equation 3-21 limited to sin(6 * lat)
        // eccentricity
        final double e = getEccentricity(shape);
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
        series = seriesComp;
    }

    /**
     * Compute the bearing (azimuth) between two points.
     *
     * @param p1
     *        first point
     * @param p2
     *        second point
     * @return the azimuth angle as a double. Convention used : azimuth is angle from the north direction to the current
     *         direction in
     *         CLOCKWISE sense
     * @throws PatriusException
     *         if points aren't associated to the same body shape<br>
     *         if the body shape isn't a {@link OneAxisEllipsoid}<br>
     *         if points are too close from each other
     */
    public static final double computeBearing(final EllipsoidPoint p1, final EllipsoidPoint p2)
        throws PatriusException {

        final EllipsoidBodyShape shape = p1.getBodyShape();
        // Check if the points are associated to the same shape by only evaluating its name
        if (!shape.getName().equals(p2.getBodyShape().getName())) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_SAME_BODYSHAPE);
        }
        if (!(shape instanceof OneAxisEllipsoid)) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_ONEAXISELLIPSOID);
        }

        // Local variables
        final double p1Lat = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double p2Lat = p2.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double p1Long = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
        final double p2Long = p2.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();

        // Test that points are different (latitude and longitude only)
        if (MathLib.abs(p1Lat - p2Lat) < Precision.EPSILON && MathLib.abs(p1Long - p2Long) < Precision.EPSILON) {
            throw new PatriusException(PatriusMessages.PDB_POINTS_TOO_CLOSE);
        }

        // compute delta of longitude ( -PI <= deltaLon < PI)
        final double lon1 = MathUtils.normalizeAngle(p1Long, 0.);
        final double lon2 = MathUtils.normalizeAngle(p2Long, lon1);
        final double deltaLon = lon2 - lon1;

        // angle from North direction in trigo sense
        double bearing = 0.;

        // test if points are along a meridian (simplified problem)
        if (MathLib.abs(deltaLon) < Precision.EPSILON && p1Lat > p2Lat) {
            bearing = FastMath.PI;
        }

        final Double dY = MathLib.abs(p2Lat - p1Lat);

        final OneAxisEllipsoid shapeCast = (OneAxisEllipsoid) shape;

        if (dY > Precision.EPSILON) {
            // Compute mercator Y for each latitude
            final double mercatorLat2 = computeMercatorLatitude(p2Lat, shapeCast);
            final double mercatorLat1 = computeMercatorLatitude(p1Lat, shapeCast);
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
     * @throws PatriusException
     *         if points aren't associated to the same body shape
     */
    public static double computeSphericalAzimuth(final EllipsoidPoint p1, final EllipsoidPoint p2)
        throws PatriusException {

        // Check if the points are associated to the same shape by only evaluating its name
        if (!p1.getBodyShape().getName().equals(p2.getBodyShape().getName())) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_SAME_BODYSHAPE);
        }

        // Intermediate variables
        // Lat/Lon
        final double phi1 = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double phi2 = p2.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double lambda1 = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
        final double lambda2 = p2.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
        final double[] sincosPhi1 = MathLib.sinAndCos(phi1);
        final double sinphi1 = sincosPhi1[0];
        final double cosphi1 = sincosPhi1[1];
        final double[] sincosPhi2 = MathLib.sinAndCos(phi2);
        final double sinphi2 = sincosPhi2[0];
        final double cosphi2 = sincosPhi2[1];
        final double ldiff = lambda2 - lambda1;
        // calculate spherical azimuth(centered around PI)
        final double azimuth =
            MathLib.atan2(cosphi2 * (float) MathLib.sin(ldiff), cosphi1 * (float) sinphi2 - sinphi1 * cosphi2
                    * (float) MathLib.cos(ldiff));
        // Normalize angle
        return MathUtils.normalizeAngle(azimuth, FastMath.PI);
    }

    /**
     * Compute crescent latitude (Le) at a given geodetic latitude. also called Mercator latitude. See following link
     * for demonstration of formula :
     *
     * @see <a href="http://cartes-martinique.pagesperso-orange.fr/LatitudesCroissantes.htm">
     *      http://cartes-martinique.pagesperso-orange.fr/LatitudesCroissantes.htm</a>
     * @param geodeticLat
     *        Geodetic latitude
     * @param shape
     *        Body shape
     * @return crescent latitude (Le) also called Mercator latitude
     */
    public static double computeMercatorLatitude(final double geodeticLat, final OneAxisEllipsoid shape) {

        double lat = geodeticLat;
        if (MathLib.abs(geodeticLat) > Mercator.MAX_LATITUDE) {
            lat = MathLib.signum(geodeticLat) * Mercator.MAX_LATITUDE;
        }
        final double e = getEccentricity(shape);
        final double eSL = e * MathLib.sin(lat);
        final double k = MathLib.pow((1.0 - eSL) / (1.0 + eSL), e / 2.);
        final double cotZ2 = k * MathLib.tan(FastMath.PI / 4.0 + lat / 2.);
        return MathLib.log(cotZ2);
    }

    /**
     * Compute radius of curvature section East/West (also called M or Re). Distance to the Intersection of normal to
     * ellipsoid at the given latitude with pole axis.
     *
     * @param geodeticLat
     *        Geodetic latitude must between : - PI/2 and PI/2
     * @param shape
     *        Body shape
     * @return radius of curvatureEast/West (also called M or Re)
     */
    public static double computeRadiusEastWest(final double geodeticLat, final OneAxisEllipsoid shape) {

        final double sinLat = MathLib.sin(geodeticLat);
        final double ecc = getEccentricity(shape);
        return MathLib.divide(shape.getARadius(), MathLib.sqrt(MathLib.max(0.0, 1. - ecc * ecc * sinLat * sinLat)));
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
     *         if points aren't associated to the same body shape<br>
     *         if the body shape isn't a {@link OneAxisEllipsoid}<br>
     *         if latitude of one point is not between -/+ 89.999 deg
     */
    public static double computeLoxodromicDistance(final EllipsoidPoint p1, final EllipsoidPoint p2)
        throws PatriusException {

        final EllipsoidBodyShape shape = p1.getBodyShape();
        // Check if the points are associated to the same shape by only evaluating its name
        if (!shape.getName().equals(p2.getBodyShape().getName())) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_SAME_BODYSHAPE);
        }
        // Check the shape in an OneAxisEllipsoid
        if (!(shape instanceof OneAxisEllipsoid)) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_ONEAXISELLIPSOID);
        }
        final OneAxisEllipsoid shapeCast = (OneAxisEllipsoid) shape;

        final double long2 = MathUtils.normalizeAngle(p2.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
            .getLongitude(), 0.);
        final double long1 = MathUtils.normalizeAngle(p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
            .getLongitude(), 0.);
        final double deltaLon = MathUtils.normalizeAngle(long2 - long1, 0.);
        final double lat1 = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double lat2 = p2.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();

        // along a meridian (simplified problem)
        if (MathLib.abs(deltaLon) < Precision.EPSILON) {
            return MathLib.abs(computeMeridionalDistance(lat2, shapeCast) - computeMeridionalDistance(lat1, shapeCast));
        }

        if (MathLib.abs(lat1) > Mercator.MAX_LATITUDE || MathLib.abs(lat2) > Mercator.MAX_LATITUDE) {
            throw new PatriusException(PatriusMessages.PDB_LATITUDE_CLOSE_90);
        }
        final double x = MathLib.abs(deltaLon);
        final double dY = MathLib.abs(lat2 - lat1);

        // result
        double loxDist = 0.;
        if (dY > LOXODROMIC_PRECISION) {
            // Compute mercator Y for each latitude
            final double y1 = computeMercatorLatitude(lat1, shapeCast);
            final double y2 = computeMercatorLatitude(lat2, shapeCast);

            final double k = MathLib.atan2(deltaLon, y2 - y1);
            final double dL = computeMeridionalDistance(lat2, shapeCast) - computeMeridionalDistance(lat1, shapeCast);
            loxDist = MathLib.abs(MathLib.divide(dL, MathLib.cos(k)));
        } else {
            // along a parallel (general method has 0/0 indetermination)
            final double[] sincos = MathLib.sinAndCos(lat1);
            final double sinLat = sincos[0];
            final double cosLat = sincos[1];
            final double eSL = getEccentricity(shapeCast) * sinLat;
            loxDist = MathLib.divide(shapeCast.getARadius() * x * cosLat,
                MathLib.sqrt(MathLib.max(0.0, 1. - eSL * eSL)));
        }
        // return distance in meters along loxodromic
        return loxDist;
    }

    /**
     * Compute the distance from a given geodetic latitude to the equator, along a meridian.
     *
     * @param geodeticLat
     *        Geodetic latitude must between : - PI/2 and PI/2
     * @param shape
     *        Body shape
     * @return meridionalDistance (in meters)
     */
    public static final double computeMeridionalDistance(final double geodeticLat, final OneAxisEllipsoid shape) {

        final double[] seriesComp = getSeries(shape);
        final double coefAP = seriesComp[0];
        final double coefBP = seriesComp[1];
        final double coefCP = seriesComp[2];
        final double coefDP = seriesComp[3];
        final double coefEP = seriesComp[4];
        return shape.getARadius() * (coefAP * geodeticLat + evalSinEven(geodeticLat, coefBP, coefCP, coefDP, coefEP));
    }

    /**
     * Compute the geodetic latitude, from a distance from the equator.
     *
     * @param distance
     *        Distance from the equator
     * @param shape
     *        Body shape
     * @return geodetic latitude
     */
    public static final double computeInverseMeridionalDistance(final double distance, final OneAxisEllipsoid shape) {

        final double[] seriesComp = getSeries(shape);
        final double coefAP = seriesComp[0];
        return computeInverseRectifyingLatitude(MathLib.divide(distance, shape.getARadius() * coefAP), shape);
    }

    /**
     * Compute the point coordinates from an origin point, an azimuth and a distance along the rhumb line (Loxodrome).
     * The computation is done on the geoid associated to the geodetic point. The method is issue from Snyder, Map
     * Projection, A working manuel, p46/47. The method comes from libSpace The precision is:
     * <ul>
     * <li>4 cm for 1000 km</li>
     * <li>4 mm for 100 km</li>
     * <li><1 mm for 1 km</li>
     * </ul>
     * but is slower than the simplified method (about 20%).
     *
     * @see <a href="https://pubs.er.usgs.gov/publication/pp1395"> https://pubs.er.usgs.gov/publication/pp1395</a>
     * @param p1
     *        Initial point
     * @param distance
     *        Distance along the rhumb line
     * @param azimuth
     *        Convention used : azimuth is angle from the north direction to the current direction in CLOCKWISE sense
     * @return the resulting point
     * @throws PatriusException
     *         if the body shape isn't a {@link OneAxisEllipsoid}<br>
     *         if computed latitudes are out of range
     */
    public static final EllipsoidPoint computePointAlongLoxodrome(final EllipsoidPoint p1, final double distance,
                                                                  final double azimuth) throws PatriusException {

        final EllipsoidBodyShape shape = p1.getBodyShape();
        if (!(shape instanceof OneAxisEllipsoid)) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_ONEAXISELLIPSOID);
        }
        final OneAxisEllipsoid shapeCast = (OneAxisEllipsoid) shape;

        // the minus sign of "-FastMath.sin(azimuth)" is due to the convention used for azimuth angle (CLOCKWISE sense)
        final double sinAz = MathLib.sin(azimuth);
        final double cosAz = MathLib.cos(azimuth);

        // geodetic lat
        final double lat1 = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double lat2;
        double lon2 = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();

        if (MathLib.abs(sinAz) < AZIMUTH_PRECISION) {
            // along a meridian
            lat2 = computeInverseMeridionalDistance(computeMeridionalDistance(lat1, shapeCast) + cosAz * distance,
                shapeCast);
        } else if (MathLib.abs(cosAz) > AZIMUTH_PRECISION) {
            // general case, cos(bearing) != 0
            final double dLat = MathLib.divide(cosAz * distance, shapeCast.getEquatorialRadius());
            // errors if computed latitudes are out of range
            if (MathLib.abs(lat1) > Mercator.MAX_LATITUDE) {
                throw new PatriusException(PatriusMessages.PDB_LATITUDE_OUT_OF_RANGE, MathUtils.RAD_TO_DEG * lat1,
                    MathUtils.RAD_TO_DEG * Mercator.MAX_LATITUDE);
            } else if (MathLib.abs(lat1 + dLat) > Mercator.MAX_LATITUDE) {
                throw new PatriusException(PatriusMessages.PDB_LATITUDE_OUT_OF_RANGE, MathUtils.RAD_TO_DEG
                        * (lat1 + dLat), MathUtils.RAD_TO_DEG * Mercator.MAX_LATITUDE);
            }

            final double y1 = computeMercatorLatitude(lat1, shapeCast);
            // M = M(geodeticLat) + dist * cos(Az) and then use M to solve for final geodeticLat
            // A) Simplified solution : Spherical earth
            // geodeticLat += dLat;
            // B) Ellipsoid solution by power series
            lat2 = computeInverseMeridionalDistance(
                dLat * shapeCast.getEquatorialRadius() + computeMeridionalDistance(lat1, shapeCast), shapeCast);
            // C) Ellipsoid solution by elliptic integral evaluation
            // geodeticLat = inverseMeridionalDistance(dLat * _A + meridionalDistance(geodeticLat));
            final double y2 = computeMercatorLatitude(lat2, shapeCast);
            double dLon = MathLib.abs(MathLib.divide((y2 - y1) * sinAz, cosAz));
            if (sinAz < 0) {
                dLon = -dLon;
            }
            lon2 += dLon * MathLib.signum(distance);
        } else {
            // along a parallel (general method has 0/0 indetermination)
            lat2 = lat1;
            final double sinLat = MathLib.sin(lat1);
            final double cosLat = MathLib.cos(lat1);
            final double eSL = getEccentricity(shapeCast) * sinLat;
            double dLon = MathLib.divide(distance * MathLib.sqrt(MathLib.max(0.0, 1. - eSL * eSL)),
                shapeCast.getEquatorialRadius() * cosLat);
            // if((sinLat > 0 && sinAz < 0) || (sinLat < 0 && sinAz > 0) ){
            if (sinAz < 0) {
                dLon = -dLon;
            }
            lon2 += dLon;
        }
        return new EllipsoidPoint(shape, shape.getLLHCoordinatesSystem(), lat2, lon2, 0., BodyPointName.DEFAULT);
    }

    /**
     * compute geodetic latitude at a given rectifying latitude. rectifying latitude -> giving sphere that has correct
     * distances along the meridians.
     *
     * @param rectifyingLat
     *        Rectifying latitude
     * @param shape
     *        Body shape
     * @return geodetic latitude (rad)
     */
    public static double computeInverseRectifyingLatitude(final double rectifyingLat, final OneAxisEllipsoid shape) {

        final double[] seriesComp = getSeries(shape);
        final double coefbp = seriesComp[5];
        final double coefcp = seriesComp[6];
        final double coefdp = seriesComp[C_7];
        final double coefep = seriesComp[C_8];
        return rectifyingLat + evalSinEven(rectifyingLat, coefbp, coefcp, coefdp, coefep);
    }

    /**
     * Compute the orthodromic distance between two points. This code is issue from <b>Vincenty's</b> works. If points
     * are equal it returns 0, else it calls the the <b>Vincenty</b> method, with more elementary parameters.
     *
     * @see <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf"> http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf</a>
     * @param p1
     *        First point
     * @param p2
     *        Second point
     * @return orthodromic distance on ellipsoid.
     * @throws PatriusException
     *         if points aren't associated to the same body shape
     *         if the body shape isn't a {@link OneAxisEllipsoid}
     */
    public static final double computeOrthodromicDistance(final EllipsoidPoint p1, final EllipsoidPoint p2)
        throws PatriusException {

        final EllipsoidBodyShape shape = p1.getBodyShape();
        // Check if the points are associated to the same shape by only evaluating its name
        if (!shape.getName().equals(p2.getBodyShape().getName())) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_SAME_BODYSHAPE);
        }
        if (!(shape instanceof OneAxisEllipsoid)) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_ONEAXISELLIPSOID);
        }
        final OneAxisEllipsoid shapeCast = (OneAxisEllipsoid) shape;

        double orthodromicDistance = 0;

        // Local variables
        final double p1Lat = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double p2Lat = p2.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double p1Long = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
        final double p2Long = p2.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();

        // Test that points are different (latitude and longitude only)
        if (MathLib.abs(p1Lat - p2Lat) > Precision.EPSILON || MathLib.abs(p1Long - p2Long) > Precision.EPSILON) {
            orthodromicDistance = computeOrthodromicDistance(p1Lat, p1Long, p2Lat, p2Long, shapeCast);
        }
        return orthodromicDistance;
    }

    /**
     * Compute the orthodromic distance. This code is issue from <b>Vincenty</b>'s works.
     *
     * @see <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf"> http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf</a>
     * @param lat1
     *        Latitude point 1
     * @param lon1
     *        Longitude point 1
     * @param lat2
     *        Latitude point 2
     * @param lon2
     *        Longitude point 2
     * @param shape
     *        Body shape
     * @return the orthodromic distance on ellipsoid
     */
    public static final double computeOrthodromicDistance(final double lat1, final double lon1, final double lat2,
                                                          final double lon2, final OneAxisEllipsoid shape) {

        // Initialization
        double s = 0;

        // Ellipsoid variables :
        final double a = shape.getEquatorialRadius();
        final double f = shape.getFlattening();
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
        final double sinU1 = MathLib.sin(u11);
        final double cosU1 = MathLib.cos(u11);

        final double tanphi2 = MathLib.tan(lat2);
        final double tanU2 = (1.0 - f) * tanphi2;
        final double u22 = MathLib.atan(tanU2);
        final double sinU2 = MathLib.sin(u22);
        final double cosU2 = MathLib.cos(u22);

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

            final double sinlambda = MathLib.sin(lambda);
            final double coslambda = MathLib.cos(lambda);

            // eq. 14
            final double sin2sigma = cosU2 * sinlambda * cosU2 * sinlambda + (cosU1sinU2 - sinU1cosU2 * coslambda)
                    * (cosU1sinU2 - sinU1cosU2 * coslambda);
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
            lambda = omega + (1 - c) * f * sinalpha
                    * (sigma + c * sinsigma * (cos2sigmam + c * cossigma * (-1 + 2 * cos2sigmam2)));

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
     * Compute a point along orthodrome, from a point <code>p<sub>1</sub></code>, at a distance <code>d</code>, in a
     * direction defined from an azimuth. This is the direct geodetic problem. This code is issue from <b>Vincenty</b>'s
     * works.
     *
     * @see <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf"> http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf</a>
     * @param p1
     *        Origin point
     * @param distance
     *        Distance to compute resulting point
     * @param azimuthDirection
     *        Azimuth direction, convention used : azimuth is angle from the north direction to the current direction in
     *        CLOCKWISE sense
     * @return the point
     * @throws PatriusException
     *         if the body shape isn't a {@link OneAxisEllipsoid}
     */
    public static final EllipsoidPoint computePointAlongOrthodrome(final EllipsoidPoint p1, final double distance,
                                                                   final double azimuthDirection)
        throws PatriusException {

        final EllipsoidBodyShape shape = p1.getBodyShape();
        if (!(shape instanceof OneAxisEllipsoid)) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_ONEAXISELLIPSOID);
        }
        final OneAxisEllipsoid shapeCast = (OneAxisEllipsoid) shape;

        // Ellipsoid variables
        final double a = shapeCast.getEquatorialRadius();
        final double f = shapeCast.getFlattening();
        final double b = a * (1. - f);

        final double aSquared = a * a;
        final double bSquared = b * b;
        final double phi1 = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
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
        final double sigma1 = MathLib.atan2(tanU1, cosAlpha1);

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
            deltaSigma = b3 * sinSigma * (cosSigmaM2 + b3 / 4.0 * (cosSignma * (-1 + 2 * cos2SigmaM2)
                    - b3 / 6.0 * cosSigmaM2 * (C_N3 + 4 * sinSigma * sinSigma) * (C_N3 + 4 * cos2SigmaM2)));

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
            (1.0 - f) * MathLib.sqrt(sin2Alpha + MathLib.pow(sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1, 2.0)));

        // eq. 9
        // This fixes the pole crossing defect spotted by Matt Feemster. When a
        // path passes a pole and essentially crosses a line of latitude twice -
        // once in each direction - the longitude calculation got messed up.
        // Using atan2 instead of atan fixes the defect. The change is in the
        // next 3 lines.
        // double tanLambda = sinSigma * sinAlpha1 / (cosU1 * cosSigma - sinU1 *
        // sinSigma * cosAlpha1);
        // double lambda = FastMath.atan(tanLambda);
        final double lambda = MathLib.atan2(sinSigma * sinAlpha1, cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1);

        // eq. 10
        final double c = f / C16 * cos2Alpha * (4 + f * (4 - 3 * cos2Alpha));

        // eq. 11
        final double l = lambda - (1 - c) * f * sinAlpha
                * (sigma + c * sinSigma * (cosSigmaM2 + c * cosSigma * (-1 + 2 * cos2SigmaM2)));

        // eq. 12
        // double alpha2 = FastMath.atan2(sinAlpha, -sinU1 * sinSigma + cosU1 *
        // cosSigma * cosAlpha1);

        // build result
        final double latitudeOut = phi2;
        final double longitudeOut = p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude() + l;

        return new EllipsoidPoint(shape, shape.getLLHCoordinatesSystem(), latitudeOut, longitudeOut, 0.,
            BodyPointName.DEFAULT);
    }

    /**
     * Discretize a rhumb line into <i>N</i> segments, between two points. The result returned contains the first and
     * the last point.
     *
     * @param from
     *        First point
     * @param to
     *        Ending point
     * @param maxLength
     *        This distance is used when a discretization occurs : i.e : when the line property is not coherent with the
     *        projection ( {@link EnumLineProperty}). This parameter represent a <b>distance</b> expressed in meters.
     *        This will be the maximal distance between two points of the projected polygon. If you set the parameter to
     *        a value <=0 no discretization will be done.
     * @return a list of points
     * @throws PatriusException
     *         if points aren't associated to the same body shape<br>
     *         if points are too close from each other
     */
    public static final List<EllipsoidPoint> discretizeRhumbLine(final EllipsoidPoint from, final EllipsoidPoint to,
                                                                 final double maxLength) throws PatriusException {

        if (maxLength <= 0.) {
            // no discretization. the two points are returned
            final ArrayList<EllipsoidPoint> result = new ArrayList<>(1);
            result.add(from);
            return result;
        }

        // Compute full length :
        final double fullLength = computeLoxodromicDistance(from, to);

        // compute number of intermediate points
        final double maximalDistance = MathLib.abs(maxLength);
        final int nbPoint = (int) MathLib.divide(fullLength, maximalDistance);

        // Compute effective delta to add :
        final double delta = MathLib.divide(fullLength, nbPoint);
        // Initialization :
        EllipsoidPoint pointToAdd = null;
        EllipsoidPoint tmpPoint = from;
        // Get the azimuth :
        final double az = computeBearing(from, to);
        // Resulting point list :
        final List<EllipsoidPoint> list = new ArrayList<>();
        // Add first point :
        list.add(from);
        // One segment only : add final point :
        if (nbPoint == 1) {
            list.add(to);
        } else {
            // Discretize :
            for (int i = 1; i < nbPoint; i++) {
                // Compute new point position :
                pointToAdd = computePointAlongLoxodrome(tmpPoint, delta, az);
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
     *        First point
     * @param to
     *        Ending point
     * @param maxLength
     *        This distance is used when a discretization occurs : i.e : when the line property is not coherent with the
     *        projection ( {@link EnumLineProperty}). This parameter represent a <b>distance</b> expressed in meters.
     *        This will be the maximal distance between two points of the projected polygon. If you set the parameter to
     *        a value <=0, no discretization will be done.
     * @return a list of points
     * @throws PatriusException
     *         if points aren't associated to the same body shape
     */
    public static final List<EllipsoidPoint> discretizeGreatCircle(final EllipsoidPoint from, final EllipsoidPoint to,
                                                                   final double maxLength) throws PatriusException {

        if (maxLength <= 0.) {
            // no discretization. the two points are returned
            final ArrayList<EllipsoidPoint> result = new ArrayList<>(2);
            result.add(from);
            result.add(to);
            return result;
        }

        // Compute full length :
        final double fullLength = computeOrthodromicDistance(from, to);
        // compute number of intermediate points
        final double maximalDistance = MathLib.abs(maxLength);
        final int nbPoint = (int) MathLib.divide(fullLength, maximalDistance);

        // Compute effective delta to add :
        final double delta = MathLib.divide(fullLength, nbPoint);
        // Initialization :
        EllipsoidPoint pointToAdd = null;
        EllipsoidPoint tmpPoint = from;
        // Get initial spherical azimuth (clockwise)
        double az = computeSphericalAzimuth(from, to);
        // Resulting point list :
        final List<EllipsoidPoint> list = new ArrayList<>();
        // Add first point :
        list.add(from);
        // One segment only : add final point :
        if (nbPoint == 1) {
            list.add(to);
        } else {
            // Discretize :
            for (int i = 1; i < nbPoint; i++) {
                // Compute new point position :
                pointToAdd = computePointAlongOrthodrome(tmpPoint, delta, az);
                // Add it :
                list.add(pointToAdd);
                tmpPoint = pointToAdd;
                // Compute new azimuth :
                az = computeSphericalAzimuth(pointToAdd, to);
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
     * Evaluation of {@code A*sin(2x) + B*sin(4x) + C*sin(6x) + D*sin(8x)}.
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
     * @return the evaluation of {@code A*sin(2x) + B*sin(4x) + C*sin(6x) + D*sin(8x)}
     */
    private static double evalSinEven(final double xCoef, final double aCoef, final double bCoef, final double cCoef,
                                      final double dCoef) {

        final double x2 = xCoef + xCoef;
        final double[] sincos = MathLib.sinAndCos(x2);
        final double sin2x = sincos[0];
        double cos2x2 = sincos[1];
        cos2x2 += cos2x2;
        return sin2x * (aCoef - cCoef + cos2x2 * (bCoef - 2. * dCoef + cos2x2 * (cCoef + dCoef * cos2x2)));
    }

    /**
     * Getter for the eccentricity.
     *
     * @param shape
     *        Body shape
     * @return the eccentricity
     */
    public static double getEccentricity(final OneAxisEllipsoid shape) {
        final double f = shape.getFlattening();
        return MathLib.sqrt(MathLib.max(0.0, f * (2.0 - f)));
    }

    /**
     * Getter for the series.
     *
     * @param shape
     *        Body shape
     * @return the series
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public static double[] getSeries(final OneAxisEllipsoid shape) {
        if (!shape.equals(ellipsoidCache)) {
            computeSeries(shape);
            ellipsoidCache = shape;
        }
        return series;
    }

    /**
     * Compute center point between two points along a loxodrome.
     *
     * @param p1
     *        First point
     * @param p2
     *        Second point
     * @return center point between two points along a loxodrome (warning: altitude is set to 0)
     * @throws PatriusException
     *         if computation failed
     */
    public static EllipsoidPoint computeCenterPointAlongLoxodrome(final EllipsoidPoint p1, final EllipsoidPoint p2)
        throws PatriusException {

        // Check if the points are associated to the same shape by only evaluating its name
        if (!p1.getBodyShape().getName().equals(p2.getBodyShape().getName())) {
            throw new PatriusException(PatriusMessages.NOT_ASSOCIATED_SAME_BODYSHAPE);
        }

        if (p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude() == p2.getLLHCoordinates(
            LLHCoordinatesSystem.ELLIPSODETIC).getLatitude()
                && p1.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude() == p2.getLLHCoordinates(
                    LLHCoordinatesSystem.ELLIPSODETIC).getLongitude()) {
            // Quick escape - Only the latitude & longitude values are used in each EllipsoidPoint
            return p1;
        }
        // Loxodromic distance between P1 and P2
        final double distance = ProjectionEllipsoidUtils.computeLoxodromicDistance(p1, p2);

        // Bearing cap from P1 to P2
        final double cap = ProjectionEllipsoidUtils.computeBearing(p1, p2);

        // Compute the center
        return ProjectionEllipsoidUtils.computePointAlongLoxodrome(p1, distance / 2., cap);
    }
}
