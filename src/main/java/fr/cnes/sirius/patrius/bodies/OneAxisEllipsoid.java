/**
 *
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
 * @history creation 15/06/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12:DM:DM-7:17/08/2023:[PATRIUS] Symétriser les méthodes closestPointTo de BodyShape
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-52:30/06/2023:[PATRIUS] Précision dans la méthode FacetBodyShape.getFieldData
 * VERSION:4.11.1:FA:FA-84:30/06/2023:[PATRIUS] Correction de la DM 3249
 * VERSION:4.11:DM:DM-3249:22/05/2023:[PATRIUS] Amelioration des transformations entre GeodeticPoint et Vector3D
 * VERSION:4.11:FA:FA-3322:22/05/2023:[PATRIUS] Erreur dans le calcul de normale autour d’un OneAxisEllipsoid
 * VERSION:4.11:FA:FA-47:22/05/2023:[PATRIUS] OneAxisEllipsoid.getApparentRadius erreur pour ptedScaled nul
 * VERSION:4.10.3:FA:FA-47:11/05/2023:[PATRIUS] OneAxisEllipsoid.getApparentRadius erreur pour ptedScaled nul
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights
 * VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection
 * a altitude
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC
 * VERSION:4.7:DM:DM-2909:18/05/2021:Ajout des methodes getIntersectionPoint, getClosestPoint(Vector2D) et getAlpha()
 * VERSION:4.6:DM:DM-2586:27/01/2021:[PATRIUS] intersection entre un objet de type «ExtendedOneAxisEllipsoid»
 * et une droite.
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] By-passer le critere du pas min dans l'integrateur numerique
 * DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::FA:471:03/11/2015:Modification method transform(point, frame, date) to take into account negative
 * altitudes
 * VERSION::DM:457:09/11/2015:Add the getflattening method to compute "Glint" direction
 * VERSION::FA:650:22/07/2016: ellipsoid corrections
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:708:13/12/2016: add documentation corrections
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:1308:12/09/2017:correct Ellipsoid non-convergence issue
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.IEllipsoid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Spheroid;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * One axis ellipsoid representation.
 * <p>
 * This ellipsoid is fully defined by its equatorial radius, its flattening and its associated body frame.
 * </p>
 *
 * @concurrency not thread-safe
 *
 * @concurrency.comment the use of frames makes this class not thread-safe
 *
 * @see AbstractEllipsoidBodyShape
 *
 * @author Thomas Trapier
 *
 * @version $Id: ExtendedOneAxisEllipsoid.java 17944 2017-09-12 15:24:41Z bignon $
 *
 * @since 1.2
 */
public class OneAxisEllipsoid extends AbstractEllipsoidBodyShape {

    /** Default ellipsoid name. */
    public static final String DEFAULT_ONE_AXIS_ELLIPSOID_NAME = "ONE_AXIS_ELLIPSOID";

    /** Serializable UID. */
    private static final long serialVersionUID = -6515537942032843597L;

    /** Flattening. */
    private final double fIn;

    /** Eccentricity power 2. */
    private final double e2;

    /** g * g. */
    private final double g2;

    /**
     * Constructor for the one axis ellipsoid with default name.
     *
     * @param ae
     *        Equatorial radius
     * @param f
     *        Flattening ({@code f = (a-b)/a})
     * @param bodyFrame
     *        Body frame related to the ellipsoid
     */
    public OneAxisEllipsoid(final double ae,
            final double f,
            final CelestialBodyFrame bodyFrame) {
        this(ae, f, bodyFrame, DEFAULT_ONE_AXIS_ELLIPSOID_NAME);
    }

    /**
     * Constructor for the one axis ellipsoid.
     *
     * @param ae
     *        Equatorial radius
     * @param f
     *        Flattening ({@code f = (a-b)/a})
     * @param bodyFrame
     *        Body frame related to the ellipsoid
     * @param name
     *        Name of the ellipsoid
     */
    public OneAxisEllipsoid(final double ae,
            final double f,
            final CelestialBodyFrame bodyFrame,
            final String name) {
        super(buildEllipsoid(ae, f), bodyFrame, isSpherical(f), name);

        // Complementary initialization
        this.fIn = f;
        this.e2 = f * (2.0 - f);
        final double g = 1.0 - f;
        this.g2 = g * g;
    }

