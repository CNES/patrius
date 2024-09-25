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
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.projections;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * The Mercator projection is a cylindrical map projection which became the standard map projection for nautical
 * purposes because of its ability to represent lines of constant course, known loxodromes, as straight segments. While
 * the linear scale is constant in all directions around any point, thus preserving the angles and the shapes of small
 * objects (which makes the projection conformal), the Mercator projection distorts the size and shape of large objects,
 * as the scale increases from the Equator to the poles, where it becomes infinite.
 * </p>
 * <p>
 * The mercator projection is defined by a point, called pivot. The longitude of this point is the center of X axis of
 * projected point. The latitude of this point is used to compute the scaleFactor. The origin of Y axis is always the
 * equator parallel. Default pivot is (lat=0,lon=0).
 * </p>
 * <p>
 * Is an azimuth is given to the Mercator projection, the (X,Y) frame will be rotated, as Y axis is folliwing the given
 * cap. Default azimuth is 0.
 * </p>
 * <p>
 * The inverse computation can be done using series, or by an iterative way The series are two times faster, but the
 * precision is 5e-5 meters instead of 1e-8 meters.
 *
 * @concurrency not thread-safe
 * @author Galpin Thomas
 * @version $Id$
 * @since 3.2
 */
public class Mercator extends AbstractProjection {

    /** Maximum latitude from which a Mercator transformation can be defined. */
    public static final double MAX_LATITUDE = 89.999 * MathUtils.DEG_TO_RAD;

    /** Maximum number of iterations in {@link Mercator#computePhi(double, double)} when solving for F(phi) = 0. */
    private static final double MAX_ITER = 100;

    /** Convergence threshold used in {@link Mercator#computePhi(double, double)} when solving for F(phi) = 0. */
    private static final double THRESHOLD = 1.e-14;

    /** Threshold. */
    private static final double EAST_EPSILON = 1e-8;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C12 = 12.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C13 = 13.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C24 = 24.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C29 = 29.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C48 = 48.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C81 = 81.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C120 = 120.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C240 = 240.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C360 = 360.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C811 = 811.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C1120 = 1120.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C4279 = 4279.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C11520 = 11520.;

    /** Constant used in {@link Mercator#computePhi(double, double)}. */
    private static final double C161280 = 161280.;

    /** Serializable UID. */
    private static final long serialVersionUID = 5232288358412768877L;

    /**
     * Azimuth (for rotated Mercator projection), can be 0. Convention used : azimuth is angle from the north direction
     * to the current direction in CLOCKWISE sense.
     */
    private final double azimuth;

    /** Cosinus of the azimuth. */
    private final double cosAz;

    /** Sinus of the azimuth. */
    private final double sinAz;

    /** If centered, Y coordinates equal 0 in pivot point. The rotation if around pivot. */
    private final boolean centered;

    /** Indicates if the inverse projection uses a direct method with series, or an iterative method. */
    private final boolean series;

    /** Maximum east value which can be set on map. */
    private final double maxEastValue;

    /** Maximum northing value which can be set on map. */
    private final double maxNorthValue;

    /** Cosinus of the latitude at the pivot point. */
    private final double pivotCosLat;

    /** Mercator latitude of pivot. */
    private final double lat0Mer;

    /** Longitude of pivot point. */
    private double lon0;

    /** Celestial body scaled radius. */
    private final double scaledRadius;

    /**
     * Complete constructor. This is the One standard parallel Mercator Projection with a scale factor at the natural
     * origin. The latitude is here defined as being the Equator. The central meridian is defined as the map center
     * longitude in radians. The scale factor is defined at the natural origin (Equator).
     *
     * @param pivotIn
     *        pivot point used for projection. It also contains reference ellipsoid
     * @param azimuthIn
     *        is angle from the north direction to the current direction in <b>CLOCKWISE</b> sense
     * @param centeredIn
     *        if true, Y coordinates equal 0 in pivot point. The rotation if around pivot.
     * @param seriesIn
     *        boolean indicating if the inverse projection uses a direct method with series, or an iterative method
     * @throws IllegalArgumentException
     *         if the pivot point's body shape isn't a {@link OneAxisEllipsoid}
     */
    public Mercator(final EllipsoidPoint pivotIn, final double azimuthIn, final boolean centeredIn,
                    final boolean seriesIn) {
        super(pivotIn);

        // This class needs an OneAxisEllipsoid associated to its pivot point to work properly
        if (!(getReference() instanceof OneAxisEllipsoid)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.NOT_ASSOCIATED_ONEAXISELLIPSOID);
        }

