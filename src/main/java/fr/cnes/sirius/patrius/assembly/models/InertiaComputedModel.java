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
 * @history creation 26/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc.
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:706:06/01/2017: synchronisation problem with the Assemby mass
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.IInertiaProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassEquation;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is an inertia model computed from the inertia properties of each
 * parts of an Assembly object.
 * 
 * <p>
 * Note : when using this model within a propagation, it is necessary to feed the additional equations to the
 * propagator. This has to be done prior to any propagation, to allow this model to account mass variations (i.e. due to
 * maneuvers), using the method NumericalPropagator.setMassProviderEquation() which will register the additional
 * equation and initialize the initial additional state.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the direct use of a not thread-safe Assembly makes this class
 *                      not thread-safe itself
 * 
 * @see IInertiaModel
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class InertiaComputedModel implements IInertiaModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -4635905728890694771L;

    /** the mass center */
    private Vector3D center;

    /** the inertia matrix */
    private Matrix3D matrix;

    /** Map of parts with inertia prop */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, IInertiaProperty> prs = new TreeMap<>();

    /** Map of parts with inertia prop */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, MassEquation> eqs = new TreeMap<>();

    /** List of parts name */
    private final List<String> partsNames = new ArrayList<>();

    /** Assembly */
    private final Assembly inAssembly;

    /**
     * Constructor for the computed inertia model.
     * 
     * @param assembly
     *        the considered assembly
     */
    public InertiaComputedModel(final Assembly assembly) {

        this.inAssembly = assembly;
        String temp;

        // loop on each part that contains a mass property
        for (final IPart part : assembly.getParts().values()) {
            if (part.hasProperty(PropertyType.INERTIA)) {
                // part name as key
                temp = part.getName();
                this.prs.put(temp, (IInertiaProperty) part.getProperty(PropertyType.INERTIA));
                this.eqs.put(temp, new MassEquation(temp));
                this.partsNames.add(temp);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getMassCenter(final Frame frame, final AbsoluteDate date) throws PatriusException {
        this.updateMassCenter(frame, date);
        return new Vector3D(1.0, this.center);
    }

    /** {@inheritDoc} */
    @Override
    public Matrix3D getInertiaMatrix(final Frame frame, final AbsoluteDate date) throws PatriusException {
        this.updateInertia(frame, date);
        return this.matrix.multiply(1.0);
    }

    /**
     * Updates the mass center and inertia matrix from the current state
     * of the assembly.
     * 
     * @param frame
     *        the expression frame
     * @param date
     *        the current date
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     */
    private void updateMassCenter(final Frame frame, final AbsoluteDate date) throws PatriusException {

        // Initializations
        double mass = 0.0;
        double temp;
        this.center = new Vector3D(0.0, 0.0, 0.0);
        Vector3D ponderedMassCenter = Vector3D.ZERO;

        // loop on each part that contains a mass property
        for (final Entry<String, IInertiaProperty> set : this.prs.entrySet()) {
            // adding the mass value
            temp = set.getValue().getMass();
            mass += temp;

            // pondered mass center vectorial sum adding
            final Transform toFrame = this.inAssembly.getPart(set.getKey()).getFrame().getTransformTo(frame, date);
            final Vector3D massCenterInFrame = toFrame.transformPosition(set.getValue().getMassCenter());
            ponderedMassCenter = ponderedMassCenter.add(temp, massCenterInFrame);
        }

        // mass center
        this.center = this.center.add(ponderedMassCenter.scalarMultiply(MathLib.divide(1.0, mass)));
    }

    /**
     * Updates the inertia matrix from the current state
     * of the assembly.
     * 
     * @param frame
     *        the expression frame
     * @param date
     *        the current date
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     */
    private void updateInertia(final Frame frame, final AbsoluteDate date) throws PatriusException {

        // initialisations
        final double[][] data = { { 0.0, 0.0, 0.0 }, { 0.0, 0.0, 0.0 }, { 0.0, 0.0, 0.0 } };
        this.matrix = new Matrix3D(data);

        this.updateMassCenter(frame, date);

        // loop on each part that contains a mass property
        for (final Entry<String, IInertiaProperty> set : this.prs.entrySet()) {

            // frame and mass center
            final Transform toFrame = this.inAssembly.getPart(set.getKey()).getFrame().getTransformTo(frame, date);
            final Vector3D massCenterInFrame = toFrame.transformPosition(set.getValue().getMassCenter());

            // frames transformations matrix
            final Rotation rot = toFrame.getRotation();
            final Matrix3D toPartFrameMatrix = new Matrix3D(rot.revert().getMatrix());
            final Matrix3D toWorkFrameMatrix = new Matrix3D(rot.getMatrix());

            // part inertia matrix
            final Matrix3D partInertia = set.getValue().getInertiaMatrix();

            // matrix frame transformation
            Matrix3D addedMatrix = toPartFrameMatrix.multiply(partInertia.multiply(toWorkFrameMatrix));
            final Matrix3D crossVectorMatrix =
                new Matrix3D(massCenterInFrame.negate().add(this.center));
            addedMatrix =
                addedMatrix
                    .subtract(crossVectorMatrix.multiply(crossVectorMatrix).multiply(set.getValue().getMass()));

            this.matrix = this.matrix.add(addedMatrix);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Matrix3D getInertiaMatrix(final Frame frame, final AbsoluteDate date,
                                     final Vector3D inertiaReferencePoint) throws PatriusException {

        this.updateInertia(frame, date);
        final Matrix3D crossVectorMatrix = new Matrix3D(inertiaReferencePoint.subtract(this.center));
        return this.matrix.subtract(crossVectorMatrix.multiply(crossVectorMatrix)
            .multiply(this.getTotalMass()));
    }

    /** {@inheritDoc} */
    @Override
    public void addMassDerivative(final String partName, final double flowRate) {
        this.checkPartProperty(partName);
        this.eqs.get(partName).addMassDerivative(flowRate);
    }

    /** {@inheritDoc} */
    @Override
    public void setMassDerivativeZero(final String partName) {
        this.checkPartProperty(partName);
        this.eqs.get(partName).setMassDerivativeZero();
    }

    /**
     * Make sure the given part has a mass property
     * 
     * @param partName
     *        name of part subject to mass flow variation
     */
    private void checkPartProperty(final String partName) {
        if (!this.prs.containsKey(partName)) {
            throw new IllegalArgumentException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateMass(final String partName, final double mass) throws PatriusException {
        this.checkPartProperty(partName);
        this.prs.get(partName).getMassProperty().updateMass(mass);
    }

    /** {@inheritDoc} */
    @Override
    public double getMass(final String partName) {
        this.checkPartProperty(partName);
        return this.prs.get(partName).getMass();
    }

    /** {@inheritDoc} */
    @Override
    public double getTotalMass() {

        double res = 0;

        for (final IInertiaProperty p : this.prs.values()) {
            res += p.getMass();
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double getTotalMass(final SpacecraftState state) {

        double mass = 0.;
        // Try to retrieve mass from state
        if (state.getAdditionalStatesMass().size() == 0) {
            // Mass is not in state, retrieve mass from mass provider
            mass = this.getTotalMass();
        } else {
            for (final double[] massi : state.getAdditionalStatesMass().values()) {
                mass += massi[0];
            }
        }

        return mass;
    }

    /** {@inheritDoc} */
    @Override
    public AdditionalEquations getAdditionalEquation(final String name) {
        this.checkPartProperty(name);
        return this.eqs.get(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAllPartsNames() {
        return this.partsNames;
    }

}
