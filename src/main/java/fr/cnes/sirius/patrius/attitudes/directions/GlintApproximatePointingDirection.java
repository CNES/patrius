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
 * @history creation 15/10/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:457:09/11/2015:Glint direction
 * VERSION::FA:559:26/02/2016:minor corrections
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1415:23/03/2018: add getter Glint position
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.solver.BisectionSolver;
import fr.cnes.sirius.patrius.math.analysis.solver.UnivariateSolver;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.twod.Vector2D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * "Glint" direction pointing. It provides methods to compute Glint point G coordinates and to create a vector/line
 * between a point and G.
 * </p>
 * 
 * <p>
 * Glint point is the point of Sun reflexion on a body shape (the Earth for instance) as seen from a spacecraft.
 * </p>
 * 
 * <p>
 * Light speed is currently never taken into account.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment Not thread-safe by default. No use case for sharing an instance between
 *                      threads found.
 * 
 * @author rodriguest
 * 
 * @version $Id: GlintApproximatePointingDirection.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 3.1
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public final class GlintApproximatePointingDirection implements IDirection {

     /** Serializable UID. */
    private static final long serialVersionUID = -4313140126777994824L;

    /** Maximum number of iterations for solver */
    private static final int MAX_ITERATIONS = 200;

    /** Body shape. */
    private final EllipsoidBodyShape bodyShape;

    /** Sun PV coordinates. */
    private final PVCoordinatesProvider sunPVCoordinates;

    /** Solver used to find Glint direction. */
    private final UnivariateSolver solver;

    /**
     * Cache mecanism - the current coordinates of the origin point of the direction
     */
    private PVCoordinatesProvider cachedOrigin;

    /** Cache mecanism - the date */
    private AbsoluteDate cachedDate;

    /** Cache mecanism - the frame */
    private Frame cachedFrame;

    /** Cache mecanism - glint point position */
    private Vector3D cachedGlintPoint;

    /** Cache mecanism - line */
    private Line cachedLine;

    /**
     * Constructor.
     * 
     * @param shape the body shape
     * @param sunPV the Sun PV coordinates
     * @param univariateSolver the solver used to find Glint direction
     */
    public GlintApproximatePointingDirection(final EllipsoidBodyShape shape,
            final PVCoordinatesProvider sunPV,
            final UnivariateSolver univariateSolver) {
        this.bodyShape = shape;
        this.sunPVCoordinates = sunPV;
        this.solver = univariateSolver;
        this.cachedOrigin = null;
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedFrame = null;
        this.cachedGlintPoint = Vector3D.ZERO;
        this.cachedLine = null;
    }

    /**
     * Constructor with default solver (
     * {@link fr.cnes.sirius.patrius.math.analysis.solver.BisectionSolver#BisectionSolver}).
     * 
     * @param shape the body shape
     * @param sunPV the Sun PV coordinates
     */
    public GlintApproximatePointingDirection(final EllipsoidBodyShape shape,
                                             final PVCoordinatesProvider sunPV) {
        this(shape, sunPV, new BisectionSolver());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVector(final PVCoordinatesProvider origin, final AbsoluteDate date,
                              final Frame frame) throws PatriusException {
        return this.getLine(origin, date, frame).getDirection();
    }

    /** {@inheritDoc} */
    @Override
    public Line getLine(final PVCoordinatesProvider origin, final AbsoluteDate date,
                        final Frame frame) throws PatriusException {
        this.computeLine(origin, date, frame);
        return this.cachedLine;
    }

    /**
     * Compute Glint position.
     * 
     * @param satPos spacecraft position in body frame
     * @param sunPos Sun position in body frame
     * @param date date
     * @return Glint position in body frame
     * @throws PatriusException thrown if computation failed
     */
    @SuppressWarnings("PMD.AvoidProtectedMethodInFinalClassNotExtending")
    // Reason: unit test of method
        protected
        Vector3D
            getGlintPosition(final Vector3D satPos, final Vector3D sunPos, final AbsoluteDate date)
                throws PatriusException {

        // To compute eclipse detection :
        // Earth radius
        // Light speed not taken into account
        final PVCoordinatesProvider pvProvider = new ConstantPVCoordinatesProvider(satPos,
            this.bodyShape.getBodyFrame());
        final double occultingRadius = this.bodyShape.getApparentRadius(pvProvider,
            date, this.sunPVCoordinates, PropagationDelayType.INSTANTANEOUS);
        // Apparent radius
        final double value1 = MathLib.divide(occultingRadius, satPos.getNorm());
        final double alphaEarth = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value1)));
        // Sat to sun vector
        final Vector3D satSunVector = sunPos.subtract(satPos);
        // Sun apparent radius
        final double value2 = MathLib.divide(Constants.SUN_RADIUS, satSunVector.getNorm());
        final double alphaSun = MathLib.asin(MathLib.min(1.0, value2));
        // Sun - Sat - Earth angle
        final double sunEarthAngle = Vector3D.angle(satSunVector, satPos.negate());

        if (sunEarthAngle - alphaEarth + alphaSun <= 0.0) {
            // We are in an eclipse
            throw new PatriusException(PatriusMessages.UNDEFINED_DIRECTION, "GLINT", "ECLIPSE");
        }

        // Else, we are out of an eclipse : Compute the glint position - velocity

        // Constants to compute ellipse resulting from a plan intersecting the earth shape
        final double a = this.bodyShape.getEquatorialRadius();
        final double f = this.bodyShape.getFlattening();

        // Compute the Flattening of the ellipse defined by the
        // intersection of earth shape and plan (P) containing M, S and
        // earth center O
        final double fe;

        // Compute the normal to (P)
        final Vector3D np;

        // If earth center, satellite and sun are aligned, (P) is considered as vertical plane
        // containing Z-earth
        if (satPos.crossProduct(sunPos).getNormSq() == 0.0) {
            np = sunPos.crossProduct(Vector3D.PLUS_K).normalize();
            fe = f;
        } else {
            // else normal to plane is cross product of earth - vehicule vector with earth - sun
            // vector
            np = satPos.crossProduct(sunPos).normalize();
            final double value = -np.dotProduct(Vector3D.PLUS_K);
            final double beta = MathLib.acos(MathLib.min(1.0, MathLib.max(-1.0, value)));
            final double t = MathLib.atan(MathLib.tan(beta) / (1.0 - f));
            final double be = a
                    * MathLib.sqrt(MathLib.pow(MathLib.cos(t), 2) + MathLib.pow(1.0 - f, 2)
                            * MathLib.pow(MathLib.sin(t), 2));
            fe = 1.0 - MathLib.divide(be, a);
        }

        // Compute axes xp and yp of frame (O,xp,yp) associated to (P)
        Vector3D xp = np.crossProduct(Vector3D.PLUS_K);
        final Vector3D yp;

        if (xp.getNormSq() == 0.0) {
            xp = Vector3D.PLUS_I;
            yp = Vector3D.PLUS_J;
        } else {
            xp = xp.normalize();
            yp = xp.crossProduct(np);
        }

        // Compute coordinates of M,S in frame (O,xp,yp)
        final double xs = xp.dotProduct(sunPos);
        final double ys = yp.dotProduct(sunPos);
        final double xm = xp.dotProduct(satPos);
        final double ym = yp.dotProduct(satPos);

        // Compute time tg of Glint point leading to its coordinates xg, yg
        // initial time
        final double t0 = MathLib.atan2(satPos.dotProduct(yp), satPos.dotProduct(xp));

        // min and max time : first guess interval for solver
        final double tmin = t0 - FastMath.PI / 2.;
        final double tmax = t0 + FastMath.PI / 2.;

        // The function which zero is searched in [tmin, tmax]
        final UnivariateFunction function = new UnivariateFunction(){
            /** Serializable UID. */
            private static final long serialVersionUID = -5995700874905709270L;

            /** {@inheritDoc} */
            @Override
            public double value(final double tg) {
                // Build the glint function
                final double[] sincos = MathLib.sinAndCos(tg);
                final double sintg = sincos[0];
                final double costg = sincos[1];

                final Vector2D n = new Vector2D(a * (1.0 - fe) * costg, a * sintg).normalize();
                final double xg = a * costg;
                final double yg = a * (1.0 - fe) * sintg;
                final Vector2D gm = new Vector2D(xm - xg, ym - yg).normalize();
                final Vector2D gs = new Vector2D(xs - xg, ys - yg).normalize();
                final Vector2D b = gm.add(gs);
                return b.getX() * n.getY() - b.getY() * n.getX();
            }
        };

        // Solve for function = 0 with a Bisection solver to compute tg
        final double tglint = this.solver.solve(MAX_ITERATIONS, function, tmin, tmax, t0);

        // Deduce Glint point coordinates in frame (0, xp, yp)
        final double xg = a * MathLib.cos(tglint);
        final double yg = a * (1.0 - fe) * MathLib.sin(tglint);

        // Glint point in earth frame
        return new Vector3D(xg, xp, yg, yp);
    }

    /**
     * @param origin the origin of the direction
     * @param date the current date
     * @param frame the current frame
     * @throws PatriusException thrown if computation failed
     */
    private void computeLine(final PVCoordinatesProvider origin, final AbsoluteDate date,
                             final Frame frame) throws PatriusException {
        if (!origin.equals(this.cachedOrigin) || !date.equals(this.cachedDate) || !frame.equals(this.cachedFrame)) {
            // computation of the origin's position in the output frame at the
            // date
            final Vector3D satPos = origin.getPVCoordinates(date, this.bodyShape.getBodyFrame())
                .getPosition();
            final Vector3D sunPos = this.sunPVCoordinates.getPVCoordinates(date,
                this.bodyShape.getBodyFrame()).getPosition();
            // Compute Glint
            this.cachedGlintPoint = this.getGlintPosition(satPos, sunPos, date);

            // Build the line in output frame
            Line result = new Line(satPos, this.cachedGlintPoint, satPos);
            if (frame != null && this.bodyShape.getBodyFrame() != frame) {
                final Transform bodyFrametoOutputFrame = this.bodyShape.getBodyFrame().getTransformTo(
                    frame, date);
                result = bodyFrametoOutputFrame.transformLine(result);
            }
            this.cachedLine = result;
            // Store input parameters to compute these results in cache
            this.cachedOrigin = origin;
            this.cachedDate = date;
            this.cachedFrame = frame;
        }

    }

    /**
     * Get the position vector of the glint point
     * 
     * @param origin the origin of the direction
     * @param date the current date
     * @param frame the current frame
     * @return position vector of the glint point
     * @throws PatriusException thrown if computation failed
     */
    public Vector3D getGlintVectorPosition(final PVCoordinatesProvider origin,
                                           final AbsoluteDate date, final Frame frame) throws PatriusException {
        this.computeLine(origin, date, frame);
        return this.cachedGlintPoint;
    }
}
