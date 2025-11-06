/**
 *
 * Copyright 2011-2024 CNES
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
 * VERSION:4.14:OPENFD-136:22/08/2024: [PATRIUS] Fitting d'un ThreeAxisEllipsoid sur un FacetBodyShape
 * VERSION:4.14:OPENFD-253:22/08/2024: [PATRIUS] Problemes e l'utilisation des bsp planetaires
 * VERSION:4.14:OPENFD-292:22/08/2024: Implementation de multi-propagateurs mixtes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import java.util.Map;

import fr.cnes.sirius.patrius.bodies.AbstractEllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.ThreeAxisEllipsoid;
import fr.cnes.sirius.patrius.math.analysis.MultivariateFunction;
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
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Body shape fitter, allowing to build shapes fitted on the main Shape. The fitting criteria are described in the
 * associated methods.
 * <p>
 * This class offers an optimal internal caching strategy to improve speed, by storing in cache the fitted ellipsoid
 * computed. Each of them is thus computed once.
 * </p>
 * <p>
 * This class implements the interface {@link BodyShape}
 * </p>
 *
 * @author Manuel Amouroux
 *
 * @since 4.14
 */

public class BodyShapeFitter {

    /** Object not supported string type */
    private static final String OBJ_NOT_SUPP = " object is not supported at the moment.";

    /** Epsilon for fitted ellipsoid computation. */
    private static final double EPS_OPT = 1E-8;

    /** First guess of the flattening value for the optimizer. Flattening value is between 0 and 1. */
    private static final double FIRST_GUESS_FLATTENING = 1E-1;

    /** Maximum number of criterion evaluation for fitted ellipsoid computation. */
    private static final int MAX_EVAL = 1000;
    
    /** Body shape to fit ellipsoids to. */
    private final BodyShape bodyShape;

    /** Type of the body shape to fit ellipsoids to. */
    private final SupportedBodyShapeType bodyShapeType;

    /** Fitted sphere, computed via volumetric approach. */
    private OneAxisEllipsoid fittedSphere;

    /** Inner sphere which is the largest sphere strictly contained in the mesh and centered around (0, 0, 0). */
    private OneAxisEllipsoid innerSphere;

    /** Outer sphere which is the smallest sphere englobing the shape and centered around (0, 0, 0). */
    private OneAxisEllipsoid outerSphere;

    /** Fitted ellipsoid which is the one-axis ellipsoid (a, f) which minimizes the distance to all vertices. */
    private OneAxisEllipsoid fittedOneAxisEllipsoid;

    /**
     * Inner ellipsoid which is the largest one-axis ellipsoid strictly contained in the mesh and centered around (0, 0,
     * 0).
     */
    private OneAxisEllipsoid innerOneAxisEllipsoid;

    /** Outer ellipsoid which is the smallest one-axis ellipsoid englobing the shape and centered around (0, 0, 0). */
    private OneAxisEllipsoid outerOneAxisEllipsoid;

    /** Fitted ellipsoid which is the three-axis ellipsoid which minimizes the distance to all vertices. */
    private ThreeAxisEllipsoid fittedThreeAxisEllipsoid;

    /**
     * Inner ellipsoid which is the largest three-axis ellipsoid strictly contained in the mesh and centered around (0,
     * 0, 0).
     */
    private ThreeAxisEllipsoid innerThreeAxisEllipsoid;

    /** Outer ellipsoid which is the smallest three-axis ellipsoid englobing the shape and centered around (0, 0, 0). */
    private ThreeAxisEllipsoid outerThreeAxisEllipsoid;

    /** Type of ellipsoid to apply transformation methods on. */
    public enum EllipsoidType {
        /** Fitted sphere. */
        SPHERE_FITTED,
        /** Inner sphere. */
        SPHERE_INNER,
        /** Outer sphere. */
        SPHERE_OUTER,
        /** Fitted one-axis ellipsoid. */
        ONE_AXIS_ELLIPSOID_FITTED,
        /** Inner one-axis ellipsoid. */
        ONE_AXIS_ELLIPSOID_INNER,
        /** Outer one-axis ellipsoid. */
        ONE_AXIS_ELLIPSOID_OUTER,
        /** Fitted three-axis ellipsoid. */
        THREE_AXIS_ELLIPSOID_FITTED,
        /** Inner three-axis ellipsoid. */
        THREE_AXIS_ELLIPSOID_INNER,
        /** Outer three-axis ellipsoid. */
        THREE_AXIS_ELLIPSOID_OUTER;
    }

