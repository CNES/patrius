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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:FA:FA-2466:27/05/2020:Bug dans la classe RediffusedRadiativeModel
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:106:16/07/2013:Account of massless parts with radiative
 * props. Account of parts with mass and without radiative properties.
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::FA:270:05/09/2014:change mass acquisition
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:318:05/11/2014:anomalies correction for class RediffusedFlux
 * VERSION::FA:367:21/11/2014:Recette V2.3 corrections (from FA 270)
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * VERSION::FA:412:05/05/2015:Changed IParamDiffFunction into Parameter in RadiativeProperty
 * VERSION::FA:461:11/06/2015:Corrected partial derivatives computation for rediffused PRS
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:1420:24/11/2017:updateMainPartFrame() speed-up
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeCrossSectionProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeIRProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.forces.radiation.ElementaryFlux;
import fr.cnes.sirius.patrius.forces.radiation.RediffusedRadiationSensitive;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
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
 * Class that represents a rediffused radiative model, based on the vehicle.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The use of a frame (Assembly attribute) linked to the tree
 *                      of frames in each of the parts makes this class not thread-safe.
 * 
 * @author clauded
 * 
 * @version $Id$
 * 
 * @since 1.2
 */
public final class RediffusedRadiativeModel extends Parameterizable
    implements RediffusedRadiationSensitive {

    /** Parameter name for K0 albedo global coefficient. */
    public static final String K0ALBEDO_COEFFICIENT = "K0 albedo coefficient";
    /** Parameter name for K0 infrared global coefficient. */
    public static final String K0IR_COEFFICIENT = "K0 infrared coefficient";

    /** Serializable UID. */
    private static final long serialVersionUID = 8286763516291639540L;

    /** -4. */
    private static final double C_N4 = -4.;

    /** -9.0 */
    private static final double C_9 = 9.;

    /** The considered vehicle on which the model is based on. */
    private final Assembly assembly;

    /** List of parts from the assembly which describe rediffused radiative properties. */
    private final List<IPart> props;

    /** calculation indicator of the albedo force */
    private final boolean albedo;

    /** calculation indicator of the infrared force */
    private final boolean ir;

    /** mass model of satellite */
    private final MassProvider massModel;

    /** derivative relative to the K0 albedo coefficient */
    private final double[] dAccK0AlParam;

    /** derivative relative to the K0 infrared coefficient */
    private final double[] dAccK0IRParam;

    /** derivative relative to the specular thermo-optic coefficient */
    private final Map<Parameter, double[]> dAccSpeParam;

    /** derivative relative to the diffused thermo-optic coefficient */
    private final Map<Parameter, double[]> dAccDiffParam;

    /** derivative relative to the absorption thermo-optic coefficient */
    private final Map<Parameter, double[]> dAccAbsParam;

    /** Parameter for K0 albedo global coefficient. */
    private final Parameter k0Albedo;

    /** Parameter for K0 infrared global coefficient. */
    private final Parameter k0Ir;

    /** Boolean indicating if attitude computation is required (for computation speed-up). */
    private boolean needAttitude;

    /**
     * Rediffused radiative model (the acceleration is computed from all the sub parts of the vehicle).
     * 
     * @param inAlbedo
     *        albedo indicator
     * @param inIr
     *        infrared indicator
     * @param inK0Albedo
     *        albedo global multiplicative factor
     * @param inK0Ir
     *        infrared global multiplicative factor
     * @param inAssembly
     *        the considered vehicle.
     */
    public RediffusedRadiativeModel(final boolean inAlbedo, final boolean inIr, final double inK0Albedo,
            final double inK0Ir, final Assembly inAssembly) {
        this(inAlbedo, inIr, new Parameter(K0ALBEDO_COEFFICIENT, inK0Albedo),
                new Parameter(K0IR_COEFFICIENT, inK0Ir), inAssembly);
    }

    /**
     * Rediffused radiative model (the acceleration is computed from all the sub parts of the vehicle).
     * 
     * @param inAlbedo
     *        albedo indicator
     * @param inIr
     *        infrared indicator
     * @param inK0Albedo
     *        albedo global multiplicative factor parameter
     * @param inK0Ir
     *        infrared global multiplicative factor parameter
     * @param inAssembly
     *        the considered vehicle.
     */
    public RediffusedRadiativeModel(final boolean inAlbedo, final boolean inIr, final Parameter inK0Albedo,
            final Parameter inK0Ir, final Assembly inAssembly) {
        super();

        // Set the albedo and ir values (which will be used in the buildPartsListWithProps() method
        this.albedo = inAlbedo;
        this.ir = inIr;

        // Store the assembly and extract the parts which describe RADIATIVE and RADIATIVE_CROSS_SECTION or
        // RADIATIVE_FACET) properties
        this.checkAssemblyProperties(inAssembly);
        this.assembly = inAssembly;
        this.props = this.buildPartsListWithProps();

        // construct the mass model
        this.massModel = new MassModel(this.assembly);
        if (this.massModel.getTotalMass() < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_RADIATIVE_MASS_PROPERTIES);
        }

        this.dAccK0AlParam = new double[3];
        this.dAccK0IRParam = new double[3];
        this.dAccSpeParam = new HashMap<>();
        this.dAccDiffParam = new HashMap<>();
        this.dAccAbsParam = new HashMap<>();

        this.k0Albedo = inK0Albedo;
        this.k0Ir = inK0Ir;
        this.addAllParameters(this.k0Albedo, this.k0Ir);

        // Store parameters
        for (final IPart part : inAssembly.getParts().values()) {
            this.storeParametersInitDerivatives(part, true, true);
        }
    }

    /**
     * This method tests if the assembly doesn't describe redundant radiative properties (both RADIATIVE_CROSS_SECTION
     * and RADIATIVE_FACET properties described in a same part) and if the attitude is needed (a part has a
     * RADIATIVE_FACET property or a non-spherical part has a RADIATIVE_CROSS_SECTION property).
     * 
     * @param assemblyIn
     *        The considered vehicle
     */
    private void checkAssemblyProperties(final Assembly assemblyIn) {

        this.needAttitude = false;

        for (final IPart part : assemblyIn.getParts().values()) {
            // check the required properties
            if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)
                    && part.hasProperty(PropertyType.RADIATIVE_FACET)) {
                throw PatriusException
                    .createIllegalArgumentException(PatriusMessages.PDB_REDUNDANT_RADIATIVE_PROPERTIES);
            }

            // Check if attitude is needed
            if (!this.needAttitude) { // optimization if the attitude has already been enabled by a previous part
                if (part.hasProperty(PropertyType.RADIATIVE_FACET)
                        || (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)
                        && !(part.getProperty(PropertyType.RADIATIVE_CROSS_SECTION)
                        instanceof RadiativeSphereProperty))) {
                    this.needAttitude = true;
                }
            }
        }
    }

    /**
     * Store parameters and initialize derivatives.
     * 
     * @param part
     *        the tested part of the assembly.
     * @param storeParameters
     *        true if parameters should be stored as Parameter.
     * @param initDerivatives
     *        true if derivatives should be initialized.
     * 
     * @since 3.0.1
     */
    private void storeParametersInitDerivatives(final IPart part, final boolean storeParameters,
                                                final boolean initDerivatives) {

        if (part.hasProperty(PropertyType.RADIATIVE)) {
            final RadiativeProperty radiativeProperty = (RadiativeProperty) part.getProperty(PropertyType.RADIATIVE);
            // storing absorption ratio parameter
            final Parameter absorptionRatio = radiativeProperty.getAbsorptionRatio();
            // storing specular ratio parameter
            final Parameter specularRatio = radiativeProperty.getSpecularReflectionRatio();
            // storing diffusion ratio parameter
            final Parameter diffusionRatio = radiativeProperty.getDiffuseReflectionRatio();

            if (storeParameters) {
                // Adding parameter to internal list
                this.addAllParameters(absorptionRatio, specularRatio, diffusionRatio);
            }
            if (initDerivatives) {
                final double[] initArray = new double[3];
                this.dAccSpeParam.put(specularRatio, initArray.clone());
                this.dAccDiffParam.put(diffusionRatio, initArray.clone());
                this.dAccAbsParam.put(absorptionRatio, initArray.clone());
            }
        }

        if (part.hasProperty(PropertyType.RADIATIVEIR)) {
            final RadiativeIRProperty radiativeIRProperty = (RadiativeIRProperty) part
                .getProperty(PropertyType.RADIATIVEIR);
            // storing absorption ratio parameter
            final Parameter absorptionRatio = radiativeIRProperty.getAbsorptionCoef();
            // storing specular ratio parameter
            final Parameter specularRatio = radiativeIRProperty.getSpecularReflectionCoef();
            // storing diffusion ratio parameter
            final Parameter diffusionRatio = radiativeIRProperty.getDiffuseReflectionCoef();
            if (storeParameters) {
                // Adding parameter to internal list
                this.addAllParameters(absorptionRatio, specularRatio, diffusionRatio);
            }
            if (initDerivatives) {
                final double[] initArray = new double[3];
                this.dAccSpeParam.put(specularRatio, initArray.clone());
                this.dAccDiffParam.put(diffusionRatio, initArray.clone());
                this.dAccAbsParam.put(absorptionRatio, initArray.clone());
            }
        }
    }

    /**
     * Method to compute the rediffused radiation pressure acceleration, based on the assembly.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param flux
     *        elementary flux.
     * @return the acceleration applied on the assembly in SpacecraftState frame.
     * 
     * @throws PatriusException
     *         when an Patrius Exception occurs (no radiative properties found)
     */
    @Override
    public Vector3D rediffusedRadiationPressureAcceleration(final SpacecraftState state,
                                                            final ElementaryFlux[] flux)
        throws PatriusException {

        Vector3D accAlbedo = Vector3D.ZERO;
        Vector3D accIR = Vector3D.ZERO;

        // If the attitude computation is required, update the main part frame's in the assembly
        if (this.needAttitude) {
            this.assembly.updateMainPartFrame(state);
        }

        // Computation of the flux with respect to mass
        final double totalMass = this.massModel.getTotalMass(state);

        // Computation of the albedo mass flux and the IR mass flux if needed
        final double[] fluxAlbedoMass = new double[flux.length];
        final double[] fluxIrMass = new double[flux.length];
        for (int i = 0; i < flux.length; i++) {
            if (this.albedo) {
                fluxAlbedoMass[i] = MathLib.divide(flux[i].getAlbedoPressure(), totalMass);
            }
            if (this.ir) {
                fluxIrMass[i] = MathLib.divide(flux[i].getInfraRedPressure(), totalMass);
            }
        }

        // Computation of the albedo/IR accelerations of each part
        for (final IPart part : this.props) {
            if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)) {
                // When the part has RADIATIVE_CROSS_SECTION property
                if (this.albedo) {
                    final Vector3D partComputedAccAlbedo = computePartAlbedoCrossSectionAcceleration(state, flux,
                        fluxAlbedoMass, part);
                    accAlbedo = accAlbedo.add(partComputedAccAlbedo);
                }
                if (this.ir) {
                    final Vector3D partComputedAccIR = computePartIRCrossSectionAcceleration(state, flux, fluxIrMass,
                        part);
                    accIR = accIR.add(partComputedAccIR);
                }
            } else if (part.hasProperty(PropertyType.RADIATIVE_FACET)) {
                // When the part has RADIATIVE_FACET property
                if (this.albedo) {
                    final Vector3D partComputedAccAlbedo =
                        computePartAlbedoFacetAcc(state, flux, fluxAlbedoMass, part);
                    accAlbedo = accAlbedo.add(partComputedAccAlbedo);
                }
                if (this.ir) {
                    final Vector3D partComputedAccIR = computePartIRFacetAcceleration(state, flux, fluxIrMass, part);
                    accIR = accIR.add(partComputedAccIR);
                }
            } else {
                // Unreachable code since it has been verified that every part had at least one shape property
                throw new IllegalStateException("InternalError");
            }
        }

        // Returns the sum of the two accelerations
        return accAlbedo.add(accIR);
    }

    /**
     * Build the list of parts from the assembly which have rediffused radiative properties.
     * 
     * @return the list of parts with rediffused radiative properties
     */
    private List<IPart> buildPartsListWithProps() {
        // Loop on all parts
        final List<IPart> properties = new ArrayList<>();
        for (final IPart part : this.assembly.getParts().values()) {

            // Test shape properties
            final boolean hasShapeProperties = part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)
                    || part.hasProperty(PropertyType.RADIATIVE_FACET);

            if (hasShapeProperties) {
                // Test if the part has, either radiative (with albedo on) or radiartiveIR (with ir on) property
                final boolean hasRadiativeProperty = (this.albedo && part.hasProperty(PropertyType.RADIATIVE)) ||
                        (this.ir && part.hasProperty(PropertyType.RADIATIVEIR));

                if (hasRadiativeProperty) {
                    // Then, a rediffused radiative force should be computed on this part
                    properties.add(part);
                }
            }
        }
        return properties;
    }

    /**
     * Computes computePartAlbedoCrossSectionAcc.
     * 
     * @param state
     *        state
     * @param flux
     *        flux
     * @param fluxAlbedoMass
     *        fluxAlbedoMass
     * @param part
     *        part
     * @return computePartAlbedoCrossSectionAcc
     * @throws PatriusException
     *         from accForFacet
     */
    private Vector3D computePartAlbedoCrossSectionAcceleration(final SpacecraftState state, final ElementaryFlux[] flux,
                                                               final double[] fluxAlbedoMass, final IPart part)
        throws PatriusException {

        // compute the acceleration on the current shape
        final RadiativeProperty radProp = (RadiativeProperty) part.getProperty(PropertyType.RADIATIVE);
        final Parameter ratio = radProp.getDiffuseReflectionRatio();
        final Vector3D partAlbedoComputedAcc = this.accOnSphere(state, part, ratio, flux, fluxAlbedoMass);

        // derivative relative to K0 albedo
        this.dAccK0AlParam[0] += partAlbedoComputedAcc.getX();
        this.dAccK0AlParam[1] += partAlbedoComputedAcc.getY();
        this.dAccK0AlParam[2] += partAlbedoComputedAcc.getZ();

        // Multiply the acceleration by the k coefficient
        return new Vector3D(this.k0Albedo.getValue(), partAlbedoComputedAcc);
    }

    /**
     * Computes computePartIRCrossSectionAcc.
     * 
     * @param state
     *        state
     * @param flux
     *        flux
     * @param fluxIrMass
     *        fluxIrMass
     * @param part
     *        part
     * @return computePartIRCrossSectionAcc
     * @throws PatriusException
     *         from accOnFacet
     */
    private Vector3D computePartIRCrossSectionAcceleration(final SpacecraftState state, final ElementaryFlux[] flux,
                                                           final double[] fluxIrMass, final IPart part)
        throws PatriusException {

        // get the radiative properties of the part
        final RadiativeIRProperty radIRProp = (RadiativeIRProperty) part.getProperty(PropertyType.RADIATIVEIR);
        final Parameter difCoef = radIRProp.getDiffuseReflectionCoef();

        final Vector3D partIrComputedAcc = this.accOnSphere(state, part, difCoef, flux, fluxIrMass);

        // derivative relative to K0 infrared
        this.dAccK0IRParam[0] += partIrComputedAcc.getX();
        this.dAccK0IRParam[1] += partIrComputedAcc.getY();
        this.dAccK0IRParam[2] += partIrComputedAcc.getZ();

        return new Vector3D(this.k0Ir.getValue(), partIrComputedAcc);
    }

    /**
     * Computes computePartAlbedoFacetAcc.
     * 
     * @param state
     *        state
     * @param flux
     *        flux
     * @param fluxAlbedoMass
     *        fluxAlbedoMass
     * @param part
     *        part
     * @return computePartAlbedoFacetAcc
     * @throws PatriusException
     *         from accForFacet
     */
    private
        Vector3D
            computePartAlbedoFacetAcc(final SpacecraftState state,
                                      final ElementaryFlux[] flux, final double[] fluxAlbedoMass, final IPart part)
                throws PatriusException {

        // Get the radiative coefficients
        final RadiativeProperty radProp = (RadiativeProperty) part.getProperty(PropertyType.RADIATIVE);
        final Parameter absRatio = radProp.getAbsorptionRatio();
        final Parameter speRatio = radProp.getSpecularReflectionRatio();
        final Parameter difRatio = radProp.getDiffuseReflectionRatio();

        // get the radiative acceleration
        final Vector3D partAlbedoComputedAcc =
            this.accOnFacet(state, part, absRatio, difRatio, speRatio, flux, fluxAlbedoMass);

        // derivative relative to K0 albedo
        this.dAccK0AlParam[0] += partAlbedoComputedAcc.getX();
        this.dAccK0AlParam[1] += partAlbedoComputedAcc.getY();
        this.dAccK0AlParam[2] += partAlbedoComputedAcc.getZ();

        return new Vector3D(this.k0Albedo.getValue(), partAlbedoComputedAcc);
    }

    /**
     * Computes computePartIRFacetAcc.
     * 
     * @param state
     *        state
     * @param flux
     *        flux
     * @param fluxIrMass
     *        fluxIrMass
     * @param part
     *        part
     * @return computePartIRFacetAcc
     * @throws PatriusException
     *         from accOnFacet
     */
    private Vector3D computePartIRFacetAcceleration(final SpacecraftState state, final ElementaryFlux[] flux,
                                                    final double[] fluxIrMass, final IPart part)
        throws PatriusException {

        // get the radiative properties of the part
        final RadiativeIRProperty radIRProp = (RadiativeIRProperty) part.getProperty(PropertyType.RADIATIVEIR);
        final Parameter absCoef = radIRProp.getAbsorptionCoef();
        final Parameter speCoef = radIRProp.getSpecularReflectionCoef();
        final Parameter difCoef = radIRProp.getDiffuseReflectionCoef();

        final Vector3D partIrComputedAcc = this.accOnFacet(state, part, absCoef, difCoef, speCoef, flux, fluxIrMass);

        // derivative relative to K0 infrared
        this.dAccK0IRParam[0] += partIrComputedAcc.getX();
        this.dAccK0IRParam[1] += partIrComputedAcc.getY();
        this.dAccK0IRParam[2] += partIrComputedAcc.getZ();

        return new Vector3D(this.k0Ir.getValue(), partIrComputedAcc);
    }

    /**
     * Method to compute the acceleration for a facet model.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        the current part of the assembly.
     * @param absCoeff
     *        the current absorption coefficient of the current part.
     * @param diffCoeff
     *        the current diffuse reflection coefficient of the current part.
     * @param speCoeff
     *        the current specular reflection coefficient of the current part.
     * @param flux
     *        elementary rediffused flux in SpacecraftState frame
     * @param fluxMass
     *        elementary rediffused flux / mass of satellite
     * @return the acceleration due to force applied on the plane.
     * 
     * @throws PatriusException
     *         when an Patrius Exception occurs (no radiative properties found)
     */
    private Vector3D accOnFacet(final SpacecraftState state, final IPart part, final Parameter absCoeff,
                                final Parameter diffCoeff, final Parameter speCoeff, final ElementaryFlux[] flux,
                                final double[] fluxMass)
        throws PatriusException {

        Vector3D facetComputedAcc = Vector3D.ZERO;
        final Transform t = part.getFrame().getTransformTo(state.getFrame(), state.getDate());

        // get the RadiativeFacetProperty property
        final RadiativeFacetProperty radFacetProp = (RadiativeFacetProperty) part
            .getProperty(PropertyType.RADIATIVE_FACET);
        final Facet currentFacet = radFacetProp.getFacet();
        final Vector3D currentNormalFacet = t.transformVector(currentFacet.getNormal()).normalize();

        // Partial derivatives arrays
        final double[] dAccSpeParamPart = this.dAccSpeParam.get(speCoeff);
        final double[] dAccDiffParamPart = this.dAccDiffParam.get(diffCoeff);
        final double[] dAccAbsParamPart = this.dAccAbsParam.get(absCoeff);

        for (int i = 0; i < flux.length; i++) {
            final Vector3D fluxDir = flux[i].getDirFlux().negate();

            if (fluxDir.getNorm() > Precision.EPSILON) {

                // orientation of the facet
                final double costh = fluxDir.getX() * currentNormalFacet.getX() + fluxDir.getY()
                        * currentNormalFacet.getY() + fluxDir.getZ() * currentNormalFacet.getZ();

                // Check of orientation of the facet
                if (costh > 0.) {
                    final double ff = -costh * currentFacet.getArea() * fluxMass[i];
                    final double c1 = (diffCoeff.getValue() + absCoeff.getValue()) * ff;
                    final double c2 = 2. * (speCoeff.getValue() * costh + diffCoeff.getValue() / 3.) * ff;

                    facetComputedAcc = facetComputedAcc.add(new Vector3D(c1, fluxDir, c2, currentNormalFacet));

                    // compute derivative relative to the specular thermo-optic coefficient
                    Vector3D v = new Vector3D(2. * ff * costh, currentNormalFacet);
                    dAccSpeParamPart[0] += v.getX();
                    dAccSpeParamPart[1] += v.getY();
                    dAccSpeParamPart[2] += v.getZ();

                    // compute derivative relative to the diffused thermo-optic coefficient
                    v = new Vector3D(ff, fluxDir, 2. / 3., currentNormalFacet);
                    dAccDiffParamPart[0] += v.getX();
                    dAccDiffParamPart[1] += v.getY();
                    dAccDiffParamPart[2] += v.getZ();

                    // compute derivative relative to the absorption thermo-optic coefficient
                    v = new Vector3D(ff, currentNormalFacet);
                    dAccAbsParamPart[0] += v.getX();
                    dAccAbsParamPart[1] += v.getY();
                    dAccAbsParamPart[2] += v.getZ();
                }
            }
        }
        return facetComputedAcc;
    }

    /**
     * Method to compute the acceleration for the part model (spherical, parallelepipedal, cylindral).
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param part
     *        the current part of the assembly.
     * @param diffCoeff
     *        the current diffuse reflection coefficient of the current part.
     * @param flux
     *        elementary rediffused flux vector in SpacecraftState frame
     * @param fluxMass
     *        elementary rediffused flux / mass of satellite
     * @return the acceleration due to force applied on the part (cylinder, parallelepiped, sphere).
     * @throws PatriusException thrown if computation failed
     */
    private Vector3D accOnSphere(final SpacecraftState state, final IPart part, final Parameter diffCoeff,
                                 final ElementaryFlux[] flux, final double[] fluxMass)
        throws PatriusException {

        final RadiativeCrossSectionProperty partRadProp =
            (RadiativeCrossSectionProperty) part.getProperty(PropertyType.RADIATIVE_CROSS_SECTION);

        Vector3D sphereComputedAcc = Vector3D.ZERO;
        final double[] dAccDiffParamPart = this.dAccDiffParam.get(diffCoeff);

        // Loop on each flux element
        for (int i = 0; i < flux.length; i++) {
            final Vector3D dirFlux = flux[i].getDirFlux();
            final Vector3D dirFluxNegate = dirFlux.negate();
            final double fluxMassI = fluxMass[i];

            // Acceleration
            final double crossSection = partRadProp.getCrossSection(state, dirFlux, part.getFrame());
            final double k = -(1. + ((4. / 9.) * diffCoeff.getValue())) * crossSection * fluxMassI;
            sphereComputedAcc = new Vector3D(1.0, sphereComputedAcc, k, dirFluxNegate);

            // Derivatives
            final double d = (C_N4 / C_9) * crossSection * fluxMassI;
            dAccDiffParamPart[0] += d * dirFluxNegate.getX();
            dAccDiffParamPart[1] += d * dirFluxNegate.getY();
            dAccDiffParamPart[2] += d * dirFluxNegate.getZ();
        }

        // Return the acceleration due to force applied on the spherical part
        return sphereComputedAcc;
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDStateRediffusedRadiativePressure(final SpacecraftState s, final double[][] dAccdPos,
                                                         final double[][] dAccdVel) {
        // Nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDParamRediffusedRadiativePressure(final SpacecraftState s, final Parameter param,
                                                         final double[] dAccdParam)
        throws PatriusException {
        // check if the parameter is supported
        this.complainIfNotSupported(param);
        if (this.k0Albedo.equals(param)) {
            // compute the derivative relative to the k0 albedo coefficient
            dAccdParam[0] += this.dAccK0AlParam[0];
            dAccdParam[1] += this.dAccK0AlParam[1];
            dAccdParam[2] += this.dAccK0AlParam[2];
        } else if (this.k0Ir.equals(param)) {
            // compute the derivative relative to the k0 infrared coefficient
            dAccdParam[0] += this.dAccK0IRParam[0];
            dAccdParam[1] += this.dAccK0IRParam[1];
            dAccdParam[2] += this.dAccK0IRParam[2];
        }
        // call complementary method
        this.addDaccDParamContinued(param, dAccdParam);
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
     * Compute acceleration derivatives. Continued from previous method for cyclomatic complexity purposes
     * 
     * @param param
     *        name of the parameter with respect to which derivatives are required
     * @param dAccdParam
     *        acceleration derivatives with respect to specified parameters
     */
    private void addDaccDParamContinued(final Parameter param,
                                        final double[] dAccdParam) {
        for (final IPart part : this.props) {
            if (part.hasProperty(PropertyType.RADIATIVE)) {
                final RadiativeProperty radiativeProperty = (RadiativeProperty) part
                    .getProperty(PropertyType.RADIATIVE);
                // storing absorption ratio parameter
                final Parameter ka = radiativeProperty.getAbsorptionRatio();
                // storing specular ratio parameter
                final Parameter ks = radiativeProperty.getSpecularReflectionRatio();
                // storing diffusion ratio parameter
                final Parameter kd = radiativeProperty.getDiffuseReflectionRatio();
                this.addDaccDParamContinued(param, ka, ks, kd, dAccdParam);
            }
            if (part.hasProperty(PropertyType.RADIATIVEIR)) {
                final RadiativeIRProperty radiativeIRProperty = (RadiativeIRProperty) part
                    .getProperty(PropertyType.RADIATIVEIR);
                // storing absorption ratio parameter
                final Parameter ka = radiativeIRProperty.getAbsorptionCoef();
                // storing specular ratio parameter
                final Parameter ks = radiativeIRProperty.getSpecularReflectionCoef();
                // storing diffusion ratio parameter
                final Parameter kd = radiativeIRProperty.getDiffuseReflectionCoef();
                this.addDaccDParamContinued(param, ka, ks, kd, dAccdParam);
            }
        }
    }

    /**
     * Compute acceleration derivatives. Continued from previous method for cyclomatic complexity purposes
     * 
     * @param param
     *        name of the parameter with respect to which derivatives are required
     * @param ka
     *        absorption ratio parameter
     * @param ks
     *        specular ratio parameter
     * @param kd
     *        the diffusion coefficient
     * @param dAccdParam
     *        acceleration derivatives with respect to specified parameters
     */
    private void addDaccDParamContinued(final Parameter param, final Parameter ka, final Parameter ks,
                                        final Parameter kd, final double[] dAccdParam) {
        if (ka.equals(param)) {
            // compute the derivative relative to the absorption coefficient
            final double[] dAccAbsParamPart = this.dAccAbsParam.get(ka);
            dAccdParam[0] += dAccAbsParamPart[0];
            dAccdParam[1] += dAccAbsParamPart[1];
            dAccdParam[2] += dAccAbsParamPart[2];
        } else if (kd.equals(param)) {
            // compute the derivative relative to the diffusion coefficient
            final double[] dAccDiffParamPart = this.dAccDiffParam.get(kd);
            dAccdParam[0] += dAccDiffParamPart[0];
            dAccdParam[1] += dAccDiffParamPart[1];
            dAccdParam[2] += dAccDiffParamPart[2];
        } else if (ks.equals(param)) {
            // compute the derivative relative to the specular coefficient
            final double[] dAccSpeParamPart = this.dAccSpeParam.get(ks);
            dAccdParam[0] += dAccSpeParamPart[0];
            dAccdParam[1] += dAccSpeParamPart[1];
            dAccdParam[2] += dAccSpeParamPart[2];
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initDerivatives() {
        Arrays.fill(this.dAccK0AlParam, 0.0);
        Arrays.fill(this.dAccK0IRParam, 0.0);
        for (final IPart part : this.props) {
            this.storeParametersInitDerivatives(part, false, true);
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
    public boolean getFlagAlbedo() {
        return this.albedo;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getFlagIr() {
        return this.ir;
    }

    /** {@inheritDoc} */
    @Override
    public Parameter getK0Albedo() {
        return this.k0Albedo;
    }

    /** {@inheritDoc} */
    @Override
    public Parameter getK0Ir() {
        return this.k0Ir;
    }

    /** {@inheritDoc} */
    @Override
    public Assembly getAssembly() {
        return this.assembly;
    }
}
