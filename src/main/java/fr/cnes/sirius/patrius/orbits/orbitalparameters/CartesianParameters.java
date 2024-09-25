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
 * @history creation 16/03/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au
 * lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3242:22/05/2023:[PATRIUS] Parametres circulaires pour orbites hyperboliques
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:658:29/07/2016:Correction in conversion keplerian orbit <=> cartesian orbit
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1798:10/12/2018 add getAlternateEquinoctialParameters
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits.orbitalparameters;

import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plane;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.EquatorialOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * This class holds cartesian orbital parameters.
 * 
 * <p>
 * The parameters used internally are the cartesian coordinates:
 * <ul>
 * <li>x</li>
 * <li>y</li>
 * <li>z</li>
 * <li>xDot</li>
 * <li>yDot</li>
 * <li>zDot</li>
 * </ul>
 * contained in {@link PVCoordinates}.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Emmanuel Bignon
 * @since 3.0
 * @version $Id: CartesianParameters.java 18071 2017-10-02 16:46:39Z bignon $
 */
public class CartesianParameters extends AbstractOrbitalParameters {

    /** Serializable UID. */
    private static final long serialVersionUID = -1963812828506172328L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Machine epsilon. */
    private static final double EPS_MACHINE = 1e-15;

    /** Root int for hash code. */
    private static final int ROOTINT = 353;

    /** PV coordinates (x, y, z, xDot, yDot, zDot) (m and m/s). */
    private final PVCoordinates pvCoordinates;

    /**
     * Constructor with PV coordinates.
     * 
     * @param pvCoordinatesIn
     *        position and velocity
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public CartesianParameters(final PVCoordinates pvCoordinatesIn, final double mu) {
        super(mu);
        this.pvCoordinates = pvCoordinatesIn;
    }

    /**
     * Constructor with position and velocity.
     * 
     * @param position
     *        position
     * @param velocity
     *        velocity
     * @param acceleration
     *        acceleration
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public CartesianParameters(final Vector3D position, final Vector3D velocity,
                               final Vector3D acceleration, final double mu) {
        super(mu);
        this.pvCoordinates = new PVCoordinates(position, velocity, acceleration);
    }

    /**
     * Get the PV coordinates.
     * 
     * @return pvCoordinates
     */
    public PVCoordinates getPVCoordinates() {
        return this.pvCoordinates;
    }

    /**
     * Get the position.
     * 
     * @return position
     */
    public Vector3D getPosition() {
        return this.pvCoordinates.getPosition();
    }

