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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import fr.cnes.sirius.patrius.frames.Frame;
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

    /**
     * ID
     */
    private static final long serialVersionUID = 8286763516291639540L;

    /** -4. */
    private static final double C_N4 = -4.;

    /** -9.0 */
    private static final double C_9 = 9.;

    /** The considered vehicle on which the model is based on. */
    private final Assembly assembly;

    /** calculation indicator of the albedo force */
    private boolean albedo;

    /** calculation indicator of the infrared force */
    private boolean ir;

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
    private Parameter k0Albedo = null;

    /** Parameter for K0 infrared global coefficient. */
    private Parameter k0Ir = null;

    /** Boolean indicating if attitude computation is required (for computation speed-up). */
    private boolean needAttitude;

    /** List of assembly parts with a flag indicating if it has a radiative property (for computation speed-up). */
    private final Map<IPart, Boolean> props;
    
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
     * @throws PatriusException
     *         when an Patrius Exception occurs (no radiative properties found)
     */
    public RediffusedRadiativeModel(final boolean inAlbedo, final boolean inIr, final double inK0Albedo,
                                    final double inK0Ir, final Assembly inAssembly) throws PatriusException {
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
     * @throws PatriusException
     *         when an Patrius Exception occurs (no radiative properties found)
     */
    public RediffusedRadiativeModel(final boolean inAlbedo, final boolean inIr, final Parameter inK0Albedo,
                                    final Parameter inK0Ir, final Assembly inAssembly) throws PatriusException {
        super();
        this.k0Albedo = inK0Albedo;
        this.k0Ir = inK0Ir;
        this.addAllParameters(this.k0Albedo, this.k0Ir);
        this.assembly = inAssembly;
        this.albedo = inAlbedo;
        this.ir = inIr;

        this.dAccK0AlParam = new double[3];
        this.dAccK0IRParam = new double[3];
        this.dAccSpeParam = new HashMap<Parameter, double[]>();
        this.dAccDiffParam = new HashMap<Parameter, double[]>();
        this.dAccAbsParam = new HashMap<Parameter, double[]>();

        // Store parameters
        for (final IPart part : inAssembly.getParts().values()) {
            this.storeParametersInitDerivatives(part, true, true);
        }

        // construct the mass model
        this.massModel = new MassModel(this.assembly);
        if (this.massModel.getTotalMass() < Precision.DOUBLE_COMPARISON_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_RADIATIVE_MASS_PROPERTIES);
        }

        this.checkAssemblyProperties(inAssembly);
        this.props = this.buildProps();
    }

    /**
     * 
     * This method tests if the required properties exist (RADIATIVE and RADIATIVE_CROSS_SECTION or RADIATIVE_FACET).
     * 
     * @param assemblyIn
     *        The considered vehicle.
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
            if (part.hasProperty(PropertyType.RADIATIVE_FACET) ||
                    (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION) &&
                    !(part.getProperty(PropertyType.RADIATIVE_CROSS_SECTION)
                    instanceof RadiativeSphereProperty))) {
                this.needAttitude = true;
            }
        }
    }

    /**
     * Store parameters and init derivatives.
     * 
     * @param part
     *        the tested part of the assembly.
     * @param storeParameters
     *        true if parameters should be stored as Parameter.
     * @param initDerivatives
     *        true if derivatives should be init.
     * 
     * @since 3.0.1
     */
    private void storeParametersInitDerivatives(final IPart part, final boolean storeParameters,
                                                final boolean initDerivatives) {

        if (part.hasProperty(PropertyType.RADIATIVE)) {
            // storing absorption ratio parameter
            final Parameter absorptionRatio = ((RadiativeProperty) part.
                getProperty(PropertyType.RADIATIVE)).getAbsorptionRatio();
            // storing specular ratio parameter
            final Parameter specularRatio = ((RadiativeProperty) part.
                getProperty(PropertyType.RADIATIVE)).getSpecularReflectionRatio();
            // storing diffusion ratio parameter
            final Parameter diffusionRatio = ((RadiativeProperty) part.
                getProperty(PropertyType.RADIATIVE)).getDiffuseReflectionRatio();

            if (storeParameters) {
                // Adding parameter to internal list
                this.addAllParameters(absorptionRatio, specularRatio, diffusionRatio);
            }
            if (initDerivatives) {
                final double[] initArray = new double[] { 0., 0., 0. };
                this.dAccSpeParam.put(specularRatio, initArray.clone());
                this.dAccDiffParam.put(diffusionRatio, initArray.clone());
                this.dAccAbsParam.put(absorptionRatio, initArray.clone());
            }
        }

        if (part.hasProperty(PropertyType.RADIATIVEIR)) {
            // storing absorption ratio parameter
            final Parameter absorptionRatio = ((RadiativeIRProperty) part.
                getProperty(PropertyType.RADIATIVEIR)).getAbsorptionCoef();
            // storing specular ratio parameter
            final Parameter specularRatio = ((RadiativeIRProperty) part.
                getProperty(PropertyType.RADIATIVEIR)).getSpecularReflectionCoef();
            // storing diffusion ratio parameter
            final Parameter diffusionRatio = ((RadiativeIRProperty) part.
                getProperty(PropertyType.RADIATIVEIR)).getDiffuseReflectionCoef();
            if (storeParameters) {
                // Adding parameter to internal list
                this.addAllParameters(absorptionRatio, specularRatio, diffusionRatio);
            }
            if (initDerivatives) {
                final double[] initArray = new double[] { 0., 0., 0. };
                this.dAccSpeParam.put(specularRatio, initArray.clone());
                this.dAccDiffParam.put(diffusionRatio, initArray.clone());
                this.dAccAbsParam.put(absorptionRatio, initArray.clone());
            }
        }
    }

    /**
     * Computes hasRadProp.
     * 
     * @param part
     *        part
     * @return hasRadProp value
     */
    private int calcHasRadProp(final IPart part) {
        int hasRadProp = 0;
        // test if required properties exist : RADIATIVE in case of albedo
        // Note : next line is a findbugs false positive
        if ((part.hasProperty(PropertyType.RADIATIVE) && this.albedo) || !this.albedo) {
            hasRadProp = 1;
        }

        // test if required properties exist : K0 albedo in case of albedo
        if (this.albedo && this.k0Albedo.getValue() == 0.) {
            hasRadProp = 0;
        }
        return hasRadProp;
    }

    /**
     * Computes hasRadIrProp.
     * 
     * @param part
     *        part
     * @return hasRadProp value
     */
    private int calcHasRadIrProp(final IPart part) {
        int hasRadIrProp = 0;
        // test if required properties exist : RADIATIVEIR in case of infrared
        // Note : next line is a findbugs false positive
        if ((part.hasProperty(PropertyType.RADIATIVEIR) && this.ir) || !this.ir) {
            hasRadIrProp = 1;
        }

        // test if required properties exist : K0 infrared in case of infrared
        if (this.ir && this.k0Ir.getValue() == 0.) {
            hasRadIrProp = 0;
        }
        return hasRadIrProp;
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
                                                            final ElementaryFlux flux) throws PatriusException {

        Vector3D accAlbedo = Vector3D.ZERO;
        Vector3D accIR = Vector3D.ZERO;

        if (this.needAttitude) {
            this.assembly.updateMainPartFrame(state);
        }

        final double totalMass = this.massModel.getTotalMass(state);
        // Albedo acceleration computation
        if (this.albedo) {
            final double fluxAlbedoMass = MathLib.divide(flux.getAlbedoPressure(), totalMass);
            if (fluxAlbedoMass > 0.) {
                // part iteration
                for (final IPart part : this.assembly.getParts().values()) {
                    // computation for the parts with the required properties only
                    if (this.props.get(part)) {
                        // acceleration due to force applied on the current part
                        final Vector3D partComputedAccAlbedo =
                            this.cmptPartComputedAccAlbedo(state, flux, fluxAlbedoMass,
                                part);
                        // add the contribution to the total acc
                        accAlbedo = accAlbedo.add(partComputedAccAlbedo);
                    }
                }
            }
        }

        // IR acceleration computation
        if (this.ir) {
            final double fluxIrMass = MathLib.divide(flux.getInfraRedPressure(), totalMass);
            if (fluxIrMass > 0.) {
                // part iteration
                for (final IPart part : this.assembly.getParts().values()) {
                    // computation for the parts with the required properties only
                    if (this.props.get(part)) {
                        // acceleration due to force applied on the current part
                        final Vector3D partComputedAccIR =
                            this.cmptPartComputedAccIR(state, this.assembly.getMainPart().getFrame(), flux, fluxIrMass,
                                part);
                        // add the contribution to the total acc
                        accIR = accIR.add(partComputedAccIR);
                    }
                }
            }
        }

        // Returns the sum of the two accelerations
        return accAlbedo.add(accIR);
    }

    /**
     * Build properties array.
     * 
     * @return properties array
     * @throws PatriusException throw if failed
     */
    private Map<IPart, Boolean> buildProps() throws PatriusException {
        // Loop on all parts
        final Map<IPart, Boolean> properties = new ConcurrentHashMap<IPart, Boolean>();
        for (final IPart part : this.assembly.getParts().values()) {

            boolean hasProp = false;
            int hasRadShapeProp = 0;

            // test if required properties exist : RADIATIVE_CROSS_SECTION or RADIATIVE_FACET
            if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)
                    || part.hasProperty(PropertyType.RADIATIVE_FACET)) {
                hasRadShapeProp = 1;
            }

            final int hasRadProp = this.calcHasRadProp(part);
            final int hasRadIrProp = this.calcHasRadIrProp(part);

            // synthesis
            if (hasRadShapeProp == 1 && hasRadProp == 1 && hasRadIrProp == 1) {
                hasProp = true;
            }
            properties.put(part, hasProp);
        }
        // Return built map
        return properties;
    }

    /**
     * Computes cmptPartComputedAccAlbedo.
     * 
     * @param state
     *        state
     * @param flux
     *        flux
     * @param fluxAlbedoMass
     *        fluxAlbedoMass
     * @param part
     *        part
     * @return cmptPartComputedAccAlbedo
     * @throws PatriusException
     *         from accForFacet
     */
    private Vector3D
        cmptPartComputedAccAlbedo(final SpacecraftState state,
                                  final ElementaryFlux flux, final double fluxAlbedoMass, final IPart part)
            throws PatriusException {
        Vector3D partComputedAccAlbedo = Vector3D.ZERO;
        RadiativeProperty radProp = null;
        if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)) {
            // compute the acceleration on the current shape (cylinder, parallelepiped, sphere)

            radProp = (RadiativeProperty) part.getProperty(PropertyType.RADIATIVE);
            final Parameter ratio = radProp.getDiffuseReflectionRatio();
            final Vector3D partAlbedoComputedAcc =
                this.accOnSphere(state, part, ratio, flux.getDirFlux(), fluxAlbedoMass);

            // derivative relative to K0 albedo
            this.dAccK0AlParam[0] += partAlbedoComputedAcc.getX();
            this.dAccK0AlParam[1] += partAlbedoComputedAcc.getY();
            this.dAccK0AlParam[2] += partAlbedoComputedAcc.getZ();

            partComputedAccAlbedo = partComputedAccAlbedo.add(
                new Vector3D(this.k0Albedo.getValue(),
                    partAlbedoComputedAcc));

        }

        if (part.hasProperty(PropertyType.RADIATIVE_FACET)) {
            // compute the acceleration on the current facet

            radProp = (RadiativeProperty) part.getProperty(PropertyType.RADIATIVE);
            final Parameter absRatio = radProp.getAbsorptionRatio();
            final Parameter speRatio = radProp.getSpecularReflectionRatio();
            final Parameter difRatio = radProp.getDiffuseReflectionRatio();
            // get the radiative properties of the part
            final Vector3D partAlbedoComputedAcc =
                this.accOnFacet(state, part, absRatio,
                    difRatio,
                    speRatio, flux.getDirFlux(),
                    fluxAlbedoMass);

            // derivative relative to K0 albedo
            this.dAccK0AlParam[0] += partAlbedoComputedAcc.getX();
            this.dAccK0AlParam[1] += partAlbedoComputedAcc.getY();
            this.dAccK0AlParam[2] += partAlbedoComputedAcc.getZ();

            partComputedAccAlbedo = partComputedAccAlbedo.add(this.k0Albedo.getValue(), partAlbedoComputedAcc);
        }
        return partComputedAccAlbedo;
    }

    /**
     * Computes cmptPartComputedAccIR.
     * 
     * @param state
     *        state
     * @param mainPartFrame
     *        the main spacecraft's part frame
     * @param flux
     *        flux
     * @param fluxIrMass
     *        fluxIrMass
     * @param part
     *        part
     * @return cmptPartComputedAccIR
     * @throws PatriusException
     *         from accOnFacet
     */
    private Vector3D cmptPartComputedAccIR(final SpacecraftState state, final Frame mainPartFrame,
                                           final ElementaryFlux flux, final double fluxIrMass, final IPart part)
        throws PatriusException {
        Vector3D partComputedAccIR = Vector3D.ZERO;
        RadiativeIRProperty radIRProp = null;
        if (part.hasProperty(PropertyType.RADIATIVE_CROSS_SECTION)) {
            // compute the acceleration for the current shape (cylinder, parallelepiped, sphere)

            radIRProp = (RadiativeIRProperty) part.getProperty(PropertyType.RADIATIVEIR);
            final Vector3D partIrComputedAcc =
                this.accOnSphere(state, part, radIRProp.getDiffuseReflectionCoef(),
                    flux.getDirFlux(), fluxIrMass);

            // derivative relative to K0 infrared
            this.dAccK0IRParam[0] += partIrComputedAcc.getX();
            this.dAccK0IRParam[1] += partIrComputedAcc.getY();
            this.dAccK0IRParam[2] += partIrComputedAcc.getZ();

            partComputedAccIR = partComputedAccIR.add(
                new Vector3D(this.k0Ir.getValue(), partIrComputedAcc));

        }

        if (part.hasProperty(PropertyType.RADIATIVE_FACET)) {
            // compute the acceleration for the current facet

            radIRProp = (RadiativeIRProperty) part.getProperty(PropertyType.RADIATIVEIR);
            // get the radiative properties of the part

            final Vector3D partIrComputedAcc =
                this.accOnFacet(state, part, radIRProp.getAbsorptionCoef(),
                    radIRProp.getDiffuseReflectionCoef(),
                    radIRProp.getSpecularReflectionCoef(),
                    flux.getDirFlux(), fluxIrMass);

            // derivative relative to K0 infrared
            this.dAccK0IRParam[0] += partIrComputedAcc.getX();
            this.dAccK0IRParam[1] += partIrComputedAcc.getY();
            this.dAccK0IRParam[2] += partIrComputedAcc.getZ();

            partComputedAccIR = partComputedAccIR.add(
                new Vector3D(this.k0Ir.getValue(),
                    partIrComputedAcc));

        }
        return partComputedAccIR;
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
                                final Parameter diffCoeff, final Parameter speCoeff, final Vector3D flux,
                                final double fluxMass) throws PatriusException {

        Vector3D facetComputedAcc = Vector3D.ZERO;

        final Vector3D fluxDir = flux.negate();

        if (fluxDir.getNorm() > Precision.EPSILON) {
            // get the RadiativeFacetProperty property
            final RadiativeFacetProperty radFacetProp = (RadiativeFacetProperty) part
                .getProperty(PropertyType.RADIATIVE_FACET);
            final Facet currentFacet = radFacetProp.getFacet();
            final Transform t = part.getFrame().getTransformTo(state.getFrame(), state.getDate());
            final Vector3D currentNormalFacet = t.transformVector(currentFacet.getNormal()).normalize();

            // orientation of the facet
            final double costh = fluxDir.getX() * currentNormalFacet.getX() + fluxDir.getY()
                    * currentNormalFacet.getY() + fluxDir.getZ() * currentNormalFacet.getZ();

            // Check of orientation of the facet
            if (costh > 0.) {
                final double ff = -costh * currentFacet.getArea() * fluxMass;
                final double c1 = (diffCoeff.getValue() + absCoeff.getValue()) * ff;
                final double c2 = 2. * (speCoeff.getValue() * costh + diffCoeff.getValue() / 3.) * ff;

                final Vector3D v1 = new Vector3D(c1, fluxDir);
                final Vector3D v2 = new Vector3D(c2, currentNormalFacet);
                facetComputedAcc = v1.add(v2);

                // derivatives
                Vector3D v = new Vector3D(2. * ff * costh, currentNormalFacet);
                // compute derivative relative to the specular thermo-optic coefficient
                final double[] dAccSpeParamPart = this.dAccSpeParam.get(speCoeff);
                dAccSpeParamPart[0] += v.getX();
                dAccSpeParamPart[1] += v.getY();
                dAccSpeParamPart[2] += v.getZ();
                this.dAccSpeParam.put(speCoeff, dAccSpeParamPart);

                final Vector3D vf = new Vector3D(ff, fluxDir);
                final Vector3D vn = new Vector3D(2. / 3., currentNormalFacet);
                // compute derivative relative to the diffused thermo-optic coefficient
                final double[] dAccDiffParamPart = this.dAccDiffParam.get(diffCoeff);
                dAccDiffParamPart[0] += vf.add(vn).getX();
                dAccDiffParamPart[1] += vf.add(vn).getY();
                dAccDiffParamPart[2] += vf.add(vn).getZ();
                this.dAccDiffParam.put(diffCoeff, dAccDiffParamPart);
                v = new Vector3D(ff, currentNormalFacet);
                // compute derivative relative to the absorption thermo-optic coefficient
                final double[] dAccAbsParamPart = this.dAccAbsParam.get(absCoeff);
                dAccAbsParamPart[0] += v.getX();
                dAccAbsParamPart[1] += v.getY();
                dAccAbsParamPart[2] += v.getZ();
                this.dAccAbsParam.put(absCoeff, dAccAbsParamPart);
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
    private Vector3D
        accOnSphere(final SpacecraftState state, final IPart part,
                    final Parameter diffCoeff, final Vector3D flux, final double fluxMass) throws PatriusException {

        final RadiativeCrossSectionProperty partRadProp = (RadiativeCrossSectionProperty) part
            .getProperty(PropertyType.RADIATIVE_CROSS_SECTION);
        final double crossSection = partRadProp.getCrossSection(state, flux, this.assembly.getMainPart().getFrame(),
            part.getFrame());
        final double k = -(1. + ((4. / 9.) * diffCoeff.getValue())) * crossSection * fluxMass;
        final Vector3D sphereComputedAcc = new Vector3D(k, flux.negate());

        // derivatives
        final double[] dAccDiffParamPart = this.dAccDiffParam.get(diffCoeff);
        dAccDiffParamPart[0] += (C_N4 / C_9) * crossSection * fluxMass * flux.negate().getX();
        dAccDiffParamPart[1] += (C_N4 / C_9) * crossSection * fluxMass * flux.negate().getY();
        dAccDiffParamPart[2] += (C_N4 / C_9) * crossSection * fluxMass * flux.negate().getZ();
        this.dAccDiffParam.put(diffCoeff, dAccDiffParamPart);
        return sphereComputedAcc;
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDStateRediffusedRadiativePressure(final SpacecraftState s, final double[][] dAccdPos,
                                                         final double[][] dAccdVel) throws PatriusException {
        for (int i = 0; i < 3; i++) {
            Arrays.fill(dAccdPos[i], 0.);
            Arrays.fill(dAccdVel[i], 0.);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDParamRediffusedRadiativePressure(final SpacecraftState s, final Parameter param,
                                                         final double[] dAccdParam) throws PatriusException {
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
        for (final IPart part : this.assembly.getParts().values()) {
            if ((part.hasProperty(PropertyType.RADIATIVE)) || (part.hasProperty(PropertyType.RADIATIVEIR))) {
                if (part.hasProperty(PropertyType.RADIATIVE)) {
                    // storing absorption ratio parameter
                    final Parameter ka = ((RadiativeProperty) part.
                        getProperty(PropertyType.RADIATIVE)).getAbsorptionRatio();
                    // storing specular ratio parameter
                    final Parameter ks = ((RadiativeProperty) part.
                        getProperty(PropertyType.RADIATIVE)).getSpecularReflectionRatio();
                    // storing diffusion ratio parameter
                    final Parameter kd = ((RadiativeProperty) part.
                        getProperty(PropertyType.RADIATIVE)).getDiffuseReflectionRatio();
                    this.addDaccDParamContinued(param, ka, ks, kd, dAccdParam);
                }
                if (part.hasProperty(PropertyType.RADIATIVEIR)) {
                    // storing absorption ratio parameter
                    final Parameter ka = ((RadiativeIRProperty) part.
                        getProperty(PropertyType.RADIATIVEIR)).getAbsorptionCoef();
                    // storing specular ratio parameter
                    final Parameter ks = ((RadiativeIRProperty) part.
                        getProperty(PropertyType.RADIATIVEIR)).getSpecularReflectionCoef();
                    // storing diffusion ratio parameter
                    final Parameter kd = ((RadiativeIRProperty) part.
                        getProperty(PropertyType.RADIATIVEIR)).getDiffuseReflectionCoef();
                    this.addDaccDParamContinued(param, ka, ks, kd, dAccdParam);
                }

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

    /**
     * infrared setter
     * 
     * @param irIn
     *        calculation indicator of the infrared force
     */
    public void setIrAcc(final boolean irIn) {
        this.ir = irIn;
    }

    /**
     * albedo setter
     * 
     * @param albedoIn
     *        calculation indicator of the albedo force
     */
    public void setAlbedoAcc(final boolean albedoIn) {
        this.albedo = albedoIn;
    }

    /** {@inheritDoc} */
    @Override
    public void initDerivatives() {
        Arrays.fill(this.dAccK0AlParam, 0.0);
        Arrays.fill(this.dAccK0IRParam, 0.0);
        for (final IPart part : this.assembly.getParts().values()) {
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
