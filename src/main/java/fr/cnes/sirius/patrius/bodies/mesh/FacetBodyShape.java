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
 * VERSION:4.12.1:FA:FA-125:05/09/2023:[PATRIUS] Reliquat OPENFD-62 sur le code des body shapes
 * VERSION:4.12:FA:FA-116:17/08/2023:[PATRIUS] Gestion du timeInterval dans
 * les classes QuaternionPolynomialProfile et QuaternionDatePolynomialProfile
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-59:30/06/2023:[PATRIUS] Code inutile dans la méthode FacetBodyShape.getIntersection
 * VERSION:4.11.1:FA:FA-52:30/06/2023:[PATRIUS] Précision dans la méthode FacetBodyShape.getFieldData
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-81:30/06/2023:[PATRIUS] Reliquat DM 3299
 * VERSION:4.11.1:FA:FA-60:30/06/2023:[PATRIUS] Erreur dans les méthodes getNeighbors de FacetBodyShape
 * VERSION:4.11:DM:DM-3288:22/05/2023:[PATRIUS] ID de facette pour un FacetBodyShape
 * VERSION:4.11:DM:DM-3297:22/05/2023:[PATRIUS] Optimisation calculs distance entre Line et FacetBodyShape 
 * VERSION:4.11:DM:DM-3317:22/05/2023:[PATRIUS] Parametres additionels non rattaches a orbite MultiOrbitalCovariance
 * VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.11:FA:FA-3314:22/05/2023:[PATRIUS] Anomalie evaluation ForceModel lorsque SpacecraftState en ITRF
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
 * VERSION:4.10.1:FA:FA-3265:02/12/2022:[PATRIUS] Calcul KO des points plus proches entre une Line et un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3250:03/11/2022:[PATRIUS] Generalisation de TopocentricFrame
 * VERSION:4.10:FA:FA-3186:03/11/2022:[PATRIUS] Corriger la duplication entre getLocalRadius et getApparentRadius
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3127:01/06/2022:[PATRIUS] Utilisation des attributs minNorm et maxNorm
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3134:10/05/2022:[PATRIUS] ameliorations mineures de Vector2D 
 * VERSION:4.9:DM:DM-3166:10/05/2022:[PATRIUS] Definir l'ICRF comme repere racine 
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3174:10/05/2022:[PATRIUS] Corriger les differences de convention entre toutes les methodes...
 * VERSION:4.9:DM:DM-3169:10/05/2022:[PATRIUS] Precision de l'hypothese de propagation instantanee de la lumiere
 * VERSION:4.9:DM:DM-3133:10/05/2022:[PATRIUS] Ajout de plusieurs fonctionnalites a la classe EclipseDetector 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3173:10/05/2022:[PATRIUS] Utilisation de FacetCelestialBody dans les calculs d'evenements 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.assembly.models.SensorModel;
import fr.cnes.sirius.patrius.bodies.AbstractBodyShape;
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plane;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.optim.InitialGuess;
import fr.cnes.sirius.patrius.math.optim.MaxEval;
import fr.cnes.sirius.patrius.math.optim.PointValuePair;
import fr.cnes.sirius.patrius.math.optim.SimpleBounds;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.GoalType;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.MultivariateOptimizer;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.ObjectiveFunction;
import fr.cnes.sirius.patrius.math.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Facet body shape defined by a list of facets. A facet is a 3D triangle defined in the body frame.
 * <p>
 * This class offers an optimal storage under a Binary Space Partition Tree (BSP Tree). Each of the body facet (class
 * {@link Triangle}) is linked to its neighbors. Each of the body vertex (class {@link Vertex}) is also linked to its
 * neighboring triangles {@link Triangle}. Hence this class provides very efficient methods (O(log n)) for intersection
 * computation, neighbors computation, etc.
 * </p>
 * <p>
 * This class implements the interface {@link BodyShape}:
 * <ul>
 * <li>As a {@link BodyShape}, this class can be used in conjunction with {@link EclipseDetector} and
 * {@link SensorModel}.</li>
 * </ul>
 * </p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.6
 */
public class FacetBodyShape extends AbstractBodyShape {

    /** Serializable UID. */
    private static final long serialVersionUID = -7564873573665379652L;

    /**
     * Bodycentric/normal used as default LLH coordinates system for the computed {@link FacetPoint facet body points}.
     */
    private static final LLHCoordinatesSystem DEFAULT_LLH_COORD_SYSTEM = LLHCoordinatesSystem.BODYCENTRIC_NORMAL;

    /** Epsilon (squared) for distance comparison. */
    private static final double EPSILON2 = 1E-12;

    /** Epsilon for fitted ellipsoid computation. */
    private static final double EPS_OPT = 1E-8;

    /** Default threshold needed for apparent radius determination convergence. */
    private static final double DEFAULT_THRESHOLD = 1E-2;

    /** Maximum number of criterion evaluation for fitted ellipsoid computation. */
    private static final int MAX_EVAL = 1000;

    /** First guess of the flattening value for the optimizer. Flattening value is between 0 and 1. */
    private static final double FIRST_GUESS_FLATTENING = 1E-1;

    /** Default maximum number of steps for apparent radius calculation */
    private static final int DEFAULT_MAX_APPARENT_RADIUS_STEPS = 100;

    /** Maximum number of steps for apparent radius calculation */
    private int maxApparentRadiusSteps = DEFAULT_MAX_APPARENT_RADIUS_STEPS;

    /** Mesh of {@link Triangle} stored under a list of {@link Triangle}. */
    private transient Triangle[] triangles;

    /** Mesh of {@link Triangle} stored under a Binary Space Partition Tree. */
    private transient TrianglesSet tree;

    /**
     * Type of ellipsoid to apply transformation methods on.
     * @deprecated as of 4.12, any kin of ellipsoid can be built on the fly.
     */
    @Deprecated
    private final EllipsoidType ellipsoidType;

    /** Distance from center to closest vertex to center. */
    private final double minNorm;

    /** Distance from center to farthest vertex to center. */
    private final double maxNorm;

    /** Fitted ellipsoid which is the ellipsoid (a, f) which minimizes the distance to all vertices. */
    private OneAxisEllipsoid fittedEllipsoid;

    /** Inner ellipsoid which is the largest ellipsoid strictly contained in the mesh and centered around (0, 0, 0). */
    private OneAxisEllipsoid innerEllipsoid;

    /** Outer ellipsoid which is the smallest ellipsoid englobing the shape and centered around (0, 0, 0). */
    private OneAxisEllipsoid outerEllipsoid;

    /** Inner sphere which is the largest sphere strictly contained in the mesh and centered around (0, 0, 0). */
    private OneAxisEllipsoid innerSphere;

    /** Outer sphere which is the smallest sphere englobing the shape and centered around (0, 0, 0). */
    private OneAxisEllipsoid outerSphere;

    /** Threshold needed for apparent radius determination convergence. */
    private double threshold;

    /** Mesh provider. */
    private final MeshProvider meshProvider;

    /**
     * Maximum angle between the normal to a facet of the body and the vector from the origin to the
     * centre of the facet.
     */
    private final double maxSlope;

    /** Type of ellipsoid to apply transformation methods on. */
    public enum EllipsoidType {
        /** Inner sphere. */
        INNER_SPHERE,
        /** Outer sphere. */
        OUTER_SPHERE,
        /** Inner ellipsoid. */
        INNER_ELLIPSOID,
        /** Outer ellipsoid. */
        OUTER_ELLIPSOID,
        /** Fitted ellipsoid. */
        FITTED_ELLIPSOID;
    }

    /**
     * Constructor.
     *
     * @param name
     *        body name
     * @param bodyFrame
     *        frame in which celestial body coordinates are defined
     * @param ellipsoidTypeIn
     *        ellipsoid type to apply the transform methods on
     * @param meshLoader
     *        mesh loader
     */
    public FacetBodyShape(final String name, final Frame bodyFrame, final EllipsoidType ellipsoidTypeIn,
                          final MeshProvider meshLoader) {
        super(name, bodyFrame);

        this.meshProvider = meshLoader;

        this.triangles = meshLoader.getTriangles();

        // Get min and max possible semi-major axis
        double a0max = 0;
        double a0min = Double.POSITIVE_INFINITY;
        for (final Vertex v : meshLoader.getVertices().values()) {
            final double pos = v.getPosition().getNorm();
            a0max = MathLib.max(a0max, pos);
            a0min = MathLib.min(a0min, pos);
        }
        this.minNorm = a0min;
        this.maxNorm = a0max;

        // Computing the maxSlope
        double maxSlopeTemp = 0;
        for (final Triangle facet : this.triangles) {
            final Vector3D normal = facet.getNormal();
            final Vector3D position = facet.getCenter();
            // .subtract(this.getBodyFrame().getPVCoordinates(AbsoluteDate.J2000_EPOCH, frame));
            // final Vector3D position = facet.getCenter();

            // the maxSlope corresponds to the angle between the normal to the facet and the vector
            // from the bodyFrame origin to the facet centre.
            final double angle = Vector3D.angle(normal, position);

            maxSlopeTemp = MathLib.max(maxSlopeTemp, angle);
        }

        this.maxSlope = maxSlopeTemp;
        this.threshold = DEFAULT_THRESHOLD;

        // Type of ellispoid to use for transformation computation
        this.ellipsoidType = ellipsoidTypeIn;

        // Build BSP tree
        this.tree = new TrianglesSet(this.triangles);

        // Link triangles to each other
        linkTriangles();

        // default coordinates system is used
        this.lLHCoordinatesSystem = DEFAULT_LLH_COORD_SYSTEM;
    }