    /**
     * Internal method to build the ellipsoid according to its flattening ({@link Sphere} or {@link Spheroid}).
     * 
     * @param ae
     *        Equatorial radius
     * @param f
     *        Flattening ({@code f = (a-b)/a})
     * @return the ellipsoid geometric representation
     */
    private static IEllipsoid buildEllipsoid(final double ae, final double f) {
        // choice : sphere or spheroid ?
        final IEllipsoid ellipsoid;
        if (MathLib.abs(f) < Precision.DOUBLE_COMPARISON_EPSILON) {
            ellipsoid = new Sphere(Vector3D.ZERO, ae);
        } else {
            ellipsoid = new Spheroid(Vector3D.ZERO, Vector3D.PLUS_K, ae, ae * (1. - f));
        }
        return ellipsoid;
    }

    /**
     * Internal method to determine if the ellipsoid can be considered as a sphere.
     * 
     * @param f
     *        Flattening ({@code f = (a-b)/a})
     * @return {@code true} if the ellipsoid can be considered as a sphere, {@code false} otherwise
     */
    private static boolean isSpherical(final double f) {
        // Evaluate the flattening to determine if the ellipsoid can be considered as a sphere
        final boolean isSpherical;
        if (MathLib.abs(f) < Precision.DOUBLE_COMPARISON_EPSILON) {
            isSpherical = true;
        } else {
            isSpherical = false;
        }
        return isSpherical;
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated since 4.13 as this method isn't relevant to this class description, use
     *             {@link #getEquatorialRadius()} instead.
     */
    @Override
    @Deprecated
    public double getTransverseRadius() {
        // Note: when the parent's deprecated method will be deleted, this method should be deleted too
        return getEquatorialRadius();
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated since 4.13 as this method isn't relevant to this class description, use {@link #getPolarRadius()}
     *             instead.
     */
    @Override
    @Deprecated
    public double getConjugateRadius() {
        // Note: when the parent's deprecated method will be deleted, this method should be deleted too
        return getPolarRadius();
    }

    /**
     * Getter for the equatorial radius of the body.
     *
     * @return the equatorial radius of the body (m)
     */
    @Override
    public double getEquatorialRadius() {
        // Note: when the parent's deprecated method will be deleted, this method should be kept without the "Override"
        // mention
        return getARadius();
    }

    /**
     * Getter for the polar radius of the body.
     *
     * @return the polar radius of the body (m)
     */
    public double getPolarRadius() {
        return getCRadius();
    }

    /**
     * Getter for the flattening.
     *
     * @return the flattening
     */
    @Override
    public double getFlattening() {
        // Note: when the parent's deprecated method will be deleted, this method should be kept without the "Override"
        // mention
        return this.fIn;
    }

    /**
     * Getter for the e2 (eccentricity e squared with {@code e = f * (2.0 - f)}).
     * 
     * @return the e2
     */
    @Override
    public double getE2() {
        // Note: when the parent's deprecated method will be deleted, this method should be kept without the "Override"
        // mention
        return this.e2;
    }

    /**
     * Getter for the g2 (g squared with {@code g = 1.0 - f}).
     * 
     * @return the g2
     */
    @Override
    public double getG2() {
        // Note: when the parent's deprecated method will be deleted, this method should be kept without the "Override"
        // mention
        return this.g2;
    }

    /** {@inheritDoc} */
    @Override
    public double getEncompassingSphereRadius() {
        return getARadius();
    }

    /** {@inheritDoc} */
    @Override
    public EllipsoidPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                               final AbsoluteDate date, final double altitude)
        throws PatriusException {

        if (MathLib.abs(altitude) < this.distanceEpsilon) {
            // Altitude is considered to be 0
            return getIntersectionPoint(line, close, frame, date);
        }

        // Build ellipsoid of required altitude
        final double aAlt = getARadius() + altitude;
        final double fAlt = 1. - (getARadius() * (1. - this.fIn) + altitude) / aAlt;
        final OneAxisEllipsoid ellipsoidAlt = new OneAxisEllipsoid(aAlt, fAlt, getBodyFrame(), getName());

        // Compute and return intersection point
        final EllipsoidPoint pointInAltitudeEllipsoid = ellipsoidAlt.getIntersectionPoint(line, close, frame, date);
        EllipsoidPoint res = null;
        if (pointInAltitudeEllipsoid != null) {
            final Vector3D pointInBodyFrame = pointInAltitudeEllipsoid.getPosition();
            res = new EllipsoidPoint(this, pointInBodyFrame, false, BodyPointName.INTERSECTION_AT_ALTITUDE);
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public OneAxisEllipsoid resize(final MarginType marginType, final double marginValue) {

        // Initialize equatorial radius and flattening
        final double aAlt;
        final double fAlt;

        // Check the margin type
        if (marginType.equals(MarginType.DISTANCE)) {
            // The margin type is distance
            // Check if the margin value is larger than the opposite of the polar (smallest) radius, to be sure that
            // the resulting polar (smallest) radius will be positive
            if (marginValue > -getEquatorialRadius() * (1 - getFlattening())) {
                // Modify equatorial radius
                aAlt = getARadius() + marginValue;
                // Modify flattening
                fAlt = 1. - (getARadius() * (1. - this.fIn) + marginValue) / aAlt;
            } else {
                // Invalid margin value
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INVALID_MARGIN_VALUE, marginValue);
            }
        } else {
            // The margin type is scale factor
            // Check if the margin value is positive, to be sure that the scale factor has a physical meaning
            if (marginValue > 0) {
                // Modify equatorial radius
                aAlt = getARadius() * marginValue;
                // To modify the polar radius, there is need to modify the flattening
                fAlt = this.fIn;
            } else {
                // Invalid margin value
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INVALID_MARGIN_VALUE, marginValue);
            }
        }

        // Return new OneAxisEllipsoid with modified equatorial radius and flattening
        return new OneAxisEllipsoid(aAlt, fAlt, getBodyFrame(), getName());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computePositionFromEllipsodeticCoordinates(final double latitude, final double longitude,
                                                               final double height) {

        // Implementation note: this implementation is more efficient than the parent's generic implmentation thanks to
        // the OneAxisEllipsoid parameters and properties

        // Longitude, cosine and sine
        final double[] sincosLon = MathLib.sinAndCos(longitude);
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];

        // Latitude, cosine and sine
        final double[] sincosLat = MathLib.sinAndCos(latitude);
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];

        // Altitude
        final double n = MathLib.divide(getARadius(), MathLib.sqrt(MathLib.max(0.0, 1.0 - this.e2 * sinLat * sinLat)));
        final double r = (n + height) * cosLat;

        // Resulting vector
        return new Vector3D(r * cosLon, r * sinLon, (this.g2 * n + height) * sinLat);
    }

    /**
     * Transform a surface-relative point to a cartesian point and compute the jacobian of the transformation.
     *
     * @param point
     *        geodetic point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * @return point at the same location but as a cartesian point
     * @deprecated since 4.13, use {@link LLHCoordinatesSystem#jacobianToCartesian(BodyPoint)} with
     *             {@link LLHCoordinatesSystem#ELLIPSODETIC ELLIPSODETIC} coordinates instead.
     */
    @Deprecated
    public Vector3D transformAndComputeJacobian(final EllipsoidPoint point, final double[][] jacobian) {
        final Vector3D transformedPoint = point.getPosition();
        this.computeJacobian(transformedPoint, point, jacobian);
        return transformedPoint;
    }

    /**
     * Compute the jacobian matrix of the transformation from geodetic point to Cartesian point.
     *
     * See "Algorithmes des routines du th&egraveme "changement de variables et de rep&egravere pour la trajectographie"
     * de la MSLIB (edition 4)".
     *
     * @param cartesianPoint
     *        cartesian point
     * @param point
     *        geodetic point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * @deprecated since 4.13, use {@link LLHCoordinatesSystem#jacobianToCartesian(BodyPoint)} with
     *             {@link LLHCoordinatesSystem#ELLIPSODETIC ELLIPSODETIC} coordinates instead.
     */
    @Deprecated
    private void computeJacobian(final Vector3D cartesianPoint, final EllipsoidPoint point,
                                 final double[][] jacobian) {

        // Temporary variables
        final double lat = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double[] sincosLat = MathLib.sinAndCos(lat);
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];

        final double lon = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
        final double[] sincosLon = MathLib.sinAndCos(lon);
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];

