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
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:271:05/09/2014:Anomalies definitions LVLH et VVLH
 * VERSION::DM:344:15/04/2015:Construction of an attitude law from a Local Orbital Frame
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:423:17/11/2015: improve computation times
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:603:29/08/2016:deleted deprecated methods and classes in package attitudes
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.EulerRotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Attitude law defined by fixed Roll, Pitch and Yaw angles (in any order) with respect to a local
 * orbital frame.
 * 
 * <p>
 * The attitude provider is defined as a rotation offset from some local orbital frame.
 * 
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class LofOffset extends AbstractAttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -713570668596014285L;

    /** Type of Local Orbital Frame. */
    private final LOFType type;

    /** Rotation from local orbital frame. */
    private final Rotation offset;

    /** Inertial frame with respect to which orbit should be computed. */
    private final Frame inertialFrame;

    /**
     * Create a LOF-aligned attitude.
     * <p>
     * Calling this constructor is equivalent to call
     * {@code LofOffset(FramesFactory.getGCRF(), LOFType, RotationOrder.XYZ, 0, 0, 0)}.
     * </p>
     * <p>
     * The GCRF frame is used as pivot in the transformation from an actual frame to the local orbital frame.
     * </p>
     * 
     * @param typeIn type of Local Orbital Frame
     */
    public LofOffset(final LOFType typeIn) {
        super();
        this.type = typeIn;
        this.offset = new Rotation(RotationOrder.XYZ, 0, 0, 0);
        this.inertialFrame = FramesFactory.getGCRF();
    }

    /**
     * Create a LOF-aligned attitude.
     * <p>
     * Calling this constructor is equivalent to call
     * {@code LofOffset(inertialFrame, LOFType, RotationOrder.XYZ, 0, 0, 0)}
     * </p>
     * 
     * @param inertialFrameIn inertial frame with respect to which orbit should be computed. This
     *        frame is the pivot in the transformation from an actual frame to the local orbital
     *        frame.
     * @param typeIn type of Local Orbital Frame
     * @exception PatriusException if inertialFrame is not a pseudo-inertial frame
     */
    public LofOffset(final Frame inertialFrameIn, final LOFType typeIn) throws PatriusException {
        this(inertialFrameIn, typeIn, RotationOrder.XYZ, 0, 0, 0);
    }

    /**
     * Creates new instance.
     * <p>
     * An important thing to note is that the rotation order and angles signs used here are compliant with an
     * <em>attitude</em> definition, i.e. they correspond to a frame that rotate in a field of fixed vectors. The
     * underlying definitions used in commons-math
     * {@link fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation} use <em>reversed</em> definition, i.e.
     * they correspond to a vectors field rotating with respect to a fixed frame. So to retrieve the angles provided
     * here from the commons-math underlying rotation, one has to <em>revert</em> the rotation, as in the following code
     * snippet:
     * </p>
     * 
     * <pre>
     * LofOffset law = new LofOffset(inertial, lofType, order, alpha1, alpha2, alpha3);
     * Rotation offsetAtt = law.getAttitude(orbit).getRotation();
     * Rotation alignedAtt = new LofOffset(inertial, lofType).getAttitude(orbit).getRotation();
     * Rotation offsetProper = offsetAtt.applyTo(alignedAtt.revert());
     * 
     * // note the call to revert in the following statement
     * double[] angles = offsetProper.revert().getAngles(order);
     * 
     * System.out.println(alpha1 + &quot; == &quot; + angles[0]);
     * System.out.println(alpha2 + &quot; == &quot; + angles[1]);
     * System.out.println(alpha3 + &quot; == &quot; + angles[2]);
     * </pre>
     * 
     * @param pInertialFrame inertial frame with respect to which orbit should be computed. This
     *        frame is the pivot in the transformation from the actual frame to the local orbital
     *        frame.
     * @param typeIn type of Local Orbital Frame
     * @param order order of rotations to use for (alpha1, alpha2, alpha3) composition
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     * @exception PatriusException if inertialFrame is not a pseudo-inertial frame
     */
    public LofOffset(final Frame pInertialFrame, final LOFType typeIn, final RotationOrder order,
        final double alpha1, final double alpha2, final double alpha3) throws PatriusException {
        super();
        this.type = typeIn;
        // Initialized as EulerRotation for GENOPUS purpose
        this.offset = new EulerRotation(order, alpha1, alpha2, alpha3);
        if (!pInertialFrame.isPseudoInertial()) {
            throw new PatriusException(
                PatriusMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS,
                pInertialFrame.getName());
        }
        this.inertialFrame = pInertialFrame;
    }

    /**
     * Creates new instance.
     * <p>
     * The GCRF frame is used as pivot in the transformation from an actual frame to the local orbital frame.
     * </p>
     * <p>
     * An important thing to note is that the rotation order and angles signs used here are compliant with an
     * <em>attitude</em> definition, i.e. they correspond to a frame that rotate in a field of fixed vectors. The
     * underlying definitions used in commons-math
     * {@link fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation} use <em>reversed</em> definition, i.e.
     * they correspond to a vectors field rotating with respect to a fixed frame. So to retrieve the angles provided
     * here from the commons-math underlying rotation, one has to <em>revert</em> the rotation, as in the following code
     * snippet:
     * </p>
     * 
     * <pre>
     * LofOffset law = new LofOffset(inertial, lofType, order, alpha1, alpha2, alpha3);
     * Rotation offsetAtt = law.getAttitude(orbit).getRotation();
     * Rotation alignedAtt = new LofOffset(inertial, lofType).getAttitude(orbit).getRotation();
     * Rotation offsetProper = offsetAtt.applyTo(alignedAtt.revert());
     * 
     * // note the call to revert in the following statement
     * double[] angles = offsetProper.revert().getAngles(order);
     * 
     * System.out.println(alpha1 + &quot; == &quot; + angles[0]);
     * System.out.println(alpha2 + &quot; == &quot; + angles[1]);
     * System.out.println(alpha3 + &quot; == &quot; + angles[2]);
     * </pre>
     * 
     * @param typeIn type of Local Orbital Frame
     * @param order order of rotations to use for (alpha1, alpha2, alpha3) composition
     * @param alpha1 angle of the first elementary rotation
     * @param alpha2 angle of the second elementary rotation
     * @param alpha3 angle of the third elementary rotation
     */
    public LofOffset(final LOFType typeIn, final RotationOrder order, final double alpha1,
        final double alpha2, final double alpha3) {
        super();
        this.type = typeIn;
        this.offset = new Rotation(order, alpha1, alpha2, alpha3);
        this.inertialFrame = FramesFactory.getGCRF();
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {
        // construction of the local orbital frame, using PV from inertial frame
        final PVCoordinates pv = pvProv.getPVCoordinates(date, this.inertialFrame);
        final Transform inertialToLof = this.type.transformFromInertial(date, pv,
            this.getSpinDerivativesComputation());

        // take into account the specified start frame (which may not be an inertial one)
        final Transform frameToInertial = frame.getTransformTo(this.inertialFrame, date,
            this.getSpinDerivativesComputation());
        final Transform frameToLof = new Transform(date, frameToInertial, inertialToLof,
            this.getSpinDerivativesComputation());

        Vector3D acc = null;
        if (this.getSpinDerivativesComputation()) {
            acc = this.offset.applyInverseTo(frameToLof.getRotationAcceleration());
        }
        final AngularCoordinates ac = new AngularCoordinates(frameToLof.getRotation().applyTo(
            this.offset), this.offset.applyInverseTo(frameToLof.getRotationRate()), acc);

        return new Attitude(date, frame, ac);
    }

    /**
     * Getter for the inertial frame with respect to which orbit should be computed. This frame is
     * the pivot in the transformation from the actual frame to the local orbital frame.
     * 
     * @return the inertial frame with respect to which orbit should be computed. This frame is the
     *         pivot in the transformation from the actual frame to the local orbital frame
     */
    public Frame getPseudoInertialFrame() {
        return this.inertialFrame;
    }

    /**
     * Getter for the type of Local Orbital Frame.
     * 
     * @return the type of Local Orbital Frame
     */
    public LOFType getLofType() {
        return this.type;
    }

    /**
     * Getter for the rotation from reference frame to satellite frame.
     * 
     * @return the rotation from reference frame to satellite frame
     */
    public Rotation getRotation() {
        return this.offset;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s: inertialFrame=%s, type=%s, rotation=[%s]", this.getClass().getSimpleName(),
            this.inertialFrame.toString(), this.type.toString(), this.offset.toString());
    }
}