        // longitude of pivot point
        this.lon0 = MathUtils.normalizeAngle(pivotIn.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
            .getLongitude(), 0.);
        // azimuth centered around Pi
        this.azimuth = MathUtils.normalizeAngle(azimuthIn, FastMath.PI);
        // Cosinus and sinus of the azimuth
        final double[] sincos = MathLib.sinAndCos(this.azimuth);
        this.cosAz = sincos[1];
        this.sinAz = sincos[0];

        this.centered = centeredIn;
        this.series = seriesIn;
        final double lat0 = pivotIn.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();

        // Re-scale CelestialBody radius
        final double pivotScaleFactor = getScaleFactor(lat0);
        this.pivotCosLat = MathLib.cos(lat0);
        this.scaledRadius = getCastedReference().getEquatorialRadius() * pivotScaleFactor;

        // Define boundary zone : at point at max latitude, opposite at the pivot longitude
        // Get parameters : use re-scaled CelestialBody radius.
        this.maxEastValue = FastMath.PI * this.scaledRadius;
        this.maxNorthValue = ProjectionEllipsoidUtils.computeMercatorLatitude(MAX_LATITUDE, getCastedReference())
                * this.scaledRadius;

        this.lat0Mer = ProjectionEllipsoidUtils.computeMercatorLatitude(lat0, getCastedReference());
    }

    /**
     * Constructor with default values :
     * <ul>
     * <li>The latitude of pivot is 0</li>
     * <li>azimuth is null (no rotation)</li>
     * <li>centered is false.</li>
     * </ul>
     *
     * @param centralMeridian
     *        central meridian
     * @param ref
     *        reference shape
     */
    public Mercator(final double centralMeridian, final EllipsoidBodyShape ref) {
        this(new EllipsoidPoint(ref, LLHCoordinatesSystem.ELLIPSODETIC, 0., centralMeridian, 0., "pivot"), 0., false,
                true);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean canMap(final EllipsoidPoint coordinates) {
        return MathLib.abs(
            coordinates.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude()) <= MAX_LATITUDE;
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D applyTo(final EllipsoidPoint point) throws PatriusException {
        return this.applyTo(point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(),
            point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude());
    }

    /** {@inheritDoc} */
    @Override
    public Vector2D applyTo(final double lat, final double lon) throws PatriusException {

        // temporary variable
        double latUsed = lat;

        // Handle boundaries
        if (MathLib.abs(lat) > MAX_LATITUDE) {
            latUsed = MathLib.signum(lat) * MAX_LATITUDE;
        }

        // compute mercator latitude (also called crescent latitude)
        final double latMerc = ProjectionEllipsoidUtils.computeMercatorLatitude(latUsed, getCastedReference());

        // compute longitude (relative to the pivot)
        if (this.lon0 == 0 && getPivotPoint() != null) {
            this.lon0 = MathUtils.normalizeAngle(getPivotPoint().getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
                .getLongitude(), 0.);
        }
        final double lonMerc = MathUtils.normalizeAngle(lon - this.lon0, 0.);

        // multiply by a and scale factor
        double xMer = lonMerc * this.scaledRadius;
        double yMer = latMerc * this.scaledRadius;

        // if centered , subtract pivot Y value
        if (this.centered) {
            yMer -= this.lat0Mer * this.scaledRadius;
        }

        // Rotation in necessary
        if (this.sinAz != 0.) {
            final double xi = xMer * this.cosAz - yMer * this.sinAz;
            final double yi = xMer * this.sinAz + yMer * this.cosAz;
            xMer = xi;
            yMer = yi;
        }
        return new Vector2D(xMer, yMer);
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint applyInverseTo(final double x, final double y, final double alt) throws PatriusException {
        final EllipsoidPoint coord = this.applyInverseTo(x, y);
        return new EllipsoidPoint(getReference(), LLHCoordinatesSystem.ELLIPSODETIC,
            coord.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(),
            coord.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude(), alt, BodyPointName.DEFAULT);
    }

    /**
     * {@inheritDoc} This is the inverse transformation process to convert two dimensional map coordinates into
     * geodetics one. The iterative method used is a Newton-Raphson iterative process. We're searching the solution of
     * F(phi) = 0. At each iterations, we compute a new value of phi : phi' = phi - F(phi)/F'(phi) where phi is the
     * trial value, F(phi) = y(phi) - phi, with y(phi) the direct Mercator transformation, and F'(phi) the derive
     * function of y(phi). The stop condition is satisfied when F(phi) < epsilon (here equal at 1E-14). This method
     * returns geodetic coordinates. The iterative process stop if the iteration's number is bigger than 100.
     */
    @Override
    public EllipsoidPoint applyInverseTo(final double x, final double y) throws PatriusException {

        double xMer = x;
        double yMer = y;

        // apply the rotation,in opposite direction if necessary
        if (this.sinAz != 0.) {
            final double xi = xMer * this.cosAz + yMer * this.sinAz;
            final double yi = -xMer * this.sinAz + yMer * this.cosAz;
            xMer = xi;
            yMer = yi;
        }

        // if centered , add pivot Y value
        if (this.centered) {
            yMer += this.lat0Mer * this.scaledRadius;
        }

        // Check input coordinates validity.
        if (MathLib.abs(yMer) > this.maxNorthValue) {
            throw new PatriusException(PatriusMessages.PDB_NORTHING_OUT_OF_RANGE, this.maxNorthValue, yMer);
        }
        // 1 e-8 = error due to the inverse rotation
        if (MathLib.abs(xMer) > this.maxEastValue + EAST_EPSILON) {
            throw new PatriusException(PatriusMessages.PDB_EASTING_OUT_OF_RANGE, this.maxEastValue, xMer);
        }

        // Divide by a and scale factor
        xMer = xMer / this.scaledRadius;
        yMer = yMer / this.scaledRadius;

        // Initialization of phi0 and phi F_phi
        double phi0 = yMer;

        // Initialize a default value
        final double c1point4 = 1.4;
        if (phi0 > c1point4) {
            phi0 = c1point4;
        } else if (phi0 < -c1point4) {
            phi0 = -c1point4;
        }

        // compute phi, with or without using series
        final double phi = computePhi(phi0, yMer);

        return new EllipsoidPoint(getReference(), LLHCoordinatesSystem.ELLIPSODETIC, phi, this.lon0 + xMer, 0.,
            BodyPointName.DEFAULT);
    }

    /**
     * Private method to compute phi.
     *
     * @param phi0
     *        initial value for phi
     * @param yMer
     *        yMer
     * @return computed phi
     */
    private double computePhi(final double phi0, final double yMer) {

        // Initialization of F_phi and phi
        double fPhi = 1.;
        double phi = 0;

        // eccentricity
        final double e = ProjectionEllipsoidUtils.getEccentricity(getCastedReference());
        final double half = 0.5;

        if (this.series) {

            final double es = e * e;
            final double es2 = es * es;
            final double coefaB = es * (half + es * (5.0 / C24 + es * (1.0 / C12 + C13 / C360 * es)));
            final double coefbB = es2 * (7.0 / C48 + es * (C29 / C240 + C811 / C11520 * es));
            final double coefcB = es2 * es * (7.0 / C120 + C81 / C1120 * es);
            final double coefdB = C4279 / C161280 * es2 * es2;

            // xphi = mercator latitude
            final double xphi = 2.0 * MathLib.atan(MathLib.exp(yMer)) - FastMath.PI / 2.0;
            final double[] sincos = MathLib.sinAndCos(xphi);
            final double cosxphi = sincos[1];
            final double cos2xphi = 2 * cosxphi * cosxphi - 1;
            final double cosxphi3 = cosxphi * cosxphi * cosxphi;
            final double sinxphi = sincos[0];
            final double sin2xphi = 2 * sinxphi * cosxphi;
            final double sinxphi3 = sinxphi * sinxphi * sinxphi;
            final double cos4xphi = 2 * cos2xphi * cos2xphi - 1;
            final double sin4xphi = 2 * cos2xphi * sin2xphi;

            // Real phi
            phi = xphi + coefaB * sin2xphi + coefbB * sin4xphi + coefcB * 2 * (4 * cosxphi3 - 3 * cosxphi)
                    * (3 * sinxphi - 4 * sinxphi3) + coefdB * 2 * cos4xphi * sin4xphi;
        } else {
            // Research of solution : F_phi = 0
            // At each iteration, a new value of phi is computed : phi : phi = phi - F/F'
            int iter = 1;
            while (MathLib.abs(fPhi) > THRESHOLD) {
                final double sinPhi = MathLib.sin(phi);
                final double tan = MathLib.tan(FastMath.PI / 4. + phi / 2);
                final double factor = MathLib.pow(MathLib.divide(1. - e * sinPhi, 1. + e * sinPhi), e * half);
                // F(phi) = y(phi) - phi
                fPhi = MathLib.log(tan * factor) - yMer;
                // Derivative function of Phi
                final double dFphi = MathLib.divide(1. - e * e, (1. - e * e * sinPhi * sinPhi) * (1. - sinPhi) * tan);
                // New value of phi
                phi -= MathLib.divide(fPhi, dFphi);
                // If the latitude is out of allowed value, it's set to the maximum latitude value
                if (phi > MAX_LATITUDE) {
                    phi = MAX_LATITUDE;
                } else if (phi < -MAX_LATITUDE) {
                    phi = -MAX_LATITUDE;
                }
                // Use watchdog to avoid infinite loop
                if (iter > MAX_ITER) {
                    break;
                }
                iter++;
            }
        }
        return phi;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConformal() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEquivalent() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public EnumLineProperty getLineProperty() {
        return EnumLineProperty.STRAIGHT_RHUMB_LINE;
    }

    /** {@inheritDoc} */
    @Override
    public final double getMaximumLatitude() {
        return MAX_LATITUDE;
    }

    /** {@inheritDoc} */
    @Override
    public final double getMaximumEastingValue() {
        return this.maxEastValue;
    }

    /**
     * Get the azimuth of the projection (CLOCKWISE).
     *
     * @return the azimuth (CLOCKWISE)
     */
    public double getAzimuth() {
        return this.azimuth;
    }

    /**
     * Get the maximum northing value in meters.
     *
     * @return maximum northing value in meters.
     */
    public final double getMaximumNorthingValue() {
        return this.maxNorthValue;
    }

    /**
     * Returns the scale factor at a specific latitude. The result is the fraction Mercator distance / real distance.
     *
     * @param lat
     *        latitude
     * @return the scale factor
     */
    public final double getScaleFactor(final double lat) {
        final double[] sincos = MathLib.sinAndCos(lat);
        final double cosLat = sincos[1];
        final double sinLat = sincos[0];
        final double e = ProjectionEllipsoidUtils.getEccentricity(getCastedReference());
        return MathLib.divide(cosLat, MathLib.sqrt(MathLib.max(0.0, 1. - e * e * sinLat * sinLat)));
    }

    /** {@inheritDoc} */
    @Override
    public final double getDistortionFactor(final double lat) {
        final EllipsoidPoint pivot = getPivotPoint();
        final double pivotRadius = ProjectionEllipsoidUtils.computeRadiusEastWest(
            pivot.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude(), getCastedReference());
        final double latRadius = ProjectionEllipsoidUtils.computeRadiusEastWest(lat, getCastedReference());
        return MathLib.divide(pivotRadius * this.pivotCosLat, latRadius * MathLib.cos(lat));
    }

    /**
     * Cast the reference to work with the services.<br>
     * This can be done safely as the verification is directly made in the constructor.
     * 
     * @return the casted reference
     */
    private OneAxisEllipsoid getCastedReference() {
        return (OneAxisEllipsoid) getReference();
    }
}
