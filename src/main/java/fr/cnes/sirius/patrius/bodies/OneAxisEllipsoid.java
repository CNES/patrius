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
* VERSION:4.8:FA:FA-2959:15/11/2021:[PATRIUS] Levee d'exception NullPointerException lors du calcul d'intersection a altitude
* VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:273:20/10/2014:Minor code problems
 * VERSION::FA:531:10/02/2016:Robustification of convergence of transform() method
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.oned.Vector1D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Modeling of a one-axis ellipsoid.
 * 
 * <p>
 * One-axis ellipsoids is a good approximate model for most planet-size and larger natural bodies. It is the equilibrium
 * shape reached by a fluid body under its own gravity field when it rotates. The symmetry axis is the rotation or polar
 * axis.
 * </p>
 * 
 * <p>
 * This class is a simple adaptation of the <a
 * href="http://www.spaceroots.org/documents/distance/Ellipsoid.java">Ellipsoid</a> example class implementing the
 * algorithms described in the paper <a href="http://www.spaceroots.org/documents/distance/distance-to-ellipse.pdf">
 * Quick computation of the distance between a point and an ellipse</a>.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class OneAxisEllipsoid implements BodyShape {

    /** Serializable UID. */
    private static final long serialVersionUID = -1418386024561514172L;

    /** One third. */
    private static final double ONE_THIRD = 1.0 / 3.0;
    
    /** -4 */
    private static final double C_N4 = -4.0;
    
    /** 9.0 */
    private static final double C_9 = 9.0;

    /** Default close approach threshold. */
    private static final double DEFAULT_CLOSE_APPROACH_THRESHOLD = 1.0e-10;

    /** Default angular threshold. */
    private static final double DEFAULT_ANGULAR_THRESHOLD = 1.0e-14;

    /** Default 2nd convergence threshold. */
    private static final double DEFAULT_2ND_CONVERGENCE_THRESHOLD = 1.0e-14;

    /** Body frame related to body shape. */
    private final Frame bodyFrame;

    /** Equatorial radius. */
    private final double ae;

    /** Eccentricity power 2. */
    private final double e2;

    /** 1 minus flatness. */
    private final double g;

    /** g * g. */
    private final double g2;

    /** Equatorial radius power 2. */
    private final double ae2;

    /** Convergence limit. */
    private double closeApproachThreshold;

    /** Convergence limit. */
    private double angularThreshold;

    /** 2nd convergence limit if first convergence failed. */
    private double threshold2;

    /**
     * Simple constructor.
     * <p>
     * The following table provides conventional parameters for global Earth models:
     * </p>
     * <table border="1" cellpadding="5">
     * <tr bgcolor="#ccccff">
     * <th>model</th>
     * <th>a<sub>e</sub> (m)</th>
     * <th>f</th>
     * </tr>
     * <tr>
     * <td bgcolor="#eeeeff">GRS 80</td>
     * <td>6378137.0</td>
     * <td>1.0 / 298.257222101</td>
     * </tr>
     * <tr>
     * <td bgcolor="#eeeeff">WGS84</td>
     * <td>6378137.0</td>
     * <td>1.0 / 298.257223563</td>
     * </tr>
     * </table>
     * 
     * @param aeIn
     *        equatorial radius
     * @param f
     *        the flattening (f = (a-b)/a)
     * @param bodyFrameIn
     *        body frame related to body shape
     * @see fr.cnes.sirius.patrius.frames.FramesFactory#getITRF()
     */
    public OneAxisEllipsoid(final double aeIn, final double f, final Frame bodyFrameIn) {
        this.ae = aeIn;
        this.e2 = f * (2.0 - f);
        this.g = 1.0 - f;
        this.g2 = this.g * this.g;
        this.ae2 = aeIn * aeIn;
        this.setCloseApproachThreshold(DEFAULT_CLOSE_APPROACH_THRESHOLD);
        this.setAngularThreshold(DEFAULT_ANGULAR_THRESHOLD);
        this.set2ndConvergenceThreshold(DEFAULT_2ND_CONVERGENCE_THRESHOLD);
        this.bodyFrame = bodyFrameIn;
    }

    /**
     * Set the close approach threshold.
     * <p>
     * The close approach threshold is a ratio used to identify special cases in the
     * {@link #transform(Vector3D, Frame, AbsoluteDate)} method.
     * </p>
     * <p>
     * Let d = (x<sup>2</sup>+y<sup>2</sup>+z<sup>2</sup>)<sup>&frac12;</sup> be the distance between the point and the
     * ellipsoid center.
     * </p>
     * <ul>
     * <li>all points such that d&lt;&epsilon; a<sub>e</sub> where a<sub>e</sub> is the equatorial radius of the
     * ellipsoid are considered at the center</li>
     * <li>all points closer to the surface of the ellipsoid than &epsilon; d are considered on the surface</li>
     * </ul>
     * <p>
     * If this method is not called, the default value is set to 10<sup>-10</sup>.
     * </p>
     * 
     * @param closeApproachThresholdIn
     *        close approach threshold (no unit)
     */
    public void setCloseApproachThreshold(final double closeApproachThresholdIn) {
        this.closeApproachThreshold = closeApproachThresholdIn;
    }

    /**
     * Set the angular convergence threshold.
     * <p>
     * The angular threshold is the convergence threshold used to stop the iterations in the
     * {@link #transform(Vector3D, Frame, AbsoluteDate)} method. It applies directly to the latitude. When convergence
     * is reached, the real latitude is guaranteed to be between &phi; - &delta;&phi;/2 and &phi; + &delta;&phi;/2 where
     * &phi; is the computed latitude and &delta;&phi; is the angular threshold set by this method.
     * </p>
     * <p>
     * If this method is not called, the default value is set to 10<sup>-14</sup>.
     * </p>
     * 
     * @param angularThresholdIn
     *        angular convergence threshold (rad)
     */
    public void setAngularThreshold(final double angularThresholdIn) {
        this.angularThreshold = angularThresholdIn;
    }

    /**
     * Set the 2nd convergence threshold.
     * <p>
     * The threshold is the 2nd convergence threshold used to stop the iterations in the
     * {@link #transform(Vector3D, Frame, AbsoluteDate)} method. This convergence threshold is used only if usual
     * convergence has not been reached under 1st threshold (very rare case). 1st threshold can be set using
     * {@link #setAngularThreshold(double)}.
     * <p>
     * Non-convergence may be resulting from numerical quality issues, found solution being very close to real solution
     * but slightly above first threshold. As a result a second convergence criterion is used. This method sets the
     * threshold of the second convergence criterion.
     * </p>
     * <p>
     * Criterion is based on evaluation of a 3rd order poynomial <i>P</i> whose solution if one of the roots: algorithm
     * is stopped if P(solution) < threshold.
     * </p>
     * <p>
     * If this method is not called, the default value is set to 10<sup>-14</sup>.
     * </p>
     * 
     * @param convergenceThreshold
     *        convergence threshold (rad)
     * @see #setAngularThreshold(double)
     */
    public void set2ndConvergenceThreshold(final double convergenceThreshold) {
        this.threshold2 = convergenceThreshold;
    }

    /**
     * Get the equatorial radius of the body.
     * 
     * @return equatorial radius of the body (m)
     */
    public double getEquatorialRadius() {
        return this.ae;
    }

    /**
     * Get the body frame related to body shape.
     * 
     * @return body frame related to body shape
     */
    @Override
    public Frame getBodyFrame() {
        return this.bodyFrame;
    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint getIntersectionPoint(final Line line, final Vector3D close,
                                              final Frame frame, final AbsoluteDate date) throws PatriusException {

        // transform line and close to body frame
        final Transform frameToBodyFrame = frame.getTransformTo(this.bodyFrame, date);
        final Line lineInBodyFrame = frameToBodyFrame.transformLine(line);

        // compute some miscellaneous variables outside of the loop
        final Vector3D point = lineInBodyFrame.getOrigin();
        final double x = point.getX();
        final double y = point.getY();
        final double z = point.getZ();
        final double z2 = z * z;
        final double r2 = x * x + y * y;

        final Vector3D direction = lineInBodyFrame.getDirection();
        final double dx = direction.getX();
        final double dy = direction.getY();
        final double dz = direction.getZ();
        final double cz2 = dx * dx + dy * dy;

        // abscissa of the intersection as a root of a 2nd degree polynomial :
        // a k^2 - 2 b k + c = 0
        final double a = 1.0 - this.e2 * cz2;
        final double b = -(this.g2 * (x * dx + y * dy) + z * dz);
        final double c = this.g2 * (r2 - this.ae2) + z2;
        final double b2 = b * b;
        final double ac = a * c;
        if (b2 < ac) {
            // Specific case
            return null;
        }
        final double s = MathLib.sqrt(MathLib.max(0.0, b2 - ac));
        final double k1 = (b < 0) ? (b - s) / a : c / (b + s);
        final double k2 = c / (a * k1);

        // select the right point
        final Vector3D closeInBodyFrame = frameToBodyFrame.transformPosition(close);
        final double closeAbscissa = lineInBodyFrame.toSubSpace(closeInBodyFrame).getX();
        final double k =
            (MathLib.abs(k1 - closeAbscissa) < MathLib.abs(k2 - closeAbscissa)) ? k1 : k2;
        final Vector3D intersection = lineInBodyFrame.toSpace(new Vector1D(k));
        final double ix = intersection.getX();
        final double iy = intersection.getY();
        final double iz = intersection.getZ();

        // Compute latitude and longitude
        final double lambda = MathLib.atan2(iy, ix);
        final double phi = MathLib.atan2(iz, this.g2 * MathLib.sqrt(ix * ix + iy * iy));

        // Build result
        return new GeodeticPoint(phi, lambda, 0.0);

    }

    /** {@inheritDoc} */
    @Override
    public GeodeticPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                              final AbsoluteDate date, final double altitude) throws PatriusException {
        if (MathLib.abs(altitude) < EPS_ALTITUDE) {
            // Altitude is considered to be 0
            return getIntersectionPoint(line, close, frame, date);
        } else {
            // Build ellipsoid of required altitude
            final double aAlt = ae + altitude;
            final double fAlt = 1. - (ae * g + altitude) / aAlt;
            final OneAxisEllipsoid ellipsoidAlt = new OneAxisEllipsoid(aAlt, fAlt, getBodyFrame());
            // Compute and return intersection point
            final GeodeticPoint pointInAltitudeEllipsoid = ellipsoidAlt.getIntersectionPoint(line, close, frame, date);
            GeodeticPoint res = null;
            if (pointInAltitudeEllipsoid != null) {
                final Vector3D pointInBodyFrame = ellipsoidAlt.transform(pointInAltitudeEllipsoid);
                res = transform(pointInBodyFrame, bodyFrame, date);
            }
            return res;
        }
    }

    /**
     * Transform a surface-relative point to a cartesian point.
     * 
     * @param point
     *        surface-relative point
     * @return point at the same location but as a cartesian point
     */
    @Override
    public Vector3D transform(final GeodeticPoint point) {
        final double[] sincosLon = MathLib.sinAndCos(point.getLongitude());
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];
        final double[] sincosLat = MathLib.sinAndCos(point.getLatitude());
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];
        final double h = point.getAltitude();
        final double n = this.ae / MathLib.sqrt(MathLib.max(0.0, 1.0 - this.e2 * sinLat * sinLat));
        final double r = (n + h) * cosLat;
        return new Vector3D(r * cosLon, r * sinLon, (this.g2 * n + h) * sinLat);
    }

    /**
     * Transform a surface-relative point to a cartesian point and compute the jacobian of
     * the transformation.
     * 
     * @param geodeticPoint
     *        geodetic point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * 
     * @return point at the same location but as a cartesian point
     * @throws PatriusException
     *         e
     */
    public Vector3D transformAndComputeJacobian(final GeodeticPoint geodeticPoint,
                                                final double[][] jacobian) throws PatriusException {
        final Vector3D transformedPoint = this.transform(geodeticPoint);
        this.computeJacobian(transformedPoint, geodeticPoint, jacobian);
        return transformedPoint;
    }

    /**
     * Transform a cartesian point to a surface-relative point.
     * 
     * @param point
     *        cartesian point
     * @param frame
     *        frame in which cartesian point is expressed
     * @param date
     *        date of the point in given frame
     * @return point at the same location but as a surface-relative point,
     *         expressed in body frame
     * @exception PatriusException
     *            if point cannot be converted to body frame
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop ReturnCount check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Orekit code kept as such
    @Override
    public GeodeticPoint transform(final Vector3D point, final Frame frame,
                                   final AbsoluteDate date) throws PatriusException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume ReturnCount check
        // CHECKSTYLE: resume CyclomaticComplexity check

        // transform line to body frame
        final Vector3D pointInBodyFrame = frame.getTransformTo(this.bodyFrame, date).transformPosition(point);

        // compute some miscellaneous variables outside of the loop
        final double z = pointInBodyFrame.getZ();
        final double r2 = pointInBodyFrame.getX() * pointInBodyFrame.getX() +
            pointInBodyFrame.getY() * pointInBodyFrame.getY();
        final double dist = MathLib.sqrt(r2 + z * z);

        // point at the center
        if (dist < (this.closeApproachThreshold * this.ae)) {
            return new GeodeticPoint(FastMath.PI / 2., 0.0, -this.ae * MathLib.sqrt(1.0 - this.e2));
        }

        final double r = MathLib.sqrt(r2);
        final double cz = r / dist;
        final double sz = z / dist;

        // distance to the ellipse along the current line
        // as the smallest root of a 2nd degree polynom :
        // a k^2 - 2 b k + c = 0
        final double g2r2ma2 = this.g2 * (r2 - this.ae2);
        final double g2r2ma2pz2 = g2r2ma2 + z * z;
        double a = 1.0 - this.e2 * cz * cz;
        double b = this.g2 * r * cz + z * sz;
        double c = g2r2ma2pz2;
        final double ac = a * c;
        double k = c / (b + MathLib.sqrt(b * b - ac));

        final double lambda = MathLib.atan2(pointInBodyFrame.getY(), pointInBodyFrame.getX());
        double phi = MathLib.atan2(z - k * sz, this.g2 * (r - k * cz));

        // point on the ellipse
        if (MathLib.abs(k) < (this.closeApproachThreshold * dist)) {
            return new GeodeticPoint(phi, lambda, k);
        }

        final boolean inside = g2r2ma2pz2 <= 0;
        double t = z / (dist + r);

        // Initialization
        double dPhi = 0;
        double tauVal = 0;

        final double nMax = 100;
        for (int iterations = 0; iterations < nMax; ++iterations) {
            // 4th degree normalized polynom describing
            // circle/ellipse intersections
            // tau^4 + b tau^3 + c tau^2 + d tau + e = 0
            // (there is no need to compute e here)
            a = g2r2ma2pz2 + this.g2 * (2.0 * r + k) * k;
            b = C_N4 * k * z / a;
            c = 2.0 * (g2r2ma2pz2 + (1.0 + this.e2) * k * k) / a;
            double d = b;

            // reduce the polynom to degree 3 by removing
            // the already known real root t
            // tau^3 + b tau^2 + c tau + d = 0
            b += t;
            c += t * b;
            d += t * c;

            // find the other real root
            final double b2 = b * b;
            double q = (3.0 * c - b2) / C_9;
            final double p3 = 3.0 * 3.0 * 3.0;
            final double p32 = p3 * 2.0;
            final double r3 = (b * (9.0 * c - 2.0 * b2) - p3 * d) / p32;
            final double d2 = q * q * q + r3 * r3;
            double tildeT;
            double tildePhi;
            if (d2 >= 0) {
                final double rootD = MathLib.sqrt(d2);
                tildeT = MathLib.cbrt(r3 + rootD) + MathLib.cbrt(r3 - rootD) - b * ONE_THIRD;
                final double tildeT2 = tildeT * tildeT;
                final double tildeT2P1 = 1.0 + tildeT2;
                tildePhi = MathLib.atan2(z * tildeT2P1 - 2 * k * tildeT,
                    this.g2 * (r * tildeT2P1 - k * (1.0 - tildeT2)));

            } else {
                q = -q;
                final double qRoot = MathLib.sqrt(q);
                final double theta = MathLib.acos(MathLib.min(1.0, MathLib.max(-1.0, r3 / (q * qRoot))));

                // first root based on theta / 3,
                tildeT = 2.0 * qRoot * MathLib.cos(theta * ONE_THIRD) - b * ONE_THIRD;
                double tildeT2 = tildeT * tildeT;
                double tildeT2P1 = 1.0 + tildeT2;

                tildePhi =
                    MathLib.atan2(z * tildeT2P1 - 2 * k * tildeT, this.g2 * (r * tildeT2P1 - k * (1.0 - tildeT2)));
                if ((tildePhi * phi) < 0) {
                    // the first root was on the wrong hemisphere,
                    // try the second root based on (theta + 2PI) / 3
                    tildeT = 2.0 * qRoot * MathLib.cos((theta + MathUtils.TWO_PI) * ONE_THIRD) - b * ONE_THIRD;

                    tildeT2 = tildeT * tildeT;
                    tildeT2P1 = 1.0 + tildeT2;
                    tildePhi = MathLib.atan2(z * tildeT2P1 - 2 * k * tildeT,
                        this.g2 * (r * tildeT2P1 - k * (1.0 - tildeT2)));

                    if (tildePhi * phi < 0) {
                        // the second root was on the wrong hemisphere,
                        // try the third (and last) root based on (theta + 4PI) / 3
                        tildeT = 2.0 * qRoot * MathLib.cos((theta + 4.0 * FastMath.PI) * ONE_THIRD) - b * ONE_THIRD;
                        tildeT2 = tildeT * tildeT;
                        tildeT2P1 = 1.0 + tildeT2;
                        tildePhi = MathLib.atan2(z * tildeT2P1 - 2 * k * tildeT,
                            this.g2 * (r * tildeT2P1 - k * (1.0 - tildeT2)));
                    }
                }
            }

            // Polynomial function : tau^3 + b tau^2 + c tau + d
            tauVal = t * (t * (t + b) + c) + d;

            // midpoint on the ellipse
            dPhi = MathLib.abs((tildePhi - phi) / 2.);
            phi = (phi + tildePhi) / 2.;
            final double cPhi = MathLib.cos(phi);
            final double sPhi = MathLib.sin(phi);
            final double coeff = MathLib.sqrt(MathLib.max(0.0, 1.0 - this.e2 * sPhi * sPhi));

            if (dPhi < this.angularThreshold) {
                // angular convergence
                return new GeodeticPoint(phi, lambda, r * cPhi + z * sPhi - this.ae * coeff);
            }

            if ((iterations >= nMax - 1) && (MathLib.abs(tauVal) < this.threshold2)) {
                // tau_val zero reached
                return new GeodeticPoint(phi, lambda, r * cPhi + z * sPhi - this.ae * coeff);
            }

            b = this.ae / coeff;
            final double dR = r - cPhi * b;
            final double dZ = z - sPhi * b * this.g2;
            k = MathLib.sqrt(dR * dR + dZ * dZ);
            if (inside) {
                k = -k;
            }
            t = dZ / (k + dR);
        }

        // Unable to converge
        throw new PatriusExceptionWrapper(new PatriusException(PatriusMessages.GEOD_CONVERGENCE_FAILED,
            String.valueOf(this.angularThreshold), String.valueOf(dPhi), String.valueOf(this.threshold2),
            String.valueOf(tauVal)));
    }

    /**
     * Transform a cartesian point to a surface-relative point and compute the jacobian of
     * the transformation.
     * 
     * @param point
     *        cartesian point
     * @param frame
     *        frame in which cartesian point is expressed
     * @param date
     *        date of the point in given frame
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * 
     * @return point at the same location but as a surface-relative point,
     *         expressed in body frame
     * 
     * @exception PatriusException
     *            if point cannot be converted to body frame
     */
    public GeodeticPoint
            transformAndComputeJacobian(final Vector3D point, final Frame frame,
                                        final AbsoluteDate date, final double[][] jacobian) throws PatriusException {
        final GeodeticPoint transformedPoint = this.transform(point, frame, date);
        this.computeJacobian(transformedPoint, point, jacobian);
        return transformedPoint;
    }

    /**
     * Compute the jacobian matrix of the transformation from Cartesian point to geodetic point.
     * 
     * See "Algorithmes des routines du th&egraveme "changement de variables et de rep&egravere pour
     * la trajectographie" de la MSLIB (edition 4)"
     * 
     * @param geodeticPoint
     *        geodetic point
     * @param cartesianPoint
     *        cartesian point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * @throws PatriusException
     *         e
     * 
     */
    private void computeJacobian(final GeodeticPoint geodeticPoint, final Vector3D cartesianPoint,
                                 final double[][] jacobian) throws PatriusException {

        // Cartesian coordinates
        final double x = cartesianPoint.getX();
        final double y = cartesianPoint.getY();
        final double dist = MathLib.sqrt(x * x + y * y);

        // case : the point is close to the poles
        if (dist < (this.closeApproachThreshold * this.ae)) {
            // the point is close to one of the poles, the jacobian matrix cannot be computed
            throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
        }
        final double sinLat = MathLib.sin(geodeticPoint.getLatitude());
        final double r = this.ae / MathLib.sqrt(1 - (1 - this.g2) * sinLat * sinLat);
        final double k = this.g2 / (1 - (1 - this.g2) * sinLat * sinLat);

        // the altitude of the point is "too negative"
        if ((k * r + geodeticPoint.getAltitude()) < (this.closeApproachThreshold * this.ae)) {
            throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
        }

        // Temporary variables
        final double[] sincos = MathLib.sinAndCos(geodeticPoint.getLongitude());
        final double sinLon = sincos[0];
        final double cosLon = sincos[1];

        final double alt = geodeticPoint.getAltitude();
        final double cosLat = MathLib.cos(geodeticPoint.getLatitude());

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

    /**
     * Compute the jacobian matrix of the transformation from geodetic point to Cartesian point.
     * 
     * See "Algorithmes des routines du th&egraveme "changement de variables et de rep&egravere pour
     * la trajectographie" de la MSLIB (edition 4)"
     * 
     * @param cartesianPoint
     *        cartesian point
     * @param geodeticPoint
     *        geodetic point
     * @param jacobian
     *        the jacobian matrix which will be computed (input and output of the method)
     * @throws PatriusException
     *         e
     * 
     */
    private void computeJacobian(final Vector3D cartesianPoint, final GeodeticPoint geodeticPoint,
                                 final double[][] jacobian) throws PatriusException {

        // error if the flatness is larger than 1 or equal to 1
        if (this.g <= 0) {
            throw new PatriusException(PatriusMessages.JACOBIAN_UNDEFINED);
        }

        // Temporary variables
        final double[] sincosLon = MathLib.sinAndCos(geodeticPoint.getLongitude());
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];
        final double[] sincosLat = MathLib.sinAndCos(geodeticPoint.getLatitude());
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];

        final double alt = geodeticPoint.getAltitude();
        // local radius
        final double r = this.ae / MathLib.sqrt(MathLib.max(0.0, 1 - (1 - this.g2) * sinLat * sinLat));
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
}
