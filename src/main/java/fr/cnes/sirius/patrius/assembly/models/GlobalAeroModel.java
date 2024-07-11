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
 * @history creation 13/09/2016
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
 * VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:599:13/09/2016: new tabulated aero model
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:705:07/12/2016: corrected anomaly in dragAcceleration()
 * VERSION::DM:711:07/12/2016: change signature of method getCoefficients()
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.cook.CookWallGasTemperature;
import fr.cnes.sirius.patrius.assembly.properties.AeroProperty;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Global aero model for generic user-provided aero coefficients.
 * <p>
 * This model requires a {@link AeroProperty}. This property has to be applied to Main part only:
 * </p>
 * <ul>
 * <li>If Main part does not have a {@link AeroProperty}, an exception is thrown.</li>
 * <li>If other parts have {@link AeroProperty}, they are not taken into account.</li>
 * </ul>
 * 
 * @concurrency not thread-safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.3
 */
public final class GlobalAeroModel implements DragSensitive {

    /** Serializable UID. */
    private static final long serialVersionUID = 8030508270216891103L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** -2. */
    private static final double MTWO = -2.;

    /** Default position step for partial derivatives computation. */
    private static final double DEFAULT_STEP = 1.;

    /** Assembly. */
    private final Assembly assembly;

    /** Aero property for the main part. */
    private final AeroProperty aeroProp;

    /** Mass model. */
    private final MassProvider massModel;

    /** Drag coefficient (x surface) provider. */
    private final DragCoefficientProvider dragCoefficientProvider;

    /** Atmosphere. */
    private final ExtendedAtmosphere atmosphere;

    /** Wall gas temperature. */
    private final CookWallGasTemperature wallGasTemperature;

    /** Position step for partial derivatives computation. */
    private final double hPos;

    /**
     * Constructor.
     * 
     * @param assemblyIn
     *        assembly
     * @param dragCoefficientProviderIn
     *        drag coefficient (x surface) provider
     * @param atmosphereIn
     *        atmosphere
     * @param partialDerivativesStep
     *        partial derivatives position step
     */
    public GlobalAeroModel(final Assembly assemblyIn, final DragCoefficientProvider dragCoefficientProviderIn,
        final ExtendedAtmosphere atmosphereIn, final double partialDerivativesStep) {
        this.aeroProp = (AeroProperty) assemblyIn.getMainPart().getProperty(PropertyType.WALL);
        checkProperties(assemblyIn);
        this.assembly = assemblyIn;
        this.massModel = new MassModel(this.assembly);
        this.dragCoefficientProvider = dragCoefficientProviderIn;
        this.atmosphere = atmosphereIn;
        this.hPos = partialDerivativesStep;

        // Build wall gas temperature
        this.wallGasTemperature = new CookWallGasTemperature(this.atmosphere, this.aeroProp.getAlpha(),
            this.aeroProp.getWallTemperature());
    }

    /**
     * Constructor with default partial derivatives time step {@link #DEFAULT_STEP}.
     * 
     * @param assemblyIn
     *        assembly
     * @param dragCoefficientProviderIn
     *        drag coefficient provider
     * @param atmosphereIn
     *        atmosphere
     */
    public GlobalAeroModel(final Assembly assemblyIn, final DragCoefficientProvider dragCoefficientProviderIn,
        final ExtendedAtmosphere atmosphereIn) {
        this(assemblyIn, dragCoefficientProviderIn, atmosphereIn, DEFAULT_STEP);
    }