    /**
     * Get the velocity.
     * 
     * @return velocity
     */
    public Vector3D getVelocity() {
        return this.pvCoordinates.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public CartesianParameters getCartesianParameters() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianParameters getKeplerianParameters() {
        return this.getKeplerianParameters(this.getMu());
    }

    /**
     * Convert current orbital parameters into circular parameters.
     * 
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @return current orbital parameters converted into circular parameters
     * @see CircularParameters
     */
    public KeplerianParameters getKeplerianParameters(final double mu) {
        // compute inclination
        final Vector3D momentum = this.pvCoordinates.getMomentum();
        final double m2 = momentum.getNormSq();
        final double i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        Vector3D node = Vector3D.crossProduct(Vector3D.PLUS_K, momentum);

        // treat the case where node is null vector. In that case node line is undefined and is set
        // to [1, 0, 0]
        node = (node.getNorm() == 0.) ? Vector3D.PLUS_I : node;
        final double raan = MathLib.atan2(node.getY(), node.getX());

        // preliminary computations for parameters depending on orbit shape (elliptic or hyperbolic)
        final Vector3D pvP = this.pvCoordinates.getPosition();
        final Vector3D pvV = this.pvCoordinates.getVelocity();
        final double r = pvP.getNorm();
        final double v2 = pvV.getNormSq();
        final double rV2OnMu = r * v2 / mu;

        // compute semi-major axis (will be negative for hyperbolic orbits)
        final double a = r / (2 - rV2OnMu);
        final double muA = mu * a;

        // compute true anomaly
        final double e;
        final double v;
        if (a > 0) {
            // elliptic or circular orbit
            final double eSE = Vector3D.dotProduct(pvP, pvV) / MathLib.sqrt(muA);
            final double eCE = rV2OnMu - 1;
            e = MathLib.sqrt(eSE * eSE + eCE * eCE);
            final double eccAnom = MathLib.atan2(eSE, eCE);
            final double beta = e / (1 + MathLib.sqrt(MathLib.max(0.0, (1 - e) * (1 + e))));
            final double[] sincos = MathLib.sinAndCos(eccAnom);
            final double sin = sincos[0];
            final double cos = sincos[1];
            v = eccAnom + 2 * MathLib.atan(beta * sin / (1 - beta * cos));
        } else {
            // hyperbolic orbit
            final double eSH = Vector3D.dotProduct(pvP, pvV) / MathLib.sqrt(-muA);
            final double eCH = rV2OnMu - 1;
            e = MathLib.sqrt(MathLib.max(0.0, 1 - m2 / muA));
            final double eccAnom = MathLib.log((eCH + eSH) / (eCH - eSH)) / 2.;
            v = 2 * MathLib.atan(MathLib.sqrt(MathLib.max(0.0, (e + 1) / (e - 1)))
                    * MathLib.tanh(eccAnom / 2.));
        }

        // compute perigee argument
        final double px = Vector3D.dotProduct(pvP, node);
        final double py = Vector3D.dotProduct(pvP, Vector3D.crossProduct(momentum, node))
                / MathLib.sqrt(m2);
        final double pa = MathLib.atan2(py, px) - v;

        // Return results
        return new KeplerianParameters(a, e, i, pa, raan, v, PositionAngle.TRUE, mu);
    }

    /** {@inheritDoc} */
    @Override
    public CircularParameters getCircularParameters() {
        return this.getCircularParameters(this.getMu());
    }

    /**
     * Convert current orbital parameters into circular parameters.
     * 
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @return current orbital parameters converted into circular parameters
     * @see CircularParameters
     */
    public CircularParameters getCircularParameters(final double mu) {

        // compute semi-major axis
        final Vector3D pvP = this.pvCoordinates.getPosition();
        final Vector3D pvV = this.pvCoordinates.getVelocity();
        final double r = pvP.getNorm();
        final double v2 = pvV.getNormSq();
        final double rV2OnMu = r * v2 / mu;

        final double a = r / (2 - rV2OnMu);

        // compute inclination
        final Vector3D momentum = this.pvCoordinates.getMomentum();
        final double m2 = momentum.getNormSq();
        final double i = Vector3D.angle(momentum, Vector3D.PLUS_K);

        // compute right ascension of ascending node
        final Vector3D node = Vector3D.crossProduct(Vector3D.PLUS_K, momentum);
        final double raan = MathLib.atan2(node.getY(), node.getX());

        // 2D-coordinates in the canonical frame

        // raan
        final double[] sincosRaan = MathLib.sinAndCos(raan);
        final double sinRaan = sincosRaan[0];
        final double cosRaan = sincosRaan[1];
        // inclination
        final double[] sincosI = MathLib.sinAndCos(i);
        final double sinI = sincosI[0];
        final double cosI = sincosI[1];

        final double xP = pvP.getX();
        final double yP = pvP.getY();
        final double zP = pvP.getZ();
        final double x2 = (xP * cosRaan + yP * sinRaan) / a;
        final double y2 = ((yP * cosRaan - xP * sinRaan) * cosI + zP * sinI)
                / a;
        // creation of common variables
        final PositionAngle posAngl;
        final double alpha;
        final double ex;
        final double ey;
        if (a > 0) {
            // compute eccentricity vector for elliptical orbit
            final double eSE = Vector3D.dotProduct(pvP, pvV)
                    / MathLib.sqrt(mu * a);
            final double eCE = rV2OnMu - 1;
            final double e2 = eCE * eCE + eSE * eSE;
            final double f = eCE - e2;
            final double g = MathLib.sqrt(MathLib.max(0.0, 1 - e2)) * eSE;
            final double aOnR = a / r;
            final double a2OnR2 = aOnR * aOnR;
            ex = a2OnR2 * (f * x2 + g * y2);
            ey = a2OnR2 * (f * y2 - g * x2);
            // compute latitude argument
            final double beta = 1 / (1 + MathLib.sqrt(MathLib.max(0.0, 1 - ex
                    * ex - ey * ey)));
            alpha = MathLib.atan2(y2 + ey + eSE * beta * ex, x2 + ex - eSE
                    * beta * ey);
            posAngl = PositionAngle.ECCENTRIC;
        } else {
            // hyperbolic orbit
            final double muA = mu * a;
            final double eSH = Vector3D.dotProduct(pvP, pvV) / MathLib.sqrt(-muA);
            final double eCH = rV2OnMu - 1;
            final double e = MathLib.sqrt(MathLib.max(0.0, 1 - m2 / muA));
            final double eccAnom = MathLib.log((eCH + eSH) / (eCH - eSH)) / 2.;
            final double v = 2 * MathLib.atan(MathLib.sqrt(MathLib.max(0.0,
                (e + 1) / (e - 1))) * MathLib.tanh(eccAnom / 2.));
            // compute perigee argument
            final double px = Vector3D.dotProduct(pvP, node);
            final double py = Vector3D.dotProduct(pvP,
                Vector3D.crossProduct(momentum, node))
                    / MathLib.sqrt(m2);
            final double pa = MathLib.atan2(py, px) - v;
            // creation of circular parameters
            alpha = pa + v;
            ex = e * MathLib.cos(pa);
            ey = e * MathLib.sin(pa);

            posAngl = PositionAngle.TRUE;
        }
        // Return result
        return new CircularParameters(a, ex, ey, i, raan, alpha, posAngl, mu);
    }

    /** {@inheritDoc} */
    @Override
    public EquatorialParameters getEquatorialParameters() {
        return this.getEquatorialParameters(this.getMu());
    }

    /**
     * Convert current orbital parameters into equatorial parameters.
     * 
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @return current orbital parameters converted into equatorial parameters
     * @see EquatorialParameters
     */
    public EquatorialParameters getEquatorialParameters(final double mu) {

        // Momentum
        final Vector3D momentum = this.pvCoordinates.getMomentum();

        // compute inclination vector
        final Vector3D w = momentum.normalize();
        final double d = 1.0 / MathLib.sqrt(MathLib.max(0.0, (1 + w.getZ()) / 2.0));
        final double hx = -d * w.getY();
        final double hy = d * w.getX();
        final double ix = hx;
        final double iy = hy;

        final double squareNormI = ix * ix + iy * iy;
        if (squareNormI > 4) {
            // Exception
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.WRONG_INVERSE_TRIGONOMETRIC_FUNCTION_ARGUMENT);
        }

        // preliminary computations for parameters depending on orbit shape (elliptic)
        final Vector3D pvP = this.pvCoordinates.getPosition();
        final Vector3D pvV = this.pvCoordinates.getVelocity();
        final double r = pvP.getNorm();
        final double v2 = pvV.getNormSq();
        final double rV2OnMu = r * v2 / mu;

        // compute semi-major axis
        final double a = r / (2. - rV2OnMu);

        if (a < 0) {
            // Exception
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                EquatorialOrbit.class.getSimpleName());
        }

