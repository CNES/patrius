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
 * @history creation 12/03/2012
 */
/*
 *          HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 *          VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 *          VERSION::FA:178:06/01/2013:Corrected log id format
 *          VERSION::FA:106:16/07/2013:Account of massless parts with
 *          radiative props. Account of parts with mass and without
 *          radiative properties.
 *          VERSION::DM:85:23/07/2013:Made some common methods static
 *          VERSION::FA:86:22/10/2013:New mass management system
 *          VERSION::FA:93:01/04/2014:Changed partial derivatives API
 *          VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 *          VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 *          VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 *          VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 *          VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 *          VERSION::FA:412:05/05/2015:Deleted massParam in DirectRadiativeModel
 *          VERSION::FA:439:12/06/2015:Corrected partial derivatives computation for PRS
 *          VERSION::FA:566:02/03/2016:Corrected partial derivatives computation for PRS wrt k0
 *          VERSION::FA:673:12/09/2016: add getTotalMass(state)
 *          VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 *          VERSION::DM:834:04/04/2017:create vehicle object
 *          VERSION::DM:1420:24/11/2017:updateMainPartFrame() speed-up
 *          VERSION::FA:1799:04/10/2018:correction Javadoc
 *          VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 *          VERSION::FA:1970:03/01/2019:quality corrections
 *          END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeCrossSectionProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * 
 * <p>
 * Class that represents a radiative model, based on the vehicle.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of a frame (Assembly attribute) linked to the
 *                      tree of frames in each of the parts makes this class not
 *                      thread-safe.
 * 
 * @author mercadierg
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class DirectRadiativeModel extends Parameterizable implements RadiationSensitive {

    /** Parameter name for K0 coefficient. */
    public static final String K0_COEFFICIENT = "K0 coefficient";

    /** Serializable UID. */
    private static final long serialVersionUID = -6974428498597665423L;

    /** CONST 9. */
    private static final double C_9 = 9.0;

    /** CONST -4. */
    private static final double C_N4 = -4.0;

    /** CONST -2.0 */
    private static final double C_N2 = -2.;

    // Parameter used to modelize ratios in radiative properties
    /** K0 coefficient parameter */
    private Parameter k0 = null;

    /** The considered vehicle on which the model is based on. */
    private final Assembly assembly;

    /** Mass model */
    private final MassProvider massModel;

    /**
     * Boolean indicating if attitude computation is required (for computation
     * speed-up).
     */
    private boolean needAttitude;

    /**
     * Radiative model (the acceleration is computed from all the sub parts of
     * the vehicle).
     * 
     * @param inAssembly
     *        The considered vehicle.
     */
    public DirectRadiativeModel(final Assembly inAssembly) {
        this(inAssembly, 1.0);
    }

    /**
     * Radiative model (the acceleration is computed from all the sub parts of
     * the vehicle).
     * 
     * @param inAssembly
     *        The considered vehicle.
     * @param inK0
     *        global multiplicative factor
     */
    public DirectRadiativeModel(final Assembly inAssembly, final double inK0) {
        this(inAssembly, new Parameter(K0_COEFFICIENT, inK0));
    }

    /**
     * Radiative model (the acceleration is computed from all the sub parts of
     * the vehicle).
     * 
     * @param inAssembly
     *        The considered vehicle.
     * @param inK0
     *        global multiplicative factor
     */
    public DirectRadiativeModel(final Assembly inAssembly, final Parameter inK0) {
        super(inK0);
        this.k0 = inK0;
        this.checkAssemblyProperties(inAssembly);
        this.assembly = inAssembly;
        this.massModel = new MassModel(this.assembly);
    }

    /**
     * 
     * This method tests if the required properties exist (RADIATIVE and
     * RADIATIVE_CROSS_SECTION or RADIATIVE_FACET). At least one part with a
     * mass is required.
     * 
     * @param assemblyIn
     *        The considered vehicle.
     */
    private void checkAssemblyProperties(final Assembly assemblyIn) {

        boolean hasRadProp = false;
        boolean hasMassProp = false;

        this.needAttitude = false;

        for (final IPart part : assemblyIn.getParts().values()) {
            if ((part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION) ^ part
                .hasProperty(PropertyType.RADIATIVE_FACET))
                    && (part.hasProperty(PropertyType.RADIATIVE))) {
                hasRadProp |= true;
                // Storing absorption ratio parameter
                final Parameter absorptionRatio = ((RadiativeProperty) part
                    .getProperty(PropertyType.RADIATIVE)).getAbsorptionRatio();
                // Storing specular ratio parameter
                final Parameter specularRatio = ((RadiativeProperty) part
                    .getProperty(PropertyType.RADIATIVE)).getSpecularReflectionRatio();
                // Storing diffusion ratio parameter
                final Parameter diffusionRatio = ((RadiativeProperty) part
                    .getProperty(PropertyType.RADIATIVE)).getDiffuseReflectionRatio();

                // Adding parameter to internal list
                this.addAllParameters(absorptionRatio, specularRatio, diffusionRatio);

                // Check if attitude is needed
                if (part.hasProperty(PropertyType.RADIATIVE_FACET)
                        || (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION) && !(part
                            .getProperty(PropertyType.RADIATIVE_CROSS_SECTION) instanceof RadiativeSphereProperty))) {
                    this.needAttitude = true;
                }
            }

            if (part.hasProperty(PropertyType.MASS)) {
                hasMassProp |= true;
            }

        }

        if (!hasRadProp || !hasMassProp) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.PDB_NO_RADIATIVE_MASS_PROPERTIES);
        }
    }

    /**
     * Method to compute the radiation pressure acceleration, based on the
     * assembly.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param flux
     *        the incoming flux.
     * @return the acceleration applied on the assembly.
     * @throws PatriusException
     *         when an error occurs.
     * 
     */
    @Override
    public Vector3D
            radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux)
                throws PatriusException {

        if (this.needAttitude) {
            this.assembly.updateMainPartFrame(state);
        }

        Vector3D partComputedForce;
        Vector3D force = Vector3D.ZERO;

        for (final IPart part : this.assembly.getParts().values()) {

            // force applied on the current part
            partComputedForce = Vector3D.ZERO;

            if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)) {
                // compute the force for the current shape (cylinder,
                // parallelepiped, sphere)
                partComputedForce = forceOnSphere(state, part, flux);
            }

            if (part.hasProperty(PropertyType.RADIATIVE_FACET)) {
                // compute the force for the current facet
                partComputedForce = forceOnFacet(state, part, flux);
            }

            // add the contribution to the total force
            force = force.add(partComputedForce);

        }
        final Vector3D forceMass = new Vector3D(MathLib.divide(1.0, this.massModel.getTotalMass(state)), force);

        // apply K0 coefficient and return total acceleration applied on the assembly
        return forceMass.scalarMultiply(this.k0.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public void addDSRPAccDState(final SpacecraftState s, final double[][] dAccdPos,
                                 final double[][] dAccdVel, final Vector3D satSunVector) {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void addDSRPAccDParam(final SpacecraftState s, final Parameter param,
                                 final double[] dAccdParam, final Vector3D satSunVector) throws PatriusException {
        this.complainIfNotSupported(param);

        if (this.k0.equals(param)) {
            // compute the derivative relative to the k0 coefficient
            final Vector3D flux = satSunVector.normalize().negate();
            final Vector3D radPressAcc = this.radiationPressureAcceleration(s, flux);
            dAccdParam[0] += MathLib.divide(radPressAcc.getX(), this.k0.getValue());
            dAccdParam[1] += MathLib.divide(radPressAcc.getY(), this.k0.getValue());
            dAccdParam[2] += MathLib.divide(radPressAcc.getZ(), this.k0.getValue());
        } else {
            // call complementary method
            final double[] dAccdTPart = new double[3];

            final double totalMass = this.massModel.getTotalMass(s);

            for (final IPart part : this.assembly.getParts().values()) {
                // computation for the parts with the required properties only

                if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)) {
                    // compute the derivative for the current shape (cylinder,
                    // parallelepiped, sphere)
                    computeDAccDParamSphere(param, s, part, satSunVector, dAccdTPart);
                }

                if (part.hasProperty(PropertyType.RADIATIVE_FACET)) {
                    // compute the derivative for the current facet
                    computeDAccDParamFacet(param, s, part, satSunVector, dAccdTPart);
                }
            }
            // Compute acceleration derivatives with respect to input param
            final double firstPartThermo = MathLib.divide(1., totalMass);
            dAccdParam[0] += firstPartThermo * dAccdTPart[0];
            dAccdParam[1] += firstPartThermo * dAccdTPart[1];
            dAccdParam[2] += firstPartThermo * dAccdTPart[2];
        }
    }

    /**
     * Use this method to know if the jacobian parameter is Handled
     * 
     * @param param
     *        the name of the parameter to check
     * @exception PatriusException
     *            if the parameter is not supported
     * @since 2.3
     */
    private void complainIfNotSupported(final Parameter param) throws PatriusException {
        // Check if the parameter if handled
        if (!this.supportsParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param.getName());
        }
    }

    /**
     * Method to compute the force for a spherical model.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        the current part of the assembly.
     * @param flux
     *        the incoming flux.
     * @return the force applied on the part (cylinder, parallelepiped, sphere).
     * @throws PatriusException
     *         thrown if computation failed
     */
    protected static Vector3D forceOnSphere(final SpacecraftState state, final IPart part,
                                            final Vector3D flux)
        throws PatriusException {

        // composite radiation pressure coefficient
        final double kP = computeSphereKP(state, part, flux);

        // compute the force applied on the current part
        return new Vector3D(kP, flux);
    }

    /**
     * Method to compute the force for a plane model.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        the current part of the assembly.
     * @param flux
     *        the incoming flux.
     * @return the force applied on the plane.
     * @throws PatriusException
     *         orekit frame exception
     */
    protected static Vector3D forceOnFacet(final SpacecraftState state, final IPart part,
                                           final Vector3D flux) throws PatriusException {

        Vector3D facetComputedForce = Vector3D.ZERO;
        final double fluxNorm = flux.getNorm();
        if (fluxNorm > Precision.EPSILON) {

            // radiation flux in inertial frame
            final Transform t = part.getFrame().getTransformTo(state.getFrame(), state.getDate());

            // get the RadiativeFacetProperty property
            final RadiativeFacetProperty radFacetProp = (RadiativeFacetProperty) part
                .getProperty(PropertyType.RADIATIVE_FACET);
            final Facet currentFacet = radFacetProp.getFacet();

            // facet in inertial frame
            final Vector3D facetNormalSc = t.transformVector(currentFacet.getNormal());

            // orientation of the facet
            final Vector3D normalizedFlux = flux.normalize();
            final double orientation = Vector3D.dotProduct(normalizedFlux, facetNormalSc);

            // we admit the facet is part of a closed object;
            // the radiative force thus only applies "outside", i.e. when the
            // orientation is negative:
            if (orientation < Precision.EPSILON) {
                // computed flux
                final double computedFlux = -orientation * currentFacet.getArea() * fluxNorm;

                // get the radiative properties of the part
                final RadiativeProperty radProp = (RadiativeProperty) part
                    .getProperty(PropertyType.RADIATIVE);
                final double absCoeff = radProp.getAbsorptionRatio().getValue();
                final double difCoeff = radProp.getDiffuseReflectionRatio().getValue();
                final double speCoeff = radProp.getSpecularReflectionRatio().getValue();

                // acceleration value along the flux direction
                final double cF = computedFlux * (difCoeff + absCoeff);
                // acceleration value along the normal direction
                final double cN = 2.0 * computedFlux * (speCoeff * orientation - difCoeff / 3);

                facetComputedForce = (normalizedFlux.scalarMultiply(cF)).add(cN, facetNormalSc);
            }
        }
        return facetComputedForce;
    }

    /**
     * 
     * Method to compute the composite radiation pressure coefficient for the
     * part (cylinder, parallelepiped, sphere).
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        the current part of the assembly.
     * @param flux
     *        the incoming flux.
     * @return the composite radiation pressure coefficient.
     * @throws PatriusException
     *         thrown if computation failed
     */
    private static double computeSphereKP(final SpacecraftState state, final IPart part,
                                          final Vector3D flux) throws PatriusException {

        // get the radiative properties of the part
        final RadiativeProperty radProp = (RadiativeProperty) part
            .getProperty(PropertyType.RADIATIVE);

        // get the cross section of the part
        final RadiativeCrossSectionProperty partRadProp = (RadiativeCrossSectionProperty) part
            .getProperty(PropertyType.RADIATIVE_CROSS_SECTION);
        final double crossSection = partRadProp.getCrossSection(state, flux, part.getFrame());
        // compute the composite radiation pressure coefficient
        final double diffReflRatio = radProp.getDiffuseReflectionRatio().getValue();

        return crossSection * (1. + 4. * diffReflRatio / C_9);
    }

    /**
     * Compute the derivative with respect to the mass or the thermo-optic
     * coefficients, for a spherical object.
     * 
     * @param param
     *        name of the parameter with respect to which derivatives are
     *        required
     * @param s
     *        the current state of the spacecraft
     * @param part
     *        part of assembly
     * @param satSunVector
     *        sun-sat vector
     * @param dAcc
     *        the acceleration derivative for the current part (cylinder, parallelepiped, sphere)
     * @throws PatriusException
     *         thrown if computation failed
     */
    private static void computeDAccDParamSphere(final Parameter param, final SpacecraftState s,
                                                final IPart part, final Vector3D satSunVector, final double[] dAcc)
        throws PatriusException {

        // sun-sat vector norm
        final double satSunVectorNorm = satSunVector.getNorm();

        // Get kd for current part
        final Parameter diffusionRatio = ((RadiativeProperty) part
            .getProperty(PropertyType.RADIATIVE)).getDiffuseReflectionRatio();

        if (diffusionRatio.equals(param)) {
            // Derivatives wrt the diffusion reflection coefficient for the part
            // shape :
            final RadiativeCrossSectionProperty radSphereProp =
                (RadiativeCrossSectionProperty) part
                    .getProperty(PropertyType.RADIATIVE_CROSS_SECTION);

            // Compute the acceleration derivative for the current part (cylinder, parallelepiped, sphere)
            final Vector3D flux = satSunVector.normalize().negate();
            final double surface = radSphereProp.getCrossSection(s, flux, part.getFrame());
            final double coef = MathLib.divide((C_N4 / C_9) * surface, satSunVectorNorm);
            dAcc[0] += coef * satSunVector.getX();
            dAcc[1] += coef * satSunVector.getY();
            dAcc[2] += coef * satSunVector.getZ();

        }
    }

    /**
     * Compute the derivative with respect to the mass or the thermo-optic
     * coefficients, for a facet.
     * 
     * @param param
     *        name of the parameter with respect to which derivatives are
     *        required
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        part of assembly
     * @param satSunVector
     *        sun-sat vector
     * @param dAcc
     *        the acceleration derivative for the current facet
     * @throws PatriusException
     *         if some frame specific error occurs
     */
    private static void computeDAccDParamFacet(final Parameter param, final SpacecraftState state,
                                               final IPart part, final Vector3D satSunVector, final double[] dAcc)
        throws PatriusException {

        // Get ka, ks , kd for current part
        final Parameter absorptionRatio = ((RadiativeProperty) part
            .getProperty(PropertyType.RADIATIVE)).getAbsorptionRatio();
        final Parameter specularRatio = ((RadiativeProperty) part
            .getProperty(PropertyType.RADIATIVE)).getSpecularReflectionRatio();
        final Parameter diffusionRatio = ((RadiativeProperty) part
            .getProperty(PropertyType.RADIATIVE)).getDiffuseReflectionRatio();
        if (diffusionRatio.equals(param) || absorptionRatio.equals(param)
                || specularRatio.equals(param)) {
            // Derivatives wrt the thermo-optic coefficients for the facet:
            computeDAccDThermoOpticFacet(param, state, part, satSunVector, dAcc);
        }
    }

    /**
     * Compute the derivatives with respect to the thermo-optic coefficient for
     * the facet
     * 
     * @param param
     *        name of the parameter with respect to which derivatives are
     *        required
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        part of assembly
     * @param satSunVector
     *        sun-sat vector
     * @param dAcc
     *        the acceleration derivatives with respect to the thermo-optic coefficient for the facet
     * @throws PatriusException
     *         if some frame specific error occurs or if the parameter name
     *         is not supported
     */
    private static void computeDAccDThermoOpticFacet(final Parameter param,
                                                     final SpacecraftState state, final IPart part,
                                                     final Vector3D satSunVector, final double[] dAcc)
        throws PatriusException {

        final RadiativeFacetProperty radFacetProp = (RadiativeFacetProperty) part
            .getProperty(PropertyType.RADIATIVE_FACET);
        final Facet currentFacet = radFacetProp.getFacet();
        final Rotation r = state.getAttitude().getRotation();

        // transform from part to spacecraft root
        IPart ppart = part;
        int level = part.getPartLevel();
        while (level > 0) {
            ppart = ppart.getParent();
            level = ppart.getPartLevel();
        }
        final Transform partToMainPart = part.getFrame().getTransformTo(ppart.getFrame(),
            state.getDate());

        // facet normal in sc frame
        final Vector3D facetNormalSc = partToMainPart.transformVector(currentFacet.getNormal());

        final Vector3D orientedFacet = r.applyTo(facetNormalSc);

        final double nf = orientedFacet.getNorm();
        final double cosDirFacetX = MathLib.divide(orientedFacet.getX(), nf);
        final double cosDirFacetY = MathLib.divide(orientedFacet.getY(), nf);
        final double cosDirFacetZ = MathLib.divide(orientedFacet.getZ(), nf);

        // set-sun vector informations
        final double ux = satSunVector.getX();
        final double vy = satSunVector.getY();
        final double wz = satSunVector.getZ();
        final double n = satSunVector.getNorm();

        // orientation of the facet
        final double orientation = Vector3D.dotProduct(satSunVector.negate().normalize(),
            orientedFacet.normalize());

        // Get ka, ks, kd for current part
        final Parameter absorptionRatio = ((RadiativeProperty) part
            .getProperty(PropertyType.RADIATIVE)).getAbsorptionRatio();
        final Parameter specularRatio = ((RadiativeProperty) part
            .getProperty(PropertyType.RADIATIVE)).getSpecularReflectionRatio();
        final Parameter diffusionRatio = ((RadiativeProperty) part
            .getProperty(PropertyType.RADIATIVE)).getDiffuseReflectionRatio();

        // The radiative force thus only applies "outside", i.e. when the orientation is negative:
        if (orientation < Precision.EPSILON) {
            final double a = currentFacet.getArea() * MathLib.abs(orientation);
            if (diffusionRatio.equals(param)) {
                // if param is supported by function for absorption ratio
                double sign = 1.;
                final double q = ux * cosDirFacetX + vy * cosDirFacetY + wz * cosDirFacetZ;
                if (q < 0.0) {
                    sign = -1.;
                }
                dAcc[0] += -a * (MathLib.divide(ux, n) + 2. * cosDirFacetX * sign / 3.);
                dAcc[1] += -a * (MathLib.divide(vy, n) + 2. * cosDirFacetY * sign / 3.);
                dAcc[2] += -a * (MathLib.divide(wz, n) + 2. * cosDirFacetZ * sign / 3.);
            } else if (absorptionRatio.equals(param)) {
                // if param is supported by function for specular ratio
                dAcc[0] += MathLib.divide(-a * ux, n);
                dAcc[1] += MathLib.divide(-a * vy, n);
                dAcc[2] += MathLib.divide(-a * wz, n);
            } else if (specularRatio.equals(param)) {
                // if param is supported by function for diffusion ratio
                final double q = ux * cosDirFacetX + vy * cosDirFacetY + wz * cosDirFacetZ;
                final double coef = MathLib.divide(C_N2 * a * q, n);
                dAcc[0] += coef * cosDirFacetX;
                dAcc[1] += coef * cosDirFacetY;
                dAcc[2] += coef * cosDirFacetZ;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // return all parameters
        return this.getParameters();
    }
}