        final double alt = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();

        // local radius
        final double r = getARadius() / MathLib.sqrt(MathLib.max(0.0, 1 - (1 - this.g2) * sinLat * sinLat));
        final double k = this.g2 / (1 - (1 - this.g2) * sinLat * sinLat);

        // components of the jacobian matrix
        jacobian[0][0] = -(k * r + alt) * sinLat * cosLon;
        jacobian[0][1] = -(r + alt) * cosLat * sinLon;
        jacobian[0][2] = cosLat * cosLon;
        jacobian[1][0] = -(k * r + alt) * sinLat * sinLon;
        jacobian[1][1] = (r + alt) * cosLat * cosLon;
        jacobian[1][2] = cosLat * sinLon;
        jacobian[2][0] = (k * r + alt) * cosLat;
        jacobian[2][1] = 0.0;
        jacobian[2][2] = sinLat;

        // No result to return, jacobian modified directly
    }

    /**
     * Transform a cartesian point to a surface-relative point and compute the jacobian of the transformation.
     *
     * @param point
     *        cartesian point
     * @param frame
     *        frame in which cartesian point is expressed
     * @param date
     *        date of the point in given frame
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * @return point at the same location but as a surface-relative point, expressed in body frame
     * @throws PatriusException
     *         if point cannot be converted to body frame<br>
     *         if the point is close to one of the poles or if the altitude of the point is "too negative"
     * @deprecated since 4.13, use {@link LLHCoordinatesSystem#jacobianFromCartesian(BodyPoint)} with
     *             {@link LLHCoordinatesSystem#ELLIPSODETIC ELLIPSODETIC} coordinates instead.
     */
    @Deprecated
    public EllipsoidPoint transformAndComputeJacobian(final Vector3D point, final Frame frame, final AbsoluteDate date,
                                                      final double[][] jacobian) throws PatriusException {
        final EllipsoidPoint transformedPoint = buildPoint(point, frame, date, "transformedPoint");
        this.computeJacobian(transformedPoint, point, jacobian);
        return transformedPoint;
    }

    /**
     * Compute the jacobian matrix of the transformation from Cartesian point to geodetic point.
     *
     * See "Algorithmes des routines du th&egraveme "changement de variables et de rep&egravere pour la trajectographie"
     * de la MSLIB (edition 4)".
     *
     * @param point
     *        geodetic point
     * @param cartesianPoint
     *        cartesian point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * @throws PatriusException
     *         if the point is close to one of the poles or if the altitude of the point is "too negative"
     * @deprecated since 4.13, use {@link LLHCoordinatesSystem#jacobianFromCartesian(BodyPoint)} with
     *             {@link LLHCoordinatesSystem#ELLIPSODETIC ELLIPSODETIC} coordinates instead.
     */
    @Deprecated
    private void computeJacobian(final EllipsoidPoint point, final Vector3D cartesianPoint,
                                 final double[][] jacobian) throws PatriusException {

        // Cartesian coordinates
        final double x = cartesianPoint.getX();
        final double y = cartesianPoint.getY();
        final double dist = MathLib.sqrt(x * x + y * y);

        // case : the point is close to the poles
        if (dist < CLOSE_APPROACH_THRESHOLD * getARadius()) {
            // the point is close to one of the poles, the jacobian matrix cannot be computed
            throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
        }

        final double lat = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
        final double[] sincosLat = MathLib.sinAndCos(lat);
        final double sinLat = sincosLat[0];
        final double r = getARadius() / MathLib.sqrt(1 - (1 - this.g2) * sinLat * sinLat);
        final double k = this.g2 / (1 - (1 - this.g2) * sinLat * sinLat);

        final double alt = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();

        // the altitude of the point is "too negative"
        if (k * r + alt < CLOSE_APPROACH_THRESHOLD * getARadius()) {
            throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
        }
        
        // Temporary variables
        final double cosLat = sincosLat[1];

        final double lon = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
        final double[] sincos = MathLib.sinAndCos(lon);
        final double sinLon = sincos[0];
        final double cosLon = sincos[1];

        // components of the jacobian matrix
        // Parameter is directly modified
        jacobian[0][0] = -sinLat * cosLon / (k * r + alt);
        jacobian[0][1] = -sinLat * sinLon / (k * r + alt);
        jacobian[0][2] = cosLat / (k * r + alt);
        jacobian[1][0] = -sinLon / ((r + alt) * cosLat);
        jacobian[1][1] = cosLon / ((r + alt) * cosLat);
        jacobian[1][2] = 0.0;
        jacobian[2][0] = cosLat * cosLon;
        jacobian[2][1] = cosLat * sinLon;
        jacobian[2][2] = sinLat;
    }
}