        // compute true anomaly (elliptic or circular orbit)
        final double eSE = Vector3D.dotProduct(pvP, pvV) / MathLib.sqrt(mu * a);
        final double eCE = rV2OnMu - 1;
        final double e = MathLib.sqrt(eSE * eSE + eCE * eCE);
        final double eccAnom = MathLib.atan2(eSE, eCE);

        final double rach2 = 0.5 * (1. + momentum.normalize().getZ());
        final double rach = MathLib.sqrt(MathLib.max(0.0, rach2));
        final Vector3D f = new Vector3D(1. - HALF * hy * hy, HALF * hx * hy, -rach * hy);
        final Vector3D g = new Vector3D(f.getY(), 1. - HALF * hx * hx, rach * hx);
        final double scalf = Vector3D.dotProduct(pvP, f);
        final double scalg = Vector3D.dotProduct(pvP, g);

        final double rmua = MathLib.sqrt(mu * a);
        final double ecose = r * v2 / mu - 1.;
        final double esine = Vector3D.dotProduct(pvP, pvV) / rmua;
        final double cose = ecose / e;
        final double sine = esine / e;
        final double race = MathLib.sqrt(MathLib.max(0.0, 1. - e * e));
        final double al1 = a * (cose - e);
        final double al2 = a * race * sine;
        final double r2coso = al1 * scalf + al2 * scalg;
        final double r2sino = al1 * scalg - al2 * scalf;