    /** Type of body shape to fit the ellipsoid to. */
    private enum SupportedBodyShapeType {
        /** Sphere. */
        SPHERE,
        /** One-axis ellipsoid. */
        ONE_AXIS_ELLIPSOID,
        /** Three-axis ellipsoid. */
        THREE_AXIS_ELLIPSOID,
        /** Facet body shape. */
        FACET_BODY_SHAPE,
        /** Other type, unsupported at the moment. */
        OTHER;
    }

    /**
     * Constructor.
     *
     * @param bodyShape
     *        bodyShape to fit ellipsoids to
     */
    public BodyShapeFitter(final BodyShape bodyShape) {
        this.bodyShape = bodyShape;

        if (bodyShape instanceof OneAxisEllipsoid) {
            if (Precision.equals(((OneAxisEllipsoid) bodyShape).getFlattening(), 0.)) {
                // Flattening is nil meaning the body shape is a sphere
                this.bodyShapeType = SupportedBodyShapeType.SPHERE;

            } else {
                // Flattening is not nil meaning the body shape is an ellipse
                this.bodyShapeType = SupportedBodyShapeType.ONE_AXIS_ELLIPSOID;
            }

        } else if (this.bodyShape instanceof ThreeAxisEllipsoid) {
            this.bodyShapeType = SupportedBodyShapeType.THREE_AXIS_ELLIPSOID;

        } else if (this.bodyShape instanceof FacetBodyShape) {
            this.bodyShapeType = SupportedBodyShapeType.FACET_BODY_SHAPE;
        } else {
            this.bodyShapeType = SupportedBodyShapeType.OTHER;
        }
    }

