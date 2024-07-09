/**
 * Copyright 2011-2017 CNES
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
 * @history creation 26/04/2012
 */
 /*
  * HISTORY
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
  * VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
  * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
  * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
  * VERSION::FA:178:06/01/2013:Corrected log id format
  * VERSION::FA:106:16/07/2013:Account of massless parts with aero props.
  * Account of parts with mass and without aero properties.
  * VERSION::DM:85:23/07/2013:Made some common methods static
  * VERSION::FA:86:22/10/2013:New mass management system
  * VERSION::DM:89:22/11/2013:New formula implementation for partial derivatives
  * VERSION::FA:180:18/03/2014:Corrected error message
  * VERSION::FA:93:31/03/2014:changed partial derivatives API
  * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
  * VERSION::FA:213:06/04/2014:correction on the partial derivatives computation
  * VERSION::DM:289:27/08/2014:Add exception to SpacececraftState.getAttitude()
  * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
  * VERSION::FA:255:13/10/2014:header correction
  * VERSION::FA:273:20/10/2014:Minor code problems
  * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
  * VERSION::FA:412:04/05/2015:surface to mass ratio
  * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
  * VERSION::FA:493:10/08/2015:correction on the jacobian parameterizable parameters
  * VERSION::FA:495:10/08/2015:correction on the partial derivatives wrt cx
  * VERSION::DM:534:10/02/2016:Parametrization of force models
  * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
  * VERSION::FA:662:26/08/2016:Computation times speed-up
  * VERSION::FA:673:12/09/2016: add getTotalMass(state)
  * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
  * VERSION::FA:707:08/12/2016:Minor points staying after V3.3
  * VERSION::DM:834:04/04/2017:create vehicle object
  * VERSION::DM:571:12/04/2017:Add density partial derivative contribution in partial derivatives
  * VERSION::FA:1177:06/09/2017:add Cook model validation test
  * VERSION::DM:1420:24/11/2017:updateMainPartFrame() speed-up
  * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
  * END-HISTORY
  */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.AeroCrossSectionProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class that represents an aero model, based on the vehicle.
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of a frame (Assembly attribute) linked to the tree of frames in each of the parts makes
 *                      this class not thread-safe.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class AeroModel extends Parameterizable implements DragSensitive {

    /** Generated serial UID. */
    private static final long serialVersionUID = 4898707923970013932L;

    /** Earth angular velocity [rad/s]. */
    private static final double OMEGA = Constants.WGS84_EARTH_ANGULAR_VELOCITY;

    /** Default step for atmospheric density derivatives altitude finite differences computation (m). */
    private static final double DEFAULT_ALT_STEP = 10.;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** The considered vehicle on which the model is based on. */
    private final Assembly assembly;
    /** Mass model */
    private final MassProvider massModel;

    /** The atmosphere. */
    private final Atmosphere atmosphere;

    /** The earth shape. */
    private final OneAxisEllipsoid earthShape;

    /** Step for atmospheric density derivatives altitude finite differences computation wrt position (m). */
    private final double altitudeStep;

    /** parameter for the derivatives computation */
    private double vCosA;
    /** parameter for the derivatives computation */
    private double vCosA2;
    /** parameter for the derivatives computation */
    private double v3CosA;
    /** parameter for the derivatives computation */
    private double tRelNorm;
    /** parameter for the derivatives computation */
    private Vector3D tangentVector;

    /** List of parts having an aero cross section property. */
    private List<IPart> aeroSphereParts;

    /** List of parts having an aero facet property. */
    private List<IPart> aeroFacetParts;

    /** Boolean indicating if attitude computation is required (for computation speed-up). */
    private boolean needAttitude;

    /**
     * Aero model (the acceleration is computed from all the sub parts of the vehicle)
     * with an {@link Atmosphere} model and a {@link OneAxisEllipsoid} for acceleration partial
     * derivatives computation with respect to state by finite differences(including density
     * partial derivatives).
     * 
     * @param inAssembly
     *        The considered vehicle
     * @param inAtmosphere
     *        the atmospheric model
     * @param earthShapeIn
     *        the spacecraft geodetic position model
     * @param altStep
     *        atmospheric density derivatives altitude finite differences computation
     */
    public AeroModel(final Assembly inAssembly, final Atmosphere inAtmosphere, final OneAxisEllipsoid earthShapeIn,
                     final double altStep) {
        super();
        this.checkProperties(inAssembly);
        this.assembly = inAssembly;
        this.massModel = new MassModel(this.assembly);
        this.atmosphere = inAtmosphere;
        this.earthShape = earthShapeIn;
        this.altitudeStep = altStep;
    }

    /**
     * Aero model (the acceleration is computed from all the sub parts of the vehicle)
     * with default step for finite differences computation.
     * 
     * @param inAssembly
     *        The considered vehicle
     * @param inAtmosphere
     *        the atmospheric model
     * @param inGeodPos
     *        the spacecraft geodetic position model
     */
    public AeroModel(final Assembly inAssembly, final Atmosphere inAtmosphere, final OneAxisEllipsoid inGeodPos) {
        this(inAssembly, inAtmosphere, inGeodPos, DEFAULT_ALT_STEP);
    }

    /**
     * Aero model (the acceleration is computed from all the sub parts of the vehicle)
     * default constructor.
     * <p>
     * <b>Warning: </b> This constructor should not be used if partial derivatives have to be computed.
     * </p>
     * 
     * @param inAssembly
     *        The considered vehicle
     */
    public AeroModel(final Assembly inAssembly) {
        this(inAssembly, null, null, Double.NaN);
    }

    /**
     * This method tests if the required properties exist (AERO_CROSS_SECTION or AERO_FACET). At least
     * one part with a mass is required. It also store jacobians parameters handled in the parts
     * 
     * @param assemblyIn
     *        The considered vehicle.
     */
    private void checkProperties(final Assembly assemblyIn) {

        // Initialization
        this.aeroSphereParts = new ArrayList<IPart>();
        this.aeroFacetParts = new ArrayList<IPart>();

        boolean hasRadProp = false;
        boolean hasMassProp = false;

        this.needAttitude = false;

        for (final IPart part : assemblyIn.getParts().values()) {
            // checking aero properties
            if (part.hasProperty(PropertyType.AERO_FACET) ^ part.hasProperty(PropertyType.AERO_CROSS_SECTION)) {
                hasRadProp |= true;

                if (part.hasProperty(PropertyType.AERO_CROSS_SECTION)) {
                    if (!(part.getProperty(PropertyType.AERO_CROSS_SECTION) instanceof AeroSphereProperty)) {
                        this.needAttitude = true;
                    }
                    this.aeroSphereParts.add(part);
                    // getting the function for the drag force coef (Cx) parameters
                    this.getDragForceParameters(part);
                } else if (part.hasProperty(PropertyType.AERO_FACET)) {
                    this.needAttitude = true;
                    this.aeroFacetParts.add(part);
                    // getting the function for the normal and tangential coef (Cn & Ct) parameters
                    this.getCoefsParameters(part);
                }
            }

            // checking mass property
            if (part.hasProperty(PropertyType.MASS)) {
                hasMassProp |= true;
            }
        }

        if (!hasRadProp || !hasMassProp) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_AERO_MASS_PROPERTIES);
        }
    }

    /**
     * Getting the normal and tangential coefficients parameters and store them in the local map (Parameterizable)
     * 
     * @param part
     *        : AeroFacetProperty
     * 
     * @since 2.3
     */
    private void getCoefsParameters(final IPart part) {
        final IParamDiffFunction aeroNormalCoefFunction = ((AeroFacetProperty) part.
            getProperty(PropertyType.AERO_FACET)).getNormalCoef();
        // adding parameters to internal map
        for (final Parameter toAdd : aeroNormalCoefFunction.getParameters()) {
            this.addParameter(toAdd);
        }
        final IParamDiffFunction aeroTangentialCoefFunction = ((AeroFacetProperty) part.
            getProperty(PropertyType.AERO_FACET)).getTangentialCoef();
        // adding parameters to internal map
        for (final Parameter toAdd : aeroTangentialCoefFunction.getParameters()) {
            this.addParameter(toAdd);
        }
    }

    /**
     * Getting the drag force parameters and store them in the local map (Parameterizable)
     * 
     * @param part
     *        : AeroCrossSectionProperty
     * 
     * @since 2.3
     */
    private void getDragForceParameters(final IPart part) {
        final IParamDiffFunction aeroDragForceFunction = ((AeroCrossSectionProperty) part.
            getProperty(PropertyType.AERO_CROSS_SECTION)).getDragForce();
        // Adding parameters to internal list
        this.addAllParameters(aeroDragForceFunction.getParameters());
    }

    /**
     * Method to compute the aero acceleration, based on the assembly.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param density
     *        the atmosphere density.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere.
     * @return the acceleration applied on the assembly.
     * @throws PatriusException
     *         when an error occurs.
     * 
     */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity) throws PatriusException {

        if (this.needAttitude) {
            this.assembly.updateMainPartFrame(state);
        }

        Vector3D force = Vector3D.ZERO;

        // Main spacecraft's part frame
        final Frame mainPartFrame = this.assembly.getMainPart().getFrame();

        // Loop on all parts having an aero cross section property
        for (final IPart part : this.aeroSphereParts) {
            force = force.add(forceOnSphere(state, part, density, relativeVelocity, mainPartFrame));
        }

        // Loop on all parts having an aero facet property
        for (final IPart part : this.aeroFacetParts) {
            force = force.add(forceOnFacet(state, part, this.assembly, density, relativeVelocity));
        }

        return new Vector3D(MathLib.divide(1.0, this.massModel.getTotalMass(state)), force);
    }

    /**
     * Method to compute the force for a plane model.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        the current part of the assembly.
     * @param assembly
     *        the assembly.
     * @param density
     *        the atmosphere density.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere.
     * @return the force applied on the facet.
     * @throws PatriusException
     *         orekit frame exception
     */
    protected static Vector3D
        forceOnFacet(final SpacecraftState state, final IPart part, final Assembly assembly,
                     final double density, final Vector3D relativeVelocity) throws PatriusException {

        Vector3D facetComputedForce = Vector3D.ZERO;

        if (relativeVelocity.getNorm() > Precision.EPSILON) {

            // Drag flux in inertial frame
            final Transform t = part.getFrame().getTransformTo(state.getFrame(), state.getDate());

            // Get the AeroFacetProperty
            final AeroFacetProperty aeroFacetProp = (AeroFacetProperty) part.getProperty(PropertyType.AERO_FACET);
            final Facet currentFacet = aeroFacetProp.getFacet();

            // Facet in propagation frame:
            final Vector3D fNorm = t.transformVector(currentFacet.getNormal().normalize());
            final double orientation = Vector3D.dotProduct(relativeVelocity.normalize(), fNorm);

            // we admit the facet is part of a closed object;
            // the drag force thus only applies "outside"
            // i.e. when the orientation is negative
            if (orientation < 0.) {
                final AeroFacetProperty aeroProp = (AeroFacetProperty) part.getProperty(PropertyType.AERO_FACET);
                final double cNorm = aeroProp.getNormalCoef().value(state);
                final double cTang = aeroProp.getTangentialCoef().value(state);

                // Angle between the facet's normal and the direction of the atmosphere's flow
                // This angle will always be between Pi/2 and Pi given the negative orientation
                final double alpha = Vector3D.angle(relativeVelocity, fNorm);
                // alpha = 0.0;
                final double velonorm = relativeVelocity.getNorm();

                final double[] sincos = MathLib.sinAndCos(alpha);
                final double sinAlpha = sincos[0];
                final double cosAlpha = sincos[1];
                final double fArea = currentFacet.getArea();
                // Always positive due to the angle value
                final double normalForceNorm = HALF * density * velonorm * velonorm * cNorm * fArea * cosAlpha
                        * cosAlpha;
                // Always negative due to the angle value
                final double tangentForceNorm = HALF * density * velonorm * velonorm * cTang * fArea * sinAlpha
                        * cosAlpha;

                // Tangent vector for the normal relative to the velocity
                Vector3D tangentVectorComp = Vector3D.ZERO;
                // If the norm for the tangent vector is big enough, we try to compute the tangent vector.

                if (MathLib.abs(tangentForceNorm) > Precision.EPSILON) {

                    // Projection of the velocity vector on the normal.
                    final Vector3D multVelo = new Vector3D(Vector3D.dotProduct(fNorm, relativeVelocity), fNorm);
                    // The tangent vector is the normalized difference between the velocity
                    // and the projection of the velocity on the normal.
                    final Vector3D multTangentVector = relativeVelocity.subtract(multVelo);
                    if (multTangentVector.getNorm() > Precision.EPSILON) {
                        // The difference is big enough for the tangent vector to have a meaning.
                        tangentVectorComp = multTangentVector.normalize();
                    }
                    // If the difference isn't big enough,
                    // it means the velocity is parallel to the normal,
                    // so the "tangent" is meaningless.
                    // tangentVector remains at ZERO with no ill effect.
                }

                // Force computation
                // The normal force is always in the direction opposite to the normal,
                // and normalForceNorm is always positive : negative sign for the norm
                final Vector3D normalForce = new Vector3D(-normalForceNorm, fNorm);
                // tangentForceNorm is always negative, and tangentVector is constructed
                // with the right direction : negative sign for the norm
                final Vector3D tangentForce = new Vector3D(-tangentForceNorm, tangentVectorComp);

                facetComputedForce = normalForce.add(tangentForce);
            }
        }

        return facetComputedForce;
    }

    /**
     * Method to compute the force for the part model (cylinder, parallelepiped, sphere).
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        the current part of the assembly.
     * @param density
     *        the atmosphere density.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere.
     * @param mainPartFrame
     *        mainPartFrame
     * @return the force applied on the facet.
     * @throws PatriusException
     *         if no attitude is defined
     */
    protected static Vector3D forceOnSphere(final SpacecraftState state, final IPart part,
                                            final double density, final Vector3D relativeVelocity,
                                            final Frame mainPartFrame) throws PatriusException {

        Vector3D sphereComputedForce = Vector3D.ZERO;

        if (relativeVelocity.getNorm() > Precision.EPSILON) {

            // Get the AeroCrossSectionProperty
            final AeroCrossSectionProperty aeroProp = (AeroCrossSectionProperty) part
                .getProperty(PropertyType.AERO_CROSS_SECTION);

            // Compute the normal force norm (tangential not needed, angle is zero)
            final double cNorm = aeroProp.getDragForce().value(state);
            final double velonorm = relativeVelocity.getNorm();

            final double fArea = aeroProp.getCrossSection(state, relativeVelocity, mainPartFrame, part.getFrame());
            final double normalForceNorm = HALF * density * velonorm * velonorm * cNorm * fArea;
            sphereComputedForce = relativeVelocity.normalize().scalarMultiply(normalForceNorm);
        }

        return sphereComputedForce;
    }

    /**
     * Compute acceleration derivatives with respect to ballistic coefficient.
     * 
     * @param s
     *        the SpacecraftState.
     * @param param
     *        name of parameter.
     * @param density
     *        the atmosphere density.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere.
     * @param dAccdParam
     *        acceleration derivatives with respect to ballistic coefficient.
     * @exception PatriusException
     *            if derivatives cannot be computed
     */
    @Override
    public void addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                  final Vector3D relativeVelocity, final double[] dAccdParam) throws PatriusException {

        // Check if the parameter if handled
        if (!this.supportsParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param.getName());
        }

        double[] dAdT = new double[3];
        final double[] dAccdTPart = new double[3];

        // Get the norm of relative velocity:
        final double velonorm = relativeVelocity.getNorm();

        // total mass
        final double m = this.massModel.getTotalMass();

        // Main satellite part frame
        final Frame mainPartFrame = this.assembly.getMainPart().getFrame();

        // Loop on all parts having an aero cross section property
        for (final IPart part : this.aeroSphereParts) {

            // Get the AeroCrossSectionProperty
            final AeroCrossSectionProperty aeroCrossProp = (AeroCrossSectionProperty) part
                .getProperty(PropertyType.AERO_CROSS_SECTION);
            final IParamDiffFunction aeroDragForceFunction = aeroCrossProp.getDragForce();
            if (aeroDragForceFunction != null && aeroDragForceFunction.supportsParameter(param)) {
                final double surf = aeroCrossProp.getCrossSection(s, relativeVelocity, mainPartFrame, part.getFrame());

                // Compute the derivative value of Cn with respect to the given parameter
                final double derivativeValue = aeroCrossProp.getDragForceDerivativeValue(param, s);

                // compute the coef
                final double coeff = MathLib.divide(HALF * density * velonorm * velonorm * derivativeValue * surf, m);

                final Vector3D computedDAccDParamSphere = relativeVelocity.normalize().scalarMultiply(coeff);
                // compute the derivatives for the current shape:
                dAdT[0] = computedDAccDParamSphere.getX();
                dAdT[1] = computedDAccDParamSphere.getY();
                dAdT[2] = computedDAccDParamSphere.getZ();
            }

            // add all the contributions:
            dAccdTPart[0] += dAdT[0];
            dAccdTPart[1] += dAdT[1];
            dAccdTPart[2] += dAdT[2];
        }

        // Loop on all parts having an aero facet property
        for (final IPart part : this.aeroFacetParts) {

            final double surf = ((AeroFacetProperty) part.getProperty(PropertyType.AERO_FACET)).getFacet().getArea();
            // compute the coef
            final double coeff = MathLib.divide(-density / 2.0 * velonorm * velonorm * surf, m);
            dAdT = this.computeDAccDParamFacet(s, param, relativeVelocity, coeff, part);

            // add all the contributions:
            dAccdTPart[0] += dAdT[0];
            dAccdTPart[1] += dAdT[1];
            dAccdTPart[2] += dAdT[2];
        }

        dAccdParam[0] += dAccdTPart[0];
        dAccdParam[1] += dAccdTPart[1];
        dAccdParam[2] += dAccdTPart[2];

    }

    /**
     * Compute the partial derivatives with respect to parameters for a facet.
     * 
     * @param s
     *        the spacecraft state.
     * @param param
     *        the parameter name.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere.
     * @param coeff
     *        the drag coefficient.
     * @param part
     *        the part of the spacecraft (a facet)
     * @return the partial derivatives with respect to parameters for a facet.
     * @throws PatriusException
     *         orekit frame exception
     */
    private double[]
        computeDAccDParamFacet(final SpacecraftState s, final Parameter param,
                               final Vector3D relativeVelocity, final double coeff, final IPart part)
            throws PatriusException {

        final double[] dAdCoeff = new double[3];
        // Orientation of the facet
        // transform from part to spacecraft root
        final Transform partToMainPart =
            part.getFrame().getTransformTo(this.assembly.getMainPart().getFrame(), s.getDate());

        // facet in spacecraft frame
        final AeroFacetProperty aeroFacetProp = (AeroFacetProperty) part.getProperty(PropertyType.AERO_FACET);
        final Vector3D fNorm = partToMainPart.transformVector(aeroFacetProp.getFacet().getNormal().normalize());

        // Relative velocity in the spacecraft frame
        final Rotation r = s.getAttitude().getRotation();
        final Vector3D relativeVelocitySat = r.applyInverseTo(relativeVelocity);
        final double orientation = Vector3D.dotProduct(relativeVelocitySat.normalize(), fNorm);

        // we admit the facet is part of a closed object;
        // the drag force thus only applies "outside"
        // i.e. when the orientation is negative
        if (orientation < 0.) {
            final double[] dAdDn = new double[2];

            final double alpha = Vector3D.angle(relativeVelocitySat, fNorm);

            // Tangent vector for the normal relative to the velocity
            Vector3D tangentVect = Vector3D.ZERO;

            // Projection of the velocity vector on the normal.
            final Vector3D multVelo = new Vector3D(Vector3D.dotProduct(fNorm, relativeVelocitySat), fNorm);
            // The tangent vector is the normalized difference between the velocity
            // and the projection of the velocity on the normal.
            final Vector3D multTangentVector = relativeVelocitySat.subtract(multVelo);
            if (multTangentVector.getNorm() > Precision.EPSILON) {
                // The difference is big enough for the tangent vector to have a meaning.
                tangentVect = multTangentVector.normalize();
            }

            // Initialization
            dAdCoeff[0] = 0.0;
            dAdCoeff[1] = 0.0;
            dAdCoeff[2] = 0.0;
            // test if normal coef support the parameter
            final IParamDiffFunction aeroNormalCoefFunction = aeroFacetProp.getNormalCoef();
            final IParamDiffFunction aeroTangentialCoefFunction = aeroFacetProp.getTangentialCoef();

            if (aeroNormalCoefFunction.supportsParameter(param)) {

                // Get the normal derivative value with respect of the parameter
                final double normalDerivativeValue = ((AeroFacetProperty) part.
                    getProperty(PropertyType.AERO_FACET)).getNormalCoef().derivativeValue(param, s);

                final double cos = MathLib.cos(alpha);
                dAdDn[0] = coeff * cos * cos;
                dAdDn[1] = 0.;
                final Vector3D dAdDnSat = new Vector3D(dAdDn[1], tangentVect).add(new Vector3D(dAdDn[0], fNorm));
                final Vector3D dAdDnIn = r.applyTo(dAdDnSat);

                dAdCoeff[0] = dAdDnIn.getX() * normalDerivativeValue;
                dAdCoeff[1] = dAdDnIn.getY() * normalDerivativeValue;
                dAdCoeff[2] = dAdDnIn.getZ() * normalDerivativeValue;
            } else if (aeroTangentialCoefFunction.supportsParameter(param)) {

                // Get the tangential derivative value with respect of the parameter
                final double tangentialDerivativeValue = ((AeroFacetProperty) part.
                    getProperty(PropertyType.AERO_FACET)).getTangentialCoef().derivativeValue(param, s);

                final double[] sincos = MathLib.sinAndCos(alpha);
                final double sin = sincos[0];
                final double cos = sincos[1];
                
                final double[] dAdDt = new double[2];
                dAdDt[0] = 0.;
                dAdDt[1] = coeff * sin *cos;
                final Vector3D dAdDtSat = new Vector3D(dAdDt[1], tangentVect).add(new Vector3D(dAdDt[0], fNorm));
                final Vector3D dAdDtIn = r.applyTo(dAdDtSat);

                dAdCoeff[0] = dAdDtIn.getX() * tangentialDerivativeValue;
                dAdCoeff[1] = dAdDtIn.getY() * tangentialDerivativeValue;
                dAdCoeff[2] = dAdDtIn.getZ() * tangentialDerivativeValue;
            }
        }
        return dAdCoeff;
    }

    /** {@inheritDoc} */
    @Override
    public void
        addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                          // Note : PMD false positive here
                          final double density, final Vector3D acceleration, final Vector3D relativeVelocity,
                          final boolean computeGradientPosition, final boolean computeGradientVelocity)
            throws PatriusException {
        // arrays initialization
        final double[][] dAdPos = new double[3][3];
        final double[][] dAdVel = new double[3][3];

        // only the jacobians with respect to position and velocity are computed

        // Loop on all parts having an aero cross section property
        for (final IPart part : this.aeroSphereParts) {
            for (int i = 0; i < 3; i++) {
                Arrays.fill(dAdPos[i], 0.);
                Arrays.fill(dAdVel[i], 0.);
            }
            // Compute the partial derivatives with respect to position and velocity for the part
            this.computeDAccSphere(s, density, acceleration, relativeVelocity, dAdPos, dAdVel, part,
                computeGradientPosition, computeGradientVelocity);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    // add new position and new velocities
                    dAccdPos[i][j] += dAdPos[i][j];
                    dAccdVel[i][j] += dAdVel[i][j];
                }
            }
        }

        // Loop on all parts having an aero facet property
        for (final IPart part : this.aeroFacetParts) {
            for (int i = 0; i < 3; i++) {
                Arrays.fill(dAdPos[i], 0.);
                Arrays.fill(dAdVel[i], 0.);
            }
            if (computeGradientVelocity) {
                // Compute the partial derivatives with respect to position and velocity for a facet
                this.computeDAccFacet(s, density, acceleration, relativeVelocity, dAdPos, dAdVel, part);
            }
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    // add new position and new velocities
                    dAccdPos[i][j] += dAdPos[i][j];
                    dAccdVel[i][j] += dAdVel[i][j];
                }
            }
        }
    }

    /**
     * put zero values to double[][]
     * 
     * @param dAd double array
     */
    private void addZeroToArray(final double[][] dAd) {

        for (int i = 0; i < dAd.length; i++) {
            for (int j = 0; j < dAd[i].length; j++) {
                dAd[i][j] += 0.0;
            }
        }
    }

    /**
     * Compute the partial derivatives with respect to position and velocity for a facet.
     * 
     * @param s
     *        the spacecraft state.
     * @param density
     *        the atmosphere density.
     * @param acceleration
     *        the spacecraft acceleration in the inertial frame.
     * @param relativeVelocity
     *        the spacecraft velocity relative to the atmosphere.
     * @param dAdPos
     *        acceleration derivatives with respect to spacecraft position.
     * @param dAdVel
     *        acceleration derivatives with respect to spacecraft velocity.
     * @param part
     *        the part of the spacecraft (a facet)
     * @throws PatriusException
     *         orekit frame exception
     */
    //CHECKSTYLE: stop MethodLength check
    private void computeDAccFacet(final SpacecraftState s, final double density, final Vector3D acceleration,
                                  final Vector3D relativeVelocity, final double[][] dAdPos, final double[][] dAdVel,
                                  final IPart part) throws PatriusException {
        //CHECKSTYLE: resume MethodLength check
        // Get the the surface of the facet:
        final AeroFacetProperty aeroFacetProp = (AeroFacetProperty) part.getProperty(PropertyType.AERO_FACET);
        final double surf = aeroFacetProp.getFacet().getArea();
        // Get the aerodynamic coefficient:
        final double cdn = aeroFacetProp.getNormalCoef().value(s);
        final double cdt = aeroFacetProp.getTangentialCoef().value(s);
        // Partial derivatives with respect to spacecraft position (they are zero):
        addZeroToArray(dAdPos);

        // Orientation of the facet: transform from part to spacecraft root:
        final Transform partToMainPart = part.getFrame().getTransformTo(this.assembly.getMainPart().getFrame(),
            s.getDate());

        // Facet in spacecraft frame:
        Vector3D fNorm = partToMainPart.transformVector(aeroFacetProp.getFacet().getNormal().normalize());
        fNorm = fNorm.normalize();

        // Relative velocity in the spacecraft frame:
        final Rotation r = s.getAttitude().getRotation();
        final Vector3D relativeVelocitySat = r.applyInverseTo(relativeVelocity);
        final double vx = relativeVelocitySat.getX();
        final double vy = relativeVelocitySat.getY();
        final double vz = relativeVelocitySat.getZ();
        // Orientation of the facet in the spacecraft frame:
        final double orientation = Vector3D.dotProduct(relativeVelocitySat.normalize(), fNorm);

        // The drag force only applies "outside" (i.e. when the orientation is negative):
        if (orientation < 0.) {

            final double b = MathLib.divide(density * surf, this.massModel.getTotalMass(s));

            final double alpha = Vector3D.angle(relativeVelocitySat, fNorm);

            final double[] sincos = MathLib.sinAndCos(alpha);
            final double sinA = sincos[0];
            final double cosA = sincos[1];
            final double bDn = b * cdn * cosA;
            final double bDt = b * cdt;
            final double vRelNorm = relativeVelocitySat.getNorm();

            // derivatives of the norm of the normal acceleration with respect to the velocity
            final double dAdnX = bDn * (vx * cosA + vRelNorm * (fNorm.getX() - MathLib.divide(vx * cosA, vRelNorm)));
            final double dAdnY = bDn * (vy * cosA + vRelNorm * (fNorm.getY() - MathLib.divide(vy * cosA, vRelNorm)));
            final double dAdnZ = bDn * (vz * cosA + vRelNorm * (fNorm.getZ() - MathLib.divide(vz * cosA, vRelNorm)));
            double dAdtX = 0;
            double dAdtY = 0;
            double dAdtZ = 0;

            // derivatives of the norm of the tangent acceleration with respect to the velocity
            if (alpha < Precision.EPSILON) {
                final double coef = -1. / 2. / MathLib.sqrt(3.) * bDt * vRelNorm;
                dAdtX = coef;
                dAdtY = coef;
                dAdtZ = coef;
            } else {
                dAdtX = bDt * (vx * sinA * cosA + MathLib.divide(1. / 2. * vRelNorm * (1. - 2. * cosA * cosA), sinA) *
                        (fNorm.getX() - MathLib.divide(vx * cosA, vRelNorm)));
                dAdtY = bDt * (vy * sinA * cosA + MathLib.divide(1. / 2. * vRelNorm * (1. - 2. * cosA * cosA), sinA) *
                        (fNorm.getY() - MathLib.divide(vy * cosA, vRelNorm)));
                dAdtZ = bDt * (vz * sinA * cosA + MathLib.divide(1. / 2. * vRelNorm * (1. - 2. * cosA * cosA), sinA) *
                        (fNorm.getZ() - MathLib.divide(vz * cosA, vRelNorm)));
            }

            final Vector3D dAdDn = new Vector3D(dAdnX, dAdnY, dAdnZ);
            final Vector3D dAdDt = new Vector3D(dAdtX, dAdtY, dAdtZ);

            final double tangentAccNorm = calculateTangentAccNorm(dAdDt, relativeVelocitySat, acceleration, fNorm);

            Vector3D[] dTdV = { Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO };
            // derivatives of the tangent vector with respect to the velocity
            // only if the tangent acceleration is not null to avoid divisions by zero
            if (tangentAccNorm > Precision.EPSILON) {
                // cosinus Alpha derivatives
                final double[] dCosAdV = {
                    MathLib.divide(fNorm.getX(), vRelNorm) - MathLib.divide(cosA * vx, vRelNorm * vRelNorm),
                    MathLib.divide(fNorm.getY(), vRelNorm) - MathLib.divide(cosA * vy, vRelNorm * vRelNorm),
                    MathLib.divide(fNorm.getZ(), vRelNorm) - MathLib.divide(cosA * vz, vRelNorm * vRelNorm) };

                // computation of the derivatives of each tangent vector element with respect to each velocity
                // vector elements
                dTdV = this.computeTangentVectDerivativeWRTvelocity(dCosAdV, vx, vy, vz, cosA, vRelNorm, fNorm);
            }

            // dAy/dVx = (dAt/dVx * Ty) + (dTy / dVx * At) + (dAn / dVx) with
            // At = Tangent acceleration norm
            // An = Normal acceleration norm
            // Ty = y term of the tangent vector
            final Vector3D dtdv1 = new Vector3D(tangentAccNorm, dTdV[0]);
            final Vector3D dtdv2 = new Vector3D(tangentAccNorm, dTdV[1]);
            final Vector3D dtdv3 = new Vector3D(tangentAccNorm, dTdV[2]);

            final Vector3D v1Sat =
                new Vector3D(dAdDt.getX(), this.tangentVector).add(new Vector3D(dAdDn.getX(), fNorm))
                    .subtract(dtdv1);
            final Vector3D v2Sat =
                new Vector3D(dAdDt.getY(), this.tangentVector).add(new Vector3D(dAdDn.getY(), fNorm))
                    .subtract(dtdv2);
            final Vector3D v3Sat =
                new Vector3D(dAdDt.getZ(), this.tangentVector).add(new Vector3D(dAdDn.getZ(), fNorm))
                    .subtract(dtdv3);

            final Vector3D v1 = r.applyTo(v1Sat);
            final Vector3D v2 = r.applyTo(v2Sat);
            final Vector3D v3 = r.applyTo(v3Sat);
            // Partial derivatives with respect to spacecraft velocity:
            dAdVel[0][0] += v1.getX();
            dAdVel[0][1] += v2.getX();
            dAdVel[0][2] += v3.getX();
            dAdVel[1][0] += v1.getY();
            dAdVel[1][1] += v2.getY();
            dAdVel[1][2] += v3.getY();
            dAdVel[2][0] += v1.getZ();
            dAdVel[2][1] += v2.getZ();
            dAdVel[2][2] += v3.getZ();
        }
    }

    /**
     * calculate tangent acceleration
     * 
     * @param dAdDt
     *        acceleration derivatives with respect to spacecraft position.
     * @param relativeVelocitySat
     *        the spacecraft velocity relative to the atmosphere.
     * @param acceleration
     *        the spacecraft acceleration in the inertial frame.
     * @param fNorm
     *        Facet in spacecraft frame
     * @return tangent acceleration
     */
    private double calculateTangentAccNorm(final Vector3D dAdDt, final Vector3D relativeVelocitySat,
            final Vector3D acceleration, final Vector3D fNorm) {
        // Tangent vector for the normal relative to the velocity
        this.tangentVector = Vector3D.ZERO;
        double tangentAccNorm = 0.;

        // If the norm for the tangent vector is big enough, we try to compute the tangent vector.
        if (MathLib.abs(dAdDt.getNorm()) > Precision.EPSILON) {

            // Projection of the velocity vector on the normal.
            final Vector3D multVelo = new Vector3D(Vector3D.dotProduct(fNorm, relativeVelocitySat), fNorm);
            // The tangent vector is the normalized difference between the velocity
            // and the projection of the velocity on the normal.
            final Vector3D multTangentVector = relativeVelocitySat.subtract(multVelo);
            if (multTangentVector.getNorm() > Precision.EPSILON) {
                // The difference is big enough for the tangent vector to have a meaning.
                this.tangentVector = multTangentVector.normalize();
                tangentAccNorm = Vector3D.dotProduct(acceleration, this.tangentVector);
            }
            // If the difference isn't big enough,
            // it means the velocity is parallel to the normal,
            // so the "tangent" is meaningless.
            // tangentVector remains at ZERO with no ill effect.
        }

        return tangentAccNorm;
    }

    /**
     * Computes the derivatives of the tangent vector elements with respect to the velocity vector elements.
     * 
     * @param dCosAdV
     *        derivatives of cos(alpha) with respect to Vx, Vy and Vz
     * @param vx
     *        Vx
     * @param vy
     *        Vy
     * @param vz
     *        Vz
     * @param cosA
     *        cosinus(alpha)
     * @param vRelNorm
     *        norm of the velocity
     * @param fNorm
     *        facet normal vector
     * @return the derivatives of the tangent vector elements with respect to the velocity vector elements
     */
    private Vector3D[] computeTangentVectDerivativeWRTvelocity(final double[] dCosAdV, final double vx,
                                                               final double vy, final double vz, final double cosA,
                                                               final double vRelNorm, final Vector3D fNorm) {

        // common parameters
        this.vCosA = vRelNorm * cosA;
        this.vCosA2 = vRelNorm * cosA * cosA;
        this.v3CosA = vRelNorm * vRelNorm * vRelNorm * cosA;
        this.tRelNorm = MathLib
            .sqrt(
            (fNorm.getX() - MathLib.divide(vx, this.vCosA)) * (fNorm.getX() - MathLib.divide(vx, this.vCosA))
                 + (fNorm.getY() - MathLib.divide(vy, this.vCosA)) * (fNorm.getY() - MathLib.divide(vy, this.vCosA))
                 + (fNorm.getZ() - MathLib.divide(vz, this.vCosA)) * (fNorm.getZ() - MathLib.divide(vz, this.vCosA)));

        final double[] v = { vx, vy, vz };

        // derivative dXt / dXv
        final double[] lastTermXX = { 1.0, 1.0, 0.0, 0.0 };
        final double dXtdXv = this.computDE(dCosAdV[0], vx, vx, v, this.tangentVector.getX(), lastTermXX);

        // derivative dXt / dYv
        final double[] lastTermXY = { 0.0, 0.0, 1.0, 0.0 };
        final double dXtdYv = this.computDE(dCosAdV[1], vx, vy, v, this.tangentVector.getX(), lastTermXY);

        // derivative dXt / dZv
        final double[] lastTermXZ = { 0.0, 0.0, 0.0, 1.0 };
        final double dXtdZv = this.computDE(dCosAdV[2], vx, vz, v, this.tangentVector.getX(), lastTermXZ);

        // derivative dYt / dXv
        final double[] lastTermYX = { 0.0, 1.0, 0.0, 0.0 };
        final double dYtdXv = this.computDE(dCosAdV[0], vy, vx, v, this.tangentVector.getY(), lastTermYX);

        // derivative dYt / dYv
        final double[] lastTermYY = { 1.0, 0.0, 1.0, 0.0 };
        final double dYtdYv = this.computDE(dCosAdV[1], vy, vy, v, this.tangentVector.getY(), lastTermYY);

        // derivative dYt / dZv
        final double[] lastTermYZ = { 0.0, 0.0, 0.0, 1.0 };
        final double dYtdZv = this.computDE(dCosAdV[2], vy, vz, v, this.tangentVector.getY(), lastTermYZ);

        // derivative dZt / dXv
        final double[] lastTermZX = { 0.0, 1.0, 0.0, 0.0 };
        final double dZtdXv = this.computDE(dCosAdV[0], vz, vx, v, this.tangentVector.getZ(), lastTermZX);

        // derivative dZt / dYv
        final double[] lastTermZY = { 0.0, 0.0, 1.0, 0.0 };
        final double dZtdYv = this.computDE(dCosAdV[1], vz, vy, v, this.tangentVector.getZ(), lastTermZY);

        // derivative dZt / dZv
        final double[] lastTermZZ = { 1.0, 0.0, 0.0, 1.0 };
        final double dZtdZv = this.computDE(dCosAdV[2], vz, vz, v, this.tangentVector.getZ(), lastTermZZ);

        // Vectors containing the results
        final Vector3D res1 = new Vector3D(dXtdXv, dYtdXv, dZtdXv);
        final Vector3D res2 = new Vector3D(dXtdYv, dYtdYv, dZtdYv);
        final Vector3D res3 = new Vector3D(dXtdZv, dYtdZv, dZtdZv);

        // creation of the result array
        return new Vector3D[] { res1, res2, res3 };
    }

    /**
     * Computes a generic element for the tangent vector derivation with respect to the velocity vector.
     * 
     * @param cosDeriv
     *        cos(alpha) derivative with respect to Vx, Vy or Vz
     * @param v1
     *        Vx, Vy or Vz
     * @param v2
     *        Vx, Vy or Vz
     * @param v
     *        relative velocity in the spacecraft frame
     * @param tangentVEctEl
     *        Tx, Ty or Tz
     * @param lastTerm
     *        sets witch term finishes with "-1/(V * cos(alpha))"
     * @return a generic element for the tangent vector derivation with respect to the velocity vector
     */
    private double computDE(final double cosDeriv, final double v1, final double v2, final double[] v,
                            final double tangentVEctEl, final double[] lastTerm) {

        // creation of each term of the sum
        final double de1 = MathLib.divide(v1 * cosDeriv, this.vCosA2) + MathLib.divide(v1 * v2, this.v3CosA)
                - MathLib.divide(lastTerm[0], this.vCosA);
        final double de2 = MathLib.divide(v[0] * cosDeriv, this.vCosA2) + MathLib.divide(v[0] * v2, this.v3CosA)
                - MathLib.divide(lastTerm[1], this.vCosA);
        final double de3 = MathLib.divide(v[1] * cosDeriv, this.vCosA2) + MathLib.divide(v[1] * v2, this.v3CosA)
                - MathLib.divide(lastTerm[2], this.vCosA);
        final double de4 = MathLib.divide(v[2] * cosDeriv, this.vCosA2) + MathLib.divide(v[2] * v2, this.v3CosA)
                - MathLib.divide(lastTerm[3], this.vCosA);

        // one derivative element
        return MathLib.divide(de1, this.tRelNorm)
                - MathLib.divide((de2 * this.tangentVector.getX()
                        + de3 * this.tangentVector.getY()
                        + de4 * this.tangentVector.getZ()) * tangentVEctEl, this.tRelNorm);
    }

    /**
     * Compute the partial derivatives with respect to position and velocity for the part
     * (cylinder, parallelepiped, sphere).
     * 
     * @param s
     *        the spacecraft state.
     * @param density
     *        the atmosphere density.
     * @param acceleration
     *        the spacecraft acceleration in the inertial frame.
     * @param relativeVelocity
     *        relative velocity in state frame
     * @param dAdPos
     *        acceleration derivatives with respect to spacecraft position.
     * @param dAdVel
     *        acceleration derivatives with respect to spacecraft velocity.
     * @param part
     *        the part of the spacecraft (cylinder, parallelepiped, sphere)
     * @param computeGradientPosition
     *        true if partial derivatives with respect to position should be computed
     * @param computeGradientVelocity
     *        true if partial derivatives with respect to position should be computed
     * @throws PatriusException
     *         if some frame specific error occurs
     */
    private void
        computeDAccSphere(final SpacecraftState s, final double density, final Vector3D acceleration,
                          final Vector3D relativeVelocity, final double[][] dAdPos, final double[][] dAdVel,
                          final IPart part,
                          final boolean computeGradientPosition, final boolean computeGradientVelocity)
            throws PatriusException {

        // Get the position of the spacecraft:
        final Vector3D position = s.getPVCoordinates().getPosition();

        // Get the velocity of the spacecraft
        final Vector3D velocity = s.getPVCoordinates().getVelocity();
        final double xp = position.getX();
        final double yp = position.getY();
        final double xv = velocity.getX();
        final double yv = velocity.getY();
        final double zv = velocity.getZ();
        // Get the the surface of the part shape (cylinder, parallelipiped or sphere) :
        final AeroCrossSectionProperty aeroCrossProp = (AeroCrossSectionProperty) part
            .getProperty(PropertyType.AERO_CROSS_SECTION);
        final double surf = aeroCrossProp.getCrossSection(s, relativeVelocity, this.assembly.getMainPart().getFrame(),
            part.getFrame());

        // Get the aerodynamic coefficient:
        final double cd = aeroCrossProp.getDragForce().value(s);

        // Compute temporary values used in computation
        final double b = MathLib.divide(1. / 2. * density * surf * cd, this.massModel.getTotalMass(s));

        // vrel = v - w_Earth x r
        final double xvrel = xv + OMEGA * yp;
        final double yvrel = yv - OMEGA * xp;
        final double zvrel = zv;
        final double c = MathLib.sqrt(xvrel * xvrel + yvrel * yvrel + zvrel * zvrel);

        if (computeGradientVelocity) {
            // Compute gradient velocity dAcc/dv
            final double bOverc = -MathLib.divide(b, c);
            // Partial derivatives with respect to spacecraft velocity:
            // X
            dAdVel[0][0] = bOverc * (xvrel) * (xvrel) - b * c;
            dAdVel[0][1] = bOverc * (yvrel) * (xvrel);
            dAdVel[0][2] = bOverc * zvrel * (xvrel);
            // Y
            dAdVel[1][0] = bOverc * (xvrel) * (yvrel);
            dAdVel[1][1] = bOverc * (yvrel) * (yvrel) - b * c;
            dAdVel[1][2] = bOverc * zvrel * (yvrel);
            // Z
            dAdVel[2][0] = bOverc * (xvrel) * zvrel;
            dAdVel[2][1] = bOverc * (yvrel) * zvrel;
            dAdVel[2][2] = bOverc * zvrel * zvrel - b * c;
        }

        if (computeGradientPosition) {
            // Compute gradient position as :
            // dAcc / dr = (- 1/2 * Cd * S / m) * ||vrel|| vrel drho/dr - [dAcc/dv] * X(w_Earth)
            // with X(omega_Earth) = [0 -w_Earth 0]
            // [w_Earth 0 0]
            // [ 0 0 0]

            // Compute density derivatives wrt position with finite differences on altitude :
            final double[] dRho = new double[3];
            final Vector3D posNewAlt =
                position.scalarMultiply(1.0 + MathLib.divide(this.altitudeStep, position.getNorm()));
            final double densityDH = this.atmosphere.getDensity(s.getDate(), posNewAlt, s.getFrame());
            final double dRhodH = MathLib.divide((densityDH - density), this.altitudeStep);
            final double longGeodCIRF = MathLib.atan2(position.getY(), position.getX());
            final double latGeod = this.earthShape.transform(position, s.getFrame(), s.getDate()).getLatitude();
            final double coslatGeod = MathLib.cos(latGeod);
            dRho[0] = MathLib.cos(longGeodCIRF) * coslatGeod * dRhodH;
            dRho[1] = MathLib.sin(longGeodCIRF) * coslatGeod * dRhodH;
            dRho[2] = MathLib.sin(latGeod) * dRhodH;

            if (!computeGradientVelocity) {
                // Throw exception since partial derivative wrt postion need partial derivative wrt velocity
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.PDB_AERO_DERIVATIVES_COMPUTATION_ERROR);
            }

            // Compute temporary value used in computation (- 1/2 * Cd * S / m)
            final double bOverRho = MathLib.divide(1. / 2. * surf * cd, this.massModel.getTotalMass(s));

            // dacc/dr
            dAdPos[0][0] = -bOverRho * c * (xvrel * dRho[0]) - OMEGA * dAdVel[0][1];
            dAdPos[0][1] = -bOverRho * c * (xvrel * dRho[1]) + OMEGA * dAdVel[0][0];
            dAdPos[0][2] = -bOverRho * c * (xvrel * dRho[2]);
            dAdPos[1][0] = -bOverRho * c * (yvrel * dRho[0]) - OMEGA * dAdVel[1][1];
            dAdPos[1][1] = -bOverRho * c * (yvrel * dRho[1]) + OMEGA * dAdVel[1][0];
            dAdPos[1][2] = -bOverRho * c * (yvrel * dRho[2]);
            dAdPos[2][0] = -bOverRho * c * (zvrel * dRho[0]) - OMEGA * dAdVel[2][1];
            dAdPos[2][1] = -bOverRho * c * (zvrel * dRho[1]) + OMEGA * dAdVel[2][0];
            dAdPos[2][2] = -bOverRho * c * (zvrel * dRho[2]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // return all parameters
        return this.getParameters();
    }

    /** {@inheritDoc} */
    @Override
    public DragSensitive copy(final Assembly newAssembly) {
        return new AeroModel(newAssembly, this.atmosphere, this.earthShape, this.altitudeStep);
    }
}