        // compute pomega (longitude of the periapsis)
        final double pomega = MathLib.atan2(r2sino, r2coso);

        // Return result
        //
        return new EquatorialParameters(a, e, pomega, ix, iy, eccAnom, PositionAngle.ECCENTRIC, mu);
    }

    /** {@inheritDoc} */
    @Override
    public EquinoctialParameters getEquinoctialParameters() {
        return this.getEquinoctialParameters(this.getMu());
    }

    /**
     * Convert current orbital parameters into equinoctial parameters.
     * 
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @return current orbital parameters converted into equinoctial parameters
     * @see EquinoctialParameters
     */
    public EquinoctialParameters getEquinoctialParameters(final double mu) {

        // compute semi-major axis
        final Vector3D pvP = this.pvCoordinates.getPosition();
        final Vector3D pvV = this.pvCoordinates.getVelocity();
        final double r = pvP.getNorm();
        final double v2 = pvV.getNormSq();
        final double rV2OnMu = r * v2 / mu;

        if (rV2OnMu > 2) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, this.getClass().getName());
        }

        // compute inclination vector
        final Vector3D w = this.pvCoordinates.getMomentum().normalize();
        final double d = 1.0 / (1 + w.getZ());
        double hx = -w.getY() / (1 + w.getZ());
        double hy = w.getX() / (1 + w.getZ());
        // Check for equatorial retrograde orbit
        if (((w.getX() * w.getX() + w.getY() * w.getY()) < Precision.EPSILON) && w.getZ() < 0) {
            hx = Double.NaN;
            hy = Double.NaN;
        }

        // compute true longitude argument
        final double cLv = (pvP.getX() - d * pvP.getZ() * w.getX()) / r;
        final double sLv = (pvP.getY() - d * pvP.getZ() * w.getY()) / r;
        final double lv = MathLib.atan2(sLv, cLv);

        // compute semi-major axis
        final double a = r / (2 - rV2OnMu);

        // compute eccentricity vector
        final double eSE = Vector3D.dotProduct(pvP, pvV) / MathLib.sqrt(mu * a);
        final double eCE = rV2OnMu - 1;
        final double e2 = eCE * eCE + eSE * eSE;
        final double f = eCE - e2;
        final double g = MathLib.sqrt(MathLib.max(0.0, 1 - e2)) * eSE;
        final double ex = a * (f * cLv + g * sLv) / r;
        final double ey = a * (f * sLv - g * cLv) / r;

        // Return result
        return new EquinoctialParameters(a, ex, ey, hx, hy, lv, PositionAngle.TRUE, mu);
    }

    /** {@inheritDoc} */
    @Override
    public ApsisAltitudeParameters getApsisAltitudeParameters(final double ae) {
        return this.getApsisAltitudeParameters(ae, this.getMu());
    }

    /**
     * Convert current orbital parameters into apsis (using altitude) parameters.
     * 
     * @param ae
     *        equatorial radius (m)
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @return current orbital parameters converted into apsis (using altitude) parameters
     * @see ApsisAltitudeParameters
     */
    public ApsisAltitudeParameters getApsisAltitudeParameters(final double ae, final double mu) {
        return this.getKeplerianParameters(mu).getApsisAltitudeParameters(ae);
    }

    /** {@inheritDoc} */
    @Override
    public ApsisRadiusParameters getApsisRadiusParameters() {
        return this.getApsisRadiusParameters(this.getMu());
    }

    /**
     * Convert current orbital parameters into apsis (using radius) parameters.
     * 
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @return current orbital parameters converted into apsis (using radius) parameters
     * @see ApsisRadiusParameters
     */
    public ApsisRadiusParameters getApsisRadiusParameters(final double mu) {
        return this.getKeplerianParameters(mu).getApsisRadiusParameters();
    }

    /** {@inheritDoc} */
    @Override
    public ReentryParameters getReentryParameters(final double ae, final double f) {
        return this.getReentryParameters(ae, f, this.getMu());
    }

    /**
     * Convert current orbital parameters into reentry parameters.
     * 
     * @param ae
     *        equatorial radius (m)
     * @param f
     *        flattening (f = (a-b)/a)
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @return current orbital parameters converted into reentry parameters
     * @see ReentryParameters
     */
    public ReentryParameters getReentryParameters(final double ae, final double f, final double mu) {

        try {
            // Get frame
            final CelestialBodyFrame frame = FramesFactory.getGCRF();

            // Build ellipsoid
            final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(ae, f, frame);

            // Get geodetic coordinates (altitude, latitude, longitude)
            // No date needs to be provided since frame is unchanged
            final EllipsoidPoint point = ellipsoid.buildPoint(getPosition(), frame, null, "");
            final double latitude = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
            final double longitude = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
            final double altitude = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();
            final TopocentricFrame topocentricFrame = new TopocentricFrame(point, "");
            final Vector3D east = topocentricFrame.getEast();
            final Vector3D zenith = topocentricFrame.getZenith();

            // Local horizontal plane (located in position and normal = zenith)
            final Plane horizontalPlane = new Plane(this.getPosition(), zenith);
            final Vector3D u = horizontalPlane.getU();
            final Vector3D v = horizontalPlane.getV();

            // Velocity
            final double velocity = this.getVelocity().getNorm();

            // Velocity projection on local horizontal plane
            final Vector3D vProj = new Vector3D(Vector3D.dotProduct(u, this.getVelocity()), u,
                Vector3D.dotProduct(v, this.getVelocity()), v);
            final double vProjNorm = vProj.getNorm();

            // Local north
            final Vector3D northProj = new Vector3D(Vector3D.dotProduct(u, Vector3D.PLUS_K), u,
                Vector3D.dotProduct(v, Vector3D.PLUS_K), v);
            final double northProjNorm = northProj.getNorm();

            // Slope of velocity
            final double slopeSign = Vector3D.dotProduct(zenith, this.getVelocity());
            double slope = 0;
            if (vProjNorm <= ReentryParameters.EPS) {
                // Specific case: velocity is aligned with zenith direction
                if (slopeSign >= 0) {
                    // Ascent phase
                    slope = FastMath.PI / 2.;
                } else {
                    // Descent phase
                    slope = -FastMath.PI / 2.;
                }
            } else {
                // Normal case (slope positive on ascent phase)
                slope = Vector3D.angle(vProj, this.getVelocity());
                if (slopeSign < 0) {
                    slope = -slope;
                }
            }

            // Azimuth of velocity
            double azimuth = 0;
            if (vProjNorm <= ReentryParameters.EPS || northProjNorm <= ReentryParameters.EPS) {
                // Specific case: local north is undefined
                azimuth = 0;
            } else {
                // Normal case (azimuth positive toward east)
                azimuth = Vector3D.angle(northProj, vProj);
                if (Vector3D.dotProduct(east, vProj) < 0) {
                    azimuth = 2. * FastMath.PI - azimuth;
                }
            }

            // Return result
            return new ReentryParameters(altitude, latitude, longitude, velocity, slope, azimuth,
                ae, f, mu);

        } catch (final PatriusException e) {
            // It cannot happen as OrekitException cannot be thrown
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public StelaEquinoctialParameters getStelaEquinoctialParameters() {
        return this.getStelaEquinoctialParameters(this.getMu());
    }

    /**
     * Convert current orbital parameters into Stela equinoctial parameters.
     * 
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @return current orbital parameters converted into Stela equinoctial parameters
     * @see StelaEquinoctialParameters
     */
    public StelaEquinoctialParameters getStelaEquinoctialParameters(final double mu) {

        // compute semi-major axis
        final Vector3D pvP = this.pvCoordinates.getPosition();
        final Vector3D pvV = this.pvCoordinates.getVelocity();
        final double r = pvP.getNorm();
        final double v2 = pvV.getNormSq();
        final double rV2OnMu = r * v2 / mu;
        if (rV2OnMu > 2) {
            // Exception
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, this.getClass().getName());
        }
        final double a = r / (2 - rV2OnMu);

        // compute inclination vector
        final Vector3D w = this.pvCoordinates.getMomentum().normalize();
        final double d = 1.0 / (1 + w.getZ());
        final double hx = -d * w.getY();
        final double hy = d * w.getX();
        final double i = 2 * MathLib.atan(MathLib.sqrt(hx * hx + hy * hy));
        final double cosI2 = MathLib.cos(i / 2.);

        double ix = cosI2 * hx;
        double iy = cosI2 * hy;
        if (i < EPS_MACHINE) {
            // Consider 0 in that case
            ix = 0;
            iy = 0;
        }

        // compute true longitude argument (for ex, ey, lM)
        final double cLv = (pvP.getX() - d * pvP.getZ() * w.getX()) / r;
        final double sLv = (pvP.getY() - d * pvP.getZ() * w.getY()) / r;
        final double lv = MathLib.atan2(sLv, cLv);

        // compute eccentricity vector
        final double eSE = Vector3D.dotProduct(pvP, pvV) / MathLib.sqrt(mu * a);
        final double eCE = rV2OnMu - 1;
        final double e2 = eCE * eCE + eSE * eSE;
        final double f = eCE - e2;
        final double g = MathLib.sqrt(MathLib.max(0.0, 1 - e2)) * eSE;
        final double ex = a * (f * cLv + g * sLv) / r;
        final double ey = a * (f * sLv - g * cLv) / r;

        // le
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, 1 - ex * ex - ey * ey));
        final double[] sincosLv = MathLib.sinAndCos(lv);
        final double sinLv = sincosLv[0];
        final double cosLv = sincosLv[1];
        final double num = ey * cosLv - ex * sinLv;
        final double den = epsilon + 1 + ex * cosLv + ey * sinLv;
        final double lE = lv + 2 * MathLib.atan(num / den);
        // lM
        final double[] sincosLE = MathLib.sinAndCos(lE);
        final double sinLE = sincosLE[0];
        final double cosLE = sincosLE[1];
        final double lM = lE - ex * sinLE + ey * cosLE;

        // Return result
        return new StelaEquinoctialParameters(a, ex, ey, ix, iy, lM, mu, true);
    }

    /** {@inheritDoc} */
    @Override
    public AlternateEquinoctialParameters getAlternateEquinoctialParameters() {
        return this.getEquinoctialParameters().getAlternateEquinoctialParameters();
    }

    /**
     * Returns a string representation of this Orbit object.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "cartesian parameters: " + this.pvCoordinates.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        // parameters : date, frame, type, mu,
        // x, y, z, xDot, yDot, zDot
        boolean isEqual = true;

        if (object == this) {
            isEqual = true;
        } else if (object instanceof CartesianParameters) {
            final CartesianParameters other = (CartesianParameters) object;

            isEqual &= (this.getMu() == other.getMu());

            // Cartesian parameters
            isEqual &= (getPVCoordinates().equals(other.getPVCoordinates()));

        } else {
            isEqual = false;
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {

        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" orbits, but
        // reasonably sure it's different otherwise.
        result = effMult * result + MathUtils.hash(this.getMu());
        result = effMult * result + getPVCoordinates().hashCode();

        return result;
    }
}