    /**
     * Getter for the ellipsoid of the desired type.
     * Once computed, the required ellipsoid is stored for future use.
     * 
     * @param ellipsoidTypeIn
     *        the type of the ellipsoid to be returned
     * @return the desired ellipsoid
     */
    public <T extends AbstractEllipsoidBodyShape> T getEllipsoid(final EllipsoidType ellipsoidTypeIn) {
        
        final AbstractEllipsoidBodyShape ellipsoid;
        
        switch (ellipsoidTypeIn) {
            case SPHERE_FITTED:
                // The inner sphere
                // If the inner sphere is null, build it
                if (this.fittedSphere == null) {
                    this.fittedSphere = getFittedSphere();
                }
                ellipsoid = this.fittedSphere;
                break;
            case SPHERE_INNER:
                // The inner sphere
                // If the inner sphere is null, build it
                if (this.innerSphere == null) {
                    this.innerSphere = getInnerSphere();
                }
                ellipsoid = this.innerSphere;
                break;
            case SPHERE_OUTER:
                // The outer sphere
                // If the outer sphere is null, build it
                if (this.outerSphere == null) {
                    this.outerSphere = getOuterSphere();
                }
                ellipsoid = this.outerSphere;
                break;
            case ONE_AXIS_ELLIPSOID_FITTED:
                // The fitted one-axis ellipsoid
                // If the fitted one-axis ellipsoid is null, build it
                if (this.fittedOneAxisEllipsoid == null) {
                    this.fittedOneAxisEllipsoid = getFittedOneAxisEllipsoid();
                }
                ellipsoid = this.fittedOneAxisEllipsoid;
                break;
            case ONE_AXIS_ELLIPSOID_INNER:
                // The inner one-axis ellipsoid
                // If the inner ellione-axis ellipsoidpsoid is null, build it
                if (this.innerOneAxisEllipsoid == null) {
                    this.innerOneAxisEllipsoid = getInnerOneAxisEllipsoid();
                }
                ellipsoid = this.innerOneAxisEllipsoid;
                break;
            case ONE_AXIS_ELLIPSOID_OUTER:
                // The outer one-axis ellipsoid
                // If the outer ellone-axis ellipsoidipsoid is null, build it
                if (this.outerOneAxisEllipsoid == null) {
                    this.outerOneAxisEllipsoid = getOuterOneAxisEllipsoid();
                }
                ellipsoid = this.outerOneAxisEllipsoid;
                break;
            case THREE_AXIS_ELLIPSOID_FITTED:
                // The fitted three-axis ellipsoid
                // If the fitted three-axis ellipsoid is null, build it
                if (this.fittedThreeAxisEllipsoid == null) {
                    this.fittedThreeAxisEllipsoid = getFittedThreeAxisEllipsoid();
                }
                ellipsoid = this.fittedThreeAxisEllipsoid;
                break;
            case THREE_AXIS_ELLIPSOID_INNER:
                // The inner three-axis ellipsoid
                // If the inner three-axis ellipsoid is null, build it
                if (this.innerThreeAxisEllipsoid == null) {
                    this.innerThreeAxisEllipsoid = getInnerThreeAxisEllipsoid();
                }
                ellipsoid = this.innerThreeAxisEllipsoid;
                break;
            case THREE_AXIS_ELLIPSOID_OUTER:
                // The outer three-axis ellipsoid
                // If the outer three-axis ellipsoid is null, build it
                if (this.outerThreeAxisEllipsoid == null) {
                    this.outerThreeAxisEllipsoid = getOuterThreeAxisEllipsoid();
                }
                ellipsoid = this.outerThreeAxisEllipsoid;
                break;
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        
        return (T) ellipsoid;
        
    }

    /**
     * Build fitted sphere centered at (0, 0, 0), with a volumetric approach.<br>
     * <p>
     * The radius of the fitted sphere is calculated depending on what kind of object the fitted sphere is fitted to :
     * <ul>
     * <li>fitted to a sphere: same radius</li>
     * <li>fitted to an ellipsoid: the radius equals the product of the semi-major axes (r = a*b*c)</li>
     * <li>fitted to a facet body shape: optimised radius such as the absolute difference of the difference between the
     * volume of the facet body shape and that of the fitted sphere is minimized</li>
     * </ul>
     * </p>
     *
     * @return fitted sphere
     */
    private OneAxisEllipsoid getFittedSphere() {
        final OneAxisEllipsoid outputFittedSphere;
        
        switch (this.bodyShapeType) {
            case SPHERE:
                outputFittedSphere = (OneAxisEllipsoid) this.bodyShape;
                break;

            case ONE_AXIS_ELLIPSOID: {
                final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) this.bodyShape;
                final double radius = ellipsoid.getARadius() * MathLib.pow(1. - ellipsoid.getFlattening(), 1. / 3.);

                outputFittedSphere =
                    new OneAxisEllipsoid(radius, 0., ellipsoid.getBodyFrame(), ellipsoid.getName());
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                final ThreeAxisEllipsoid ellipsoid = (ThreeAxisEllipsoid) this.bodyShape;
                final double radius =
                    MathLib.pow(ellipsoid.getARadius() * ellipsoid.getBRadius() * ellipsoid.getCRadius(), 1. / 3.);

                outputFittedSphere =
                    new OneAxisEllipsoid(radius, 0., ellipsoid.getBodyFrame(), ellipsoid.getName());
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;
                outputFittedSphere = buildFittedSphereFromFacetBodyShape(facetBodyShape);
                break;
            }

            case OTHER:
                throw new IllegalArgumentException("Fitting a fitted sphere to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputFittedSphere;
    }

    /**
     * Build the inner sphere which is the largest sphere strictly contained in the body shape and centered at (0, 0,
     * 0).
     *
     * @return inner sphere
     */
    private OneAxisEllipsoid getInnerSphere() {
        final OneAxisEllipsoid outputInnerSphere;

        switch (this.bodyShapeType) {
            case SPHERE:
                outputInnerSphere = (OneAxisEllipsoid) this.bodyShape;
                break;

            case ONE_AXIS_ELLIPSOID: {
                final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) this.bodyShape;
                final double radius = MathLib.min(ellipsoid.getEquatorialRadius(), ellipsoid.getPolarRadius());

                outputInnerSphere =
                    new OneAxisEllipsoid(radius, 0., ellipsoid.getBodyFrame(), ellipsoid.getName());
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                final ThreeAxisEllipsoid ellipsoid = (ThreeAxisEllipsoid) this.bodyShape;
                final double radius =
                    MathLib.min(ellipsoid.getARadius(), MathLib.min(ellipsoid.getBRadius(), ellipsoid.getCRadius()));

                outputInnerSphere =
                    new OneAxisEllipsoid(radius, 0., ellipsoid.getBodyFrame(), ellipsoid.getName());
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;

                outputInnerSphere = new OneAxisEllipsoid(facetBodyShape.getMinNorm(), 0., facetBodyShape.getBodyFrame(),
                    facetBodyShape.getName());
                break;
            }

            case OTHER:
                throw new IllegalArgumentException("Fitting an inner sphere to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputInnerSphere;
    }

    /**
     * Build outer sphere which is the smallest sphere englobing the body shape and centered at (0, 0, 0).
     *
     * @return outer sphere
     */
    private OneAxisEllipsoid getOuterSphere() {
        final OneAxisEllipsoid outputOuterSphere;

        switch (this.bodyShapeType) {
            case SPHERE:
                outputOuterSphere = (OneAxisEllipsoid) this.bodyShape;
                break;

            case ONE_AXIS_ELLIPSOID: {
                final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) this.bodyShape;

                outputOuterSphere = new OneAxisEllipsoid(ellipsoid.getEncompassingSphereRadius(), 0.,
                    ellipsoid.getBodyFrame(), ellipsoid.getName());
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                final ThreeAxisEllipsoid ellipsoid = (ThreeAxisEllipsoid) this.bodyShape;

                outputOuterSphere =
                    new OneAxisEllipsoid(ellipsoid.getEncompassingSphereRadius(), 0., ellipsoid.getBodyFrame(),
                        ellipsoid.getName());
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;

                outputOuterSphere = new OneAxisEllipsoid(facetBodyShape.getMaxNorm(), 0., facetBodyShape.getBodyFrame(),
                    facetBodyShape.getName());
                break;
            }

            case OTHER:
                throw new IllegalArgumentException("Fitting an outer sphere to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputOuterSphere;
    }

    /**
     * Build fitted one-axis ellipsoid which is the one-axis ellipsoid (a, f) which minimizes the distance to all
     * vertices.<br>
     * Minimization is reached with a {@link PowellOptimizer} in the case of a facet body shape.
     *
     * @return fitted one-axis ellipsoid
     */
    private OneAxisEllipsoid getFittedOneAxisEllipsoid() {
        final OneAxisEllipsoid outputFittedOneAxisEllipsoid;

        switch (this.bodyShapeType) {
            case SPHERE:
                outputFittedOneAxisEllipsoid = (OneAxisEllipsoid) this.bodyShape;
                break;

            case ONE_AXIS_ELLIPSOID: {
                outputFittedOneAxisEllipsoid = (OneAxisEllipsoid) this.bodyShape;
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                final ThreeAxisEllipsoid ellipsoid = (ThreeAxisEllipsoid) this.bodyShape;
                final double ae = MathLib.sqrt(ellipsoid.getARadius() * ellipsoid.getBRadius());
                final double flattening = 1 - ellipsoid.getCRadius() / ae;

                outputFittedOneAxisEllipsoid = new OneAxisEllipsoid(ae, flattening, this.bodyShape.getBodyFrame());
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;
                outputFittedOneAxisEllipsoid = buildFittedOneAxisEllipsoidFromFacetBodyShape(facetBodyShape);
                break;
            }

            case OTHER:
                throw new IllegalArgumentException(
                    "Fitting a fitted one-axis ellipsoid to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputFittedOneAxisEllipsoid;
    }

    /**
     * Build inner one-axis ellipsoid which is the largest one-axis ellipsoid strictly contained in the mesh and
     * centered around (0, 0, 0).
     *
     * @return inner one-axis ellipsoid
     */
    private OneAxisEllipsoid getInnerOneAxisEllipsoid() {
        final OneAxisEllipsoid outputInnerOneAxisEllipsoid;

        switch (this.bodyShapeType) {
            case SPHERE:
                outputInnerOneAxisEllipsoid = (OneAxisEllipsoid) this.bodyShape;
                break;

            case ONE_AXIS_ELLIPSOID: {
                outputInnerOneAxisEllipsoid = (OneAxisEllipsoid) this.bodyShape;
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                final ThreeAxisEllipsoid ellipsoid = (ThreeAxisEllipsoid) this.bodyShape;
                final double ae = MathLib.min(ellipsoid.getARadius(), ellipsoid.getBRadius());
                final double flattening = 1. - ellipsoid.getCRadius() / ae;

                outputInnerOneAxisEllipsoid = new OneAxisEllipsoid(ae, flattening, this.bodyShape.getBodyFrame());
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;
                outputInnerOneAxisEllipsoid = buildInnerOneAxisEllipsoidFromFacetBodyShape(facetBodyShape);
                break;
            }

            case OTHER:
                throw new IllegalArgumentException(
                    "Fitting an inner one-axis ellipsoid to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputInnerOneAxisEllipsoid;
    }

    /**
     * Build outer one-axis ellipsoid which is the smallest one-axis ellipsoid englobing the body shape and centered at
     * (0, 0, 0).
     *
     * @return outer one-axis ellipsoid
     */
    private OneAxisEllipsoid getOuterOneAxisEllipsoid() {
        final OneAxisEllipsoid outputOuterOneAxisEllipsoid;

        switch (this.bodyShapeType) {
            case SPHERE:
                outputOuterOneAxisEllipsoid = (OneAxisEllipsoid) this.bodyShape;
                break;

            case ONE_AXIS_ELLIPSOID: {
                outputOuterOneAxisEllipsoid = (OneAxisEllipsoid) this.bodyShape;
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                final ThreeAxisEllipsoid ellipsoid = (ThreeAxisEllipsoid) this.bodyShape;
                final double ae = MathLib.max(ellipsoid.getARadius(), ellipsoid.getBRadius());
                final double flattening = 1. - ellipsoid.getCRadius() / ae;

                outputOuterOneAxisEllipsoid = new OneAxisEllipsoid(ae, flattening, this.bodyShape.getBodyFrame());
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;
                outputOuterOneAxisEllipsoid = buildOuterOneAxisEllipsoidFromFacetBodyShape(facetBodyShape);
                break;
            }

            case OTHER:
                throw new IllegalArgumentException(
                    "Fitting an outer one-axis ellipsoid to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputOuterOneAxisEllipsoid;
    }

    /**
     * Build fitted three-axis ellipsoid which is the three-axis ellipsoid (a, f) which minimizes the distance to all
     * vertices.<br>
     * Minimization is reached with a {@link PowellOptimizer} in the case of a facet body shape.
     *
     * @return fitted three-axis ellipsoid
     */
    private ThreeAxisEllipsoid getFittedThreeAxisEllipsoid() {
        final ThreeAxisEllipsoid outputFittedThreeAxisEllipsoid;

        switch (this.bodyShapeType) {
            case SPHERE:
                final OneAxisEllipsoid sphere = (OneAxisEllipsoid) this.bodyShape;

                final double radius = sphere.getEquatorialRadius();
                outputFittedThreeAxisEllipsoid = new ThreeAxisEllipsoid(radius, radius, radius,
                    this.bodyShape.getBodyFrame(), this.bodyShape.getName());
                break;

            case ONE_AXIS_ELLIPSOID: {
                final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) this.bodyShape;
                final double ab = ellipsoid.getEquatorialRadius();
                final double c = ellipsoid.getPolarRadius();

                outputFittedThreeAxisEllipsoid =
                    new ThreeAxisEllipsoid(ab, ab, c, ellipsoid.getBodyFrame(), ellipsoid.getName());
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                outputFittedThreeAxisEllipsoid = (ThreeAxisEllipsoid) this.bodyShape;
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;
                outputFittedThreeAxisEllipsoid = buildFittedThreeAxisEllipsoidFromFacetBodyShape(facetBodyShape);
                break;
            }

            case OTHER:
                throw new IllegalArgumentException(
                    "Fitting a fitted three-axis ellipsoid to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputFittedThreeAxisEllipsoid;
    }

    /**
     * Build inner three-axis ellipsoid which is the largest three-axis ellipsoid strictly contained in the mesh and
     * centered around (0, 0, 0).
     *
     * @return inner three-axis ellipsoid
     */
    private ThreeAxisEllipsoid getInnerThreeAxisEllipsoid() {
        final ThreeAxisEllipsoid outputInnerThreeAxisEllipsoid;

        switch (this.bodyShapeType) {
            case SPHERE:
                final OneAxisEllipsoid sphere = (OneAxisEllipsoid) this.bodyShape;
                final double radius = sphere.getEquatorialRadius();

                outputInnerThreeAxisEllipsoid = new ThreeAxisEllipsoid(radius, radius, radius,
                    this.bodyShape.getBodyFrame(), this.bodyShape.getName());
                break;

            case ONE_AXIS_ELLIPSOID: {
                final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) this.bodyShape;
                final double ab = ellipsoid.getEquatorialRadius();
                final double c = ellipsoid.getPolarRadius();

                outputInnerThreeAxisEllipsoid =
                    new ThreeAxisEllipsoid(ab, ab, c, ellipsoid.getBodyFrame(), ellipsoid.getName());
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                outputInnerThreeAxisEllipsoid = (ThreeAxisEllipsoid) this.bodyShape;
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;
                outputInnerThreeAxisEllipsoid = buildInnerThreeAxisEllipsoidFromFacetBodyShape(facetBodyShape);
                break;
            }

            case OTHER:
                throw new IllegalArgumentException(
                    "Fitting an inner three-axis ellipsoid to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputInnerThreeAxisEllipsoid;
    }

    /**
     * Build outer three-axis ellipsoid which is the smallest three-axis ellipsoid englobing the body shape and centered
     * at (0, 0, 0).
     *
     * @return outer three-axis ellipsoid
     */
    private ThreeAxisEllipsoid getOuterThreeAxisEllipsoid() {
        final ThreeAxisEllipsoid outputOuterThreeAxisEllipsoid;

        switch (this.bodyShapeType) {
            case SPHERE:
                final OneAxisEllipsoid sphere = (OneAxisEllipsoid) this.bodyShape;
                final double radius = sphere.getEquatorialRadius();

                outputOuterThreeAxisEllipsoid = new ThreeAxisEllipsoid(radius, radius, radius,
                    this.bodyShape.getBodyFrame(), this.bodyShape.getName());
                break;

            case ONE_AXIS_ELLIPSOID: {
                final OneAxisEllipsoid ellipsoid = (OneAxisEllipsoid) this.bodyShape;
                final double ab = ellipsoid.getEquatorialRadius();
                final double c = ellipsoid.getPolarRadius();

                outputOuterThreeAxisEllipsoid =
                    new ThreeAxisEllipsoid(ab, ab, c, ellipsoid.getBodyFrame(), ellipsoid.getName());
                break;
            }

            case THREE_AXIS_ELLIPSOID: {
                outputOuterThreeAxisEllipsoid = (ThreeAxisEllipsoid) this.bodyShape;
                break;
            }

            case FACET_BODY_SHAPE: {
                final FacetBodyShape facetBodyShape = (FacetBodyShape) this.bodyShape;
                outputOuterThreeAxisEllipsoid = buildOuterThreeAxisEllipsoidFromFacetBodyShape(facetBodyShape);
                break;
            }

            case OTHER:
                throw new IllegalArgumentException(
                    "Fitting an outer three-axis ellipsoid to a " + this.bodyShape.getClass()
                        + OBJ_NOT_SUPP);
            default:
                // cannot happen
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return outputOuterThreeAxisEllipsoid;
    }

    /**
     * Build a fitted sphere from a facet body shape.
     * 
     * @param facetBodyShape
     *        the facet body shape to fit the sphere with
     * @return the fitted Sphere
     */
    private OneAxisEllipsoid buildFittedSphereFromFacetBodyShape(final FacetBodyShape facetBodyShape) {
        
        final Map<Integer, Vertex> vertices = facetBodyShape.getMeshProvider().getVertices();

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

        // Cost function to minimize
        final MultivariateFunction func = point -> {
            final double r = point[0];
            double cost = 0;
            int i1 = 0;
            for (final Vertex v : vertices.values()) {
                // Theoretical point for current a,b,c values
                final Vector3D vTh = new Vector3D(r * cosLat[i1] * cosLon[i1], r * cosLat[i1] * sinLon[i1], r
                        * sinLat[i1]);
                // Distance squared: add to cost function
                cost += vTh.distanceSq(v.getPosition());
                i1++;
            }
            return cost;
        };
        // Sphere radius is in [this.minNorm, this.maxNorm]
        final double minNorm = facetBodyShape.getMinNorm();
        final double maxNorm = facetBodyShape.getMaxNorm();
        final SimpleBounds bounds = new SimpleBounds(new double[] { minNorm },
            new double[] { maxNorm });
        // Sphere radius is initialized at mid norm
        final double midNorm = (minNorm + maxNorm) / 2.;
        final InitialGuess initialGuess = new InitialGuess(new double[] { midNorm });
        
        // Use optimizer
        final MultivariateOptimizer optimizer = new PowellOptimizer(EPS_OPT, EPS_OPT);
        // Run optimizer
        final PointValuePair res = optimizer.optimize(new MaxEval(MAX_EVAL), new ObjectiveFunction(func),
            GoalType.MINIMIZE, initialGuess, bounds);

        // Build ellipsoid with optimum values (a, b, c)
        return new OneAxisEllipsoid(res.getPoint()[0], 0., this.bodyShape.getBodyFrame(), this.bodyShape.getName());
    }

    /**
     * Build a fitted one-axis ellipsoid from a facet body shape.
     * 
     * @param facetBodyShape
     *        the facet body shape to fit the one-axis ellipsoid with
     * @return the fitted one-axis ellipsoid
     */
    private OneAxisEllipsoid buildFittedOneAxisEllipsoidFromFacetBodyShape(final FacetBodyShape facetBodyShape) {
        final Map<Integer, Vertex> vertices = facetBodyShape.getMeshProvider().getVertices();

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
        // Use optimizer
        final MultivariateOptimizer optimizer = new PowellOptimizer(EPS_OPT, EPS_OPT);
        final PointValuePair res = optimizer.optimize(new MaxEval(MAX_EVAL), new ObjectiveFunction(func),
            GoalType.MINIMIZE, GoalType.MINIMIZE,
            new InitialGuess(new double[] { (facetBodyShape.getMinNorm() + facetBodyShape.getMaxNorm()) / 2.,
                FIRST_GUESS_FLATTENING }),
            new SimpleBounds(new double[] { facetBodyShape.getMinNorm(), 0. },
                new double[] { facetBodyShape.getMaxNorm(), 1. }));

        // Build ellipsoid with optimum values (a, f)
        return new OneAxisEllipsoid(res.getPoint()[0], res.getPoint()[1], this.bodyShape.getBodyFrame(),
            this.bodyShape.getName());
    }

    /**
     * Build an inner one-axis ellipsoid from a facet body shape.
     * 
     * @param facetBodyShape
     *        the facet body shape to fit the inner one-axis ellipsoid with
     * @return the inner one-axis ellipsoid
     */
    private OneAxisEllipsoid buildInnerOneAxisEllipsoidFromFacetBodyShape(final FacetBodyShape facetBodyShape) {
        final Map<Integer, Vertex> vertices = facetBodyShape.getMeshProvider().getVertices();

        // Flattening of the fitted ellipsoid
        final double flattening =
            ((OneAxisEllipsoid) getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_FITTED)).getFlattening();
        // Dilation of the ellipsoidal DTM into a spherical DTM to find the largest inscribed
        // sphere contained within it.
        final double dilation = 1. / (1. - flattening);

        double minDilatedRadius = Double.POSITIVE_INFINITY;
        for (final Vertex v : vertices.values()) {
            final Vector3D dilatedPoint = new Vector3D(v.getPosition().getX(), v.getPosition().getY(), v.getPosition()
                .getZ() * dilation);
            final double dilatedRadius = dilatedPoint.getNorm();
            minDilatedRadius = MathLib.min(minDilatedRadius, dilatedRadius);
        }

        // Return the inner ellipsoid, i.e. the biggest ellipsoid strictly contained in
        // the mesh. The dilated radius of the inscribed sphere (found in the spherical problem)
        // corresponds to the equatorial radius of the largest inner ellipsoid (flattened problem)
        return new OneAxisEllipsoid(minDilatedRadius, flattening, this.bodyShape.getBodyFrame(),
            this.bodyShape.getName());
    }

    /**
     * Build an outer one-axis ellipsoid from a facet body shape.
     * 
     * @param facetBodyShape
     *        the facet body shape to fit the outer one-axis ellipsoid with
     * @return the outer one-axis ellipsoid
     */
    private OneAxisEllipsoid buildOuterOneAxisEllipsoidFromFacetBodyShape(final FacetBodyShape facetBodyShape) {
        final Map<Integer, Vertex> vertices = facetBodyShape.getMeshProvider().getVertices();

        final double flattening = ((OneAxisEllipsoid) getEllipsoid(EllipsoidType.ONE_AXIS_ELLIPSOID_FITTED)).getFlattening();
        final double dilatation = 1. / (1. -  flattening);

        double maxDilatedRadius = Double.NEGATIVE_INFINITY;
        for (final Vertex v : vertices.values()) {
            final Vector3D dilatedPoint =
                new Vector3D(v.getPosition().getX(), v.getPosition().getY(), v.getPosition()
                    .getZ() * dilatation);
            final double dilatedRadius = dilatedPoint.getNorm();
            maxDilatedRadius = MathLib.max(maxDilatedRadius, dilatedRadius);
        }

        // Return the outer ellipsoid, i.e. the smallest ellipsoid englobing the mesh. The dilated
        // radius of the englobing sphere (found in the spherical problem) corresponds to the
        // equatorial radius of the smaller outer ellipsoid (flattened problem)
        return new OneAxisEllipsoid(maxDilatedRadius, flattening, this.bodyShape.getBodyFrame(),
                this.bodyShape.getName());
    }

    /**
     * Build a fitted three-axis ellipsoid from a facet body shape.
     * 
     * @param facetBodyShape
     *        the facet body shape to fit the three-axis ellipsoid with
     * @return the fitted three-axis ellipsoid
     */
    private ThreeAxisEllipsoid buildFittedThreeAxisEllipsoidFromFacetBodyShape(final FacetBodyShape facetBodyShape) {
        final Map<Integer, Vertex> vertices = facetBodyShape.getMeshProvider().getVertices();

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

        // Cost function to minimize
        final MultivariateFunction func = point -> {
            final double a = point[0];
            final double b = point[1];
            final double c = point[2];
            final double e2 = 1 - FastMath.pow(b / a, 2);
            double cost = 0;
            int i1 = 0;
            for (final Vertex v : vertices.values()) {
                // 1st ellipse computation: in the XY plane
                final double abis = b / FastMath.sqrt(1. - e2 * cosLon[i1] * cosLon[i1]);
                // 2nd ellipse computation: in the vertical plane
                final double e2bis = 1 - FastMath.pow(c / abis, 2);
                final double r = c / FastMath.sqrt(1. - e2bis * cosLat[i1] * cosLat[i1]);
                // Theoretical point for current a,b,c values
                final Vector3D vTh = new Vector3D(r * cosLat[i1] * cosLon[i1], r * cosLat[i1] * sinLon[i1], r
                        * sinLat[i1]);
                // Distance squared: add to cost function
                cost += vTh.distanceSq(v.getPosition());
                i1++;
            }
            return cost;
        };
        // All axis are in [this.minNorm, this.maxNorm]
        final double minNorm = facetBodyShape.getMinNorm();
        final double maxNorm = facetBodyShape.getMaxNorm();
        final SimpleBounds bounds = new SimpleBounds(new double[] { minNorm, minNorm, minNorm },
            new double[] { maxNorm, maxNorm, maxNorm });
        // All axis are initialized at mid norm
        final double midNorm = (minNorm + maxNorm) / 2.;
        final InitialGuess initialGuess = new InitialGuess(new double[] { midNorm, midNorm, midNorm });
        // Run optimizer
        // Use optimizer
        final MultivariateOptimizer optimizer = new PowellOptimizer(EPS_OPT, EPS_OPT);
        final PointValuePair res = optimizer.optimize(new MaxEval(MAX_EVAL), new ObjectiveFunction(func),
            GoalType.MINIMIZE, initialGuess, bounds);

        // Build ellipsoid with optimum values (a, b, c)
        return new ThreeAxisEllipsoid(res.getPoint()[0], res.getPoint()[1], res.getPoint()[2],
            facetBodyShape.getBodyFrame(), facetBodyShape.getName());
    }

    /**
     * Build an inner three-axis ellipsoid from a facet body shape.
     * 
     * @param facetBodyShape
     *        the facet body shape to fit the inner three-axis ellipsoid with
     * @return the inner three-axis ellipsoid
     */
    private ThreeAxisEllipsoid buildInnerThreeAxisEllipsoidFromFacetBodyShape(final FacetBodyShape facetBodyShape) {
        final Map<Integer, Vertex> vertices = facetBodyShape.getMeshProvider().getVertices();

        // Get the fitted ellipsoid
        final ThreeAxisEllipsoid fittedEllipsoid = buildFittedThreeAxisEllipsoidFromFacetBodyShape(facetBodyShape);
        final double aRatio = 1. / fittedEllipsoid.getARadius();
        final double bRatio = 1. / fittedEllipsoid.getBRadius();
        final double cRatio = 1. / fittedEllipsoid.getCRadius();

        double minDilatedRadius = Double.POSITIVE_INFINITY;
        for (final Vertex v : vertices.values()) {
            final Vector3D dilatedPoint = new Vector3D(v.getPosition().getX() * aRatio, v.getPosition().getY() * bRatio,
                v.getPosition().getZ() * cRatio);
            final double dilatedRadius = dilatedPoint.getNorm();
            minDilatedRadius = MathLib.min(minDilatedRadius, dilatedRadius);
        }

        // Return the inner ellipsoid, i.e. the biggest ellipsoid strictly contained in
        // the mesh.
        return new ThreeAxisEllipsoid(minDilatedRadius / aRatio, minDilatedRadius / bRatio,
            minDilatedRadius / cRatio, facetBodyShape.getBodyFrame(), facetBodyShape.getName());
    }

    /**
     * Build an outer three-axis ellipsoid from a facet body shape.
     * 
     * @param facetBodyShape
     *        the facet body shape to fit the outer three-axis ellipsoid with
     * @return the outer three-axis ellipsoid
     */
    private ThreeAxisEllipsoid buildOuterThreeAxisEllipsoidFromFacetBodyShape(final FacetBodyShape facetBodyShape) {
        final Map<Integer, Vertex> vertices = facetBodyShape.getMeshProvider().getVertices();

        // Get the fitted ellipsoid
        final ThreeAxisEllipsoid fittedEllipsoid = buildFittedThreeAxisEllipsoidFromFacetBodyShape(facetBodyShape);
        final double aRatio = 1. / fittedEllipsoid.getARadius();
        final double bRatio = 1. / fittedEllipsoid.getBRadius();
        final double cRatio = 1. / fittedEllipsoid.getCRadius();

        double maxDilatedRadius = Double.NEGATIVE_INFINITY;
        for (final Vertex v : vertices.values()) {
            final Vector3D dilatedPoint = new Vector3D(v.getPosition().getX() * aRatio, v.getPosition().getY() * bRatio,
                v.getPosition().getZ() * cRatio);
            final double dilatedRadius = dilatedPoint.getNorm();
            maxDilatedRadius = MathLib.max(maxDilatedRadius, dilatedRadius);
        }

        // Return the outer ellipsoid, i.e. the smallest ellipsoid englobing the mesh.
        return new ThreeAxisEllipsoid(maxDilatedRadius / aRatio, maxDilatedRadius / bRatio,
            maxDilatedRadius / cRatio, facetBodyShape.getBodyFrame(), facetBodyShape.getName());
    }
}
