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
 * @history creation 19/04/2012
 *
 * HISTORY
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9.1:DM:DM-3168:01/06/2022:[PATRIUS] Ajout de la classe ConstantPVCoordinatesProvider
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius de GeometricBodyShape...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2956:15/11/2021:[PATRIUS] Temps de propagation non implemente pour certains evenements 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.8:FA:FA-3091:15/11/2021:[PATRIUS] Corriger la methode bodyShapeMaskingDistance de SensorModel
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::FA:512:08/02/2016:Corrected the target angular radius computation
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.GeometricProperty;
import fr.cnes.sirius.patrius.assembly.properties.SensorProperty;
import fr.cnes.sirius.patrius.bodies.GeometricBodyShape;
import fr.cnes.sirius.patrius.events.sensor.SecondarySpacecraft;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.LineSegment;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plate;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.SolidShape;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.events.LocalRadiusProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * @description
 *              <p>
 *              This class is a model for a generic sensor of the assembly : it uses a SensorProperty object. It
 *              provides computations using the tree of frames and some measures are available.
 *              </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the direct use of a not thread-safe Assembly makes this class
 *                      not thread-safe itself
 * 
 * @see SensorProperty
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class SensorModel implements PVCoordinatesProvider, Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = 2071997109297592846L;

    /** The sensor property */
    private SensorProperty property;

    /** main target position point */
    private PVCoordinatesProvider mainTarget;

    /** main field of view in sensor frame */
    private IFieldOfView mainField;

    /** inhibitions targets position points */
    private PVCoordinatesProvider[] inhibitionTargets;

    /** inhibitions fields of view in sensor frame */
    private IFieldOfView[] inhibitionFields;

    /** number of inhibitions fields */
    private int length;

    /** the assembly of this sensor */
    private final Assembly inAssembly;

    /** the name of the sensor's part */
    private final String inPartName;

    /** the radiuses of the inhibition targets */
    private LocalRadiusProvider[] inhibitionTargetsRadiuses;

    /** the radius of the main target */
    private LocalRadiusProvider mainTargetRadius;

    /** all spacecraft's potentially masking parts */
    private final List<List<IPart>> maskingParts;

    /** number of the masking spacecraft */
    private int maskingSpacecraftNumber;

    /** Current masking spacecraft. */
    private transient PVCoordinatesProvider maskingSpacecraft;

    /** number of the masking body */
    private String maskingBodyName;

    /** Current masking body. */
    private PVCoordinatesProvider maskingBody;

    /** masking spacecrafts names */
    private final List<String> maskingSpacecraftNames;

    /** masking part's name */
    private String maskingPartName;

    /** masking celestial bodies */
    private final List<GeometricBodyShape> maskingBodies;

    /** masking assemblies */
    private final List<SecondarySpacecraft> maskingAssemblies;

    /** index of this spacecraft in the masking parts list */
    private int ownSpacecraftIndex;

    /**
     * Constructor for a sensor model.
     * 
     * @param assembly
     *        the considered vehicle
     * @param partName
     *        the name of the part supporting the sensor
     */
    public SensorModel(final Assembly assembly, final String partName) {

        // initialisations
        this.inPartName = partName;
        this.inAssembly = assembly;
        this.resetProperty();

        this.maskingParts = new ArrayList<List<IPart>>();
        this.maskingSpacecraftNames = new ArrayList<String>();
        this.maskingBodies = new ArrayList<GeometricBodyShape>();
        this.maskingAssemblies = new ArrayList<SecondarySpacecraft>();
        this.ownSpacecraftIndex = -1;
        this.maskingPartName = "none";
    }

    /**
     * Resets the sensor property features. Shall be used each time the
     * associated sensor property has been modified.
     */
    public void resetProperty() {
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        this.property = (SensorProperty) sensorPart.getProperty(PropertyType.SENSOR);

        this.mainTarget = this.property.getMainTarget();
        this.mainField = this.property.getMainField();

        this.inhibitionTargets = this.property.getInhibitionTargets();
        this.inhibitionFields = this.property.getInhibitionFields();

        this.length = this.inhibitionFields.length;
        this.inhibitionTargetsRadiuses = this.property.getInhibitionTargetsRadiuses();
        this.mainTargetRadius = this.property.getMainTargetRadius();
    }

    /**
     * Checks if the main target at least partially is in the field of view at a date
     * 
     * @param date
     *        the date of the computation
     * @return true if the main target is in the field of view at this date
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public boolean isMainTargetInField(final AbsoluteDate date) throws PatriusException {

        // direction vector of this target in the sensor's frame
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();
        final Vector3D targetInFrame = this.mainTarget.getPVCoordinates(date, sensorFrame).getPosition();

        // Compute the transformation between sensor frame and ellipsoid frame
        final Transform transform = sensorFrame.getTransformTo(FramesFactory.getGCRF(), date);

        // get spacecraft position in ellipsoid frame
        final Vector3D position = transform.getTranslation().negate();

        // get sensor sight axis in ellipsoid frame
        final Vector3D sightAxisInEllFrame = transform.transformVector(this.property.getInSightAxis());

        // Create the virtual "occulted body" (twice the distance between sensor and main target)
        final double distance = this.mainTarget.getPVCoordinates(date, FramesFactory.getGCRF())
            .getPosition().subtract(position).getNorm();

        final Vector3D occultedDir = position.add(sightAxisInEllFrame.scalarMultiply(2. * distance));
        final PVCoordinates pvDir = new PVCoordinates(occultedDir, Vector3D.ZERO);
        final BasicPVCoordinatesProvider pvProv = new BasicPVCoordinatesProvider(pvDir, sensorFrame);

        // angular distance check
        final double value = MathLib.divide(
            this.mainTargetRadius.getLocalRadius(position, FramesFactory.getGCRF(), date, pvProv),
            targetInFrame.getNorm());
        final double angularRadius = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));
        final double correctedAngularDist = this.mainField.getAngularDistance(targetInFrame) + angularRadius;
        return correctedAngularDist > 0.0;
    }

    /**
     * Checks if at least an inhibition target is at least partially in its associated
     * inhibition field at a date
     * 
     * @param date
     *        the date of the computation
     * @return false if one of the targets is in its field
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public boolean noInhibition(final AbsoluteDate date) throws PatriusException {

        // initialisations
        Vector3D targetInFrame;
        boolean result = true;
        int i = 0;
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();

        // Compute the transformation between sensor frame and ellipsoid frame
        final Transform transform = sensorFrame.getTransformTo(FramesFactory.getGCRF(), date);

        // get spacecraft position in ellipsoid frame
        final Vector3D position = transform.getTranslation().negate();

        // get sensor sight axis in ellipsoid frame
        final Vector3D sightAxisInEllFrame = transform.transformVector(this.property.getInSightAxis());

        // loop on each field / target
        // while no target is in its field
        while (i < this.length && result) {
            // Create the virtual "occulted body" (twice the distance between sensor and target)
            final double distance = this.inhibitionTargets[i].getPVCoordinates(date, FramesFactory.getGCRF())
                .getPosition().subtract(position).getNorm();

            final Vector3D occultedDir = position.add(sightAxisInEllFrame.scalarMultiply(2. * distance));
            final PVCoordinates pvDir = new PVCoordinates(occultedDir, Vector3D.ZERO);
            final BasicPVCoordinatesProvider pvProv = new BasicPVCoordinatesProvider(pvDir, sensorFrame);

            targetInFrame = this.inhibitionTargets[i].getPVCoordinates(date, sensorFrame).getPosition();
            final double value = MathLib.divide(
                this.inhibitionTargetsRadiuses[i].getLocalRadius(position, FramesFactory.getGCRF(), date, pvProv),
                targetInFrame.getNorm());
            final double angularRadius = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));
            final double correctedAngularDist =
                this.inhibitionFields[i].getAngularDistance(targetInFrame) + angularRadius;
            result = (correctedAngularDist < 0.0);
            i++;
        }

        return result;
    }

    /**
     * Checks if the main target is in the field of view and no inhibition target in its inhibition field
     * at a given date.
     * 
     * @param date
     *        the date of the computation
     * @return true if the main target is in the field of view and no inhibition target in its inhibition field.
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public boolean visibilityOk(final AbsoluteDate date) throws PatriusException {
        return this.noInhibition(date) && this.isMainTargetInField(date);
    }

    /**
     * Computes the angular distance of the CENTER of the main target to the border of the main field
     * of view at a date. The result is positive if the center of the target is in the field. Please
     * refer the specific used field's javadoc for details.
     * 
     * @param date
     *        the date of the computation
     * @return the angular distance
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double getTargetCenterFOVAngle(final AbsoluteDate date) throws PatriusException {
        return this.mainField.getAngularDistance(this.getTargetVectorInSensorFrame(date));
    }

    /**
     * Computes the angular distance of the CENTER of an inhibition target to the border
     * of the associated inhibition field
     * at a date. The result is positive if the center of the target is in the field. Please
     * refer the specific used field's javadoc for details.
     * 
     * @param date
     *        the date of the computation
     * @param inhibitionFieldNumber
     *        number of the inhibition field to consider (first is 1)
     * @return the angular distance
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double getInhibitTargetCenterToFieldAngle(final AbsoluteDate date,
                                                     final int inhibitionFieldNumber) throws PatriusException {

        // direction vector of this target in the sensor's frame
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();
        final Vector3D targetInFrame = this.inhibitionTargets[inhibitionFieldNumber - 1]
            .getPVCoordinates(date, sensorFrame).getPosition();

        return this.inhibitionFields[inhibitionFieldNumber - 1].getAngularDistance(targetInFrame);
    }

    /**
     * Computes the sight axis of the sensor in a given frame at a date
     * 
     * @param frame
     *        the frame of expression
     * @param date
     *        the date of the computation
     * @return the sight axis vector
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public Vector3D getSightAxis(final Frame frame, final AbsoluteDate date) throws PatriusException {
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();
        return sensorFrame.getTransformTo(frame, date).transformVector(this.property.getInSightAxis());
    }

    /**
     * Computes the reference axis of the sensor in a given frame at a date
     * 
     * @param frame
     *        the frame of expression
     * @param date
     *        the date of the computation
     * @return the reference axis vectors
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public Vector3D[] getRefrenceAxis(final Frame frame, final AbsoluteDate date) throws PatriusException {
        final Vector3D[] result = this.property.getReferenceAxis();

        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();
        final Transform trans = sensorFrame.getTransformTo(frame, date);

        // each reference axis is transformed
        for (int i = 0; i < result.length; i++) {
            result[i] = trans.transformVector(result[i]);
        }
        return result;
    }

    /**
     * Computes the target vector at a date in the sensor's frame.
     * 
     * @param date
     *        the computation date
     * @return the target vector
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public Vector3D getTargetVectorInSensorFrame(final AbsoluteDate date) throws PatriusException {

        // direction vector of this target in the sensor's frame
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();
        return this.mainTarget.getPVCoordinates(date, sensorFrame).getPosition();
    }

    /**
     * Computes the target vector at a date in the sensor's frame.
     * The vector is then normalised (the X, Y and Z coefficients of
     * this vector are so the directing cosine).
     * 
     * @param date
     *        the computation date
     * @return the target vector
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public Vector3D getNormalisedTargetVectorInSensorFrame(final AbsoluteDate date) throws PatriusException {
        return this.getTargetVectorInSensorFrame(date).normalize();
    }

    /**
     * Computes the dihedral angles of the target at a date in the sensor's frame.
     * The array is filled with {AX, AY, AZ}.
     * 
     * @param date
     *        the computation date
     * @return the dihedral angles
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double[] getTargetDihedralAngles(final AbsoluteDate date) throws PatriusException {
        final Vector3D targetInFrame = this.getTargetVectorInSensorFrame(date);
        return new double[] { MathLib.atan2(targetInFrame.getZ(), targetInFrame.getY()),
            MathLib.atan2(targetInFrame.getX(), targetInFrame.getZ()),
            MathLib.atan2(targetInFrame.getY(), targetInFrame.getX()) };
    }

    /**
     * @param date
     *        the date of computation
     * @return the vector angle (target - sight axis) at this date.
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double getTargetSightAxisAngle(final AbsoluteDate date) throws PatriusException {
        return Vector3D.angle(this.getNormalisedTargetVectorInSensorFrame(date), this.property.getInSightAxis());
    }

    /**
     * @param date
     *        the date of computation
     * @param axisNumber
     *        the number of the reference axis for this computation (first is 1)
     * @return the vector angle (target - reference axis) at this date.
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double getTargetRefAxisAngle(final AbsoluteDate date, final int axisNumber) throws PatriusException {
        // right reference axis
        final Vector3D refAxis = this.property.getReferenceAxis()[axisNumber - 1];

        return Vector3D.angle(this.getNormalisedTargetVectorInSensorFrame(date), refAxis);
    }

    /**
     * @param date
     *        the date of computation
     * @return the elevation angle (target - normal plane to the sight axis) at this date.
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double getTargetSightAxisElevation(final AbsoluteDate date) throws PatriusException {
        return MathUtils.HALF_PI - this.getTargetSightAxisAngle(date);
    }

    /**
     * @param date
     *        the date of computation
     * @param axisNumber
     *        the number of the reference axis for this computation (first is 1)
     * @return the elevation angle (target - normal plane to the reference axis) at this date.
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double getTargetRefAxisElevation(final AbsoluteDate date, final int axisNumber) throws PatriusException {
        return MathUtils.HALF_PI - this.getTargetRefAxisAngle(date, axisNumber);
    }

    /**
     * @return the assembly of this sensor
     */
    public Assembly getAssembly() {
        return this.inAssembly;
    }

    /**
     * @return the number of couples inhibition field / inhibition target
     */
    public int getInhibitionFieldsNumber() {
        return this.length;
    }

    /**
     * Sets the main target of the sensor property.
     * 
     * @param target
     *        the new main target center
     * @param radius
     *        the target's radius
     */
    public void setMainTarget(final PVCoordinatesProvider target, final LocalRadiusProvider radius) {
        this.property.setMainTarget(target, radius);
        this.mainTarget = target;
        this.mainTargetRadius = radius;
    }

    /**
     * Computes the angular radius from the sensor of the main target at a date.
     * 
     * @param date
     *        the computation date
     * @return the angular radius
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double getMainTargetAngularRadius(final AbsoluteDate date) throws PatriusException {
        // angular distance check

        // Sensor frame
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();

        // Compute the transformation between sensor frame and ellipsoid frame
        final Transform transform = sensorFrame.getTransformTo(FramesFactory.getGCRF(), date);

        // get spacecraft position in ellipsoid frame
        final Vector3D position = transform.getTranslation().negate();

        // get sensor sight axis in ellipsoid frame
        final Vector3D sightAxisInEllFrame = transform.transformVector(this.property.getInSightAxis());

        // Create the virtual "occulted body" (twice the distance between sensor and main target)
        final double distance = this.mainTarget.getPVCoordinates(date, FramesFactory.getGCRF())
            .getPosition().subtract(position).getNorm();

        final Vector3D occultedDir = position.add(sightAxisInEllFrame.scalarMultiply(2. * distance));
        final PVCoordinates pvDir = new PVCoordinates(occultedDir, Vector3D.ZERO);
        final BasicPVCoordinatesProvider pvProv = new BasicPVCoordinatesProvider(pvDir, FramesFactory.getGCRF());

        final double value = MathLib.divide(this.mainTargetRadius.getLocalRadius(position, FramesFactory.getGCRF(),
            date, pvProv), this.getTargetVectorInSensorFrame(date).getNorm());
        return MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));
    }

    /**
     * Computes the angular radius from the sensor of the main target at a date.
     * 
     * @param date
     *        the computation date
     * @param inhibitionFieldNumber
     *        number of the inhibition field to consider (first is 1)
     * @return the angular radius
     * @throws PatriusException
     *         if some frame transformation problem occurs
     *         (if the assembly is not linked to the tree of frame in witch the target is defined)
     */
    public double getInhibitionTargetAngularRadius(final AbsoluteDate date,
                                                   final int inhibitionFieldNumber) throws PatriusException {
        // direction vector of this target in the sensor's frame
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();
        final Vector3D targetInFrame = this.inhibitionTargets[inhibitionFieldNumber - 1]
            .getPVCoordinates(date, sensorFrame).getPosition();

        // Compute the transformation between sensor frame and ellipsoid frame
        final Transform transform = sensorFrame.getTransformTo(FramesFactory.getGCRF(), date);

        // get spacecraft position in ellipsoid frame
        final Vector3D position = transform.getTranslation().negate();

        // get sensor sight axis in ellipsoid frame
        final Vector3D sightAxisInEllFrame = transform.transformVector(this.property.getInSightAxis());

        // Create the virtual "occulted body" (twice the distance between sensor and target)
        final double distance = this.inhibitionTargets[inhibitionFieldNumber - 1].getPVCoordinates(date,
            FramesFactory.getGCRF()).getPosition().subtract(position).getNorm();

        final Vector3D occultedDir = position.add(sightAxisInEllFrame.scalarMultiply(2. * distance));
        final PVCoordinates pvDir = new PVCoordinates(occultedDir, Vector3D.ZERO);
        final BasicPVCoordinatesProvider pvProv = new BasicPVCoordinatesProvider(pvDir, sensorFrame);

        final double value = MathLib.divide(this.inhibitionTargetsRadiuses[inhibitionFieldNumber - 1].getLocalRadius(
            position, FramesFactory.getGCRF(), date, pvProv), targetInFrame.getNorm());
        return MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));
    }

    /**
     * Get the {@link PVCoordinates} of the sensor part in the selected frame.
     * 
     * @param date
     *        current date
     * @param frame
     *        the frame where to define the position
     * @return position/velocity of the body (m and m/s)
     * @exception PatriusException
     *            if position cannot be computed in given frame : occurs
     *            if the assembly is not correctly linked to the main tree of frames.
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        // sensor part's frame
        final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
        final Frame sensorFrame = sensorPart.getFrame();

        // transformation to output frame
        final Transform t = sensorFrame.getTransformTo(frame, date);
        final PVCoordinates centerNullCoordinates = new PVCoordinates();

        // sensor center in output frame
        return t.transformPVCoordinates(centerNullCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date,
            final Frame frame) throws PatriusException {
        return this.inAssembly.getPart(this.inPartName).getFrame();
    }

    /**
     * Computes the minimal euclidian distance to the spacecraft's shapes (GEOMERTY properties).
     * If the line between the sensor and the target intersects the shape, a negative value
     * is returned in order to compute events detections.
     * 
     * @param date
     *        the current computation date
     * @return the minimal distance to the spacecraft's shapes
     * @throws PatriusException
     *         if a problem occurs in frames transformations
     */
    public double spacecraftsMaskingDistance(final AbsoluteDate date) throws PatriusException {

        // updates of all the orbits and attitudes
        for (final SecondarySpacecraft assembly : this.maskingAssemblies) {
            assembly.updateSpacecraftState(date);
        }

        // initialization
        double distance = Double.POSITIVE_INFINITY;

        if (!this.maskingParts.isEmpty()) {
            // sensor part's frame
            final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
            final Frame sensorFrame = sensorPart.getFrame();
            final Vector3D targetInSensorFrame = this.getTargetVectorInSensorFrame(date);
            final double targetDistance = targetInSensorFrame.getNorm();

            // Loop on all parts
            int currentNumber = 0;
            for (final List<IPart> partList : this.maskingParts) {
                for (final IPart part : partList) {
                    // the part's shape
                    final GeometricProperty geomProperty =
                        (GeometricProperty) (part.getProperty(PropertyType.GEOMETRY));
                    final SolidShape shape = geomProperty.getShape();

                    // the transformation to the part's frame
                    final Frame partFrame = part.getFrame();
                    final Transform trans = sensorFrame.getTransformTo(partFrame, date);

                    // the line sensor - target in the part's frame
                    final Vector3D sensorPosInPartFrame = trans.transformPosition(Vector3D.ZERO);
                    final Vector3D targetVectorInPartFrame = trans.transformVector(targetInSensorFrame);

                    final Line line = new Line(sensorPosInPartFrame,
                        sensorPosInPartFrame.add(targetVectorInPartFrame));

                    // distance computation
                    final double distToShape = this.maskingDistFromShape(line, trans.getInverse(), shape,
                        targetDistance, targetInSensorFrame);

                    if (Comparators.lowerStrict(distToShape, distance)) {
                        this.maskingSpacecraftNumber = currentNumber;
                        this.maskingPartName = part.getName();
                        this.maskingSpacecraft = new PVCoordinatesProvider() {
                            /** {@inheritDoc} */
                            @Override
                            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                    final Frame frame) throws PatriusException {
                                return part.getFrame().getTransformTo(frame, date).getCartesian();
                            }

                            /** {@inheritDoc} */
                            @Override
                            public Frame getNativeFrame(final AbsoluteDate date,
                                    final Frame frame) throws PatriusException {
                                return part.getFrame();
                            }
                        };
                    }
                    distance = MathLib.min(distToShape, distance);
                }
                currentNumber++;
            }
        }

        // Return result
        return distance;
    }

    /**
     * Computes the minimal euclidian distance to the celestial body shapes.
     * If the line between the sensor and the target intersects the shape, a negative value
     * is returned in order to compute events detections.
     * 
     * @param date
     *        the current computation date
     * @return the minimal distance to the body shapes
     * @throws PatriusException
     *         if a problem occurs in frames transformations
     */
    public double celestialBodiesMaskingDistance(final AbsoluteDate date) throws PatriusException {

        // initialization
        double distance = Double.POSITIVE_INFINITY;

        final int maskingBodiesNumber = this.maskingBodies.size();

        if (maskingBodiesNumber > 0) {

            // sensor part's frame
            final IPart sensorPart = this.inAssembly.getPart(this.inPartName);
            final Frame sensorFrame = sensorPart.getFrame();
            final Vector3D targetInSensorFrame = this.getTargetVectorInSensorFrame(date);
            final double targetDistance = targetInSensorFrame.getNorm();

            final Line line = new Line(Vector3D.ZERO, targetInSensorFrame);

            // minimal distance to the masking bobies
            for (int i = 0; i < maskingBodiesNumber; i++) {
                final double distToBody = this.bodyShapeMaskingDistance(
                    this.maskingBodies.get(i), date, line, sensorFrame, targetDistance, targetInSensorFrame);
                distance = MathLib.min(distance, distToBody);
                this.maskingBodyName = this.maskingBodies.get(i).getName();
                this.maskingBody = this.maskingBodies.get(i);
            }
        }

        // Return result
        return distance;
    }

    /**
     * @return the last masking spacecraft name
     */
    public String getMaskingSpacecraftName() {
        if (this.maskingSpacecraftNumber == this.ownSpacecraftIndex) {
            return "main spacecraft";
        }
        return this.maskingSpacecraftNames.get(this.maskingSpacecraftNumber);
    }

    /**
     * @return the last masking spacecraft's part name
     */
    public String getMaskingSpacecraftPartName() {
        return this.maskingPartName;
    }

    /**
     * @return the last masking body number
     */
    public String getMaskingBodyName() {
        return this.maskingBodyName;
    }

    /**
     * Returns the last masking spacecraft.
     * @return the last masking spacecraft
     */
    public PVCoordinatesProvider getMaskingSpacecraft() {
        return this.maskingSpacecraft;
    }

    /**
     * Returns the last masking body.
     * @return the last masking body
     */
    public PVCoordinatesProvider getMaskingBody() {
        return this.maskingBody;
    }

    /**
     * Computes the distance between a line and a shape, with a negative value if
     * the line intersects the shape.
     * 
     * @param line
     *        the line in the shape's frame
     * @param trans
     *        the frame transformation from the shape frame to the sensor frame
     * @param shape
     *        the shape to consider
     * @param targetDistance
     *        the sensor - target distance
     * @param targetInSensorFrame
     *        target vector in sensor frame
     * @return the distance
     */
    private double maskingDistFromShape(final Line line, final Transform trans,
                                        final SolidShape shape, final double targetDistance,
                                        final Vector3D targetInSensorFrame) {

        // distance to the shape
        double distToShape = 0.;

        // intersection points
        final Vector3D[] points = shape.getIntersectionPoints(line);

        // if the line intersects the shape, the returned distance is negative, and
        // equal to minus the distance between the intersection points.
        if (points.length > 1) {

            // first intersection point in sensor frame
            final Vector3D firstPointInSensorFrame = trans.transformPosition(points[0]);
            final double distToMask = firstPointInSensorFrame.getNorm();

            final boolean shapeIsBehindSensor =
                (Vector3D.dotProduct(targetInSensorFrame, firstPointInSensorFrame) < 0.);
            final boolean shapeIsBehindTarget = (targetDistance < distToMask);

            // if the masking shape is behind the target : no masking !
            // if the masking shape is behind the sensor : no masking !
            if (shapeIsBehindTarget || shapeIsBehindSensor) {
                distToShape = 1.;
            } else {
                // test of the distance of the masking shape
                distToShape = -points[1].distance(points[0]);
            }
        } else if (points.length == 0) {
            // if the line does not intersect the shape, the right distance is returned
            distToShape = shape.distanceTo(line);
        } else if (shape instanceof Plate) {
            // if the line intersect the shape, the shape being a plate, minus
            // the minimal distance to the edges of the plate is returned
            final LineSegment[] edges = ((Plate) shape).getEdges();
            double egdesDist = Double.POSITIVE_INFINITY;
            for (final LineSegment egde : edges) {
                egdesDist = MathLib.min(egde.distanceTo(line), egdesDist);
            }

            // intersection point in sensor frame
            final Vector3D firstPointInSensorFrame = trans.transformPosition(points[0]);
            final double distToMask = firstPointInSensorFrame.getNorm();
            final boolean shapeIsBehindTarget = (targetDistance < distToMask);
            final boolean shapeIsBehindSensor =
                (Vector3D.dotProduct(targetInSensorFrame, firstPointInSensorFrame) < 0.);

            // if the masking shape is behind the target : no masking !
            // if the masking shape is behind the sensor : no masking !
            if (shapeIsBehindTarget || shapeIsBehindSensor) {
                distToShape = 1.;
            } else {
                // test of the distance of the masking shape
                distToShape = -egdesDist;
            }
        }

        return distToShape;
    }

    /**
     * Computes the minimal distance to a celestial body shape.
     * If the line between the sensor and the target intersects the body, a negative value
     * is returned in order to compute events detections.
     * 
     * @param body
     *        the celestial body's shape
     * @param date
     *        the date
     * @param line
     *        the line sensor - target
     * @param sensorFrame
     *        the sensor part local frame
     * @param targetDistance
     *        distance from the sensor to the target
     * @param targetInSensorFrame
     *        the target position in the sensor local frame
     * @return the minimal distance
     * @throws PatriusException
     *         if a problem occurs in frames transformations
     */
    private double bodyShapeMaskingDistance(final GeometricBodyShape body, final AbsoluteDate date, final Line line,
            final Frame sensorFrame, final double targetDistance, final Vector3D targetInSensorFrame)
            throws PatriusException {

        // distance to the shape
        double distLineToBody = 0.;

        // intersection points
        final Vector3D[] points = body.getIntersectionPoints(line, sensorFrame, date);

        if (points.length == 0) {
            distLineToBody = body.distanceTo(line, sensorFrame, date);
        } else if (Vector3D.dotProduct(points[0], targetInSensorFrame) > 0.) {

            // if the line intersects the body, the returned distance is negative, and
            // equal to minus the distance between the closest intersection points
            // and the target.
            if (points.length > 1) {
                // distance to the target
                double distToMask = Double.POSITIVE_INFINITY;
                int index = 0;
                for (int i = 0; i < points.length; i++) {
                    if (points[i].getNorm() < distToMask) {
                        distToMask = points[i].getNorm();
                        index = i;
                    }
                }
                if (distToMask < targetDistance) {
                    distLineToBody = -points[index].distance(targetInSensorFrame);
                } else {
                    // if the masking body is behind the target : no masking !
                    distLineToBody = 1.;
                }
            }
        } else {
            // if the masking body is behind the sensor : no masking !
            distLineToBody = 1.;
        }

        return distLineToBody;
    }

    /**
     * Enables the masking by the considered spacecraft's own parts, by giving the names
     * of the parts that can cause maskings.
     * 
     * @param partsNames
     *        the names of the considered parts
     */
    public void addOwnMaskingParts(final String[] partsNames) {
        final List<IPart> ownMaskingParts = new ArrayList<IPart>();
        for (final String partsName : partsNames) {
            final IPart part = this.inAssembly.getPart(partsName);
            if (!part.hasProperty(PropertyType.GEOMETRY)) {
                throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_GEOMETRY_PROPERTY);
            }
            ownMaskingParts.add(part);
        }
        this.maskingParts.add(ownMaskingParts);
        this.ownSpacecraftIndex = this.maskingParts.indexOf(ownMaskingParts);
    }

    /**
     * Enables the masking by a secondary spacecraft's parts, by giving the names
     * of the parts that can cause maskings.
     * 
     * @param spacecraft
     *        the secondary masking spacecraft
     * @param partsNames
     *        partsNames the names of the considered parts
     */
    public void addSecondaryMaskingSpacecraft(final SecondarySpacecraft spacecraft, final String[] partsNames) {
        final Assembly assembly = spacecraft.getAssembly();
        final List<IPart> spacecraftMaskingParts = new ArrayList<IPart>();
        for (final String partsName : partsNames) {
            final IPart part = assembly.getPart(partsName);
            if (!part.hasProperty(PropertyType.GEOMETRY)) {
                throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_NO_GEOMETRY_PROPERTY);
            }
            spacecraftMaskingParts.add(part);
        }
        this.maskingParts.add(spacecraftMaskingParts);
        this.maskingSpacecraftNames.add(spacecraft.getName());
        this.maskingAssemblies.add(spacecraft);
    }

    /**
     * Adds a celestial body shape to consider in maskings.
     * 
     * @param body
     *        the celestial body shape to consider
     */
    public void addMaskingCelestialBody(final GeometricBodyShape body) {
        this.maskingBodies.add(body);
    }

    /**
     * Returns the main target.
     * @return the main target
     */
    public PVCoordinatesProvider getMainTarget() {
        return mainTarget;
    }
    
    /**
     * Returns inhibition field number #i.
     * Warning: no check is performed if provided number is beyond limits.
     * @param inhibitionFieldNumber number of the inhibition field to consider (first is 1)
     * @return inhibition field number #i
     */
    public PVCoordinatesProvider getInhibitionTarget(final int inhibitionFieldNumber) {
        return inhibitionTargets[inhibitionFieldNumber];
    }

    /**
     * @description This class is only used in the tests of the package Directions. This
     *              is a basic PVCoordinatesProvider : the attributes are a PVCoordinates object that can
     *              be returned and its frame of expression.
     * 
     * @concurrency not thread-safe
     * 
     * @author Thomas Rodrigues
     * 
     * @since 3.3
     * 
     */
    private static class BasicPVCoordinatesProvider implements PVCoordinatesProvider {

        /** PVCoordinates point. */
        private final PVCoordinates coordinates;

        /** Expression frame. */
        private final Frame frame;

        /**
         * Build a direction from an origin and a target described by their
         * PVCoordinatesProvider
         * 
         * @param inCoordinates
         *        the PVCoordinates
         * @param inFrame
         *        the frame in which the coordinates are expressed
         * */
        public BasicPVCoordinatesProvider(final PVCoordinates inCoordinates, final Frame inFrame) {

            // Initialisation
            this.coordinates = inCoordinates;
            this.frame = inFrame;
        }

        /** {@inheritDoc} */
        @Override
        public final PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                                    final Frame frameOut) throws PatriusException {

            // the coordinates are expressed in the output frame
            final Transform toOutputFrame = this.frame.getTransformTo(frameOut, date);
            return toOutputFrame.transformPVCoordinates(this.coordinates);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date,
                final Frame frameIn) throws PatriusException {
            return this.frame;
        }
    }
}