    /**
     * This method tests if the required property exist (WALL) in the main part. At least
     * one part with a mass is required.
     * 
     * @param assemblyIn
     *        the considered vehicle
     */
    private static void checkProperties(final Assembly assemblyIn) {

        // Checking aero properties (main part only)
        final boolean hasAeroProp = assemblyIn.getMainPart().hasProperty(PropertyType.WALL);

        // Checking mass property
        boolean hasMassProp = false;
        for (final IPart part : assemblyIn.getParts().values()) {
            if (part.hasProperty(PropertyType.MASS)) {
                hasMassProp |= true;
            }
        }

        if (!hasAeroProp || !hasMassProp) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_AERO_MASS_PROPERTIES);
        }
    }

    /**
     * Method to compute the aero acceleration, based on the assembly.
     * 
     * @param state
     *        the current state of the spacecraft.
     * @param density
     *        the atmosphere density.
     * @param relativeVelocity
     *        relative velocity of atmosphere with respect to spacecraft,
     *        in the same inertial frame as spacecraft orbit (m/s).
     *        <b>WARNING :</b> Remember that consequently, the spacecraft velocity is
     *        obtained as the opposite of <b>relativeVelocity</b>.
     * @return the acceleration applied on the assembly.
     * @throws PatriusException
     *         when an error occurs.
     * 
     */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity) throws PatriusException {

        // Compute drag coefficient x area
        final Vector3D sc = computeSC(state, state.getFrame(), relativeVelocity);

        // Return total acceleration
        return new Vector3D(MathLib.divide(HALF * density * relativeVelocity.getNormSq(),
            this.massModel.getTotalMass(state)), sc);
    }

    /** {@inheritDoc} */
    @Override
    public void addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                  final Vector3D relativeVelocity, final double[] dAccdParam) throws PatriusException {
        throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param.getName());
    }

    /** {@inheritDoc} */
    @Override
    public
            void
            addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                              // Note : PMD false positive here
                              final double density, final Vector3D acceleration, final Vector3D relativeVelocity,
                              final boolean computeGradientPosition,
                              final boolean computeGradientVelocity) throws PatriusException {

        // Compute derivatives wrt position using centered finite differences
        // Note that finite differences algorithm is also available in Jacobianizer class but is used for ForceModel
        if (computeGradientPosition) {
            // dPx
            final SpacecraftState stateXp = shiftState(s, new Vector3D(this.hPos, Vector3D.PLUS_I), Vector3D.ZERO);
            final SpacecraftState stateXm = shiftState(s, new Vector3D(this.hPos, Vector3D.MINUS_I), Vector3D.ZERO);
            final Vector3D shiftedXp = this.dragAcceleration(stateXp);
            final Vector3D shiftedXm = this.dragAcceleration(stateXm);

            dAccdPos[0][0] += (shiftedXp.getX() - shiftedXm.getX()) / (2. * this.hPos);
            dAccdPos[1][0] += (shiftedXp.getY() - shiftedXm.getY()) / (2. * this.hPos);
            dAccdPos[2][0] += (shiftedXp.getZ() - shiftedXm.getZ()) / (2. * this.hPos);

            // dPy
            final SpacecraftState stateYp = shiftState(s, new Vector3D(this.hPos, Vector3D.PLUS_J), Vector3D.ZERO);
            final SpacecraftState stateYm = shiftState(s, new Vector3D(this.hPos, Vector3D.MINUS_J), Vector3D.ZERO);
            final Vector3D shiftedYp = this.dragAcceleration(stateYp);
            final Vector3D shiftedYm = this.dragAcceleration(stateYm);

            dAccdPos[0][1] += (shiftedYp.getX() - shiftedYm.getX()) / (2. * this.hPos);
            dAccdPos[1][1] += (shiftedYp.getY() - shiftedYm.getY()) / (2. * this.hPos);
            dAccdPos[2][1] += (shiftedYp.getZ() - shiftedYm.getZ()) / (2. * this.hPos);

            // dPz
            final SpacecraftState stateZp = shiftState(s, new Vector3D(this.hPos, Vector3D.PLUS_K), Vector3D.ZERO);
            final SpacecraftState stateZm = shiftState(s, new Vector3D(this.hPos, Vector3D.MINUS_K), Vector3D.ZERO);
            final Vector3D shiftedZp = this.dragAcceleration(stateZp);
            final Vector3D shiftedZm = this.dragAcceleration(stateZm);

            dAccdPos[0][2] += (shiftedZp.getX() - shiftedZm.getX()) / (2. * this.hPos);
            dAccdPos[1][2] += (shiftedZp.getY() - shiftedZm.getY()) / (2. * this.hPos);
            dAccdPos[2][2] += (shiftedZp.getZ() - shiftedZm.getZ()) / (2. * this.hPos);
        }

        // Compute derivatives wrt velocity using analytical derivation and Sc derivative computed by finite 
        // differences
        if (computeGradientVelocity) {
            // Compute derivatives wrt velocity
            final PVCoordinates pv = s.getPVCoordinates();
            final double hVel = s.getMu() * this.hPos / (pv.getVelocity().getNorm() * pv.getPosition().getNormSq());

            // SC vector
            final Vector3D sc = this.computeSC(s, s.getFrame(), relativeVelocity);

            // SC derivatives
            final Vector3D scDerX = computeSCderivative(s, relativeVelocity, Vector3D.PLUS_I, hVel);
            final Vector3D scDerY = computeSCderivative(s, relativeVelocity, Vector3D.PLUS_J, hVel);
            final Vector3D scDerZ = computeSCderivative(s, relativeVelocity, Vector3D.PLUS_K, hVel);

            final double relVelX = relativeVelocity.getX();
            final double relVelY = relativeVelocity.getY();
            final double relVelZ = relativeVelocity.getZ();
            final double relVel2 = relativeVelocity.getNormSq();
            final double coef = HALF / this.massModel.getTotalMass(s) * density;

            // AccX
            dAccdVel[0][0] = coef * (MTWO * relVelX * sc.getX() + relVel2 * scDerX.getX());
            dAccdVel[0][1] = coef * (MTWO * relVelY * sc.getX() + relVel2 * scDerY.getX());
            dAccdVel[0][2] = coef * (MTWO * relVelZ * sc.getX() + relVel2 * scDerZ.getX());

            // AccY
            dAccdVel[1][0] = coef * (MTWO * relVelX * sc.getY() + relVel2 * scDerX.getY());
            dAccdVel[1][1] = coef * (MTWO * relVelY * sc.getY() + relVel2 * scDerY.getY());
            dAccdVel[1][2] = coef * (MTWO * relVelZ * sc.getY() + relVel2 * scDerZ.getY());

            // AccZ
            dAccdVel[2][0] = coef * (MTWO * relVelX * sc.getZ() + relVel2 * scDerX.getZ());
            dAccdVel[2][1] = coef * (MTWO * relVelY * sc.getZ() + relVel2 * scDerY.getZ());
            dAccdVel[2][2] = coef * (MTWO * relVelZ * sc.getZ() + relVel2 * scDerZ.getZ());
        }
    }

    /**
     * Compute Sc derivative with respect to spacecraft velocity.
     * @param s spacecraft state
     * @param relativeVelocity relative velocity in spacecraft state frame
     * @param direction derivation direction
     * @param hVel delta-velocity
     * @return Sc derivative with respect to spacecraft velocity along velocity direction
     * @throws PatriusException thrown if Sc computation failed
     */
    private final Vector3D computeSCderivative(final SpacecraftState s,
            final Vector3D relativeVelocity,
            final Vector3D direction,
            final double hVel) throws PatriusException {
        final Vector3D dVel = new Vector3D(hVel, direction);
        final Vector3D relativeVelocityP = relativeVelocity.subtract(dVel);
        final Vector3D relativeVelocityM = relativeVelocity.add(dVel);
        final Vector3D scP = this.computeSC(s, s.getFrame(), relativeVelocityP);
        final Vector3D scM = this.computeSC(s, s.getFrame(), relativeVelocityM);
        return scP.subtract(scM).scalarMultiply(1. / (2. * hVel));
    }

    /**
     * Compute full drag acceleration for partial derivatives computation.
     * 
     * @param s
     *        spacecraft state
     * @return drag acceleration
     * @throws PatriusException
     *         thrown if computation failed
     */
    private Vector3D dragAcceleration(final SpacecraftState s) throws PatriusException {
        final double rho = this.atmosphere.getDensity(s.getDate(), s.getPVCoordinates().getPosition(), s.getFrame());
        final Vector3D vAtm =
            this.atmosphere.getVelocity(s.getDate(), s.getPVCoordinates().getPosition(), s.getFrame());
        final Vector3D relvel = vAtm.subtract(s.getPVCoordinates().getVelocity());
        return this.dragAcceleration(s, rho, relvel);
    }

    /**
     * Shift provided spacecraft state position and velocity with provided increments.
     * 
     * @param s spacecraft state
     * @param dp position increment
     * @param dv velocity increment
     * @return shifted spacecraft state
     * @throws PatriusException if attitude cannot be computed
     */
    private static SpacecraftState
            shiftState(final SpacecraftState s, final Vector3D dp, final Vector3D dv)
                                                                                     throws PatriusException {
        final PVCoordinates pv = s.getPVCoordinates();
        final PVCoordinates pvNew = new PVCoordinates(pv.getPosition().add(dp), pv.getVelocity().add(dv));
        final Orbit orbit = new CartesianOrbit(pvNew, s.getFrame(), s.getDate(), s.getMu());
        return new SpacecraftState(orbit, s.getAttitudeForces(), s.getAttitudeEvents(), s.getAdditionalStates());
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // There is no parameters to return
        return new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public DragSensitive copy(final Assembly newAssembly) {
        return new GlobalAeroModel(newAssembly, this.dragCoefficientProvider, this.atmosphere, this.hPos);
    }
    
    /**
     * Compute the surface drag coefficient (drag coefficient x area)
     * 
     * @param state
     *        spacecraft state
     * @param frame
     *        frame in which the sc will be defined
     * @param relativeVelocity
     *        relative velocity of atmosphere with respect to spacecraft,
     *        in the same inertial frame as spacecraft orbit (m/s).
     * @return sc
     * @throws PatriusException
     *         thrown if computation failed
     */
    public Vector3D computeSC(final SpacecraftState state, final Frame frame, final Vector3D relativeVelocity) 
            throws PatriusException{
        
        // Get atmospheric temperature
        final AtmosphereData data = this.atmosphere.getData(state.getDate(), state.getPVCoordinates().getPosition(),
            state.getFrame());
        final double tAtmos = data.getLocalTemperature();

        // Convert the relative velocity in sc frame
        final Rotation rotSatFrame = state.getAttitude(frame).getRotation();
        final Vector3D relativeVelocityInSatFrame = rotSatFrame.applyInverseTo(relativeVelocity);

        // Compute Tav and Tar (only sign of theta is required)
        final double tAv = this.wallGasTemperature.getWallGasTemperature(state, relativeVelocity, 1);
        final double tAr = this.wallGasTemperature.getWallGasTemperature(state, relativeVelocity, -1);

        // Compute drag coefficient x area
        final DragCoefficient dragCoefficients =
            this.dragCoefficientProvider.getCoefficients(relativeVelocityInSatFrame,
                data, this.assembly);
        final Vector3D scDiff = new Vector3D(MathLib.sqrt(MathLib.divide(tAv, tAtmos)),
            dragCoefficients.getScDiffAv(),
            MathLib.sqrt(MathLib.divide(tAr, tAtmos)), dragCoefficients.getScDiffAr());
        final Vector3D sc = new Vector3D(1., dragCoefficients.getScAbs(), this.aeroProp.getEpsilon(),
            dragCoefficients.getScSpec(), 1 - this.aeroProp.getEpsilon(), scDiff);

        // Convert sc in inertial frame and return        
        return rotSatFrame.applyTo(sc);
    }
    
}