    /**
     * Link triangles to each other. Triangles are linked to their neighbors.
     */
    private void linkTriangles() {
        for (final Triangle triangle : this.triangles) {
            for (final Vertex vertex : triangle.getVertices()) {
                for (final Triangle triangle2 : vertex.getNeighbors()) {
                    // Check if triangles are neighbors, if so update list of neighbors
                    if (triangle.isNeighborByVertexID(triangle2) && !triangle.getNeighbors().contains(triangle2)) {
                        triangle.addNeighbors(triangle2);
                        triangle2.addNeighbors(triangle);
                    }
                }
            }
        }
    }

    /**
     * Build fitted ellipsoid which is the ellipsoid (a, f) which minimizes the distance to all vertices. Minimization
     * is reached with a {@link PowellOptimizer}.
     *
     * @param vertices
     *        list of vertices
     * @return fitted ellipsoid
     */
    private OneAxisEllipsoid buildFittedEllipsoid(final Map<Integer, Vertex> vertices) {
        // Precompute sin and cos of all vertices position
        final double[] cosLon = new double[vertices.size()];
        final double[] sinLon = new double[vertices.size()];
        final double[] cosLat = new double[vertices.size()];
        final double[] sinLat = new double[vertices.size()];
        int i = 0;
        for (final Vertex v : vertices.values()) {
            // Geodetic point
            Vector3D normedPoint = Vector3D.ZERO;
            if (v.getPosition().getNorm() > 0) {
                normedPoint = v.getPosition().normalize();
            }
            final double latitude = MathLib.asin(normedPoint.getZ());
            final double longitude = MathLib.atan2(normedPoint.getY(), normedPoint.getX());

            // cos/sin for given geodetic point
            final double[] sincosLon = MathLib.sinAndCos(longitude);
            sinLon[i] = sincosLon[0];
            cosLon[i] = sincosLon[1];
            final double[] sincosLat = MathLib.sinAndCos(latitude);
            sinLat[i] = sincosLat[0];
            cosLat[i] = sincosLat[1];
            i++;
        }

        // Use optimizer
        final MultivariateOptimizer optimizer = new PowellOptimizer(EPS_OPT, EPS_OPT);
        // Cost function to minimize
        final MultivariateFunction func = point -> {
            final double a = point[0];
            final double f = point[1];
            final double b = a * (1. - f);
            final double e2 = 1 - (1. - f) * (1. - f);
            double cost = 0;
            int i1 = 0;
            for (final Vertex v : vertices.values()) {
                final double r = b / FastMath.sqrt(1. - e2 * cosLat[i1] * cosLat[i1]);
                // Theoretical point for current a and f values
                final Vector3D vTh = new Vector3D(r * cosLat[i1] * cosLon[i1], r * cosLat[i1] * sinLon[i1], r
                        * sinLat[i1]);
                // Distance squared: add to cost function
                cost += vTh.distanceSq(v.getPosition());
                i1++;
            }
            return cost;
        };
        // Run optimizer
        // Semi-major axis is in [this.minNorm, this.maxNorm]
        // Flattening is in [0, 1]
        final PointValuePair res = optimizer.optimize(new MaxEval(MAX_EVAL), new ObjectiveFunction(func),
            GoalType.MINIMIZE, GoalType.MINIMIZE, new InitialGuess(new double[] { (this.minNorm + this.maxNorm) / 2.,
                FIRST_GUESS_FLATTENING }), new SimpleBounds(new double[] { this.minNorm, 0. },
                new double[] { this.maxNorm, 1. }));

        // Build ellipsoid with optimum values (a, f)
        return new OneAxisEllipsoid(res.getPoint()[0], res.getPoint()[1], getBodyFrame(), getName());
    }

    /**
     * Build inner ellipsoid which is the largest ellipsoid strictly contained in the mesh and centered around (0, 0,
     * 0).
     *
     * @param vertices
     *        list of vertices
     * @return inner ellipsoid
     */
    private OneAxisEllipsoid buildInnerEllipsoid(final Map<Integer, Vertex> vertices) {

        // Flattening of the fitted ellipsoid
        final double flattening = getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getFlattening();
        // Dilatation of the ellipsoidal DTM into a spherical DTM to find the largest inscribed
        // sphere contained within it.
        final double dilatation = 1. / (1. - flattening);

        double minDilatedRadius = Double.POSITIVE_INFINITY;
        for (final Vertex v : this.meshProvider.getVertices().values()) {
            final Vector3D dilatedPoint = new Vector3D(v.getPosition().getX(), v.getPosition().getY(), v.getPosition()
                .getZ() * dilatation);
            final double dilatedRadius = dilatedPoint.getNorm();
            minDilatedRadius = MathLib.min(minDilatedRadius, dilatedRadius);
        }

        // Return the inner ellipsoid, i.e. the biggest ellipsoid strictly contained in
        // the mesh. The dilated radius of the inscribed sphere (found in the spherical problem)
        // corresponds to the equatorial radius of the largest inner ellipsoid (flattened problem)
        return new OneAxisEllipsoid(minDilatedRadius, flattening, getBodyFrame(), getName());
    }

    /**
     * Build outer ellipsoid which is the smallest ellipsoid englobing the shape and centered around (0, 0, 0).
     *
     * @param vertices
     *        list of vertices
     * @return outer ellipsoid
     */
    private OneAxisEllipsoid buildOuterEllipsoid(final Map<Integer, Vertex> vertices) {
        final double dilatation = 1. / (1. - getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getFlattening());

        double maxDilatedRadius = Double.NEGATIVE_INFINITY;
        for (final Vertex v : this.meshProvider.getVertices().values()) {
            final Vector3D dilatedPoint = new Vector3D(v.getPosition().getX(), v.getPosition().getY(), v.getPosition()
                .getZ() * dilatation);
            final double dilatedRadius = dilatedPoint.getNorm();
            maxDilatedRadius = MathLib.max(maxDilatedRadius, dilatedRadius);
        }

        // Return the outer ellipsoid, i.e. the smallest ellipsoid englobing the mesh. The dilated
        // radius of the englobing sphere (found in the spherical problem) corresponds to the
        // equatorial radius of the smaller outer ellipsoid (flattened problem)
        return new OneAxisEllipsoid(maxDilatedRadius, getEllipsoid(EllipsoidType.FITTED_ELLIPSOID).getFlattening(),
            getBodyFrame(), getName());
    }

    /**
     * Build inner sphere which is largest sphere strictly contained in the mesh and centered around (0, 0, 0).
     *
     * @param vertices
     *        list of vertices
     * @return inner sphere
     */
    private OneAxisEllipsoid buildInnerSphere(final Map<Integer, Vertex> vertices) {
        // Return the inner sphere (whose radius is equal to the minimum distance) strictly
        // contained in the mesh
        return new OneAxisEllipsoid(getMinNorm(), 0., getBodyFrame(), getName());
    }

    /**
     * Build outer sphere which is the smallest sphere englobing the shape and centered around (0, 0, 0).
     *
     * @param vertices
     *        list of vertices
     * @return outer sphere
     */
    private OneAxisEllipsoid buildOuterSphere(final Map<Integer, Vertex> vertices) {
        // Return the outer sphere (whose radius is equal to the maximum distance) englobing the
        // shape
        return new OneAxisEllipsoid(getMaxNorm(), 0., getBodyFrame(), getName());
    }

    /**
     * Retrieve the ellipsoid specified by the {@link EllipsoidType} attribute use to apply transformation methods on.
     *
     * @return an ellipsoid that models the body.
     * @deprecated as of 4.12 {@link EllipsoidPoint} is restrained with use of {@link OneAxisEllipsoid} and associated
     *             method {@link OneAxisEllipsoid#buildPoint(Vector3D, Frame, AbsoluteDate)}.
     */
    @Deprecated
    private OneAxisEllipsoid getTransformEllipsoid() {
        return getEllipsoid(this.ellipsoidType);
    }

