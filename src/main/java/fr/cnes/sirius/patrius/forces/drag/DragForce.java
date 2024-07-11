/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:93:31/03/2014:changed API for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:01/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:490:17/11/2015:Add a multiplicative coefficient k on drag force
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:561:25/02/2016:Wiki and Javadoc corrections
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::FA:677:27/09/2016:Code optimization to avoid recomputation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.drag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.DragLiftModel;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.ConstantFunction;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Atmospheric drag force model.
 * 
 * <p>
 * The drag acceleration is computed as follows :
 * 
 * &gamma; = (1/2 * k * &rho; * V<sup>2</sup> * S / Mass) * DragCoefVector<br>
 * 
 * With:
 * <ul>
 * <li>DragCoefVector = {Cx, Cy, Cz} and S given by the user through the interface {@link DragSensitive}.</li>
 * <li>k: user-added multiplicative coefficient to atmospheric drag. Partial derivatives wrt this coefficient can be
 * computed.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives with respect to <b>normal</b> and
 * <b>tangential ballistic coefficients</b>.
 * </p>
 * 
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 */
public class DragForce extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** Parameter name for k coefficient. */
    public static final String K_COEFFICIENT = "K coefficient";

    /** Serializable UID. */
    private static final long serialVersionUID = 5386256916056674950L;

    /** Maximum size for the cache maps. */
    private static final int MAX_BUFFER_SIZE = 15;

    /** Density cache map. */
    private final transient Map<PosVelDateFrame, Double> densityMap;

    /** Relative velocity cache map. */
    private final transient Map<PosVelDateFrame, Vector3D> relativeVelocityMap;

    /** Drag acceleration cache map. */
    private final transient Map<PosVelDateFrame, Vector3D> dragAccMap;

    /** k multiplicative factor. */
    private final IParamDiffFunction k;

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Spacecraft. */
    private final DragSensitive spacecraft;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    private final boolean computePartialDerivativesWrtPosition;

    /** True if acceleration partial derivatives with respect to velocity have to be computed. */
    private final boolean computePartialDerivativesWrtVelocity;

    /**
     * Constructor with multiplicative factor k = 1.0.
     * 
     * @param atmosphereIn atmospheric model
     * @param spacecraftIn the object physical and geometrical information
     */
    public DragForce(final Atmosphere atmosphereIn, final DragSensitive spacecraftIn) {
        this(atmosphereIn, spacecraftIn, true, true);
    }

    /**
     * Constructor with multiplicative factor k = 1.0.
     * 
     * @param atmosphereIn atmospheric model
     * @param spacecraftIn the object physical and geometrical information
     * @param computePDPos if partial derivatives wrt position have to be computed
     * @param computePDVel if partial derivatives wrt velocity have to be computed
     */
    public DragForce(final Atmosphere atmosphereIn, final DragSensitive spacecraftIn,
        final boolean computePDPos, final boolean computePDVel) {
        this(1.0, atmosphereIn, spacecraftIn, computePDPos, computePDVel);
    }

    /**
     * Constructor with multiplicative factor k.
     * 
     * @param kIn drag multiplicative factor
     * @param atmosphereIn atmospheric model
     * @param spacecraftIn the object physical and geometrical information
     */
    public DragForce(final double kIn, final Atmosphere atmosphereIn,
        final DragSensitive spacecraftIn) {
        this(kIn, atmosphereIn, spacecraftIn, true, true);
    }

    /**
     * Constructor with multiplicative factor k.
     * 
     * @param kIn multiplicative factor
     * @param atmosphereIn atmospheric model
     * @param spacecraftIn the object physical and geometrical information
     * @param computePDPos if partial derivatives wrt position have to be computed
     * @param computePDVel if partial derivatives wrt velocity have to be computed
     */
    public DragForce(final double kIn, final Atmosphere atmosphereIn,
        final DragSensitive spacecraftIn, final boolean computePDPos, final boolean computePDVel) {
        this(new Parameter(K_COEFFICIENT, kIn), atmosphereIn, spacecraftIn, computePDPos,
            computePDVel);
    }

    /**
     * Constructor with multiplicative factor k.
     * 
     * @param kIn parameter representing the multiplicative factor
     * @param atmosphereIn atmospheric model
     * @param spacecraftIn the object physical and geometrical information
     */
    public DragForce(final Parameter kIn, final Atmosphere atmosphereIn,
        final DragSensitive spacecraftIn) {
        this(kIn, atmosphereIn, spacecraftIn, true, true);
    }

    /**
     * Constructor with multiplicative factor k.
     * 
     * @param kIn parameter representing the multiplicative factor
     * @param atmosphereIn atmospheric model
     * @param spacecraftIn the object physical and geometrical information
     * @param computePDPos if partial derivatives wrt position have to be computed
     * @param computePDVel if partial derivatives wrt velocity have to be computed
     */
    public DragForce(final Parameter kIn, final Atmosphere atmosphereIn,
        final DragSensitive spacecraftIn, final boolean computePDPos, final boolean computePDVel) {
        this(new ConstantFunction(kIn), atmosphereIn, spacecraftIn, computePDPos, computePDVel);
    }

    /**
     * Constructor with multiplicative factor k.
     * 
     * @param kIn parameterizable function representing the multiplicative factor
     * @param atmosphereIn atmospheric model
     * @param spacecraftIn the object physical and geometrical information
     */
    public DragForce(final IParamDiffFunction kIn, final Atmosphere atmosphereIn,
        final DragSensitive spacecraftIn) {
        this(kIn, atmosphereIn, spacecraftIn, true, true);
    }

    /**
     * Constructor with multiplicative factor k.
     * 
     * @param kIn parameterizable function representing the multiplicative factor
     * @param atmosphereIn atmospheric model
     * @param spacecraftIn the object physical and geometrical information
     * @param computePDPos if partial derivatives wrt position have to be computed
     * @param computePDVel if partial derivatives wrt velocity have to be computed
     */
    public DragForce(final IParamDiffFunction kIn, final Atmosphere atmosphereIn,
        final DragSensitive spacecraftIn, final boolean computePDPos, final boolean computePDVel) {
        // storing jacobians parameters from spacecraft
        super();

        // Initialize all maps
        this.densityMap = new HashMap<PosVelDateFrame, Double>();
        this.relativeVelocityMap = new HashMap<PosVelDateFrame, Vector3D>();
        this.dragAccMap = new HashMap<PosVelDateFrame, Vector3D>();

        this.k = kIn;
        final ArrayList<Parameter> params = kIn.getParameters();
        for (int i = 0; i < params.size(); i++) {
            final Parameter param = params.get(i);
            if (kIn.isDifferentiableBy(param)) {
                this.addJacobiansParameter(param);
            } else {
                this.addParameter(param);
            }
        }
        this.addJacobiansParameter(spacecraftIn.getJacobianParameters());
        this.enrichParameterDescriptors();
        this.atmosphere = atmosphereIn;
        this.spacecraft = spacecraftIn;
        this.computePartialDerivativesWrtPosition = computePDPos;
        this.computePartialDerivativesWrtVelocity = computePDVel;
    }

    /**
     * Creates a new instance.
     * 
     * @param kIn multiplicative factor
     * @param atmosphereIn atmosphere model
     * @param assembly assembly with aerodynamic properties
     * @throws PatriusException if the assembly does not have only one valid aerodynamic property.
     */
    public DragForce(final double kIn, final Atmosphere atmosphereIn, final Assembly assembly)
        throws PatriusException {
        this(kIn, atmosphereIn, spacecraftFromAssembly(assembly));
    }

    /**
     * Creates a new instance from the data in another one but with a different assembly.
     * 
     * @param otherDragForce the other instance
     * @param assembly the new assembly
     * @throws PatriusException if the assembly does not have only one valid aerodynamic property.
     */
    public DragForce(final DragForce otherDragForce, final Assembly assembly)
        throws PatriusException {
        this(otherDragForce.getMultiplicativeFactor(), otherDragForce.getAtmosphere().copy(),
                otherDragForce.spacecraft.copy(assembly));
    }

    /**
     * Creates an instance of {@link DragSensitive} which is either a {@link DragLiftModel} or an {@link AeroModel}
     * depending on the type of aerodynamic property found in the assembly ( {@link PropertyType#AERO_GLOBAL} or
     * {@link PropertyType.AERO_SPHERE} respectively).
     * 
     * @param assembly with aerodynamic properties.
     * @return the {@link DragSensitive} object.
     * @throws PatriusException if the assembly does not have only one valid aerodynamic property.
     */
    private static DragSensitive spacecraftFromAssembly(final Assembly assembly)
                                                                                throws PatriusException {
        // Create drag sensitive spacecraft model
        DragSensitive dragSensitive = null;

        if (assembly.getMainPart().hasProperty(PropertyType.AERO_GLOBAL)) {
            // if aero global property has been added

            // Drag lift model
            dragSensitive = new DragLiftModel(assembly);

        } else if (assembly.getMainPart().hasProperty(PropertyType.AERO_CROSS_SECTION)) {
            // if aero sphere property has been added

            if (assembly.getMainPart().hasProperty(PropertyType.AERO_GLOBAL)) {
                throw new PatriusException(PatriusMessages.PDB_REDUNDANT_AERO_PROPERTIES);
            }

            // Drag force model
            dragSensitive = new AeroModel(assembly);

        } else {
            throw new PatriusException(PatriusMessages.PDB_NO_AERO_MASS_PROPERTIES);
        }
        return dragSensitive;
    }

    /**
     * Compute the contribution of the drag to the perturbing acceleration.
     * 
     * @param s the current state information : date, kinematics, attitude
     * @param adder object where the contribution should be added
     * @exception PatriusException if some specific error occurs
     */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
                                                                                              throws PatriusException {

        // Addition of calculated acceleration to adder
        adder.addAcceleration(this.computeAcceleration(s), s.getFrame());
    }

    /**
     * There are no discrete events for this model.
     * 
     * @return an empty array
     */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPosition;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return this.computePartialDerivativesWrtVelocity;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {

        // compute or retrieve spacecraft acceleration.
        final Vector3D dragAcc = this.getDragAcc(s);

        // Addition of calculated acceleration to adder
        return new Vector3D(this.k.value(s), dragAcc);
    }

    /**
     * <p>
     * Method to compute the acceleration. This method has been implemented in order to validate the force model only.
     * The reason is that for the validation context, we do not want to set up an instance of the SpacecraftState object
     * to avoid the inertial frame of the spacecraft orbit.
     * </p>
     * 
     * <p>
     * (see Story #V84 and Feature #34 on https://www.orekit.org/forge/issues/34)
     * </p>
     * 
     * <p>
     * Out of the validation context, one must use the method Vector3D computeAcceleration(final SpacecraftState s)
     * </p>
     * 
     * <p>
     * In the validation context, we assume that the multiplicative factor is equal to 1.
     * </p>
     * 
     * @param pv PV coordinates of the spacecraft (spherical spacecraft only for the validation)
     * @param frame frame in which the PV coordinates are given
     * @param atm atmosphere
     * @param date date
     * @param kD Composite drag coefficient (S.Cd/2).
     * @param mass mass of the spacecraft
     * @throws PatriusException if an Orekit error occurs
     * @return acceleration
     * 
     */
    public static Vector3D computeAcceleration(final PVCoordinates pv, final Frame frame,
                                               final Atmosphere atm, final AbsoluteDate date, final double kD,
                                               final double mass)
                                                                 throws PatriusException {

        final double rho = atm.getDensity(date, pv.getPosition(), frame);
        final Vector3D vAtm = atm.getVelocity(date, pv.getPosition(), frame);
        final Vector3D relativeVelocity = vAtm.subtract(pv.getVelocity());

        // Addition of calculated acceleration to adder
        return new Vector3D(rho * relativeVelocity.getNorm() * kD / mass, relativeVelocity);
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState s, final double[][] dAccdPos,
                              final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition() || this.computeGradientVelocity()) {
            // Only the derivatives with respect to position and velocity are computed:
            final double[][] dAdP = new double[3][3];
            final double[][] dAdV = new double[3][3];
            // Get density at date
            final double density = this.getDensity(s.getDate(), s.getPVCoordinates().getPosition(), s
                .getPVCoordinates().getVelocity(), s.getFrame());
            // Get relative velocity at date
            final Vector3D relativeVelocity = this.getRelativeVelocity(s.getDate(), s.getPVCoordinates()
                .getPosition(), s.getPVCoordinates().getVelocity(), s.getFrame());

            // Call the DragSensitive method to compute the partial derivatives with respect to
            // state parameters:
            this.spacecraft.addDDragAccDState(s, dAdP, dAdV, density, this.computeAcceleration(s),
                relativeVelocity, this.computeGradientPosition(), this.computeGradientVelocity());

            if (this.computeGradientPosition()) {
                // Partial derivatives with respect to position:
                dAccdPos[0][0] += dAdP[0][0];
                dAccdPos[0][1] += dAdP[0][1];
                dAccdPos[0][2] += dAdP[0][2];
                dAccdPos[1][0] += dAdP[1][0];
                dAccdPos[1][1] += dAdP[1][1];
                dAccdPos[1][2] += dAdP[1][2];
                dAccdPos[2][0] += dAdP[2][0];
                dAccdPos[2][1] += dAdP[2][1];
                dAccdPos[2][2] += dAdP[2][2];
            }

            if (this.computeGradientVelocity()) {
                // Partial derivatives with respect to velocity:
                dAccdVel[0][0] += dAdV[0][0];
                dAccdVel[0][1] += dAdV[0][1];
                dAccdVel[0][2] += dAdV[0][2];
                dAccdVel[1][0] += dAdV[1][0];
                dAccdVel[1][1] += dAdV[1][1];
                dAccdVel[1][2] += dAdV[1][2];
                dAccdVel[2][0] += dAdV[2][0];
                dAccdVel[2][1] += dAdV[2][1];
                dAccdVel[2][2] += dAdV[2][2];
            }
        }

        // No result to return
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {

        // check parameter name
        if (!this.supportsJacobianParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }

        // SpacecraftState data
        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final Vector3D position = s.getPVCoordinates().getPosition();
        final Vector3D velocity = s.getPVCoordinates().getVelocity();
        final double density = this.getDensity(date, position, velocity, frame);
        final Vector3D relativeVelocity = this.getRelativeVelocity(date, position, velocity, frame);

        if (this.k.supportsParameter(param)) {
            // Derivative value
            final double kprime = this.k.derivativeValue(param, s);
            if (MathLib.abs(kprime) > Precision.DOUBLE_COMPARISON_EPSILON) {
                final Vector3D dragAcc = this.getDragAcc(s);
                dAccdParam[0] += kprime * dragAcc.getX();
                dAccdParam[1] += kprime * dragAcc.getY();
                dAccdParam[2] += kprime * dragAcc.getZ();
            }

        } else {
            // container
            final double[] dAccdParamModel = new double[3];

            // Partial derivatives with respect to the ballistic coefficient
            // Call the addDAccDParam method of DragSensitive to compute the derivatives:
            this.spacecraft.addDDragAccDParam(s, param, density, relativeVelocity, dAccdParamModel);
            dAccdParam[0] += dAccdParamModel[0];
            dAccdParam[1] += dAccdParamModel[1];
            dAccdParam[2] += dAccdParamModel[2];
        }
    }

    /**
     * Return the density corresponding to the current {@link PosVelDateFrame} if it has already
     * been computed, compute it otherwise.
     * 
     * @param date the date
     * @param position spacecraft's position
     * @param velocity spacecraft's velocity
     * @param frame the frame in which position is expressed
     * @return the density
     * @throws PatriusException if an Orekit error occurs
     */
    private Double getDensity(final AbsoluteDate date, final Vector3D position,
                              final Vector3D velocity, final Frame frame) throws PatriusException {

        // Initialization of the quantity
        Double density = Double.NaN;
        final PosVelDateFrame pdf = new PosVelDateFrame(date, position, velocity, frame);

        // Check if density has been already computed
        if (this.densityMap.containsKey(pdf)) {
            // Extract data from map
            density = this.densityMap.get(pdf);
        } else {
            // Compute density
            density = this.atmosphere.getDensity(date, position, frame);
            if (this.densityMap.size() > MAX_BUFFER_SIZE) {
                this.densityMap.clear();
            }
            this.densityMap.put(pdf, density);
        }

        return density;
    }

    /**
     * Return the relative velocity corresponding to the current {@link PosVelDateFrame} if it has
     * already been computed, compute it otherwise.
     * 
     * @param date the date
     * @param position spacecraft's position
     * @param velocity spacecraft's velocity
     * @param frame the frame in which position is expressed
     * @return the relative velocity
     * @throws PatriusException if an Orekit error occurs
     */
    private Vector3D getRelativeVelocity(final AbsoluteDate date, final Vector3D position,
                                         final Vector3D velocity, final Frame frame) throws PatriusException {

        // Initialization of the quantity
        Vector3D relativeVelocity = Vector3D.ZERO;
        final PosVelDateFrame pdf = new PosVelDateFrame(date, position, velocity, frame);

        // Check if relative velocity has been already computed
        if (this.relativeVelocityMap.containsKey(pdf)) {
            // Extract data from map
            relativeVelocity = this.relativeVelocityMap.get(pdf);
        } else {
            // Compute relative velocity
            relativeVelocity = this.atmosphere.getVelocity(date, position, frame).subtract(velocity);
            // Store value in map
            if (this.relativeVelocityMap.size() > MAX_BUFFER_SIZE) {
                this.relativeVelocityMap.clear();
            }
            this.relativeVelocityMap.put(pdf, relativeVelocity);
        }

        return relativeVelocity;
    }

    /**
     * Return the acceleration corresponding to the current {@link PosVelDateFrame} if it has
     * already been computed, compute it otherwise.
     * 
     * @param s the spacecraft state
     * @return the acceleration
     * @throws PatriusException if an Orekit error occurs
     */
    private Vector3D getDragAcc(final SpacecraftState s) throws PatriusException {

        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final Vector3D position = s.getPVCoordinates().getPosition();
        final Vector3D velocity = s.getPVCoordinates().getVelocity();
        final PosVelDateFrame pdf = new PosVelDateFrame(date, position, velocity, frame);

        // Initialization of the quantity
        Vector3D dragAcc = null;

        // Check if acceleration has been already computed
        if (this.dragAccMap.containsKey(pdf)) {
            // Extract data from map
            dragAcc = this.dragAccMap.get(pdf);
        } else {
            // Compute acceleration
            final double rho = this.getDensity(date, position, velocity, frame);
            final Vector3D relativeVelocity = this.getRelativeVelocity(date, position, velocity, frame);
            dragAcc = this.spacecraft.dragAcceleration(s, rho, relativeVelocity);
            if (this.dragAccMap.size() > MAX_BUFFER_SIZE) {
                this.dragAccMap.clear();
            }
            this.dragAccMap.put(pdf, dragAcc);
        }

        return dragAcc;
    }

    /**
     * Getter for the multiplicative factor.
     * 
     * @return the multiplicative factor
     */
    public IParamDiffFunction getMultiplicativeFactor() {
        return this.k;
    }

    /**
     * Getter for the multiplicative factor used at construction.
     * 
     * @param state state
     * @return the multiplicative factor.
     */
    public double getMultiplicativeFactor(final SpacecraftState state) {
        return this.k.value(state);
    }

    /**
     * Get the atmosphere model.
     * 
     * @return the atmosphere
     */
    public Atmosphere getAtmosphere() {
        return this.atmosphere;
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Check solar activity data
        this.atmosphere.checkSolarActivityData(start, end);
    }

    /**
     * Utilitary inner class to manage the density, relative velocity and acceleration maps : these
     * are used to avoid quantities recomputation.
     * 
     * @author rodriguest
     * 
     */
    private static class PosVelDateFrame {

        /** The date. */
        private final AbsoluteDate date;

        /** The spacecraft's position. */
        private final Vector3D position;

        /** The spacecraft's velocity. */
        private final Vector3D velocity;

        /** The frame in which position and velocity are given. */
        private final Frame frame;

        /**
         * Build a new instance of the class.
         * 
         * @param dateIn the date
         * @param pos spacecraft's position
         * @param vel spacecraft's velocity
         * @param frameIn the frame in which position is expressed
         */
        public PosVelDateFrame(final AbsoluteDate dateIn, final Vector3D pos, final Vector3D vel,
            final Frame frameIn) {
            this.date = dateIn;
            this.position = pos;
            this.velocity = vel;
            this.frame = frameIn;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {

            if (obj == this) {
                // first fast check
                return true;
            }

            if ((obj != null) && (obj instanceof PosVelDateFrame)) {
                boolean res = true;
                // Cast obj as PosVelDateFrame
                final PosVelDateFrame pdf = (PosVelDateFrame) obj;
                res &= this.date.equals(pdf.date);
                res &= this.position.equals(pdf.position);
                res &= this.velocity.equals(pdf.velocity);
                res &= this.frame.equals(pdf.frame);
                return res;
            }

            // Default result
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return this.date.hashCode() + this.position.hashCode() + this.velocity.hashCode() + this.frame.hashCode();
        }

    }
}