    /**
     * Getter for the ellipsoid of the desired type.
     * 
     * @param ellipsoidTypeIn
     *        the type of the ellipsoid to be returned
     * @return the desired ellipsoid
     */
    public OneAxisEllipsoid getEllipsoid(final EllipsoidType ellipsoidTypeIn) {
        final OneAxisEllipsoid ellipsoid;
        switch (ellipsoidTypeIn) {
            case FITTED_ELLIPSOID:
                // The fitted ellipsoid
                // If the fitted ellipsoid is null, build it
                if (this.fittedEllipsoid == null) {
                    this.fittedEllipsoid = buildFittedEllipsoid(this.meshProvider.getVertices());
                }
                ellipsoid = this.fittedEllipsoid;
                break;
            case INNER_ELLIPSOID:
                // The inner ellipsoid
                // If the inner ellipsoid is null, build it
                if (this.innerEllipsoid == null) {
                    this.innerEllipsoid = buildInnerEllipsoid(this.meshProvider.getVertices());
                }
                ellipsoid = this.innerEllipsoid;
                break;
            case OUTER_ELLIPSOID:
                // The outer ellipsoid
                // If the outer ellipsoid is null, build it
                if (this.outerEllipsoid == null) {
                    this.outerEllipsoid = buildOuterEllipsoid(this.meshProvider.getVertices());
                }
                ellipsoid = this.outerEllipsoid;
                break;
            case INNER_SPHERE:
                // The inner sphere
                // If the inner sphere is null, build it
                if (this.innerSphere == null) {
                    this.innerSphere = buildInnerSphere(this.meshProvider.getVertices());
                }
                ellipsoid = this.innerSphere;
                break;
            case OUTER_SPHERE:
                // The outer sphere
                // If the outer sphere is null, build it
                if (this.outerSphere == null) {
                    this.outerSphere = buildOuterSphere(this.meshProvider.getVertices());
                }
                ellipsoid = this.outerSphere;
                break;
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return ellipsoid;
    }

    /**
     * Getter for the mesh under a list of triangles. The triangles are not sorted in any particular way.
     *
     * @return the mesh under a list of triangles
     */
    // Reason: performances
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Triangle[] getTriangles() {
        return this.triangles;
    }

    /**
     * Getter for the intersection point of a line with the surface of the body.
     * <p>
     * A line may have several intersection points with a closed surface (we consider the one point case as a
     * degenerated two points case). The close parameter is used to select which of these points should be returned. The
     * selected point is the one that is closest to the close point.
     * </p>
     *
     * @param line
     *        test line (may intersect the body or not)
     * @param close
     *        point used for intersections selection expressed in the body frame
     * @param frame
     *        frame in which line is expressed
     * @param date
     *        date of the line in given frame
     * @return <intersection triangle, intersection point in frame> or null if the line does not intersect the surface
     * @throws PatriusException
     *         if line cannot be converted to body frame
     */
    public Intersection getIntersection(final Line line, final Vector3D close, final Frame frame,
                                        final AbsoluteDate date)
        throws PatriusException {

        // Convert line to body frame
        final Line lineInBodyFrame = frame.getTransformTo(getBodyFrame(), date).transformLine(line);

        // Intersection in body frame
        final Intersection intersectInBodyFrame = getIntersection(lineInBodyFrame, close);

        if (intersectInBodyFrame == null) {
            return null;
        }

        // Convert intersection to frame
        return new Intersection(intersectInBodyFrame.getTriangle(),
            getBodyFrame().getTransformTo(frame, date).transformPosition(intersectInBodyFrame.getPoint()));
    }

    /**
     * Getter for the intersection point of a line, expressed in the body frame, with the surface of the body.
     * <p>
     * A line may have several intersection points with a closed surface (we consider the one point case as a
     * degenerated two points case). The close parameter is used to select which of these points should be returned. The
     * selected point is the one that is closest to the close point.
     * </p>
     *
     * @param lineInBodyFrame
     *        test line expressed in the body frame (may intersect the body or not)
     * @param close
     *        point used for intersections selection expressed in the body frame
     * @return the intersection point <u>in body frame</u> associated with the intersection triangle, or null if the
     *         line does not intersect the surface
     */
    private Intersection getIntersection(final Line lineInBodyFrame, final Vector3D close) {

        // Get list of intersection points from triangles which intersect line
        final Intersection[] intersections = this.tree.getIntersections(lineInBodyFrame);

        // Initialize result
        Intersection res = null;

        // Get closest intersection point from "close" point
        if (intersections != null) {
            // Define the minimum squared distance to positive infinity to be sure that the squared
            // distance of the first valid point will be smaller than this minimum distance
            Double minDist2 = Double.POSITIVE_INFINITY;
            // Initialize the boolean which identifies the first valid point to true
            boolean firstValidPoint = true;
            // Initialize the index of the closest valid point to zero
            int indexClosestPoint = 0;
            // Loop on all the intersections
            for (int i = 0; i < intersections.length; i++) {
                // Retrieve current intersection point
                final Vector3D currentInterPoint = intersections[i].getPoint();
                // Check if the intersection point is on the right side of the line by comparing its
                // abscissa with the minimum abscissa
                if (lineInBodyFrame.getAbscissa(currentInterPoint) > lineInBodyFrame.getMinAbscissa()) {
                    if (firstValidPoint) {
                        minDist2 = Vector3D.distanceSq(close, currentInterPoint);
                        // Set the boolean which identifies the first valid point to false
                        firstValidPoint = false;
                    }
                    final double dist2 = Vector3D.distanceSq(close, intersections[i].getPoint());
                    if (dist2 < minDist2) {
                        indexClosestPoint = i;
                        minDist2 = dist2;
                    }
                }
            }

            // intersection position expressed in body frame
            res = new Intersection(intersections[indexClosestPoint].getTriangle(),
                intersections[indexClosestPoint].getPoint());
        }

        return res;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned facet point is associated with one or several triangles following the closest point on shape
     * position:
     * <ul>
     * <li>3 triangles or more if the closest point on shape corresponds to one shape vertex,</li>
     * <li>2 triangles if the closest point on shape belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     * </p>
     */
    @Override
    public FacetPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                           final AbsoluteDate date) throws PatriusException {
        return getIntersectionPoint(line, close, frame, date, BodyPointName.INTERSECTION);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned facet point is associated with one or several triangles following the closest point on shape
     * position:
     * <ul>
     * <li>3 triangles or more if the closest point on shape corresponds to one shape vertex,</li>
     * <li>2 triangles if the closest point on shape belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     * </p>
     */
    @Override
    public FacetPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                           final AbsoluteDate date, final String nameIn)
        throws PatriusException {
        
        // Convert line to body frame
        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        final Line lineInBodyFrame = t.transformLine(line);

        // compute intersection and one belonging triangle
        final Intersection intersection = getIntersection(lineInBodyFrame, close);

        FacetPoint res = null;
        if (intersection != null) {

            // compute the neighbors to all vertices delimiting the triangle of computed intersection
            // Note: method getNeighbors cannot be used as there must be a lot of neighbors for a given vertex
            final Map<Integer, Triangle> neighborsMap = new ConcurrentHashMap<>();
            // loop over the vertices delimiting the triangle of computed intersection
            for (final Vertex vertex : intersection.getTriangle().getVertices()) {
                // compute the neighbors triangles to the current vertex, and put in the map
                for (final Triangle neighbor : vertex.getNeighbors()) {
                    neighborsMap.put(neighbor.getID(), neighbor);
                }
            }

            // compute all the triangles containing the computed intersection (with a tolerance of altitude epsilon)
            final List<Triangle> trianglesList = new ArrayList<>();
            for (final Triangle neighbor : neighborsMap.values()) {
                if (neighbor.closestPointTo(line)[1].distance(intersection.getPoint()) < getDistanceEpsilon()) {
                    // the computed intersection belongs to current neighbor : add this neighbor to output list if not
                    // added yet
                    trianglesList.add(neighbor);
                }
            }

            res = new FacetPoint(this, intersection.getPoint(), trianglesList, true, nameIn);
        }
        return res;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Warning: this method returns {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)} if altitude is
     * close to 0. An exception is thrown otherwise.
     * </p>
     * <p>
     * The returned facet point is associated with one or several triangles following the closest point on shape
     * position:
     * <ul>
     * <li>3 triangles or more if the closest point on shape corresponds to one shape vertex,</li>
     * <li>2 triangles if the closest point on shape belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     * </p>
     *
     * @throws PatriusException
     *         if entered altitude is not close to 0
     *         if line cannot be converted to body frame
     */
    @Override
    public FacetPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                           final AbsoluteDate date, final double altitude) throws PatriusException {
        if (MathLib.abs(altitude) >= this.distanceEpsilon) {
            throw new PatriusException(PatriusMessages.UNAVAILABLE_FACETBODYSHAPE_INTERSECTION_POINT_METHOD);
        }

        // Altitude is considered to be 0
        return getIntersectionPoint(line, close, frame, date);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Each facet point in the returned intersections array is associated with one single triangle (other possible
     * belonging triangles not computed).
     * </p>
     */
    @Override
    public FacetPoint[] getIntersectionPoints(final Line line, final Frame frame, final AbsoluteDate date)
        throws PatriusException {

        // Convert line to body frame if necessary
        Transform t = Transform.IDENTITY;
        Line lineInBodyFrame = line;
        if (!frame.equals(getBodyFrame())) {
            t = frame.getTransformTo(getBodyFrame(), date);
            lineInBodyFrame = t.transformLine(line);
        }

        // Get list of intersection points from triangles which intersect line
        final Intersection[] intersections = this.tree.getIntersections(lineInBodyFrame);

        final FacetPoint[] result;
        if (intersections != null) {
            // Remove duplicates
            // May happen if intersection exactly lies on some triangles edge
            final List<Intersection> intersectionPoints = new ArrayList<>();
            for (int i = 0; i < intersections.length; i++) {
                // Define current intersection point
                final Vector3D currentInterPoint = intersections[i].getPoint();
                boolean isDuplicate = false;
                for (int j = 0; j < i; j++) {
                    if (currentInterPoint.distanceSq(intersections[j].getPoint()) < EPSILON2) {
                        isDuplicate = true;
                        break;
                    }
                }
                // Check if the current intersection point is duplicate and whether it is on the
                // right side of the line by comparing its abscissa with the minimum abscissa
                if (!isDuplicate && lineInBodyFrame.getAbscissa(currentInterPoint) > lineInBodyFrame.getMinAbscissa()) {
                    // Add the current intersection point to the list of valid intersection points
                    intersectionPoints.add(intersections[i]);
                }
            }

            result = new FacetPoint[intersectionPoints.size()];
            if (frame.equals(getBodyFrame())) {
                // No conversion
                for (int i = 0; i < result.length; i++) {
                    result[i] = new FacetPoint(this, intersectionPoints.get(i), BodyPointName.INTERSECTION);
                }
            } else {
                // Conversion
                final Transform tInv = t.getInverse();
                for (int i = 0; i < result.length; i++) {
                    final Intersection intersectionPoint = intersectionPoints.get(i);
                    final Intersection intersectionTransformed = new Intersection(intersectionPoint.getTriangle(),
                        tInv.transformPosition(intersectionPoint.getPoint()));
                    // The triangle is expressed in the body frame, so it isn't transformed
                    // The point is expressed in the output frame, so it is transformed from body frame to output frame
                    result[i] = new FacetPoint(this, intersectionTransformed, BodyPointName.INTERSECTION);
                }
            }

        } else {
            // No intersection case
            result = new FacetPoint[0];
        }

        // Return result
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Warning: this method considers the body either as the inner sphere, the outer ellipsoid or the fitted ellipsoid
     * depending on the {@link EllipsoidType} attribute.
     * </p>
     *
     * @deprecated as of 4.12 {@link EllipsoidPoint} is restrained with use of {@link OneAxisEllipsoid} and associated
     *             method {@link OneAxisEllipsoid#buildPoint(Vector3D, Frame, AbsoluteDate)}.
     */
    @Deprecated
    @Override
    public EllipsoidPoint transform(final Vector3D point, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Approximation of body by its transform ellipsoid
        return getTransformEllipsoid().buildPoint(point, frame, date, BodyPointName.DEFAULT);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated as of 4.12 {@link EllipsoidPoint} is restrained with use of {@link OneAxisEllipsoid} and associated
     *             method {@link OneAxisEllipsoid#buildPoint(Vector3D, Frame, AbsoluteDate)}. Simply returns the point
     *             position.
     */
    @Deprecated
    @Override
    public Vector3D transform(final EllipsoidPoint point) {
        return point.getPosition();
    }

    /**
     * Find all facets from the mesh that break the convexity property of the facet body shape: the
     * slope angle is over PI/2.
     * The slope angle is defined as the angle between the normal to the facet and the vector from
     * the origin of the body shape to the
     * centre of the facet. If the maxSlope is under PI/2, then the list shall be empty. See {@link #maxSlope}.
     * 
     * @return the list of facets violating the convexity property of the body shape
     */
    public List<Triangle> getOverPerpendicularSteepFacets() {
        // If the maxSlope is under PI/2, then no facet has an angle steeper than PI/2: no need to
        // find them. Return an empty list
        final List<Triangle> steepTriangleList = new ArrayList<>();
        if (this.maxSlope >= MathLib.PI / 2) {
            for (final Triangle facet : this.triangles) {
                // normal to the facet
                final Vector3D normal = facet.getNormal();
                // vector from the origin of the body frame to the center of the facet
                final Vector3D position = facet.getCenter();
                // Computation of the angle giving the slope of the facet
                final double angle = Vector3D.angle(normal, position);
                if (angle >= MathLib.PI / 2) {
                    steepTriangleList.add(facet);
                }
            }
        }
        return steepTriangleList;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is exact and in O(n).
     * </p>
     */
    @Override
    public double distanceTo(final Line line, final Frame frame, final AbsoluteDate date) throws PatriusException {

        // Convert line to body frame
        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        final Line lineInBodyFrame = t.transformLine(line);
        // Compute distance to all facets
        double minDistance = Double.POSITIVE_INFINITY;
        for (final Triangle triangle : this.triangles) {
            final double dist = triangle.distanceTo(lineInBodyFrame);
            minDistance = MathLib.min(minDistance, dist);
        }
        return minDistance;
    }

    /**
     * {@inheritDoc}
     * <p>
     * To compute the apparent radius (in meters), the algorithm iterates, until convergence, between the minimum and
     * the maximum angles (and so radii too), given by the apparent angles (and so radii too) of the inner and outer
     * ellipsoid of this occulting body. Given the plane containing the observer, the occulted body and this occulting
     * body, if the line lying on this plane and connecting the observer to the hypothetical tangential point of this
     * occulting body actually intersects this occulting body, the search for the correct angle value by which this line
     * shall be rotated (within the given plane) to be tangential to this occulting body will continue towards a larger
     * angle, otherwise it will continue towards a smaller angle. Each time, this line is rotated by the current value
     * of the angle and the value of the apparent radius is computed thanks to the current angle and the distance
     * between the observer and this occulting body. Convergence is reached when the absolute value of the difference
     * between the current value of the apparent radius and the previous one is smaller than a specified threshold (in
     * meters).
     * </p>
     */
    @Override
    public double getApparentRadius(final PVCoordinatesProvider pvObserver, final AbsoluteDate date,
                                    final PVCoordinatesProvider occultedBody,
                                    final PropagationDelayType propagationDelayType)
        throws PatriusException {
        // Position of observer is at date of observation taking into account light speed if required
        // Signal propagation is performed in closest (in term of distance on frames tree) inertial frame

        // Initialization (case of instantaneous propagation)
        AbsoluteDate occultingReceptionDate = date;
        Frame frameAtOccultingDate = getBodyFrame();
        AbsoluteDate emissionDate = date;

        // Case of light speed propagation (dedicated in order to optimize computation times)
        if (propagationDelayType.equals(PropagationDelayType.LIGHT_SPEED)) {
            // Native frames
            final Frame nativeFrameObserver = pvObserver.getNativeFrame(date, getBodyFrame());
            final Frame nativeFrameOccultingBody = getNativeFrame(date, getBodyFrame());
            final Frame nativeFrameOccultedBody = occultedBody.getNativeFrame(date, getBodyFrame());

            // Position of emitter is at date of emission taking into account light speed if required
            // Signal propagation is performed in closest (in term of distance on frames tree) inertial frame
            final Frame inertialFrame = nativeFrameObserver
                .getFirstCommonPseudoInertialAncestor(nativeFrameOccultedBody);
            emissionDate = VacuumSignalPropagationModel.getSignalEmissionDate(occultedBody, pvObserver, date,
                getEpsilonSignalPropagation(), propagationDelayType, inertialFrame);

            // Occulting body data (defined by its frame) are computed when signal from occulted body is received
            // This frame is then frozen in order to perform computations since this occulting body is only defined
            // by its frame
            final Frame occultingInertialFrame = nativeFrameOccultingBody
                .getFirstCommonPseudoInertialAncestor(nativeFrameOccultedBody);
            occultingReceptionDate = VacuumSignalPropagationModel.getSignalReceptionDate(this, occultedBody,
                emissionDate, getEpsilonSignalPropagation(), propagationDelayType, occultingInertialFrame);
            frameAtOccultingDate = getBodyFrame().getFrozenFrame(FramesFactory.getICRF(), occultingReceptionDate,
                getBodyFrame().getName() + "-Frozen");
        }

        final Vector3D posObserver = pvObserver.getPVCoordinates(date, frameAtOccultingDate).getPosition();
        // Retrieve the minimum radius as the (minimum) distance from the center to the closest vertex
        final double minRadius = this.minNorm;
        // Retrieve the maximum radius as the (maximum) distance from the center to the farthest vertex
        final double maxRadius = this.maxNorm;
        // Initialize the new apparent radius to the average between the minimum radius and the maximum radius
        double newApparentRadius = (minRadius + maxRadius) / 2;
        // Retrieve the position of occulted body
        final Vector3D posOcculted = occultedBody.getPVCoordinates(emissionDate, frameAtOccultingDate).getPosition();
        // Retrieve the position of this occulting body at date of signal reception from occulting body
        final Vector3D posOcculting = getPVCoordinates(occultingReceptionDate, frameAtOccultingDate).getPosition();
        // Compute the direction observer-occulting
        final Vector3D obsOcculting = posOcculting.subtract(posObserver);
        // test whether the objects are aligned
        if (Vector3D.angle(obsOcculting, posOcculted.subtract(posObserver)) != 0) {
            // Compute the plane containing the observer, the occulted and the occulting
            final Plane plane = new Plane(posObserver, posOcculted, posOcculting);
            // Retrieve the normal axis to the plane
            final Vector3D normalAxis = plane.getNormal();
            // Initialize the minimum angle to the angle corresponding to the minimum radius
            double minAngle = FastMath.asin(minRadius / obsOcculting.getNorm());
            // Initialize the maximum angle to the angle corresponding to the maximum radius
            double maxAngle = FastMath.asin(maxRadius / obsOcculting.getNorm());
            // Initialize the current angle between the direction observer-occulting and the direction
            // observer-tangentialPoint to the angle corresponding to the new apparent radius
            double currentAngle = FastMath.asin(newApparentRadius / obsOcculting.getNorm());
            // Initialize the old apparent radius to the minimum radius
            double oldApparentRadius = minRadius;
            // Initialize the error to the difference between the new apparent radius and the old apparent radius
            double error = newApparentRadius - oldApparentRadius;
            // Define the direction observer-tangentialPoint
            Vector3D obsTangentialPoint;
            // Define the rotation to be applied to the direction observer-occulting
            Rotation rotation;
            // Loop while the error is larger than the threshold
            int i = 0;
            while (i < this.maxApparentRadiusSteps && Math.abs(error) > getThreshold()) {
                // Save as old apparent radius the current value of the new apparent radius to compute the error later
                oldApparentRadius = newApparentRadius;
                // Create the rotation to be applied to the direction observer-occulting
                rotation = new Rotation(normalAxis, -currentAngle);
                // Compute the direction observer-tangentialPoint by applying a rotation of the current angle to the
                // direction observer-occulting
                obsTangentialPoint = rotation.applyTo(obsOcculting);
                // Retrieve the closest intersection point between the line observer-tangentialPoint and this occulting
                // body, at reception date (= date of Line definition)
                final FacetPoint[] geodeticInterPoints = getIntersectionPoints(new Line(posObserver,
                    posObserver.add(obsTangentialPoint), posObserver), frameAtOccultingDate, date);
                // Check if the line observer-tangentialPoint intersects this occulting body
                if (geodeticInterPoints.length > 0) {
                    // Set the current angle as the new minimum angle to search for a larger angle
                    minAngle = currentAngle;
                } else {
                    // Set the current angle as the new maximum angle to search for a smaller angle
                    maxAngle = currentAngle;
                }
                // Update the value of the current angle to the average between the minimum angle and the maximum angle
                currentAngle = (minAngle + maxAngle) / 2;
                // Update the value of the apparent radius to the radius corresponding to the current angles
                newApparentRadius = obsOcculting.getNorm() * FastMath.abs(FastMath.sin(currentAngle));
                // Compute the error as the difference between the new apparent radius and the old apparent radius
                error = newApparentRadius - oldApparentRadius;
                // Keep track of steps
                i++;
            }
            if (i >= this.maxApparentRadiusSteps) {
                throw new MaxCountExceededException(PatriusMessages.CONVERGENCE_FAILED, this.maxApparentRadiusSteps);
            }
        } else {
            // return the minimum radius in default case
            newApparentRadius = this.minNorm;
        }

        // Return the new apparent radius
        return newApparentRadius;
    }

    /**
     * Getter for the neighbors of provided triangle whose center is closer than provided distance of provided triangle
     * center.
     * <p>
     * Provided triangle is included in the list of neighbors.
     * </p>
     * <p>
     * Beware not to confuse this method with {@link #getNeighbors(Triangle, int)}
     * </p>
     *
     * @param triangle
     *        a triangle
     * @param maxDistance
     *        max distance
     * @return the neighbors of provided triangle whose center is closer than provided distance of provided triangle
     *         center
     */
    public List<Triangle> getNeighbors(final Triangle triangle, final double maxDistance) {
        return getNeighbors(triangle, triangle.getCenter(), maxDistance, Integer.MAX_VALUE);
    }

    /**
     * Getter for the neighbors of provided triangle whose distance in terms of triangle is closer or equal to provided
     * order of "neighborhood". For example:
     * <ul>
     * <li>Order 0 returns provided triangle</li>
     * <li>Order 1 returns provided triangle and the immediate neighbors of provided triangle</li>
     * <li>Order 2 returns provided triangle, the immediate neighbors of provided triangle and also their own immediate
     * neighbors</li>
     * </ul>
     * <p>
     * Beware not to confuse this method with {@link #getNeighbors(Triangle, double)}
     * </p>
     *
     * @param triangle
     *        a triangle
     * @param order
     *        order of "neighborhood"
     * @return the neighbors of provided triangle whose distance in terms of triangle is closer or equal to provided
     *         order of "neighborhood"
     */
    public List<Triangle> getNeighbors(final Triangle triangle, final int order) {
        return getNeighbors(triangle, triangle.getCenter(), Double.POSITIVE_INFINITY, order);
    }

    /**
     * Getter for the neighbors of provided geodetic point whose center is closer than provided distance.
     *
     * @param point
     *        a body point
     * @param maxDistance
     *        max distance
     * @return the neighbors of provided geodetic point whose center is closer than provided distance
     */
    public List<Triangle> getNeighbors(final BodyPoint point, final double maxDistance) {
        return getNeighbors(point.getPosition(), maxDistance);
    }

    /**
     * Getter for the neighbor triangles of provided body point which are closer or equal to provided order of
     * "neighborhood".<br>
     * For example:
     * <ul>
     * <li>Order 0 returns closest triangle</li>
     * <li>Order 1 returns closest triangle and the immediate neighbors of closest triangle</li>
     * <li>Order 2 returns closest triangle, the immediate neighbors of closest triangle and also their own immediate
     * neighbors</li>
     * </ul>
     * <p>
     * Beware not to confuse this method with {@link #getNeighbors(BodyPoint, double)}
     * </p>
     *
     * @param point
     *        a body point
     * @param order
     *        order of "neighborhood"
     * @return the neighbor triangles of provided body point which are closer or equal to provided order of
     *         "neighborhood"
     */
    public List<Triangle> getNeighbors(final BodyPoint point, final int order) {
        return getNeighbors(point.getPosition(), order);
    }

    /**
     * Getter for the neighbors of provided cartesian point whose center is closer than provided distance.
     *
     * @param pos
     *        a point in body frame
     * @param maxDistance
     *        max distance
     * @return the neighbors of provided cartesian point whose center is closer than provided distance
     */
    public List<Triangle> getNeighbors(final Vector3D pos, final double maxDistance) {
        // Get the closest triangle to the given point by computing the distance for all facets
        double minDistance = Double.POSITIVE_INFINITY;
        Triangle closestTriangle = null;
        // Loop on all triangles
        for (final Triangle triangle : this.triangles) {
            // Compute the distance between the current triangle and the given point
            final double distance = triangle.distanceTo(pos);
            // Check if the computed distance is less than the last minimum distance
            if (distance < minDistance) {
                // The new minimum distance is the current one
                minDistance = distance;
                // The new closest triangle is the current one
                closestTriangle = triangle;
            }
        }

        // Return the neighbors
        return getNeighbors(closestTriangle, pos, maxDistance, Integer.MAX_VALUE);
    }

    /**
     * Getter for the neighbor triangles of provided cartesian point which are closer or equal to provided order of
     * "neighborhood". For example:
     * <ul>
     * <li>Order 0 returns closest triangle</li>
     * <li>Order 1 returns closest triangle and the immediate neighbors of closest triangle</li>
     * <li>Order 2 returns closest triangle, the immediate neighbors of closest triangle and also their own immediate
     * neighbors</li>
     * </ul>
     * <p>
     * Beware not to confuse this method with {@link #getNeighbors(Vector3D, double)}
     * </p>
     *
     * @param pos
     *        a point in body frame
     * @param order
     *        order of "neighborhood"
     * @return the neighbor triangles of provided cartesian point which are closer or equal to provided order of
     *         "neighborhood"
     */
    public List<Triangle> getNeighbors(final Vector3D pos, final int order) {
        // Get the closest triangle to the given point by computing the distance for all facets
        double minDistance = Double.POSITIVE_INFINITY;
        Triangle closestTriangle = null;
        // Loop on all triangles
        for (final Triangle triangle : this.triangles) {
            // Compute the distance between the current triangle and the given point
            final double distance = triangle.distanceTo(pos);
            // Check if the computed distance is less than the last minimum distance
            if (distance < minDistance) {
                // The new minimum distance is the current one
                minDistance = distance;
                // The new closest triangle is the current one
                closestTriangle = triangle;
            }
        }

        // Return the neighbors
        return getNeighbors(closestTriangle, pos, Double.POSITIVE_INFINITY, order);
    }

    /**
     * Getter for the neighbors of provided triangle whose center is closer than provided distance of provided fitted
     * point
     * AND closer in terms of "neighborhood" order.
     * <p>
     * Provided triangle is included in the list of neighbors.
     * </p>
     *
     * @param triangle
     *        a triangle
     * @param referencePoint
     *        fitted point from which distance are computed
     * @param maxDistance
     *        max distance
     * @param order
     *        order of "neighborhood"
     * @return the neighbors of provided triangle whose center is closer than provided distance of provided fitted point
     *         AND closer in terms of "neighborhood" order
     */
    private List<Triangle> getNeighbors(final Triangle triangle, final Vector3D referencePoint,
                                        final double maxDistance, final int order) {

        final double maxDistance2 = maxDistance * maxDistance;

        // Reset all triangles
        for (final Triangle t : this.triangles) {
            t.setHandled(false);
        }

        // List of remaining triangles to treat in recursive algorithm
        final List<Triangle> remainingTriangles = new ArrayList<>();
        final List<Integer> remainingOrders = new ArrayList<>();
        remainingTriangles.add(triangle);
        triangle.setHandled(true);
        remainingOrders.add(0);

        // Iterative algorithm starting from neighbors of provided triangle
        final List<Triangle> res = new ArrayList<>();
        while (!remainingTriangles.isEmpty()) {
            final Triangle t = remainingTriangles.get(0);
            final int o = remainingOrders.get(0);
            remainingTriangles.remove(0);
            remainingOrders.remove(0);
            if (t.getCenter().distanceSq(referencePoint) <= maxDistance2 && o <= order) {
                res.add(t);
                // Add neighbors only if not treated yet
                // Neighbors are an order further of provided triangle
                for (final Triangle neighbor : t.getNeighbors()) {
                    if (!neighbor.isHandled()) {
                        remainingTriangles.add(neighbor);
                        remainingOrders.add(o + 1);
                        neighbor.setHandled(true);
                    }
                }
            }
        }

        return res;
    }

    /**
     * Getter for the field data as {@link FieldData} for each state in provided list.
     *
     * @param state
     *        spacecraft state
     * @param fieldOfView
     *        sensor field of view
     * @param lineOfSight
     *        optional parameter in order to indicate field of view main line of sight in spacecraft frame. If provided,
     *        this parameter may fasten algorithm by several order of magnitudes (O(log n) vs O(n)). Be careful, in this
     *        case, strongly not convex bodies may lead to missing some triangles.
     * @return {@link FieldData} for corresponding state
     * @throws PatriusException
     *         if state position could not be retrieved in body frame
     */
    public FieldData getFieldData(final SpacecraftState state, final IFieldOfView fieldOfView,
                                  final Vector3D lineOfSight)
        throws PatriusException {

        // Transform from body frame to spacecraft frame and inverse
        final Transform t = new Transform(state.getDate(), state.getAttitude(getBodyFrame()).getRotation());
        final Transform tInv = t.getInverse();
        // Position in body frame
        final Vector3D pos = state.getPVCoordinates(getBodyFrame()).getPosition();

        // Check if fast algorithm can be used (if line of sight has been provided and intersect
        // body)
        boolean useFastAlgorithm = false;
        Intersection intersection = null;
        if (lineOfSight != null) {
            // Get intersection between body and main line of sight
            final Vector3D lineOfSightBodyFrame = tInv.transformVector(lineOfSight);
            final Line line = new Line(pos, pos.add(lineOfSightBodyFrame.scalarMultiply(DIRECTION_FACTOR)));
            intersection = getIntersection(line, pos);
            useFastAlgorithm = intersection != null;
        }

        // List of visible triangles
        final List<Triangle> visibleTriangles = new ArrayList<>();

        if (useFastAlgorithm) {
            // Fast algorithm can be used
            visibleTriangles.addAll(getFastVisibleTriangles(intersection, fieldOfView, pos, t));
        } else {
            // Standard algorithm (loop on all triangles)
            for (final Triangle triangle : this.triangles) {
                // A triangle is visible if in the field of view and oriented toward the field of
                // view
                if (isVisible(triangle, pos, fieldOfView, t)) {
                    visibleTriangles.add(triangle);
                }
            }
        }

        // Build field data
        return new FieldData(state.getDate(), visibleTriangles, this);
    }

    /**
     * Getter for the list of visible triangles in the field of view using a fast iterative algorithm starting from the
     * triangle at the center of the field of view.
     *
     * @param intersection
     *        intersection between line of sight and body
     * @param fieldOfView
     *        field of view
     * @param pos
     *        spacecraft position in body frame
     * @param t
     *        transform from body frame to spacecraft frame
     * @return list of visible triangles in the field of view
     * @throws PatriusException
     *         if failed to compute masked triangles
     */
    private List<Triangle> getFastVisibleTriangles(final Intersection intersection, final IFieldOfView fieldOfView,
                                                   final Vector3D pos, final Transform t)
        throws PatriusException {

        // List of remaining triangles to treat in recursive algorithm
        final List<Triangle> remainingTriangles = new ArrayList<>();
        remainingTriangles.add(intersection.getTriangle());

        // Reset all triangles
        for (final Triangle triangle : this.triangles) {
            triangle.setHandled(false);
        }

        // Iterative algorithm starting from neighbors of provided triangle
        intersection.getTriangle().setHandled(true);
        final List<Triangle> res = new ArrayList<>();
        int index = 0;
        while (index < remainingTriangles.size()) {
            final Triangle triangle = remainingTriangles.get(index);
            index++;
            if (isVisible(triangle, pos, fieldOfView, t)) {
                res.add(triangle);
                // Add neighbors only if not treated yet
                final List<Triangle> neighbors = triangle.getNeighbors();
                for (final Triangle neighbor : neighbors) {
                    if (!neighbor.isHandled()) {
                        remainingTriangles.add(neighbor);
                        neighbor.setHandled(true);
                    }
                }
            }
        }

        return res;
    }

    /**
     * Returns true if the triangle is visible from the field of view, i.e.:
     * <ul>
     * <li>Is oriented toward the field of view</li>
     * <li>All points of the triangle are in the field of view</li>
     * <li>Triangle not masked by another facet</li>
     * </ul>
     *
     * @param triangle
     *        a triangle
     * @param fieldOfView
     *        field of view
     * @param pos
     *        spacecraft position in body frame
     * @param t
     *        transform from body frame to spacecraft frame
     * @return true if the triangle is visible from the field of view
     * @throws PatriusException
     *         if intersection computation failed
     */
    public boolean isVisible(final Triangle triangle, final Vector3D pos, final IFieldOfView fieldOfView,
                             final Transform t)
        throws PatriusException {

        // Check orientation
        boolean res = triangle.isVisible(pos);
        if (res) {
            // Check all triangles vertices are in the field (if provided)
            if (fieldOfView != null) {
                for (final Vertex v : triangle.getVertices()) {
                    final Vector3D vertexDirInBodyFrame = v.getPosition().subtract(pos);
                    final Vector3D vertexDirInSensorFrame = t.transformVector(vertexDirInBodyFrame);
                    res &= fieldOfView.isInTheField(vertexDirInSensorFrame);
                }
            }

            // Check potential masking
            if (res) {
                res = !isMasked(triangle, pos);
            }
        }
        return res;
    }

    /**
     * Returns true if the triangle is masked by another triangle as seen from the provided position. A triangle is
     * masked if all its vertices are masked by at least another triangle.
     *
     * @param triangle
     *        a triangle
     * @param pos
     *        spacecraft position in body frame
     * @return true if the triangle is masked by another triangle as seen from the provided position
     * @throws PatriusException
     *         if intersection computation failed
     */
    public boolean isMasked(final Triangle triangle, final Vector3D pos) throws PatriusException {

        for (final Vertex v : triangle.getVertices()) {
            // Compute potential intersection point
            final Line line = new Line(pos, v.getPosition());
            final FacetPoint[] intersections = getIntersectionPoints(line, getBodyFrame(), null);
            boolean res = false;
            for (final FacetPoint intersection : intersections) {
                // Discard current vertex which is an obvious intersection point
                if (intersection.getPosition().distanceSq(v.getPosition()) > EPSILON2) {
                    // Vertex is hidden if intersection point is between vertex and position
                    final Vector3D v1 = v.getPosition().subtract(intersection.getPosition());
                    final Vector3D v2 = pos.subtract(intersection.getPosition());
                    res |= Triangle.dotProduct(v1, v2) < 0;
                }
            }
            if (!res) {
                // One vertex is not masked: no masking
                return false;
            }
        }
        // All vertices are masked
        return true;
    }

    /**
     * Returns true if provided position in provided frame at provided date in in eclipse or not.
     * <p>
     * Computed eclipse status is exact and takes into account full body shape. This method cannot however be used in
     * conjunction with {@link EclipseDetector} since they use a different framework.
     * </p>
     * <p>
     * This method uses the two following hypotheses: the propagation of the Sun light is instantaneous and the Sun is a
     * point source. Please note that the apparent angle of the Sun as seen from the Earth is about 0.5 degrees, while
     * the one seen from Mars is, to the nearest, roughly 0.38 degrees.
     * </p>
     *
     * @param date
     *        date
     * @param position
     *        position
     * @param frame
     *        frame in which position is expressed
     * @param sun
     *        Sun body
     * @return true if provided position in provided frame at provided date in in eclipse or not
     * @throws PatriusException
     *         if failed to retrieve Sun position of intersection points with body
     */
    public boolean isInEclipse(final AbsoluteDate date, final Vector3D position, final Frame frame,
                               final PVCoordinatesProvider sun)
        throws PatriusException {

        // Retrieve the position of the Sun
        final Vector3D sunPos = sun.getPVCoordinates(date, frame).getPosition();
        // Define the line connecting the given position and the Sun position
        final Line line = new Line(position, sunPos);
        // Transform the provided position in body frame
        final Transform toBodyFrame = frame.getTransformTo(getBodyFrame(), date);
        // Transform the position in the body frame
        final Vector3D posInBodyFrame = toBodyFrame.transformPosition(position);
        // Compute the intersection
        final Intersection intersection = getIntersection(line, posInBodyFrame, frame, date);
        // In eclipse if intersection point with (pos, Sun) vector is not null and body is between
        // satellite and Sun
        boolean res = false;
        // Check if there are intersections
        if (intersection != null) {
            // Compute the relative given position with respect to the intersection point
            final Vector3D posToIntersection = position.subtract(intersection.getPoint());
            // Compute the relative position of the Sun with respect to the intersection point
            final Vector3D sunToIntersection = sunPos.subtract(intersection.getPoint());
            res = Vector3D.dotProduct(posToIntersection, sunToIntersection) < 0;
        }
        // Return the boolean for the eclipse
        return res;
    }

    /**
     * Getter for the list of triangles never visible from the satellite field of view during the whole ephemeris. A
     * visible triangle must be properly oriented, in the field of view and not masked by any other triangle.
     *
     * @param states
     *        list of spacecraft states
     * @param fieldOfView
     *        field of view
     * @return list of {@link Triangle} never visible from the satellite field of view during the whole ephemeris
     * @throws PatriusException
     *         if state position could not be retrieved in body frame
     */
    public List<Triangle> getNeverVisibleTriangles(final List<SpacecraftState> states, final IFieldOfView fieldOfView)
        throws PatriusException {

        // Initially add all triangles
        final List<Triangle> res = new ArrayList<>();
        for (final Triangle triangle : this.triangles) {
            res.add(triangle);
        }

        // Remove triangles not matching conditions
        for (final SpacecraftState state : states) {
            // Transform from body frame to spacecraft frame
            final Transform t = new Transform(state.getDate(), state.getAttitude(getBodyFrame()).getRotation());
            // Position in body frame
            final Vector3D pos = state.getPVCoordinates(getBodyFrame()).getPosition();
            for (int i = res.size() - 1; i >= 0; i--) {
                final Triangle triangle = res.get(i);
                if (isVisible(triangle, pos, fieldOfView, t)) {
                    res.remove(triangle);
                }
            }
        }

        return res;
    }

    /**
     * Getter for the list of triangles never enlightened by the Sun at provided dates. An enlightened triangle must be
     * properly oriented toward the Sun and not masked by any other triangle.
     *
     * @param dates
     *        list of dates
     * @param sun
     *        Sun body
     * @return list of {@link Triangle} never enlightened by the Sun
     * @throws PatriusException
     *         if state position could not be retrieved in body frame
     */
    public List<Triangle> getNeverEnlightenedTriangles(final List<AbsoluteDate> dates, final PVCoordinatesProvider sun)
        throws PatriusException {

        // Initially add all triangles
        final List<Triangle> res = new ArrayList<>();
        for (final Triangle triangle : this.triangles) {
            res.add(triangle);
        }

        // Remove triangles not matching conditions
        for (final AbsoluteDate date : dates) {
            // Position in body frame
            final Vector3D pos = sun.getPVCoordinates(date, getBodyFrame()).getPosition();
            for (int i = res.size() - 1; i >= 0; i--) {
                final Triangle triangle = res.get(i);
                if (isVisible(triangle, pos, null, null)) {
                    res.remove(triangle);
                }
            }
        }

        return res;
    }

    /**
     * Getter for the list of triangles enlightened (by the Sun) and visible at least once during the whole ephemeris
     * from
     * the satellite field of view. A visible triangle must be properly oriented, in the field of view and not masked by
     * any other triangle.<br>
     * An enlightened triangle must be properly oriented toward the Sun and not masked by any other triangle.
     *
     * @param states
     *        list of spacecraft states
     * @param sun
     *        sun
     * @param fieldOfView
     *        field of view
     * @return list of {@link Triangle} enlightened and visible at least once from the satellite field of view
     * @throws PatriusException
     *         if state position could not be retrieved in body frame
     */
    public List<Triangle> getVisibleAndEnlightenedTriangles(final List<SpacecraftState> states,
                                                            final PVCoordinatesProvider sun,
                                                            final IFieldOfView fieldOfView)
        throws PatriusException {

        // Initialization
        final List<Triangle> res = new ArrayList<>();

        // Add triangles matching conditions
        for (final SpacecraftState state : states) {
            // Transform from body frame to spacecraft frame
            final Transform t = new Transform(state.getDate(), state.getAttitude(getBodyFrame()).getRotation());
            // Position in body frame
            final Vector3D pos = state.getPVCoordinates(getBodyFrame()).getPosition();
            // Sun position in body frame
            final Vector3D sunPos = sun.getPVCoordinates(state.getDate(), getBodyFrame()).getPosition();

            for (final Triangle triangle : this.triangles) {
                if (!res.contains(triangle)) {
                    if (isVisible(triangle, pos, fieldOfView, t) && isVisible(triangle, sunPos, null, null)) {
                        res.add(triangle);
                    }
                }
            }
        }

        return res;
    }

    /**
     * Getter for the distance from center to closest vertex to center of body.
     *
     * @return the distance from center to closest vertex to center of body
     */
    public double getMinNorm() {
        return this.minNorm;
    }

    /**
     * Getter for the distance from center to farthest vertex to center of body.
     *
     * @return the distance from center to farthest vertex to center of body
     */
    public double getMaxNorm() {
        return this.maxNorm;
    }

    /**
     * Getter for the threshold for apparent radius determination convergence.
     *
     * @return the threshold for apparent radius determination convergence
     */
    public double getThreshold() {
        return this.threshold;
    }

    /**
     * Getter for the mesh provider.
     *
     * @return the mesh provider
     */
    public MeshProvider getMeshProvider() {
        return this.meshProvider;
    }

    /**
     * Getter for the maximum angle between the normal to a facet of the body and the vector from the
     * origin to the centre of the facet.
     *
     * @return the maxSlope
     */
    public double getMaxSlope() {
        return this.maxSlope;
    }

    /**
     * Getter for the type of ellipsoid to apply transformation methods on.
     *
     * @return the maxSlope
     * @deprecated as of 4.12, any type of ellipsoid can be retrieved using {@link #getEllipsoid(EllipsoidType)}.
     */
    @Deprecated
    public EllipsoidType getEllipsoidType() {
        return this.ellipsoidType;
    }

    /**
     * Setter for the threshold for apparent radius determination convergence.
     *
     * @param threshold
     *        the threshold for apparent radius determination convergence to set
     */
    public void setThreshold(final double threshold) {
        this.threshold = threshold;
    }

    /**
     * Resize the geometric body shape by a margin.
     *
     * @param marginType
     *        margin type to be used
     * @param marginValue
     *        margin value to be used (in meters if the margin type is DISTANCE)
     * @return resized geometric body shape with the margin
     * @throws IllegalArgumentException
     *         if the margin value is invalid
     */
    @Override
    public FacetBodyShape resize(final MarginType marginType, final double marginValue) {

        // Define map of vertices
        final Map<Integer, Vertex> newVerticesMap = new ConcurrentHashMap<>();
        // Retrieve triangles
        final Triangle[] trianglesArray = getTriangles();
        // Initialize new triangles
        final Triangle[] newTriangles = new Triangle[trianglesArray.length];
        // Initialize new vertices of each triangle
        final Vertex[] newVertices = new Vertex[3];
        // Check the margin type
        if (marginType.equals(MarginType.DISTANCE)) {
            // The margin type is distance
            // Check if the margin value is larger than the min norm, to be sure that the margin distance has a physical
            // meaning
            if (marginValue > -getMinNorm()) {
                // Loop on triangles
                for (int i = 0; i < trianglesArray.length; i++) {
                    // Retrieve vertices of the current triangle
                    final Vertex[] currentVertices = trianglesArray[i].getVertices();
                    // Loop on vertices of the current triangle
                    for (int j = 0; j < currentVertices.length; j++) {
                        // Modify vertices of the current triangle
                        if (currentVertices[j].getPosition().getNorm() != 0) {
                            newVertices[j] = new Vertex(currentVertices[j].getID(), currentVertices[j].getPosition()
                                .normalize()
                                .scalarMultiply(currentVertices[j].getPosition().getNorm() + marginValue));
                            newVerticesMap.put(newVertices[j].getID(), newVertices[j]);
                        } else {
                            newVertices[j] = currentVertices[j];
                        }
                    }
                    // Modify vertices of the current triangle
                    newTriangles[i] = new Triangle(trianglesArray[i].getID(), newVertices[0], newVertices[1],
                        newVertices[2]);
                }
            } else {
                // Invalid margin value
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INVALID_MARGIN_VALUE, marginValue);
            }
        } else {
            // The margin type is scale factor
            // Check if the margin value is positive, to be sure that the scale factor has a physical meaning
            if (marginValue > 0) {
                // Loop on triangles
                for (int i = 0; i < trianglesArray.length; i++) {
                    // Retrieve vertices of the current triangle
                    final Vertex[] currentVertices = trianglesArray[i].getVertices();
                    // Loop on vertices of the current triangle
                    for (int j = 0; j < currentVertices.length; j++) {
                        // Modify vertices of the current triangle
                        newVertices[j] = new Vertex(currentVertices[j].getID(), currentVertices[j].getPosition()
                            .scalarMultiply(marginValue));
                        newVerticesMap.put(newVertices[j].getID(), newVertices[j]);
                    }
                    // Modify vertices of the current triangle
                    newTriangles[i] = new Triangle(trianglesArray[i].getID(), newVertices[0], newVertices[1],
                        newVertices[2]);
                }
            } else {
                // Invalid margin value
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.INVALID_MARGIN_VALUE, marginValue);
            }
        }

        // Build consistent mesh provider
        final MeshProvider newMeshProvider = new MeshProvider(){

            /** Serializable UID. */
            private static final long serialVersionUID = 5527270072660559626L;

            /** {@inheritDoc} */
            @Override
            public Triangle[] getTriangles() {

                return newTriangles;
            }

            /** {@inheritDoc} */
            @Override
            public Map<Integer, Vertex> getVertices() {

                return newVerticesMap;
            }
        };
        // Copy and return the new FacetBodyShape with modified vertices of triangles FacetBodyShape
        return new FacetBodyShape(getName(), getBodyFrame(), this.ellipsoidType, newMeshProvider);
    }

    /**
     * Setter for the maximum number of steps in the while loop of
     * {@link #getApparentRadius(PVCoordinatesProvider, AbsoluteDate, PVCoordinatesProvider, PropagationDelayType)}
     * method.
     *
     * @param newLimit
     *        new maximum number of steps for apparent radius computation
     */
    public void setMaxApparentRadiusSteps(final int newLimit) {
        this.maxApparentRadiusSteps = newLimit;
    }

    /**
     * Custom deserialization is needed.
     *
     * @param stream
     *        Object stream
     * @throws IOException
     *         if an I/O error occurs
     * @throws ClassNotFoundException
     *         if the class of a serialized object cannot be found
     */
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {

        stream.defaultReadObject();
        // manually deserialize + reload the triangles and link them to each other
        this.triangles = this.meshProvider.getTriangles();
        this.tree = new TrianglesSet(this.triangles);
        linkTriangles();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This method is exact and in O(n).
     * </p>
     *
     * The returned facet points are associated with one or several triangles following the closest point on shape
     * position:
     * <ul>
     * <li>3 triangles or more if the closest point on shape corresponds to one shape vertex,</li>
     * <li>2 triangles if the closest point on shape belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     */
    @Override
    public FacetPoint[] closestPointTo(final Line line, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Convert line to body frame
        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        final Line lineInBodyFrame = t.transformLine(line);

        return closestPointTo(lineInBodyFrame);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This method is exact and in O(n).
     * </p>
     *
     * The returned facet points are associated with one or several triangles following the closest point on shape
     * position:
     * <ul>
     * <li>3 triangles or more if the closest point on shape corresponds to one shape vertex,</li>
     * <li>2 triangles if the closest point on shape belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     */
    @Override
    public FacetPoint[] closestPointTo(final Line line) {
        
        // Compute distance to all facets
        double minDistance = Double.POSITIVE_INFINITY;
        Vector3D[] closestPoints = null;
        List<Triangle> closestTriangles = null;

        for (final Triangle triangle : this.triangles) {
            final double distLineCenter = line.distance(triangle.getCenter());

            if (distLineCenter <= triangle.getSphereRadius() + minDistance) {

                final Vector3D[] closestPointsTriangle = triangle.closestPointTo(line);
                final double dist = Vector3D.distance(closestPointsTriangle[0], closestPointsTriangle[1]);

                if (dist < minDistance) {
                    // Current points are closer

                    closestPoints = closestPointsTriangle;
                    closestTriangles = new ArrayList<>();

                    minDistance = dist;

                } else if (Double.compare(dist, minDistance) == 0.) {
                    // line - body intersection case: take point of line with min abscissa

                    if (line.getAbscissa(closestPointsTriangle[0]) < line.getAbscissa(closestPoints[0])) {

                        closestPoints = closestPointsTriangle;
                        closestTriangles = new ArrayList<>();
                        closestTriangles.add(triangle);

                    } else if (line.getAbscissa(closestPointsTriangle[0]) == line.getAbscissa(closestPoints[0])) {
                        // same points on the line and on the shape: add triangle to current optimal intersection

                        closestTriangles.add(triangle);
                    }
                }
            }
        }

         // closest point on shape as a FacetPoint known to be on the shape surface
        final FacetPoint closestOnShape = new FacetPoint(this, closestPoints[1], closestTriangles, true,
                BodyPointName.CLOSEST_ON_SHAPE);

        // closest point on line as a FacetPoint whose closest point on shape is closestOnShape
        final FacetPoint closestOnLine = new FacetPoint(this, closestPoints[0], BodyPointName.CLOSEST_ON_LINE);
        closestOnLine.setClosestPointOnShape(closestOnShape);

        return new FacetPoint[] { closestOnLine, closestOnShape };
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned facet point is associated with one or several triangles following the closest point position on the
     * shape:
     * <ul>
     * <li>3 triangles or more if the closest point corresponds to one shape vertex,</li>
     * <li>2 triangles if the closest point belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     * </p>
     * Note: If several points on the shape are at the exact same minimal distance with respect to entered point, the
     * method arbitrarily returns the first one.
     */
    @Override
    public FacetPoint closestPointTo(final Vector3D point, final Frame frame, final AbsoluteDate date)
        throws PatriusException {
        // Convert point to body frame
        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        final Vector3D pointInBodyFrame = t.transformPosition(point);

        return closestPointTo(pointInBodyFrame);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned facet point is associated with one or several triangles following the closest point position on the
     * shape:
     * <ul>
     * <li>3 triangles or more if the closest point corresponds to one shape vertex,</li>
     * <li>2 triangles if the closest point belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     * </p>
     * Note: If several points on the shape are at the exact same minimal distance with respect to entered point, the
     * method arbitrarily returns the first one.
     */
    @Override
    public FacetPoint closestPointTo(final Vector3D point) {
        return closestPointTo(point, BodyPointName.CLOSEST_ON_SHAPE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The returned facet point is associated with one or several triangles following the closest point position on the
     * shape:
     * <ul>
     * <li>3 triangles or more if the closest point corresponds to one shape vertex,</li>
     * <li>2 triangles if the closest point belongs to one shape segment,</li>
     * <li>1 triangle in other cases.</li>
     * </ul>
     * </p>
     * Note: If several points on the shape are at the exact same minimal distance with respect to entered point, the
     * method arbitrarily returns the first one.
     */
    @Override
    public FacetPoint closestPointTo(final Vector3D point, final String nameIn) {

        // Compute distance to all facets
        double minDistance = Double.POSITIVE_INFINITY;
        Vector3D closestOnShape = null;
        List<Triangle> closestTriangles = null;
        for (final Triangle triangle : this.triangles) {
            if (Vector3D.distance(point, triangle.getCenter()) <= triangle.getSphereRadius() + minDistance) {
                final Vector3D closestOnTriangle = triangle.closestPointTo(point);
                final double dist = Vector3D.distance(point, closestOnTriangle);
                if (Double.compare(dist, minDistance) == 0) {
                    // same distance as minDistance: add the triangle to current list if it is the same closest point on
                    // triangle
                    if (Vector3D.distance(closestOnTriangle, closestOnShape) < this.distanceEpsilon) {
                        closestTriangles.add(triangle);
                    }

                } else if (dist < minDistance) {
                    // Current points are closer
                    closestOnShape = closestOnTriangle;
                    minDistance = dist;

                    // initialize the list with the triangle
                    closestTriangles = new ArrayList<>();
                    closestTriangles.add(triangle);
                }
            } // else the current triangle cannot be closest: continue with next triangle
        }

        // Return closest point as FacetBodyPoint known to be on the shape surface
        return new FacetPoint(this, closestOnShape, closestTriangles, true, nameIn);
    }

    /** {@inheritDoc} */
    @Override
    public FacetPoint buildPoint(final LLHCoordinatesSystem coordSystem, final double latitude, final double longitude,
                                 final double height, final String nameIn) {
        return new FacetPoint(this, coordSystem, latitude, longitude, height, nameIn);
    }

    /** {@inheritDoc} */
    @Override
    public FacetPoint buildPoint(final Vector3D position, final String nameIn) {
        return new FacetPoint(this, position, nameIn);
    }

    /** {@inheritDoc} */
    @Override
    public FacetPoint buildPoint(final Vector3D position, final Frame frame, final AbsoluteDate date,
                                 final String nameIn) throws PatriusException {
        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        return buildPoint(t.transformPosition(position), nameIn);
    }

    /** {@inheritDoc} */
    @Override
    public double getEncompassingSphereRadius() {
        return getMaxNorm();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefaultLLHCoordinatesSystem() {
        return this.lLHCoordinatesSystem == DEFAULT_LLH_COORD_SYSTEM;
    }
}
